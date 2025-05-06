package org.keycloak.models.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;

public interface ClientModelMapper {

    ClientRepresentation fromModel(ClientModel model);

    // ClientModel toModel(ClientModel baseModel, ClientRepresentation representation);
}
