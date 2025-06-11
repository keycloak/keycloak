package org.keycloak.models.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;

public interface ClientModelMapper {

    ClientRepresentation fromModel(ClientModel model);

    void toModel(ClientModel model, ClientRepresentation rep, RealmModel realm);
}
