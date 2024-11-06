package org.keycloak.test.framework.server;

import io.quarkus.maven.dependency.Dependency;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Set;

public class RemoteKeycloakTestServer implements KeycloakTestServer {

    private static final Logger LOGGER = Logger.getLogger(RemoteKeycloakTestServer.class);

    @Override
    public void start(List<String> rawOptions, Set<Dependency> dependencies) {
        LOGGER.infov("Requested server config: {0}", String.join(" ", rawOptions));
        LOGGER.infov("Requested dependencies: {0}", String.join(" ", dependencies.toString()));
    }

    @Override
    public void stop() {
    }

    @Override
    public String getBaseUrl() {
        return "http://localhost:8080";
    }

}
