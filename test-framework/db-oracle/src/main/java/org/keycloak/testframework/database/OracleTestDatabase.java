package org.keycloak.testframework.database;

import org.keycloak.testframework.util.ContainerImages;

import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

class OracleTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(OracleTestDatabase.class);

    public static final String NAME = "oracle";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new OracleContainer(DockerImageName.parse(ContainerImages.getContainerImageName(NAME)).asCompatibleSubstituteFor("gvenzl/oracle-free"));
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
