package org.keycloak.services.client;

import java.util.Optional;
import java.util.stream.Stream;

import jakarta.ws.rs.core.Response;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.mapper.ClientModelMapper;
import org.keycloak.models.mapper.ModelMapper;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.representations.admin.v2.validation.CreateClientDefault;
import org.keycloak.services.ServiceException;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.ClientsResource;
import org.keycloak.validation.jakarta.JakartaValidator;

// TODO
public class DefaultClientService implements ClientService {
    private final KeycloakSession session;
    private final ClientModelMapper mapper;
    private final JakartaValidator validator;

    public DefaultClientService(KeycloakSession session) {
        this.session = session;
        this.mapper = session.getProvider(ModelMapper.class).clients();
        this.validator = session.getProvider(JakartaValidator.class);
    }

    @Override
    public Optional<ClientRepresentation> getClient(ClientResource clientResource, RealmModel realm, String clientId,
            ClientProjectionOptions projectionOptions) {
        // TODO: is the access map on the representation needed
        return Optional.ofNullable(clientResource).map(ClientResource::viewClientModel).map(mapper::fromModel);
    }

    @Override
    public Stream<ClientRepresentation> getClients(ClientsResource clientsResource, RealmModel realm,
            ClientProjectionOptions projectionOptions, ClientSearchOptions searchOptions,
            ClientSortAndSliceOptions sortAndSliceOptions) {
        // TODO: is the access map on the representation needed
        return clientsResource.getClientModels(null, true, false, null, null, null).map(mapper::fromModel);
    }

    @Override
    public CreateOrUpdateResult createOrUpdate(ClientsResource clientsResource, ClientResource clientResource,
            RealmModel realm, ClientRepresentation client, boolean allowUpdate) throws ServiceException {
        boolean created = false;
        ClientModel model;
        if (clientResource != null) {
            if (!allowUpdate) {
                throw new ServiceException("Client already exists", Response.Status.CONFLICT);
            }
            model = clientResource.viewClientModel();
            mapper.toModel(model, client, realm);
            var rep = ModelToRepresentation.toRepresentation(model, session);
            clientResource.update(rep);
        } else {
            created = true;
            validator.validate(client, CreateClientDefault.class); // TODO improve it to avoid second validation when we know it is create and not update
            model = clientsResource.createClientModel(mapper.mapRepresentationV2toV1(client));
        }

        var updated = mapper.fromModel(model);

        return new CreateOrUpdateResult(updated, created);
    }

    @Override
    public Stream<ClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions) {
        // TODO Auto-generated method stub
        return null;
    }

}
