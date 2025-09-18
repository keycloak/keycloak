package org.keycloak.representations.workflows;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

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
