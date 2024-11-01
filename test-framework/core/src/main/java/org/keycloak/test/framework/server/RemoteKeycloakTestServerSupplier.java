package org.keycloak.test.framework.server;

public class RemoteKeycloakTestServerSupplier extends AbstractKeycloakTestServerSupplier {

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
}
