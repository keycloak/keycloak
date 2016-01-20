package org.keycloak.adapters;

import org.keycloak.adapters.spi.AuthenticationError;

/**
 * Object that describes the OIDC error that happened.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class OIDCAuthenticationError implements AuthenticationError {
    public static enum Reason {
        NO_BEARER_TOKEN,
        NO_REDIRECT_URI,
        INVALID_STATE_COOKIE,
        OAUTH_ERROR,
        SSL_REQUIRED,
        CODE_TO_TOKEN_FAILURE,
        INVALID_TOKEN,
        STALE_TOKEN,
        NO_AUTHORIZATION_HEADER
    }

    private Reason reason;
    private String description;

    public OIDCAuthenticationError(Reason reason, String description) {
        this.reason = reason;
        this.description = description;
    }

    public Reason getReason() {
        return reason;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "OIDCAuthenticationError [reason=" + reason + ", description=" + description + "]";
    }
    
    
}
