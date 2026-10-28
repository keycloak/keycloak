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

package org.keycloak.quarkus.runtime.storage.database.jpa;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.jpa.updater.jdbc.MigrationManifestLoader;
import org.keycloak.enums.SqlDialect;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import org.jboss.logging.Logger;

/**
 * Checks for missing database indexes at startup by comparing the indexes recorded in the generated migration
 * manifest against those actually present in the database.
 *
 * <p>The migration runner may skip creating indexes on tables that exceed
 * a configurable row count threshold. When that happens, the change-set is marked as executed, so subsequent
 * migrations will not retry. This checker detects those (and any other) missing indexes and logs a {@code WARN} with
 * the {@code CREATE INDEX} statement so operators can apply it manually.
 *
 * <p>The check is non-blocking and never prevents startup. If the metadata query itself fails (e.g. insufficient
 * permissions), the error is logged and silently ignored.
 */
public class DatabaseIndexChecker implements Runnable {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final Supplier<Connection> connectionSupplier;
    private final KeycloakSessionFactory factory;
    private final String dbSchema;
    private final boolean autoCreate;
    private final int migrationTimeoutSeconds;

    public DatabaseIndexChecker(Supplier<Connection> connectionSupplier, KeycloakSessionFactory factory, String dbSchema, boolean autoCreate, int migrationTimeoutSeconds) {
        this.connectionSupplier = Objects.requireNonNull(connectionSupplier);
        this.factory = Objects.requireNonNull(factory);
        this.dbSchema = dbSchema;
        this.autoCreate = autoCreate;
        this.migrationTimeoutSeconds = migrationTimeoutSeconds;
    }

    @Override
    public void run() {
        KeycloakModelUtils.setTransactionLimit(factory, migrationTimeoutSeconds);
        try {
            runInternal();
        } finally {
            KeycloakModelUtils.setTransactionLimit(factory, 0);
        }
    }

    private static final String TASK_KEY = "db-index-checker";

    private void runInternal() {
        logger.debug("Running database index checker");

        if (autoCreate) {
            try (var session = factory.create()) {
                var clusterProvider = session.getProvider(ClusterProvider.class);
                clusterProvider.executeIfNotExecuted(TASK_KEY, migrationTimeoutSeconds, () -> {
                    var missing = getMissingIndexes();
                    if (missing.isEmpty()) {
                        return null;
                    }
                    try (var connection = connectionSupplier.get()) {
                        if (supportsOnlineIndexCreation(connection)) {
                            createIndexes(connection, missing);
                            return null;
                        }
                    } catch (SQLException e) {
                        logger.warn("Unable to automatically create missing indexes, logging them for manual creation instead", e);
                    }
                    for (var info : missing) {
                        logMissingIndex(info);
                    }
                    return null;
                });
            }
        } else {
            var missing = getMissingIndexes();
            if (missing.isEmpty()) {
                return;
            }

            for (var info : missing) {
                logMissingIndex(info);
            }
        }
    }

    public List<String> getMissingIndexesName() {
        return getMissingIndexes().stream().map(IndexInfo::indexName).toList();
    }

    public Map<String, String> getMissingIndexesSql() {
        return getMissingIndexes().stream()
                .collect(Collectors.toMap(IndexInfo::indexName, IndexInfo::sql));
    }

    private List<IndexInfo> getMissingIndexes() {
        try (var connection = connectionSupplier.get()) {
            var expectedIndexes = getExpectedIndexes(connection);
            if (expectedIndexes.isEmpty()) {
                return List.of();
            }

            var metaData = connection.getMetaData();
            var storesLower = metaData.storesLowerCaseIdentifiers();
            var storesUpper = metaData.storesUpperCaseIdentifiers();

            var tablesToCheck = expectedIndexes.values().stream()
                    .map(info -> normalizeIdentifier(info.tableName, storesLower, storesUpper))
                    .collect(Collectors.toSet());
            var existingIndexes = getExistingIndexesFromDatabase(metaData, tablesToCheck);

            var invalidIndexNames = getInvalidIndexNames(connection, metaData, expectedIndexes.keySet());
            existingIndexes.removeAll(invalidIndexNames);

            return expectedIndexes.entrySet().stream()
                    .filter(e -> !existingIndexes.contains(e.getKey()))
                    .map(e -> new IndexInfo(e.getValue().tableName, e.getValue().indexName, e.getValue().sql, invalidIndexNames.contains(e.getKey())))
                    .toList();
        } catch (SQLException | RuntimeException e) {
            logger.warn("Unable to check for missing database indexes", e);
        }
        return List.of();
    }

    private boolean supportsOnlineIndexCreation(Connection connection) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName().toLowerCase();
        if (productName.contains("postgres")
                || productName.contains("mysql") || productName.contains("mariadb")) {
            return true;
        }
        if (productName.contains("oracle")) {
            try (var stmt = connection.createStatement();
                 var rs = stmt.executeQuery("SELECT BANNER FROM V$VERSION WHERE BANNER LIKE 'Oracle%' AND ROWNUM <= 1")) {
                if (rs.next()) {
                    String banner = rs.getString(1);
                    return banner != null && banner.toUpperCase().contains("ENTERPRISE");
                }
            }
            return false;
        }
        if (productName.contains("microsoft")) {
            try (var stmt = connection.createStatement();
                 var rs = stmt.executeQuery("SELECT CAST(SERVERPROPERTY('EngineEdition') AS INT)")) {
                if (rs.next()) {
                    int edition = rs.getInt(1);
                    return edition == 3 || edition == 5 || edition == 8;
                }
            }
        }
        return false;
    }

    private void createIndexes(Connection connection, List<IndexInfo> indexes) throws SQLException {
        String productName = connection.getMetaData().getDatabaseProductName().toLowerCase();
        boolean isPostgres = productName.contains("postgres");
        boolean isOracle = productName.contains("oracle");
        boolean isMssql = productName.contains("microsoft");

        // The manifest SQL is not schema-qualified; resolve names against the configured schema.
        String origSchema = connection.getSchema();
        boolean schemaChanged = dbSchema != null && !dbSchema.equals(origSchema);
        if (schemaChanged) {
            connection.setSchema(dbSchema);
        }

        boolean origAutoCommit = connection.getAutoCommit();
        if (isPostgres && !origAutoCommit) {
            // CREATE INDEX CONCURRENTLY requires running outside a transaction block.
            // Connection pool wrappers (e.g. Agroal) may block setAutoCommit on enlisted
            // connections, so fall back to the underlying connection.
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                connection.unwrap(Connection.class).setAutoCommit(true);
            }
        }
        try {
            for (var info : indexes) {
                try {
                    if (info.invalid && isPostgres) {
                        // On PostgreSQL, CREATE INDEX CONCURRENTLY can leave an invalid index if it fails mid-build (e.g. timeout, deadlock, or crash).
                        // The invalid index occupies the name but doesn't serve queries, so it must be dropped before the index can be recreated.
                        String qualifiedIndex = qualifyPostgresIdentifier(info.indexName);
                        logger.infov("Dropping invalid index {0} before recreating", qualifiedIndex);
                        try (var stmt = connection.createStatement()) {
                            stmt.execute("DROP INDEX CONCURRENTLY IF EXISTS " + qualifiedIndex);
                        }
                    }
                    String sql = addOnlineSyntax(info.sql, isPostgres, isOracle, isMssql);
                    logger.infov("Creating index: {0}", sql);
                    try (var stmt = connection.createStatement()) {
                        stmt.execute(sql);
                    }
                    logger.infov("Successfully created index {0}", info.indexName);
                } catch (SQLException e) {
                    if (isPostgres) {
                        String qualifiedIndex = qualifyPostgresIdentifier(info.indexName);
                        logger.warnf("Failed to create index %s automatically: %s. Drop and recreate the index manually: DROP INDEX CONCURRENTLY IF EXISTS %s; %s",
                                info.indexName, e.getMessage(), qualifiedIndex, addOnlineSyntax(info.sql, true, false, false));
                    } else {
                        logger.warnf("Failed to create index %s automatically: %s. Create the index manually: %s",
                                info.indexName, e.getMessage(), info.sql);
                    }
                }
            }
        } finally {
            if (isPostgres && !origAutoCommit) {
                try {
                    connection.setAutoCommit(origAutoCommit);
                } catch (SQLException e) {
                    connection.unwrap(Connection.class).setAutoCommit(origAutoCommit);
                }
            }
            if (schemaChanged) {
                connection.setSchema(origSchema);
            }
        }
    }

    static String addOnlineSyntax(String sql, boolean isPostgres, boolean isOracle, boolean isMssql) {
        if (isPostgres) {
            return sql.replaceFirst("(?i)(CREATE\\s+(?:UNIQUE\\s+)?INDEX\\s+)", "$1CONCURRENTLY ");
        }
        if (isOracle) {
            return sql + " ONLINE";
        }
        if (isMssql) {
            int withPos = sql.toUpperCase().lastIndexOf("WITH (");
            if (withPos >= 0) {
                return sql.substring(0, withPos) + "WITH (ONLINE = ON, " + sql.substring(withPos + 6);
            }
            return sql + " WITH (ONLINE = ON)";
        }
        return sql;
    }

    private String qualifyPostgresIdentifier(String identifier) {
        // PostgreSQL folds unquoted identifiers to lowercase
        String quoted = "\"" + identifier.toLowerCase().replace("\"", "\"\"") + "\"";
        if (dbSchema != null) {
            return "\"" + dbSchema.replace("\"", "\"\"") + "\"." + quoted;
        }
        return quoted;
    }

    private void logMissingIndex(IndexInfo info) {
        if (info.invalid) {
            logger.warnf("Invalid database index %s on table %s (possibly from a failed concurrent creation). Drop and recreate the index: DROP INDEX CONCURRENTLY IF EXISTS %s; %s",
                    info.indexName, info.tableName, qualifyPostgresIdentifier(info.indexName), addOnlineSyntax(info.sql, true, false, false));
        } else {
            logger.warnf("Missing database index %s on table %s. Create the index manually: %s",
                    info.indexName, info.tableName, info.sql);
        }
    }

    private Map<String, IndexInfo> getExpectedIndexes(Connection connection) throws SQLException {
        var dialect = SqlDialect.of(connection);
        var descriptors = MigrationManifestLoader.load().getExpectedIndexes().get(dialect.manifestKey());
        var expectedIndexes = new HashMap<String, IndexInfo>();
        if (descriptors != null) {
            for (var descriptor : descriptors) {
                var info = new IndexInfo(descriptor.getTableName(), descriptor.getIndexName(), descriptor.getSql());
                expectedIndexes.put(descriptor.getIndexName().toUpperCase(), info);
            }
        }
        return expectedIndexes;
    }

    private Set<String> getExistingIndexesFromDatabase(DatabaseMetaData metaData, Collection<String> tables) throws SQLException {
        var existingIndexes = new HashSet<String>();

        for (var table : tables) {
            try (var rs = metaData.getIndexInfo(null, dbSchema, table, false, true)) {
                while (rs.next()) {
                    var indexName = rs.getString("INDEX_NAME");
                    if (indexName != null) {
                        existingIndexes.add(indexName.toUpperCase());
                    }
                }
            }
        }
        return existingIndexes;
    }

    // On PostgreSQL, CREATE INDEX CONCURRENTLY can leave an invalid index if it fails mid-build (e.g. timeout, deadlock, or crash).
    // The invalid index occupies the name but doesn't serve queries, so it must be dropped before the index can be recreated.
    private Set<String> getInvalidIndexNames(Connection connection, DatabaseMetaData metaData, Set<String> expectedIndexNames) throws SQLException {
        if (!metaData.getDatabaseProductName().toLowerCase().contains("postgres")) {
            return Set.of();
        }
        var invalidIndexes = new HashSet<String>();
        String sql = dbSchema != null
                ? "SELECT UPPER(c.relname) FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = ? AND NOT i.indisvalid"
                : "SELECT UPPER(c.relname) FROM pg_index i JOIN pg_class c ON c.oid = i.indexrelid JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = current_schema AND NOT i.indisvalid";
        try (var ps = connection.prepareStatement(sql)) {
            if (dbSchema != null) {
                ps.setString(1, dbSchema);
            }
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    var name = rs.getString(1);
                    if (expectedIndexNames.contains(name)) {
                        invalidIndexes.add(name);
                    }
                }
            }
        }
        return invalidIndexes;
    }

    private static String normalizeIdentifier(String name, boolean storesLower, boolean storesUpper) {
        if (storesLower) return name.toLowerCase();
        if (storesUpper) return name.toUpperCase();
        return name;
    }

    private record IndexInfo(String tableName, String indexName, String sql, boolean invalid) {
        IndexInfo(String tableName, String indexName, String sql) {
            this(tableName, indexName, sql, false);
        }
    }

}
