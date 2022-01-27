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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class MapPolicyAdapter extends AbstractPolicyModel<MapPolicyEntity> {
    
    public MapPolicyAdapter(MapPolicyEntity entity, StoreFactory storeFactory) {
        super(entity, storeFactory);
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public String getType() {
        return entity.getType();
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        DecisionStrategy ds = entity.getDecisionStrategy();
        return ds == null ? DecisionStrategy.UNANIMOUS : ds;
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        throwExceptionIfReadonly();
        entity.setDecisionStrategy(decisionStrategy);
    }

    @Override
    public Logic getLogic() {
        Logic l = entity.getLogic();
        return l == null ? Logic.POSITIVE : l;
    }

    @Override
    public void setLogic(Logic logic) {
        throwExceptionIfReadonly();
        entity.setLogic(logic);
    }

    @Override
    public Map<String, String> getConfig() {
        Map<String, String> c = entity.getConfigs();
        return c == null ? Collections.emptyMap() : c;
    }

    @Override
    public void setConfig(Map<String, String> config) {
        throwExceptionIfReadonly();
        entity.setConfigs(config);
    }

    @Override
    public void removeConfig(String name) {
        throwExceptionIfReadonly();
        entity.removeConfig(name);
    }

    @Override
    public void putConfig(String name, String value) {
        throwExceptionIfReadonly();
        entity.setConfig(name, value);
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
        Set<String> ids = entity.getAssociatedPolicyIds();
        return ids == null ? Collections.emptySet() : ids.stream()
                .map(policyId -> storeFactory.getPolicyStore().findById(policyId, resourceServerId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Resource> getResources() {
        String resourceServerId = entity.getResourceServerId();
        Set<String> ids = entity.getResourceIds();
        return ids == null ? Collections.emptySet() : ids.stream()
                .map(resourceId -> storeFactory.getResourceStore().findById(resourceId, resourceServerId))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Scope> getScopes() {
        String resourceServerId = entity.getResourceServerId();
        Set<String> ids = entity.getScopeIds();
        return ids == null ? Collections.emptySet() : ids.stream()
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
        entity.addScopeId(scope.getId());
    }

    @Override
    public void removeScope(Scope scope) {
        throwExceptionIfReadonly();
        entity.removeScopeId(scope.getId());
    }

    @Override
    public void addAssociatedPolicy(Policy associatedPolicy) {
        throwExceptionIfReadonly();
        entity.addAssociatedPolicyId(associatedPolicy.getId());
    }

    @Override
    public void removeAssociatedPolicy(Policy associatedPolicy) {
        throwExceptionIfReadonly();
        entity.removeAssociatedPolicyId(associatedPolicy.getId());
    }

    @Override
    public void addResource(Resource resource) {
        throwExceptionIfReadonly();
        entity.addResourceId(resource.getId());
    }

    @Override
    public void removeResource(Resource resource) {
        throwExceptionIfReadonly();
        entity.removeResourceId(resource.getId());
    }

    @Override
    public String toString() {
        return String.format("%s@%08x", getId(), System.identityHashCode(this));
    }
}
