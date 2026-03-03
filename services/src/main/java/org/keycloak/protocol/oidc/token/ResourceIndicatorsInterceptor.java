package org.keycloak.protocol.oidc.token;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.representations.AccessToken;

public class ResourceIndicatorsInterceptor implements TokenInterceptorProvider {

    private final KeycloakSession session;

    public ResourceIndicatorsInterceptor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public AccessToken intercept(AccessToken accessToken, ClientSessionContext clientSessionCtx) {
        if (!Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATORS)) {
            throw new TokenInterceptorException(OAuthErrorException.INVALID_REQUEST, "resource parameter support is not enabled");
        }

        String requestedResource = clientSessionCtx.getAttribute(OAuth2Constants.RESOURCE, String.class);
        if (requestedResource == null) {
            return null;
        }

        final Set<String> audienceToSet = new HashSet<>();

        if (requestedResource.startsWith("urn:client:")) {
            requestedResource = requestedResource.substring("urn:client:".length());
            for (String audience : accessToken.getAudience()) {
                if (audience.equals(requestedResource)) {
                    audienceToSet.add(audience);
                } else {
                    throw new TokenInterceptorException("invalid_target", "The requested resource is invalid, missing, unknown, or malformed.");
                }
            }
        } else {
            for (String audience : accessToken.getAudience()) {
                ClientModel client = session.clients().getClientByClientId(session.getContext().getRealm(), audience);
                if (client != null) {
                    String clientResourceUrl = client.getAttribute("resource_url");
                    if (clientResourceUrl.equals(requestedResource)) {
                        audienceToSet.add(requestedResource);
                    }
                }
            }
        }

        accessToken.audience(audienceToSet.toArray(String[]::new));

        return accessToken;
    }

    @Override
    public void close() {
    }

}
