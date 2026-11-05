package org.keycloak.schema.generator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.representations.migration.ExpectedIndexDescriptor;

import liquibase.change.Change;
import liquibase.change.ColumnConfig;
import liquibase.change.core.AddColumnChange;
import liquibase.change.core.AddForeignKeyConstraintChange;
import liquibase.change.core.CreateIndexChange;
import liquibase.change.core.CreateTableChange;
import liquibase.change.core.DropColumnChange;
import liquibase.change.core.DropForeignKeyConstraintChange;
import liquibase.change.core.DropIndexChange;
import liquibase.change.core.DropTableChange;
import liquibase.change.core.RenameColumnChange;
import liquibase.change.core.RenameTableChange;
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
import liquibase.precondition.core.ColumnExistsPrecondition;
import liquibase.precondition.core.DBMSPrecondition;
import liquibase.precondition.core.ForeignKeyExistsPrecondition;
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
 * <p>The changelog is replayed without touching a database: {@code createIndex}/{@code dropIndex},
 * {@code createTable}/{@code dropTable}/{@code renameTable} and {@code addColumn}/{@code dropColumn}/
 * {@code renameColumn} changes, and foreign key constraints, maintain a transient model, and change-set
 * preconditions are evaluated against that transient model (only {@code dbms}, {@code changeSetExecuted},
 * {@code indexExists}, {@code tableExists}, {@code columnExists}, {@code foreignKeyConstraintExists} and the
 * logical operators are evaluated). Any other precondition is assumed to be true,
 * which is only acceptable for change-sets that do not alter the modelled indexes/tables; for a change-set that
 * does, such a precondition fails the build.
 * The result is stored in the manifest so the runtime index checker does not need Liquibase.
 */
final class ExpectedIndexComputer {

    private record ChangesetInfo(String id, String author, String filePath) {
    }

    private final Database database;
    private final Map<String, ExpectedIndexDescriptor> expectedIndexes = new LinkedHashMap<>();
    private final Set<ChangesetInfo> runChangesets = new HashSet<>();
    private final Set<String> tables = new HashSet<>();
    /** Upper-cased table name to upper-cased column names. */
    private final Map<String, Set<String>> columns = new HashMap<>();
    /** Upper-cased foreign key constraint name to upper-cased base table name. */
    private final Map<String, String> foreignKeys = new HashMap<>();
    /** Set when a precondition that is not modelled here was assumed to be true while evaluating a change-set. */
    private boolean assumedUnmodelled;

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
            Set<String> tableColumns = columnsOf(ctc.getTableName());
            tableColumns.clear();
            addColumns(tableColumns, ctc.getColumns());
            if (ctc.getColumns() != null) {
                for (ColumnConfig c : ctc.getColumns()) {
                    if (c.getConstraints() != null && c.getConstraints().getForeignKeyName() != null) {
                        foreignKeys.put(key(c.getConstraints().getForeignKeyName()), key(ctc.getTableName()));
                    }
                }
            }
        } else if (change instanceof DropTableChange dtc && dtc.getTableName() != null) {
            String droppedTable = dtc.getTableName();
            expectedIndexes.values().removeIf(info -> info.getTableName().equalsIgnoreCase(droppedTable));
            tables.remove(droppedTable);
            columns.remove(key(droppedTable));
            foreignKeys.values().removeIf(base -> base.equals(key(droppedTable)));
        } else if (change instanceof RenameTableChange rtc) {
            tables.remove(rtc.getOldTableName());
            tables.add(rtc.getNewTableName());
            columns.put(key(rtc.getNewTableName()), columnsOf(rtc.getOldTableName()));
            columns.remove(key(rtc.getOldTableName()));
            foreignKeys.replaceAll((fk, base) -> base.equals(key(rtc.getOldTableName())) ? key(rtc.getNewTableName()) : base);
        } else if (change instanceof AddForeignKeyConstraintChange afk && afk.getConstraintName() != null) {
            foreignKeys.put(key(afk.getConstraintName()), key(afk.getBaseTableName()));
        } else if (change instanceof DropForeignKeyConstraintChange dfk && dfk.getConstraintName() != null) {
            foreignKeys.remove(key(dfk.getConstraintName()));
        } else if (change instanceof AddColumnChange acc) {
            addColumns(columnsOf(acc.getTableName()), acc.getColumns());
        } else if (change instanceof DropColumnChange dcc) {
            Set<String> tableColumns = columnsOf(dcc.getTableName());
            if (dcc.getColumnName() != null) {
                tableColumns.remove(key(dcc.getColumnName()));
            }
            if (dcc.getColumns() != null) {
                dcc.getColumns().forEach(c -> tableColumns.remove(key(c.getName())));
            }
        } else if (change instanceof RenameColumnChange rcc) {
            Set<String> tableColumns = columnsOf(rcc.getTableName());
            tableColumns.remove(key(rcc.getOldColumnName()));
            tableColumns.add(key(rcc.getNewColumnName()));
        }
    }

    private static String key(String name) {
        return name.toUpperCase(Locale.ROOT);
    }

    private Set<String> columnsOf(String tableName) {
        return columns.computeIfAbsent(key(tableName), k -> new HashSet<>());
    }

    private static void addColumns(Set<String> target, List<? extends ColumnConfig> configs) {
        if (configs != null) {
            configs.forEach(c -> target.add(key(c.getName())));
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
        assumedUnmodelled = false;
        boolean applies = true;
        for (var precondition : preconditions.getNestedPreconditions()) {
            try {
                evaluate(precondition);
            } catch (PreconditionFailedException e) {
                applies = false;
                break;
            } catch (PreconditionErrorException e) {
                throw new IllegalStateException("Failed to evaluate precondition of change-set " + changeSet.getId()
                        + " by " + changeSet.getAuthor() + " in " + changeSet.getFilePath(), e);
            }
        }
        if (assumedUnmodelled && alters(changeSet)) {
            throw new IllegalStateException("Change-set " + changeSet.getId() + " by " + changeSet.getAuthor()
                    + " in " + changeSet.getFilePath() + " has a precondition that cannot be evaluated when computing"
                    + " the expected indexes, but the change-set creates/drops indexes or tables. Add support for the"
                    + " precondition to " + ExpectedIndexComputer.class.getSimpleName() + ".");
        }
        return applies;
    }

    /** Whether the change-set contains a change that {@link #apply} uses to maintain the transient model. */
    private static boolean alters(ChangeSet changeSet) {
        return changeSet.getChanges().stream().anyMatch(c -> c instanceof CreateIndexChange
                || c instanceof DropIndexChange || c instanceof CreateTableChange || c instanceof DropTableChange);
    }

    /**
     * Evaluate the precondition against the transient state for indexes, tables and change-sets.
     * Conditions that are not implemented here are assumed to be "true" and recorded in {@link #assumedUnmodelled}.
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
        } else if (p instanceof ColumnExistsPrecondition cep && cep.getTableName() != null && cep.getColumnName() != null) {
            if (!columnsOf(cep.getTableName()).contains(key(cep.getColumnName()))) {
                throw new PreconditionFailedException("Precondition failed", null, cep);
            }
        } else if (p instanceof ForeignKeyExistsPrecondition fkp && fkp.getForeignKeyName() != null) {
            String base = foreignKeys.get(key(fkp.getForeignKeyName()));
            if (base == null || (fkp.getForeignKeyTableName() != null && !base.equals(key(fkp.getForeignKeyTableName())))) {
                throw new PreconditionFailedException("Precondition failed", null, fkp);
            }
        } else {
            assumedUnmodelled = true;
        }
    }
}
