package org.keycloak.representations.workflows;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.keycloak.common.util.MultivaluedHashMap;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CANCEL_IF_RUNNING;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONCURRENCY;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_CONDITIONS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ENABLED;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_IF;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_NAME;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ON_EVENT;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_STATE;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_STEPS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_USES;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_WITH;

@JsonPropertyOrder({"id", CONFIG_NAME, CONFIG_USES, CONFIG_ENABLED, CONFIG_ON_EVENT, CONFIG_CONCURRENCY, CONFIG_IF, CONFIG_STEPS, CONFIG_STATE})
@JsonIgnoreProperties(CONFIG_WITH)
public final class WorkflowRepresentation extends AbstractWorkflowComponentRepresentation {

    public static Builder withName(String name) {
        return new Builder().withName(name);
    }

    private List<WorkflowStepRepresentation> steps;

    private WorkflowStateRepresentation state;

    @JsonProperty(CONFIG_CONCURRENCY)
    private WorkflowConcurrencyRepresentation concurrency;

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
        if (this.concurrency == null) {
            Boolean cancelIfRunning = getConfigValue(CONFIG_CANCEL_IF_RUNNING, Boolean.class);
            if (cancelIfRunning != null) {
                this.concurrency = new WorkflowConcurrencyRepresentation();
                this.concurrency.setCancelIfRunning(cancelIfRunning);
            }
        }
        return this.concurrency;
    }

    public void setConcurrency(WorkflowConcurrencyRepresentation concurrency) {
        this.concurrency = concurrency;
        if (concurrency != null) {
            setConfigValue(CONFIG_CANCEL_IF_RUNNING, concurrency.isCancelIfRunning());
        }
    }

    @JsonIgnore
    public boolean isCancelIfRunning() {
        return concurrency != null && Boolean.TRUE.equals(concurrency.isCancelIfRunning());
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
        // TODO: include state in comparison?
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
            representation.setConcurrency(new WorkflowConcurrencyRepresentation());
            return this;
        }

        // move this to its own builder if we expand the capabilities of the concurrency settings.
        public Builder cancelIfRunning() {
            if (representation.getConcurrency() == null) {
                representation.setConcurrency(new WorkflowConcurrencyRepresentation());
            }
            representation.getConcurrency().setCancelIfRunning(true);
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

        public WorkflowRepresentation build() {
            return representation;
        }
    }
}
