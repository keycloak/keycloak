package org.keycloak.protocol.ssf.event.types.caep;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * Token Claims Change signals that a claim in a token, identified by the subject claim, has changed.
 */
public class TokenClaimsChanged extends CaepEvent {

    /**
     * See: https://openid.github.io/sharedsignals/openid-caep-1_0.html#name-token-claims-change
     */
    public static final String TYPE = "https://schemas.openid.net/secevent/caep/event-type/token-claims-change";

    /**
     * One or more claims with their new value(s)
     */
    @JsonProperty("claims")
    protected Map<String, Object> claims;

    public TokenClaimsChanged() {
        super(TYPE);
    }

    public Map<String, Object> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, Object> claims) {
        this.claims = claims;
    }

    @Override
    public String toString() {
        return "TokenClaimsChanged{" +
               "claims=" + claims +
               '}';
    }
}
