package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.utils.MediaType;

import org.apache.http.client.methods.CloseableHttpResponse;

public class IntrospectionRequest extends AbstractHttpPostRequest<IntrospectionRequest, IntrospectionResponse> {

    private final String token;
    private String tokenTypeHint;
    private boolean jwtResponse = false;

    IntrospectionRequest(String token, AbstractOAuthClient<?> client) {
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
    protected IntrospectionResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new IntrospectionResponse(response);
    }

    @Override
    protected String getAccept() {
        return jwtResponse ? MediaType.APPLICATION_JWT : super.getAccept();
    }
}
