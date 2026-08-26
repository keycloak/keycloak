package org.keycloak.tests.session;

import java.util.Map;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that note removals on persistent user/client sessions reach the database and not only the cached entity.
 * <p>
 * Each step runs in its own server request (and therefore its own transaction), so that removals are replayed by
 * {@code JpaChangesPerformer} as an update of an already persisted session. The persisted state is read back through
 * {@link UserSessionPersisterProvider}, bypassing the session cache.
 */
@KeycloakIntegrationTest
public class SessionNotePersistenceTest {

    private static final String USERNAME = "user1";
    private static final String CLIENT_ID = "test-app";

    @InjectRealm(config = SessionNotePersistenceRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void assumePersistentUserSessionsWithEmbeddedCaches() {
        boolean supported = runOnServer.fetch(session -> MultiSiteUtils.isPersistentSessionsEnabled() && InfinispanUtils.isEmbeddedInfinispan(), Boolean.class);
        Assumptions.assumeTrue(supported, "Requires persistent user sessions with embedded Infinispan caches");
    }

    @Test
    public void testClientSessionNoteRemovalIsPersisted() {
        final String realmName = managedRealm.getName();
        final String noteName = "note-to-remove";
        final String userSessionId = createUserSessionWithClientSessionNote(realmName, noteName, "value");

        // sanity check: setting the note is persisted
        runOnServer.run(session -> assertEquals("value", loadClientSessionFromDatabase(session, realmName, userSessionId).getNote(noteName)));

        runOnServer.run(session -> {
            AuthenticatedClientSessionModel clientSession = loadClientSessionFromProvider(session, realmName, userSessionId);
            clientSession.removeNote(noteName);
            assertNull(clientSession.getNote(noteName), "note must be gone from the in-memory session");
        });

        runOnServer.run(session -> assertNull(loadClientSessionFromDatabase(session, realmName, userSessionId).getNote(noteName),
                "removed client session note must not survive in the database"));
    }

    @Test
    public void testClientSessionRestartClearsPersistedNotes() {
        final String realmName = managedRealm.getName();
        final String noteName = "note-before-restart";
        final String userSessionId = createUserSessionWithClientSessionNote(realmName, noteName, "value");

        runOnServer.run(session -> assertEquals("value", loadClientSessionFromDatabase(session, realmName, userSessionId).getNote(noteName)));

        runOnServer.run(session -> {
            AuthenticatedClientSessionModel clientSession = loadClientSessionFromProvider(session, realmName, userSessionId);
            clientSession.restartClientSession();
            assertNull(clientSession.getNote(noteName), "note must be gone from the in-memory session");
            assertNotNull(clientSession.getNote(AuthenticatedClientSessionModel.STARTED_AT_NOTE));
        });

        runOnServer.run(session -> {
            AuthenticatedClientSessionModel persisted = loadClientSessionFromDatabase(session, realmName, userSessionId);
            assertNotNull(persisted.getNote(AuthenticatedClientSessionModel.STARTED_AT_NOTE), "restart writes the started-at note");
            assertNull(persisted.getNote(noteName), "notes of the previous authentication must not survive restartClientSession() in the database");
        });
    }

    @Test
    public void testUserSessionNoteRemovalIsPersisted() {
        final String realmName = managedRealm.getName();
        final String noteName = "note-to-remove";
        final String userSessionId = createUserSessionWithNote(realmName, noteName, "value");

        runOnServer.run(session -> assertEquals("value", loadUserSessionFromDatabase(session, realmName, userSessionId).getNote(noteName)));

        runOnServer.run(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
            userSession.removeNote(noteName);
            assertNull(userSession.getNote(noteName), "note must be gone from the in-memory session");
        });

        runOnServer.run(session -> assertNull(loadUserSessionFromDatabase(session, realmName, userSessionId).getNote(noteName),
                "removed user session note must not survive in the database"));
    }

    @Test
    public void testUserSessionRestartClearsPersistedNotes() {
        // Regression guard: UserSessionAdapter#restartSession clears the notes via Map#clear on the synthetic entity used by
        // JpaChangesPerformer#mergeUserSession, which delegates to the live notes map of PersistentUserSessionAdapter.
        final String realmName = managedRealm.getName();
        final String noteName = "note-before-restart";
        final String userSessionId = createUserSessionWithNote(realmName, noteName, "value");

        runOnServer.run(session -> assertEquals("value", loadUserSessionFromDatabase(session, realmName, userSessionId).getNote(noteName)));

        runOnServer.run(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
            UserModel user = session.users().getUserByUsername(realm, USERNAME);
            userSession.restartSession(realm, user, USERNAME, "127.0.0.2", "form", false, null, null);
            assertNull(userSession.getNote(noteName), "note must be gone from the in-memory session");
        });

        runOnServer.run(session -> {
            UserSessionModel persisted = loadUserSessionFromDatabase(session, realmName, userSessionId);
            assertEquals("127.0.0.2", persisted.getIpAddress(), "restart is persisted");
            Map<String, String> notes = persisted.getNotes();
            assertTrue(notes == null || notes.isEmpty(), "notes of the previous authentication must not survive restartSession() in the database, but got: " + notes);
        });
    }

    @Test
    public void testUserSessionNoteSetToNullIsRemovedFromDatabase() {
        // Regression guard: UserSessionAdapter#setNote(name, null) must remove directly from the replay entity so the
        // removal is applied both to the cached session and to the persisted session during task replay.
        final String realmName = managedRealm.getName();
        final String noteName = "note-to-null";
        final String userSessionId = createUserSessionWithNote(realmName, noteName, "value");

        runOnServer.run(session -> assertEquals("value", loadUserSessionFromDatabase(session, realmName, userSessionId).getNote(noteName)));

        runOnServer.run(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
            userSession.setNote(noteName, null);
            assertNull(userSession.getNote(noteName), "note must be gone from the in-memory session");
        });

        runOnServer.run(session -> assertNull(loadUserSessionFromDatabase(session, realmName, userSessionId).getNote(noteName),
                "user session note set to null must be removed from the database"));
    }

    private String createUserSessionWithNote(String realmName, String noteName, String noteValue) {
        return runOnServer.fetch(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            UserSessionModel userSession = createUserSession(session, realm);
            userSession.setNote(noteName, noteValue);
            return userSession.getId();
        }, String.class);
    }

    private String createUserSessionWithClientSessionNote(String realmName, String noteName, String noteValue) {
        return runOnServer.fetch(session -> {
            RealmModel realm = setRealmContext(session, realmName);
            UserSessionModel userSession = createUserSession(session, realm);
            AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realm, realm.getClientByClientId(CLIENT_ID), userSession);
            clientSession.setRedirectUri("http://redirect");
            clientSession.setNote(noteName, noteValue);
            return userSession.getId();
        }, String.class);
    }

    private static RealmModel setRealmContext(KeycloakSession session, String realmName) {
        RealmModel realm = session.realms().getRealmByName(realmName);
        session.getContext().setRealm(realm);
        return realm;
    }

    private static UserSessionModel createUserSession(KeycloakSession session, RealmModel realm) {
        UserModel user = session.users().getUserByUsername(realm, USERNAME);
        return session.sessions().createUserSession(null, realm, user, USERNAME, "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT);
    }

    private static AuthenticatedClientSessionModel loadClientSessionFromProvider(KeycloakSession session, String realmName, String userSessionId) {
        RealmModel realm = setRealmContext(session, realmName);
        UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
        return session.sessions().getClientSession(userSession, realm.getClientByClientId(CLIENT_ID), false);
    }

    /**
     * Bypasses the session cache and reads the client session as stored by the {@link UserSessionPersisterProvider}.
     */
    private static AuthenticatedClientSessionModel loadClientSessionFromDatabase(KeycloakSession session, String realmName, String userSessionId) {
        RealmModel realm = setRealmContext(session, realmName);
        ClientModel client = realm.getClientByClientId(CLIENT_ID);
        UserSessionModel userSession = session.sessions().getUserSession(realm, userSessionId);
        return session.getProvider(UserSessionPersisterProvider.class).loadClientSession(realm, client, userSession, false);
    }

    /**
     * Bypasses the session cache and reads the user session as stored by the {@link UserSessionPersisterProvider}.
     */
    private static UserSessionModel loadUserSessionFromDatabase(KeycloakSession session, String realmName, String userSessionId) {
        RealmModel realm = setRealmContext(session, realmName);
        return session.getProvider(UserSessionPersisterProvider.class).loadUserSession(realm, userSessionId, false);
    }

    private static class SessionNotePersistenceRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("client-session-notes");
            realm.users(UserBuilder.create(USERNAME));
            realm.clients(ClientBuilder.create(CLIENT_ID));
            return realm;
        }
    }
}
