package org.keycloak.protocol.oidc.endpoints;

import jakarta.ws.rs.core.Response;

// Exception propagated to the caller, which will allow caller to send proper error response based on the context (Browser OIDC Authorization Endpoint, PAR etc)
public class AuthorizationCheckException extends Exception {

    private final Response.Status status;
    private final String error;
    private final String errorDescription;

    public AuthorizationCheckException(Response.Status status, String error, String errorDescription) {
        this.status = status;
        this.error = error;
        this.errorDescription = errorDescription;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
