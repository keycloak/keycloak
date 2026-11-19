package org.keycloak.protocol.oauth2.cimd.resourceindicators;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class CimdResourceIndicatorPostProcessorFactory implements TokenPostProcessorFactory, EnvironmentDependentProviderFactory {

    public static final String ID = "cimd-resource-indicators";

    @Override
    public TokenPostProcessor create(KeycloakSession session) {
        return new CimdResourceIndicatorPostProcessor(session);
    }

    @Override
    public String getId() {
        return ID;
    }

    // has to run before ResourceIndicatorsPostProcessor, which resolves the resource against the audience of the token.
    @Override
    public int order() {
        return 100;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CIMD);
    }
}
