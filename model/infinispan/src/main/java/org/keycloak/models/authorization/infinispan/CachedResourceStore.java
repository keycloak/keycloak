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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.authorization.infinispan.InfinispanStoreFactoryProvider.CacheTransaction;
import org.keycloak.models.authorization.infinispan.entities.CachedResource;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedResourceStore implements ResourceStore {

    private static final String RESOURCE_ID_CACHE_PREFIX = "rsc-id-";
    private static final String RESOURCE_NAME_CACHE_PREFIX = "rsc-name-";

    private final CachedStoreFactoryProvider cacheStoreFactory;
    private final CacheTransaction transaction;
    private final List<String> cacheKeys;
    private StoreFactory delegateStoreFactory;
    private ResourceStore delegate;
    private final Cache<String, Map<String, List<CachedResource>>> cache;

    public CachedResourceStore(KeycloakSession session, CachedStoreFactoryProvider cacheStoreFactory, CacheTransaction transaction, StoreFactory delegate) {
        this.cacheStoreFactory = cacheStoreFactory;
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
        this.transaction = transaction;
        cacheKeys = new ArrayList<>();
        cacheKeys.add("findByOwner");
        cacheKeys.add("findByUri");
        cacheKeys.add("findByName");
        this.delegateStoreFactory = delegate;
    }

    @Override
    public Resource create(String name, ResourceServer resourceServer, String owner) {
        Resource resource = getDelegate().create(name, getDelegateStoreFactory().getResourceServerStore().findById(resourceServer.getId()), owner);

        this.transaction.whenRollback(() -> {
            resolveResourceServerCache(resourceServer.getId()).remove(getCacheKeyForResource(resource.getId()));
        });

        this.transaction.whenCommit(() -> {
            invalidateCache(resourceServer.getId());
        });

        return createAdapter(new CachedResource(resource));
    }

    @Override
    public void delete(String id) {
        Resource resource = getDelegate().findById(id, null);
        if (resource == null) {
            return;
        }
        ResourceServer resourceServer = resource.getResourceServer();
        getDelegate().delete(id);
        this.transaction.whenCommit(() -> {
            invalidateCache(resourceServer.getId());
        });
    }

    @Override
    public Resource findById(String id, String resourceServerId) {
        String cacheKeyForResource = getCacheKeyForResource(id);
        List<CachedResource> cached = resolveResourceServerCache(resourceServerId).get(cacheKeyForResource);

        if (cached == null) {
            Resource resource = getDelegate().findById(id, resourceServerId);

            if (resource != null) {
                CachedResource cachedResource = new CachedResource(resource);
                resolveResourceServerCache(resourceServerId).put(cacheKeyForResource, Arrays.asList(cachedResource));
                return createAdapter(cachedResource);
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public List<Resource> findByOwner(String ownerId, String resourceServerId) {
        return cacheResult(resourceServerId, new StringBuilder("findByOwner").append(ownerId).toString(), () -> getDelegate().findByOwner(ownerId, resourceServerId));
    }

    @Override
    public List<Resource> findByUri(String uri, String resourceServerId) {
        return cacheResult(resourceServerId, new StringBuilder("findByUri").append(uri).toString(), () -> getDelegate().findByUri(uri, resourceServerId));
    }

    @Override
    public List<Resource> findByResourceServer(String resourceServerId) {
        return getDelegate().findByResourceServer(resourceServerId);
    }

    @Override
    public List<Resource> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        return getDelegate().findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
    }

    @Override
    public List<Resource> findByScope(List<String> id, String resourceServerId) {
        return getDelegate().findByScope(id, resourceServerId);
    }

    @Override
    public Resource findByName(String name, String resourceServerId) {
        String cacheKeyForResource = getCacheKeyForResourceName(name, resourceServerId);
        List<CachedResource> cached = resolveResourceServerCache(resourceServerId).get(cacheKeyForResource);

        if (cached == null) {
            Resource resource = getDelegate().findByName(name, resourceServerId);

            if (resource != null) {
                invalidateCache(resourceServerId);
                resolveResourceServerCache(resourceServerId).put(cacheKeyForResource, Arrays.asList(new CachedResource(resource)));
                return findById(resource.getId(), resourceServerId);
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public List<Resource> findByType(String type, String resourceServerId) {
        return  getDelegate().findByType(type, resourceServerId);
    }

    private String getCacheKeyForResource(String id) {
        return RESOURCE_ID_CACHE_PREFIX + id;
    }

    private String getCacheKeyForResourceName(String name, String resourceServerId) {
        return RESOURCE_NAME_CACHE_PREFIX + name + "-" + resourceServerId;
    }

    private ResourceStore getDelegate() {
        if (this.delegate == null) {
            this.delegate = getDelegateStoreFactory().getResourceStore();
        }

        return this.delegate;
    }

    private StoreFactory getDelegateStoreFactory() {
        return this.delegateStoreFactory;
    }

    private Resource createAdapter(CachedResource cached) {
        return new Resource() {

            private List<Scope> scopes;
            private Resource updated;

            @Override
            public String getId() {
                return cached.getId();
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
            public String getUri() {
                return cached.getUri();
            }

            @Override
            public void setUri(String uri) {
                getDelegateForUpdate().setUri(uri);
                cached.setUri(uri);
            }

            @Override
            public String getType() {
                return cached.getType();
            }

            @Override
            public void setType(String type) {
                getDelegateForUpdate().setType(type);
                cached.setType(type);
            }

            @Override
            public List<Scope> getScopes() {
                if (scopes == null) {
                    scopes = new ArrayList<>();

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
            public String getIconUri() {
                return cached.getIconUri();
            }

            @Override
            public void setIconUri(String iconUri) {
                getDelegateForUpdate().setIconUri(iconUri);
                cached.setIconUri(iconUri);
            }

            @Override
            public ResourceServer getResourceServer() {
                return getCachedStoreFactory().getResourceServerStore().findById(cached.getResourceServerId());
            }

            @Override
            public String getOwner() {
                return cached.getOwner();
            }

            @Override
            public void updateScopes(Set<Scope> scopes) {
                getDelegateForUpdate().updateScopes(scopes.stream().map(scope -> getDelegateStoreFactory().getScopeStore().findById(scope.getId(), cached.getResourceServerId())).collect(Collectors.toSet()));
                cached.updateScopes(scopes);
            }

            private Resource getDelegateForUpdate() {
                if (this.updated == null) {
                    String resourceServerId = cached.getResourceServerId();
                    this.updated = getDelegate().findById(getId(), resourceServerId);
                    if (this.updated == null) throw new IllegalStateException("Not found in database");
                    transaction.whenCommit(() -> {
                        invalidateCache(resourceServerId);
                    });
                    transaction.whenRollback(() -> {
                        resolveResourceServerCache(resourceServerId).remove(getCacheKeyForResource(cached.getId()));
                    });
                }

                return this.updated;
            }
        };
    }

    private CachedStoreFactoryProvider getCachedStoreFactory() {
        return cacheStoreFactory;
    }

    private List<Resource> cacheResult(String resourceServerId, String key, Supplier<List<Resource>> provider) {
        List<CachedResource> cached = resolveResourceServerCache(resourceServerId).computeIfAbsent(key, (Function<String, List<CachedResource>>) o -> {
            List<Resource> result = provider.get();

            if (result.isEmpty()) {
                return null;
            }

            return result.stream().map(resource -> new CachedResource(resource)).collect(Collectors.toList());
        });

        if (cached == null) {
            return Collections.emptyList();
        }

        List<Resource> adapters = new ArrayList<>();

        for (CachedResource resource : cached) {
            adapters.add(createAdapter(resource));
        }

        return adapters;
    }

    private void invalidateCache(String resourceServerId) {
        cache.remove(resourceServerId);
    }

    private Map<String, List<CachedResource>> resolveResourceServerCache(String id) {
        return cache.computeIfAbsent(id, key -> new HashMap<>());
    }
}
