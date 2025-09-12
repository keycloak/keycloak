package org.keycloak.testframework.database;

import org.jboss.logging.Logger;
import org.keycloak.testframework.util.JavaPropertiesUtil;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MSSQLServerContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;

class MSSQLServerTestDatabase extends AbstractContainerTestDatabase {

    private static final Logger LOGGER = Logger.getLogger(MSSQLServerTestDatabase.class);

    public static final String NAME = "mssql";

    @SuppressWarnings("resource")
    @Override
    public JdbcDatabaseContainer<?> createContainer() {
        String containerName = JavaPropertiesUtil.getContainerImageName("database.properties", NAME);;

        return new MSSQLServerContainer<>(DockerImageName.parse(containerName)).withPassword(getPassword()).withEnv("MSSQL_PID", "Express").acceptLicense();
    }

    @Override
    public void withDatabaseAndUser(String database, String username, String password) {
        // MSSQLServerContainer does not support withUsername and withDatabase
    }

    @Override
    public String getDatabaseVendor() {
        return NAME;
    }

    @Override
    public String getUsername() {
        return "sa";
    }

    @Override
    public String getPassword() {
        return "vEry$tron9Pwd";
    }

    @Override
    public String getJdbcUrl(boolean internal) {
        return super.getJdbcUrl(internal) + ";integratedSecurity=false;encrypt=false;trustServerCertificate=true;sendStringParametersAsUnicode=false;";
    }

    @Override
    public List<String> getPostStartCommand() {
        return List.of("/opt/mssql-tools18/bin/sqlcmd", "-U", "sa", "-P", getPassword(), "-No", "-Q", "CREATE DATABASE " + getDatabase());
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
