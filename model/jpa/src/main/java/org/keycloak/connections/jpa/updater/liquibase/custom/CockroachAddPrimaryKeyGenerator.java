package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.Scope;
import liquibase.change.core.DropPrimaryKeyChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.core.CockroachDatabase;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.AddPrimaryKeyGenerator;
import liquibase.statement.core.AddPrimaryKeyStatement;

/**
 * Skips the primary key that was already created by {@link CockroachDropPrimaryKeyGenerator} when it processed the preceding {@code dropPrimaryKey} for the same table.
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
        if (hasDropInChangeSet(statement.getTableName())) {
            return new Sql[0];
        }
        return super.generateSql(statement, database, sqlGeneratorChain);
    }

    private boolean hasDropInChangeSet(String tableName) {
        ChangeSet changeSet = Scope.getCurrentScope().get(Scope.Attr.changeSet, ChangeSet.class);
        return changeSet != null && changeSet.getChanges().stream()
                .anyMatch(c -> c instanceof DropPrimaryKeyChange drop && tableName.equalsIgnoreCase(drop.getTableName()));
    }

}
