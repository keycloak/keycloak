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

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.TargetsContainer;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.*;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.*;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.federation.ldap.AbstractLDAPTest;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserSessionPersisterProviderTest extends AbstractTestRealmKeycloakTest {

    @Deployment
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class, UserSessionPersisterProvider.class)
                .addPackages(true,
                        "org.keycloak.testsuite",
                        "org.keycloak.testsuite.model");
    }

    //private static KeycloakSession session;
    //private static RealmModel realm;
    //private static UserSessionPersisterProvider persister;

    @Before
    public void before() {
        testingClient.server().run(session -> {
            initStuff(session);
        });
    }

    public static void initStuff(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm("test");
        session.users().addUser(realm, "user1").setEmail("user1@localhost");
        session.users().addUser(realm, "user2").setEmail("user2@localhost");
    }

    @After
    public void after() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealm("test");
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
        });
    }

    @Test
    @ModelTest
    public void testPersistenceWithLoad(KeycloakSession session) {
        UserSessionModel[][] origSessions = new UserSessionModel[1][1];
        int started = Time.currentTime();

        final UserSessionModel[] userSession = new UserSessionModel[1];
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionWL) -> {
                    // Create some sessions in infinispan

                    RealmModel realm = sessionWL.realms().getRealm("test");
                    origSessions[0] = createSessions(sessionWL);
                });
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionWL22) -> {
            // Persist 3 created userSessions and clientSessions as offline
            RealmModel realm = sessionWL22.realms().getRealm("test");
            ClientModel testApp = realm.getClientByClientId("test-app");
            List<UserSessionModel> userSessions = sessionWL22.sessions().getUserSessions(realm, testApp);
            for (UserSessionModel userSessionLooper : userSessions) {
                persistUserSession(sessionWL22, userSessionLooper, true);
            }
        });
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionWL2) -> {
            // Persist 1 online session
            RealmModel realm = sessionWL2.realms().getRealm("test");
            userSession[0] = sessionWL2.sessions().getUserSession(realm, origSessions[0][0].getId());
            persistUserSession(sessionWL2, userSession[0], false);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionWL3) ->
        {// Assert online session
            RealmModel realm = sessionWL3.realms().getRealm("test");
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(sessionWL3, false, 1, 1, 1);
            UserSessionProviderTest.assertSession(loadedSessions.get(0), sessionWL3.users().getUserByUsername("user1", realm), "127.0.0.1", started, started, "test-app", "third-party");

        });
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionWL4) ->
        {
            // Assert offline sessions
            RealmModel realm = sessionWL4.realms().getRealm("test");
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(sessionWL4, true, 2, 2, 3);
            UserSessionProviderTest.assertSessions(loadedSessions, origSessions[0]);


            assertSessionLoaded(loadedSessions, origSessions[0][0].getId(), sessionWL4.users().getUserByUsername("user1", realm), "127.0.0.1", started, started, "test-app", "third-party");
            assertSessionLoaded(loadedSessions, origSessions[0][1].getId(), sessionWL4.users().getUserByUsername("user1", realm), "127.0.0.2", started, started, "test-app");
            assertSessionLoaded(loadedSessions, origSessions[0][2].getId(), sessionWL4.users().getUserByUsername("user2", realm), "127.0.0.3", started, started, "test-app");
        });
    }


    @Test
    @ModelTest
    public void testUpdateAndRemove(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionUR) -> {
            // Create some sessions in infinispan
            int started = Time.currentTime();
            RealmModel realm = sessionUR.realms().getRealm("test");
            UserSessionPersisterProvider persister = sessionUR.getProvider(UserSessionPersisterProvider.class);
            persister = sessionUR.getProvider(UserSessionPersisterProvider.class);
            UserSessionModel[] origSessions = createSessions(sessionUR);

            resetSession();

            // Persist 1 offline session
            UserSessionModel userSession = sessionUR.sessions().getUserSession(realm, origSessions[1].getId());
            persistUserSession(sessionUR, userSession, true);

            resetSession();

            // Load offline session
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(sessionUR, true, 10, 1, 1);
            UserSessionModel persistedSession = loadedSessions.get(0);
            UserSessionProviderTest.assertSession(persistedSession, sessionUR.users().getUserByUsername("user1", realm), "127.0.0.2", started, started, "test-app");

            // create new clientSession
            AuthenticatedClientSessionModel clientSession = createClientSession(sessionUR, realm.getClientByClientId("third-party"), sessionUR.sessions().getUserSession(realm, persistedSession.getId()),
                    "http://redirect", "state");
            persister.createClientSession(clientSession, true);


            // Remove clientSession
            persister.removeClientSession(userSession.getId(), realm.getClientByClientId("third-party").getId(), true);


            // Assert clientSession removed
            loadedSessions = loadPersistedSessionsPaginated(sessionUR, true, 10, 1, 1);
            persistedSession = loadedSessions.get(0);
            UserSessionProviderTest.assertSession(persistedSession, sessionUR.users().getUserByUsername("user1", realm), "127.0.0.2", started, started, "test-app");

            // Remove userSession
            persister.removeUserSession(persistedSession.getId(), true);

            // Assert nothing found
            loadPersistedSessionsPaginated(sessionUR, true, 10, 0, 0);
        });
    }

    @Test
    @ModelTest
    public void testOnRealmRemoved(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRR) -> {
            RealmModel fooRealm = sessionRR.realms().createRealm("foo", "foo");
            fooRealm.addClient("foo-app");
            sessionRR.users().addUser(fooRealm, "user3");

            UserSessionModel userSession = sessionRR.sessions().createUserSession(fooRealm, sessionRR.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
            createClientSession(sessionRR, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");

            // Persist offline session
            fooRealm = sessionRR.realms().getRealm("foo");
            userSession = sessionRR.sessions().getUserSession(fooRealm, userSession.getId());
            persistUserSession(sessionRR, userSession, true);


            // Assert session was persisted
            loadPersistedSessionsPaginated(sessionRR, true, 10, 1, 1);

            // Remove realm
            RealmManager realmMgr = new RealmManager(sessionRR);
            realmMgr.removeRealm(realmMgr.getRealm("foo"));

            // Assert nothing loaded
            loadPersistedSessionsPaginated(sessionRR, true, 10, 0, 0);
        });
    }

    @Test
    @ModelTest
    public void testOnClientRemoved(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCR) -> {
            int started = Time.currentTime();

            RealmModel fooRealm = sessionCR.realms().createRealm("foo", "foo");
            fooRealm.addClient("foo-app");
            fooRealm.addClient("bar-app");
            sessionCR.users().addUser(fooRealm, "user3");

            UserSessionModel userSession = sessionCR.sessions().createUserSession(fooRealm, sessionCR.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
            createClientSession(sessionCR, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
            createClientSession(sessionCR, fooRealm.getClientByClientId("bar-app"), userSession, "http://redirect", "state");

            resetSession();

            // Persist offline session
            fooRealm = sessionCR.realms().getRealm("foo");
            userSession = sessionCR.sessions().getUserSession(fooRealm, userSession.getId());
            persistUserSession(sessionCR, userSession, true);

            resetSession();

            RealmManager realmMgr = new RealmManager(sessionCR);
            ClientManager clientMgr = new ClientManager(realmMgr);
            fooRealm = realmMgr.getRealm("foo");

            // Assert session was persisted with both clientSessions
            UserSessionModel persistedSession = loadPersistedSessionsPaginated(sessionCR, true, 10, 1, 1).get(0);
            UserSessionProviderTest.assertSession(persistedSession, sessionCR.users().getUserByUsername("user3", fooRealm), "127.0.0.1", started, started, "foo-app", "bar-app");

            // Remove foo-app client
            ClientModel client = fooRealm.getClientByClientId("foo-app");
            clientMgr.removeClient(fooRealm, client);

            resetSession();

            realmMgr = new RealmManager(sessionCR);
            clientMgr = new ClientManager(realmMgr);
            fooRealm = realmMgr.getRealm("foo");

            // Assert just one bar-app clientSession persisted now
            persistedSession = loadPersistedSessionsPaginated(sessionCR, true, 10, 1, 1).get(0);
            UserSessionProviderTest.assertSession(persistedSession, sessionCR.users().getUserByUsername("user3", fooRealm), "127.0.0.1", started, started, "bar-app");

            // Remove bar-app client
            client = fooRealm.getClientByClientId("bar-app");
            clientMgr.removeClient(fooRealm, client);

            resetSession();

            // Assert loading still works - last userSession is still there, but no clientSession on it
            loadPersistedSessionsPaginated(sessionCR, true, 10, 1, 1);

            // Cleanup
            realmMgr = new RealmManager(sessionCR);
            realmMgr.removeRealm(realmMgr.getRealm("foo"));
        });
    }

    @Test
    @ModelTest
    public void testOnUserRemoved(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionOR) -> {
            // Create some sessions in infinispan
            int started = Time.currentTime();
            RealmModel realm = sessionOR.realms().getRealm("test");
            UserSessionPersisterProvider persister = sessionOR.getProvider(UserSessionPersisterProvider.class);
            UserSessionModel[] origSessions = createSessions(sessionOR);

            resetSession();

            // Persist 2 offline sessions of 2 users
            UserSessionModel userSession1 = sessionOR.sessions().getUserSession(realm, origSessions[1].getId());
            UserSessionModel userSession2 = sessionOR.sessions().getUserSession(realm, origSessions[2].getId());
            persistUserSession(sessionOR, userSession1, true);
            persistUserSession(sessionOR, userSession2, true);

            resetSession();

            // Load offline sessions
            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(sessionOR, true, 10, 1, 2);

            // Properly delete user and assert his offlineSession removed
            UserModel user1 = sessionOR.users().getUserByUsername("user1", realm);
            new UserManager(sessionOR).removeUser(realm, user1);

            resetSession();

            Assert.assertEquals(1, persister.getUserSessionsCount(true));
            loadedSessions = loadPersistedSessionsPaginated(sessionOR, true, 10, 1, 1);
            UserSessionModel persistedSession = loadedSessions.get(0);
            UserSessionProviderTest.assertSession(persistedSession, sessionOR.users().getUserByUsername("user2", realm), "127.0.0.3", started, started, "test-app");

            // KEYCLOAK-2431 Assert that userSessionPersister is resistent even to situation, when users are deleted "directly".
            // No exception will happen. However session will be still there
            UserModel user2 = sessionOR.users().getUserByUsername("user2", realm);
            sessionOR.users().removeUser(realm, user2);

            loadedSessions = loadPersistedSessionsPaginated(sessionOR, true, 10, 1, 1);

            // Cleanup
            UserSessionModel userSession = loadedSessions.get(0);
            sessionOR.sessions().removeUserSession(realm, userSession);
            persister.removeUserSession(userSession.getId(), userSession.isOffline());
        });
    }

    // KEYCLOAK-1999
    @Test
    @ModelTest
    public void testNoSessions(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionNS) -> {
            UserSessionPersisterProvider persister = sessionNS.getProvider(UserSessionPersisterProvider.class);
            List<UserSessionModel> sessions = persister.loadUserSessions(0, 1, true, 0, "abc");
            Assert.assertEquals(0, sessions.size());
        });
    }


    @Test
    @ModelTest
    public void testMoreSessions(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionMS) -> {
            RealmModel realm = sessionMS.realms().getRealm("test");
            // Create 10 userSessions - each having 1 clientSession
            List<UserSessionModel> userSessions = new ArrayList<>();
            UserModel user = sessionMS.users().getUserByUsername("user1", realm);

            for (int i = 0; i < 20; i++) {
                // Having different offsets for each session (to ensure that lastSessionRefresh is also different)
                Time.setOffset(i);

                UserSessionModel userSession = sessionMS.sessions().createUserSession(realm, user, "user1", "127.0.0.1", "form", true, null, null);
                createClientSession(sessionMS, realm.getClientByClientId("test-app"), userSession, "http://redirect", "state");
                userSessions.add(userSession);
            }

            //resetSession();

            for (UserSessionModel userSession : userSessions) {
                UserSessionModel userSession2 = sessionMS.sessions().getUserSession(realm, userSession.getId());
                persistUserSession(sessionMS, userSession2, true);
            }

            //resetSession();

            List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(sessionMS, true, 2, 10, 20);
            user = sessionMS.users().getUserByUsername("user1", realm);
            ClientModel testApp = realm.getClientByClientId("test-app");

            for (UserSessionModel loadedSession : loadedSessions) {
                assertEquals(user.getId(), loadedSession.getUser().getId());
                assertEquals("127.0.0.1", loadedSession.getIpAddress());
                assertEquals(user.getUsername(), loadedSession.getLoginUsername());

                assertEquals(1, loadedSession.getAuthenticatedClientSessions().size());
                assertTrue(loadedSession.getAuthenticatedClientSessions().containsKey(testApp.getId()));
            }
        });
    }
    @Test
    @ModelTest
    public void testExpiredSessions(KeycloakSession session) {
        UserSessionModel[][] origSessions = {new UserSessionModel[1]};
        int started = Time.currentTime();
        final UserSessionModel[] userSession1 = {null};
        final UserSessionModel[] userSession2 = {null};

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionES) -> {
                    // Create some sessions in infinispan

                    RealmModel realm = sessionES.realms().getRealm("test");
                    UserSessionPersisterProvider persister = sessionES.getProvider(UserSessionPersisterProvider.class);
                    persister = sessionES.getProvider(UserSessionPersisterProvider.class);
                    origSessions[0] = createSessions(sessionES);
                });
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionES2) -> {
                    // Persist 2 offline sessions of 2 users
                    RealmModel realm = sessionES2.realms().getRealm("test");
                    UserSessionPersisterProvider persister = sessionES2.getProvider(UserSessionPersisterProvider.class);
                    userSession1[0] = sessionES2.sessions().getUserSession(realm, origSessions[0][1].getId());
                    userSession2[0] = sessionES2.sessions().getUserSession(realm, origSessions[0][2].getId());
                    persistUserSession(sessionES2, userSession1[0], true);
                    persistUserSession(sessionES2, userSession2[0], true);
                });
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionES3) -> {
            // Update one of the sessions with lastSessionRefresh of 20 days ahead
            int lastSessionRefresh = Time.currentTime() + 1728000;
            RealmModel realm = sessionES3.realms().getRealm("test");
            UserSessionPersisterProvider persister = sessionES3.getProvider(UserSessionPersisterProvider.class);

            persister.updateLastSessionRefreshes(realm, lastSessionRefresh, Collections.singleton(userSession1[0].getId()), true);

            // Increase time offset - 40 days
            Time.setOffset(3456000);
            try {
                // Run expiration thread
                persister.removeExpired(realm);

                // Test the updated session is still in persister. Not updated session is not there anymore
                List<UserSessionModel> loadedSessions = loadPersistedSessionsPaginated(sessionES3, true, 10, 1, 1);
                UserSessionModel persistedSession = loadedSessions.get(0);
                UserSessionProviderTest.assertSession(persistedSession, sessionES3.users().getUserByUsername("user1", realm), "127.0.0.2", started, lastSessionRefresh, "test-app");

            } finally {
                // Cleanup
                Time.setOffset(0);
            }

        });
    }


    private AuthenticatedClientSessionModel createClientSession(KeycloakSession session, ClientModel client, UserSessionModel userSession, String redirect, String state) {
        RealmModel realm = session.realms().getRealm("test");
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        return clientSession;
    }

    private UserSessionModel[] createSessions(KeycloakSession session) {
        RealmModel realm = session.realms().getRealm("test");
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null);

        createClientSession(session, realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state");
        createClientSession(session, realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state");

        sessions[1] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true, null, null);
        createClientSession(session, realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state");

        sessions[2] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.3", "form", true, null, null);
        createClientSession(session, realm.getClientByClientId("test-app"), sessions[2], "http://redirect", "state");

        return sessions;
    }

    private void persistUserSession(KeycloakSession session, UserSessionModel userSession, boolean offline) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);
        persister.createUserSession(userSession, offline);
        for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
            persister.createClientSession(clientSession, offline);
        }
    }

    private void resetSession() {
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

    private List<UserSessionModel> loadPersistedSessionsPaginated(KeycloakSession session, boolean offline, int sessionsPerPage, int expectedPageCount, int expectedSessionsCount) {
        UserSessionPersisterProvider persister = session.getProvider(UserSessionPersisterProvider.class);

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

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }
}
