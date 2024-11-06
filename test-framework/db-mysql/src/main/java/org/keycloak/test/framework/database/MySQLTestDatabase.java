package org.keycloak.test.framework.database;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;

class MySQLTestDatabase extends AbstractContainerTestDatabase {

    private static final String IMAGE_NAME = "mysql:latest";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new MySQLContainer<>(IMAGE_NAME);
    }

    @Override
    public String getKeycloakDatabaseName() {
        return "mysql";
    }
}
