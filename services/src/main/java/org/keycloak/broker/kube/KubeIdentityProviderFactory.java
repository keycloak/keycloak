package org.keycloak.broker.kube;

import org.keycloak.Config;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.broker.spiffe.SpiffeIdentityProviderConfig;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.Map;

public class KubeIdentityProviderFactory extends AbstractIdentityProviderFactory<KubeIdentityProvider> implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "kube";

    @Override
    public String getName() {
        return "Kubernetes";
    }

    @Override
    public KubeIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new KubeIdentityProvider(session, new OIDCIdentityProviderConfig(model));
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String configString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new OIDCIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.CLIENT_AUTH_FEDERATED);
    }

}
