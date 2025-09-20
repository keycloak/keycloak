package org.keycloak.testframework.http;

import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.ssl.SSLContextBuilder;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.https.ManagedCertificate;
import org.keycloak.testframework.https.ManagedCertificateException;
import org.keycloak.testframework.injection.InstanceContext;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.injection.RequestedInstance;
import org.keycloak.testframework.injection.Supplier;
import org.keycloak.testframework.server.KeycloakServer;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class HttpClientSupplier implements Supplier<HttpClient, InjectHttpClient> {

    @Override
    public HttpClient getValue(InstanceContext<HttpClient, InjectHttpClient> instanceContext) {
        HttpClientBuilder builder = HttpClientBuilder.create();

        KeycloakServer server = instanceContext.getDependency(KeycloakServer.class);
        if (server.isTlsEnabled()) {
            ManagedCertificate managedCert = instanceContext.getDependency(ManagedCertificate.class);
            try {
                KeyStore serverKeyStore = managedCert.getKeyStore();
                SSLContext sslContext = SSLContextBuilder.create()
                        .loadTrustMaterial(serverKeyStore, new TrustSelfSignedStrategy())
                        .build();

                SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(
                        sslContext,
                        NoopHostnameVerifier.INSTANCE);

                builder.setSSLSocketFactory(sslSocketFactory);
            } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
                throw new ManagedCertificateException(e);
            }
        }

        if (!instanceContext.getAnnotation().followRedirects()) {
            builder.disableRedirectHandling();
        }

        return builder.build();
    }

    @Override
    public void close(InstanceContext<HttpClient, InjectHttpClient> instanceContext) {
        try {
            ((CloseableHttpClient) instanceContext.getValue()).close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public LifeCycle getDefaultLifecycle() {
        return LifeCycle.GLOBAL;
    }

    @Override
    public boolean compatible(InstanceContext<HttpClient, InjectHttpClient> a, RequestedInstance<HttpClient, InjectHttpClient> b) {
        return a.getAnnotation().followRedirects() == b.getAnnotation().followRedirects();
    }

}
