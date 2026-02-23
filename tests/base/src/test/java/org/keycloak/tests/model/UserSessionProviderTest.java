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

package org.keycloak.tests.model;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.provider.ProviderEventListener;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.utils.infinispan.InfinispanTimeUtil;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class UserSessionProviderTest {

    @InjectRealm(config = UserSessionProviderRealm.class)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public  void before() {
        runOnServer.run( session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            session.getContext().setRealm(realm);
            session.users().addUser(realm, "user1").setEmail("user1@localhost");
            session.users().addUser(realm, "user2").setEmail("user2@localhost");
        });
    }

    @AfterEach
    public void after() {
        runOnServer.run( session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            session.getContext().setRealm(realm);
            session.sessions().removeUserSessions(realm);
            UserModel user1 = session.users().getUserByUsername(realm, "user1");
            UserModel user2 = session.users().getUserByUsername(realm, "user2");

            UserManager um = new UserManager(session);
            if (user1 != null) {
                um.removeUser(realm, user1);
            }
            if (user2 != null) {
                um.removeUser(realm, user2);
            }
        });
    }

    @TestOnServer
    public  void testCreateSessions(KeycloakSession session) {
        int started = Time.currentTime();
        RealmModel realm = session.realms().getRealmByName("test");
        UserSessionModel[] sessions = createSessions(session);
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            assertSession(kcSession.sessions().getUserSession(realm, sessions[0].getId()), session.users().getUserByUsername(realm, "user1"), "127.0.0.1", started, started, "test-app", "third-party");
            assertSession(kcSession.sessions().getUserSession(realm, sessions[1].getId()), session.users().getUserByUsername(realm, "user1"), "127.0.0.2", started, started, "test-app");
            assertSession(kcSession.sessions().getUserSession(realm, sessions[2].getId()), session.users().getUserByUsername(realm, "user2"), "127.0.0.3", started, started, "test-app");
        });
    }

    @TestOnServer
    public void testUpdateSession(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        UserSessionModel[] sessions = createSessions(session);
        int lastRefresh = Time.currentTime();
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            kcSession.sessions().getUserSession(realm, sessions[0].getId()).setLastSessionRefresh(lastRefresh);
            assertEquals(lastRefresh, kcSession.sessions().getUserSession(realm, sessions[0].getId()).getLastSessionRefresh());
        });
    }

    @TestOnServer
    public void testUpdateSessionInSameTransaction(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        UserSessionModel[] sessions = createSessions(session);
        int lastRefresh = Time.currentTime();
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            kcSession.sessions().getUserSession(realm, sessions[0].getId()).setLastSessionRefresh(lastRefresh);
            assertEquals(lastRefresh, kcSession.sessions().getUserSession(realm, sessions[0].getId()).getLastSessionRefresh());
        });
    }

    @TestOnServer
    public void testRestartSession(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        int started = Time.currentTime();
        UserSessionModel[] sessions = createSessions(session);

        Time.setOffset(100);
        try {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
                kcSession.getContext().setRealm(session.realms().getRealm(realm.getId()));
                UserSessionModel userSession = kcSession.sessions().getUserSession(realm, sessions[1].getId());
                assertSession(userSession, kcSession.users().getUserByUsername(realm, "user1"), "127.0.0.2", started, started, "test-app");
                AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(realm.getClientByClientId("test-app").getId());
                assertNotNull(clientSession);

                // add some dummy session notes just to test if restart bellow will work
                userSession.setNote("k1", "v1");
                userSession.setNote("k2", "v2");
                userSession.setNote("k3", "v3");

                // A user session can be restarted AuthenticationProcessor.attachSession()
                // It invokes TokenManager.attachAuthenticationSession() that will restart an existing client session (if not valid) or create a new one.
                // We test both cases here. "test-app" is restarted and "third-party" is a new client session
                userSession.restartSession(realm, kcSession.users().getUserByUsername(realm, "user2"), "user2", "127.0.0.6", "form", true, null, null);
                clientSession.restartClientSession();
                createClientSession(kcSession, realm.getClientByClientId("third-party"), sessions[1], "http://redirect", "state");
            });

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
                kcSession.getContext().setRealm(session.realms().getRealm(realm.getId()));
                UserSessionModel userSession = kcSession.sessions().getUserSession(realm, sessions[1].getId());

                assertThat(userSession.getNotes(), Matchers.anEmptyMap());

                assertSession(userSession, kcSession.users().getUserByUsername(realm, "user2"), "127.0.0.6", started + 100, started + 100, "test-app", "third-party");
            });
        } finally {
            Time.setOffset(0);
        }
    }

    @TestOnServer
    public void testCreateClientSession(KeycloakSession session) {

        RealmModel realm = session.realms().getRealmByName("test");
        UserSessionModel[] sessions = createSessions(session);

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            Map<String, AuthenticatedClientSessionModel> clientSessions = kcSession.sessions().getUserSession(realm, sessions[0].getId()).getAuthenticatedClientSessions();
            assertEquals(2, clientSessions.size());

            String clientUUID = realm.getClientByClientId("test-app").getId();

            AuthenticatedClientSessionModel session1 = clientSessions.get(clientUUID);

            assertNull(session1.getAction());
            assertEquals(realm.getClientByClientId("test-app").getClientId(), session1.getClient().getClientId());
            assertEquals(sessions[0].getId(), session1.getUserSession().getId());
            assertEquals("http://redirect", session1.getRedirectUri());
            assertEquals("state", session1.getNote(OIDCLoginProtocol.STATE_PARAM));
        });
    }

    @TestOnServer
    public void testUpdateClientSession(KeycloakSession session) {

        RealmModel realm = session.realms().getRealmByName("test");
        UserSessionModel[] sessions = createSessions(session);

        String userSessionId = sessions[0].getId();
        String clientUUID = realm.getClientByClientId("test-app").getId();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            UserSessionModel userSession = kcSession.sessions().getUserSession(realm, userSessionId);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(clientUUID);

            int time = clientSession.getTimestamp();
            assertNull(clientSession.getAction());

            clientSession.setAction(AuthenticatedClientSessionModel.Action.LOGGED_OUT.name());
            clientSession.setTimestamp(time + 10);

            AuthenticatedClientSessionModel updated = kcSession.sessions().getUserSession(realm, userSessionId).getAuthenticatedClientSessions().get(clientUUID);
            assertEquals(AuthenticatedClientSessionModel.Action.LOGGED_OUT.name(), updated.getAction());
            assertEquals(time + 10, updated.getTimestamp());
        });
    }

    @TestOnServer
    public void testUpdateClientSessionWithGetByClientId(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        UserSessionModel[] sessions = createSessions(session);

        String userSessionId = sessions[0].getId();
        String clientUUID = realm.getClientByClientId("test-app").getId();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            UserSessionModel userSession = kcSession.sessions().getUserSession(realm, userSessionId);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientUUID);

            int time = clientSession.getTimestamp();
            assertNull(clientSession.getAction());

            clientSession.setAction(AuthenticatedClientSessionModel.Action.LOGGED_OUT.name());
            clientSession.setTimestamp(time + 10);

            AuthenticatedClientSessionModel updated = kcSession.sessions().getUserSession(realm, userSessionId).getAuthenticatedClientSessionByClient(clientUUID);
            assertEquals(AuthenticatedClientSessionModel.Action.LOGGED_OUT.name(), updated.getAction());
            assertEquals(time + 10, updated.getTimestamp());
        });
    }

    @TestOnServer
    public void testUpdateClientSessionInSameTransaction(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        UserSessionModel[] sessions = createSessions(session);

        String userSessionId = sessions[0].getId();
        String clientUUID = realm.getClientByClientId("test-app").getId();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            UserSessionModel userSession = kcSession.sessions().getUserSession(realm, userSessionId);
            AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessionByClient(clientUUID);

            clientSession.setAction(AuthenticatedClientSessionModel.Action.LOGGED_OUT.name());
            clientSession.setNote("foo", "bar");

            AuthenticatedClientSessionModel updated = kcSession.sessions().getUserSession(realm, userSessionId).getAuthenticatedClientSessionByClient(clientUUID);
            assertEquals(AuthenticatedClientSessionModel.Action.LOGGED_OUT.name(), updated.getAction());
            assertEquals("bar", updated.getNote("foo"));
        });
    }

    @TestOnServer
    public void testGetUserSessions(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        UserSessionModel[] sessions = createSessions(session);

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            assertSessions(kcSession.sessions().getUserSessionsStream(realm, session.users().getUserByUsername(realm, "user1"))
                    .collect(Collectors.toList()), sessions[0], sessions[1]);
            assertSessions(kcSession.sessions().getUserSessionsStream(realm, session.users().getUserByUsername(realm, "user2"))
                    .collect(Collectors.toList()), sessions[2]);
        });
    }

    @TestOnServer
    public void testRemoveUserSessionsByUser(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        createSessions(session);

        final Map<String, Integer> clientSessionsKept = new HashMap<>();
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
            kcSession.getContext().setRealm(realm);
            clientSessionsKept.putAll(kcSession.sessions().getUserSessionsStream(realm,
                            kcSession.users().getUserByUsername(realm, "user2"))
                    .collect(Collectors.toMap(UserSessionModel::getId, model -> model.getAuthenticatedClientSessions().size())));

            kcSession.sessions().removeUserSessions(realm, kcSession.users().getUserByUsername(realm, "user1"));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            assertEquals(0, kcSession.sessions().getUserSessionsStream(realm, kcSession.users().getUserByUsername(realm, "user1"))
                    .count());
            List<UserSessionModel> userSessions = kcSession.sessions().getUserSessionsStream(realm,
                    kcSession.users().getUserByUsername(realm, "user2")).toList();

            assertSame(1, userSessions.size());

            for (UserSessionModel userSession : userSessions) {
                Assertions.assertEquals((int) clientSessionsKept.get(userSession.getId()),
                        userSession.getAuthenticatedClientSessions().size());
            }
        });
    }

    @TestOnServer
    public void testRemoveUserSession(KeycloakSession session) {
        String userSessionId = KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), kcSession -> {
            RealmModel realm = kcSession.realms().getRealmByName("test");
            kcSession.getContext().setRealm(realm);
            UserSessionModel userSession = createSessions(kcSession)[0];
            userSession = kcSession.sessions().getUserSession(realm, userSession.getId());

            kcSession.sessions().removeUserSession(realm, userSession);
            return userSession.getId();
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            RealmModel realm = kcSession.realms().getRealmByName("test");
            assertNull(kcSession.sessions().getUserSession(realm, userSessionId));
        });
    }

    @TestOnServer
    public void testRemoveUserSessionsByRealm(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        session.getContext().setRealm(realm);
        createSessions(session);

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            kcSession.sessions().removeUserSessions(realm);
        });

        var user1 = session.users().getUserByUsername(realm, "user1");
        var user2 = session.users().getUserByUsername(realm, "user2");

        assertEquals(0, session.sessions().getUserSessionsStream(realm, user1).count());
        assertEquals(0, session.sessions().getUserSessionsStream(realm, user2).count());
    }

    @TestOnServer
    public void testOnClientRemoved(KeycloakSession session) {
        UserSessionModel[] sessions = createSessions(session);

        boolean clientRemoved = false;
        try {
            clientRemoved = KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), kcSession -> {
                RealmModel realm = kcSession.realms().getRealmByName("test");
                kcSession.getContext().setRealm(realm);
                String thirdPartyClientUUID = realm.getClientByClientId("third-party").getId();
                Map<String, Set<String>> clientSessionsKept = new HashMap<>();

                for (UserSessionModel s : sessions) {
                    // session associated with the model was closed, load it by id into a new session
                    s = kcSession.sessions().getUserSession(realm, s.getId());
                    Set<String> clientUUIDS = new HashSet<>(s.getAuthenticatedClientSessions().keySet());
                    clientUUIDS.remove(thirdPartyClientUUID); // This client will be later removed, hence his clientSessions too
                    clientSessionsKept.put(s.getId(), clientUUIDS);
                }

                boolean cr = realm.removeClient(thirdPartyClientUUID);

                for (UserSessionModel s : sessions) {
                    s = kcSession.sessions().getUserSession(realm, s.getId());
                    Set<String> clientUUIDS = s.getAuthenticatedClientSessions().keySet();
                    assertEquals(clientUUIDS, clientSessionsKept.get(s.getId()));
                }

                return cr;
            });
        } finally {
            if (clientRemoved) {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
                    // Revert client
                    RealmModel realm = kcSession.realms().getRealmByName("test");
                    kcSession.getContext().setRealm(realm);
                    realm.addClient("third-party");
                });
            }
        }
    }

    @TestOnServer
    public  void testRemoveUserSessionsByExpired(KeycloakSession session) {
        InfinispanTimeUtil.enableTestingTimeService(session);
        try {
            RealmModel realm = session.realms().getRealmByName("test");
            session.getContext().setRealm(realm);
            ClientModel client = realm.getClientByClientId("test-app");

            Set<String> validUserSessions = new HashSet<>();
            Set<String> validClientSessions = new HashSet<>();
            Set<String> expiredUserSessions = new HashSet<>();

            // create an user session that is older than the max lifespan timeout.
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session1) -> {
                session1.getContext().setRealm(realm);
                Time.setOffset(-(realm.getSsoSessionMaxLifespan() + 1));
                UserSessionModel userSession = session1.sessions().createUserSession(null, realm, session1.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                expiredUserSessions.add(userSession.getId());
                AuthenticatedClientSessionModel clientSession = session1.sessions().createClientSession(realm, client, userSession);
                assertEquals(userSession, clientSession.getUserSession());
            });

            // create an user session whose last refresh exceeds the max session idle timeout.
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session1) -> {
                session1.getContext().setRealm(realm);
                Time.setOffset(-(realm.getSsoSessionIdleTimeout() + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 1));
                UserSessionModel s = session1.sessions().createUserSession(null, realm, session1.users().getUserByUsername(realm, "user2"), "user2", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                // no need to explicitly set the last refresh time - it is the same as the creation time.
                expiredUserSessions.add(s.getId());
            });

            // create an user session and associated client session that conforms to the max lifespan and max idle timeouts.
            Time.setOffset(0);
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session1) -> {
                session1.getContext().setRealm(realm);
                UserSessionModel userSession = session1.sessions().createUserSession(null, realm, session1.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                validUserSessions.add(userSession.getId());
                validClientSessions.add(session1.sessions().createClientSession(realm, client, userSession).getId());
            });

            // remove the expired sessions - we expect the first two sessions to have been removed as they either expired the max lifespan or the session idle timeouts.
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session1) -> {
                session1.getContext().setRealm(realm);
                session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);
            });

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
                kcSession.getContext().setRealm(realm);
                for (String e : expiredUserSessions) {
                    assertNull(kcSession.sessions().getUserSession(realm, e));
                }

                for (String v : validUserSessions) {
                    UserSessionModel userSessionLoaded = kcSession.sessions().getUserSession(realm, v);
                    assertNotNull(userSessionLoaded);
                    // the only valid user session should also have a valid client session that hasn't expired.
                    AuthenticatedClientSessionModel clientSessionModel = userSessionLoaded.getAuthenticatedClientSessions().get(client.getId());
                    assertNotNull(clientSessionModel);
                    assertTrue(validClientSessions.contains(clientSessionModel.getId()));
                }
            });
        } finally {
            Time.setOffset(0);
            session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            InfinispanTimeUtil.disableTestingTimeService(session);
        }
    }

    @TestOnServer
    public  void testTransientUserSession(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        session.getContext().setRealm(realm);
        ClientModel client = realm.getClientByClientId("test-app");
        String userSessionId = UUID.randomUUID().toString();

        // create an user session, but don't persist it to infinispan
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session1) -> {
            session1.getContext().setRealm(realm);
            long sessionsBefore = session1.sessions().getActiveUserSessions(realm, client);

            UserSessionModel userSession = session1.sessions().createUserSession(userSessionId, realm, session1.users().getUserByUsername(realm, "user1"),
                    "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.TRANSIENT);
            AuthenticatedClientSessionModel clientSession = session1.sessions().createClientSession(realm, client, userSession);
            assertEquals(userSession, clientSession.getUserSession());

            assertSession(userSession, session.users().getUserByUsername(realm, "user1"), "127.0.0.1", userSession.getStarted(), userSession.getStarted(), "test-app");

            // Can find session by ID in current transaction
            UserSessionModel foundSession = session1.sessions().getUserSession(realm, userSessionId);
            Assertions.assertEquals(userSession, foundSession);

            // Count of sessions should be still the same
            Assertions.assertEquals(session1.sessions().getActiveUserSessions(realm, client), sessionsBefore);
        });

        // create an user session whose last refresh exceeds the max session idle timeout.
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession session1) -> {
            session1.getContext().setRealm(realm);
            UserSessionModel userSession = session1.sessions().getUserSession(realm, userSessionId);
            Assertions.assertNull(userSession);
        });
    }

    /**
     * Tests the removal of expired sessions with remember-me enabled. It differs from the non remember me scenario by
     * taking into consideration the specific remember-me timeout values.
     *
     * @param session the {@code KeycloakSession}
     */
    @TestOnServer
    public  void testRemoveUserSessionsByExpiredRememberMe(KeycloakSession session) {
        InfinispanTimeUtil.enableTestingTimeService(session);
        RealmModel testRealm = session.realms().getRealmByName("test");
        session.getContext().setRealm(testRealm);
        int previousMaxLifespan = testRealm.getSsoSessionMaxLifespanRememberMe();
        int previousMaxIdle = testRealm.getSsoSessionIdleTimeoutRememberMe();
        try {
            ClientModel client = testRealm.getClientByClientId("test-app");
            Set<String> validUserSessions = new HashSet<>();
            Set<String> validClientSessions = new HashSet<>();
            Set<String> expiredUserSessions = new HashSet<>();

            // first lets update the realm by setting remember-me timeout values, which will be 4 times higher than the default timeout values.
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
                RealmModel r = kcSession.realms().getRealmByName("test");
                kcSession.getContext().setRealm(r);
                r.setSsoSessionMaxLifespanRememberMe(r.getSsoSessionMaxLifespan() * 4);
                r.setSsoSessionIdleTimeoutRememberMe(r.getSsoSessionIdleTimeout() * 4);
                r.setRememberMe(true);
            });

            // create an user session with remember-me enabled that is older than the default 'max lifespan' timeout but not older than the 'max lifespan remember-me' timeout.
            // the session's last refresh also exceeds the default 'session idle' timeout but doesn't exceed the 'session idle remember-me' timeout.
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
                RealmModel realm = kcSession.realms().getRealmByName("test");
                kcSession.getContext().setRealm(realm);
                Time.setOffset(-(realm.getSsoSessionMaxLifespan() * 2));
                UserSessionModel userSession = kcSession.sessions().createUserSession(null, realm, kcSession.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                AuthenticatedClientSessionModel clientSession = kcSession.sessions().createClientSession(realm, client, userSession);
                assertEquals(userSession, clientSession.getUserSession());
                Time.setOffset(-(realm.getSsoSessionIdleTimeout() * 2));
                userSession.setLastSessionRefresh(Time.currentTime());
                clientSession.setTimestamp(Time.currentTime());
                validUserSessions.add(userSession.getId());
                validClientSessions.add(clientSession.getId());
            });

            // create an user session with remember-me enabled that is older than the 'max lifespan remember-me' timeout.
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
                RealmModel realm = kcSession.realms().getRealmByName("test");
                kcSession.getContext().setRealm(realm);
                Time.setOffset(-(realm.getSsoSessionMaxLifespanRememberMe() + 1));
                UserSessionModel userSession = kcSession.sessions().createUserSession(null, realm, kcSession.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                expiredUserSessions.add(userSession.getId());
            });

            // finally create an user session with remember-me enabled whose last refresh exceeds the 'session idle remember-me' timeout.
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
                RealmModel realm = kcSession.realms().getRealmByName("test");
                kcSession.getContext().setRealm(realm);
                Time.setOffset(-(realm.getSsoSessionIdleTimeoutRememberMe() + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 1));
                UserSessionModel userSession = kcSession.sessions().createUserSession(null, realm, kcSession.users().getUserByUsername(realm, "user2"), "user2", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                // no need to explicitly set the last refresh time - it is the same as the creation time.
                expiredUserSessions.add(userSession.getId());
            });

            // remove the expired sessions - the first session should not be removed as it doesn't exceed any of the remember-me timeout values.
            Time.setOffset(0);
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
                RealmModel realm = kcSession.realms().getRealmByName("test");
                kcSession.getContext().setRealm(realm);
                session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);
            });

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
                RealmModel realm = kcSession.realms().getRealmByName("test");
                kcSession.getContext().setRealm(realm);

                for (String sessionId : expiredUserSessions) {
                    assertNull(kcSession.sessions().getUserSession(realm, sessionId));
                }

                for (String sessionId : validUserSessions) {
                    UserSessionModel userSessionLoaded = kcSession.sessions().getUserSession(realm, sessionId);
                    assertNotNull(userSessionLoaded);
                    // the only valid user session should also have a valid client session that hasn't expired.
                    AuthenticatedClientSessionModel clientSessionModel = userSessionLoaded.getAuthenticatedClientSessions().get(client.getId());
                    assertNotNull(clientSessionModel);
                    assertTrue(validClientSessions.contains(clientSessionModel.getId()));
                }
            });
        } finally {
            Time.setOffset(0);
            session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            // restore the original remember-me timeout values in the realm.
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
                RealmModel r = kcSession.realms().getRealmByName("test");
                kcSession.getContext().setRealm(r);
                r.setSsoSessionMaxLifespanRememberMe(previousMaxLifespan);
                r.setSsoSessionIdleTimeoutRememberMe(previousMaxIdle);
                r.setRememberMe(false);
            });
            InfinispanTimeUtil.disableTestingTimeService(session);
        }
    }

    // KEYCLOAK-2508
    @TestOnServer
     public void testRemovingExpiredSession(KeycloakSession session) {
        UserSessionModel[] sessions = createSessions(session);
        try {
            UserSessionModel userSession = sessions[0];
            RealmModel realm = userSession.getRealm();
            session.getContext().setRealm(realm);
            // reload userSession in current session
            userSession = session.sessions().getUserSession(realm, userSession.getId());
            Time.setOffset(3600000);
            session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);

            // Assert no exception is thrown here
            session.sessions().removeUserSession(realm, userSession);
        } finally {
            Time.setOffset(0);
            session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
        }
    }

    @TestOnServer
    public void testGetByClient(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        final UserSessionModel[] sessions = createSessions(session);

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
            kcSession.getContext().setRealm(realm);
            assertSessions(kcSession.sessions().readOnlyStreamUserSessions(realm, realm.getClientByClientId("test-app"), -1, -1)
                    .collect(Collectors.toList()), sessions[0], sessions[1], sessions[2]);
            assertSessions(kcSession.sessions().readOnlyStreamUserSessions(realm, realm.getClientByClientId("third-party"), -1 ,-1)
                    .collect(Collectors.toList()), sessions[0]);
        });
    }

    @TestOnServer
    public void testGetByClientPaginated(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
            kcSession.getContext().setRealm(realm);
            try {
                for (int i = 0; i < 25; i++) {
                    Time.setOffset(i);
                    UserSessionModel userSession = kcSession.sessions().createUserSession(null, realm, kcSession.users().getUserByUsername(realm, "user1"), "user1", "127.0.0." + i, "form", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                    AuthenticatedClientSessionModel clientSession = kcSession.sessions().createClientSession(realm, realm.getClientByClientId("test-app"), userSession);
                    assertNotNull(clientSession);
                    clientSession.setRedirectUri("http://redirect");
                    clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, "state");
                    clientSession.setTimestamp(userSession.getStarted());
                    userSession.setLastSessionRefresh(userSession.getStarted());
                }
            } finally {
                Time.setOffset(0);
            }
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
            kcSession.getContext().setRealm(realm);
            assertPaginatedSession(kcSession, realm, realm.getClientByClientId("test-app"), 0, 1, 1);
            assertPaginatedSession(kcSession, realm, realm.getClientByClientId("test-app"), 0, 10, 10);
            assertPaginatedSession(kcSession, realm, realm.getClientByClientId("test-app"), 10, 10, 10);
            assertPaginatedSession(kcSession, realm, realm.getClientByClientId("test-app"), 20, 10, 5);
            assertPaginatedSession(kcSession, realm, realm.getClientByClientId("test-app"), 30, 10, 0);
        });
    }

    @TestOnServer
    public void testCreateAndGetInSameTransaction(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        session.getContext().setRealm(realm);
        ClientModel client = realm.getClientByClientId("test-app");
        UserSessionModel userSession = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.2", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
        createClientSession(session, client, userSession, "http://redirect", "state");

        UserSessionModel userSessionLoaded = session.sessions().getUserSession(realm, userSession.getId());
        AuthenticatedClientSessionModel clientSessionLoaded = userSessionLoaded.getAuthenticatedClientSessions().get(client.getId());
        Assertions.assertNotNull(userSessionLoaded);
        Assertions.assertNotNull(clientSessionLoaded);

        Assertions.assertEquals(userSession.getId(), clientSessionLoaded.getUserSession().getId());
        Assertions.assertEquals(1, userSessionLoaded.getAuthenticatedClientSessions().size());
    }

    @TestOnServer
    public void testAuthenticatedClientSessions(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        session.getContext().setRealm(realm);

        realm.setSsoSessionIdleTimeout(1800);
        realm.setSsoSessionMaxLifespan(36000);
        UserSessionModel userSession = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.2", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);

        ClientModel client1 = realm.getClientByClientId("test-app");
        ClientModel client2 = realm.getClientByClientId("third-party");

        // Create client1 session
        AuthenticatedClientSessionModel clientSession1 = session.sessions().createClientSession(realm, client1, userSession);
        clientSession1.setAction("foo1");
        int currentTime1 = Time.currentTime();
        clientSession1.setTimestamp(currentTime1);

        // Create client2 session
        AuthenticatedClientSessionModel clientSession2 = session.sessions().createClientSession(realm, client2, userSession);
        clientSession2.setAction("foo2");
        int currentTime2 = Time.currentTime();
        clientSession2.setTimestamp(currentTime2);

        // Ensure sessions are here
        userSession = session.sessions().getUserSession(realm, userSession.getId());
        Map<String, AuthenticatedClientSessionModel> clientSessions = userSession.getAuthenticatedClientSessions();
        Assertions.assertEquals(2, clientSessions.size());
        testAuthenticatedClientSession(clientSessions.get(client1.getId()), "test-app", userSession.getId(), "foo1", currentTime1);
        testAuthenticatedClientSession(clientSessions.get(client2.getId()), "third-party", userSession.getId(), "foo2", currentTime2);

        // Update session1
        clientSessions.get(client1.getId()).setAction("foo1-updated");


        // Ensure updated
        userSession = session.sessions().getUserSession(realm, userSession.getId());
        clientSessions = userSession.getAuthenticatedClientSessions();
        testAuthenticatedClientSession(clientSessions.get(client1.getId()), "test-app", userSession.getId(), "foo1-updated", currentTime1);

        // Rewrite session2
        clientSession2 = session.sessions().createClientSession(realm, client2, userSession);
        clientSession2.setAction("foo2-rewrited");
        int currentTime3 = Time.currentTime();
        clientSession2.setTimestamp(currentTime3);


        // Ensure updated
        userSession = session.sessions().getUserSession(realm, userSession.getId());
        clientSessions = userSession.getAuthenticatedClientSessions();
        Assertions.assertEquals(2, clientSessions.size());
        testAuthenticatedClientSession(clientSessions.get(client1.getId()), "test-app", userSession.getId(), "foo1-updated", currentTime1);
        testAuthenticatedClientSession(clientSessions.get(client2.getId()), "third-party", userSession.getId(), "foo2-rewrited", currentTime3);

        // remove session
        clientSession1 = userSession.getAuthenticatedClientSessions().get(client1.getId());
        clientSession1.detachFromUserSession();

        userSession = session.sessions().getUserSession(realm, userSession.getId());
        clientSessions = userSession.getAuthenticatedClientSessions();
        Assertions.assertEquals(1, clientSessions.size());
        Assertions.assertNull(clientSessions.get(client1.getId()));
    }


    private static void testAuthenticatedClientSession(AuthenticatedClientSessionModel clientSession, String expectedClientId, String expectedUserSessionId, String expectedAction, int expectedTimestamp) {
        Assertions.assertEquals(expectedClientId, clientSession.getClient().getClientId());
        Assertions.assertEquals(expectedUserSessionId, clientSession.getUserSession().getId());
        Assertions.assertEquals(expectedAction, clientSession.getAction());
        Assertions.assertEquals(expectedTimestamp, clientSession.getTimestamp());
    }

    private static void assertPaginatedSession(KeycloakSession session, RealmModel realm, ClientModel client, int start, int max, int expectedSize) {
        assertEquals(expectedSize, session.sessions().readOnlyStreamUserSessions(realm, client, start, max).count());
    }

    @Test
    public void testGetCountByClient() {
        runOnServer.run(UserSessionProviderTest::testGetCountByClient);
    }

    public static void testGetCountByClient(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        createSessions(session);

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            kcSession.getContext().setRealm(realm);
            assertEquals(3, kcSession.sessions().getActiveUserSessions(realm, realm.getClientByClientId("test-app")));
            assertEquals(1, kcSession.sessions().getActiveUserSessions(realm, realm.getClientByClientId("third-party")));
        });
    }

    @Test
    public void loginFailures() {
        runOnServer.run((KeycloakSession kcSession) -> {
            RealmModel realm = kcSession.realms().getRealmByName("test");
            kcSession.getContext().setRealm(realm);
            UserLoginFailureModel failure1 = kcSession.loginFailures().addUserLoginFailure(realm, "user1");
            failure1.incrementFailures();
            failure1.setLastFailure(Time.currentTimeMillis());

            UserLoginFailureModel failure2 = kcSession.loginFailures().addUserLoginFailure(realm, "user2");
            failure2.incrementFailures();
            failure2.incrementFailures();
            failure2.setLastFailure(Time.currentTimeMillis());
        });

        runOnServer.run((KeycloakSession kcSession) -> {
            RealmModel realm = kcSession.realms().getRealmByName("test");
            kcSession.getContext().setRealm(realm);

            UserLoginFailureModel failure1 = kcSession.loginFailures().getUserLoginFailure(realm, "user1");
            assertEquals(1, failure1.getNumFailures());

            UserLoginFailureModel failure2 = kcSession.loginFailures().getUserLoginFailure(realm, "user2");
            assertEquals(2, failure2.getNumFailures());

            // Add the failure, which already exists
            failure1.incrementFailures();

            assertEquals(2, failure1.getNumFailures());

            failure1 = kcSession.loginFailures().getUserLoginFailure(realm, "user1");
            failure1.clearFailures();

            failure1 = kcSession.loginFailures().getUserLoginFailure(realm, "user1");
            assertEquals(0, failure1.getNumFailures());
        });

        runOnServer.run((KeycloakSession kcSession) -> {
            RealmModel realm = kcSession.realms().getRealmByName("test");
            kcSession.getContext().setRealm(realm);
            kcSession.loginFailures().removeUserLoginFailure(realm, "user1");
        });

        runOnServer.run((KeycloakSession kcSession) -> {
            RealmModel realm = kcSession.realms().getRealmByName("test");
            kcSession.getContext().setRealm(realm);

            assertNull(kcSession.loginFailures().getUserLoginFailure(realm, "user1"));

            kcSession.loginFailures().removeAllUserLoginFailures(realm);
        });

        runOnServer.run((KeycloakSession kcSession) -> {
            RealmModel realm = kcSession.realms().getRealmByName("test");
            kcSession.getContext().setRealm(realm);
            assertNull(kcSession.loginFailures().getUserLoginFailure(realm, "user1"));
            assertNull(kcSession.loginFailures().getUserLoginFailure(realm, "user2"));
        });
    }

    @Test
    public void testOnUserRemoved() {
        runOnServer.run(UserSessionProviderTest::testOnUserRemoved);
    }

    public static void testOnUserRemoved(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        session.getContext().setRealm(realm);
        UserModel user1 = session.users().getUserByUsername(realm, "user1");
        UserModel user2 = session.users().getUserByUsername(realm, "user2");

        createSessions(session);

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
            kcSession.getContext().setRealm(realm);
            assertEquals(2, kcSession.sessions().getUserSessionsStream(realm, user1).count());
            assertEquals(1, kcSession.sessions().getUserSessionsStream(realm, user2).count());
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
            kcSession.getContext().setRealm(realm);
            // remove user1
            new UserManager(kcSession).removeUser(realm, user1);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
            kcSession.getContext().setRealm(realm);
            assertEquals(0, kcSession.sessions().getUserSessionsStream(realm, user1).count());
            assertEquals(1, kcSession.sessions().getUserSessionsStream(realm, user2).count());
        });
    }

    @Test
    public void testOnUserRemovedLazyUserAttributesAreLoaded() {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            UserModel user1 = session.users().getUserByUsername(realm, "user1");
            user1.setSingleAttribute("customAttribute", "value1");
        });
        runOnServer.run(UserSessionProviderTest::testOnUserRemovedLazyUserAttributesAreLoaded);
    }

    @TestOnServer
    public void testReadOnlyStreams(KeycloakSession session) {
        var sessions = KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), UserSessionProviderTest::createSessions);

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), kcSession -> {
            RealmModel realm = kcSession.realms().getRealmByName("test");
            kcSession.getContext().setRealm(realm);
            var readOnlySessionList = kcSession.sessions().readOnlyStreamUserSessions(realm).toList();
            assertSessions(readOnlySessionList, sessions);

            // all sessions have client sessions from test-app
            readOnlySessionList = kcSession.sessions().readOnlyStreamUserSessions(realm, realm.getClientByClientId("test-app"), -1, -1).toList();
            assertSessions(readOnlySessionList, sessions);

            // only the first one have a client session form third-party
            readOnlySessionList = kcSession.sessions().readOnlyStreamUserSessions(realm, realm.getClientByClientId("third-party"), -1, -1).toList();
            assertSessions(readOnlySessionList, sessions[0]);
        });
    }

    public static void testOnUserRemovedLazyUserAttributesAreLoaded(KeycloakSession session) {
        RealmModel realm = session.realms().getRealmByName("test");
        UserModel user1 = session.users().getUserByUsername(realm, "user1");

        Map<String, List<String>> attributes = new HashMap<>();
        ProviderEventListener providerEventListener = event -> {
            if (event instanceof UserModel.UserRemovedEvent userRemovedEvent) {
                attributes.putAll(userRemovedEvent.getUser().getAttributes());
            }
        };
        session.getKeycloakSessionFactory().register(providerEventListener);
        try {
            new UserManager(session).removeUser(realm, user1);
            // UserModel.FIRST_NAME, UserModel.LAST_NAME, UserModel.EMAIL, UserModel.USERNAME, customAttribute;
            assertEquals(5, attributes.size());
        } finally {
            session.getKeycloakSessionFactory().unregister(providerEventListener);
        }
    }

    private static void createClientSession(KeycloakSession session, ClientModel client, UserSessionModel userSession, String redirect, String state) {
        RealmModel realm = session.realms().getRealmByName("test");
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
    }

    private static UserSessionModel[] createSessions(KeycloakSession session) {
        UserSessionModel[] sessions = new UserSessionModel[3];
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession kcSession) -> {
            RealmModel realm = kcSession.realms().getRealmByName("test");
            kcSession.getContext().setRealm(realm);

            sessions[0] = kcSession.sessions().createUserSession(null, realm, kcSession.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);

            createClientSession(kcSession, realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state");
            createClientSession(kcSession, realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state");

            sessions[1] = kcSession.sessions().createUserSession(null, realm, kcSession.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.2", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            createClientSession(kcSession, realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state");

            sessions[2] = kcSession.sessions().createUserSession(null, realm, kcSession.users().getUserByUsername(realm, "user2"), "user2", "127.0.0.3", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            createClientSession(kcSession, realm.getClientByClientId("test-app"), sessions[2], "http://redirect", "state");
        });

        return sessions;
    }

    public static void assertSessions(List<UserSessionModel> actualSessions, UserSessionModel... expectedSessions) {
        String[] expected = new String[expectedSessions.length];
        for (int i = 0; i < expected.length; i++) {
            expected[i] = expectedSessions[i].getId();
        }

        String[] actual = new String[actualSessions.size()];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = actualSessions.get(i).getId();
        }

        Arrays.sort(expected);
        Arrays.sort(actual);

        assertArrayEquals(expected, actual);
    }

    public static  void assertSession(UserSessionModel session, UserModel user, String ipAddress, int started, int lastRefresh, String... clients) {
        assertEquals(user.getId(), session.getUser().getId());
        assertEquals(ipAddress, session.getIpAddress());
        assertEquals(user.getUsername(), session.getLoginUsername());
        assertEquals("form", session.getAuthMethod());
        assertTrue(session.isRememberMe());
        assertTrue(session.getStarted() >= started - 1 && session.getStarted() <= started + 1);
        assertTrue(session.getLastSessionRefresh() >= lastRefresh - 1 && session.getLastSessionRefresh() <= lastRefresh + 1);

        String[] actualClients = new String[session.getAuthenticatedClientSessions().size()];
        int i = 0;
        for (Map.Entry<String, AuthenticatedClientSessionModel> entry : session.getAuthenticatedClientSessions().entrySet()) {
            String clientUUID = entry.getKey();
            AuthenticatedClientSessionModel clientSession = entry.getValue();
            Assertions.assertEquals(clientUUID, clientSession.getClient().getId());
            actualClients[i] = clientSession.getClient().getClientId();
            i++;
        }

        Arrays.sort(clients);
        Arrays.sort(actualClients);

        assertArrayEquals(clients, actualClients);
    }

    private static class UserSessionProviderRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name("test");
            realm.addClient("test-app");
            realm.addClient("third-party");
            return realm;
        }
    }

}
