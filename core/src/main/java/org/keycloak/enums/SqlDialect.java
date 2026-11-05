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
package org.keycloak.enums;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Database dialect detected at runtime from {@link java.sql.DatabaseMetaData}.
 * The {@link #manifestKey()} value matches the dialect keys used in the migration manifest and
 * in the {@code dbms} precondition descriptor, so that statement lookup and precondition evaluation
 * can use the same enum without string comparisons scattered across the codebase.
 */
public enum SqlDialect {

    POSTGRESQL("postgresql"),
    MYSQL("mysql"),
    MARIADB("mariadb"),
    ORACLE("oracle"),
    MSSQL("mssql"),
    H2("h2"),
    OTHER("other");

    private static final Pattern SIMPLE_IDENTIFIER = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    private final String manifestKey;

    SqlDialect(String manifestKey) {
        this.manifestKey = manifestKey;
    }

    /**
     * Whether {@code identifier} is a plain SQL identifier (letters, digits and underscores, not starting with a
     * digit) that can be embedded in SQL without quoting. Returns {@code false} for {@code null}.
     */
    public static boolean isSimpleIdentifier(String identifier) {
        return identifier != null && SIMPLE_IDENTIFIER.matcher(identifier).matches();
    }

    /**
     * Returns {@code schema.table} when a schema with a plain identifier name is configured, otherwise just
     * {@code table}.
     *
     * <p>A schema that needs quoting (for example {@code my-schema}) is not embedded in SQL: the updater applies it with
     * {@link Connection#setSchema(String)}, which quotes it as the driver requires, so unqualified table names resolve
     * to it.
     */
    public static String qualified(String table, String schema) {
        if (!isSimpleIdentifier(schema)) {
            return table;
        }
        return schema + "." + table;
    }

    /**
     * The key used in the migration manifest's {@code statements} map and in {@code dbms} precondition
     * {@code dbmsType} values. Matches the offline dialect name used by the schema generator plugin.
     */
    public String manifestKey() {
        return manifestKey;
    }

    /**
     * Detects the dialect from the live connection's database product name.
     * MariaDB is detected separately from MySQL (it reports itself as "MySQL" on older drivers but
     * also contains "MariaDB" in the version string; newer MariaDB Connector/J reports "MariaDB").
     */
    public static SqlDialect of(Connection connection) throws SQLException {
        String product = connection.getMetaData().getDatabaseProductName().toLowerCase(Locale.ROOT);
        // MariaDB connector/j reports "MariaDB"; older MySQL-compatible path: check version string
        if (product.contains("mariadb")) {
            return MARIADB;
        }
        if (product.contains("mysql")) {
            // Some MariaDB servers still expose "MySQL" as product name; check version string
            String version = connection.getMetaData().getDatabaseProductVersion().toLowerCase(Locale.ROOT);
            if (version.contains("mariadb")) {
                return MARIADB;
            }
            return MYSQL;
        }
        if (product.contains("postgres") || product.contains("enterprisedb")) {
            return POSTGRESQL;
        }
        if (product.contains("oracle")) {
            return ORACLE;
        }
        if (product.contains("sql server")) {
            return MSSQL;
        }
        if (product.startsWith("h2")) {
            return H2;
        }
        return OTHER;
    }

    /**
     * Serializes a Java value to a dialect-aware SQL literal safe for embedding in a statement string.
     * <ul>
     *   <li>{@code null} → {@code NULL}</li>
     *   <li>{@code Boolean} → {@link #trueLiteral()} / {@link #falseLiteral()}</li>
     *   <li>{@code Number} → decimal representation</li>
     *   <li>Everything else → {@code 'toString()'} with single quotes doubled; MySQL and MariaDB treat
     *       backslash as an escape character by default, so backslashes are additionally doubled there
     *       (matching Liquibase's {@code escapeStringForDatabase})</li>
     * </ul>
     *
     * <p><strong>Note:</strong> this is intended for generating migration SQL that is either executed
     * directly or written to an export file.  Do not use it for user-supplied values in production
     * query paths — use parameterized statements there.
     */
    public String literal(Object value) {
        if (value == null) {
            return "NULL";
        }
        if (value instanceof Boolean) {
            return (Boolean) value ? trueLiteral() : falseLiteral();
        }
        if (value instanceof Number) {
            return value.toString();
        }
        String escaped = value.toString().replace("'", "''");
        if (this == MYSQL || this == MARIADB) {
            escaped = escaped.replace("\\", "\\\\");
        }
        return "'" + escaped + "'";
    }

    /**
     * Returns the SQL literal for {@code true}. Oracle and MSSQL use {@code 1}; all others use {@code TRUE}.
     */
    public String trueLiteral() {
        return (this == ORACLE || this == MSSQL) ? "1" : "TRUE";
    }

    /**
     * Returns the SQL literal for {@code false}. Oracle and MSSQL use {@code 0}; all others use {@code FALSE}.
     */
    public String falseLiteral() {
        return (this == ORACLE || this == MSSQL) ? "0" : "FALSE";
    }

    /**
     * Returns a SQL statement that selects one row from {@code qualifiedTable}, using this dialect's
     * row-limiting syntax.
     */
    public String firstRowSql(String qualifiedTable) {
        if (this == MSSQL) {
            return "SELECT TOP 1 1 FROM " + qualifiedTable;
        }
        if (this == MYSQL || this == MARIADB) {
            return "SELECT 1 FROM " + qualifiedTable + " LIMIT 1";
        }
        // ANSI SQL:2008 — works on PostgreSQL, Oracle 12c+, H2
        return "SELECT 1 FROM " + qualifiedTable + " FETCH FIRST 1 ROWS ONLY";
    }

    /**
     * Returns true when this dialect matches the {@code dbmsType} string from a {@code dbms} precondition.
     * Handles aliases: {@code mariadb} matches MARIADB; {@code mysql} matches MYSQL only (not MARIADB,
     * mirroring Liquibase's behaviour).
     */
    public boolean matchesDbmsType(String dbmsType) {
        if (dbmsType == null) {
            return false;
        }
        String lower = dbmsType.toLowerCase(Locale.ROOT);
        return manifestKey.equals(lower);
    }
}
