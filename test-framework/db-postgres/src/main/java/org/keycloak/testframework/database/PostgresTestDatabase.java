package org.keycloak.testframework.database;

import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

class PostgresTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(PostgresTestDatabase.class);

    public static final String NAME = "postgres";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new PostgreSQLContainer<>(DockerImageName.parse(DatabaseProperties.getContainerImageName(NAME)).asCompatibleSubstituteFor(NAME));
    }

    @Override
    public String getDatabaseVendor() {
        return NAME;
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
