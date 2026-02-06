package org.keycloak.protocol.ssf.receiver.verification;

import org.keycloak.protocol.ssf.SsfException;

public class SsfStreamVerificationException extends SsfException {

    public SsfStreamVerificationException() {
    }

    public SsfStreamVerificationException(String message) {
        super(message);
    }

    public SsfStreamVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
