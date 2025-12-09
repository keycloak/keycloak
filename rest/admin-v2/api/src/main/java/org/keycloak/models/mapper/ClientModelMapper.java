package org.keycloak.models.mapper;

import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.admin.v2.ClientRepresentation;
import org.keycloak.services.ServiceException;

public interface ClientModelMapper {

    ClientRepresentation fromModel(KeycloakSession session, ClientModel model);

    ClientModel toModel(KeycloakSession session, RealmModel realm, ClientModel existingModel, ClientRepresentation rep) throws ServiceException;

    ClientModel toModel(KeycloakSession session, RealmModel realm, ClientRepresentation rep) throws ServiceException;
}
