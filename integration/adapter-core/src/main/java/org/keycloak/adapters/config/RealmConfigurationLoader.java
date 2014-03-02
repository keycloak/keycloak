package org.keycloak.adapters.config;

import org.apache.http.client.HttpClient;
import org.keycloak.ServiceUrlConstants;
import org.keycloak.adapters.HttpClientBuilder;
import org.keycloak.util.KeycloakUriBuilder;

import java.io.InputStream;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmConfigurationLoader extends AdapterConfigLoader {
    protected HttpClient client;
    protected RealmConfiguration realmConfiguration;

    public RealmConfigurationLoader() {
    }

    public RealmConfigurationLoader(InputStream is) {
        loadConfig(is);
    }

    public void init(boolean setupClient) {
        init();
        initRealmConfiguration(setupClient);
    }

    protected void initRealmConfiguration(boolean setupClient) {
        realmConfiguration = new RealmConfiguration();
        realmConfiguration.setMetadata(resourceMetadata);
        realmConfiguration.setSslRequired(!adapterConfig.isSslNotRequired());
        realmConfiguration.setResourceCredentials(adapterConfig.getCredentials());
        if (!setupClient || adapterConfig.isBearerOnly()) return;
        initClient();
        if (adapterConfig.getAuthServerUrl() == null) {
            throw new RuntimeException("You must specify auth-url");
        }
        KeycloakUriBuilder serverBuilder = KeycloakUriBuilder.fromUri(adapterConfig.getAuthServerUrl());
        String authUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_LOGIN_PATH).build(adapterConfig.getRealm()).toString();
        String tokenUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_ACCESS_CODE_PATH).build(adapterConfig.getRealm()).toString();
        String refreshUrl = serverBuilder.clone().path(ServiceUrlConstants.TOKEN_SERVICE_REFRESH_PATH).build(adapterConfig.getRealm()).toString();


        HttpClient client = getClient();

        realmConfiguration.setClient(client);
        realmConfiguration.setAuthUrl(KeycloakUriBuilder.fromUri(authUrl).queryParam("client_id", resourceMetadata.getResourceName()));
        realmConfiguration.setCodeUrl(tokenUrl);
        realmConfiguration.setRefreshUrl(refreshUrl);
    }

    protected void initClient() {
        int size = 10;
        if (adapterConfig.getConnectionPoolSize() > 0)
            size = adapterConfig.getConnectionPoolSize();
        HttpClientBuilder.HostnameVerificationPolicy policy = HttpClientBuilder.HostnameVerificationPolicy.WILDCARD;
        if (adapterConfig.isAllowAnyHostname())
            policy = HttpClientBuilder.HostnameVerificationPolicy.ANY;
        HttpClientBuilder builder = new HttpClientBuilder()
                .connectionPoolSize(size)
                .hostnameVerification(policy)
                .keyStore(clientCertKeystore, adapterConfig.getClientKeyPassword());
        if (adapterConfig.isDisableTrustManager()) {
            builder.disableTrustManager();
        } else {
            builder.trustStore(truststore);
        }
        client = builder.build();
    }

    public HttpClient getClient() {
        return client;
    }

    public RealmConfiguration getRealmConfiguration() {
        return realmConfiguration;
    }

}
