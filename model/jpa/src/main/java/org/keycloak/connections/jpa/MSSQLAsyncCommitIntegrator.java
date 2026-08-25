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

import org.hibernate.engine.jdbc.connections.spi.JdbcConnectionAccess;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.jboss.logging.Logger;

/**
 * SQL Server implementation that commits the transaction with delayed durability before
 * Hibernate's normal commit, which then becomes a no-op.
 * <p>
 * Unlike PostgreSQL's {@code SET LOCAL synchronous_commit TO OFF} which is a transaction-scoped
 * setting applied before Hibernate's commit, SQL Server's {@code DELAYED_DURABILITY = ON} is a
 * clause on the {@code COMMIT} statement itself. Since Hibernate controls the JDBC
 * {@code Connection.commit()} call, there is no way to append this clause to it.
 * <p>
 * Instead, this implementation commits the transaction explicitly in the
 * {@linkplain #applyAsyncCommit(Connection) before-completion callback} via
 * {@code COMMIT TRAN WITH (DELAYED_DURABILITY = ON)}. When Hibernate subsequently calls
 * {@code Connection.commit()}, the mssql-jdbc driver executes
 * {@code IF @@TRANCOUNT > 0 COMMIT TRAN} — but since the transaction was already committed,
 * {@code @@TRANCOUNT} is 0 and the statement is a no-op.
 * <p>
 * Requires the database to have {@code DELAYED_DURABILITY = ALLOWED} set by the DBA:
 * <pre>ALTER DATABASE ... SET DELAYED_DURABILITY = ALLOWED</pre>
 *
 * @author Alexander Schwartz
 */
class MSSQLAsyncCommitIntegrator extends AsyncCommitIntegrator {

    private static final Logger logger = Logger.getLogger(MSSQLAsyncCommitIntegrator.class);

    @Override
    protected boolean isEnabled(SessionFactoryImplementor sf) {
        return isDelayedDurabilityAllowed(sf);
    }

    @Override
    protected void applyAsyncCommit(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("COMMIT TRAN WITH (DELAYED_DURABILITY = ON)");
        }
    }

    /**
     * Checks whether the database has {@code DELAYED_DURABILITY} set to {@code ALLOWED}.
     * <ul>
     *   <li>{@code DISABLED} — the optimization cannot be used; log a message and return false.</li>
     *   <li>{@code FORCED} — all transactions already use delayed durability; no per-transaction
     *       control needed, so return false.</li>
     *   <li>{@code ALLOWED} — per-transaction opt-in is possible; return true.</li>
     * </ul>
     */
    private static boolean isDelayedDurabilityAllowed(SessionFactoryImplementor sf) {
        try {
            JdbcConnectionAccess bootstrapJdbcConnectionAccess = sf.getJdbcServices().getBootstrapJdbcConnectionAccess();
            Connection connection = bootstrapJdbcConnectionAccess.obtainConnection();
            try (Statement stmt = connection.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT delayed_durability_desc FROM sys.databases WHERE name = DB_NAME()")) {
                if (!rs.next()) {
                    logger.warn("Could not determine DELAYED_DURABILITY setting; disabling asynchronous commit optimization for SQL Server");
                    return false;
                }

                String setting = rs.getString(1);
                switch (setting) {
                    case "DISABLED":
                        logger.info("Asynchronous commit optimization not available for SQL Server: " +
                                "DELAYED_DURABILITY is DISABLED. Set it to ALLOWED to enable: " +
                                "ALTER DATABASE ... SET DELAYED_DURABILITY = ALLOWED");
                        return false;
                    case "FORCED":
                        logger.info("Asynchronous commit optimization not needed for SQL Server: " +
                                "DELAYED_DURABILITY is FORCED — all transactions already use delayed durability");
                        return false;
                    case "ALLOWED":
                        return true;
                    default:
                        logger.warnf("Unexpected DELAYED_DURABILITY value '%s'; disabling asynchronous commit optimization for SQL Server", setting);
                        return false;
                }
            } finally {
                bootstrapJdbcConnectionAccess.releaseConnection(connection);
            }
        } catch (SQLException e) {
            logger.warn("Failed to check DELAYED_DURABILITY setting; disabling asynchronous commit optimization for SQL Server", e);
            return false;
        }
    }
}
