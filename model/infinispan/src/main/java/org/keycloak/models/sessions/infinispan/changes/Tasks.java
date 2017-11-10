/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask.CacheOperation;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask.CrossDCMessageStatus;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;

/**
 *
 * @author hmlnarik
 */
public class Tasks {

    private static final SessionUpdateTask<? extends SessionEntity> ADD_IF_ABSENT_SYNC = new SessionUpdateTask<SessionEntity>() {
        @Override
        public void runUpdate(SessionEntity entity) {
        }

        @Override
        public CacheOperation getOperation(SessionEntity entity) {
            return CacheOperation.ADD_IF_ABSENT;
        }

        @Override
        public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<SessionEntity> sessionWrapper) {
            return CrossDCMessageStatus.SYNC;
        }
    };

    private static final SessionUpdateTask<? extends SessionEntity> REMOVE_SYNC = new SessionUpdateTask<SessionEntity>() {
        @Override
        public void runUpdate(SessionEntity entity) {
        }

        @Override
        public CacheOperation getOperation(SessionEntity entity) {
            return CacheOperation.REMOVE;
        }

        @Override
        public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<SessionEntity> sessionWrapper) {
            return CrossDCMessageStatus.SYNC;
        }
    };

    /**
     * Returns a typed task of type {@link CacheOperation#ADD_IF_ABSENT} that does no other update. This operation has DC message
     * status {@link CrossDCMessageStatus#SYNC}.
     * @param <S>
     * @return
     */
    public static <S extends SessionEntity> SessionUpdateTask<S> addIfAbsentSync() {
        return (SessionUpdateTask<S>) ADD_IF_ABSENT_SYNC;
    }

    /**
     * Returns a typed task of type {@link CacheOperation#REMOVE} that does no other update. This operation has DC message
     * status {@link CrossDCMessageStatus#SYNC}.
     * @param <S>
     * @return
     */
    public static <S extends SessionEntity> SessionUpdateTask<S> removeSync() {
        return (SessionUpdateTask<S>) REMOVE_SYNC;
    }
}
