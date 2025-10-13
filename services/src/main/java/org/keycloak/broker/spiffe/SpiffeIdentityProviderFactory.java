package org.keycloak.broker.spiffe;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;

import java.util.List;
import java.util.Map;

public class SpiffeIdentityProviderFactory extends AbstractIdentityProviderFactory<SpiffeIdentityProvider> implements EnvironmentDependentProviderFactory {

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
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of(
            new ProviderConfigProperty(
                SpiffeIdentityProviderConfig.TRUST_DOMAIN_KEY,
                "SPIFFE Trust Domain",
                "The trust domain of the SPIFFE identity (e.g., 'spiffe://example.com'). This is used for validation.",
                ProviderConfigProperty.STRING_TYPE,
                null
            ),
            new ProviderConfigProperty(
                SpiffeIdentityProviderConfig.BUNDLE_ENDPOINT_KEY,
                "JWKS Bundle Endpoint",
                "The URL of the SPIFFE bundle endpoint that provides the JWKS for signature validation.",
                ProviderConfigProperty.STRING_TYPE,
                null
            )
        );
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.SPIFFE);
    }

}
