package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;

import java.util.HashMap;
import java.util.Map;

/**
 * OAuth 2.0 Access Token Response json
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class AccessTokenResponse {

    @com.fasterxml.jackson.annotation.JsonProperty("access_token")
    @JsonProperty("access_token")
    protected String token;

    @com.fasterxml.jackson.annotation.JsonProperty("expires_in")
    @JsonProperty("expires_in")
    protected long expiresIn;

    @com.fasterxml.jackson.annotation.JsonProperty("refresh_expires_in")
    @JsonProperty("refresh_expires_in")
    protected long refreshExpiresIn;

    @com.fasterxml.jackson.annotation.JsonProperty("refresh_token")
    @JsonProperty("refresh_token")
    protected String refreshToken;

    @com.fasterxml.jackson.annotation.JsonProperty("token_type")
    @JsonProperty("token_type")
    protected String tokenType;

    @com.fasterxml.jackson.annotation.JsonProperty("id_token")
    @JsonProperty("id_token")
    protected String idToken;

    @com.fasterxml.jackson.annotation.JsonProperty("not-before-policy")
    @JsonProperty("not-before-policy")
    protected int notBeforePolicy;

    @com.fasterxml.jackson.annotation.JsonProperty("session-state")
    @JsonProperty("session-state")
    protected String sessionState;

    protected Map<String, Object> otherClaims = new HashMap<String, Object>();



    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public long getRefreshExpiresIn() {
        return refreshExpiresIn;
    }

    public void setRefreshExpiresIn(long refreshExpiresIn) {
        this.refreshExpiresIn = refreshExpiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public int getNotBeforePolicy() {
        return notBeforePolicy;
    }

    public void setNotBeforePolicy(int notBeforePolicy) {
        this.notBeforePolicy = notBeforePolicy;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(String name, Object value) {
        otherClaims.put(name, value);
    }

}
