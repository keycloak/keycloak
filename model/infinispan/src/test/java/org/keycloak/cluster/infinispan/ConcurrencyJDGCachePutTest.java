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
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.remote.RemoteStore;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;

/**
 * Test concurrency for remoteStore (backed by HotRod RemoteCaches) against external JDG. Especially tests "putIfAbsent" contract.
 *
 * Steps: {@see ConcurrencyJDGRemoteCacheClientListenersTest}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrencyJDGCachePutTest {

    private static Map<String, EntryInfo> state = new HashMap<>();

    private RemoteCache remoteCache1;
    private RemoteCache remoteCache2;


    public static void main(String[] args) throws Exception {
        // Init map somehow
        for (int i=0 ; i<1000 ; i++) {
            String key = "key-" + i;
            state.put(key, new EntryInfo());
        }

        // Create caches, listeners and finally worker threads
        Worker worker1 = createWorker(1);
        Worker worker2 = createWorker(2);

        long start = System.currentTimeMillis();

        // Start and join workers
        worker1.start();
        worker2.start();

        worker1.join();
        worker2.join();

        long took = System.currentTimeMillis() - start;

        Map<String, EntryInfo> failedState = new HashMap<>();

        // Output
        for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
            System.out.println(entry.getKey() + ":::" + entry.getValue());

            if (entry.getValue().th1.get() != entry.getValue().th2.get()) {
                failedState.put(entry.getKey(), entry.getValue());
            }

            worker1.cache.remove(entry.getKey());
        }

        System.out.println("\nFAILED ENTRIES. SIZE: " + failedState.size() + "\n");
        for (Map.Entry<String, EntryInfo> entry : failedState.entrySet()) {
            System.out.println(entry.getKey() + ":::" + entry.getValue());
        }

        System.out.println("Took: " + took + " ms");

        // Finish JVM
        worker1.cache.getCacheManager().stop();
        worker2.cache.getCacheManager().stop();
    }

    private static Worker createWorker(int threadId) {
        EmbeddedCacheManager manager = new TestCacheManagerFactory().createManager(threadId, InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, RemoteStoreConfigurationBuilder.class);
        Cache<String, Integer> cache = manager.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

        System.out.println("Retrieved cache: " + threadId);

        RemoteStore remoteStore = cache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).getStores(RemoteStore.class).iterator().next();
        HotRodListener listener = new HotRodListener();
        remoteStore.getRemoteCache().addClientListener(listener);

        return new Worker(cache, threadId);
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

                int val = getClusterStartupTime(this.cache, cacheKey, wrapper, myThreadId);
                if (myThreadId == 1) {
                    wrapper.th1.set(val);
                } else {
                    wrapper.th2.set(val);
                }

            }

            System.out.println("Worker finished: " + myThreadId);
        }

    }

    public static int getClusterStartupTime(Cache<String, Integer> cache, String cacheKey, EntryInfo wrapper, int myThreadId) {
        Integer startupTime = myThreadId==1 ? Integer.parseInt(cacheKey.substring(4)) : Integer.parseInt(cacheKey.substring(4)) * 2;

        // Concurrency doesn't work correctly with this
        //Integer existingClusterStartTime = (Integer) cache.putIfAbsent(cacheKey, startupTime);

        // Concurrency works fine with this
        RemoteCache remoteCache = cache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).getStores(RemoteStore.class).iterator().next().getRemoteCache();

        Integer existingClusterStartTime = null;
        for (int i=0 ; i<10 ; i++) {
            try {
                existingClusterStartTime = (Integer) remoteCache.withFlags(Flag.FORCE_RETURN_VALUE).putIfAbsent(cacheKey, startupTime);
                break;
            } catch (HotRodClientException ce) {
                if (i == 9) {
                    throw ce;
                    //break;
                } else {
                    wrapper.exceptions.incrementAndGet();
                    System.err.println("Exception: i=" + i + " for key: " + cacheKey + " and myThreadId: " + myThreadId);
                }
            }
        }

        if (existingClusterStartTime == null
//                || startupTime.equals(remoteCache.get(cacheKey))
                ) {
            wrapper.successfulInitializations.incrementAndGet();
            return startupTime;
        } else {
            wrapper.failedInitializations.incrementAndGet();
            return existingClusterStartTime;
        }
    }

    public static class EntryInfo {
        AtomicInteger successfulInitializations = new AtomicInteger(0);
        AtomicInteger successfulListenerWrites = new AtomicInteger(0);
        AtomicInteger th1 = new AtomicInteger();
        AtomicInteger th2 = new AtomicInteger();
        AtomicInteger failedInitializations = new AtomicInteger();
        AtomicInteger exceptions = new AtomicInteger();

        @Override
        public String toString() {
            return String.format("Inits: %d, listeners: %d, failedInits: %d, exceptions: %s, th1: %d, th2: %d", successfulInitializations.get(), successfulListenerWrites.get(),
            failedInitializations.get(), exceptions.get(), th1.get(), th2.get());
        }
    }



}
