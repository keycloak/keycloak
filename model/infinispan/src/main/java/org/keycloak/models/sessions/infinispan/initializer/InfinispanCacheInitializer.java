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
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.io.Serializable;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Startup initialization for reading persistent userSessions to be filled into infinispan/memory.
 *
 * Implementation is pretty generic and doesn't contain any "userSession" specific stuff. All logic related to how sessions are loaded is in the SessionLoader implementation
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class InfinispanCacheInitializer extends BaseCacheInitializer {

    private static final Logger log = Logger.getLogger(InfinispanCacheInitializer.class);

    private final int maxErrors;

    // Effectively no timeout
    private final int stalledTimeoutInSeconds;

    public InfinispanCacheInitializer(KeycloakSessionFactory sessionFactory, Cache<String, Serializable> workCache, SessionLoader sessionLoader, String stateKeySuffix, int maxErrors, int stalledTimeoutInSeconds) {
        super(sessionFactory, workCache, sessionLoader, stateKeySuffix);
        this.maxErrors = maxErrors;
        this.stalledTimeoutInSeconds = stalledTimeoutInSeconds;
    }


    // Just coordinator will run this
    @Override
    protected void startLoading() {
        InitializerState state = getStateFromCache();
        SessionLoader.LoaderContext[] ctx = new SessionLoader.LoaderContext[1];
        if (state == null) {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession session) {
                    ctx[0] = sessionLoader.computeLoaderContext();
                }

            });

            state = new InitializerState(ctx[0].getSegmentsCount());
        } else {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {
                @Override
                public void run(KeycloakSession session) {
                    ctx[0] = sessionLoader.computeLoaderContext();
                }

            });
        }

        log.debugf("Start loading with loader: '%s', ctx: '%s' , state: %s",
                sessionLoader.toString(), ctx[0].toString(), state.toString());

        startLoadingImpl(state, ctx[0]);
    }

    @Override
    protected int getStalledTimeoutInSeconds() {
        return this.stalledTimeoutInSeconds;
    }

    protected void startLoadingImpl(InitializerState state, SessionLoader.LoaderContext loaderCtx) {
        final int errors = 0;
        int segmentToLoad = 0;

        int distributedWorkersCount = 1;

        while (segmentToLoad < state.getSegmentsCount()) {

            log.debugf("Starting next iteration with %d workers", distributedWorkersCount);

            List<Integer> segments = state.getSegmentsToLoad(segmentToLoad, distributedWorkersCount);

            if (log.isTraceEnabled()) {
                log.trace("unfinished segments for this iteration: " + segments);
            }

            Queue<SessionLoader.WorkerResult> results = new ConcurrentLinkedQueue<>();

            for (Integer segment : segments) {
                SessionLoader.WorkerContext workerCtx = sessionLoader.computeWorkerContext(segment);

                SessionInitializerWorker worker = new SessionInitializerWorker();
                worker.setWorkerEnvironment(loaderCtx, workerCtx, sessionLoader);

                results.add(worker.apply(sessionFactory));
            }

            boolean anyFailure = false;

            // Check the results
            for (SessionLoader.WorkerResult result : results) {
                if (result.success()) {
                    state.markSegmentFinished(result.segment());
                    if (result.segment() == segmentToLoad + distributedWorkersCount - 1) {
                        // last result for next iteration when complete
                    }
                } else {
                    if (log.isTraceEnabled()) {
                        log.tracef("Segment %d failed to compute", result.segment());
                    }
                    anyFailure = true;
                }
            }

            if (errors >= maxErrors) {
                throw new RuntimeException("Maximum count of worker errors occurred. Limit was " + maxErrors + ". See server.log for details");
            }

            if (!anyFailure) {
                // everything is OK, prepare the new row
                segmentToLoad += distributedWorkersCount;
                if (log.isTraceEnabled()) {
                    log.debugf("New initializer state is: %s", state);
                }
            }
        }

        // Push the state after computation is finished
        saveStateToCache(state);

        // Loader callback after the task is finished
        this.sessionLoader.afterAllSessionsLoaded();

    }
}
