package org.keycloak.protocol.ciba;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CIBAAuthReqIdJwt extends JsonWebToken {
    public static final String SCOPE = OAuth2Constants.SCOPE;
    public static final String SESSION_STATE = IDToken.SESSION_STATE;
    public static final String AUTH_RESULT_ID = "auth_result_id";
    public static final String THROTTLING_ID = "throttling_id";

    @JsonProperty(SCOPE)
    protected String scope;

    @JsonProperty(SESSION_STATE)
    protected String sessionState;

    @JsonProperty(AUTH_RESULT_ID)
    protected String authResultId;

    @JsonProperty(THROTTLING_ID)
    protected String throttlingId;

    @JsonProperty("key")
    protected String key;

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    public String getAuthResultId() {
        return authResultId;
    }

    public void setAuthResultId(String authResultId) {
        this.authResultId = authResultId;
    }

    public String getThrottlingId() {
        return throttlingId;
    }

    public void setThrottlingId(String throttlingId) {
        this.throttlingId = throttlingId;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

}
