package org.keycloak.testsuite.util.oauth;

import java.io.IOException;

import org.keycloak.OAuth2Constants;
import org.keycloak.util.BasicAuthHelper;

import org.apache.http.client.methods.CloseableHttpResponse;

/**
 * Request class for sending invalid token requests for testing purposes.
 * This class extends AbstractHttpPostRequest to allow full control over parameters,
 * enabling tests to send requests with missing or invalid parameters.
 * <p>
 * This should only be used in test cases that specifically test error handling
 * for invalid requests. For valid requests, use {@link AccessTokenRequest} instead.
 */
public class InvalidTokenRequest extends AbstractHttpPostRequest<InvalidTokenRequest, AccessTokenResponse> {
    private final String code;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private String authDetailsJson;

    public InvalidTokenRequest(String code, AbstractOAuthClient<?> client) {
        super(client);
        this.code = code;
    }

    @Override
    protected String getEndpoint() {
        return client.getEndpoints().getToken();
    }

    public InvalidTokenRequest withClientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public InvalidTokenRequest withClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

    public InvalidTokenRequest withRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
        return this;
    }

    public InvalidTokenRequest withAuthDetails(String authDetailsJson) {
        this.authDetailsJson = authDetailsJson;
        return this;
    }

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE);
        parameter(OAuth2Constants.CODE, code);
        if (redirectUri != null) {
            parameter(OAuth2Constants.REDIRECT_URI, redirectUri);
        }
        if (authDetailsJson != null) {
            parameter(OAuth2Constants.AUTHORIZATION_DETAILS, authDetailsJson);
        }
    }

    @Override
    protected void authorization() {
        if (clientSecret != null && clientId != null) {
            String authorization = BasicAuthHelper.RFC6749.createHeader(clientId, clientSecret);
            header("Authorization", authorization);
        } else if (clientId != null) {
            parameter(OAuth2Constants.CLIENT_ID, clientId);
        }
        if (clientSecret != null && clientId == null) {
            // Edge case: client_secret without client_id
            parameter(OAuth2Constants.CLIENT_SECRET, clientSecret);
        }
    }

    @Override
    protected AccessTokenResponse toResponse(CloseableHttpResponse response) throws IOException {
        return new AccessTokenResponse(response);
    }
}
