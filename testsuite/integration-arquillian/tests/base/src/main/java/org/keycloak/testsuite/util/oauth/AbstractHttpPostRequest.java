package org.keycloak.testsuite.util.oauth;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.keycloak.OAuth2Constants;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

public abstract class AbstractHttpPostRequest<R> {

    protected final OAuthClient client;

    protected HttpPost post;

    protected List<NameValuePair> parameters = new LinkedList<>();

    public AbstractHttpPostRequest(OAuthClient client) {
        this.client = client;
    }

    protected abstract String getEndpoint();

    protected abstract void initRequest();

    public R send() {
        post = new HttpPost(getEndpoint());
        header("Accept", getAccept());
        header("Origin", client.getOrigin());

        if (client.getRequestHeaders() != null) {
            client.getRequestHeaders().forEach(this::header);
        }

        if (client.getCustomParameters() != null) {
            client.getCustomParameters().forEach(this::parameter);
        }

        initRequest();

        UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
        post.setEntity(formEntity);

        try {
            return toResponse(client.httpClient().get().execute(post));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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

    protected void authorization(String clientId, String clientSecret) {
        if (clientSecret != null) {
            String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
            header("Authorization", authorization);
        } else {
            parameter("client_id", clientId);
        }
    }

    protected void scope() {
        String scopeParam = client.isOpenid() ? TokenUtil.attachOIDCScope(client.getScope()) : client.getScope();
        if (scopeParam != null && !scopeParam.isEmpty()) {
            parameter(OAuth2Constants.SCOPE, scopeParam);
        }
    }

    protected String getAccept() {
        return MediaType.APPLICATION_JSON;
    }

    protected abstract R toResponse(CloseableHttpResponse response) throws IOException;

}
