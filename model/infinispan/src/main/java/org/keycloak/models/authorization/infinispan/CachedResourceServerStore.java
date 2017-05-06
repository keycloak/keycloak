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

import java.util.List;

import org.keycloak.authorization.model.ResourceServer;
import org.keycloak.authorization.store.ResourceServerStore;
import org.keycloak.authorization.store.StoreFactory;
import org.keycloak.models.authorization.infinispan.entities.CachedResourceServer;
import org.keycloak.representations.idm.authorization.PolicyEnforcementMode;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class CachedResourceServerStore extends AbstractCachedStore implements ResourceServerStore {

    private static final String RS_PREFIX = "rs-";

    private final ResourceServerStore delegate;

    public CachedResourceServerStore(InfinispanStoreFactoryProvider cachedStoreFactory, StoreFactory storeFactory) {
        super(cachedStoreFactory, storeFactory);
        this.delegate = storeFactory.getResourceServerStore();
    }

    @Override
    public ResourceServer create(String clientId) {
        ResourceServer resourceServer = getDelegate().create(clientId);

        getTransaction().whenCommit(() -> getCachedStoreFactory().removeEntries(resourceServer));
        getTransaction().whenRollback(() -> removeCachedEntry(resourceServer.getId(), getCacheKeyForResourceServer(resourceServer.getId())));

        return createAdapter(new CachedResourceServer(resourceServer));
    }

    @Override
    public void delete(String id) {
        ResourceServer resourceServer = getDelegate().findById(id);

        if (resourceServer != null) {
            getDelegate().delete(id);
            getTransaction().whenCommit(() -> getCachedStoreFactory().removeEntries(resourceServer));
        }
    }

    @Override
    public ResourceServer findById(String id) {
        String cacheKey = getCacheKeyForResourceServer(id);

        if (isInvalid(cacheKey)) {
            return getDelegate().findById(id);
        }

        List<Object> cached = resolveCacheEntry(id, cacheKey);

        if (cached == null) {
            ResourceServer resourceServer = getDelegate().findById(id);

            if (resourceServer != null) {
                return createAdapter(putCacheEntry(id, cacheKey, new CachedResourceServer(resourceServer)));
            }

            return null;
        }

        return createAdapter(CachedResourceServer.class.cast(cached.get(0)));
    }

    @Override
    public ResourceServer findByClient(String id) {
        String cacheKey = getCacheKeyForResourceServerClientId(id);

        if (isInvalid(cacheKey)) {
            return getDelegate().findByClient(id);
        }

        List<Object> cached = resolveCacheEntry(id, cacheKey);

        if (cached == null) {
            ResourceServer resourceServer = getDelegate().findByClient(id);

            if (resourceServer != null) {
                return findById(putCacheEntry(id, cacheKey, resourceServer.getId()));
            }

            return null;
        }

        return findById(cached.get(0).toString());
    }

    private String getCacheKeyForResourceServer(String id) {
        return new StringBuilder(RS_PREFIX).append("id-").append(id).toString();
    }

    private String getCacheKeyForResourceServerClientId(String id) {
        return new StringBuilder(RS_PREFIX).append("findByClientId-").append(id).toString();
    }

    private ResourceServerStore getDelegate() {
        return this.delegate;
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
                    addInvalidation(getCacheKeyForResourceServer(updated.getId()));
                    getTransaction().whenCommit(() -> {
                        invalidate(updated.getId());
                    });
                }

                return this.updated;
            }
        };
    }
}
