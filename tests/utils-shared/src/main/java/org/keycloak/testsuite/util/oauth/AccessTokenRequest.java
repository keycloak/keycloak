package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.util.TokenUtil;

import org.apache.http.client.methods.CloseableHttpResponse;

public class AccessTokenRequest extends AbstractHttpPostRequest<AccessTokenRequest, AccessTokenResponse> {

    private final String code;

    AccessTokenRequest(String code, AbstractOAuthClient<?> client) {
        super(client);
        this.code = code;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }


    public AccessTokenRequest signedJwt(String signedJwt) {
        parameter(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT);
        parameter(OAuth2Constants.CLIENT_ASSERTION, signedJwt);
        return this;
    }

    public AccessTokenRequest codeVerifier(PkceGenerator pkceGenerator) {
        if (pkceGenerator != null) {
            codeVerifier(pkceGenerator.getCodeVerifier());
        }
        return this;
    }

    public AccessTokenRequest codeVerifier(String codeVerifier) {
        parameter(OAuth2Constants.CODE_VERIFIER, codeVerifier);
        return this;
    }

    public AccessTokenRequest dpopProof(String dpopProof) {
        header(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        return this;
    }

    public AccessTokenRequest param(String name, String value) {
        parameter(name, value);
        return this;
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE);

        parameter(OAuth2Constants.CODE, code);
        parameter(OAuth2Constants.REDIRECT_URI, client.getRedirectUri());
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
