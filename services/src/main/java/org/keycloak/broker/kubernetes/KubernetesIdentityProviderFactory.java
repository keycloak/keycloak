package org.keycloak.broker.kubernetes;

import org.keycloak.Config;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.Map;

public class KubernetesIdentityProviderFactory extends AbstractIdentityProviderFactory<KubernetesIdentityProvider> implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "kube";

    private String globalJwksUrl;

    @Override
    public String getName() {
        return "Kubernetes";
    }

    @Override
    public KubernetesIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new KubernetesIdentityProvider(session, new OIDCIdentityProviderConfig(model), globalJwksUrl);
    }

    @Override
    public void init(Config.Scope config) {
        globalJwksUrl = config.get("jwksUrl", "https://kubernetes.default.svc/openid/v1/jwks");
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
        return Profile.isFeatureEnabled(Profile.Feature.KUBERNETES_SERVICE_ACCOUNTS);
    }

}
