package org.keycloak.test.framework.database;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;

class PostgresTestDatabase extends AbstractContainerTestDatabase {

    private static final String IMAGE_NAME = "postgres:latest";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new PostgreSQLContainer<>(IMAGE_NAME);
    }

    @Override
    public String getKeycloakDatabaseName() {
        return "postgres";
    }
}
