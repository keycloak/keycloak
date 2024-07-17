package org.keycloak.test.framework.server;

public class DistributionKeycloakTestServerSupplier extends AbstractKeycloakTestServerSupplier {

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
}
