package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.ApplicationModel;
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
import org.keycloak.representations.idm.ApplicationRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.services.managers.ApplicationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.resources.KeycloakApplication;
import org.keycloak.services.resources.flows.Flows;
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
 * Base resource class for managing one particular application of a realm.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ApplicationResource {
    protected static final Logger logger = Logger.getLogger(ApplicationResource.class);
    protected RealmModel realm;
    private RealmAuth auth;
    protected ApplicationModel application;
    protected KeycloakSession session;
    @Context
    protected UriInfo uriInfo;

    @Context
    protected KeycloakApplication keycloak;

    protected KeycloakApplication getKeycloakApplication() {
        return (KeycloakApplication)keycloak;
    }

    public ApplicationResource(RealmModel realm, RealmAuth auth, ApplicationModel applicationModel, KeycloakSession session) {
        this.realm = realm;
        this.auth = auth;
        this.application = applicationModel;
        this.session = session;

        auth.init(RealmAuth.Resource.APPLICATION);
    }

    /**
     * base path for managing allowed application claims
     *
     * @return
     */
    @Path("claims")
    public ClaimResource getClaimResource() {
        return new ClaimResource(application, auth);
    }

    @Path("protocol-mappers")
    public ClientProtocolMappersResource getProtocolMappers() {
        ClientProtocolMappersResource mappers = new ClientProtocolMappersResource(realm, auth, application);
        ResteasyProviderFactory.getInstance().injectProperties(mappers);
        //resourceContext.initResource(mappers);
        return mappers;
    }

    /**
     * Update the application.
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    public Response update(final ApplicationRepresentation rep) {
        auth.requireManage();

        try {
            RepresentationToModel.updateApplication(rep, application);
            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Application " + rep.getName() + " already exists");
        }
    }


    /**
     * Get representation of the application.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public ApplicationRepresentation getApplication() {
        auth.requireView();

        return ModelToRepresentation.toRepresentation(application);
    }

    /**
     *
     * @param attributePrefix
     * @return
     */
    @Path("certificates/{attr}")
    public ClientAttributeCertificateResource getCertficateResource(@PathParam("attr") String attributePrefix) {
        return new ClientAttributeCertificateResource(realm, auth, application, session, attributePrefix);
    }


    /**
     * Return keycloak.json file for this application to be used to configure the adapter of that application.
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

        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(session));
        Object rep = applicationManager.toInstallationRepresentation(realm, application, getKeycloakApplication().getBaseUri(uriInfo));

        // TODO Temporary solution to pretty-print
        return JsonSerialization.mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rep);
    }

    /**
     * Return XML that can be included in the JBoss/Wildfly Keycloak subsystem to configure the adapter of that application.
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

        ApplicationManager applicationManager = new ApplicationManager(new RealmManager(session));
        return applicationManager.toJBossSubsystemConfig(realm, application, getKeycloakApplication().getBaseUri(uriInfo));
    }

    /**
     * Delete this application.
     *
     */
    @DELETE
    @NoCache
    public void deleteApplication() {
        auth.requireManage();

        new ApplicationManager(new RealmManager(session)).removeApplication(realm, application);
    }


    /**
     * Generates a new secret for this application
     *
     * @return
     */
    @Path("client-secret")
    @POST
    @Produces("application/json")
    @Consumes("application/json")
    public CredentialRepresentation regenerateSecret() {
        auth.requireManage();

        logger.debug("regenerateSecret");
        UserCredentialModel cred = KeycloakModelUtils.generateSecret(application);
        CredentialRepresentation rep = ModelToRepresentation.toRepresentation(cred);
        return rep;
    }

    /**
     * Get the secret of this application
     *
     * @return
     */
    @Path("client-secret")
    @GET
    @NoCache
    @Produces("application/json")
    public CredentialRepresentation getClientSecret() {
        auth.requireView();

        logger.debug("getClientSecret");
        UserCredentialModel model = UserCredentialModel.secret(application.getSecret());
        if (model == null) throw new NotFoundException("Application does not have a secret");
        return ModelToRepresentation.toRepresentation(model);
    }

    /**
     * Base path for managing the scope mappings for this application
     *
     * @return
     */
    @Path("scope-mappings")
    public ScopeMappedResource getScopeMappedResource() {
        return new ScopeMappedResource(realm, auth, application, session);
    }

    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(realm, auth, application);
    }

    /**
     * Returns set of allowed origin.  This is used for CORS requests.  Access tokens will have
     * their allowedOrigins claim set to this value for tokens created for this application.
     *
     * @return
     */
    @Path("allowed-origins")
    @GET
    @NoCache
    @Produces("application/json")
    public Set<String> getAllowedOrigins()
    {
        auth.requireView();

        return application.getWebOrigins();
    }

    /**
     * Change the set of allowed origins.   This is used for CORS requests.  Access tokens will have
     * their allowedOrigins claim set to this value for tokens created for this application.
     *
     * @param allowedOrigins
     */
    @Path("allowed-origins")
    @PUT
    @Consumes("application/json")
    public void updateAllowedOrigins(Set<String> allowedOrigins)
    {
        auth.requireManage();

        application.setWebOrigins(allowedOrigins);
    }

    /**
     * Remove set of allowed origins from current allowed origins list.  This is used for CORS requests.  Access tokens will have
     * their allowedOrigins claim set to this value for tokens created for this application.
     *
     * @param allowedOrigins
     */
    @Path("allowed-origins")
    @DELETE
    @Consumes("application/json")
    public void deleteAllowedOrigins(Set<String> allowedOrigins)
    {
        auth.requireManage();

        for (String origin : allowedOrigins) {
            application.removeWebOrigin(origin);
        }
    }

    /**
     * If the application has an admin URL, push the application's revocation policy to it.
     *
     */
    @Path("push-revocation")
    @POST
    public GlobalRequestResult pushRevocation() {
        auth.requireManage();
        return new ResourceAdminManager().pushApplicationRevocationPolicy(uriInfo.getRequestUri(), realm, application);
    }

    /**
     * Number of user sessions associated with this application
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
        map.put("count", session.sessions().getActiveUserSessions(application.getRealm(), application));
        return map;
    }

    /**
     * Return a list of user sessions associated with this application
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
        for (UserSessionModel userSession : session.sessions().getUserSessions(application.getRealm(), application, firstResult, maxResults)) {
            UserSessionRepresentation rep = ModelToRepresentation.toRepresentation(userSession);
            sessions.add(rep);
        }
        return sessions;
    }

    /**
     * If the application has an admin URL, invalidate all sessions associated with that application directly.
     *
     */
    @Path("logout-all")
    @POST
    public GlobalRequestResult logoutAll() {
        auth.requireManage();
        return new ResourceAdminManager().logoutApplication(uriInfo.getRequestUri(), realm, application);
    }

    /**
     * If the application has an admin URL, invalidate the sessions for a particular user directly.
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

        new ResourceAdminManager().logoutUserFromApplication(uriInfo.getRequestUri(), realm, application, user, session);
    }

    /**
     * Manually register cluster node to this application - usually it's not needed to call this directly as adapter should handle
     * by sending registration request to Keycloak
     *
     * @param formParams
     */
    @Path("nodes")
    @POST
    @Consumes("application/json")
    public void registerNode(Map<String, String> formParams) {
        auth.requireManage();
        String node = formParams.get("node");
        if (node == null) {
            throw new BadRequestException("Node not found in params");
        }
        if (logger.isDebugEnabled()) logger.debug("Register node: " + node);
        application.registerNode(node, Time.currentTime());
    }

    /**
     * Unregister cluster node from this application
     *
     * @param node
     */
    @Path("nodes/{node}")
    @DELETE
    @NoCache
    public void unregisterNode(final @PathParam("node") String node) {
        auth.requireManage();
        if (logger.isDebugEnabled()) logger.debug("Unregister node: " + node);

        Integer time = application.getRegisteredNodes().get(node);
        if (time == null) {
            throw new NotFoundException("Application does not have a node " + node);
        }

        application.unregisterNode(node);
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

        return new ResourceAdminManager().testNodesAvailability(uriInfo.getRequestUri(), realm, application);
    }

}
