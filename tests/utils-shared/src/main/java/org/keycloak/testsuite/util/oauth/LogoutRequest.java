package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.OAuth2Constants;

import org.apache.http.client.methods.CloseableHttpResponse;

public class LogoutRequest extends AbstractHttpPostRequest<LogoutRequest, LogoutResponse> {

    private String refreshToken;
    private String idTokenHint;

    LogoutRequest(AbstractOAuthClient<?> client) {
        super(client);
    }

    public LogoutRequest refreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public LogoutRequest idTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
        return this;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getLogout();
    }

    protected void initRequest() {
        if (refreshToken != null) {
            parameter(OAuth2Constants.REFRESH_TOKEN, refreshToken);
        }
        if (idTokenHint != null) {
            parameter(OAuth2Constants.ID_TOKEN_HINT, idTokenHint);
        }
    }

    @Override
    protected LogoutResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new LogoutResponse(response);
    }

}
