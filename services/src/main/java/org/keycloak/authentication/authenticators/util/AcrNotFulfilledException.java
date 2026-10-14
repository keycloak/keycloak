package org.keycloak.authentication.authenticators.util;

import org.keycloak.authentication.AuthenticationFlowError;
import org.keycloak.authentication.AuthenticationFlowException;
import org.keycloak.services.messages.Messages;

/**
 * Thrown when an authentication finishes without reaching a forced level of authentication.
 */
public class AcrNotFulfilledException extends AuthenticationFlowException {

    public AcrNotFulfilledException(int requestedLevel, int fulfilledLevel) {
        super(AuthenticationFlowError.GENERIC_AUTHENTICATION_ERROR,
                String.format("Forced level of authentication did not meet the requirements. Requested level: %d, Fulfilled level: %d", requestedLevel, fulfilledLevel),
                Messages.ACR_NOT_FULFILLED);
    }
}
