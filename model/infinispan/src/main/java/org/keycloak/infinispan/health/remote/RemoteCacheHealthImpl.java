/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.infinispan.health.remote;

import java.lang.invoke.MethodHandles;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.infinispan.health.ClusterHealth;

import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.impl.InternalRemoteCache;
import org.jboss.logging.Logger;

/**
 * Health check for the external Infinispan cluster, verifying connectivity by pinging each remote cache.
 */
public class RemoteCacheHealthImpl implements ClusterHealth {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final RemoteCacheManager remoteCacheManager;
    private final Executor executor;
    private final AtomicBoolean inProgress = new AtomicBoolean(false);
    private volatile boolean healthy = false;

    public RemoteCacheHealthImpl(RemoteCacheManager remoteCacheManager, Executor executor) {
        this.remoteCacheManager = Objects.requireNonNull(remoteCacheManager);
        this.executor = Objects.requireNonNull(executor);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public void triggerClusterHealthCheck() {
        executor.execute(this::checkHealth);
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    private void checkHealth() {
        if (!inProgress.compareAndSet(false, true)) {
            logger.debug("A thread is running. Ignoring health check request");
            return;
        }
        try {
            Set<String> cacheNames;
            try {
                cacheNames = remoteCacheManager.getCacheNames();
            } catch (Exception e) {
                logger.debugf(e, "Failed to retrieve remote cache names");
                healthy = false;
                return;
            }
            for (var name : cacheNames) {
                try {
                    var cache = remoteCacheManager.getCache(name);
                    if (cache instanceof InternalRemoteCache<?, ?> internalCache) {
                        var response = internalCache.ping().toCompletableFuture().join();
                        if (response == null || !response.isSuccess()) {
                            logger.warnf("Ping failed for remote cache '%s'", name);
                            healthy = false;
                            return;
                        }
                    }
                } catch (Exception e) {
                    logger.debugf(e, "Failed to ping remote cache '%s'", name);
                    healthy = false;
                    return;
                }
            }
            healthy = true;
        } finally {
            inProgress.set(false);
        }
    }
}
