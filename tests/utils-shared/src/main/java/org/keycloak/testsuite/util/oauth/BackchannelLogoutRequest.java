package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.OAuth2Constants;

import org.apache.http.client.methods.CloseableHttpResponse;

public class BackchannelLogoutRequest extends AbstractHttpPostRequest<BackchannelLogoutRequest, BackchannelLogoutResponse> {

    private final String logoutToken;

    BackchannelLogoutRequest(String logoutToken, AbstractOAuthClient<?> client) {
        super(client);
        this.logoutToken = logoutToken;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getBackChannelLogout();
    }

    protected void initRequest() {
        parameter(OAuth2Constants.LOGOUT_TOKEN, logoutToken);
    }

    @Override
    protected BackchannelLogoutResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new BackchannelLogoutResponse(response);
    }

}
