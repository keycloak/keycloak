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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.infinispan.Cache;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.authorization.infinispan.InfinispanStoreFactoryProvider.CacheTransaction;
import org.keycloak.models.authorization.infinispan.entities.CachedScope;
import org.keycloak.models.cache.authorization.CachedStoreFactoryProvider;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedScopeStore implements ScopeStore {

    private static final String SCOPE_ID_CACHE_PREFIX = "scp-id-";
    private static final String SCOPE_NAME_CACHE_PREFIX = "scp-name-";

    private final Cache<String, Map<String, List<CachedScope>>> cache;
    private final CachedStoreFactoryProvider cacheStoreFactory;
    private final CacheTransaction transaction;
    private ScopeStore delegate;
    private StoreFactory storeFactory;

    public CachedScopeStore(KeycloakSession session, CachedStoreFactoryProvider cacheStoreFactory, CacheTransaction transaction, StoreFactory delegate) {
        this.cacheStoreFactory = cacheStoreFactory;
        this.transaction = transaction;
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
        this.storeFactory = delegate;
    }

    @Override
    public Scope create(String name, ResourceServer resourceServer) {
        Scope scope = getDelegate().create(name, getStoreFactory().getResourceServerStore().findById(resourceServer.getId()));

        this.transaction.whenRollback(() -> resolveResourceServerCache(resourceServer.getId()).remove(getCacheKeyForScope(scope.getId())));
        this.transaction.whenCommit(() -> {
            invalidateCache(resourceServer.getId());
        });

        return createAdapter(new CachedScope(scope));
    }

    @Override
    public void delete(String id) {
        Scope scope = getDelegate().findById(id, null);
        if (scope == null) {
            return;
        }
        ResourceServer resourceServer = scope.getResourceServer();
        getDelegate().delete(id);
        this.transaction.whenCommit(() -> {
            invalidateCache(resourceServer.getId());
        });
    }

    @Override
    public Scope findById(String id, String resourceServerId) {
        String cacheKeyForScope = getCacheKeyForScope(id);
        List<CachedScope> cached = resolveResourceServerCache(resourceServerId).get(cacheKeyForScope);

        if (cached == null) {
            Scope scope = getDelegate().findById(id, resourceServerId);

            if (scope != null) {
                CachedScope cachedScope = new CachedScope(scope);
                resolveResourceServerCache(resourceServerId).put(cacheKeyForScope, Arrays.asList(cachedScope));
                return createAdapter(cachedScope);
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public Scope findByName(String name, String resourceServerId) {
        String cacheKeyForScope = getCacheKeyForScopeName(name);
        List<CachedScope> cached = resolveResourceServerCache(resourceServerId).get(cacheKeyForScope);

        if (cached == null) {
            Scope scope = getDelegate().findByName(name, resourceServerId);

            if (scope != null) {
                resolveResourceServerCache(resourceServerId).put(cacheKeyForScope, Arrays.asList(new CachedScope(scope)));
                return findById(scope.getId(), resourceServerId);
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public List<Scope> findByResourceServer(String id) {
        return getDelegate().findByResourceServer(id);
    }

    @Override
    public List<Scope> findByResourceServer(Map<String, String[]> attributes, String resourceServerId, int firstResult, int maxResult) {
        return getDelegate().findByResourceServer(attributes, resourceServerId, firstResult, maxResult);
    }

    private String getCacheKeyForScope(String id) {
        return SCOPE_ID_CACHE_PREFIX + id;
    }

    private String getCacheKeyForScopeName(String name) {
        return SCOPE_NAME_CACHE_PREFIX + name;
    }

    private ScopeStore getDelegate() {
        if (this.delegate == null) {
            this.delegate = getStoreFactory().getScopeStore();
        }

        return this.delegate;
    }

    private StoreFactory getStoreFactory() {
        return this.storeFactory;
    }

    private Scope createAdapter(CachedScope cached) {
        return new Scope() {

            private Scope updated;

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

            private Scope getDelegateForUpdate() {
                if (this.updated == null) {
                    this.updated = getDelegate().findById(getId(), cached.getResourceServerId());
                    if (this.updated == null) throw new IllegalStateException("Not found in database");
                    transaction.whenCommit(() -> {
                        invalidateCache(cached.getResourceServerId());
                    });
                    transaction.whenRollback(() -> {
                        resolveResourceServerCache(cached.getResourceServerId()).remove(getCacheKeyForScope(cached.getId()));
                        resolveResourceServerCache(cached.getResourceServerId()).remove(getCacheKeyForScopeName(cached.getName()));
                    });
                }

                return this.updated;
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || !Scope.class.isInstance(o)) return false;
                Scope that = (Scope) o;
                return Objects.equals(getId(), that.getId());
            }

            @Override
            public int hashCode() {
                return Objects.hash(getId());
            }
        };
    }

    private CachedStoreFactoryProvider getCachedStoreFactory() {
        return cacheStoreFactory;
    }

    private void invalidateCache(String resourceServerId) {
        cache.remove(resourceServerId);
    }

    private Map<String, List<CachedScope>> resolveResourceServerCache(String id) {
        return cache.computeIfAbsent(id, key -> new HashMap<>());
    }
}
