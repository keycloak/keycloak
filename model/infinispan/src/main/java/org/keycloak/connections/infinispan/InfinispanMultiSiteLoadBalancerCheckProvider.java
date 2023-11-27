/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.infinispan;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.manager.PersistenceManager;
import org.jboss.logging.Logger;
import org.keycloak.health.LoadBalancerCheckProvider;

import java.util.Objects;

import static org.keycloak.connections.infinispan.InfinispanConnectionProvider.ALL_CACHES_NAME;

public class InfinispanMultiSiteLoadBalancerCheckProvider implements LoadBalancerCheckProvider {
    private static final Logger LOG = Logger.getLogger(InfinispanMultiSiteLoadBalancerCheckProvider.class);
    private final EmbeddedCacheManager cacheManager;

    public InfinispanMultiSiteLoadBalancerCheckProvider(EmbeddedCacheManager cacheManager) {
        Objects.requireNonNull(cacheManager, "cacheManager");
        this.cacheManager = cacheManager;
    }

    @Override
    public boolean isDown() {
        for (String cacheName : ALL_CACHES_NAME) {
            // do not block in cache creation
            Cache<?,?> cache = cacheManager.getCache(cacheName, false);

            // check if cache is started
            if (cache == null || !cache.getStatus().allowInvocations()) {
                LOG.debugf("Cache '%s' is not started yet.", cacheName);
                return true; // no need to check other caches
            }

            PersistenceManager persistenceManager = cache.getAdvancedCache()
                    .getComponentRegistry()
                    .getComponent(PersistenceManager.class);

            if (persistenceManager != null && !persistenceManager.isAvailable()) {
                LOG.debugf("PersistenceManager for cache '%s' is down.", cacheName);
                return true; // no need to check other caches
            }
            LOG.debugf("Cache '%s' is up.", cacheName);
        }

        return false;
    }

    @Override
    public void close() {

    }
}
