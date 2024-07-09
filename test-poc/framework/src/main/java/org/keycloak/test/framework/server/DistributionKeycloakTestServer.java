package org.keycloak.test.framework.server;

public class DistributionKeycloakTestServer implements KeycloakTestServer {

    @Override
    public void start(KeycloakTestServerConfig serverConfig) {
        throw new RuntimeException("Method not implemented!");
    }

    @Override
    public void stop() {
        throw new RuntimeException("Method not implemented!");
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
