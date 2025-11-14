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

package org.keycloak.keys.infinispan;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.crypto.PublicKeysWrapper;
import org.keycloak.keys.PublicKeyLoader;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanKeyStorageProviderTest {

    private final Map<String, AtomicInteger> counters = new ConcurrentHashMap<>();

    Cache<String, PublicKeysEntry> keys = getKeysCache();
    Map<String, FutureTask<PublicKeysEntry>> tasksInProgress = new ConcurrentHashMap<>();
    int minTimeBetweenRequests = 10;
    int maxCacheTime = 600;

    @Before
    public void before() {
        Time.setOffset(0);
    }

    @After
    public void after() {
        Time.setOffset(0);
    }

    @Test
    public void testConcurrency() throws Exception {
        // Just one thread will execute the task
        List<Thread> threads = new LinkedList<>();
        for (int i=0 ; i<10 ; i++) {
            Thread t = new Thread(new SampleWorker("model1"));
            threads.add(t);
        }
        startAndJoinAll(threads);
        Assert.assertEquals(counters.get("model1").get(), 1);
        threads.clear();

        // model1 won't be executed due to lastRequestTime. model2 will be executed just with one thread
        for (int i=0 ; i<10 ; i++) {
            Thread t = new Thread(new SampleWorker("model1"));
            threads.add(t);
        }
        for (int i=0 ; i<10 ; i++) {
            Thread t = new Thread(new SampleWorker("model2"));
            threads.add(t);
        }
        startAndJoinAll(threads);
        Assert.assertEquals(counters.get("model1").get(), 1);
        Assert.assertEquals(counters.get("model2").get(), 1);
        threads.clear();

        // Increase time offset
        Time.setOffset(20);

        // Time updated. So another thread should successfully run loader for both model1 and model2
        for (int i=0 ; i<10 ; i++) {
            Thread t = new Thread(new SampleWorker("model1"));
            threads.add(t);
        }
        for (int i=0 ; i<10 ; i++) {
            Thread t = new Thread(new SampleWorker("model2"));
            threads.add(t);
        }
        startAndJoinAll(threads);
        Assert.assertEquals(counters.get("model1").get(), 2);
        Assert.assertEquals(counters.get("model2").get(), 2);
        threads.clear();
    }


    private void startAndJoinAll(List<Thread> threads) throws Exception {
        for (Thread t : threads) {
            t.start();
        }
        for (Thread t : threads) {
            t.join();
        }
    }


    private class SampleWorker implements Runnable {


        private final String modelKey;

        private SampleWorker(String modelKey) {
            this.modelKey = modelKey;
        }

        @Override
        public void run() {
            InfinispanPublicKeyStorageProvider provider = new InfinispanPublicKeyStorageProvider(null, keys, tasksInProgress, minTimeBetweenRequests, maxCacheTime);
            provider.getPublicKey(modelKey, "kid1", null, new SampleLoader(modelKey));
        }

    }


    private class SampleLoader implements PublicKeyLoader {

        private final String modelKey;

        private SampleLoader(String modelKey) {
            this.modelKey = modelKey;
        }

        @Override
        public PublicKeysWrapper loadKeys() throws Exception {
            counters.putIfAbsent(modelKey, new AtomicInteger(0));
            AtomicInteger currentCounter = counters.get(modelKey);

            currentCounter.incrementAndGet();
            return PublicKeysWrapper.EMPTY;
        }
    }


    protected Cache<String, PublicKeysEntry> getKeysCache() {
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();
        gcb.jmx().domain(InfinispanConnectionProvider.JMX_DOMAIN).enable();
        DefaultCacheManager cacheManager = new DefaultCacheManager(gcb.build());

        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.memory()
                .whenFull(EvictionStrategy.REMOVE)
                .maxCount(InfinispanConnectionProvider.KEYS_CACHE_DEFAULT_MAX);
        cb.statistics().enabled(true);
        Configuration cfg = cb.build();
        cacheManager.defineConfiguration(InfinispanConnectionProvider.KEYS_CACHE_NAME, cfg);

        return cacheManager.getCache(InfinispanConnectionProvider.KEYS_CACHE_NAME);
    }
}
