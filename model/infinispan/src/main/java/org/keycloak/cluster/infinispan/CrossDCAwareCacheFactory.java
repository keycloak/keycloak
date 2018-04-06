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

package org.keycloak.cluster.infinispan;

import java.io.Serializable;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.persistence.remote.RemoteStore;
import org.jboss.logging.Logger;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
abstract class CrossDCAwareCacheFactory {

    protected static final Logger logger = Logger.getLogger(CrossDCAwareCacheFactory.class);


    abstract BasicCache<String, Serializable> getCache();


    static CrossDCAwareCacheFactory getFactory(Cache<String, Serializable> workCache, Set<RemoteStore> remoteStores) {
        if (remoteStores.isEmpty()) {
            logger.debugf("No configured remoteStore available. Cross-DC scenario is not used");
            return new InfinispanCacheWrapperFactory(workCache);
        } else {
            logger.debugf("RemoteStore is available. Cross-DC scenario will be used");

            if (remoteStores.size() > 1) {
                logger.warnf("More remoteStores configured for work cache. Will use just the first one");
            }

            // For cross-DC scenario, we need to return underlying remoteCache for atomic operations to work properly
            RemoteStore remoteStore = remoteStores.iterator().next();
            RemoteCache remoteCache = remoteStore.getRemoteCache();

            if (remoteCache == null) {
                String cacheName = remoteStore.getConfiguration().remoteCacheName();
                throw new IllegalStateException("Remote cache '" + cacheName + "' is not available.");
            }

            return new RemoteCacheWrapperFactory(remoteCache);
        }
    }


    // We don't have external JDG configured. No cross-DC.
    private static class InfinispanCacheWrapperFactory extends CrossDCAwareCacheFactory {

        private final Cache<String, Serializable> workCache;

        InfinispanCacheWrapperFactory(Cache<String, Serializable> workCache) {
            this.workCache = workCache;
        }

        @Override
        BasicCache<String, Serializable> getCache() {
            return workCache;
        }

    }


    // We have external JDG configured. Cross-DC should be enabled
    private static class RemoteCacheWrapperFactory extends CrossDCAwareCacheFactory {

        private final RemoteCache<String, Serializable> remoteCache;

        RemoteCacheWrapperFactory(RemoteCache<String, Serializable> remoteCache) {
            this.remoteCache = remoteCache;
        }

        @Override
        BasicCache<String, Serializable> getCache() {
            // Flags are per-invocation!
            return remoteCache.withFlags(Flag.FORCE_RETURN_VALUE);
        }

    }
}
