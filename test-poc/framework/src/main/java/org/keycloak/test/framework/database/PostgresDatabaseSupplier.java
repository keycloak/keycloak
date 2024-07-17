package org.keycloak.test.framework.database;

public class PostgresDatabaseSupplier extends AbstractDatabaseSupplier {

    public static final String VENDOR = "postgres";

    @Override
    TestDatabase getTestDatabase() {
        DatabaseConfig databaseConfig = new DatabaseConfig()
                .vendor(VENDOR)
                .username("keycloak")
                .password("keycloak")
                .containerImage("the-postgres-container:the-version");
        return new TestDatabase(databaseConfig);
    }

    @Override
    public String getAlias() {
        return VENDOR;
    }
}
