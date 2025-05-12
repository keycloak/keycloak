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

public class DefaultClientService implements ClientService {
    private final KeycloakSession session;
    private final ClientModelMapper mapper;

    private final MockJPAClientRepresentationProvider mockJPAClientRepresentationProvider = new MockJPAClientRepresentationProvider();
    private final Defaults defaults = new Defaults();

    public DefaultClientService(KeycloakSession session) {
        this.session = session;
        this.mapper = session.getProvider(ModelMapper.class).clients();
    }

    @Override
    public Optional<ClientRepresentation> getClient(RealmModel realm, ClientProjectionOptions projectionOptions, ClientSearchOptions searchOptions) {
        // 1. Depending on the projection options, we either need to search for runtime representations or requested ones.
        //    The runtime representation is nothing more than ClientModel from the database.
        //    The requested representation needs to be taken from the ClientRepresentationProvider
        Stream<ClientRepresentation> stream = null;
        if (projectionOptions == ClientProjectionOptions.FULL_REPRESENTATION) {
            stream = mockJPAClientRepresentationProvider.stream();
        } else {
            stream = realm.getClientsStream()
                    .map(client -> mapper.fromModel(client));
        }

        // 2. Returning a collection or a single result is just a results of proper filtering.
        stream.filter(searchOptions.filter());

        // 3. Finally, we need to return the result as a stream.
        return stream.findAny();
    }

    @Override
    public Stream<ClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions, ClientSearchOptions searchOptions) {
        return realm.getClientsStream()
                .map(client -> projectionOptions != null && Boolean.TRUE.equals(projectionOptions.fullRepresentation)
                        ? mapper.fromModel(client)
                        : getReducedClientRepresentation(client.getClientId()));
    }

    @Override
    public ClientRepresentation deleteClient(RealmModel realm, String clientId) {
        var client = realm.getClientByClientId(clientId);
        if (client == null) {
            throw new ServiceException("Client not found", Response.Status.NOT_FOUND);
        }
        realm.removeClient(client.getId());
        return mapper.fromModel(client);
    }

    @Override
    public Stream<ClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
        var clients = realm.getClientsStream().toList();
        clients.forEach(client -> realm.removeClient(client.getId()));
        return clients.stream().map(mapper::fromModel);
    }

    @Override
    public ClientRepresentation createOrUpdateClient(RealmModel realm, ClientRepresentation client) throws ServiceException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public ClientRepresentation createClient(RealmModel realm, ClientRepresentation client) throws ServiceException {
        if (realm.getClientByClientId(client.getClientId()) != null) {
            throw new ServiceException("Client already exists", Response.Status.CONFLICT);
        }

        // 1. At first, we store the Client Representation as it was sent to us. This is the Requested Client Representation
        //    from https://github.com/keycloak/keycloak/discussions/38551
        this.mockJPAClientRepresentationProvider.addClient(client);

        // 2. Next, we apply defaults to it. Since the #getClient method returns what was actually requested,
        //    this needs to happen after persisting the clients to the "requested" Client Representations database table.
        var clientWithDefaults = defaults.applyDefaults(client);

        // 3. Finally, we create the Client model and persist it in the database. End of the day all Client Representations
        //    need to be pushed there to maintain backwards compatibility and play nicely with other Keycloak features
        var clientModel = mapper.toModel(clientWithDefaults);

        // We need a new method. Calling realm.addClient(client.getClientId()) and relying that Hibernates tracks
        // the returned value is a leaky abstraction. This should be corrected.
        //var model = realm.addClient(clientModel);

        return client;
    }

    @Override
    public void close() {
        // No resources to close
    }

    private static ClientRepresentation getReducedClientRepresentation(String clientId) {
        return new ClientRepresentation(clientId);
    }
}


