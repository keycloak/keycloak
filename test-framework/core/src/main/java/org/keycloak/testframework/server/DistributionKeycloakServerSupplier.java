package org.keycloak.testframework.server;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.file.Path;

public class DistributionKeycloakServerSupplier extends AbstractKeycloakServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(DistributionKeycloakServerSupplier.class);

    @ConfigProperty(name = "debug", defaultValue = "false")
    boolean debug = false;

    @Override
    public KeycloakServer getServer(Path serverKeyStorePath) {
        return new DistributionKeycloakServer(debug, serverKeyStorePath);
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
