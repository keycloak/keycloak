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
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.ClientManager;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Base resource class for managing a realm's clients.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientsResource {
    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;
    protected RealmModel realm;
    private RealmAuth auth;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ClientsResource(RealmModel realm, RealmAuth auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT);

        auth.init(RealmAuth.Resource.CLIENT);
    }

    /**
     * Get clients belonging to the realm
     *
     * Returns a list of clients belonging to the realm
     *
     * @param clientId filter by clientId
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ClientRepresentation> getClients(@QueryParam("clientId") String clientId) {
        auth.requireAny();

        List<ClientRepresentation> rep = new ArrayList<>();

        if (clientId == null) {
            List<ClientModel> clientModels = realm.getClients();

            boolean view = auth.hasView();
            for (ClientModel clientModel : clientModels) {
                if (view) {
                    rep.add(ModelToRepresentation.toRepresentation(clientModel));
                } else {
                    ClientRepresentation client = new ClientRepresentation();
                    client.setId(clientModel.getId());
                    client.setClientId(clientModel.getClientId());
                    client.setDescription(clientModel.getDescription());
                    rep.add(client);
                }
            }
        } else {
            ClientModel client = realm.getClientByClientId(clientId);
            if (client != null) {
                rep.add(ModelToRepresentation.toRepresentation(client));
            }
        }
        return rep;
    }

    /**
     * Create a new client
     *
     * Client's client_id must be unique!
     *
     * @param uriInfo
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createClient(final @Context UriInfo uriInfo, final ClientRepresentation rep) {
        auth.requireManage();

        try {
            ClientModel clientModel = ClientManager.createClient(session, realm, rep, true);

            adminEvent.operation(OperationType.CREATE).resourcePath(uriInfo, clientModel.getId()).representation(rep).success();

            return Response.created(uriInfo.getAbsolutePathBuilder().path(clientModel.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client " + rep.getClientId() + " already exists");
        }
    }

    /**
     * Base path for managing a specific client.
     *
     * @param id id of client (not client-id)
     * @return
     */
    @Path("{id}")
    public ClientResource getClient(final @PathParam("id") String id) {
        ClientModel clientModel = realm.getClientById(id);

        session.getContext().setClient(clientModel);

        ClientResource clientResource = new ClientResource(realm, auth, clientModel, session, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientResource);
        return clientResource;
    }

}
