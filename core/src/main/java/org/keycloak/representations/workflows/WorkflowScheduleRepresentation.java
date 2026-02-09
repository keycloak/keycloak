package org.keycloak.representations.workflows;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_AFTER;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_BATCH_SIZE;

@JsonPropertyOrder({CONFIG_AFTER, CONFIG_BATCH_SIZE})
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkflowScheduleRepresentation {

    private String after;

    @JsonProperty(CONFIG_BATCH_SIZE)
    private Integer batchSize;

    public static Builder create() {
        return new Builder();
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public Integer getBatchSize() {
        return this.batchSize;
    }

    public void setBatchSize(Integer batchSize) {
        this.batchSize = batchSize;
    }

    public static class Builder {

        private final WorkflowScheduleRepresentation schedule = new WorkflowScheduleRepresentation();

        public Builder after(String after) {
            schedule.setAfter(after);
            return this;
        }

        public Builder batchSize(int batchSize) {
            schedule.setBatchSize(batchSize);
            return this;
        }

        public WorkflowScheduleRepresentation build() {
            return schedule;
        }
    }
}
