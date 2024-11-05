package org.keycloak.test.framework.database;

import io.quarkus.maven.dependency.Dependency;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.oracle.OracleContainer;

class OracleTestDatabase extends AbstractContainerTestDatabase {

    private static final String IMAGE_NAME = "gvenzl/oracle-free:slim-faststart";

    @SuppressWarnings("resource")
    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new OracleContainer(IMAGE_NAME);
    }

    @Override
    public String getKeycloakDatabaseName() {
        return "oracle";
    }

    @Override
    public Dependency jdbcDriver() {
        return Dependency.of("com.oracle.database.jdbc", "ojdbc11");
    }
}
