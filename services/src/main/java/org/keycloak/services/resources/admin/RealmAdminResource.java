package org.keycloak.services.resources.admin;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.ClientConnection;
import org.keycloak.authentication.RequiredActionFactory;
import org.keycloak.authentication.RequiredActionProvider;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.exportimport.ClientImporter;
import org.keycloak.models.ClientModel;
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
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.LDAPConnectionTestManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.services.ErrorResponse;
import org.keycloak.timer.TimerProvider;

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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;

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
    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    @Context
    protected UriInfo uriInfo;

    @Context
    protected ClientConnection connection;

    @Context
    protected HttpHeaders headers;

    public RealmAdminResource(RealmAuth auth, RealmModel realm, TokenManager tokenManager, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.tokenManager = tokenManager;
        this.adminEvent = adminEvent.realm(realm);

        auth.init(RealmAuth.Resource.REALM);
    }

    /**
     * Base path for importing clients under this realm.
     *
     * @return
     */
    @Path("client-importers/{formatId}")
    public Object getClientImporter(@PathParam("formatId") String formatId) {
        ClientImporter importer = session.getProvider(ClientImporter.class, formatId);
        return importer.createJaxrsService(realm, auth);
    }

    /**
     * Base path for managing attack detection.
     *
     * @return
     */
    @Path("attack-detection")
    public AttackDetectionResource getClientImporter() {
        AttackDetectionResource resource = new AttackDetectionResource(auth, realm, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * Base path for managing clients under this realm.
     *
     * @return
     */
    @Path("clients")
    public ClientsResource getClients() {
        ClientsResource clientsResource = new ClientsResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientsResource);
        return clientsResource;
    }

    /**
     * base path for managing realm-level roles of this realm
     *
     * @return
     */
    @Path("roles")
    public RoleContainerResource getRoleContainerResource() {
        return new RoleContainerResource(uriInfo, realm, auth, realm, adminEvent);
    }

    /**
     * Get the top-level representation of the realm.  It will not include nested information like User and Client representations.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
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
     * Update the top-level information of this realm.  Any user, roles or client information in the representation
     * will be ignored.  This will only update top-level attributes of the realm.
     *
     * @param rep
     * @return
     */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
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
            
            adminEvent.operation(OperationType.UPDATE).representation(rep).success();
            return Response.noContent().build();
        } catch (PatternSyntaxException e) {
            return ErrorResponse.error("Specified regex pattern(s) is invalid.", Response.Status.BAD_REQUEST);
        } catch (ModelDuplicateException e) {
            throw e;
        } catch (Exception e) {
            logger.error(e);
            return ErrorResponse.error("Failed to update " + rep.getRealm() + " Realm.", Response.Status.INTERNAL_SERVER_ERROR);
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
        } else {
            clearAdminEvents();
        }
    }

    /**
     * Base path for managing users in this realm.
     *
     * @return
     */
    @Path("users")
    public UsersResource users() {
        UsersResource users = new UsersResource(realm, auth, tokenManager, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(users);
        //resourceContext.initResource(users);
        return users;
    }

    @Path("user-federation")
    public UserFederationProvidersResource userFederation() {
        UserFederationProvidersResource fed = new UserFederationProvidersResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(fed);
        //resourceContext.initResource(fed);
        return fed;
    }

    @Path("authentication")
    public AuthenticationManagementResource flows() {
        AuthenticationManagementResource resource = new AuthenticationManagementResource(realm, session, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;

    }

    /**
     * Path for managing all realm-level or client-level roles defined in this realm by it's id.
     *
     * @return
     */
    @Path("roles-by-id")
    public RoleByIdResource rolesById() {
        RoleByIdResource resource = new RoleByIdResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        //resourceContext.initResource(resource);
        return resource;
    }

    /**
     * Push the realm's revocation policy to any client that has an admin url associated with it.
     *
     */
    @Path("push-revocation")
    @POST
    public GlobalRequestResult pushRevocation() {
        auth.requireManage();
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
        return new ResourceAdminManager(session).pushRealmRevocationPolicy(uriInfo.getRequestUri(), realm);
    }

    /**
     * Removes all user sessions.  Any client that has an admin url will also be told to invalidate any sessions
     * they have.
     *
     */
    @Path("logout-all")
    @POST
    public GlobalRequestResult logoutAll() {
        auth.init(RealmAuth.Resource.USER).requireManage();
        session.sessions().removeUserSessions(realm);
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
        return new ResourceAdminManager(session).logoutAll(uriInfo.getRequestUri(), realm);
    }

    /**
     * Remove a specific user session. Any client that has an admin url will also be told to invalidate this
     * particular session.
     *
     * @param sessionId
     */
    @Path("sessions/{session}")
    @DELETE
    public void deleteSession(@PathParam("session") String sessionId) {
        auth.init(RealmAuth.Resource.USER).requireManage();
        UserSessionModel userSession = session.sessions().getUserSession(realm, sessionId);
        if (userSession == null) throw new NotFoundException("Sesssion not found");
        AuthenticationManager.backchannelLogout(session, realm, userSession, uriInfo, connection, headers, true);
        adminEvent.operation(OperationType.DELETE).resourcePath(uriInfo).success();

    }

    /**
     * Returns a JSON map.  The key is the client id, the value is the number of sessions that currently are active
     * with that client.  Only client's that actually have a session associated with them will be in this map.
     *
     * @return
     */
    @Path("client-session-stats")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, String>> getClientSessionStats() {
        auth.requireView();
        List<Map<String, String>> data = new LinkedList<Map<String, String>>();
        for (ClientModel client : realm.getClients()) {
            int size = session.sessions().getActiveUserSessions(client.getRealm(), client);
            if (size == 0) continue;
            Map<String, String> map = new HashMap<String, String>();
            map.put("id", client.getId());
            map.put("clientId", client.getClientId());
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
    @Produces(MediaType.APPLICATION_JSON)
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
    @Consumes(MediaType.APPLICATION_JSON)
    public void updateRealmEventsConfig(final RealmEventsConfigRepresentation rep) {
        auth.init(RealmAuth.Resource.EVENTS).requireManage();

        logger.debug("updating realm events config: " + realm.getName());
        new RealmManager(session).updateRealmEventsConfig(rep, realm);
    }

    /**
     * Query events.  Returns all events, or will query based on URL query parameters listed here
     *
     * @param client app or oauth client name
     * @param user user id
     * @param ipAddress
     * @param dateTo
     * @param dateFrom
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
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date from = null;
            try {
                from = df.parse(dateFrom);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(From)', expected format is yyyy-MM-dd");
            }
            query.fromDate(from);
        }
        
        if(dateTo != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date to = null;
            try {
                to = df.parse(dateTo);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(To)', expected format is yyyy-MM-dd");
            }
            query.toDate(to);
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
     * Query admin events.  Returns all admin events, or will query based on URL query parameters listed here
     *
     * @param authRealm
     * @param authClient
     * @param authUser user id
     * @param authIpAddress
     * @param resourcePath
     * @param dateTo
     * @param dateFrom
     * @param resourcePath
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Path("admin-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<AdminEvent> getEvents(@QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
            @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
            @QueryParam("resourcePath") String resourcePath, @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults) {
        auth.init(RealmAuth.Resource.EVENTS).requireView();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        AdminEventQuery query = eventStore.createAdminQuery().realm(realm.getId());;

        if (authRealm != null) {
            query.authRealm(authRealm);
        }

        if (authClient != null) {
            query.authClient(authClient);
        }
        
        if (authUser != null) {
            query.authUser(authUser);
        }
        
        if (authIpAddress != null) {
            query.authIpAddress(authIpAddress);
        }
        
        if (resourcePath != null) {
            query.resourcePath(resourcePath);
        }

        List<String> operationTypes = uriInfo.getQueryParameters().get("operationTypes");
        if (operationTypes != null) {
            OperationType[] t = new OperationType[operationTypes.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = OperationType.valueOf(operationTypes.get(i));
            }
            query.operation(t);
        }
        
        if(dateFrom != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date from = null;
            try {
                from = df.parse(dateFrom);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(From)', expected format is yyyy-MM-dd");
            }
            query.fromTime(from);
        }
        
        if(dateTo != null) {
            SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
            Date to = null;
            try {
                to = df.parse(dateTo);
            } catch (ParseException e) {
                throw new BadRequestException("Invalid value for 'Date(To)', expected format is yyyy-MM-dd");
            }
            query.toTime(to);
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
    
    /**
     * Delete all admin events.
     *
     */
    @Path("admin-events")
    @DELETE
    public void clearAdminEvents() {
        auth.init(RealmAuth.Resource.EVENTS).requireManage();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(realm.getId());
    }

    @Path("testLDAPConnection")
    @GET
    @NoCache
    public Response testLDAPConnection(@QueryParam("action") String action, @QueryParam("connectionUrl") String connectionUrl,
                                       @QueryParam("bindDn") String bindDn, @QueryParam("bindCredential") String bindCredential) {
        auth.init(RealmAuth.Resource.REALM).requireManage();

        boolean result = new LDAPConnectionTestManager().testLDAP(action, connectionUrl, bindDn, bindCredential);
        return result ? Response.noContent().build() : ErrorResponse.error("LDAP test error", Response.Status.BAD_REQUEST);
    }

    @Path("identity-provider")
    public IdentityProvidersResource getIdentityProviderResource() {
        return new IdentityProvidersResource(realm, session, this.auth, adminEvent);
    }

}
