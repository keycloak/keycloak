package org.keycloak.testsuite.model;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.util.Time;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
        session.users().addUser(realm, "user1");
        session.users().addUser(realm, "user2");
    }

    @After
    public void after() {
        resetSession();
        session.sessions().removeUserSessions(realm);
        UserModel user1 = session.users().getUserByUsername("user1", realm);
        UserModel user2 = session.users().getUserByUsername("user2", realm);
        session.users().removeUser(realm, user1);
        session.users().removeUser(realm, user2);
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
    public void testCreateClientSession() {
        UserSessionModel[] sessions = createSessions();

        List<ClientSessionModel> clientSessions = sessions[0].getClientSessions();
        assertEquals(2, clientSessions.size());
        ClientSessionModel session = clientSessions.get(0);

        assertEquals(null, session.getAction());
        assertEquals(realm.findClient("test-app").getClientId(), session.getClient().getClientId());
        assertEquals(sessions[0].getId(), session.getUserSession().getId());
        assertEquals("http://redirect", session.getRedirectUri());
        assertEquals("state", session.getState());
        assertEquals(2, session.getRoles().size());
        assertTrue(session.getRoles().contains("one"));
        assertTrue(session.getRoles().contains("two"));
    }

    @Test
    public void testUpdateClientSession() {
        UserSessionModel[] sessions = createSessions();

        String id = sessions[0].getClientSessions().get(0).getId();

        ClientSessionModel clientSession = session.sessions().getClientSession(realm, id);

        int time = clientSession.getTimestamp();
        assertEquals(null, clientSession.getAction());

        clientSession.setAction(ClientSessionModel.Action.CODE_TO_TOKEN);
        clientSession.setTimestamp(time + 10);

        kc.stopSession(session, true);
        session = kc.startSession();

        ClientSessionModel updated = session.sessions().getClientSession(realm, id);
        assertEquals(ClientSessionModel.Action.CODE_TO_TOKEN, updated.getAction());
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

        session.sessions().onClientRemoved(realm, realm.findClient("third-party"));
        resetSession();

        for (String c : clientSessionsRemoved) {
            assertNull(session.sessions().getClientSession(realm, c));
        }
        for (String c : clientSessionsKept) {
            assertNotNull(session.sessions().getClientSession(realm, c));
        }

        session.sessions().onClientRemoved(realm, realm.findClient("test-app"));
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
        UserSessionModel[] sessions = createSessions();

        session.sessions().getUserSession(realm, sessions[0].getId()).setStarted(Time.currentTime() - realm.getSsoSessionMaxLifespan() - 1);
        session.sessions().getUserSession(realm, sessions[1].getId()).setLastSessionRefresh(Time.currentTime() - realm.getSsoSessionIdleTimeout() - 1);

        resetSession();

        session.sessions().removeExpiredUserSessions(realm);
        resetSession();

        assertNull(session.sessions().getUserSession(realm, sessions[0].getId()));
        assertNull(session.sessions().getUserSession(realm, sessions[1].getId()));
        assertNotNull(session.sessions().getUserSession(realm, sessions[2].getId()));
    }

    @Test
    public void testGetByClient() {
        UserSessionModel[] sessions = createSessions();

        assertSessions(session.sessions().getUserSessions(realm, realm.findClient("test-app")), sessions[0], sessions[1], sessions[2]);
        assertSessions(session.sessions().getUserSessions(realm, realm.findClient("third-party")), sessions[0]);
    }

    @Test
    public void testGetByClientPaginated() {
        for (int i = 0; i < 25; i++) {
            UserSessionModel userSession = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0." + i, "form", false);
            userSession.setStarted(Time.currentTime() + i);
            session.sessions().createClientSession(realm, realm.findClient("test-app"), userSession, "http://redirect", "state", new HashSet<String>());
        }

        resetSession();

        assertPaginatedSession(realm, realm.findClient("test-app"), 0, 1, 1);
        assertPaginatedSession(realm, realm.findClient("test-app"), 0, 10, 10);
        assertPaginatedSession(realm, realm.findClient("test-app"), 10, 10, 10);
        assertPaginatedSession(realm, realm.findClient("test-app"), 20, 10, 5);
        assertPaginatedSession(realm, realm.findClient("test-app"), 30, 10, 0);
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

        assertEquals(3, session.sessions().getActiveUserSessions(realm, realm.findClient("test-app")));
        assertEquals(1, session.sessions().getActiveUserSessions(realm, realm.findClient("third-party")));
    }

    private UserSessionModel[] createSessions() {
        UserSessionModel[] sessions = new UserSessionModel[3];
        sessions[0] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.1", "form", true);

        Set<String> roles = new HashSet<String>();
        roles.add("one");
        roles.add("two");

        session.sessions().createClientSession(realm, realm.findClient("test-app"), sessions[0], "http://redirect", "state", roles);
        session.sessions().createClientSession(realm, realm.findClient("third-party"), sessions[0], "http://redirect", "state", new HashSet<String>());

        sessions[1] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user1", realm), "user1", "127.0.0.2", "form", true);
        session.sessions().createClientSession(realm, realm.findClient("test-app"), sessions[1], "http://redirect", "state", new HashSet<String>());

        sessions[2] = session.sessions().createUserSession(realm, session.users().getUserByUsername("user2", realm), "user2", "127.0.0.3", "form", true);
        session.sessions().createClientSession(realm, realm.findClient("test-app"), sessions[2], "http://redirect", "state", new HashSet<String>());

        resetSession();

        return sessions;
    }

    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        realm = session.realms().getRealm("test");
    }

    public void assertSessions(List<UserSessionModel> actualSessions, UserSessionModel... expectedSessions) {
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

    public void assertSession(UserSessionModel session, UserModel user, String ipAddress, int started, int lastRefresh, String... clients) {
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
