package org.keycloak.testframework.database;

public class DefaultDatabaseConfig implements DatabaseConfig {
    @Override
    public DatabaseConfigBuilder configure(DatabaseConfigBuilder database) {
        return database;
    }
}
