package org.keycloak.authorization.client.token;

public class TokenExchangeRequest {
    private String clientId = null;
    private String clientSecret = null;
    private String grantType = "urn:ietf:params:oauth:grant-type:token-exchange";
    private String subjectToken = null;
    private String requestedTokenType = "urn:ietf:params:oauth:token-type:refresh_token";
    private String audience = null;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getSubjectToken() {
        return subjectToken;
    }

    public void setSubjectToken(String subjectToken) {
        this.subjectToken = subjectToken;
    }

    public String getRequestedTokenType() {
        return requestedTokenType;
    }

    public void setRequestedTokenType(String requestedTokenType) {
        this.requestedTokenType = requestedTokenType;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }
}
