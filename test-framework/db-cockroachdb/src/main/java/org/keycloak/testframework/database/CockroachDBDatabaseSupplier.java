package org.keycloak.testframework.database;

public class CockroachDBDatabaseSupplier extends AbstractContainerDatabaseSupplier {

    @Override
    public String getAlias() {
        return CockroachDBTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new CockroachDBTestDatabase();
    }

}
