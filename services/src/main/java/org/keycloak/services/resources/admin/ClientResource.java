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

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.NoCache;
import org.keycloak.OAuthErrorException;
import org.keycloak.authorization.fgap.AdminPermissionsSchema;
import org.keycloak.authorization.admin.AuthorizationService;
import org.keycloak.client.clienttype.ClientTypeException;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.Profile;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.Time;
import org.keycloak.events.Errors;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.ClientSecretConstants;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.ClientInstallationProvider;
import org.keycloak.protocol.oidc.OIDCClientSecretConfigWrapper;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ManagementPermissionReference;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.AdminClientUnregisterContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdateContext;
import org.keycloak.services.clientpolicy.context.AdminClientUpdatedContext;
import org.keycloak.services.clientpolicy.context.AdminClientViewContext;
import org.keycloak.services.clientpolicy.context.ClientSecretRotationContext;
import org.keycloak.services.clientregistration.ClientRegistrationTokenUtils;
import org.keycloak.services.clientregistration.policy.RegistrationAuth;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.KeycloakOpenAPI;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.fgap.AdminPermissionManagement;
import org.keycloak.services.resources.admin.fgap.AdminPermissions;
import org.keycloak.utils.ProfileHelper;
import org.keycloak.utils.ReservedCharValidator;
import org.keycloak.validation.ValidationUtil;

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
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;


/**
 * Base resource class for managing one particular client of a realm.
 *
 * @resource Clients
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@Extension(name = KeycloakOpenAPI.Profiles.ADMIN, value = "")
public class ClientResource {
    protected static final Logger logger = Logger.getLogger(ClientResource.class);
    protected RealmModel realm;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;
    protected ClientModel client;
    protected final KeycloakSession session;

    protected final ClientConnection clientConnection;

    public ClientResource(RealmModel realm, AdminPermissionEvaluator auth, ClientModel clientModel, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.client = clientModel;
        this.session = session;
        this.adminEvent = adminEvent.resource(ResourceType.CLIENT);
        this.clientConnection = session.getContext().getConnection();
    }

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers() {
        AdminPermissionEvaluator.RequirePermissionCheck manageCheck = () -> auth.clients().requireManage(client);
        AdminPermissionEvaluator.RequirePermissionCheck viewCheck = () -> auth.clients().requireView(client);
        return new ProtocolMappersResource(session, client, auth, adminEvent, manageCheck, viewCheck);
    }

    /**
     * Update the client
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Update the client")
    @APIResponses(value = {
        @APIResponse(responseCode = "204", description = "No Content"),
        @APIResponse(responseCode = "400", description = "Bad Request"),
        @APIResponse(responseCode = "409", description = "Conflict")
    })
    public Response update(final ClientRepresentation rep) {
        auth.clients().requireConfigure(client);

        try {
            session.setAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED,Boolean.FALSE);
            session.clientPolicy().triggerOnEvent(new AdminClientUpdateContext(rep, client, auth.adminAuth()));

            updateClientFromRep(rep, client, session);

            ValidationUtil.validateClient(session, client, false, r -> {
                session.getTransactionManager().setRollbackOnly();
                throw new ErrorResponseException(
                        Errors.INVALID_INPUT,
                        r.getAllLocalizedErrorsAsString(AdminRoot.getMessages(session, realm, auth.adminAuth().getToken().getLocale())),
                        Response.Status.BAD_REQUEST);
            });

            session.clientPolicy().triggerOnEvent(new AdminClientUpdatedContext(rep, client, auth.adminAuth()));

            if (!(boolean) session.getAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED)){
                logger.debugv("Removing the previous rotation info for client {0}{1}, if there is",client.getClientId(),client.getName());
                OIDCClientSecretConfigWrapper.fromClientModel(client, session).removeClientSecretRotationInfo();
            }
            session.removeAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED);

            adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).representation(rep).success();
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            throw ErrorResponse.exists("Client already exists");
        } catch (ClientTypeException cte) {
            throw ErrorResponse.error(cte.getMessage(), cte.getParameters(), Response.Status.BAD_REQUEST);
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get representation of the client")
    public ClientRepresentation getClient() {
        try {
            session.clientPolicy().triggerOnEvent(new AdminClientViewContext(client, auth.adminAuth()));
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

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
        return new ClientAttributeCertificateResource(auth, client, session, attributePrefix, adminEvent);
    }

    @GET
    @NoCache
    @Path("installation/providers/{providerId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation()
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Delete the client")
    public void deleteClient() {
        auth.clients().requireManage(client);

        if (client == null) {
            throw new NotFoundException("Could not find client");
        }

        AdminPermissionsSchema.SCHEMA.throwExceptionIfAdminPermissionClient(session, client.getId());

        try {
            session.clientPolicy().triggerOnEvent(new AdminClientUnregisterContext(client, auth.adminAuth()));
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(), Response.Status.BAD_REQUEST);
        }

        if (new ClientManager(new RealmManager(session)).removeClient(realm, client)) {
            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
        }
        else {
            throw new ErrorResponseException(OAuthErrorException.INVALID_REQUEST, "Could not delete client",
                    Response.Status.BAD_REQUEST);
        }
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Generate a new secret for the client")
    public CredentialRepresentation regenerateSecret() {
        try{
            auth.clients().requireConfigure(client);

            logger.debug("regenerateSecret");
            session.setAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED,Boolean.FALSE);

            ClientRepresentation representation = ModelToRepresentation.toRepresentation(client, session);
            ClientSecretRotationContext secretRotationContext = new ClientSecretRotationContext(
                representation, client, client.getSecret());

            String secret = KeycloakModelUtils.generateSecret(client);

            session.clientPolicy().triggerOnEvent(secretRotationContext);

            CredentialRepresentation rep = new CredentialRepresentation();
            rep.setType(CredentialRepresentation.SECRET);
            rep.setValue(secret);

            if (!(boolean) session.getAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED)){
                logger.debugv("Removing the previous rotation info for client {0}{1}, if there is",client.getClientId(),client.getName());
                OIDCClientSecretConfigWrapper.fromClientModel(client, session).removeClientSecretRotationInfo();
            }

            adminEvent.operation(OperationType.ACTION).resourcePath(session.getContext().getUri()).representation(rep).success();
            session.removeAttribute(ClientSecretConstants.CLIENT_SECRET_ROTATION_ENABLED);
            rep.setValue(secret);
            return rep;
        } catch (ClientPolicyException cpe) {
            throw new ErrorResponseException(cpe.getError(), cpe.getErrorDetail(),
                Response.Status.BAD_REQUEST);
        }
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Generate a new registration access token for the client")
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get the client secret")
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get default client scopes.  Only name and ids are returned.")
    public Stream<ClientScopeRepresentation> getDefaultClientScopes() {
        return getDefaultClientScopes(true);
    }

    private Stream<ClientScopeRepresentation> getDefaultClientScopes(boolean defaultScope) {
        auth.clients().requireView(client);

        return client.getClientScopes(defaultScope).values().stream().map(ClientResource::toRepresentation);
    }


    @PUT
    @NoCache
    @Path("default-client-scopes/{clientScopeId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation()
    public void addDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId,true);
    }

    private void addDefaultClientScope(String clientScopeId, boolean defaultScope) {
        auth.clients().requireManage(client);

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new jakarta.ws.rs.NotFoundException("Client scope not found");
        }
        if (defaultScope && clientScope.isDynamicScope()) {
            throw new ErrorResponseException("invalid_request", "Can't assign a Dynamic Scope to a Client as a Default Scope", Response.Status.BAD_REQUEST);
        }
        client.addClientScope(clientScope, defaultScope);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.CLIENT_SCOPE_CLIENT_MAPPING).resourcePath(session.getContext().getUri()).success();
    }


    @DELETE
    @NoCache
    @Path("default-client-scopes/{clientScopeId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation()
    public void removeDefaultClientScope(@PathParam("clientScopeId") String clientScopeId) {
        auth.clients().requireManage(client);

        ClientScopeModel clientScope = realm.getClientScopeById(clientScopeId);
        if (clientScope == null) {
            throw new jakarta.ws.rs.NotFoundException("Client scope not found");
        }
        client.removeClientScope(clientScope);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.CLIENT_SCOPE_CLIENT_MAPPING).resourcePath(session.getContext().getUri()).success();
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get optional client scopes.  Only name and ids are returned.")
    public Stream<ClientScopeRepresentation> getOptionalClientScopes() {
        return getDefaultClientScopes(false);
    }

    @PUT
    @NoCache
    @Path("optional-client-scopes/{clientScopeId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation()
    public void addOptionalClientScope(@PathParam("clientScopeId") String clientScopeId) {
        addDefaultClientScope(clientScopeId, false);
    }

    @DELETE
    @NoCache
    @Path("optional-client-scopes/{clientScopeId}")
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation()
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get a user dedicated to the service account")
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Push the client's revocation policy to its admin URL If the client has an admin URL, push revocation policy to it.")
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get application session count Returns a number of user sessions associated with this client { \"count\": number }")
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get user sessions for client Returns a list of user sessions associated with this client\n")
    public Stream<UserSessionRepresentation> getUserSessions(@Parameter(description = "Paging offset") @QueryParam("first") Integer firstResult, @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults) {
        auth.clients().requireView(client);

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;
        return session.sessions().getUserSessionsStream(client.getRealm(), client, firstResult, maxResults)
                .map(ModelToRepresentation::toRepresentation);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get application offline session count Returns a number of offline user sessions associated with this client { \"count\": number }")
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get offline sessions for client Returns a list of offline user sessions associated with this client")
    public Stream<UserSessionRepresentation> getOfflineUserSessions(@Parameter(description = "Paging offset") @QueryParam("first") Integer firstResult, @Parameter(description = "Maximum results size (defaults to 100)") @QueryParam("max") Integer maxResults) {
        auth.clients().requireView(client);

        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : Constants.DEFAULT_MAX_RESULTS;

        return session.sessions().getOfflineUserSessionsStream(client.getRealm(), client, firstResult, maxResults)
                .map(this::toUserSessionRepresentation);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Register a cluster node with the client Manually register cluster node to this client - usually it’s not needed to call this directly as adapter should handle by sending registration request to Keycloak")
    @APIResponse(responseCode = "204", description = "No Content")
    public void registerNode(Map<String, String> formParams) {
        auth.clients().requireConfigure(client);

        String node = formParams.get("node");
        if (node == null) {
            throw new BadRequestException("Node not found in params");
        }

        ReservedCharValidator.validate(node);

        logger.debugf("Register node: %s", node);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Unregister a cluster node from the client")
    public void unregisterNode(final @PathParam("node") String node) {
        auth.clients().requireConfigure(client);

        logger.debugf("Unregister node: %s", node);

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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Test if registered cluster nodes are available Tests availability by sending 'ping' request to all cluster nodes.")
    public GlobalRequestResult testNodesAvailable() {
        auth.clients().requireConfigure(client);

        logger.debug("Test availability of cluster nodes");
        GlobalRequestResult result = new ResourceAdminManager(session).testNodesAvailability(realm, client);
        adminEvent.operation(OperationType.ACTION).resource(ResourceType.CLUSTER_NODE).resourcePath(session.getContext().getUri()).representation(result).success();
        return result;
    }

    @Path("/authz")
    public AuthorizationService authorization() {
        ProfileHelper.requireFeature(Profile.Feature.AUTHORIZATION);

        return new AuthorizationService(this.session, this.client, this.auth, adminEvent);
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Return object stating whether client Authorization permissions have been initialized or not and a reference")
    public ManagementPermissionReference getManagementPermissions() {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        auth.roles().requireView(client);

        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        if (!permissions.clients().isPermissionsEnabled(client)) {
            return new ManagementPermissionReference();
        }
        return toMgmtRef(client, permissions);
    }

    private ManagementPermissionReference toMgmtRef(ClientModel client, AdminPermissionManagement permissions) {
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
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Return object stating whether client Authorization permissions have been initialized or not and a reference")
    public ManagementPermissionReference setManagementPermissionsEnabled(ManagementPermissionReference ref) {
        ProfileHelper.requireFeature(Profile.Feature.ADMIN_FINE_GRAINED_AUTHZ);
        auth.clients().requireManage(client);
        AdminPermissionManagement permissions = AdminPermissions.management(session, realm);
        permissions.clients().setPermissionsEnabled(client, ref.isEnabled());
        if (ref.isEnabled()) {
            return toMgmtRef(client, permissions);
        } else {
            return new ManagementPermissionReference();
        }
    }

    /**
     * Invalidate the rotated secret for the client
     *
     * @return
     */
    @Path("client-secret/rotated")
    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Invalidate the rotated secret for the client")
    public Response invalidateRotatedSecret() {
        try{
            auth.clients().requireConfigure(client);

            logger.debug("delete rotated secret");

            OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientModel(client, session);

            CredentialRepresentation rep = new CredentialRepresentation();
            rep.setType(CredentialRepresentation.SECRET);
            rep.setValue(wrapper.getClientRotatedSecret());

            adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).representation(rep).success();

            wrapper.removeClientSecretRotated();

            return Response.noContent().build();
        } catch (RuntimeException rte) {
            throw new ErrorResponseException(rte.getCause().getMessage(), rte.getMessage(),
                Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Get the rotated client secret
     *
     * @return
     */
    @Path("client-secret/rotated")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Tag(name = KeycloakOpenAPI.Admin.Tags.CLIENTS)
    @Operation( summary = "Get the rotated client secret")
    public CredentialRepresentation getClientRotatedSecret() {
        auth.clients().requireView(client);

        logger.debug("getClientRotatedSecret");
        OIDCClientSecretConfigWrapper wrapper = OIDCClientSecretConfigWrapper.fromClientModel(client, session);
        if (!wrapper.hasRotatedSecret())
            throw new NotFoundException("Client does not have a rotated secret");
        else {
            UserCredentialModel model = UserCredentialModel.secret(wrapper.getClientRotatedSecret());
            return ModelToRepresentation.toRepresentation(model);
        }
    }

    private void updateClientFromRep(ClientRepresentation rep, ClientModel client, KeycloakSession session) throws ModelDuplicateException {
        UserModel serviceAccount = this.session.users().getServiceAccount(client);
        boolean serviceAccountScopeAssigned = client.getClientScopes(true).containsKey(ServiceAccountConstants.SERVICE_ACCOUNT_SCOPE);
        if (Boolean.TRUE.equals(rep.isServiceAccountsEnabled())) {
            if (serviceAccount == null || !serviceAccountScopeAssigned) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(client);
            }
        } else if (Boolean.FALSE.equals(rep.isServiceAccountsEnabled()) || !client.isServiceAccountsEnabled()) {
            if (serviceAccount != null || serviceAccountScopeAssigned) {
                new ClientManager(new RealmManager(session)).disableServiceAccount(client);
            }
        }

        if (rep.getClientId() != null && !rep.getClientId().equals(client.getClientId())) {
            new ClientManager(new RealmManager(session)).clientIdChanged(client, rep);
        }

        if (rep.isFullScopeAllowed() != null && rep.isFullScopeAllowed() != client.isFullScopeAllowed()) {
            auth.clients().requireManage(client);
        }

        if ((rep.isBearerOnly() != null && rep.isBearerOnly()) || (rep.isPublicClient() != null && rep.isPublicClient())) {
            rep.setAuthorizationServicesEnabled(false);
        }

        RepresentationToModel.updateClient(rep, client, session);
        RepresentationToModel.updateClientProtocolMappers(rep, client);
        updateAuthorizationSettings(rep);
    }

    private void updateAuthorizationSettings(ClientRepresentation rep) {
        if (Profile.isFeatureEnabled(Profile.Feature.AUTHORIZATION)) {
            if (Boolean.TRUE.equals(rep.getAuthorizationServicesEnabled())) {
                authorization().enable(false);
            } else {
                authorization().disable();
            }
        }
    }

    /**
     * Converts the specified {@link UserSessionModel} into a {@link UserSessionRepresentation}.
     *
     * @param userSession the model to be converted.
     * @return a reference to the constructed representation.
     */
    private UserSessionRepresentation toUserSessionRepresentation(final UserSessionModel userSession) {
        UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(userSession);

        // Update lastSessionRefresh with the timestamp from clientSession
        Map.Entry<String, AuthenticatedClientSessionModel> result = userSession.getAuthenticatedClientSessions().entrySet().stream()
                .filter(entry -> Objects.equals(client.getId(), entry.getKey()))
                .findFirst().orElse(null);
        if (result != null) {
            rep.setLastAccess(Time.toMillis(result.getValue().getTimestamp()));
        }
        return rep;
    }

    private static ClientScopeRepresentation toRepresentation(ClientScopeModel clientScopeModel) {
        ClientScopeRepresentation rep = new ClientScopeRepresentation();
        rep.setId(clientScopeModel.getId());
        rep.setName(clientScopeModel.getName());
        return rep;
    }
}
