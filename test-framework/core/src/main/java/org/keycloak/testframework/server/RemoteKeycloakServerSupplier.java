package org.keycloak.testframework.server;

import org.jboss.logging.Logger;

import java.nio.file.Path;

public class RemoteKeycloakServerSupplier extends AbstractKeycloakServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(RemoteKeycloakServerSupplier.class);

    @Override
    public KeycloakServer getServer(Path serverKeyStorePath) {
        return new RemoteKeycloakServer(serverKeyStorePath);
    }

    @Override
    public boolean requiresDatabase() {
        return false;
    }

    @Override
    public String getAlias() {
        return "remote";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
