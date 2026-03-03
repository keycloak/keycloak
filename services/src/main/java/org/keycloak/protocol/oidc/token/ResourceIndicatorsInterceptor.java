package org.keycloak.protocol.oidc.token;

import java.util.HashSet;
import java.util.Set;

import org.keycloak.Config;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.representations.AccessToken;

public class ResourceIndicatorsInterceptor implements TokenInterceptorProviderFactory, TokenInterceptorProvider, EnvironmentDependentProviderFactory {

    @Override
    public AccessToken intercept(AccessToken accessToken, ClientSessionContext clientSessionCtx) {
        if (!Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATORS)) {
            throw new TokenInterceptorException(OAuthErrorException.INVALID_REQUEST, "resource parameter support is not enabled");
        }

        String requestedResource = clientSessionCtx.getAttribute(OAuth2Constants.RESOURCE, String.class);
        if (requestedResource == null) {
            return null;
        }

        if (requestedResource.startsWith("urn:client:")) {
            requestedResource = requestedResource.substring("urn:client:".length());
            final Set<String> audienceToSet = new HashSet<>();
            for (String audience : accessToken.getAudience()) {
                if (audience.equals(requestedResource)) {
                    audienceToSet.add(audience);
                } else {
                    throw new TokenInterceptorException("invalid_target", "The requested resource is invalid, missing, unknown, or malformed.");
                }
            }
            accessToken.audience(audienceToSet.toArray(String[]::new));
        }
        return accessToken;
    }

    @Override
    public TokenInterceptorProvider create(KeycloakSession session) {
        return this;
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return "resource-indicators";
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATORS);
    }
}
