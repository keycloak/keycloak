package org.keycloak.tests.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.JpaConnectionProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.tests.suites.DatabaseTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The persister locks rows without waiting, and treats a row locked by someone else as "not locked".
 * Locking must not wait, fail or mark the transaction as rollback-only, as the caller continues in the same
 * transaction and retries in a separate one. This is not the case for every database and Hibernate version
 * (for example when NOWAIT is used on a database that doesn't support SKIP LOCKED).
 */
@KeycloakIntegrationTest
@DatabaseTest
public class UserSessionPersisterLockTest {

    private static final String SESSION_ID = "__test_lock_session";

    @TestOnServer
    public void testLockOfRowLockedByOtherTransactionKeepsTransactionUsable(KeycloakSession session) throws Exception {
        var factory = (JpaConnectionProviderFactory) session.getKeycloakSessionFactory()
                .getProviderFactory(JpaConnectionProvider.class);
        String product;
        try (Connection connection = factory.getConnection()) {
            product = connection.getMetaData().getDatabaseProductName().toLowerCase();
        }
        if (product.contains("microsoft")) {
            // different syntax for locking a row
            return;
        }

        String table = (factory.getSchema() != null ? factory.getSchema() + "." : "") + "OFFLINE_USER_SESSION";
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var failure = new AtomicReference<Throwable>();

        // Runs outside the current thread to avoid JTA enlistment of the connection holding the lock.
        Thread holder = new Thread(() -> {
            try (Connection connection = factory.getConnection()) {
                insertRow(connection, table);
                try {
                    connection.setAutoCommit(false);
                    try (PreparedStatement stmt = connection.prepareStatement("SELECT VERSION FROM " + table + " WHERE USER_SESSION_ID = ? AND OFFLINE_FLAG = '1' FOR UPDATE")) {
                        stmt.setString(1, SESSION_ID);
                        stmt.executeQuery().close();
                    }
                    locked.countDown();
                    release.await(60, TimeUnit.SECONDS);
                    connection.rollback();
                } finally {
                    connection.setAutoCommit(true);
                    try (Statement stmt = connection.createStatement()) {
                        stmt.executeUpdate("DELETE FROM " + table + " WHERE USER_SESSION_ID = '" + SESSION_ID + "'");
                    }
                }
            } catch (Throwable t) {
                failure.set(t);
                locked.countDown();
            }
        });
        holder.start();

        try {
            assertTrue(locked.await(60, TimeUnit.SECONDS), "Row should have been locked");
            assertThat("Failed to set up the locked row: " + failure.get(), failure.get() == null, is(true));

            RealmModel realm = session.getContext().getRealm();
            var persister = session.getProvider(UserSessionPersisterProvider.class);

            assertThat(persister.lockUserSession(realm, SESSION_ID, true, false), is(false));

            assertThat(session.getTransactionManager().getRollbackOnly(), is(false));
            // the transaction can still be used
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            em.createQuery("select count(e) from PersistentUserSessionEntity e", Long.class).getSingleResult();
        } finally {
            release.countDown();
            holder.join();
        }
        assertThat("Failure in thread holding the lock: " + failure.get(), failure.get() == null, is(true));
    }

    private static void insertRow(Connection connection, String table) throws SQLException {
        connection.setAutoCommit(true);
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DELETE FROM " + table + " WHERE USER_SESSION_ID = '" + SESSION_ID + "'");
            stmt.executeUpdate("INSERT INTO " + table + " (USER_SESSION_ID, OFFLINE_FLAG, USER_ID, REALM_ID, CREATED_ON, LAST_SESSION_REFRESH, VERSION, SESSION_BUCKET, LAST_SESSION_REFRESH_COARSE) VALUES ('"
                    + SESSION_ID + "', '1', '__test', '__test', 0, 0, 0, 0, 0)");
        }
    }
}
