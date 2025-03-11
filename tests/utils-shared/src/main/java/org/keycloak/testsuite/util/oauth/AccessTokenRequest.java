package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.util.TokenUtil;

import java.io.IOException;

public class AccessTokenRequest extends AbstractHttpPostRequest<AccessTokenRequest, AccessTokenResponse> {

    private final String code;

    AccessTokenRequest(String code, AbstractOAuthClient client) {
        super(client);
        this.code = code;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
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

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE);

        parameter(OAuth2Constants.CODE, code);
        parameter(OAuth2Constants.REDIRECT_URI, client.getRedirectUri());

        parameter(AdapterConstants.CLIENT_SESSION_STATE, client.getClientSessionState());
        parameter(AdapterConstants.CLIENT_SESSION_HOST, client.getClientSessionHost());
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
