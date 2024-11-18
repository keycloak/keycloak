package org.keycloak.test.framework.server;

import org.jboss.logging.Logger;

public class RemoteKeycloakTestServerSupplier extends AbstractKeycloakTestServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(RemoteKeycloakTestServerSupplier.class);

    @Override
    public KeycloakTestServer getServer() {
        return new RemoteKeycloakTestServer();
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
