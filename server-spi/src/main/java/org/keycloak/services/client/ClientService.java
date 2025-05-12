package org.keycloak.services.client;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.Service;
import org.keycloak.services.ServiceException;

import java.util.Optional;
import java.util.stream.Stream;

public interface ClientService extends Service {

    enum ClientSearchOptions {
        DEFAULT
    }

    enum ClientProjectionOptions {
        DEFAULT(false),
        FULL_REPRESENTATION (true);

        Boolean fullRepresentation;

        ClientProjectionOptions(Boolean fullRepresentation) {
            this.fullRepresentation = fullRepresentation;
        }
    }

    Optional<ClientRepresentation> getClient(RealmModel realm, String clientId, ClientProjectionOptions projectionOptions);

    Stream<ClientRepresentation> getClients(RealmModel realm, ClientProjectionOptions projectionOptions, ClientSearchOptions searchOptions);

    ClientRepresentation deleteClient(RealmModel realm, String clientId);

    Stream<ClientRepresentation> deleteClients(RealmModel realm, ClientSearchOptions searchOptions);

    ClientRepresentation createOrUpdateClient(RealmModel realm, ClientRepresentation client) throws ServiceException;

    ClientRepresentation createClient(RealmModel realm, ClientRepresentation client) throws ServiceException;

}
