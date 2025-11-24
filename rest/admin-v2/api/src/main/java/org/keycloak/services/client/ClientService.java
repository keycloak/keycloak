package org.keycloak.services.client;

import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.Service;
import org.keycloak.services.ServiceException;
import org.keycloak.services.resources.admin.ClientResource;
import org.keycloak.services.resources.admin.ClientsResource;

public interface ClientService extends Service {

    class ClientSearchOptions {
        // TODO
    }

    class ClientProjectionOptions {
        // TODO
    }

    class ClientSortAndSliceOptions {
        // order by
        // offset
        // limit
        // NOTE: this is not always the most desirable way to do pagination
    }

    record CreateOrUpdateResult(ClientRepresentation representation, boolean created) {}

    Optional<ClientRepresentation> getClient(ClientResource clientResource, RealmModel realm, String clientId, ClientProjectionOptions projectionOptions);

    Stream<ClientRepresentation> getClients(ClientsResource clientsResource, RealmModel realm, ClientProjectionOptions projectionOptions, ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions);

    Stream<ClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions);

    CreateOrUpdateResult createOrUpdate(ClientsResource clientsResource, ClientResource clientResource, RealmModel realm, ClientRepresentation client, boolean allowUpdate) throws ServiceException;

}
