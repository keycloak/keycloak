package org.keycloak.adapters.exception;

import org.keycloak.adapters.RefreshTokenError;

public class RefreshTokenException extends RuntimeException {

    private final RefreshTokenError error;


    public RefreshTokenException(RefreshTokenError error) {
        this.error = error;
    }

    public RefreshTokenError getError() {
        return error;
    }
}
