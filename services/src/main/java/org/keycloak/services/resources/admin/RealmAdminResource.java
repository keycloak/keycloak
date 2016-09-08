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
import org.jboss.resteasy.spi.BadRequestException;
import org.jboss.resteasy.spi.NotFoundException;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.KeyPairVerifier;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.exportimport.ClientDescriptionConverter;
import org.keycloak.exportimport.ClientDescriptionConverterFactory;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.cache.CacheRealmProvider;
import org.keycloak.models.cache.CacheUserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.adapters.action.GlobalRequestResult;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.RealmEventsConfigRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.LDAPConnectionTestManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.ResourceAdminManager;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.RealmAuth.Resource;
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
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.PatternSyntaxException;
import org.keycloak.partialimport.PartialImportManager;

/**
 * Base resource class for the admin REST api of one realm
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmAdminResource {
    protected static final ServicesLogger logger = ServicesLogger.ROOT_LOGGER;
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
        this.adminEvent = adminEvent.realm(realm).resource(ResourceType.REALM);

        auth.init(RealmAuth.Resource.REALM);
        auth.requireAny();
    }

    /**
     * Base path for importing clients under this realm.
     *
     * @return
     */
    @Path("client-description-converter")
    @Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_PLAIN })
    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public ClientRepresentation convertClientDescription(String description) {
        auth.init(Resource.CLIENT).requireManage();

        if (realm == null) {
            throw new NotFoundException("Realm not found.");
        }

        for (ProviderFactory<ClientDescriptionConverter> factory : session.getKeycloakSessionFactory().getProviderFactories(ClientDescriptionConverter.class)) {
            if (((ClientDescriptionConverterFactory) factory).isSupported(description)) {
                return factory.create(session).convertToInternal(description);
            }
        }

        throw new BadRequestException("Unsupported format");
    }

    /**
     * Base path for managing attack detection.
     *
     * @return
     */
    @Path("attack-detection")
    public AttackDetectionResource getAttackDetection() {
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
     * Base path for managing client templates under this realm.
     *
     * @return
     */
    @Path("client-templates")
    public ClientTemplatesResource getClientTemplates() {
        ClientTemplatesResource clientsResource = new ClientTemplatesResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(clientsResource);
        return clientsResource;
    }

    /**
     * Base path for managing client initial access tokens
     *
     * @return
     */
    @Path("clients-initial-access")
    public ClientInitialAccessResource getClientInitialAccess() {
        ClientInitialAccessResource resource = new ClientInitialAccessResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }


    /**
     * Base path for managing client initial access tokens
     *
     * @return
     */
    @Path("clients-trusted-hosts")
    public ClientRegistrationTrustedHostResource getClientRegistrationTrustedHost() {
        ClientRegistrationTrustedHostResource resource = new ClientRegistrationTrustedHostResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }

    /**
     * Base path for managing components under this realm.
     *
     * @return
     */
    @Path("components")
    public ComponentResource getComponents() {
        ComponentResource resource = new ComponentResource(realm, auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
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
     * Get the top-level representation of the realm
     *
     * It will not include nested information like User and Client representations.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public RealmRepresentation getRealm() {
        if (auth.hasView()) {
            return ModelToRepresentation.toRepresentation(realm, false);
        } else {
            auth.requireAny();

            RealmRepresentation rep = new RealmRepresentation();
            rep.setRealm(realm.getName());

            if (auth.init(Resource.IDENTITY_PROVIDER).hasView()) {
                RealmRepresentation r = ModelToRepresentation.toRepresentation(realm, false);
                rep.setIdentityProviders(r.getIdentityProviders());
                rep.setIdentityProviderMappers(r.getIdentityProviderMappers());
            }

            return rep;
        }
    }

    /**
     * Update the top-level information of the realm
     *
     * Any user, roles or client information in the representation
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
            if (!"GENERATE".equals(rep.getPublicKey()) && (rep.getPrivateKey() != null && rep.getPublicKey() != null)) {
                try {
                    KeyPairVerifier.verify(rep.getPrivateKey(), rep.getPublicKey());
                } catch (VerificationException e) {
                    return ErrorResponse.error(e.getMessage(), Status.BAD_REQUEST);
                }
            }

            if (!"GENERATE".equals(rep.getPublicKey()) && (rep.getCertificate() != null)) {
                try {
                    X509Certificate cert = PemUtils.decodeCertificate(rep.getCertificate());
                    if (cert == null) {
                        return ErrorResponse.error("Failed to decode certificate", Status.BAD_REQUEST);
                    }
                } catch (Exception e)  {
                    return ErrorResponse.error("Failed to decode certificate", Status.BAD_REQUEST);
                }
            }

            RepresentationToModel.updateRealm(rep, realm, session);

            // Refresh periodic sync tasks for configured federationProviders
            List<UserFederationProviderModel> federationProviders = realm.getUserFederationProviders();
            UsersSyncManager usersSyncManager = new UsersSyncManager();
            for (final UserFederationProviderModel fedProvider : federationProviders) {
                usersSyncManager.notifyToRefreshPeriodicSync(session, realm, fedProvider, false);
            }

            adminEvent.operation(OperationType.UPDATE).representation(rep).success();
            return Response.noContent().build();
        } catch (PatternSyntaxException e) {
            return ErrorResponse.error("Specified regex pattern(s) is invalid.", Response.Status.BAD_REQUEST);
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Realm with same name exists");
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return ErrorResponse.error("Failed to update realm", Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Delete the realm
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
        UsersResource users = new UsersResource(realm, auth, adminEvent);
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
     * Path for managing all realm-level or client-level roles defined in this realm by its id.
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

        GlobalRequestResult result = new ResourceAdminManager(session).pushRealmRevocationPolicy(uriInfo.getRequestUri(), realm);
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).representation(result).success();
        return result;
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
        GlobalRequestResult result = new ResourceAdminManager(session).logoutAll(uriInfo.getRequestUri(), realm);
        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).representation(result).success();
        return result;
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
        adminEvent.operation(OperationType.DELETE).resource(ResourceType.USER_SESSION).resourcePath(uriInfo).success();

    }

    /**
     * Get client session stats
     *
     * Returns a JSON map.  The key is the client id, the value is the number of sessions that currently are active
     * with that client.  Only clients that actually have a session associated with them will be in this map.
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
            long size = session.sessions().getActiveUserSessions(client.getRealm(), client);
            if (size == 0) continue;
            Map<String, String> map = new HashMap<>();
            map.put("id", client.getId());
            map.put("clientId", client.getClientId());
            map.put("active", size + "");
            data.add(map);
        }
        return data;
    }

    /**
     * Get the events provider configuration
     *
     * Returns JSON object with events provider configuration
     *
     * @return
     */
    @GET
    @NoCache
    @Path("events/config")
    @Produces(MediaType.APPLICATION_JSON)
    public RealmEventsConfigRepresentation getRealmEventsConfig() {
        auth.init(RealmAuth.Resource.EVENTS).requireView();

        RealmEventsConfigRepresentation config = ModelToRepresentation.toEventsConfigReprensetation(realm);
        if (config.getEnabledEventTypes() == null || config.getEnabledEventTypes().isEmpty()) {
            config.setEnabledEventTypes(new LinkedList<String>());
            for (EventType e : EventType.values()) {
                if (e.isSaveByDefault()) {
                    config.getEnabledEventTypes().add(e.name());
                }
            }
        }
        return config;
    }

    /**
     * Update the events provider
     *
     * Change the events provider and/or its configuration
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
     * Get events
     *
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param types The types of events to return
     * @param client App or oauth client name
     * @param user User id
     * @param ipAddress IP address
     * @param dateTo To date
     * @param dateFrom From date
     * @param firstResult Paging offset
     * @param maxResults Paging size
     * @return
     */
    @Path("events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventRepresentation> getEvents(@QueryParam("type") List<String> types, @QueryParam("client") String client,
                                               @QueryParam("user") String user, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo,
                                               @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
                                               @QueryParam("max") Integer maxResults) {
        auth.init(RealmAuth.Resource.EVENTS).requireView();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery().realm(realm.getId());
        if (client != null) {
            query.client(client);
        }

        if (types != null & !types.isEmpty()) {
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

        return toEventListRep(query.getResultList());
    }

    private List<EventRepresentation> toEventListRep(List<Event> events) {
        List<EventRepresentation> reps = new ArrayList<>();
        for (Event event : events) {
            reps.add(ModelToRepresentation.toRepresentation(event));
        }
        return reps;
    }

    /**
     * Get admin events
     *
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param operationTypes
     * @param authRealm
     * @param authClient
     * @param authUser user id
     * @param authIpAddress
     * @param resourcePath
     * @param dateTo
     * @param dateFrom
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Path("admin-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<AdminEventRepresentation> getEvents(@QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
                                                    @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
                                                    @QueryParam("resourcePath") String resourcePath, @QueryParam("dateFrom") String dateFrom,
                                                    @QueryParam("dateTo") String dateTo, @QueryParam("first") Integer firstResult,
                                                    @QueryParam("max") Integer maxResults,
                                                    @QueryParam("resourceTypes") List<String> resourceTypes) {
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

        if (operationTypes != null && !operationTypes.isEmpty()) {
            OperationType[] t = new OperationType[operationTypes.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = OperationType.valueOf(operationTypes.get(i));
            }
            query.operation(t);
        }

        if (resourceTypes != null && !resourceTypes.isEmpty()) {
            ResourceType[] t = new ResourceType[resourceTypes.size()];
            for (int i = 0; i < t.length; i++) {
                t[i] = ResourceType.valueOf(resourceTypes.get(i));
            }
            query.resourceType(t);
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

        return toAdminEventRep(query.getResultList());
    }

    private List<AdminEventRepresentation> toAdminEventRep(List<AdminEvent> events) {
        List<AdminEventRepresentation> reps = new ArrayList<>();
        for (AdminEvent event : events) {
            reps.add(ModelToRepresentation.toRepresentation(event));
        }

        return reps;
    }

    /**
     * Delete all events
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
     * Delete all admin events
     *
     */
    @Path("admin-events")
    @DELETE
    public void clearAdminEvents() {
        auth.init(RealmAuth.Resource.EVENTS).requireManage();

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(realm.getId());
    }

    /**
     * Test LDAP connection
     *
     * @param action
     * @param connectionUrl
     * @param bindDn
     * @param bindCredential
     * @return
     */
    @Path("testLDAPConnection")
    @GET
    @NoCache
    public Response testLDAPConnection(@QueryParam("action") String action, @QueryParam("connectionUrl") String connectionUrl,
                                       @QueryParam("bindDn") String bindDn, @QueryParam("bindCredential") String bindCredential,
                                       @QueryParam("useTruststoreSpi") String useTruststoreSpi) {
        auth.init(RealmAuth.Resource.REALM).requireManage();

        boolean result = new LDAPConnectionTestManager().testLDAP(action, connectionUrl, bindDn, bindCredential, useTruststoreSpi);
        return result ? Response.noContent().build() : ErrorResponse.error("LDAP test error", Response.Status.BAD_REQUEST);
    }

    @Path("identity-provider")
    public IdentityProvidersResource getIdentityProviderResource() {
        return new IdentityProvidersResource(realm, session, this.auth, adminEvent);
    }

    /**
     * Get group hierarchy.  Only name and ids are returned.
     *
     * @return
     */
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    @Path("default-groups")
    public List<GroupRepresentation> getDefaultGroups() {
        auth.requireView();

        List<GroupRepresentation> defaults = new LinkedList<>();
        for (GroupModel group : realm.getDefaultGroups()) {
            defaults.add(ModelToRepresentation.toRepresentation(group, false));
        }
        return defaults;
    }
    @PUT
    @NoCache
    @Path("default-groups/{groupId}")
    public void addDefaultGroup(@PathParam("groupId") String groupId) {
        auth.requireManage();

        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        realm.addDefaultGroup(group);

        adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP).resourcePath(uriInfo).success();
    }

    @DELETE
    @NoCache
    @Path("default-groups/{groupId}")
    public void removeDefaultGroup(@PathParam("groupId") String groupId) {
        auth.requireManage();

        GroupModel group = realm.getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException("Group not found");
        }
        realm.removeDefaultGroup(group);

        adminEvent.operation(OperationType.DELETE).resource(ResourceType.GROUP).resourcePath(uriInfo).success();
    }


    @Path("groups")
    public GroupsResource getGroups() {
        GroupsResource resource =  new GroupsResource(realm, session, this.auth, adminEvent);
        ResteasyProviderFactory.getInstance().injectProperties(resource);
        return resource;
    }


    @GET
    @Path("group-by-path/{path: .*}")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public GroupRepresentation getGroupByPath(@PathParam("path") String path) {
        auth.requireView();

        GroupModel found = KeycloakModelUtils.findGroupByPath(realm, path);
        if (found == null) {
            throw new NotFoundException("Group path does not exist");

        }
        return ModelToRepresentation.toGroupHierarchy(found, true);
    }

    /**
     * Partial import from a JSON file to an existing realm.
     *
     * @param rep
     * @return
     */
    @Path("partialImport")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response partialImport(PartialImportRepresentation rep) {
        auth.requireManage();

        PartialImportManager partialImport = new PartialImportManager(rep, session, realm, adminEvent);
        return partialImport.saveResources();
    }

    /**
     * Clear realm cache
     *
     */
    @Path("clear-realm-cache")
    @POST
    public void clearRealmCache() {
        auth.requireManage();

        CacheRealmProvider cache = session.getProvider(CacheRealmProvider.class);
        if (cache != null) {
            cache.clear();
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
    }

    /**
     * Clear user cache
     *
     */
    @Path("clear-user-cache")
    @POST
    public void clearUserCache() {
        auth.requireManage();

        CacheUserProvider cache = session.getProvider(CacheUserProvider.class);
        if (cache != null) {
            cache.clear();
        }

        adminEvent.operation(OperationType.ACTION).resourcePath(uriInfo).success();
    }

}
