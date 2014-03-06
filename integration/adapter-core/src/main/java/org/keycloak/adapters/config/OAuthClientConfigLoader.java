package org.keycloak.adapters.config;

import java.io.InputStream;

import org.keycloak.AbstractOAuthClient;
import org.keycloak.ServiceUrlConstants;
import org.keycloak.util.KeycloakUriBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class OAuthClientConfigLoader extends RealmConfigurationLoader {

    public OAuthClientConfigLoader() {
    }

    public OAuthClientConfigLoader(InputStream is) {
        super(is);
    }

    /**
     * For now, configure just things supported by AbstractOAuthClient
     */
    public void initOAuthClientConfiguration() {
        initTruststore();
        initClientKeystore();
    }

    public void configureOAuthClient(AbstractOAuthClient oauthClient) {
        oauthClient.setClientId(adapterConfig.getResource());
        oauthClient.setPublicClient(adapterConfig.isPublicClient());
        oauthClient.setCredentials(adapterConfig.getCredentials());
        if (adapterConfig.getAuthServerUrl() == null) {
            throw new RuntimeException("You must specify auth-url");
        }
        KeycloakUriBuilder serverBuilder = KeycloakUriBuilder.fromUri(adapterConfig.getAuthServerUrl());
        String authUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGIN_PATH).build(adapterConfig.getRealm()).toString();
        String tokenUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_ACCESS_CODE_PATH).build(adapterConfig.getRealm()).toString();
        String refreshUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_REFRESH_PATH).build(adapterConfig.getRealm()).toString();
        oauthClient.setAuthUrl(authUrl);
        oauthClient.setCodeUrl(tokenUrl);
        oauthClient.setRefreshUrl(refreshUrl);
        oauthClient.setTruststore(truststore);
        if (adapterConfig.getScope() != null) {
            String scope = encodeScope(adapterConfig.getScope());
            oauthClient.setScope(scope);
        }
    }
}
