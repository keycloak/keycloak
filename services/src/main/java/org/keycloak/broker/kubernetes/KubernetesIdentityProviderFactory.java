package org.keycloak.broker.kubernetes;

import java.util.Map;

import org.keycloak.Config;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

public class KubernetesIdentityProviderFactory extends AbstractIdentityProviderFactory<KubernetesIdentityProvider> implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "kubernetes";

    @Override
    public String getName() {
        return "Kubernetes";
    }

    @Override
    public KubernetesIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new KubernetesIdentityProvider(session, new KubernetesIdentityProviderConfig(model));
    }

    @Override
    public Map<String, String> parseConfig(KeycloakSession session, String configString) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IdentityProviderModel createConfig() {
        return new KubernetesIdentityProviderConfig();
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.KUBERNETES_SERVICE_ACCOUNTS);
    }

}
