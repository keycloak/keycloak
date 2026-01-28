package org.keycloak.models.workflow;

import org.keycloak.models.ModelValidationException;

public class WorkflowInvalidStateException extends ModelValidationException {

    public WorkflowInvalidStateException(String message) {
        super(message);
    }
}
