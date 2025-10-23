package org.keycloak.services.client;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultClientServiceFactory implements ClientServiceFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public ClientService create(KeycloakSession session) {
        return new DefaultClientService(session);
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
}
