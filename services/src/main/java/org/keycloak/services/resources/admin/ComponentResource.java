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

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

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
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.ComponentTypeRepresentation;
import org.keycloak.representations.idm.ConfigPropertyRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;

/**
 * @resource Component
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ComponentResource {
    protected static final Logger logger = Logger.getLogger(ComponentResource.class);

    protected final RealmModel realm;

    private final AdminPermissionEvaluator auth;

    private final AdminEventBuilder adminEvent;

    protected final ClientConnection clientConnection;

    protected final KeycloakSession session;

    protected final HttpHeaders headers;

    public ComponentResource(KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.session = session;
        this.auth = auth;
        this.realm = session.getContext().getRealm();
        this.adminEvent = adminEvent.resource(ResourceType.COMPONENT);
        this.clientConnection = session.getContext().getConnection();
        this.headers = session.getContext().getRequestHeaders();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.COMPONENT)
    @Operation()
    public Stream<ComponentRepresentation> getComponents(@QueryParam("parent") String parent,
                                                       @QueryParam("type") String type,
                                                       @QueryParam("name") String name) {
        auth.realm().requireViewRealm();
        Stream<ComponentModel> components;
        if (parent == null && type == null) {
            components = realm.getComponentsStream();

        } else if (type == null) {
            components = realm.getComponentsStream(parent);
        } else if (parent == null) {
            components = realm.getComponentsStream(realm.getId(), type);
        } else {
            components = realm.getComponentsStream(parent, type);
        }

        return components
                .filter(component -> Objects.isNull(name) || Objects.equals(component.getName(), name))
                .map(component -> {
                    try {
                        return ModelToRepresentation.toRepresentation(session, component, false);
                    } catch (Exception e) {
                        logger.error("Failed to get component list for component model " + component.getName() + " of realm " + realm.getName());
                        return ModelToRepresentation.toRepresentationWithoutConfig(component);
                    }
                });
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.COMPONENT)
    @Operation()
    public Response create(ComponentRepresentation rep) {
        auth.realm().requireManageRealm();
        try {
            ComponentModel model = RepresentationToModel.toModel(session, rep);
            if (model.getParentId() == null) model.setParentId(realm.getId());

            model = realm.addComponentModel(model);

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), model.getId()).representation(rep).success();
            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(model.getId()).build()).build();
        } catch (ComponentValidationException e) {
            return localizedErrorResponse(e);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid provider type or no such provider", e);
        }
    }

    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    @Tag(name = KeycloakOpenAPI.Admin.Tags.COMPONENT)
    @Operation()
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.COMPONENT)
    @Operation()
    public Response updateComponent(@PathParam("id") String id, ComponentRepresentation rep) {
        auth.realm().requireManageRealm();
        try {
            ComponentModel model = realm.getComponent(id);
            if (model == null) {
                throw new NotFoundException("Could not find component");
            }
            RepresentationToModel.updateComponent(session, rep, model, false);
            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
            realm.updateComponent(model);
            return Response.noContent().build();
        } catch (ComponentValidationException e) {
            return localizedErrorResponse(e);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid provider type or no such provider", e);
        }
    }
    @DELETE
    @Path("{id}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.COMPONENT)
    @Operation()
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
        throw ErrorResponse.error(message, Response.Status.BAD_REQUEST);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.COMPONENT)
    @Operation( summary = "List of subcomponent types that are available to configure for a particular parent component.")
    public Stream<ComponentTypeRepresentation> getSubcomponentConfig(@PathParam("id") String parentId, @QueryParam("type") String subtype) {
        auth.realm().requireViewRealm();
        ComponentModel parent = realm.getComponent(parentId);
        if (parent == null) {
            throw new NotFoundException("Could not find parent component");
        }
        if (subtype == null) {
            throw new BadRequestException("must specify a subtype");
        }
        Class<? extends Provider> providerClass;
        try {
            providerClass = (Class<? extends Provider>)Class.forName(subtype);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        return session.getKeycloakSessionFactory().getProviderFactoriesStream(providerClass)
            .filter(ComponentFactory.class::isInstance)
            .map(factory -> toComponentTypeRepresentation(factory, parent));
    }

    private ComponentTypeRepresentation toComponentTypeRepresentation(ProviderFactory factory, ComponentModel parent) {
        ComponentTypeRepresentation rep = new ComponentTypeRepresentation();
        rep.setId(factory.getId());

        ComponentFactory componentFactory = (ComponentFactory)factory;

        rep.setHelpText(componentFactory.getHelpText());
        List<ProviderConfigProperty> props;
        Map<String, Object> metadata;
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
        return rep;
    }
}
