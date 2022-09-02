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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryRemoved;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryRemovedEvent;
import org.infinispan.client.hotrod.exceptions.HotRodClientException;
import org.infinispan.context.Flag;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.connections.infinispan.InfinispanUtil;
import java.util.UUID;

/**
 * Check that removing of session from remoteCache is session immediately removed on remoteCache in other DC. This is true.
 *
 * Also check that listeners are executed asynchronously with some delay.
 *
 * Steps: {@see ConcurrencyJDGRemoteCacheClientListenersTest}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrencyJDGRemoveSessionTest {

    protected static final Logger logger = Logger.getLogger(ConcurrencyJDGRemoveSessionTest.class);

    private static final int ITERATIONS = 10000;

    private static RemoteCache remoteCache1;
    private static RemoteCache remoteCache2;

    private static final AtomicInteger errorsCounter = new AtomicInteger(0);

    private static final AtomicInteger successfulListenerWrites = new AtomicInteger(0);
    private static final AtomicInteger successfulListenerWrites2 = new AtomicInteger(0);

    private static Map<String, AtomicInteger> removalCounts = new ConcurrentHashMap<>();


    private static final UUID CLIENT_1_UUID = UUID.randomUUID();

    public static void main(String[] args) throws Exception {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache1 = createManager(1).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache2 = createManager(2).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

        // Create caches, listeners and finally worker threads
        Thread worker1 = createWorker(cache1, 1);
        Thread worker2 = createWorker(cache2, 2);
        Thread worker3 = createWorker(cache1, 1);
        Thread worker4 = createWorker(cache2, 2);

        // Create 100 initial sessions
        for (int i=0 ; i<ITERATIONS ; i++) {
            String sessionId = String.valueOf(i);
            SessionEntityWrapper<UserSessionEntity> wrappedSession = createSessionEntity(sessionId);
            cache1.put(sessionId, wrappedSession);

            removalCounts.put(sessionId, new AtomicInteger(0));
        }

        logger.info("SESSIONS CREATED");

        // Create 100 initial sessions
        for (int i=0 ; i<ITERATIONS ; i++) {
            String sessionId = String.valueOf(i);
            SessionEntityWrapper loadedWrapper = cache2.get(sessionId);
            Assert.assertNotNull("Loaded wrapper for key " + sessionId, loadedWrapper);
        }

        logger.info("SESSIONS AVAILABLE ON DC2");


        long start = System.currentTimeMillis();

        try {
            worker1.start();
            worker2.start();
            worker3.start();
            worker4.start();

            worker1.join();
            worker2.join();
            worker3.join();
            worker4.join();

            logger.info("SESSIONS REMOVED");

            Map<Integer, Integer> histogram = new HashMap<>();
            for (Map.Entry<String, AtomicInteger> entry : removalCounts.entrySet()) {
                int count = entry.getValue().get();

                int current = histogram.get(count) == null ? 0 : histogram.get(count);
                current++;
                histogram.put(count, current);
            }

            logger.infof("Histogram: %s", histogram.toString());
            logger.infof("Errors: %d", errorsCounter.get());

            //Thread.sleep(5000);

            // Doing it in opposite direction to ensure that newer are checked first.
            // This us currently FAILING (expected) as listeners are executed asynchronously.
//            for (int i=ITERATIONS-1 ; i>=0 ; i--) {
//                String sessionId = String.valueOf(i);
//
//                logger.infof("Before call cache2.get: %s", sessionId);
//
//                SessionEntityWrapper loadedWrapper = cache2.get(sessionId);
//                Assert.assertNull("Loaded wrapper not null for key " + sessionId, loadedWrapper);
//            }
//
//            logger.info("SESSIONS NOT AVAILABLE ON DC2");

            long took = System.currentTimeMillis() - start;
            logger.infof("took %d ms", took);

            //        // Start and join workers
//        worker1.start();
//        worker2.start();
//
//        worker1.join();
//        worker2.join();

        } finally {
            Thread.sleep(2000);

            // Finish JVM
            cache1.getCacheManager().stop();
            cache2.getCacheManager().stop();
        }

//        // Output
//        for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
//            System.out.println(entry.getKey() + ":::" + entry.getValue());
//            worker1.cache.remove(entry.getKey());
//        }

//        System.out.println("Finished. Took: " + took + " ms. Notes: " + cache1.get("123").getEntity().getNotes().size() +
//                ", successfulListenerWrites: " + successfulListenerWrites.get() + ", successfulListenerWrites2: " + successfulListenerWrites2.get() +
//                ", failedReplaceCounter: " + failedReplaceCounter.get() + ", failedReplaceCounter2: " + failedReplaceCounter2.get() );
//
//        System.out.println("Sleeping before other report");
//
//        Thread.sleep(1000);
//
//        System.out.println("Finished. Took: " + took + " ms. Notes: " + cache1.get("123").getEntity().getNotes().size() +
//                ", successfulListenerWrites: " + successfulListenerWrites.get() + ", successfulListenerWrites2: " + successfulListenerWrites2.get() +
//                ", failedReplaceCounter: " + failedReplaceCounter.get() + ", failedReplaceCounter2: " + failedReplaceCounter2.get());


    }


    private static SessionEntityWrapper<UserSessionEntity> createSessionEntity(String sessionId) {
        // Create 100 initial sessions
        UserSessionEntity session = new UserSessionEntity();
        session.setId(sessionId);
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

        SessionEntityWrapper<UserSessionEntity> wrappedSession = new SessionEntityWrapper<>(session);
        return wrappedSession;
    }


    private static Thread createWorker(Cache<String, SessionEntityWrapper<UserSessionEntity>> cache, int threadId) {
        System.out.println("Retrieved cache: " + threadId);

        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache);

        if (threadId == 1) {
            remoteCache1 = remoteCache;
        } else {
            remoteCache2 = remoteCache;
        }

        AtomicInteger counter = threadId ==1 ? successfulListenerWrites : successfulListenerWrites2;
        HotRodListener listener = new HotRodListener(cache, remoteCache, counter);
        remoteCache.addClientListener(listener);

        return new RemoteCacheWorker(remoteCache, threadId);
        //return new CacheWorker(cache, threadId);
    }


    private static EmbeddedCacheManager createManager(int threadId) {
        return new TestCacheManagerFactory().createManager(threadId, InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, RemoteStoreConfigurationBuilder.class);
    }


    @ClientListener
    public static class HotRodListener {

        private Cache<String, SessionEntityWrapper<UserSessionEntity>> origCache;
        private RemoteCache remoteCache;
        private AtomicInteger listenerCount;

        public HotRodListener(Cache<String, SessionEntityWrapper<UserSessionEntity>> origCache, RemoteCache remoteCache, AtomicInteger listenerCount) {
            this.listenerCount = listenerCount;
            this.remoteCache = remoteCache;
            this.origCache = origCache;
        }


        @ClientCacheEntryCreated
        public void created(ClientCacheEntryCreatedEvent event) {
            String cacheKey = (String) event.getKey();

            logger.infof("Listener executed for creating of session %s", cacheKey);
        }


        @ClientCacheEntryModified
        public void modified(ClientCacheEntryModifiedEvent event) {
            String cacheKey = (String) event.getKey();

            logger.infof("Listener executed for modifying of session %s", cacheKey);
        }


        @ClientCacheEntryRemoved
        public void removed(ClientCacheEntryRemovedEvent event) {
            String cacheKey = (String) event.getKey();

            logger.infof("Listener executed for removing of session %s", cacheKey);

            // TODO: for distributed caches, ensure that it is executed just on owner OR if event.isCommandRetried
            origCache
                    .getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE)
                    .remove(cacheKey);

        }

    }

    private static class RemoteCacheWorker extends Thread {

        private final RemoteCache<String, Object> remoteCache;

        private final int myThreadId;

        private RemoteCacheWorker(RemoteCache remoteCache, int myThreadId) {
            this.remoteCache = remoteCache;
            this.myThreadId = myThreadId;
        }

        @Override
        public void run() {

            for (int i=0 ; i<ITERATIONS ; i++) {
                String sessionId = String.valueOf(i);

                try {
                    Object o = remoteCache
                            .withFlags(org.infinispan.client.hotrod.Flag.FORCE_RETURN_VALUE)
                            .remove(sessionId);

                    if (o != null) {
                        removalCounts.get(sessionId).incrementAndGet();
                    }
                } catch (HotRodClientException hrce) {
                    errorsCounter.incrementAndGet();
                }
//
//
//                logger.infof("Session %s removed on DC1", sessionId);
//
//                // Check if it's immediately seen that session is removed on 2nd DC
//                RemoteCache secondDCRemoteCache = myThreadId == 1 ? remoteCache2 : remoteCache1;
//                SessionEntityWrapper thatSession = (SessionEntityWrapper) secondDCRemoteCache.get(sessionId);
//                Assert.assertNull("Session with ID " + sessionId + " not removed on the other DC. ThreadID: " + myThreadId, thatSession);
//
//                // Also check that it's immediatelly removed on my DC
//                SessionEntityWrapper mySession = (SessionEntityWrapper) remoteCache.get(sessionId);
//                Assert.assertNull("Session with ID " + sessionId + " not removed on the other DC. ThreadID: " + myThreadId, mySession);
            }

        }

    }

}
