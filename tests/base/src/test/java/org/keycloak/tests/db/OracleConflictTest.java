package org.keycloak.tests.db;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.keycloak.connections.jpa.dialect.KeycloakOracleDialect;
import org.keycloak.testframework.annotations.InjectTestDatabase;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conditions.DisabledForDatabases;
import org.keycloak.testframework.database.DatabaseConfig;
import org.keycloak.testframework.database.DatabaseConfigBuilder;
import org.keycloak.testframework.database.TestDatabase;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.tests.suites.DatabaseTest;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executes conflict statements against the Oracle database managed by the database test suite.
 */
@KeycloakIntegrationTest
@DatabaseTest
@DisabledForDatabases({ "dev-mem", "dev-file", "postgres", "mariadb", "mysql", "mssql", "tidb", "edb", "remote" })
public class OracleConflictTest {

    private static final String INSERT = "insert into OracleDatabaseConflictEntity (id, payload) values (:id, :payload)";
    private static final String DO_NOTHING = INSERT + " on conflict (id) do nothing";

    @InjectTestDatabase(config = OracleConflictDatabaseConfig.class, lifecycle = LifeCycle.CLASS)
    TestDatabase database;

    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;

    @BeforeEach
    public void createSessionFactory() {
        var config = database.serverConfig();
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "oracle.jdbc.OracleDriver")
                .applySetting("hibernate.connection.url", config.get("db-url"))
                .applySetting("hibernate.connection.username", config.get("db-username"))
                .applySetting("hibernate.connection.password", config.get("db-password"))
                .applySetting("hibernate.dialect", KeycloakOracleDialect.class.getName())
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .build();
        sessionFactory = new MetadataSources(registry).addAnnotatedClass(ConflictEntity.class).addAnnotatedClass(CompositeEntity.class)
                .buildMetadata().buildSessionFactory();
    }

    @AfterEach
    public void closeSessionFactory() {
        if (sessionFactory != null) {
            sessionFactory.close();
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }

    @Test
    public void concurrentDuplicateDoesNothingAfterCompetingCommit() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try (Session first = sessionFactory.openSession()) {
            first.beginTransaction();
            assertEquals(1, execute(first, DO_NOTHING, 1, "winner"));
            CountDownLatch started = new CountDownLatch(1);
            Future<Integer> competing = executor.submit(() -> {
                try (Session second = sessionFactory.withOptions().statementInspector((StatementInspector) sql -> {
                    started.countDown();
                    return sql;
                }).openSession()) {
                    second.beginTransaction();
                    int rows = execute(second, DO_NOTHING, 1, "loser");
                    assertEquals(1, execute(second, DO_NOTHING, 2, "after-conflict"));
                    second.getTransaction().commit();
                    return rows;
                }
            });
            assertTrue(started.await(10, TimeUnit.SECONDS));
            // The competing insert must still be waiting while the first row is uncommitted.
            assertThrows(TimeoutException.class, () -> competing.get(1, TimeUnit.SECONDS));
            first.getTransaction().commit();
            assertEquals(Integer.valueOf(0), competing.get(30, TimeUnit.SECONDS));
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
        assertEquals("winner", payload(1));
        assertEquals("after-conflict", payload(2));
    }

    @Test
    public void unrelatedUniqueViolationIsNotIgnored() {
        mutate(DO_NOTHING, 1, "unique-payload");
        assertThrows(ConstraintViolationException.class, () -> mutate(DO_NOTHING, 2, "unique-payload"));
    }

    @Test
    public void doUpdateAndPlainInsertKeepTheirBehavior() {
        mutate(DO_NOTHING, 1, "first");
        assertEquals(1, mutate(INSERT + " on conflict (id) do update set payload = excluded.payload", 1, "updated"));
        assertEquals("updated", payload(1));
        assertThrows(ConstraintViolationException.class, () -> mutate(INSERT, 1, "plain-duplicate"));
    }

    @Test
    public void compositeConflictSuppressesOnlyItsOwnUniqueConstraint() {
        String hql = "insert into OracleCompositeConflict (id, realmId, externalId) values (:id, :realm, :external)"
                + " on conflict (realmId, externalId) do nothing";
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            assertEquals(1, session.createMutationQuery(hql).setParameter("id", 1)
                    .setParameter("realm", 7).setParameter("external", "key").executeUpdate());
            assertEquals(0, session.createMutationQuery(hql).setParameter("id", 2)
                    .setParameter("realm", 7).setParameter("external", "key").executeUpdate());
            session.getTransaction().commit();
        }
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            try {
                assertThrows(ConstraintViolationException.class, () -> session.createMutationQuery(hql).setParameter("id", 1)
                        .setParameter("realm", 8).setParameter("external", "different-key").executeUpdate());
            } finally {
                session.getTransaction().rollback();
            }
        }
    }

    private int mutate(String hql, int id, String payload) {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            try {
                int count = execute(session, hql, id, payload);
                session.getTransaction().commit();
                return count;
            } catch (RuntimeException e) {
                session.getTransaction().rollback();
                throw e;
            }
        }
    }

    private int execute(Session session, String hql, int id, String payload) {
        return session.createMutationQuery(hql).setParameter("id", id).setParameter("payload", payload).executeUpdate();
    }

    private String payload(int id) {
        try (Session session = sessionFactory.openSession()) {
            return session.find(ConflictEntity.class, id).payload;
        }
    }

    public static class OracleConflictDatabaseConfig implements DatabaseConfig {
        @Override
        public DatabaseConfigBuilder configure(DatabaseConfigBuilder database) {
            return database.preventReuse(true);
        }
    }

    @Entity(name = "OracleDatabaseConflictEntity")
    @Table(name = "\"Oracle.Conflict\"")
    public static class ConflictEntity {
        @Id
        private int id;
        @Column(unique = true)
        private String payload;
    }

    @Entity(name = "OracleCompositeConflict")
    @Table(uniqueConstraints = @UniqueConstraint(columnNames = { "realm_id", "external_id" }))
    public static class CompositeEntity {
        @Id
        private int id;
        @Column(name = "realm_id")
        private int realmId;
        @Column(name = "external_id")
        private String externalId;
    }
}
