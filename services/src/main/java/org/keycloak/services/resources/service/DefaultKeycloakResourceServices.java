package org.keycloak.services.resources.service;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.resource.ClientService;
import org.keycloak.models.resource.KeycloakResourceServices;

public class DefaultKeycloakResourceServices implements KeycloakResourceServices {
    private final KeycloakSession session;
    private ClientService clients;

    public DefaultKeycloakResourceServices(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public ClientService clients() {
        if (clients == null) {
            clients = session.getProvider(ClientService.class);
        }
        return clients;
    }

    @Override
    public void close() {

    }
}
