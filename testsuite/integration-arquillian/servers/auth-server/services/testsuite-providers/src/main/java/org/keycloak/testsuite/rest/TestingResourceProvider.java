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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.common.enums.HostnameVerificationPolicy;
import org.keycloak.common.profile.PropertiesProfileConfigResolver;
import org.keycloak.common.util.HtmlUtils;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.cookie.CookieProvider;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventQuery;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.events.admin.AdminEvent;
import org.keycloak.events.admin.AdminEventQuery;
import org.keycloak.events.admin.AuthDetails;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.email.EmailEventListenerProviderFactory;
import org.keycloak.http.HttpRequest;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.encode.TokenContextEncoderProvider;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantType;
import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.representations.idm.AdminEventRepresentation;
import org.keycloak.representations.idm.AuthDetailsRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.scheduled.ClearExpiredUserSessions;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.datastore.PeriodicEventInvalidation;
import org.keycloak.testsuite.components.amphibian.TestAmphibianProvider;
import org.keycloak.testsuite.events.TestEventsListenerProvider;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;
import org.keycloak.testsuite.forms.PassThroughAuthenticator;
import org.keycloak.testsuite.forms.PassThroughClientAuthenticator;
import org.keycloak.testsuite.model.infinispan.InfinispanTestUtil;
import org.keycloak.testsuite.rest.representation.AuthenticatorState;
import org.keycloak.testsuite.rest.resource.TestCacheResource;
import org.keycloak.testsuite.rest.resource.TestLDAPResource;
import org.keycloak.testsuite.rest.resource.TestingExportImportResource;
import org.keycloak.testsuite.runonserver.FetchOnServer;
import org.keycloak.testsuite.runonserver.RunOnServer;
import org.keycloak.testsuite.runonserver.SerializationUtil;
import org.keycloak.testsuite.util.FeatureDeployerUtil;
import org.keycloak.timer.TimerProvider;
import org.keycloak.truststore.FileTruststoreProvider;
import org.keycloak.truststore.FileTruststoreProviderFactory;
import org.keycloak.truststore.TruststoreProvider;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import org.jboss.resteasy.reactive.NoCache;

import static java.util.Objects.requireNonNull;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class TestingResourceProvider implements RealmResourceProvider {

    private final KeycloakSession session;
    private final Map<String, TimerProvider.TimerTaskContext> suspendedTimerTasks;

    private final HttpRequest request;

    private final TestingResourceProviderFactory factory;

    @Override
    public Object getResource() {
        return this;
    }

    public TestingResourceProvider(KeycloakSession session, TestingResourceProviderFactory factory, Map<String, TimerProvider.TimerTaskContext> suspendedTimerTasks) {
        this.session = session;
        this.factory = factory;
        this.suspendedTimerTasks = suspendedTimerTasks;
        this.request = session.getContext().getHttpRequest();
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
        InfinispanTestUtil.revertTimeService(session);
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
    @Path("/clear-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearEventStore(@QueryParam("realmId") String realmId) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        RealmModel realm = session.realms().getRealm(realmId);

        if (realm == null) throw ErrorResponse.error("Realm not found", Response.Status.NOT_FOUND);

        eventStore.clear(realm);
        return Response.noContent().build();
    }

    @GET
    @Path("/clear-expired-events")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearExpiredEvents() {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        eventStore.clearExpiredEvents();
        session.invalidate(PeriodicEventInvalidation.JPA_EVENT_STORE);
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

        if (types != null && !types.isEmpty()) {
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
    @Path("/clear-admin-event-store-for-realm")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore(@QueryParam("realmId") String realmId) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        RealmModel realm = session.realms().getRealm(realmId);

        if (realm == null) throw ErrorResponse.error("Realm not found", Response.Status.NOT_FOUND);

        eventStore.clearAdmin(realm);
        return Response.noContent().build();
    }

    @GET
    @Path("/clear-admin-event-store-older-than")
    @Produces(MediaType.APPLICATION_JSON)
    public Response clearAdminEventStore(@QueryParam("realmId") String realmId, @QueryParam("olderThan") long olderThan) {
        EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
        RealmModel realm = session.realms().getRealm(realmId);

        if (realm == null) throw ErrorResponse.error("Realm not found", Response.Status.NOT_FOUND);

        eventStore.clearAdmin(realm, olderThan);
        return Response.noContent().build();
    }

    /**
     * Get admin events
     * <p>
     * Returns all admin events, or filters events based on URL query parameters listed here
     *
     * @param realmId
     * @param operationTypes
     * @param authRealm
     * @param authClient
     * @param authUser       user id
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
        return session.getProvider(CookieProvider.class).get(CookieType.IDENTITY);
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
        RealmModel realm = session.realms().getRealmByName(realmName);
        if (realm == null) return false;
        UserProvider userProvider = session.getProvider(UserProvider.class);
        UserModel user = userProvider.getUserByUsername(realm, userName);
        return user.credentialManager().isValid(UserCredentialModel.password(password));
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
        return ModelToRepresentation.toRepresentation(session, realm, flow);
    }

    @GET
    @Path("/get-reset-cred-flow")
    @Produces(MediaType.APPLICATION_JSON)
    public AuthenticationFlowRepresentation getResetCredFlow(@QueryParam("realmName") String realmName) {
        RealmModel realm = getRealmByName(realmName);
        AuthenticationFlowModel flow = realm.getResetCredentialsFlow();
        if (flow == null) return null;
        return ModelToRepresentation.toRepresentation(session, realm, flow);
    }

    @GET
    @Path("/get-user-by-service-account-client")
    @Produces(MediaType.APPLICATION_JSON)
    public UserRepresentation getUserByServiceAccountClient(@QueryParam("realmName") String realmName, @QueryParam("clientId") String clientId) {
        RealmModel realm = getRealmByName(realmName);
        ClientModel client = realm.getClientByClientId(clientId);
        UserModel user = session.users().getServiceAccount(client);
        if (user == null) return null;
        return ModelToRepresentation.toRepresentation(session, realm, user);
    }

    @Path("/export-import")
    public TestingExportImportResource getExportImportResource() {
        return new TestingExportImportResource(session);
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
        return session.identityProviders().getByAlias(alias).getConfig();
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

    @POST
    @Path("/run-on-server")
    @Consumes(MediaType.TEXT_PLAIN_UTF_8)
    @Produces(MediaType.TEXT_PLAIN_UTF_8)
    public String runOnServer(String runOnServer) {
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
                                       @QueryParam("testMethodName") String testMethodName) {
        try {
            Class<?> testClass = TestClassLoader.getInstance().loadClass(testClassName);
            Method testMethod = testClass.getDeclaredMethod(testMethodName, KeycloakSession.class);

            Object test = testClass.getDeclaredConstructor().newInstance();
            testMethod.invoke(test, session);

            return "SUCCESS";
        } catch (Throwable t) {
            if (t instanceof InvocationTargetException) {
                t = ((InvocationTargetException) t).getTargetException();
            }

            return SerializationUtil.encodeException(t);
        }
    }

    private void setFeatureInProfileFile(File file, Profile.Feature featureProfile, String newState) {
        doWithProperties(file, props -> props.setProperty(PropertiesProfileConfigResolver.getPropertyKey(featureProfile), newState));
    }

    private void unsetFeatureInProfileFile(File file, Profile.Feature featureProfile) {
        doWithProperties(file, props -> props.remove(PropertiesProfileConfigResolver.getPropertyKey(featureProfile)));
    }

    private void doWithProperties(File file, Consumer<Properties> callback) {

        Properties properties = new Properties();
        if (file.isFile() && file.exists()) {
            try (FileInputStream fis = new FileInputStream(file)) {
                properties.load(fis);
            } catch (IOException e) {
                throw new RuntimeException("Unable to read profile.properties file");
            }
        }

        callback.accept(properties);

        if (file.isFile() && !file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        try (FileOutputStream fos = new FileOutputStream(file)) {
            properties.store(fos, null);
        } catch (IOException e) {
            throw new RuntimeException("Unable to write to profile.properties file");
        }
    }

    @GET
    @Path("/list-disabled-features")
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Profile.Feature> listDisabledFeatures() {
        return Profile.getInstance().getDisabledFeatures();
    }

    @POST
    @Path("/enable-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Profile.Feature> enableFeature(@PathParam("feature") String feature) {
        return updateFeature(feature, true);
    }

    @POST
    @Path("/disable-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<Profile.Feature> disableFeature(@PathParam("feature") String feature) {
        return updateFeature(feature, false);
    }

    @POST
    @Path("/reset-feature/{feature}")
    @Consumes(MediaType.APPLICATION_JSON)
    public void resetFeature(@PathParam("feature") String featureKey) {

        featureKey = featureKey.contains(":") ? featureKey.split(":")[0] : featureKey;
        Profile.Feature feature = Profile.getFeatureVersions(featureKey).iterator().next();

        if (feature == null) {
            System.err.printf("Feature '%s' doesn't exist!!\n", featureKey);
            throw new BadRequestException();
        }

        FeatureDeployerUtil.initBeforeChangeFeature(feature);

        String jbossServerConfigDir = System.getProperty("jboss.server.config.dir");
        // If we are in jboss-based container, we need to write profile.properties file, otherwise the change in system property will disappear after restart
        if (jbossServerConfigDir != null) {
            File file = new File(jbossServerConfigDir, "profile.properties");
            unsetFeatureInProfileFile(file, feature);
        }
    }

    private Set<Profile.Feature> updateFeature(String featureKey, boolean shouldEnable) {
        Collection<Profile.Feature> features = null;

        if (featureKey.contains(":")) {
            String unversionedKey = featureKey.split(":")[0];
            int version = Integer.parseInt(featureKey.split(":")[1].replace("v", ""));

            for (Feature versionedFeature : Profile.getFeatureVersions(unversionedKey)) {
                if (versionedFeature.getVersion() == version) {
                    features = Set.of(versionedFeature);
                    break;
                }
            }
        } else {
            features = Profile.getFeatureVersions(featureKey);
        }

        if (features == null || features.isEmpty()) {
            System.err.printf("Feature '%s' doesn't exist!!\n", featureKey);
            throw new BadRequestException();
        }

        for (Feature feature : features) {
            if (Profile.getInstance().getFeatures().get(feature) != shouldEnable) {
                FeatureDeployerUtil.initBeforeChangeFeature(feature);

                String jbossServerConfigDir = System.getProperty("jboss.server.config.dir");
                // If we are in jboss-based container, we need to write profile.properties file, otherwise the change in system property will disappear after restart
                if (jbossServerConfigDir != null) {
                    setFeatureInProfileFile(new File(jbossServerConfigDir, "profile.properties"), feature, shouldEnable ? "enabled" : "disabled");
                }

                Profile current = Profile.getInstance();

                Map<Feature, Boolean> updatedFeatures = new HashMap<>(current.getFeatures());
                updatedFeatures.put(feature, shouldEnable);

                Profile.init(current.getName(), updatedFeatures);

                if (shouldEnable) {
                    FeatureDeployerUtil.deployFactoriesAfterFeatureEnabled(feature);
                } else {
                    FeatureDeployerUtil.undeployFactoriesAfterFeatureDisabled(feature);
                }
            }
        }

        return Profile.getInstance().getDisabledFeatures();
    }


    @GET
    @Path("/set-system-property")
    @Consumes(MediaType.TEXT_HTML_UTF_8)
    @NoCache
    public void setSystemPropertyOnServer(@QueryParam("property-name") String propertyName, @QueryParam("property-value") String propertyValue) {
        if (propertyValue == null) {
            System.getProperties().remove(propertyName);
        } else {
            System.setProperty(propertyName, propertyValue);
        }
    }

    @GET
    @Path("/reinitialize-provider-factory-with-system-properties-scope")
    @Consumes(MediaType.TEXT_HTML_UTF_8)
    public void reinitializeProviderFactoryWithSystemPropertiesScope(@QueryParam("provider-type") String providerType, @QueryParam("provider-id") String providerId,
                                                                     @QueryParam("system-properties-prefix") String systemPropertiesPrefix) throws Exception {
        Class<? extends Provider> providerClass = (Class<? extends Provider>) Class.forName(providerType);
        ProviderFactory<?> factory = session.getKeycloakSessionFactory().getProviderFactory(providerClass, providerId);
        factory.init(new Config.SystemPropertiesScope(systemPropertiesPrefix));
    }

    /**
     * This will send POST request to specified URL with specified form parameters. It's not easily possible to "trick" web driver to send POST
     * request with custom parameters, which are not directly available in the form.
     * <p>
     * See URLUtils.sendPOSTWithWebDriver for more details
     *
     * @param postRequestUrl        Absolute URL. It can include query parameters etc. The POST request will be send to this URL
     * @param encodedFormParameters Encoded parameters in the form of "param1=value1&param2=value2"
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

        builder.append("    <FORM METHOD=\"POST\" ACTION=\"").append(postRequestUrl).append("\">");

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
                .type(jakarta.ws.rs.core.MediaType.TEXT_HTML_TYPE)
                .entity(builder.toString()).build();

    }

    /**
     * Display message to Error Page - for testing purposes
     *
     * @param message message
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/display-error-message")
    public Response displayErrorMessage(@QueryParam("message") String message) {
        return ErrorPage.error(session, session.getContext().getAuthenticationSession(), Response.Status.BAD_REQUEST, message == null ? "" : message);
    }

    @GET
    @Path("/get-provider-implementation-class")
    @Produces(MediaType.APPLICATION_JSON)
    public String getProviderClassName(@QueryParam("providerClass") String providerClass, @QueryParam("providerId") String providerId) {
        try {
            Class<? extends Provider> providerClazz = (Class<? extends Provider>) Class.forName(providerClass);
            Provider provider = (providerId == null) ? session.getProvider(providerClazz) : session.getProvider(providerClazz, providerId);
            return provider.getClass().getName();
        } catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Cannot find provider class: " + providerClass, cnfe);
        }
    }

    private RealmModel getRealmByName(String realmName) {
        RealmProvider realmProvider = session.getProvider(RealmProvider.class);
        RealmModel realm = realmProvider.getRealmByName(realmName);
        if (realm == null) {
            throw new NotFoundException("Realm not found");
        }
        return realm;
    }

    @GET
    @Path("/disable-truststore-spi")
    @NoCache
    public void disableTruststoreSpi() {
        FileTruststoreProviderFactory factory = (FileTruststoreProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(TruststoreProvider.class);
        this.factory.truststoreProvider = factory.create(session);
        factory.setProvider(null);
    }

    @GET
    @Path("/modify-truststore-spi-hostname-policy")
    @NoCache
    public void modifyTruststoreSpiHostnamePolicy(@QueryParam("hostnamePolicy") final HostnameVerificationPolicy hostnamePolicy) {
        FileTruststoreProviderFactory fact = (FileTruststoreProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(TruststoreProvider.class);
        this.factory.truststoreProvider = fact.create(session);
        FileTruststoreProvider origTrustProvider = (FileTruststoreProvider) this.factory.truststoreProvider;
        TruststoreProvider newTrustProvider = new FileTruststoreProvider(
                origTrustProvider.getTruststore(), hostnamePolicy,
                Collections.unmodifiableMap(origTrustProvider.getRootCertificates()),
                Collections.unmodifiableMap(origTrustProvider.getIntermediateCertificates()));
        fact.setProvider(newTrustProvider);
    }

    @GET
    @Path("/reenable-truststore-spi")
    @NoCache
    public void reenableTruststoreSpi() {
        if (this.factory.truststoreProvider == null) {
            throw new IllegalStateException("Cannot reenable provider as it was not disabled");
        }
        FileTruststoreProviderFactory factory = (FileTruststoreProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(TruststoreProvider.class);
        factory.setProvider(this.factory.truststoreProvider);
    }

    @GET
    @Path("/get-authentication-session-tabs-count")
    @NoCache
    public Integer getAuthenticationSessionTabsCount(@QueryParam("realm") String realmName, @QueryParam("authSessionId") String authSessionId) {
        RealmModel realm = getRealmByName(realmName);
        session.getContext().setRealm(realm);
        String decodedAuthSessionId = new AuthenticationSessionManager(session).decodeBase64AndValidateSignature(authSessionId);
        RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realm, decodedAuthSessionId);
        if (rootAuthSession == null) {
            return 0;
        }

        return rootAuthSession.getAuthenticationSessions().size();
    }

    @GET
    @Path("/no-cache-annotated-endpoint")
    @Produces(MediaType.APPLICATION_JSON)
    @NoCache
    public Response getNoCacheAnnotatedEndpointResponse(@QueryParam("programmatic_max_age_value") Integer programmaticMaxAgeValue) {
        requireNonNull(programmaticMaxAgeValue);

        CacheControl cacheControl = new CacheControl();
        cacheControl.setMaxAge(programmaticMaxAgeValue);

        return Response.noContent().cacheControl(cacheControl).build();
    }

    @GET
    @Path("/blank")
    @Produces(MediaType.TEXT_HTML_UTF_8)
    public Response getBlankPage() {
        return Response.ok("<html><body></body></html>").build();
    }

    @GET
    @Path("/pre-authorized-code")
    @NoCache
    public String getPreAuthorizedCode(@QueryParam("realm") final String realmName, @QueryParam("userSessionId") final String userSessionId, @QueryParam("clientId") final String clientId, @QueryParam("expiration") final int expiration) {
        RealmModel realm = getRealmByName(realmName);
        AuthenticatedClientSessionModel ascm = session.sessions()
                .getUserSession(realm, userSessionId)
                .getAuthenticatedClientSessions()
                .values()
                .stream().filter(acsm -> acsm.getClient().getClientId().equals(clientId))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No authenticatedClientSession found."));
        return PreAuthorizedCodeGrantType.getPreAuthorizedCode(session, ascm, expiration);
    }

    @POST
    @Path("/email-event-litener-provide/add-events")
    @Consumes(MediaType.APPLICATION_JSON)
    public void addEventsToEmailEventListenerProvider(List<EventType> events) {
        if (events != null && !events.isEmpty()) {
            EmailEventListenerProviderFactory prov = (EmailEventListenerProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(EventListenerProvider.class, EmailEventListenerProviderFactory.ID);
            prov.addIncludedEvents(events.toArray(EventType[]::new));
        }
    }

    @POST
    @Path("/email-event-litener-provide/remove-events")
    @Consumes(MediaType.APPLICATION_JSON)
    public void removeEventsToEmailEventListenerProvider(List<EventType> events) {
        if (events != null && !events.isEmpty()) {
            EmailEventListenerProviderFactory prov = (EmailEventListenerProviderFactory) session.getKeycloakSessionFactory()
                    .getProviderFactory(EventListenerProvider.class, EmailEventListenerProviderFactory.ID);
            prov.removeIncludedEvents(events.toArray(EventType[]::new));
        }
    }

    @GET
    @Path("/token-context")
    @Produces(MediaType.APPLICATION_JSON)
    public AccessTokenContext getTokenContext(@QueryParam("tokenId") String tokenId) {
        return session.getProvider(TokenContextEncoderProvider.class).getTokenContextFromTokenId(tokenId);
    }

}
