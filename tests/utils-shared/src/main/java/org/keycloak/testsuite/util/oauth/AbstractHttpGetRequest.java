package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.utils.MediaType;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;

public abstract class AbstractHttpGetRequest<T, R> {

    protected final AbstractOAuthClient<?> client;

    private HttpGet get;
    protected String endpointOverride;
    protected String bearerToken;
    protected Map<String, String> headers = new HashMap<>();

    public AbstractHttpGetRequest(AbstractOAuthClient<?> client) {
        this.client = client;
        this.headers.put("Accept", MediaType.APPLICATION_JSON);
    }

    protected abstract String getEndpoint();

    protected abstract void initRequest();

    /**
     * Override the endpoint URL for this request.
     * When specified, this takes precedence over {@link #getEndpoint()}.
     *
     * @param endpoint the endpoint URL to use
     * @return this request instance for method chaining
     */
    public T endpoint(String endpoint) {
        this.endpointOverride = endpoint;
        return request();
    }

    public T bearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
        return request();
    }

    public T header(String name, String value) {
        if (value != null) {
            this.headers.put(name, value);
        }
        return request();
    }

    public R send() {
        get = new HttpGet(endpointOverride != null ? endpointOverride : getEndpoint());

        initRequest();

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            get.setHeader(entry.getKey(), entry.getValue());
        }

        if (bearerToken != null) {
            get.addHeader("Authorization", "Bearer " + bearerToken);
        }

        try {
            return toResponse(client.httpClient().get().execute(get));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract R toResponse(CloseableHttpResponse response) throws IOException;

    @SuppressWarnings("unchecked")
    private T request() {
        return (T) this;
    }

}
