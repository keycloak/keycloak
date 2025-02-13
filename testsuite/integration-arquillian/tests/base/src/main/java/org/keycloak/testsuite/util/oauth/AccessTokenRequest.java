package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.util.TokenUtil;

import java.io.IOException;

public class AccessTokenRequest extends AbstractHttpPostRequest<AccessTokenResponse> {

    private final String code;
    private String clientSecret;

    AccessTokenRequest(String code, OAuthClient client) {
        super(client);
        this.code = code;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    public AccessTokenRequest clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE);

        authorization(client.getClientId(), clientSecret);

        parameter(OAuth2Constants.CODE, code);
        parameter(OAuth2Constants.REDIRECT_URI, client.getRedirectUri());

        parameter(AdapterConstants.CLIENT_SESSION_STATE, client.getClientSessionState());
        parameter(AdapterConstants.CLIENT_SESSION_HOST, client.getClientSessionHost());

        parameter(OAuth2Constants.CODE_VERIFIER, client.getCodeVerifier());

        header(TokenUtil.TOKEN_TYPE_DPOP, client.getDpopProof());
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
