package org.keycloak.federation.scim.core.exceptions;

public abstract class ScimPropagationException extends Exception {

    protected ScimPropagationException(String message) {
        super(message);
    }

    protected ScimPropagationException(String message, Exception e) {
        super(message, e);
    }
}
