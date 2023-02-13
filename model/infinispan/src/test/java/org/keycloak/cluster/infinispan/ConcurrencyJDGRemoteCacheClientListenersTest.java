/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.remote.RemoteStore;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.junit.Assert;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.connections.infinispan.InfinispanUtil;

/**
 * Test that hotrod ClientListeners are correctly executed as expected
 *
 * STEPS TO REPRODUCE:
 * - Unzip infinispan-server-9.2.4.Final to some locations ISPN1 and ISPN2
 *
 * - Edit both ISPN1/standalone/configuration/clustered.xml and ISPN2/standalone/configuration/clustered.xml . Configure cache in container "clustered"
 *
 * 		<replicated-cache-configuration name="sessions-cfg" mode="ASYNC" start="EAGER" batching="false">
            <transaction mode="NON_XA" locking="PESSIMISTIC"/>
        </replicated-cache-configuration>

        <replicated-cache name="work" configuration="sessions-cfg" />

    - Run server1
 ./standalone.sh -c clustered.xml -Djava.net.preferIPv4Stack=true -Djboss.socket.binding.port-offset=1010 -Djboss.default.multicast.address=234.56.78.99 -Djboss.node.name=cache-server

    - Run server2
 ./standalone.sh -c clustered.xml -Djava.net.preferIPv4Stack=true -Djboss.socket.binding.port-offset=2010 -Djboss.default.multicast.address=234.56.78.100 -Djboss.node.name=cache-server-dc-2

    - Run this test as main class from IDE
 *
 *
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrencyJDGRemoteCacheClientListenersTest {

    // Helper map to track if listeners were executed
    private static Map<String, EntryInfo> state = new HashMap<>();

    private static AtomicInteger totalListenerCalls = new AtomicInteger(0);

    private static AtomicInteger totalErrors = new AtomicInteger(0);


    public static void main(String[] args) throws Exception {
        // Init map somehow
        for (int i=0 ; i<1000 ; i++) {
            String key = "key-" + i;
            EntryInfo entryInfo = new EntryInfo();
            entryInfo.val.set(i);
            state.put(key, entryInfo);
        }

        // Create caches, listeners and finally worker threads
        Worker worker1 = createWorker(1);
        Worker worker2 = createWorker(2);

        // Note "run", so it's not executed asynchronously here!!!
        worker1.run();

//
//        // Start and join workers
//        worker1.start();
//        worker2.start();
//
//        worker1.join();
//        worker2.join();

        // Output
        for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
            System.out.println(entry.getKey() + ":::" + entry.getValue());
        }

        System.out.println("totalListeners: " + totalListenerCalls.get() + ", totalErrors: " + totalErrors.get());


        // Assert that ClientListener was able to read the value and save it into EntryInfo
        try {
            for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
                EntryInfo info = entry.getValue();
                Assert.assertEquals(info.val.get(), info.dc1Created.get());
                Assert.assertEquals(info.val.get(), info.dc2Created.get());
                Assert.assertEquals(info.val.get() * 2, info.dc1Updated.get());
                Assert.assertEquals(info.val.get() * 2, info.dc2Updated.get());
            }
        } finally {
            // Remove items
            for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
                worker1.cache.remove(entry.getKey());
            }

            // Finish JVM
            worker1.cache.getCacheManager().stop();
            worker2.cache.getCacheManager().stop();
        }
    }

    private static Worker createWorker(int threadId) {
        EmbeddedCacheManager manager = new TestCacheManagerFactory().createManager(threadId, InfinispanConnectionProvider.WORK_CACHE_NAME, RemoteStoreConfigurationBuilder.class);
        Cache<String, Integer> cache = manager.getCache(InfinispanConnectionProvider.WORK_CACHE_NAME);

        System.out.println("Retrieved cache: " + threadId);

        RemoteStore remoteStore = cache.getAdvancedCache().getComponentRegistry().getComponent(PersistenceManager.class).getStores(RemoteStore.class).iterator().next();
        HotRodListener listener = new HotRodListener(cache, threadId);
        remoteStore.getRemoteCache().addClientListener(listener);

        return new Worker(cache, threadId);
    }


    @ClientListener
    public static class HotRodListener {

        private final RemoteCache<String, Integer> remoteCache;
        private final int threadId;
        private Executor executor;

        public HotRodListener(Cache<String, Integer> cache, int threadId) {
            this.remoteCache = InfinispanUtil.getRemoteCache(cache);
            this.threadId = threadId;
            this.executor = Executors.newCachedThreadPool();
        }

        //private AtomicInteger listenerCount = new AtomicInteger(0);

        @ClientCacheEntryCreated
        public void created(ClientCacheEntryCreatedEvent event) {
            String cacheKey = (String) event.getKey();

            executor.execute(() -> {

                event(cacheKey, event.getVersion(), true);

            });

        }


        @ClientCacheEntryModified
        public void updated(ClientCacheEntryModifiedEvent event) {
            String cacheKey = (String) event.getKey();
            executor.execute(() -> {

                event(cacheKey, event.getVersion(), false);

            });
        }


        private void event(String cacheKey, long version, boolean created) {
            EntryInfo entryInfo = state.get(cacheKey);
            entryInfo.successfulListenerWrites.incrementAndGet();

            totalListenerCalls.incrementAndGet();

            VersionedValue<Integer> versionedVal = remoteCache.getWithMetadata(cacheKey);

            if (versionedVal.getVersion() < version) {
                System.err.println("INCOMPATIBLE VERSION. event version: " + version + ", entity version: " + versionedVal.getVersion());
                totalErrors.incrementAndGet();
                return;
            }

            Integer val = versionedVal.getValue();
            if (val != null) {
                AtomicInteger dcVal;
                if (created) {
                    dcVal = threadId == 1 ? entryInfo.dc1Created : entryInfo.dc2Created;
                } else {
                    dcVal = threadId == 1 ? entryInfo.dc1Updated : entryInfo.dc2Updated;
                }
                dcVal.set(val);
            } else {
                System.err.println("NOT A VALUE FOR KEY: " + cacheKey);
                totalErrors.incrementAndGet();
            }
        }

    }


    private static void createItems(Cache<String, Integer> cache, int myThreadId) {
        for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
            String cacheKey = entry.getKey();
            Integer value = entry.getValue().val.get();

            cache.put(cacheKey, value);
        }

        System.out.println("Worker creating finished: " + myThreadId);
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
            createItems(cache, myThreadId);

            for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
                String cacheKey = entry.getKey();
                Integer value = entry.getValue().val.get() * 2;

                this.cache.replace(cacheKey, value);
            }

            System.out.println("Worker updating finished: " + myThreadId);
        }

    }


    public static class EntryInfo {
        AtomicInteger val = new AtomicInteger();
        AtomicInteger successfulListenerWrites = new AtomicInteger(0);
        AtomicInteger dc1Created = new AtomicInteger();
        AtomicInteger dc2Created = new AtomicInteger();
        AtomicInteger dc1Updated = new AtomicInteger();
        AtomicInteger dc2Updated = new AtomicInteger();

        @Override
        public String toString() {
            return String.format("val: %d, successfulListenerWrites: %d, dc1Created: %d, dc2Created: %d, dc1Updated: %d, dc2Updated: %d", val.get(), successfulListenerWrites.get(),
                    dc1Created.get(), dc2Created.get(), dc1Updated.get(), dc2Updated.get());
        }
    }
}
