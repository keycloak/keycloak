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

package org.keycloak.connections.infinispan.shutdown;

import java.util.Objects;

import org.infinispan.Cache;
import org.infinispan.distribution.DistributionManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.TopologyChanged;
import org.infinispan.notifications.cachelistener.event.TopologyChangedEvent;
import org.jboss.logging.Logger;

/**
 * An Infinispan cache listener that delays server shutdown until the cache topology is stable (i.e. no rehash is in
 * progress).
 * <p>
 * Use {@link #waitForStableTopology(Cache)} to register this listener on a cache. It returns a
 * {@link WaitConditionShutdownListener} that should be added to the {@link ShutdownManager}.
 * <p>
 * <b>Limitation:</b> when two or more Keycloak instances receive a shutdown signal simultaneously, each instance may
 * observe a stable topology before any of them have actually left the cluster. In this scenario, all instances proceed
 * with shutdown concurrently, increasing the risk of data loss.
 */
@Listener
public class TopologyChangeCacheListener {

    private static final Logger logger = Logger.getLogger(TopologyChangeCacheListener.class);

    private final WaitConditionShutdownListener listener;

    /**
     * Registers a topology change listener on the given cache and returns a {@link WaitConditionShutdownListener} that
     * blocks shutdown while the cache topology is unstable.
     *
     * @param cache         The cache to monitor for topology changes.
     * @return A {@link WaitConditionShutdownListener} to be registered with the {@link ShutdownManager}.
     */
    public static WaitConditionShutdownListener waitForStableTopology(Cache<?, ?> cache) {
        var dm = cache.getAdvancedCache().getDistributionManager();
        var condition = new TopologyShutdownCondition(cache.getName(), dm);
        var listener = new WaitConditionShutdownListener(condition);
        cache.addListener(new TopologyChangeCacheListener(listener));
        return listener;
    }

    private TopologyChangeCacheListener(WaitConditionShutdownListener listener) {
        this.listener = Objects.requireNonNull(listener, "listener");
    }

    @TopologyChanged
    public void onTopologyChange(TopologyChangedEvent<?, ?> event) {
        listener.check();
    }

    private record TopologyShutdownCondition(String cacheName, DistributionManager dm) implements ShutdownCondition {

        @Override
        public boolean inProgress() {
            return dm.isRehashInProgress() || !dm.isJoinComplete();
        }

        @Override
        public void onTimeout() {
            logger.warnf("Cache '%s': timed out waiting for stable topology during shutdown. Check for possible causes, or extend the shutdown timeout to allow for more time for the cache to rebalance.", cacheName);
        }

        @Override
        public void complete() {
            logger.infof("Cache '%s': topology stable, proceeding with shutdown", cacheName);
        }
    }
}
