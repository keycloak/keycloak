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

import org.infinispan.Cache;
import org.infinispan.commons.CacheConfigurationException;
import org.infinispan.commons.CacheException;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.manager.ClusterExecutor;
import org.infinispan.remoting.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.io.Serializable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * Startup initialization for reading persistent userSessions to be filled into infinispan/memory . In cluster,
 * the initialization is distributed among all cluster nodes, so the startup time is even faster
 *
 * Implementation is pretty generic and doesn't contain any "userSession" specific stuff. All logic related to how are sessions loaded is in the SessionLoader implementation
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanCacheInitializer extends BaseCacheInitializer {

    private static final Logger log = Logger.getLogger(InfinispanCacheInitializer.class);

    private final int maxErrors;


    public InfinispanCacheInitializer(KeycloakSessionFactory sessionFactory, Cache<String, Serializable> workCache, SessionLoader sessionLoader, String stateKeySuffix, int sessionsPerSegment, int maxErrors) {
        super(sessionFactory, workCache, sessionLoader, stateKeySuffix, sessionsPerSegment);
        this.maxErrors = maxErrors;
    }


    @Override
    public void initCache() {
        final ComponentRegistry cr = this.workCache.getAdvancedCache().getComponentRegistry();
        try {
            cr.registerComponent(sessionFactory, KeycloakSessionFactory.class);
        } catch (UnsupportedOperationException | CacheConfigurationException ex) {
            if (cr.getComponent(KeycloakSessionFactory.class) != sessionFactory) {
                throw ex;
            }
        }
    }


    // Just coordinator will run this
    @Override
    protected void startLoading() {
        InitializerState state = getStateFromCache();
        SessionLoader.LoaderContext[] ctx = new SessionLoader.LoaderContext[1];
        if (state == null) {
            // Rather use separate transactions for update and counting
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession session) {
                    sessionLoader.init(session);
                }

            });

            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession session) {
                    ctx[0] = sessionLoader.computeLoaderContext(session);
                }

            });

            state = new InitializerState(ctx[0].getSegmentsCount());
        } else {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession session) {
                    ctx[0] = sessionLoader.computeLoaderContext(session);
                }

            });
        }

        log.debugf("Start loading with loader: '%s', ctx: '%s' , state: %s",
                sessionLoader.toString(), ctx[0].toString(), state.toString());

        startLoadingImpl(state, ctx[0]);
    }


    protected void startLoadingImpl(InitializerState state, SessionLoader.LoaderContext loaderCtx) {
        // Assume each worker has same processor's count
        int processors = Runtime.getRuntime().availableProcessors();

        Transport transport = workCache.getCacheManager().getTransport();

        // Every worker iteration will be executed on single node. Use 3 failover attempts for each segment (should be sufficient in all cases)
        ClusterExecutor clusterExecutor = workCache.getCacheManager().executor()
                .singleNodeSubmission(3);

        int errors = 0;
        int segmentToLoad = 0;

        //try {
            SessionLoader.WorkerResult previousResult = null;
            SessionLoader.WorkerResult nextResult = null;
            int distributedWorkersCount = 0;
            boolean firstTryForSegment = true;

            while (segmentToLoad < state.getSegmentsCount()) {
                if (firstTryForSegment) {
                    // do not change the node count if it's not the first try
                    int nodesCount = transport==null ? 1 : transport.getMembers().size();
                    distributedWorkersCount = processors * nodesCount;
                }

                log.debugf("Starting next iteration with %d workers", distributedWorkersCount);

                List<Integer> segments = state.getSegmentsToLoad(segmentToLoad, distributedWorkersCount);

                if (log.isTraceEnabled()) {
                    log.trace("unfinished segments for this iteration: " + segments);
                }

                List<CompletableFuture<Void>> futures = new LinkedList<>();
                final Queue<SessionLoader.WorkerResult> results = new ConcurrentLinkedQueue<>();

                CompletableFuture<Void> completableFuture = null;
                for (Integer segment : segments) {
                    SessionLoader.WorkerContext workerCtx = sessionLoader.computeWorkerContext(loaderCtx, segment, segment - segmentToLoad, previousResult);

                    SessionInitializerWorker worker = new SessionInitializerWorker();
                    worker.setWorkerEnvironment(loaderCtx, workerCtx, sessionLoader, workCache.getName());

                    completableFuture = clusterExecutor.submitConsumer(worker, (address, workerResult, throwable) -> {
                        log.tracef("Calling triConsumer on address %s, throwable message: %s, segment: %s", address, throwable == null ? "null" : throwable.getMessage(),
                                workerResult == null ? null : workerResult.getSegment());

                        if (throwable != null) {
                            throw new CacheException(throwable);
                        }
                        results.add(workerResult);
                    });

                    futures.add(completableFuture);
                }

                boolean anyFailure = false;

                // Make sure that all workers are finished
                for (CompletableFuture<Void> future : futures) {
                    try {
                        future.get();
                    } catch (InterruptedException ie) {
                        anyFailure = true;
                        errors++;
                        log.error("Interruped exception when computed future. Errors: " + errors, ie);
                    } catch (ExecutionException ee) {
                        anyFailure = true;
                        errors++;
                        log.error("ExecutionException when computed future. Errors: " + errors, ee);
                    }
                }

                // Check the results
                for (SessionLoader.WorkerResult result : results) {
                    if (result.isSuccess()) {
                        state.markSegmentFinished(result.getSegment());
                        if (result.getSegment() == segmentToLoad + distributedWorkersCount - 1) {
                            // last result for next iteration when complete
                            nextResult = result;
                        }
                    } else {
                        if (log.isTraceEnabled()) {
                            log.tracef("Segment %d failed to compute", result.getSegment());
                        }
                        anyFailure = true;
                    }
                }

                if (errors >= maxErrors) {
                    throw new RuntimeException("Maximum count of worker errors occured. Limit was " + maxErrors + ". See server.log for details");
                }

                if (!anyFailure) {
                    // everything is OK, prepare the new row
                    segmentToLoad += distributedWorkersCount;
                    firstTryForSegment = true;
                    previousResult = nextResult;
                    nextResult = null;
                    if (log.isTraceEnabled()) {
                        log.debugf("New initializer state is: %s", state);
                    }
                } else {
                    // some segments failed, try to load unloaded segments
                    firstTryForSegment = false;
                }
            }

            // Push the state after computation is finished
            saveStateToCache(state);

            // Loader callback after the task is finished
            this.sessionLoader.afterAllSessionsLoaded(this);

    }
}
