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

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RealmProvider;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import org.keycloak.models.Constants;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.hamcrest.Matchers;
import org.keycloak.testsuite.model.KeycloakModelTest;
import org.keycloak.testsuite.model.RequireProvider;
import java.util.LinkedList;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
@RequireProvider(UserSessionPersisterProvider.class)
@RequireProvider(value = UserSessionProvider.class, only = InfinispanUserSessionProviderFactory.PROVIDER_ID)
@RequireProvider(UserProvider.class)
@RequireProvider(RealmProvider.class)
public class UserSessionPersisterProviderTest extends KeycloakModelTest {

    private static final int USER_SESSION_COUNT = 2000;
    private String realmId;

    @Override
    public void createEnvironment(KeycloakSession s) {
        RealmModel realm = s.realms().createRealm("test");
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
            ClientModel testApp = realm.getClientByClientId("test-app");
            session.sessions().getUserSessionsStream(realm, testApp).collect(Collectors.toList())
                    .forEach(userSessionLooper -> persistUserSession(session, userSessionLooper, true));
        });

        inComittedTransaction(session -> {
            // Persist 1 online session
            RealmModel realm = session.realms().getRealm(realmId);
            userSession[0] = session.sessions().getUserSession(realm, origSessions[0].getId());
            persistUserSession(session, userSession[0], false);
        });

        inComittedTransaction(session -> { // Assert online session
            RealmModel realm = session.realms().getRealm(realmId);
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, false, 1, 1, 1);
            assertSession(loadedSessions.get(0), session.users().getUserByUsername(realm, "user1"), "127.0.0.1", started, started, "test-app", "third-party");
        });

        inComittedTransaction(session -> {
            // Assert offline sessions
            RealmModel realm = session.realms().getRealm(realmId);
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
        AtomicReference<List<UserSessionModel>> loadedSessionsAt = new AtomicReference<>();

        AtomicReference<UserSessionModel> userSessionAt = new AtomicReference<>();
        AtomicReference<UserSessionModel> persistedSessionAt = new AtomicReference<>();

        inComittedTransaction(session -> {
            // Create some sessions in infinispan
            UserSessionModel[] origSessions = createSessions(session, realmId);
            origSessionsAt.set(origSessions);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);
            UserSessionModel[] origSessions = origSessionsAt.get();

            // Persist 1 offline session
            UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
            userSessionAt.set(userSession);

            persistUserSession(session, userSession, true);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

            // Load offline session
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, true, 10, 1, 1);
            loadedSessionsAt.set(loadedSessions);

            UserSessionModel persistedSession = loadedSessions.get(0);
            persistedSessionAt.set(persistedSession);

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
            RealmModel fooRealm = session.realms().createRealm("foo", "foo");
            fooRealm.setDefaultRole(session.roles().addRealmRole(fooRealm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + fooRealm.getName()));

            fooRealm.addClient("foo-app");
            session.users().addUser(fooRealm, "user3");

            UserSessionModel userSession = session.sessions().createUserSession(fooRealm, session.users().getUserByUsername(fooRealm, "user3"), "user3", "127.0.0.1", "form", true, null, null);
            userSessionID.set(userSession.getId());

            createClientSession(session, realmId, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
        });

        inComittedTransaction(session -> {
            // Persist offline session
            RealmModel fooRealm = session.realms().getRealm("foo");
            UserSessionModel userSession = session.sessions().getUserSession(fooRealm, userSessionID.get());
            persistUserSession(session, userSession, true);
        });

        inComittedTransaction(session -> {
            // Assert session was persisted
            loadPersistedSessionsPaginated(session, true, 10, 1, 1);

            // Remove realm
            RealmManager realmMgr = new RealmManager(session);
            realmMgr.removeRealm(realmMgr.getRealm("foo"));
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
            RealmModel fooRealm = session.realms().createRealm("foo", "foo");
            fooRealm.setDefaultRole(session.roles().addRealmRole(fooRealm, Constants.DEFAULT_ROLES_ROLE_PREFIX));

            fooRealm.addClient("foo-app");
            fooRealm.addClient("bar-app");
            session.users().addUser(fooRealm, "user3");

            UserSessionModel userSession = session.sessions().createUserSession(fooRealm, session.users().getUserByUsername(fooRealm, "user3"), "user3", "127.0.0.1", "form", true, null, null);
            userSessionID.set(userSession.getId());

            createClientSession(session, realmId, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
            createClientSession(session, realmId, fooRealm.getClientByClientId("bar-app"), userSession, "http://redirect", "state");
        });

        inComittedTransaction(session -> {
            RealmModel fooRealm = session.realms().getRealm("foo");

            // Persist offline session
            UserSessionModel userSession = session.sessions().getUserSession(fooRealm, userSessionID.get());
            persistUserSession(session, userSession, true);
        });

        inComittedTransaction(session -> {
            RealmManager realmMgr = new RealmManager(session);
            ClientManager clientMgr = new ClientManager(realmMgr);
            RealmModel fooRealm = realmMgr.getRealm("foo");

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
            RealmModel fooRealm = realmMgr.getRealm("foo");

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
            realmMgr.removeRealm(realmMgr.getRealm("foo"));
        });
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

            UserSessionModel[] origSessions = origSessionsAt.get();

            // Persist 2 offline sessions of 2 users
            UserSessionModel userSession1 = session.sessions().getUserSession(realm, origSessions[1].getId());
            UserSessionModel userSession2 = session.sessions().getUserSession(realm, origSessions[2].getId());
            persistUserSession(session, userSession1, true);
            persistUserSession(session, userSession2, true);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            // Load offline sessions
            loadPersistedSessionsPaginated(session, true, 10, 1, 2);

            // Properly delete user and assert his offlineSession removed
            UserModel user1 = session.users().getUserByUsername(realm, "user1");
            new UserManager(session).removeUser(realm, user1);
        });

        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
            Assert.assertEquals(1, persister.getUserSessionsCount(true));

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
            Stream<UserSessionModel> sessions = persister.loadUserSessionsStream(0, 1, true, "00000000-0000-0000-0000-000000000000");
            Assert.assertEquals(0, sessions.count());
        });
    }

    @Test
    public void testMoreSessions() {
        inComittedTransaction(session -> {
            RealmModel realm = session.realms().getRealm(realmId);

            // Create 10 userSessions - each having 1 clientSession
            List<String> userSessionsInner = new LinkedList<>();
            UserModel user = session.users().getUserByUsername(realm, "user1");

            for (int i = 0; i < USER_SESSION_COUNT; i++) {
                // Having different offsets for each session (to ensure that lastSessionRefresh is also different)
                Time.setOffset(i);

                UserSessionModel userSession = session.sessions().createUserSession(realm, user, "user1", "127.0.0.1", "form", true, null, null);
                createClientSession(session, realmId, realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
                userSessionsInner.add(userSession.getId());
            }

            for (String userSessionId : userSessionsInner) {
                UserSessionModel userSession2 = session.sessions().getUserSession(realm, userSessionId);
                persistUserSession(session, userSession2, true);
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
            userSession1[0] = session.sessions().getUserSession(realm, origSessions[1].getId());
            userSession2[0] = session.sessions().getUserSession(realm, origSessions[2].getId());
            persistUserSession(session, userSession1[0], true);
            persistUserSession(session, userSession2[0], true);
        });

        inComittedTransaction(session -> {
            // Update one of the sessions with lastSessionRefresh of 20 days ahead
            int lastSessionRefresh = Time.currentTime() + 1728000;
            RealmModel realm = session.realms().getRealm(realmId);
            UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

            persister.updateLastSessionRefreshes(realm, lastSessionRefresh, Collections.singleton(userSession1[0].getId()), true);

            // Increase time offset - 40 days
            Time.setOffset(3456000);
            try {
                // Run expiration thread
                persister.removeExpired(realm);

                // Test the updated session is still in persister. Not updated session is not there anymore
                List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(session, true, 10, 1, 1);
                UserSessionModel persistedSession = loadedSessions.get(0);
                assertSession(persistedSession, session.users().getUserByUsername(realm, "user1"), "127.0.0.2", started, lastSessionRefresh, "test-app");

            } finally {
                // Cleanup
                Time.setOffset(0);
                session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            }
        });
    }

    protected static AuthenticatedClientSessionModel createClientSession(KeycloakSession session, String realmId, ClientModel client, UserSessionModel userSession, String redirect, String state) {
        RealmModel realm = session.realms().getRealm(realmId);
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        return clientSession;
    }

    protected static UserSessionModel[] createSessions(KeycloakSession session, String realmId) {
        RealmModel realm = session.realms().getRealm(realmId);
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null);

        createClientSession(session, realmId, realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state");
        createClientSession(session, realmId, realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state");

        sessions[1] = session.sessions().createUserSession(realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.2", "form", true, null, null);
        createClientSession(session, realmId, realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state");

        sessions[2] = session.sessions().createUserSession(realm, session.users().getUserByUsername(realm, "user2"), "user2", "127.0.0.3", "form", true, null, null);
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

        int count = persister.getUserSessionsCount(offline);

        int pageCount = 0;
        boolean next = true;
        List<UserSessionModel> result = new ArrayList<>();
        String lastSessionId = "00000000-0000-0000-0000-000000000000";

        while (next) {
            List<UserSessionModel> sess = persister
                    .loadUserSessionsStream(0, sessionsPerPage, offline, lastSessionId)
                    .collect(Collectors.toList());

            if (sess.size() < sessionsPerPage) {
                next = false;

                // We had at least some session
                if (sess.size() > 0) {
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
}
