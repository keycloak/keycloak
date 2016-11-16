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

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.remote.RemoteStore;
import org.infinispan.persistence.remote.configuration.ExhaustedAction;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.junit.Ignore;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

/**
 * Test concurrency for remoteStore (backed by HotRod RemoteCaches) against external JDG
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Ignore
public class ConcurrencyJDGRemoteCacheTest {

    private static Map<String, EntryInfo> state = new HashMap<>();

    public static void main(String[] args) throws Exception {
        // Init map somehow
        for (int i=0 ; i<100 ; i++) {
            String key = "key-" + i;
            state.put(key, new EntryInfo());
        }

        // Create caches, listeners and finally worker threads
        Worker worker1 = createWorker(1);
        Worker worker2 = createWorker(2);

        // Start and join workers
        worker1.start();
        worker2.start();

        worker1.join();
        worker2.join();

        // Output
        for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
            System.out.println(entry.getKey() + ":::" + entry.getValue());
            worker1.cache.remove(entry.getKey());
        }

        // Finish JVM
        worker1.cache.getCacheManager().stop();
        worker2.cache.getCacheManager().stop();
    }

    private static Worker createWorker(int threadId) {
        EmbeddedCacheManager manager = createManager(threadId);
        Cache<String, Integer> cache = manager.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

        System.out.println("Retrieved cache: " + threadId);

        RemoteStore remoteStore = cache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).getStores(RemoteStore.class).iterator().next();
        HotRodListener listener = new HotRodListener();
        remoteStore.getRemoteCache().addClientListener(listener);

        return new Worker(cache, threadId);
    }

    private static EmbeddedCacheManager createManager(int threadId) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jgroups.tcp.port", "53715");
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();

        boolean clustered = false;
        boolean async = false;
        boolean allowDuplicateJMXDomains = true;

        if (clustered) {
            gcb = gcb.clusteredDefault();
            gcb.transport().clusterName("test-clustering");
        }

        gcb.globalJmxStatistics().allowDuplicateDomains(allowDuplicateJMXDomains);

        EmbeddedCacheManager cacheManager = new DefaultCacheManager(gcb.build());

        Configuration invalidationCacheConfiguration = getCacheBackedByRemoteStore(threadId);

        cacheManager.defineConfiguration(InfinispanConnectionProvider.WORK_CACHE_NAME, invalidationCacheConfiguration);
        return cacheManager;

    }

    private static Configuration getCacheBackedByRemoteStore(int threadId) {
        ConfigurationBuilder cacheConfigBuilder = new ConfigurationBuilder();

        // int port = threadId==1 ? 11222 : 11322;
        int port = 11222;

        return cacheConfigBuilder.persistence().addStore(RemoteStoreConfigurationBuilder.class)
                .fetchPersistentState(false)
                .ignoreModifications(false)
                .purgeOnStartup(false)
                .preload(false)
                .shared(true)
                .remoteCacheName(InfinispanConnectionProvider.WORK_CACHE_NAME)
                .rawValues(true)
                .forceReturnValues(false)
                .addServer()
                    .host("localhost")
                    .port(port)
                .connectionPool()
                    .maxActive(20)
                    .exhaustedAction(ExhaustedAction.CREATE_NEW)
                .async()
                .   enabled(false).build();
    }


    @ClientListener
    public static class HotRodListener {

        //private AtomicInteger listenerCount = new AtomicInteger(0);

        @ClientCacheEntryCreated
        public void created(ClientCacheEntryCreatedEvent event) {
            String cacheKey = (String) event.getKey();
            state.get(cacheKey).successfulListenerWrites.incrementAndGet();
        }

        @ClientCacheEntryModified
        public void updated(ClientCacheEntryModifiedEvent event) {
            String cacheKey = (String) event.getKey();
            state.get(cacheKey).successfulListenerWrites.incrementAndGet();
        }

    }


    private static class Worker extends Thread {

        private final Cache<String, Integer> cache;

        private final int myThreadId;

        private Worker(Cache<String, Integer> cache, int myThreadId) {
            this.cache = cache;
            this.myThreadId = myThreadId;
        }

        @Override
        public void run() {
            for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
                String cacheKey = entry.getKey();
                EntryInfo wrapper = state.get(cacheKey);

                int val = getClusterStartupTime(this.cache, cacheKey, wrapper);
                if (myThreadId == 1) {
                    wrapper.th1.set(val);
                } else {
                    wrapper.th2.set(val);
                }

            }

            System.out.println("Worker finished: " + myThreadId);
        }

    }

    public static int getClusterStartupTime(Cache<String, Integer> cache, String cacheKey, EntryInfo wrapper) {
        int startupTime = new Random().nextInt(1024);

        // Concurrency doesn't work correctly with this
        //Integer existingClusterStartTime = (Integer) cache.putIfAbsent(cacheKey, startupTime);

        // Concurrency works fine with this
        RemoteCache remoteCache = cache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).getStores(RemoteStore.class).iterator().next().getRemoteCache();
        Integer existingClusterStartTime = (Integer) remoteCache.withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(cacheKey, startupTime);

        if (existingClusterStartTime == null) {
            wrapper.successfulInitializations.incrementAndGet();
            return startupTime;
        } else {
            return existingClusterStartTime;
        }
    }

    private static class EntryInfo {
        AtomicInteger successfulInitializations = new AtomicInteger(0);
        AtomicInteger successfulListenerWrites = new AtomicInteger(0);
        AtomicInteger th1 = new AtomicInteger();
        AtomicInteger th2 = new AtomicInteger();

        @Override
        public String toString() {
            return String.format("Inits: %d, listeners: %d, th1: %d, th2: %d", successfulInitializations.get(), successfulListenerWrites.get(), th1.get(), th2.get());
        }
    }



}
