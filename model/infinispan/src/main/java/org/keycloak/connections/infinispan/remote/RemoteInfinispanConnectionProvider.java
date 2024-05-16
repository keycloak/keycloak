/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.connections.infinispan.remote;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.factories.KnownComponentNames;
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
    public Executor getExecutor(String name) {
        return GlobalComponentRegistry.componentOf(embeddedCacheManager, BlockingManager.class).asExecutor(name);
    }

    @Override
    public ScheduledExecutorService getScheduledExecutor() {
        //noinspection removal
        return GlobalComponentRegistry.of(embeddedCacheManager).getComponent(ScheduledExecutorService.class, KnownComponentNames.TIMEOUT_SCHEDULE_EXECUTOR);
    }

    @Override
    public void close() {
        //no-op
    }
}
