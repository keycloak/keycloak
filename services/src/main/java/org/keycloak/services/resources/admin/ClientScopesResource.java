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
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Base resource class for managing a realm's client scopes.
 *
 * @resource Client Scopes
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientScopesResource {
    protected static final Logger logger = Logger.getLogger(ClientScopesResource.class);
    protected RealmModel realm;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public ClientScopesResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT_SCOPE);
    }

    /**
     * Get client scopes belonging to the realm
     *
     * Returns a list of client scopes belonging to the realm
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public List<ClientScopeRepresentation> getClientScopes() {
        auth.clients().requireListClientScopes();

        List<ClientScopeRepresentation> rep = new ArrayList<>();
        List<ClientScopeModel> clientModels = realm.getClientScopes();

        boolean viewable = auth.clients().canViewClientScopes();
        for (ClientScopeModel clientModel : clientModels) {
            if (viewable) rep.add(ModelToRepresentation.toRepresentation(clientModel));
            else {
                ClientScopeRepresentation tempRep = new ClientScopeRepresentation();
                tempRep.setName(clientModel.getName());
                tempRep.setId(clientModel.getId());
                tempRep.setProtocol(clientModel.getProtocol());
            }
        }
        return rep;
    }

    /**
     * Create a new client scope
     *
     * Client Scope's name must be unique!
     *
     * @param rep
     * @return
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response createClientScope(ClientScopeRepresentation rep) {
        auth.clients().requireManageClientScopes();

        try {
            ClientScopeModel clientModel = RepresentationToModel.createClientScope(session, realm, rep);

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri(), clientModel.getId()).representation(rep).success();

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(clientModel.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client Scope " + rep.getName() + " already exists");
        }
    }

    /**
     * Generate new client scope for specified service client. The "Frontend" clients, who will use this client scope, will be able to
     * send their access token to authenticate against specified service client
     *
     * @param clientId Client ID of service client (typically bearer-only client)
     * @return
     */
    @Path("generate-audience-client-scope")
    @POST
    @NoCache
    public Response generateAudienceClientScope(final @QueryParam("clientId") String clientId) {
        auth.clients().requireManageClientScopes();

        logger.debugf("Generating audience scope for service client: " + clientId);

        String clientScopeName = clientId;
        try {
            ClientModel serviceClient = realm.getClientByClientId(clientId);
            if (serviceClient == null) {
                logger.warnf("Referenced service client '%s' doesn't exists", clientId);
                return ErrorResponse.exists("Referenced service client doesn't exists");
            }

            ClientScopeModel clientScopeModel = realm.addClientScope(clientScopeName);
            clientScopeModel.setDescription("Client scope useful for frontend clients, which want to call service " + clientId);
            clientScopeModel.setProtocol(serviceClient.getProtocol()==null ? OIDCLoginProtocol.LOGIN_PROTOCOL : serviceClient.getProtocol());
            clientScopeModel.setDisplayOnConsentScreen(true);

            String consentText = serviceClient.getName() != null ? serviceClient.getName() : serviceClient.getClientId();
            consentText = consentText.substring(0, 1).toUpperCase() + consentText.substring(1);
            clientScopeModel.setConsentScreenText(consentText);

            clientScopeModel.setIncludeInTokenScope(true);

            // Add audience protocol mapper
            ProtocolMapperModel audienceMapper = AudienceProtocolMapper.createClaimMapper("Audience for " + clientId, clientId, null,true, false);
            clientScopeModel.addProtocolMapper(audienceMapper);

            // Add scope to client roles
            for (RoleModel role : serviceClient.getRoles()) {
                clientScopeModel.addScopeMapping(role);
            }

            adminEvent.operation(OperationType.CREATE).resourcePath(session.getContext().getUri()).success();

            return Response.created(session.getContext().getUri().getAbsolutePathBuilder().path(clientScopeModel.getId()).build()).build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client Scope " + clientScopeName + " already exists");
        }
    }

    /**
     * Base path for managing a specific client scope.
     *
     * @param id id of client scope (not name)
     * @return
     */
    @Path("{id}")
    @NoCache
    public ClientScopeResource getClientScope(final @PathParam("id") String id) {
        auth.clients().requireListClientScopes();
        ClientScopeModel clientModel = realm.getClientScopeById(id);
        if (clientModel == null) {
            throw new NotFoundException("Could not find client scope");
        }
        ClientScopeResource clientResource = new ClientScopeResource(realm, auth, clientModel, session, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientResource);
        return clientResource;
    }

}
