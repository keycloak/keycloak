package org.keycloak.testframework.server;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

public class ContainerKeycloakClusterSupplier extends AbstractKeycloakServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(ContainerKeycloakClusterSupplier.class);

    @ConfigProperty(name = "debug", defaultValue = "false")
    boolean debug = false;

    @ConfigProperty(name = "numContainer", defaultValue = "2")
    int numContainers = 2;

    @ConfigProperty(name = "images", defaultValue = ContainerKeycloakCluster.SNAPSHOT_IMAGE)
    String images = ContainerKeycloakCluster.SNAPSHOT_IMAGE;

    @Override
    public KeycloakServer getServer() {
        return new ContainerKeycloakCluster(numContainers, images, debug);
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
