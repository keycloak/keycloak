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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.VersionedValue;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryCreated;
import org.infinispan.client.hotrod.annotation.ClientCacheEntryModified;
import org.infinispan.client.hotrod.annotation.ClientListener;
import org.infinispan.client.hotrod.event.ClientCacheEntryCreatedEvent;
import org.infinispan.client.hotrod.event.ClientCacheEntryModifiedEvent;
import org.infinispan.context.Flag;
import org.infinispan.manager.EmbeddedCacheManager;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.connections.infinispan.InfinispanUtil;
import java.util.UUID;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;

/**
 * Test concurrency for remoteStore (backed by HotRod RemoteCaches) against external JDG. Especially tests "replaceWithVersion" contract.
 *
 * Steps: {@see ConcurrencyJDGRemoteCacheClientListenersTest}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ConcurrencyJDGCacheReplaceTest {

    protected static final Logger logger = Logger.getLogger(ConcurrencyJDGCacheReplaceTest.class);

    private static final int ITERATION_PER_WORKER = 1000;

    private static RemoteCache remoteCache1;
    private static RemoteCache remoteCache2;

    private static List<ExecutorService> executors = new ArrayList<>();

    private static final AtomicInteger failedReplaceCounter = new AtomicInteger(0);
    private static final AtomicInteger failedReplaceCounter2 = new AtomicInteger(0);

    private static final AtomicInteger successfulListenerWrites = new AtomicInteger(0);
    private static final AtomicInteger successfulListenerWrites2 = new AtomicInteger(0);

    private static final ConcurrencyTestHistogram histogram = new ConcurrencyTestHistogram();

    //private static Map<String, EntryInfo> state = new HashMap<>();

    private static final UUID CLIENT_1_UUID = UUID.randomUUID();

    
    public static void main(String[] args) throws Exception {
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache1 = createManager(1).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
        Cache<String, SessionEntityWrapper<UserSessionEntity>> cache2 = createManager(2).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);

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

        SessionEntityWrapper<UserSessionEntity> wrappedSession = new SessionEntityWrapper<>(session);

        // Some dummy testing of remoteStore behaviour
        logger.info("Before put");

        cache1
                .getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL) // will still invoke remoteStore . Just doesn't propagate to cluster
                .put("123", wrappedSession);

        logger.info("After put");

        cache1.replace("123",  wrappedSession);

        logger.info("After replace");

        cache1.get("123");

        logger.info("After cache1.get");

        cache2.get("123");

        logger.info("After cache2.get");

        cache1.get("123");

        logger.info("After cache1.get - second call");

        cache2.get("123");

        logger.info("After cache2.get - second call");

        cache2.replace("123",  wrappedSession);

        logger.info("After replace - second call");

        cache1.get("123");

        logger.info("After cache1.get - third call");

        cache2.get("123");

        logger.info("After cache2.get - third call");

        cache1
                .getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD)
                .entrySet().stream().forEach(e -> {
        });

        logger.info("After cache1.stream");

        // Explicitly call put on remoteCache (KcRemoteCache.write ignores remote writes)
        InfinispanUtil.getRemoteCache(cache1).put("123", session);
        InfinispanUtil.getRemoteCache(cache2).replace("123", session);

        // Create caches, listeners and finally worker threads
        remoteCache1 = InfinispanUtil.getRemoteCache(cache1);
        remoteCache2 = InfinispanUtil.getRemoteCache(cache2);

        // Manual test of lifespans
        testLifespans();

        Thread worker1 = createWorker(cache1, 1);
        Thread worker2 = createWorker(cache2, 2);

        long start = System.currentTimeMillis();

        // Start and join workers
        worker1.start();
        worker2.start();

        worker1.join();
        worker2.join();

        long took = System.currentTimeMillis() - start;

//        // Output
//        for (Map.Entry<String, EntryInfo> entry : state.entrySet()) {
//            System.out.println(entry.getKey() + ":::" + entry.getValue());
//            worker1.cache.remove(entry.getKey());
//        }

        System.out.println("Finished. Took: " + took + " ms. Notes: " + cache1.get("123").getEntity().getNotes().size() +
                ", successfulListenerWrites: " + successfulListenerWrites.get() + ", successfulListenerWrites2: " + successfulListenerWrites2.get() +
                ", failedReplaceCounter: " + failedReplaceCounter.get() + ", failedReplaceCounter2: " + failedReplaceCounter2.get() );

        System.out.println("Sleeping before other report");

        Thread.sleep(2000);

        System.out.println("Finished. Took: " + took + " ms. Notes: " + cache1.get("123").getEntity().getNotes().size() +
                ", successfulListenerWrites: " + successfulListenerWrites.get() + ", successfulListenerWrites2: " + successfulListenerWrites2.get() +
                ", failedReplaceCounter: " + failedReplaceCounter.get() + ", failedReplaceCounter2: " + failedReplaceCounter2.get());

        System.out.println("remoteCache1.notes: " + ((UserSessionEntity) remoteCache1.get("123")).getNotes().size() );
        System.out.println("remoteCache2.notes: " + ((UserSessionEntity) remoteCache2.get("123")).getNotes().size() );

        System.out.println("Histogram: ");
        //histogram.dumpStats();

        // shutdown pools
        for (ExecutorService ex : executors) {
            ex.shutdown();
        }

        // Finish JVM
        cache1.getCacheManager().stop();
        cache2.getCacheManager().stop();
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

        private ExecutorService executor;

        public HotRodListener(Cache<String, SessionEntityWrapper<UserSessionEntity>> origCache, RemoteCache remoteCache, AtomicInteger listenerCount) {
            this.listenerCount = listenerCount;
            this.remoteCache = remoteCache;
            this.origCache = origCache;
            executor = Executors.newCachedThreadPool();
            executors.add(executor);

        }

        @ClientCacheEntryCreated
        public void created(ClientCacheEntryCreatedEvent event) {
            String cacheKey = (String) event.getKey();
            listenerCount.incrementAndGet();
        }

        @ClientCacheEntryModified
        public void updated(ClientCacheEntryModifiedEvent event) {
            String cacheKey = (String) event.getKey();
            listenerCount.incrementAndGet();

            executor.submit(() -> {
                // TODO: can be optimized - object sent in the event
                VersionedValue<SessionEntity> versionedVal = remoteCache.getWithMetadata(cacheKey);
                for (int i = 0; i < 10; i++) {

                    if (versionedVal.getVersion() < event.getVersion()) {
                        System.err.println("INCOMPATIBLE VERSION. event version: " + event.getVersion() + ", entity version: " + versionedVal.getVersion() + ", i=" + i);
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            throw new RuntimeException(ie);
                        }

                        versionedVal = remoteCache.getWithMetadata(cacheKey);
                    } else {
                        break;
                    }
                }

                SessionEntity session = (SessionEntity) versionedVal.getValue();
                SessionEntityWrapper sessionWrapper = new SessionEntityWrapper(session);

                if (listenerCount.get() % 100 == 0) {
                    logger.infof("Listener count: " + listenerCount.get());
                }

                // TODO: for distributed caches, ensure that it is executed just on owner OR if event.isCommandRetried
                origCache
                        .getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE)
                        .replace(cacheKey, sessionWrapper);
            });
        }




    }

    private static class RemoteCacheWorker extends Thread {

        private final RemoteCache<String, UserSessionEntity> remoteCache;

        private final int myThreadId;

        private RemoteCacheWorker(RemoteCache remoteCache, int myThreadId) {
            this.remoteCache = remoteCache;
            this.myThreadId = myThreadId;
        }

        @Override
        public void run() {

            for (int i=0 ; i<ITERATION_PER_WORKER ; i++) {

                // Histogram will contain value 1 in all places as it's always different note and hence session is changed to different value
                String noteKey = "n-" + myThreadId + "-" + i;

                // In case it's hardcoded (eg. all the replaces are doing same change, so session is defacto not changed), then histogram may contain bigger value than 1 on some places.
                //String noteKey = "some";

                ReplaceStatus replaced = ReplaceStatus.NOT_REPLACED;
                while (replaced != ReplaceStatus.REPLACED) {
                    VersionedValue<UserSessionEntity> versioned = remoteCache.getWithMetadata("123");
                    UserSessionEntity oldSession = versioned.getValue();
                    //UserSessionEntity clone = DistributedCacheConcurrentWritesTest.cloneSession(oldSession);
                    UserSessionEntity clone = oldSession;

                    // In case that exception was thrown (ReplaceStatus.ERROR), the remoteCache may have the note. Seems that transactions are not fully rolled-back on the JDG side
                    // in case that backup fails
                    if (replaced == ReplaceStatus.NOT_REPLACED) {
                        clone.getNotes().put(noteKey, "someVal");
                    } else if (replaced == ReplaceStatus.ERROR) {
                        if (clone.getNotes().containsKey(noteKey)) {
                            System.err.println("I HAVE THE KEY: " + noteKey);
                        } else {
                            System.err.println("I DON'T HAVE THE KEY: " + noteKey);
                            clone.getNotes().put(noteKey, "someVal");
                        }
                    }

                    //cache.replace("123", clone);
                    replaced = cacheReplace(versioned, clone);
                }

                // Try to see if remoteCache on 2nd DC is immediatelly seeing our change
                RemoteCache secondDCRemoteCache = myThreadId == 1 ? remoteCache2 : remoteCache1;
                //UserSessionEntity thatSession = (UserSessionEntity) secondDCRemoteCache.get("123");

                //Assert.assertEquals("someVal", thatSession.getNotes().get(noteKey));
                //System.out.println("Passed");
            }

        }

        private ReplaceStatus cacheReplace(VersionedValue<UserSessionEntity> oldSession, UserSessionEntity newSession) {
            try {
                boolean replaced = remoteCache.replaceWithVersion("123", newSession, oldSession.getVersion());
                //boolean replaced = true;
                //remoteCache.replace("123", newSession);
                if (!replaced) {
                    failedReplaceCounter.incrementAndGet();
                    //return false;
                    //System.out.println("Replace failed!!!");
                } else {
                    histogram.increaseSuccessOpsCount(oldSession.getVersion());
                }
                return replaced ? ReplaceStatus.REPLACED : ReplaceStatus.NOT_REPLACED;
            } catch (Exception re) {
                failedReplaceCounter2.incrementAndGet();
                return ReplaceStatus.ERROR;
            }
            //return replaced;
        }

    }

    private enum ReplaceStatus {
        REPLACED, NOT_REPLACED, ERROR
    }


    private static void testLifespans() throws Exception {
        long l1 = InfinispanUtil.toHotrodTimeMs(remoteCache1, 5000);
        long l2 = InfinispanUtil.toHotrodTimeMs(remoteCache2, 2592000000L);
        long l3 = InfinispanUtil.toHotrodTimeMs(remoteCache2, 2592000001L);
        //long l4 = InfinispanUtil.getLifespanMs(remoteCache1, Time.currentTimeMillis() + 5000);

        remoteCache1.put("k1", "v1", l1, TimeUnit.MILLISECONDS);
        remoteCache1.put("k2", "v2", l2, TimeUnit.MILLISECONDS);
        remoteCache1.put("k3", "v3", l3, TimeUnit.MILLISECONDS);
        remoteCache1.put("k4", "v4", Time.currentTimeMillis() + 5000, TimeUnit.MILLISECONDS);

        System.out.println("l1=" + l1 + ", l2=" + l2 + ", l3=" + l3);
        System.out.println("k1=" + remoteCache1.get("k1") + ", k2=" + remoteCache1.get("k2") + ", k3=" + remoteCache1.get("k3") + ", k4=" + remoteCache1.get("k4"));

        Thread.sleep(4000);

        System.out.println("k1=" + remoteCache1.get("k1") + ", k2=" + remoteCache1.get("k2") + ", k3=" + remoteCache1.get("k3") + ", k4=" + remoteCache1.get("k4"));

        Thread.sleep(2000);

        System.out.println("k1=" + remoteCache1.get("k1") + ", k2=" + remoteCache1.get("k2") + ", k3=" + remoteCache1.get("k3") + ", k4=" + remoteCache1.get("k4"));
    }
/*
    // Worker, which operates on "classic" cache and rely on operations delegated to the second cache
    private static class CacheWorker extends Thread {

        private final Cache<String, SessionEntityWrapper<UserSessionEntity>> cache;

        private final int myThreadId;

        private CacheWorker(Cache<String, SessionEntityWrapper<UserSessionEntity>> cache, int myThreadId) {
            this.cache = cache;
            this.myThreadId = myThreadId;
        }

        @Override
        public void run() {

            for (int i=0 ; i<ITERATION_PER_WORKER ; i++) {

                String noteKey = "n-" + myThreadId + "-" + i;

                boolean replaced = false;
                while (!replaced) {
                    VersionedValue<UserSessionEntity> versioned = cache.getVersioned("123");
                    UserSessionEntity oldSession = versioned.getValue();
                    //UserSessionEntity clone = DistributedCacheConcurrentWritesTest.cloneSession(oldSession);
                    UserSessionEntity clone = oldSession;

                    clone.getNotes().put(noteKey, "someVal");
                    //cache.replace("123", clone);
                    replaced = cacheReplace(versioned, clone);
                }
            }

        }

    }*/


}
