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
        String qualifiedTable = ChangeLogHistory.qualifiedName(tableName, schema);
        return switch (dialect) {
            case ORACLE -> "CREATE TABLE " + qualifiedTable + " (" +
                    "ID VARCHAR2(255) NOT NULL, " +
                    "AUTHOR VARCHAR2(255) NOT NULL, " +
                    "FILENAME VARCHAR2(255) NOT NULL, " +
                    "DATEEXECUTED TIMESTAMP NOT NULL, " +
                    "ORDEREXECUTED INTEGER NOT NULL, " +
                    "EXECTYPE VARCHAR2(10) NOT NULL, " +
                    "MD5SUM VARCHAR2(35), " +
                    "DESCRIPTION VARCHAR2(255), " +
                    "COMMENTS VARCHAR2(255), " +
                    "TAG VARCHAR2(255), " +
                    "LIQUIBASE VARCHAR2(20), " +
                    "CONTEXTS VARCHAR2(255), " +
                    "LABELS VARCHAR2(255), " +
                    "DEPLOYMENT_ID VARCHAR2(10))";
            case MSSQL -> "CREATE TABLE " + qualifiedTable + " (" +
                    "ID nvarchar(255) NOT NULL, " +
                    "AUTHOR nvarchar(255) NOT NULL, " +
                    "FILENAME nvarchar(255) NOT NULL, " +
                    "DATEEXECUTED datetime2(3) NOT NULL, " +
                    "ORDEREXECUTED int NOT NULL, " +
                    "EXECTYPE nvarchar(10) NOT NULL, " +
                    "MD5SUM nvarchar(35), " +
                    "DESCRIPTION nvarchar(255), " +
                    "COMMENTS nvarchar(255), " +
                    "TAG nvarchar(255), " +
                    "LIQUIBASE nvarchar(20), " +
                    "CONTEXTS nvarchar(255), " +
                    "LABELS nvarchar(255), " +
                    "DEPLOYMENT_ID nvarchar(10))";
            case MYSQL, MARIADB -> "CREATE TABLE " + qualifiedTable + " (" +
                    "ID varchar(255) NOT NULL, " +
                    "AUTHOR varchar(255) NOT NULL, " +
                    "FILENAME varchar(255) NOT NULL, " +
                    "DATEEXECUTED datetime NOT NULL, " +
                    "ORDEREXECUTED INT NOT NULL, " +
                    "EXECTYPE varchar(10) NOT NULL, " +
                    "MD5SUM varchar(35), " +
                    "DESCRIPTION varchar(255), " +
                    "COMMENTS varchar(255), " +
                    "TAG varchar(255), " +
                    "LIQUIBASE varchar(20), " +
                    "CONTEXTS varchar(255), " +
                    "LABELS varchar(255), " +
                    "DEPLOYMENT_ID varchar(10))";
            default -> // PostgreSQL, H2, OTHER
                    "CREATE TABLE " + qualifiedTable + " (" +
                    "ID VARCHAR(255) NOT NULL, " +
                    "AUTHOR VARCHAR(255) NOT NULL, " +
                    "FILENAME VARCHAR(255) NOT NULL, " +
                    "DATEEXECUTED TIMESTAMP NOT NULL, " +
                    "ORDEREXECUTED INT NOT NULL, " +
                    "EXECTYPE VARCHAR(10) NOT NULL, " +
                    "MD5SUM VARCHAR(35), " +
                    "DESCRIPTION VARCHAR(255), " +
                    "COMMENTS VARCHAR(255), " +
                    "TAG VARCHAR(255), " +
                    "LIQUIBASE VARCHAR(20), " +
                    "CONTEXTS VARCHAR(255), " +
                    "LABELS VARCHAR(255), " +
                    "DEPLOYMENT_ID VARCHAR(10))";
        };
    }

    /**
     * Returns the {@code ALTER TABLE ... ADD CONSTRAINT ... PRIMARY KEY} statement needed by MySQL and
     * MariaDB (where Liquibase uses {@code MySQLCustomChangeLogHistoryService}).
     */
    public static String mysqlAddPrimaryKeySql(String schema, String tableName) {
        String qualifiedTable = ChangeLogHistory.qualifiedName(tableName, schema);
        return "ALTER TABLE " + qualifiedTable +
                " ADD CONSTRAINT PK_DATABASECHANGELOG PRIMARY KEY (ID, AUTHOR, FILENAME)";
    }

    // -------------------------------------------------------------------------
    // DEPLOYMENT_ID backfill
    // -------------------------------------------------------------------------

    /**
     * Returns the {@code ALTER TABLE ... ADD COLUMN DEPLOYMENT_ID} statement.
     */
    public static String addDeploymentIdColumnSql(SqlDialect dialect, String schema, String tableName) {
        String qualifiedTable = ChangeLogHistory.qualifiedName(tableName, schema);
        String colType = switch (dialect) {
            case ORACLE -> "VARCHAR2(10)";
            case MSSQL -> "nvarchar(10)";
            case MYSQL, MARIADB -> "varchar(10)";
            default -> "VARCHAR(10)";
        };
        return "ALTER TABLE " + qualifiedTable + " ADD DEPLOYMENT_ID " + colType;
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
     * Inserts a row into the history table recording execution of a change-set.
     *
     * @param execType     {@code "EXECUTED"} or {@code "MARK_RAN"}
     * @param orderExecuted the next {@code ORDEREXECUTED} value (caller must compute this)
     */
    public static void insertHistoryRow(Connection connection, String schema, String tableName,
            String id, String author, String filename, String md5sum, String description,
            String execType, int orderExecuted, String deploymentId, String liquibaseVersion) throws SQLException {
        String qualifiedTable = ChangeLogHistory.qualifiedName(tableName, schema);
        String sql = "INSERT INTO " + qualifiedTable +
                " (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, EXECTYPE, LIQUIBASE, DEPLOYMENT_ID)" +
                " VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, truncate(id, 255));
            ps.setString(2, truncate(author, 255));
            ps.setString(3, truncate(filename, 255));
            ps.setInt(4, orderExecuted);
            ps.setString(5, truncate(md5sum, 35));
            ps.setString(6, truncate(description, 255));
            ps.setString(7, execType);
            ps.setString(8, truncate(liquibaseVersion, 20));
            ps.setString(9, truncate(deploymentId, 10));
            ps.executeUpdate();
        }
    }

    /**
     * Formats the history INSERT as a SQL literal string for the export script.
     */
    public static String historyInsertSql(String schema, String tableName,
            String id, String author, String filename, String md5sum, String description,
            String execType, int orderExecuted, String deploymentId, String liquibaseVersion) {
        String qualifiedTable = ChangeLogHistory.qualifiedName(tableName, schema);
        return "INSERT INTO " + qualifiedTable +
                " (ID, AUTHOR, FILENAME, DATEEXECUTED, ORDEREXECUTED, MD5SUM, DESCRIPTION, EXECTYPE, LIQUIBASE, DEPLOYMENT_ID)" +
                " VALUES (" +
                sqlStr(id, 255) + ", " +
                sqlStr(author, 255) + ", " +
                sqlStr(filename, 255) + ", " +
                "CURRENT_TIMESTAMP, " +
                orderExecuted + ", " +
                sqlStr(md5sum, 35) + ", " +
                sqlStr(description, 255) + ", " +
                sqlStr(execType, 10) + ", " +
                sqlStr(liquibaseVersion, 20) + ", " +
                sqlStr(deploymentId, 10) +
                ")";
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static String sqlStr(String s, int max) {
        if (s == null) return "NULL";
        String v = truncate(s, max);
        // Escape single quotes
        return "'" + v.replace("'", "''") + "'";
    }
}
