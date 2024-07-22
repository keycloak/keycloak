package org.keycloak.test.framework.server;

import java.util.List;

public class RemoteKeycloakTestServer implements KeycloakTestServer {

    @Override
    public void start(List<String> rawOptions) {
    }

    @Override
    public void stop() {
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
