package org.keycloak.testframework.database;

import org.jboss.logging.Logger;
import org.keycloak.testframework.util.JavaPropertiesUtil;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

class MariaDBTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(MariaDBTestDatabase.class);

    public static final String NAME = "mariadb";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        String containerName = JavaPropertiesUtil.getContainerImageName("database.properties", NAME);;

        return new MariaDBContainer<>(DockerImageName.parse(containerName).asCompatibleSubstituteFor(NAME)).withCommand("--character-set-server=utf8 --collation-server=utf8_unicode_ci");
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
