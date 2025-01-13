package org.keycloak.testframework.database;

public class MySQLDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return "mysql";
    }

    @Override
    TestDatabase getTestDatabase() {
        return new MySQLTestDatabase();
    }

}
