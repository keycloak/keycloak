package org.keycloak.testsuite.util.oauth;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.testsuite.util.MutualTLSUtils;

public class HttpClientManager {

    private CloseableHttpClient defaultClient;
    private CloseableHttpClient customClient;

    public HttpClientManager() {
        defaultClient = createDefault();
    }

    public CloseableHttpClient get() {
        if (customClient != null) {
            return customClient;
        } else {
            return defaultClient;
        }
    }

    public void set(CloseableHttpClient httpClient) {
        this.customClient = httpClient;
    }

    public void reset() {
        this.customClient = null;
    }

    public static CloseableHttpClient createDefault() {
        if (OAuthClient.SSL_REQUIRED) {
            String keyStorePath = System.getProperty("client.certificate.keystore");
            String keyStorePassword = System.getProperty("client.certificate.keystore.passphrase");
            String trustStorePath = System.getProperty("client.truststore");
            String trustStorePassword = System.getProperty("client.truststore.passphrase");
            return createHttpClientSSL(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
        }
        return HttpClientBuilder.create().build();
    }

    public static CloseableHttpClient createHttpClientSSL(String keyStorePath,
                                                                String keyStorePassword, String trustStorePath, String trustStorePassword) {
        return MutualTLSUtils.newCloseableHttpClient(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
    }

}
