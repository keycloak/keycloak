package org.keycloak.services.clientregistration.oidc;

import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

import java.net.URI;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DescriptionConverter {

    public static ClientRepresentation toInternal(OIDCClientRepresentation clientOIDC) {
        ClientRepresentation client = new ClientRepresentation();
        client.setClientId(clientOIDC.getClientId());
        client.setName(clientOIDC.getClientName());
        client.setRedirectUris(clientOIDC.getRedirectUris());
        client.setBaseUrl(clientOIDC.getClientUri());
        return client;
    }

    public static OIDCClientRepresentation toExternalResponse(ClientRepresentation client, URI uri) {
        OIDCClientRepresentation response = new OIDCClientRepresentation();
        response.setClientId(client.getClientId());
        response.setClientName(client.getName());
        response.setClientUri(client.getBaseUrl());
        response.setClientSecret(client.getSecret());
        response.setRedirectUris(client.getRedirectUris());
        response.setRegistrationAccessToken(client.getRegistrationAccessToken());
        response.setRegistrationClientUri(uri.toString());
        return response;
    }

}
