package org.keycloak.testsuite.model;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.util.Time;

import java.util.Arrays;
import java.util.List;

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
        realm = session.model().getRealm("test");
        realm.addUser("user1");
        realm.addUser("user2");
    }

    @After
    public void after() {
        resetSession();
        session.sessions().removeUserSessions(realm);
        realm.removeUser("user1");
        realm.removeUser("user2");
        kc.stopSession(session, true);
    }

    @Test
    public void testCreateSessions() {
        int started = Time.currentTime();
        UserSessionModel[] sessions = createSessions();

        assertSession(session.sessions().getUserSession(realm, sessions[0].getId()), realm.getUser("user1"), "127.0.0.1", started, started, "test-app", "third-party");
        assertSession(session.sessions().getUserSession(realm, sessions[1].getId()), realm.getUser("user1"), "127.0.0.2", started, started, "test-app");
        assertSession(session.sessions().getUserSession(realm, sessions[2].getId()), realm.getUser("user2"), "127.0.0.3", started, started);
    }

    @Test
    public void testGetUserSessions() {
        UserSessionModel[] sessions = createSessions();

        assertSessions(session.sessions().getUserSessions(realm, realm.getUser("user1")), sessions[0], sessions[1]);
        assertSessions(session.sessions().getUserSessions(realm, realm.getUser("user2")), sessions[2]);
    }

    @Test
    public void testRemoveUserSessionsByUser() {
        createSessions();
        session.sessions().removeUserSessions(realm, realm.getUser("user1"));
        resetSession();

        assertTrue(session.sessions().getUserSessions(realm, realm.getUser("user1")).isEmpty());
        assertFalse(session.sessions().getUserSessions(realm, realm.getUser("user2")).isEmpty());
    }

    @Test
    public void testRemoveUserSessionsByRealm() {
        createSessions();
        session.sessions().removeUserSessions(realm);
        resetSession();

        assertTrue(session.sessions().getUserSessions(realm, realm.getUser("user1")).isEmpty());
        assertTrue(session.sessions().getUserSessions(realm, realm.getUser("user2")).isEmpty());
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

        assertSessions(session.sessions().getUserSessions(realm, realm.findClient("test-app")), sessions[0], sessions[1]);
        assertSessions(session.sessions().getUserSessions(realm, realm.findClient("third-party")), sessions[0]);
    }

    @Test
    public void testGetCountByClient() {
        createSessions();

        assertEquals(2, session.sessions().getActiveUserSessions(realm, realm.findClient("test-app")));
        assertEquals(1, session.sessions().getActiveUserSessions(realm, realm.findClient("third-party")));
    }

    private UserSessionModel[] createSessions() {
        UserSessionModel[] sessions = new UserSessionModel[4];
        sessions[0] = session.sessions().createUserSession(realm, realm.getUser("user1"), "127.0.0.1");
        sessions[0].associateClient(realm.findClient("test-app"));
        sessions[0].associateClient(realm.findClient("third-party"));

        sessions[1] = session.sessions().createUserSession(realm, realm.getUser("user1"), "127.0.0.2");
        sessions[1].associateClient(realm.findClient("test-app"));

        sessions[2] = session.sessions().createUserSession(realm, realm.getUser("user2"), "127.0.0.3");

        resetSession();

        return sessions;
    }

    private void resetSession() {
        kc.stopSession(session, true);
        session = kc.startSession();
        realm = session.model().getRealm("test");
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
        assertTrue(session.getStarted() >= started - 1 && session.getStarted() <= started + 1);
        assertTrue(session.getLastSessionRefresh() >= lastRefresh - 1 && session.getLastSessionRefresh() <= lastRefresh + 1);

        String[] actualClients = new String[session.getClientAssociations().size()];
        for (int i = 0; i < actualClients.length; i++) {
            actualClients[i] = session.getClientAssociations().get(i).getClientId();
        }

        Arrays.sort(clients);
        Arrays.sort(actualClients);

        assertArrayEquals(clients, actualClients);
    }

}
