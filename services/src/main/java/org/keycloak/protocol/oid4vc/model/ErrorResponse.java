package org.keycloak.protocol.oid4vc.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private ErrorType error;

    public ErrorType getError() {
        return error;
    }

    public ErrorResponse setError(ErrorType error) {
        this.error = error;
        return this;
    }
}