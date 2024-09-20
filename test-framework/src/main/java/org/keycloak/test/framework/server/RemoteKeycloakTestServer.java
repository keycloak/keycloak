package org.keycloak.test.framework.server;

import org.jboss.logging.Logger;

import java.util.List;

public class RemoteKeycloakTestServer implements KeycloakTestServer {

    private static final Logger LOGGER = Logger.getLogger(RemoteKeycloakTestServer.class);

    @Override
    public void start(List<String> rawOptions) {
        LOGGER.infov("Requested server config: {0}", String.join(" ", rawOptions));
    }

    @Override
    public void stop() {
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
