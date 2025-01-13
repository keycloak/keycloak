package org.keycloak.testframework.server;

import org.jboss.logging.Logger;

public class DistributionKeycloakServerSupplier extends AbstractKeycloakServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(DistributionKeycloakServerSupplier.class);

    @Override
    public KeycloakServer getServer() {
        return new DistributionKeycloakServer();
    }

    @Override
    public boolean requiresDatabase() {
        return true;
    }

    @Override
    public String getAlias() {
        return "distribution";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
