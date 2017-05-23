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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedPolicy;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedResource;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedResourceServer;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedScope;
import org.keycloak.models.cache.infinispan.authorization.entities.PolicyListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PolicyQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PolicyResourceListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PolicyScopeListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.ResourceListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.ResourceQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.ResourceScopeListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.ResourceServerListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.ScopeListQuery;
import org.keycloak.models.cache.infinispan.authorization.events.PolicyRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.PolicyUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceServerRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceServerUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ScopeRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ScopeUpdatedEvent;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;

/**
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
public class StoreFactoryCacheSession implements CachedStoreFactoryProvider {
    protected static final Logger logger = Logger.getLogger(StoreFactoryCacheSession.class);

    protected StoreFactoryCacheManager cache;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;

    protected Map<String, ResourceServerAdapter> managedResourceServers = new HashMap<>();
    protected Map<String, ScopeAdapter> managedScopes = new HashMap<>();
    protected Map<String, ResourceAdapter> managedResources = new HashMap<>();
    protected Map<String, PolicyAdapter> managedPolicies = new HashMap<>();
    protected Set<String> invalidations = new HashSet<>();
    protected Set<InvalidationEvent> invalidationEvents = new HashSet<>(); // Events to be sent across cluster

    protected boolean clearAll;
    protected final long startupRevision;
    protected StoreFactory delegate;
    protected KeycloakSession session;
    protected ResourceServerCache resourceServerCache;
    protected ScopeCache scopeCache;
    protected ResourceCache resourceCache;
    protected PolicyCache policyCache;

    public StoreFactoryCacheSession(StoreFactoryCacheManager cache, KeycloakSession session) {
        this.cache = cache;
        this.startupRevision = cache.getCurrentCounter();
        this.session = session;
        this.resourceServerCache = new ResourceServerCache();
        this.scopeCache = new ScopeCache();
        this.resourceCache = new ResourceCache();
        this.policyCache = new PolicyCache();
        session.getTransactionManager().enlistPrepare(getPrepareTransaction());
        session.getTransactionManager().enlistAfterCompletion(getAfterTransaction());
    }

    @Override
    public ResourceServerStore getResourceServerStore() {
        return resourceServerCache;
    }

    @Override
    public ScopeStore getScopeStore() {
        return scopeCache;
    }

    @Override
    public ResourceStore getResourceStore() {
        return resourceCache;
    }

    @Override
    public PolicyStore getPolicyStore() {
        return policyCache;
    }

    public void close() {
    }

    private KeycloakTransaction getPrepareTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
                transactionActive = false;
            }

            @Override
            public void setRollbackOnly() {
                setRollbackOnly = true;
            }

            @Override
            public boolean getRollbackOnly() {
                return setRollbackOnly;
            }

            @Override
            public boolean isActive() {
                return transactionActive;
            }
        };
    }

    private KeycloakTransaction getAfterTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                try {
                    if (getDelegate() == null) return;
                    if (clearAll) {
                        cache.clear();
                    }
                    runInvalidations();
                    transactionActive = false;
                } finally {
                    cache.endRevisionBatch();
                }
            }

            @Override
            public void rollback() {
                try {
                    setRollbackOnly = true;
                    runInvalidations();
                    transactionActive = false;
                } finally {
                    cache.endRevisionBatch();
                }
            }

            @Override
            public void setRollbackOnly() {
                setRollbackOnly = true;
            }

            @Override
            public boolean getRollbackOnly() {
                return setRollbackOnly;
            }

            @Override
            public boolean isActive() {
                return transactionActive;
            }
        };
    }

    protected void runInvalidations() {
        for (String id : invalidations) {
            cache.invalidateObject(id);
        }

        cache.sendInvalidationEvents(session, invalidationEvents);
    }



    public long getStartupRevision() {
        return startupRevision;
    }

    public boolean isInvalid(String id) {
        return invalidations.contains(id);
    }

    public void registerResourceServerInvalidation(String id, String clientId) {
        cache.resourceServerUpdated(id, clientId, invalidations);
        ResourceServerAdapter adapter = managedResourceServers.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(ResourceServerUpdatedEvent.create(id, clientId));
    }

    public void registerScopeInvalidation(String id, String name, String serverId) {
        cache.scopeUpdated(id, name, serverId, invalidations);
        ScopeAdapter adapter = managedScopes.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(ScopeUpdatedEvent.create(id, name, serverId));
    }

    public void registerResourceInvalidation(String id, String name, String type, String uri, Set<String> scopes, String serverId) {
        cache.resourceUpdated(id, name, type, uri, scopes, serverId, invalidations);
        ResourceAdapter adapter = managedResources.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(ResourceUpdatedEvent.create(id, name, type, uri, scopes, serverId));
    }

    public void registerPolicyInvalidation(String id, String name, Set<String> resources, String serverId) {
        cache.policyUpdated(id, name, resources, serverId, invalidations);
        PolicyAdapter adapter = managedPolicies.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(PolicyUpdatedEvent.create(id, name, resources, serverId));
    }

    public ResourceServerStore getResourceServerStoreDelegate() {
        return getDelegate().getResourceServerStore();
    }

    public ScopeStore getScopeStoreDelegate() {
        return getDelegate().getScopeStore();
    }

    public ResourceStore getResourceStoreDelegate() {
        return getDelegate().getResourceStore();
    }

    public PolicyStore getPolicyStoreDelegate() {
        return getDelegate().getPolicyStore();
    }

    public static String getResourceServerByClientCacheKey(String clientId) {
        return "resource.server.client.id." + clientId;
    }

    public static String getScopeByNameCacheKey(String name, String serverId) {
        return "scope.name." + name + "." + serverId;
    }

    public static String getResourceByNameCacheKey(String name, String serverId) {
        return "resource.name." + name + "." + serverId;
    }

    public static String getResourceByOwnerCacheKey(String owner, String serverId) {
        return "resource.owner." + owner + "." + serverId;
    }

    public static String getResourceByTypeCacheKey(String type, String serverId) {
        return "resource.type." + type + "." + serverId;
    }

    public static String getResourceByUriCacheKey(String uri, String serverId) {
        return "resource.uri." + uri + "." + serverId;
    }

    public static String getResourceByScopeCacheKey(String scopeId, String serverId) {
        return "resource.scope." + scopeId + "." + serverId;
    }

    public static String getPolicyByNameCacheKey(String name, String serverId) {
        return "policy.name." + name + "." + serverId;
    }

    public static String getPolicyByResource(String resourceId, String serverId) {
        return "policy.resource." + resourceId + "." + serverId;
    }

    public static String getPolicyByResourceType(String type, String serverId) {
        return "policy.resource.type." + type + "." + serverId;
    }

    public static String getPolicyByScope(String scope, String serverId) {
        return "policy.scope." + scope + "." + serverId;
    }

    public StoreFactory getDelegate() {
        if (delegate != null) return delegate;
        delegate = session.getProvider(StoreFactory.class);
        return delegate;
    }


    protected class ResourceServerCache implements ResourceServerStore {
        @Override
        public ResourceServer create(String clientId) {
            ResourceServer server = getResourceServerStoreDelegate().create(clientId);
            registerResourceServerInvalidation(server.getId(), server.getClientId());
            return server;
        }

        @Override
        public void delete(String id) {
            if (id == null) return;
            ResourceServer server = findById(id);
            if (server == null) return;

            cache.invalidateObject(id);
            invalidationEvents.add(ResourceServerRemovedEvent.create(id, server.getClientId()));
            cache.resourceServerRemoval(id, server.getClientId(), invalidations);
            getResourceServerStoreDelegate().delete(id);

        }

       @Override
        public ResourceServer findById(String id) {
            if (id == null) return null;
            CachedResourceServer cached = cache.get(id, CachedResourceServer.class);
            if (cached != null) {
                logger.tracev("by id cache hit: {0}", cached.getId());
            }
            boolean wasCached = false;
            if (cached == null) {
                Long loaded = cache.getCurrentRevision(id);
                ResourceServer model = getResourceServerStoreDelegate().findById(id);
                if (model == null) return null;
                if (invalidations.contains(id)) return model;
                cached = new CachedResourceServer(loaded, model);
                cache.addRevisioned(cached, startupRevision);
                wasCached =true;
            } else if (invalidations.contains(id)) {
                return getResourceServerStoreDelegate().findById(id);
            } else if (managedResourceServers.containsKey(id)) {
                return managedResourceServers.get(id);
            }
            ResourceServerAdapter adapter = new ResourceServerAdapter(cached, StoreFactoryCacheSession.this);
             managedResourceServers.put(id, adapter);
            return adapter;
        }


        @Override
        public ResourceServer findByClient(String clientId) {
            String cacheKey = getResourceServerByClientCacheKey(clientId);
            ResourceServerListQuery query = cache.get(cacheKey, ResourceServerListQuery.class);
            if (query != null) {
                logger.tracev("ResourceServer by clientId cache hit: {0}", clientId);
            }
            if (query == null) {
                Long loaded = cache.getCurrentRevision(cacheKey);
                ResourceServer model = getResourceServerStoreDelegate().findByClient(clientId);
                if (model == null) return null;
                if (invalidations.contains(model.getId())) return model;
                query = new ResourceServerListQuery(loaded, cacheKey, model.getId());
                cache.addRevisioned(query, startupRevision);
                return model;
            } else if (invalidations.contains(cacheKey)) {
                return getResourceServerStoreDelegate().findByClient(clientId);
            } else {
                String serverId = query.getResourceServers().iterator().next();
                if (invalidations.contains(serverId)) {
                    return getResourceServerStoreDelegate().findByClient(clientId);
                }
                return findById(serverId);
            }
        }
    }

    protected class ScopeCache implements ScopeStore {
        @Override
        public Scope create(String name, ResourceServer resourceServer) {
            Scope scope = getScopeStoreDelegate().create(name, resourceServer);
            registerScopeInvalidation(scope.getId(), scope.getName(), resourceServer.getId());
            return scope;
        }

        @Override
        public void delete(String id) {
            if (id == null) return;
            Scope scope = findById(id, null);
            if (scope == null) return;

            cache.invalidateObject(id);
            invalidationEvents.add(ScopeRemovedEvent.create(id, scope.getName(), scope.getResourceServer().getId()));
            cache.scopeRemoval(id, scope.getName(), scope.getResourceServer().getId(), invalidations);
            getScopeStoreDelegate().delete(id);
        }

        @Override
        public Scope findById(String id, String resourceServerId) {
            if (id == null) return null;
            CachedScope cached = cache.get(id, CachedScope.class);
            if (cached != null) {
                logger.tracev("by id cache hit: {0}", cached.getId());
            }
            boolean wasCached = false;
            if (cached == null) {
                Long loaded = cache.getCurrentRevision(id);
                Scope model = getScopeStoreDelegate().findById(id, resourceServerId);
                if (model == null) return null;
                if (invalidations.contains(id)) return model;
                cached = new CachedScope(loaded, model);
                cache.addRevisioned(cached, startupRevision);
                wasCached =true;
            } else if (invalidations.contains(id)) {
                return getScopeStoreDelegate().findById(id, resourceServerId);
            } else if (managedScopes.containsKey(id)) {
                return managedScopes.get(id);
            }
            ScopeAdapter adapter = new ScopeAdapter(cached, StoreFactoryCacheSession.this);
            managedScopes.put(id, adapter);
            return adapter;
        }

        @Override
        public Scope findByName(String name, String resourceServerId) {
            if (name == null) return null;
            String cacheKey = getScopeByNameCacheKey(name, resourceServerId);
            ScopeListQuery query = cache.get(cacheKey, ScopeListQuery.class);
            if (query != null) {
                logger.tracev("scope by name cache hit: {0}", name);
            }
            if (query == null) {
                Long loaded = cache.getCurrentRevision(cacheKey);
                Scope model = getScopeStoreDelegate().findByName(name, resourceServerId);
                if (model == null) return null;
                if (invalidations.contains(model.getId())) return model;
                query = new ScopeListQuery(loaded, cacheKey, model.getId(), resourceServerId);
                cache.addRevisioned(query, startupRevision);
                return model;
            } else if (invalidations.contains(cacheKey)) {
                return getScopeStoreDelegate().findByName(name, resourceServerId);
            } else {
                String id = query.getScopes().iterator().next();
                if (invalidations.contains(id)) {
                    return getScopeStoreDelegate().findByName(name, resourceServerId);
                }
                return findById(id, query.getResourceServerId());
            }
        }

        @Override
        public List<Scope> findByResourceServer(String id) {
            return getScopeStoreDelegate().findByResourceServer(id);
        }

        @Override
        public List<Scope> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
            return getScopeStoreDelegate().findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
        }
    }

    protected class ResourceCache implements ResourceStore {
        @Override
        public Resource create(String name, ResourceServer resourceServer, String owner) {
            Resource resource = getResourceStoreDelegate().create(name, resourceServer, owner);
            registerResourceInvalidation(resource.getId(), resource.getName(), resource.getType(), resource.getUri(), resource.getScopes().stream().map(scope -> scope.getId()).collect(Collectors.toSet()), resourceServer.getId());
            return resource;
        }

        @Override
        public void delete(String id) {
            if (id == null) return;
            Resource resource = findById(id, null);
            if (resource == null) return;

            cache.invalidateObject(id);
            invalidationEvents.add(ResourceRemovedEvent.create(id, resource.getName(), resource.getType(), resource.getUri(), resource.getOwner(), resource.getScopes().stream().map(scope -> scope.getId()).collect(Collectors.toSet()), resource.getResourceServer().getId()));
            cache.resourceRemoval(id, resource.getName(), resource.getType(), resource.getUri(), resource.getOwner(), resource.getScopes().stream().map(scope -> scope.getId()).collect(Collectors.toSet()), resource.getResourceServer().getId(), invalidations);
            getResourceStoreDelegate().delete(id);

        }

        @Override
        public Resource findById(String id, String resourceServerId) {
            if (id == null) return null;
            CachedResource cached = cache.get(id, CachedResource.class);
            if (cached != null) {
                logger.tracev("by id cache hit: {0}", cached.getId());
            }
            boolean wasCached = false;
            if (cached == null) {
                Long loaded = cache.getCurrentRevision(id);
                Resource model = getResourceStoreDelegate().findById(id, resourceServerId);
                if (model == null) return null;
                if (invalidations.contains(id)) return model;
                cached = new CachedResource(loaded, model);
                cache.addRevisioned(cached, startupRevision);
                wasCached =true;
            } else if (invalidations.contains(id)) {
                return getResourceStoreDelegate().findById(id, resourceServerId);
            } else if (managedResources.containsKey(id)) {
                return managedResources.get(id);
            }
            ResourceAdapter adapter = new ResourceAdapter(cached, StoreFactoryCacheSession.this);
            managedResources.put(id, adapter);
            return adapter;
        }

        @Override
        public Resource findByName(String name, String resourceServerId) {
            if (name == null) return null;
            String cacheKey = getResourceByNameCacheKey(name, resourceServerId);
            List<Resource> result = cacheQuery(cacheKey, ResourceListQuery.class, () -> {
                Resource resource = getResourceStoreDelegate().findByName(name, resourceServerId);

                if (resource == null) {
                    return Collections.emptyList();
                }

                return Arrays.asList(resource);
            },
                    (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(resource -> resource.getId()).collect(Collectors.toSet()), resourceServerId), resourceServerId);

            if (result.isEmpty()) {
                return null;
            }

            return result.get(0);
        }

        @Override
        public List<Resource> findByOwner(String ownerId, String resourceServerId) {
            String cacheKey = getResourceByOwnerCacheKey(ownerId, resourceServerId);
            return cacheQuery(cacheKey, ResourceListQuery.class, () -> getResourceStoreDelegate().findByOwner(ownerId, resourceServerId),
                    (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(resource -> resource.getId()).collect(Collectors.toSet()), resourceServerId), resourceServerId);
        }

        @Override
        public List<Resource> findByUri(String uri, String resourceServerId) {
            if (uri == null) return null;
            String cacheKey = getResourceByUriCacheKey(uri, resourceServerId);
            return cacheQuery(cacheKey, ResourceListQuery.class, () -> getResourceStoreDelegate().findByUri(uri, resourceServerId),
                    (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(resource -> resource.getId()).collect(Collectors.toSet()), resourceServerId), resourceServerId);
        }

        @Override
        public List<Resource> findByResourceServer(String resourceServerId) {
            return getResourceStoreDelegate().findByResourceServer(resourceServerId);
        }

        @Override
        public List<Resource> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
            return getResourceStoreDelegate().findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
        }

        @Override
        public List<Resource> findByScope(List<String> ids, String resourceServerId) {
            if (ids == null) return null;
            List<Resource> result = new ArrayList<>();

            for (String id : ids) {
                String cacheKey = getResourceByScopeCacheKey(id, resourceServerId);
                result.addAll(cacheQuery(cacheKey, ResourceScopeListQuery.class, () -> getResourceStoreDelegate().findByScope(Arrays.asList(id), resourceServerId), (revision, resources) -> new ResourceScopeListQuery(revision, cacheKey, id, resources.stream().map(resource -> resource.getId()).collect(Collectors.toSet()), resourceServerId), resourceServerId));
            }

            return result;
        }

         @Override
        public List<Resource> findByType(String type, String resourceServerId) {
             if (type == null) return null;
             String cacheKey = getResourceByTypeCacheKey(type, resourceServerId);
             return cacheQuery(cacheKey, ResourceListQuery.class, () -> getResourceStoreDelegate().findByType(type, resourceServerId),
                     (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(resource -> resource.getId()).collect(Collectors.toSet()), resourceServerId), resourceServerId);
        }

        private <R, Q extends ResourceQuery> List<R> cacheQuery(String cacheKey, Class<Q> queryType, Supplier<List<R>> resultSupplier, BiFunction<Long, List<R>, Q> querySupplier, String resourceServerId) {
            Q query = cache.get(cacheKey, queryType);
            if (query != null) {
                logger.tracev("cache hit for key: {0}", cacheKey);
            }
            if (query == null) {
                Long loaded = cache.getCurrentRevision(cacheKey);
                List<R> model = resultSupplier.get();
                if (model == null) return null;
                if (invalidations.contains(cacheKey)) return model;
                query = querySupplier.apply(loaded, model);
                cache.addRevisioned(query, startupRevision);
                return model;
            } else if (query.isInvalid(invalidations)) {
                return resultSupplier.get();
            } else {
                return query.getResources().stream().map(resourceId -> (R) findById(resourceId, resourceServerId)).collect(Collectors.toList());
            }
        }
    }

    protected class PolicyCache implements PolicyStore {
        @Override
        public Policy create(AbstractPolicyRepresentation representation, ResourceServer resourceServer) {
            Policy policy = getPolicyStoreDelegate().create(representation, resourceServer);
            registerPolicyInvalidation(policy.getId(), policy.getName(), policy.getResources().stream().map(resource1 -> resource1.getId()).collect(Collectors.toSet()), resourceServer.getId());
            return policy;
        }

        @Override
        public void delete(String id) {
            if (id == null) return;
            Policy policy = findById(id, null);
            if (policy == null) return;

            cache.invalidateObject(id);
            invalidationEvents.add(PolicyRemovedEvent.create(id, policy.getName(), policy.getResources().stream().map(resource1 -> resource1.getId()).collect(Collectors.toSet()), policy.getResourceServer().getId()));
            cache.policyRemoval(id, policy.getName(), policy.getResources().stream().map(resource1 -> resource1.getId()).collect(Collectors.toSet()), policy.getResourceServer().getId(), invalidations);
            getPolicyStoreDelegate().delete(id);

        }

        @Override
        public Policy findById(String id, String resourceServerId) {
            if (id == null) return null;

            CachedPolicy cached = cache.get(id, CachedPolicy.class);
            if (cached != null) {
                logger.tracev("by id cache hit: {0}", cached.getId());
            }
            boolean wasCached = false;
            if (cached == null) {
                Long loaded = cache.getCurrentRevision(id);
                Policy model = getPolicyStoreDelegate().findById(id, resourceServerId);
                if (model == null) return null;
                if (invalidations.contains(id)) return model;
                cached = new CachedPolicy(loaded, model);
                cache.addRevisioned(cached, startupRevision);
                wasCached =true;
            } else if (invalidations.contains(id)) {
                return getPolicyStoreDelegate().findById(id, resourceServerId);
            } else if (managedPolicies.containsKey(id)) {
                return managedPolicies.get(id);
            }
            PolicyAdapter adapter = new PolicyAdapter(cached, StoreFactoryCacheSession.this);
            managedPolicies.put(id, adapter);
            return adapter;
        }

        @Override
        public Policy findByName(String name, String resourceServerId) {
            if (name == null) return null;
            String cacheKey = getPolicyByNameCacheKey(name, resourceServerId);
            List<Policy> result = cacheQuery(cacheKey, PolicyListQuery.class, () -> {
                Policy policy = getPolicyStoreDelegate().findByName(name, resourceServerId);

                if (policy == null) {
                    return Collections.emptyList();
                }

                return Arrays.asList(policy);
            },
                    (revision, policies) -> new PolicyListQuery(revision, cacheKey, policies.stream().map(policy -> policy.getId()).collect(Collectors.toSet()), resourceServerId), resourceServerId);

            if (result.isEmpty()) {
                return null;
            }

            return result.get(0);
        }

        @Override
        public List<Policy> findByResourceServer(String resourceServerId) {
            return getPolicyStoreDelegate().findByResourceServer(resourceServerId);
        }

        @Override
        public List<Policy> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
            return getPolicyStoreDelegate().findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
        }

        @Override
        public List<Policy> findByResource(String resourceId, String resourceServerId) {
            String cacheKey = getPolicyByResource(resourceId, resourceServerId);
            return cacheQuery(cacheKey, PolicyResourceListQuery.class, () -> getPolicyStoreDelegate().findByResource(resourceId, resourceServerId),
                    (revision, policies) -> new PolicyResourceListQuery(revision, cacheKey, resourceId, policies.stream().map(policy -> policy.getId()).collect(Collectors.toSet()), resourceServerId), resourceServerId);
        }

        @Override
        public List<Policy> findByResourceType(String resourceType, String resourceServerId) {
            String cacheKey = getPolicyByResourceType(resourceType, resourceServerId);
            return cacheQuery(cacheKey, PolicyResourceListQuery.class, () -> getPolicyStoreDelegate().findByResourceType(resourceType, resourceServerId),
                    (revision, policies) -> new PolicyResourceListQuery(revision, cacheKey, resourceType, policies.stream().map(policy -> policy.getId()).collect(Collectors.toSet()), resourceServerId), resourceServerId);
        }

        @Override
        public List<Policy> findByScopeIds(List<String> scopeIds, String resourceServerId) {
            if (scopeIds == null) return null;
            List<Policy> result = new ArrayList<>();

            for (String id : scopeIds) {
                String cacheKey = getPolicyByScope(id, resourceServerId);
                result.addAll(cacheQuery(cacheKey, PolicyScopeListQuery.class, () -> getPolicyStoreDelegate().findByScopeIds(Arrays.asList(id), resourceServerId), (revision, resources) -> new PolicyScopeListQuery(revision, cacheKey, id, resources.stream().map(resource -> resource.getId()).collect(Collectors.toSet()), resourceServerId), resourceServerId));
            }

            return result;
        }

        @Override
        public List<Policy> findByType(String type, String resourceServerId) {
            return getPolicyStoreDelegate().findByType(type, resourceServerId);
        }

        @Override
        public List<Policy> findDependentPolicies(String id, String resourceServerId) {
            return getPolicyStoreDelegate().findDependentPolicies(id, resourceServerId);
        }

        private <R, Q extends PolicyQuery> List<R> cacheQuery(String cacheKey, Class<Q> queryType, Supplier<List<R>> resultSupplier, BiFunction<Long, List<R>, Q> querySupplier, String resourceServerId) {
            Q query = cache.get(cacheKey, queryType);
            if (query != null) {
                logger.tracev("cache hit for key: {0}", cacheKey);
            }
            if (query == null) {
                Long loaded = cache.getCurrentRevision(cacheKey);
                List<R> model = resultSupplier.get();
                if (model == null) return null;
                if (invalidations.contains(cacheKey)) return model;
                query = querySupplier.apply(loaded, model);
                cache.addRevisioned(query, startupRevision);
                return model;
            } else if (query.isInvalid(invalidations)) {
                return resultSupplier.get();
            } else {
                return query.getPolicies().stream().map(resourceId -> (R) findById(resourceId, resourceServerId)).collect(Collectors.toList());
            }
        }
    }


}
