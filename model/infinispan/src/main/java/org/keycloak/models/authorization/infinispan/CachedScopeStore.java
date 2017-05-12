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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.model.Scope;
import org.keycloak.authorization.store.ScopeStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.authorization.infinispan.entities.CachedScope;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedScopeStore extends AbstractCachedStore implements ScopeStore {

    private static final String SCOPE_CACHE_PREFIX = "scp-";

    private final ScopeStore delegate;

    public CachedScopeStore(InfinispanStoreFactoryProvider cacheStoreFactory, StoreFactory storeFactory) {
        super(cacheStoreFactory, storeFactory);
        this.delegate = storeFactory.getScopeStore();
    }

    @Override
    public Scope create(String name, ResourceServer resourceServer) {
        Scope scope = getDelegate().create(name, getStoreFactory().getResourceServerStore().findById(resourceServer.getId()));

        addInvalidation(getCacheKeyForScope(scope.getId()));
        addInvalidation(getCacheKeyForScopeName(scope.getName()));
        getCachedStoreFactory().getPolicyStore().addInvalidations(scope);

        getTransaction().whenRollback(() -> removeCachedEntry(resourceServer.getId(), getCacheKeyForScope(scope.getId())));
        getTransaction().whenCommit(() -> invalidate(resourceServer.getId()));

        return createAdapter(new CachedScope(scope));
    }

    @Override
    public void delete(String id) {
        Scope scope = getDelegate().findById(id, null);

        if (scope == null) {
            return;
        }

        ResourceServer resourceServer = scope.getResourceServer();

        addInvalidation(getCacheKeyForScope(scope.getId()));
        addInvalidation(getCacheKeyForScopeName(scope.getName()));
        getCachedStoreFactory().getPolicyStore().addInvalidations(scope);

        getDelegate().delete(id);

        getTransaction().whenCommit(() -> invalidate(resourceServer.getId()));
    }

    @Override
    public Scope findById(String id, String resourceServerId) {
        String cacheKey = getCacheKeyForScope(id);

        if (isInvalid(cacheKey)) {
            return getDelegate().findById(id, resourceServerId);
        }

        List<Object> cached = resolveCacheEntry(resourceServerId, cacheKey);

        if (cached == null) {
            Scope scope = getDelegate().findById(id, resourceServerId);

            if (scope != null) {
                return createAdapter(putCacheEntry(resourceServerId, cacheKey, new CachedScope(scope)));
            }

            return null;
        }

        return createAdapter(CachedScope.class.cast(cached.get(0)));
    }

    @Override
    public Scope findByName(String name, String resourceServerId) {
        String cacheKey = getCacheKeyForScopeName(name);

        if (isInvalid(cacheKey)) {
            return getDelegate().findByName(name, resourceServerId);
        }

        return cacheResult(resourceServerId, cacheKey, () -> {
            Scope scope = getDelegate().findByName(name, resourceServerId);

            if (scope == null) {
                return Collections.emptyList();
            }

            return Arrays.asList(scope);
        }).stream().findFirst().orElse(null);
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
        return new StringBuilder(SCOPE_CACHE_PREFIX).append("id-").append(id).toString();
    }

    private String getCacheKeyForScopeName(String name) {
        return new StringBuilder(SCOPE_CACHE_PREFIX).append("findByName-").append(name).toString();
    }

    private ScopeStore getDelegate() {
        return this.delegate;
    }

    private List<Scope> cacheResult(String resourceServerId, String key, Supplier<List<Scope>> provider) {
        List<Object> cached = getCachedStoreFactory().computeIfCachedEntryAbsent(resourceServerId, key, (Function<String, List<Object>>) o -> {
            List<Scope> result = provider.get();

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
                addInvalidation(getCacheKeyForScopeName(name));
                addInvalidation(getCacheKeyForScopeName(cached.getName()));
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
                    addInvalidation(getCacheKeyForScope(updated.getId()));
                    getCachedStoreFactory().getPolicyStore().addInvalidations(updated);
                    getTransaction().whenCommit(() -> invalidate(cached.getResourceServerId()));
                    getTransaction().whenRollback(() -> removeCachedEntry(cached.getResourceServerId(), getCacheKeyForScope(cached.getId())));
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
}
