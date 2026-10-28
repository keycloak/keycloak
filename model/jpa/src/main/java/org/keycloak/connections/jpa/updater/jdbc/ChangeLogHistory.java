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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

import org.keycloak.enums.SqlDialect;
import org.keycloak.representations.migration.ChangeSetDescriptor;
import org.keycloak.representations.migration.MigrationManifest;

import org.jboss.logging.Logger;

/**
 * Reads the {@code DATABASECHANGELOG} table in a strictly read-only manner.
 * The table's existence is checked via {@link java.sql.DatabaseMetaData} so no DDL rights are required.
 * <p>
 * A history entry is identified by the triple {@code (ID, AUTHOR, FILENAME)} which is the same composite
 * key that Liquibase uses.  History rows with {@code EXECTYPE='MARK_RAN'} are considered executed for the
 * purposes of status evaluation.
 */
public class ChangeLogHistory {

    private static final Logger logger = Logger.getLogger(ChangeLogHistory.class);

    static final String CHANGELOG_TABLE = "DATABASECHANGELOG";
    static final String DEPLOYMENT_ID_COLUMN = "DEPLOYMENT_ID";

    /**
     * Immutable triple uniquely identifying a change-set in the history table.
     * Equality is case-insensitive to match Liquibase semantics.
     */
    public record ChangeSetKey(String id, String author, String filename) {

        public ChangeSetKey {
            Objects.requireNonNull(id);
            Objects.requireNonNull(author);
            Objects.requireNonNull(filename);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ChangeSetKey other)) return false;
            return id.equalsIgnoreCase(other.id)
                    && author.equalsIgnoreCase(other.author)
                    && filename.equalsIgnoreCase(other.filename);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id.toLowerCase(Locale.ROOT),
                    author.toLowerCase(Locale.ROOT),
                    filename.toLowerCase(Locale.ROOT));
        }
    }

    /**
     * Whether the {@code DATABASECHANGELOG} table exists in the given schema.
     * Uses {@link java.sql.DatabaseMetaData#getTables} and never executes any DML.
     */
    public static boolean tableExists(Connection connection, String schema) throws SQLException {
        return namedTableExists(connection, schema, CHANGELOG_TABLE);
    }

    /**
     * Whether the named table exists in the given schema.
     * Uses {@link java.sql.DatabaseMetaData#getTables} and never executes any DML.
     */
    static boolean namedTableExists(Connection connection, String schema, String tableName) throws SQLException {
        String effectiveSchema = effectiveSchema(connection, schema);
        try (ResultSet rs = connection.getMetaData().getTables(null, effectiveSchema, tableName, null)) {
            if (rs.next()) {
                return true;
            }
        }
        // Some drivers (e.g. H2) do not filter by schema when the catalog arg is null; try upper-cased table name
        try (ResultSet rs = connection.getMetaData().getTables(null, effectiveSchema, tableName.toUpperCase(Locale.ROOT), null)) {
            return rs.next();
        }
    }

    /**
     * Returns true when {@code DATABASECHANGELOG} exists but lacks the {@code DEPLOYMENT_ID} column.
     * This is used to decide whether a backfill migration is needed before inserting new rows.
     */
    public static boolean missingDeploymentIdColumn(Connection connection, String schema) throws SQLException {
        String effectiveSchema = effectiveSchema(connection, schema);
        try (ResultSet rs = connection.getMetaData().getColumns(null, effectiveSchema, CHANGELOG_TABLE, DEPLOYMENT_ID_COLUMN)) {
            if (rs.next()) {
                return false;
            }
        }
        // Try upper-cased variants
        try (ResultSet rs = connection.getMetaData().getColumns(null, effectiveSchema,
                CHANGELOG_TABLE.toUpperCase(Locale.ROOT), DEPLOYMENT_ID_COLUMN.toUpperCase(Locale.ROOT))) {
            return !rs.next();
        }
    }

    /**
     * Returns the set of change-set keys that are recorded in {@code DATABASECHANGELOG} (regardless of
     * {@code EXECTYPE}). Returns an empty set when the table does not exist.
     * <p>
     * This method is strictly read-only: it only executes {@code SELECT} statements.
     *
     * @param connection live JDBC connection (read-only access is sufficient)
     * @param schema     the default schema; {@code null} means use the connection's current schema
     * @param tableName  the actual history table name (e.g. {@code DATABASECHANGELOG} or a custom one)
     */
    public static Set<ChangeSetKey> loadExecuted(Connection connection, String schema, String tableName)
            throws SQLException {
        Set<ChangeSetKey> executed = new HashSet<>();

        // Check existence first so we can return empty without an exception on missing table.
        // This covers both the master changelog table and custom provider history tables.
        if (!namedTableExists(connection, schema, tableName)) {
            logger.debugf("History table %s does not exist — treating as empty", tableName);
            return executed;
        }

        String qualifiedTable = qualifiedName(tableName, schema);
        String sql = "SELECT ID, AUTHOR, FILENAME FROM " + qualifiedTable;
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                executed.add(new ChangeSetKey(
                        trimToEmpty(rs.getString(1)),
                        trimToEmpty(rs.getString(2)),
                        trimToEmpty(rs.getString(3))));
            }
        }
        logger.debugf("Loaded %d executed change-sets from %s", executed.size(), qualifiedTable);
        return executed;
    }

    /**
     * Returns the maximum {@code ORDEREXECUTED} value in {@code tableName}, or {@code 0} when the table
     * is empty or does not exist (as in export mode on an empty database, where the table is only written
     * to the script). Used to compute the next order value when inserting new rows.
     */
    public static int maxOrderExecuted(Connection connection, String schema, String tableName) throws SQLException {
        if (!namedTableExists(connection, schema, tableName)) {
            return 0;
        }
        String qualifiedTable = qualifiedName(tableName, schema);
        try (PreparedStatement ps = connection.prepareStatement(
                "SELECT MAX(ORDEREXECUTED) FROM " + qualifiedTable);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int val = rs.getInt(1);
                return rs.wasNull() ? 0 : val;
            }
        }
        return 0;
    }

    /**
     * Computes the list of change-sets from {@code manifest} that have not yet been recorded in
     * {@code executed}. Change-sets filtered by {@code dbms} that do not apply to {@code dialect}
     * are skipped.
     */
    public static List<ChangeSetDescriptor> computePending(MigrationManifest manifest,
            Set<ChangeSetKey> executed, SqlDialect dialect) {
        List<ChangeSetDescriptor> pending = new ArrayList<>();
        for (ChangeSetDescriptor cs : manifest.getChangeSets()) {
            if (isExcludedByDbms(cs, dialect)) {
                continue;
            }
            ChangeSetKey key = new ChangeSetKey(cs.getId(), cs.getAuthor(), cs.getChangeLogFile());
            if (!executed.contains(key)) {
                pending.add(cs);
            }
        }
        return pending;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    static String qualifiedName(String table, String schema) {
        return NativeSqlHelper.qualified(table, schema);
    }

    static String effectiveSchema(Connection connection, String schema) throws SQLException {
        if (schema != null && !schema.isBlank()) {
            return schema;
        }
        return connection.getSchema();
    }

    private static boolean isExcludedByDbms(ChangeSetDescriptor cs, SqlDialect dialect) {
        if (cs.getDbms() == null || cs.getDbms().isEmpty()) {
            return false;
        }
        // Change-set applies if any of its dbms values matches the current dialect
        for (String dbmsType : cs.getDbms()) {
            if (dialect.matchesDbmsType(dbmsType)) {
                return false;
            }
        }
        return true;
    }

    private static String trimToEmpty(String s) {
        return s == null ? "" : s.trim();
    }
}
