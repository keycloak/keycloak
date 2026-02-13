package org.keycloak.services.client;

import java.util.Optional;
import java.util.stream.Stream;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.BaseClientRepresentation;
import org.keycloak.services.Service;
import org.keycloak.services.ServiceException;

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

    record CreateOrUpdateResult(BaseClientRepresentation representation, boolean created) {}

    default Optional<BaseClientRepresentation> getClient(RealmModel realm, String clientId) throws ServiceException {
        return getClient(realm, clientId, null);
    }

    Optional<BaseClientRepresentation> getClient(RealmModel realm, String clientId, ClientProjectionOptions projectionOptions) throws ServiceException;

    default Stream<BaseClientRepresentation> getClients(RealmModel realm) {
        return getClients(realm, null, null, null);
    }

    Stream<BaseClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions, ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions);

    Stream<BaseClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions);

    void deleteClient(RealmModel realm, String clientId) throws ServiceException;

    CreateOrUpdateResult createOrUpdate(RealmModel realm, BaseClientRepresentation client, boolean allowUpdate) throws ServiceException;

}
