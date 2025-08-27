package org.keycloak.testframework.database;

import org.jboss.logging.Logger;
import org.keycloak.testframework.util.JavaPropertiesUtil;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

class MySQLTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(MySQLTestDatabase.class);

    public static final String NAME = "mysql";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        String containerName = JavaPropertiesUtil.getContainerImageName("database.properties", NAME);;

        return new MySQLContainer<>(DockerImageName.parse(containerName).asCompatibleSubstituteFor(NAME));
    }

    @Override
    public String getDatabaseVendor() {
        return NAME;
    }

    @Override
    public String getJdbcUrl(boolean internal) {
        return super.getJdbcUrl(internal) + "?allowPublicKeyRetrieval=true";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
