package org.keycloak.test.framework.database;

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
