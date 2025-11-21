/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.authorization.UserManagedPermissionUtil;
import org.keycloak.authorization.model.PermissionTicket;
import org.keycloak.authorization.model.Policy;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.PermissionTicketStore;
import org.keycloak.authorization.store.PolicyStore;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.ModelException;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedPermissionTicket;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedPolicy;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedResource;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedResourceServer;
import org.keycloak.models.cache.infinispan.authorization.entities.CachedScope;
import org.keycloak.models.cache.infinispan.authorization.entities.PermissionTicketListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PermissionTicketQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PermissionTicketResourceListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PermissionTicketScopeListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PolicyListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PolicyQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PolicyResourceListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.PolicyScopeListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.ResourceListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.ResourceQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.ResourceScopeListQuery;
import org.keycloak.models.cache.infinispan.authorization.entities.ScopeListQuery;
import org.keycloak.models.cache.infinispan.authorization.events.PermissionTicketRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.PermissionTicketUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.PolicyRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.PolicyUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceServerRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceServerUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ResourceUpdatedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ScopeRemovedEvent;
import org.keycloak.models.cache.infinispan.authorization.events.ScopeUpdatedEvent;
import org.keycloak.models.cache.infinispan.entities.NonExistentItem;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;
import org.keycloak.representations.idm.authorization.AbstractPolicyRepresentation;
import org.keycloak.storage.StorageId;

import org.jboss.logging.Logger;

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
    protected Map<String, PermissionTicketAdapter> managedPermissionTickets = new HashMap<>();
    protected Set<String> invalidations = new HashSet<>();
    protected Set<InvalidationEvent> invalidationEvents = new HashSet<>(); // Events to be sent across cluster

    protected final long startupRevision;
    protected StoreFactory delegate;
    protected KeycloakSession session;
    protected ResourceServerCache resourceServerCache;
    protected ScopeCache scopeCache;
    protected ResourceCache resourceCache;
    protected PolicyCache policyCache;
    protected PermissionTicketCache permissionTicketCache;

    public StoreFactoryCacheSession(StoreFactoryCacheManager cache, KeycloakSession session) {
        this.cache = cache;
        this.startupRevision = cache.getCurrentCounter();
        this.session = session;
        this.resourceServerCache = new ResourceServerCache();
        this.scopeCache = new ScopeCache();
        this.resourceCache = new ResourceCache();
        this.policyCache = new PolicyCache();
        this.permissionTicketCache = new PermissionTicketCache();
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

    @Override
    public PermissionTicketStore getPermissionTicketStore() {
        return permissionTicketCache;
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        getDelegate().setReadOnly(readOnly);
    }

    @Override
    public boolean isReadOnly() {
        return getDelegate().isReadOnly();
    }

    public void close() {
        if (delegate != null) {
            delegate.close();
        }
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

        cache.sendInvalidationEvents(session, invalidationEvents, InfinispanCacheStoreFactoryProviderFactory.AUTHORIZATION_INVALIDATION_EVENTS);
    }

    public void registerResourceServerInvalidation(String id) {
        cache.resourceServerUpdated(id, invalidations);
        ResourceServerAdapter adapter = managedResourceServers.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(ResourceServerUpdatedEvent.create(id));
    }

    public void registerScopeInvalidation(String id, String name, String serverId) {
        cache.scopeUpdated(id, name, serverId, invalidations);
        ScopeAdapter adapter = managedScopes.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(ScopeUpdatedEvent.create(id, name, serverId));
    }

    public void registerResourceInvalidation(String id, String name, String type, Set<String> uris, Set<String> scopes, String serverId, String owner) {
        cache.resourceUpdated(id, name, type, uris, scopes, serverId, owner, invalidations);
        ResourceAdapter adapter = managedResources.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(ResourceUpdatedEvent.create(id, name, type, uris, owner, scopes, serverId));
    }

    public void registerPolicyInvalidation(String id, String name, Set<String> resources, Set<String> scopes, String defaultResourceType, String serverId) {
        Set<String> resourceTypes = getResourceTypes(resources, serverId);
        if (Objects.nonNull(defaultResourceType)) {
            resourceTypes.add(defaultResourceType);
        }
        cache.policyUpdated(id, name, resources, resourceTypes, scopes, serverId, invalidations);
        PolicyAdapter adapter = managedPolicies.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(PolicyUpdatedEvent.create(id, name, resources, resourceTypes, scopes, serverId));
    }

    public void registerPermissionTicketInvalidation(String id, String owner, String requester, String resource, String resourceName, String scope, String serverId) {
        cache.permissionTicketUpdated(id, owner, requester, resource, resourceName, scope, serverId, invalidations);
        PermissionTicketAdapter adapter = managedPermissionTickets.get(id);
        if (adapter != null) adapter.invalidateFlag();

        invalidationEvents.add(PermissionTicketUpdatedEvent.create(id, owner, requester, resource, resourceName, scope, serverId));
    }

    private Set<String> getResourceTypes(Set<String> resources, String serverId) {
        if (resources == null) {
            return Collections.emptySet();
        }

        ResourceServer resourceServer = getResourceServerStore().findById(serverId);
        return resources.stream().map(resourceId -> {
            Resource resource = getResourceStore().findById(resourceServer, resourceId);

            if (resource == null) {
                return null;
            }
            return resource.getType();

        }).filter(Objects::nonNull).collect(Collectors.toSet());
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

    public PermissionTicketStore getPermissionTicketStoreDelegate() {
        return getDelegate().getPermissionTicketStore();
    }

    public static String getResourceServerByClientCacheKey(String clientId) {
        return "resource.server.client.id." + clientId;
    }

    public static String getScopeByNameCacheKey(String name, String serverId) {
        return "scope.name." + name + "." + serverId;
    }

    public static String getResourceByNameCacheKey(String name, String ownerId, String serverId) {
        return "resource.name." + name + "." + ownerId + "." + serverId;
    }

    public static String getResourceByOwnerCacheKey(String owner, String serverId) {
        return "resource.owner." + owner + "." + serverId;
    }

    public static String getResourceByTypeCacheKey(String type, String serverId) {
        return "resource.type." + type + "." + serverId;
    }

    public static String getResourceByTypeCacheKey(String type, String owner, String serverId) {
        return "resource.type." + type + ".owner." + owner + "." + serverId;
    }

    public static String getResourceByTypeInstanceCacheKey(String type, String serverId) {
        return "resource.type.instance." + type + "." + serverId;
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

    public static String getPolicyByResourceScope(String scope, String resourceId, String serverId) {
        return "policy.resource. " + resourceId + ".scope." + scope + "." + serverId;
    }

    public static String getPermissionTicketByResource(String resourceId, String serverId) {
        return "permission.ticket.resource." + resourceId + "." + serverId;
    }

    public static String getPermissionTicketByScope(String scopeId, String serverId) {
        return "permission.ticket.scope." + scopeId + "." + serverId;
    }

    public static String getPermissionTicketByGranted(String userId, String serverId) {
        return "permission.ticket.granted." + userId + "." + serverId;
    }

    public static String getPermissionTicketByResourceNameAndGranted(String resourceName, String userId, String serverId) {
        return "permission.ticket.granted." + resourceName + "." + userId + "." + serverId;
    }

    public static String getPermissionTicketByOwner(String owner, String serverId) {
        return "permission.ticket.owner." + owner + "." + serverId;
    }

    public StoreFactory getDelegate() {
        if (delegate != null) return delegate;
        delegate = session.getProvider(StoreFactory.class);
        return delegate;
    }

    private void setModelDoesNotExists(String id, Long loaded) {
        if (! invalidations.contains(id)) {
            cache.addRevisioned(new NonExistentItem(id, loaded), startupRevision);
        }
    }

    boolean modelMightExist(String id) {
        return invalidations.contains(id) || cache.get(id, NonExistentItem.class) == null;
    }

    protected class ResourceServerCache implements ResourceServerStore {
        @Override
        public ResourceServer create(ClientModel client) {
            String clientId = client.getId();
            if (!StorageId.isLocalStorage(clientId)) {
                throw new ModelException("Creating resource server from federated ClientModel not supported");
            }
            ResourceServer server = getResourceServerStoreDelegate().create(client);
            registerResourceServerInvalidation(server.getId());
            return server;
        }

        @Override
        public void delete(ClientModel client) {
            String id = client.getId();
            if (id == null) return;
            ResourceServer server = findById(id);
            if (server == null) return;

            cache.invalidateObject(id);
            invalidationEvents.add(ResourceServerRemovedEvent.create(id));
            cache.resourceServerRemoval(id, invalidations);
            getResourceServerStoreDelegate().delete(client);

        }

        @Override
        public ResourceServer findById(String id) {
            if (id == null) return null;
            CachedResourceServer cached = cache.get(id, CachedResourceServer.class);
            if (cached != null) {
                logger.tracev("by id cache hit: {0}", cached.getId());
            }

            if (cached == null) {
                Long loaded = cache.getCurrentRevision(id);
                if (! modelMightExist(id)) return null;
                ResourceServer model = getResourceServerStoreDelegate().findById(id);
                if (model == null) {
                    setModelDoesNotExists(id, loaded);
                    return null;
                }
                if (invalidations.contains(id)) return model;
                cached = new CachedResourceServer(loaded, model);
                cache.addRevisioned(cached, startupRevision);
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
        public ResourceServer findByClient(ClientModel client) {
            return findById(client.getId());
        }
    }

    protected class ScopeCache implements ScopeStore {
        @Override
        public Scope create(ResourceServer resourceServer, String name) {
            return create(resourceServer, null, name);
        }

        @Override
        public Scope create(ResourceServer resourceServer, String id, String name) {
            Scope scope = getScopeStoreDelegate().create(resourceServer, id, name);
            registerScopeInvalidation(scope.getId(), scope.getName(), resourceServer.getId());
            return scope;
        }

        @Override
        public void delete(String id) {
            if (id == null) return;
            Scope scope = findById(null, id);
            if (scope == null) return;

            cache.invalidateObject(id);
            invalidationEvents.add(ScopeRemovedEvent.create(id, scope.getName(), scope.getResourceServer().getId()));
            cache.scopeRemoval(id, scope.getName(), scope.getResourceServer().getId(), invalidations);
            getScopeStoreDelegate().delete(id);
        }

        @Override
        public Scope findById(ResourceServer resourceServer, String id) {
            if (id == null) return null;
            CachedScope cached = cache.get(id, CachedScope.class);
            if (cached != null) {
                logger.tracev("by id cache hit: {0}", cached.getId());
            }
            if (cached == null) {
                Long loaded = cache.getCurrentRevision(id);
                if (! modelMightExist(id)) return null;
                Scope model = getScopeStoreDelegate().findById(resourceServer, id);
                if (model == null) {
                    setModelDoesNotExists(id, loaded);
                    return null;
                }
                if (invalidations.contains(id)) return model;
                cached = new CachedScope(loaded, model);
                cache.addRevisioned(cached, startupRevision);
            } else if (invalidations.contains(id)) {
                return getScopeStoreDelegate().findById(resourceServer, id);
            } else if (managedScopes.containsKey(id)) {
                return managedScopes.get(id);
            }
            ScopeAdapter adapter = new ScopeAdapter(cached, StoreFactoryCacheSession.this);
            managedScopes.put(id, adapter);
            return adapter;
        }

        @Override
        public Scope findByName(ResourceServer resourceServer, String name) {
            if (name == null) return null;
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getScopeByNameCacheKey(name, resourceServerId);
            ScopeListQuery query = cache.get(cacheKey, ScopeListQuery.class);
            if (query != null) {
                logger.tracev("scope by name cache hit: {0}", name);
            }
            if (query == null) {
                Long loaded = cache.getCurrentRevision(cacheKey);
                Scope model = getScopeStoreDelegate().findByName(resourceServer, name);
                if (model == null) return null;
                if (invalidations.contains(model.getId())) return model;
                query = new ScopeListQuery(loaded, cacheKey, model.getId(), resourceServerId);
                cache.addRevisioned(query, startupRevision);
                return model;
            } else if (invalidations.contains(cacheKey)) {
                return getScopeStoreDelegate().findByName(resourceServer, name);
            } else {
                String id = query.getScopes().iterator().next();
                if (invalidations.contains(id)) {
                    return getScopeStoreDelegate().findByName(resourceServer, name);
                }
                return findById(resourceServer, id);
            }
        }

        @Override
        public List<Scope> findByResourceServer(ResourceServer resourceServer) {
            return getScopeStoreDelegate().findByResourceServer(resourceServer);
        }

        @Override
        public List<Scope> findByResourceServer(ResourceServer resourceServer, Map<Scope.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
            return getScopeStoreDelegate().findByResourceServer(resourceServer, attributes, firstResult, maxResults);
        }
    }

    protected class ResourceCache implements ResourceStore {

        @Override
        public Resource create(ResourceServer resourceServer, String id, String name, String owner) {
            Resource resource = getResourceStoreDelegate().create(resourceServer, id, name, owner);
            Resource cached = findById(resourceServer, resource.getId());
            registerResourceInvalidation(resource.getId(), resource.getName(), resource.getType(), resource.getUris(), resource.getScopes().stream().map(Scope::getId).collect(Collectors.toSet()), resourceServer.getId(), resource.getOwner());
            if (cached == null) {
                cached = findById(resourceServer, resource.getId());
            }
            return cached;
        }

        @Override
        public void delete(String id) {
            if (id == null) return;
            Resource resource = findById(null, id);
            if (resource == null) return;

            cache.invalidateObject(id);
            invalidationEvents.add(ResourceRemovedEvent.create(id, resource.getName(), resource.getType(), resource.getUris(), resource.getOwner(), resource.getScopes().stream().map(Scope::getId).collect(Collectors.toSet()), resource.getResourceServer().getId()));
            cache.resourceRemoval(id, resource.getName(), resource.getType(), resource.getUris(), resource.getOwner(), resource.getScopes().stream().map(Scope::getId).collect(Collectors.toSet()), resource.getResourceServer().getId(), invalidations);
            getResourceStoreDelegate().delete(id);

        }

        @Override
        public Resource findById(ResourceServer resourceServer, String id) {
            if (id == null) return null;
            CachedResource cached = cache.get(id, CachedResource.class);
            if (cached != null) {
                logger.tracev("by id cache hit: {0}", cached.getId());
            }
            if (cached == null) {
                Long loaded = cache.getCurrentRevision(id);
                if (! modelMightExist(id)) return null;
                Resource model = getResourceStoreDelegate().findById(resourceServer, id);
                if (model == null) {
                    setModelDoesNotExists(id, loaded);
                    return null;
                }
                if (invalidations.contains(id)) return model;
                cached = new CachedResource(loaded, model);
                cache.addRevisioned(cached, startupRevision);
            } else if (invalidations.contains(id)) {
                return getResourceStoreDelegate().findById(resourceServer, id);
            } else if (managedResources.containsKey(id)) {
                return managedResources.get(id);
            }
            ResourceAdapter adapter = new ResourceAdapter(cached, StoreFactoryCacheSession.this);
            managedResources.put(id, adapter);
            return adapter;
        }

        @Override
        public Resource findByName(ResourceServer resourceServer, String name, String ownerId) {
            if (name == null) return null;
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getResourceByNameCacheKey(name, ownerId, resourceServerId);
            List<Resource> result = cacheQuery(cacheKey, ResourceListQuery.class, () -> {
                        Resource resource = getResourceStoreDelegate().findByName(resourceServer, name, ownerId);

                        if (resource == null) {
                            return Collections.emptyList();
                        }

                        return Arrays.asList(resource);
                    },
                    (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(Resource::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);

            if (result.isEmpty()) {
                return null;
            }

            return result.get(0);
        }

        @Override
        public List<Resource> findByOwner(ResourceServer resourceServer, String ownerId) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getResourceByOwnerCacheKey(ownerId, resourceServerId);
            return cacheQuery(cacheKey, ResourceListQuery.class, () -> getResourceStoreDelegate().findByOwner(resourceServer, ownerId),
                    (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(Resource::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);
        }

        @Override
        public void findByOwner(ResourceServer resourceServer, String ownerId, Consumer<Resource> consumer) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getResourceByOwnerCacheKey(ownerId, resourceServerId);
            cacheQuery(cacheKey, ResourceListQuery.class, () -> {
                        List<Resource> resources = new ArrayList<>();
                        getResourceStoreDelegate().findByOwner(resourceServer, ownerId, new Consumer<Resource>() {
                            @Override
                            public void accept(Resource resource) {
                                consumer.andThen(resources::add)
                                        .andThen(StoreFactoryCacheSession.this::cacheResource)
                                        .accept(resource);
                            }
                        });
                        return resources;
                    },
                    (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(Resource::getId).collect(Collectors.toSet()), resourceServerId), resourceServer, consumer);
        }

        @Override
        public List<Resource> findByResourceServer(ResourceServer resourceServer) {
            return getResourceStoreDelegate().findByResourceServer(resourceServer);
        }

        @Override
        public List<Resource> find(ResourceServer resourceServer, Map<Resource.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
            return getResourceStoreDelegate().find(resourceServer, attributes, firstResult, maxResults);
        }

        @Override
        public List<Resource> findByScopes(ResourceServer resourceServer, Set<Scope> scopes) {
            if (scopes == null) return null;
            List<Resource> result = new ArrayList<>();
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();

            for (Scope scope : scopes) {
                String cacheKey = getResourceByScopeCacheKey(scope.getId(), resourceServerId);
                result.addAll(cacheQuery(cacheKey, ResourceScopeListQuery.class, () -> getResourceStoreDelegate().findByScopes(resourceServer, Collections.singleton(scope)), (revision, resources) -> new ResourceScopeListQuery(revision, cacheKey, scope.getId(), resources.stream().map(Resource::getId).collect(Collectors.toSet()), resourceServerId), resourceServer));
            }

            return result;
        }

        @Override
        public void findByScopes(ResourceServer resourceServer, Set<Scope> scopes, Consumer<Resource> consumer) {
            if (scopes == null) return;
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();

            for (Scope scope : scopes) {
                String cacheKey = getResourceByScopeCacheKey(scope.getId(), resourceServerId);
                cacheQuery(cacheKey, ResourceScopeListQuery.class, () -> {
                    List<Resource> resources = new ArrayList<>();
                    getResourceStoreDelegate().findByScopes(resourceServer, Collections.singleton(scope), new Consumer<Resource>() {
                        @Override
                        public void accept(Resource resource) {
                            consumer.andThen(resources::add)
                                    .andThen(StoreFactoryCacheSession.this::cacheResource)
                                    .accept(resource);
                            
                        }
                    });
                    return resources;
                }, (revision, resources) -> new ResourceScopeListQuery(revision, cacheKey, scope.getId(), resources.stream().map(Resource::getId).collect(Collectors.toSet()), resourceServerId), resourceServer, consumer);
            }
        }

        @Override
        public List<Resource> findByType(ResourceServer resourceServer, String type) {
             if (type == null) return Collections.emptyList();
             String resourceServerId = resourceServer == null ? null : resourceServer.getId();
             String cacheKey = getResourceByTypeCacheKey(type, resourceServerId);
             return cacheQuery(cacheKey, ResourceListQuery.class, () -> getResourceStoreDelegate().findByType(resourceServer, type),
                     (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(Resource::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);
        }

        @Override
        public void findByType(ResourceServer resourceServer, String type, Consumer<Resource> consumer) {
            if (type == null) return;
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getResourceByTypeCacheKey(type, resourceServerId);
            cacheQuery(cacheKey, ResourceListQuery.class, () -> {
                        List<Resource> resources = new ArrayList<>();
                        getResourceStoreDelegate().findByType(resourceServer, type, new Consumer<Resource>() {
                            @Override
                            public void accept(Resource resource) {
                                consumer.andThen(resources::add)
                                        .andThen(StoreFactoryCacheSession.this::cacheResource)
                                        .accept(resource);
                            }
                        });
                        return resources;
                    },
                    (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(Resource::getId).collect(Collectors.toSet()), resourceServerId), resourceServer, consumer);
        }

        @Override
        public void findByType(ResourceServer resourceServer, String type, String owner, Consumer<Resource> consumer) {
            if (type == null) return;
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getResourceByTypeCacheKey(type, owner, resourceServerId);
            cacheQuery(cacheKey, ResourceListQuery.class, () -> {
                        List<Resource> resources = new ArrayList<>();
                        getResourceStoreDelegate().findByType(resourceServer, type, owner, new Consumer<Resource>() {
                            @Override
                            public void accept(Resource resource) {
                                consumer.andThen(resources::add)
                                        .andThen(StoreFactoryCacheSession.this::cacheResource)
                                        .accept(resource);
                            }
                        });
                        return resources;
                    },
                    (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(Resource::getId).collect(Collectors.toSet()), resourceServerId), resourceServer, consumer);
        }

        @Override
        public void findByTypeInstance(ResourceServer resourceServer, String type, Consumer<Resource> consumer) {
            if (type == null) return;
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getResourceByTypeInstanceCacheKey(type, resourceServerId);
            cacheQuery(cacheKey, ResourceListQuery.class, () -> {
                        List<Resource> resources = new ArrayList<>();
                        getResourceStoreDelegate().findByTypeInstance(resourceServer, type, new Consumer<Resource>() {
                            @Override
                            public void accept(Resource resource) {
                                consumer.andThen(resources::add)
                                        .andThen(StoreFactoryCacheSession.this::cacheResource)
                                        .accept(resource);
                            }
                        });
                        return resources;
                    },
                    (revision, resources) -> new ResourceListQuery(revision, cacheKey, resources.stream().map(Resource::getId).collect(Collectors.toSet()), resourceServerId), resourceServer, consumer);
        }

        private <R extends Resource, Q extends ResourceQuery> List<R> cacheQuery(String cacheKey, Class<Q> queryType, Supplier<List<R>> resultSupplier, BiFunction<Long, List<R>, Q> querySupplier, ResourceServer resourceServer, Consumer<R> consumer) {
            return cacheQuery(cacheKey, queryType, resultSupplier, querySupplier, resourceServer, consumer, false);
        }

        private <R extends Resource, Q extends ResourceQuery> List<R> cacheQuery(String cacheKey, Class<Q> queryType, Supplier<List<R>> resultSupplier, BiFunction<Long, List<R>, Q> querySupplier, ResourceServer resourceServer) {
            return cacheQuery(cacheKey, queryType, resultSupplier, querySupplier, resourceServer, null, true);
        }

        private <R extends Resource, Q extends ResourceQuery> List<R> cacheQuery(String cacheKey, Class<Q> queryType, Supplier<List<R>> resultSupplier, BiFunction<Long, List<R>, Q> querySupplier, ResourceServer resourceServer, Consumer<R> consumer, boolean cacheResult) {
            Q query = cache.get(cacheKey, queryType);
            if (query != null) {
                logger.tracev("cache hit for key: {0}", cacheKey);
            }
            List<R> model = Collections.emptyList();
            if (query == null) {
                Long loaded = cache.getCurrentRevision(cacheKey);
                model = resultSupplier.get();
                if (model == null) return null;
                if (!invalidations.contains(cacheKey)) {
                    query = querySupplier.apply(loaded, model);
                    cache.addRevisioned(query, startupRevision);
                }
            } else if (query.isInvalid(invalidations)) {
                model = resultSupplier.get();
            } else {
                cacheResult = false;
                Set<String> resources = query.getResources();

                if (consumer != null) {
                    resources.stream().map(resourceId -> (R) findById(resourceServer, resourceId)).filter(Objects::nonNull).forEach(consumer);
                } else {
                    model = resources.stream().map(resourceId -> (R) findById(resourceServer, resourceId)).filter(Objects::nonNull).collect(Collectors.toList());
                }
            }
            
            if (cacheResult) {
                model.forEach(StoreFactoryCacheSession.this::cacheResource);
            }
            
            return model;
        }
    }

    protected class PolicyCache implements PolicyStore {
        @Override
        public Policy create(ResourceServer resourceServer, AbstractPolicyRepresentation representation) {
            Policy policy = getPolicyStoreDelegate().create(resourceServer, representation);
            Policy cached = findById(resourceServer, policy.getId());
            registerPolicyInvalidation(policy.getId(), representation.getName(), representation.getResources(), representation.getScopes(), null, resourceServer.getId());
            if (cached == null) {
                cached = findById(resourceServer, policy.getId());
            }
            return cached;
        }

        @Override
        public void delete(String id) {
            if (id == null) return;
            Policy policy = findById(null, id);
            if (policy == null) return;

            cache.invalidateObject(id);
            Set<String> resources = policy.getResources().stream().map(Resource::getId).collect(Collectors.toSet());
            ResourceServer resourceServer = policy.getResourceServer();
            Set<String> resourceTypes = getResourceTypes(resources, resourceServer.getId());
            String defaultResourceType = policy.getConfig().get("defaultResourceType");
            if (Objects.nonNull(defaultResourceType)) {
                resourceTypes.add(defaultResourceType);
            }
            Set<String> scopes = policy.getScopes().stream().map(Scope::getId).collect(Collectors.toSet());
            invalidationEvents.add(PolicyRemovedEvent.create(id, policy.getName(), resources, resourceTypes, scopes, resourceServer.getId()));
            cache.policyRemoval(id, policy.getName(), resources, resourceTypes, scopes, resourceServer.getId(), invalidations);
            getPolicyStoreDelegate().delete(id);

        }

        @Override
        public Policy findById(ResourceServer resourceServer, String id) {
            if (id == null) return null;

            CachedPolicy cached = cache.get(id, CachedPolicy.class);
            if (cached != null) {
                logger.tracev("by id cache hit: {0}", cached.getId());
            }
            if (cached == null) {
                if (! modelMightExist(id)) return null;
                Policy model = getPolicyStoreDelegate().findById(resourceServer, id);
                Long loaded = cache.getCurrentRevision(id);
                if (model == null) {
                    setModelDoesNotExists(id, loaded);
                    return null;
                }
                if (invalidations.contains(id)) return model;
                cached = new CachedPolicy(loaded, model);
                cache.addRevisioned(cached, startupRevision);
            } else if (invalidations.contains(id)) {
                return getPolicyStoreDelegate().findById(resourceServer, id);
            } else if (managedPolicies.containsKey(id)) {
                return managedPolicies.get(id);
            }
            PolicyAdapter adapter = new PolicyAdapter(cached, StoreFactoryCacheSession.this);
            managedPolicies.put(id, adapter);
            return adapter;
        }

        @Override
        public Policy findByName(ResourceServer resourceServer, String name) {
            if (name == null) return null;
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getPolicyByNameCacheKey(name, resourceServerId);
            List<Policy> result = cacheQuery(cacheKey, PolicyListQuery.class, () -> {
                Policy policy = getPolicyStoreDelegate().findByName(resourceServer, name);

                if (policy == null) {
                    return Collections.emptyList();
                }

                return Arrays.asList(policy);
            }, (revision, policies) -> new PolicyListQuery(revision, cacheKey, policies.stream().map(Policy::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);

            if (result.isEmpty()) {
                return null;
            }

            return result.get(0);
        }

        @Override
        public List<Policy> findByResourceServer(ResourceServer resourceServer) {
            return getPolicyStoreDelegate().findByResourceServer(resourceServer);
        }

        @Override
        public List<Policy> find(ResourceServer resourceServer, Map<Policy.FilterOption, String[]> attributes, Integer firstResult, Integer maxResults) {
            return getPolicyStoreDelegate().find(resourceServer, attributes, firstResult, maxResults);
        }

        @Override
        public List<Policy> findByResource(ResourceServer resourceServer, Resource resource) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getPolicyByResource(resource.getId(), resourceServerId);
            return cacheQuery(cacheKey, PolicyResourceListQuery.class, () -> getPolicyStoreDelegate().findByResource(resourceServer, resource),
                    (revision, policies) -> new PolicyResourceListQuery(revision, cacheKey, resource.getId(), policies.stream().map(Policy::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);
        }

        @Override
        public void findByResource(ResourceServer resourceServer, Resource resource, Consumer<Policy> consumer) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getPolicyByResource(resource.getId(), resourceServerId);
            cacheQuery(cacheKey, PolicyResourceListQuery.class, () -> {
                        List<Policy> policies = new ArrayList<>();
                        getPolicyStoreDelegate().findByResource(resourceServer, resource, new Consumer<Policy>() {
                            @Override
                            public void accept(Policy policy) {
                                consumer.andThen(policies::add)
                                        .andThen(StoreFactoryCacheSession.this::cachePolicy)
                                        .accept(policy);
                            }
                        });
                        return policies;
                    },
                    (revision, policies) -> new PolicyResourceListQuery(revision, cacheKey, resource.getId(), policies.stream().map(Policy::getId).collect(Collectors.toSet()), resourceServerId), resourceServer, consumer);
        }

        @Override
        public List<Policy> findByResourceType(ResourceServer resourceServer, String resourceType) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getPolicyByResourceType(resourceType, resourceServerId);
            return cacheQuery(cacheKey, PolicyResourceListQuery.class, () -> getPolicyStoreDelegate().findByResourceType(resourceServer, resourceType),
                    (revision, policies) -> new PolicyResourceListQuery(revision, cacheKey, resourceType, policies.stream().map(Policy::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);
        }

        @Override
        public void findByResourceType(ResourceServer resourceServer, String resourceType, Consumer<Policy> consumer) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getPolicyByResourceType(resourceType, resourceServerId);
            cacheQuery(cacheKey, PolicyResourceListQuery.class, () -> {
                        List<Policy> policies = new ArrayList<>();
                        getPolicyStoreDelegate().findByResourceType(resourceServer, resourceType, new Consumer<Policy>() {
                            @Override
                            public void accept(Policy policy) {
                                consumer.andThen(policies::add)
                                        .andThen(StoreFactoryCacheSession.this::cachePolicy)
                                        .accept(policy);
                            }
                        });
                        return policies;
                    },
                    (revision, policies) -> new PolicyResourceListQuery(revision, cacheKey, resourceType, policies.stream().map(Policy::getId).collect(Collectors.toSet()), resourceServerId), resourceServer, consumer);
        }

        @Override
        public List<Policy> findByScopes(ResourceServer resourceServer, List<Scope> scopes) {
            if (scopes == null) return null;
            Set<Policy> result = new HashSet<>();
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();

            for (Scope scope : scopes) {
                String cacheKey = getPolicyByScope(scope.getId(), resourceServerId);
                result.addAll(cacheQuery(cacheKey, PolicyScopeListQuery.class, () -> getPolicyStoreDelegate().findByScopes(resourceServer, Collections.singletonList(scope)), (revision, resources) -> new PolicyScopeListQuery(revision, cacheKey, scope.getId(), resources.stream().map(resource -> resource.getId()).collect(Collectors.toSet()), resourceServerId), resourceServer));
            }

            return new ArrayList<>(result);
        }

        @Override
        public List<Policy> findByScopes(ResourceServer resourceServer, Resource resource, List<Scope> scopes) {
            if (scopes == null) return null;
            Set<Policy> result = new HashSet<>();
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();

            for (Scope scope : scopes) {
                String cacheKey = getPolicyByResourceScope(scope.getId(), resource == null ? null : resource.getId(), resourceServerId);
                result.addAll(cacheQuery(cacheKey, PolicyScopeListQuery.class, () -> getPolicyStoreDelegate().findByScopes(resourceServer, resource, Collections.singletonList(scope)), (revision, resources) -> new PolicyScopeListQuery(revision, cacheKey, scope.getId(), resources.stream().map(Policy::getId).collect(Collectors.toSet()), resourceServerId), resourceServer));
            }

            return new ArrayList<>(result);
        }

        @Override
        public void findByScopes(ResourceServer resourceServer, Resource resource, List<Scope> scopes, Consumer<Policy> consumer) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String resourceId = resource == null ? null : resource.getId();
            for (Scope scope : scopes) {
                String cacheKey = getPolicyByResourceScope(scope.getId(), resourceId, resourceServerId);
                cacheQuery(cacheKey, PolicyScopeListQuery.class, () -> {
                    List<Policy> policies = new ArrayList<>();
                    getPolicyStoreDelegate().findByScopes(resourceServer, resource, Collections.singletonList(scope),
                            policy -> {
                                consumer.andThen(policies::add)
                                        .andThen(StoreFactoryCacheSession.this::cachePolicy)
                                        .accept(policy);
                            });
                    return policies;
                }, (revision, resources) -> new PolicyScopeListQuery(revision, cacheKey, scope.getId(), resources.stream().map(Policy::getId).collect(Collectors.toSet()), resourceServerId), resourceServer, consumer);
            }
        }

        @Override
        public List<Policy> findByType(ResourceServer resourceServer, String type) {
            return getPolicyStoreDelegate().findByType(resourceServer, type);
        }

        @Override
        public List<Policy> findDependentPolicies(ResourceServer resourceServer, String id) {
            return getPolicyStoreDelegate().findDependentPolicies(resourceServer, id);
        }

        @Override
        public Stream<Policy> findDependentPolicies(ResourceServer resourceServer, String resourceType, String associatedPolicyType, String configKey, String configValue) {
            return getPolicyStoreDelegate().findDependentPolicies(resourceServer, resourceType, associatedPolicyType, configKey, configValue);
        }

        @Override
        public Stream<Policy> findDependentPolicies(ResourceServer resourceServer, String resourceType, String associatedPolicyType, String configKey, List<String> configValue) {
            return getPolicyStoreDelegate().findDependentPolicies(resourceServer, resourceType, associatedPolicyType, configKey, configValue);
        }

        private <R extends Policy, Q extends PolicyQuery> List<R> cacheQuery(String cacheKey, Class<Q> queryType, Supplier<List<R>> resultSupplier, BiFunction<Long, List<R>, Q> querySupplier, ResourceServer resourceServer) {
            return cacheQuery(cacheKey, queryType, resultSupplier, querySupplier, resourceServer, null, true);
        }

        private <R extends Policy, Q extends PolicyQuery> List<R> cacheQuery(String cacheKey, Class<Q> queryType, Supplier<List<R>> resultSupplier, BiFunction<Long, List<R>, Q> querySupplier, ResourceServer resourceServer, Consumer<R> consumer) {
            return cacheQuery(cacheKey, queryType, resultSupplier, querySupplier, resourceServer, consumer, false);
        }
        
        private <R extends Policy, Q extends PolicyQuery> List<R> cacheQuery(String cacheKey, Class<Q> queryType, Supplier<List<R>> resultSupplier, BiFunction<Long, List<R>, Q> querySupplier, ResourceServer resourceServer, Consumer<R> consumer, boolean cacheResults) {
            Q query = cache.get(cacheKey, queryType);
            if (query != null) {
                logger.tracev("cache hit for key: {0}", cacheKey);
            }
            List<R> model = Collections.emptyList();
            if (query == null) {
                Long loaded = cache.getCurrentRevision(cacheKey);
                model = resultSupplier.get();
                if (model == null) return null;
                if (!invalidations.contains(cacheKey)) {
                    query = querySupplier.apply(loaded, model);
                    cache.addRevisioned(query, startupRevision);
                }
            } else if (query.isInvalid(invalidations)) {
                model = resultSupplier.get();
            } else {
                cacheResults = false;
                Set<String> policies = query.getPolicies();

                if (consumer != null) {
                    for (String id : policies) {
                        consumer.accept((R) findById(resourceServer, id));
                    }
                } else {
                    model = policies.stream().map(resourceId -> (R) findById(resourceServer, resourceId))
                            .filter(Objects::nonNull).collect(Collectors.toList());
                }
            }
            if (cacheResults) {
                model.forEach(StoreFactoryCacheSession.this::cachePolicy);
            }
            return model;
        }
    }

    protected class PermissionTicketCache implements PermissionTicketStore {
        @Override
        public long count(ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes) {
            return getPermissionTicketStoreDelegate().count(resourceServer, attributes);
        }

        @Override
        public PermissionTicket create(ResourceServer resourceServer, Resource resource, Scope scope, String requester) {
            PermissionTicket created = getPermissionTicketStoreDelegate().create(resourceServer, resource, scope, requester);
            registerPermissionTicketInvalidation(created.getId(), created.getOwner(), created.getRequester(), created.getResource().getId(), created.getResource().getName(), scope == null ? null : scope.getId(), created.getResourceServer().getId());
            return created;
        }

        @Override
        public void delete(String id) {
            if (id == null) return;
            PermissionTicket permission = findById(null, id);
            if (permission == null) return;

            cache.invalidateObject(id);
            String scopeId = null;
            if (permission.getScope() != null) {
                scopeId = permission.getScope().getId();
            }
            invalidationEvents.add(PermissionTicketRemovedEvent.create(id, permission.getOwner(), permission.getRequester(), permission.getResource().getId(), permission.getResource().getName(), scopeId, permission.getResourceServer().getId()));
            cache.permissionTicketRemoval(id, permission.getOwner(), permission.getRequester(), permission.getResource().getId(), permission.getResource().getName(),scopeId, permission.getResourceServer().getId(), invalidations);
            getPermissionTicketStoreDelegate().delete(id);
            UserManagedPermissionUtil.removePolicy(permission, StoreFactoryCacheSession.this);

        }

        @Override
        public PermissionTicket findById(ResourceServer resourceServer, String id) {
            if (id == null) return null;

            CachedPermissionTicket cached = cache.get(id, CachedPermissionTicket.class);
            if (cached != null) {
                logger.tracev("by id cache hit: {0}", cached.getId());
            }
            if (cached == null) {
                Long loaded = cache.getCurrentRevision(id);
                if (! modelMightExist(id)) return null;
                PermissionTicket model = getPermissionTicketStoreDelegate().findById(resourceServer, id);
                if (model == null) {
                    setModelDoesNotExists(id, loaded);
                    return null;
                }
                if (invalidations.contains(id)) return model;
                cached = new CachedPermissionTicket(loaded, model);
                cache.addRevisioned(cached, startupRevision);
            } else if (invalidations.contains(id)) {
                return getPermissionTicketStoreDelegate().findById(resourceServer, id);
            } else if (managedPermissionTickets.containsKey(id)) {
                return managedPermissionTickets.get(id);
            }
            PermissionTicketAdapter adapter = new PermissionTicketAdapter(cached, StoreFactoryCacheSession.this);
            managedPermissionTickets.put(id, adapter);
            return adapter;
        }

        @Override
        public List<PermissionTicket> findByResource(ResourceServer resourceServer, Resource resource) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getPermissionTicketByResource(resource.getId(), resourceServerId);
            return cacheQuery(cacheKey, PermissionTicketResourceListQuery.class, () -> getPermissionTicketStoreDelegate().findByResource(resourceServer, resource),
                    (revision, permissions) -> new PermissionTicketResourceListQuery(revision, cacheKey, resource.getId(), permissions.stream().map(PermissionTicket::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);
        }

        @Override
        public List<PermissionTicket> findByScope(ResourceServer resourceServer, Scope scope) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getPermissionTicketByScope(scope.getId(), resourceServerId);
            return cacheQuery(cacheKey, PermissionTicketScopeListQuery.class, () -> getPermissionTicketStoreDelegate().findByScope(resourceServer, scope),
                    (revision, permissions) -> new PermissionTicketScopeListQuery(revision, cacheKey, scope.getId(), permissions.stream().map(PermissionTicket::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);
        }

        @Override
        public List<PermissionTicket> find(ResourceServer resourceServer, Map<PermissionTicket.FilterOption, String> attributes, Integer firstResult, Integer maxResult) {
            return getPermissionTicketStoreDelegate().find(resourceServer, attributes, firstResult, maxResult);
        }

        @Override
        public List<PermissionTicket> findGranted(ResourceServer resourceServer, String userId) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getPermissionTicketByGranted(userId, resourceServerId);
            return cacheQuery(cacheKey, PermissionTicketListQuery.class, () -> getPermissionTicketStoreDelegate().findGranted(resourceServer, userId),
                    (revision, permissions) -> new PermissionTicketListQuery(revision, cacheKey, permissions.stream().map(PermissionTicket::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);
        }

        @Override
        public List<PermissionTicket> findGranted(ResourceServer resourceServer, String resourceName, String userId) {
            String resourceServerId = resourceServer == null ? null : resourceServer.getId();
            String cacheKey = getPermissionTicketByResourceNameAndGranted(resourceName, userId, resourceServerId);
            return cacheQuery(cacheKey, PermissionTicketListQuery.class, () -> getPermissionTicketStoreDelegate().findGranted(resourceServer, resourceName, userId),
                    (revision, permissions) -> new PermissionTicketResourceListQuery(revision, cacheKey, resourceName, permissions.stream().map(PermissionTicket::getId).collect(Collectors.toSet()), resourceServerId), resourceServer);
        }

        @Override
        public List<Resource> findGrantedResources(String requester, String name, Integer first, Integer max) {
            return getPermissionTicketStoreDelegate().findGrantedResources(requester, name, first, max);
        }

        @Override
        public List<Resource> findGrantedOwnerResources(String owner, Integer firstResult, Integer maxResults) {
            return getPermissionTicketStoreDelegate().findGrantedOwnerResources(owner, firstResult, maxResults);
        }

        private <R, Q extends PermissionTicketQuery> List<R> cacheQuery(String cacheKey, Class<Q> queryType, Supplier<List<R>> resultSupplier, BiFunction<Long, List<R>, Q> querySupplier, ResourceServer resourceServer) {
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
                return query.getPermissions().stream().map(resourceId -> (R) findById(resourceServer, resourceId)).collect(Collectors.toList());
            }
        }
    }

    void cachePolicy(Policy model) {
        String id = model.getId();
        if (cache.getCache().containsKey(id)) {
            return;
        }
        if (!modelMightExist(id)) {
            return;
        }
        if (invalidations.contains(id)) return;
        cache.addRevisioned(createCachedPolicy(model, id), startupRevision);
    }

    CachedPolicy createCachedPolicy(Policy model, String id) {
        Long loaded = cache.getCurrentRevision(id);
        return new CachedPolicy(loaded, model);
    }

    void cacheResource(Resource model) {
        String id = model.getId();
        if (cache.getCache().containsKey(id)) {
            return;
        }
        Long loaded = cache.getCurrentRevision(id);
        if (!modelMightExist(id)) {
            return;
        }
        if (invalidations.contains(id)) return;
        cache.addRevisioned(new CachedResource(loaded, model), startupRevision);
    }

    void cacheScope(Scope model) {
        String id = model.getId();
        if (cache.getCache().containsKey(id)) {
            return;
        }
        Long loaded = cache.getCurrentRevision(id);
        if (!modelMightExist(id)) {
            return;
        }
        if (invalidations.contains(id)) return;
        cache.addRevisioned(new CachedScope(loaded, model), startupRevision);
    }
}
