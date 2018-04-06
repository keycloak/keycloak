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
import org.keycloak.models.KeycloakSessionFactory;

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

                log.debugf("New initializer state pushed. The state is: %s", state);
            }

            // Loader callback after the task is finished
            this.sessionLoader.afterAllSessionsLoaded(this);

        } finally {
            if (distributed) {
                executorService.shutdown();
            }
            localExecutor.shutdown();
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
