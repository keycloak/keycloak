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

package org.keycloak.models.sessions.infinispan;

import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.context.Flag;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CacheDecorators {

    /**
     * Adds {@link Flag#CACHE_MODE_LOCAL} flag to the cache.
     * @param cache
     * @return Cache with the flag applied.
     */
    public static <K, V> AdvancedCache<K, V> localCache(Cache<K, V> cache) {
        return cache.getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL);
    }

    /**
     * Adds {@link Flag#SKIP_CACHE_LOAD} and {@link Flag#SKIP_CACHE_STORE} flags to the cache.
     * @param cache
     * @return Cache with the flags applied.
     */
    public static <K, V> AdvancedCache<K, V> skipCacheLoaders(Cache<K, V> cache) {
        return cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE);
    }

    /**
     * Adds {@link Flag#SKIP_CACHE_STORE} flag to the cache.
     * @param cache
     * @return Cache with the flags applied.
     */
    public static <K, V> AdvancedCache<K, V> skipCacheStore(Cache<K, V> cache) {
        return cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_STORE);
    }

}
