package org.keycloak.test.framework.database;

import io.quarkus.maven.dependency.Dependency;
import org.jboss.logging.Logger;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.oracle.OracleContainer;

class OracleTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(OracleTestDatabase.class);

    public static final String NAME = "oracle";

    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        return new OracleContainer(DatabaseProperties.getContainerImageName(NAME));
    }

    @Override
    public String getDatabaseVendor() {
        return NAME;
    }

    @Override
    public Dependency jdbcDriver() {
        return Dependency.of("com.oracle.database.jdbc", "ojdbc11");
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
