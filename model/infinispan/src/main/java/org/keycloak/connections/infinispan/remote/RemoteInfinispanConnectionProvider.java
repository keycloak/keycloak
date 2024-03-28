package org.keycloak.connections.infinispan.remote;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.util.concurrent.BlockingManager;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.TopologyInfo;

import java.util.Objects;
import java.util.concurrent.Executor;

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
    public Executor getExecutor(String name) {
        //noinspection removal
        return embeddedCacheManager.getGlobalComponentRegistry().getComponent(BlockingManager.class).asExecutor(name);
    }

    @Override
    public void close() {
        //no-op
    }
}
