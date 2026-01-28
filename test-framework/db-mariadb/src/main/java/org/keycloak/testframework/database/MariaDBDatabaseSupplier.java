package org.keycloak.testframework.database;

public class MariaDBDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return MariaDBTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new MariaDBTestDatabase();
    }

}
