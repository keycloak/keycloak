package org.keycloak.adapters.saml;

import org.keycloak.adapters.spi.AuthenticationError;
import org.keycloak.dom.saml.v2.protocol.StatusResponseType;
import org.keycloak.dom.saml.v2.protocol.StatusType;

/**
 * Object that describes the SAML error that happened.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class SamlAuthenticationError implements AuthenticationError {
    public static enum Reason {
        EXTRACTION_FAILURE,
        INVALID_SIGNATURE,
        ERROR_STATUS
    }

    private Reason reason;

    private StatusResponseType status;

    public SamlAuthenticationError(Reason reason) {
        this.reason = reason;
    }

    public SamlAuthenticationError(Reason reason, StatusResponseType status) {
        this.reason = reason;
        this.status = status;
    }

    public SamlAuthenticationError(StatusResponseType statusType) {
        this.status = statusType;
    }

    public Reason getReason() {
        return reason;
    }
    public StatusResponseType getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "SamlAuthenticationError [reason=" + reason + ", status=" + status + "]";
    }
    
}
