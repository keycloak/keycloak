package org.keycloak.testframework.database;

public class EnterpriseDbDatabaseSupplier extends AbstractContainerDatabaseSupplier {

    @Override
    public String getAlias() {
        return EnterpriseDbTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new EnterpriseDbTestDatabase();
    }

}
