package org.keycloak.test.framework.database;

public class MSSQLServerDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return MSSQLServerTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new MSSQLServerTestDatabase();
    }

}
