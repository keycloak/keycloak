package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.Scope;
import liquibase.change.core.DropPrimaryKeyChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.core.CockroachDatabase;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AddPrimaryKeyGenerator;
import liquibase.statement.core.AddPrimaryKeyStatement;

/**
 * Replaces the primary key of a table whose primary key drop was skipped by {@link CockroachDropPrimaryKeyGenerator}.
 */
public class CockroachAddPrimaryKeyGenerator extends AddPrimaryKeyGenerator {

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE + 100;
    }

    @Override
    public boolean supports(AddPrimaryKeyStatement statement, Database database) {
        return database instanceof CockroachDatabase;
    }

    @Override
    public Sql[] generateSql(AddPrimaryKeyStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        if (!hasSkippedDrop(statement.getTableName())) {
            return super.generateSql(statement, database, sqlGeneratorChain);
        }
        String table = database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName());
        String columns = database.escapeColumnNameList(statement.getColumnNames());
        return new Sql[] {
                new UnparsedSql("ALTER TABLE " + table + " ALTER PRIMARY KEY USING COLUMNS (" + columns + ")", getAffectedPrimaryKey(statement))
        };
    }

    private boolean hasSkippedDrop(String tableName) {
        ChangeSet changeSet = Scope.getCurrentScope().get(Scope.Attr.changeSet, ChangeSet.class);
        return changeSet != null && changeSet.getChanges().stream()
                .anyMatch(c -> c instanceof DropPrimaryKeyChange drop && tableName.equalsIgnoreCase(drop.getTableName()));
    }

}
