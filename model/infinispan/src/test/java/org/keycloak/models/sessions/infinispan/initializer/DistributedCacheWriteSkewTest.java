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

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.VersioningScheme;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.jgroups.JGroupsTransport;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.lookup.EmbeddedTransactionManagerLookup;
import org.infinispan.util.concurrent.IsolationLevel;
import org.jgroups.JChannel;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import java.util.UUID;

/**
 * Test concurrent writes to distributed cache with usage of write skew
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
//@Ignore
public class DistributedCacheWriteSkewTest {

    private static final int ITERATION_PER_WORKER = 1000;

    private static final AtomicInteger failedReplaceCounter = new AtomicInteger(0);

    private static final UUID CLIENT_1_UUID = UUID.randomUUID();

    public static void main(String[] args) throws Exception {
        Cache<String, UserSessionEntity> cache1 = createManager("node1").getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        Cache<String, UserSessionEntity> cache2 = createManager("node2").getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

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

        cache1.put("123", session);

        //cache1.replace("123", session);

        // Create 2 workers for concurrent write and start them
        Worker worker1 = new Worker(1, cache1);
        Worker worker2 = new Worker(2, cache2);

        long start = System.currentTimeMillis();

        System.out.println("Started clustering test");

        worker1.start();
        //worker1.join();
        worker2.start();

        worker1.join();
        worker2.join();

        long took = System.currentTimeMillis() - start;
        session = cache1.get("123");
        System.out.println("Took: " + took + " ms. Notes count: " + session.getNotes().size() + ", failedReplaceCounter: " + failedReplaceCounter.get());

        // JGroups statistics
        JChannel channel = (JChannel)((JGroupsTransport)cache1.getAdvancedCache().getRpcManager().getTransport()).getChannel();
        System.out.println("Sent MB: " + channel.getSentBytes() / 1000000 + ", sent messages: " + channel.getSentMessages() + ", received MB: " + channel.getReceivedBytes() / 1000000 +
                ", received messages: " + channel.getReceivedMessages());

        // Kill JVM
        cache1.stop();
        cache2.stop();
        cache1.getCacheManager().stop();
        cache2.getCacheManager().stop();

        System.out.println("Managers killed");
    }


    private static class Worker extends Thread {

        private final Cache<String, UserSessionEntity> cache;
        private final int threadId;

        public Worker(int threadId, Cache<String, UserSessionEntity> cache) {
            this.threadId = threadId;
            this.cache = cache;
        }

        @Override
        public void run() {

            for (int i=0 ; i<ITERATION_PER_WORKER ; i++) {

                String noteKey = "n-" + threadId + "-" + i;

                boolean replaced = false;
                while (!replaced) {
                    try {
                        //cache.startBatch();

                        UserSessionEntity oldSession = cache.get("123");

                        //UserSessionEntity clone = DistributedCacheConcurrentWritesTest.cloneSession(oldSession);
                        UserSessionEntity clone = oldSession;

                        clone.getNotes().put(noteKey, "someVal");

                        cache.replace("123", clone);
                        //cache.getAdvancedCache().withFlags(Flag.FAIL_SILENTLY).endBatch(true);
                        replaced = true;
                    } catch (Exception e) {
                        System.out.println(e);
                        e.printStackTrace();
                        failedReplaceCounter.incrementAndGet();
                    }

                }
            }

        }

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
            distConfigBuilder.clustering().hash().numOwners(1);

            // Disable L1 cache
            distConfigBuilder.clustering().hash().l1().enabled(false);

            //distConfigBuilder.storeAsBinary().enable().storeKeysAsBinary(false).storeValuesAsBinary(true);

            distConfigBuilder.versioning().enabled(true);
            distConfigBuilder.versioning().scheme(VersioningScheme.SIMPLE);

            distConfigBuilder.locking().writeSkewCheck(true);
            distConfigBuilder.locking().isolationLevel(IsolationLevel.REPEATABLE_READ);
            distConfigBuilder.locking().concurrencyLevel(32);
            distConfigBuilder.locking().lockAcquisitionTimeout(1000, TimeUnit.SECONDS);

            distConfigBuilder.versioning().enabled(true);
            distConfigBuilder.versioning().scheme(VersioningScheme.SIMPLE);


           // distConfigBuilder.invocationBatching().enable();
            //distConfigBuilder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
            distConfigBuilder.transaction().transactionManagerLookup(new EmbeddedTransactionManagerLookup());
            distConfigBuilder.transaction().lockingMode(LockingMode.OPTIMISTIC);
        }
        Configuration distConfig = distConfigBuilder.build();

        cacheManager.defineConfiguration(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, distConfig);
        return cacheManager;

    }
}
