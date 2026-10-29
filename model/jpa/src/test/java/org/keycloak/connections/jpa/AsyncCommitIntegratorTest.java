package org.keycloak.connections.jpa;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import com.arjuna.ats.jta.TransactionManager;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.event.spi.EventType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

@RunWith(Parameterized.class)
public class AsyncCommitIntegratorTest {

    @Parameterized.Parameters(name = "queue={0}, transactions={1}")
    public static Collection<Object[]> queues() {
        return Arrays.asList(new Object[][] {
                { "graph", "jdbc" }, { "legacy", "jdbc" }, { "graph", "jta" }, { "legacy", "jta" }
        });
    }

    @Parameterized.Parameter
    public String queue;

    @Parameterized.Parameter(1)
    public String transactions;

    private final AtomicInteger asyncCommits = new AtomicInteger();
    private StandardServiceRegistry registry;
    private SessionFactory sessionFactory;

    @Before
    public void createSessionFactory() {
        registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.connection.driver_class", "org.h2.Driver")
                .applySetting("hibernate.connection.url", "jdbc:h2:mem:" + UUID.randomUUID())
                .applySetting("hibernate.hbm2ddl.auto", "create-drop")
                .applySetting("hibernate.flush.queue.type", queue)
                .applySetting("hibernate.transaction.coordinator_class", transactions)
                .applySetting("hibernate.transaction.jta.platform", transactions.equals("jta")
                        ? "org.hibernate.engine.transaction.jta.platform.internal.NarayanaJtaPlatform"
                        : "org.hibernate.engine.transaction.jta.platform.internal.NoJtaPlatform")
                .build();
        sessionFactory = new MetadataSources(registry)
                .addAnnotatedClass(AsyncEntity.class)
                .addAnnotatedClass(SyncEntity.class)
                .buildMetadata().buildSessionFactory();
        AsyncCommitIntegrator integrator = new AsyncCommitIntegrator() {
            @Override
            protected boolean isEnabled(SessionFactoryImplementor sf) {
                return true;
            }

            @Override
            protected void applyAsyncCommit(Connection connection) {
                asyncCommits.incrementAndGet();
            }
        };
        sessionFactory.unwrap(SessionFactoryImplementor.class).getEventEngine().getListenerRegistry()
                .appendListeners(EventType.PRE_INSERT, integrator);
    }

    @After
    public void closeSessionFactory() throws Exception {
        try {
            if (transactions.equals("jta")) {
                var transactionManager = TransactionManager.transactionManager();
                if (transactionManager.getTransaction() != null) {
                    transactionManager.rollback();
                }
            }
        } finally {
            if (sessionFactory != null) {
                sessionFactory.close();
            }
            if (registry != null) {
                StandardServiceRegistryBuilder.destroy(registry);
            }
        }
    }

    @Test
    public void commitRunsCallbackOnce() {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(new AsyncEntity(1));
            session.flush();
            session.persist(new AsyncEntity(2));
            session.getTransaction().commit();
        }
        assertEquals(1, asyncCommits.get());
    }

    @Test
    public void rollbackDoesNotRunCallback() {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(new AsyncEntity(1));
            session.flush();
            session.getTransaction().rollback();
            assertFalse("Rollback must clear before-completion callbacks",
                    session.unwrap(EventSource.class).getActionQueue().hasBeforeTransactionActions());
            assertEquals(0, asyncCommits.get());
        }
        assertEquals(0, asyncCommits.get());

        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(new AsyncEntity(2));
            session.getTransaction().commit();
        }
        assertEquals(1, asyncCommits.get());
    }

    @Test
    public void mixedChangesRequireSynchronousCommit() {
        try (Session session = sessionFactory.openSession()) {
            session.beginTransaction();
            session.persist(new AsyncEntity(1));
            session.flush();
            session.persist(new SyncEntity(2));
            session.getTransaction().commit();
        }
        assertEquals(0, asyncCommits.get());
    }

    @Entity(name = "AsyncCommitTestAsyncEntity")
    public static class AsyncEntity implements AsynchronousCommitAllowed {
        @Id
        private int id;

        public AsyncEntity() {
        }

        public AsyncEntity(int id) {
            this.id = id;
        }

        @Override
        public boolean isAsyncCommitAllowed(EntityOperationType operationType) {
            return true;
        }
    }

    @Entity(name = "AsyncCommitTestSyncEntity")
    public static class SyncEntity {
        @Id
        private int id;

        public SyncEntity() {
        }

        public SyncEntity(int id) {
            this.id = id;
        }
    }
}
