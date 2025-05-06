package org.keycloak.services.client;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.Service;
import org.keycloak.services.ServiceException;

import java.util.Optional;
import java.util.stream.Stream;

public interface ClientService extends Service {

    Optional<ClientRepresentation> getClient(RealmModel realm, String clientId);

    Optional<ClientRepresentation> getClient(RealmModel realm, String clientId, Boolean fullRepresentation);

    Stream<ClientRepresentation> getClients(RealmModel realm);

    ClientRepresentation createOrUpdateClient(RealmModel realm, ClientRepresentation client) throws ServiceException;

    ClientRepresentation createClient(RealmModel realm, ClientRepresentation client) throws ServiceException;
}
