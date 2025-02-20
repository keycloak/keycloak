package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.keycloak.utils.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class IntrospectionRequest extends AbstractHttpPostRequest<IntrospectionRequest, String> {

    private final String token;
    private String tokenTypeHint;
    private boolean jwtResponse = false;

    IntrospectionRequest(String token, OAuthClient client) {
        super(client);
        this.token = token;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getIntrospection();
    }

    public IntrospectionRequest tokenTypeHint(String tokenTypeHint) {
        this.tokenTypeHint = tokenTypeHint;
        return this;
    }

    public IntrospectionRequest jwtResponse() {
        this.jwtResponse = true;
        return this;
    }

    protected void initRequest() {
        parameter("token", token);
        parameter("token_type_hint", tokenTypeHint);
    }

    @Override
    protected String toResponse(CloseableHttpResponse response) throws IOException {
        return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
    }

    @Override
    protected String getAccept() {
        return jwtResponse ? MediaType.APPLICATION_JWT : super.getAccept();
    }
}
