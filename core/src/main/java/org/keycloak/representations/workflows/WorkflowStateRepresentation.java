package org.keycloak.representations.workflows;

import static java.util.Optional.ofNullable;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ERROR;

import java.util.Collections;
import java.util.List;

public class WorkflowStateRepresentation {

    private List<String> errors = Collections.emptyList();

    public WorkflowStateRepresentation() {}

    public WorkflowStateRepresentation(WorkflowRepresentation workflow) {
        this.errors = ofNullable(workflow.getConfigValues(CONFIG_ERROR)).orElse(Collections.emptyList());
    }

    public List<String> getErrors() {
        return errors;
    }
}
