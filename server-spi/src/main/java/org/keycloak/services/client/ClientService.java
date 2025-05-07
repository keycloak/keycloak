package org.keycloak.services.client;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.Service;
import org.keycloak.services.ServiceException;

import jakarta.ws.rs.core.Response.Status;

import java.util.Optional;
import java.util.stream.Stream;

public interface ClientService extends Service {

    public static class ClientSearchOptions {
        // TODO
    }

    public static class ClientProjectionOptions {
        // TODO
    }

    public static class ClientSortAndSliceOptions {
        // order by
        // offset
        // limit
        // NOTE: this is not always the most desirable way to do pagination
    }

    Optional<ClientRepresentation> getClient(RealmModel realm, String clientId, ClientProjectionOptions projectionOptions);

    Stream<ClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions, ClientSearchOptions searchOptions, ClientSortAndSliceOptions sortAndSliceOptions);

    ClientRepresentation deleteClient(RealmModel realm, String clientId);

    Stream<ClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions);

    default ClientRepresentation createOrUpdateClient(RealmModel realm, ClientRepresentation client) throws ServiceException {
        // If we can paramertize the services, it makes sense to define default handling like this
        // but this duplicate the existence check
        try {
            return createClient(realm, client);
        } catch (ServiceException e) {
            if (!e.getSuggestedResponseStatus().filter(Status.CONFLICT::equals).isPresent()) {
                throw e;
            }
        }
        return updateClient(realm, client);
    }

    ClientRepresentation updateClient(RealmModel realm, ClientRepresentation client) throws ServiceException;

    ClientRepresentation createClient(RealmModel realm, ClientRepresentation client) throws ServiceException;

}
