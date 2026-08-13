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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.keycloak.connections.jpa.updater.liquibase.conn.LiquibaseConnectionProvider;
import org.keycloak.connections.jpa.updater.liquibase.custom.CustomCreateIndexChange;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

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
 * <p>The check is non-blocking and never prevents startup. If the metadata query itself fails (e.g. insufficient
 * permissions), the error is logged and silently ignored.
 */
public class DatabaseIndexChecker implements Runnable {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final Supplier<Connection> connectionSupplier;
    private final KeycloakSessionFactory factory;
    private final String dbSchema;

    public DatabaseIndexChecker(Supplier<Connection> connectionSupplier, KeycloakSessionFactory factory, String dbSchema) {
        this.connectionSupplier = Objects.requireNonNull(connectionSupplier);
        this.factory = Objects.requireNonNull(factory);
        this.dbSchema = dbSchema;
    }

    @Override
    public void run() {
        logger.info("Running database index checker");
        var missing = getMissingIndexes();
        for (var info : missing) {
            logger.warnf("Missing database index %s on table %s. Create the index manually: %s", info.indexName, info.tableName, info.sql);
        }
    }

    public List<String> getMissingIndexesName() {
        return getMissingIndexes().stream().map(IndexInfo::indexName).toList();
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

            var tablesToCheck = expectedIndexes.values().stream()
                    .map(info -> normalizeIdentifier(info.tableName, storesLower, storesUpper))
                    .collect(Collectors.toSet());
            var existingIndexes = getExistingIndexesFromDatabase(metaData, tablesToCheck);

            return expectedIndexes.entrySet().stream().filter(e -> !existingIndexes.contains(e.getKey())).map(Map.Entry::getValue).toList();
        } catch (SQLException | LiquibaseException e) {
            logger.warn("Unable to check for missing database indexes", e);
        }
        return List.of();
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
                        var sql = Arrays.stream(statement)
                                .map(sqlStatement -> sqlStatement.getFormattedStatement(database))
                                .collect(Collectors.joining("; "));
                        var info = new IndexInfo(cic.getTableName(), cic.getIndexName(), sql);
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
        if (storesLower) return name.toLowerCase();
        if (storesUpper) return name.toUpperCase();
        return name;
    }

    private record IndexInfo(String tableName, String indexName, String sql) {
    }
    private record ChangesetInfo(String id, String author, String filePath) {
    }
    private record TableInfo(String tableName) {
    }

}
