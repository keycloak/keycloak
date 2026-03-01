package org.keycloak.testframework.database;

public class PostgresDatabaseSupplier extends AbstractContainerDatabaseSupplier {

    @Override
    public String getAlias() {
        return PostgresTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new PostgresTestDatabase();
    }

}
