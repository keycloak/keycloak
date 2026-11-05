/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.connections.jpa.updater.jdbc;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.jboss.logging.Logger;

/**
 * {@link SqlSink} that executes SQL directly on a JDBC connection ({@code update} mode).
 * Each SQL statement is sent in its own {@link Statement} on the provided connection.
 * Transaction management (commit/rollback per change-set) is handled by {@link ChangeSetRunner}.
 */
public class JdbcSqlSink implements SqlSink {

    private static final Logger logger = Logger.getLogger(JdbcSqlSink.class);

    private final Connection connection;

    public JdbcSqlSink(Connection connection) {
        this.connection = connection;
    }

    @Override
    public void execute(String sql, String changeSetId) throws SQLException {
        logger.tracef("[%s] Executing: %s", changeSetId, sql);
        try (Statement st = connection.createStatement()) {
            st.execute(sql);
        }
    }

    @Override
    public void comment(String text) {
        // Comments are no-ops in live execution mode
    }

    @Override
    public boolean isExportMode() {
        return false;
    }
}
