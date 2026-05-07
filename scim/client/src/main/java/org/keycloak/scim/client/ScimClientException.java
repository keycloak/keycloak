package org.keycloak.scim.client;

import org.keycloak.scim.protocol.response.ErrorResponse;

public class ScimClientException extends RuntimeException {

    private final String response;
    private ErrorResponse error;

    public ScimClientException(String message, Throwable cause, String response) {
        super(message, cause);
        this.response = response;
    }

    public ScimClientException(String message, Throwable cause) {
        this(message, cause, null);
    }

    public ScimClientException(String message, String response) {
        this(message, null, response);
    }

    public <T> ScimClientException(String message, ErrorResponse error) {
        this(message, null, null);
        this.error = error;
    }

    public String getResponse() {
        return response;
    }

    public ErrorResponse getError() {
        return error;
    }
}
