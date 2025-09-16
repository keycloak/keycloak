package org.keycloak.representations.workflows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.keycloak.common.util.MultivaluedHashMap;

public class WorkflowRepresentation {

    public static Builder create() {
        return new Builder();
    }

    private String id;
    private String providerId;
    private MultivaluedHashMap<String, String> config;
    private List<WorkflowStepRepresentation> steps;
    private List<WorkflowConditionRepresentation> conditions;

    public WorkflowRepresentation() {
        // reflection
    }

    public WorkflowRepresentation(String providerId) {
        this(providerId, null);
    }

    public WorkflowRepresentation(String providerId, Map<String, List<String>> config) {
        this(null, providerId, config);
    }

    public WorkflowRepresentation(String id, String providerId, Map<String, List<String>> config) {
        this.id = id;
        this.providerId = providerId;
        this.config = new MultivaluedHashMap<>(config);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getName() {
        return Optional.ofNullable(config).orElse(new MultivaluedHashMap<>()).getFirst("name");
    }

    public void setName(String name) {
        if (this.config == null) {
            this.config = new MultivaluedHashMap<>();
        }
        this.config.putSingle("name", name);
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

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public void addStep(WorkflowStepRepresentation step) {
        if (steps == null) {
            steps = new ArrayList<>();
        }
        steps.add(step);
    }

    public static class Builder {
        private String providerId;
        private final Map<String, List<String>> config = new HashMap<>();
        private List<WorkflowConditionRepresentation> conditions = new ArrayList<>();
        private final Map<String, List<WorkflowStepRepresentation>> steps = new HashMap<>();
        private List<Builder> builders = new ArrayList<>();

        private Builder() {
        }

        private Builder(String providerId, List<Builder> builders) {
            this.providerId = providerId;
            this.builders = builders;
        }

        public Builder of(String providerId) {
            Builder builder = new Builder(providerId, builders);
            builders.add(builder);
            return builder;
        }

        public Builder onEvent(String operation) {
            List<String> events = config.computeIfAbsent("events", k -> new ArrayList<>());

            events.add(operation);

            return this;
        }

        public Builder onConditions(WorkflowConditionRepresentation... condition) {
            if (conditions == null) {
                conditions = new ArrayList<>();
            }
            conditions.addAll(Arrays.asList(condition));
            return this;
        }

        public Builder withSteps(WorkflowStepRepresentation... steps) {
            this.steps.computeIfAbsent(providerId, (k) -> new ArrayList<>()).addAll(Arrays.asList(steps));
            return this;
        }

        public Builder withConfig(String key, String value) {
            config.put(key, Collections.singletonList(value));
            return this;
        }

        public Builder withConfig(String key, List<String> value) {
            config.put(key, value);
            return this;
        }

        public Builder name(String name) {
            return withConfig("name", name);
        }

        public Builder immediate() {
            return withConfig("scheduled", "false");
        }

        public Builder recurring() {
            return withConfig("recurring", "true");
        }

        public List<WorkflowRepresentation> build() {
            List<WorkflowRepresentation> workflows = new ArrayList<>();

            for (Builder builder : builders) {
                for (Entry<String, List<WorkflowStepRepresentation>> entry : builder.steps.entrySet()) {
                    WorkflowRepresentation workflow = new WorkflowRepresentation(entry.getKey(), builder.config);

                    workflow.setSteps(entry.getValue());
                    workflow.setConditions(builder.conditions);

                    workflows.add(workflow);
                }
            }

            return workflows;
        }
    }
}
