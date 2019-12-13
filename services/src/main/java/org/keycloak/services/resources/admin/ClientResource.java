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
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.authorization.admin.AuthorizationService;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientregistration.ClientRegistrationTokenUtils;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissionManagement;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;
import org.keycloak.services.validation.ClientValidator;
import org.keycloak.services.validation.PairwiseClientValidator;
import org.keycloak.services.validation.ValidationMessages;
import org.keycloak.utils.ProfileHelper;
import org.keycloak.validation.ClientValidationUtil;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.Boolean.TRUE;


/**
 * Base resource class for managing one particular client of a realm.
 *
 * @resource Clients
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientResource {
    protected static final Logger logger = Logger.getLogger(ClientResource.class);
    protected RealmModel realm;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    protected ClientModel client;
    protected KeycloakSession session;

    @Context
    protected KeycloakApplication keycloak;

    @Context
    protected ClientConnection clientConnection;

    protected KeycloakApplication getKeycloakApplication() {
        return keycloak;
    }

    public ClientResource(RealmModel realm, AdminPermissionEvaluator auth, ClientModel clientModel, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.client = clientModel;
        this.session = session;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT);
    }

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.clients().requireManage(client);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.clients().requireView(client);
        ProtocolMappersResource mappers = new ProtocolMappersResource(realm, client, auth, adminEvent, manageCheck, viewCheck);
        ResteasyProviderFactory.getInstance().injectProperties(mappers);
        return mappers;
    }

    /**
     * Update the client
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(final ClientRepresentation rep) {
        auth.clients().requireConfigure(client);

        ValidationMessages validationMessages = new ValidationMessages();
        if (!ClientValidator.validate(rep, validationMessages) || !PairwiseClientValidator.validate(session, rep, validationMessages)) {
            Properties messages = AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale());
            throw new ErrorResponseException(
                    validationMessages.getStringMessages(),
                    validationMessages.getStringMessages(messages),
                    Response.Status.BAD_REQUEST
            );
        }

        try {
            updateClientFromRep(rep, client, session);

            ClientValidationUtil.validate(session, client, false, c -> {
                session.getTransactionManager().setRollbackOnly();
                throw new ErrorResponseException(Errors.INVALID_INPUT ,c.getError(), Response.Status.BAD_REQUEST);
            });

            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client already exists");
        }
    }

    /**
     * Get representation of the client
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation getClient() {
        auth.clients().requireView(client);

        ClientRepresentation representation = ModelToRepresentation.toRepresentation(client, session);

        representation.setAccess(auth.clients().getAccess(client));

        return representation;
    }

    /**
     * Get representation of certificate resource
     *
     * @param attributePrefix
     * @return
     */
    @Path("certificates/{attr}")
    public ClientAttributeCertificateResource getCertficateResource(@PathParam("attr") String attributePrefix) {
        return new ClientAttributeCertificateResource(realm, auth, client, session, attributePrefix, adminEvent);
    }

    @GET
    @NoCache
    @Path("installation/providers/{providerId}")
    public Response getInstallationProvider(@PathParam("providerId") String providerId) {
        auth.clients().requireView(client);

        ClientInstallationProvider provider = session.getProvider(ClientInstallationProvider.class, providerId);
        if (provider == null) throw new NotFoundException("Unknown Provider");
        return provider.generateInstallation(session, realm, client, session.getContext().getUri().getBaseUri());
    }

    /**
     * Delete the client
     *
     */
    @DELETE
    @NoCache
    public void deleteClient() {
        auth.clients().requireManage(client);

        if (client == null) {
            throw new NotFoundException("Could not find client");
        }

        new ClientManager(new RealmManager(session)).removeClient(realm, client);
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
    }


    /**
     * Generate a new secret for the client
     *
     * @return
     */
    @Path("client-secret")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CredentialRepresentation regenerateSecret() {
        auth.clients().requireConfigure(client);

        logger.debug("regenerateSecret");
        UserCredentialModel cred = KeycloakModelUtils.generateSecret(client);
        CredentialRepresentation rep = ModelToRepresentation.toRepresentation(cred);
        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(rep).success();
        return rep;
    }

    /**
     * Generate a new registration access token for the client
     *
     * @return
     */
    @Path("registration-access-token")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public ClientRepresentation regenerateRegistrationAccessToken() {
        auth.clients().requireManage(client);

        String token = ClientRegistrationTokenUtils.updateRegistrationAccessToken(session, realm, client, RegistrationAuth.AUTHENTICATED);

        ClientRepresentation rep = ModelToRepresentation.toRepresentation(client, session);
        rep.setRegistrationAccessToken(token);

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(rep).success();
        return rep;
    }

    /**
     * Get the client secret
     *
     * @return
     */
    @Path("client-secret")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialRepresentation getClientSecret() {
        auth.clients().requireView(client);

        logger.debug("getClientSecret");
        UserCredentialModel model = UserCredentialModel.secret(client.getSecret());
        if (model == null) throw new NotFoundException("Client does not have a secret");
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Base path for managing the scope mappings for the client
     *
     * @return
     */
    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.clients().requireManage(client);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.clients().requireView(client);
        return new ScopeMappedResource(realm, auth, client, session, adminEvent, manageCheck, viewCheck);
    }

    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(session, session.getContext().getUri(), realm, auth, client, adminEvent);
    }


    /**
     * Get default client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-client-scopes")
    public List<ClientScopeRepresentation> getDefaultClientScopes() {
        return getDefaultClientScopes(true);
    }

    private List<ClientScopeRepresentation> getDefaultClientScopes(boolean defaultScope) {
        auth.clients().requireView(client);

        List<ClientScopeRepresentation> defaults = new LinkedList<>();
        for (ClientScopeModel clientScope : client.getClientScopes(defaultScope, true).values()) {
            ClientScopeRepresentation rep = new ClientScopeRepresentation();
            rep.setId(clientScope.getId());
            rep.setName(clientScope.getName());
            defaults.add(rep);
        }
        return defaults;
    }


    @PUT
    @NoCache
    @Path("default-client-scopes/{clientScopeId}")
    public void addDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId,true);
    }

    private void addDefaultClientScope(String clientScopeId, boolean defaultScope) {
        auth.clients().requireManage(client);

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new javax.ws.rs.NotFoundException("Client scope not found");
        }
        client.addClientScope(clientScope, defaultScope);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.CLIENT).resourcePath(session.getContext().getUri()).success();
    }


    @DELETE
    @NoCache
    @Path("default-client-scopes/{clientScopeId}")
    public void removeDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        auth.clients().requireManage(client);

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new javax.ws.rs.NotFoundException("Client scope not found");
        }
        client.removeClientScope(clientScope);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.CLIENT).resourcePath(session.getContext().getUri()).success();
    }


    /**
     * Get optional client scopes.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("optional-client-scopes")
    public List<ClientScopeRepresentation> getOptionalClientScopes() {
        return getDefaultClientScopes(false);
    }

    @PUT
    @NoCache
    @Path("optional-client-scopes/{clientScopeId}")
    public void addOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId, false);
    }

    @DELETE
    @NoCache
    @Path("optional-client-scopes/{clientScopeId}")
    public void removeOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        removeDefaultClientScope(clientScopeId);
    }

    @Path("evaluate-scopes")
    public ClientScopeEvaluateResource clientScopeEvaluateResource() {
        return new ClientScopeEvaluateResource(session, session.getContext().getUri(), realm, auth, client, clientConnection);
    }

    /**
     * Get a user dedicated to the service account
     *
     * @return
     */
    @Path("service-account-user")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getServiceAccountUser() {
        auth.clients().requireView(client);

        UserModel user = session.users().getServiceAccount(client);
        if (user == null) {
            if (client.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(client);
                user = session.users().getServiceAccount(client);
            } else {
                throw new BadRequestException("Service account not enabled for the client '" + client.getClientId() + "'");
            }
        }

        return ModelToRepresentation.toRepresentation(session, realm, user);
    }

    /**
     * Push the client's revocation policy to its admin URL
     *
     * If the client has an admin URL, push revocation policy to it.
     */
    @Path("push-revocation")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public GlobalRequestResult pushRevocation() {
        auth.clients().requireConfigure(client);

        adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).resource(ResourceType.CLIENT).success();
        return new ResourceAdminManager(session).pushClientRevocationPolicy(realm, client);

    }

    /**
     * Get application session count
     *
     * Returns a number of user sessions associated with this client
     *
     * {
     *     "count": number
     * }
     *
     * @return
     */
    @Path("session-count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getApplicationSessionCount() {
        auth.clients().requireView(client);

        Map<String, Long> map = new HashMap<>();
        map.put("count", session.sessions().getActiveUserSessions(client.getRealm(), client));
        return map;
    }

    /**
     * Get user sessions for client
     *
     * Returns a list of user sessions associated with this client
     *
     * @param firstResult Paging offset
     * @param maxResults Maximum results size (defaults to 100)
     * @return
     */
    @Path("user-sessions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSessionRepresentation> getUserSessions(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults) {
        auth.clients().requireView(client);

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;
        List<UserSessionRepresentation> sessions = new ArrayList<UserSessionRepresentation>();
        for (UserSessionModel userSession : session.sessions().getUserSessions(client.getRealm(), client, firstResult, maxResults)) {
            UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(userSession);
            sessions.add(rep);
        }
        return sessions;
    }

    /**
     * Get application offline session count
     *
     * Returns a number of offline user sessions associated with this client
     *
     * {
     *     "count": number
     * }
     *
     * @return
     */
    @Path("offline-session-count")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Long> getOfflineSessionCount() {
        auth.clients().requireView(client);

        Map<String, Long> map = new HashMap<>();
        map.put("count", session.sessions().getOfflineSessionsCount(client.getRealm(), client));
        return map;
    }

    /**
     * Get offline sessions for client
     *
     * Returns a list of offline user sessions associated with this client
     *
     * @param firstResult Paging offset
     * @param maxResults Maximum results size (defaults to 100)
     * @return
     */
    @Path("offline-sessions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSessionRepresentation> getOfflineUserSessions(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults) {
        auth.clients().requireView(client);

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;
        List<UserSessionRepresentation> sessions = new ArrayList<UserSessionRepresentation>();
        List<UserSessionModel> userSessions = session.sessions().getOfflineUserSessions(client.getRealm(), client, firstResult, maxResults);
        for (UserSessionModel userSession : userSessions) {
            UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(userSession);

            // Update lastSessionRefresh with the timestamp from clientSession
            for (Map.Entry<String, AuthenticatedClientSessionModel> csEntry : userSession.getAuthenticatedClientSessions().entrySet()) {
                String clientUuid = csEntry.getKey();
                AuthenticatedClientSessionModel clientSession = csEntry.getValue();

                if (client.getId().equals(clientUuid)) {
                    rep.setLastAccess(Time.toMillis(clientSession.getTimestamp()));
                    break;
                }
            }

            sessions.add(rep);
        }
        return sessions;
    }

    /**
     * Register a cluster node with the client
     *
     * Manually register cluster node to this client - usually it's not needed to call this directly as adapter should handle
     * by sending registration request to Keycloak
     *
     * @param formParams
     */
    @Path("nodes")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void registerNode(Map<String, String> formParams) {
        auth.clients().requireConfigure(client);

        String node = formParams.get("node");
        if (node == null) {
            throw new BadRequestException("Node not found in params");
        }
        if (logger.isDebugEnabled()) logger.debug("Register node: " + node);
        client.registerNode(node, Time.currentTime());
        adminEvent.operation(OperationType.CREATE).resource(ResourceType.CLUSTER_NODE).resourcePath(session.getContext().getUri(), node).success();
    }

    /**
     * Unregister a cluster node from the client
     *
     * @param node
     */
    @Path("nodes/{node}")
    @DELETE
    @NoCache
    public void unregisterNode(final @PathParam("node") String node) {
        auth.clients().requireConfigure(client);

        if (logger.isDebugEnabled()) logger.debug("Unregister node: " + node);

        Integer time = client.getRegisteredNodes().get(node);
        if (time == null) {
            throw new NotFoundException("Client does not have node ");
        }
        client.unregisterNode(node);
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.CLUSTER_NODE).resourcePath(session.getContext().getUri()).success();
    }

    /**
     * Test if registered cluster nodes are available
     *
     * Tests availability by sending 'ping' request to all cluster nodes.
     *
     * @return
     */
    @Path("test-nodes-available")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GlobalRequestResult testNodesAvailable() {
        auth.clients().requireConfigure(client);

        logger.debug("Test availability of cluster nodes");
        GlobalRequestResult result = new ResourceAdminManager(session).testNodesAvailability(realm, client);
        adminEvent.operation(OperationType.ACTION).resource(ResourceType.CLUSTER_NODE).resourcePath(session.getContext().getUri()).representation(result).success();
        return result;
    }

    @Path("/authz")
    public AuthorizationService authorization() {
        AuthorizationService resource = new AuthorizationService(this.session, this.client, this.auth, adminEvent);

        ResteasyProviderFactory.getInstance().injectProperties(resource);

        return resource;
    }

    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     * @return
     */
    @Path("management/permissions")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference getManagementPermissions() {
        auth.roles().requireView(client);

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.clients().isPermissionsEnabled(client)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(client, permissions);
    }

    public static ManagementPermissionReference toMgmtRef(ClientModel client, AdminPermissionManagement permissions) {
        ManagementPermissionReference ref = new ManagementPermissionReference();
        ref.setEnabled(true);
        ref.setResource(permissions.clients().resource(client).getId());
        ref.setScopePermissions(permissions.clients().getPermissions(client));
        return ref;
    }


    /**
     * Return object stating whether client Authorization permissions have been initialized or not and a reference
     *
     *
     * @return initialized manage permissions reference
     */
    @Path("management/permissions")
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public ManagementPermissionReference setManagementPermissionsEnabled(ManagementPermissionReference ref) {
        auth.clients().requireManage(client);
        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.clients().setPermissionsEnabled(client, ref.isEnabled());
        if (ref.isEnabled()) {
            return toMgmtRef(client, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }


    private void updateClientFromRep(ClientRepresentation rep, ClientModel client, KeycloakSession session) throws ModelDuplicateException {
        UserModel serviceAccount = this.session.users().getServiceAccount(client);
        if (TRUE.equals(rep.isServiceAccountsEnabled())) {
            if (serviceAccount == null) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            }
        }
        else {
            if (serviceAccount != null) {
                new UserManager(session).removeUser(realm, serviceAccount);
            }
        }

        if (!rep.getClientId().equals(client.getClientId())) {
            new ClientManager(new RealmManager(session)).clientIdChanged(client, rep.getClientId());
        }

        if (rep.isFullScopeAllowed() != null && rep.isFullScopeAllowed() != client.isFullScopeAllowed()) {
            auth.clients().requireManage(client);
        }

        if ((rep.isBearerOnly() != null && rep.isBearerOnly()) || (rep.isPublicClient() != null && rep.isPublicClient())) {
            rep.setAuthorizationServicesEnabled(false);
        }

        RepresentationToModel.updateClient(rep, client);
        updateAuthorizationSettings(rep);
    }

    private void updateAuthorizationSettings(ClientRepresentation rep) {
        if (TRUE.equals(rep.getAuthorizationServicesEnabled())) {
            authorization().enable(false);
        } else {
            authorization().disable();
        }
    }
}
