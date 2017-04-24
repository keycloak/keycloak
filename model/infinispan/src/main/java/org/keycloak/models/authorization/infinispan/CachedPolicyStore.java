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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

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
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedPolicyStore implements PolicyStore {

    private static final String POLICY_ID_CACHE_PREFIX = "policy-id-";

    private final Cache<String, Map<String, List<CachedPolicy>>> cache;
    private final CachedStoreFactoryProvider cacheStoreFactory;
    private final CacheTransaction transaction;
    private final List<String> cacheKeys;
    private final StoreFactory storeFactory;
    private PolicyStore delegate;

    public CachedPolicyStore(KeycloakSession session, CachedStoreFactoryProvider cacheStoreFactory, CacheTransaction transaction, StoreFactory delegate) {
        this.cacheStoreFactory = cacheStoreFactory;
        this.transaction = transaction;
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
        cacheKeys = new ArrayList<>();
        cacheKeys.add("findByResource");
        cacheKeys.add("findByResourceType");
        cacheKeys.add("findByScopeIds");
        cacheKeys.add("findByType");
        this.storeFactory = delegate;
    }

    @Override
    public Policy create(AbstractPolicyRepresentation representation, ResourceServer resourceServer) {
        Policy policy = getDelegate().create(representation, getStoreFactory().getResourceServerStore().findById(resourceServer.getId()));
        String id = policy.getId();

        this.transaction.whenRollback(() -> {
            resolveResourceServerCache(resourceServer.getId()).remove(getCacheKeyForPolicy(id));
        });

        this.transaction.whenCommit(() -> {
            invalidateCache(resourceServer.getId());
        });

        return createAdapter(new CachedPolicy(policy));
    }

    @Override
    public void delete(String id) {
        Policy policy = getDelegate().findById(id, null);
        if (policy == null) {
            return;
        }
        ResourceServer resourceServer = policy.getResourceServer();
        getDelegate().delete(id);
        invalidateCache(resourceServer.getId());
    }

    @Override
    public Policy findById(String id, String resourceServerId) {
        if (resourceServerId == null) {
            return getDelegate().findById(id, null);
        }

        String cacheKeyForPolicy = getCacheKeyForPolicy(id);
        List<CachedPolicy> cached = resolveResourceServerCache(resourceServerId).get(cacheKeyForPolicy);

        if (cached == null) {
            Policy policy = getDelegate().findById(id, resourceServerId);

            if (policy != null) {
                CachedPolicy cachedPolicy = new CachedPolicy(policy);
                resolveResourceServerCache(resourceServerId).put(cacheKeyForPolicy, Arrays.asList(cachedPolicy));
                return createAdapter(cachedPolicy);
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
        return getDelegate().findByResourceServer(resourceServerId);
    }

    @Override
    public List<Policy> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        return getDelegate().findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
    }

    @Override
    public List<Policy> findByResource(String resourceId, String resourceServerId) {
        return cacheResult(resourceServerId, new StringBuilder("findByResource").append(resourceId).toString(), () -> getDelegate().findByResource(resourceId, resourceServerId));
    }

    @Override
    public List<Policy> findByResourceType(String resourceType, String resourceServerId) {
        return cacheResult(resourceServerId, new StringBuilder("findByResourceType").append(resourceType).toString(), () -> getDelegate().findByResourceType(resourceType, resourceServerId));
    }

    @Override
    public List<Policy> findByScopeIds(List<String> scopeIds, String resourceServerId) {
        List<Policy> policies = new ArrayList<>();

        for (String scopeId : scopeIds) {
            policies.addAll(cacheResult(resourceServerId, new StringBuilder("findByScopeIds").append(scopeId).toString(), () -> getDelegate().findByScopeIds(Arrays.asList(scopeId), resourceServerId)));
        }

        return policies;
    }

    @Override
    public List<Policy> findByType(String type, String resourceServerId) {
        return cacheResult(resourceServerId, new StringBuilder("findByType").append(type).toString(), () -> getDelegate().findByType(type, resourceServerId));
    }

    @Override
    public List<Policy> findDependentPolicies(String id, String resourceServerId) {
        return getDelegate().findDependentPolicies(id, resourceServerId);
    }

    @Override
    public void notifyChange(Object cached) {
        String resourceServerId;

        if (Resource.class.isInstance(cached)) {
            resourceServerId = ((Resource) cached).getResourceServer().getId();
        } else if (Scope.class.isInstance(cached)){
            resourceServerId = ((Scope) cached).getResourceServer().getId();
        } else {
            throw new RuntimeException("Unexpected notification [" + cached + "]");
        }

        invalidateCache(resourceServerId);
    }

    private String getCacheKeyForPolicy(String policyId) {
        return POLICY_ID_CACHE_PREFIX + policyId;
    }

    private StoreFactory getStoreFactory() {
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

            private Set<Scope> scopes;
            private Set<Resource> resources;
            private Set<Policy> associatedPolicies;
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
                return getCachedStoreFactory().getResourceServerStore().findById(cached.getResourceServerId());
            }

            @Override
            public void addScope(Scope scope) {
                getDelegateForUpdate().addScope(getStoreFactory().getScopeStore().findById(scope.getId(), cached.getResourceServerId()));
                cached.addScope(scope);
            }

            @Override
            public void removeScope(Scope scope) {
                getDelegateForUpdate().removeScope(getStoreFactory().getScopeStore().findById(scope.getId(), cached.getResourceServerId()));
                cached.removeScope(scope);
                scopes.remove(scope);
            }

            @Override
            public void addAssociatedPolicy(Policy associatedPolicy) {
                getDelegateForUpdate().addAssociatedPolicy(getStoreFactory().getPolicyStore().findById(associatedPolicy.getId(), cached.getResourceServerId()));
                cached.addAssociatedPolicy(associatedPolicy);
            }

            @Override
            public void removeAssociatedPolicy(Policy associatedPolicy) {
                getDelegateForUpdate().removeAssociatedPolicy(getStoreFactory().getPolicyStore().findById(associatedPolicy.getId(), cached.getResourceServerId()));
                cached.removeAssociatedPolicy(associatedPolicy);
                associatedPolicies.remove(associatedPolicy);
            }

            @Override
            public void addResource(Resource resource) {
                getDelegateForUpdate().addResource(getStoreFactory().getResourceStore().findById(resource.getId(), cached.getResourceServerId()));
                cached.addResource(resource);
            }

            @Override
            public void removeResource(Resource resource) {
                getDelegateForUpdate().removeResource(getStoreFactory().getResourceStore().findById(resource.getId(), cached.getResourceServerId()));
                cached.removeResource(resource);
                resources.remove(resource);
            }

            @Override
            public Set<Policy> getAssociatedPolicies() {
                if (associatedPolicies == null) {
                    associatedPolicies = new HashSet<>();

                    for (String id : cached.getAssociatedPoliciesIds()) {
                        Policy policy = findById(id, cached.getResourceServerId());

                        if (policy != null) {
                            associatedPolicies.add(policy);
                        }
                    }
                }

                return associatedPolicies;
            }

            @Override
            public Set<Resource> getResources() {
                if (resources == null) {
                    resources = new HashSet<>();

                    for (String id : cached.getResourcesIds()) {
                        Resource resource = getCachedStoreFactory().getResourceStore().findById(id, cached.getResourceServerId());

                        if (resource != null) {
                            resources.add(resource);
                        }
                    }
                }

                return resources;
            }

            @Override
            public Set<Scope> getScopes() {
                if (scopes == null) {
                    scopes = new HashSet<>();

                    for (String id : cached.getScopesIds()) {
                        Scope scope = getCachedStoreFactory().getScopeStore().findById(id, cached.getResourceServerId());

                        if (scope != null) {
                            scopes.add(scope);
                        }
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
                    this.updated = getDelegate().findById(getId(), cached.getResourceServerId());
                    if (this.updated == null) throw new IllegalStateException("Not found in database");
                    transaction.whenCommit(() -> {
                        invalidateCache(cached.getResourceServerId());
                    });
                    transaction.whenRollback(() -> {
                        resolveResourceServerCache(cached.getResourceServerId()).remove(getCacheKeyForPolicy(getId()));
                    });
                }

                return this.updated;
            }
        };
    }

    private CachedStoreFactoryProvider getCachedStoreFactory() {
        return cacheStoreFactory;
    }

    private void invalidateCache(String resourceServerId) {
        cache.remove(resourceServerId);
    }

    private List<Policy> cacheResult(String resourceServerId, String key, Supplier<List<Policy>> provider) {
        List<CachedPolicy> cached = resolveResourceServerCache(resourceServerId).computeIfAbsent(key, (Function<String, List<CachedPolicy>>) o -> {
            List<Policy> result = provider.get();

            if (result.isEmpty()) {
                return Collections.emptyList();
            }

            return result.stream().map(policy -> new CachedPolicy(policy)).collect(Collectors.toList());
        });

        if (cached == null) {
            return Collections.emptyList();
        }

        return cached.stream().map(cachedPolicy -> createAdapter(cachedPolicy)).collect(Collectors.toList());
    }

    private Map<String, List<CachedPolicy>> resolveResourceServerCache(String id) {
        return cache.computeIfAbsent(id, key -> new HashMap<>());
    }
}