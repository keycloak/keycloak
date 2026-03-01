package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.util.TokenUtil;

import org.apache.http.client.methods.CloseableHttpResponse;

public class TokenRevocationRequest extends AbstractHttpPostRequest<TokenRevocationRequest, TokenRevocationResponse> {

    private final String token;
    private String tokenTypeHint;

    TokenRevocationRequest(String token, AbstractOAuthClient<?> client) {
        super(client);
        this.token = token;
    }

    public TokenRevocationRequest tokenTypeHint(String tokenTypeHint) {
        this.tokenTypeHint = tokenTypeHint;
        return this;
    }

    public TokenRevocationRequest accessToken() {
        this.tokenTypeHint = "access_token";
        return this;
    }

    public TokenRevocationRequest refreshToken() {
        this.tokenTypeHint = "refresh_token";
        return this;
    }

    public TokenRevocationRequest dpopProof(String dpopProof) {
        header(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        return this;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getRevocation();
    }

    protected void initRequest() {
        parameter("token", token);
        parameter("token_type_hint", tokenTypeHint);
    }

    @Override
    protected TokenRevocationResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new TokenRevocationResponse(response);
    }

}
