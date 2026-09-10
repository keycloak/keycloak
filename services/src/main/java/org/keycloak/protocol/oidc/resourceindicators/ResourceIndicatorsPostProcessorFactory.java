package org.keycloak.protocol.oidc.resourceindicators;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class ResourceIndicatorsPostProcessorFactory implements TokenPostProcessorFactory, EnvironmentDependentProviderFactory {

    public static final String ID = "resource-indicators";

    @Override
    public TokenPostProcessor create(KeycloakSession session) {
        return new ResourceIndicatorsPostProcessor(session);
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
