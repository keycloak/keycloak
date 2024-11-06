package org.keycloak.test.framework.database;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;

class MariaDBTestDatabase extends AbstractContainerTestDatabase {

    private static final String IMAGE_NAME = "mariadb:latest";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new MariaDBContainer<>(IMAGE_NAME);
    }

    @Override
    public String getKeycloakDatabaseName() {
        return "mariadb";
    }
}
