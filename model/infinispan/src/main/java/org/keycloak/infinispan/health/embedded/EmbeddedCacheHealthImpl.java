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

package org.keycloak.infinispan.health.embedded;

import java.lang.invoke.MethodHandles;
import java.util.Objects;

import org.keycloak.infinispan.health.ClusterHealth;

import org.infinispan.factories.ComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.manager.PersistenceManager;
import org.jboss.logging.Logger;

/**
 * Health check for the embedded Infinispan cache manager, verifying that all caches allow invocations.
 */
public class EmbeddedCacheHealthImpl implements ClusterHealth {

    private static final Logger logger = Logger.getLogger(MethodHandles.lookup().lookupClass());

    private final EmbeddedCacheManager cacheManager;
    private volatile boolean healthy = false;

    public EmbeddedCacheHealthImpl(EmbeddedCacheManager cacheManager) {
        this.cacheManager = Objects.requireNonNull(cacheManager);
    }

    @Override
    public boolean isHealthy() {
        return healthy;
    }

    @Override
    public void triggerClusterHealthCheck() {
        checkHealth();
    }

    @Override
    public boolean isSupported() {
        return true;
    }

    private void checkHealth() {
        var cacheNames = cacheManager.getCacheNames();
        for (var name : cacheNames) {
            try {
                var cache = cacheManager.getCache(name, false);
                if (cache == null || !cache.getStatus().allowInvocations()) {
                    logger.debugf("Embedded cache '%s' is not ready", name);
                    healthy = false;
                    return;
                }

                var persistenceManager = ComponentRegistry.componentOf(cache, PersistenceManager.class);
                if (persistenceManager != null && !persistenceManager.isAvailable()) {
                    logger.debugf("Persistence for embedded cache '%s' is down.", name);
                    healthy = false;
                    return;
                }
            } catch (Exception e) {
                logger.debugf(e, "Failed to check embedded cache '%s'", name);
                healthy = false;
                return;
            }
        }
        healthy = true;
    }
}
