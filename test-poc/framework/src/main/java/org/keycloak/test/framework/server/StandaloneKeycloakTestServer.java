package org.keycloak.test.framework.server;

import org.keycloak.Keycloak;

public class StandaloneKeycloakTestServer implements KeycloakTestServer {

    private Keycloak keycloak;

    @Override
    public void start(KeycloakTestServerConfig keycloakTestServerConfig) {

    }

    @Override
    public void stop() {

    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }
}
