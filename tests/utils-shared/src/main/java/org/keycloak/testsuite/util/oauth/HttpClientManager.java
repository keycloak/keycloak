package org.keycloak.testsuite.util.oauth;

import org.apache.http.impl.client.CloseableHttpClient;

public class HttpClientManager {

    private final CloseableHttpClient defaultClient;
    private CloseableHttpClient customClient;

    public HttpClientManager(CloseableHttpClient defaultClient) {
        this.defaultClient = defaultClient;
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

}
