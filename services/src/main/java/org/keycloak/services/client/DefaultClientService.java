package org.keycloak.services.client;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import java.util.Optional;
import java.util.stream.Stream;

public class DefaultClientService implements ClientService {

    private final KeycloakSession session;

    public DefaultClientService(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Optional<ClientRepresentation> getClient(RealmModel realm, String clientId) {
        return Optional.empty();
    }

    @Override
    public Optional<ClientRepresentation> getClient(RealmModel realm, String clientId, Boolean fullRepresentation) {
        return Optional.empty();
    }

    @Override
    public Stream<ClientRepresentation> getClients(RealmModel realm) {
        return null;
    }

    @Override
    public ClientRepresentation createOrUpdateClient(ClientRepresentation client) {
        return null;
    }

    @Override
    public ClientRepresentation createClient(ClientRepresentation client) {
        return null;
    }

    @Override
    public void close() {

    }
}
