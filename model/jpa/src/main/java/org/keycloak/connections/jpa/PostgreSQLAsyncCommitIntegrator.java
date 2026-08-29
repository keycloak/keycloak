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
 * PostgreSQL implementation that issues {@code SET LOCAL synchronous_commit TO OFF} before commit.
 * This skips the WAL fsync wait, improving throughput for ephemeral data. The database remains
 * crash-consistent; only the last few milliseconds of such transactions may be lost on a crash.
 *
 * @author Alexander Schwartz
 */
class PostgreSQLAsyncCommitIntegrator extends AsyncCommitIntegrator {

    private static final Logger logger = Logger.getLogger(PostgreSQLAsyncCommitIntegrator.class);

    @Override
    protected boolean isEnabled(SessionFactoryImplementor sf) {
        if (isAuroraWithLogicalReplication(sf)) {
            logger.warn("Asynchronous commit optimization disabled: Aurora PostgreSQL with logical replication " +
                    "detected. Aurora may not deliver async-committed transactions to logical decoding consumers.");
            return false;
        }
        return true;
    }

    @Override
    protected void applyAsyncCommit(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("SET LOCAL synchronous_commit TO OFF");
        }
    }

    /**
     * Detects Aurora PostgreSQL with logical replication enabled — a combination where
     * {@code SET LOCAL synchronous_commit TO OFF} can cause committed transactions to
     * never appear (or appear with extreme delay) in logical decoding consumers like Debezium.
     * <p>
     * Detection: {@code to_regproc('pg_catalog.aurora_version')} is non-null only on Aurora
     * (standard PostgreSQL returns null without logging ERROR);
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
                if (!auroraVersionFunctionExists(connection)) {
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

    /**
     * Returns whether the Aurora-only {@code aurora_version()} function exists.
     * Uses {@code to_regproc} so standard PostgreSQL does not log
     * {@code ERROR: function aurora_version() does not exist}.
     */
    static boolean auroraVersionFunctionExists(Connection connection) throws SQLException {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT to_regproc('pg_catalog.aurora_version') IS NOT NULL")) {
            return rs.next() && rs.getBoolean(1);
        }
    }
}
