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
import java.util.List;
import java.util.Map;

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

    private final Cache<String, List> cache;
    private final KeycloakSession session;
    private final CacheTransaction transaction;
    private ScopeStore delegate;
    private StoreFactory storeFactory;

    public CachedScopeStore(KeycloakSession session, CacheTransaction transaction, StoreFactory storeFactory) {
        this.session = session;
        this.transaction = transaction;
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
        this.storeFactory = storeFactory;
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
        this.transaction.whenCommit(() -> {
            List<CachedScope> scopes = cache.remove(getCacheKeyForScope(id));

            if (scopes != null) {
                CachedScope entry = scopes.get(0);
                cache.remove(getCacheKeyForScopeName(entry.getName(), entry.getResourceServerId()));
            }
        });
    }

    @Override
    public Scope findById(String id, String resourceServerId) {
        String cacheKeyForScope = getCacheKeyForScope(id);
        List<CachedScope> cached = this.cache.get(cacheKeyForScope);

        if (cached == null) {
            Scope scope = getDelegate().findById(id, resourceServerId);

            if (scope != null) {
                return createAdapter(updateScopeCache(scope));
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public Scope findByName(String name, String resourceServerId) {
        String cacheKeyForScope = getCacheKeyForScopeName(name, resourceServerId);
        List<String> cached = this.cache.get(cacheKeyForScope);

        if (cached == null) {
            Scope scope = getDelegate().findByName(name, resourceServerId);

            if (scope != null) {
                cache.put(cacheKeyForScope, Arrays.asList(scope.getId()));
                return findById(scope.getId(), resourceServerId);
            }

            return null;
        }

        return findById(cached.get(0), resourceServerId);
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

    private String getCacheKeyForScopeName(String name, String resourceServerId) {
        return SCOPE_NAME_CACHE_PREFIX + name + "-" + resourceServerId;
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
                        cache.remove(getCacheKeyForScope(getId()));
                        getCachedStoreFactory().getPolicyStore().notifyChange(updated);
                    });
                }

                return this.updated;
            }
        };
    }

    private CachedStoreFactoryProvider getCachedStoreFactory() {
        return session.getProvider(CachedStoreFactoryProvider.class);
    }

    private CachedScope updateScopeCache(Scope scope) {
        CachedScope cached = new CachedScope(scope);

        List cache = new ArrayList();

        cache.add(cached);

        this.transaction.whenCommit(() -> this.cache.put(getCacheKeyForScope(scope.getId()), cache));

        return cached;
    }
}
