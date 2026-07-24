package org.keycloak.admin.client;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.ClientBuilder;

import org.keycloak.admin.client.spi.ResteasyClientClassicProvider;

import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

/**
 * Use {@link ResteasyClientClassicProvider#createClientBuilder()} instead
 */
@Deprecated
public class ClientBuilderWrapper {

    public static ClientBuilder create(SSLContext sslContext, boolean disableTrustManager) {
        ResteasyClientBuilderImpl result = ResteasyClientClassicProvider.createClientBuilder();
        result.sslContext(sslContext);
        if (disableTrustManager) {
            result.disableTrustManager();
        }
        return result;
    }

}
