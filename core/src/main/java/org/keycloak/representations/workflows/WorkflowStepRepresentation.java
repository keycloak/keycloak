package org.keycloak.representations.workflows;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.common.util.MultivaluedHashMap;

public class WorkflowStepRepresentation {

    private static final String AFTER_KEY = "after";

    public static Builder create() {
        return new Builder();
    }

    private String id;
    private String providerId;
    private MultivaluedHashMap<String, String> config;
    private List<WorkflowStepRepresentation> steps;

    public WorkflowStepRepresentation() {
        // reflection
    }

    public WorkflowStepRepresentation(String providerId) {
        this(providerId, null);
    }

    public WorkflowStepRepresentation(String providerId, MultivaluedHashMap<String, String> config) {
        this(null, providerId, config, null);
    }

    public WorkflowStepRepresentation(String id, String providerId, MultivaluedHashMap<String, String> config, List<WorkflowStepRepresentation> steps) {
        this.id = id;
        this.providerId = providerId;
        this.config = config;
        this.steps = steps;
    }

    public String getId() {
        return id;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public void setConfig(MultivaluedHashMap<String, String> config) {
        this.config = config;
    }

    public void setConfig(String key, String value) {
        setConfig(key, Collections.singletonList(value));
    }

    public void setConfig(String key, List<String> values) {
        if (this.config == null) {
            this.config = new MultivaluedHashMap<>();
        }
        this.config.put(key, values);
    }

    private void setAfter(long ms) {
        setConfig(AFTER_KEY, String.valueOf(ms));
    }

    public List<WorkflowStepRepresentation> getSteps() {
        return steps;
    }

    public void setSteps(List<WorkflowStepRepresentation> steps) {
        this.steps = steps;
    }

    public static class Builder {

        private WorkflowStepRepresentation step;

        public Builder of(String providerId) {
            this.step = new WorkflowStepRepresentation(providerId);
            return this;
        }

        public Builder after(Duration duration) {
            step.setAfter(duration.toMillis());
            return this;
        }

        public Builder before(WorkflowStepRepresentation targetStep, Duration timeBeforeTarget) {
            // Calculate absolute time: targetStep.after - timeBeforeTarget
            String targetAfter = targetStep.getConfig().get(AFTER_KEY).get(0);
            long targetTime = Long.parseLong(targetAfter);
            long thisTime = targetTime - timeBeforeTarget.toMillis();
            step.setAfter(thisTime);
            return this;
        }

        public Builder withConfig(String key, String value) {
            step.setConfig(key, value);
            return this;
        }

        public Builder withSteps(WorkflowStepRepresentation... steps) {
            step.setSteps(Arrays.asList(steps));
            return this;
        }

        public Builder withConfig(String key, List<String> values) {
            step.setConfig(key, values);
            return this;
        }

        public WorkflowStepRepresentation build() {
            return step;
        }
    }
}
