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

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.Profile;
import org.keycloak.common.util.HtmlUtils;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.events.Event;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStoreFactory;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.components.TestProvider;
import org.keycloak.testsuite.components.TestProviderFactory;
import org.keycloak.testsuite.components.amphibian.TestAmphibianProvider;
import org.keycloak.testsuite.events.TestEventsListenerProvider;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;
import org.keycloak.testsuite.forms.PassThroughAuthenticator;
import org.keycloak.testsuite.forms.PassThroughClientAuthenticator;
import org.keycloak.testsuite.model.infinispan.InfinispanTestUtil;
import org.keycloak.testsuite.rest.representation.AuthenticatorState;
import org.keycloak.testsuite.rest.resource.TestCacheResource;
import org.keycloak.testsuite.rest.resource.TestJavascriptResource;
import org.keycloak.testsuite.rest.resource.TestLDAPResource;
import org.keycloak.testsuite.rest.resource.TestingExportImportResource;
import org.keycloak.testsuite.runonserver.FetchOnServer;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.runonserver.SerializationUtil;
import org.keycloak.testsuite.util.FeatureDeployerUtil;
import org.keycloak.timer.TimerProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.UUID;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TestingResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;
    private final Map<String, TimerProvider.TimerTaskContext> suspendedTimerTasks;

    @Context
    private HttpRequest request;

    @Override
    public Object getResource() {
        return this;
    }

    public TestingResourceProvider(KeycloakSession session, Map<String, TimerProvider.TimerTaskContext> suspendedTimerTasks) {
        this.session = session;
        this.suspendedTimerTasks = suspendedTimerTasks;
    }

    @POST
    @Path("/remove-user-session")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserSession(@QueryParam("realm") final String name, @QueryParam("session") final String sessionId) {
        RealmModel realm = getRealmByName(name);

        UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
        if (sessionModel == null) {
            throw new NotFoundException("Session not found");
        }

        session.sessions().removeUserSession(realm, sessionModel);
        return Response.noContent().build();
    }

    @POST
    @Path("/remove-user-sessions")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeUserSessions(@QueryParam("realm") final String realmName) {
        RealmModel realm = getRealmByName(realmName);

        session.sessions().removeUserSessions(realm);
        return Response.noContent().build();
    }

    @GET
    @Path("/get-last-session-refresh")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getLastSessionRefresh(@QueryParam("realm") final String name, @QueryParam("session") final String sessionId, @QueryParam("offline") boolean offline) {
        RealmModel realm = getRealmByName(name);

        UserSessionModel sessionModel = offline ? session.sessions().getOfflineUserSession(realm, sessionId) : session.sessions().getUserSession(realm, sessionId);
        if (sessionModel == null) {
            throw new NotFoundException("Session not found");
        }

        return sessionModel.getLastSessionRefresh();
    }

    @POST
    @Path("/remove-expired")
    @Produces(MediaType.APPLICATION_JSON)
    public Response removeExpired(@QueryParam("realm") final String name) {
        RealmModel realm = getRealmByName(name);

        session.sessions().removeExpired(realm);
        session.authenticationSessions().removeExpired(realm);
        session.realms().removeExpiredClientInitialAccess();

        return Response.noContent().build();
    }

    @POST
    @Path("/set-testing-infinispan-time-service")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setTestingInfinispanTimeService() {
        InfinispanTestUtil.setTestingTimeService(session);
        return Response.noContent().build();
    }

    @POST
    @Path("/revert-testing-infinispan-time-service")
    @Produces(MediaType.APPLICATION_JSON)
    public Response revertTestingInfinispanTimeService() {
        InfinispanTestUtil.revertTimeService();
        return Response.noContent().build();
    }

    @GET
    @Path("/get-client-sessions-count")
    @Produces(MediaType.APPLICATION_JSON)
    public Integer getClientSessionsCountInUserSession(@QueryParam("realm") final String name, @QueryParam("session") final String sessionId) {

        RealmModel realm = getRealmByName(name);

        UserSessionModel sessionModel = session.sessions().getUserSession(realm, sessionId);
        if (sessionModel == null) {
            throw new NotFoundException("Session not found");
        }

        // TODO: Might need optimization to prevent loading client sessions from cache
        return sessionModel.getAuthenticatedClientSessions().size();
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

        // Time offset was restarted
        if (offset == 0) {
            session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
        }

        return getTimeOffset();
    }

    @POST
    @Path("/poll-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public EventRepresentation getEvent() {
        Event event = TestEventsListenerProvider.poll();
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
        AdminEvent adminEvent = TestEventsListenerProvider.pollAdminEvent();
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
        TestEventsListenerProvider.clear();
        return Response.noContent().build();
    }

    @POST
    @Path("/clear-admin-event-queue")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventQueue() {
        TestEventsListenerProvider.clearAdminEvents();
        return Response.noContent().build();
    }

    @GET
    @Path("/clear-event-store")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventStore() {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clear();
        return Response.noContent().build();
    }

    @GET
    @Path("/clear-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventStore(@QueryParam("realmId") String realmId) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clear(realmId);
        return Response.noContent().build();
    }

    @GET
    @Path("/clear-expired-events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearExpiredEvents() {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearExpiredEvents();
        return Response.noContent().build();
    }

    /**
     * Query events
     * <p>
     * Returns all events, or filters them based on URL query parameters listed here
     *
     * @param realmId     The realm
     * @param types       The types of events to return
     * @param client      App or oauth client name
     * @param user        User id
     * @param dateFrom    From date
     * @param dateTo      To date
     * @param ipAddress   IP address
     * @param firstResult Paging offset
     * @param maxResults  Paging size
     * @return
     */
    @Path("query-events")
    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<EventRepresentation> queryEvents(@QueryParam("realmId") String realmId, @QueryParam("type") List<String> types, @QueryParam("client") String client,
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

        if (dateFrom != null) {
            Date from = formatDate(dateFrom, "Date(From)");
            query.fromDate(from);
        }

        if (dateTo != null) {
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

        return query.getResultStream().map(ModelToRepresentation::toRepresentation);
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
        event.setId(UUID.randomUUID().toString());
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
        return Response.noContent().build();
    }

    @GET
    @Path("/clear-admin-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore(@QueryParam("realmId") String realmId) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(realmId);
        return Response.noContent().build();
    }

    @GET
    @Path("/clear-admin-event-store-older-than")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore(@QueryParam("realmId") String realmId, @QueryParam("olderThan") long olderThan) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearAdmin(realmId, olderThan);
        return Response.noContent().build();
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
    public Stream<AdminEventRepresentation> getAdminEvents(@QueryParam("realmId") String realmId, @QueryParam("operationTypes") List<String> operationTypes, @QueryParam("authRealm") String authRealm, @QueryParam("authClient") String authClient,
                                                         @QueryParam("authUser") String authUser, @QueryParam("authIpAddress") String authIpAddress,
                                                         @QueryParam("resourcePath") String resourcePath, @QueryParam("dateFrom") String dateFrom,
                                                         @QueryParam("dateTo") String dateTo, @QueryParam("first") Integer firstResult,
                                                         @QueryParam("max") Integer maxResults) {

        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        AdminEventQuery query = eventStore.createAdminQuery();

        if (realmId != null) {
            query.realm(realmId);
        }

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

        if (dateFrom != null) {
            Date from = formatDate(dateFrom, "Date(From)");
            query.fromTime(from);
        }

        if (dateTo != null) {
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

        return query.getResultStream().map(ModelToRepresentation::toRepresentation);
    }

    private Date formatDate(String date, String paramName) {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return df.parse(date);
        } catch (ParseException e) {
            throw new BadRequestException("Invalid value for '" + paramName + "', expected format is yyyy-MM-dd");
        }
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
        event.setId(UUID.randomUUID().toString());
        event.setAuthDetails(repToModel(rep.getAuthDetails()));
        event.setError(rep.getError());
        event.setOperationType(OperationType.valueOf(rep.getOperationType()));
        if (rep.getResourceType() != null) {
            event.setResourceTypeAsString(rep.getResourceType());
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
    @Path("/get-sso-cookie")
    @Produces(MediaType.APPLICATION_JSON)
    public String getSSOCookieValue() {
        Map<String, Cookie> cookies = request.getHttpHeaders().getCookies();
        Cookie cookie = CookieHelper.getCookie(cookies, AuthenticationManager.KEYCLOAK_IDENTITY_COOKIE);
        if (cookie == null) return null;
        return cookie.getValue();
    }


    @Path("/cache/{cache}")
    public TestCacheResource getCacheResource(@PathParam("cache") String cacheName) {
        return new TestCacheResource(session, cacheName);
    }


    @Path("/ldap/{realm}")
    public TestLDAPResource ldap(@PathParam("realm") final String realmName) {
        RealmModel realm = session.realms().getRealmByName(realmName);
        return new TestLDAPResource(session, realm);
    }


    @Override
    public void close() {
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
    @Path("/valid-credentials")
    @Produces(MediaType.APPLICATION_JSON)
    public boolean validCredentials(@QueryParam("realmName") String realmName, @QueryParam("userName") String userName, @QueryParam("password") String password) {
        RealmModel realm = session.realms().getRealm(realmName);
        if (realm == null) return false;
        UserProvider userProvider = session.getProvider(UserProvider.class);
        UserModel user = userProvider.getUserByUsername(realm, userName);
        return session.userCredentialManager().isValid(realm, user, UserCredentialModel.password(password));
    }

    @GET
    @Path("/user-by-federated-identity")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUserByFederatedIdentity(@QueryParam("realmName") String realmName,
                                                         @QueryParam("identityProvider") String identityProvider,
                                                         @QueryParam("userId") String userId,
                                                         @QueryParam("userName") String userName) {
        RealmModel realm = getRealmByName(realmName);
        UserModel foundFederatedUser = session.users().getUserByFederatedIdentity(realm, new FederatedIdentityModel(identityProvider, userId, userName));
        if (foundFederatedUser == null) return null;
        return ModelToRepresentation.toRepresentation(session, realm, foundFederatedUser);
    }

    @GET
    @Path("/user-by-username-from-fed-factory")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUserByUsernameFromFedProviderFactory(@QueryParam("realmName") String realmName,
                                                                      @QueryParam("userName") String userName) {
        RealmModel realm = getRealmByName(realmName);
        DummyUserFederationProviderFactory factory = (DummyUserFederationProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserStorageProvider.class, "dummy");
        UserModel user = factory.create(session, null).getUserByUsername(realm, userName);
        if (user == null) return null;
        return ModelToRepresentation.toRepresentation(session, realm, user);
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
        return ModelToRepresentation.toRepresentation(session, realm, user);
    }

    @Path("/export-import")
    public TestingExportImportResource getExportImportResource() {
        return new TestingExportImportResource(session);
    }

    @GET
    @Path("/test-component")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, TestProvider.DetailsRepresentation> getTestComponentDetails() {
        RealmModel realm = session.getContext().getRealm();
        return realm.getComponentsStream(realm.getId(), TestProvider.class.getName())
                .collect(Collectors.toMap(ComponentModel::getName,
                        componentModel -> {
                            ProviderFactory<TestProvider> f = session.getKeycloakSessionFactory()
                                .getProviderFactory(TestProvider.class, componentModel.getProviderId());
                        TestProviderFactory factory = (TestProviderFactory) f;
                        TestProvider p = (TestProvider) factory.create(session, componentModel);
                        return p.getDetails();
                        }));
    }

    @GET
    @Path("/test-amphibian-component")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Map<String, Object>> getTestAmphibianComponentDetails() {
        RealmModel realm = session.getContext().getRealm();
        return realm.getComponentsStream(realm.getId(), TestAmphibianProvider.class.getName())
                .collect(Collectors.toMap(
                  ComponentModel::getName,
                  componentModel -> {
                      TestAmphibianProvider t = session.getComponentProvider(TestAmphibianProvider.class, componentModel.getId());
                      return t == null ? null : t.getDetails();
                  }));
    }


    @GET
    @Path("/identity-config")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, String> getIdentityProviderConfig(@QueryParam("alias") String alias) {
        return session.getContext().getRealm().getIdentityProviderByAlias(alias).getConfig();
    }

    @PUT
    @Path("/set-krb5-conf-file")
    @Consumes(MediaType.APPLICATION_JSON)
    public void setKrb5ConfFile(@QueryParam("krb5-conf-file") String krb5ConfFile) {
        System.setProperty("java.security.krb5.conf", krb5ConfFile);
    }

    @POST
    @Path("/suspend-periodic-tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response suspendPeriodicTasks() {
        suspendTask(ClearExpiredUserSessions.TASK_NAME);
        suspendTask(CrossDCLastSessionRefreshStoreFactory.LSR_PERIODIC_TASK_NAME);
        suspendTask(CrossDCLastSessionRefreshStoreFactory.LSR_OFFLINE_PERIODIC_TASK_NAME);

        return Response.noContent().build();
    }

    @GET
    @Path("/uncaught-error")
    public Response uncaughtError() {
        throw new RuntimeException("Uncaught error");
    }

    private void suspendTask(String taskName) {
        TimerProvider.TimerTaskContext taskContext = session.getProvider(TimerProvider.class).cancelTask(taskName);

        if (taskContext != null) {
            suspendedTimerTasks.put(taskName, taskContext);
        }
    }

    @POST
    @Path("/restore-periodic-tasks")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restorePeriodicTasks() {
        TimerProvider timer = session.getProvider(TimerProvider.class);

        for (Map.Entry<String, TimerProvider.TimerTaskContext> task : suspendedTimerTasks.entrySet()) {
            timer.schedule(task.getValue().getRunnable(), task.getValue().getIntervalMillis(), task.getKey());
        }

        suspendedTimerTasks.clear();

        return Response.noContent().build();
    }


    /**
     * Generate new client scope for specified service client. The "Frontend" clients, who will use this client scope, will be able to
     * send their access token to authenticate against specified service client
     *
     * @param clientId Client ID of service client (typically bearer-only client)
     * @return ID of the newly generated clientScope
     */
    @Path("generate-audience-client-scope")
    @POST
    @NoCache
    public String generateAudienceClientScope(@QueryParam("realm") final String realmName, final @QueryParam("clientId") String clientId) {
        try {
            RealmModel realm = getRealmByName(realmName);
            ClientModel serviceClient = realm.getClientByClientId(clientId);
            if (serviceClient == null) {
                throw new NotFoundException("Referenced service client doesn't exist");
            }

            ClientScopeModel clientScopeModel = realm.addClientScope(clientId);
            clientScopeModel.setProtocol(serviceClient.getProtocol()==null ? OIDCLoginProtocol.LOGIN_PROTOCOL : serviceClient.getProtocol());
            clientScopeModel.setDisplayOnConsentScreen(true);
            clientScopeModel.setConsentScreenText(clientId);
            clientScopeModel.setIncludeInTokenScope(true);

            // Add audience protocol mapper
            ProtocolMapperModel audienceMapper = AudienceProtocolMapper.createClaimMapper("Audience for " + clientId, clientId, null,true, false);
            clientScopeModel.addProtocolMapper(audienceMapper);

            return clientScopeModel.getId();
        } catch (ModelDuplicateException e) {
            throw new BadRequestException("Client Scope " + clientId + " already exists");
        }
    }


    @POST
    @Path("/run-on-server")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    public String runOnServer(String runOnServer) throws Exception {
        try {
            Object r = SerializationUtil.decode(runOnServer, TestClassLoader.getInstance());

            if (r instanceof FetchOnServer) {
                Object result = ((FetchOnServer) r).run(session);
                return result != null ? JsonSerialization.writeValueAsString(result) : null;
            } else if (r instanceof RunOnServer) {
                ((RunOnServer) r).run(session);
                return null;
            } else {
                throw new IllegalArgumentException();
            }
        } catch (Throwable t) {
            return SerializationUtil.encodeException(t);
        }
    }


    @POST
    @Path("/run-model-test-on-server")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    public String runModelTestOnServer(@QueryParam("testClassName") String testClassName,
                                       @QueryParam("testMethodName") String testMethodName) throws Exception {
        try {
            Class testClass = TestClassLoader.getInstance().loadClass(testClassName);
            Method testMethod = testClass.getDeclaredMethod(testMethodName, KeycloakSession.class);

            Object test = testClass.newInstance();
            testMethod.invoke(test, session);

            return "SUCCESS";
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            }

            return SerializationUtil.encodeException(t);
        }
    }


    @Path("/javascript")
    public TestJavascriptResource getJavascriptResource() {
        return new TestJavascriptResource(session);
    }

    private void setFeatureInProfileFile(File file, Profile.Feature featureProfile, String newState) {
        Properties properties = new Properties();
        if (file.isFile() && file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read profile.properties file");
            }
        }

        properties.setProperty("feature." + featureProfile.toString().toLowerCase(), newState);

        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, null);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to profile.properties file");
        }
    }

    @POST
    @Path("/enable-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response enableFeature(@PathParam("feature") String feature) {
        Profile.Feature featureProfile;

        try {
            featureProfile = Profile.Feature.valueOf(feature);
        } catch (IllegalArgumentException e) {
            System.err.printf("Feature '%s' doesn't exist!!\n", feature);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (Profile.isFeatureEnabled(featureProfile))
            return Response.noContent().build();

        FeatureDeployerUtil.initBeforeChangeFeature(featureProfile);

        System.setProperty("keycloak.profile.feature." + featureProfile.toString().toLowerCase(), "enabled");

        String jbossServerConfigDir = System.getProperty("jboss.server.config.dir");
        // If we are in jboss-based container, we need to write profile.properties file, otherwise the change in system property will disappear after restart
        if (jbossServerConfigDir != null) {
            setFeatureInProfileFile(new File(jbossServerConfigDir, "profile.properties"), featureProfile, "enabled");
        }

        Profile.init();

        FeatureDeployerUtil.deployFactoriesAfterFeatureEnabled(featureProfile);

        if (Profile.isFeatureEnabled(featureProfile))
            return Response.noContent().build();
        else
            return Response.status(Response.Status.NOT_FOUND).build();
    }

    @POST
    @Path("/disable-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response disableFeature(@PathParam("feature") String feature) {
        Profile.Feature featureProfile;

        try {
            featureProfile = Profile.Feature.valueOf(feature);
        } catch (IllegalArgumentException e) {
            System.err.printf("Feature '%s' doesn't exist!!\n", feature);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        if (!Profile.isFeatureEnabled(featureProfile))
            return Response.noContent().build();

        FeatureDeployerUtil.initBeforeChangeFeature(featureProfile);

        disableFeatureProperties(featureProfile);

        String jbossServerConfigDir = System.getProperty("jboss.server.config.dir");
        // If we are in jboss-based container, we need to write profile.properties file, otherwise the change in system property will disappear after restart
        if (jbossServerConfigDir != null) {
            setFeatureInProfileFile(new File(jbossServerConfigDir, "profile.properties"), featureProfile, "disabled");
        }

        Profile.init();

        FeatureDeployerUtil.undeployFactoriesAfterFeatureDisabled(featureProfile);

        if (!Profile.isFeatureEnabled(featureProfile))
            return Response.noContent().build();
        else
            return Response.status(Response.Status.NOT_FOUND).build();
    }

    /**
     * KEYCLOAK-12958
     */
    private void disableFeatureProperties(Profile.Feature feature) {
        Profile.Type type = Profile.getName().equals("product") ? feature.getTypeProduct() : feature.getTypeProject();
        if (type.equals(Profile.Type.DEFAULT)) {
            System.setProperty("keycloak.profile.feature." + feature.toString().toLowerCase(), "disabled");
        } else {
            System.getProperties().remove("keycloak.profile.feature." + feature.toString().toLowerCase());
        }
    }

    /**
     * This will send POST request to specified URL with specified form parameters. It's not easily possible to "trick" web driver to send POST
     * request with custom parameters, which are not directly available in the form.
     *
     * See URLUtils.sendPOSTWithWebDriver for more details
     *
     * @param postRequestUrl Absolute URL. It can include query parameters etc. The POST request will be send to this URL
     * @param encodedFormParameters Encoded parameters in the form of "param1=value1:param2=value2"
     * @return
     */
    @GET
    @Path("/simulate-post-request")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response simulatePostRequest(@QueryParam("postRequestUrl") String postRequestUrl,
                                         @QueryParam("encodedFormParameters") String encodedFormParameters) {
        Map<String, String> params = new HashMap<>();

        // Parse parameters to use in the POST request
        for (String param : encodedFormParameters.split("&")) {
            String[] paramParts = param.split("=");
            String value = paramParts.length == 2 ? paramParts[1] : "";
            params.put(paramParts[0], value);
        }

        // Send the POST request "manually"
        StringBuilder builder = new StringBuilder();

        builder.append("<HTML>");
        builder.append("  <HEAD>");
        builder.append("    <TITLE>OIDC Form_Post Response</TITLE>");
        builder.append("  </HEAD>");
        builder.append("  <BODY Onload=\"document.forms[0].submit()\">");

        builder.append("    <FORM METHOD=\"POST\" ACTION=\"" + postRequestUrl + "\">");

        for (Map.Entry<String, String> param : params.entrySet()) {
            builder.append("  <INPUT TYPE=\"HIDDEN\" NAME=\"")
                    .append(param.getKey())
                    .append("\" VALUE=\"")
                    .append(HtmlUtils.escapeAttribute(param.getValue()))
                    .append("\" />");
        }

        builder.append("      <NOSCRIPT>");
        builder.append("        <P>JavaScript is disabled. We strongly recommend to enable it. Click the button below to continue .</P>");
        builder.append("        <INPUT name=\"continue\" TYPE=\"SUBMIT\" VALUE=\"CONTINUE\" />");
        builder.append("      </NOSCRIPT>");
        builder.append("    </FORM>");
        builder.append("  </BODY>");
        builder.append("</HTML>");

        return Response.status(Response.Status.OK)
                .type(javax.ws.rs.core.MediaType.TEXT_HTML_TYPE)
                .entity(builder.toString()).build();

    }

    private RealmModel getRealmByName(String realmName) {
        RealmProvider realmProvider = session.getProvider(RealmProvider.class);
        RealmModel realm = realmProvider.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }
        return realm;
    }

}
