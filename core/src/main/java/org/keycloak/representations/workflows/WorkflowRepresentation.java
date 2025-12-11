package org.keycloak.representations.workflows;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.keycloak.common.util.MultivaluedHashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CANCEL_IN_PROGRESS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONCURRENCY;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ENABLED;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_IF;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_NAME;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ON_EVENT;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_RESTART_IN_PROGRESS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_SCHEDULE;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_SCHEDULE_AFTER;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_SCHEDULE_BATCH_SIZE;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_STATE;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_STEPS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_USES;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_WITH;

@JsonPropertyOrder({"id", CONFIG_NAME, CONFIG_USES, CONFIG_ENABLED, CONFIG_ON_EVENT, CONFIG_SCHEDULE, CONFIG_CONCURRENCY, CONFIG_IF, CONFIG_STEPS, CONFIG_STATE})
@JsonIgnoreProperties(CONFIG_WITH)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class WorkflowRepresentation extends AbstractWorkflowComponentRepresentation {

    public static Builder withName(String name) {
        return new Builder().withName(name);
    }

    private List<WorkflowStepRepresentation> steps;

    private WorkflowStateRepresentation state;

    @JsonProperty(CONFIG_CONCURRENCY)
    private WorkflowConcurrencyRepresentation concurrency;

    @JsonProperty(CONFIG_SCHEDULE)
    private WorkflowScheduleRepresentation schedule;

    public WorkflowRepresentation() {
        super(null, null);
    }

    public WorkflowRepresentation(String id, String name, MultivaluedHashMap<String, String> config, List<WorkflowStepRepresentation> steps) {
        super(id, config);
        setName(name);
        this.steps = steps;
    }

    public String getOn() {
        return getConfigValue(CONFIG_ON_EVENT, String.class);
    }

    public void setOn(String eventConditions) {
        setConfigValue(CONFIG_ON_EVENT, eventConditions);
    }

    public WorkflowScheduleRepresentation getSchedule() {
        if (schedule == null) {
            String after = getConfigValue(CONFIG_SCHEDULE_AFTER, String.class);
            Integer batchSize = getConfigValue(CONFIG_SCHEDULE_BATCH_SIZE, Integer.class);

            if (after != null || batchSize != null) {
                this.schedule = new WorkflowScheduleRepresentation();
                this.schedule.setAfter(after);
                this.schedule.setBatchSize(batchSize);
            }
        }

        return this.schedule;
    }

    public void setSchedule(WorkflowScheduleRepresentation schedule) {
        this.schedule = schedule;
        if (schedule != null) {
            setConfigValue(CONFIG_SCHEDULE_AFTER, schedule.getAfter());
            setConfigValue(CONFIG_SCHEDULE_BATCH_SIZE, schedule.getBatchSize());
        }
    }

    public String getName() {
        return getConfigValue(CONFIG_NAME, String.class);
    }

    public void setName(String name) {
        setConfigValue(CONFIG_NAME, name);
    }

    public Boolean getEnabled() {
        return getConfigValue(CONFIG_ENABLED, Boolean.class);
    }

    public void setEnabled(Boolean enabled) {
        setConfigValue(CONFIG_ENABLED, enabled);
    }

    @JsonProperty(CONFIG_IF)
    public String getConditions() {
        return getConfigValue(CONFIG_CONDITIONS, String.class);
    }

    public void setConditions(String conditions) {
        setConfigValue(CONFIG_CONDITIONS, conditions);
    }

    public void setSteps(List<WorkflowStepRepresentation> steps) {
        this.steps = steps;
    }

    public List<WorkflowStepRepresentation> getSteps() {
        return steps;
    }

    public WorkflowStateRepresentation getState() {
        if (state == null) {
            state = new WorkflowStateRepresentation(this);
        }

        if (state.getErrors().isEmpty()) {
            return null;
        }

        return state;
    }

    public void setState(WorkflowStateRepresentation state) {
        this.state = state;
    }

    public WorkflowConcurrencyRepresentation getConcurrency() {
        String cancelInProgress = getConfigValue(CONFIG_CANCEL_IN_PROGRESS, String.class);
        String restartInProgress = getConfigValue(CONFIG_RESTART_IN_PROGRESS, String.class);
        if (this.concurrency == null) {
            if (cancelInProgress != null || restartInProgress != null) {
                this.concurrency = new WorkflowConcurrencyRepresentation();
                this.concurrency.setCancelInProgress(cancelInProgress);
                this.concurrency.setRestartInProgress(restartInProgress);
            }
        }
        return this.concurrency;
    }

    public void setConcurrency(WorkflowConcurrencyRepresentation concurrency) {
        this.concurrency = concurrency;
        if (concurrency != null) {
            setConfigValue(CONFIG_CANCEL_IN_PROGRESS, concurrency.getCancelInProgress());
            setConfigValue(CONFIG_RESTART_IN_PROGRESS, concurrency.getRestartInProgress());
        }
    }

    @JsonIgnore
    public String getCancelInProgress() {
        return concurrency != null ? concurrency.getCancelInProgress() : null;
    }

    @JsonIgnore
    public String getRestartInProgress() {
        return concurrency != null ? concurrency.getRestartInProgress() : null;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WorkflowRepresentation)) {
            return false;
        }
        WorkflowRepresentation that = (WorkflowRepresentation) obj;
        return Objects.equals(getConfig(), that.getConfig()) && Objects.equals(getSteps(), that.getSteps());
    }

    public static class Builder {

        private WorkflowRepresentation representation;

        private Builder() {
            this.representation = new WorkflowRepresentation();
        }

        public Builder onEvent(String operation) {
            representation.addConfigValue(CONFIG_ON_EVENT, operation);
            return this;
        }

        public Builder onEvent(String... operation) {
            return onEvent(String.join(" or ", operation).toUpperCase());
        }

        public Builder onCondition(String condition) {
            representation.setConditions(condition);
            return this;
        }

        public Builder concurrency() {
            if (representation.getConcurrency() == null) {
                representation.setConcurrency(new WorkflowConcurrencyRepresentation());
            }
            return this;
        }

        // move this to its own builder if we expand the capabilities of the concurrency settings.
        public Builder cancelInProgress(String cancelInProgress) {
            if (representation.getConcurrency() == null) {
                representation.setConcurrency(new WorkflowConcurrencyRepresentation());
            }
            representation.getConcurrency().setCancelInProgress(cancelInProgress);
            return this;
        }

        public Builder restartInProgress(String restartInProgress) {
            if (representation.getConcurrency() == null) {
                representation.setConcurrency(new WorkflowConcurrencyRepresentation());
            }
            representation.getConcurrency().setRestartInProgress(restartInProgress);
            return this;
        }

        public Builder withSteps(WorkflowStepRepresentation... steps) {
            representation.setSteps(Arrays.asList(steps));
            return this;
        }

        public Builder withConfig(String key, String value) {
            representation.addConfigValue(key, value);
            return this;
        }

        public Builder withConfig(String key, List<String> values) {
            representation.setConfigValue(key, values);
            return this;
        }

        public Builder withName(String name) {
            representation.setName(name);
            return this;
        }

        public Builder schedule(WorkflowScheduleRepresentation schedule) {
            representation.setSchedule(schedule);
            return this;
        }

        public WorkflowRepresentation build() {
            return representation;
        }
    }
}
