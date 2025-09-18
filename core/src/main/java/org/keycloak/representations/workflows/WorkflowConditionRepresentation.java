package org.keycloak.representations.workflows;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.keycloak.common.util.MultivaluedHashMap;

@JsonPropertyOrder({"uses", "config"})
public final class WorkflowConditionRepresentation extends AbstractWorkflowComponentRepresentation {

    public static Builder create() {
        return new Builder();
    }

    public WorkflowConditionRepresentation() {
        super(null, null, null);
    }

    public WorkflowConditionRepresentation(String condition) {
        this(condition, null);
    }

    public WorkflowConditionRepresentation(String condition, MultivaluedHashMap<String, String> config) {
        super(null, condition, config);
    }

    @Override
    @JsonSerialize(using = MultivaluedHashMapValueSerializer.class)
    @JsonDeserialize(using = MultivaluedHashMapValueDeserializer.class)
    public MultivaluedHashMap<String, String> getConfig() {
        return super.getConfig();
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

        public Builder withConfig(String key, String... values) {
            action.setConfig(key, Arrays.asList(values));
            return this;
        }

        public Builder withConfig(Map<String, List<String>> config) {
            action.setConfig(new MultivaluedHashMap<>(config));
            return this;
        }

        public WorkflowConditionRepresentation build() {
            return action;
        }
    }
}
