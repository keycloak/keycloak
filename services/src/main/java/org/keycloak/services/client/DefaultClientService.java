package org.keycloak.services.client;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
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
    public Optional<ClientRepresentation> getClient(RealmModel realm, String clientId,
            ClientProjectionOptions projectionOptions) {
        return Optional.ofNullable(realm.getClientByClientId(clientId)).map(mapper::fromModel);
    }

    @Override
    public Stream<ClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions,
            ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions) {
        return realm.getClientsStream().map(mapper::fromModel);
    }

    @Override
    public ClientRepresentation createClient(RealmModel realm, ClientRepresentation client) throws ServiceException {
        if (realm.getClientByClientId(client.getClientId()) != null) {
            throw new ServiceException("Client already exists", Response.Status.CONFLICT);
        }

        var model = realm.addClient(client.getClientId());
        // TODO: overlay model
        return mapper.fromModel(model);
    }

    @Override
    public ClientRepresentation deleteClient(RealmModel realm, String clientId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Stream<ClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ClientRepresentation updateClient(RealmModel realm, ClientRepresentation client) throws ServiceException {
        ClientModel model = realm.getClientByClientId(client.getClientId());
        if (model == null) {
            throw new ServiceException("Client does not exist", Response.Status.NOT_FOUND);
        }

        // TODO: overlay model
        return mapper.fromModel(model);
    }

    @Override
    public void close() {

    }

}
