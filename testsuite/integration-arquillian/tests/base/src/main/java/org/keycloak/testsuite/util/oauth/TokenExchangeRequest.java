package org.keycloak.testsuite.util.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.util.TokenUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class TokenExchangeRequest extends AbstractHttpPostRequest<AccessTokenResponse> {

    private final String subjectToken;
    private String clientId;
    private String clientSecret;
    private List<String> audience;
    private Map<String, String> additionalParams;

    TokenExchangeRequest(String subjectToken, String clientId, String clientSecret, OAuthClient client) {
        super(client);
        this.subjectToken = subjectToken;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    public TokenExchangeRequest audience(List<String> audience) {
        this.audience = audience;
        return this;
    }

    public TokenExchangeRequest additionalParams(Map<String, String> additionalParams) {
        this.additionalParams = additionalParams;
        return this;
    }

    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);

        authorization(clientId, clientSecret);

        parameter(OAuth2Constants.SUBJECT_TOKEN, subjectToken);
        parameter(OAuth2Constants.SUBJECT_TOKEN_TYPE, OAuth2Constants.ACCESS_TOKEN_TYPE);

        if (audience != null) {
            audience.forEach(a -> parameter(OAuth2Constants.AUDIENCE, a));
        }

        if (additionalParams != null) {
            additionalParams.forEach(this::parameter);
        }

        parameter(AdapterConstants.CLIENT_SESSION_STATE, client.getClientSessionState());
        parameter(AdapterConstants.CLIENT_SESSION_HOST, client.getClientSessionHost());

        parameter(OAuth2Constants.SCOPE, client.getScope());
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }

}
