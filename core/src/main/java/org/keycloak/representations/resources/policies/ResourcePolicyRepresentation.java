package org.keycloak.representations.resources.policies;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.keycloak.common.util.MultivaluedHashMap;

public class ResourcePolicyRepresentation {

    public static Builder create() {
        return new Builder();
    }

    private String id;
    private String providerId;
    private MultivaluedHashMap<String, String> config;
    private List<ResourcePolicyActionRepresentation> actions;
    private List<ResourcePolicyConditionRepresentation> conditions;

    public ResourcePolicyRepresentation() {
        // reflection
    }

    public ResourcePolicyRepresentation(String providerId) {
        this(providerId, null);
    }

    public ResourcePolicyRepresentation(String providerId, Map<String, List<String>> config) {
        this(null, providerId, config);
    }

    public ResourcePolicyRepresentation(String id, String providerId, Map<String, List<String>> config) {
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

    public void setConditions(List<ResourcePolicyConditionRepresentation> conditions) {
        this.conditions = conditions;
    }

    public List<ResourcePolicyConditionRepresentation> getConditions() {
        return conditions;
    }

    public void setActions(List<ResourcePolicyActionRepresentation> actions) {
        this.actions = actions;
    }

    public List<ResourcePolicyActionRepresentation> getActions() {
        return actions;
    }

    public MultivaluedHashMap<String, String> getConfig() {
        return config;
    }

    public void addAction(ResourcePolicyActionRepresentation action) {
        if (actions == null) {
            actions = new ArrayList<>();
        }
        actions.add(action);
    }

    public static class Builder {
        private String providerId;
        private Map<String, List<String>> config = new HashMap<>();
        private List<ResourcePolicyConditionRepresentation> conditions = new ArrayList<>();
        private final Map<String, List<ResourcePolicyActionRepresentation>> actions = new HashMap<>();
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

        public Builder onCoditions(ResourcePolicyConditionRepresentation... condition) {
            if (conditions == null) {
                conditions = new ArrayList<>();
            }
            conditions.addAll(Arrays.asList(condition));
            return this;
        }

        public Builder withActions(ResourcePolicyActionRepresentation... actions) {
            this.actions.computeIfAbsent(providerId, (k) -> new ArrayList<>()).addAll(Arrays.asList(actions));
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

        public List<ResourcePolicyRepresentation> build() {
            List<ResourcePolicyRepresentation> policies = new ArrayList<>();

            for (Builder builder : builders) {
                for (Entry<String, List<ResourcePolicyActionRepresentation>> entry : builder.actions.entrySet()) {
                    ResourcePolicyRepresentation policy = new ResourcePolicyRepresentation(entry.getKey(), builder.config);

                    policy.setActions(entry.getValue());
                    policy.setConditions(builder.conditions);

                    policies.add(policy);
                }
            }

            return policies;
        }
    }
}
