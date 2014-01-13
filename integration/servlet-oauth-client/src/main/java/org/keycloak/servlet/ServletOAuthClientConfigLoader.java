package org.keycloak.servlet;

import java.io.InputStream;

import org.keycloak.adapters.config.OAuthClientConfigLoader;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServletOAuthClientConfigLoader extends OAuthClientConfigLoader {

    public ServletOAuthClientConfigLoader() {
    }

    public ServletOAuthClientConfigLoader(InputStream is) {
        super(is);
    }

    /**
     * For now, configure just things supported by ServletOAuthClient
     * @param setupClient
     */
    public void initOAuthClientConfiguration(boolean setupClient) {
        initOAuthClientConfiguration();
        if (setupClient) {
            initClient();
        }
    }

    public void configureServletOAuthClient(ServletOAuthClient oauthClient) {
        configureOAuthClient(oauthClient);
        if (client != null) {
            oauthClient.setClient(client);
        }
    }
}
