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
     *
     * Will be triggered just once on cluster coordinator node to count the number of segments and other context data specific to whole computation.
     * Each segment will be then later computed in one "worker" task
     *
     * This method could be expensive to call, so the "computed" loaderContext object is passed among workers/loaders and needs to be serializable
     *
     * @return
     */
    LOADER_CONTEXT computeLoaderContext();


    /**
     * Compute the worker context for current iteration
     *
     * @param segment the current segment (page) to compute
     * @return
     */
    WORKER_CONTEXT computeWorkerContext(int segment);


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
     * Callback triggered on cluster coordinator once it recognize that all sessions were successfully loaded
     */
    void afterAllSessionsLoaded();


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
    record WorkerContext(int segment) implements Serializable {
    }


    /**
     * Result of single worker iteration
     */
    record WorkerResult(boolean success, int segment) implements Serializable {
    }
}
