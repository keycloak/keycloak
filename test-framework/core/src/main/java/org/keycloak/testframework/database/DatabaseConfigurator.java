package org.keycloak.testframework.database;

public interface DatabaseConfigurator {
    DatabaseConfigBuilder configure(DatabaseConfigBuilder builder);
}
