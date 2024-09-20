package org.keycloak.test.framework.database;

public class MySQLDatabaseSupplier extends AbstractDatabaseSupplier {

    public static final String VENDOR = "mysql";

    @Override
    TestDatabase getTestDatabase() {
        DatabaseConfig databaseConfig = new DatabaseConfig()
                .vendor(VENDOR)
                .username(DEFAULT_DB_USERNAME)
                .password(DEFAULT_DB_PASSWORD)
                .containerImage("mysql:latest");
        return new TestDatabase(databaseConfig);
    }

    @Override
    public String getAlias() {
        return VENDOR;
    }
}
