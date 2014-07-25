package org.keycloak.servlet;

import org.apache.http.client.HttpClient;
import org.keycloak.ServiceUrlConstants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.KeycloakUriBuilder;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class ServletOAuthClientBuilder {

    public static ServletOAuthClient build(InputStream is) {
        AdapterConfig adapterConfig = getAdapterConfig(is);
        return build(adapterConfig);
    }

    private static AdapterConfig getAdapterConfig(InputStream is) {
        try {
            return JsonSerialization.readValue(is, AdapterConfig.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static ServletOAuthClient build(AdapterConfig adapterConfig) {
        ServletOAuthClient oauthClient = new ServletOAuthClient();
        build(adapterConfig, oauthClient);
        return oauthClient;
    }

    public static void build(InputStream is, ServletOAuthClient oauthClient) {
        build(getAdapterConfig(is), oauthClient);
    }


    public static void build(AdapterConfig adapterConfig, ServletOAuthClient oauthClient) {
        HttpClient client = new HttpClientBuilder().build(adapterConfig);
        oauthClient.setClient(client);
        oauthClient.setClientId(adapterConfig.getResource());
        oauthClient.setPublicClient(adapterConfig.isPublicClient());
        oauthClient.setCredentials(adapterConfig.getCredentials());
        if (adapterConfig.getAuthServerUrl() == null) {
            throw new RuntimeException("You must specify auth-url");
        }
        KeycloakUriBuilder serverBuilder = KeycloakUriBuilder.fromUri(adapterConfig.getAuthServerUrl());
        oauthClient.setRelativeUrls(serverBuilder.clone().getHost() == null);

        String authUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGIN_PATH).build(adapterConfig.getRealm()).toString();
        String tokenUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_ACCESS_CODE_PATH).build(adapterConfig.getRealm()).toString();
        String refreshUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_REFRESH_PATH).build(adapterConfig.getRealm()).toString();
        oauthClient.setAuthUrl(authUrl);
        oauthClient.setCodeUrl(tokenUrl);
        oauthClient.setRefreshUrl(refreshUrl);
    }
}
