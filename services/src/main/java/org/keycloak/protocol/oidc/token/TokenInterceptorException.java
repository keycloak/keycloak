package org.keycloak.protocol.oidc.token;

public class TokenInterceptorException extends RuntimeException {

    private String error;
    private String description;

    public TokenInterceptorException(String error, String description) {
        this.error = error;
        this.description = description;
    }

    public String getError() {
        return error;
    }

    public String getDescription() {
        return description;
    }
}
