package org.keycloak.testsuite.util.oauth.ciba;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AbstractOAuthClient;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.http.client.methods.CloseableHttpResponse;

import static org.keycloak.protocol.oidc.grants.ciba.CibaGrantType.AUTH_REQ_ID;

public class BackchannelAuthenticationTokenRequest extends AbstractHttpPostRequest<BackchannelAuthenticationTokenRequest, AccessTokenResponse> {

    private final String authReqId;

    BackchannelAuthenticationTokenRequest(String authReqId, AbstractOAuthClient<?> client) {
        super(client);
        this.authReqId = authReqId;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.CIBA_GRANT_TYPE);
        parameter(AUTH_REQ_ID, authReqId);
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
