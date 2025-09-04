package org.keycloak.representations.resources.policies;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResourcePolicyActionRepresentation {

    private static final String AFTER_KEY = "after";

    public static Builder create() {
        return new Builder();
    }

    private String id;
    private String providerId;
    private Map<String, List<String>> config;

    public ResourcePolicyActionRepresentation() {
        // reflection
    }

    public ResourcePolicyActionRepresentation(String providerId) {
        this(providerId, null);
    }

    public ResourcePolicyActionRepresentation(String providerId, Map<String, List<String>> config) {
        this(null, providerId, config);
    }

    public ResourcePolicyActionRepresentation(String id, String providerId, Map<String, List<String>> config) {
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

    private void setAfter(long ms) {
        setConfig(AFTER_KEY, String.valueOf(ms));
    }

    public static class Builder {

        private ResourcePolicyActionRepresentation action;

        public Builder of(String providerId) {
            this.action = new ResourcePolicyActionRepresentation(providerId);
            return this;
        }

        public Builder after(Duration duration) {
            action.setAfter(duration.toMillis());
            return this;
        }

        public Builder before(ResourcePolicyActionRepresentation targetAction, Duration timeBeforeTarget) {
            // Calculate absolute time: targetAction.after - timeBeforeTarget
            String targetAfter = targetAction.getConfig().get(AFTER_KEY).get(0);
            long targetTime = Long.parseLong(targetAfter);
            long thisTime = targetTime - timeBeforeTarget.toMillis();
            action.setAfter(thisTime);
            return this;
        }

        public Builder withConfig(String key, String value) {
            action.setConfig(key, value);
            return this;
        }

        public ResourcePolicyActionRepresentation build() {
            return action;
        }
    }
}
