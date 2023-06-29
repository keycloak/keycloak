package org.keycloak.protocol.oidc.ida.mappers.connector;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class IdaHttpConnectorFactory implements IdaConnectorFactory {

    private static final String PROVIDER_ID = "ida-http-connector";

    @Override
    public IdaConnector create(KeycloakSession session) {
        return new IdaHttpConnector();
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
