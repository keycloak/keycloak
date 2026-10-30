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
import java.util.List;

import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSession;

/**
 * Java database migration task that can be referenced by a {@code customTask} step of a migration manifest.
 *
 * <p>The generate-then-apply model is required so that export mode (manual strategy) can write
 * the generated statements to a script file rather than executing them directly against the database.
 *
 * <h3>Contract</h3>
 * <ol>
 *   <li>{@link #generateStatements} is called once per migration run.  Implementations may issue
 *       read-only queries against the live database (e.g. checking {@code REALM} rows) to decide which
 *       statements to return, but must not execute any DML or DDL.</li>
 *   <li>The returned statements are applied to the database ({@code update} mode) or written to the
 *       export file ({@code export} mode) by the {@link JdbcJpaUpdaterProvider}.</li>
 *   <li>All implementations must be idempotent: if a task is re-run on an already-migrated database
 *       the returned statements must either be no-ops or produce the same end state.</li>
 * </ol>
 */
public interface JpaMigrationTask {

    /**
     * Returns a unique identifier for this task.  The id is stored in the history table under the
     * same change-set identity as the parent change-set; it is not used independently for history
     * lookup.
     */
    String getTaskId();

    /**
     * Whether this task should run at all; checked by the runner before {@link #generateStatements}, so
     * implementations do not need to repeat the check. The default mirrors the former
     * {@code CustomKeycloakTask} behaviour: a task is only applicable when the {@code REALM} table exists
     * and has at least one row. This keeps tasks from querying tables that do not exist yet (export mode
     * on an empty database) and from migrating data that is not there (initial database creation).
     */
    default boolean isApplicable(Connection connection, String defaultSchema, SqlDialect dialect) throws SQLException {
        // Check via metadata first: a failing query would abort the transaction on PostgreSQL
        if (!ChangeLogHistory.namedTableExists(connection, defaultSchema, "REALM")) {
            return false;
        }
        String sql = dialect.firstRowSql(SqlDialect.qualified("REALM", defaultSchema));
        try (var st = connection.createStatement();
             var rs = st.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            // Table does not exist or is not accessible — not applicable
            return false;
        }
    }

    /**
     * Inspects the current database state and returns the SQL statements to execute.
     * This method must be read-only with respect to the database.
     *
     * @param session        live Keycloak session (may be used for model queries)
     * @param connection     live JDBC connection (read-only queries only)
     * @param defaultSchema  the configured default schema, or {@code null}
     * @param dialect        the SQL dialect of {@code connection}
     * @return ordered list of SQL statements to apply; may be empty but never {@code null}
     * @throws SQLException  on database errors during inspection
     */
    List<String> generateStatements(KeycloakSession session, Connection connection, String defaultSchema,
            SqlDialect dialect)
            throws SQLException;
}
