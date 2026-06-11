/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.jpa;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.EventType;
import org.hibernate.event.spi.PreDeleteEvent;
import org.hibernate.event.spi.PreDeleteEventListener;
import org.hibernate.event.spi.PreInsertEvent;
import org.hibernate.event.spi.PreInsertEventListener;
import org.hibernate.event.spi.PreUpdateEvent;
import org.hibernate.event.spi.PreUpdateEventListener;
import org.jboss.logging.Logger;

/**
 * Hibernate event listener that enables asynchronous commit for PostgreSQL transactions
 * that only modify entities implementing {@link AsynchronousCommitAllowed}.
 * <p>
 * On PostgreSQL, issues {@code SET LOCAL synchronous_commit TO OFF} before commit when
 * all modified entities in the transaction allow it. This skips the WAL fsync wait,
 * improving throughput for ephemeral data. The database remains crash-consistent;
 * only the last few milliseconds of such transactions may be lost on a crash.
 * <p>
 * On non-PostgreSQL databases, {@link #registerListeners(EntityManagerFactory)} is a no-op.
 *
 * @author Alexander Schwartz
 */
public class AsyncCommitIntegrator implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener {

    private static final Logger logger = Logger.getLogger(AsyncCommitIntegrator.class);

    private static final String SYNC_REQUIRED = "kc.sync_commit_required";
    private static final String CALLBACK_REGISTERED = "kc.async_commit.registered";

    /**
     * Registers asynchronous commit listeners on the given {@link EntityManagerFactory}
     * if the underlying database is PostgreSQL. No-op for other databases.
     */
    public static void registerListeners(EntityManagerFactory emf) {
        SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);
        if (!(sf.getJdbcServices().getDialect() instanceof PostgreSQLDialect)) {
            return;
        }

        if (isAuroraWithLogicalReplication(sf)) {
            logger.warn("Asynchronous commit optimization disabled: Aurora PostgreSQL with logical replication " +
                    "detected. Aurora may not deliver async-committed transactions to logical decoding consumers.");
            return;
        }

        AsyncCommitIntegrator listener = new AsyncCommitIntegrator();
        var registry = sf.getEventEngine().getListenerRegistry();
        registry.appendListeners(EventType.PRE_INSERT, listener);
        registry.appendListeners(EventType.PRE_UPDATE, listener);
        registry.appendListeners(EventType.PRE_DELETE, listener);

        logger.debug("Registered asynchronous commit listeners for PostgreSQL");
    }

    @Override
    public boolean onPreInsert(PreInsertEvent event) {
        handleEntity(event.getEntity(), event.getSession(), AsynchronousCommitAllowed.EntityOperationType.INSERT);
        return false;
    }

    @Override
    public boolean onPreUpdate(PreUpdateEvent event) {
        handleEntity(event.getEntity(), event.getSession(), AsynchronousCommitAllowed.EntityOperationType.UPDATE);
        return false;
    }

    @Override
    public boolean onPreDelete(PreDeleteEvent event) {
        handleEntity(event.getEntity(), event.getSession(), AsynchronousCommitAllowed.EntityOperationType.DELETE);
        return false;
    }

    private void handleEntity(Object entity, SharedSessionContractImplementor session, AsynchronousCommitAllowed.EntityOperationType opType) {
        if (!(session instanceof Session s)) {
            return;
        }

        Map<String, Object> props = s.getProperties();

        if (Boolean.TRUE.equals(props.get(SYNC_REQUIRED))) {
            return;
        }

        if (props.get(CALLBACK_REGISTERED) == null) {
            s.setProperty(CALLBACK_REGISTERED, Boolean.TRUE);
            session.getTransactionCompletionCallbacks().registerCallback(
                    (SharedSessionContractImplementor sess) -> {
                        if (!Boolean.TRUE.equals(((Session) sess).getProperties().get(SYNC_REQUIRED))) {
                            sess.doWork(AsyncCommitIntegrator::setAsyncCommit);
                        }
                    }
            );
        }

        if (entity instanceof AsynchronousCommitAllowed asyncEntity) {
            if (!asyncEntity.isAsyncCommitAllowed(opType)) {
                s.setProperty(SYNC_REQUIRED, Boolean.TRUE);
            }
        } else {
            s.setProperty(SYNC_REQUIRED, Boolean.TRUE);
        }
    }

    /**
     * Detects Aurora PostgreSQL with logical replication enabled — a combination where
     * {@code SET LOCAL synchronous_commit TO OFF} can cause committed transactions to
     * never appear (or appear with extreme delay) in logical decoding consumers like Debezium.
     * <p>
     * Detection: {@code SELECT aurora_version()} only exists on Aurora (throws on standard PG);
     * {@code SHOW wal_level = 'logical'} indicates a CDC consumer may be reading the WAL.
     * Fails safe (returns {@code true}) on unexpected errors to avoid silent CDC data loss.
     *
     * @see <a href="https://repost.aws/questions/QU_4m9WIVUQ1aC-w4v2MzC7g">Aurora PostgreSQL does not perform logical decoding when synchronous_commit = off</a>
     */
    private static boolean isAuroraWithLogicalReplication(SessionFactoryImplementor sf) {
        try {
            JdbcConnectionAccess bootstrapJdbcConnectionAccess = sf.getJdbcServices().getBootstrapJdbcConnectionAccess();
            Connection connection = bootstrapJdbcConnectionAccess.obtainConnection();
            try {
                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SELECT aurora_version()")) {
                    if (!rs.next()) {
                        return false;
                    }
                } catch (SQLException e) {
                    return false;
                }

                try (Statement stmt = connection.createStatement();
                     ResultSet rs = stmt.executeQuery("SHOW wal_level")) {
                    return rs.next() && "logical".equals(rs.getString(1));
                }
            } finally {
                bootstrapJdbcConnectionAccess.releaseConnection(connection);
            }
        } catch (SQLException e) {
            logger.warn("Failed to detect Aurora/logical replication status; disabling asynchronous commit optimization", e);
            return true;
        }
    }

    private static void setAsyncCommit(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET LOCAL synchronous_commit TO OFF");
        }
    }
}
