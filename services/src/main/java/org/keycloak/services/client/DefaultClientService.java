package org.keycloak.services.client;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.ModelMapper;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ServiceException;
import org.keycloak.services.util.ResourceQueryFilter;

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
            String searchQuery, ClientSortAndSliceOptions sortAndSliceOptions) {
        ResourceQueryFilter<ClientRepresentation> filter = new ResourceQueryFilter<>(searchQuery);
        // here might call filter.getParsedQuery() to check some known/expected conditions to call optimized model fetches
        return filter.filterByQuery(realm.getClientsStream().map(mapper::fromModel));
    }

    @Override
    public CreateOrUpdateResult createOrUpdate(RealmModel realm, ClientRepresentation client, boolean allowUpdate)
            throws ServiceException {
        boolean created = false;
        ClientModel model = realm.getClientByClientId(client.getClientId());
        if (model != null) {
            if (!allowUpdate) {
                throw new ServiceException("Client already exists", Response.Status.CONFLICT);
            }
        } else {
            model = realm.addClient(client.getClientId());
            created = true;
        }

        // TODO: defaulting, validation, canonicalization

        mapper.toModel(model, client, realm);

        var updated = mapper.fromModel(model);

        return new CreateOrUpdateResult(updated, created);
    }

    @Override
    public ClientRepresentation deleteClient(RealmModel realm, String clientId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Stream<ClientRepresentation> deleteClients(RealmModel realm, String searchQuery) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void close() {

    }

}
