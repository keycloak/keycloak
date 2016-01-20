package org.keycloak.servlet;

import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletOAuthClientBuilder {

    public static ServletOAuthClient build(InputStream is) {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(is);
        ServletOAuthClient client = new ServletOAuthClient();
        client.setDeployment(deployment);
        return client;
    }

    public static ServletOAuthClient build(AdapterConfig adapterConfig) {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(adapterConfig);
        ServletOAuthClient client = new ServletOAuthClient();
        client.setDeployment(deployment);
        return client;
    }

    public static void build(InputStream is, ServletOAuthClient oauthClient) {
        KeycloakDeployment deployment = KeycloakDeploymentBuilder.build(is);
        oauthClient.setDeployment(deployment);
    }
}
