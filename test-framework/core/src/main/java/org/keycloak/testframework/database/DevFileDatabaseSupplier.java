package org.keycloak.testframework.database;

import java.util.Map;

public class DevFileDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return "dev-file";
    }

    @Override
    TestDatabase getTestDatabase() {
        return new DevFileTestDatabase();
    }

    private static class DevFileTestDatabase implements TestDatabase {

        @Override
        public void start(DatabaseConfiguration config) {
            if (config.getInitScript() != null)
                throw new IllegalArgumentException("init script not supported, configure h2 properties via --db-url-properties");
        }

        @Override
        public void stop() {
            // TODO Should we clean-up H2 database here?
        }

        @Override
        public Map<String, String> serverConfig() {
            return Map.of("db", "dev-file");
        }
    }

}
