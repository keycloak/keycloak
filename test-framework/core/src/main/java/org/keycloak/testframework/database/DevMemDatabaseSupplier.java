package org.keycloak.testframework.database;

import java.util.Map;

public class DevMemDatabaseSupplier extends AbstractDatabaseSupplier {

    @Override
    public String getAlias() {
        return "dev-mem";
    }

    @Override
    TestDatabase getTestDatabase() {
        return new DevMemTestDatabase();
    }

    private static class DevMemTestDatabase implements TestDatabase {

        @Override
        public void start() {
        }

        @Override
        public void stop() {
        }

        @Override
        public Map<String, String> serverConfig() {
            return Map.of("db", "dev-mem");
        }
    }

}
