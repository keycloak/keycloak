package org.keycloak.services;

import org.keycloak.models.KeycloakSession;
import org.keycloak.services.client.ClientService;

public class DefaultKeycloakServices implements KeycloakServices {
    private final KeycloakSession session;
    private ClientService clients;

    public DefaultKeycloakServices(KeycloakSession session) {
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
