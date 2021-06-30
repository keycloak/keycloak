/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.authorization.entity;

import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.Objects;

public class MapPolicyEntity<K> implements AbstractEntity<K> {
    
    private final K id;
    private String name;
    private String description;
    private String type;
    private DecisionStrategy decisionStrategy = DecisionStrategy.UNANIMOUS;
    private Logic logic = Logic.POSITIVE;
    private final Map<String, String> config = new HashMap<>();
    private String resourceServerId;
    private final Set<String> associatedPoliciesIds = new HashSet<>();
    private final Set<String> resourceIds = new HashSet<>();
    private final Set<String> scopeIds = new HashSet<>();
    private String owner;
    private boolean updated = false;

    public MapPolicyEntity(K id) {
        this.id = id;
    }

    public MapPolicyEntity() {
        this.id = null;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated |= !Objects.equals(this.name, name);
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.updated |= !Objects.equals(this.description, description);
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.updated |= !Objects.equals(this.type, type);
        this.type = type;
    }

    public DecisionStrategy getDecisionStrategy() {
        return decisionStrategy;
    }

    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        this.updated |= !Objects.equals(this.decisionStrategy, decisionStrategy);
        this.decisionStrategy = decisionStrategy;
    }

    public Logic getLogic() {
        return logic;
    }

    public void setLogic(Logic logic) {
        this.updated |= !Objects.equals(this.logic, logic);
        this.logic = logic;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public String getConfigValue(String name) {
        return config.get(name);
    }

    public void setConfig(Map<String, String> config) {
        if (Objects.equals(this.config, config)) return;

        this.updated = true;
        this.config.clear();
        if (config != null) {
            this.config.putAll(config);
        }
    }
    
    public void removeConfig(String name) {
        this.updated |= this.config.remove(name) != null;
    }

    public void putConfig(String name, String value) {
        this.updated |= !Objects.equals(value, this.config.put(name, value));
    }

    public String getResourceServerId() {
        return resourceServerId;
    }

    public void setResourceServerId(String resourceServerId) {
        this.updated |= !Objects.equals(this.resourceServerId, resourceServerId);
        this.resourceServerId = resourceServerId;
    }

    public Set<String> getAssociatedPoliciesIds() {
        return associatedPoliciesIds;
    }

    public void addAssociatedPolicy(String policyId) {
        this.updated |= this.associatedPoliciesIds.add(policyId);
    }

    public void removeAssociatedPolicy(String policyId) {
        this.updated |= this.associatedPoliciesIds.remove(policyId);
    }

    public Set<String> getResourceIds() {
        return resourceIds;
    }

    public void addResource(String resourceId) {
        this.updated |= this.resourceIds.add(resourceId);
    }

    public void removeResource(String resourceId) {
        this.updated |= this.resourceIds.remove(resourceId);
    }

    public Set<String> getScopeIds() {
        return scopeIds;
    }
    
    public void addScope(String scopeId) {
        this.updated |= this.scopeIds.add(scopeId);
    }
    
    public void removeScope(String scopeId) {
        this.updated |= this.scopeIds.remove(scopeId);
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.updated |= !Objects.equals(this.owner, owner);
        this.owner = owner;
    }

    @Override
    public K getId() {
        return id;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
