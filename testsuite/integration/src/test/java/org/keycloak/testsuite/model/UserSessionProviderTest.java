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
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.managers.UserManager;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.common.util.Time;

import java.util.*;

import static org.junit.Assert.*;

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
        um.removeUser(realm, user1);
        um.removeUser(realm, user2);
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
    public void testCreateClientSession() {
        UserSessionModel[] sessions = createSessions();

        List<ClientSessionModel> clientSessions = session.sessions().getUserSession(realm, sessions[0].getId()).getClientSessions();
        assertEquals(2, clientSessions.size());

        String client1 = realm.getClientByClientId("test-app").getId();

        ClientSessionModel session1;

        if (clientSessions.get(0).getClient().getId().equals(client1)) {
            session1 = clientSessions.get(0);
        } else {
            session1 = clientSessions.get(1);
        }

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

        String id = sessions[0].getClientSessions().get(0).getId();

        ClientSessionModel clientSession = session.sessions().getClientSession(realm, id);

        int time = clientSession.getTimestamp();
        assertEquals(null, clientSession.getAction());

        clientSession.setAction(ClientSessionModel.Action.CODE_TO_TOKEN.name());
        clientSession.setTimestamp(time + 10);

        kc.stopSession(session, true);
        session = kc.startSession();

        ClientSessionModel updated = session.sessions().getClientSession(realm, id);
        assertEquals(ClientSessionModel.Action.CODE_TO_TOKEN.name(), updated.getAction());
        assertEquals(time + 10, updated.getTimestamp());
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

        List<String> clientSessionsRemoved = new LinkedList<String>();
        List<String> clientSessionsKept = new LinkedList<String>();
        for (UserSessionModel s : sessions) {
            s = session.sessions().getUserSession(realm, s.getId());

            for (ClientSessionModel c : s.getClientSessions()) {
                if (c.getUserSession().getUser().getUsername().equals("user1")) {
                    clientSessionsRemoved.add(c.getId());
                } else {
                    clientSessionsKept.add(c.getId());
                }
            }
        }

        session.sessions().removeUserSessions(realm, session.users().getUserByUsername("user1", realm));
        resetSession();

        assertTrue(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user1", realm)).isEmpty());
        assertFalse(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user2", realm)).isEmpty());

        for (String c : clientSessionsRemoved) {
            assertNull(session.sessions().getClientSession(realm, c));
        }
        for (String c : clientSessionsKept) {
            assertNotNull(session.sessions().getClientSession(realm, c));
        }
    }

    @Test
    public void testRemoveUserSession() {
        UserSessionModel userSession = createSessions()[0];

        List<String> clientSessionsRemoved = new LinkedList<String>();
        for (ClientSessionModel c : userSession.getClientSessions()) {
            clientSessionsRemoved.add(c.getId());
        }

        session.sessions().removeUserSession(realm, userSession);
        resetSession();

        assertNull(session.sessions().getUserSession(realm, userSession.getId()));
        for (String c : clientSessionsRemoved) {
            assertNull(session.sessions().getClientSession(realm, c));
        }
    }

    @Test
    public void testRemoveUserSessionsByRealm() {
        UserSessionModel[] sessions = createSessions();

        List<ClientSessionModel> clientSessions = new LinkedList<ClientSessionModel>();
        for (UserSessionModel s : sessions) {
            clientSessions.addAll(s.getClientSessions());
        }

        session.sessions().removeUserSessions(realm);
        resetSession();

        assertTrue(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user1", realm)).isEmpty());
        assertTrue(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user2", realm)).isEmpty());

        for (ClientSessionModel c : clientSessions) {
            assertNull(session.sessions().getClientSession(realm, c.getId()));
        }
    }

    @Test
    public void testOnClientRemoved() {
        UserSessionModel[] sessions = createSessions();

        List<String> clientSessionsRemoved = new LinkedList<String>();
        List<String> clientSessionsKept = new LinkedList<String>();
        for (UserSessionModel s : sessions) {
            s = session.sessions().getUserSession(realm, s.getId());
            for (ClientSessionModel c : s.getClientSessions()) {
                if (c.getClient().getClientId().equals("third-party")) {
                    clientSessionsRemoved.add(c.getId());
                } else {
                    clientSessionsKept.add(c.getId());
                }
            }
        }

        session.sessions().onClientRemoved(realm, realm.getClientByClientId("third-party"));
        resetSession();

        for (String c : clientSessionsRemoved) {
            assertNull(session.sessions().getClientSession(realm, c));
        }
        for (String c : clientSessionsKept) {
            assertNotNull(session.sessions().getClientSession(realm, c));
        }

        session.sessions().onClientRemoved(realm, realm.getClientByClientId("test-app"));
        resetSession();

        for (String c : clientSessionsRemoved) {
            assertNull(session.sessions().getClientSession(realm, c));
        }
        for (String c : clientSessionsKept) {
            assertNull(session.sessions().getClientSession(realm, c));
        }
    }

    @Test
    public void testRemoveUserSessionsByExpired() {
        session.sessions().getUserSessions(realm, session.users().getUserByUsername("user1", realm));
        ClientModel client = realm.getClientByClientId("test-app");

        try {
            Set<String> expired = new HashSet<String>();
            Set<String> expiredClientSessions = new HashSet<String>();

            Time.setOffset(-(realm.getSsoSessionMaxLifespan() + 1));
            expired.add(session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null).getId());
            expiredClientSessions.add(session.sessions().createClientSession(realm, client).getId());

            Time.setOffset(0);
            UserSessionModel s = session.sessions().createUserSession(realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.1", "form", true, null, null);
            //s.setLastSessionRefresh(Time.currentTime() - (realm.getSsoSessionIdleTimeout() + 1));
            s.setLastSessionRefresh(0);
            expired.add(s.getId());

            ClientSessionModel clSession = session.sessions().createClientSession(realm, client);
            clSession.setUserSession(s);
            expiredClientSessions.add(clSession.getId());

            Set<String> valid = new HashSet<String>();
            Set<String> validClientSessions = new HashSet<String>();

            valid.add(session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null).getId());
            validClientSessions.add(session.sessions().createClientSession(realm, client).getId());

            resetSession();

            session.sessions().removeExpired(realm);
            resetSession();

            for (String e : expired) {
                assertNull(session.sessions().getUserSession(realm, e));
            }
            for (String e : expiredClientSessions) {
                assertNull(session.sessions().getClientSession(realm, e));
            }

            for (String v : valid) {
                assertNotNull(session.sessions().getUserSession(realm, v));
            }
            for (String e : validClientSessions) {
                assertNotNull(session.sessions().getClientSession(realm, e));
            }
        } finally {
            Time.setOffset(0);
        }
    }

    @Test
    public void testExpireDetachedClientSessions() {
        try {
            realm.setAccessCodeLifespan(10);
            realm.setAccessCodeLifespanUserAction(10);
            realm.setAccessCodeLifespanLogin(30);

            // Login lifespan is largest
            String clientSessionId = session.sessions().createClientSession(realm, realm.getClientByClientId("test-app")).getId();
            resetSession();

            Time.setOffset(25);
            session.sessions().removeExpired(realm);
            resetSession();

            assertNotNull(session.sessions().getClientSession(clientSessionId));

            Time.setOffset(35);
            session.sessions().removeExpired(realm);
            resetSession();

            assertNull(session.sessions().getClientSession(clientSessionId));

            // User action is largest
            realm.setAccessCodeLifespanUserAction(40);

            Time.setOffset(0);
            clientSessionId = session.sessions().createClientSession(realm, realm.getClientByClientId("test-app")).getId();
            resetSession();

            Time.setOffset(35);
            session.sessions().removeExpired(realm);
            resetSession();

            assertNotNull(session.sessions().getClientSession(clientSessionId));

            Time.setOffset(45);
            session.sessions().removeExpired(realm);
            resetSession();

            assertNull(session.sessions().getClientSession(clientSessionId));

            // Access code is largest
            realm.setAccessCodeLifespan(50);

            Time.setOffset(0);
            clientSessionId = session.sessions().createClientSession(realm, realm.getClientByClientId("test-app")).getId();
            resetSession();

            Time.setOffset(45);
            session.sessions().removeExpired(realm);
            resetSession();

            assertNotNull(session.sessions().getClientSession(clientSessionId));

            Time.setOffset(55);
            session.sessions().removeExpired(realm);
            resetSession();

            assertNull(session.sessions().getClientSession(clientSessionId));
        } finally {
            Time.setOffset(0);

            realm.setAccessCodeLifespan(60);
            realm.setAccessCodeLifespanUserAction(300);
            realm.setAccessCodeLifespanLogin(1800);

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
                UserSessionModel userSession = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0." + i, "form", false, null, null);
                ClientSessionModel clientSession = session.sessions().createClientSession(realm, realm.getClientByClientId("test-app"));
                clientSession.setUserSession(userSession);
                clientSession.setRedirectUri("http://redirect");
                clientSession.setRoles(new HashSet<String>());
                clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, "state");
                clientSession.setTimestamp(userSession.getStarted());
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
        UserSessionModel userSession = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true, null, null);
        ClientSessionModel clientSession = createClientSession(realm.getClientByClientId("test-app"), userSession, "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        Assert.assertNotNull(session.sessions().getUserSession(realm, userSession.getId()));
        Assert.assertNotNull(session.sessions().getClientSession(realm, clientSession.getId()));

        Assert.assertEquals(userSession.getId(), clientSession.getUserSession().getId());
        Assert.assertEquals(1, userSession.getClientSessions().size());
        Assert.assertEquals(clientSession.getId(), userSession.getClientSessions().get(0).getId());
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

        session.sessions().addUserLoginFailure(realm, "user1");
        session.sessions().addUserLoginFailure(realm, "user1@localhost");
        session.sessions().addUserLoginFailure(realm, "user2");

        resetSession();

        session.sessions().onUserRemoved(realm, session.users().getUserByUsername("user1", realm));

        resetSession();

        assertTrue(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user1", realm)).isEmpty());
        assertFalse(session.sessions().getUserSessions(realm, session.users().getUserByUsername("user2", realm)).isEmpty());

        assertNull(session.sessions().getUserLoginFailure(realm, "user1"));
        assertNull(session.sessions().getUserLoginFailure(realm, "user1@localhost"));
        assertNotNull(session.sessions().getUserLoginFailure(realm, "user2"));
    }

    private ClientSessionModel createClientSession(ClientModel client, UserSessionModel userSession, String redirect, String state, Set<String> roles, Set<String> protocolMappers) {
        ClientSessionModel clientSession = session.sessions().createClientSession(realm, client);
        if (userSession != null) clientSession.setUserSession(userSession);
        clientSession.setRedirectUri(redirect);
        if (state != null) clientSession.setNote(OIDCLoginProtocol.STATE_PARAM, state);
        if (roles != null) clientSession.setRoles(roles);
        if (protocolMappers != null) clientSession.setProtocolMappers(protocolMappers);
        return clientSession;
    }

    private UserSessionModel[] createSessions() {
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true, null, null);

        Set<String> roles = new HashSet<String>();
        roles.add("one");
        roles.add("two");

        Set<String> protocolMappers = new HashSet<String>();
        protocolMappers.add("mapper-one");
        protocolMappers.add("mapper-two");

        createClientSession(realm.getClientByClientId("test-app"), sessions[0], "http://redirect", "state", roles, protocolMappers);
        createClientSession(realm.getClientByClientId("third-party"), sessions[0], "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        sessions[1] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true, null, null);
        createClientSession(realm.getClientByClientId("test-app"), sessions[1], "http://redirect", "state", new HashSet<String>(), new HashSet<String>());

        sessions[2] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.3", "form", true, null, null);
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

        String[] actualClients = new String[session.getClientSessions().size()];
        for (int i = 0; i < actualClients.length; i++) {
            actualClients[i] = session.getClientSessions().get(i).getClient().getClientId();
        }

        Arrays.sort(clients);
        Arrays.sort(actualClients);

        assertArrayEquals(clients, actualClients);
    }
}
