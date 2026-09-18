package org.keycloak.http.simple;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;

public class SimpleHttp {

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = JsonSerialization.mapper;

    private final HttpClient client;
    private long maxConsumedResponseSize;
    private RequestConfig requestConfig;
    private ObjectMapper objectMapper;

    private SimpleHttp(HttpClient client, long maxConsumedResponseSize) {
        this.client = client;
        this.maxConsumedResponseSize = maxConsumedResponseSize;
        this.objectMapper = DEFAULT_OBJECT_MAPPER;
    }

    public static SimpleHttp create(KeycloakSession session) {
        HttpClientProvider provider = session.getProvider(HttpClientProvider.class);
        return new SimpleHttp(provider.getHttpClient(), provider.getMaxConsumedResponseSize());
    }

    public static SimpleHttp create(HttpClient httpClient) {
        return new SimpleHttp(httpClient, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE);
    }

    public SimpleHttp withRequestConfig(RequestConfig requestConfig) {
        this.requestConfig = requestConfig;
        return this;
    }

    public SimpleHttp withObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public SimpleHttp withMaxConsumedResponseSize(long maxConsumedResponseSize) {
        this.maxConsumedResponseSize = maxConsumedResponseSize;
        return this;
    }

    private SimpleHttpRequest doRequest(String url, SimpleHttpMethod method) {
        return new SimpleHttpRequest(url, method, client, requestConfig, maxConsumedResponseSize, objectMapper);
    }

    public SimpleHttpRequest doGet(String url) {
        return doRequest(url, SimpleHttpMethod.GET);
    }

    public SimpleHttpRequest doPost(String url) {
        return doRequest(url, SimpleHttpMethod.POST);
    }

    public SimpleHttpRequest doPut(String url) {
        return doRequest(url, SimpleHttpMethod.PUT);
    }

    public SimpleHttpRequest doDelete(String url) {
        return doRequest(url, SimpleHttpMethod.DELETE);
    }

    public SimpleHttpRequest doHead(String url) {
        return doRequest(url, SimpleHttpMethod.HEAD);
    }

    public SimpleHttpRequest doPatch(String url) {
        return doRequest(url, SimpleHttpMethod.PATCH);
    }

    public SimpleHttpRequest doOptions(String url) {
        return doRequest(url, SimpleHttpMethod.OPTIONS);
    }

}
