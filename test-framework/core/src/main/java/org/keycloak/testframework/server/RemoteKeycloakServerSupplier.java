package org.keycloak.testframework.server;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

public class RemoteKeycloakServerSupplier extends AbstractKeycloakServerSupplier {

    @ConfigProperty(name = "start.timeout", defaultValue = "120")
    long startTimeout;

    private static final Logger LOGGER = Logger.getLogger(RemoteKeycloakServerSupplier.class);

    @Override
    public KeycloakServer getServer() {
        return new RemoteKeycloakServer(startTimeout);
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
