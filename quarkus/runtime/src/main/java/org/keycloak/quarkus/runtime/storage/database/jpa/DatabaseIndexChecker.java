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
import liquibase.change.core.DropIndexChange;
import liquibase.change.core.DropTableChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.DatabaseList;
import liquibase.database.core.H2Database;
import liquibase.database.core.MSSQLDatabase;
import liquibase.exception.LiquibaseException;
import liquibase.exception.PreconditionErrorException;
import liquibase.exception.PreconditionFailedException;
import liquibase.precondition.Precondition;
import liquibase.precondition.PreconditionLogic;
import liquibase.precondition.core.DBMSPrecondition;
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
        liquibase.getDatabaseChangeLog().getChangeSets().stream()
                .filter(cs -> isChangeSetForCurrentDatabase(cs, database))
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
                    } else if (change instanceof DropTableChange dtc && dtc.getTableName() != null) {
                        var droppedTable = dtc.getTableName();
                        expectedIndexes.values().removeIf(info -> info.tableName.equalsIgnoreCase(droppedTable));
                        logger.debugf("Drop table (%s), %s", change.getChangeSet(), droppedTable);
                    }
                });
        excludeIndexes(database, expectedIndexes);
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

    private static boolean isChangeSetForCurrentDatabase(ChangeSet changeSet, Database database) {
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
            if (containsDbmsPrecondition(precondition)) {
                try {
                    precondition.check(database, null, changeSet, null);
                } catch (PreconditionFailedException | PreconditionErrorException e) {
                    logger.debugf(e, "ChangeSet not valid for current database '%s'. %s", database.getShortName(), changeSet);
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean containsDbmsPrecondition(Precondition precondition) {
        if (precondition instanceof DBMSPrecondition) {
            return true;
        }
        if (precondition instanceof PreconditionLogic logic) {
            return logic.getNestedPreconditions().stream().anyMatch(DatabaseIndexChecker::containsDbmsPrecondition);
        }
        return false;
    }

    private static String normalizeIdentifier(String name, boolean storesLower, boolean storesUpper) {
        if (storesLower) return name.toLowerCase();
        if (storesUpper) return name.toUpperCase();
        return name;
    }

    private static void excludeIndexes(Database database, HashMap<String, IndexInfo> expectedIndexes) {
        if (database instanceof MSSQLDatabase) {
            // This is a bug. Remove this line after https://github.com/keycloak/keycloak/issues/48716 is fixed
            expectedIndexes.values().removeIf(indexInfo -> "IDX_IDP_FOR_LOGIN".equalsIgnoreCase(indexInfo.indexName));
            // This index was dropped and re-added to another databases, but not MSSQL.
            // See https://github.com/keycloak/keycloak/issues/26618#issuecomment-1964096990
            expectedIndexes.values().removeIf(indexInfo -> "IDX_CLIENT_ATT_BY_NAME_VALUE".equalsIgnoreCase(indexInfo.indexName));
        } else if (database instanceof H2Database) {
            // This index was dropped and re-added to another databases, but not H2.
            // See https://github.com/keycloak/keycloak/issues/26618#issuecomment-1964096990
            expectedIndexes.values().removeIf(indexInfo -> "IDX_CLIENT_ATT_BY_NAME_VALUE".equalsIgnoreCase(indexInfo.indexName));
        }
    }

    private record IndexInfo(String tableName, String indexName, String sql) {
    }
}
