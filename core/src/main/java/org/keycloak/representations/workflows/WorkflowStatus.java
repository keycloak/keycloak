package org.keycloak.representations.workflows;

import java.util.List;

public class WorkflowStatus {

    private List<String> errors;

    public WorkflowStatus() {}

    public WorkflowStatus(List<String> errors) {
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
