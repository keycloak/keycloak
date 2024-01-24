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

package org.keycloak.models.sessions.infinispan.remotestore;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Retry;
import org.keycloak.connections.infinispan.DefaultInfinispanConnectionProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.initializer.BaseCacheInitializer;
import org.keycloak.models.sessions.infinispan.initializer.OfflinePersistentUserSessionLoader;
import org.keycloak.models.sessions.infinispan.initializer.SessionLoader;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class RemoteCacheSessionsLoader implements SessionLoader<RemoteCacheSessionsLoaderContext, SessionLoader.WorkerContext, SessionLoader.WorkerResult>, Serializable {

    private static final Logger log = Logger.getLogger(RemoteCacheSessionsLoader.class);

    private final String cacheName;
    private final int sessionsPerSegment;

    public RemoteCacheSessionsLoader(String cacheName, int sessionsPerSegment) {
        this.cacheName = cacheName;
        this.sessionsPerSegment = sessionsPerSegment;
    }


    @Override
    public void init(KeycloakSession session) {
    }


    @Override
    public RemoteCacheSessionsLoaderContext computeLoaderContext(KeycloakSession session) {
        return new RemoteCacheSessionsLoaderContext(sessionsPerSegment);

    }

    @Override
    public WorkerContext computeWorkerContext(RemoteCacheSessionsLoaderContext loaderCtx, int segment, int workerId, WorkerResult previousResult) {
        return new WorkerContext(segment, workerId);
    }


    @Override
    public WorkerResult createFailedWorkerResult(RemoteCacheSessionsLoaderContext loaderContext, WorkerContext workerContext) {
        return new WorkerResult(false, workerContext.getSegment(), workerContext.getWorkerId());
    }

    @Override
    public WorkerResult loadSessions(KeycloakSession session, RemoteCacheSessionsLoaderContext loaderContext, WorkerContext ctx) {
        Cache<Object, Object> cache = getCache(session);
        Cache<Object, Object> decoratedCache = cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE, Flag.IGNORE_RETURN_VALUES);
        RemoteCache<?, ?> remoteCache = getRemoteCache(session);

        int countLoaded = 0;
        try (CloseableIterator<Map.Entry<Object, Object>> it = remoteCache.retrieveEntries(null, loaderContext.getSessionsPerSegment())) {
            Map<Object, Object> toInsert = new HashMap<>(loaderContext.getSessionsPerSegment());
            int count = 0;
            while (it.hasNext()) {
                Map.Entry<?,?> entry = it.next();
                toInsert.put(entry.getKey(), entry.getValue());
                ++countLoaded;
                if (++count == loaderContext.getSessionsPerSegment()) {
                    insertSessions(decoratedCache, toInsert);
                    toInsert = new HashMap<>(loaderContext.getSessionsPerSegment());
                    count = 0;
                }
            }

            if (!toInsert.isEmpty()) {
                // last batch
                insertSessions(decoratedCache, toInsert);
            }
        } catch (RuntimeException e) {
            log.warnf(e, "Error loading sessions from remote cache '%s' for segment '%d'", remoteCache.getName(), ctx.getSegment());
            throw e;
        }

        log.debugf("Successfully finished loading sessions from cache '%s' . Segment: %d, Count of sessions loaded: %d", cache.getName(), ctx.getSegment(), countLoaded);

        return new WorkerResult(true, ctx.getSegment(), ctx.getWorkerId());
    }

    private void insertSessions(Cache<Object, Object> cache, Map<Object, Object> entries) {
        log.debugf("Adding %d entries to cache '%s'", entries.size(), cacheName);

        // The `putAll` operation might time out when a node becomes unavailable, therefore, retry.
        Retry.executeWithBackoff(
                (int iteration) -> {
                    DefaultInfinispanConnectionProviderFactory.runWithReadLockOnCacheManager(() -> {
                        // With Infinispan 14.0.21/14.0.19, we've seen deadlocks in tests where this future never completed when shutting down the internal Infinispan.
                        // Therefore, prevent the shutdown of the internal Infinispan during this step.

                        cache.putAll(entries);
                    });
                },
                (iteration, throwable) -> log.warnf("Unable to put entries into the cache in iteration %s", iteration, throwable),
                3,
                10);
    }

    @Override
    public boolean isFinished(BaseCacheInitializer initializer) {
        Cache<String, Serializable> workCache = initializer.getWorkCache();

        // Check if persistent sessions were already loaded in this DC. This is possible just for offline sessions ATM
        Boolean sessionsLoaded = (Boolean) workCache
                .getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE)
                .get(OfflinePersistentUserSessionLoader.PERSISTENT_SESSIONS_LOADED_IN_CURRENT_DC);

        if ((cacheName.equals(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME) || (cacheName.equals(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME)))
                && sessionsLoaded != null && sessionsLoaded) {
            log.debugf("Sessions already loaded in current DC. Skip sessions loading from remote cache '%s'", cacheName);
            return true;
        } else {
            log.debugf("Sessions maybe not yet loaded in current DC. Will load them from remote cache '%s'", cacheName);
            return false;
        }
    }


    @Override
    public void afterAllSessionsLoaded(BaseCacheInitializer initializer) {
    }


    protected Cache getCache(KeycloakSession session) {
        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);
        return ispn.getCache(cacheName);
    }


    // Get remoteCache, which may be secured
    protected RemoteCache getRemoteCache(KeycloakSession session) {
        InfinispanConnectionProvider ispn = session.getProvider(InfinispanConnectionProvider.class);
        return ispn.getRemoteCache(cacheName);
    }


    @Override
    public String toString() {
        return new StringBuilder("RemoteCacheSessionsLoader [ ")
                .append("cacheName: ").append(cacheName)
                .append(", sessionsPerSegment: ").append(sessionsPerSegment)
                .append(" ]")
                .toString();
    }
}
