package org.keycloak.scim.protocol;

/**
 * Thrown to indicate that access to a protected resource is forbidden.
 */
public class ForbiddenException extends RuntimeException{

    public ForbiddenException() {
    }

    /**
     * @param message the detail returned to the caller in the error response
     */
    public ForbiddenException(String message) {
        super(message);
    }
}
