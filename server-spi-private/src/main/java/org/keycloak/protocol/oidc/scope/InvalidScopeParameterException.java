package org.keycloak.protocol.oidc.scope;

public class InvalidScopeParameterException extends RuntimeException {

    public InvalidScopeParameterException(String message) {
        super(message);
    }

    public InvalidScopeParameterException(String message, Throwable cause) {
        super(message, cause);
    }
}
