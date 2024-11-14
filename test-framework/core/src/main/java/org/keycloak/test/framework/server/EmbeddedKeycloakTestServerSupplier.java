package org.keycloak.test.framework.server;

import org.jboss.logging.Logger;

public class EmbeddedKeycloakTestServerSupplier extends AbstractKeycloakTestServerSupplier {

    private static final Logger LOGGER = Logger.getLogger(EmbeddedKeycloakTestServerSupplier.class);

    @Override
    public KeycloakTestServer getServer() {
        return new EmbeddedKeycloakTestServer();
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
