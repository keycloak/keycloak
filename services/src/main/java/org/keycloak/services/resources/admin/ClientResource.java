package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.ErrorResponse;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.Time;

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
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Base resource class for managing one particular client of a realm.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ClientResource {
    protected static final Logger logger = Logger.getLogger(ClientResource.class);
    protected RealmModel realm;
    private RealmAuth auth;
    private AdminEventBuilder adminEvent;
    protected ClientModel client;
    protected KeycloakSession session;
    
    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakApplication keycloak;

    protected KeycloakApplication getKeycloakApplication() {
        return keycloak;
    }

    public ClientResource(RealmModel realm, RealmAuth auth, ClientModel clientModel, KeycloakSession session, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.auth = auth;
        this.client = clientModel;
        this.session = session;
        this.adminEvent = adminEvent;

        auth.init(RealmAuth.Resource.CLIENT);
    }

    @Path("protocol-mappers")
    public ProtocolMappersResource getProtocolMappers() {
        ProtocolMappersResource mappers = new ProtocolMappersResource(client, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(mappers);
        return mappers;
    }

    /**
     * Update the client.
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(final ClientRepresentation rep) {
        auth.requireManage();

        try {
            if (rep.isServiceAccountsEnabled() && !client.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(client);;
            }

            RepresentationToModel.updateClient(rep, client);
            adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(rep).success();
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Client " + rep.getClientId() + " already exists");
        }
    }


    /**
     * Get representation of the client.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation getClient() {
        auth.requireView();
        return ModelToRepresentation.toRepresentation(client);
    }

    /**
     *
     * @param attributePrefix
     * @return
     */
    @Path("certificates/{attr}")
    public ClientAttributeCertificateResource getCertficateResource(@PathParam("attr") String attributePrefix) {
        return new ClientAttributeCertificateResource(realm, auth, client, session, attributePrefix, adminEvent);
    }


    /**
     * Return keycloak.json file for this client to be used to configure the adapter of that client.
     *
     * @return
     * @throws IOException
     */
    @GET
    @NoCache
    @Path("installation/json")
    @Produces(MediaType.APPLICATION_JSON)
    public String getInstallation() throws IOException {
        auth.requireView();

        ClientManager clientManager = new ClientManager(new RealmManager(session));
        Object rep = clientManager.toInstallationRepresentation(realm, client, getKeycloakApplication().getBaseUri(uriInfo));

        // TODO Temporary solution to pretty-print
        return JsonSerialization.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rep);
    }

    /**
     * Return XML that can be included in the JBoss/Wildfly Keycloak subsystem to configure the adapter of that client.
     *
     * @return
     * @throws IOException
     */
    @GET
    @NoCache
    @Path("installation/jboss")
    @Produces(MediaType.TEXT_PLAIN)
    public String getJBossInstallation() throws IOException {
        auth.requireView();

        ClientManager clientManager = new ClientManager(new RealmManager(session));
        return clientManager.toJBossSubsystemConfig(realm, client, getKeycloakApplication().getBaseUri(uriInfo));
    }

    /**
     * Delete this client.
     *
     */
    @DELETE
    @NoCache
    public void deleteClient() {
        auth.requireManage();
        new ClientManager(new RealmManager(session)).removeClient(realm, client);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }


    /**
     * Generates a new secret for this client
     *
     * @return
     */
    @Path("client-secret")
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public CredentialRepresentation regenerateSecret() {
        auth.requireManage();

        logger.debug("regenerateSecret");
        UserCredentialModel cred = KeycloakModelUtils.generateSecret(client);
        CredentialRepresentation rep = ModelToRepresentation.toRepresentation(cred);
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).representation(rep).success();
        return rep;
    }

    /**
     * Get the secret of this client
     *
     * @return
     */
    @Path("client-secret")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public CredentialRepresentation getClientSecret() {
        auth.requireView();

        logger.debug("getClientSecret");
        UserCredentialModel model = UserCredentialModel.secret(client.getSecret());
        if (model == null) throw new NotFoundException("Client does not have a secret");
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Base path for managing the scope mappings for this client
     *
     * @return
     */
    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        return new ScopeMappedResource(realm, auth, client, session, adminEvent);
    }

    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(uriInfo, realm, auth, client, adminEvent);
    }

    /**
     * Returns set of allowed origin.  This is used for CORS requests.  Access tokens will have
     * their allowedOrigins claim set to this value for tokens created for this client.
     *
     * @return
     */
    @Path("allowed-origins")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> getAllowedOrigins()
    {
        auth.requireView();
        return client.getWebOrigins();
    }

    /**
     * Change the set of allowed origins.   This is used for CORS requests.  Access tokens will have
     * their allowedOrigins claim set to this value for tokens created for this client.
     *
     * @param allowedOrigins
     */
    @Path("allowed-origins")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateAllowedOrigins(Set<String> allowedOrigins)
    {
        auth.requireManage();

        client.setWebOrigins(allowedOrigins);
        adminEvent.operation(OperationType.UPDATE).resourcePath(uriInfo).representation(client).success();
    }

    /**
     * Remove set of allowed origins from current allowed origins list.  This is used for CORS requests.  Access tokens will have
     * their allowedOrigins claim set to this value for tokens created for this client.
     *
     * @param allowedOrigins
     */
    @Path("allowed-origins")
    @DELETE
    @Consumes(MediaType.APPLICATION_JSON)
    public void deleteAllowedOrigins(Set<String> allowedOrigins)
    {
        auth.requireManage();

        for (String origin : allowedOrigins) {
            client.removeWebOrigin(origin);
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }

    /**
     * Returns user dedicated to this service account
     *
     * @return
     */
    @Path("service-account-user")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getServiceAccountUser() {
        auth.requireView();

        UserModel user = session.users().getUserByServiceAccountClient(client);
        if (user == null) {
            if (client.isServiceAccountsEnabled()) {
                new ClientManager(new RealmManager(session)).enableServiceAccount(client);
                user = session.users().getUserByServiceAccountClient(client);
            } else {
                throw new BadRequestException("Service account not enabled for the client '" + client.getClientId() + "'");
            }
        }

        return ModelToRepresentation.toRepresentation(user);
    }

    /**
     * If the client has an admin URL, push the client's revocation policy to it.
     *
     */
    @Path("push-revocation")
    @POST
    public GlobalRequestResult pushRevocation() {
        auth.requireManage();
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
        return new ResourceAdminManager(session).pushClientRevocationPolicy(uriInfo.getRequestUri(), realm, client);
    
    }
    
    /**
     * Number of user sessions associated with this client
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
    public Map<String, Integer> getApplicationSessionCount() {
        auth.requireView();
        Map<String, Integer> map = new HashMap<String, Integer>();
        map.put("count", session.sessions().getActiveUserSessions(client.getRealm(), client));
        return map;
    }

    /**
     * Return a list of user sessions associated with this client
     *
     * @return
     */
    @Path("user-sessions")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserSessionRepresentation> getUserSessions(@QueryParam("first") Integer firstResult, @QueryParam("max") Integer maxResults) {
        auth.requireView();
        firstResult = firstResult != null ? firstResult : -1;
        maxResults = maxResults != null ? maxResults : -1;
        List<UserSessionRepresentation> sessions = new ArrayList<UserSessionRepresentation>();
        for (UserSessionModel userSession : session.sessions().getUserSessions(client.getRealm(), client, firstResult, maxResults)) {
            UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(userSession);
            sessions.add(rep);
        }
        return sessions;
    }

    /**
     * If the client has an admin URL, invalidate all sessions associated with that client directly.
     *
     */
    @Path("logout-all")
    @POST
    public GlobalRequestResult logoutAll() {
        auth.requireManage();
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
        return new ResourceAdminManager(session).logoutClient(uriInfo.getRequestUri(), realm, client);

    }

    /**
     * If the client has an admin URL, invalidate the sessions for a particular user directly.
     *
     */
    @Path("logout-user/{username}")
    @POST
    public void logout(final @PathParam("username") String username) {
        auth.requireManage();
        UserModel user = session.users().getUserByUsername(username, realm);
        if (user == null) {
            throw new NotFoundException("User not found");
        }
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
        new ResourceAdminManager(session).logoutUserFromClient(uriInfo.getRequestUri(), realm, client, user);

    }

    /**
     * Manually register cluster node to this client - usually it's not needed to call this directly as adapter should handle
     * by sending registration request to Keycloak
     *
     * @param formParams
     */
    @Path("nodes")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public void registerNode(Map<String, String> formParams) {
        auth.requireManage();
        String node = formParams.get("node");
        if (node == null) {
            throw new BadRequestException("Node not found in params");
        }
        if (logger.isDebugEnabled()) logger.debug("Register node: " + node);
        client.registerNode(node, Time.currentTime());
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
    }

    /**
     * Unregister cluster node from this client
     *
     * @param node
     */
    @Path("nodes/{node}")
    @DELETE
    @NoCache
    public void unregisterNode(final @PathParam("node") String node) {
        auth.requireManage();
        if (logger.isDebugEnabled()) logger.debug("Unregister node: " + node);

        Integer time = client.getRegisteredNodes().get(node);
        if (time == null) {
            throw new NotFoundException("Client does not have node ");
        }
        client.unregisterNode(node);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();
    }

    /**
     * Test if registered cluster nodes are available by sending 'ping' request to all of them
     *
     * @return
     */
    @Path("test-nodes-available")
    @GET
    @NoCache
    public GlobalRequestResult testNodesAvailable() {
        auth.requireManage();
        logger.debug("Test availability of cluster nodes");
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
        return new ResourceAdminManager(session).testNodesAvailability(uriInfo.getRequestUri(), realm, client);

    }

}
