package org.keycloak.representations;

import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.annotate.JsonUnwrapped;

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

    public UserClaimSet getUserClaimSet() {
        return this.userClaimSet;
    }

    public void setUserClaimSet(UserClaimSet userClaimSet) {
        this.userClaimSet = userClaimSet;
    }
}
