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

package org.keycloak.testsuite.model;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.models.UserManager;
import org.keycloak.testsuite.rule.KeycloakRule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserSessionPersisterProviderTest {


    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    private KeycloakSession session;
    private RealmModel realm;
    private UserSessionPersisterProvider persister;

    @Before
    public void before() {
        session = kc.startSession();
        realm = session.realms().getRealm("test");
        session.users().addUser(realm, "user1").setEmail("user1@localhost");
        session.users().addUser(realm, "user2").setEmail("user2@localhost");
        persister = session.getProvider(UserSessionPersisterProvider.class);
    }

    @After
    public void after() {
        resetSession();
        session.sessions().removeUserSessions(realm);
        UserModel user1 = session.users().getUserByUsername("user1", realm);
        UserModel user2 = session.users().getUserByUsername("user2", realm);

        UserManager um = new UserManager(session);
        if (user1 != null) {
            um.removeUser(realm, user1);
        }
        if (user2 != null) {
            um.removeUser(realm, user2);
        }
        kc.stopSession(session, true);
    }

    @Test
    public void testPersistenceWithLoad() {
        // Create some sessions in infinispan
        int started = Time.currentTime();
        UserSessionModel[] origSessions = createSessions();

        resetSession();

        // Persist 3 created userSessions and clientSessions as offline
        ClientModel testApp = realm.getClientByClientId("test-app");
        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, testApp);
        for (UserSessionModel userSession : userSessions) {
            persistUserSession(userSession, true);
        }

        // Persist 1 online session
        UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[0].getId());
        persistUserSession(userSession, false);

        resetSession();

        // Assert online session
        List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(false, 1, 1, 1);
        UserSessionProviderTest.assertSession(loadedSessions.get(0), session.users().getUserByUsername("user1", realm), "127.0.0.1", started, started, "test-app", "third-party");

        // Assert offline sessions
        loadedSessions = loadPersistedSessionsPaginated(true, 2, 2, 3);
        UserSessionProviderTest.assertSessions(loadedSessions, origSessions);

        assertSessionLoaded(loadedSessions, origSessions[0].getId(), session.users().getUserByUsername("user1", realm), "127.0.0.1", started, started, "test-app", "third-party");
        assertSessionLoaded(loadedSessions, origSessions[1].getId(), session.users().getUserByUsername("user1", realm), "127.0.0.2", started, started, "test-app");
        assertSessionLoaded(loadedSessions, origSessions[2].getId(), session.users().getUserByUsername("user2", realm), "127.0.0.3", started, started, "test-app");
    }


    @Test
    public void testUpdateAndRemove() {
        // Create some sessions in infinispan
        int started = Time.currentTime();
        UserSessionModel[] origSessions = createSessions();

        resetSession();

        // Persist 1 offline session
        UserSessionModel userSession = session.sessions().getUserSession(realm, origSessions[1].getId());
        persistUserSession(userSession, true);

        resetSession();

        // Load offline session
        List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(true, 10, 1, 1);
        UserSessionModel persistedSession = loadedSessions.get(0);
        UserSessionProviderTest.assertSession(persistedSession, session.users().getUserByUsername("user1", realm), "127.0.0.2", started, started, "test-app");

        // create new clientSession
        AuthenticatedClientSessionModel clientSession = createClientSession(realm.getClientByClientId("third-party"), session.sessions().getUserSession(realm, persistedSession.getId()),
                "http://redirect", "state");
        persister.createClientSession(clientSession, true);

        resetSession();

        // Remove clientSession
        persister.removeClientSession(userSession.getId(), realm.getClientByClientId("third-party").getId(), true);

        resetSession();

        // Assert clientSession removed
        loadedSessions = loadPersistedSessionsPaginated(true, 10, 1, 1);
        persistedSession = loadedSessions.get(0);
        UserSessionProviderTest.assertSession(persistedSession, session.users().getUserByUsername("user1", realm), "127.0.0.2", started, started , "test-app");

        // Remove userSession
        persister.removeUserSession(persistedSession.getId(), true);

        resetSession();

        // Assert nothing found
        loadPersistedSessionsPaginated(true, 10, 0, 0);
    }

    @Test
    public void testOnRealmRemoved() {
        RealmModel fooRealm = session.realms().createRealm("foo", "foo");
        fooRealm.addClient("foo-app");
        session.users().addUser(fooRealm, "user3");

        UserSessionModel userSession = session.sessions().createUserSession(fooRealm, session.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
        createClientSession(fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");

        resetSession();

        // Persist offline session
        fooRealm = session.realms().getRealm("foo");
        userSession = session.sessions().getUserSession(fooRealm, userSession.getId());
        persistUserSession(userSession, true);

        resetSession();

        // Assert session was persisted
        loadPersistedSessionsPaginated(true, 10, 1, 1);

        // Remove realm
        RealmManager realmMgr = new RealmManager(session);
        realmMgr.removeRealm(realmMgr.getRealm("foo"));

        resetSession();

        // Assert nothing loaded
        loadPersistedSessionsPaginated(true, 10, 0, 0);
    }

    @Test
    public void testOnClientRemoved() {
        int started = Time.currentTime();

        RealmModel fooRealm = session.realms().createRealm("foo", "foo");
        fooRealm.addClient("foo-app");
        fooRealm.addClient("bar-app");
        session.users().addUser(fooRealm, "user3");

        UserSessionModel userSession = session.sessions().createUserSession(fooRealm, session.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
        createClientSession(fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
        createClientSession(fooRealm.getClientByClientId("bar-app"), userSession, "http://redirect", "state");

        resetSession();

        // Persist offline session
        fooRealm = session.realms().getRealm("foo");
        userSession = session.sessions().getUserSession(fooRealm, userSession.getId());
        persistUserSession(userSession, true);

        resetSession();

        RealmManager realmMgr = new RealmManager(session);
        ClientManager clientMgr = new ClientManager(realmMgr);
        fooRealm = realmMgr.getRealm("foo");

        // Assert session was persisted with both clientSessions
        UserSessionModel persistedSession = loadPersistedSessionsPaginated(true, 10, 1, 1).get(0);
        UserSessionProviderTest.assertSession(persistedSession, session.users().getUserByUsername("user3", fooRealm), "127.0.0.1", started, started, "foo-app", "bar-app");

        // Remove foo-app client
        ClientModel client = fooRealm.getClientByClientId("foo-app");
        clientMgr.removeClient(fooRealm, client);

        resetSession();

        realmMgr = new RealmManager(session);
        clientMgr = new ClientManager(realmMgr);
        fooRealm = realmMgr.getRealm("foo");

        // Assert just one bar-app clientSession persisted now
        persistedSession = loadPersistedSessionsPaginated(true, 10, 1, 1).get(0);
        UserSessionProviderTest.assertSession(persistedSession, session.users().getUserByUsername("user3", fooRealm), "127.0.0.1", started, started, "bar-app");

        // Remove bar-app client
        client = fooRealm.getClientByClientId("bar-app");
        clientMgr.removeClient(fooRealm, client);

        resetSession();

        // Assert loading still works - last userSession is still there, but no clientSession on it
        loadPersistedSessionsPaginated(true, 10, 1, 1);

        // Cleanup
        realmMgr = new RealmManager(session);
        realmMgr.removeRealm(realmMgr.getRealm("foo"));
    }

    @Test
    public void testOnUserRemoved() {
        // Create some sessions in infinispan
        int started = Time.currentTime();
        UserSessionModel[] origSessions = createSessions();

        resetSession();

        // Persist 2 offline sessions of 2 users
        UserSessionModel userSession1 = session.sessions().getUserSession(realm, origSessions[1].getId());
        UserSessionModel userSession2 = session.sessions().getUserSession(realm, origSessions[2].getId());
        persistUserSession(userSession1, true);
        persistUserSession(userSession2, true);

        resetSession();

        // Load offline sessions
        List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(true, 10, 1, 2);

        // Properly delete user and assert his offlineSession removed
        UserModel user1 = session.users().getUserByUsername("user1", realm);
        new UserManager(session).removeUser(realm, user1);

        resetSession();

        Assert.assertEquals(1, persister.getUserSessionsCount(true));
        loadedSessions = loadPersistedSessionsPaginated(true, 10, 1, 1);
        UserSessionModel persistedSession = loadedSessions.get(0);
        UserSessionProviderTest.assertSession(persistedSession, session.users().getUserByUsername("user2", realm), "127.0.0.3", started, started, "test-app");

        // KEYCLOAK-2431 Assert that userSessionPersister is resistent even to situation, when users are deleted "directly".
        // No exception will happen. However session will be still there
        UserModel user2 = session.users().getUserByUsername("user2", realm);
        session.users().removeUser(realm, user2);

        loadedSessions = loadPersistedSessionsPaginated(true, 10, 1, 1);

        // Cleanup
        UserSessionModel userSession = loadedSessions.get(0);
        session.sessions().removeUserSession(realm, userSession);
        persister.removeUserSession(userSession.getId(), userSession.isOffline());
    }

    // KEYCLOAK-1999
    @Test
    public void testNoSessions() {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        List<UserSessionModel> sessions = persister.loadUserSessions(0, 1, true, 0, "abc");
        Assert.assertEquals(0, sessions.size());
    }


    @Test
    public void testMoreSessions() {
        // Create 10 userSessions - each having 1 clientSession
        List<UserSessionModel> userSessions = new ArrayList<>();
        UserModel user = session.users().getUserByUsername("user1", realm);

        for (int i=0 ; i<20 ; i++) {
            // Having different offsets for each session (to ensure that lastSessionRefresh is also different)
            Time.setOffset(i);

            UserSessionModel userSession = session.sessions().createUserSession(realm, user, "user1", "127.0.0.1", "form", true, null, null);
            createClientSession(realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
            userSessions.add(userSession);
        }

        resetSession();

        for (UserSessionModel userSession : userSessions) {
            UserSessionModel userSession2 = session.sessions().getUserSession(realm, userSession.getId());
            persistUserSession(userSession2, true);
        }

        resetSession();

        List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(true, 2, 10, 20);
        user = session.users().getUserByUsername("user1", realm);
        ClientModel testApp = realm.getClientByClientId("test-app");

        for (UserSessionModel loadedSession : loadedSessions) {
            assertEquals(user.getId(), loadedSession.getUser().getId());
            assertEquals("127.0.0.1", loadedSession.getIpAddress());
            assertEquals(user.getUsername(), loadedSession.getLoginUsername());

            assertEquals(1, loadedSession.getAuthenticatedClientSessions().size());
            assertTrue(loadedSession.getAuthenticatedClientSessions().containsKey(testApp.getId()));
        }
    }


    @Test
    public void testExpiredSessions() {
        // Create some sessions in infinispan
        int started = Time.currentTime();
        UserSessionModel[] origSessions = createSessions();

        resetSession();

        // Persist 2 offline sessions of 2 users
        UserSessionModel userSession1 = session.sessions().getUserSession(realm, origSessions[1].getId());
        UserSessionModel userSession2 = session.sessions().getUserSession(realm, origSessions[2].getId());
        persistUserSession(userSession1, true);
        persistUserSession(userSession2, true);

        resetSession();

        // Update one of the sessions with lastSessionRefresh of 20 days ahead
        int lastSessionRefresh = Time.currentTime() + 1728000;
        persister.updateLastSessionRefreshes(realm, lastSessionRefresh, Collections.singleton(userSession1.getId()), true);

        resetSession();

        // Increase time offset - 40 days
        Time.setOffset(3456000);
        try {
            // Run expiration thread
            persister.removeExpired(realm);

            // Test the updated session is still in persister. Not updated session is not there anymore
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(true, 10, 1, 1);
            UserSessionModel persistedSession = loadedSessions.get(0);
            UserSessionProviderTest.assertSession(persistedSession, session.users().getUserByUsername("user1", realm), "127.0.0.2", started, lastSessionRefresh, "test-app");

        } finally {
            // Cleanup
            Time.setOffset(0);
        }

    }


    private AuthenticatedClientSessionModel createClientSession(ClientModel client, UserSessionModel userSession, String redirect, String state) {
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        return clientSession;
    }

    private UserSessionModel[] createSessions() {
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null);

        createClientSession(realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state");
        createClientSession(realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state");

        sessions[1] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true, null, null);
        createClientSession(realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state");

        sessions[2] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.3", "form", true, null, null);
        createClientSession(realm.getClientByClientId("test-app"), sessions[2], "http://redirect", "state");

        return sessions;
    }

    private void persistUserSession(UserSessionModel userSession, boolean offline) {
        persister.createUserSession(userSession, offline);
        for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
            persister.createClientSession(clientSession, offline);
        }
    }

    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        realm = session.realms().getRealm("test");
        persister = session.getProvider(UserSessionPersisterProvider.class);
    }

    public static void assertSessionLoaded(List<UserSessionModel> sessions, String id, UserModel user, String ipAddress, int started, int lastRefresh, String... clients) {
        for (UserSessionModel session : sessions) {
            if (session.getId().equals(id)) {
                UserSessionProviderTest.assertSession(session, user, ipAddress, started, lastRefresh, clients);
                return;
            }
        }
        Assert.fail("Session with ID " + id + " not found in the list");
    }

    private List<UserSessionModel> loadPersistedSessionsPaginated(boolean offline, int sessionsPerPage, int expectedPageCount, int expectedSessionsCount) {
        int count = persister.getUserSessionsCount(offline);


        int pageCount = 0;
        boolean next = true;
        List<UserSessionModel> result = new ArrayList<>();
        int lastCreatedOn = 0;
        String lastSessionId = "abc";

        while (next) {
            List<UserSessionModel> sess = persister.loadUserSessions(0, sessionsPerPage, offline, lastCreatedOn, lastSessionId);

            if (sess.size() < sessionsPerPage) {
                next = false;

                // We had at least some session
                if (sess.size() > 0) {
                    pageCount++;
                }
            } else {
                pageCount++;

                UserSessionModel lastSession = sess.get(sess.size() - 1);
                lastCreatedOn = lastSession.getStarted();
                lastSessionId = lastSession.getId();
            }

            result.addAll(sess);
        }

        Assert.assertEquals(pageCount, expectedPageCount);
        Assert.assertEquals(result.size(), expectedSessionsCount);
        return result;
    }
}
