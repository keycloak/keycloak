package org.keycloak.admin.client.jackson3;

import javax.net.ssl.SSLContext;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.WebTarget;

import org.keycloak.admin.client.spi.ResteasyClientProvider;

import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.jboss.resteasy.client.jaxrs.internal.ResteasyClientBuilderImpl;

public class ResteasyClientClassicProvider3 implements ResteasyClientProvider {

    @Override
    public Client newRestEasyClient(Object customJacksonProvider, SSLContext sslContext, boolean disableTrustManager) {
        Object provider = customJacksonProvider != null ? customJacksonProvider : new JacksonProvider3();
        ResteasyClientBuilderImpl builder = new ResteasyClientBuilderImpl()
                .connectionPoolSize(10)
                .register(provider, 100)
                .register(new StreamMessageBodyReader3(provider))
                .sslContext(sslContext);

        if (disableTrustManager) {
            builder.disableTrustManager();
        }
        return builder.build();
    }

    @Override
    public <R> R targetProxy(WebTarget client, Class<R> targetClass) {
        return ((ResteasyWebTarget) client).proxy(targetClass);
    }
}
