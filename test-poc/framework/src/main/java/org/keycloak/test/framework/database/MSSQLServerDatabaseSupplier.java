package org.keycloak.test.framework.database;

public class MSSQLServerDatabaseSupplier extends AbstractDatabaseSupplier {
    public static final String VENDOR = "mssql";

    @Override
    TestDatabase getTestDatabase() {
        DatabaseConfig databaseConfig = new DatabaseConfig()
                .vendor(VENDOR)
                .containerImage("mcr.microsoft.com/mssql/server:latest");
        return new TestDatabase(databaseConfig);
    }

    @Override
    public String getAlias() {
        return VENDOR;
    }
}
