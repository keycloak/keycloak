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

import org.keycloak.authorization.model.Resource;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ResourceStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.authorization.infinispan.entities.CachedResource;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedResourceStore extends AbstractCachedStore implements ResourceStore {

    private static final String RESOURCE_CACHE_PREFIX = "rs-";

    private ResourceStore delegate;

    public CachedResourceStore(InfinispanStoreFactoryProvider cacheStoreFactory, StoreFactory storeFactory) {
        super(cacheStoreFactory, storeFactory);
        delegate = storeFactory.getResourceStore();
    }

    @Override
    public Resource create(String name, ResourceServer resourceServer, String owner) {
        Resource resource = getDelegate().create(name, getStoreFactory().getResourceServerStore().findById(resourceServer.getId()), owner);

        addInvalidation(getCacheKeyForResource(resource.getId()));
        addInvalidation(getCacheKeyForResourceName(resource.getName()));
        addInvalidation(getCacheKeyForOwner(owner));

        getCachedStoreFactory().getPolicyStore().addInvalidations(resource);

        getTransaction().whenRollback(() -> removeCachedEntry(resourceServer.getId(), getCacheKeyForResource(resource.getId())));
        getTransaction().whenCommit(() -> invalidate(resourceServer.getId()));

        return createAdapter(new CachedResource(resource));
    }

    @Override
    public void delete(String id) {
        Resource resource = getDelegate().findById(id, null);

        if (resource == null) {
            return;
        }

        ResourceServer resourceServer = resource.getResourceServer();

        addInvalidation(getCacheKeyForResource(resource.getId()));
        addInvalidation(getCacheKeyForResourceName(resource.getName()));
        addInvalidation(getCacheKeyForOwner(resource.getOwner()));
        addInvalidation(getCacheKeyForUri(resource.getUri()));
        getCachedStoreFactory().getPolicyStore().addInvalidations(resource);

        getDelegate().delete(id);

        getTransaction().whenCommit(() -> {
            invalidate(resourceServer.getId());
        });
    }

    @Override
    public Resource findById(String id, String resourceServerId) {
        String cacheKeyForResource = getCacheKeyForResource(id);

        if (isInvalid(cacheKeyForResource)) {
            return getDelegate().findById(id, resourceServerId);
        }

        List<Object> cached = resolveCacheEntry(resourceServerId, cacheKeyForResource);

        if (cached == null) {
            Resource resource = getDelegate().findById(id, resourceServerId);

            if (resource != null) {
                return createAdapter(putCacheEntry(resourceServerId, cacheKeyForResource, new CachedResource(resource)));
            }

            return null;
        }

        return createAdapter(CachedResource.class.cast(cached.get(0)));
    }

    @Override
    public List<Resource> findByOwner(String ownerId, String resourceServerId) {
        String cacheKey = getCacheKeyForOwner(ownerId);

        if (isInvalid(cacheKey)) {
            return getDelegate().findByOwner(ownerId, resourceServerId);
        }

        return cacheResult(resourceServerId, cacheKey, () -> getDelegate().findByOwner(ownerId, resourceServerId));
    }

    @Override
    public List<Resource> findByUri(String uri, String resourceServerId) {
        String cacheKey = getCacheKeyForUri(uri);

        if (isInvalid(cacheKey)) {
            return getDelegate().findByUri(uri, resourceServerId);
        }

        return cacheResult(resourceServerId, cacheKey, () -> getDelegate().findByUri(uri, resourceServerId));
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
        String cacheKey = getCacheKeyForResourceName(name);

        if (isInvalid(cacheKey)) {
            return getDelegate().findByName(name, resourceServerId);
        }

        return cacheResult(resourceServerId, cacheKey, () -> {
            Resource resource = getDelegate().findByName(name, resourceServerId);

            if (resource == null) {
                return Collections.emptyList();
            }

            return Arrays.asList(resource);
        }).stream().findFirst().orElse(null);
    }

    @Override
    public List<Resource> findByType(String type, String resourceServerId) {
        return  getDelegate().findByType(type, resourceServerId);
    }

    private String getCacheKeyForResource(String id) {
        return new StringBuilder(RESOURCE_CACHE_PREFIX).append("id-").append(id).toString();
    }

    private String getCacheKeyForResourceName(String name) {
        return new StringBuilder(RESOURCE_CACHE_PREFIX).append("findByName-").append(name).toString();
    }

    private String getCacheKeyForOwner(String name) {
        return new StringBuilder(RESOURCE_CACHE_PREFIX).append("findByOwner-").append(name).toString();
    }

    private String getCacheKeyForUri(String uri) {
        return new StringBuilder(RESOURCE_CACHE_PREFIX).append("findByUri-").append(uri).toString();
    }

    private ResourceStore getDelegate() {
        return this.delegate;
    }

    private List<Resource> cacheResult(String resourceServerId, String key, Supplier<List<Resource>> provider) {
        List<Object> cached = getCachedStoreFactory().computeIfCachedEntryAbsent(resourceServerId, key, (Function<String, List<Object>>) o -> {
            List<Resource> result = provider.get();

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
                addInvalidation(getCacheKeyForResourceName(name));
                addInvalidation(getCacheKeyForResourceName(cached.getName()));
                getDelegateForUpdate().setName(name);
                cached.setName(name);
            }

            @Override
            public String getUri() {
                return cached.getUri();
            }

            @Override
            public void setUri(String uri) {
                addInvalidation(getCacheKeyForUri(uri));
                addInvalidation(getCacheKeyForUri(cached.getUri()));
                getDelegateForUpdate().setUri(uri);
                cached.setUri(uri);
            }

            @Override
            public String getType() {
                return cached.getType();
            }

            @Override
            public void setType(String type) {
                getCachedStoreFactory().getPolicyStore().addInvalidations(cached);
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
                    String resourceServerId = cached.getResourceServerId();
                    this.updated = getDelegate().findById(getId(), resourceServerId);
                    if (this.updated == null) throw new IllegalStateException("Not found in database");
                    addInvalidation(getCacheKeyForResource(updated.getId()));
                    getCachedStoreFactory().getPolicyStore().addInvalidations(updated);
                    getTransaction().whenCommit(() -> invalidate(resourceServerId));
                    getTransaction().whenRollback(() -> removeCachedEntry(resourceServerId, getCacheKeyForResource(cached.getId())));
                }

                return this.updated;
            }
        };
    }
}
