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
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.models.UserManager;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LoggingRule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserSessionProviderOfflineTest {

    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    @Rule
    public LoggingRule loggingRule = new LoggingRule(this);

    private KeycloakSession session;
    private RealmModel realm;
    private UserSessionManager sessionManager;
    private UserSessionPersisterProvider persister;

    @Before
    public void before() {
        session = kc.startSession();
        realm = session.realms().getRealm("test");
        session.users().addUser(realm, "user1").setEmail("user1@localhost");
        session.users().addUser(realm, "user2").setEmail("user2@localhost");
        sessionManager = new UserSessionManager(session);
        persister = session.getProvider(UserSessionPersisterProvider.class);
    }

    @After
    public void after() {
        resetSession();
        session.sessions().removeUserSessions(realm);
        UserModel user1 = session.users().getUserByUsername("user1", realm);
        UserModel user2 = session.users().getUserByUsername("user2", realm);

        UserManager um = new UserManager(session);
        um.removeUser(realm, user1);
        um.removeUser(realm, user2);
        kc.stopSession(session, true);
    }


    @Test
    public void testOfflineSessionsCrud() {
        // Create some online sessions in infinispan
        int started = Time.currentTime();
        UserSessionModel[] origSessions = createSessions();

        resetSession();

        // Key is userSession ID, values are client UUIDS
        Map<String, Set<String>> offlineSessions = new HashMap<>();

        // Persist 3 created userSessions and clientSessions as offline
        ClientModel testApp = realm.getClientByClientId("test-app");
        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, testApp);
        for (UserSessionModel userSession : userSessions) {
            offlineSessions.put(userSession.getId(), createOfflineSessionIncludeClientSessions(userSession));
        }

        resetSession();

        // Assert all previously saved offline sessions found
        for (Map.Entry<String, Set<String>> entry : offlineSessions.entrySet()) {
            UserSessionModel offlineSession = sessionManager.findOfflineUserSession(realm, entry.getKey());
            Assert.assertNotNull(offlineSession);
            Assert.assertEquals(offlineSession.getAuthenticatedClientSessions().keySet(), entry.getValue());
        }

        // Find clients with offline token
        UserModel user1 = session.users().getUserByUsername("user1", realm);
        Set<ClientModel> clients = sessionManager.findClientsWithOfflineToken(realm, user1);
        Assert.assertEquals(clients.size(), 2);
        for (ClientModel client : clients) {
            Assert.assertTrue(client.getClientId().equals("test-app") || client.getClientId().equals("third-party"));
        }

        UserModel user2 = session.users().getUserByUsername("user2", realm);
        clients = sessionManager.findClientsWithOfflineToken(realm, user2);
        Assert.assertEquals(clients.size(), 1);
        Assert.assertTrue(clients.iterator().next().getClientId().equals("test-app"));

        // Test count
        testApp = realm.getClientByClientId("test-app");
        ClientModel thirdparty = realm.getClientByClientId("third-party");
        Assert.assertEquals(3, session.sessions().getOfflineSessionsCount(realm, testApp));
        Assert.assertEquals(1, session.sessions().getOfflineSessionsCount(realm, thirdparty));

        // Revoke "test-app" for user1
        sessionManager.revokeOfflineToken(user1, testApp);

        resetSession();

        // Assert userSession revoked
        testApp = realm.getClientByClientId("test-app");
        thirdparty = realm.getClientByClientId("third-party");
        Assert.assertEquals(1, session.sessions().getOfflineSessionsCount(realm, testApp));
        Assert.assertEquals(1, session.sessions().getOfflineSessionsCount(realm, thirdparty));

        List<UserSessionModel> testAppSessions = session.sessions().getOfflineUserSessions(realm, testApp, 0, 10);
        List<UserSessionModel> thirdpartySessions = session.sessions().getOfflineUserSessions(realm, thirdparty, 0, 10);
        Assert.assertEquals(1, testAppSessions.size());
        Assert.assertEquals("127.0.0.3", testAppSessions.get(0).getIpAddress());
        Assert.assertEquals("user2", testAppSessions.get(0).getUser().getUsername());
        Assert.assertEquals(1, thirdpartySessions.size());
        Assert.assertEquals("127.0.0.1", thirdpartySessions.get(0).getIpAddress());
        Assert.assertEquals("user1", thirdpartySessions.get(0).getUser().getUsername());

        user1 = session.users().getUserByUsername("user1", realm);
        user2 = session.users().getUserByUsername("user2", realm);
        clients = sessionManager.findClientsWithOfflineToken(realm, user1);
        Assert.assertEquals(1, clients.size());
        Assert.assertEquals("third-party", clients.iterator().next().getClientId());
        clients = sessionManager.findClientsWithOfflineToken(realm, user2);
        Assert.assertEquals(1, clients.size());
        Assert.assertEquals("test-app", clients.iterator().next().getClientId());
    }

    @Test
    public void testOnRealmRemoved() {
        RealmModel fooRealm = session.realms().createRealm("foo", "foo");
        fooRealm.addClient("foo-app");
        session.users().addUser(fooRealm, "user3");

        UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), fooRealm, session.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
        AuthenticatedClientSessionModel clientSession = createClientSession(fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        resetSession();

        // Persist offline session
        fooRealm = session.realms().getRealm("foo");
        userSession = session.sessions().getUserSession(fooRealm, userSession.getId());
        createOfflineSessionIncludeClientSessions(userSession);

        resetSession();

        UserSessionModel offlineUserSession = sessionManager.findOfflineUserSession(fooRealm, userSession.getId());
        Assert.assertEquals(offlineUserSession.getAuthenticatedClientSessions().size(), 1);
        AuthenticatedClientSessionModel offlineClientSession = offlineUserSession.getAuthenticatedClientSessions().values().iterator().next();
        Assert.assertEquals("foo-app", offlineClientSession.getClient().getClientId());
        Assert.assertEquals("user3", offlineClientSession.getUserSession().getUser().getUsername());

        // Remove realm
        RealmManager realmMgr = new RealmManager(session);
        realmMgr.removeRealm(realmMgr.getRealm("foo"));

        resetSession();

        fooRealm = session.realms().createRealm("foo", "foo");
        fooRealm.addClient("foo-app");
        session.users().addUser(fooRealm, "user3");

        resetSession();

        // Assert nothing loaded
        fooRealm = session.realms().getRealm("foo");
        Assert.assertNull(sessionManager.findOfflineUserSession(fooRealm, userSession.getId()));
        Assert.assertEquals(0, session.sessions().getOfflineSessionsCount(fooRealm, fooRealm.getClientByClientId("foo-app")));

        // Cleanup
        realmMgr = new RealmManager(session);
        realmMgr.removeRealm(realmMgr.getRealm("foo"));
    }

    @Test
    public void testOnClientRemoved() {
        int started = Time.currentTime();

        RealmModel fooRealm = session.realms().createRealm("foo", "foo");
        fooRealm.addClient("foo-app");
        fooRealm.addClient("bar-app");
        session.users().addUser(fooRealm, "user3");

        UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), fooRealm, session.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
        createClientSession(fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state", new HashSet<String>(), new HashSet<String>());
        createClientSession(fooRealm.getClientByClientId("bar-app"), userSession, "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        resetSession();

        // Create offline session
        fooRealm = session.realms().getRealm("foo");
        userSession = session.sessions().getUserSession(fooRealm, userSession.getId());
        createOfflineSessionIncludeClientSessions(userSession);

        resetSession();

        RealmManager realmMgr = new RealmManager(session);
        ClientManager clientMgr = new ClientManager(realmMgr);
        fooRealm = realmMgr.getRealm("foo");

        // Assert session was persisted with both clientSessions
        UserSessionModel offlineSession = session.sessions().getOfflineUserSession(fooRealm, userSession.getId());
        UserSessionProviderTest.assertSession(offlineSession, session.users().getUserByUsername("user3", fooRealm), "127.0.0.1", started, started, "foo-app", "bar-app");

        // Remove foo-app client
        ClientModel client = fooRealm.getClientByClientId("foo-app");
        clientMgr.removeClient(fooRealm, client);

        resetSession();

        realmMgr = new RealmManager(session);
        clientMgr = new ClientManager(realmMgr);
        fooRealm = realmMgr.getRealm("foo");

        // Assert just one bar-app clientSession persisted now
        offlineSession = session.sessions().getOfflineUserSession(fooRealm, userSession.getId());
        Assert.assertEquals(1, offlineSession.getAuthenticatedClientSessions().size());
        Assert.assertEquals("bar-app", offlineSession.getAuthenticatedClientSessions().values().iterator().next().getClient().getClientId());

        // Remove bar-app client
        client = fooRealm.getClientByClientId("bar-app");
        clientMgr.removeClient(fooRealm, client);

        resetSession();

        // Assert nothing loaded - userSession was removed as well because it was last userSession
        realmMgr = new RealmManager(session);
        fooRealm = realmMgr.getRealm("foo");
        offlineSession = session.sessions().getOfflineUserSession(fooRealm, userSession.getId());
        Assert.assertEquals(0, offlineSession.getAuthenticatedClientSessions().size());

        // Cleanup
        realmMgr = new RealmManager(session);
        realmMgr.removeRealm(realmMgr.getRealm("foo"));
    }

    @Test
    public void testOnUserRemoved() {
        int started = Time.currentTime();

        RealmModel fooRealm = session.realms().createRealm("foo", "foo");
        fooRealm.addClient("foo-app");
        session.users().addUser(fooRealm, "user3");

        UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), fooRealm, session.users().getUserByUsername("user3", fooRealm), "user3", "127.0.0.1", "form", true, null, null);
        AuthenticatedClientSessionModel clientSession = createClientSession(fooRealm.getClientByClientId("foo-app"), userSession, "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        resetSession();

        // Create offline session
        fooRealm = session.realms().getRealm("foo");
        userSession = session.sessions().getUserSession(fooRealm, userSession.getId());
        createOfflineSessionIncludeClientSessions(userSession);

        resetSession();

        RealmManager realmMgr = new RealmManager(session);
        fooRealm = realmMgr.getRealm("foo");
        UserModel user3 = session.users().getUserByUsername("user3", fooRealm);

        // Assert session was persisted with both clientSessions
        UserSessionModel offlineSession = session.sessions().getOfflineUserSession(fooRealm, userSession.getId());
        UserSessionProviderTest.assertSession(offlineSession, user3, "127.0.0.1", started, started, "foo-app");

        // Remove user3
        new UserManager(session).removeUser(fooRealm, user3);

        resetSession();

        // Assert userSession removed as well
        Assert.assertNull(session.sessions().getOfflineUserSession(fooRealm, userSession.getId()));

        // Cleanup
        realmMgr = new RealmManager(session);
        realmMgr.removeRealm(realmMgr.getRealm("foo"));

    }

    @Test
    public void testExpired() {
        // Create some online sessions in infinispan
        int started = Time.currentTime();
        UserSessionModel[] origSessions = createSessions();

        resetSession();

        // Key is userSessionId, value is set of client UUIDS
        Map<String, Set<String>> offlineSessions = new HashMap<>();

        // Persist 3 created userSessions and clientSessions as offline
        ClientModel testApp = realm.getClientByClientId("test-app");
        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, testApp);
        for (UserSessionModel userSession : userSessions) {
            offlineSessions.put(userSession.getId(), createOfflineSessionIncludeClientSessions(userSession));
        }

        resetSession();

        // Assert all previously saved offline sessions found
        for (Map.Entry<String, Set<String>> entry : offlineSessions.entrySet()) {
            UserSessionModel foundSession = sessionManager.findOfflineUserSession(realm, entry.getKey());
            Assert.assertEquals(foundSession.getAuthenticatedClientSessions().keySet(), entry.getValue());
        }

        UserSessionModel session0 = session.sessions().getOfflineUserSession(realm, origSessions[0].getId());
        Assert.assertNotNull(session0);

        // sessions are in persister too
        Assert.assertEquals(3, persister.getUserSessionsCount(true));

        // Set lastSessionRefresh to session[0] to 0
        session0.setLastSessionRefresh(0);

        resetSession();

        session.sessions().removeExpired(realm);

        resetSession();

        // assert session0 not found now
        Assert.assertNull(session.sessions().getOfflineUserSession(realm, origSessions[0].getId()));

        Assert.assertEquals(2, persister.getUserSessionsCount(true));

        // Expire everything and assert nothing found
        Time.setOffset(3000000);
        try {
            session.sessions().removeExpired(realm);

            resetSession();

            for (String userSessionId : offlineSessions.keySet()) {
                Assert.assertNull(sessionManager.findOfflineUserSession(realm, userSessionId));
            }
            Assert.assertEquals(0, persister.getUserSessionsCount(true));

        } finally {
            Time.setOffset(0);
        }
    }

    private Set<String> createOfflineSessionIncludeClientSessions(UserSessionModel userSession) {
        Set<String> offlineSessions = new HashSet<>();

        for (AuthenticatedClientSessionModel clientSession : userSession.getAuthenticatedClientSessions().values()) {
            sessionManager.createOrUpdateOfflineSession(clientSession, userSession);
            offlineSessions.add(clientSession.getClient().getId());
        }
        return offlineSessions;
    }


    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        realm = session.realms().getRealm("test");
        sessionManager = new UserSessionManager(session);
        persister = session.getProvider(UserSessionPersisterProvider.class);
    }

    private AuthenticatedClientSessionModel createClientSession(ClientModel client, UserSessionModel userSession, String redirect, String state, Set<String> roles, Set<String> protocolMappers) {
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(client.getRealm(), client, userSession);
        if (userSession != null) clientSession.setUserSession(userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        if (roles != null) clientSession.setRoles(roles);
        if (protocolMappers != null) clientSession.setProtocolMappers(protocolMappers);
        return clientSession;
    }

    private UserSessionModel[] createSessions() {
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null);

        Set<String> roles = new HashSet<String>();
        roles.add("one");
        roles.add("two");

        Set<String> protocolMappers = new HashSet<String>();
        protocolMappers.add("mapper-one");
        protocolMappers.add("mapper-two");

        createClientSession(realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state", roles, protocolMappers);
        createClientSession(realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        sessions[1] = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true, null, null);
        createClientSession(realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        sessions[2] = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.3", "form", true, null, null);
        createClientSession(realm.getClientByClientId("test-app"), sessions[2], "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        return sessions;
    }
}
