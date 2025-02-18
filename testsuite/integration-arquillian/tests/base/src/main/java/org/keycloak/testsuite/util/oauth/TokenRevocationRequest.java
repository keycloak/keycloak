package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.util.TokenUtil;

import java.io.IOException;

public class TokenRevocationRequest extends AbstractHttpPostRequest<TokenRevocationRequest, TokenRevocationResponse> {

    private final String token;
    private final String tokenTypeHint;

    TokenRevocationRequest(String token, String tokenTypeHint, OAuthClient client) {
        super(client);
        this.token = token;
        this.tokenTypeHint = tokenTypeHint;
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
