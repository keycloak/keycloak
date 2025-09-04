package org.keycloak.testframework.database;

public class EnterpriseDbDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return EnterpriseDbTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new EnterpriseDbTestDatabase();
    }

}
