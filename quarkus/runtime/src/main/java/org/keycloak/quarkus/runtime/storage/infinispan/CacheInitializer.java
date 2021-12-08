/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.quarkus.runtime.storage.infinispan;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.manager.DefaultCacheManager;
import org.jboss.logging.Logger;

public class CacheInitializer implements Callable<DefaultCacheManager> {

    private final ConfigurationBuilderHolder config;
    private DefaultCacheManager cacheManager;
    private Future<DefaultCacheManager> cacheManagerFuture;
    private ExecutorService executor;
    private boolean terminated;

    public CacheInitializer(ConfigurationBuilderHolder config) {
        this.config = config;
        this.executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "keycloak-cache-init");
            }
        });
        this.cacheManagerFuture = executor.submit(this);
    }

    @Override
    public DefaultCacheManager call() {
        // eagerly starts caches
        return new DefaultCacheManager(config, true);
    }

    public DefaultCacheManager getCacheManager() {
        if (cacheManager == null) {
            if (terminated) {
                return null;
            }

            try {
                // for now, we don't have any explicit property for setting the cache start timeout
                return cacheManager = cacheManagerFuture.get(Integer.getInteger("kc.cluster.cache.start-timeout", 120), TimeUnit.SECONDS);
            } catch (Exception e) {
                throw new RuntimeException("Failed to bootstrap cache manager", e);
            } finally {
                shutdownExecutor();
            }
        }

        return cacheManager;
    }

    private void shutdownExecutor() {
        if (executor != null) {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                    if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                        Logger.getLogger(CacheInitializer.class).warn("Cache init thread pool not terminated");
                    }
                }
            } catch (Exception cause) {
                executor.shutdownNow();
            } finally {
                executor = null;
                cacheManagerFuture = null;
                terminated = true;
            }
        }
    }
}
