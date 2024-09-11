package org.keycloak.models.resource;

import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;

import java.util.Optional;

public interface ClientService extends ResourceService {

    Optional<ClientRepresentation> getClient(RealmModel realm, String clientId);

    @Override
    default void close() {
    }
}
