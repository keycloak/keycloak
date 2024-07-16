package org.keycloak.test.framework.database;

import java.util.Map;

public class TestDatabase {

    private DatabaseConfig databaseConfig;

    public TestDatabase(DatabaseConfig databaseConfig) {
        this.databaseConfig = databaseConfig;
    }

    public void start() {
        if (databaseConfig.getContainerImage() != null) {
            // TODO Start container
        }
    }

    public void stop() {
        if (databaseConfig.getContainerImage() != null) {
            // TODO Stop container
        } else if (databaseConfig.getVendor().equals("dev-mem")) {
            // TODO Stop in-mem H2 database
        }
    }

    public Map<String, String> getServerConfig() {
        return databaseConfig.toConfig();
    }

}
