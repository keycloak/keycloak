package org.keycloak.test.framework.server;

public class EmbeddedKeycloakTestServerSupplier extends AbstractKeycloakTestServerSupplier {

    @Override
    public KeycloakTestServer getServer() {
        return new EmbeddedKeycloakTestServer();
    }

}
