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

package org.keycloak.testsuite.rest;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import org.infinispan.Cache;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.events.Event;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.testsuite.events.EventsListenerProvider;
import org.keycloak.testsuite.forms.PassThroughAuthenticator;
import org.keycloak.testsuite.forms.PassThroughClientAuthenticator;
import org.keycloak.testsuite.rest.representation.AuthenticatorState;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.BadRequestException;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.exportimport.ExportImportManager;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import static org.keycloak.exportimport.ExportImportConfig.*;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TestingResourceProvider implements RealmResourceProvider {

    private KeycloakSession session;

    @Override
    public Object getResource() {
        return this;
    }

    public TestingResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @POST
    @Path("/remove-user-session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserSession(@QueryParam("realm") final String name, @QueryParam("session") final String sessionId) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }

        UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
        if (sessionModel == null) {
            throw new NotFoundException("Session not found");
        }

        session.sessions().removeUserSession(realm, sessionModel);
        return Response.ok().build();
    }

    @POST
    @Path("/remove-user-sessions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserSessions(@QueryParam("realm") final String realmName) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }

        session.sessions().removeUserSessions(realm);
        return Response.ok().build();
    }

    @GET
    @Path("/get-user-session")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getLastSessionRefresh(@QueryParam("realm") final String name, @QueryParam("session") final String sessionId) {

        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }

        UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
        if (sessionModel == null) {
            throw new NotFoundException("Session not found");
        }

        return sessionModel.getLastSessionRefresh();
    }

    @POST
    @Path("/remove-expired")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeExpired(@QueryParam("realm") final String name) {
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(name);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }

        session.sessions().removeExpired(realm);
        return Response.ok().build();
    }

    @GET
    @Path("/time-offset")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getTimeOffset() {
        Map<String, String> response = new HashMap<>();
        response.put("currentTime", String.valueOf(Time.currentTime()));
        response.put("offset", String.valueOf(Time.getOffset()));
        return response;
    }

    @PUT
    @Path("/time-offset")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> setTimeOffset(Map<String, String> time) {
        int offset = Integer.parseInt(time.get("offset"));
        Time.setOffset(offset);
        return getTimeOffset();
    }

    @POST
    @Path("/poll-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public EventRepresentation getEvent() {
        Event event = EventsListenerProvider.poll();
        if (event != null) {
            return ModelToRepresentation.toRepresentation(event);
        } else {
            return null;
        }
    }

    @POST
    @Path("/poll-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public AdminEventRepresentation getAdminEvent() {
        AdminEvent adminEvent = EventsListenerProvider.pollAdminEvent();
        if (adminEvent != null) {
            return ModelToRepresentation.toRepresentation(adminEvent);
        } else {
            return null;
        }
    }

    @POST
    @Path("/clear-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventQueue() {
        EventsListenerProvider.clear();
        return Response.ok().build();
    }

    @POST
    @Path("/clear-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventQueue() {
        EventsListenerProvider.clearAdminEvents();
        return Response.ok().build();
    }

    @GET
    @Path("/clear-event-store")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventStore() {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clear();
        return Response.ok().build();
    }

    @GET
    @Path("/clear-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventStore(@QueryParam("realmId") String realmId) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clear(realmId);
        return Response.ok().build();
    }

    @GET
    @Path("/clear-event-store-older-than")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventStore(@QueryParam("realmId") String realmId, @QueryParam("olderThan") long olderThan) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clear(realmId, olderThan);
        return Response.ok().build();
    }

    /**
     * Query events
     *
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param realmId The realm
     * @param types The types of events to return
     * @param client App or oauth client name
     * @param user User id
     * @param dateFrom From date
     * @param dateTo To date
     * @param ipAddress IP address
     * @param firstResult Paging offset
     * @param maxResults Paging size
     * @return
     */
    @Path("query-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<EventRepresentation> queryEvents(@QueryParam("realmId") String realmId, @QueryParam("type") List<String> types, @QueryParam("client") String client,
            @QueryParam("user") String user, @QueryParam("dateFrom") String dateFrom, @QueryParam("dateTo") String dateTo,
            @QueryParam("ipAddress") String ipAddress, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults) {

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        EventQuery query = eventStore.createQuery();

        if (realmId != null) {
            query.realm(realmId);
        }

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
            Date from = formatDate(dateFrom, "Date(From)");
            query.fromDate(from);
        }

        if(dateTo != null) {
            Date to = formatDate(dateTo, "Date(To)");
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

    @PUT
    @Path("/on-event")
    @Consumes(MediaType.APPLICATION_JSON)
    public void onEvent(final EventRepresentation rep) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);

        eventStore.onEvent(repToModel(rep));
    }

    private Event repToModel(EventRepresentation rep) {
        Event event = new Event();
        event.setClientId(rep.getClientId());
        event.setDetails(rep.getDetails());
        event.setError(rep.getError());
        event.setIpAddress(rep.getIpAddress());
        event.setRealmId(rep.getRealmId());
        event.setSessionId(rep.getSessionId());
        event.setTime(rep.getTime());
        event.setType(EventType.valueOf(rep.getType()));
        event.setUserId(rep.getUserId());
        return event;
    }

    @GET
    @Path("/clear-admin-event-store")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore() {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin();
        return Response.ok().build();
    }

    @GET
    @Path("/clear-admin-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore(@QueryParam("realmId") String realmId) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(realmId);
        return Response.ok().build();
    }

    @GET
    @Path("/clear-admin-event-store-older-than")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore(@QueryParam("realmId") String realmId, @QueryParam("olderThan") long olderThan) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(realmId, olderThan);
        return Response.ok().build();
    }

    /**
     * Get admin events
     *
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param realmId
     * @param operationTypes
     * @param authRealm
     * @param authClient
     * @param authUser user id
     * @param authIpAddress
     * @param resourcePath
     * @param dateFrom
     * @param dateTo
     * @param firstResult
     * @param maxResults
     * @return
     */
    @Path("query-admin-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<AdminEventRepresentation> getAdminEvents(@QueryParam("realmId") String realmId, @QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
            @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
            @QueryParam("resourcePath") String resourcePath, @QueryParam("dateFrom") String dateFrom,
            @QueryParam("dateTo") String dateTo, @QueryParam("first") Integer firstResult,
            @QueryParam("max") Integer maxResults) {

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        AdminEventQuery query = eventStore.createAdminQuery();

        if (realmId != null) {
            query.realm(realmId);
        };

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

        if(dateFrom != null) {
            Date from = formatDate(dateFrom, "Date(From)");
            query.fromTime(from);
        }

        if(dateTo != null) {
            Date to = formatDate(dateTo, "Date(To)");
            query.toTime(to);
        }

        if (firstResult != null || maxResults != null) {
            if (firstResult == null) {
                firstResult = 0;
            }
            if (maxResults == null) {
                maxResults = 100;
            }
            query.firstResult(firstResult);
            query.maxResults(maxResults);
        }

        return toAdminEventRep(query.getResultList());
    }

    private Date formatDate(String date, String paramName) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return df.parse(date);
        } catch (ParseException e) {
            throw new BadRequestException("Invalid value for '" + paramName + "', expected format is yyyy-MM-dd");
        }
    }

    private List<AdminEventRepresentation> toAdminEventRep(List<AdminEvent> events) {
        List<AdminEventRepresentation> reps = new ArrayList<>();
        for (AdminEvent event : events) {
            reps.add(ModelToRepresentation.toRepresentation(event));
        }

        return reps;
    }

    @POST
    @Path("/on-admin-event")
    @Consumes(MediaType.APPLICATION_JSON)
    public void onAdminEvent(final AdminEventRepresentation rep, @QueryParam("includeRepresentation") boolean includeRepresentation) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.onEvent(repToModel(rep), includeRepresentation);
    }

    private AdminEvent repToModel(AdminEventRepresentation rep) {
        AdminEvent event = new AdminEvent();
        event.setAuthDetails(repToModel(rep.getAuthDetails()));
        event.setError(rep.getError());
        event.setOperationType(OperationType.valueOf(rep.getOperationType()));
        if (rep.getResourceType() != null) {
            event.setResourceType(ResourceType.valueOf(rep.getResourceType()));
        }
        event.setRealmId(rep.getRealmId());
        event.setRepresentation(rep.getRepresentation());
        event.setResourcePath(rep.getResourcePath());
        event.setTime(rep.getTime());
        return event;
    }

    private AuthDetails repToModel(AuthDetailsRepresentation rep) {
        AuthDetails details = new AuthDetails();
        details.setClientId(rep.getClientId());
        details.setIpAddress(rep.getIpAddress());
        details.setRealmId(rep.getRealmId());
        details.setUserId(rep.getUserId());
        return details;
    }

    @GET
    @Path("/cache/{cache}/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean isCached(@PathParam("cache") String cacheName, @PathParam("id") String id) {
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        Cache<Object, Object> cache = provider.getCache(cacheName);
        return cache.containsKey(id);
    }

    @Override
    public void close() {
    }

    /*
     * Migration from KeycloakRule#verifyCode
     */
    @GET
    @Path("/verify-code")
    @Produces(MediaType.APPLICATION_JSON)
    public String verifyCode(@QueryParam("realm") String realmName, @QueryParam("code") String code) {
        RealmModel realm = session.realms().getRealm(realmName);
        try {
            ClientSessionCode accessCode = ClientSessionCode.parse(code, session, realm);
            if (accessCode == null) {
                throw new AssertionError("Invalid code");
            }
            return accessCode.getClientSession().getId();
        } catch (Throwable t) {
            throw new AssertionError("Failed to parse code", t);
        }
    }

    @POST
    @Path("/update-pass-through-auth-state")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticatorState updateAuthenticator(AuthenticatorState state) {
        if (state.getClientId() != null) {
            PassThroughClientAuthenticator.clientId = state.getClientId();
        }
        if (state.getUsername() != null) {
            PassThroughAuthenticator.username = state.getUsername();
        }

        AuthenticatorState result = new AuthenticatorState();
        result.setClientId(PassThroughClientAuthenticator.clientId);
        result.setUsername(PassThroughAuthenticator.username);
        return result;
    }

    @GET
    @Path("/run-import")
    @Produces(MediaType.APPLICATION_JSON)
    public Response runImport() {
        new ExportImportManager(session).runImport();
        return Response.ok().build();
    }

    @GET
    @Path("/run-export")
    @Produces(MediaType.APPLICATION_JSON)
    public Response runExport() {
        new ExportImportManager(session).runExport();
        return Response.ok().build();
    }

    @GET
    @Path("/valid-credentials")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean validCredentials(@QueryParam("realmName") String realmName, @QueryParam("userName") String userName, @QueryParam("password") String password) {
        RealmModel realm = session.realms().getRealm(realmName);
        if (realm == null) return false;
        UserProvider userProvider = session.getProvider(UserProvider.class);
        UserModel user = userProvider.getUserByUsername(userName, realm);
        return userProvider.validCredentials(session, realm, user, UserCredentialModel.password(password));
    }

    @GET
    @Path("/user-by-federated-identity")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUserByFederatedIdentity(@QueryParam("realmName") String realmName,
                                                         @QueryParam("identityProvider") String identityProvider,
                                                         @QueryParam("userId") String userId,
                                                         @QueryParam("userName") String userName) {
        RealmModel realm = getRealmByName(realmName);
        UserModel foundFederatedUser = session.users().getUserByFederatedIdentity(new FederatedIdentityModel(identityProvider, userId, userName), realm);
        if (foundFederatedUser == null) return null;
        return ModelToRepresentation.toRepresentation(foundFederatedUser);
    }

    @GET
    @Path("/user-by-username-from-fed-factory")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUserByUsernameFromFedProviderFactory(@QueryParam("realmName") String realmName,
                                                                      @QueryParam("userName") String userName) {
        RealmModel realm = getRealmByName(realmName);
        UserFederationProviderFactory factory = (UserFederationProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(UserFederationProvider.class, "dummy");
        UserModel user = factory.getInstance(session, null).getUserByUsername(realm, userName);
        if (user == null) return null;
        return ModelToRepresentation.toRepresentation(user);
    }

    @GET
    @Path("/get-client-auth-flow")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowRepresentation getClientAuthFlow(@QueryParam("realmName") String realmName) {
        RealmModel realm = getRealmByName(realmName);
        AuthenticationFlowModel flow = realm.getClientAuthenticationFlow();
        if (flow == null) return null;
        return ModelToRepresentation.toRepresentation(realm, flow);
    }

    @GET
    @Path("/get-reset-cred-flow")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowRepresentation getResetCredFlow(@QueryParam("realmName") String realmName) {
        RealmModel realm = getRealmByName(realmName);
        AuthenticationFlowModel flow = realm.getResetCredentialsFlow();
        if (flow == null) return null;
        return ModelToRepresentation.toRepresentation(realm, flow);
    }

    @GET
    @Path("/get-user-by-service-account-client")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUserByServiceAccountClient(@QueryParam("realmName") String realmName, @QueryParam("clientId") String clientId) {
        RealmModel realm = getRealmByName(realmName);
        ClientModel client =  realm.getClientByClientId(clientId);
        UserModel user = session.users().getServiceAccount(client);
        if (user == null) return null;
        return ModelToRepresentation.toRepresentation(user);
    }

    private RealmModel getRealmByName(String realmName) {
        RealmProvider realmProvider = session.getProvider(RealmProvider.class);
        return realmProvider.getRealmByName(realmName);
    }

    @GET
    @Path("/get-users-per-file")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getUsersPerFile() {
        String usersPerFile = System.getProperty(USERS_PER_FILE, String.valueOf(DEFAULT_USERS_PER_FILE));
        return Integer.parseInt(usersPerFile.trim());
    }

    @PUT
    @Path("/set-users-per-file")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setUsersPerFile(@QueryParam("usersPerFile") Integer usersPerFile) {
        System.setProperty(USERS_PER_FILE, String.valueOf(usersPerFile));
    }

    @GET
    @Path("/get-dir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getDir() {
        return System.getProperty(DIR);
    }

    @PUT
    @Path("/set-dir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String setDir(@QueryParam("dir") String dir) {
        return System.setProperty(DIR, dir);
    }

    @PUT
    @Path("/export-import-provider")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setProvider(@QueryParam("exportImportProvider") String exportImportProvider) {
        System.setProperty(PROVIDER, exportImportProvider);
    }

    @PUT
    @Path("/export-import-file")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setFile(@QueryParam("file") String file) {
        System.setProperty(FILE, file);
    }

    @PUT
    @Path("/export-import-action")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setAction(@QueryParam("exportImportAction") String exportImportAction) {
        System.setProperty(ACTION, exportImportAction);
    }

    @PUT
    @Path("/set-realm-name")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setRealmName(@QueryParam("realmName") String realmName) {
        if (realmName != null && !realmName.isEmpty()) {
            System.setProperty(REALM_NAME, realmName);
        } else {
            System.getProperties().remove(REALM_NAME);
        }
    }

    @GET
    @Path("/get-test-dir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public String getExportImportTestDirectory() {
        System.setProperty("project.build.directory", "target");
        String absolutePath = new File(System.getProperty("project.build.directory", "target")).getAbsolutePath();
        return absolutePath;
    }

}
