package org.keycloak.testframework.server;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

public class ClusteredKeycloakServerSupplier extends AbstractKeycloakServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(ClusteredKeycloakServerSupplier.class);

    @ConfigProperty(name = "numContainer", defaultValue = "2")
    int numContainers = 2;

    @ConfigProperty(name = "images", defaultValue = ClusteredKeycloakServer.SNAPSHOT_IMAGE)
    String images = ClusteredKeycloakServer.SNAPSHOT_IMAGE;

    @Override
    public KeycloakServer getServer() {
        return new ClusteredKeycloakServer(numContainers, images);
    }

    @Override
    public boolean requiresDatabase() {
        return true;
    }

    @Override
    public String getAlias() {
        return "cluster";
    }

    @Override
    public Logger getLogger() {
        return LOGGER;
    }
}
