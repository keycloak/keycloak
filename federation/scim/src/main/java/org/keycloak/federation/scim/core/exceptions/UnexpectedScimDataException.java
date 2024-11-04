package org.keycloak.federation.scim.core.exceptions;

public class UnexpectedScimDataException extends ScimPropagationException {
    public UnexpectedScimDataException(String message) {
        super(message);
    }
}
