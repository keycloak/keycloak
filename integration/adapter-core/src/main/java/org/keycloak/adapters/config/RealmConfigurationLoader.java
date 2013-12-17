package org.keycloak.adapters.config;

import org.apache.http.client.HttpClient;
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
        if (!setupClient || adapterConfig.isBearerOnly()) return;
        initClient();
        realmConfiguration = new RealmConfiguration();
        String authUrl = adapterConfig.getAuthUrl();
        if (authUrl == null) {
            throw new RuntimeException("You must specify auth-url");
        }
        String tokenUrl = adapterConfig.getCodeUrl();
        if (tokenUrl == null) {
            throw new RuntimeException("You mut specify code-url");
        }
        realmConfiguration.setMetadata(resourceMetadata);
        realmConfiguration.setSslRequired(!adapterConfig.isSslNotRequired());
        realmConfiguration.setResourceCredentials(adapterConfig.getCredentials());

        HttpClient client = getClient();

        realmConfiguration.setClient(client);
        realmConfiguration.setAuthUrl(KeycloakUriBuilder.fromUri(authUrl).queryParam("client_id", resourceMetadata.getResourceName()));
        realmConfiguration.setCodeUrl(tokenUrl);
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
