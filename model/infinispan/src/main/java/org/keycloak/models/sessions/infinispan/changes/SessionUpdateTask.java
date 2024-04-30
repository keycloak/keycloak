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

package org.keycloak.models.sessions.infinispan.changes;

import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface SessionUpdateTask<S extends SessionEntity> {

    void runUpdate(S entity);

    CacheOperation getOperation(S entity);

    CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<S> sessionWrapper);

    enum CacheOperation {

        ADD,
        ADD_IF_ABSENT, // ADD_IF_ABSENT throws an exception if there is existing value
        REMOVE,
        REPLACE;

        CacheOperation merge(CacheOperation other, SessionEntity entity) {
            if (this == REMOVE || other == REMOVE) {
                return REMOVE;
            }

            if (this == ADD | this == ADD_IF_ABSENT) {
                if (other == ADD | other == ADD_IF_ABSENT) {
                    throw new IllegalStateException("Illegal state. Task already in progress for session " + entity.toString());
                }

                return this;
            }

            // Lowest priority
            return REPLACE;
        }
    }


    enum CrossDCMessageStatus {
        SYNC,
        //ASYNC,
        // QUEUE,
        NOT_NEEDED;


        CrossDCMessageStatus merge(CrossDCMessageStatus other) {
            if (this == SYNC || other == SYNC) {
                return SYNC;
            }

            /*if (this == ASYNC || other == ASYNC) {
                return ASYNC;
            }*/

            return NOT_NEEDED;
        }

    }

}
