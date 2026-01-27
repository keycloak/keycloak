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

    public T endpoint(String endpoint) {
        this.endpointOverride = endpoint;
        return request();
    }

    public T bearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
        return request();
    }

    public T header(String name, String value) {
        this.headers.put(name, value);
        return request();
    }

    public R send() {
        get = new HttpGet(endpointOverride != null ? endpointOverride : getEndpoint());

        for (Map.Entry<String, String> entry : headers.entrySet()) {
            get.setHeader(entry.getKey(), entry.getValue());
        }

        if (bearerToken != null) {
            get.addHeader("Authorization", "Bearer " + bearerToken);
        }
        
        initRequest();
        try {
            return toResponse(client.httpClient().get().execute(get));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void addHeader(String name, String value) {
        if (value != null) {
            get.addHeader(name, value);
        }
    }

    protected abstract R toResponse(CloseableHttpResponse response) throws IOException;

    private T request() {
        return (T) this;
    }

}
