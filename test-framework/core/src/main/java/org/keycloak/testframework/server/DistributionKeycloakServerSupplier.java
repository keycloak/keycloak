package org.keycloak.testframework.server;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

public class DistributionKeycloakServerSupplier extends AbstractKeycloakServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(DistributionKeycloakServerSupplier.class);

    @ConfigProperty(name = "start.timeout", defaultValue = "120")
    long startTimeout;

    @ConfigProperty(name = "debug", defaultValue = "false")
    boolean debug = false;

    @ConfigProperty(name = "reuse", defaultValue = "false")
    boolean reuse;

    @Override
    public KeycloakServer getServer() {
        return new DistributionKeycloakServer(debug, reuse, startTimeout);
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
