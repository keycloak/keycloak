package org.keycloak.test.framework.database;

public class DevFileDatabaseSupplier extends AbstractDatabaseSupplier {

    public static final String VENDOR = "dev-file";

    @Override
    TestDatabase getTestDatabase() {
        DatabaseConfig databaseConfig = new DatabaseConfig().vendor(VENDOR);
        return new TestDatabase(databaseConfig);
    }

    @Override
    public String getAlias() {
        return VENDOR;
    }
}
