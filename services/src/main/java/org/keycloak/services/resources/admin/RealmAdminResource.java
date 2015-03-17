package org.keycloak.services.resources.admin;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.exportimport.ApplicationImporter;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.LDAPConnectionTestManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.services.resources.flows.Flows;
import org.keycloak.timer.TimerProvider;

/**
 * Base resource class for the admin REST api of one realm
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdminResource {
    protected static final Logger logger = Logger.getLogger(RealmAdminResource.class);
    protected RealmAuth auth;
    protected RealmModel realm;
    private TokenManager tokenManager;

    @Context
    protected KeycloakSession session;

    @Context
    protected UriInfo uriInfo;

    public RealmAdminResource(RealmAuth auth, RealmModel realm, TokenManager tokenManager) {
        this.auth = auth;
        this.realm = realm;
        this.tokenManager = tokenManager;

        auth.init(RealmAuth.Resource.REALM);
    }

    /**
     * Base path for importing applications under this realm.
     *
     * @return
     */
    @Path("application-importers/{formatId}")
    public Object getApplicationImporter(@PathParam("formatId") String formatId) {
        ApplicationImporter importer = session.getProvider(ApplicationImporter.class, formatId);
        return importer.createJaxrsService(realm, auth);
    }

    /**
     * Base path for managing applications under this realm.
     *
     * @return
     */
    @Path("applications")
    public ApplicationsResource getApplications() {
        ApplicationsResource applicationsResource = new ApplicationsResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(applicationsResource);
        //resourceContext.initResource(applicationsResource);
        return applicationsResource;
    }

    /**
     * Base path for managing applications under this realm.
     *
     * @return
     */
    @Path("applications-by-id")
    public ApplicationsByIdResource getApplicationsById() {
        ApplicationsByIdResource applicationsResource = new ApplicationsByIdResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(applicationsResource);
        //resourceContext.initResource(applicationsResource);
        return applicationsResource;
    }

    /**
     * base path for managing oauth clients in this realm uses name of client
     *
     * @return
     */
    @Path("oauth-clients")
    public OAuthClientsResource getOAuthClients() {
        OAuthClientsResource oauth = new OAuthClientsResource(realm, auth, session);
        ResteasyProviderFactory.getInstance().injectProperties(oauth);
        //resourceContext.initResource(oauth);
        return oauth;
    }

    /**
     * base path for managing oauth clients in this realm uses ids
     *
     * @return
     */
    @Path("oauth-clients-by-id")
    public OAuthClientsByIdResource getOAuthClientsById() {
        OAuthClientsByIdResource oauth = new OAuthClientsByIdResource(realm, auth, session);
        ResteasyProviderFactory.getInstance().injectProperties(oauth);
        //resourceContext.initResource(oauth);
        return oauth;
    }

    /**
     * base path for managing realm-level roles of this realm
     *
     * @return
     */
    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(realm, auth, realm);
    }

    /**
     * Get the top-level representation of the realm.  It will not include nested information like User, Application, or OAuth
     * Client representations.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces("application/json")
    public RealmRepresentation getRealm() {
        if (auth.hasView()) {
            RealmRepresentation rep = ModelToRepresentation.toRepresentation(realm, false);
            if (session.realms() instanceof CacheRealmProvider) {
                CacheRealmProvider cacheRealmProvider = (CacheRealmProvider)session.realms();
                rep.setRealmCacheEnabled(cacheRealmProvider.isEnabled());
            }
            if (session.userStorage() instanceof CacheUserProvider) {
                CacheUserProvider cache = (CacheUserProvider)session.userStorage();
                rep.setUserCacheEnabled(cache.isEnabled());
            }
            return rep;
        } else {
            auth.requireAny();

            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());

            return rep;
        }
    }

    /**
     * Update the top-level information of this realm.  Any user, roles, application, or oauth client information in the representation
     * will be ignored.  This will only update top-level attributes of the realm.
     *
     * @param rep
     * @return
     */
    @PUT
    @Consumes("application/json")
    public Response updateRealm(final RealmRepresentation rep) {
        auth.requireManage();

        logger.debug("updating realm: " + realm.getName());
        try {
            RepresentationToModel.updateRealm(rep, realm);
            if (rep.isRealmCacheEnabled() != null && session.realms() instanceof CacheRealmProvider) {
                CacheRealmProvider cacheRealmProvider = (CacheRealmProvider)session.realms();
                cacheRealmProvider.setEnabled(rep.isRealmCacheEnabled());
            }
            if (rep.isUserCacheEnabled() != null && session.userStorage() instanceof CacheUserProvider) {
                CacheUserProvider cache = (CacheUserProvider)session.userStorage();
                cache.setEnabled(rep.isUserCacheEnabled());
            }

            // Refresh periodic sync tasks for configured federationProviders
            List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();
            UsersSyncManager usersSyncManager = new UsersSyncManager();
            for (final UserFederationProviderModel fedProvider : federationProviders) {
                usersSyncManager.refreshPeriodicSyncForProvider(session.getKeycloakSessionFactory(), session.getProvider(TimerProvider.class), fedProvider, realm.getId());
            }

            return Response.noContent().build();
        } catch (ModelDuplicateException e) {
            return Flows.errors().exists("Realm " + rep.getRealm() + " already exists");
        }
    }

    /**
     * Delete this realm.
     *
     */
    @DELETE
    public void deleteRealm() {
        auth.requireManage();

        if (!new RealmManager(session).removeRealm(realm)) {
            throw new NotFoundException("Realm doesn't exist");
        }
    }

    /**
     * Base path for managing users in this realm.
     *
     * @return
     */
    @Path("users")
    public UsersResource users() {
        UsersResource users = new UsersResource(realm, auth, tokenManager);
        ResteasyProviderFactory.getInstance().injectProperties(users);
        //resourceContext.initResource(users);
        return users;
    }

    @Path("user-federation")
    public UserFederationResource userFederation() {
        UserFederationResource fed = new UserFederationResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(fed);
        //resourceContext.initResource(fed);
        return fed;
    }

    /**
     * Path for managing all realm-level or application-level roles defined in this realm by it's id.
     *
     * @return
     */
    @Path("roles-by-id")
    public RoleByIdResource rolesById() {
        RoleByIdResource resource = new RoleByIdResource(realm, auth);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;
    }

    /**
     * Push the realm's revocation policy to any application that has an admin url associated with it.
     *
     */
    @Path("push-revocation")
    @POST
    public GlobalRequestResult pushRevocation() {
        auth.requireManage();
        return new ResourceAdminManager().pushRealmRevocationPolicy(uriInfo.getRequestUri(), realm);
    }

    /**
     * Removes all user sessions.  Any application that has an admin url will also be told to invalidate any sessions
     * they have.
     *
     */
    @Path("logout-all")
    @POST
    public GlobalRequestResult logoutAll() {
        session.sessions().removeUserSessions(realm);
        return new ResourceAdminManager().logoutAll(uriInfo.getRequestUri(), realm);
    }

    /**
     * Remove a specific user session. Any application that has an admin url will also be told to invalidate this
     * particular session.
     *
     * @param sessionId
     */
    @Path("sessions/{session}")
    @DELETE
    public void deleteSession(@PathParam("session") String sessionId) {
        UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
        if (userSession == null) throw new NotFoundException("Sesssion not found");
        session.sessions().removeUserSession(realm, userSession);
        new ResourceAdminManager().logoutSession(uriInfo.getRequestUri(), realm, userSession);
    }

    /**
     * Returns a JSON map.  The key is the application name, the value is the number of sessions that currently are active
     * with that application.  Only application's that actually have a session associated with them will be in this map.
     *
     * @return
     */
    @Path("application-session-stats")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Deprecated
    public Map<String, Integer> getApplicationSessionStats() {
        auth.requireView();
        Map<String, Integer> stats = new HashMap<String, Integer>();
        for (ApplicationModel application : realm.getApplications()) {
            int size = session.sessions().getActiveUserSessions(application.getRealm(), application);
            if (size == 0) continue;
            stats.put(application.getName(), size);
        }
        return stats;
    }

    /**
     * Returns a JSON map.  The key is the application id, the value is the number of sessions that currently are active
     * with that application.  Only application's that actually have a session associated with them will be in this map.
     *
     * @return
     */
    @Path("application-by-id-session-stats")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, String>> getApplicationByIdSessionStats() {
        auth.requireView();
        List<Map<String, String>> data = new LinkedList<Map<String, String>>();
        for (ApplicationModel application : realm.getApplications()) {
            int size = session.sessions().getActiveUserSessions(application.getRealm(), application);
            if (size == 0) continue;
            Map<String, String> map = new HashMap<String, String>();
            map.put("id", application.getId());
            map.put("name", application.getName());
            map.put("active", size + "");
            data.add(map);
        }
        return data;
    }

    /**
     * View the events provider and how it is configured.
     *
     * @return
     */
    @GET
    @NoCache
    @Path("events/config")
    @Produces("application/json")
    public RealmEventsConfigRepresentation getRealmEventsConfig() {
        auth.init(RealmAuth.Resource.EVENTS).requireView();

        return ModelToRepresentation.toEventsConfigReprensetation(realm);
    }

    /**
     * Change the events provider and/or it's configuration
     *
     * @param rep
     */
    @PUT
    @Path("events/config")
    @Consumes("application/json")
    public void updateRealmEventsConfig(final RealmEventsConfigRepresentation rep) {
        auth.init(RealmAuth.Resource.EVENTS).requireManage();

        logger.debug("updating realm events config: " + realm.getName());
        new RealmManager(session).updateRealmEventsConfig(rep, realm);
    }

    /**
     * Query events.  Returns all events, or will query based on URL query parameters listed here
     *
     * @param client app or oauth client name
     * @param types type type
     * @param user user id
     * @param ipAddress
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Path("events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Event> getEvents(@QueryParam("client") String client,
            @QueryParam("user") String user, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo,
            @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults) {
        auth.init(RealmAuth.Resource.EVENTS).requireView();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(realm.getId());
        if (client != null) {
            query.client(client);
        }

        List<String> types = uriInfo.getQueryParameters().get("type");
        if (types != null) {
            EventType[] t = new EventType[types.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = EventType.valueOf(types.get(i));
            }
            query.type(t);
        }

        if (user != null) {
            query.user(user);
        }
        
        if(dateFrom != null) {
            query.fromDate(dateFrom);
        }
        if(dateTo != null) {
            query.toDate(dateTo);
        }

        if (ipAddress != null) {
            query.ipAddress(ipAddress);
        }
        if (firstResult != null) {
            query.firstResult(firstResult);
        }
        if (maxResults != null) {
            query.maxResults(maxResults);
        }

        return query.getResultList();
    }

    /**
     * Delete all events.
     *
     */
    @Path("events")
    @DELETE
    public void clearEvents() {
        auth.init(RealmAuth.Resource.EVENTS).requireManage();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clear(realm.getId());
    }

    @Path("testLDAPConnection")
    @GET
    @NoCache
    public Response testLDAPConnection(@QueryParam("action") String action, @QueryParam("connectionUrl") String connectionUrl,
                                       @QueryParam("bindDn") String bindDn, @QueryParam("bindCredential") String bindCredential) {
        auth.init(RealmAuth.Resource.REALM).requireManage();

        boolean result = new LDAPConnectionTestManager().testLDAP(action, connectionUrl, bindDn, bindCredential);
        return result ? Response.noContent().build() : Flows.errors().error("LDAP test error", Response.Status.BAD_REQUEST);
    }

    @Path("identity-provider")
    public IdentityProvidersResource getIdentityProviderResource() {
        return new IdentityProvidersResource(realm, session, this.auth);
    }
}
