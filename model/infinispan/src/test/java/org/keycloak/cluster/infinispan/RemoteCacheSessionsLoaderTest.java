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


import java.util.HashSet;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.remote.configuration.RemoteStoreConfigurationBuilder;
import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.keycloak.common.util.Time;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.sessions.infinispan.initializer.SessionLoader;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionsLoader;
import org.keycloak.models.sessions.infinispan.remotestore.RemoteCacheSessionsLoaderContext;
import org.keycloak.connections.infinispan.InfinispanUtil;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RemoteCacheSessionsLoaderTest {

    protected static final Logger logger = Logger.getLogger(RemoteCacheSessionsLoaderTest.class);

    private static final int COUNT = 10000;

    @Test
    @Ignore
    public void testRemoteCache() throws Exception {
        String cacheName = InfinispanConnectionProvider.USER_SESSION_CACHE_NAME;
        Cache cache1 = createManager(1, cacheName).getCache(cacheName);
        Cache cache2 = cache1.getCacheManager().getCache("local");
        RemoteCache remoteCache = InfinispanUtil.getRemoteCache(cache1);
        cache1.clear();
        cache2.clear();
        remoteCache.clear();

        try {

            for (int i=0 ; i<COUNT ; i++) {
                // Create initial item
                UserSessionEntity session = new UserSessionEntity();
                session.setId("loader-key-" + i);
                session.setRealmId("master");
                session.setBrokerSessionId("!23123123");
                session.setBrokerUserId(null);
                session.setUser("admin");
                session.setLoginUsername("admin");
                session.setIpAddress("123.44.143.178");
                session.setStarted(Time.currentTime());
                session.setLastSessionRefresh(Time.currentTime());

                SessionEntityWrapper<UserSessionEntity> wrappedSession = new SessionEntityWrapper<>(session);

                // Create caches, listeners and finally worker threads
                remoteCache.put("loader-key-" + i, wrappedSession);
                Assert.assertFalse(cache2.containsKey("loader-key-" + i));

                if (i % 1000 == 0) {
                    logger.infof("%d sessions added", i);
                }
            }


//            RemoteCacheSessionsLoader loader = new RemoteCacheSessionsLoader(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME, 64) {
//
//                @Override
//                protected Cache getCache(KeycloakSession session) {
//                    return cache2;
//                }
//
//                @Override
//                protected RemoteCache getRemoteCache(KeycloakSession session) {
//                    return remoteCache;
//                }
//
//            };

            // Just to be able to test serializability
            RemoteCacheSessionsLoader loader = new CustomLoader(cacheName, 64, cache2, remoteCache);

            loader.init(null);
            RemoteCacheSessionsLoaderContext ctx = loader.computeLoaderContext(null);
            Assert.assertEquals(ctx.getSessionsTotal(), COUNT);
            Assert.assertEquals(ctx.getIspnSegmentsCount(), 256);
            //Assert.assertEquals(ctx.getSegmentsCount(), 16);
            Assert.assertEquals(ctx.getSessionsPerSegment(), 64);

            int totalCount = 0;
            logger.infof("segmentsCount: %d", ctx.getSegmentsCount());

            Set<String> visitedKeys = new HashSet<>();
            for (int currentSegment=0 ; currentSegment<ctx.getSegmentsCount() ; currentSegment++) {
                logger.infof("Loading segment %d", currentSegment);
                loader.loadSessions(null, ctx, new SessionLoader.WorkerContext(currentSegment, currentSegment));

                logger.infof("Loaded %d keys for segment %d", cache2.keySet().size(), currentSegment);
                totalCount = totalCount + cache2.keySet().size();
                visitedKeys.addAll(cache2.keySet());
                cache2.clear();
            }

            Assert.assertEquals(totalCount, COUNT);
            Assert.assertEquals(visitedKeys.size(), COUNT);
            logger.infof("SUCCESS: Loaded %d sessions", totalCount);
        } finally {
            // Finish JVM
            cache1.getCacheManager().stop();
        }
    }


    private static EmbeddedCacheManager createManager(int threadId, String cacheName) {
        return new TestCacheManagerFactory().createManager(threadId, cacheName, RemoteStoreConfigurationBuilder.class);
    }


    public static class CustomLoader extends RemoteCacheSessionsLoader {

        private final transient Cache cache2;
        private final transient RemoteCache remoteCache;

        public CustomLoader(String cacheName, int sessionsPerSegment, Cache cache2, RemoteCache remoteCache) {
            super(cacheName, sessionsPerSegment);
            this.cache2 = cache2;
            this.remoteCache = remoteCache;

        }

        @Override
        protected Cache getCache(KeycloakSession session) {
            return cache2;
        }

        @Override
        protected RemoteCache getRemoteCache(KeycloakSession session) {
            return remoteCache;
        }

    }

}
