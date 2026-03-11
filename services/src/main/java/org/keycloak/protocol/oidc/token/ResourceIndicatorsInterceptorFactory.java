package org.keycloak.protocol.oidc.token;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class ResourceIndicatorsInterceptorFactory implements TokenInterceptorProviderFactory, EnvironmentDependentProviderFactory {

    public static final String ID = "resource-indicators";

    @Override
    public TokenInterceptorProvider create(KeycloakSession session) {
        return new ResourceIndicatorsInterceptor(session);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATORS);
    }
}
