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

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.infinispan.Cache;
import org.infinispan.context.Flag;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.remoting.transport.Transport;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * Startup initialization for reading persistent userSessions/clientSessions to be filled into infinispan/memory . In cluster,
 * the initialization is distributed among all cluster nodes, so the startup time is even faster
 *
 * TODO: Move to clusterService. Implementation is already pretty generic and doesn't contain any "userSession" specific stuff. All sessions-specific logic is in the SessionLoader implementation
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanUserSessionInitializer {

    private static final String STATE_KEY_PREFIX = "distributed::";

    private static final Logger log = Logger.getLogger(InfinispanUserSessionInitializer.class);

    private final KeycloakSessionFactory sessionFactory;
    private final Cache<String, Serializable> workCache;
    private final SessionLoader sessionLoader;
    private final int maxErrors;
    private final int sessionsPerSegment;
    private final String stateKey;


    public InfinispanUserSessionInitializer(KeycloakSessionFactory sessionFactory, Cache<String, Serializable> workCache, SessionLoader sessionLoader, int maxErrors, int sessionsPerSegment, String stateKeySuffix) {
        this.sessionFactory = sessionFactory;
        this.workCache = workCache;
        this.sessionLoader = sessionLoader;
        this.maxErrors = maxErrors;
        this.sessionsPerSegment = sessionsPerSegment;
        this.stateKey = STATE_KEY_PREFIX + stateKeySuffix;
    }

    public void initCache() {
        this.workCache.getAdvancedCache().getComponentRegistry().registerComponent(sessionFactory, KeycloakSessionFactory.class);
    }


    public void loadPersistentSessions() {
        if (isFinished()) {
            return;
        }

        while (!isFinished()) {
            if (!isCoordinator()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    log.error("Interrupted", ie);
                }
            } else {
                startLoading();
            }
        }
    }


    private boolean isFinished() {
        InitializerState state = (InitializerState) workCache.get(stateKey);
        return state != null && state.isFinished();
    }


    private InitializerState getOrCreateInitializerState() {
        InitializerState state = (InitializerState) workCache.get(stateKey);
        if (state == null) {
            final int[] count = new int[1];

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
                    count[0] = sessionLoader.getSessionsCount(session);
                }

            });

            state = new InitializerState();
            state.init(count[0], sessionsPerSegment);
            saveStateToCache(state);
        }
        return state;

    }


    private void saveStateToCache(final InitializerState state) {

        // 3 attempts to send the message (it may fail if some node fails in the meantime)
        retry(3, new Runnable() {

            @Override
            public void run() {

                // Save this synchronously to ensure all nodes read correct state
                InfinispanUserSessionInitializer.this.workCache.getAdvancedCache().
                        withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FORCE_SYNCHRONOUS)
                        .put(stateKey, state);
            }

        });
    }


    private boolean isCoordinator() {
        Transport transport = workCache.getCacheManager().getTransport();
        return transport == null || transport.isCoordinator();
    }


    // Just coordinator will run this
    private void startLoading() {
        InitializerState state = getOrCreateInitializerState();

        // Assume each worker has same processor's count
        int processors = Runtime.getRuntime().availableProcessors();

        ExecutorService localExecutor = Executors.newCachedThreadPool();
        Transport transport = workCache.getCacheManager().getTransport();
        boolean distributed = transport != null;
        ExecutorService executorService = distributed ? new DefaultExecutorService(workCache, localExecutor) : localExecutor;

        int errors = 0;

        try {
            while (!state.isFinished()) {
                int nodesCount = transport==null ? 1 : transport.getMembers().size();
                int distributedWorkersCount = processors * nodesCount;

                log.debugf("Starting next iteration with %d workers", distributedWorkersCount);

                List<Integer> segments = state.getUnfinishedSegments(distributedWorkersCount);

                if (log.isTraceEnabled()) {
                    log.trace("unfinished segments for this iteration: " + segments);
                }

                List<Future<WorkerResult>> futures = new LinkedList<>();
                for (Integer segment : segments) {
                    SessionInitializerWorker worker = new SessionInitializerWorker();
                    worker.setWorkerEnvironment(segment, sessionsPerSegment, sessionLoader);
                    if (!distributed) {
                        worker.setEnvironment(workCache, null);
                    }

                    Future<WorkerResult> future = executorService.submit(worker);
                    futures.add(future);
                }

                for (Future<WorkerResult> future : futures) {
                    try {
                        WorkerResult result = future.get();

                        if (result.getSuccess()) {
                            int computedSegment = result.getSegment();
                            state.markSegmentFinished(computedSegment);
                        } else {
                            if (log.isTraceEnabled()) {
                                log.tracef("Segment %d failed to compute", result.getSegment());
                            }
                        }
                    } catch (InterruptedException ie) {
                        errors++;
                        log.error("Interruped exception when computed future. Errors: " + errors, ie);
                    } catch (ExecutionException ee) {
                        errors++;
                        log.error("ExecutionException when computed future. Errors: " + errors, ee);
                    }
                }

                if (errors >= maxErrors) {
                    throw new RuntimeException("Maximum count of worker errors occured. Limit was " + maxErrors + ". See server.log for details");
                }

                saveStateToCache(state);

                if (log.isDebugEnabled()) {
                    log.debug("New initializer state pushed. The state is: " + state.printState());
                }
            }
        } finally {
            if (distributed) {
                executorService.shutdown();
            }
            localExecutor.shutdown();
        }
    }

    private void retry(int retry, Runnable runnable) {
        while (true) {
            try {
                runnable.run();
                return;
            } catch (RuntimeException e) {
                ComponentStatus status = workCache.getStatus();
                if (status.isStopping() || status.isTerminated()) {
                    log.warn("Failed to put initializerState to the cache. Cache is already terminating");
                    log.debug(e.getMessage(), e);
                    return;
                }
                retry--;
                if (retry == 0) {
                    throw e;
                }
            }
        }
    }

    public static class WorkerResult implements Serializable {

        private Integer segment;
        private Boolean success;

        public static WorkerResult create (Integer segment, boolean success) {
            WorkerResult res = new WorkerResult();
            res.setSegment(segment);
            res.setSuccess(success);
            return res;
        }

        public Integer getSegment() {
            return segment;
        }

        public void setSegment(Integer segment) {
            this.segment = segment;
        }

        public Boolean getSuccess() {
            return success;
        }

        public void setSuccess(Boolean success) {
            this.success = success;
        }
    }
}
