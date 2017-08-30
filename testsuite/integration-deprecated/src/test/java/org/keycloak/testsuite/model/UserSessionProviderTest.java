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
import org.keycloak.common.util.Time;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.models.UserManager;
import org.keycloak.testsuite.rule.KeycloakRule;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class UserSessionProviderTest {

    @ClassRule
    public static KeycloakRule kc = new KeycloakRule();

    private KeycloakSession session;
    private RealmModel realm;

    @Before
    public void before() {
        session = kc.startSession();
        realm = session.realms().getRealm("test");
        session.users().addUser(realm, "user1").setEmail("user1@localhost");
        session.users().addUser(realm, "user2").setEmail("user2@localhost");
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
    public void testCreateSessions() {
        int started = Time.currentTime();
        UserSessionModel[] sessions = createSessions();

        assertSession(session.sessions().getUserSession(realm, sessions[0].getId()), session.users().getUserByUsername("user1", realm), "127.0.0.1", started, started, "test-app", "third-party");
        assertSession(session.sessions().getUserSession(realm, sessions[1].getId()), session.users().getUserByUsername("user1", realm), "127.0.0.2", started, started, "test-app");
        assertSession(session.sessions().getUserSession(realm, sessions[2].getId()), session.users().getUserByUsername("user2", realm), "127.0.0.3", started, started, "test-app");
    }

    @Test
    public void testUpdateSession() {
        UserSessionModel[] sessions = createSessions();
        session.sessions().getUserSession(realm, sessions[0].getId()).setLastSessionRefresh(1000);

        resetSession();

        assertEquals(1000, session.sessions().getUserSession(realm, sessions[0].getId()).getLastSessionRefresh());
    }

    @Test
    public void testUpdateSessionInSameTransaction() {
        UserSessionModel[] sessions = createSessions();
        session.sessions().getUserSession(realm, sessions[0].getId()).setLastSessionRefresh(1000);
        assertEquals(1000, session.sessions().getUserSession(realm, sessions[0].getId()).getLastSessionRefresh());
    }

    @Test
    public void testRestartSession() {
        int started = Time.currentTime();
        UserSessionModel[] sessions = createSessions();

        Time.setOffset(100);

        UserSessionModel userSession = session.sessions().getUserSession(realm, sessions[0].getId());
        assertSession(userSession, session.users().getUserByUsername("user1", realm), "127.0.0.1", started, started, "test-app", "third-party");

        userSession.restartSession(realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.6", "form", true, null, null);

        resetSession();

        userSession = session.sessions().getUserSession(realm, sessions[0].getId());
        assertSession(userSession, session.users().getUserByUsername("user2", realm), "127.0.0.6", started + 100, started + 100);

        Time.setOffset(0);
    }

    @Test
    public void testCreateClientSession() {
        UserSessionModel[] sessions = createSessions();

        Map<String, AuthenticatedClientSessionModel> clientSessions = session.sessions().getUserSession(realm, sessions[0].getId()).getAuthenticatedClientSessions();
        assertEquals(2, clientSessions.size());

        String clientUUID = realm.getClientByClientId("test-app").getId();

        AuthenticatedClientSessionModel session1 = clientSessions.get(clientUUID);

        assertEquals(null, session1.getAction());
        assertEquals(realm.getClientByClientId("test-app").getClientId(), session1.getClient().getClientId());
        assertEquals(sessions[0].getId(), session1.getUserSession().getId());
        assertEquals("http://redirect", session1.getRedirectUri());
        assertEquals("state", session1.getNote(OIDCLoginProtocol.STATE_PARAM));
        assertEquals(2, session1.getRoles().size());
        assertTrue(session1.getRoles().contains("one"));
        assertTrue(session1.getRoles().contains("two"));
        assertEquals(2, session1.getProtocolMappers().size());
        assertTrue(session1.getProtocolMappers().contains("mapper-one"));
        assertTrue(session1.getProtocolMappers().contains("mapper-two"));
    }

    @Test
    public void testUpdateClientSession() {
        UserSessionModel[] sessions = createSessions();

        String userSessionId = sessions[0].getId();
        String clientUUID = realm.getClientByClientId("test-app").getId();

        UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(clientUUID);

        int time = clientSession.getTimestamp();
        assertEquals(null, clientSession.getAction());

        clientSession.setAction(AuthenticatedClientSessionModel.Action.CODE_TO_TOKEN.name());
        clientSession.setTimestamp(time + 10);

        kc.stopSession(session, true);
        session = kc.startSession();

        AuthenticatedClientSessionModel updated = session.sessions().getUserSession(realm, userSessionId).getAuthenticatedClientSessions().get(clientUUID);
        assertEquals(AuthenticatedClientSessionModel.Action.CODE_TO_TOKEN.name(), updated.getAction());
        assertEquals(time + 10, updated.getTimestamp());
    }

    @Test
    public void testUpdateClientSessionInSameTransaction() {
        UserSessionModel[] sessions = createSessions();

        String userSessionId = sessions[0].getId();
        String clientUUID = realm.getClientByClientId("test-app").getId();

        UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
        AuthenticatedClientSessionModel clientSession = userSession.getAuthenticatedClientSessions().get(clientUUID);

        clientSession.setAction(AuthenticatedClientSessionModel.Action.CODE_TO_TOKEN.name());
        clientSession.setNote("foo", "bar");

        AuthenticatedClientSessionModel updated = session.sessions().getUserSession(realm, userSessionId).getAuthenticatedClientSessions().get(clientUUID);
        assertEquals(AuthenticatedClientSessionModel.Action.CODE_TO_TOKEN.name(), updated.getAction());
        assertEquals("bar", updated.getNote("foo"));
    }

    @Test
    public void testGetUserSessions() {
        UserSessionModel[] sessions = createSessions();

        assertSessions(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user1", realm)), sessions[0], sessions[1]);
        assertSessions(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user2", realm)), sessions[2]);
    }

    @Test
    public void testRemoveUserSessionsByUser() {
        UserSessionModel[] sessions = createSessions();

        Map<String, Integer> clientSessionsKept = new HashMap<>();
        for (UserSessionModel s : sessions) {
            s = session.sessions().getUserSession(realm, s.getId());

            if (!s.getUser().getUsername().equals("user1")) {
                clientSessionsKept.put(s.getId(),  s.getAuthenticatedClientSessions().keySet().size());
            }
        }

        session.sessions().removeUserSessions(realm, session.users().getUserByUsername("user1", realm));
        resetSession();

        assertTrue(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user1", realm)).isEmpty());
        List<UserSessionModel> userSessions = session.sessions().getUserSessions(realm, session.users().getUserByUsername("user2", realm));
        assertFalse(userSessions.isEmpty());

        Assert.assertEquals(userSessions.size(), clientSessionsKept.size());
        for (UserSessionModel userSession : userSessions) {
            Assert.assertEquals((int) clientSessionsKept.get(userSession.getId()), userSession.getAuthenticatedClientSessions().size());
        }
    }

    @Test
    public void testRemoveUserSession() {
        UserSessionModel userSession = createSessions()[0];

        session.sessions().removeUserSession(realm, userSession);
        resetSession();

        assertNull(session.sessions().getUserSession(realm, userSession.getId()));
    }

    @Test
    public void testRemoveUserSessionsByRealm() {
        UserSessionModel[] sessions = createSessions();

        session.sessions().removeUserSessions(realm);
        resetSession();

        assertTrue(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user1", realm)).isEmpty());
        assertTrue(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user2", realm)).isEmpty());
    }

    @Test
    public void testOnClientRemoved() {
        UserSessionModel[] sessions = createSessions();

        String thirdPartyClientUUID = realm.getClientByClientId("third-party").getId();

        Map<String, Set<String>> clientSessionsKept = new HashMap<>();
        for (UserSessionModel s : sessions) {
            Set<String> clientUUIDS = new HashSet<>(s.getAuthenticatedClientSessions().keySet());
            clientUUIDS.remove(thirdPartyClientUUID); // This client will be later removed, hence his clientSessions too
            clientSessionsKept.put(s.getId(), clientUUIDS);
        }

        realm.removeClient(thirdPartyClientUUID);
        resetSession();

        for (UserSessionModel s : sessions) {
            s = session.sessions().getUserSession(realm, s.getId());
            Set<String> clientUUIDS = s.getAuthenticatedClientSessions().keySet();
            assertEquals(clientUUIDS, clientSessionsKept.get(s.getId()));
        }

        // Revert client
        realm.addClient("third-party");
    }

    @Test
    public void testRemoveUserSessionsByExpired() {
        session.sessions().getUserSessions(realm, session.users().getUserByUsername("user1", realm));
        ClientModel client = realm.getClientByClientId("test-app");

        try {
            Set<String> expired = new HashSet<String>();

            Time.setOffset(-(realm.getSsoSessionMaxLifespan() + 1));
            UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null);
            expired.add(userSession.getId());
            AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
            Assert.assertEquals(userSession, clientSession.getUserSession());

            Time.setOffset(0);
            UserSessionModel s = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.1", "form", true, null, null);
            //s.setLastSessionRefresh(Time.currentTime() - (realm.getSsoSessionIdleTimeout() + 1));
            s.setLastSessionRefresh(0);
            expired.add(s.getId());

            Set<String> valid = new HashSet<String>();
            Set<String> validClientSessions = new HashSet<String>();

            userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null);
            valid.add(userSession.getId());
            validClientSessions.add(session.sessions().createClientSession(realm, client, userSession).getId());

            resetSession();

            session.sessions().removeExpired(realm);
            resetSession();

            for (String e : expired) {
                assertNull(session.sessions().getUserSession(realm, e));
            }

            for (String v : valid) {
                UserSessionModel userSessionLoaded = session.sessions().getUserSession(realm, v);
                assertNotNull(userSessionLoaded);
                Assert.assertEquals(1, userSessionLoaded.getAuthenticatedClientSessions().size());
                Assert.assertNotNull(userSessionLoaded.getAuthenticatedClientSessions().get(client.getId()));
            }
        } finally {
            Time.setOffset(0);
        }
    }

    // KEYCLOAK-2508
    @Test
    public void testRemovingExpiredSession() {
        UserSessionModel[] sessions = createSessions();
        try {
            Time.setOffset(3600000);
            UserSessionModel userSession = sessions[0];
            RealmModel realm = userSession.getRealm();
            session.sessions().removeExpired(realm);

            resetSession();

            // Assert no exception is thrown here
            session.sessions().removeUserSession(realm, userSession);
        } finally {
            Time.setOffset(0);
        }
    }

    @Test
    public void testGetByClient() {
        UserSessionModel[] sessions = createSessions();

        assertSessions(session.sessions().getUserSessions(realm, realm.getClientByClientId("test-app")), sessions[0], sessions[1], sessions[2]);
        assertSessions(session.sessions().getUserSessions(realm, realm.getClientByClientId("third-party")), sessions[0]);
    }

    @Test
    public void testGetByClientPaginated() {
        try {
            for (int i = 0; i < 25; i++) {
                Time.setOffset(i);
                UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0." + i, "form", false, null, null);
                AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, realm.getClientByClientId("test-app"), userSession);
                clientSession.setUserSession(userSession);
                clientSession.setRedirectUri("http://redirect");
                clientSession.setRoles(new HashSet<String>());
                clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, "state");
                clientSession.setTimestamp(userSession.getStarted());
                userSession.setLastSessionRefresh(userSession.getStarted());
            }
        } finally {
            Time.setOffset(0);
        }

        resetSession();

        assertPaginatedSession(realm, realm.getClientByClientId("test-app"), 0, 1, 1);
        assertPaginatedSession(realm, realm.getClientByClientId("test-app"), 0, 10, 10);
        assertPaginatedSession(realm, realm.getClientByClientId("test-app"), 10, 10, 10);
        assertPaginatedSession(realm, realm.getClientByClientId("test-app"), 20, 10, 5);
        assertPaginatedSession(realm, realm.getClientByClientId("test-app"), 30, 10, 0);
    }

    @Test
    public void testCreateAndGetInSameTransaction() {
        ClientModel client = realm.getClientByClientId("test-app");
        UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true, null, null);
        AuthenticatedClientSessionModel clientSession = createClientSession(client, userSession, "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        UserSessionModel userSessionLoaded = session.sessions().getUserSession(realm, userSession.getId());
        AuthenticatedClientSessionModel clientSessionLoaded = userSessionLoaded.getAuthenticatedClientSessions().get(client.getId());
        Assert.assertNotNull(userSessionLoaded);
        Assert.assertNotNull(clientSessionLoaded);

        Assert.assertEquals(userSession.getId(), clientSessionLoaded.getUserSession().getId());
        Assert.assertEquals(1, userSessionLoaded.getAuthenticatedClientSessions().size());
    }

    @Test
    public void testAuthenticatedClientSessions() {
        UserSessionModel userSession = session.sessions().createUserSession(KeycloakModelUtils.generateId(), realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true, null, null);

        ClientModel client1 = realm.getClientByClientId("test-app");
        ClientModel client2 = realm.getClientByClientId("third-party");

        // Create client1 session
        AuthenticatedClientSessionModel clientSession1 = session.sessions().createClientSession(realm, client1, userSession);
        clientSession1.setAction("foo1");
        clientSession1.setTimestamp(100);

        // Create client2 session
        AuthenticatedClientSessionModel clientSession2 = session.sessions().createClientSession(realm, client2, userSession);
        clientSession2.setAction("foo2");
        clientSession2.setTimestamp(200);

        // commit
        resetSession();

        // Ensure sessions are here
        userSession = session.sessions().getUserSession(realm, userSession.getId());
        Map<String, AuthenticatedClientSessionModel> clientSessions = userSession.getAuthenticatedClientSessions();
        Assert.assertEquals(2, clientSessions.size());
        testAuthenticatedClientSession(clientSessions.get(client1.getId()), "test-app", userSession.getId(), "foo1", 100);
        testAuthenticatedClientSession(clientSessions.get(client2.getId()), "third-party", userSession.getId(), "foo2", 200);

        // Update session1
        clientSessions.get(client1.getId()).setAction("foo1-updated");

        // commit
        resetSession();

        // Ensure updated
        userSession = session.sessions().getUserSession(realm, userSession.getId());
        clientSessions = userSession.getAuthenticatedClientSessions();
        testAuthenticatedClientSession(clientSessions.get(client1.getId()), "test-app", userSession.getId(), "foo1-updated", 100);

        // Rewrite session2
        clientSession2 = session.sessions().createClientSession(realm, client2, userSession);
        clientSession2.setAction("foo2-rewrited");
        clientSession2.setTimestamp(300);

        // commit
        resetSession();

        // Ensure updated
        userSession = session.sessions().getUserSession(realm, userSession.getId());
        clientSessions = userSession.getAuthenticatedClientSessions();
        Assert.assertEquals(2, clientSessions.size());
        testAuthenticatedClientSession(clientSessions.get(client1.getId()), "test-app", userSession.getId(), "foo1-updated", 100);
        testAuthenticatedClientSession(clientSessions.get(client2.getId()), "third-party", userSession.getId(), "foo2-rewrited", 300);

        // remove session
        clientSession1 = userSession.getAuthenticatedClientSessions().get(client1.getId());
        clientSession1.setUserSession(null);

        // Commit and ensure removed
        resetSession();

        userSession = session.sessions().getUserSession(realm, userSession.getId());
        clientSessions = userSession.getAuthenticatedClientSessions();
        Assert.assertEquals(1, clientSessions.size());
        Assert.assertNull(clientSessions.get(client1.getId()));
    }


    private void testAuthenticatedClientSession(AuthenticatedClientSessionModel clientSession, String expectedClientId, String expectedUserSessionId, String expectedAction, int expectedTimestamp) {
        Assert.assertEquals(expectedClientId, clientSession.getClient().getClientId());
        Assert.assertEquals(expectedUserSessionId, clientSession.getUserSession().getId());
        Assert.assertEquals(expectedAction, clientSession.getAction());
        Assert.assertEquals(expectedTimestamp, clientSession.getTimestamp());
    }

    private void assertPaginatedSession(RealmModel realm, ClientModel client, int start, int max, int expectedSize) {
        List<UserSessionModel> sessions = session.sessions().getUserSessions(realm, client, start, max);
        String[] actualIps = new String[sessions.size()];
        for (int i = 0; i < actualIps.length; i++) {
            actualIps[i] = sessions.get(i).getIpAddress();
        }

        String[] expectedIps = new String[expectedSize];
        for (int i = 0; i < expectedSize; i++) {
            expectedIps[i] = "127.0.0." + (i + start);
        }

        assertArrayEquals(expectedIps, actualIps);
    }

    @Test
    public void testGetCountByClient() {
        createSessions();

        assertEquals(3, session.sessions().getActiveUserSessions(realm, realm.getClientByClientId("test-app")));
        assertEquals(1, session.sessions().getActiveUserSessions(realm, realm.getClientByClientId("third-party")));
    }

    @Test
    public void loginFailures() {
        UserLoginFailureModel failure1 = session.sessions().addUserLoginFailure(realm, "user1");
        failure1.incrementFailures();

        UserLoginFailureModel failure2 = session.sessions().addUserLoginFailure(realm, "user2");
        failure2.incrementFailures();
        failure2.incrementFailures();

        resetSession();

        failure1 = session.sessions().getUserLoginFailure(realm, "user1");
        assertEquals(1, failure1.getNumFailures());

        failure2 = session.sessions().getUserLoginFailure(realm, "user2");
        assertEquals(2, failure2.getNumFailures());

        resetSession();

        // Add the failure, which already exists
        failure1 = session.sessions().addUserLoginFailure(realm, "user1");
        failure1.incrementFailures();

        resetSession();

        failure1 = session.sessions().getUserLoginFailure(realm, "user1");
        assertEquals(2, failure1.getNumFailures());

        failure1 = session.sessions().getUserLoginFailure(realm, "user1");
        failure1.clearFailures();

        resetSession();

        failure1 = session.sessions().getUserLoginFailure(realm, "user1");
        assertEquals(0, failure1.getNumFailures());

        session.sessions().removeUserLoginFailure(realm, "user1");

        resetSession();

        assertNull(session.sessions().getUserLoginFailure(realm, "user1"));

        session.sessions().removeAllUserLoginFailures(realm);

        resetSession();

        assertNull(session.sessions().getUserLoginFailure(realm, "user2"));
    }

    @Test
    public void testOnUserRemoved() {
        createSessions();

        UserModel user1 = session.users().getUserByUsername("user1", realm);
        UserModel user2 = session.users().getUserByUsername("user2", realm);

        session.sessions().addUserLoginFailure(realm, user1.getId());
        session.sessions().addUserLoginFailure(realm, user2.getId());

        resetSession();

        user1 = session.users().getUserByUsername("user1", realm);
        new UserManager(session).removeUser(realm, user1);

        resetSession();

        assertTrue(session.sessions().getUserSessions(realm, user1).isEmpty());
        assertFalse(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user2", realm)).isEmpty());

        assertNull(session.sessions().getUserLoginFailure(realm, user1.getId()));
        assertNotNull(session.sessions().getUserLoginFailure(realm, user2.getId()));
    }

    private AuthenticatedClientSessionModel createClientSession(ClientModel client, UserSessionModel userSession, String redirect, String state, Set<String> roles, Set<String> protocolMappers) {
        AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, client, userSession);
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

        resetSession();

        return sessions;
    }

    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        realm = session.realms().getRealm("test");
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

    public static void assertSession(UserSessionModel session, UserModel user, String ipAddress, int started, int lastRefresh, String... clients) {
        assertEquals(user.getId(), session.getUser().getId());
        assertEquals(ipAddress, session.getIpAddress());
        assertEquals(user.getUsername(), session.getLoginUsername());
        assertEquals("form", session.getAuthMethod());
        assertEquals(true, session.isRememberMe());
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

        Arrays.sort(clients);
        Arrays.sort(actualClients);

        assertArrayEquals(clients, actualClients);
    }
}
