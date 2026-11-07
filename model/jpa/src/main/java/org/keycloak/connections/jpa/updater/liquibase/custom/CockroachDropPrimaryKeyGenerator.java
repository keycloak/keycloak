package org.keycloak.connections.jpa.updater.liquibase.custom;

import java.util.Optional;

import liquibase.Scope;
import liquibase.change.core.AddPrimaryKeyChange;
import liquibase.changelog.ChangeSet;
import liquibase.database.Database;
import liquibase.database.core.CockroachDatabase;
import liquibase.sql.Sql;
import liquibase.sql.UnparsedSql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.DropPrimaryKeyGenerator;
import liquibase.statement.core.DropPrimaryKeyStatement;

/**
 * CockroachDB rejects dropping a primary key without adding a new one in the same statement, and doesn't allow dropping a column that is part of the primary key.
 * The primary key is therefore replaced with {@code ALTER PRIMARY KEY} at the place of the drop, using the columns of the next {@code addPrimaryKey} for the same table in the changeset.
 * The {@code addPrimaryKey} is then skipped by {@link CockroachAddPrimaryKeyGenerator}.
 */
public class CockroachDropPrimaryKeyGenerator extends DropPrimaryKeyGenerator {

    @Override
    public int getPriority() {
        return PRIORITY_DATABASE + 100;
    }

    @Override
    public boolean supports(DropPrimaryKeyStatement statement, Database database) {
        return database instanceof CockroachDatabase;
    }

    @Override
    public Sql[] generateSql(DropPrimaryKeyStatement statement, Database database, SqlGeneratorChain sqlGeneratorChain) {
        Optional<AddPrimaryKeyChange> add = findAdd(statement.getTableName());
        if (add.isEmpty()) {
            return new Sql[0];
        }
        String table = database.escapeTableName(statement.getCatalogName(), statement.getSchemaName(), statement.getTableName());
        String columns = database.escapeColumnNameList(add.get().getColumnNames());
        return new Sql[] {
                new UnparsedSql("ALTER TABLE " + table + " ALTER PRIMARY KEY USING COLUMNS (" + columns + ")")
        };
    }

    static Optional<AddPrimaryKeyChange> findAdd(String tableName) {
        ChangeSet changeSet = Scope.getCurrentScope().get(Scope.Attr.changeSet, ChangeSet.class);
        if (changeSet == null) {
            return Optional.empty();
        }
        return changeSet.getChanges().stream()
                .filter(c -> c instanceof AddPrimaryKeyChange add && tableName.equalsIgnoreCase(add.getTableName()))
                .map(c -> (AddPrimaryKeyChange) c)
                .findFirst();
    }

}
