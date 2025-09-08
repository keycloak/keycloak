package org.keycloak.testframework.server;

import org.jboss.logging.Logger;

import java.nio.file.Path;

public class EmbeddedKeycloakServerSupplier extends AbstractKeycloakServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(EmbeddedKeycloakServerSupplier.class);

    @Override
    public KeycloakServer getServer(Path serverKeyStorePath) {
        return new EmbeddedKeycloakServer(serverKeyStorePath);
    }

    @Override
    public boolean requiresDatabase() {
        return true;
    }

    @Override
    public String getAlias() {
        return "embedded";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
