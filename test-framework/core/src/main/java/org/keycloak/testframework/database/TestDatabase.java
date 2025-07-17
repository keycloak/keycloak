package org.keycloak.testframework.database;

import java.util.Map;

public interface TestDatabase {

    void start(DatabaseConfig databaseConfig);

    void stop();

    Map<String, String> serverConfig();
}
