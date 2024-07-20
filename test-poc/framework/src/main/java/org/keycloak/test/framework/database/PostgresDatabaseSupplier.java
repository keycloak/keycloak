package org.keycloak.test.framework.database;

public class PostgresDatabaseSupplier extends AbstractDatabaseSupplier {

    public static final String VENDOR = "postgres";

    @Override
    TestDatabase getTestDatabase() {
        DatabaseConfig databaseConfig = new DatabaseConfig()
                .vendor(VENDOR)
                .username(DEFAULT_DB_USERNAME)
                .password(DEFAULT_DB_PASSWORD)
                .containerImage("postgres:latest");
        return new TestDatabase(databaseConfig);
    }

    @Override
    public String getAlias() {
        return VENDOR;
    }
}
