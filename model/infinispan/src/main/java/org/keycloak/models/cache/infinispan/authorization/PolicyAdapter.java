/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.cache.infinispan.authorization;

import org.keycloak.authorization.model.CachedModel;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedPolicy;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PolicyAdapter implements Policy, CachedModel<Policy> {

    private final Supplier<Policy> modelSupplier;
    protected final CachedPolicy cached;
    protected final StoreFactoryCacheSession cacheSession;
    protected Policy updated;

    public PolicyAdapter(CachedPolicy cached, StoreFactoryCacheSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
        this.modelSupplier = this::getPolicyModel;
    }

    @Override
    public Policy getDelegateForUpdate() {
        if (updated == null) {
            updated = modelSupplier.get();
            String defaultResourceType = updated.getConfig().get("defaultResourceType");
            cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(modelSupplier), cached.getScopesIds(modelSupplier), defaultResourceType, cached.getResourceServerId());
            if (updated == null) throw new IllegalStateException("Not found in database");
        }
        return updated;
    }

    protected boolean invalidated;

    protected void invalidateFlag() {
        invalidated = true;

    }

    @Override
    public void invalidate() {
        invalidated = true;
        getDelegateForUpdate();
    }

    @Override
    public long getCacheTimestamp() {
        return cached.getCacheTimestamp();
    }

    protected boolean isUpdated() {
        if (updated != null) return true;
        if (!invalidated) return false;
        updated = cacheSession.getPolicyStoreDelegate().findById(cached.getId(), cached.getResourceServerId());
        if (updated == null) throw new IllegalStateException("Not found in database");
        return true;
    }


    @Override
    public String getId() {
        if (isUpdated()) return updated.getId();
        return cached.getId();
    }

    @Override
    public String getName() {
        if (isUpdated()) return updated.getName();
        return cached.getName();
    }

    @Override
    public void setName(String name) {
        getDelegateForUpdate();
        cacheSession.registerPolicyInvalidation(cached.getId(), name, cached.getResourcesIds(modelSupplier), cached.getScopesIds(modelSupplier), cached.getConfig(modelSupplier).get("defaultResourceType"), cached.getResourceServerId());
        updated.setName(name);
    }

    @Override
    public ResourceServer getResourceServer() {
        return cacheSession.getResourceServerStore().findById(cached.getResourceServerId());
    }

    @Override
    public String getType() {
        if (isUpdated()) return updated.getType();
        return cached.getType();
    }

    @Override
    public DecisionStrategy getDecisionStrategy() {
        if (isUpdated()) return updated.getDecisionStrategy();
        return cached.getDecisionStrategy();
    }

    @Override
    public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
        getDelegateForUpdate();
        updated.setDecisionStrategy(decisionStrategy);

    }

    @Override
    public Logic getLogic() {
        if (isUpdated()) return updated.getLogic();
        return cached.getLogic();
    }

    @Override
    public void setLogic(Logic logic) {
        getDelegateForUpdate();
        updated.setLogic(logic);

    }

    @Override
    public Map<String, String> getConfig() {
        if (isUpdated()) return updated.getConfig();
        return cached.getConfig(modelSupplier);
    }

    @Override
    public void setConfig(Map<String, String> config) {
        getDelegateForUpdate();
        if (config.containsKey("defaultResourceType") || cached.getConfig(modelSupplier).containsKey("defaultResourceType")) {
            cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(modelSupplier), cached.getScopesIds(modelSupplier), cached.getConfig(modelSupplier).get("defaultResourceType"), cached.getResourceServerId());
            cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(modelSupplier), cached.getScopesIds(modelSupplier), config.get("defaultResourceType"), cached.getResourceServerId());
        }
        updated.setConfig(config);

    }

    @Override
    public void removeConfig(String name) {
        getDelegateForUpdate();
        if (name.equals("defaultResourceType")) {
            cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(modelSupplier), cached.getScopesIds(modelSupplier), cached.getConfig(modelSupplier).get("defaultResourceType"), cached.getResourceServerId());
        }
        updated.removeConfig(name);

    }

    @Override
    public void putConfig(String name, String value) {
        getDelegateForUpdate();
        if (name.equals("defaultResourceType")) {
            cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(modelSupplier), cached.getScopesIds(modelSupplier), cached.getConfig(modelSupplier).get("defaultResourceType"), cached.getResourceServerId());
            cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(modelSupplier), cached.getScopesIds(modelSupplier), value, cached.getResourceServerId());
        }
        updated.putConfig(name, value);
    }

    @Override
    public String getDescription() {
        if (isUpdated()) return updated.getDescription();
        return cached.getDescription();
    }

    @Override
    public void setDescription(String description) {
        getDelegateForUpdate();
        updated.setDescription(description);
    }

    protected Set<Policy> associatedPolicies;

    @Override
    public Set<Policy> getAssociatedPolicies() {
        if (isUpdated()) {
            return updated.getAssociatedPolicies().stream().map(policy -> new PolicyAdapter(cacheSession.createCachedPolicy(policy, policy.getId()), cacheSession)).collect(Collectors.toSet());
        }
        if (associatedPolicies != null) return associatedPolicies;
        associatedPolicies = new HashSet<>();
        PolicyStore policyStore = cacheSession.getPolicyStore();
        String resourceServerId = cached.getResourceServerId();
        for (String id : cached.getAssociatedPoliciesIds(modelSupplier)) {
            Policy policy = policyStore.findById(id, resourceServerId);
            cacheSession.cachePolicy(policy);
            associatedPolicies.add(policy);
        }
        return associatedPolicies = Collections.unmodifiableSet(associatedPolicies);
    }

    protected Set<Resource> resources;

    @Override
    public Set<Resource> getResources() {
        if (isUpdated()) return updated.getResources();
        if (resources != null) return resources;
        resources = new HashSet<>();
        ResourceStore resourceStore = cacheSession.getResourceStore();
        for (String resourceId : cached.getResourcesIds(modelSupplier)) {
            String resourceServerId = cached.getResourceServerId();
            Resource resource = resourceStore.findById(resourceId, resourceServerId);
            cacheSession.cacheResource(resource);
            resources.add(resource);
        }
        return resources = Collections.unmodifiableSet(resources);
    }

    @Override
    public void addScope(Scope scope) {
        getDelegateForUpdate();
        cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(modelSupplier), new HashSet<>(Arrays.asList(scope.getId())), cached.getConfig(modelSupplier).get("defaultResourceType"), cached.getResourceServerId());
        updated.addScope(scope);
    }

    @Override
    public void removeScope(Scope scope) {
        getDelegateForUpdate();
        cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(modelSupplier), new HashSet<>(Arrays.asList(scope.getId())), cached.getConfig(modelSupplier).get("defaultResourceType"), cached.getResourceServerId());
        updated.removeScope(scope);
    }

    @Override
    public void addAssociatedPolicy(Policy associatedPolicy) {
        getDelegateForUpdate();
        updated.addAssociatedPolicy(associatedPolicy);

    }

    @Override
    public void removeAssociatedPolicy(Policy associatedPolicy) {
        getDelegateForUpdate();
        updated.removeAssociatedPolicy(associatedPolicy);

    }

    @Override
    public void addResource(Resource resource) {
        getDelegateForUpdate();
        HashSet<String> resources = new HashSet<>();
        resources.add(resource.getId());
        cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), resources, cached.getScopesIds(modelSupplier), cached.getConfig(modelSupplier).get("defaultResourceType"), cached.getResourceServerId());
        updated.addResource(resource);

    }

    @Override
    public void removeResource(Resource resource) {
        getDelegateForUpdate();
        HashSet<String> resources = new HashSet<>();
        resources.add(resource.getId());
        cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), resources, cached.getScopesIds(modelSupplier), cached.getConfig(modelSupplier).get("defaultResourceType"), cached.getResourceServerId());
        updated.removeResource(resource);

    }

    protected Set<Scope> scopes;

    @Override
    public Set<Scope> getScopes() {
        if (isUpdated()) return updated.getScopes();
        if (scopes != null) return scopes;
        scopes = new HashSet<>();
        ScopeStore scopeStore = cacheSession.getScopeStore();
        String resourceServerId = cached.getResourceServerId();
        for (String scopeId : cached.getScopesIds(modelSupplier)) {
            Scope scope = scopeStore.findById(scopeId, resourceServerId);
            cacheSession.cacheScope(scope);
            scopes.add(scope);
        }
        return scopes = Collections.unmodifiableSet(scopes);
    }

    @Override
    public String getOwner() {
        if (isUpdated()) return updated.getOwner();
        return cached.getOwner();
    }

    @Override
    public void setOwner(String owner) {
        getDelegateForUpdate();
        cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(modelSupplier), cached.getScopesIds(modelSupplier), cached.getConfig(modelSupplier).get("defaultResourceType"), cached.getResourceServerId());
        updated.setOwner(owner);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Policy)) return false;

        Policy that = (Policy) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    private Policy getPolicyModel() {
        return cacheSession.getPolicyStoreDelegate().findById(cached.getId(), cached.getResourceServerId());
    }
}
