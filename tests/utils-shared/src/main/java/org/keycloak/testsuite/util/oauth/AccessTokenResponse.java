package org.keycloak.testsuite.util.oauth;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.keycloak.OAuth2Constants;

import org.apache.http.client.methods.CloseableHttpResponse;

public class AccessTokenResponse extends AbstractHttpResponse {

    private String idToken;
    private String accessToken;
    private String issuedTokenType;
    private String tokenType;
    private int expiresIn;
    private int refreshExpiresIn;
    private String refreshToken;
    private String scope;
    private String sessionState;

    private Map<String, Object> otherClaims;

    public AccessTokenResponse(CloseableHttpResponse response) throws IOException {
        super(response);
    }

    protected void parseContent() throws IOException {
        @SuppressWarnings("unchecked")
        Map<String, Object> responseJson = asJson(Map.class);

        otherClaims = new HashMap<>();

        for (Map.Entry<String, Object> entry : responseJson.entrySet()) {
            switch (entry.getKey()) {
                case OAuth2Constants.ID_TOKEN:
                    idToken = (String) entry.getValue();
                    break;
                case OAuth2Constants.ACCESS_TOKEN:
                    accessToken = (String) entry.getValue();
                    break;
                case OAuth2Constants.ISSUED_TOKEN_TYPE:
                    issuedTokenType = (String) entry.getValue();
                    break;
                case OAuth2Constants.TOKEN_TYPE:
                    tokenType = (String) entry.getValue();
                    break;
                case OAuth2Constants.EXPIRES_IN:
                    expiresIn = (Integer) entry.getValue();
                    break;
                case "refresh_expires_in":
                    refreshExpiresIn = (Integer) entry.getValue();
                    break;
                case OAuth2Constants.SESSION_STATE:
                    sessionState = (String) entry.getValue();
                    break;
                case OAuth2Constants.SCOPE:
                    scope = (String) entry.getValue();
                    break;
                case OAuth2Constants.REFRESH_TOKEN:
                    refreshToken = (String) entry.getValue();
                    break;
                default:
                    otherClaims.put(entry.getKey(), entry.getValue());
                    break;
            }
        }
    }

    public String getIdToken() {
        return idToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public int getExpiresIn() {
        return expiresIn;
    }

    public int getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getIssuedTokenType() {
        return issuedTokenType;
    }

    public String getTokenType() {
        return tokenType;
    }

    public String getScope() {
        return scope;
    }

    public String getSessionState() {
        return sessionState;
    }

    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

}
