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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientTemplateModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.resources.KeycloakApplication;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;


/**
 * Base resource class for managing one particular client of a realm.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientTemplateResource {
    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;
    protected RealmModel realm;
    private RealmAuth auth;
    private AdminEventBuilder adminEvent;
    protected ClientTemplateModel template;
    protected KeycloakSession session;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakApplication keycloak;

    protected KeycloakApplication getKeycloakApplication() {
        return keycloak;
    }

    public ClientTemplateResource(RealmModel realm, RealmAuth auth, ClientTemplateModel template, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.template = template;
        this.session = session;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_TEMPLATE);

        auth.init(RealmAuth.Resource.CLIENT);
    }

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers() {
        ProtocolMappersResource mappers = new ProtocolMappersResource(template, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(mappers);
        return mappers;
    }

    /**
     * Base path for managing the scope mappings for the client
     *
     * @return
     */
    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        return new ScopeMappedResource(realm, auth, template, session, adminEvent);
    }

    /**
     * Update the client template
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(final ClientTemplateRepresentation rep) {
        auth.requireManage();

        if (template == null) {
            throw new NotFoundException("Could not find client template");
        }

        try {
            RepresentationToModel.updateClientTemplate(rep, template);
            adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(rep).success();
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client Template " + rep.getName() + " already exists");
        }
    }


    /**
     * Get representation of the client template
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ClientTemplateRepresentation getClient() {
        auth.requireView();

        if (template == null) {
            throw new NotFoundException("Could not find client template");
        }

        return ModelToRepresentation.toRepresentation(template);
    }

    /**
     * Delete the client template
     *
     */
    @DELETE
    @NoCache
    public Response deleteClientTemplate() {
        auth.requireManage();

        if (template == null) {
            throw new NotFoundException("Could not find client template");
        }

        try {
            realm.removeClientTemplate(template.getId());
            adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
            return Response.noContent().build();
        } catch (ModelException me) {
            return ErrorResponse.error(me.getMessage(), Response.Status.BAD_REQUEST);
        }
    }



}
