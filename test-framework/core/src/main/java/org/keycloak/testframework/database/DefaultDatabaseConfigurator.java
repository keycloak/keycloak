package org.keycloak.testframework.database;

public class DefaultDatabaseConfigurator implements DatabaseConfigurator {
    @Override
    public DatabaseConfigBuilder configure(DatabaseConfigBuilder builder) {
        return builder;
    }
}
