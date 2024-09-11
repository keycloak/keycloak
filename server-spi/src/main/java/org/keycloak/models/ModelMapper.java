package org.keycloak.models;

import org.keycloak.provider.Provider;
import org.keycloak.representations.admin.v2.ClientRepresentation;

public interface ModelMapper extends Provider {

    ClientRepresentation fromModel(ClientModel model);

    //ClientModel toModel(ClientRepresentation representation);

    default void close() {
    }
}
