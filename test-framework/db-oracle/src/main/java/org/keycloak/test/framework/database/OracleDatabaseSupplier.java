package org.keycloak.test.framework.database;

public class OracleDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return OracleTestDatabase.NAME;
    }

    @Override
    TestDatabase getTestDatabase() {
        return new OracleTestDatabase();
    }

}
