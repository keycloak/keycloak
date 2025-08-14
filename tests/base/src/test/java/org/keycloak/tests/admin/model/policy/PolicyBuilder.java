/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.tests.admin.model.policy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.policy.ResourceAction;
import org.keycloak.models.policy.ResourcePolicy;
import org.keycloak.models.policy.ResourcePolicyManager;

public class PolicyBuilder {

    public static PolicyBuilder create() {
        return new PolicyBuilder();
    }

    private String providerId;
    private Map<String, List<String>> config = new HashMap<>();
    private final Map<String, List<ResourceAction>> actions = new HashMap<>();

    private PolicyBuilder() {
    }

    public PolicyBuilder of(String providerId) {
        this.providerId = providerId;
        return this;
    }

    public PolicyBuilder withActions(ResourceAction... actions) {
        this.actions.computeIfAbsent(providerId, (k) -> new ArrayList<>()).addAll(List.of(actions));
        return this;
    }

    public PolicyBuilder withConfig(String key, String value) {
        config.put(key, List.of(value));
        return this;
    }

    public PolicyBuilder withConfig(String key, List<String> value) {
        config.put(key, value);
        return this;
    }

    public ResourcePolicyManager build(KeycloakSession session) {
        ResourcePolicyManager manager = new ResourcePolicyManager(session);

        for (Entry<String, List<ResourceAction>> entry : actions.entrySet()) {
            ResourcePolicy policy = manager.addPolicy(entry.getKey(), config);
            manager.updateActions(policy, entry.getValue());
        }

        return manager;
    }
}
