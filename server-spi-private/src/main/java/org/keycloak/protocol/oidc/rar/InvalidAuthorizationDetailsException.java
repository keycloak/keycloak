package org.keycloak.protocol.oidc.rar;

public class InvalidAuthorizationDetailsException extends RuntimeException {

    public InvalidAuthorizationDetailsException(String message) {
        super(message);
    }
}
