/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.models.authorization.infinispan;

import org.infinispan.Cache;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.authorization.infinispan.InfinispanStoreFactoryProvider.CacheTransaction;
import org.keycloak.models.authorization.infinispan.entities.CachedPolicy;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedPolicyStore implements PolicyStore {

    private static final String POLICY_ID_CACHE_PREFIX = "policy-id-";

    private final Cache<String, List> cache;
    private final KeycloakSession session;
    private final CacheTransaction transaction;
    private StoreFactory storeFactory;
    private PolicyStore delegate;

    public CachedPolicyStore(KeycloakSession session, CacheTransaction transaction) {
        this.session = session;
        this.transaction = transaction;
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
    }

    @Override
    public Policy create(String name, String type, ResourceServer resourceServer) {
        Policy policy = getDelegate().create(name, type, getStoreFactory().getResourceServerStore().findById(resourceServer.getId()));

        this.transaction.whenRollback(() -> cache.remove(getCacheKeyForPolicy(policy.getId())));

        return createAdapter(new CachedPolicy(policy));
    }

    @Override
    public void delete(String id) {
        getDelegate().delete(id);
        this.transaction.whenCommit(() -> cache.remove(getCacheKeyForPolicy(id)));
    }

    @Override
    public Policy findById(String id) {
        String cacheKeyForPolicy = getCacheKeyForPolicy(id);
        List<CachedPolicy> cached = this.cache.get(cacheKeyForPolicy);

        if (cached == null) {
            Policy policy = getDelegate().findById(id);

            if (policy != null) {
                return createAdapter(updatePolicyCache(policy));
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public Policy findByName(String name, String resourceServerId) {
        return getDelegate().findByName(name, resourceServerId);
    }

    @Override
    public List<Policy> findByResourceServer(String resourceServerId) {
        return getDelegate().findByResourceServer(resourceServerId).stream().map(policy -> findById(policy.getId())).collect(Collectors.toList());
    }

    @Override
    public List<Policy> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        return getDelegate().findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
    }

    @Override
    public List<Policy> findByResource(String resourceId) {
        List<Policy> cache = new ArrayList<>();

        for (Entry entry : this.cache.entrySet()) {
            String cacheKey = (String) entry.getKey();

            if (cacheKey.startsWith(POLICY_ID_CACHE_PREFIX)) {
                List<CachedPolicy> value = (List<CachedPolicy>) entry.getValue();
                CachedPolicy policy = value.get(0);

                if (policy.getResourcesIds().contains(resourceId)) {
                    cache.add(findById(policy.getId()));
                }
            }
        }

        if (cache.isEmpty()) {
            getDelegate().findByResource(resourceId).forEach(policy -> cache.add(findById(updatePolicyCache(policy).getId())));
        }

        return cache;
    }

    @Override
    public List<Policy> findByResourceType(String resourceType, String resourceServerId) {
        List<Policy> cache = new ArrayList<>();

        for (Entry entry : this.cache.entrySet()) {
            String cacheKey = (String) entry.getKey();

            if (cacheKey.startsWith(POLICY_ID_CACHE_PREFIX)) {
                List<CachedPolicy> value = (List<CachedPolicy>) entry.getValue();
                CachedPolicy policy = value.get(0);

                if (policy.getResourceServerId().equals(resourceServerId) && policy.getConfig().getOrDefault("defaultResourceType", "").equals(resourceType)) {
                    cache.add(findById(policy.getId()));
                }
            }
        }

        if (cache.isEmpty()) {
            getDelegate().findByResourceType(resourceType, resourceServerId).forEach(policy -> cache.add(findById(updatePolicyCache(policy).getId())));
        }

        return cache;
    }

    @Override
    public List<Policy> findByScopeIds(List<String> scopeIds, String resourceServerId) {
        List<Policy> cache = new ArrayList<>();

        for (Entry entry : this.cache.entrySet()) {
            String cacheKey = (String) entry.getKey();

            if (cacheKey.startsWith(POLICY_ID_CACHE_PREFIX)) {
                List<CachedPolicy> value = (List<CachedPolicy>) entry.getValue();
                CachedPolicy policy = value.get(0);

                for (String scopeId : policy.getScopesIds()) {
                    if (scopeIds.contains(scopeId)) {
                        cache.add(findById(policy.getId()));
                        break;
                    }
                }
            }
        }

        if (cache.isEmpty()) {
            getDelegate().findByScopeIds(scopeIds, resourceServerId).forEach(policy -> cache.add(findById(updatePolicyCache(policy).getId())));
        }

        return cache;
    }

    @Override
    public List<Policy> findByType(String type) {
        return getDelegate().findByType(type).stream().map(policy -> findById(policy.getId())).collect(Collectors.toList());
    }

    @Override
    public List<Policy> findDependentPolicies(String id) {
        return getDelegate().findDependentPolicies(id).stream().map(policy -> findById(policy.getId())).collect(Collectors.toList());
    }

    private String getCacheKeyForPolicy(String policyId) {
        return POLICY_ID_CACHE_PREFIX + policyId;
    }

    private StoreFactory getStoreFactory() {
        if (this.storeFactory == null) {
            this.storeFactory = this.session.getProvider(StoreFactory.class);
        }

        return this.storeFactory;
    }

    private PolicyStore getDelegate() {
        if (this.delegate == null) {
            this.delegate = getStoreFactory().getPolicyStore();
        }

        return this.delegate;
    }

    private Policy createAdapter(CachedPolicy cached) {
        return new Policy() {

            private Policy updated;

            @Override
            public String getId() {
                return cached.getId();
            }

            @Override
            public String getType() {
                return cached.getType();
            }

            @Override
            public DecisionStrategy getDecisionStrategy() {
                return cached.getDecisionStrategy();
            }

            @Override
            public void setDecisionStrategy(DecisionStrategy decisionStrategy) {
                getDelegateForUpdate().setDecisionStrategy(decisionStrategy);
                cached.setDecisionStrategy(decisionStrategy);
            }

            @Override
            public Logic getLogic() {
                return cached.getLogic();
            }

            @Override
            public void setLogic(Logic logic) {
                getDelegateForUpdate().setLogic(logic);
                cached.setLogic(logic);
            }

            @Override
            public Map<String, String> getConfig() {
                return cached.getConfig();
            }

            @Override
            public void setConfig(Map<String, String> config) {
                getDelegateForUpdate().setConfig(config);
                cached.setConfig(config);
            }

            @Override
            public String getName() {
                return cached.getName();
            }

            @Override
            public void setName(String name) {
                getDelegateForUpdate().setName(name);
                cached.setName(name);
            }

            @Override
            public String getDescription() {
                return cached.getDescription();
            }

            @Override
            public void setDescription(String description) {
                getDelegateForUpdate().setDescription(description);
                cached.setDescription(description);
            }

            @Override
            public ResourceServer getResourceServer() {
                return getStoreFactory().getResourceServerStore().findById(cached.getResourceServerId());
            }

            @Override
            public void addScope(Scope scope) {
                getDelegateForUpdate().addScope(getStoreFactory().getScopeStore().findById(scope.getId()));
                cached.addScope(scope);
            }

            @Override
            public void removeScope(Scope scope) {
                getDelegateForUpdate().removeScope(getStoreFactory().getScopeStore().findById(scope.getId()));
                cached.removeScope(scope);
            }

            @Override
            public void addAssociatedPolicy(Policy associatedPolicy) {
                getDelegateForUpdate().addAssociatedPolicy(getStoreFactory().getPolicyStore().findById(associatedPolicy.getId()));
                cached.addAssociatedPolicy(associatedPolicy);
            }

            @Override
            public void removeAssociatedPolicy(Policy associatedPolicy) {
                getDelegateForUpdate().removeAssociatedPolicy(getStoreFactory().getPolicyStore().findById(associatedPolicy.getId()));
                cached.removeAssociatedPolicy(associatedPolicy);
            }

            @Override
            public void addResource(Resource resource) {
                getDelegateForUpdate().addResource(getStoreFactory().getResourceStore().findById(resource.getId()));
                cached.addResource(resource);
            }

            @Override
            public void removeResource(Resource resource) {
                getDelegateForUpdate().removeResource(getStoreFactory().getResourceStore().findById(resource.getId()));
                cached.removeResource(resource);
            }

            @Override
            public Set<Policy> getAssociatedPolicies() {
                Set<Policy> associatedPolicies = new HashSet<>();

                for (String id : cached.getAssociatedPoliciesIds()) {
                    Policy cached = findById(id);

                    if (cached != null) {
                        associatedPolicies.add(cached);
                    }
                }

                return associatedPolicies;
            }

            @Override
            public Set<Resource> getResources() {
                Set<Resource> resources = new HashSet<>();

                for (String id : cached.getResourcesIds()) {
                    Resource cached = getStoreFactory().getResourceStore().findById(id);

                    if (cached != null) {
                        resources.add(cached);
                    }
                }

                return resources;
            }

            @Override
            public Set<Scope> getScopes() {
                Set<Scope> scopes = new HashSet<>();

                for (String id : cached.getScopesIds()) {
                    Scope cached = getStoreFactory().getScopeStore().findById(id);

                    if (cached != null) {
                        scopes.add(cached);
                    }
                }

                return scopes;
            }

            @Override
            public boolean equals(Object o) {
                if (o == this) return true;

                if (getId() == null) return false;

                if (!Policy.class.isInstance(o)) return false;

                Policy that = (Policy) o;

                if (!getId().equals(that.getId())) return false;

                return true;

            }

            @Override
            public int hashCode() {
                return getId()!=null ? getId().hashCode() : super.hashCode();
            }

            private Policy getDelegateForUpdate() {
                if (this.updated == null) {
                    this.updated = getDelegate().findById(getId());
                    if (this.updated == null) throw new IllegalStateException("Not found in database");
                    transaction.whenCommit(() -> cache.remove(getCacheKeyForPolicy(getId())));
                }

                return this.updated;
            }
        };
    }

    private CachedPolicy updatePolicyCache(Policy policy) {
        CachedPolicy cached = new CachedPolicy(policy);
        List<Policy> cache = new ArrayList<>();

        cache.add(cached);

        this.cache.put(getCacheKeyForPolicy(policy.getId()), cache);

        return cached;
    }

}