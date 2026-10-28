package org.keycloak.schema.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.representations.migration.ExpectedIndexDescriptor;

import liquibase.change.Change;
import liquibase.change.core.CreateIndexChange;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.DropIndexChange;
import liquibase.change.core.DropTableChange;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.database.Database;
import liquibase.database.DatabaseList;
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
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorFactory;
import liquibase.statement.SqlStatement;

/**
 * Computes, for one dialect, the set of indexes that exist after the whole changelog has been applied.
 *
 * <p>The changelog is replayed without touching a database: {@code createIndex}/{@code dropIndex} and
 * {@code createTable}/{@code dropTable} changes maintain a transient model, and change-set preconditions are
 * evaluated against that transient model (only {@code dbms}, {@code changeSetExecuted}, {@code indexExists},
 * {@code tableExists} and the logical operators are evaluated; every other precondition defaults to true).
 * The result is stored in the manifest so the runtime index checker does not need Liquibase.
 */
final class ExpectedIndexComputer {

    private record ChangesetInfo(String id, String author, String filePath) {
    }

    private final Database database;
    private final Map<String, ExpectedIndexDescriptor> expectedIndexes = new LinkedHashMap<>();
    private final Set<ChangesetInfo> runChangesets = new HashSet<>();
    private final Set<String> tables = new HashSet<>();

    private ExpectedIndexComputer(Database database) {
        this.database = database;
    }

    static List<ExpectedIndexDescriptor> compute(DatabaseChangeLog changeLog, Database database) {
        ExpectedIndexComputer computer = new ExpectedIndexComputer(database);
        for (ChangeSet changeSet : changeLog.getChangeSets()) {
            if (computer.isChangeSetForCurrentDatabase(changeSet)) {
                computer.runChangesets.add(new ChangesetInfo(changeSet.getId(), changeSet.getAuthor(), changeSet.getFilePath()));
                for (Change change : changeSet.getChanges()) {
                    computer.apply(change, changeSet);
                }
            }
        }
        return new ArrayList<>(computer.expectedIndexes.values());
    }

    private void apply(Change change, ChangeSet changeSet) {
        if (change instanceof CreateIndexChange cic && cic.getIndexName() != null) {
            expectedIndexes.put(cic.getIndexName().toUpperCase(),
                    new ExpectedIndexDescriptor(cic.getTableName(), cic.getIndexName(), generateSql(cic, changeSet)));
        } else if (change instanceof DropIndexChange dic && dic.getIndexName() != null) {
            expectedIndexes.remove(dic.getIndexName().toUpperCase());
        } else if (change instanceof CreateTableChange ctc) {
            tables.add(ctc.getTableName());
        } else if (change instanceof DropTableChange dtc && dtc.getTableName() != null) {
            String droppedTable = dtc.getTableName();
            expectedIndexes.values().removeIf(info -> info.getTableName().equalsIgnoreCase(droppedTable));
            tables.remove(droppedTable);
        }
    }

    private String generateSql(CreateIndexChange change, ChangeSet changeSet) {
        SqlStatement[] statements = change.generateStatements(database);
        return Arrays.stream(statements)
                .flatMap(s -> Arrays.stream(SqlGeneratorFactory.getInstance().generateSql(s, database)))
                .map(Sql::toSql)
                .map(sql -> SchemaGenerator.applySqlVisitors(sql, changeSet, database))
                .map(sql -> SchemaGenerator.resolveSchemaPlaceholder(sql).trim())
                .collect(Collectors.joining("; "));
    }

    private boolean isChangeSetForCurrentDatabase(ChangeSet changeSet) {
        if (!DatabaseList.definitionMatches(changeSet.getDbmsSet(), database, true)) {
            // definitionMatches returns true if getDbmsSet() is empty or null - i.e. for all databases
            return false;
        }
        var preconditions = changeSet.getPreconditions();
        if (preconditions == null) {
            return true;
        }
        for (var precondition : preconditions.getNestedPreconditions()) {
            try {
                evaluate(precondition);
            } catch (PreconditionFailedException | PreconditionErrorException e) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluate the precondition against the transient state for indexes, tables and change-sets.
     * All conditions that are not implemented here default to "true".
     */
    private void evaluate(Precondition p) throws PreconditionFailedException, PreconditionErrorException {
        if (p instanceof DBMSPrecondition dbmsPrecondition) {
            dbmsPrecondition.check(database, null, null, null);
        } else if (p instanceof AndPrecondition andCondition) {
            List<FailedPrecondition> failures = new ArrayList<>();
            for (Precondition precondition : andCondition.getNestedPreconditions()) {
                try {
                    evaluate(precondition);
                } catch (PreconditionFailedException e) {
                    failures.addAll(e.getFailedPreconditions());
                    throw new PreconditionFailedException(failures);
                }
            }
        } else if (p instanceof NotPrecondition notPrecondition) {
            for (Precondition precondition : notPrecondition.getNestedPreconditions()) {
                boolean threwException = false;
                try {
                    evaluate(precondition);
                } catch (PreconditionFailedException e) {
                    // that's what we want with a Not precondition
                    threwException = true;
                }
                if (!threwException) {
                    throw new PreconditionFailedException("Not precondition failed", null, notPrecondition);
                }
            }
        } else if (p instanceof OrPrecondition orPrecondition) {
            List<FailedPrecondition> failures = new ArrayList<>();
            for (Precondition precondition : orPrecondition.getNestedPreconditions()) {
                try {
                    evaluate(precondition);
                    return;
                } catch (PreconditionFailedException e) {
                    failures.addAll(e.getFailedPreconditions());
                }
            }
            throw new PreconditionFailedException(failures);
        } else if (p instanceof ChangeSetExecutedPrecondition cse) {
            if (!runChangesets.contains(new ChangesetInfo(cse.getId(), cse.getAuthor(), cse.getChangeLogFile()))) {
                throw new PreconditionFailedException("Precondition failed", null, cse);
            }
        } else if (p instanceof IndexExistsPrecondition iep) {
            if (iep.getIndexName() == null || !expectedIndexes.containsKey(iep.getIndexName().toUpperCase())) {
                throw new PreconditionFailedException("Precondition failed", null, iep);
            }
        } else if (p instanceof TableExistsPrecondition tep) {
            if (!tables.contains(tep.getTableName())) {
                throw new PreconditionFailedException("Precondition failed", null, tep);
            }
        }
        // All other conditions default to true
    }
}
