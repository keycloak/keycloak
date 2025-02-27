package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.util.TokenUtil;

import java.io.IOException;

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

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getRevocation();
    }

    protected void initRequest() {
        parameter("token", token);
        parameter("token_type_hint", tokenTypeHint);

        header(TokenUtil.TOKEN_TYPE_DPOP, client.getDpopProof());
    }

    @Override
    protected TokenRevocationResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new TokenRevocationResponse(response);
    }

}
