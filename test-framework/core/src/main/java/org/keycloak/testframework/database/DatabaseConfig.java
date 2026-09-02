package org.keycloak.testframework.database;

/**
 * Declarative configuration for the managed database
 */
public interface DatabaseConfig {

    DatabaseConfigBuilder configure(DatabaseConfigBuilder database);

}
