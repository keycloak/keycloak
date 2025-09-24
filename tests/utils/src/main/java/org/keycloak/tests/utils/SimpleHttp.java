package org.keycloak.tests.utils;

import org.apache.http.client.HttpClient;
import org.keycloak.connections.httpclient.HttpClientProvider;

public class SimpleHttp extends org.keycloak.broker.provider.util.SimpleHttp {

    protected SimpleHttp(String url, String method, HttpClient client, long maxConsumedResponseSize) {
        super(url, method, client, maxConsumedResponseSize);
    }

    public static org.keycloak.broker.provider.util.SimpleHttp doDelete(String url, HttpClient client) {
        return org.keycloak.broker.provider.util.SimpleHttp.doDelete(url, client, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

    public static org.keycloak.broker.provider.util.SimpleHttp doPost(String url, HttpClient client) {
        return org.keycloak.broker.provider.util.SimpleHttp.doPost(url, client, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

    public static org.keycloak.broker.provider.util.SimpleHttp doPut(String url, HttpClient client) {
        return org.keycloak.broker.provider.util.SimpleHttp.doPut(url, client, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

    public static org.keycloak.broker.provider.util.SimpleHttp doGet(String url, HttpClient client) {
        return org.keycloak.broker.provider.util.SimpleHttp.doGet(url, client, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

    public static org.keycloak.broker.provider.util.SimpleHttp doHead(String url, HttpClient client) {
        return org.keycloak.broker.provider.util.SimpleHttp.doHead(url, client, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

}
