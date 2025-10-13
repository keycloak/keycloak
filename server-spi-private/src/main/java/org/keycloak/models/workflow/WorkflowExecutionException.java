package org.keycloak.models.workflow;

import org.keycloak.models.ModelException;

public class WorkflowExecutionException extends ModelException {

    public WorkflowExecutionException(String message) {
        super(message);
    }
}
