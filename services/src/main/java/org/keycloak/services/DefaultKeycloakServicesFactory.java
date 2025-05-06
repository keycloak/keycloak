package org.keycloak.services;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class DefaultKeycloakServicesFactory implements KeycloakServicesFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public KeycloakServices create(KeycloakSession session) {
        return new DefaultKeycloakServices(session);
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
