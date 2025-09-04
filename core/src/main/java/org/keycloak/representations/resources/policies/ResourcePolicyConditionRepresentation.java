package org.keycloak.representations.resources.policies;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourcePolicyConditionRepresentation {

    public static Builder create() {
        return new Builder();
    }

    private String id;
    private String providerId;
    private Map<String, List<String>> config;

    public ResourcePolicyConditionRepresentation() {
        // reflection
    }

    public ResourcePolicyConditionRepresentation(String providerId) {
        this(providerId, null);
    }

    public ResourcePolicyConditionRepresentation(String providerId, Map<String, List<String>> config) {
        this(null, providerId, config);
    }

    public ResourcePolicyConditionRepresentation(String id, String providerId, Map<String, List<String>> config) {
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

    public static class Builder {

        private ResourcePolicyConditionRepresentation action;

        public Builder of(String providerId) {
            this.action = new ResourcePolicyConditionRepresentation(providerId);
            return this;
        }

        public Builder withConfig(String key, String value) {
            action.setConfig(key, value);
            return this;
        }

        public ResourcePolicyConditionRepresentation build() {
            return action;
        }
    }
}
