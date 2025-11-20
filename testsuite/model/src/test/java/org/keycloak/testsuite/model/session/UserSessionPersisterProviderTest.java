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

package org.keycloak.testsuite.model.session;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import jakarta.persistence.EntityManager;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.jpa.session.JpaUserSessionPersisterProvider;
import org.keycloak.models.jpa.session.JpaUserSessionPersisterProviderFactory;
import org.keycloak.models.jpa.session.PersistentUserSessionEntity;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.PersistentUserSessionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.utils.RealmExpiration;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.client.ClientStorageProvider;
import org.keycloak.storage.client.ClientStorageProviderModel;
import org.keycloak.testsuite.federation.HardcodedClientStorageProviderFactory;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;

import org.hamcrest.Matchers;
import org.infinispan.Cache;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME;
import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
import static org.keycloak.models.utils.SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@RequireProvider(UserSessionPersisterProvider.class)
@RequireProvider(UserSessionProvider.class)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
@RequireProvider(EventStoreProvider.class)
public class UserSessionPersisterProviderTest extends KeycloakModelTest {

    private static final int USER_SESSION_COUNT = 2000;
    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = createRealm(s, "test");
        s.getContext().setRealm(realm);
        realm.setSsoSessionMaxLifespan(Constants.DEFAULT_SESSION_MAX_LIFESPAN);
        realm.setSsoSessionIdleTimeout(Constants.DEFAULT_SESSION_IDLE_TIMEOUT);
        realm.setOfflineSessionIdleTimeout(Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT);
        realm.setOfflineSessionMaxLifespan(Constants.DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN);
        realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
        this.realmId = realm.getId();

        s.users().addUser(realm, "user1").setEmail("user1@localhost");
        s.users().addUser(realm, "user2").setEmail("user2@localhost");

        createClients(s, realm);
    }

    protected static void createClients(KeycloakSession s, RealmModel realm) {
        ClientModel clientModel = s.clients().addClient(realm, "test-app");
        clientModel.setEnabled(true);
        clientModel.setBaseUrl("http://localhost:8180/auth/realms/master/app/auth");
        Set<String> redirects = new HashSet<>(Arrays.asList("http://localhost:8180/auth/realms/master/app/auth/*",
                "https://localhost:8543/auth/realms/master/app/auth/*",
                "http://localhost:8180/auth/realms/test/app/auth/*",
                "https://localhost:8543/auth/realms/test/app/auth/*"));
        clientModel.setRedirectUris(redirects);
        clientModel.setSecret("password");

        clientModel = s.clients().addClient(realm, "third-party");
        clientModel.setEnabled(true);
        clientModel.setConsentRequired(true);
        clientModel.setBaseUrl("http://localhost:8180/auth/realms/master/app/auth");
        clientModel.setRedirectUris(redirects);
        clientModel.setSecret("password");
    }

    @Override
    public void cleanEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().getRealm(realmId);
        s.getContext().setRealm(realm);
        s.sessions().removeUserSessions(realm);

        UserModel user1 = s.users().getUserByUsername(realm, "user1");
        UserModel user2 = s.users().getUserByUsername(realm, "user2");

        UserManager um = new UserManager(s);
        if (user1 != null) {
            um.removeUser(realm, user1);
        }
        if (user2 != null) {
            um.removeUser(realm, user2);
        }

        s.realms().removeRealm(realmId);
    }

    @Test
    public void testPersistenceWithLoad() {
        int started = Time.currentTime();
        final UserSessionModel[] userSession = new UserSessionModel[1];

        UserSessionModel[] origSessions = inComittedTransaction(session -> {
            // Create some sessions in infinispan
            return createSessions(session, realmId);
        });

        inComittedTransaction(session -> {
            // Persist 3 created userSessions and clientSessions as offline
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            ClientModel testApp = realm.getClientByClientId("test-app");
            session.sessions().getUserSessionsStream(realm, testApp).toList()
                    .forEach(userSessionLooper -> persistUserSession(session, userSessionLooper, true));
        });

        if (!MultiSiteUtils.isPersistentSessionsEnabled()) {
            inComittedTransaction(session -> {
                // Persist 1 online session
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                userSession[0] = session.sessions().getUserSession(realm, origSessions[0].getId());
                persistUserSession(session, userSession[0], false);
            });

            inComittedTransaction(session -> { // Assert online session
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, false, 1, 1, 1);
                assertSession(loadedSessions.get(0), session.users().getUserByUsername(realm, "user1"), "127.0.0.1", started, started, "test-app", "third-party");
            });
        }

        inComittedTransaction(session -> {
            // Assert offline sessions
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, true, 2, 2, 3);
            assertSessions(loadedSessions, new String[] { origSessions[0].getId(), origSessions[1].getId(), origSessions[2].getId() });

            assertSessionLoaded(loadedSessions, origSessions[0].getId(), session.users().getUserByUsername(realm, "user1"), "127.0.0.1", started, started, "test-app", "third-party");
            assertSessionLoaded(loadedSessions, origSessions[1].getId(), session.users().getUserByUsername(realm, "user1"), "127.0.0.2", started, started, "test-app");
            assertSessionLoaded(loadedSessions, origSessions[2].getId(), session.users().getUserByUsername(realm, "user2"), "127.0.0.3", started, started, "test-app");
        });
    }

    @Test
    public void testUpdateAndRemove() {
        int started = Time.currentTime();

        AtomicReference<UserSessionModel[]> origSessionsAt = new AtomicReference<>();
        AtomicReference<UserSessionModel> userSessionAt = new AtomicReference<>();

        inComittedTransaction(session -> {
            // Create some sessions in infinispan
            UserSessionModel[] origSessions = createSessions(session, realmId);
            origSessionsAt.set(origSessions);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            UserSessionModel[] origSessions = origSessionsAt.get();

            // Persist 1 offline session
            UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
            userSessionAt.set(userSession);

            persistUserSession(session, userSession, true);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

            // Load offline session
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, true, 10, 1, 1);

            UserSessionModel persistedSession = loadedSessions.get(0);

            assertSession(persistedSession, session.users().getUserByUsername(realm, "user1"), "127.0.0.2", started, started, "test-app");

            // create new clientSession
            AuthenticatedClientSessionModel clientSession = createClientSession(session, realmId, realm.getClientByClientId("third-party"), session.sessions().getUserSession(realm, persistedSession.getId()),
                    "http://redirect", "state");
            persister.createClientSession(clientSession, true);

        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            UserSessionModel userSession = userSessionAt.get();

            // Remove clientSession
            persister.removeClientSession(userSession.getId(), realm.getClientByClientId("third-party").getId(), true);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

            // Assert clientSession removed
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, true, 10, 1, 1);
            UserSessionModel persistedSession = loadedSessions.get(0);
            assertSession(persistedSession, session.users().getUserByUsername(realm, "user1"), "127.0.0.2", started, started, "test-app");

            // Remove userSession
            persister.removeUserSession(persistedSession.getId(), true);
        });

        inComittedTransaction(session -> {
            // Assert nothing found
            loadPersistedSessionsPaginated(session, true, 10, 0, 0);
        });
    }

    @Test
    public void testOnRealmRemoved() {
        AtomicReference<String> userSessionID = new AtomicReference<>();

        inComittedTransaction(session -> {
            RealmModel fooRealm = session.realms().createRealm("foo");
            session.getContext().setRealm(fooRealm);
            fooRealm.setDefaultRole(session.roles().addRealmRole(fooRealm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + fooRealm.getName()));

            fooRealm.addClient("foo-app");
            session.users().addUser(fooRealm, "user3");

            UserSessionModel userSession = session.sessions().createUserSession(null, fooRealm, session.users().getUserByUsername(fooRealm, "user3"), "user3", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            userSessionID.set(userSession.getId());

            createClientSession(session, fooRealm.getId(), fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
        });

        inComittedTransaction(session -> {
            // Persist offline session
            RealmModel fooRealm = session.realms().getRealmByName("foo");
            session.getContext().setRealm(fooRealm);
            UserSessionModel userSession = session.sessions().getUserSession(fooRealm, userSessionID.get());
            assertNotNull(userSession);
            persistUserSession(session, userSession, true);
        });

        inComittedTransaction(session -> {
            // Assert session was persisted
            loadPersistedSessionsPaginated(session, true, 10, 1, 1);

            // Remove realm
            RealmManager realmMgr = new RealmManager(session);
            RealmModel fooRealm = realmMgr.getRealmByName("foo");
            session.getContext().setRealm(fooRealm);
            realmMgr.removeRealm(fooRealm);
        });

        inComittedTransaction(session -> {
            // Assert nothing loaded
            loadPersistedSessionsPaginated(session, true, 10, 0, 0);
        });
    }

    @Test
    public void testOnClientRemoved() {
        int started = Time.currentTime();
        AtomicReference<String> userSessionID = new AtomicReference<>();

        inComittedTransaction(session -> {
            RealmModel fooRealm = session.realms().createRealm("foo");
            session.getContext().setRealm(fooRealm);
            fooRealm.setDefaultRole(session.roles().addRealmRole(fooRealm, Constants.DEFAULT_ROLES_ROLE_PREFIX));

            fooRealm.addClient("foo-app");
            fooRealm.addClient("bar-app");
            session.users().addUser(fooRealm, "user3");

            UserSessionModel userSession = session.sessions().createUserSession(null, fooRealm, session.users().getUserByUsername(fooRealm, "user3"), "user3", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            userSessionID.set(userSession.getId());

            createClientSession(session, fooRealm.getId(), fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
            createClientSession(session, fooRealm.getId(), fooRealm.getClientByClientId("bar-app"), userSession, "http://redirect", "state");
        });

        inComittedTransaction(session -> {
            RealmModel fooRealm = session.realms().getRealmByName("foo");
            session.getContext().setRealm(fooRealm);

            // Persist offline session
            UserSessionModel userSession = session.sessions().getUserSession(fooRealm, userSessionID.get());
            persistUserSession(session, userSession, true);
        });

        inComittedTransaction(session -> {
            RealmManager realmMgr = new RealmManager(session);
            ClientManager clientMgr = new ClientManager(realmMgr);
            RealmModel fooRealm = realmMgr.getRealmByName("foo");
            session.getContext().setRealm(fooRealm);

            // Assert session was persisted with both clientSessions
            UserSessionModel persistedSession = loadPersistedSessionsPaginated(session, true, 10, 1, 1).get(0);
            assertSession(persistedSession, session.users().getUserByUsername(fooRealm, "user3"), "127.0.0.1", started, started, "foo-app", "bar-app");

            // Remove foo-app client
            ClientModel client = fooRealm.getClientByClientId("foo-app");
            clientMgr.removeClient(fooRealm, client);
        });

        inComittedTransaction(session -> {
            RealmManager realmMgr = new RealmManager(session);
            ClientManager clientMgr = new ClientManager(realmMgr);
            RealmModel fooRealm = realmMgr.getRealmByName("foo");
            session.getContext().setRealm(fooRealm);

            // Assert just one bar-app clientSession persisted now
            UserSessionModel persistedSession = loadPersistedSessionsPaginated(session, true, 10, 1, 1).get(0);
            assertSession(persistedSession, session.users().getUserByUsername(fooRealm, "user3"), "127.0.0.1", started, started, "bar-app");

            // Remove bar-app client
            ClientModel client = fooRealm.getClientByClientId("bar-app");
            clientMgr.removeClient(fooRealm, client);
        });

        inComittedTransaction(session -> {
            // Assert loading still works - last userSession is still there, but no clientSession on it
            loadPersistedSessionsPaginated(session, true, 10, 1, 1);

            // Cleanup
            RealmManager realmMgr = new RealmManager(session);
            RealmModel fooRealm = realmMgr.getRealmByName("foo");
            session.getContext().setRealm(fooRealm);
            realmMgr.removeRealm(fooRealm);
        });
    }

    @Test
    public void testClientTimestampUpdate() {
        final String realmName = "client-test";
        final String username = "my-user";
        final String clientId = "my-app";
        final AtomicReference<String> userSessionID = new AtomicReference<>();

        // create user and client
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().createRealm(realmName);
            session.getContext().setRealm(realm);
            realm.setDefaultRole(session.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX));

            realm.addClient(clientId);
            session.users().addUser(realm, username);

            UserSessionModel userSession = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, username), username, "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            userSessionID.set(userSession.getId());

            createClientSession(session, realm.getId(), realm.getClientByClientId(clientId), userSession, "http://redirect", "state");
        });

        if (InfinispanUtils.isEmbeddedInfinispan()) {
            // causes https://github.com/keycloak/keycloak/issues/42012
            inComittedTransaction(session -> {
                RealmModel realm = session.realms().getRealmByName(realmName);
                session.getContext().setRealm(realm);

                var cacheKey = new EmbeddedClientSessionKey(userSessionID.get(), realm.getClientByClientId(clientId).getId());
                Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientSessoinCache = session.getProvider(InfinispanConnectionProvider.class).getCache(CLIENT_SESSION_CACHE_NAME);
                SessionEntityWrapper<AuthenticatedClientSessionEntity> clientSession = clientSessoinCache.get(cacheKey);
                assertNotNull(clientSession);
                assertNotNull(clientSession.getEntity());
                // user session id is not stored in the cache
                // when reading from a remote keycloak instance, this field is null
                // we are simulating a “remote read” here.
                clientSession.getEntity().setUserSessionId(null);
            });
        }


        Function<KeycloakSession, Integer> fetchTimestamp = session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            ClientModel client = realm.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionID.get());
            // read from database!
            if (Profile.isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS)) {
                return session.getProvider(UserSessionPersisterProvider.class)
                        .loadClientSession(realm, client, userSession, false)
                        .getTimestamp();
            }
            return session.sessions()
                    .getClientSession(userSession, client, false)
                    .getTimestamp();
        };

        // fetch the current timestamp
        int currentTimestamp = inComittedTransaction(fetchTimestamp);

        // update timestamp
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realm);

            ClientModel client = realm.getClientByClientId(clientId);
            UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionID.get());
            session.sessions()
                    .getClientSession(userSession, client, false)
                    .setTimestamp(currentTimestamp + 10);
        });

        // check if it is updated
        int timestamp = inComittedTransaction(fetchTimestamp);
        assertEquals(currentTimestamp + 10, timestamp);
    }

    @Test
    public void testOnUserRemoved() {
        int started = Time.currentTime();
        AtomicReference<UserSessionModel[]> origSessionsAt = new AtomicReference<>();

        inComittedTransaction(session -> {
            // Create some sessions in infinispan
            UserSessionModel[] origSessions = createSessions(session, realmId);
            origSessionsAt.set(origSessions);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            UserSessionModel[] origSessions = origSessionsAt.get();

            // Persist 2 offline sessions of 2 users
            UserSessionModel userSession1 = session.sessions().getUserSession(realm, origSessions[1].getId());
            UserSessionModel userSession2 = session.sessions().getUserSession(realm, origSessions[2].getId());
            persistUserSession(session, userSession1, true);
            persistUserSession(session, userSession2, true);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            // Load offline sessions
            loadPersistedSessionsPaginated(session, true, 10, 1, 2);

            // Properly delete user and assert his offlineSession removed
            UserModel user1 = session.users().getUserByUsername(realm, "user1");
            new UserManager(session).removeUser(realm, user1);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            if (InfinispanUtils.isEmbeddedInfinispan()) {
                // when configured with external Infinispan only, the sessions are not persisted into the database.
                Assert.assertEquals(1, persister.getUserSessionsCount(true));
            }

            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, true, 10, 1, 1);
            UserSessionModel persistedSession = loadedSessions.get(0);
            assertSession(persistedSession, session.users().getUserByUsername(realm, "user2"), "127.0.0.3", started, started, "test-app");

            // KEYCLOAK-2431 Assert that userSessionPersister is resistent even to situation, when users are deleted "directly".
            // No exception will happen. However session will be still there
            UserModel user2 = session.users().getUserByUsername(realm, "user2");
            session.users().removeUser(realm, user2);

            loadedSessions = loadPersistedSessionsPaginated(session, true, 10, 1, 1);

            // Cleanup
            UserSessionModel userSession = loadedSessions.get(0);
            session.sessions().removeUserSession(realm, userSession);
            persister.removeUserSession(userSession.getId(), userSession.isOffline());
        });
    }

    // KEYCLOAK-1999
    @Test
    public void testNoSessions() {
        inComittedTransaction(session -> {
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            Stream<UserSessionModel> sessions = persister.loadUserSessionsStream(0, 1, true, "");
            Assert.assertEquals(0, sessions.count());
        });
    }

    @Test
    public void testMoreSessions() {
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);

            // Create 10 userSessions - each having 1 clientSession
            List<String> userSessionsInner = new LinkedList<>();
            UserModel user = session.users().getUserByUsername(realm, "user1");

            for (int i = 0; i < USER_SESSION_COUNT; i++) {
                // Having different offsets for each session (to ensure that lastSessionRefresh is also different)
                setTimeOffset(i);

                UserSessionModel userSession = session.sessions().createUserSession(null, realm, user, "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                if (userSessionsInner.contains(userSession.getId())) {
                    Assert.fail("Duplicate session id generated: " + userSession.getId());
                }

                createClientSession(session, realmId, realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
                userSessionsInner.add(userSession.getId());
                persistUserSession(session, userSession, true);
            }
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            final int sessionsPerPage = 3;
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, true, sessionsPerPage,
              USER_SESSION_COUNT / sessionsPerPage + (USER_SESSION_COUNT / sessionsPerPage == 0 ? 0 : 1), USER_SESSION_COUNT);
            UserModel user = session.users().getUserByUsername(realm, "user1");
            ClientModel testApp = realm.getClientByClientId("test-app");

            for (UserSessionModel loadedSession : loadedSessions) {
                assertEquals(user.getId(), loadedSession.getUser().getId());
                assertEquals("127.0.0.1", loadedSession.getIpAddress());
                assertEquals(user.getUsername(), loadedSession.getLoginUsername());

                assertEquals(1, loadedSession.getAuthenticatedClientSessions().size());
                assertTrue(loadedSession.getAuthenticatedClientSessions().containsKey(testApp.getId()));
            }
            return null;
        });

    }

    @Test
    public void testConcurrentSessionCreation() {
        String userSessionId = withRealm(realmId, (session, realm) -> {
            UserModel user = session.users().getUserByUsername(realm, "user1");
            UserSessionModel userSession = session.sessions().createUserSession(null, realm, user, "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            userSession.setNote("ITERATION1", "true");
            return userSession.getId();
        });

        // Simulate a concurrently created session
        withRealm(realmId, (session, realm) -> {
            UserModel user = session.users().getUserByUsername(realm, "user1");
            UserSessionModel userSession = session.sessions().createUserSession(userSessionId, realm, user, "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
            userSession.setNote("ITERATION2", "true");
            return null;
        });

        withRealm(realmId, (session, realm) -> {
            UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
            assertThat(userSession.getNote("ITERATION1"), Matchers.equalTo("true"));
            assertThat(userSession.getNote("ITERATION2"), Matchers.equalTo("true"));
            return null;
        });

        if (MultiSiteUtils.isPersistentSessionsEnabled()) {
            try {
                // Simulate a concurrently created session with a different user
                withRealm(realmId, (session, realm) -> {
                    UserModel user = session.users().getUserByUsername(realm, "user2");
                    UserSessionModel userSession = session.sessions().createUserSession(userSessionId, realm, user, "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                    userSession.setNote("ITERATION2", "true");
                    return null;
                });
                Assert.fail("Exception expected");
            } catch (RuntimeException e) {
                assertThat(e.getMessage(), Matchers.containsString("Maximum number of retries reached"));
                assertThat(e.getCause().getMessage(), Matchers.containsString("User ID of the session does not match"));
            }
        }

    }

    @Test
    public void testExpiredSessions() {
        int started = Time.currentTime();
        final UserSessionModel[] userSession1 = {null};
        final UserSessionModel[] userSession2 = {null};

        UserSessionModel[] origSessions = inComittedTransaction(session -> {
            // Create some sessions in infinispan
            return createSessions(session, realmId);
        });

        inComittedTransaction(session -> {
            // Persist 2 offline sessions of 2 users
            RealmModel realm = session.realms().getRealm(realmId);
            session.getContext().setRealm(realm);
            userSession1[0] = session.sessions().getUserSession(realm, origSessions[1].getId());
            userSession2[0] = session.sessions().getUserSession(realm, origSessions[2].getId());
            persistUserSession(session, userSession1[0], true);
            persistUserSession(session, userSession2[0], true);
        });

        int lastSessionRefresh = withRealm(realmId, (session, realm) -> {
            // Update one of the sessions with lastSessionRefresh of 20 days ahead
            int newCurrentTime = Time.currentTime() + 1728000;
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

            persister.updateLastSessionRefreshes(realm, newCurrentTime, Collections.singleton(userSession1[0].getId()), true);

            return newCurrentTime;
        });

        withRealmConsumer(realmId, (session, realm) -> {
            // Increase time offset - 40 days
            setTimeOffset(3456000);
            try {
                // Run expiration thread
                session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);

                // Test the updated session is still in persister. Not updated session is not there anymore
                List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, true, 10, 1, 1);
                UserSessionModel persistedSession = loadedSessions.get(0);
                assertSession(persistedSession, session.users().getUserByUsername(realm, "user1"), "127.0.0.2", started, lastSessionRefresh, "test-app");

            } finally {
                // Cleanup
                setTimeOffset(0);
                session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            }
        });
    }

    @Test
    @RequireProvider(ClientStorageProvider.class)
    public void testPersistenceWithLoadWithExternalClientStorage() {
        try {
            inComittedTransaction(session -> {
                setupClientStorageComponents(session, session.realms().getRealm(realmId));
            });

            int started = Time.currentTime();

            UserSessionModel origSession = inComittedTransaction(session -> {
                // Create session in infinispan
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);

                UserSessionModel userSession = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                createClientSession(session, realmId, realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
                createClientSession(session, realmId, realm.getClientByClientId("external-storage-client"), userSession, "http://redirect", "state");

                return userSession;
            });

            inComittedTransaction(session -> {
                // Persist created userSession and clientSessions as offline
                persistUserSession(session, origSession, true);
            });

            inComittedTransaction(session -> {
                // Assert offline session
                RealmModel realm = session.realms().getRealm(realmId);
                session.getContext().setRealm(realm);
                List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, true, 1, 1, 1);

                assertSessions(loadedSessions, new String[]{origSession.getId()});
                assertSessionLoaded(loadedSessions, origSession.getId(), session.users().getUserByUsername(realm, "user1"), "127.0.0.1", started, started, "test-app", "external-storage-client");
            });
        } finally {
            inComittedTransaction(session -> {
                cleanClientStorageComponents(session, session.realms().getRealm(realmId));
            });
        }
    }

    @Test
    @Deprecated(since = "26.4", forRemoval = true)
    public void testMigrateSession() {
        Assume.assumeTrue(MultiSiteUtils.isPersistentSessionsEnabled());
        Assume.assumeTrue(InfinispanUtils.isEmbeddedInfinispan());

        UserSessionModel[] sessions = inComittedTransaction(session -> {
            // Create some sessions in infinispan
            return createSessions(session, realmId);
        });

        inComittedTransaction(session -> {
            // clear the entries in the database to enable the migration
            JpaUserSessionPersisterProvider sessionPersisterProvider = (JpaUserSessionPersisterProvider) session.getProvider(UserSessionPersisterProvider.class);
            sessionPersisterProvider.removeUserSessions(session.realms().getRealm(realmId), false);

            // verify that clearing was successful
            Assert.assertEquals(0, countUserSessionsInRealm(session));
        });

        inComittedTransaction(session -> {
            // trigger a migration with the entries that are still in the cache
            PersistentUserSessionProvider userSessionProvider = (PersistentUserSessionProvider) session.getProvider(UserSessionProvider.class);
            userSessionProvider.migrateNonPersistentSessionsToPersistentSessions();

            // verify that import was complete
            Assert.assertEquals(sessions.length, countUserSessionsInRealm(session));
        });
    }


    @Test
    public void testUserRemoved() throws InterruptedException {
        final String userName = "to-remove";
        final int numberOfSessions = 5;
        final int clusterSize = 4;
        withRealmConsumer(realmId, (session, realm) -> {
            session.sessions().removeUserSessions(realm);
            session.users().addUser(realm, userName).setEmail(userName + "@localhost");
        });

        final UserSessionCount initial = getUserSessionCount();
        final CyclicBarrier barrier = new CyclicBarrier(clusterSize);
        final AtomicBoolean userDeleted = new AtomicBoolean(false);

        inIndependentFactories(clusterSize, 60, () -> {
            try {
                barrier.await(10, TimeUnit.SECONDS);
                withRealmConsumer(realmId, (session, realm) -> {
                    UserModel user = session.users().getUserByUsername(realm, userName);
                    ClientModel testApp = realm.getClientByClientId("test-app");
                    IntStream.range(0, numberOfSessions)
                            .forEach(ignored -> {
                                UserSessionModel us = session.sessions().createUserSession(null, realm, user, userName, "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                                session.sessions().createClientSession(realm, testApp, us);
                            });
                });

                barrier.await(10, TimeUnit.SECONDS);
                assertSessionCount(numberOfSessions * clusterSize, initial);

                barrier.await(10, TimeUnit.SECONDS);
                if (userDeleted.compareAndSet(false, true)) {
                    withRealmConsumer(realmId, (session, realm) -> {
                        UserModel user = session.users().getUserByUsername(realm, userName);
                        new UserManager(session).removeUser(realm, user);
                    });
                }

                barrier.await(10, TimeUnit.SECONDS);
                assertSessionCount(0, initial);

                barrier.await(10, TimeUnit.SECONDS);
            } catch (BrokenBarrierException | TimeoutException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    @Deprecated(since = "26.5", forRemoval = true)
    @Test
    public void testUserSessionRememberMeMigration() {
        Assume.assumeTrue(MultiSiteUtils.isPersistentSessionsEnabled());
        // It tests if the "remember_me" value is applied from the data to the new column.
        // To be removed when the field is removed from PersistentUserSessionData.

        List<String> userSessionIds = Arrays.stream((UserSessionModel[]) withRealm(realmId, (session, realm) -> createSessions(session, realmId)))
                .map(UserSessionModel::getId)
                .toList();

        // let ensure it has remember_me set to true
        withRealmConsumer(realmId, (session, realm) -> assertTrue(loadUserSessionDirectlyDatabase(session, userSessionIds.get(0)).isRememberMe()));

        // set the remember_me to false in the database, to simulate a migration.
        withRealmConsumer(realmId, (session, realm) -> {
            loadUserSessionDirectlyDatabase(session, userSessionIds.get(0)).setRememberMe(false);

            //no cache with remote infinispan, but we have to clear the embedded cache.
            if (InfinispanUtils.isEmbeddedInfinispan()) {
                session.getProvider(InfinispanConnectionProvider.class).getCache(USER_SESSION_CACHE_NAME).clear();
            }
        });

        // double-check if the column value is false now.
        withRealmConsumer(realmId, (session, realm) -> assertFalse(loadUserSessionDirectlyDatabase(session, userSessionIds.get(0)).isRememberMe()));

        // loading the session via UserSessionProvider should update the column
        withRealmConsumer(realmId, (session, realm) -> assertTrue(session.sessions().getUserSession(realm, userSessionIds.get(0)).isRememberMe()));

        // ensure the column is updated.
        withRealmConsumer(realmId, (session, realm) -> assertTrue(loadUserSessionDirectlyDatabase(session, userSessionIds.get(0)).isRememberMe()));
    }

    @Deprecated(since = "26.5", forRemoval = true) // to be removed when remember_me is removed from the data column
    @Test
    public void testUserSessionRememberMeMigrationWithExpiration() {
        Assume.assumeTrue(MultiSiteUtils.isPersistentSessionsEnabled());

        RealmExpiration realmExpiration = withRealm(realmId, (session, realm) -> {
            // enable remember me
            realm.setRememberMe(true);
            RealmExpiration expiration = RealmExpiration.fromRealm(realm);

            // double max-idle and lifespan for remember me
            realm.setSsoSessionIdleTimeoutRememberMe(expiration.maxIdle() * 2);
            realm.setSsoSessionMaxLifespanRememberMe(expiration.lifespan() * 2);
            return expiration;
        });

        final int initialCount = getPersistedUserSessionsCount();
        final int sessionCount = JpaUserSessionPersisterProviderFactory.DEFAULT_EXPIRATION_BATCH * 2;

        createSessions(sessionCount, value -> value % 2 == 0);
        assertEquals(initialCount + sessionCount, getPersistedUserSessionsCount());

        // se the column to null.
        withRealmConsumer(realmId, (session, realm) -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            int count = em.createQuery("UPDATE PersistentUserSessionEntity sess SET sess.rememberMe = NULL WHERE sess.realmId = :realmId")
                    .setParameter("realmId", realmId)
                    .executeUpdate();
            assertEquals(sessionCount, count);
        });

        // trigger expiration, it should perform the migration
        // because PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS, nothing should be removed but all session should be migrated and the remember me column must be updated.
        triggerExpiration(realmExpiration.maxIdle() + 10);
        assertEquals(initialCount + sessionCount, getPersistedUserSessionsCount());

        // check if everything worked as expected.
        withRealmConsumer(realmId, (session, realm) -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            long count = em.createQuery("SELECT count(*) FROM PersistentUserSessionEntity sess WHERE sess.rememberMe IS NOT NULL AND sess.realmId = :realmId", Number.class)
                    .setParameter("realmId", realmId)
                    .getSingleResult()
                    .longValue();
            assertEquals(sessionCount, count);
        });

        // lets expire regular sessions
        triggerExpiration(realmExpiration.maxIdle() + PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 10);
        assertEquals(initialCount + (sessionCount / 2), getPersistedUserSessionsCount());

        // lets expire regular sessions with remember me
        triggerExpiration((realmExpiration.maxIdle() * 2) + PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 10);
        assertEquals(initialCount, getPersistedUserSessionsCount());
    }

    @Test
    public void testUserSessionWithRememberMeRemovedAfterRememberMeDisabled() {
        Assume.assumeTrue(MultiSiteUtils.isPersistentSessionsEnabled());

        RealmExpiration realmExpiration = withRealm(realmId, (session, realm) -> {
            // enable remember me
            realm.setRememberMe(true);
            RealmExpiration expiration = RealmExpiration.fromRealm(realm);

            // double max-idle and lifespan for remember me
            realm.setSsoSessionIdleTimeoutRememberMe(expiration.maxIdle() * 2);
            realm.setSsoSessionMaxLifespanRememberMe(expiration.lifespan() * 2);
            return expiration;
        });

        final int initialCount = getPersistedUserSessionsCount();
        final int sessionCount = JpaUserSessionPersisterProviderFactory.DEFAULT_EXPIRATION_BATCH * 2;

        createSessions(sessionCount, value -> value % 2 == 0);
        assertEquals(initialCount + sessionCount, getPersistedUserSessionsCount());

        withRealmConsumer(realmId, (session, realm) -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            // se the column to null.
            int count = em.createQuery("UPDATE PersistentUserSessionEntity sess SET sess.rememberMe = NULL WHERE sess.realmId = :realmId")
                    .setParameter("realmId", realmId)
                    .executeUpdate();
            assertEquals(sessionCount, count);
            // disable remember me
            realm.setRememberMe(false);
        });

        // trigger expiration, it should perform the migration
        // realm has remember me disabled, so half of the session should be deleted by the expiration job.
        triggerExpiration(realmExpiration.maxIdle() + 10);
        assertEquals(initialCount + (sessionCount / 2), getPersistedUserSessionsCount());

        // check if everything worked as expected.
        withRealmConsumer(realmId, (session, realm) -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            long count = em.createQuery("SELECT count(*) FROM PersistentUserSessionEntity sess WHERE sess.rememberMe IS NOT NULL AND sess.realmId = :realmId", Number.class)
                    .setParameter("realmId", realmId)
                    .getSingleResult()
                    .longValue();
            assertEquals((sessionCount / 2), count);
        });

        // lets expire regular sessions
        triggerExpiration(realmExpiration.maxIdle() + PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 10);
        assertEquals(initialCount, getPersistedUserSessionsCount());

        // these sessions have remember me column not null.
        // ensure those are deleted too.
        createSessions(sessionCount, value -> value % 2 == 0);
        assertEquals(initialCount + sessionCount, getPersistedUserSessionsCount());

        // realm has remember me disabled, so half of the session should be deleted by the expiration job.
        triggerExpiration(realmExpiration.maxIdle() + 10);
        assertEquals(initialCount + (sessionCount / 2), getPersistedUserSessionsCount());

        triggerExpiration(realmExpiration.maxIdle() + PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 10);
        assertEquals(initialCount, getPersistedUserSessionsCount());
    }

    private PersistentUserSessionEntity loadUserSessionDirectlyDatabase(KeycloakSession session, String userSessionId) {
        EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
        return  em.createNamedQuery("findUserSession", PersistentUserSessionEntity.class)
                .setParameter("realmId", realmId)
                .setParameter("offline", "0")
                .setParameter("userSessionId", userSessionId)
                .setParameter("lastSessionRefresh", 0)
                .setMaxResults(1)
                .getSingleResult();
    }

    private UserSessionCount getUserSessionCount() {
        if (InfinispanUtils.isEmbeddedInfinispan()) {
            return MultiSiteUtils.isPersistentSessionsEnabled() ?
                    new UserSessionCount(getPersistedUserSessionsCount(), getEmbeddedCachedUserSessionsCount()) :
                    new UserSessionCount(-1, getEmbeddedCachedUserSessionsCount());

        }
        return MultiSiteUtils.isPersistentSessionsEnabled() ?
                new UserSessionCount(getPersistedUserSessionsCount(), -1) :
                new UserSessionCount(-1, getRemoteCachedUserSessionsCount());
    }

    private void assertSessionCount(int offset, UserSessionCount initial) {
        UserSessionCount current = getUserSessionCount();
        if (initial.database() != -1) {
            assertEquals("Wrong number of session in database", initial.database() + offset, current.database());
        } else {
            assertEquals("Wrong number of session in database", initial.database(), current.database());
        }
        if (initial.cache() != -1) {
            assertEquals("Wrong number of session in cache", initial.cache() + offset, current.cache());
        } else {
            assertEquals("Wrong number of session in cache", initial.cache(), current.cache());
        }
    }

    private int getRemoteCachedUserSessionsCount() {
        return withRealm(realmId, (session, ignored) -> session.getProvider(InfinispanConnectionProvider.class).getRemoteCache(USER_SESSION_CACHE_NAME).size());
    }

    private int getEmbeddedCachedUserSessionsCount() {
        return withRealm(realmId, (session, ignored) -> session.getProvider(InfinispanConnectionProvider.class).getCache(USER_SESSION_CACHE_NAME).size());
    }

    private int getPersistedUserSessionsCount() {
        return withRealm(realmId, (session, ignored) -> session.getProvider(UserSessionPersisterProvider.class).getUserSessionsCount(false));
    }

    @Test
    public void testSessionExpirationBatch() {
        Assume.assumeTrue(MultiSiteUtils.isPersistentSessionsEnabled());
        String userId = withRealm(realmId, (session, realm) -> {
            // enable events
            realm.setEventsEnabled(true);
            realm.setEnabledEventTypes(Set.of(EventType.USER_SESSION_DELETED.name()));
            return session.users().getUserByUsername(realm, "user1").getId();
        });
        long eventCount = getUserSessionExpirationEventCount(userId);
        // it seems some sessions are already present
        int initialSessions = getPersistedUserSessionsCount();
        // no sessions
        eventCount = doExpirationWithSessions(0, initialSessions, eventCount);

        // create half batch size sessions
        eventCount = doExpirationWithSessions(Math.max(1, JpaUserSessionPersisterProviderFactory.DEFAULT_EXPIRATION_BATCH / 2), initialSessions, eventCount);

        // exactly batch size sessions
        eventCount = doExpirationWithSessions(JpaUserSessionPersisterProviderFactory.DEFAULT_EXPIRATION_BATCH, initialSessions, eventCount);

        // double batch size sessions
        doExpirationWithSessions(JpaUserSessionPersisterProviderFactory.DEFAULT_EXPIRATION_BATCH * 2, initialSessions, eventCount);
    }

    private long doExpirationWithSessions(int count, int initialSessionCount, long currentEventCount) {
        String userId = withRealm(realmId, (session, realm) -> session.users().getUserByUsername(realm, "user1").getId());
        int offset = withRealm(realmId, (session, realm) -> realm.getSsoSessionMaxLifespan() + PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 10);
        createSessions(count, value -> false);
        assertEquals(count + initialSessionCount, getPersistedUserSessionsCount());
        triggerExpiration(offset);
        assertEquals(initialSessionCount, getPersistedUserSessionsCount());
        long eventCount = getUserSessionExpirationEventCount(userId);
        assertEquals(currentEventCount + count, eventCount);
        return eventCount;
    }

    private void createSessions(int count, IntFunction<Boolean> rememberMeFunction) {
        withRealmConsumer(realmId, (session, realm) -> {
            UserModel user = session.users().getUserByUsername(realm, "user1");
            ClientModel client = realm.getClientByClientId("test-app");
            IntStream.range(0, count)
                    .forEach(value -> {
                        var us = session.sessions().createUserSession(null, realm, user, "user1", "127.0.0." + value, "form", rememberMeFunction.apply(value), null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
                        createClientSession(session, realmId, client, us, "http://redirect", "state");
                    });
        });
    }

    private void triggerExpiration(int offset) {
        withRealmConsumer(realmId, (session, realm) -> {
            Time.setOffset(offset);
            try {
                session.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);
            } finally {
                Time.setOffset(0);
            }
        });
    }

    private long getUserSessionExpirationEventCount(String userId) {
        return withRealm(realmId, (session, ignored) -> {
            EventStoreProvider eventStore = session.getProvider(EventStoreProvider.class);
            return eventStore.createQuery()
                    .realm(realmId)
                    .user(userId)
                    .type(EventType.USER_SESSION_DELETED)
                    .getResultStream()
                    .count();
        });
    }

    private long countUserSessionsInRealm(KeycloakSession session) {
        JpaUserSessionPersisterProvider sessionPersisterProvider = (JpaUserSessionPersisterProvider) session.getProvider(UserSessionPersisterProvider.class);
        RealmModel realm = session.realms().getRealm(realmId);
        return sessionPersisterProvider.getUserSessionsCountsByClients(realm, false).keySet().stream()
                .flatMap(s -> sessionPersisterProvider.loadUserSessionsStream(realm, session.clients().getClientById(realm, s), false, 0, -1))
                .distinct().count();
    }

    private void setupClientStorageComponents(KeycloakSession s, RealmModel realm) {
        s.getContext().setRealm(realm);
        getParameters(ClientStorageProviderModel.class).forEach(cm -> {
            cm.put(HardcodedClientStorageProviderFactory.CLIENT_ID, "external-storage-client");
            cm.put(HardcodedClientStorageProviderFactory.DELAYED_SEARCH, Boolean.toString(false));
            realm.addComponentModel(cm);
        });

        // Required by HardcodedClientStorageProvider
        s.roles().addRealmRole(realm, OAuth2Constants.OFFLINE_ACCESS);
        s.clientScopes().addClientScope(realm, OAuth2Constants.OFFLINE_ACCESS);
        s.clientScopes().addClientScope(realm, OIDCLoginProtocolFactory.ROLES_SCOPE);
        s.clientScopes().addClientScope(realm, OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE);
    }

    private void cleanClientStorageComponents(KeycloakSession s, RealmModel realm) {
        s.getContext().setRealm(realm);
        s.roles().removeRoles(realm);
        s.clientScopes().removeClientScopes(realm);

        realm.removeComponents(realm.getId());
    }

    protected static AuthenticatedClientSessionModel createClientSession(KeycloakSession session, String realmId, ClientModel client, UserSessionModel userSession, String redirect, String state) {
        RealmModel realm = session.realms().getRealm(realmId);
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        return clientSession;
    }

    protected static UserSessionModel[] createSessions(KeycloakSession session, String realmId) {
       return createSessions(session, realmId, true);
    }

    protected static UserSessionModel[] createSessions(KeycloakSession session, String realmId, boolean rememberMe) {
        RealmModel realm = session.realms().getRealm(realmId);
        session.getContext().setRealm(realm);
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", rememberMe, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);

        createClientSession(session, realmId, realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state");
        createClientSession(session, realmId, realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state");

        sessions[1] = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.2", "form", rememberMe, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
        createClientSession(session, realmId, realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state");

        sessions[2] = session.sessions().createUserSession(null, realm, session.users().getUserByUsername(realm, "user2"), "user2", "127.0.0.3", "form", rememberMe, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
        createClientSession(session, realmId, realm.getClientByClientId("test-app"), sessions[2], "http://redirect", "state");

        return sessions;
    }

    private void persistUserSession(KeycloakSession session, UserSessionModel userSession, boolean offline) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        persister.createUserSession(userSession, offline);
        for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
            persister.createClientSession(clientSession, offline);
        }
    }

    public static void assertSessionLoaded(List<UserSessionModel> sessions, String id, UserModel user, String ipAddress, int started, int lastRefresh, String... clients) {
        for (UserSessionModel session : sessions) {
            if (session.getId().equals(id)) {
                assertSession(session, user, ipAddress, started, lastRefresh, clients);
                return;
            }
        }
        Assert.fail("Session with ID " + id + " not found in the list");
    }

    private List<UserSessionModel> loadPersistedSessionsPaginated(KeycloakSession session, boolean offline, int sessionsPerPage, int expectedPageCount, int expectedSessionsCount) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

        int pageCount = 0;
        boolean next = true;
        List<UserSessionModel> result = new ArrayList<>();
        String lastSessionId = "";

        while (next) {
            List<UserSessionModel> sess = persister
                    .loadUserSessionsStream(0, sessionsPerPage, offline, lastSessionId)
                    .toList();

            if (sess.size() < sessionsPerPage) {
                next = false;

                // We had at least some session
                if (!sess.isEmpty()) {
                    pageCount++;
                }
            } else {
                pageCount++;

                UserSessionModel lastSession = sess.get(sess.size() - 1);
                lastSessionId = lastSession.getId();
            }

            result.addAll(sess);
        }

        Assert.assertEquals(expectedPageCount, pageCount);
        Assert.assertEquals(expectedSessionsCount, result.size());
        return result;
    }

    public static void assertSession(UserSessionModel session, UserModel user, String ipAddress, int started, int lastRefresh, String... clients) {
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
            Assert.assertEquals(clientUUID, clientSession.getClient().getId());
            actualClients[i] = clientSession.getClient().getClientId();
            i++;
        }

        assertThat(actualClients, Matchers.arrayContainingInAnyOrder(clients));
    }

    public static void assertSessions(List<UserSessionModel> actualSessions, String[] expectedSessionIds) {
        String[] actual = new String[actualSessions.size()];
        for (int i = 0; i < actual.length; i++) {
            actual[i] = actualSessions.get(i).getId();
        }

        assertThat(actual, Matchers.arrayContainingInAnyOrder(expectedSessionIds));
    }

    private record UserSessionCount(int database, int cache) {}
}
