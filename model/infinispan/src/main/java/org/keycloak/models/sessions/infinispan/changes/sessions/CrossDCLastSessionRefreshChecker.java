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

import java.util.UUID;

import org.jboss.logging.Logger;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.changes.SessionUpdateTask;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class CrossDCLastSessionRefreshChecker {

    public static final Logger logger = Logger.getLogger(CrossDCLastSessionRefreshChecker.class);

    private final CrossDCLastSessionRefreshStore store;
    private final CrossDCLastSessionRefreshStore offlineStore;
    private static final String REVOKE_REFRESH_TOKEN = "revoke.refresh.token";
    private static final String OFFLINE_SESSION_IDLE_TIMEOUT = "offline.session.idle.timeout";

    public CrossDCLastSessionRefreshChecker(CrossDCLastSessionRefreshStore store, CrossDCLastSessionRefreshStore offlineStore) {
        this.store = store;
        this.offlineStore = offlineStore;
    }

    public SessionUpdateTask.CrossDCMessageStatus shouldSaveUserSessionToRemoteCache(KeycloakSession kcSession,
        RealmModel realm, SessionEntityWrapper<UserSessionEntity> sessionWrapper, boolean offline, int newLastSessionRefresh,
        UserSessionModel userSession) {

        SessionUpdateTask.CrossDCMessageStatus baseChecks = baseChecksForUserSession(kcSession, realm, offline, userSession);
        if (baseChecks != null) {
            return baseChecks;
        }

        String userSessionId = sessionWrapper.getEntity().getId();

        if (offline) {
            Integer lsrr = sessionWrapper.getLocalMetadataNoteInt(UserSessionEntity.LAST_SESSION_REFRESH_REMOTE);
            if (lsrr == null) {
                lsrr = sessionWrapper.getEntity().getStarted();
            }

            int offlineSessionIdleTimeout = 0;
            for (AuthenticatedClientSessionModel cs : userSession.getAuthenticatedClientSessions().values()) {
                String clientOfflineSessionIdleTimeout = cs.getClient().getAttribute(OFFLINE_SESSION_IDLE_TIMEOUT);
                if (clientOfflineSessionIdleTimeout != null && !clientOfflineSessionIdleTimeout.trim().isEmpty()) {
                    if (offlineSessionIdleTimeout == 0
                        || offlineSessionIdleTimeout > Integer.parseInt(clientOfflineSessionIdleTimeout)) {
                        offlineSessionIdleTimeout = Integer.parseInt(clientOfflineSessionIdleTimeout);
                    }
                }
            }
            if (offlineSessionIdleTimeout == 0) {
                offlineSessionIdleTimeout = realm.getOfflineSessionIdleTimeout();
            }

            if (lsrr + (offlineSessionIdleTimeout / 2) <= newLastSessionRefresh) {
                logger.debugf("We are going to write remotely userSession %s. Remote last session refresh: %d, New last session refresh: %d",
                        userSessionId, lsrr, newLastSessionRefresh);
                return SessionUpdateTask.CrossDCMessageStatus.SYNC;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debugf("Skip writing last session refresh to the remoteCache. Session %s newLastSessionRefresh %d", userSessionId, newLastSessionRefresh);
        }

        CrossDCLastSessionRefreshStore storeToUse = offline ? offlineStore : store;
        storeToUse.putLastSessionRefresh(kcSession, userSessionId, realm.getId(), newLastSessionRefresh);

        return SessionUpdateTask.CrossDCMessageStatus.NOT_NEEDED;
    }


    public SessionUpdateTask.CrossDCMessageStatus shouldSaveClientSessionToRemoteCache(KeycloakSession kcSession,
        RealmModel realm, SessionEntityWrapper<AuthenticatedClientSessionEntity> sessionWrapper, UserSessionModel userSession,
        boolean offline, int newTimestamp, ClientModel client) {

        SessionUpdateTask.CrossDCMessageStatus baseChecks = baseChecksForClientSession(kcSession, realm, offline, client);
        if (baseChecks != null) {
            return baseChecks;
        }

        UUID clientSessionId = sessionWrapper.getEntity().getId();

        if (offline) {
            Integer lsrr = sessionWrapper.getLocalMetadataNoteInt(AuthenticatedClientSessionEntity.LAST_TIMESTAMP_REMOTE);
            if (lsrr == null) {
                lsrr = userSession.getStarted();
            }

            int offlineSessionIdleTimeout;
            String clientOfflineSessionIdleTimeout = client.getAttribute(OFFLINE_SESSION_IDLE_TIMEOUT);
            if (clientOfflineSessionIdleTimeout != null && !clientOfflineSessionIdleTimeout.trim().isEmpty()) {
                offlineSessionIdleTimeout = Integer.parseInt(clientOfflineSessionIdleTimeout);
            } else {
                offlineSessionIdleTimeout = realm.getOfflineSessionIdleTimeout();
            }

            if (lsrr + (offlineSessionIdleTimeout / 2) <= newTimestamp) {
                    logger.debugf("We are going to write remotely for clientSession %s. Remote timestamp: %d, New timestamp: %d",
                            clientSessionId, lsrr, newTimestamp);
                return SessionUpdateTask.CrossDCMessageStatus.SYNC;
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debugf("Skip writing timestamp to the remoteCache. ClientSession %s timestamp %d", clientSessionId, newTimestamp);
        }

        return SessionUpdateTask.CrossDCMessageStatus.NOT_NEEDED;
    }

    private SessionUpdateTask.CrossDCMessageStatus baseChecksForUserSession(KeycloakSession kcSession, RealmModel realm,
        boolean offline, UserSessionModel userSession) {
        // revokeRefreshToken always writes everything to remoteCache immediately
        boolean allClientsNotRevokeRefreshToken = true;
        for (AuthenticatedClientSessionModel cs : userSession.getAuthenticatedClientSessions().values()) {
            String clientIsRevokeRefreshToken = cs.getClient().getAttribute(REVOKE_REFRESH_TOKEN);
            if ("ON".equals(clientIsRevokeRefreshToken))
                return SessionUpdateTask.CrossDCMessageStatus.SYNC;
            if (!"OFF".equals(clientIsRevokeRefreshToken))
                allClientsNotRevokeRefreshToken = false;
        }

        boolean isRevokeRefreshToken = !allClientsNotRevokeRefreshToken && realm.isRevokeRefreshToken();
        if (isRevokeRefreshToken) {
            return SessionUpdateTask.CrossDCMessageStatus.SYNC;
        }

        return baseChecks(kcSession, realm, offline);
    }

    private SessionUpdateTask.CrossDCMessageStatus baseChecksForClientSession(KeycloakSession kcSession, RealmModel realm,
        boolean offline, ClientModel client) {
        // revokeRefreshToken always writes everything to remoteCache immediately
        String clientIsRevokeRefreshToken = client.getAttribute(REVOKE_REFRESH_TOKEN);
        boolean isRevokeRefreshToken = "ON".equals(clientIsRevokeRefreshToken)
            || (!"OFF".equals(clientIsRevokeRefreshToken) && realm.isRevokeRefreshToken());
        if (isRevokeRefreshToken) {
            return SessionUpdateTask.CrossDCMessageStatus.SYNC;
        }

        return baseChecks(kcSession, realm, offline);
    }

    private SessionUpdateTask.CrossDCMessageStatus baseChecks(KeycloakSession kcSession, RealmModel realm, boolean offline) {
        // We're likely not in cross-dc environment. Doesn't matter what we return
        CrossDCLastSessionRefreshStore storeToUse = offline ? offlineStore : store;
        if (storeToUse == null) {
            return SessionUpdateTask.CrossDCMessageStatus.SYNC;
        }

        // Received the message from the other DC that we should update the lastSessionRefresh in local cluster
        Boolean ignoreRemoteCacheUpdate = (Boolean) kcSession.getAttribute(CrossDCLastSessionRefreshListener.IGNORE_REMOTE_CACHE_UPDATE);
        if (ignoreRemoteCacheUpdate != null && ignoreRemoteCacheUpdate) {
            return SessionUpdateTask.CrossDCMessageStatus.NOT_NEEDED;
        }

        return null;
    }

}
