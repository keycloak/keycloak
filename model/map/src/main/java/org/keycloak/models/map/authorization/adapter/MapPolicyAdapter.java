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

package org.keycloak.models.map.authorization.adapter;

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.map.authorization.entity.MapPolicyEntity;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class MapPolicyAdapter<K> extends AbstractPolicyModel<MapPolicyEntity<K>> {
    
    public MapPolicyAdapter(MapPolicyEntity<K> entity, StoreFactory storeFactory) {
        super(entity, storeFactory);
    }

    @Override
    public String getType() {
        return entity.getType();
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        return entity.getDecisionStrategy();
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        throwExceptionIfReadonly();
        entity.setDecisionStrategy(decisionStrategy);
    }

    @Override
    public Logic getLogic() {
        return entity.getLogic();
    }

    @Override
    public void setLogic(Logic logic) {
        throwExceptionIfReadonly();
        entity.setLogic(logic);
    }

    @Override
    public Map<String, String> getConfig() {
        return entity.getConfig();
    }

    @Override
    public void setConfig(Map<String, String> config) {
        throwExceptionIfReadonly();
        entity.setConfig(config);
    }

    @Override
    public void removeConfig(String name) {
        throwExceptionIfReadonly();
        entity.removeConfig(name);
    }

    @Override
    public void putConfig(String name, String value) {
        throwExceptionIfReadonly();
        entity.putConfig(name, value);
    }

    @Override
    public String getName() {
        return entity.getName();
    }

    @Override
    public void setName(String name) {
        throwExceptionIfReadonly();
        entity.setName(name);
    }

    @Override
    public String getDescription() {
        return entity.getDescription();
    }

    @Override
    public void setDescription(String description) {
        throwExceptionIfReadonly();
        entity.setDescription(description);
    }

    @Override
    public ResourceServer getResourceServer() {
        return storeFactory.getResourceServerStore().findById(entity.getResourceServerId());
    }

    @Override
    public Set<Policy> getAssociatedPolicies() {
        String resourceServerId = entity.getResourceServerId();
        return entity.getAssociatedPoliciesIds().stream()
                .map(policyId -> storeFactory.getPolicyStore().findById(policyId, resourceServerId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Resource> getResources() {
        String resourceServerId = entity.getResourceServerId();
        return entity.getResourceIds().stream()
                .map(resourceId -> storeFactory.getResourceStore().findById(resourceId, resourceServerId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Scope> getScopes() {
        String resourceServerId = entity.getResourceServerId();
        return entity.getScopeIds().stream()
                .map(scopeId -> storeFactory.getScopeStore().findById(scopeId, resourceServerId))
                .collect(Collectors.toSet());
    }

    @Override
    public String getOwner() {
        return entity.getOwner();
    }

    @Override
    public void setOwner(String owner) {
        throwExceptionIfReadonly();
        entity.setOwner(owner);
    }

    @Override
    public void addScope(Scope scope) {
        throwExceptionIfReadonly();
        entity.addScope(scope.getId());
    }

    @Override
    public void removeScope(Scope scope) {
        throwExceptionIfReadonly();
        entity.removeScope(scope.getId());
    }

    @Override
    public void addAssociatedPolicy(Policy associatedPolicy) {
        throwExceptionIfReadonly();
        entity.addAssociatedPolicy(associatedPolicy.getId());
    }

    @Override
    public void removeAssociatedPolicy(Policy associatedPolicy) {
        throwExceptionIfReadonly();
        entity.removeAssociatedPolicy(associatedPolicy.getId());
    }

    @Override
    public void addResource(Resource resource) {
        throwExceptionIfReadonly();
        entity.addResource(resource.getId());
    }

    @Override
    public void removeResource(Resource resource) {
        throwExceptionIfReadonly();
        entity.removeResource(resource.getId());
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
