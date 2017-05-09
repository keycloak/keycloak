/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.authorization.infinispan;

import java.util.Arrays;
import java.util.List;

import org.keycloak.authorization.store.StoreFactory;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public abstract class AbstractCachedStore {

    private final InfinispanStoreFactoryProvider cacheStoreFactory;
    private final StoreFactory storeFactory;

    AbstractCachedStore(InfinispanStoreFactoryProvider cacheStoreFactory, StoreFactory storeFactory) {
        this.cacheStoreFactory = cacheStoreFactory;
        this.storeFactory = storeFactory;
    }

    protected void addInvalidation(String cacheKeyForPolicy) {
        getCachedStoreFactory().addInvalidation(cacheKeyForPolicy);
    }

    protected <E> E putCacheEntry(String resourceServerId, String cacheKeyForPolicy, E cachedPolicy) {
        cacheStoreFactory.putCacheEntry(resourceServerId, cacheKeyForPolicy, Arrays.asList(cachedPolicy));
        return cachedPolicy;
    }

    protected List<Object> resolveCacheEntry(String resourceServerId, String cacheKeyForPolicy) {
        return cacheStoreFactory.resolveCachedEntry(resourceServerId, cacheKeyForPolicy);
    }

    protected void removeCachedEntry(String resourceServerId, String key) {
        getCachedStoreFactory().removeCachedEntry(resourceServerId, key);
    }

    protected void invalidate(String resourceServerId) {
        cacheStoreFactory.invalidate(resourceServerId);
    }

    protected StoreFactory getStoreFactory() {
        return this.storeFactory;
    }

    protected boolean isInvalid(String cacheKey) {
        return cacheStoreFactory.isInvalid(cacheKey);
    }

    protected InfinispanStoreFactoryProvider.CacheTransaction getTransaction() {
        return cacheStoreFactory.getTransaction();
    }

    protected InfinispanStoreFactoryProvider getCachedStoreFactory() {
        return cacheStoreFactory;
    }
}
