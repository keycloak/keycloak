package org.keycloak.test.framework.database;

public class OracleDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return "oracle";
    }

    @Override
    TestDatabase getTestDatabase() {
        return new OracleTestDatabase();
    }

}
