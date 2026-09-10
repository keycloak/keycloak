package org.keycloak.protocol.oidc.refresh;

public class RefreshTokenException extends RuntimeException {

    private final String error;
    private final String errorDescription;

    public RefreshTokenException(String error, String errorDescription) {
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
