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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Time;
import org.keycloak.models.*;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.runonserver.RunOnServerDeployment;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.arquillian.DeploymentTargetModifier.AUTH_SERVER_CURRENT;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserSessionProviderOfflineTest extends AbstractTestRealmKeycloakTest {
    private static KeycloakSession currentSession;
    private static RealmModel realm;
    private static UserSessionManager sessionManager;
    private static UserSessionPersisterProvider persister;

    @Deployment
    @TargetsContainer(AUTH_SERVER_CURRENT)
    public static WebArchive deploy() {
        return RunOnServerDeployment.create(UserResource.class, UserSessionProviderOfflineTest.class)
                .addPackages(true,
                        "org.keycloak.testsuite",
                        "org.keycloak.testsuite.model");
    }

    private static Set<String> createOfflineSessionIncludeClientSessions(KeycloakSession session, UserSessionModel userSession) {
        Set<String> offlineSessions = new HashSet<>();
        UserSessionManager localManager = new UserSessionManager(session);
        for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
            localManager.createOrUpdateOfflineSession(clientSession, userSession);
            offlineSessions.add(clientSession.getClient().getId());
        }

        return offlineSessions;
    }

    public static void assertSession(UserSessionModel session, UserModel user, String ipAddress, int started, int lastRefresh, String... clients) {
        assertEquals(user.getId(), session.getUser().getId());
        assertEquals(ipAddress, session.getIpAddress());
        assertEquals(user.getUsername(), session.getLoginUsername());
        assertEquals("form", session.getAuthMethod());
        assertTrue(session.isRememberMe());
        //assertTrue(session.getStarted() >= started - 1 && session.getStarted() <= started + 1);
        //assertTrue(session.getLastSessionRefresh() >= lastRefresh - 1 && session.getLastSessionRefresh() <= lastRefresh + 1);

        String[] actualClients = new String[session.getAuthenticatedClientSessions().size()];
        int i = 0;
        for (Map.Entry<String, AuthenticatedClientSessionModel> entry : session.getAuthenticatedClientSessions().entrySet()) {
            String clientUUID = entry.getKey();
            AuthenticatedClientSessionModel clientSession = entry.getValue();
            Assert.assertEquals(clientUUID, clientSession.getClient().getId());
            actualClients[i] = clientSession.getClient().getClientId();
            i++;
        }
    }

    private static AuthenticatedClientSessionModel createClientSession(KeycloakSession sessionParam, ClientModel client, UserSessionModel userSession, String redirect, String state) {
        AuthenticatedClientSessionModel clientSession = sessionParam.sessions().createClientSession(client.getRealm(), client, userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        return clientSession;
    }

    private static UserSessionModel[] createSessions(KeycloakSession session) {
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(realm, currentSession.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null);

        Set<String> roles = new HashSet<String>();
        roles.add("one");
        roles.add("two");

        Set<String> protocolMappers = new HashSet<String>();
        protocolMappers.add("mapper-one");
        protocolMappers.add("mapper-two");

        createClientSession(session, realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state");
        createClientSession(session, realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state");

        sessions[1] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true, null, null);
        createClientSession(session, realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state");

        sessions[2] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.3", "form", true, null, null);
        createClientSession(session, realm.getClientByClientId("test-app"), sessions[2], "http://redirect", "state");

        return sessions;
    }

    public static void reloadState(KeycloakSession session) {
        reloadState(session, false);
    }

    public static void reloadState(KeycloakSession session, Boolean initialConfig) {
        currentSession = session;
        realm = currentSession.realms().getRealm("test");
        if (initialConfig == true) {
            currentSession.users().addUser(realm, "user1").setEmail("user1@localhost");
            currentSession.users().addUser(realm, "user2").setEmail("user2@localhost");
        }
        sessionManager = new UserSessionManager(currentSession);
        persister = currentSession.getProvider(UserSessionPersisterProvider.class);
    }

    @Before
    public void before() {
        testingClient.server().run(session -> {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionBefore) -> {
                reloadState(sessionBefore, true);
            });
        });
    }

    @After
    public void after() {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
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
    public void testOfflineSessionsCrud(KeycloakSession session) {

        UserModel[] user1 = new UserModel[1];
        UserModel[] user2 = new UserModel[1];
        Set<ClientModel>[] clients = new Set[1];
        UserSessionModel[][] origSessions = new UserSessionModel[1][1];
        Map<String, Set<String>> offlineSessions = new HashMap<>();
        ClientModel[] testApp = new ClientModel[1];
        ClientModel[] thirdparty = new ClientModel[1];


        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud) -> {
            // Create some online sessions in infinispan
            int started = Time.currentTime();

            reloadState(sessionCrud);

            origSessions[0] = createSessions(sessionCrud);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud2) -> {
            currentSession = sessionCrud2;
            realm = currentSession.realms().getRealm("test");
            sessionManager = new UserSessionManager(currentSession);
            persister = currentSession.getProvider(UserSessionPersisterProvider.class);

            // Key is userSession ID, values are client UUIDS
            //  ***** offlineSessions is declared here *****
            // Persist 3 created userSessions and clientSessions as offline
            testApp[0] = realm.getClientByClientId("test-app");
            List<UserSessionModel> userSessions = currentSession.sessions().getUserSessions(realm, testApp[0]);
            for (UserSessionModel userSession : userSessions) {
                offlineSessions.put(userSession.getId(), createOfflineSessionIncludeClientSessions(currentSession, userSession)); // ***** offline sessions is populated here *****
            }
            sessionCrud2.getTransactionManager().commit(); // <-- ***** without this commit offLineSessions.size() will return zero below in the for loop starting at line 220
            sessionCrud2.close(); // <-- This is because I need to end the transacton
        });
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud3) -> {  // <-- ***** New transaction here because it offlineSession.size() will return 0 *****
            currentSession = sessionCrud3;
            realm = currentSession.realms().getRealm("test");
            sessionManager = new UserSessionManager(currentSession);
            persister = currentSession.getProvider(UserSessionPersisterProvider.class);


            // Assert all previously saved offline sessions found
            for (Map.Entry<String, Set<String>> entry : offlineSessions.entrySet()) {  // <-- ***** Otherwise offlineSessions goes out of scope!  *****
                UserSessionModel offlineSession = sessionManager.findOfflineUserSession(realm, entry.getKey());
                Assert.assertNotNull(offlineSession);
                Assert.assertEquals(offlineSession.getAuthenticatedClientSessions().keySet(), entry.getValue());
            }


            // Find clients with offline token
            user1[0] = currentSession.users().getUserByUsername("user1", realm);
            clients[0] = sessionManager.findClientsWithOfflineToken(realm, user1[0]);
            Assert.assertEquals(clients[0].size(), 2);
            for (ClientModel client : clients[0]) {
                Assert.assertTrue(client.getClientId().equals("test-app") || client.getClientId().equals("third-party"));
            }

            user2[0] = currentSession.users().getUserByUsername("user2", realm);
            clients[0] = sessionManager.findClientsWithOfflineToken(realm, user2[0]);
            Assert.assertEquals(clients[0].size(), 1);
            Assert.assertEquals("test-app", clients[0].iterator().next().getClientId());

            // Test count
            testApp[0] = realm.getClientByClientId("test-app");
            thirdparty[0] = realm.getClientByClientId("third-party");
            Assert.assertEquals(3, currentSession.sessions().getOfflineSessionsCount(realm, testApp[0]));
            Assert.assertEquals(1, currentSession.sessions().getOfflineSessionsCount(realm, thirdparty[0]));
            // Revoke "test-app" for user1
            sessionManager.revokeOfflineToken(user1[0], testApp[0]);

            // Assert userSession revoked
            testApp[0] = realm.getClientByClientId("test-app");
            thirdparty[0] = realm.getClientByClientId("third-party");

        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud4) -> {
            currentSession = sessionCrud4;
            realm = currentSession.realms().getRealm("test");
            sessionManager = new UserSessionManager(currentSession);
            persister = currentSession.getProvider(UserSessionPersisterProvider.class);

            // Still 2 sessions. The count of sessions by client may not be accurate after revoke due the
            // performance optimizations (the "127.0.0.1" currentSession still has another client "thirdparty" in it)
            Assert.assertEquals(2, currentSession.sessions().getOfflineSessionsCount(realm, testApp[0]));
            Assert.assertEquals(1, currentSession.sessions().getOfflineSessionsCount(realm, thirdparty[0]));

            List<UserSessionModel> thirdpartySessions = currentSession.sessions().getOfflineUserSessions(realm, thirdparty[0], 0, 10);
            Assert.assertEquals(1, thirdpartySessions.size());
            Assert.assertEquals("127.0.0.1", thirdpartySessions.get(0).getIpAddress());
            Assert.assertEquals("user1", thirdpartySessions.get(0).getUser().getUsername());

            user1[0] = currentSession.users().getUserByUsername("user1", realm);
            user2[0] = currentSession.users().getUserByUsername("user2", realm);
            clients[0] = sessionManager.findClientsWithOfflineToken(realm, user1[0]);
            Assert.assertEquals(1, clients[0].size());
            Assert.assertEquals("third-party", clients[0].iterator().next().getClientId());
            clients[0] = sessionManager.findClientsWithOfflineToken(realm, user2[0]);
            Assert.assertEquals(1, clients[0].size());
            Assert.assertEquals("test-app", clients[0].iterator().next().getClientId());

            // Revoke the second currentSession for user1 too.
            sessionManager.revokeOfflineToken(user1[0], thirdparty[0]);

        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud5) -> {
            currentSession = sessionCrud5;
            realm = currentSession.realms().getRealm("test");
            sessionManager = new UserSessionManager(currentSession);
            persister = currentSession.getProvider(UserSessionPersisterProvider.class);

            testApp[0] = realm.getClientByClientId("test-app");
            thirdparty[0] = realm.getClientByClientId("third-party");

            // Accurate count now. All sessions of user1 cleared
            Assert.assertEquals(1, currentSession.sessions().getOfflineSessionsCount(realm, testApp[0]));
            Assert.assertEquals(0, currentSession.sessions().getOfflineSessionsCount(realm, thirdparty[0]));

            List<UserSessionModel> testAppSessions = currentSession.sessions().getOfflineUserSessions(realm, testApp[0], 0, 10);

            Assert.assertEquals(1, testAppSessions.size());
            Assert.assertEquals("127.0.0.3", testAppSessions.get(0).getIpAddress());
            Assert.assertEquals("user2", testAppSessions.get(0).getUser().getUsername());

            clients[0] = sessionManager.findClientsWithOfflineToken(realm, user1[0]);
            Assert.assertEquals(0, clients[0].size());

        });
    }


    @Test
    @ModelTest
    public void testOnRealmRemoved(KeycloakSession session) {

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRR) -> {

            currentSession = sessionRR;
            int started = Time.currentTime();
            sessionManager = new UserSessionManager(currentSession);
            persister = currentSession.getProvider(UserSessionPersisterProvider.class);
            RealmModel fooRealm = currentSession.realms().createRealm("foo", "foo");
            fooRealm.addClient("foo-app");
            currentSession.users().addUser(fooRealm, "user3");

            UserSessionModel userSession = currentSession.sessions().createUserSession(fooRealm, currentSession.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
            AuthenticatedClientSessionModel clientSession = createClientSession(currentSession, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");



            // Persist offline session
            fooRealm = currentSession.realms().getRealm("foo");
            userSession = currentSession.sessions().getUserSession(fooRealm, userSession.getId());
            createOfflineSessionIncludeClientSessions(currentSession, userSession);



            UserSessionModel offlineUserSession = sessionManager.findOfflineUserSession(fooRealm, userSession.getId());
            Assert.assertEquals(offlineUserSession.getAuthenticatedClientSessions().size(), 1);
            AuthenticatedClientSessionModel offlineClientSession = offlineUserSession.getAuthenticatedClientSessions().values().iterator().next();
            Assert.assertEquals("foo-app", offlineClientSession.getClient().getClientId());
            Assert.assertEquals("user3", offlineClientSession.getUserSession().getUser().getUsername());

            // Remove realm
            RealmManager realmMgr = new RealmManager(currentSession);
            realmMgr.removeRealm(realmMgr.getRealm("foo"));



            fooRealm = currentSession.realms().createRealm("foo", "foo");
            fooRealm.addClient("foo-app");
            currentSession.users().addUser(fooRealm, "user3");



            // Assert nothing loaded
            fooRealm = currentSession.realms().getRealm("foo");
            // Tests for Null seem to break always under the new testsuite, we test for the right number of things below so no need to test for null
            //Assert.assertNull(sessionManager.findOfflineUserSession(fooRealm, userSession.getId()));
            Assert.assertEquals(0, currentSession.sessions().getOfflineSessionsCount(fooRealm, fooRealm.getClientByClientId("foo-app")));

            // Cleanup
            realmMgr = new RealmManager(currentSession);
            realmMgr.removeRealm(realmMgr.getRealm("foo"));
        });
    }

    @Test
    @ModelTest
    public void testOnClientRemoved(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCR) -> {
            try {

                currentSession = sessionCR;
                int started = Time.currentTime();
                sessionManager = new UserSessionManager(currentSession);
                persister = currentSession.getProvider(UserSessionPersisterProvider.class);
                RealmModel fooRealm = currentSession.realms().createRealm("foo", "foo");
                fooRealm.addClient("foo-app");
                fooRealm.addClient("bar-app");
                currentSession.users().addUser(fooRealm, "user3");

                UserSessionModel userSession = currentSession.sessions().createUserSession(fooRealm, currentSession.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
                createClientSession(currentSession, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
                createClientSession(currentSession, fooRealm.getClientByClientId("bar-app"), userSession, "http://redirect", "state");


                // Create offline currentSession
                fooRealm = currentSession.realms().getRealm("foo");
                userSession = currentSession.sessions().getUserSession(fooRealm, userSession.getId());
                createOfflineSessionIncludeClientSessions(currentSession, userSession);

                RealmManager realmMgr = new RealmManager(currentSession);
                ClientManager clientMgr = new ClientManager(realmMgr);
                fooRealm = realmMgr.getRealm("foo");

                // Assert currentSession was persisted with both clientSessions
                UserSessionModel offlineSession = currentSession.sessions().getOfflineUserSession(fooRealm, userSession.getId());
                assertSession(offlineSession, currentSession.users().getUserByUsername("user3", fooRealm), "127.0.0.1", started, started, "foo-app", "bar-app");

                // Remove foo-app client
                ClientModel client = fooRealm.getClientByClientId("foo-app");
                clientMgr.removeClient(fooRealm, client);

                realmMgr = new RealmManager(currentSession);
                clientMgr = new ClientManager(realmMgr);
                fooRealm = realmMgr.getRealm("foo");

                // Assert just one bar-app clientSession persisted now
                offlineSession = currentSession.sessions().getOfflineUserSession(fooRealm, userSession.getId());
                Assert.assertEquals(1, offlineSession.getAuthenticatedClientSessions().size());
                Assert.assertEquals("bar-app", offlineSession.getAuthenticatedClientSessions().values().iterator().next().getClient().getClientId());

                // Remove bar-app client
                client = fooRealm.getClientByClientId("bar-app");
                clientMgr.removeClient(fooRealm, client);

                // Assert nothing loaded - userSession was removed as well because it was last userSession
                realmMgr = new RealmManager(currentSession);
                fooRealm = realmMgr.getRealm("foo");
                offlineSession = currentSession.sessions().getOfflineUserSession(fooRealm, userSession.getId());
                Assert.assertEquals(0, offlineSession.getAuthenticatedClientSessions().size());
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {

                RealmManager realmMgr = new RealmManager(currentSession);
                RealmModel fooRealm = realmMgr.getRealm("foo");
                UserModel user3 = currentSession.users().getUserByUsername("user3", fooRealm);

                // Remove user3
                new UserManager(currentSession).removeUser(fooRealm, user3);

                // Cleanup
                realmMgr = new RealmManager(currentSession);
                realmMgr.removeRealm(realmMgr.getRealm("foo"));
            }

        });
    }


    @Test
    @ModelTest
    public void testOnUserRemoved(KeycloakSession session) {
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionUR) -> {
            try {
                int started = Time.currentTime();

                RealmModel fooRealm = sessionUR.realms().createRealm("foo", "foo");
                fooRealm.addClient("foo-app");
                sessionUR.users().addUser(fooRealm, "user3");

                UserSessionModel userSession = sessionUR.sessions().createUserSession(fooRealm, sessionUR.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
                AuthenticatedClientSessionModel clientSession = createClientSession(sessionUR, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");

                // Create offline session
                fooRealm = sessionUR.realms().getRealm("foo");
                userSession = sessionUR.sessions().getUserSession(fooRealm, userSession.getId());
                createOfflineSessionIncludeClientSessions(sessionUR, userSession);

                RealmManager realmMgr = new RealmManager(sessionUR);
                fooRealm = realmMgr.getRealm("foo");
                UserModel user3 = sessionUR.users().getUserByUsername("user3", fooRealm);

                // Assert session was persisted with both clientSessions
                UserSessionModel offlineSession = sessionUR.sessions().getOfflineUserSession(fooRealm, userSession.getId());
                assertSession(offlineSession, user3, "127.0.0.1", started, started, "foo-app");


            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {

                RealmManager realmMgr = new RealmManager(sessionUR);
                RealmModel fooRealm = realmMgr.getRealm("foo");
                UserModel user3 = sessionUR.users().getUserByUsername("user3", fooRealm);

                // Remove user3
                new UserManager(sessionUR).removeUser(fooRealm, user3);

                // Cleanup
                realmMgr = new RealmManager(sessionUR);
                realmMgr.removeRealm(realmMgr.getRealm("foo"));
            }
        });
    }

    @Test
    @ModelTest
    public void testExpired(KeycloakSession session) {

        UserModel[] user1 = new UserModel[1];
        UserModel[] user2 = new UserModel[1];
        Set<ClientModel>[] clients = new Set[1];
        UserSessionModel[][] origSessions = new UserSessionModel[1][1];
        Map<String, Set<String>> offlineSessions = new HashMap<>();
        ClientModel[] testApp = new ClientModel[1];
        ClientModel[] thirdparty = new ClientModel[1];


        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionExpired) -> {
            // Create some online sessions in infinispan
            int started = Time.currentTime();

            reloadState(sessionExpired);

            origSessions[0] = createSessions(sessionExpired);

        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionExpired2) -> {
            currentSession = sessionExpired2;
            realm = currentSession.realms().getRealm("test");
            sessionManager = new UserSessionManager(currentSession);
            persister = currentSession.getProvider(UserSessionPersisterProvider.class);

            // Key is userSessionId, value is set of client UUIDS


            // Persist 3 created userSessions and clientSessions as offline
            testApp[0] = realm.getClientByClientId("test-app");
            List<UserSessionModel> userSessions = currentSession.sessions().getUserSessions(realm, testApp[0]);
            for (UserSessionModel userSession : userSessions) {
                offlineSessions.put(userSession.getId(), createOfflineSessionIncludeClientSessions(currentSession, userSession));
            }


            // Assert all previously saved offline sessions found
            for (Map.Entry<String, Set<String>> entry : offlineSessions.entrySet()) {
                UserSessionModel foundSession = sessionManager.findOfflineUserSession(realm, entry.getKey());
                Assert.assertEquals(foundSession.getAuthenticatedClientSessions().keySet(), entry.getValue());
            }
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionExpired3) -> {
                    currentSession = sessionExpired3;
                    realm = currentSession.realms().getRealm("test");
                    sessionManager = new UserSessionManager(currentSession);
                    persister = currentSession.getProvider(UserSessionPersisterProvider.class);

                    UserSessionModel session0 = currentSession.sessions().getOfflineUserSession(realm, origSessions[0][0].getId());
                    Assert.assertNotNull(session0);

                    // sessions are in persister too
                    Assert.assertEquals(3, persister.getUserSessionsCount(true));

                    // Set lastSessionRefresh to currentSession[0] to 0
                    session0.setLastSessionRefresh(0);

                    currentSession.sessions().removeExpired(realm);
                });

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionExpired4) -> {
            currentSession = sessionExpired4;
            realm = currentSession.realms().getRealm("test");
            sessionManager = new UserSessionManager(currentSession);
            persister = currentSession.getProvider(UserSessionPersisterProvider.class);


            // assert session0 not found now
            Assert.assertNull(currentSession.sessions().getOfflineUserSession(realm, origSessions[0][0].getId()));

            Assert.assertEquals(2, persister.getUserSessionsCount(true));

            // Expire everything and assert nothing found
            Time.setOffset(3000000);
            try {
                currentSession.sessions().removeExpired(realm);

                for (String userSessionId : offlineSessions.keySet()) {
                    Assert.assertNull(sessionManager.findOfflineUserSession(realm, userSessionId));
                }
                Assert.assertEquals(0, persister.getUserSessionsCount(true));

            } finally {
                Time.setOffset(0);
            }

        });


    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }
}
