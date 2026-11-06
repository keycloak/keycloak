package org.keycloak.testframework.database;

public class TiDBDatabaseSupplier extends AbstractContainerDatabaseSupplier {

    @Override
    public String getAlias() {
        return "tidb";
    }

    @Override
    TestDatabase getTestDatabase() {
        return new TiDBTestDatabase();
    }

}
