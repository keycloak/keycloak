package org.keycloak.test.framework.server;

public class RemoteKeycloakTestServer implements KeycloakTestServer {

    @Override
    public void start(KeycloakTestServerConfig serverConfig) {

    }

    @Override
    public void stop() {

    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
