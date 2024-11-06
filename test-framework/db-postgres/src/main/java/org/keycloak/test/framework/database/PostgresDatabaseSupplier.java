package org.keycloak.test.framework.database;

public class PostgresDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return "postgres";
    }

    @Override
    TestDatabase getTestDatabase() {
        return new PostgresTestDatabase();
    }

}
