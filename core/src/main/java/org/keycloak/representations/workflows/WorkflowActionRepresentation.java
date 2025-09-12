package org.keycloak.representations.workflows;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.keycloak.common.util.MultivaluedHashMap;

public class WorkflowActionRepresentation {

    private static final String AFTER_KEY = "after";

    public static Builder create() {
        return new Builder();
    }

    private String id;
    private String providerId;
    private MultivaluedHashMap<String, String> config;
    private List<WorkflowActionRepresentation> actions;

    public WorkflowActionRepresentation() {
        // reflection
    }

    public WorkflowActionRepresentation(String providerId) {
        this(providerId, null);
    }

    public WorkflowActionRepresentation(String providerId, MultivaluedHashMap<String, String> config) {
        this(null, providerId, config, null);
    }

    public WorkflowActionRepresentation(String id, String providerId, MultivaluedHashMap<String, String> config, List<WorkflowActionRepresentation> actions) {
        this.id = id;
        this.providerId = providerId;
        this.config = config;
        this.actions = actions;
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

    public List<WorkflowActionRepresentation> getActions() {
        return actions;
    }

    public void setActions(List<WorkflowActionRepresentation> actions) {
        this.actions = actions;
    }

    public static class Builder {

        private WorkflowActionRepresentation action;

        public Builder of(String providerId) {
            this.action = new WorkflowActionRepresentation(providerId);
            return this;
        }

        public Builder after(Duration duration) {
            action.setAfter(duration.toMillis());
            return this;
        }

        public Builder before(WorkflowActionRepresentation targetAction, Duration timeBeforeTarget) {
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

        public Builder withActions(WorkflowActionRepresentation... actions) {
            action.setActions(Arrays.asList(actions));
            return this;
        }

        public Builder withConfig(String key, List<String> values) {
            action.setConfig(key, values);
            return this;
        }

        public WorkflowActionRepresentation build() {
            return action;
        }
    }
}
