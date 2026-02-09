package org.keycloak.testsuite.util.oauth.oid4vc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.util.JsonSerialization;
import org.keycloak.utils.MediaType;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

public abstract class AbstractOid4vcRequest<T, R> {

    protected final AbstractOAuthClient<?> client;
    protected String bearerToken;
    protected String endpointOverride;
    protected Map<String, String> headers = new HashMap<>();

    public AbstractOid4vcRequest(AbstractOAuthClient<?> client) {
        this.client = client;
    }

    public T bearerToken(String bearerToken) {
        this.bearerToken = bearerToken;
        return request();
    }

    public T endpoint(String endpoint) {
        this.endpointOverride = endpoint;
        return request();
    }

    public T header(String name, String value) {
        headers.put(name, value);
        return request();
    }

    protected abstract String getEndpoint();

    protected abstract Object getBody();

    public R send() {
        HttpPost post = new HttpPost(endpointOverride != null ? endpointOverride : getEndpoint());
        post.addHeader("Accept", MediaType.APPLICATION_JSON);
        post.addHeader("Content-Type", MediaType.APPLICATION_JSON);

        if (bearerToken != null) {
            post.addHeader("Authorization", "Bearer " + bearerToken);
        }

        headers.forEach(post::addHeader);

        try {
            Object body = getBody();
            if (body != null) {
                String jsonBody;
                if (body instanceof String) {
                    jsonBody = (String) body;
                } else {
                    jsonBody = JsonSerialization.writeValueAsString(body);
                }
                // Set entity even if empty string to support empty payload tests
                post.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
            }
            // If body is null, don't set entity (no body)
            return toResponse(client.httpClient().get().execute(post));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected abstract R toResponse(CloseableHttpResponse response) throws IOException;

    private T request() {
        return (T) this;
    }
}
