package org.keycloak.testsuite.util;

import org.apache.http.client.RedirectStrategy;
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
        // Compatibility helper for migrated tests. Tests that need real mTLS should use
        // dedicated providers in the current test framework.
        return HttpClientBuilder.create().setRedirectStrategy(redirectStrategy).build();
    }
}
