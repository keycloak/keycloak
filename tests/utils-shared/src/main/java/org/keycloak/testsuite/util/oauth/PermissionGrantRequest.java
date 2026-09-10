package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.authorization.authorization.AuthorizationTokenService;
import org.keycloak.utils.StringUtil;

import org.apache.http.client.methods.CloseableHttpResponse;

public class PermissionGrantRequest extends AbstractHttpPostRequest<JWTAuthorizationGrantRequest, AccessTokenResponse> {

    public <T> PermissionGrantRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.UMA_GRANT_TYPE);
        if (!parameters.contains(OAuth2Constants.AUDIENCE)) {
            parameter(OAuth2Constants.AUDIENCE, client.getClientId());
        }
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

    public PermissionGrantRequest claimToken(String idToken) {
        if (StringUtil.isBlank(idToken)) {
            throw new IllegalArgumentException("idToken cannot be blank");
        }
        parameter("claim_token", idToken);
        parameter("claim_token_format", AuthorizationTokenService.CLAIM_TOKEN_FORMAT_ID_TOKEN);
        return this;
    }
}
