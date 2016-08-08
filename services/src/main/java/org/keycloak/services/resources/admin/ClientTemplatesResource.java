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
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.ClientTemplateRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

/**
 * Base resource class for managing a realm's client templates.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientTemplatesResource {
    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;
    protected RealmModel realm;
    private RealmAuth auth;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ClientTemplatesResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_TEMPLATE);

        auth.init(RealmAuth.Resource.CLIENT);
    }

    /**
     * Get client templates belonging to the realm
     *
     * Returns a list of client templates belonging to the realm
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ClientTemplateRepresentation> getClientTemplates() {
        auth.requireView();

        List<ClientTemplateRepresentation> rep = new ArrayList<>();
        List<ClientTemplateModel> clientModels = realm.getClientTemplates();

        boolean view = auth.hasView();
        for (ClientTemplateModel clientModel : clientModels) {
            if (view) {
                rep.add(ModelToRepresentation.toRepresentation(clientModel));
            } else {
                ClientTemplateRepresentation client = new ClientTemplateRepresentation();
                client.setId(clientModel.getId());
                client.setName(clientModel.getName());
                client.setDescription(clientModel.getDescription());
                client.setProtocol(clientModel.getProtocol());
                rep.add(client);
            }
        }
        return rep;
    }

    /**
     * Create a new client template
     *
     * Client Template's name must be unique!
     *
     * @param uriInfo
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClientTemplate(final @Context UriInfo uriInfo, final ClientTemplateRepresentation rep) {
        auth.requireManage();

        try {
            ClientTemplateModel clientModel = RepresentationToModel.createClientTemplate(session, realm, rep);

            adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo, clientModel.getId()).representation(rep).success();

            return Response.created(uriInfo.getAbsolutePathBuilder().path(clientModel.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client Template " + rep.getName() + " already exists");
        }
    }

    /**
     * Base path for managing a specific client template.
     *
     * @param id id of client template (not name)
     * @return
     */
    @Path("{id}")
    public ClientTemplateResource getClient(final @PathParam("id") String id) {
        ClientTemplateModel clientModel = realm.getClientTemplateById(id);
        ClientTemplateResource clientResource = new ClientTemplateResource(realm, auth, clientModel, session, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientResource);
        return clientResource;
    }

}
