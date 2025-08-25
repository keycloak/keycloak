package org.keycloak.authentication.authenticators.client;

import org.keycloak.Config;
import org.keycloak.authentication.ClientAuthenticationFlowContext;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.provider.EnvironmentDependentProviderFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.ServicesLogger;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Authenticator for Kubernetes ServiceAccount Tokens using the Kubernetes JWKS endpoint.
 *
 * @author <a href="mailto:sebastian.laskawiec@defenseunicorns.com">Sebastian Laskawiec</a>
 */
public class KubernetesJWTClientAuthenticator extends AbstractJWTClientAuthenticator implements EnvironmentDependentProviderFactory {

    public static final String PROVIDER_ID = "kubernetes-jwt";

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getDisplayType() {
        return "Kubernetes JWKS";
    }

    @Override
    public String getHelpText() {
        return "Validates client using Kubernetes ServiceAccount Token via Kubernetes JWKS Endpoint";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }

    @Override
    public List<ProviderConfigProperty> getConfigPropertiesPerClient() {
        return Collections.emptyList();
    }

    @Override
    public Map<String, Object> getAdapterConfiguration(ClientModel client) {
        return Collections.emptyMap();
    }

    @Override
    protected JWTClientValidator getValidator(ClientAuthenticationFlowContext context) {
        return new KubernetesJWTClientValidator(context, getId());
    }

    @Override
    public boolean isSupported(Config.Scope config) {
        return Profile.isFeatureEnabled(Profile.Feature.KUBERNETES_JWKS);
    }

    @Override
    protected String getAuthToken() {
        var tokenPath = java.nio.file.Paths.get(KubernetesJWTConstants.KUBERNETES_API_SERVER_ACCESS_TOKEN_PATH);
        if (!Files.exists(tokenPath)) {
            ServicesLogger.LOGGER.noKubernetesAPIServerAccessTokenFile(tokenPath.toString());
            return null;
        }
        try {
            var bearerToken = new String(java.nio.file.Files.readAllBytes(tokenPath));
            return bearerToken;
        } catch (IOException e) {
            ServicesLogger.LOGGER.failedReadingKubernetesAPIServerAccessTokenFile(tokenPath.toString());
            return  null;
        }
    }

    @Override
    protected void prepareClient(ClientModel client) {
        OIDCAdvancedConfigWrapper config = OIDCAdvancedConfigWrapper.fromClientModel(client);
        config.setJwksAuthToken(getAuthToken());
        config.setJwksUrl(KubernetesJWTConstants.KUBERNETES_JWKS_URL);
        config.setUseJwksUrl(true);
        if (config.getTokenEndpointAuthSigningMaxExp() < KubernetesJWTConstants.KUBERNETES_MAX_EXPIRATION_TIME_SECONDS) {
            config.setTokenEndpointAuthSigningMaxExp(KubernetesJWTConstants.KUBERNETES_MAX_EXPIRATION_TIME_SECONDS);
        }
    }
}
