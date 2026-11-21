package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.util.TokenUtil;

import org.apache.http.client.methods.CloseableHttpResponse;

public class AccountRequest extends AbstractHttpGetRequest<AccountRequest, AccountResponse> {

    private final String token;

    private boolean dpop = false;
    private String dpopProof;

    public AccountRequest(String token, AbstractOAuthClient<?> client) {
        super(client);
        this.token = token;
    }

    @Override
    protected String getEndpoint() {
        return UriBuilder.fromUri(client.getBaseUrl()).path("realms/{realm}/account").build(client.getRealm()).toString();
    }

    public AccountRequest dpop(String dpopProof) {
        this.dpop = true;
        this.dpopProof = dpopProof;
        return this;
    }

    @Override
    protected void initRequest() {
        String authorization = (dpop ? "DPoP" : "Bearer") + " " + token;
        header("Authorization", authorization);
        header(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
    }

    @Override
    protected AccountResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccountResponse(response);
    }

}
