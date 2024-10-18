package org.keycloak.test.framework.server;

import org.jboss.logging.Logger;
import org.keycloak.it.TestProvider;

import java.util.List;

public class RemoteKeycloakTestServer implements KeycloakTestServer {

    private static final Logger LOGGER = Logger.getLogger(RemoteKeycloakTestServer.class);

    @Override
    public void start(List<String> rawOptions, List<? extends TestProvider> customProviders) {
        LOGGER.infov("Requested server config: {0}", String.join(" ", rawOptions));
        LOGGER.infov("Providers requested for deployment: {0}",
                String.join(" ", customProviders.stream()
                        .map(TestProvider::getName)
                        .toList())
        );
    }

    @Override
    public void stop() {
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
