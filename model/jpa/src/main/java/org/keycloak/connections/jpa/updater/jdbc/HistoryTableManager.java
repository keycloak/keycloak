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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.keycloak.enums.SqlDialect;

import org.jboss.logging.Logger;

/**
 * Manages the {@code DATABASECHANGELOG} history table DDL.
 *
 * <ul>
 *   <li>Generates the dialect-specific {@code CREATE TABLE} DDL.</li>
 *   <li>Handles the MySQL/MariaDB-specific primary key addition.</li>
 *   <li>Handles backfilling the {@code DEPLOYMENT_ID} column when upgrading from older Liquibase installations.</li>
 * </ul>
 *
 * Column types follow Liquibase's {@code CreateDatabaseChangeLogTableGenerator} exactly so that
 * Liquibase and the native runner can be used interchangeably on the same database.
 */
public class HistoryTableManager {

    private static final Logger logger = Logger.getLogger(HistoryTableManager.class);

    /**
     * Fallback for the {@code LIQUIBASE} column when a manifest does not carry the version of the
     * Liquibase that generated it (see {@link org.keycloak.representations.migration.MigrationManifest#getLiquibaseVersion()}).
     */
    static final String DEFAULT_LIQUIBASE_VERSION = "4.33.0";

    private HistoryTableManager() {
    }

    // -------------------------------------------------------------------------
    // CREATE TABLE DDL
    // -------------------------------------------------------------------------

    /**
     * Returns the {@code CREATE TABLE DATABASECHANGELOG} DDL for the given dialect and schema.
     * The table name is schema-qualified when {@code schema} is non-null.
     */
    public static String createTableSql(SqlDialect dialect, String schema, String tableName) {
        String v255 = varchar(dialect, 255);
        String v10 = varchar(dialect, 10);
        String v35 = varchar(dialect, 35);
        String v20 = varchar(dialect, 20);
        String timestamp = switch (dialect) {
            case MSSQL -> "datetime2(3)";
            case MYSQL, MARIADB -> "datetime";
            default -> "TIMESTAMP"; // Oracle, PostgreSQL, H2, OTHER
        };
        String integer = switch (dialect) {
            case ORACLE -> "INTEGER";
            case MSSQL -> "int";
            default -> "INT";
        };
        return "CREATE TABLE " + ChangeLogHistory.qualifiedName(tableName, schema) + " (" +
                "ID " + v255 + " NOT NULL, " +
                "AUTHOR " + v255 + " NOT NULL, " +
                "FILENAME " + v255 + " NOT NULL, " +
                "DATEEXECUTED " + timestamp + " NOT NULL, " +
                "ORDEREXECUTED " + integer + " NOT NULL, " +
                "EXECTYPE " + v10 + " NOT NULL, " +
                "MD5SUM " + v35 + ", " +
                "DESCRIPTION " + v255 + ", " +
                "COMMENTS " + v255 + ", " +
                "TAG " + v255 + ", " +
                "LIQUIBASE " + v20 + ", " +
                "CONTEXTS " + v255 + ", " +
                "LABELS " + v255 + ", " +
                "DEPLOYMENT_ID " + v10 + ")";
    }

    /** The dialect-specific variable length character type used by Liquibase. */
    private static String varchar(SqlDialect dialect, int length) {
        String type = switch (dialect) {
            case ORACLE -> "VARCHAR2";
            case MSSQL -> "nvarchar";
            case MYSQL, MARIADB -> "varchar";
            default -> "VARCHAR"; // PostgreSQL, H2, OTHER
        };
        return type + "(" + length + ")";
    }

    /**
     * Returns the {@code ALTER TABLE ... ADD PRIMARY KEY} statement needed by MySQL and
     * MariaDB (where Liquibase uses {@code MySQLCustomChangeLogHistoryService}).
     */
    public static String mysqlAddPrimaryKeySql(String schema, String tableName) {
        String qualifiedTable = ChangeLogHistory.qualifiedName(tableName, schema);
        return "ALTER TABLE " + qualifiedTable + " ADD PRIMARY KEY (ID, AUTHOR, FILENAME)";
    }

    // -------------------------------------------------------------------------
    // DEPLOYMENT_ID backfill
    // -------------------------------------------------------------------------

    /**
     * Returns the {@code ALTER TABLE ... ADD COLUMN DEPLOYMENT_ID} statement.
     */
    public static String addDeploymentIdColumnSql(SqlDialect dialect, String schema, String tableName) {
        String qualifiedTable = ChangeLogHistory.qualifiedName(tableName, schema);
        return "ALTER TABLE " + qualifiedTable + " ADD DEPLOYMENT_ID " + varchar(dialect, 10);
    }

    /** Returns the {@code UPDATE} statement that sets the given {@code deploymentId} on all existing rows. */
    public static String backfillDeploymentIdSql(String schema, String tableName) {
        return "UPDATE " + ChangeLogHistory.qualifiedName(tableName, schema) + " SET DEPLOYMENT_ID = ?";
    }

    /** Returns the {@code ALTER TABLE ... ALTER COLUMN ... NOT NULL} statement. */
    public static String setDeploymentIdNotNullSql(SqlDialect dialect, String schema, String tableName) {
        String qualifiedTable = ChangeLogHistory.qualifiedName(tableName, schema);
        // Syntax differs between databases
        return switch (dialect) {
            case ORACLE -> "ALTER TABLE " + qualifiedTable + " MODIFY DEPLOYMENT_ID NOT NULL";
            case MSSQL -> "ALTER TABLE " + qualifiedTable + " ALTER COLUMN DEPLOYMENT_ID nvarchar(10) NOT NULL";
            case MYSQL, MARIADB -> "ALTER TABLE " + qualifiedTable + " MODIFY DEPLOYMENT_ID varchar(10) NOT NULL";
            default -> "ALTER TABLE " + qualifiedTable + " ALTER COLUMN DEPLOYMENT_ID SET NOT NULL";
        };
    }

    /**
     * Executes the three-step DEPLOYMENT_ID backfill (ADD COLUMN → UPDATE → NOT NULL) on the live
     * connection and commits each step individually.
     */
    public static void backfillDeploymentId(Connection connection, SqlDialect dialect, String schema,
            String tableName, String deploymentId) throws SQLException {
        logger.debugf("Adding missing DEPLOYMENT_ID column to %s with value %s",
                ChangeLogHistory.qualifiedName(tableName, schema), deploymentId);

        try (java.sql.Statement st = connection.createStatement()) {
            st.execute(addDeploymentIdColumnSql(dialect, schema, tableName));
            connection.commit();
        }

        try (PreparedStatement ps = connection.prepareStatement(backfillDeploymentIdSql(schema, tableName))) {
            ps.setString(1, deploymentId);
            ps.executeUpdate();
            connection.commit();
        }

        try (java.sql.Statement st = connection.createStatement()) {
            st.execute(setDeploymentIdNotNullSql(dialect, schema, tableName));
            connection.commit();
        }
    }

    // -------------------------------------------------------------------------
    // History INSERT
    // -------------------------------------------------------------------------

    /**
     * Inserts a row into the history table recording execution of a change-set. The row has the same content
     * as the one Liquibase's {@code MarkChangeSetRanGenerator} writes.
     *
     * @param execType     {@code "EXECUTED"} or {@code "MARK_RAN"}
     * @param orderExecuted the next {@code ORDEREXECUTED} value (caller must compute this)
     */
    public static void insertHistoryRow(Connection connection, SqlDialect dialect, String schema, String tableName,
            String id, String author, String filename, String md5sum, String description, String comments,
            String execType, int orderExecuted, String deploymentId, String liquibaseVersion) throws SQLException {
        String sql = "INSERT INTO " + ChangeLogHistory.qualifiedName(tableName, schema) +
                " (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, " + descriptionColumn(dialect) +
                ", COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID)" +
                " VALUES (?, ?, ?, " + currentDateTimeFunction(dialect) + ", ?, ?, ?, ?, ?, NULL, NULL, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, truncate(id, 255));
            ps.setString(2, truncate(author, 255));
            ps.setString(3, truncate(filename, 255));
            ps.setInt(4, orderExecuted);
            ps.setString(5, truncate(md5sum, 35));
            ps.setString(6, limitSize(description, 250));
            ps.setString(7, limitSize(comments(comments), 250));
            ps.setString(8, execType);
            ps.setString(9, truncate(liquibaseVersion, 20));
            ps.setString(10, truncate(deploymentId, 10));
            ps.executeUpdate();
        }
    }

    /**
     * Formats the history INSERT as a SQL literal string for the export script, with the same column order
     * and values as the statement Liquibase's {@code MarkChangeSetRanGenerator} generates.
     */
    public static String historyInsertSql(SqlDialect dialect, String schema, String tableName,
            String id, String author, String filename, String md5sum, String description, String comments,
            String execType, int orderExecuted, String deploymentId, String liquibaseVersion) {
        return "INSERT INTO " + ChangeLogHistory.qualifiedName(tableName, schema) +
                " (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, " + descriptionColumn(dialect) +
                ", COMMENTS, EXECTYPE, CONTEXTS, LABELS, LIQUIBASE, DEPLOYMENT_ID)" +
                " VALUES (" +
                sqlStr(dialect, id, 255) + ", " +
                sqlStr(dialect, author, 255) + ", " +
                sqlStr(dialect, filename, 255) + ", " +
                currentDateTimeFunction(dialect) + ", " +
                orderExecuted + ", " +
                sqlStr(dialect, md5sum, 35) + ", " +
                sqlStr(dialect, limitSize(description, 250), 255) + ", " +
                sqlStr(dialect, limitSize(comments(comments), 250), 255) + ", " +
                sqlStr(dialect, execType, 10) + ", " +
                "NULL, NULL, " +
                sqlStr(dialect, liquibaseVersion, 20) + ", " +
                sqlStr(dialect, deploymentId, 10) +
                ")";
    }

    /** Liquibase quotes {@code DESCRIPTION} on MySQL/MariaDB, where it is a reserved word. */
    private static String descriptionColumn(SqlDialect dialect) {
        return dialect == SqlDialect.MYSQL || dialect == SqlDialect.MARIADB ? "`DESCRIPTION`" : "DESCRIPTION";
    }

    /** The function Liquibase uses for the current timestamp of each database. */
    private static String currentDateTimeFunction(SqlDialect dialect) {
        return switch (dialect) {
            case ORACLE -> "SYSTIMESTAMP";
            case MSSQL -> "GETDATE()";
            case OTHER -> "CURRENT_TIMESTAMP";
            default -> "NOW()";
        };
    }

    private static String comments(String comments) {
        return comments == null ? "" : comments.trim();
    }

    /** Same as Liquibase's {@code StringUtil.limitSize}: cut to {@code max} characters, ending in "...". */
    private static String limitSize(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max - 3) + "..." : s;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String sqlStr(SqlDialect dialect, String s, int max) {
        return NativeSqlHelper.sqlLiteral(truncate(s, max), dialect);
    }
}
