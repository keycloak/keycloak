package org.keycloak.test.framework.server;

import java.util.Map;

public class RemoteKeycloakTestServer implements KeycloakTestServer {

    @Override
    public void start(KeycloakTestServerConfig serverConfig, Map<String, String> databaseConfig) {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
