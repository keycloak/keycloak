package org.keycloak.protocol.oidc.endpoints;

import java.text.MessageFormat;

import jakarta.ws.rs.core.Response;

// Exception propagated to the caller, which will allow caller to send proper error response based on the context (Browser OIDC Authorization Endpoint, PAR etc)
public class AuthorizationCheckException extends Exception {

    private final Response.Status status;
    private final String error;
    private final String errorMessage;
    private final Object[] parameters;

    public AuthorizationCheckException(Response.Status status, String error, String errorDescription) {
        this(error, status, errorDescription);
    }

    public AuthorizationCheckException(String error, Response.Status status, String errorMessage, Object... params) {
        this.status = status;
        this.error = error;
        this.errorMessage = errorMessage;
        this.parameters = params;
    }

    public Response.Status getStatus() {
        return status;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return MessageFormat.format(errorMessage, parameters);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Object[] getParameters() {
        return parameters;
    }
}
