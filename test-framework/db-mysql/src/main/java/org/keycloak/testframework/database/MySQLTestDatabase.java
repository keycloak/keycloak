package org.keycloak.testframework.database;

import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

class MySQLTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(MySQLTestDatabase.class);

    public static final String NAME = "mysql";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new MySQLContainer<>(DockerImageName.parse(DatabaseProperties.getContainerImageName(NAME)).asCompatibleSubstituteFor(NAME));
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
