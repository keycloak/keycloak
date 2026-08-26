package org.keycloak.testsuite.util;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.keycloak.common.util.KeystoreUtil;

import org.apache.http.client.RedirectStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;

public class MutualTLSUtils {

    private MutualTLSUtils() {
    }

    public static CloseableHttpClient newCloseableHttpClient(String keyStorePath, String keyStorePassword,
            String trustStorePath, String trustStorePassword) {
        return newCloseableHttpClient(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword,
                DefaultRedirectStrategy.INSTANCE);
    }

    public static CloseableHttpClient newCloseableHttpClient(String keyStorePath, String keyStorePassword,
            String trustStorePath, String trustStorePassword, RedirectStrategy redirectStrategy) {
        KeyStore keyStore = null;
        if (keyStorePath != null) {
            try {
                keyStore = KeystoreUtil.loadKeyStore(keyStorePath, keyStorePassword);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        KeyStore trustStore = null;
        if (trustStorePath != null) {
            try {
                trustStore = KeystoreUtil.loadKeyStore(trustStorePath, trustStorePassword);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        if (keyStore != null || trustStore != null) {
            return newCloseableHttpClientSSL(keyStore, keyStorePassword, trustStore, redirectStrategy);
        }

        return HttpClientBuilder.create().setRedirectStrategy(redirectStrategy).build();
    }

    private static CloseableHttpClient newCloseableHttpClientSSL(KeyStore keyStore, String keyStorePassword,
            KeyStore trustStore, RedirectStrategy redirectStrategy) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");

            KeyManagerFactory keyManagerFactory = null;
            if (keyStore != null) {
                keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePassword != null ? keyStorePassword.toCharArray() : null);
            }

            TrustManagerFactory trustManagerFactory = null;
            if (trustStore != null) {
                trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(trustStore);
            }

            sslContext.init(
                    keyManagerFactory != null ? keyManagerFactory.getKeyManagers() : null,
                    trustManagerFactory != null ? trustManagerFactory.getTrustManagers() : null,
                    null);
            SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            return HttpClientBuilder.create()
                    .setSSLSocketFactory(socketFactory)
                    .setRedirectStrategy(redirectStrategy)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }
}
