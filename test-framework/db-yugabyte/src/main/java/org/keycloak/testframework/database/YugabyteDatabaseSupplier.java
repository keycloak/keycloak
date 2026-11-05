package org.keycloak.testframework.database;

public class YugabyteDatabaseSupplier extends AbstractContainerDatabaseSupplier {

    @Override
    public String getAlias() {
        return YugabyteTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new YugabyteTestDatabase();
    }

}
