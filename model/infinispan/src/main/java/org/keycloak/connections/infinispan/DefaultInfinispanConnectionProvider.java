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

package org.keycloak.connections.infinispan;

import java.util.Arrays;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.KnownComponentNames;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.util.concurrent.BlockingManager;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class DefaultInfinispanConnectionProvider implements InfinispanConnectionProvider {

    private final EmbeddedCacheManager cacheManager;
    private final RemoteCacheProvider remoteCacheProvider;
    private final TopologyInfo topologyInfo;

    public DefaultInfinispanConnectionProvider(EmbeddedCacheManager cacheManager, RemoteCacheProvider remoteCacheProvider, TopologyInfo topologyInfo) {
        this.cacheManager = cacheManager;
        this.remoteCacheProvider = remoteCacheProvider;
        this.topologyInfo = topologyInfo;
    }

    private static PersistenceManager persistenceManager(Cache<?, ?> cache) {
        return ComponentRegistry.componentOf(cache, PersistenceManager.class);
    }

    private static CompletionStage<Void> clearPersistenceManager(PersistenceManager persistenceManager) {
        return persistenceManager.clearAllStores(PersistenceManager.AccessMode.BOTH);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name, boolean createIfAbsent) {
        return cacheManager.getCache(name, createIfAbsent);
    }

    @Override
    public <K, V> RemoteCache<K, V> getRemoteCache(String cacheName) {
        return remoteCacheProvider.getRemoteCache(cacheName);
    }

    @Override
    public TopologyInfo getTopologyInfo() {
        return topologyInfo;
    }

    @Override
    public CompletionStage<Void> migrateToProtostream() {
        // Only the CacheStore (persistence) stores data in binary format and needs to be deleted.
        // We assume rolling-upgrade between KC 25 and KC 26 is not available, in other words, KC 25 and KC 26 servers are not present in the same cluster.
        var stage = CompletionStages.aggregateCompletionStage();
        Arrays.stream(CLUSTERED_CACHE_NAMES)
                .map(this::getCache)
                .map(DefaultInfinispanConnectionProvider::persistenceManager)
                .map(DefaultInfinispanConnectionProvider::clearPersistenceManager)
                .forEach(stage::dependsOn);
        return stage.freeze();
    }

    @Override
    public Executor getExecutor(String name) {
        return GlobalComponentRegistry.componentOf(cacheManager, BlockingManager.class).asExecutor(name);
    }

    @Override
    public ScheduledExecutorService getScheduledExecutor() {
        //noinspection removal
        return GlobalComponentRegistry.of(cacheManager).getComponent(ScheduledExecutorService.class, KnownComponentNames.TIMEOUT_SCHEDULE_EXECUTOR);
    }

    @Override
    public void close() {
    }

}
