package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.util.TokenUtil;

import org.apache.http.client.methods.CloseableHttpResponse;

public class PasswordGrantRequest extends AbstractHttpPostRequest<PasswordGrantRequest, AccessTokenResponse> {

    private final String username;
    private final String password;
    private String otp;

    PasswordGrantRequest(String username, String password, AbstractOAuthClient<?> client) {
        super(client);
        this.username = username;
        this.password = password;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    public PasswordGrantRequest otp(String otp) {
        this.otp = otp;
        return this;
    }

    public PasswordGrantRequest dpopProof(String dpopProof) {
        header(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        return this;
    }

    public PasswordGrantRequest param(String name, String value) {
        parameter(name, value);
        return this;
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
        parameter("username", username);
        parameter("password", password);
        parameter("otp", otp);

        scope();
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
