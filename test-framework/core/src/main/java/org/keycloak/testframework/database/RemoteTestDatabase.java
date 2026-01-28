package org.keycloak.testframework.database;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.keycloak.testframework.config.Config;

public class RemoteTestDatabase implements TestDatabase {

    @Override
    public void start(DatabaseConfiguration databaseConfiguration) {
        // noop
    }

    @Override
    public void stop() {
        // noop
    }

    private String getDatabaseVendor() {
        return getRequiredDbConfigOption("vendor");
    }

    private String getJdbcUrl() {
        return getRequiredDbConfigOption("url");
    }

    private String getUsername() {
        return getRequiredDbConfigOption("user");
    }

    private String getPassword() {
        return getRequiredDbConfigOption("password");
    }

    private String getDatabaseDriver() {
        return Config.getValueTypeConfig(TestDatabase.class, "driver", null, String.class);
    }

    private static String getRequiredDbConfigOption(String option) {
        String value = Config.getValueTypeConfig(TestDatabase.class, option, null, String.class);
        if (value == null) {
            throw new NoSuchElementException("Missing required config for a remote DB: " + Config.getValueTypeFQN(TestDatabase.class, option));
        }
        return value;
    }

    @Override
    public Map<String, String> serverConfig() {
        Map<String, String> serverConfig = new HashMap<>(Map.of(
                "db", getDatabaseVendor(),
                "db-url", getJdbcUrl(),
                "db-username", getUsername(),
                "db-password", getPassword()
        ));
        if (getDatabaseDriver() != null) {
            serverConfig.put("db-driver", getDatabaseDriver());
        }

        return serverConfig;
    }
}
