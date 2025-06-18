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

package org.keycloak.testsuite.util.cli;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.utils.KeycloakModelUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
class BatchTaskRunner {

    static void runInBatches(int first, int count, int batchCount, KeycloakSessionFactory sessionFactory, BatchTask batchTask) {

        final StateHolder state = new StateHolder();
        state.firstInThisBatch = first;
        state.remaining = count;
        state.countInThisBatch = Math.min(batchCount, state.remaining);
        while (state.remaining > 0) {
            KeycloakModelUtils.runJobInTransaction(sessionFactory, new KeycloakSessionTask() {

                @Override
                public void run(KeycloakSession session) {
                    batchTask.run(session, state.firstInThisBatch, state.countInThisBatch);
                }
            });

            // update state
            state.firstInThisBatch = state.firstInThisBatch + state.countInThisBatch;
            state.remaining = state.remaining - state.countInThisBatch;
            state.countInThisBatch = Math.min(batchCount, state.remaining);
        }
    }


    private static class StateHolder {
        int firstInThisBatch;
        int countInThisBatch;
        int remaining;
    };


    @FunctionalInterface
    public interface BatchTask {

        void run(KeycloakSession session, int firstInThisIteration, int countInThisIteration);

    }

}
