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
import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.authorization.infinispan.InfinispanStoreFactoryProvider.CacheTransaction;
import org.keycloak.models.authorization.infinispan.entities.CachedResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedResourceStore implements ResourceStore {

    private static final String RESOURCE_ID_CACHE_PREFIX = "rsc-id-";
    private static final String RESOURCE_OWNER_CACHE_PREFIX = "rsc-owner-";

    private final KeycloakSession session;
    private final CacheTransaction transaction;
    private StoreFactory storeFactory;
    private ResourceStore delegate;
    private final Cache<String, List> cache;

    public CachedResourceStore(KeycloakSession session, CacheTransaction transaction) {
        this.session = session;
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
        this.transaction = transaction;
    }

    @Override
    public Resource create(String name, ResourceServer resourceServer, String owner) {
        Resource resource = getDelegate().create(name, getStoreFactory().getResourceServerStore().findById(resourceServer.getId()), owner);

        this.transaction.whenRollback(() -> cache.remove(getCacheKeyForResource(resource.getId())));

        return createAdapter(new CachedResource(resource));
    }

    @Override
    public void delete(String id) {
        List<CachedResource> removed = this.cache.remove(getCacheKeyForResource(id));

        if (removed != null) {
            CachedResource cachedResource = removed.get(0);
            List<String> byOwner = this.cache.get(getResourceOwnerCacheKey(cachedResource.getOwner()));

            if (byOwner != null) {
                byOwner.remove(id);

                if (byOwner.isEmpty()) {
                    this.cache.remove(getResourceOwnerCacheKey(cachedResource.getOwner()));
                }
            }
        }

        getDelegate().delete(id);
    }

    @Override
    public Resource findById(String id) {
        String cacheKeyForResource = getCacheKeyForResource(id);
        List<CachedResource> cached = this.cache.get(cacheKeyForResource);

        if (cached == null) {
            Resource resource = getDelegate().findById(id);

            if (resource != null) {
                updateCachedIds(getResourceOwnerCacheKey(resource.getOwner()), resource, false);
                return createAdapter(updateResourceCache(resource));
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public List<Resource> findByOwner(String ownerId) {
        List<String> cachedIds = this.cache.get(getResourceOwnerCacheKey(ownerId));

        if (cachedIds == null) {
            for (Resource resource : getDelegate().findByOwner(ownerId)) {
                updateCachedIds(getResourceOwnerCacheKey(ownerId), resource, true);
            }
            cachedIds = this.cache.getOrDefault(getResourceOwnerCacheKey(ownerId), Collections.emptyList());
        }

        return  ((List<String>) this.cache.getOrDefault(getResourceOwnerCacheKey(ownerId), Collections.emptyList())).stream().map(this::findById)
                        .filter(resource -> resource != null)
                        .collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByResourceServer(String resourceServerId) {
        return getDelegate().findByResourceServer(resourceServerId).stream().map(resource -> findById(resource.getId())).collect(Collectors.toList());
    }

    @Override
    public List<Resource> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        return getDelegate().findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
    }

    @Override
    public List<Resource> findByScope(String... id) {
        return getDelegate().findByScope(id).stream().map(resource -> findById(resource.getId())).collect(Collectors.toList());
    }

    @Override
    public Resource findByName(String name, String resourceServerId) {
        for (Entry entry : this.cache.entrySet()) {
            String cacheKey = (String) entry.getKey();

            if (cacheKey.startsWith(RESOURCE_ID_CACHE_PREFIX)) {
                List<CachedResource> value = (List<CachedResource>) entry.getValue();
                CachedResource resource = value.get(0);

                if (resource.getResourceServerId().equals(resourceServerId) && resource.getName().equals(name)) {
                    return findById(resource.getId());
                }
            }
        }

        Resource resource = getDelegate().findByName(name, resourceServerId);

        if (resource != null) {
            return findById(updateResourceCache(resource).getId());
        }

        return null;
    }

    @Override
    public List<Resource> findByType(String type) {
        return  getDelegate().findByType(type).stream().map(resource -> findById(resource.getId())).collect(Collectors.toList());
    }

    private String getCacheKeyForResource(String id) {
        return RESOURCE_ID_CACHE_PREFIX + id;
    }

    private ResourceStore getDelegate() {
        if (this.delegate == null) {
            this.delegate = getStoreFactory().getResourceStore();
        }

        return this.delegate;
    }

    private StoreFactory getStoreFactory() {
        if (this.storeFactory == null) {
            this.storeFactory = session.getProvider(StoreFactory.class);
        }

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
                List<Scope> scopes = new ArrayList<>();

                for (String id : cached.getScopesIds()) {
                    Scope cached = getStoreFactory().getScopeStore().findById(id);

                    if (cached != null) {
                        scopes.add(cached);
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
                return getStoreFactory().getResourceServerStore().findById(cached.getResourceServerId());
            }

            @Override
            public String getOwner() {
                return cached.getOwner();
            }

            @Override
            public void updateScopes(Set<Scope> scopes) {
                getDelegateForUpdate().updateScopes(scopes.stream().map(scope -> getStoreFactory().getScopeStore().findById(scope.getId())).collect(Collectors.toSet()));
                cached.updateScopes(scopes);
            }

            private Resource getDelegateForUpdate() {
                if (this.updated == null) {
                    this.updated = getDelegate().findById(getId());
                    if (this.updated == null) throw new IllegalStateException("Not found in database");
                    transaction.whenCommit(() -> cache.remove(getCacheKeyForResource(getId())));
                }

                return this.updated;
            }
        };
    }

    private CachedResource updateResourceCache(Resource resource) {
        CachedResource cached = new CachedResource(resource);
        List cache = new ArrayList<>();

        cache.add(cached);

        this.cache.put(getCacheKeyForResource(resource.getId()), cache);

        return cached;
    }

    private void updateCachedIds(String cacheKey, Resource resource, boolean create) {
        List<String> cached = this.cache.get(cacheKey);

        if (cached == null) {
            if (!create) {
                return;
            }
            cached = new ArrayList<>();
            this.cache.put(cacheKey, cached);
        }

        if (cached != null && !cached.contains(resource.getId())) {
            cached.add(resource.getId());
        }
    }

    private String getResourceOwnerCacheKey(String ownerId) {
        return RESOURCE_OWNER_CACHE_PREFIX + ownerId;
    }
}
