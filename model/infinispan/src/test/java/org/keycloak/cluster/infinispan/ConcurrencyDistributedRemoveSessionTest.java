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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.initializer.DistributedCacheConcurrentWritesTest;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrencyDistributedRemoveSessionTest {


    protected static final Logger logger = Logger.getLogger(ConcurrencyJDGRemoveSessionTest.class);

    private static final int ITERATIONS = 10000;

    private static final AtomicInteger errorsCounter = new AtomicInteger(0);

    private static final AtomicInteger successfulListenerWrites = new AtomicInteger(0);
    private static final AtomicInteger successfulListenerWrites2 = new AtomicInteger(0);

    private static Map<String, AtomicInteger> removalCounts = new ConcurrentHashMap<>();


    private static final UUID CLIENT_1_UUID = UUID.randomUUID();

    public static void main(String[] args) throws Exception {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache1 = DistributedCacheConcurrentWritesTest.createManager("node1").getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache2 = DistributedCacheConcurrentWritesTest.createManager("node2").getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

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

            long took = System.currentTimeMillis() - start;
            logger.infof("took %d ms", took);


        } finally {
            Thread.sleep(2000);

            // Finish JVM
            cache1.getCacheManager().stop();
            cache2.getCacheManager().stop();
        }
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
        return new CacheWorker(cache, threadId);
    }


    private static class CacheWorker extends Thread {

        private final Cache<String, Object> cache;

        private final int myThreadId;

        private CacheWorker(Cache cache, int myThreadId) {
            this.cache = cache;
            this.myThreadId = myThreadId;
        }


        @Override
        public void run() {

            for (int i=0 ; i<ITERATIONS ; i++) {
                String sessionId = String.valueOf(i);

                Object o = cache.remove(sessionId);

                if (o != null) {
                    removalCounts.get(sessionId).incrementAndGet();
                }

            }

        }

    }
}
