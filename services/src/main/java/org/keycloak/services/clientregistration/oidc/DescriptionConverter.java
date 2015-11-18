package org.keycloak.services.clientregistration.oidc;

import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.representations.OIDCClientRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DescriptionConverter {

    public static ClientRepresentation toInternal(OIDCClientRepresentation clientOIDC) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(KeycloakModelUtils.generateId());
        client.setName(clientOIDC.getClientName());
        client.setRedirectUris(clientOIDC.getRedirectUris());
        client.setBaseUrl(clientOIDC.getClientUri());
        return client;
    }

    public static OIDCClientResponseRepresentation toExternalResponse(ClientRepresentation client) {
        OIDCClientResponseRepresentation response = new OIDCClientResponseRepresentation();
        response.setClientId(client.getClientId());

        response.setClientName(client.getName());
        response.setClientUri(client.getBaseUrl());

        response.setClientSecret(client.getSecret());
        response.setClientSecretExpiresAt(0);

        response.setRedirectUris(client.getRedirectUris());

        response.setRegistrationAccessToken(client.getRegistrationAccessToken());

        return response;
    }

}
