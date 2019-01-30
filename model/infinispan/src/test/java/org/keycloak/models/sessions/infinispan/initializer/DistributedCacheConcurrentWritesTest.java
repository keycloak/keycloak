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

package org.keycloak.models.sessions.infinispan.initializer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.jgroups.JChannel;
import org.junit.Ignore;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import java.util.UUID;

/**
 * Test concurrent writes to distributed cache with usage of atomic replace
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Ignore
public class DistributedCacheConcurrentWritesTest {

    private static final int ITERATION_PER_WORKER = 1000;

    private static final AtomicInteger failedReplaceCounter = new AtomicInteger(0);
    private static final AtomicInteger failedReplaceCounter2 = new AtomicInteger(0);

    private static final UUID CLIENT_1_UUID = UUID.randomUUID();

    public static void main(String[] args) throws Exception {
        CacheWrapper<String, UserSessionEntity> cache1 = createCache("node1");
        CacheWrapper<String, UserSessionEntity> cache2 = createCache("node2");

        // Create initial item
        UserSessionEntity session = new UserSessionEntity();
        session.setId("123");
        session.setRealmId("foo");
        session.setBrokerSessionId("!23123123");
        session.setBrokerUserId(null);
        session.setUser("foo");
        session.setLoginUsername("foo");
        session.setIpAddress("123.44.143.178");
        session.setStarted(Time.currentTime());
        session.setLastSessionRefresh(Time.currentTime());

        AuthenticatedClientSessionEntity clientSession = new AuthenticatedClientSessionEntity(UUID.randomUUID());
        clientSession.setAuthMethod("saml");
        clientSession.setAction("something");
        clientSession.setTimestamp(1234);
        session.getAuthenticatedClientSessions().put(CLIENT_1_UUID.toString(), clientSession.getId());

        try {
            for (int i = 0; i < 10; i++) {
                testConcurrentReplaceWithRemove("key-" + i, session, cache1, cache2);
            }
        } finally {

            // Kill JVM
            cache1.getCache().stop();
            cache2.getCache().stop();
            cache1.getCache().getCacheManager().stop();
            cache2.getCache().getCacheManager().stop();

            System.out.println("Managers killed");
        }
    }


    // Reproducer for KEYCLOAK-7443 and KEYCLOAK-7489. The infinite loop can happen if cache.replace(key, old, new) is called and entity was removed on one cluster node in the meantime
    private static void testConcurrentReplaceWithRemove(String key, UserSessionEntity session, CacheWrapper<String, UserSessionEntity> cache1,
                                                 CacheWrapper<String, UserSessionEntity> cache2) throws InterruptedException {
        cache1.put(key, session);

        // Create 2 workers for concurrent write and start them
        Worker worker1 = new Worker(1, cache1, key);
        Worker worker2 = new Worker(2, cache2, key);

        long start = System.currentTimeMillis();

        System.out.println("Started clustering test for key " + key);

        worker1.start();
        //worker1.join();
        worker2.start();

        Thread.sleep(1000);
        // Try to remove the entity after some sleep time.
        cache1.wrappedCache.getAdvancedCache()
                .withFlags(Flag.CACHE_MODE_LOCAL)
                .remove(key);

        worker1.join();
        worker2.join();

        long took = System.currentTimeMillis() - start;

        System.out.println("Test finished for key '" + key + "'. Took: " + took + " ms");

//        System.out.println("Took: " + took + " ms for key . Notes count: " + session.getNotes().size() + ", failedReplaceCounter: " + failedReplaceCounter.get()
//                + ", failedReplaceCounter2: " + failedReplaceCounter2.get());

//        // JGroups statistics
//        JChannel channel = (JChannel)((JGroupsTransport)cache1.wrappedCache.getAdvancedCache().getRpcManager().getTransport()).getChannel();
//        System.out.println("Sent MB: " + channel.getSentBytes() / 1000000 + ", sent messages: " + channel.getSentMessages() + ", received MB: " + channel.getReceivedBytes() / 1000000 +
//                ", received messages: " + channel.getReceivedMessages());
    }


    private static class Worker extends Thread {

        private final CacheWrapper<String, UserSessionEntity> cache;
        private final int threadId;
        private final String key;

        public Worker(int threadId, CacheWrapper<String, UserSessionEntity> cache, String key) {
            this.threadId = threadId;
            this.cache = cache;
            this.key = key;
            setName("th-" + key + "-" + threadId);
        }

        @Override
        public void run() {

            for (int i=0 ; i<ITERATION_PER_WORKER ; i++) {

                String noteKey = "n-" + threadId + "-" + i;

                  // This code can be used to reproduce infinite loop ( KEYCLOAK-7443 )
//                boolean replaced = false;
//                while (!replaced) {
//                    SessionEntityWrapper<UserSessionEntity> oldWrapped = cache.get(key);
//                    oldWrapped.getEntity().getNotes().put(noteKey, "someVal");
//                    replaced = cacheReplace(oldWrapped, oldWrapped.getEntity());
//                }

                int count = 0;
                boolean replaced = false;
                while (!replaced && count < 25) {
                    count++;
                    SessionEntityWrapper<UserSessionEntity> oldWrapped = cache.get(key);
                    oldWrapped.getEntity().getNotes().put(noteKey, "someVal");
                    replaced = cacheReplace(oldWrapped, oldWrapped.getEntity());
                }
                if (!replaced) {
                    System.err.println("FAILED TO REPLACE ENTITY: " + key);
                    return;
                }
            }

        }

        private boolean cacheReplace(SessionEntityWrapper<UserSessionEntity> oldSession, UserSessionEntity newSession) {
            try {
                boolean replaced = cache.replace(key, oldSession, newSession);
                //cache.replace(key, newSession);
                if (!replaced) {
                    failedReplaceCounter.incrementAndGet();
                    //return false;
                    //System.out.println("Replace failed!!!");
                }
                return replaced;
            } catch (Exception re) {
                failedReplaceCounter2.incrementAndGet();
                return false;
            }
            //return replaced;
        }

    }

    // Session clone

    private static UserSessionEntity cloneSession(UserSessionEntity session) {
        UserSessionEntity clone = new UserSessionEntity();
        clone.setId(session.getId());
        clone.setRealmId(session.getRealmId());
        clone.setNotes(new ConcurrentHashMap<>(session.getNotes()));
        return clone;
    }


    // Cache creation utils

    public static class CacheWrapper<K, V extends SessionEntity> {

        private final Cache<K, SessionEntityWrapper<V>> wrappedCache;

        public CacheWrapper(Cache<K, SessionEntityWrapper<V>> wrappedCache) {
            this.wrappedCache = wrappedCache;
        }


        public SessionEntityWrapper<V> get(K key) {
            SessionEntityWrapper<V> val = wrappedCache.get(key);
            return val;
        }

        public void put(K key, V newVal) {
            SessionEntityWrapper<V> newWrapper = new SessionEntityWrapper<>(newVal);
            wrappedCache.put(key, newWrapper);
        }


        public boolean replace(K key, SessionEntityWrapper<V> oldVal, V newVal) {
            SessionEntityWrapper<V> newWrapper = new SessionEntityWrapper<>(newVal);
            return wrappedCache.replace(key, oldVal, newWrapper);
        }

        private Cache<K, SessionEntityWrapper<V>> getCache() {
            return wrappedCache;
        }

    }


    public static CacheWrapper<String, UserSessionEntity> createCache(String nodeName) {
        EmbeddedCacheManager mgr = createManager(nodeName);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> wrapped = mgr.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        return new CacheWrapper<>(wrapped);
    }


    public static EmbeddedCacheManager createManager(String nodeName) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("jgroups.tcp.port", "53715");
        GlobalConfigurationBuilder gcb = new GlobalConfigurationBuilder();

        boolean clustered = true;
        boolean async = false;
        boolean allowDuplicateJMXDomains = true;

        if (clustered) {
            gcb = gcb.clusteredDefault();
            gcb.transport().clusterName("test-clustering");
            gcb.transport().nodeName(nodeName);
        }
        gcb.globalJmxStatistics().allowDuplicateDomains(allowDuplicateJMXDomains);

        EmbeddedCacheManager cacheManager = new DefaultCacheManager(gcb.build());


        ConfigurationBuilder distConfigBuilder = new ConfigurationBuilder();
        if (clustered) {
            distConfigBuilder.clustering().cacheMode(async ? CacheMode.DIST_ASYNC : CacheMode.DIST_SYNC);
            distConfigBuilder.clustering().hash().numOwners(2);

            // Disable L1 cache
            distConfigBuilder.clustering().hash().l1().enabled(false);
        }
        Configuration distConfig = distConfigBuilder.build();

        cacheManager.defineConfiguration(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, distConfig);
        return cacheManager;

    }
}
