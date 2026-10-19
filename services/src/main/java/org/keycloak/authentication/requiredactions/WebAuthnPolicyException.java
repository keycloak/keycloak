package org.keycloak.authentication.requiredactions;

import com.webauthn4j.util.exception.WebAuthnException;

/**
 * Signals that a WebAuthn registration or authentication response violates the realm's WebAuthn policy.
 * Carries a localizable message key, allowing the error to be displayed using a specific, translatable string
 * rather than the raw exception text.
 */
public class WebAuthnPolicyException extends WebAuthnException {

    private final String messageKey;
    private final Object[] parameters;

    public WebAuthnPolicyException(String messageKey, String technicalDetail, Object... parameters) {
        super(technicalDetail);
        this.messageKey = messageKey;
        this.parameters = parameters;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public Object[] getParameters() {
        return parameters;
    }
}
