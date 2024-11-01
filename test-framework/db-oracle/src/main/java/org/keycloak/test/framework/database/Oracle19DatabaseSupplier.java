package org.keycloak.test.framework.database;

public class Oracle19DatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return "oracle19";
    }

    @Override
    TestDatabase getTestDatabase() {
        return new Oracle19TestDatabase();
    }


}
