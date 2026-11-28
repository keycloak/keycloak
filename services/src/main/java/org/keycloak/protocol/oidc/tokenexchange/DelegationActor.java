package org.keycloak.protocol.oidc.tokenexchange;

import java.util.Map;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;

/**
 * A single actor of the nested "act" claim of RFC 8693 section 4.1.
 */
public final class DelegationActor {

    private final Map<String, Object> claims;

    DelegationActor(Map<String, Object> claims) {
        this.claims = claims;
    }

    /**
     * The actor user id, or null when the claim carries no usable "sub".
     */
    public String getSubject() {
        return getString(JsonWebToken.SUBJECT);
    }

    /**
     * The client the actor token was issued for, or null for admin delegation and impersonation.
     */
    public String getClientId() {
        return getString(OAuth2Constants.CLIENT_ID);
    }

    /**
     * Identifies the actor for auditing, preferring the client over the service account user behind it.
     */
    public String getIdentifier() {
        String clientId = getClientId();
        if (clientId != null) {
            return clientId;
        }
        String subject = getSubject();
        return subject != null ? subject : "unknown";
    }

    Object getNestedAct() {
        return claims.get(IDToken.ACT);
    }

    private String getString(String name) {
        Object value = claims.get(name);
        return value instanceof String ? (String) value : null;
    }
}
