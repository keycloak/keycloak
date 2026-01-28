package org.keycloak.broker.spiffe;

import java.util.Map;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.provider.ClientAssertionIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class SpiffeIdentityProviderFactory extends AbstractIdentityProviderFactory<SpiffeIdentityProvider> implements EnvironmentDependentProviderFactory, ClientAssertionIdentityProviderFactory {

    public static final String PROVIDER_ID = "spiffe";

    @Override
    public String getName() {
        return "SPIFFE";
    }

    @Override
    public SpiffeIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new SpiffeIdentityProvider(session, new SpiffeIdentityProviderConfig(model));
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String configString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new SpiffeIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SPIFFE);
    }

    @Override
    public ClientAssertionStrategy getClientAssertionStrategy() {
        return new SpiffeClientAssertionStrategy();
    }

}
