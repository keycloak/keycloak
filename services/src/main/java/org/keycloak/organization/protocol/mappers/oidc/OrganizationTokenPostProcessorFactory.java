package org.keycloak.organization.protocol.mappers.oidc;

import org.keycloak.Config;
import org.keycloak.common.Profile;
import org.keycloak.common.Profile.Feature;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.token.TokenPostProcessor;
import org.keycloak.protocol.oidc.token.TokenPostProcessorFactory;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class OrganizationTokenPostProcessorFactory implements TokenPostProcessorFactory, EnvironmentDependentProviderFactory {

    @Override
    public TokenPostProcessor create(KeycloakSession session) {
        return new OrganizationTokenPostProcessor(session);
    }

    @Override
    public String getId() {
        return "organizations";
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Feature.ORGANIZATION);
    }
}
