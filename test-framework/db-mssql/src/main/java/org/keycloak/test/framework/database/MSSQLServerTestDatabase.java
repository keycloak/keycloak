package org.keycloak.test.framework.database;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;

class MSSQLServerTestDatabase extends AbstractContainerTestDatabase {

    private static final String IMAGE_NAME = "mcr.microsoft.com/mssql/server:latest";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new MSSQLServerContainer<>(IMAGE_NAME);
    }

    @Override
    public String getKeycloakDatabaseName() {
        return "mssql";
    }
}
