package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.utils.MediaType;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;

public abstract class AbstractHttpPostRequest<T, R> {

    protected final AbstractOAuthClient<?> client;

    protected String clientId;
    protected String clientSecret;

    protected String clientAssertion;
    protected String clientAssertionType;

    protected String bearerToken;

    protected HttpPost post;

    protected Map<String, String> headers = new HashMap<>();
    protected List<NameValuePair> parameters = new LinkedList<>();
    protected HttpEntity entity;

    protected String endpoint;

    public AbstractHttpPostRequest(AbstractOAuthClient<?> client) {
        this.client = client;
    }

    protected abstract String getEndpoint();

    /**
     * Override the endpoint URL for this request.
     * When specified, this takes precedence over {@link #getEndpoint()}.
     *
     * @param endpoint the endpoint URL to use
     * @return this request instance for method chaining
     */
    public T endpoint(String endpoint) {
        this.endpoint = endpoint;
        return request();
    }

    protected abstract void initRequest();

    public R send() {
        post = new HttpPost(endpoint != null ? endpoint : getEndpoint());
        post.addHeader("Accept", getAccept());
        post.addHeader("Origin", client.config().getOrigin());

        authorization();

        initRequest();

        headers.forEach((n, v) -> post.addHeader(n, v));

        if (entity == null && !parameters.isEmpty()) {
            entity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        }

        // If entity is null, don't set (no body)
        if (entity != null) {
            post.setEntity(entity);
        }

        try {
            return toResponse(client.httpClient().get().execute(post));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public T bearerToken(String token) {
        this.bearerToken = token;
        return request();
    }

    public T client(String clientId) {
        this.clientId = clientId;
        this.clientSecret = null;
        return request();
    }

    public T client(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        return request();
    }

    public T clientJwt(String clientAssertion) {
        this.clientAssertion = clientAssertion;
        this.clientAssertionType = OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT;
        return request();
    }

    public T clientJwt(String clientAssertion, String clientAssertionType) {
        this.clientAssertion = clientAssertion;
        this.clientAssertionType = clientAssertionType;
        return request();
    }

    protected void header(String name, String value) {
        if (value != null) {
            headers.put(name, value);
        }
    }

    protected void parameter(String name, String value) {
        if (value != null) {
            parameters.add(new BasicNameValuePair(name, value));
        }
    }

    protected void authorization() {
        String clientId = this.clientId != null ? this.clientId : client.config().getClientId();
        String clientSecret = this.clientId != null ? this.clientSecret : client.config().getClientSecret();

        if (clientAssertion != null && clientAssertionType != null) {
            parameter("client_assertion_type", clientAssertionType);
            parameter("client_assertion", clientAssertion);
        } else if (bearerToken != null) {
            header("Authorization", "Bearer " + bearerToken);
        } else if (clientSecret != null) {
            String authorization = BasicAuthHelper.RFC6749.createHeader(clientId, clientSecret);
            header("Authorization", authorization);
        } else if (clientId != null) {
            parameter("client_id", clientId);
        }
    }

    protected void scope() {
        scope(true);
    }

    protected void scope(boolean attachOpenidIfNull) {
        parameter(OAuth2Constants.SCOPE, client.config().getScope(attachOpenidIfNull));
    }

    protected String getAccept() {
        return MediaType.APPLICATION_JSON;
    }

    protected String getParameter(String key) {
        return parameters.stream()
                .filter(vp -> vp.getName().equals(key))
                .map(NameValuePair::getValue)
                .findFirst().orElse(null);
    }

    protected boolean hasParameter(String key) {
        return getParameter(key) != null;
    }

    protected abstract R toResponse(CloseableHttpResponse response) throws IOException;

    private T request() {
        return (T) this;
    }
}
