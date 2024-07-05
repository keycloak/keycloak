package org.keycloak.test.framework.database;

public class TestDatabase {

    private DatabaseConfig databaseConfig;

    public TestDatabase(DatabaseConfig config) {
        databaseConfig = config;
    }

    public void start() {

    }

    public void stop() {

    }

    public DatabaseConfig getDatabaseConfig() {
        return databaseConfig;
    }
}
