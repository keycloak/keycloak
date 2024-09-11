package org.keycloak.services.resources.service;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.resource.ClientService;
import org.keycloak.models.resource.ClientServiceFactory;

public class DefaultClientServiceFactory implements ClientServiceFactory {
    public static final String PROVIDER_ID = "default";

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

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}
