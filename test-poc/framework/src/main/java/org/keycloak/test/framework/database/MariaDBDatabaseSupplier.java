package org.keycloak.test.framework.database;

public class MariaDBDatabaseSupplier extends AbstractDatabaseSupplier {
    public static final String VENDOR = "mariadb";

    @Override
    TestDatabase getTestDatabase() {
        DatabaseConfig databaseConfig = new DatabaseConfig()
                .vendor(VENDOR)
                .username(DEFAULT_DB_USERNAME)
                .password(DEFAULT_DB_PASSWORD)
                .containerImage("mariadb:latest");
        return new TestDatabase(databaseConfig);
    }

    @Override
    public String getAlias() {
        return VENDOR;
    }
}
