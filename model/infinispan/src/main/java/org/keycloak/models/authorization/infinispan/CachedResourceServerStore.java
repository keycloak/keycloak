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

import org.infinispan.Cache;
import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.authorization.infinispan.InfinispanStoreFactoryProvider.CacheTransaction;
import org.keycloak.models.authorization.infinispan.entities.CachedResourceServer;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedResourceServerStore implements ResourceServerStore {

    private static final String RS_ID_CACHE_PREFIX = "rs-id-";
    private static final String RS_CLIENT_ID_CACHE_PREFIX = "rs-client-id-";

    private final CacheTransaction transaction;
    private StoreFactory storeFactory;
    private ResourceServerStore delegate;
    private final Cache<String, Map<String, List<CachedResourceServer>>> cache;

    public CachedResourceServerStore(KeycloakSession session, CacheTransaction transaction, StoreFactory delegate) {
        this.transaction = transaction;
        InfinispanConnectionProvider provider = session.getProvider(InfinispanConnectionProvider.class);
        this.cache = provider.getCache(InfinispanConnectionProvider.AUTHORIZATION_CACHE_NAME);
        this.storeFactory = delegate;
    }

    @Override
    public ResourceServer create(String clientId) {
        ResourceServer resourceServer = getDelegate().create(clientId);

        this.transaction.whenRollback(() -> resolveResourceServerCache(resourceServer.getId()).remove(getCacheKeyForResourceServer(resourceServer.getId())));

        return createAdapter(new CachedResourceServer(resourceServer));
    }

    @Override
    public void delete(String id) {
        ResourceServer resourceServer = getDelegate().findById(id);
        getDelegate().delete(id);
        this.transaction.whenCommit(() -> {
            cache.remove(id);
            cache.remove(resourceServer.getClientId());
        });
    }

    @Override
    public ResourceServer findById(String id) {
        String cacheKeyForResourceServer = getCacheKeyForResourceServer(id);
        List<CachedResourceServer> cached = resolveResourceServerCache(id).get(cacheKeyForResourceServer);

        if (cached == null) {
            ResourceServer resourceServer = getDelegate().findById(id);

            if (resourceServer != null) {
                CachedResourceServer cachedResourceServer = new CachedResourceServer(resourceServer);
                resolveResourceServerCache(id).put(cacheKeyForResourceServer, Arrays.asList(cachedResourceServer));
                return createAdapter(cachedResourceServer);
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    @Override
    public ResourceServer findByClient(String id) {
        String cacheKeyForResourceServer = getCacheKeyForResourceServerClientId(id);
        List<CachedResourceServer> cached = resolveResourceServerCache(id).get(cacheKeyForResourceServer);

        if (cached == null) {
            ResourceServer resourceServer = getDelegate().findByClient(id);

            if (resourceServer != null) {
                resolveResourceServerCache(cacheKeyForResourceServer).put(cacheKeyForResourceServer, Arrays.asList(new CachedResourceServer(resourceServer)));
                return findById(resourceServer.getId());
            }

            return null;
        }

        return createAdapter(cached.get(0));
    }

    private String getCacheKeyForResourceServer(String id) {
        return RS_ID_CACHE_PREFIX + id;
    }

    private String getCacheKeyForResourceServerClientId(String id) {
        return RS_CLIENT_ID_CACHE_PREFIX + id;
    }

    private ResourceServerStore getDelegate() {
        if (this.delegate == null) {
            this.delegate = getStoreFactory().getResourceServerStore();
        }

        return this.delegate;
    }

    private StoreFactory getStoreFactory() {
        return this.storeFactory;
    }
    private ResourceServer createAdapter(ResourceServer cached) {
        return new ResourceServer() {

            private ResourceServer updated;

            @Override
            public String getId() {
                return cached.getId();
            }

            @Override
            public String getClientId() {
                return cached.getClientId();
            }

            @Override
            public boolean isAllowRemoteResourceManagement() {
                return cached.isAllowRemoteResourceManagement();
            }

            @Override
            public void setAllowRemoteResourceManagement(boolean allowRemoteResourceManagement) {
                getDelegateForUpdate().setAllowRemoteResourceManagement(allowRemoteResourceManagement);
                cached.setAllowRemoteResourceManagement(allowRemoteResourceManagement);
            }

            @Override
            public PolicyEnforcementMode getPolicyEnforcementMode() {
                return cached.getPolicyEnforcementMode();
            }

            @Override
            public void setPolicyEnforcementMode(PolicyEnforcementMode enforcementMode) {
                getDelegateForUpdate().setPolicyEnforcementMode(enforcementMode);
                cached.setPolicyEnforcementMode(enforcementMode);
            }

            private ResourceServer getDelegateForUpdate() {
                if (this.updated == null) {
                    this.updated = getDelegate().findById(getId());
                    if (this.updated == null) throw new IllegalStateException("Not found in database");
                    transaction.whenCommit(() -> {
                        cache.remove(getId());
                        cache.remove(getClientId());
                    });
                }

                return this.updated;
            }
        };
    }

    private Map<String, List<CachedResourceServer>> resolveResourceServerCache(String id) {
        return cache.computeIfAbsent(id, key -> new HashMap<>());
    }
}
