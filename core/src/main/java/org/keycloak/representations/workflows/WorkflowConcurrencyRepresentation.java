package org.keycloak.representations.workflows;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CANCEL_IF_RUNNING;

public class WorkflowConcurrencyRepresentation {

    @JsonProperty(CONFIG_CANCEL_IF_RUNNING)
    private Boolean cancelIfRunning;

    // A no-argument constructor is needed for Jackson deserialization
    public WorkflowConcurrencyRepresentation() {}

    public WorkflowConcurrencyRepresentation(Boolean cancelIfRunning) {
        this.cancelIfRunning = cancelIfRunning;
    }

    public Boolean isCancelIfRunning() {
        return cancelIfRunning;
    }

    public void setCancelIfRunning(Boolean cancelIfRunning) {
        this.cancelIfRunning = cancelIfRunning;
    }

    @Override
    public int hashCode() {
        return cancelIfRunning != null ? cancelIfRunning.hashCode() : 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WorkflowConcurrencyRepresentation that = (WorkflowConcurrencyRepresentation) obj;
        return Objects.equals(cancelIfRunning, that.cancelIfRunning);
    }

}
