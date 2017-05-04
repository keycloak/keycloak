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
import java.util.function.Predicate;
import java.util.stream.Collectors;

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

        this.transaction.whenRollback(() -> removeCachedResourceServer(resourceServer));
        //Cache item
        return createAdapter(putCachedResourceServer(resourceServer));

    }

    @Override
    public void delete(String id) {
        ResourceServer resourceServer = getDelegate().findById(id);
        getDelegate().delete(id);
        this.transaction.whenCommit(() -> removeCachedResourceServer(resourceServer));
    }

    @Override
    public ResourceServer findById(String id) {
        String cacheKeyForResourceServer = getCacheKeyForResourceServer(id);
        List<CachedResourceServer> cached = resolveResourceServerCache(id).get(cacheKeyForResourceServer);

        if (cached == null) {
            ResourceServer resourceServer = getDelegate().findById(id);

            if (resourceServer != null) {
                CachedResourceServer cachedResourceServer = putCachedResourceServer(resourceServer);
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
                return findById(resourceServer.getId());
            }
            //Keep empty list to prevent performance issue
            else {
                putCachedResourceServer(null, id, Collections.emptyList());
            }

            return null;
        } else if (cached.isEmpty()) {
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

    /**
     * Keep resource in each cache
     *
     * @param resourceServer resource to cache
     * @return cached element instance
     */
    private CachedResourceServer putCachedResourceServer(ResourceServer resourceServer) {
        CachedResourceServer cachedResourceServer = null;
        if (resourceServer != null) {
            cachedResourceServer = new CachedResourceServer(resourceServer);
            putCachedResourceServer(resourceServer.getId(), resourceServer.getClientId(),
                    Arrays.asList(cachedResourceServer));
        }
        return cachedResourceServer;
    }

    /**
     * Keep resource in each cache
     *
     * @param id entity id
     * @param clientId associate client id
     * @param lst values to cache
     * @return cached element instance
     */
    private void putCachedResourceServer(String id, String clientId, List<CachedResourceServer> lst) {
        if (id != null) {
            resolveResourceServerCache(id).put(getCacheKeyForResourceServer(id), lst);
        }
        if (clientId != null) {
            resolveResourceServerCache(clientId).put(getCacheKeyForResourceServerClientId(clientId), lst);
        }
    }

    /**
     * Remove cached elements
     *
     * @param resourceServer resource to remove
     */
    private void removeCachedResourceServer(ResourceServer resourceServer) {
        if (resourceServer != null) {
            removeCachedResourceServer(resourceServer.getId(), resourceServer.getClientId());
        }
    }

    /**
     * Remove cached elements
     *
     * @param id entity id
     * @param clientId associate client id
     */
    private void removeCachedResourceServer(String id, String clientId) {
        if (id != null) {
            cache.remove(id);
        }
        if (clientId != null) {
            cache.remove(clientId);
        }
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
                        removeCachedResourceServer(getId(), getClientId());
                    });
                }

                return this.updated;
            }
        };
    }

    private Map<String, List<CachedResourceServer>> resolveResourceServerCache(String id) {
        return cache.computeIfAbsent(id, key -> new HashMap<>());
    }

    @Override
    public List<ResourceServer> findByClients(String... ids) {
        List<ResourceServer> results = new ArrayList<>(ids.length);

        //Get cached elements
        ids = Arrays.stream(ids).filter(id -> {
            List<CachedResourceServer> cachedElements = resolveResourceServerCache(id).get(getCacheKeyForResourceServerClientId(id));
            if (cachedElements != null && !cachedElements.isEmpty()) {
                results.add(findByClient(id));
            }
            return cachedElements == null;
        }).toArray(String[]::new);

        if (ids.length > 0) {
            getDelegate().findByClients(ids).forEach(
                    resourceServer -> {
                        //Cache item
                        putCachedResourceServer(resourceServer);
                        results.add(findById(resourceServer.getId()));
                    }
            );

            //Create empty list in cache for missing resource
            Arrays.stream(ids)
                    .filter(((Predicate<String>) results.stream().map(resourceServer -> resourceServer.getClientId())
                            .collect(Collectors.toList())::contains).negate()).forEach(id -> {
                putCachedResourceServer(null, id, Collections.emptyList());
            });
        }

        return results;
    }
}
