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
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.authorization.infinispan.InfinispanStoreFactoryProvider.CacheTransaction;
import org.keycloak.models.authorization.infinispan.entities.CachedScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedScopeStore implements ScopeStore {

    private static final String SCOPE_ID_CACHE_PREFIX = "scp-id-";

    private final Cache<String, List> cache;
    private final KeycloakSession session;
    private final CacheTransaction transaction;
    private ScopeStore delegate;
    private StoreFactory storeFactory;

    public CachedScopeStore(KeycloakSession session, CacheTransaction transaction) {
        this.session = session;
        this.transaction = transaction;
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
    }

    @Override
    public Scope create(String name, ResourceServer resourceServer) {
        Scope scope = getDelegate().create(name, getStoreFactory().getResourceServerStore().findById(resourceServer.getId()));

        this.transaction.whenRollback(() -> cache.remove(getCacheKeyForScope(scope.getId())));

        return createAdapter(new CachedScope(scope));
    }

    @Override
    public void delete(String id) {
        getDelegate().delete(id);
        this.transaction.whenCommit(() -> cache.remove(getCacheKeyForScope(id)));
    }

    @Override
    public Scope findById(String id) {
        String cacheKeyForScope = getCacheKeyForScope(id);
        List<CachedScope> cached = this.cache.get(cacheKeyForScope);

        if (cached == null) {
            Scope scope = getDelegate().findById(id);

            if (scope != null) {
                return createAdapter(updateScopeCache(scope));
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public Scope findByName(String name, String resourceServerId) {
        for (Entry entry : this.cache.entrySet()) {
            String cacheKey = (String) entry.getKey();

            if (cacheKey.startsWith(SCOPE_ID_CACHE_PREFIX)) {
                List<CachedScope> cache = (List<CachedScope>) entry.getValue();
                CachedScope scope = cache.get(0);

                if (scope.getResourceServerId().equals(resourceServerId) && scope.getName().equals(name)) {
                    return findById(scope.getId());
                }
            }
        }

        Scope scope = getDelegate().findByName(name, resourceServerId);

        if (scope != null) {
            return findById(updateScopeCache(scope).getId());
        }

        return null;
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

    private ScopeStore getDelegate() {
        if (this.delegate == null) {
            this.delegate = getStoreFactory().getScopeStore();
        }

        return this.delegate;
    }

    private StoreFactory getStoreFactory() {
        if (this.storeFactory == null) {
            this.storeFactory = session.getProvider(StoreFactory.class);
        }

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
                return getStoreFactory().getResourceServerStore().findById(cached.getResourceServerId());
            }

            private Scope getDelegateForUpdate() {
                if (this.updated == null) {
                    this.updated = getDelegate().findById(getId());
                    if (this.updated == null) throw new IllegalStateException("Not found in database");
                    transaction.whenCommit(() -> cache.remove(getCacheKeyForScope(getId())));
                }

                return this.updated;
            }
        };
    }

    private CachedScope updateScopeCache(Scope scope) {
        CachedScope cached = new CachedScope(scope);

        List cache = new ArrayList();

        cache.add(cached);

        this.transaction.whenCommit(() -> this.cache.put(getCacheKeyForScope(scope.getId()), cache));

        return cached;
    }
}
