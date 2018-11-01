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
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.remoting.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

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
        this.workCache.getAdvancedCache().getComponentRegistry().registerComponent(sessionFactory, KeycloakSessionFactory.class);
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

        ExecutorService localExecutor = Executors.newCachedThreadPool();
        Transport transport = workCache.getCacheManager().getTransport();
        boolean distributed = transport != null;
        ExecutorService executorService = distributed ? new DefaultExecutorService(workCache, localExecutor) : localExecutor;

        int errors = 0;

        try {
            List<SessionLoader.WorkerResult> previousResults = new LinkedList<>();

            while (!state.isFinished()) {
                int nodesCount = transport==null ? 1 : transport.getMembers().size();
                int distributedWorkersCount = processors * nodesCount;

                log.debugf("Starting next iteration with %d workers", distributedWorkersCount);

                List<Integer> segments = state.getUnfinishedSegments(distributedWorkersCount);

                if (log.isTraceEnabled()) {
                    log.trace("unfinished segments for this iteration: " + segments);
                }

                List<Future<SessionLoader.WorkerResult>> futures = new LinkedList<>();

                int workerId = 0;
                for (Integer segment : segments) {
                    SessionLoader.WorkerContext workerCtx = sessionLoader.computeWorkerContext(loaderCtx, segment, workerId, previousResults);

                    SessionInitializerWorker worker = new SessionInitializerWorker();
                    worker.setWorkerEnvironment(loaderCtx, workerCtx, sessionLoader);

                    if (!distributed) {
                        worker.setEnvironment(workCache, null);
                    }

                    Future<SessionLoader.WorkerResult> future = executorService.submit(worker);
                    futures.add(future);

                    workerId++;
                }

                boolean anyFailure = false;
                for (Future<SessionLoader.WorkerResult> future : futures) {
                    try {
                        SessionLoader.WorkerResult result = future.get();
                        previousResults.add(result);

                        if (!result.isSuccess()) {
                            if (log.isTraceEnabled()) {
                                log.tracef("Segment %d failed to compute", result.getSegment());
                            }
                            anyFailure = true;
                        }
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

                if (errors >= maxErrors) {
                    throw new RuntimeException("Maximum count of worker errors occured. Limit was " + maxErrors + ". See server.log for details");
                }

                // Save just if no error happened. Otherwise re-compute
                if (!anyFailure) {
                    for (SessionLoader.WorkerResult result : previousResults) {
                        state.markSegmentFinished(result.getSegment());
                    }

                    log.debugf("New initializer state is: %s", state);
                }
            }

            // Push the state after computation is finished
            saveStateToCache(state);

            // Loader callback after the task is finished
            this.sessionLoader.afterAllSessionsLoaded(this);

        } finally {
            if (distributed) {
                executorService.shutdown();
            }
            localExecutor.shutdown();
        }
    }

}
