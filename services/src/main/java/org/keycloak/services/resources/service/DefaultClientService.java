package org.keycloak.services.resources.service;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.resource.ClientService;

public class DefaultClientService implements ClientService {
    private final KeycloakSession session;

    public DefaultClientService(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public void close() {

    }
}
