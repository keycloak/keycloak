package org.keycloak.connections.infinispan.remote;

import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.commons.util.concurrent.CompletionStages;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.concurrent.BlockingManager;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.TopologyInfo;

public record RemoteInfinispanConnectionProvider(EmbeddedCacheManager embeddedCacheManager,
                                                 RemoteCacheManager remoteCacheManager,
                                                 TopologyInfo topologyInfo) implements InfinispanConnectionProvider {

    public RemoteInfinispanConnectionProvider(EmbeddedCacheManager embeddedCacheManager, RemoteCacheManager remoteCacheManager, TopologyInfo topologyInfo) {
        this.embeddedCacheManager = Objects.requireNonNull(embeddedCacheManager);
        this.remoteCacheManager = Objects.requireNonNull(remoteCacheManager);
        this.topologyInfo = Objects.requireNonNull(topologyInfo);
    }

    @Override
    public <K, V> Cache<K, V> getCache(String name, boolean createIfAbsent) {
        return embeddedCacheManager.getCache(name, createIfAbsent);
    }

    @Override
    public <K, V> RemoteCache<K, V> getRemoteCache(String name) {
        return remoteCacheManager.getCache(name);
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
        DISTRIBUTED_REPLICATED_CACHE_NAMES.stream()
                .map(this::getRemoteCache)
                .map(RemoteCache::clearAsync)
                .forEach(stage::dependsOn);
        return stage.freeze();
    }

    @Override
    public Executor getExecutor(String name) {
        return GlobalComponentRegistry.componentOf(embeddedCacheManager, BlockingManager.class).asExecutor(name);
    }

    @Override
    public void close() {
        //no-op
    }
}
