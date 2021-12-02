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
package org.keycloak.adapters.authorization;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import org.keycloak.common.util.Time;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig;
import org.keycloak.representations.adapters.config.PolicyEnforcerConfig.PathConfig;

/**
 * A simple LRU cache implementation supporting expiration and maximum number of entries.
 *
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class PathCache {

    /**
     * The load factor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final Map<String, CacheEntry> cache;

    private final AtomicBoolean writing = new AtomicBoolean(false);

    private final long maxAge;
    private final boolean enabled;
    private final Map<String, PathConfig> paths;

    /**
     * Creates a new instance.
     *  @param maxEntries the maximum number of entries to keep in the cache
     * @param maxAge the time in milliseconds that an entry can stay in the cache. If {@code -1}, entries never expire
     * @param paths the pre-configured paths
     */
    public PathCache(final int maxEntries, long maxAge,
            Map<String, PathConfig> paths) {
        cache = new LinkedHashMap<String, CacheEntry>(16, DEFAULT_LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return cache.size()  > maxEntries;
            }
        };
        this.maxAge = maxAge;
        this.enabled = ! (maxAge < -1 || (maxAge > -1 && maxAge <= 0));
        this.paths = paths;
    }

    public void put(String uri, PathConfig newValue) {
        if (!enabled) {
            if (newValue != null) {
                // if disabled we also remove from the pre-defined paths map
                markForInvalidation(newValue);
            }
            return;
        }

        try {
            if (parkForWriteAndCheckInterrupt()) {
                return;
            }

            CacheEntry cacheEntry = cache.get(uri);

            if (cacheEntry == null) {
                cache.put(uri, new CacheEntry(uri, newValue, maxAge));
            }
        } finally {
            writing.lazySet(false);
        }
    }

    private void markForInvalidation(PathConfig newValue) {
        PathConfig pathConfig = paths.get(newValue.getPath());
        
        if (pathConfig != null && !pathConfig.isStatic()) {
            // invalidate the configuration so that the path config is reload based on latest changes on the server
            pathConfig.invalidate();       
        }
    }

    public boolean containsKey(String uri) {
        return cache.containsKey(uri);
    }

    public PathConfig get(String uri) {
        if (parkForReadAndCheckInterrupt()) {
            return null;
        }

        CacheEntry cached = cache.get(uri);

        if (cached != null) {
            return removeIfExpired(cached);
        }

        return null;
    }

    public void remove(String key) {
        try {
            if (parkForWriteAndCheckInterrupt()) {
                return;
            }

            cache.remove(key);
        } finally {
            writing.lazySet(false);
        }
    }

    private PathConfig removeIfExpired(CacheEntry cached) {
        if (cached == null) {
            return null;
        }

        PathConfig config = cached.value();

        if (cached.isExpired()) {
            remove(cached.key());
            
            if (config != null && config.getPath() != null) {
                // also remove from pre-defined paths map so that changes on the server are properly reflected
                markForInvalidation(config);
            }
            return null;
        }

        return config;
    }

    private boolean parkForWriteAndCheckInterrupt() {
        while (!writing.compareAndSet(false, true)) {
            LockSupport.parkNanos(1L);
            if (Thread.interrupted()) {
                return true;
            }
        }
        return false;
    }

    private boolean parkForReadAndCheckInterrupt() {
        while (writing.get()) {
            LockSupport.parkNanos(1L);
            if (Thread.interrupted()) {
                return true;
            }
        }
        return false;
    }

    public int size() {
        return cache.size();
    }

    private static final class CacheEntry {

        final String key;
        final PathConfig value;
        final long expiration;

        CacheEntry(String key, PathConfig value, long maxAge) {
            this.key = key;
            this.value = value;
            if(maxAge == -1) {
                expiration = -1;
            } else {
                expiration = Time.currentTimeMillis() + maxAge;
            }
        }

        String key() {
            return key;
        }

        PathConfig value() {
            return value;
        }

        boolean isExpired() {
            return expiration != -1 ? Time.currentTimeMillis() > expiration : false;
        }
    }
}
