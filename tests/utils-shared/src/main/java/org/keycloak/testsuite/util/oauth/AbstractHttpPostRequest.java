package org.keycloak.testsuite.util.oauth;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.utils.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractHttpPostRequest<T, R> {

    protected final AbstractOAuthClient<?> client;

    protected String clientId;

    protected String clientSecret;

    protected HttpPost post;

    protected List<NameValuePair> parameters = new LinkedList<>();

    public AbstractHttpPostRequest(AbstractOAuthClient<?> client) {
        this.client = client;
    }

    protected abstract String getEndpoint();

    protected abstract void initRequest();

    public R send() {
        post = new HttpPost(getEndpoint());
        header("Accept", getAccept());
        header("Origin", client.config().getOrigin());

        if (client.getCustomParameters() != null) {
            client.getCustomParameters().forEach(this::parameter);
        }

        authorization();

        initRequest();

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        try {
            return toResponse(client.httpClient().get().execute(post));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public T client(String clientId) {
        this.clientId = clientId;
        this.clientSecret = null;
        return (T) this;
    }

    public T client(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        return (T) this;
    }

    protected void header(String name, String value) {
        if (value != null) {
            post.addHeader(name, value);
        }
    }

    protected void parameter(String name, String value) {
        if (value != null) {
            parameters.add(new BasicNameValuePair(name, value));
        }
    }

    private void authorization() {
        String clientId = this.clientId != null ? this.clientId : client.config().getClientId();
        String clientSecret = this.clientId != null ? this.clientSecret : client.config().getClientSecret();

        if (clientSecret != null) {
            String authorization = BasicAuthHelper.RFC6749.createHeader(clientId, clientSecret);
            header("Authorization", authorization);
        } else {
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

    protected abstract R toResponse(CloseableHttpResponse response) throws IOException;

}
