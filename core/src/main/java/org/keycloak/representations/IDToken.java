package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonAnyGetter;
import org.codehaus.jackson.annotate.JsonAnySetter;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonUnwrapped;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class IDToken extends JsonWebToken {

    @JsonProperty("nonce")
    protected String nonce;

    @JsonProperty("session_state")
    protected String sessionState;

    @JsonUnwrapped
    protected UserClaimSet userClaimSet = new UserClaimSet();

    protected Map<String, Object> otherClaims = new HashMap<String, Object>();

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getSessionState() {
        return sessionState;
    }

    public void setSessionState(String sessionState) {
        this.sessionState = sessionState;
    }

    /**
     * Standardized OpenID Connect claims
     *
     * @return
     */
    public UserClaimSet getUserClaimSet() {
        return this.userClaimSet;
    }

    public void setUserClaimSet(UserClaimSet userClaimSet) {
        this.userClaimSet = userClaimSet;
    }

    /**
     * This is a map of any other claims and data that might be in the IDToken.  Could be custom claims set up by the auth server
     *
     * @return
     */
    @JsonAnyGetter
    public Map<String, Object> getOtherClaims() {
        return otherClaims;
    }

    @JsonAnySetter
    public void setOtherClaims(Map<String, Object> otherClaims) {
        this.otherClaims = otherClaims;
    }
}
