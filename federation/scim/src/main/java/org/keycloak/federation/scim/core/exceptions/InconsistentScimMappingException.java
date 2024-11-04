package org.keycloak.federation.scim.core.exceptions;

public class InconsistentScimMappingException extends ScimPropagationException {
    public InconsistentScimMappingException(String message) {
        super(message);
    }
}
