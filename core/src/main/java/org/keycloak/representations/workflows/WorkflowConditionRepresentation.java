package org.keycloak.representations.workflows;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkflowConditionRepresentation {

    public static Builder create() {
        return new Builder();
    }

    private String id;
    private String providerId;
    private Map<String, List<String>> config;

    public WorkflowConditionRepresentation() {
        // reflection
    }

    public WorkflowConditionRepresentation(String providerId) {
        this(providerId, null);
    }

    public WorkflowConditionRepresentation(String providerId, Map<String, List<String>> config) {
        this(null, providerId, config);
    }

    public WorkflowConditionRepresentation(String id, String providerId, Map<String, List<String>> config) {
        this.id = id;
        this.providerId = providerId;
        this.config = config;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public Map<String, List<String>> getConfig() {
        return config;
    }

    public void setConfig(Map<String, List<String>> config) {
        this.config = config;
    }

    public void setConfig(String key, String value) {
        if (this.config == null) {
            this.config = new HashMap<>();
        }
        this.config.put(key, Collections.singletonList(value));
    }

    public void setConfig(String key, List<String> values) {
        if (this.config == null) {
            this.config = new HashMap<>();
        }
        this.config.put(key, values);
    }

    public static class Builder {

        private WorkflowConditionRepresentation action;

        public Builder of(String providerId) {
            this.action = new WorkflowConditionRepresentation(providerId);
            return this;
        }

        public Builder withConfig(String key, String value) {
            action.setConfig(key, value);
            return this;
        }

        public Builder withConfig(String key, List<String> value) {
            action.setConfig(key, value);
            return this;
        }

        public Builder withConfig(Map<String, List<String>> config) {
            action.setConfig(config);
            return this;
        }

        public WorkflowConditionRepresentation build() {
            return action;
        }
    }
}
