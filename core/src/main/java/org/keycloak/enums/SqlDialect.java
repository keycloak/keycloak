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

    private final String manifestKey;

    SqlDialect(String manifestKey) {
        this.manifestKey = manifestKey;
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
