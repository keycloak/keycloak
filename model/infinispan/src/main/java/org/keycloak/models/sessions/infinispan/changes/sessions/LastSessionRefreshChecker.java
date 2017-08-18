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

package org.keycloak.models.sessions.infinispan.changes.sessions;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class LastSessionRefreshChecker {

    public static final Logger logger = Logger.getLogger(LastSessionRefreshChecker.class);

    private final LastSessionRefreshStore store;
    private final LastSessionRefreshStore offlineStore;


    public LastSessionRefreshChecker(LastSessionRefreshStore store, LastSessionRefreshStore offlineStore) {
        this.store = store;
        this.offlineStore = offlineStore;
    }


    public SessionUpdateTask.CrossDCMessageStatus getCrossDCMessageStatus(KeycloakSession kcSession, RealmModel realm, SessionEntityWrapper<UserSessionEntity> sessionWrapper, boolean offline, int newLastSessionRefresh) {
        // revokeRefreshToken always writes everything to remoteCache immediately
        if (realm.isRevokeRefreshToken()) {
            return SessionUpdateTask.CrossDCMessageStatus.SYNC;
        }

        // We're likely not in cross-dc environment. Doesn't matter what we return
        LastSessionRefreshStore storeToUse = offline ? offlineStore : store;
        if (storeToUse == null) {
            return SessionUpdateTask.CrossDCMessageStatus.SYNC;
        }

        Boolean ignoreRemoteCacheUpdate = (Boolean) kcSession.getAttribute(LastSessionRefreshListener.IGNORE_REMOTE_CACHE_UPDATE);
        if (ignoreRemoteCacheUpdate != null && ignoreRemoteCacheUpdate) {
            return SessionUpdateTask.CrossDCMessageStatus.NOT_NEEDED;
        }

        Integer lsrr = sessionWrapper.getLocalMetadataNoteInt(UserSessionEntity.LAST_SESSION_REFRESH_REMOTE);
        if (lsrr == null) {
            logger.debugf("Not available lsrr note on user session %s.", sessionWrapper.getEntity().getId());
            return SessionUpdateTask.CrossDCMessageStatus.SYNC;
        }

        int idleTimeout = offline ? realm.getOfflineSessionIdleTimeout() : realm.getSsoSessionIdleTimeout();

        if (lsrr + (idleTimeout / 2) <= newLastSessionRefresh) {
            logger.debugf("We are going to write remotely. Remote last session refresh: %d, New last session refresh: %d", (int) lsrr, newLastSessionRefresh);
            return SessionUpdateTask.CrossDCMessageStatus.SYNC;
        }

        logger.debugf("Skip writing last session refresh to the remoteCache. Session %s newLastSessionRefresh %d", sessionWrapper.getEntity().getId(), newLastSessionRefresh);

        storeToUse.putLastSessionRefresh(kcSession, sessionWrapper.getEntity().getId(), realm.getId(), newLastSessionRefresh);

        return SessionUpdateTask.CrossDCMessageStatus.NOT_NEEDED;
    }

}
