package org.keycloak.http.simple;

import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;

public class SimpleHttp {

    public enum OnCompletion {
        /**
         * No action.
         */
        NONE,

        /**
         * Automatically close the {@link HttpClient} once the response body is consumed.
         * Use only with a dedicated, per-call client (for example a per-IdP mTLS client), never with a shared/pooled client that must outlive the request.
         */
        CLOSE_CLIENT
    }

    private static final ObjectMapper DEFAULT_OBJECT_MAPPER = JsonSerialization.mapper;

    private final HttpClient client;
    private long maxConsumedResponseSize;
    private final OnCompletion onCompletion;
    private RequestConfig requestConfig;
    private ObjectMapper objectMapper;

    private SimpleHttp(HttpClient client, long maxConsumedResponseSize, OnCompletion onCompletion) {
        this.client = client;
        this.maxConsumedResponseSize = maxConsumedResponseSize;
        this.objectMapper = DEFAULT_OBJECT_MAPPER;
        this.onCompletion = onCompletion;
    }

    public static SimpleHttp create(KeycloakSession session) {
        HttpClientProvider provider = session.getProvider(HttpClientProvider.class);
        return new SimpleHttp(provider.getHttpClient(), provider.getMaxConsumedResponseSize(), OnCompletion.NONE);
    }

    public static SimpleHttp create(HttpClient httpClient) {
        return new SimpleHttp(httpClient, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE, OnCompletion.NONE);
    }

    public static SimpleHttp create(HttpClient httpClient, OnCompletion onCompletion) {
        return new SimpleHttp(httpClient, HttpClientProvider.DEFAULT_MAX_CONSUMED_RESPONSE_SIZE, onCompletion);
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
        return new SimpleHttpRequest(url, method, client, requestConfig, maxConsumedResponseSize, objectMapper, onCompletion);
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
