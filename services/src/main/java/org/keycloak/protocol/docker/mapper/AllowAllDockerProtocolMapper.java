package org.keycloak.protocol.docker.mapper;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.docker.DockerAuthV2Protocol;
import org.keycloak.representations.docker.DockerAccess;
import org.keycloak.representations.docker.DockerResponseToken;

/**
 * Populates token with requested scope.  If more scopes are present than what has been requested, they will be removed.
 */
public class AllowAllDockerProtocolMapper extends DockerAuthV2ProtocolMapper implements DockerAuthV2AttributeMapper {

    public static final String PROVIDER_ID = "docker-v2-allow-all-mapper";

    @Override
    public String getDisplayType() {
        return "Allow All";
    }

    @Override
    public String getHelpText() {
        return "Allows all grants, returning the full set of requested access attributes as permitted attributes.";
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean appliesTo(final DockerResponseToken responseToken) {
        return true;
    }

    @Override
    public DockerResponseToken transformDockerResponseToken(final DockerResponseToken responseToken, final ProtocolMapperModel mappingModel,
                                                            final KeycloakSession session, final UserSessionModel userSession, final AuthenticatedClientSessionModel clientSession) {

        responseToken.getAccessItems().clear();

        final String requestedScope = clientSession.getNote(DockerAuthV2Protocol.SCOPE_PARAM);
        if (requestedScope != null) {
            final DockerAccess allRequestedAccess = new DockerAccess(requestedScope);
            responseToken.getAccessItems().add(allRequestedAccess);
        }

        return responseToken;
    }
}
