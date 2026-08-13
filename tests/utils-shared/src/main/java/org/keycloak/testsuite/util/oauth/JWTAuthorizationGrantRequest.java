package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.OAuth2Constants;

import org.apache.http.client.methods.CloseableHttpResponse;

public class JWTAuthorizationGrantRequest extends AbstractHttpPostRequest<JWTAuthorizationGrantRequest, AccessTokenResponse> {

    private String assertion;

    JWTAuthorizationGrantRequest(String assertion, AbstractOAuthClient<?> client) {
        super(client);
        this.assertion = assertion;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    public JWTAuthorizationGrantRequest assertion(String assertion) {
        this.assertion = assertion;
        return this;
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.JWT_AUTHORIZATION_GRANT);
        parameter("assertion", assertion);
        scope();
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
