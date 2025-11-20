package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.util.TokenUtil;

import org.apache.http.client.methods.CloseableHttpResponse;

public class UserInfoRequest extends AbstractHttpGetRequest<UserInfoResponse> {

    private final String token;

    private boolean dpop = false;
    private String dpopProof;

    public UserInfoRequest(String token, AbstractOAuthClient<?> client) {
        super(client);
        this.token = token;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getUserInfo();
    }

    public UserInfoRequest dpop(String dpopProof) {
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
    protected UserInfoResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new UserInfoResponse(response);
    }

}
