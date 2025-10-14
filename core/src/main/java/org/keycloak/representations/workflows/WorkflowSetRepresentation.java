package org.keycloak.representations.workflows;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.util.List;

public final class WorkflowSetRepresentation {

    @JsonUnwrapped
    private List<WorkflowRepresentation> workflows;

    public WorkflowSetRepresentation() {

    }

    public WorkflowSetRepresentation(List<WorkflowRepresentation> workflows) {
        this.workflows = workflows;
    }

    public void setWorkflows(List<WorkflowRepresentation> workflows) {
        this.workflows = workflows;
    }

    public List<WorkflowRepresentation> getWorkflows() {
        return workflows;
    }
}
