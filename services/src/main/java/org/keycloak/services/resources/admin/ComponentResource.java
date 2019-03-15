/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import javax.ws.rs.NotFoundException;
import org.keycloak.common.ClientConnection;
import org.keycloak.component.ComponentFactory;
import org.keycloak.component.ComponentModel;
import org.keycloak.component.ComponentValidationException;
import org.keycloak.component.SubComponentFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.models.utils.StripSecretsUtils;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * @resource Component
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ComponentResource {
    protected static final Logger logger = Logger.getLogger(ComponentResource.class);

    protected RealmModel realm;

    private AdminPermissionEvaluator auth;

    private AdminEventBuilder adminEvent;

    @Context
    protected ClientConnection clientConnection;

    @Context
    protected KeycloakSession session;

    @Context
    protected HttpHeaders headers;

    public ComponentResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.COMPONENT);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ComponentRepresentation> getComponents(@QueryParam("parent") String parent,
                                                       @QueryParam("type") String type,
                                                       @QueryParam("name") String name) {
        auth.realm().requireViewRealm();
        List<ComponentModel> components = Collections.EMPTY_LIST;
        if (parent == null && type == null) {
            components = realm.getComponents();

        } else if (type == null) {
            components = realm.getComponents(parent);
        } else if (parent == null) {
            components = realm.getComponents(realm.getId(), type);
        } else {
            components = realm.getComponents(parent, type);
        }
        List<ComponentRepresentation> reps = new LinkedList<>();
        for (ComponentModel component : components) {
            if (name != null && !name.equals(component.getName())) continue;
            ComponentRepresentation rep = null;
            try {
                rep = ModelToRepresentation.toRepresentation(session, component, false);
            } catch (Exception e) {
                logger.error("Failed to get component list for component model" + component.getName() + "of realm " + realm.getName());
                rep = ModelToRepresentation.toRepresentationWithoutConfig(component);
            }
            reps.add(rep);
        }
        return reps;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(ComponentRepresentation rep) {
        auth.realm().requireManageRealm();
        try {
            ComponentModel model = RepresentationToModel.toModel(session, rep);
            if (model.getParentId() == null) model.setParentId(realm.getId());

            model = realm.addComponentModel(model);

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), model.getId()).representation(StripSecretsUtils.strip(session, rep)).success();
            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
        } catch (ComponentValidationException e) {
            return localizedErrorResponse(e);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e);
        }
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ComponentRepresentation getComponent(@PathParam("id") String id) {
        auth.realm().requireViewRealm();
        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        ComponentRepresentation rep = ModelToRepresentation.toRepresentation(session, model, false);
        return rep;
    }

    @PUT
    @Path("{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateComponent(@PathParam("id") String id, ComponentRepresentation rep) {
        auth.realm().requireManageRealm();
        try {
            ComponentModel model = realm.getComponent(id);
            if (model == null) {
                throw new NotFoundException("Could not find component");
            }
            RepresentationToModel.updateComponent(session, rep, model, false);
            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(StripSecretsUtils.strip(session, rep)).success();
            realm.updateComponent(model);
            return Response.noContent().build();
        } catch (ComponentValidationException e) {
            return localizedErrorResponse(e);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException();
        }
    }
    @DELETE
    @Path("{id}")
    public void removeComponent(@PathParam("id") String id) {
        auth.realm().requireManageRealm();
        ComponentModel model = realm.getComponent(id);
        if (model == null) {
            throw new NotFoundException("Could not find component");
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
        realm.removeComponent(model);

    }

    private Response localizedErrorResponse(ComponentValidationException cve) {
        Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale(), "admin-messages", "messages");

        Object[] localizedParameters = cve.getParameters()==null ? null : Arrays.asList(cve.getParameters()).stream().map((Object parameter) -> {

            if (parameter instanceof String) {
                String paramStr = (String) parameter;
                return messages.getProperty(paramStr, paramStr);
            } else {
                return parameter;
            }

        }).toArray();

        String message = MessageFormat.format(messages.getProperty(cve.getMessage(), cve.getMessage()), localizedParameters);
        return ErrorResponse.error(message, Response.Status.BAD_REQUEST);
    }

    /**
     * List of subcomponent types that are available to configure for a particular parent component.
     *
     * @param parentId
     * @param subtype
     * @return
     */
    @GET
    @Path("{id}/sub-component-types")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ComponentTypeRepresentation> getSubcomponentConfig(@PathParam("id") String parentId, @QueryParam("type") String subtype) {
        auth.realm().requireViewRealm();
        ComponentModel parent = realm.getComponent(parentId);
        if (parent == null) {
            throw new NotFoundException("Could not find parent component");
        }
        if (subtype == null) {
            throw new BadRequestException("must specify a subtype");
        }
        Class<? extends Provider> providerClass = null;
        try {
            providerClass = (Class<? extends Provider>)Class.forName(subtype);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        List<ComponentTypeRepresentation> subcomponents = new LinkedList<>();
        for (ProviderFactory factory : session.getKeycloakSessionFactory().getProviderFactories(providerClass)) {
            ComponentTypeRepresentation rep = new ComponentTypeRepresentation();
            rep.setId(factory.getId());
            if (!(factory instanceof ComponentFactory)) {
                continue;
            }
            ComponentFactory componentFactory = (ComponentFactory)factory;

            rep.setHelpText(componentFactory.getHelpText());
            List<ProviderConfigProperty> props = null;
            Map<String, Object> metadata = null;
            if (factory instanceof SubComponentFactory) {
                props = ((SubComponentFactory)factory).getConfigProperties(realm, parent);
                metadata = ((SubComponentFactory)factory).getTypeMetadata(realm, parent);

            } else {
                props = componentFactory.getConfigProperties();
                metadata = componentFactory.getTypeMetadata();
            }

            List<ConfigPropertyRepresentation> propReps =  ModelToRepresentation.toRepresentation(props);
            rep.setProperties(propReps);
            rep.setMetadata(metadata);
            subcomponents.add(rep);
        }
        return subcomponents;
    }



}
