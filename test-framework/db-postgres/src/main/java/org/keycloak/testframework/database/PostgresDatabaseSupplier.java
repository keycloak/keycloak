package org.keycloak.testframework.database;

public class PostgresDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return PostgresTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new PostgresTestDatabase();
    }

}
