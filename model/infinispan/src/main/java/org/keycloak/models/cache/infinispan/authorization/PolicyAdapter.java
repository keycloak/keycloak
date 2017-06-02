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
import org.keycloak.models.cache.infinispan.authorization.entities.CachedPolicy;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class PolicyAdapter implements Policy, CachedModel<Policy> {
    protected CachedPolicy cached;
    protected StoreFactoryCacheSession cacheSession;
    protected Policy updated;

    public PolicyAdapter(CachedPolicy cached, StoreFactoryCacheSession cacheSession) {
        this.cached = cached;
        this.cacheSession = cacheSession;
    }

    @Override
    public Policy getDelegateForUpdate() {
        if (updated == null) {
            cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), cached.getResourcesIds(), cached.getResourceServerId());
            updated = cacheSession.getPolicyStoreDelegate().findById(cached.getId(), cached.getResourceServerId());
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
        cacheSession.registerPolicyInvalidation(cached.getId(), name, cached.getResourcesIds(), cached.getResourceServerId());
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
        return cached.getConfig();
    }

    @Override
    public void setConfig(Map<String, String> config) {
        getDelegateForUpdate();
        updated.setConfig(config);

    }

    @Override
    public void removeConfig(String name) {
        getDelegateForUpdate();
        updated.removeConfig(name);

    }

    @Override
    public void putConfig(String name, String value) {
        getDelegateForUpdate();
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
        if (isUpdated()) return updated.getAssociatedPolicies();
        if (associatedPolicies != null) return associatedPolicies;
        associatedPolicies = new HashSet<>();
        for (String scopeId : cached.getAssociatedPoliciesIds()) {
            associatedPolicies.add(cacheSession.getPolicyStore().findById(scopeId, cached.getResourceServerId()));
        }
        associatedPolicies = Collections.unmodifiableSet(associatedPolicies);
        return associatedPolicies;
    }

    protected Set<Resource> resources;
    @Override
    public Set<Resource> getResources() {
        if (isUpdated()) return updated.getResources();
        if (resources != null) return resources;
        resources = new HashSet<>();
        for (String resourceId : cached.getResourcesIds()) {
            resources.add(cacheSession.getResourceStore().findById(resourceId, cached.getResourceServerId()));
        }
        resources = Collections.unmodifiableSet(resources);
        return resources;
    }

    @Override
    public void addScope(Scope scope) {
        getDelegateForUpdate();
        updated.addScope(scope);
    }

    @Override
    public void removeScope(Scope scope) {
        getDelegateForUpdate();
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
        cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), resources, cached.getResourceServerId());
        updated.addResource(resource);

    }

    @Override
    public void removeResource(Resource resource) {
        getDelegateForUpdate();
        HashSet<String> resources = new HashSet<>();
        resources.add(resource.getId());
        cacheSession.registerPolicyInvalidation(cached.getId(), cached.getName(), resources, cached.getResourceServerId());
        updated.removeResource(resource);

    }

    protected Set<Scope> scopes;

    @Override
    public Set<Scope> getScopes() {
        if (isUpdated()) return updated.getScopes();
        if (scopes != null) return scopes;
        scopes = new HashSet<>();
        for (String scopeId : cached.getScopesIds()) {
            scopes.add(cacheSession.getScopeStore().findById(scopeId, cached.getResourceServerId()));
        }
        scopes = Collections.unmodifiableSet(scopes);
        return scopes;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof Policy)) return false;

        Policy that = (Policy) o;
        return that.getId().equals(getId());
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }




}
