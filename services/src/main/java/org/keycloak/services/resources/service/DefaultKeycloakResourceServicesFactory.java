package org.keycloak.services.resources.service;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.resource.KeycloakResourceServices;
import org.keycloak.models.resource.KeycloakResourceServicesFactory;

public class DefaultKeycloakResourceServicesFactory implements KeycloakResourceServicesFactory {
    public static final String PROVIDER_ID = "default";

    @Override
    public KeycloakResourceServices create(KeycloakSession session) {
        return new DefaultKeycloakResourceServices(session);
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
