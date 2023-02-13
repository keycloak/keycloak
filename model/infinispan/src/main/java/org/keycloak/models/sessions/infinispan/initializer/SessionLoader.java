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

import org.keycloak.models.KeycloakSession;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface SessionLoader<LOADER_CONTEXT extends SessionLoader.LoaderContext,
        WORKER_CONTEXT extends SessionLoader.WorkerContext,
        WORKER_RESULT extends SessionLoader.WorkerResult> extends Serializable {

    /**
     * Will be triggered just once on cluster coordinator node to perform some generic initialization tasks (Eg. update DB before starting load).
     *
     * NOTE: This shouldn't be used for the initialization of loader instance itself!
     *
     * @param session
     */
    void init(KeycloakSession session);


    /**
     *
     * Will be triggered just once on cluster coordinator node to count the number of segments and other context data specific to whole computation.
     * Each segment will be then later computed in one "worker" task
     *
     * This method could be expensive to call, so the "computed" loaderContext object is passed among workers/loaders and needs to be serializable
     *
     * @param session
     * @return
     */
    LOADER_CONTEXT computeLoaderContext(KeycloakSession session);


    /**
     * Compute the worker context for current iteration
     *
     * @param loaderCtx global loader context
     * @param segment the current segment (page) to compute
     * @param workerId ID of worker for current worker iteration. Usually the number 0-8 (with single cluster node)
     * @param previousResult last workerResult from previous computation. Can be empty list in case of the operation is triggered for the 1st time
     * @return
     */
    WORKER_CONTEXT computeWorkerContext(LOADER_CONTEXT loaderCtx, int segment, int workerId, WORKER_RESULT previousResult);


    /**
     * Will be called on all cluster nodes to load the specified page.
     *
     * @param session
     * @param loaderContext global loaderContext object, which was already computed before
     * @param workerContext for current iteration
     * @return
     */
    WORKER_RESULT loadSessions(KeycloakSession session, LOADER_CONTEXT loaderContext, WORKER_CONTEXT workerContext);


    /**
     * Called when it's not possible to compute current iteration and load session for some reason (EG. infinispan not yet fully initialized)
     *
     * @param loaderContext
     * @param workerContext
     * @return
     */
    WORKER_RESULT createFailedWorkerResult(LOADER_CONTEXT loaderContext, WORKER_CONTEXT workerContext);


    /**
     * This will be called on nodes to check if loading is finished. It allows loader to notify that loading is finished for some reason.
     *
     * @param initializer
     * @return
     */
    boolean isFinished(BaseCacheInitializer initializer);


    /**
     * Callback triggered on cluster coordinator once it recognize that all sessions were successfully loaded
     *
     * @param initializer
     */
    void afterAllSessionsLoaded(BaseCacheInitializer initializer);


    /**
     * Object, which contains some context data to be used by SessionLoader implementation. It's computed just once and then passed
     * to each {@link SessionLoader}. It needs to be {@link Serializable}
     */
    class LoaderContext implements Serializable {

        private final int segmentsCount;

        public LoaderContext(int segmentsCount) {
            this.segmentsCount = segmentsCount;
        }


        public int getSegmentsCount() {
            return segmentsCount;
        }

    }


    /**
     * Object, which is computed before each worker iteration and contains some data to be used by the corresponding worker iteration.
     * For example info about which segment/page should be loaded by current worker.
     */
    class WorkerContext implements Serializable {

        private final int segment;
        private final int workerId;

        public WorkerContext(int segment, int workerId) {
            this.segment = segment;
            this.workerId = workerId;
        }


        public int getSegment() {
            return this.segment;
        }


        public int getWorkerId() {
            return this.workerId;
        }
    }


    /**
     * Result of single worker iteration
     */
    class WorkerResult implements Serializable {

        private final boolean success;
        private final int segment;
        private final int workerId;


        public WorkerResult(boolean success, int segment, int workerId) {
            this.success = success;
            this.segment = segment;
            this.workerId = workerId;
        }


        public boolean isSuccess() {
            return success;
        }


        public int getSegment() {
            return segment;
        }


        public int getWorkerId() {
            return workerId;
        }

    }
}
