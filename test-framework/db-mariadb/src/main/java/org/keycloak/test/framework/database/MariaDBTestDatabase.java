package org.keycloak.test.framework.database;

import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;

class MariaDBTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(MariaDBTestDatabase.class);

    public static final String NAME = "mariadb";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new MariaDBContainer<>(DatabaseProperties.getContainerImageName(NAME));
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
