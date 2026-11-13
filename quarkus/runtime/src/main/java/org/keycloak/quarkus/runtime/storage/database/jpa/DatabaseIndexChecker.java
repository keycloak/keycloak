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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProvider;
import org.keycloak.connections.jpa.updater.liquibase.custom.CustomCreateIndexChange;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.KeycloakModelUtils;

import liquibase.change.core.CreateIndexChange;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.DropIndexChange;
import liquibase.change.core.DropTableChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseList;
import liquibase.exception.LiquibaseException;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.precondition.FailedPrecondition;
import liquibase.precondition.Precondition;
import liquibase.precondition.core.AndPrecondition;
import liquibase.precondition.core.ChangeSetExecutedPrecondition;
import liquibase.precondition.core.DBMSPrecondition;
import liquibase.precondition.core.IndexExistsPrecondition;
import liquibase.precondition.core.NotPrecondition;
import liquibase.precondition.core.OrPrecondition;
import liquibase.precondition.core.TableExistsPrecondition;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import org.jboss.logging.Logger;

/**
 * Checks for missing database indexes at startup by comparing the indexes defined in the Liquibase changelogs against
 * those actually present in the database.
 *
 * <p>Keycloak's {@code CustomCreateIndexChange} may skip creating indexes on tables that exceed
 * a configurable row count threshold. When that happens, the Liquibase changeset is marked as executed, so subsequent
 * migrations will not retry. This checker detects those (and any other) missing indexes and logs a {@code WARN} with
 * the {@code CREATE INDEX} statement so operators can apply it manually.
 *
 * <p>An index whose name is already occupied by an existing index with a different definition
 * (wrong columns on the expected table, or the name taken on another table where index names
 * are unique per schema) is reported as conflicting: it is never created automatically, because
 * the creation would fail with the name already in use, and the log asks for a rename or drop
 * of the existing index instead. Where index names are unique per schema, the occupant of a
 * missing index name is looked up schema-wide, so an index on a table without expected indexes
 * cannot hide the conflict.
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

    /**
     * Marker for a function or expression index part that the database reports without a
     * column name. A changelog column defined via {@code valueComputed} never produces this
     * marker: such a definition yields no usable expected column list, so the comparison
     * matches by index name on the expected table.
     */
    private static final String EXPRESSION_COLUMN = "__expression__";

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
                    // a conflicting index name blocks the creation of the expected index, so it is
                    // reported separately and never passed to the automatic creation path
                    var conflicting = missing.stream().filter(info -> info.conflictingTable() != null).toList();
                    for (var info : conflicting) {
                        logMissingIndex(info);
                    }
                    var creatable = missing.stream().filter(info -> info.conflictingTable() == null).toList();
                    if (creatable.isEmpty()) {
                        return null;
                    }
                    try (var connection = connectionSupplier.get()) {
                        if (supportsOnlineIndexCreation(connection)) {
                            createIndexes(connection, creatable);
                            return null;
                        }
                    } catch (SQLException e) {
                        logger.warn("Unable to automatically create missing indexes, logging them for manual creation instead", e);
                    }
                    for (var info : creatable) {
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

    /**
     * The {@code CREATE INDEX} statement for each missing index whose name is not occupied by
     * a conflicting existing index: an occupied name would make the statement fail, so those
     * indexes are reported by {@link #getConflictingIndexesName()} with a rename or drop of the
     * existing index instead.
     */
    public Map<String, String> getMissingIndexesSql() {
        return getMissingIndexes().stream()
                .filter(info -> info.conflictingTable() == null)
                .collect(Collectors.toMap(IndexInfo::indexName, IndexInfo::sql));
    }

    /**
     * Names of the missing indexes whose name is already occupied by an existing index with
     * a different definition: those are never created automatically, because the creation
     * would fail with the name in use, and are logged for a manual rename or drop instead.
     */
    public List<String> getConflictingIndexesName() {
        return getMissingIndexes().stream().filter(info -> info.conflictingTable() != null).map(IndexInfo::indexName).toList();
    }

    private List<IndexInfo> getMissingIndexes() {
        try (var connection = connectionSupplier.get(); var session = factory.create()) {
            var expectedIndexes = getExpectedIndexesFromLiquibase(connection, session);
            if (expectedIndexes.isEmpty()) {
                return List.of();
            }

            var metaData = connection.getMetaData();
            var storesLower = metaData.storesLowerCaseIdentifiers();
            var storesUpper = metaData.storesUpperCaseIdentifiers();
            var productName = metaData.getDatabaseProductName().toLowerCase();

            var tablesToCheck = expectedIndexes.values().stream()
                    .map(info -> normalizeIdentifier(info.tableName, storesLower, storesUpper))
                    .collect(Collectors.toSet());
            var existingIndexes = getExistingIndexesFromDatabase(connection, metaData, storesLower, storesUpper, tablesToCheck);
            if (areIndexNamesSchemaUnique(productName)) {
                // The scan above only visits tables that have an expected index. Where index
                // names are unique per schema, an existing index with the same name on any
                // other table occupies the name as well, so such occupants are looked up
                // schema-wide and reported as conflicts instead of a create attempt that
                // cannot succeed.
                findIndexNameOccupants(connection, metaData, expectedIndexes.keySet(), existingIndexes.keySet(), tablesToCheck, productName, storesLower, storesUpper)
                        .forEach((name, table) -> existingIndexes.computeIfAbsent(name, k -> new ArrayList<>())
                                .add(new ExistingIndex(table, List.of())));
            }

            var invalidIndexNames = getInvalidIndexNames(connection, metaData, expectedIndexes.keySet());
            invalidIndexNames.forEach(existingIndexes::remove);

            return expectedIndexes.entrySet().stream()
                    .filter(e -> !isIndexPresent(e.getValue(), existingIndexes.get(e.getKey()), storesLower, storesUpper))
                    .map(e -> {
                        boolean invalid = invalidIndexNames.contains(e.getKey());
                        return new IndexInfo(e.getValue().tableName, e.getValue().indexName, e.getValue().sql, e.getValue().columns,
                                invalid, conflictingTable(e.getValue(), existingIndexes.get(e.getKey()), invalid, productName, storesLower, storesUpper));
                    })
                    .toList();
        } catch (SQLException | LiquibaseException e) {
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
        if (info.conflictingTable() != null) {
            if (info.conflictingTable().equals(info.tableName())) {
                logger.warnf("Conflicting database index %s on table %s: an index with this name already exists on this table"
                        + " with a different definition. Rename or drop the existing index, then create the expected index"
                        + " manually: %s",
                        info.indexName, info.tableName, info.sql);
            } else {
                logger.warnf("Conflicting database index %s on table %s: an index with this name already exists on table %s."
                        + " Rename or drop the existing index, then create the expected index manually: %s",
                        info.indexName, info.tableName, info.conflictingTable(), info.sql);
            }
        } else if (info.invalid) {
            logger.warnf("Invalid database index %s on table %s (possibly from a failed concurrent creation). Drop and recreate the index: DROP INDEX CONCURRENTLY IF EXISTS %s; %s",
                    info.indexName, info.tableName, qualifyPostgresIdentifier(info.indexName), addOnlineSyntax(info.sql, true, false, false));
        } else {
            logger.warnf("Missing database index %s on table %s. Create the index manually: %s",
                    info.indexName, info.tableName, info.sql);
        }
    }

    private Map<String, IndexInfo> getExpectedIndexesFromLiquibase(Connection connection, KeycloakSession session) throws LiquibaseException {
        var liquibaseProvider = session.getProvider(LiquibaseConnectionProvider.class);
        var liquibase = liquibaseProvider.getLiquibase(connection, dbSchema);

        var expectedIndexes = new HashMap<String, IndexInfo>();
        var database = liquibase.getDatabase();
        var runChangesets = new HashSet<ChangesetInfo>();
        var tables = new HashSet<TableInfo>();
        liquibase.getDatabaseChangeLog().getChangeSets().stream()
                .filter(cs -> {
                    boolean changeSetForCurrentDatabase = isChangeSetForCurrentDatabase(cs, database, expectedIndexes, runChangesets, tables);
                    if (changeSetForCurrentDatabase) {
                        runChangesets.add(new ChangesetInfo(cs.getId(), cs.getAuthor(), cs.getFilePath()));
                    }
                    return changeSetForCurrentDatabase;
                })
                .map(ChangeSet::getChanges)
                .flatMap(Collection::stream)
                .forEach(change -> {
                    if (change instanceof CreateIndexChange cic && cic.getIndexName() != null) {
                        var statement = cic instanceof CustomCreateIndexChange ?
                                ((CustomCreateIndexChange) cic).generateOriginalStatement(database) :
                                cic.generateStatements(database);
                        var sqlVisitors = change.getChangeSet().getSqlVisitors();
                        var sql = Arrays.stream(statement)
                                .flatMap(s -> Arrays.stream(SqlGeneratorFactory.getInstance().generateSql(s, database)))
                                .map(generatedSql -> {
                                    var sqlStr = generatedSql.toSql();
                                    for (var visitor : sqlVisitors) {
                                        if (DatabaseList.definitionMatches(visitor.getApplicableDbms(), database, true)) {
                                            sqlStr = visitor.modifySql(sqlStr, database);
                                        }
                                    }
                                    return sqlStr;
                                })
                                .collect(Collectors.joining("; "));
                        var info = new IndexInfo(cic.getTableName(), cic.getIndexName(), sql, expectedColumns(cic));
                        expectedIndexes.put(cic.getIndexName().toUpperCase(), info);
                        logger.debugf("Create index (%s), %s", change.getChangeSet(), info);
                    } else if (change instanceof DropIndexChange dic && dic.getIndexName() != null) {
                        var info = expectedIndexes.remove(dic.getIndexName().toUpperCase());
                        logger.debugf("Drop index (%s), %s", change.getChangeSet(), info);
                    } else if (change instanceof CreateTableChange dic) {
                        TableInfo info = new TableInfo(dic.getTableName());
                        logger.debugf("Create table (%s), %s", change.getChangeSet(), info);
                        tables.add(info);
                    } else if (change instanceof DropTableChange dtc && dtc.getTableName() != null) {
                        var droppedTable = dtc.getTableName();
                        expectedIndexes.values().removeIf(info -> info.tableName.equalsIgnoreCase(droppedTable));
                        TableInfo info = new TableInfo(droppedTable);
                        tables.remove(info);
                        logger.debugf("Drop table (%s), %s", change.getChangeSet(), droppedTable);
                    }
                });
        return expectedIndexes;
    }

    /**
     * Columns of the changelog index in ordinal order, or {@code null} when any column is an
     * expression ({@code valueComputed}) whose resulting column name cannot be compared with
     * the metadata reported by the database.
     */
    private static List<String> expectedColumns(CreateIndexChange cic) {
        List<String> columns = new ArrayList<>();
        for (var column : cic.getColumns()) {
            var name = column.getName();
            if (name == null || name.isBlank()) {
                return null;
            }
            if (column.getValueComputed() != null) {
                return null;
            }
            columns.add(name);
        }
        return columns;
    }

    /**
     * An index matches the expected changelog entry when its name is present and it is defined
     * on the same table with the same column list. When either side contains an expression
     * column (or the database did not report columns for the index), the column comparison is
     * skipped and the index matches by name on the expected table.
     */
    private static boolean isIndexPresent(IndexInfo expected, List<ExistingIndex> existing, boolean storesLower, boolean storesUpper) {
        if (existing == null) {
            return false;
        }
        for (var candidate : existing) {
            if (isIndexPresentOnExpectedTable(expected, candidate, storesLower, storesUpper)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isIndexPresentOnExpectedTable(IndexInfo expected, ExistingIndex existing, boolean storesLower, boolean storesUpper) {
        if (!existing.tableName().equals(normalizeIdentifier(expected.tableName(), storesLower, storesUpper))) {
            // an index with the same name exists on a different table: it does not satisfy this expectation
            return false;
        }
        var expectedColumns = expected.columns();
        var existingColumns = existing.columns();
        if (expectedColumns == null || existingColumns == null || expectedColumns.isEmpty() || existingColumns.isEmpty()) {
            return true;
        }
        if (expectedColumns.contains(EXPRESSION_COLUMN) || existingColumns.contains(EXPRESSION_COLUMN)) {
            return true;
        }
        if (expectedColumns.size() != existingColumns.size()) {
            return false;
        }
        for (int i = 0; i < expectedColumns.size(); i++) {
            if (!normalizeColumn(expectedColumns.get(i), storesLower, storesUpper)
                    .equals(normalizeColumn(existingColumns.get(i), storesLower, storesUpper))) {
                return false;
            }
        }
        return true;
    }

    /**
     * The table of an existing index that occupies the name of the expected index but does not
     * satisfy it, or {@code null} when the name is free. The expected index cannot be created
     * until that index is renamed or dropped, so the conflict is reported instead of a create
     * attempt that is guaranteed to fail.
     *
     * <p>For a same-named index on the expected table (with a different definition) the value is
     * the expected table itself. For a same-named index on another table the value is that table;
     * this only applies where index names are unique per schema (e.g. PostgreSQL and Oracle), since
     * MySQL, MariaDB and MSSQL scope index names per table and creating the expected index on its
     * own table succeeds there. An invalid same-named index is handled by the drop-and-recreate
     * path instead, so it is not a conflict.
     */
    private static String conflictingTable(IndexInfo expected, List<ExistingIndex> existing, boolean invalid, String productName, boolean storesLower, boolean storesUpper) {
        if (invalid || existing == null || existing.isEmpty()) {
            return null;
        }
        var expectedTable = normalizeIdentifier(expected.tableName(), storesLower, storesUpper);
        for (var candidate : existing) {
            if (candidate.tableName().equals(expectedTable)) {
                // the name is occupied on the expected table with a different definition
                return expected.tableName();
            }
        }
        if (areIndexNamesSchemaUnique(productName)) {
            // index names are unique per schema: a same-named index on any table blocks the creation
            return existing.get(0).tableName();
        }
        // index names are scoped per table: a same-named index on another table does not
        // block creating the expected index on its own table
        return null;
    }

    /**
     * Normalizes a column name for comparison: case-folded per the database identifier storage,
     * and with a Liquibase-style prefix-length suffix such as {@code BROKER_SESSION_ID(255)}
     * stripped. Indexes with a {@code valueComputed} column are never compared column by
     * column, so the stripping only matters for a plain column definition that carries such
     * a suffix while the database reports the bare column name.
     */
    private static String normalizeColumn(String column, boolean storesLower, boolean storesUpper) {
        var normalized = column.trim();
        int paren = normalized.indexOf('(');
        if (paren > 0 && normalized.endsWith(")")) {
            normalized = normalized.substring(0, paren).trim();
        }
        return normalizeIdentifier(normalized, storesLower, storesUpper);
    }

    /**
     * Whether index names are unique per schema rather than scoped per table: the case for
     * PostgreSQL, Oracle and H2. MySQL, MariaDB and MSSQL scope index names per table, so a
     * same-named index on another table does not block creating the expected index on its
     * own table there.
     */
    private static boolean areIndexNamesSchemaUnique(String productName) {
        return !(productName.contains("mysql") || productName.contains("mariadb") || productName.contains("microsoft"));
    }

    /**
     * Finds the existing indexes that occupy the name of an expected index on a table without
     * expected indexes: the per-table scan never visits those tables, but where index names
     * are unique per schema such an index still blocks creating the expected index, so it must
     * be reported as a conflict rather than left to a create attempt that cannot succeed.
     *
     * <p>Only names the per-table scan did not find anywhere are looked up: a name found on a
     * checked table is already known to the conflict detection. The columns of the occupant
     * are not captured: an occupant never sits on the table of the expected index, so only
     * its table is needed, for the conflict report.
     *
     * <p>The lookup is scoped to the schema of the checked tables: the current schema of the
     * connection when no explicit schema is configured. A null schema pattern would be treated
     * as no restriction by the JDBC metadata calls, so it must never be passed: index names
     * are unique per schema, and an occupant in another schema does not block the creation.
     *
     * <p>The lookup is best-effort: if it fails (e.g. the database rejects the query, the
     * current schema cannot be resolved, or the permissions are insufficient), the error is
     * logged and no occupant is reported.
     */
    private Map<String, String> findIndexNameOccupants(Connection connection, DatabaseMetaData metaData, Set<String> expectedIndexNames, Set<String> foundIndexNames, Set<String> checkedTables, String productName, boolean storesLower, boolean storesUpper) {
        var unfoundNames = new HashSet<>(expectedIndexNames);
        unfoundNames.removeAll(foundIndexNames);
        if (unfoundNames.isEmpty()) {
            return Map.of();
        }
        var occupants = new HashMap<String, String>();
        try {
            if (productName.contains("postgres")) {
                // the PostgreSQL driver matches the table name in getIndexInfo by equality
                // only, so query the catalog directly instead
                findPostgresIndexNameOccupants(connection, unfoundNames, checkedTables, occupants, storesLower, storesUpper);
            } else if (productName.contains("oracle")) {
                findOracleIndexNameOccupants(connection, unfoundNames, checkedTables, occupants, storesLower, storesUpper);
            } else {
                // the remaining databases match the table name in getIndexInfo exactly, so
                // the tables are enumerated first and each is queried on its own. The lookup
                // stays in the schema of the checked tables: a null schema pattern would lift
                // the restriction and report occupants from other schemas as conflicts, where
                // index names are only unique per schema and those do not block the creation.
                String metadataSchema = resolveMetadataSchema(connection, storesLower, storesUpper);
                if (metadataSchema != null) {
                    try (var tables = metaData.getTables(null, metadataSchema, "%", null)) {
                        while (tables.next()) {
                            var table = tables.getString("TABLE_NAME");
                            try (var rs = metaData.getIndexInfo(null, metadataSchema, table, false, true)) {
                                while (rs.next()) {
                                    addIndexNameOccupant(occupants, unfoundNames, checkedTables, rs.getString("INDEX_NAME"), table, storesLower, storesUpper);
                                }
                            }
                        }
                    }
                }
            }
        } catch (SQLException e) {
            logger.warn("Unable to look up existing indexes schema-wide, an index occupying the name of a missing index on a table without expected indexes may go unnoticed", e);
        }
        return occupants;
    }

    private void findPostgresIndexNameOccupants(Connection connection, Set<String> unfoundNames, Set<String> checkedTables, Map<String, String> occupants, boolean storesLower, boolean storesUpper) throws SQLException {
        String sql = dbSchema != null
                ? "SELECT UPPER(ci.relname), ct.relname FROM pg_index i JOIN pg_class ci ON ci.oid = i.indexrelid JOIN pg_class ct ON ct.oid = i.indrelid JOIN pg_namespace n ON n.oid = ct.relnamespace WHERE n.nspname = ? AND i.indisvalid"
                : "SELECT UPPER(ci.relname), ct.relname FROM pg_index i JOIN pg_class ci ON ci.oid = i.indexrelid JOIN pg_class ct ON ct.oid = i.indrelid JOIN pg_namespace n ON n.oid = ct.relnamespace WHERE n.nspname = current_schema AND i.indisvalid";
        try (var ps = connection.prepareStatement(sql)) {
            if (dbSchema != null) {
                ps.setString(1, dbSchema);
            }
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    addIndexNameOccupant(occupants, unfoundNames, checkedTables, rs.getString(1), rs.getString(2), storesLower, storesUpper);
                }
            }
        }
    }

    private void findOracleIndexNameOccupants(Connection connection, Set<String> unfoundNames, Set<String> checkedTables, Map<String, String> occupants, boolean storesLower, boolean storesUpper) throws SQLException {
        String sql = dbSchema != null
                ? "SELECT INDEX_NAME, TABLE_NAME FROM ALL_INDEXES WHERE OWNER = ?"
                : "SELECT INDEX_NAME, TABLE_NAME FROM ALL_INDEXES WHERE OWNER = SYS_CONTEXT('USERENV', 'CURRENT_SCHEMA')";
        try (var ps = connection.prepareStatement(sql)) {
            if (dbSchema != null) {
                ps.setString(1, dbSchema);
            }
            try (var rs = ps.executeQuery()) {
                while (rs.next()) {
                    addIndexNameOccupant(occupants, unfoundNames, checkedTables, rs.getString(1), rs.getString(2), storesLower, storesUpper);
                }
            }
        }
    }

    /**
     * The schema the JDBC metadata calls of the occupant lookup are scoped to: the configured
     * schema when set, else the current schema of the connection, identifier-normalized the same
     * way as the table names the schema pattern is matched against. {@code null} when the
     * current schema cannot be resolved: the lookup is skipped then, because JDBC treats a
     * null schema pattern as no restriction and a schema-wide restriction is what makes the
     * occupant lookup correct in the first place.
     */
    private String resolveMetadataSchema(Connection connection, boolean storesLower, boolean storesUpper) throws SQLException {
        String schema = dbSchema != null ? dbSchema : connection.getSchema();
        if (schema == null || schema.isBlank()) {
            return null;
        }
        return normalizeIdentifier(schema, storesLower, storesUpper);
    }

    private static void addIndexNameOccupant(Map<String, String> occupants, Set<String> unfoundNames, Set<String> checkedTables, String indexName, String tableName, boolean storesLower, boolean storesUpper) {
        if (indexName == null || tableName == null) {
            return;
        }
        var name = indexName.toUpperCase();
        if (!unfoundNames.contains(name) || occupants.containsKey(name)) {
            return;
        }
        if (checkedTables.contains(normalizeIdentifier(tableName, storesLower, storesUpper))) {
            // the per-table scan is authoritative for tables with expected indexes
            return;
        }
        occupants.put(name, tableName);
    }

    /**
     * The existing indexes of the checked tables, by index name, as reported by the JDBC
     * metadata.
     *
     * <p>The scan is kept to the schema of the checked tables: the current schema of the
     * connection when no explicit schema is configured. JDBC treats a null schema pattern
     * as no restriction, so with {@code dbSchema} unset the call can also return indexes of
     * an identically named table in another schema, which are then reported under the
     * requested table name and would wrongly satisfy the expectation; rows of other
     * schemas and catalogs are therefore filtered out.
     *
     * <p>The rows carry {@code TABLE_SCHEM} and {@code TABLE_CAT}, so a row can be
     * attributed to the requested table only when its schema and catalog match the
     * connection scope. Drivers report one or the other (e.g. the MySQL family fills the
     * catalog and leaves the schema null), and a driver that reports neither cannot leak
     * rows of other schemas in the first place: a row is only dropped when a non-null
     * reported value differs from the resolved scope.
     */
    private Map<String, List<ExistingIndex>> getExistingIndexesFromDatabase(Connection connection, DatabaseMetaData metaData, boolean storesLower, boolean storesUpper, Collection<String> tables) throws SQLException {
        var existingIndexes = new HashMap<String, List<ExistingIndex>>();
        String metadataSchema = resolveMetadataSchema(connection, storesLower, storesUpper);
        String metadataCatalog = normalizeIdentifier(connection.getCatalog(), storesLower, storesUpper);

        for (var table : tables) {
            // indexName -> (ordinal position -> column name), kept per table so an index
            // with the same name on a different table is not conflated with this one
            var indexesOnTable = new HashMap<String, TreeMap<Short, String>>();
            try (var rs = metaData.getIndexInfo(null, dbSchema, table, false, true)) {
                while (rs.next()) {
                    if (!isRowInScope(rs, metadataSchema, metadataCatalog, storesLower, storesUpper)) {
                        // the row is an index of an identically named table in another
                        // schema or catalog: it must not satisfy the expectation of this table
                        continue;
                    }
                    var indexName = rs.getString("INDEX_NAME");
                    if (indexName != null) {
                        var column = rs.getString("COLUMN_NAME");
                        // JDBC drivers report a function/expression index part with a null COLUMN_NAME
                        indexesOnTable.computeIfAbsent(indexName.toUpperCase(), k -> new TreeMap<>())
                                .put(rs.getShort("ORDINAL_POSITION"), column != null ? column : EXPRESSION_COLUMN);
                    }
                }
            }
            // On MySQL and MariaDB index names are scoped per table, so the same name may
            // legitimately exist on more than one checked table: keep every candidate.
            indexesOnTable.forEach((name, columnsByPosition) ->
                    existingIndexes.computeIfAbsent(name, k -> new ArrayList<>())
                            .add(new ExistingIndex(table, columnsByPosition.values().stream().toList())));
        }
        return existingIndexes;
    }

    /**
     * Whether a row of {@link DatabaseMetaData#getIndexInfo} belongs to the schema or
     * catalog the checked tables live in: the configured schema when set, else the current
     * schema and catalog of the connection. A row of another schema or catalog is an index
     * of an identically named table there and must not satisfy an expectation of this
     * table. Only a non-null reported value that differs rules the row out, so a driver
     * that reports neither value keeps all rows, as it cannot attribute rows of other
     * schemas to the requested table in the first place.
     */
    private static boolean isRowInScope(ResultSet rs, String metadataSchema, String metadataCatalog, boolean storesLower, boolean storesUpper) throws SQLException {
        String rowSchema = normalizeIdentifier(rs.getString("TABLE_SCHEM"), storesLower, storesUpper);
        String rowCatalog = normalizeIdentifier(rs.getString("TABLE_CAT"), storesLower, storesUpper);
        if (metadataSchema != null && rowSchema != null && !rowSchema.equals(metadataSchema)) {
            return false;
        }
        return metadataCatalog == null || rowCatalog == null || rowCatalog.equals(metadataCatalog);
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

    private static boolean isChangeSetForCurrentDatabase(ChangeSet changeSet, Database database, HashMap<String, IndexInfo> expectedIndexes, HashSet<ChangesetInfo> changes, HashSet<TableInfo> tables) {
        if (!DatabaseList.definitionMatches(changeSet.getDbmsSet(), database, true)) {
            // returns true if `getDbmsSet()` returns empty or null - i.e. for all databases
            logger.debugf("ChangeSet not valid for current database '%s'. %s", database.getShortName(), changeSet);
            return false;
        }
        var preconditions = changeSet.getPreconditions();
        if (preconditions == null) {
            // no pre-conditions
            return true;
        }
        for (var precondition : preconditions.getNestedPreconditions()) {
            try {
                evaluate(precondition, database, expectedIndexes, changes, tables);
            } catch (PreconditionFailedException | PreconditionErrorException e) {
                logger.debugf(e, "ChangeSet not valid for current database '%s'. %s", database.getShortName(), changeSet);
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluate the precondition based on the transient state for indexes, tables and changesets and the database.
     * It will not use any changelog or database information directly, as that has already been fully migrated.
     * All conditions that are not implemented here will default to "true".
     */
    private static void evaluate(Precondition p, Database database, HashMap<String, IndexInfo> expectedIndexes, HashSet<ChangesetInfo> changes, HashSet<TableInfo> tables)
            throws PreconditionFailedException, PreconditionErrorException {
        if (p instanceof DBMSPrecondition dbmsPrecondition) {
            dbmsPrecondition.check(database, null, null, null);
        } else if (p instanceof AndPrecondition andCondition) {
            boolean allPassed = true;
            List<FailedPrecondition> failures = new ArrayList<>();
            for (Precondition precondition : andCondition.getNestedPreconditions()) {
                try {
                    evaluate(precondition, database, expectedIndexes, changes, tables);
                } catch (PreconditionFailedException e) {
                    failures.addAll(e.getFailedPreconditions());
                    allPassed = false;
                    break;
                }
            }
            if (!allPassed) {
                throw new PreconditionFailedException(failures);
            }
        } else if (p instanceof NotPrecondition notPrecondition) {
            for (Precondition precondition : notPrecondition.getNestedPreconditions()) {
                boolean threwException = false;
                try {
                    evaluate(precondition, database, expectedIndexes, changes, tables);
                } catch (PreconditionFailedException e) {
                    //that's what we want with a Not precondition
                    threwException = true;
                }
                if (!threwException) {
                    throw new PreconditionFailedException("Not precondition failed", null, notPrecondition);
                }
            }
        } else if (p instanceof OrPrecondition orPrecondition) {
            boolean onePassed = false;
            List<FailedPrecondition> failures = new ArrayList<>();
            for (Precondition precondition : orPrecondition.getNestedPreconditions()) {
                try {
                    evaluate(precondition, database, expectedIndexes, changes, tables);
                    onePassed = true;
                    break;
                } catch (PreconditionFailedException e) {
                    failures.addAll(e.getFailedPreconditions());
                }
            }
            if (!onePassed) {
                throw new PreconditionFailedException(failures);
            }
        } else if (p instanceof ChangeSetExecutedPrecondition changeSetExecutedPrecondition) {
            if (!changes.contains(new ChangesetInfo(changeSetExecutedPrecondition.getId(), changeSetExecutedPrecondition.getAuthor(), changeSetExecutedPrecondition.getChangeLogFile()))) {
                throw new PreconditionFailedException("Precondition failed", null, changeSetExecutedPrecondition);
            }
        } else if (p instanceof IndexExistsPrecondition indexExistsPrecondition) {
            if (expectedIndexes.get(indexExistsPrecondition.getIndexName()) == null) {
                throw new PreconditionFailedException("Precondition failed", null, indexExistsPrecondition);
            }
        } else if (p instanceof TableExistsPrecondition tableExistsPrecondition) {
            if (!tables.contains(new TableInfo(tableExistsPrecondition.getTableName()))) {
                throw new PreconditionFailedException("Precondition failed", null, tableExistsPrecondition);
            }
        }
        // All other conditions default to true
    }

    private static String normalizeIdentifier(String name, boolean storesLower, boolean storesUpper) {
        if (name == null) {
            return null;
        }
        if (storesLower) return name.toLowerCase();
        if (storesUpper) return name.toUpperCase();
        return name;
    }

    private record IndexInfo(String tableName, String indexName, String sql, List<String> columns, boolean invalid, String conflictingTable) {
        IndexInfo(String tableName, String indexName, String sql, List<String> columns) {
            this(tableName, indexName, sql, columns, false, null);
        }
    }

    /**
     * An index found in the database: the table it is defined on and its columns in ordinal order.
     */
    private record ExistingIndex(String tableName, List<String> columns) {
    }
    private record ChangesetInfo(String id, String author, String filePath) {
    }
    private record TableInfo(String tableName) {
    }

}
