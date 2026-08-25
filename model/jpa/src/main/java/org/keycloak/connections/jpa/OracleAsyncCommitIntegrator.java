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
import java.sql.Statement;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.jboss.logging.Logger;

/**
 * Oracle implementation that commits the transaction with {@code COMMIT WRITE BATCH NOWAIT}
 * before Hibernate's normal commit, which then becomes a no-op.
 * <p>
 * Like the {@link MSSQLAsyncCommitIntegrator SQL Server variant}, Oracle's asynchronous redo
 * control is a clause on the {@code COMMIT} statement, not a session variable. This implementation
 * commits explicitly in the {@linkplain #applyAsyncCommit(Connection) before-completion callback}
 * via {@code COMMIT WRITE BATCH NOWAIT}. When Hibernate subsequently calls
 * {@code Connection.commit()}, the Oracle JDBC driver (ojdbc) sends a protocol-level OCOMMIT
 * to the server unconditionally — unlike the mssql-jdbc driver, there is no client-side guard.
 * The Oracle server accepts a {@code COMMIT} with no active transaction as a no-op (no error
 * is raised); this is documented Oracle behavior and matches how any Oracle application that
 * commits after read-only work already operates.
 * <p>
 * {@code BATCH} groups redo entries; {@code NOWAIT} returns control without waiting for the
 * redo log write to complete. The database remains crash-consistent; only the last few
 * milliseconds of such transactions may be lost on a crash.
 * <p>
 * No database-level prerequisite is required — Oracle supports {@code COMMIT WRITE} options
 * without any prior configuration.
 *
 * @author Alexander Schwartz
 */
class OracleAsyncCommitIntegrator extends AsyncCommitIntegrator {

    private static final Logger logger = Logger.getLogger(OracleAsyncCommitIntegrator.class);

    @Override
    protected boolean isEnabled(SessionFactoryImplementor sf) {
        return true;
    }

    @Override
    protected void applyAsyncCommit(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("COMMIT WRITE BATCH NOWAIT");
        }
    }
}
