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
    public int order() {
        // the processors with a higher order run first. This one runs after the ones with the default order, so that
        // the resources are resolved against the audience that the other processors may have restricted (e.g. OID4VCI)
        // and none of them replaces the audience afterwards.
        return -1;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.RESOURCE_INDICATORS);
    }
}
