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

import java.util.LinkedList;
import java.util.List;

import org.jboss.logging.Logger;
import org.keycloak.models.sessions.infinispan.entities.SessionEntity;
import org.keycloak.models.sessions.infinispan.util.SessionTimeouts;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MergedUpdate<S extends SessionEntity> implements SessionUpdateTask<S> {

    private static final Logger logger = Logger.getLogger(MergedUpdate.class);

    private final List<SessionUpdateTask<S>> childUpdates = new LinkedList<>();
    private CacheOperation operation;
    private CrossDCMessageStatus crossDCMessageStatus;
    private final long lifespanMs;
    private final long maxIdleTimeMs;


    private MergedUpdate(CacheOperation operation, CrossDCMessageStatus crossDCMessageStatus, long lifespanMs, long maxIdleTimeMs) {
        this.operation = operation;
        this.crossDCMessageStatus = crossDCMessageStatus;
        this.lifespanMs = lifespanMs;
        this.maxIdleTimeMs = maxIdleTimeMs;
    }

    @Override
    public void runUpdate(S session) {
        for (SessionUpdateTask<S> child : childUpdates) {
            child.runUpdate(session);
        }
    }

    @Override
    public CacheOperation getOperation(S session) {
        return operation;
    }

    @Override
    public CrossDCMessageStatus getCrossDCMessageStatus(SessionEntityWrapper<S> sessionWrapper) {
        return crossDCMessageStatus;
    }

    public long getLifespanMs() {
        return lifespanMs;
    }

    public long getMaxIdleTimeMs() {
        return maxIdleTimeMs;
    }


    public static <S extends SessionEntity> MergedUpdate<S> computeUpdate(List<SessionUpdateTask<S>> childUpdates, SessionEntityWrapper<S> sessionWrapper, long lifespanMs, long maxIdleTimeMs) {
        if (childUpdates == null || childUpdates.isEmpty()) {
            return null;
        }

        MergedUpdate<S> result = null;
        S session = sessionWrapper.getEntity();
        for (SessionUpdateTask<S> child : childUpdates) {
            if (result == null) {
                CacheOperation operation = child.getOperation(session);

                if (lifespanMs == SessionTimeouts.ENTRY_EXPIRED_FLAG || maxIdleTimeMs == SessionTimeouts.ENTRY_EXPIRED_FLAG) {
                    operation = CacheOperation.REMOVE;
                    logger.tracef("Entry '%s' is expired. Will remove it from the cache", sessionWrapper);
                }

                result = new MergedUpdate<>(operation, child.getCrossDCMessageStatus(sessionWrapper), lifespanMs, maxIdleTimeMs);
                result.childUpdates.add(child);
            } else {

                // Merge the operations. REMOVE is special case as other operations are not needed then.
                CacheOperation mergedOp = result.getOperation(session).merge(child.getOperation(session), session);
                if (mergedOp == CacheOperation.REMOVE) {
                    result = new MergedUpdate<>(child.getOperation(session), child.getCrossDCMessageStatus(sessionWrapper), lifespanMs, maxIdleTimeMs);
                    result.childUpdates.add(child);
                    return result;
                }

                result.operation = mergedOp;

                // Check if we need to send message to other DCs and how critical it is
                CrossDCMessageStatus currentDCStatus = result.getCrossDCMessageStatus(sessionWrapper);

                // Optimization. If we already have SYNC, we don't need to retrieve childDCStatus
                if (currentDCStatus != CrossDCMessageStatus.SYNC) {
                    CrossDCMessageStatus childDCStatus = child.getCrossDCMessageStatus(sessionWrapper);
                    result.crossDCMessageStatus = currentDCStatus.merge(childDCStatus);
                }

                // Finally add another update to the result
                result.childUpdates.add(child);
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "MergedUpdate" + childUpdates;
    }


}
