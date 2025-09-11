package org.keycloak.models.policy;

import org.keycloak.models.ModelValidationException;

public class ResourcePolicyInvalidStateException extends ModelValidationException {

    public ResourcePolicyInvalidStateException(String message) {
        super(message);
    }
}
