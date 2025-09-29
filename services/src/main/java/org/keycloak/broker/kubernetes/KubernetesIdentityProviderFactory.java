package org.keycloak.broker.kubernetes;

import org.keycloak.Config;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.common.Profile;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderShowInAccountConsole;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.EnvironmentDependentProviderFactory;

import java.util.Map;

import static org.keycloak.broker.kubernetes.KubernetesConstants.KUBERNETES_SERVICE_HOST_KEY;
import static org.keycloak.broker.kubernetes.KubernetesConstants.KUBERNETES_SERVICE_PORT_HTTPS_KEY;

public class KubernetesIdentityProviderFactory extends AbstractIdentityProviderFactory<KubernetesIdentityProvider> implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "kubernetes";

    private String globalJwksUrl;

    @Override
    public String getName() {
        return "Kubernetes";
    }

    @Override
    public KubernetesIdentityProvider create(KeycloakSession session, IdentityProviderModel model) {
        return new KubernetesIdentityProvider(session, new KubernetesIdentityProviderConfig(model), globalJwksUrl);
    }

    @Override
    public void init(Config.Scope config) {
        String kubernetesServiceHost = System.getenv(KUBERNETES_SERVICE_HOST_KEY);
        String kubernetesServicePortHttps = System.getenv(KUBERNETES_SERVICE_PORT_HTTPS_KEY);
        if (kubernetesServiceHost != null && kubernetesServicePortHttps != null) {
            globalJwksUrl = "https://" + kubernetesServiceHost + ":" + kubernetesServicePortHttps + "/openid/v1/jwks";
        }
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
