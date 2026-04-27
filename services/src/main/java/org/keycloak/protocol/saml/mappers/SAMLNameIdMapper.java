package org.keycloak.protocol.saml.mappers;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;

public interface SAMLNameIdMapper {

    String mapperNameId(String nameIdFormat, ProtocolMapperModel mappingModel, KeycloakSession session,
                                        UserSessionModel userSession, AuthenticatedClientSessionModel clientSession);

}