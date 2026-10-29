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
import java.util.regex.Pattern;

import org.keycloak.enums.SqlDialect;

/**
 * SQL-building utilities shared by {@link JpaMigrationTask} implementations.
 */
public final class NativeSqlHelper {

    private NativeSqlHelper() {
    }

    // -------------------------------------------------------------------------
    // Table name qualification
    // -------------------------------------------------------------------------

    static final Pattern SIMPLE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /**
     * Returns {@code schema.table} when a schema with a plain identifier name is configured, otherwise just
     * {@code table}.
     *
     * <p>A schema that needs quoting (for example {@code my-schema}) is not embedded in SQL: the updater applies it with
     * {@link Connection#setSchema(String)}, which quotes it as the driver requires, so unqualified table names resolve
     * to it.
     */
    public static String qualified(String table, String schema) {
        if (schema == null || schema.isBlank() || !SIMPLE_IDENTIFIER.matcher(schema).matches()) {
            return table;
        }
        return schema + "." + table;
    }

    // -------------------------------------------------------------------------
    // SQL literal serialization
    // -------------------------------------------------------------------------

    /**
     * Serializes a Java value to a SQL literal safe for embedding in a statement string.
     * <ul>
     *   <li>{@code null} → {@code NULL}</li>
     *   <li>{@code Boolean} → {@code TRUE} / {@code FALSE}</li>
     *   <li>{@code Number} → decimal representation</li>
     *   <li>{@code String} → {@code 'value'} with single quotes doubled</li>
     *   <li>Everything else → {@code 'toString()'} form</li>
     * </ul>
     *
     * <p><strong>Note:</strong> this is intended for generating migration SQL that is either executed
     * directly or written to an export file.  Do not use it for user-supplied values in production
     * query paths — use parameterized statements there.
     */
    public static String sqlLiteral(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Boolean b) {
            return b ? "TRUE" : "FALSE";
        }
        if (value instanceof Number n) {
            return n.toString();
        }
        // String and everything else: single-quote with escaping
        String s = value.toString();
        return "'" + s.replace("'", "''") + "'";
    }

    /**
     * Serializes a string value to a SQL literal using dialect-aware escaping.
     *
     * <p>MySQL and MariaDB treat backslash as an escape character by default, so backslashes must be
     * doubled in addition to single quotes. All other dialects only need single quotes doubled.
     *
     * <p><strong>Note:</strong> this is intended for generating migration SQL that is either executed
     * directly or written to an export file.  Do not use it for user-supplied values in production
     * query paths — use parameterized statements there.
     */
    public static String sqlLiteral(String value, SqlDialect dialect) {
        if (value == null) {
            return "NULL";
        }
        if (dialect == SqlDialect.MYSQL || dialect == SqlDialect.MARIADB) {
            // Escape backslashes first, then single quotes, so that the backslash
            // escape sequences introduced by quote-escaping are not re-processed.
            return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
        }
        return "'" + value.replace("'", "''") + "'";
    }

    // -------------------------------------------------------------------------
    // isApplicable() — REALM row check
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the {@code REALM} table exists <em>and</em> contains at least one
     * row. 
     *
     * <p>Returns {@code false} (silently) when the table does not exist rather than propagating the
     * SQL exception, since "table not found" is a normal state on an empty database.
     *
     * @param conn    live JDBC connection (read-only)
     * @param schema  configured default schema, or {@code null}
     * @param dialect database dialect (used to pick the correct row-limiting syntax)
     */
    public static boolean isApplicable(Connection conn, String schema, SqlDialect dialect)
            throws SQLException {
        // Check via metadata first: a failing query would abort the transaction on PostgreSQL
        if (!ChangeLogHistory.namedTableExists(conn, schema, "REALM")) {
            return false;
        }
        String tableName = qualified("REALM", schema);
        String sql = firstRowSql(tableName, dialect);
        try (var st = conn.createStatement();
             var rs = st.executeQuery(sql)) {
            return rs.next();
        } catch (SQLException e) {
            // Table does not exist or is not accessible — not applicable
            return false;
        }
    }

    /**
     * Returns a SQL statement that selects one row from {@code qualifiedTable}, using the
     * dialect-appropriate row-limiting syntax.
     */
    public static String firstRowSql(String qualifiedTable, SqlDialect dialect) {
        if (dialect == SqlDialect.MSSQL) {
            return "SELECT TOP 1 1 FROM " + qualifiedTable;
        }
        if (dialect == SqlDialect.MYSQL || dialect == SqlDialect.MARIADB) {
            return "SELECT 1 FROM " + qualifiedTable + " LIMIT 1";
        }
        // ANSI SQL:2008 — works on PostgreSQL, Oracle 12c+, H2
        return "SELECT 1 FROM " + qualifiedTable + " FETCH FIRST 1 ROWS ONLY";
    }

    // -------------------------------------------------------------------------
    // Boolean literal helpers (for dialects that differ)
    // -------------------------------------------------------------------------

    /**
     * Returns the SQL literal for {@code true} on the given dialect.
     * Oracle and MSSQL use {@code 1}; all others use {@code TRUE}.
     */
    public static String trueLiteral(SqlDialect dialect) {
        return (dialect == SqlDialect.ORACLE || dialect == SqlDialect.MSSQL) ? "1" : "TRUE";
    }

    /**
     * Returns the SQL literal for {@code false} on the given dialect.
     * Oracle and MSSQL use {@code 0}; all others use {@code FALSE}.
     */
    public static String falseLiteral(SqlDialect dialect) {
        return (dialect == SqlDialect.ORACLE || dialect == SqlDialect.MSSQL) ? "0" : "FALSE";
    }
}
