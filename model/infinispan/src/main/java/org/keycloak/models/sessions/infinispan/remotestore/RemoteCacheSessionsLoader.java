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
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.MetadataValue;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Retry;
import org.keycloak.connections.infinispan.DefaultInfinispanConnectionProviderFactory;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
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
    public RemoteCacheSessionsLoaderContext computeLoaderContext() {
        return new RemoteCacheSessionsLoaderContext(sessionsPerSegment);

    }

    @Override
    public WorkerContext computeWorkerContext(int segment) {
        return new WorkerContext(segment);
    }


    @Override
    public WorkerResult loadSessions(KeycloakSession session, RemoteCacheSessionsLoaderContext loaderContext, WorkerContext ctx) {
        Cache<Object, Object> cache = getCache(session);
        Cache<Object, Object> decoratedCache = cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE, Flag.IGNORE_RETURN_VALUES);
        RemoteCache<?, ?> remoteCache = getRemoteCache(session);

        int countLoaded = 0;
        try (CloseableIterator<Map.Entry<Object, MetadataValue<Object>>> it = remoteCache.retrieveEntriesWithMetadata(null, loaderContext.getSessionsPerSegment())) {
            Map<Object, Object> toInsertExpiring = new HashMap<>(loaderContext.getSessionsPerSegment());
            Map<Object, Object> toInsertImmortal = new HashMap<>(loaderContext.getSessionsPerSegment());
            int count = 0;
            int maxLifespanExpiring = 0;
            int maxIdleExpiring = -1;
            int maxIdleImmortal = -1;
            while (it.hasNext()) {
                Map.Entry<Object, MetadataValue<Object>> entry = it.next();
                boolean isImmortal = entry.getValue().getLifespan() < 0;
                boolean shouldInsert = true;

                if (!isImmortal) {
                    // Calculate the remaining lifetime reduced by the current time, not Keycloak time as the remote Infinispan isn't on Keycloak's clock.
                    // The lifetime will be larger than on the remote store for those entries, but all sessions contain timestamp which will be validated anyway.
                    // If we don't trust the clock calculations here, we would instead use the maxLifeSpan as is, which could enlarge the expiry time significantly.
                    int remainingLifespan = entry.getValue().getLifespan() - (int) ((System.currentTimeMillis() - entry.getValue().getCreated()) / 1000);
                    maxLifespanExpiring = Math.max(maxLifespanExpiring, remainingLifespan);
                    if (remainingLifespan <= 0) {
                        shouldInsert = false;
                    }
                }

                if (entry.getValue().getMaxIdle() > 0) {
                    // The max idle time on the remote store is set to the max lifetime as remote store entries are not touched on read, and therefore would otherwise expire too early.
                    // Still, this is the only number we have available, so we use it.
                    if (isImmortal) {
                        maxIdleImmortal = Math.max(maxIdleImmortal, entry.getValue().getMaxIdle());
                    } else {
                        maxIdleExpiring = Math.max(maxIdleExpiring, entry.getValue().getMaxIdle());
                    }
                }

                if (shouldInsert) {
                    (isImmortal ? toInsertImmortal : toInsertExpiring).put(entry.getKey(), entry.getValue().getValue());
                    ++countLoaded;
                }

                if (++count == loaderContext.getSessionsPerSegment()) {
                    if (!toInsertExpiring.isEmpty()) {
                        insertSessions(decoratedCache, toInsertExpiring, maxIdleExpiring, maxLifespanExpiring);
                        toInsertExpiring.clear();
                        maxLifespanExpiring = 0;
                        maxIdleExpiring = -1;
                    }
                    if (!toInsertImmortal.isEmpty()) {
                        insertSessions(decoratedCache, toInsertImmortal, maxIdleImmortal, -1);
                        toInsertImmortal.clear();
                        maxIdleImmortal = -1;
                    }
                    count = 0;
                }
            }

            // last batch
            if (!toInsertExpiring.isEmpty()) {
                insertSessions(decoratedCache, toInsertExpiring, maxIdleExpiring, maxLifespanExpiring);
            }
            if (!toInsertImmortal.isEmpty()) {
                insertSessions(decoratedCache, toInsertImmortal, maxIdleImmortal, -1);
            }
        } catch (RuntimeException e) {
            log.warnf(e, "Error loading sessions from remote cache '%s' for segment '%d'", remoteCache.getName(), ctx.segment());
            throw e;
        }

        log.debugf("Successfully finished loading sessions from cache '%s' . Segment: %d, Count of sessions loaded: %d", cache.getName(), ctx.segment(), countLoaded);

        return new WorkerResult(true, ctx.segment());
    }

    private void insertSessions(Cache<Object, Object> cache, Map<Object, Object> entries, int maxIdle, int lifespan) {
        log.debugf("Adding %d entries to cache '%s'", entries.size(), cacheName);

        // The `putAll` operation might time out when a node becomes unavailable, therefore, retry.
        Retry.executeWithBackoff(
                (int iteration) -> {
                    DefaultInfinispanConnectionProviderFactory.runWithReadLockOnCacheManager(() -> {
                        // With Infinispan 14.0.21/14.0.19, we've seen deadlocks in tests where this future never completed when shutting down the internal Infinispan.
                        // Therefore, prevent the shutdown of the internal Infinispan during this step.
                        cache.putAll(entries, lifespan, TimeUnit.SECONDS, maxIdle, TimeUnit.SECONDS);
                    });
                },
                (iteration, throwable) -> log.warnf("Unable to put entries into the cache in iteration %s", iteration, throwable),
                3,
                10);
    }

    @Override
    public void afterAllSessionsLoaded() {
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
