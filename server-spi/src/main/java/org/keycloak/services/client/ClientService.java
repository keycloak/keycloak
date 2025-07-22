package org.keycloak.services.client;

import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.Service;
import org.keycloak.services.ServiceException;

public interface ClientService extends Service {

    public static class ClientProjectionOptions {
        // TODO
    }

    public static class ClientSortAndSliceOptions {
        // order by
        // offset
        // limit
        // NOTE: this is not always the most desirable way to do pagination
    }

    public record CreateOrUpdateResult(ClientRepresentation representation, boolean created) {}

    Optional<ClientRepresentation> getClient(RealmModel realm, String clientId, ClientProjectionOptions projectionOptions);

    Stream<ClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions, String searchQuery, ClientSortAndSliceOptions sortAndSliceOptions);

    ClientRepresentation deleteClient(RealmModel realm, String clientId);

    Stream<ClientRepresentation> deleteClients(RealmModel realm, String searchQuery);

    CreateOrUpdateResult createOrUpdate(RealmModel realm, ClientRepresentation client, boolean allowUpdate) throws ServiceException;

}
