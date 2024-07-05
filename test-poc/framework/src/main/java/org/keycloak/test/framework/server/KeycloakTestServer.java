package org.keycloak.test.framework.server;

import org.keycloak.test.framework.database.DatabaseConfig;
import org.keycloak.test.framework.database.TestDatabase;

public interface KeycloakTestServer {

    void start(KeycloakTestServerConfig serverConfig, DatabaseConfig databaseConfig);

    void stop();

    String getBaseUrl();

}
