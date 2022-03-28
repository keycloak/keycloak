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
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.keycloak.models.Constants;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class UserSessionProviderOfflineTest extends AbstractTestRealmKeycloakTest {

    private static KeycloakSession currentSession;
    private static RealmModel realm;
    private static UserSessionManager sessionManager;

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

    @Test
    @ModelTest
    public void testOfflineSessionsCrud(KeycloakSession session) {
        Map<String, Set<String>> offlineSessions = new HashMap<>();

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud) -> {
            // Create some online sessions in infinispan
            reloadState(sessionCrud);
            createSessions(sessionCrud);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud2) -> {
            currentSession = sessionCrud2;
            realm = currentSession.realms().getRealmByName("test");
            sessionManager = new UserSessionManager(currentSession);

            // Key is userSession ID, values are client UUIDS
            // Persist 3 created userSessions and clientSessions as offline
            ClientModel testApp = realm.getClientByClientId("test-app");
            currentSession.sessions().getUserSessionsStream(realm, testApp).collect(Collectors.toList())
                    .forEach(userSession -> offlineSessions.put(userSession.getId(), createOfflineSessionIncludeClientSessions(currentSession, userSession)));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud3) -> {
            currentSession = sessionCrud3;
            realm = currentSession.realms().getRealmByName("test");
            sessionManager = new UserSessionManager(currentSession);

            // Assert all previously saved offline sessions found
            for (Map.Entry<String, Set<String>> entry : offlineSessions.entrySet()) {
                UserSessionModel offlineSession = sessionManager.findOfflineUserSession(realm, entry.getKey());
                Assert.assertNotNull(offlineSession);
                Assert.assertEquals(offlineSession.getAuthenticatedClientSessions().keySet(), entry.getValue());
            }

            // Find clients with offline token
            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");

            Set<ClientModel> clients = sessionManager.findClientsWithOfflineToken(realm, user1);
            Assert.assertEquals(clients.size(), 2);
            for (ClientModel client : clients) {
                Assert.assertTrue(client.getClientId().equals("test-app") || client.getClientId().equals("third-party"));
            }

            UserModel user2 = currentSession.users().getUserByUsername(realm, "user2");

            clients = sessionManager.findClientsWithOfflineToken(realm, user2);
            Assert.assertEquals(clients.size(), 1);
            Assert.assertEquals("test-app", clients.iterator().next().getClientId());

            // Test count
            ClientModel testApp = realm.getClientByClientId("test-app");
            ClientModel thirdparty = realm.getClientByClientId("third-party");
            Assert.assertEquals(3, currentSession.sessions().getOfflineSessionsCount(realm, testApp));
            Assert.assertEquals(1, currentSession.sessions().getOfflineSessionsCount(realm, thirdparty));
            // Revoke "test-app" for user1
            sessionManager.revokeOfflineToken(user1, testApp);
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud4) -> {
            currentSession = sessionCrud4;
            realm = currentSession.realms().getRealmByName("test");
            sessionManager = new UserSessionManager(currentSession);

            // Assert userSession revoked
            ClientModel thirdparty = realm.getClientByClientId("third-party");

            List<UserSessionModel> thirdpartySessions = currentSession.sessions().getOfflineUserSessionsStream(realm, thirdparty, 0, 10)
                    .collect(Collectors.toList());
            Assert.assertEquals(1, thirdpartySessions.size());
            Assert.assertEquals("127.0.0.1", thirdpartySessions.get(0).getIpAddress());
            Assert.assertEquals("user1", thirdpartySessions.get(0).getUser().getUsername());

            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");
            UserModel user2 = currentSession.users().getUserByUsername(realm, "user2");

            Set<ClientModel> clients = sessionManager.findClientsWithOfflineToken(realm, user1);
            Assert.assertEquals(1, clients.size());
            Assert.assertEquals("third-party", clients.iterator().next().getClientId());
            clients = sessionManager.findClientsWithOfflineToken(realm, user2);
            Assert.assertEquals(1, clients.size());
            Assert.assertEquals("test-app", clients.iterator().next().getClientId());

            // Revoke the second currentSession for user1 too.
            sessionManager.revokeOfflineToken(user1, thirdparty);

        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCrud5) -> {
            currentSession = sessionCrud5;
            realm = currentSession.realms().getRealmByName("test");
            sessionManager = new UserSessionManager(currentSession);

            ClientModel testApp = realm.getClientByClientId("test-app");
            ClientModel thirdparty = realm.getClientByClientId("third-party");

            // Accurate count now. All sessions of user1 cleared
            Assert.assertEquals(1, currentSession.sessions().getOfflineSessionsCount(realm, testApp));
            Assert.assertEquals(0, currentSession.sessions().getOfflineSessionsCount(realm, thirdparty));

            List<UserSessionModel> testAppSessions = currentSession.sessions().getOfflineUserSessionsStream(realm, testApp, 0, 10)
                    .collect(Collectors.toList());

            Assert.assertEquals(1, testAppSessions.size());
            Assert.assertEquals("127.0.0.3", testAppSessions.get(0).getIpAddress());
            Assert.assertEquals("user2", testAppSessions.get(0).getUser().getUsername());

            UserModel user1 = currentSession.users().getUserByUsername(realm, "user1");

            Set<ClientModel> clients = sessionManager.findClientsWithOfflineToken(realm, user1);
            Assert.assertEquals(0, clients.size());
        });
    }

    @Test
    @ModelTest
    public void testOnRealmRemoved(KeycloakSession session) {
        AtomicReference<String> userSessionID = new AtomicReference<>();

        String realmId = KeycloakModelUtils.generateId();
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRR1) -> {
            currentSession = sessionRR1;
            RealmModel fooRealm = currentSession.realms().createRealm(realmId, "foo");
            fooRealm.setDefaultRole(currentSession.roles().addRealmRole(fooRealm, Constants.DEFAULT_ROLES_ROLE_PREFIX  + "-" + fooRealm.getName()));
            fooRealm.setSsoSessionIdleTimeout(1800);
            fooRealm.setSsoSessionMaxLifespan(36000);
            fooRealm.setOfflineSessionIdleTimeout(2592000);
            fooRealm.setOfflineSessionMaxLifespan(5184000);
            fooRealm.addClient("foo-app");
            currentSession.users().addUser(fooRealm, "user3");

            UserSessionModel userSession = currentSession.sessions().createUserSession(fooRealm, currentSession.users().getUserByUsername(fooRealm, "user3"), "user3", "127.0.0.1", "form", true, null, null);
            userSessionID.set(userSession.getId());

            createClientSession(currentSession, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRR2) -> {
            currentSession = sessionRR2;
            sessionManager = new UserSessionManager(currentSession);

            // Persist offline session
            RealmModel fooRealm = currentSession.realms().getRealm(realmId);
            UserSessionModel userSession = currentSession.sessions().getUserSession(fooRealm, userSessionID.get());
            createOfflineSessionIncludeClientSessions(currentSession, userSession);

            UserSessionModel offlineUserSession = sessionManager.findOfflineUserSession(fooRealm, userSession.getId());
            Assert.assertEquals(offlineUserSession.getAuthenticatedClientSessions().size(), 1);
            AuthenticatedClientSessionModel offlineClientSession = offlineUserSession.getAuthenticatedClientSessions().values().iterator().next();
            Assert.assertEquals("foo-app", offlineClientSession.getClient().getClientId());
            Assert.assertEquals("user3", offlineClientSession.getUserSession().getUser().getUsername());

            // Remove realm
            RealmManager realmMgr = new RealmManager(currentSession);
            realmMgr.removeRealm(realmMgr.getRealm(realmId));
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRR3) -> {
            currentSession = sessionRR3;
            RealmModel fooRealm = currentSession.realms().createRealm(realmId, "foo");
            fooRealm.setDefaultRole(currentSession.roles().addRealmRole(fooRealm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + fooRealm.getName()));

            fooRealm.addClient("foo-app");
            currentSession.users().addUser(fooRealm, "user3");
        });

        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionRR4) -> {
            currentSession = sessionRR4;
            RealmModel fooRealm = currentSession.realms().getRealm(realmId);

            Assert.assertEquals(0, currentSession.sessions().getOfflineSessionsCount(fooRealm, fooRealm.getClientByClientId("foo-app")));

            // Cleanup
            RealmManager realmMgr = new RealmManager(currentSession);
            realmMgr.removeRealm(realmMgr.getRealm(realmId));
        });
    }

    @Test
    @ModelTest
    public void testOnClientRemoved(KeycloakSession session) {

        String realmId = KeycloakModelUtils.generateId();
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCR) -> {
            try {
                int started = Time.currentTime();
                AtomicReference<String> userSessionID = new AtomicReference<>();

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCR1) -> {
                    currentSession = sessionCR1;
                    sessionManager = new UserSessionManager(currentSession);
                    RealmModel fooRealm = currentSession.realms().createRealm(realmId, "foo");
                    fooRealm.setDefaultRole(currentSession.roles().addRealmRole(fooRealm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + fooRealm.getName()));
                    fooRealm.setSsoSessionIdleTimeout(1800);
                    fooRealm.setSsoSessionMaxLifespan(36000);
                    fooRealm.setOfflineSessionIdleTimeout(2592000);
                    fooRealm.setOfflineSessionMaxLifespan(5184000);

                    fooRealm.addClient("foo-app");
                    fooRealm.addClient("bar-app");
                    currentSession.users().addUser(fooRealm, "user3");

                    UserSessionModel userSession = currentSession.sessions().createUserSession(fooRealm, currentSession.users().getUserByUsername(fooRealm, "user3"), "user3", "127.0.0.1", "form", true, null, null);
                    userSessionID.set(userSession.getId());

                    createClientSession(currentSession, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
                    createClientSession(currentSession, fooRealm.getClientByClientId("bar-app"), userSession, "http://redirect", "state");
                });

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCR2) -> {
                    currentSession = sessionCR2;
                    // Create offline currentSession
                    RealmModel fooRealm = currentSession.realms().getRealm(realmId);
                    UserSessionModel userSession = currentSession.sessions().getUserSession(fooRealm, userSessionID.get());
                    createOfflineSessionIncludeClientSessions(currentSession, userSession);
                });

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCR3) -> {
                    currentSession = sessionCR3;
                    RealmManager realmMgr = new RealmManager(currentSession);
                    ClientManager clientMgr = new ClientManager(realmMgr);
                    RealmModel fooRealm = realmMgr.getRealm(realmId);

                    // Assert currentSession was persisted with both clientSessions
                    UserSessionModel offlineSession = currentSession.sessions().getOfflineUserSession(fooRealm, userSessionID.get());
                    assertSession(offlineSession, currentSession.users().getUserByUsername(fooRealm, "user3"), "127.0.0.1", started, started, "foo-app", "bar-app");

                    // Remove foo-app client
                    ClientModel client = fooRealm.getClientByClientId("foo-app");
                    clientMgr.removeClient(fooRealm, client);
                });

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCR4) -> {
                    currentSession = sessionCR4;
                    RealmManager realmMgr = new RealmManager(currentSession);
                    ClientManager clientMgr = new ClientManager(realmMgr);
                    RealmModel fooRealm = realmMgr.getRealm(realmId);

                    // Assert just one bar-app clientSession persisted now
                    UserSessionModel offlineSession = currentSession.sessions().getOfflineUserSession(fooRealm, userSessionID.get());
                    Assert.assertEquals(1, offlineSession.getAuthenticatedClientSessions().size());
                    Assert.assertEquals("bar-app", offlineSession.getAuthenticatedClientSessions().values().iterator().next().getClient().getClientId());

                    // Remove bar-app client
                    ClientModel client = fooRealm.getClientByClientId("bar-app");
                    clientMgr.removeClient(fooRealm, client);
                });

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionCR5) -> {
                    currentSession = sessionCR5;
                    // Assert nothing loaded - userSession was removed as well because it was last userSession
                    RealmManager realmMgr = new RealmManager(currentSession);
                    RealmModel fooRealm = realmMgr.getRealm(realmId);
                    UserSessionModel offlineSession = currentSession.sessions().getOfflineUserSession(fooRealm, userSessionID.get());
                    Assert.assertEquals(0, offlineSession.getAuthenticatedClientSessions().size());
                });

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionTearDown) -> {
                    currentSession = sessionTearDown;
                    RealmManager realmMgr = new RealmManager(currentSession);
                    RealmModel fooRealm = realmMgr.getRealm(realmId);
                    UserModel user3 = currentSession.users().getUserByUsername(fooRealm, "user3");

                    // Remove user3
                    new UserManager(currentSession).removeUser(fooRealm, user3);

                    // Cleanup
                    realmMgr = new RealmManager(currentSession);
                    realmMgr.removeRealm(realmMgr.getRealm(realmId));
                });
            }
        });
    }

    @Test
    @ModelTest
    public void testOnUserRemoved(KeycloakSession session) {

        String realmId = KeycloakModelUtils.generateId();
        KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionUR) -> {
            try {
                int started = Time.currentTime();
                AtomicReference<String> userSessionID = new AtomicReference<>();

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionUR1) -> {
                    currentSession = sessionUR1;
                    RealmModel fooRealm = currentSession.realms().createRealm(realmId, "foo");
                    fooRealm.setDefaultRole(currentSession.roles().addRealmRole(fooRealm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + fooRealm.getName()));
                    fooRealm.setSsoSessionIdleTimeout(1800);
                    fooRealm.setSsoSessionMaxLifespan(36000);
                    fooRealm.setOfflineSessionIdleTimeout(2592000);
                    fooRealm.setOfflineSessionMaxLifespan(5184000);
                    fooRealm.addClient("foo-app");
                    currentSession.users().addUser(fooRealm, "user3");

                    UserSessionModel userSession = currentSession.sessions().createUserSession(fooRealm, currentSession.users().getUserByUsername(fooRealm, "user3"), "user3", "127.0.0.1", "form", true, null, null);
                    userSessionID.set(userSession.getId());

                    createClientSession(currentSession, fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state");
                });

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionUR2) -> {
                    currentSession = sessionUR2;

                    // Create offline session
                    RealmModel fooRealm = currentSession.realms().getRealm(realmId);
                    UserSessionModel userSession = currentSession.sessions().getUserSession(fooRealm, userSessionID.get());
                    createOfflineSessionIncludeClientSessions(currentSession, userSession);
                });

                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionUR3) -> {
                    currentSession = sessionUR3;

                    RealmManager realmMgr = new RealmManager(currentSession);
                    RealmModel fooRealm = realmMgr.getRealm(realmId);
                    UserModel user3 = currentSession.users().getUserByUsername(fooRealm, "user3");

                    // Assert session was persisted with both clientSessions
                    UserSessionModel offlineSession = currentSession.sessions().getOfflineUserSession(fooRealm, userSessionID.get());
                    assertSession(offlineSession, user3, "127.0.0.1", started, started, "foo-app");
                });

            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), (KeycloakSession sessionTearDown) -> {
                    currentSession = sessionTearDown;

                    RealmManager realmMgr = new RealmManager(currentSession);
                    RealmModel fooRealm = realmMgr.getRealm(realmId);
                    UserModel user3 = currentSession.users().getUserByUsername(fooRealm, "user3");

                    // Remove user3
                    new UserManager(currentSession).removeUser(fooRealm, user3);

                    // Cleanup
                    realmMgr = new RealmManager(currentSession);
                    realmMgr.removeRealm(fooRealm);
                });
            }
        });
    }

    private static Set<String> createOfflineSessionIncludeClientSessions(KeycloakSession session, UserSessionModel
            userSession) {
        Set<String> offlineSessions = new HashSet<>();
        UserSessionManager localManager = new UserSessionManager(session);
        for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
            localManager.createOrUpdateOfflineSession(clientSession, userSession);
            offlineSessions.add(clientSession.getClient().getId());
        }

        return offlineSessions;
    }

    public static void assertSession(UserSessionModel session, UserModel user, String ipAddress, int started,
                                     int lastRefresh, String... clients) {
        assertEquals(user.getId(), session.getUser().getId());
        assertEquals(ipAddress, session.getIpAddress());
        assertEquals(user.getUsername(), session.getLoginUsername());
        assertEquals("form", session.getAuthMethod());
        assertTrue(session.isRememberMe());
        assertTrue((session.getStarted() >= started - 1) && (session.getStarted() <= started + 1));
        assertTrue((session.getLastSessionRefresh() >= lastRefresh - 1) && (session.getLastSessionRefresh() <= lastRefresh + 1));

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

    private static AuthenticatedClientSessionModel createClientSession(KeycloakSession sessionParam, ClientModel
            client, UserSessionModel userSession, String redirect, String state) {
        AuthenticatedClientSessionModel clientSession = sessionParam.sessions().createClientSession(client.getRealm(), client, userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        return clientSession;
    }

    private static UserSessionModel[] createSessions(KeycloakSession session) {
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(realm, currentSession.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.1", "form", true, null, null);

        Set<String> roles = new HashSet<String>();
        roles.add("one");
        roles.add("two");

        Set<String> protocolMappers = new HashSet<String>();
        protocolMappers.add("mapper-one");
        protocolMappers.add("mapper-two");

        createClientSession(session, realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state");
        createClientSession(session, realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state");

        sessions[1] = session.sessions().createUserSession(realm, session.users().getUserByUsername(realm, "user1"), "user1", "127.0.0.2", "form", true, null, null);
        createClientSession(session, realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state");

        sessions[2] = session.sessions().createUserSession(realm, session.users().getUserByUsername(realm, "user2"), "user2", "127.0.0.3", "form", true, null, null);
        createClientSession(session, realm.getClientByClientId("test-app"), sessions[2], "http://redirect", "state");

        return sessions;
    }

    public static void reloadState(KeycloakSession session) {
        reloadState(session, false);
    }

    public static void reloadState(KeycloakSession session, Boolean initialConfig) {
        currentSession = session;
        realm = currentSession.realms().getRealmByName("test");
        if (initialConfig) {
            currentSession.users().addUser(realm, "user1").setEmail("user1@localhost");
            currentSession.users().addUser(realm, "user2").setEmail("user2@localhost");
        }
        sessionManager = new UserSessionManager(currentSession);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }
}
