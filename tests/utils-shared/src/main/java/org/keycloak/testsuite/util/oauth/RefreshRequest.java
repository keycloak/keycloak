package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.util.TokenUtil;

import java.io.IOException;

public class RefreshRequest extends AbstractHttpPostRequest<RefreshRequest, AccessTokenResponse> {

    private final String refreshToken;

    RefreshRequest(String refreshToken, AbstractOAuthClient<?> client) {
        super(client);
        this.refreshToken = refreshToken;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    public RefreshRequest dpopProof(String dpopProof) {
        header(TokenUtil.TOKEN_TYPE_DPOP, dpopProof);
        return this;
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN);
        parameter(OAuth2Constants.REFRESH_TOKEN, refreshToken);
        scope(false);

        parameter(AdapterConstants.CLIENT_SESSION_STATE, client.getClientSessionState());
        parameter(AdapterConstants.CLIENT_SESSION_HOST, client.getClientSessionHost());
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
