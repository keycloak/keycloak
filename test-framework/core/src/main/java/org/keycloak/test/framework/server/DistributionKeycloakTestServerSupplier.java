package org.keycloak.test.framework.server;

import org.jboss.logging.Logger;

public class DistributionKeycloakTestServerSupplier extends AbstractKeycloakTestServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(DistributionKeycloakTestServerSupplier.class);

    @Override
    public KeycloakTestServer getServer() {
        return new DistributionKeycloakTestServer();
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
