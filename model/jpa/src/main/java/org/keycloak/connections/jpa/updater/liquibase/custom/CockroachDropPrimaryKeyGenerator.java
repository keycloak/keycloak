package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.database.Database;
import liquibase.database.core.CockroachDatabase;
import liquibase.sql.Sql;
import liquibase.sqlgenerator.SqlGeneratorChain;
import liquibase.sqlgenerator.core.DropPrimaryKeyGenerator;
import liquibase.statement.core.DropPrimaryKeyStatement;

/**
 * CockroachDB rejects dropping a primary key without adding a new one in the same statement.
 * The drop is skipped, and the following {@code addPrimaryKey} on the same table is turned into {@code ALTER PRIMARY KEY} by {@link CockroachAddPrimaryKeyGenerator}.
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
        return new Sql[0];
    }

}
