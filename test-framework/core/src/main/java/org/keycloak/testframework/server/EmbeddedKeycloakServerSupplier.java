package org.keycloak.testframework.server;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

public class EmbeddedKeycloakServerSupplier extends AbstractKeycloakServerSupplier {

    @ConfigProperty(name = "start.timeout", defaultValue = "120")
    long startTimeout;

    private static final Logger LOGGER = Logger.getLogger(EmbeddedKeycloakServerSupplier.class);

    @Override
    public KeycloakServer getServer() {
        return new EmbeddedKeycloakServer(startTimeout);
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
