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

import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.authorization.infinispan.entities.CachedPolicy;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.representations.idm.authorization.DecisionStrategy;
import org.keycloak.representations.idm.authorization.Logic;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedPolicyStore extends AbstractCachedStore implements PolicyStore {

    private static final String POLICY_CACHE_PREFIX = "pc-";

    private PolicyStore delegate;

    public CachedPolicyStore(InfinispanStoreFactoryProvider cacheStoreFactory, StoreFactory storeFactory) {
        super(cacheStoreFactory, storeFactory);
        this.delegate = storeFactory.getPolicyStore();
    }

    @Override
    public Policy create(AbstractPolicyRepresentation representation, ResourceServer resourceServer) {
        Policy policy = getDelegate().create(representation, getStoreFactory().getResourceServerStore().findById(resourceServer.getId()));
        String id = policy.getId();

        addInvalidation(getCacheKeyForPolicy(policy.getId()));
        addInvalidation(getCacheKeyForPolicyName(policy.getName()));
        addInvalidation(getCacheKeyForPolicyType(policy.getType()));

        configureTransaction(resourceServer, id);

        return createAdapter(new CachedPolicy(policy));
    }

    @Override
    public void delete(String id) {
        Policy policy = getDelegate().findById(id, null);
        if (policy == null) {
            return;
        }

        addInvalidation(getCacheKeyForPolicy(policy.getId()));
        addInvalidation(getCacheKeyForPolicyName(policy.getName()));
        addInvalidation(getCacheKeyForPolicyType(policy.getType()));

        getDelegate().delete(id);
        configureTransaction(policy.getResourceServer(), policy.getId());
    }

    @Override
    public Policy findById(String id, String resourceServerId) {
        if (resourceServerId == null) {
            return getDelegate().findById(id, null);
        }

        if (isInvalid(getCacheKeyForPolicy(id))) {
            return getDelegate().findById(id, resourceServerId);
        }

        String cacheKeyForPolicy = getCacheKeyForPolicy(id);
        List<Object> cached = resolveCacheEntry(resourceServerId, cacheKeyForPolicy);

        if (cached == null) {
            Policy policy = getDelegate().findById(id, resourceServerId);

            if (policy != null) {
                return createAdapter(putCacheEntry(resourceServerId, cacheKeyForPolicy, new CachedPolicy(policy)));
            }

            return null;
        }

        return createAdapter(CachedPolicy.class.cast(cached.get(0)));
    }

    @Override
    public Policy findByName(String name, String resourceServerId) {
        String cacheKey = getCacheKeyForPolicyName(name);

        if (isInvalid(cacheKey)) {
            return getDelegate().findByName(name, resourceServerId);
        }

        return cacheResult(resourceServerId, cacheKey, () -> {
            Policy policy = getDelegate().findByName(name, resourceServerId);

            if (policy == null) {
                return Collections.emptyList();
            }

            return Arrays.asList(policy);
        }).stream().findFirst().orElse(null);
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
        String cacheKey = getCacheKeyForResource(resourceId);

        if (isInvalid(cacheKey)) {
            return getDelegate().findByResource(resourceId, resourceServerId);
        }

        return cacheResult(resourceServerId, cacheKey, () -> getDelegate().findByResource(resourceId, resourceServerId));
    }

    @Override
    public List<Policy> findByResourceType(String resourceType, String resourceServerId) {
        String cacheKey = getCacheKeyForResourceType(resourceType);

        if (isInvalid(cacheKey)) {
            return getDelegate().findByResourceType(resourceType, resourceServerId);
        }

        return cacheResult(resourceServerId, cacheKey, () -> getDelegate().findByResourceType(resourceType, resourceServerId));
    }

    @Override
    public List<Policy> findByScopeIds(List<String> scopeIds, String resourceServerId) {
        List<Policy> policies = new ArrayList<>();

        for (String scopeId : scopeIds) {
            String cacheKey = getCacheForScope(scopeId);

            if (isInvalid(cacheKey)) {
                policies.addAll(getDelegate().findByScopeIds(Arrays.asList(scopeId), resourceServerId));
            } else {
                policies.addAll(cacheResult(resourceServerId, cacheKey, () -> getDelegate().findByScopeIds(Arrays.asList(scopeId), resourceServerId)));
            }
        }

        return policies;
    }

    @Override
    public List<Policy> findByType(String type, String resourceServerId) {
        String cacheKey = getCacheKeyForPolicyType(type);

        if (isInvalid(cacheKey)) {
            return getDelegate().findByType(type, resourceServerId);
        }

        return cacheResult(resourceServerId, cacheKey, () -> getDelegate().findByType(type, resourceServerId));
    }

    @Override
    public List<Policy> findDependentPolicies(String id, String resourceServerId) {
        return getDelegate().findDependentPolicies(id, resourceServerId);
    }

    private String getCacheKeyForPolicy(String id) {
        return new StringBuilder().append(POLICY_CACHE_PREFIX).append("id-").append(id).toString();
    }

    private String getCacheKeyForPolicyType(String type) {
        return new StringBuilder().append(POLICY_CACHE_PREFIX).append("findByType-").append(type).toString();
    }

    private String getCacheKeyForPolicyName(String name) {
        return new StringBuilder().append(POLICY_CACHE_PREFIX).append("findByName-").append(name).toString();
    }

    private String getCacheKeyForResourceType(String resourceType) {
        return new StringBuilder().append(POLICY_CACHE_PREFIX).append("findByResourceType-").append(resourceType).toString();
    }

    private String getCacheForScope(String scopeId) {
        return new StringBuilder().append(POLICY_CACHE_PREFIX).append("findByScopeIds-").append(scopeId).toString();
    }

    private String getCacheKeyForResource(String resourceId) {
        return new StringBuilder().append(POLICY_CACHE_PREFIX).append("findByResource-").append(resourceId).toString();
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
                return new HashMap<>(cached.getConfig());
            }

            @Override
            public void setConfig(Map<String, String> config) {
                String resourceType = config.get("defaultResourceType");

                if (resourceType != null) {
                    addInvalidation(getCacheKeyForResourceType(resourceType));
                    String cachedResourceType = cached.getConfig().get("defaultResourceType");
                    if (cachedResourceType != null && !resourceType.equals(cachedResourceType)) {
                        addInvalidation(getCacheKeyForResourceType(cachedResourceType));
                    }
                }

                getDelegateForUpdate().setConfig(config);
                cached.setConfig(config);
            }

            @Override
            public String getName() {
                return cached.getName();
            }

            @Override
            public void setName(String name) {
                addInvalidation(getCacheKeyForPolicyName(name));
                addInvalidation(getCacheKeyForPolicyName(cached.getName()));
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
                Scope model = getStoreFactory().getScopeStore().findById(scope.getId(), cached.getResourceServerId());
                addInvalidation(getCacheForScope(model.getId()));
                getDelegateForUpdate().addScope(model);
                cached.addScope(scope);
                scopes.add(scope);
            }

            @Override
            public void removeScope(Scope scope) {
                Scope model = getStoreFactory().getScopeStore().findById(scope.getId(), cached.getResourceServerId());
                addInvalidation(getCacheForScope(scope.getId()));
                getDelegateForUpdate().removeScope(model);
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
                Resource model = getStoreFactory().getResourceStore().findById(resource.getId(), cached.getResourceServerId());

                addInvalidation(getCacheKeyForResource(model.getId()));

                if (model.getType() != null) {
                    addInvalidation(getCacheKeyForResourceType(model.getType()));
                }

                getDelegateForUpdate().addResource(model);
                cached.addResource(resource);
                resources.add(resource);
            }

            @Override
            public void removeResource(Resource resource) {
                Resource model = getStoreFactory().getResourceStore().findById(resource.getId(), cached.getResourceServerId());

                addInvalidation(getCacheKeyForResource(model.getId()));

                if (model.getType() != null) {
                    addInvalidation(getCacheKeyForResourceType(model.getType()));
                }

                getDelegateForUpdate().removeResource(model);
                cached.removeResource(resource);
                resources.remove(resource);
            }

            @Override
            public Set<Policy> getAssociatedPolicies() {
                if (associatedPolicies == null || updated != null) {
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
                if (resources == null || updated != null) {
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
                if (scopes == null || updated != null) {
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
                    addInvalidation(getCacheKeyForPolicy(updated.getId()));
                    configureTransaction(updated.getResourceServer(), updated.getId());
                }

                return this.updated;
            }
        };
    }

    private List<Policy> cacheResult(String resourceServerId, String key, Supplier<List<Policy>> provider) {
        List<Object> cached = getCachedStoreFactory().computeIfCachedEntryAbsent(resourceServerId, key, (Function<String, List<Object>>) o -> {
            List<Policy> result = provider.get();

            if (result.isEmpty()) {
                return Collections.emptyList();
            }

            return result.stream().map(policy -> policy.getId()).collect(Collectors.toList());
        });

        if (cached == null) {
            return Collections.emptyList();
        }

        return cached.stream().map(id -> findById(id.toString(), resourceServerId)).collect(Collectors.toList());
    }

    private void configureTransaction(ResourceServer resourceServer, String id) {
        getTransaction().whenRollback(() -> removeCachedEntry(resourceServer.getId(), getCacheKeyForPolicy(id)));
        getTransaction().whenCommit(() -> invalidate(resourceServer.getId()));
    }

    private PolicyStore getDelegate() {
        return delegate;
    }

    void addInvalidations(Object object) {
        if (Resource.class.isInstance(object)) {
            Resource resource = (Resource) object;
            addInvalidation(getCacheKeyForResource(resource.getId()));
            String type = resource.getType();

            if (type != null) {
                addInvalidation(getCacheKeyForResourceType(type));
            }
        } else if (Scope.class.isInstance(object)) {
            Scope scope = (Scope) object;
            addInvalidation(getCacheForScope(scope.getId()));
        } else {
            throw new RuntimeException("Unexpected notification [" + object + "]");
        }
    }
}