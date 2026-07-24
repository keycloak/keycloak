package org.keycloak.testsuite.util;

import org.apache.http.client.RedirectStrategy;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpClientUtils {

    private static final boolean SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

    // Add configurable socket timeout (default 30 seconds for better compatibility with slower environments like Windows)
    private static final int SOCKET_TIMEOUT_MILLIS = Integer.parseInt(
        System.getProperty("http.socket.timeout", "30000")
    );

    private static final int CONNECTION_TIMEOUT_MILLIS = Integer.parseInt(
        System.getProperty("http.connection.timeout", "10000")
    );

    public static CloseableHttpClient createDefault() {
        return createDefault(DefaultRedirectStrategy.INSTANCE);
    }

    public static CloseableHttpClient createDefault(RedirectStrategy redirectStrategy) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(SOCKET_TIMEOUT_MILLIS)
                .setConnectTimeout(CONNECTION_TIMEOUT_MILLIS)
                .build();

        if (SSL_REQUIRED) {
            String keyStorePath = System.getProperty("client.certificate.keystore");
            String keyStorePassword = System.getProperty("client.certificate.keystore.passphrase");
            String trustStorePath = System.getProperty("client.truststore");
            String trustStorePassword = System.getProperty("client.truststore.passphrase");
            return MutualTLSUtils.newCloseableHttpClient(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword, redirectStrategy);
        }

        return HttpClientBuilder.create()
                .setDefaultRequestConfig(requestConfig)
                .setRedirectStrategy(redirectStrategy)
                .build();
    }

}
