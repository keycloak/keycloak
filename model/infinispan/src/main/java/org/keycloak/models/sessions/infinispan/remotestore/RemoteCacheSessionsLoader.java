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
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.impl.RemoteCacheImpl;
import org.infinispan.client.hotrod.impl.operations.IterationStartOperation;
import org.infinispan.client.hotrod.impl.operations.IterationStartResponse;
import org.infinispan.client.hotrod.impl.operations.OperationsFactory;
import org.infinispan.commons.util.CloseableIterator;
import org.infinispan.context.Flag;
import org.jboss.logging.Logger;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.sessions.infinispan.initializer.BaseCacheInitializer;
import org.keycloak.models.sessions.infinispan.initializer.OfflinePersistentUserSessionLoader;
import org.keycloak.models.sessions.infinispan.initializer.SessionLoader;

import static org.infinispan.client.hotrod.impl.Util.await;

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
        RemoteCache remoteCache = getRemoteCache(session);
        int sessionsTotal = remoteCache.size();
        int ispnSegments = getIspnSegmentsCount(remoteCache);

        return new RemoteCacheSessionsLoaderContext(ispnSegments, sessionsPerSegment, sessionsTotal);

    }


    protected int getIspnSegmentsCount(RemoteCache remoteCache) {
        OperationsFactory operationsFactory = ((RemoteCacheImpl) remoteCache).getOperationsFactory();
        Map<SocketAddress, Set<Integer>> segmentsByAddress = operationsFactory.getPrimarySegmentsByAddress();

        for (Map.Entry<SocketAddress, Set<Integer>> entry : segmentsByAddress.entrySet()) {
            SocketAddress targetAddress = entry.getKey();

            // Same like RemoteCloseableIterator.startInternal
            IterationStartOperation iterationStartOperation = operationsFactory.newIterationStartOperation(null, null, null, sessionsPerSegment, false, null, targetAddress);
            IterationStartResponse startResponse = await(iterationStartOperation.execute());

            try {
                // Could happen for non-clustered caches
                if (startResponse.getSegmentConsistentHash() == null) {
                    return -1;
                } else {
                    return startResponse.getSegmentConsistentHash().getNumSegments();
                }
            } finally {
                startResponse.getChannel().close();
            }
        }
        // Handle the case when primary segments owned by the address are not known
        return -1;
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
        Cache cache = getCache(session);
        Cache decoratedCache = cache.getAdvancedCache().withFlags(Flag.SKIP_CACHE_LOAD, Flag.SKIP_CACHE_STORE, Flag.IGNORE_RETURN_VALUES);
        RemoteCache remoteCache = getRemoteCache(session);

        Set<Integer> myIspnSegments = getMyIspnSegments(ctx.getSegment(), loaderContext);

        log.debugf("Will do bulk load of sessions from remote cache '%s' . Segment: %d", cache.getName(), ctx.getSegment());

        Map<Object, Object> remoteEntries = new HashMap<>();
        CloseableIterator<Map.Entry> iterator = null;
        int countLoaded = 0;
        try {
            iterator = remoteCache.retrieveEntries(null, myIspnSegments, loaderContext.getSessionsPerSegment());
            while (iterator.hasNext()) {
                countLoaded++;
                Map.Entry entry = iterator.next();
                remoteEntries.put(entry.getKey(), entry.getValue());
            }
        } catch (RuntimeException e) {
            log.warnf(e, "Error loading sessions from remote cache '%s' for segment '%d'", remoteCache.getName(), ctx.getSegment());
            throw e;
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }

        decoratedCache.putAll(remoteEntries);

        log.debugf("Successfully finished loading sessions from cache '%s' . Segment: %d, Count of sessions loaded: %d", cache.getName(), ctx.getSegment(), countLoaded);

        return new WorkerResult(true, ctx.getSegment(), ctx.getWorkerId());
    }


    // Compute set of ISPN segments into 1 "worker" segment
    protected Set<Integer> getMyIspnSegments(int segment, RemoteCacheSessionsLoaderContext ctx) {
        // Remote cache is non-clustered
        if (ctx.getIspnSegmentsCount() < 0) {
            return null;
        }

        if (ctx.getIspnSegmentsCount() % ctx.getSegmentsCount() > 0) {
            throw new IllegalStateException("Illegal state. IspnSegmentsCount: " + ctx.getIspnSegmentsCount() + ", segmentsCount: " + ctx.getSegmentsCount());
        }

        int countPerSegment = ctx.getIspnSegmentsCount() / ctx.getSegmentsCount();
        int first = segment * countPerSegment;
        int last = first + countPerSegment - 1;

        Set<Integer> myIspnSegments = new HashSet<>();
        for (int i=first ; i<=last ; i++) {
            myIspnSegments.add(i);
        }
        return myIspnSegments;

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
