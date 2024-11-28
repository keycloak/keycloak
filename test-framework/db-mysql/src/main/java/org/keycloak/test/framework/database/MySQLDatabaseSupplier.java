package org.keycloak.test.framework.database;

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
