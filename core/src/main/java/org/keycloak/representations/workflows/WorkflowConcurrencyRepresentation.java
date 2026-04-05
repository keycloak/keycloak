package org.keycloak.representations.workflows;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CANCEL_IN_PROGRESS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_RESTART_IN_PROGRESS;

@JsonPropertyOrder({CONFIG_CANCEL_IN_PROGRESS, CONFIG_RESTART_IN_PROGRESS})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowConcurrencyRepresentation {

    @JsonProperty(CONFIG_CANCEL_IN_PROGRESS)
    private String cancelInProgress;

    @JsonProperty(CONFIG_RESTART_IN_PROGRESS)
    private String restartInProgress;

    // A no-argument constructor is needed for Jackson deserialization
    public WorkflowConcurrencyRepresentation() {}

    public WorkflowConcurrencyRepresentation(String restartInProgress, String cancelInProgress) {
        this.restartInProgress = restartInProgress;
        this.cancelInProgress = cancelInProgress;
    }

    public String getCancelInProgress() {
        return cancelInProgress;
    }

    public void setCancelInProgress(String cancelInProgress) {
        this.cancelInProgress = cancelInProgress;
    }

    public String getRestartInProgress() {
        return restartInProgress;
    }

    public void setRestartInProgress(String restartInProgress) {
        this.restartInProgress = restartInProgress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(cancelInProgress, restartInProgress);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        WorkflowConcurrencyRepresentation that = (WorkflowConcurrencyRepresentation) obj;
        return Objects.equals(cancelInProgress, that.cancelInProgress) &&
               Objects.equals(restartInProgress, that.restartInProgress);
    }

}
