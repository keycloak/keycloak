package org.keycloak.testframework.database;

import org.jboss.logging.Logger;
import org.keycloak.testframework.util.JavaPropertiesUtil;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

public class PostgresTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(PostgresTestDatabase.class);

    public static final String NAME = "postgres";

    PostgresTestDatabase() {}

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        String containerName = JavaPropertiesUtil.getContainerImageName("database.properties", NAME);;

        return new PostgreSQLContainer<>(DockerImageName.parse(containerName).asCompatibleSubstituteFor(NAME));
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
