package org.keycloak.test.framework.server;

import org.keycloak.test.framework.database.DatabaseConfig;
import org.keycloak.test.framework.database.TestDatabase;

public class RemoteKeycloakTestServer implements KeycloakTestServer {

    @Override
    public void start(KeycloakTestServerConfig serverConfig, DatabaseConfig databaseConfig) {

    }

    @Override
    public void stop() {

    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
