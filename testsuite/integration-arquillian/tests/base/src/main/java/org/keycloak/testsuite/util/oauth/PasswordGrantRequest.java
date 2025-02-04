package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.util.TokenUtil;

import java.io.IOException;

public class PasswordGrantRequest extends AbstractHttpPostRequest<AccessTokenResponse> {

    private final String realm;
    private final String username;
    private final String password;
    private final String clientId;
    private String clientSecret;
    private String otp;

    PasswordGrantRequest(String realm, String username, String password, String clientId, String clientSecret, OAuthClient client) {
        super(client);
        this.realm = realm;
        this.username = username;
        this.password = password;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints(realm).getToken();
    }

    public PasswordGrantRequest clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public PasswordGrantRequest otp(String otp) {
        this.otp = otp;
        return this;
    }

    protected void initRequest() {
        header(TokenUtil.TOKEN_TYPE_DPOP, client.getDpopProof());

        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.PASSWORD);
        parameter("username", username);
        parameter("password", password);
        parameter("otp", otp);

        authorization(clientId, clientSecret);

        parameter(AdapterConstants.CLIENT_SESSION_STATE, client.getClientSessionState());
        parameter(AdapterConstants.CLIENT_SESSION_HOST, client.getClientSessionHost());

        scope();
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
