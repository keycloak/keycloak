package org.keycloak.admin.api.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;

public interface ApiClientMapper {

    ClientRepresentation fromModel(ClientModel model);

    // ClientModel toModel(ClientModel baseModel, ClientRepresentation representation);
}
