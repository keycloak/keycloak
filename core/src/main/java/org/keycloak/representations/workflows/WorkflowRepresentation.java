package org.keycloak.representations.workflows;

import static java.util.Optional.ofNullable;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ENABLED;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_IF;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_NAME;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_ON_EVENT;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_RECURRING;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_RESET_ON;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_STATE;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_STEPS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_WITH;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_USES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import org.keycloak.common.util.MultivaluedHashMap;

@JsonPropertyOrder({"id", CONFIG_NAME, CONFIG_USES, CONFIG_ENABLED, CONFIG_ON_EVENT, CONFIG_RESET_ON, CONFIG_RECURRING, CONFIG_IF, CONFIG_STEPS, CONFIG_STATE})
@JsonIgnoreProperties(CONFIG_WITH)
public final class WorkflowRepresentation extends AbstractWorkflowComponentRepresentation {

    public static Builder create() {
        return new Builder().of(WorkflowConstants.DEFAULT_WORKFLOW);
    }

    private List<WorkflowStepRepresentation> steps;

    @JsonProperty(CONFIG_IF)
    private List<WorkflowConditionRepresentation> conditions;

    private WorkflowStateRepresentation state;

    public WorkflowRepresentation() {
        super(null, null, null);
    }

    public WorkflowRepresentation(String id, String workflow, MultivaluedHashMap<String, String> config, List<WorkflowConditionRepresentation> conditions, List<WorkflowStepRepresentation> steps) {
        super(id, workflow, config);
        this.conditions = conditions;
        this.steps = steps;
    }

    public <T> T getOn() {
        return getConfigValuesOrSingle(CONFIG_ON_EVENT);
    }

    public void setOn(String... events) {
        setConfigValue(CONFIG_ON_EVENT, Arrays.asList(events));
    }

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public void setOn(List<String> events) {
        setConfigValue(CONFIG_ON_EVENT, events);
    }

    @JsonIgnore
    public List<String> getOnValues() {
        return ofNullable(getConfigValues(CONFIG_ON_EVENT)).orElse(Collections.emptyList());
    }

    @JsonProperty(CONFIG_RESET_ON)
    public <T> T getOnEventReset() {
        return getConfigValuesOrSingle(CONFIG_RESET_ON);
    }

    @JsonIgnore
    public List<String> getOnEventsReset() {
        return ofNullable(getConfigValues(CONFIG_RESET_ON)).orElse(Collections.emptyList());
    }

    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    public void setOnEventReset(List<String> names) {
        setConfigValue(CONFIG_RESET_ON, names);
    }

    @JsonIgnore
    public void setOnEventReset(String... names) {
        setOnEventReset(Arrays.asList(names));
    }

    public String getName() {
        return getConfigValue(CONFIG_NAME, String.class);
    }

    public void setName(String name) {
        setConfigValue(CONFIG_NAME, name);
    }

    public Boolean getRecurring() {
        return getConfigValue(CONFIG_RECURRING, Boolean.class);
    }

    public void setRecurring(Boolean recurring) {
        setConfigValue(CONFIG_RECURRING, recurring);
    }

    public Boolean getEnabled() {
        return getConfigValue(CONFIG_ENABLED, Boolean.class);
    }

    public void setEnabled(Boolean enabled) {
        setConfigValue(CONFIG_ENABLED, enabled);
    }

    public void setConditions(List<WorkflowConditionRepresentation> conditions) {
        this.conditions = conditions;
    }

    public List<WorkflowConditionRepresentation> getConditions() {
        return conditions;
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
        return Objects.equals(getUses(), that.getUses()) && Objects.equals(getConfig(), that.getConfig())
            && Objects.equals(getConditions(), that.getConditions()) && Objects.equals(getSteps(), that.getSteps());
    }

    public static class Builder {

        private final Map<WorkflowRepresentation, List<WorkflowStepRepresentation>> steps = new HashMap<>();
        private List<Builder> builders = new ArrayList<>();
        private WorkflowRepresentation representation;

        private Builder() {
        }

        private Builder(WorkflowRepresentation representation, List<Builder> builders) {
            this.representation = representation;
            this.builders = builders;
        }

        public Builder of(String providerId) {
            WorkflowRepresentation representation = new WorkflowRepresentation();
            representation.setUses(providerId);
            Builder builder = new Builder(representation, builders);
            builders.add(builder);
            return builder;
        }

        public Builder onEvent(String operation) {
            representation.addConfigValue(CONFIG_ON_EVENT, operation);
            return this;
        }

        public Builder onConditions(WorkflowConditionRepresentation... condition) {
            representation.setConditions(Arrays.asList(condition));
            return this;
        }

        public Builder withSteps(WorkflowStepRepresentation... steps) {
            this.steps.computeIfAbsent(representation, (k) -> new ArrayList<>()).addAll(Arrays.asList(steps));
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

        public Builder name(String name) {
            representation.setName(name);
            return this;
        }

        public Builder recurring() {
            representation.setRecurring(true);
            return this;
        }

        public WorkflowSetRepresentation build() {
            List<WorkflowRepresentation> workflows = new ArrayList<>();

            for (Builder builder : builders) {
                if (builder.steps.isEmpty()) {
                    continue;
                }
                for (Entry<WorkflowRepresentation, List<WorkflowStepRepresentation>> entry : builder.steps.entrySet()) {
                    WorkflowRepresentation workflow = entry.getKey();

                    workflow.setSteps(entry.getValue());

                    workflows.add(workflow);
                }
            }

            return new WorkflowSetRepresentation(workflows);
        }
    }
}
