package org.keycloak.testsuite.util.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;

import java.util.Map;

public class AuthorizationEndpointResponse {

    private boolean isRedirected;
    private String code;
    private String state;
    private String error;
    private String errorDescription;

    private String sessionState;

    // Just during OIDC implicit or hybrid flow
    private String accessToken;
    private String idToken;
    private String tokenType;
    private String expiresIn;

    // Just during FAPI JARM response mode JWT
    private String response;

    private String issuer;

    public AuthorizationEndpointResponse(OAuthClient client) {
        boolean fragment;
        String responseMode = client.config().getResponseMode();
        String responseType = client.config().getResponseType();
        if (responseMode == null || "jwt".equals(responseMode)) {
            try {
                fragment = responseType != null && OIDCResponseType.parse(responseType).isImplicitOrHybridFlow();
            } catch (IllegalArgumentException iae) {
                fragment = false;
            }
        } else {
            fragment = "fragment".equals(responseMode) || "fragment.jwt".equals(responseMode);
        }
        init(client, fragment);
    }

    public AuthorizationEndpointResponse(OAuthClient client, boolean fragment) {
        init(client, fragment);
    }

    private void init(OAuthClient client, boolean fragment) {
        isRedirected = client.getCurrentRequest().equals(client.getRedirectUri());
        Map<String, String> params = fragment ? client.getCurrentFragment() : client.getCurrentQuery();

        code = params.get(OAuth2Constants.CODE);
        state = params.get(OAuth2Constants.STATE);
        error = params.get(OAuth2Constants.ERROR);
        errorDescription = params.get(OAuth2Constants.ERROR_DESCRIPTION);
        sessionState = params.get(OAuth2Constants.SESSION_STATE);
        accessToken = params.get(OAuth2Constants.ACCESS_TOKEN);
        idToken = params.get(OAuth2Constants.ID_TOKEN);
        tokenType = params.get(OAuth2Constants.TOKEN_TYPE);
        expiresIn = params.get(OAuth2Constants.EXPIRES_IN);
        response = params.get(OAuth2Constants.RESPONSE);
        issuer = params.get(OAuth2Constants.ISSUER);
    }

    public boolean isRedirected() {
        return isRedirected;
    }

    public String getCode() {
        return code;
    }

    public String getState() {
        return state;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public String getSessionState() {
        return sessionState;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getExpiresIn() {
        return expiresIn;
    }

    public String getResponse() {
        return response;
    }

    public String getIssuer() {
        return issuer;
    }
}
