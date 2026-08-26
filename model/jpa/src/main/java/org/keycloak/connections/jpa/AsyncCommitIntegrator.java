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
import java.sql.SQLException;
import java.util.Map;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.Session;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
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
 * Hibernate event listener that enables asynchronous commit for transactions
 * that only modify entities implementing {@link AsynchronousCommitAllowed}.
 * <p>
 * Database-specific subclasses implement the actual mechanism:
 * <ul>
 *   <li>{@link PostgreSQLAsyncCommitIntegrator} — {@code SET LOCAL synchronous_commit TO OFF}</li>
 *   <li>{@link MSSQLAsyncCommitIntegrator} — {@code COMMIT TRAN WITH (DELAYED_DURABILITY = ON)}</li>
 *   <li>{@link OracleAsyncCommitIntegrator} — {@code COMMIT WRITE BATCH NOWAIT}</li>
 * </ul>
 * <p>
 * MySQL and MariaDB are not supported: their equivalent setting {@code innodb_flush_log_at_trx_commit}
 * is session-scoped, not transaction-scoped. Setting it before commit would leave the relaxed durability
 * on the pooled connection, silently affecting subsequent unrelated transactions if the reset fails.
 * <p>
 * On unsupported databases, {@link #registerListeners(EntityManagerFactory, boolean)} is a no-op.
 *
 * @author Alexander Schwartz
 */
public abstract class AsyncCommitIntegrator implements PreInsertEventListener, PreUpdateEventListener, PreDeleteEventListener {

    private static final Logger logger = Logger.getLogger(AsyncCommitIntegrator.class);

    private static final String SYNC_REQUIRED = "kc.sync_commit_required";
    private static final String CALLBACK_REGISTERED = "kc.async_commit.registered";

    /**
     * Registers asynchronous commit listeners on the given {@link EntityManagerFactory}
     * if the underlying database supports it. No-op for unsupported databases.
     *
     * @param xaEnabled whether the datasource uses XA transactions. Implementations that issue
     *                  a raw {@code COMMIT} (SQL Server, Oracle) are incompatible with XA because
     *                  the XA coordinator owns the transaction boundaries.
     */
    public static void registerListeners(EntityManagerFactory emf, boolean xaEnabled) {
        SessionFactoryImplementor sf = emf.unwrap(SessionFactoryImplementor.class);

        AsyncCommitIntegrator listener;
        var dialect = sf.getJdbcServices().getDialect();
        if (dialect instanceof PostgreSQLDialect) {
            listener = new PostgreSQLAsyncCommitIntegrator();
        } else if (dialect instanceof SQLServerDialect) {
            listener = new MSSQLAsyncCommitIntegrator();
        } else if (dialect instanceof OracleDialect) {
            listener = new OracleAsyncCommitIntegrator();
        } else {
            return;
        }

        if (xaEnabled && !listener.supportsXa()) {
            logger.debugf("Asynchronous commit optimization disabled for %s: not supported with XA datasources",
                    dialect.getClass().getSimpleName());
            return;
        }

        if (!listener.isEnabled(sf)) {
            return;
        }

        var registry = sf.getEventEngine().getListenerRegistry();
        registry.appendListeners(EventType.PRE_INSERT, listener);
        registry.appendListeners(EventType.PRE_UPDATE, listener);
        registry.appendListeners(EventType.PRE_DELETE, listener);

        logger.debugf("Registered asynchronous commit listeners for %s", dialect.getClass().getSimpleName());
    }

    /**
     * Whether this implementation is compatible with XA datasources.
     * Implementations that issue a raw {@code COMMIT} inside the managed transaction must
     * return {@code false} — XA coordinators do not allow explicit commit on enlisted connections
     * (Oracle raises ORA-02089, SQL Server rejects commits outside the XA coordinator).
     * <p>
     * Defaults to {@code true} for implementations like PostgreSQL that only set session variables.
     */
    protected boolean supportsXa() {
        return true;
    }

    /**
     * Whether asynchronous commit should be enabled for this database.
     * Called once at startup to perform database-specific validation.
     */
    protected abstract boolean isEnabled(SessionFactoryImplementor sf);

    /**
     * Apply the database-specific mechanism to make the current transaction's commit asynchronous.
     * Called via {@code doWork} just before Hibernate issues the JDBC commit.
     */
    protected abstract void applyAsyncCommit(Connection connection) throws SQLException;

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
                            sess.doWork(this::applyAsyncCommit);
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
}
