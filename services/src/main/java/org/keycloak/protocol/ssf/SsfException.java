package org.keycloak.protocol.ssf;

public class SsfException extends RuntimeException {

    public SsfException() {
    }

    public SsfException(String message) {
        super(message);
    }

    public SsfException(String message, Throwable cause) {
        super(message, cause);
    }
}
