package org.keycloak.adapters.config;

import java.io.InputStream;

import org.keycloak.AbstractOAuthClient;

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
        oauthClient.setPassword(adapterConfig.getCredentials().get("password"));
        oauthClient.setAuthUrl(adapterConfig.getAuthUrl());
        oauthClient.setCodeUrl(adapterConfig.getCodeUrl());
        oauthClient.setTruststore(truststore);
        if (adapterConfig.getScope() != null) {
            String scope = encodeScope(adapterConfig.getScope());
            oauthClient.setScope(scope);
        }
    }
}
