package org.keycloak.testsuite.util;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class HttpClientUtils {

    private static final boolean SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required"));

    public static CloseableHttpClient createDefault() {
        if (SSL_REQUIRED) {
            String keyStorePath = System.getProperty("client.certificate.keystore");
            String keyStorePassword = System.getProperty("client.certificate.keystore.passphrase");
            String trustStorePath = System.getProperty("client.truststore");
            String trustStorePassword = System.getProperty("client.truststore.passphrase");
            return MutualTLSUtils.newCloseableHttpClient(keyStorePath, keyStorePassword, trustStorePath, trustStorePassword);
        }
        return HttpClientBuilder.create().build();
    }

}
