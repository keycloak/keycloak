package org.keycloak.test.framework.server;

public class RemoteKeycloakTestServerSupplier extends AbstractKeycloakTestServerSupplier {

    @Override
    public KeycloakTestServer getServer() {
        return new RemoteKeycloakTestServer();
    }

}
