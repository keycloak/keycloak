package org.keycloak.adapters.undertow;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.plugins.providers.RegisterBuiltin;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.RealmConfiguration;
import org.keycloak.adapters.config.AdapterConfigLoader;

import javax.ws.rs.core.UriBuilder;
import java.io.InputStream;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class RealmConfigurationLoader extends AdapterConfigLoader {
    protected ResteasyClient client;
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

        for (Map.Entry<String, String> entry : getAdapterConfig().getCredentials().entrySet()) {
            realmConfiguration.getResourceCredentials().param(entry.getKey(), entry.getValue());
        }

        ResteasyClient client = getClient();

        realmConfiguration.setClient(client);
        realmConfiguration.setAuthUrl(UriBuilder.fromUri(authUrl).queryParam("client_id", resourceMetadata.getResourceName()));
        realmConfiguration.setCodeUrl(client.target(tokenUrl));
    }

    protected void initClient() {
        int size = 10;
        if (adapterConfig.getConnectionPoolSize() > 0)
            size = adapterConfig.getConnectionPoolSize();
        ResteasyClientBuilder.HostnameVerificationPolicy policy = ResteasyClientBuilder.HostnameVerificationPolicy.WILDCARD;
        if (adapterConfig.isAllowAnyHostname())
            policy = ResteasyClientBuilder.HostnameVerificationPolicy.ANY;
        ResteasyProviderFactory providerFactory = new ResteasyProviderFactory();
        ClassLoader old = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(RealmConfigurationLoader.class.getClassLoader());
        try {
            ResteasyProviderFactory.getInstance(); // initialize builtins
            RegisterBuiltin.register(providerFactory);
        } finally {
            Thread.currentThread().setContextClassLoader(old);
        }
        ResteasyClientBuilder builder = new ResteasyClientBuilder()
                .providerFactory(providerFactory)
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

    public ResteasyClient getClient() {
        return client;
    }

    public RealmConfiguration getRealmConfiguration() {
        return realmConfiguration;
    }

}
