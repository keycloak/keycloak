package org.keycloak.admin.api.mapper;

import org.keycloak.admin.api.client.ClientRepresentation;
import org.keycloak.models.ClientModel;
import org.keycloak.provider.Provider;

public interface ApiModelMapper extends Provider {

    ClientRepresentation fromModel(ClientModel model);

}
