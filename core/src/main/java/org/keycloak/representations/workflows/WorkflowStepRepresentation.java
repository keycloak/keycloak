package org.keycloak.representations.workflows;

import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_AFTER;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_PRIORITY;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_STEPS;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_USES;
import static org.keycloak.representations.workflows.WorkflowConstants.CONFIG_WITH;

import java.time.Duration;
import java.util.Arrays;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.keycloak.common.util.MultivaluedHashMap;

@JsonPropertyOrder({"id", CONFIG_USES, CONFIG_AFTER, CONFIG_PRIORITY, CONFIG_WITH})
public final class WorkflowStepRepresentation extends AbstractWorkflowComponentRepresentation {

    public static Builder create() {
        return new Builder();
    }

    public WorkflowStepRepresentation() {
        this(null, null, null);
    }

    public WorkflowStepRepresentation(String uses) {
        this(null, uses, null);
    }

    public WorkflowStepRepresentation(String id, String uses, MultivaluedHashMap<String, String> config) {
        super(id, uses, config);
    }

    @JsonSerialize(using = MultivaluedHashMapValueSerializer.class)
    @JsonDeserialize(using = MultivaluedHashMapValueDeserializer.class)
    public MultivaluedHashMap<String, String> getConfig() {
        return super.getConfig();
    }

    public String getAfter() {
        return getConfigValue(CONFIG_AFTER, String.class);
    }

    public void setAfter(long ms) {
        setConfig(CONFIG_AFTER, String.valueOf(ms));
    }

    public String getPriority() {
        return getConfigValue(CONFIG_PRIORITY, String.class);
    }

    public void setPriority(long ms) {
        setConfig(CONFIG_PRIORITY, String.valueOf(ms));
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof WorkflowStepRepresentation)) {
            return false;
        }
        WorkflowStepRepresentation that = (WorkflowStepRepresentation) obj;
        return Objects.equals(getUses(), that.getUses()) && Objects.equals(getConfig(), that.getConfig());
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

        public Builder id(String id) {
            step.setId(id);
            return this;
        }

        public Builder before(WorkflowStepRepresentation targetStep, Duration timeBeforeTarget) {
            // Calculate absolute time: targetStep.after - timeBeforeTarget
            String targetAfter = targetStep.getConfig().get(CONFIG_AFTER).get(0);
            long targetTime = Long.parseLong(targetAfter);
            long thisTime = targetTime - timeBeforeTarget.toMillis();
            step.setAfter(thisTime);
            return this;
        }

        public Builder withConfig(String key, String value) {
            step.setConfig(key, value);
            return this;
        }

        public Builder withConfig(String key, String... value) {
            step.setConfig(key, Arrays.asList(value));
            return this;
        }

        public WorkflowStepRepresentation build() {
            return step;
        }
    }
}
