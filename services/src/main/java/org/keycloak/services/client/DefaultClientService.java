package org.keycloak.services.client;

import jakarta.ws.rs.core.Response;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.ModelMapper;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;
import org.keycloak.services.ServiceException;
import org.keycloak.validation.jakarta.JakartaValidatorProvider;

import java.util.Optional;
import java.util.stream.Stream;

// TODO
public class DefaultClientService implements ClientService {
    private final KeycloakSession session;
    private final ClientModelMapper mapper;
    private final JakartaValidatorProvider validator;

    public DefaultClientService(KeycloakSession session) {
        this.session = session;
        this.mapper = session.getProvider(ModelMapper.class).clients();
        this.validator = session.getProvider(JakartaValidatorProvider.class);
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
    public CreateOrUpdateResult createOrUpdate(RealmModel realm, ClientRepresentation client, boolean allowUpdate)
            throws ServiceException {
        boolean created = false;
        ClientModel model = realm.getClientByClientId(client.getClientId());
        if (model != null) {
            if (!allowUpdate) {
                throw new ServiceException("Client already exists", Response.Status.CONFLICT);
            }
        } else {
            validator.validate(client, CreateClientDefault.class); // TODO improve it to avoid second validation when we know it is create and not update
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
    public Stream<ClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public void close() {

    }

}
