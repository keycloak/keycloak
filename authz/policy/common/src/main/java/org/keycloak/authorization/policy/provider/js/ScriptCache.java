/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.authorization.policy.provider.js;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;

import org.keycloak.scripting.EvaluatableScriptAdapter;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class ScriptCache {

    /**
     * The load factor.
     */
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    private final Map<String, CacheEntry> cache;

    private final AtomicBoolean writing = new AtomicBoolean(false);

    private final long maxAge;

    /**
     * Creates a new instance.
     *
     * @param maxEntries the maximum number of entries to keep in the cache
     */
    public ScriptCache(int maxEntries) {
        this(maxEntries, -1);
    }

    /**
     * Creates a new instance.
     *
     * @param maxEntries the maximum number of entries to keep in the cache
     * @param maxAge the time in milliseconds that an entry can stay in the cache. If {@code -1}, entries never expire
     */
    public ScriptCache(final int maxEntries, long maxAge) {
        cache = Collections.synchronizedMap(new LinkedHashMap<String, CacheEntry>(16, DEFAULT_LOAD_FACTOR, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry eldest) {
                return cache.size()  > maxEntries;
            }
        });
        this.maxAge = maxAge;
    }

    public EvaluatableScriptAdapter computeIfAbsent(String id, Function<String, EvaluatableScriptAdapter> function) {
        try {
            EvaluatableScriptAdapter adapter = removeIfExpired(cache.get(id));

            if (adapter != null) {
                return adapter;
            }

            if (parkForWriteAndCheckInterrupt()) {
                return null;
            }

            CacheEntry entry = cache.computeIfAbsent(id, key -> new CacheEntry(key, function.apply(id), maxAge));

            if (entry != null) {
                return entry.value();
            }

            return null;
        } finally {
            writing.lazySet(false);
        }
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

    private EvaluatableScriptAdapter removeIfExpired(CacheEntry cached) {
        if (cached == null) {
            return null;
        }

        if (cached.isExpired()) {
            remove(cached.key());
            return null;
        }

        return cached.value();
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

    private static final class CacheEntry {

        final String key;
        final EvaluatableScriptAdapter value;
        final long expiration;

        CacheEntry(String key, EvaluatableScriptAdapter value, long maxAge) {
            this.key = key;
            this.value = value;
            if(maxAge == -1) {
                expiration = -1;
            } else {
                expiration = System.currentTimeMillis() + maxAge;
            }
        }

        String key() {
            return key;
        }

        EvaluatableScriptAdapter value() {
            return value;
        }

        boolean isExpired() {
            return expiration != -1 ? System.currentTimeMillis() > expiration : false;
        }
    }
}
