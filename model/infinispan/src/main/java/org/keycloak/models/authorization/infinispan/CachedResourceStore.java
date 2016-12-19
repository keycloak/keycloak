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

    private final KeycloakSession session;
    private final CacheTransaction transaction;
    private final List<String> cacheKeys;
    private StoreFactory storeFactory;
    private ResourceStore delegate;
    private final Cache<String, List<CachedResource>> cache;

    public CachedResourceStore(KeycloakSession session, CacheTransaction transaction, StoreFactory storeFactory) {
        this.session = session;
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
        this.transaction = transaction;
        cacheKeys = new ArrayList<>();
        cacheKeys.add("findByOwner");
        this.storeFactory = storeFactory;
    }

    @Override
    public Resource create(String name, ResourceServer resourceServer, String owner) {
        Resource resource = getDelegate().create(name, getStoreFactory().getResourceServerStore().findById(resourceServer.getId()), owner);

        this.transaction.whenRollback(() -> {
            cache.remove(getCacheKeyForResource(resource.getId()));
            invalidateCache(resourceServer.getId());
        });

        return createAdapter(new CachedResource(resource));
    }

    @Override
    public void delete(String id) {
        Resource resource = findById(id, null);
        ResourceServer resourceServer = resource.getResourceServer();
        getDelegate().delete(id);
        this.transaction.whenCommit(() -> {
            List<CachedResource> resources = cache.remove(getCacheKeyForResource(id));

            if (resources != null) {
                CachedResource entry = resources.get(0);
                cache.remove(getCacheKeyForResourceName(entry.getName(), entry.getResourceServerId()));
            }

            invalidateCache(resourceServer.getId());
        });
    }

    @Override
    public Resource findById(String id, String resourceServerId) {
        String cacheKeyForResource = getCacheKeyForResource(id);
        List<CachedResource> cached = this.cache.get(cacheKeyForResource);

        if (cached == null) {
            Resource resource = getDelegate().findById(id, resourceServerId);

            if (resource != null) {
                return createAdapter(updateResourceCache(resource));
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public List<Resource> findByOwner(String ownerId, String resourceServerId) {
        return cacheResult(new StringBuilder("findByOwner").append(resourceServerId).append(ownerId).toString(), () -> getDelegate().findByOwner(ownerId, resourceServerId));
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
        List<CachedResource> cached = this.cache.get(cacheKeyForResource);

        if (cached == null) {
            Resource resource = getDelegate().findByName(name, resourceServerId);

            if (resource != null) {
                cache.put(cacheKeyForResource, Arrays.asList(new CachedResource(resource)));
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
            this.delegate = getStoreFactory().getResourceStore();
        }

        return this.delegate;
    }

    private StoreFactory getStoreFactory() {
        return this.storeFactory;
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
                getDelegateForUpdate().updateScopes(scopes.stream().map(scope -> getStoreFactory().getScopeStore().findById(scope.getId(), cached.getResourceServerId())).collect(Collectors.toSet()));
                cached.updateScopes(scopes);
            }

            private Resource getDelegateForUpdate() {
                if (this.updated == null) {
                    this.updated = getDelegate().findById(getId(), cached.getResourceServerId());
                    if (this.updated == null) throw new IllegalStateException("Not found in database");
                    transaction.whenRollback(() -> {
                        cache.remove(getCacheKeyForResource(cached.getId()));
                        invalidateCache(cached.getResourceServerId());
                        getCachedStoreFactory().getPolicyStore().notifyChange(cached);
                    });
                }

                return this.updated;
            }
        };
    }

    private CachedStoreFactoryProvider getCachedStoreFactory() {
        return session.getProvider(CachedStoreFactoryProvider.class);
    }

    private CachedResource updateResourceCache(Resource resource) {
        CachedResource cached = new CachedResource(resource);
        List cache = new ArrayList<>();

        cache.add(cached);

        this.cache.put(getCacheKeyForResource(resource.getId()), cache);

        return cached;
    }

    private List<Resource> cacheResult(String key, Supplier<List<Resource>> provider) {
        List<CachedResource> cached = cache.computeIfAbsent(key, (Function<String, List<CachedResource>>) o -> {
            List<Resource> result = provider.get();

            if (result.isEmpty()) {
                return null;
            }

            return result.stream().map(resource -> new CachedResource(resource)).collect(Collectors.toList());
        });

        if (cached == null) {
            return Collections.emptyList();
        }

        return cached.stream().map(this::createAdapter).collect(Collectors.toList());
    }

    private void invalidateCache(String resourceServerId) {
        cacheKeys.forEach(cacheKey -> cache.keySet().stream().filter(key -> key.startsWith(cacheKey + resourceServerId)).forEach(cache::remove));
    }
}
