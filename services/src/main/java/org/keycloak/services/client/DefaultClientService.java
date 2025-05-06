package org.keycloak.services.client;

import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.ModelMapper;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ServiceException;

import java.util.Optional;
import java.util.stream.Stream;

// TODO
public class DefaultClientService implements ClientService {
    private final KeycloakSession session;
    private final ClientModelMapper mapper;

    public DefaultClientService(KeycloakSession session) {
        this.session = session;
        this.mapper = session.getProvider(ModelMapper.class).clients();
    }

    @Override
    public Optional<ClientRepresentation> getClient(RealmModel realm, String clientId) {
        return Optional.ofNullable(realm.getClientByClientId(clientId)).map(mapper::fromModel);
    }

    @Override
    public Optional<ClientRepresentation> getClient(RealmModel realm, String clientId, Boolean fullRepresentation) {
        // TODO reduced client rep
        return fullRepresentation != null && fullRepresentation ? getClient(realm, clientId) : Optional.of(getTestReducedClientRep(clientId));
    }

    @Override
    public Stream<ClientRepresentation> getClients(RealmModel realm) {
        return realm.getClientsStream().map(mapper::fromModel);
    }

    @Override
    public ClientRepresentation createOrUpdateClient(RealmModel realm, ClientRepresentation client) throws ServiceException {
        return null; // TODO
    }

    @Override
    public ClientRepresentation createClient(RealmModel realm, ClientRepresentation client) throws ServiceException {
        if (realm.getClientByClientId(client.getClientId()) != null) {
            throw new ServiceException("Client already exists", Response.Status.CONFLICT);
        }

        var model = realm.addClient(client.getClientId());
        return mapper.fromModel(model);
    }

    @Override
    public void close() {

    }

    // TODO tested reduced client representation
    private static ClientRepresentation getTestReducedClientRep(String clientId) {
        return new ClientRepresentation(clientId);
    }
}
