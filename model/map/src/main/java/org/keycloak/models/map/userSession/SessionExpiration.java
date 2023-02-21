/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.userSession;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.TimeAdapter;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class SessionExpiration {

    public static void setClientSessionExpiration(MapAuthenticatedClientSessionEntity entity, RealmModel realm, ClientModel client) {
        long timestampMillis = entity.getTimestamp() != null ? entity.getTimestamp() : 0L;
        if (Boolean.TRUE.equals(entity.isOffline())) {
            long sessionExpires = timestampMillis + TimeAdapter.fromSecondsToMilliseconds(realm.getOfflineSessionIdleTimeout());
            if (realm.isOfflineSessionMaxLifespanEnabled()) {
                sessionExpires = timestampMillis + TimeAdapter.fromSecondsToMilliseconds(realm.getOfflineSessionMaxLifespan());

                long clientOfflineSessionMaxLifespan;
                String clientOfflineSessionMaxLifespanPerClient = client.getAttribute(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN);
                if (clientOfflineSessionMaxLifespanPerClient != null && !clientOfflineSessionMaxLifespanPerClient.trim().isEmpty()) {
                    clientOfflineSessionMaxLifespan = TimeAdapter.fromSecondsToMilliseconds(Long.parseLong(clientOfflineSessionMaxLifespanPerClient));
                } else {
                    clientOfflineSessionMaxLifespan = TimeAdapter.fromSecondsToMilliseconds(realm.getClientOfflineSessionMaxLifespan());
                }

                if (clientOfflineSessionMaxLifespan > 0) {
                    long clientOfflineSessionMaxExpiration = timestampMillis + clientOfflineSessionMaxLifespan;
                    sessionExpires = Math.min(sessionExpires, clientOfflineSessionMaxExpiration);
                }
            }

            long expiration = timestampMillis + TimeAdapter.fromSecondsToMilliseconds(realm.getOfflineSessionIdleTimeout());

            long clientOfflineSessionIdleTimeout;
            String clientOfflineSessionIdleTimeoutPerClient = client.getAttribute(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT);
            if (clientOfflineSessionIdleTimeoutPerClient != null && !clientOfflineSessionIdleTimeoutPerClient.trim().isEmpty()) {
                clientOfflineSessionIdleTimeout = TimeAdapter.fromSecondsToMilliseconds(Long.parseLong(clientOfflineSessionIdleTimeoutPerClient));
            } else {
                clientOfflineSessionIdleTimeout = TimeAdapter.fromSecondsToMilliseconds(realm.getClientOfflineSessionIdleTimeout());
            }

            if (clientOfflineSessionIdleTimeout > 0) {
                long clientOfflineSessionIdleExpiration = timestampMillis + clientOfflineSessionIdleTimeout;
                expiration = Math.min(expiration, clientOfflineSessionIdleExpiration);
            }

            entity.setExpiration(Math.min(expiration, sessionExpires));
        } else {
            long sessionExpires = timestampMillis + (realm.getSsoSessionMaxLifespanRememberMe() > 0
                    ? TimeAdapter.fromSecondsToMilliseconds(realm.getSsoSessionMaxLifespanRememberMe()) : TimeAdapter.fromSecondsToMilliseconds(realm.getSsoSessionMaxLifespan()));

            long clientSessionMaxLifespan;
            String clientSessionMaxLifespanPerClient = client.getAttribute(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN);
            if (clientSessionMaxLifespanPerClient != null && !clientSessionMaxLifespanPerClient.trim().isEmpty()) {
                clientSessionMaxLifespan = TimeAdapter.fromSecondsToMilliseconds(Long.parseLong(clientSessionMaxLifespanPerClient));
            } else {
                clientSessionMaxLifespan = TimeAdapter.fromSecondsToMilliseconds(realm.getClientSessionMaxLifespan());
            }

            if (clientSessionMaxLifespan > 0) {
                long clientSessionMaxExpiration = timestampMillis + clientSessionMaxLifespan;
                sessionExpires = Math.min(sessionExpires, clientSessionMaxExpiration);
            }

            long expiration = timestampMillis + (realm.getSsoSessionIdleTimeoutRememberMe() > 0
                    ? TimeAdapter.fromSecondsToMilliseconds(realm.getSsoSessionIdleTimeoutRememberMe()) : TimeAdapter.fromSecondsToMilliseconds(realm.getSsoSessionIdleTimeout()));

            long clientSessionIdleTimeout;
            String clientSessionIdleTimeoutPerClient = client.getAttribute(OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT);
            if (clientSessionIdleTimeoutPerClient != null && !clientSessionIdleTimeoutPerClient.trim().isEmpty()) {
                clientSessionIdleTimeout = TimeAdapter.fromSecondsToMilliseconds(Long.parseLong(clientSessionIdleTimeoutPerClient));
            } else {
                clientSessionIdleTimeout = TimeAdapter.fromSecondsToMilliseconds(realm.getClientSessionIdleTimeout());
            }

            if (clientSessionIdleTimeout > 0) {
                long clientSessionIdleExpiration = timestampMillis + clientSessionIdleTimeout;
                expiration = Math.min(expiration, clientSessionIdleExpiration);
            }

            entity.setExpiration(Math.min(expiration, sessionExpires));
        }
    }

    public static void setUserSessionExpiration(MapUserSessionEntity entity, RealmModel realm) {
        long timestampMillis = entity.getTimestamp() != null ? entity.getTimestamp() : 0L;
        long lastSessionRefreshMillis = entity.getLastSessionRefresh() != null ? entity.getLastSessionRefresh() : 0L;
        if (Boolean.TRUE.equals(entity.isOffline())) {
            long sessionExpires = lastSessionRefreshMillis + TimeAdapter.fromSecondsToMilliseconds(realm.getOfflineSessionIdleTimeout());
            if (realm.isOfflineSessionMaxLifespanEnabled()) {
                sessionExpires = timestampMillis + TimeAdapter.fromSecondsToMilliseconds(realm.getOfflineSessionMaxLifespan());

                long clientOfflineSessionMaxLifespan = TimeAdapter.fromSecondsToMilliseconds(realm.getClientOfflineSessionMaxLifespan());

                if (clientOfflineSessionMaxLifespan > 0) {
                    long clientOfflineSessionMaxExpiration = timestampMillis + clientOfflineSessionMaxLifespan;
                    sessionExpires = Math.min(sessionExpires, clientOfflineSessionMaxExpiration);
                }
            }

            long expiration = lastSessionRefreshMillis + TimeAdapter.fromSecondsToMilliseconds(realm.getOfflineSessionIdleTimeout());

            long clientOfflineSessionIdleTimeout = TimeAdapter.fromSecondsToMilliseconds(realm.getClientOfflineSessionIdleTimeout());

            if (clientOfflineSessionIdleTimeout > 0) {
                long clientOfflineSessionIdleExpiration = Time.currentTimeMillis() + clientOfflineSessionIdleTimeout;
                expiration = Math.min(expiration, clientOfflineSessionIdleExpiration);
            }

            entity.setExpiration(Math.min(expiration, sessionExpires));
        } else {
            long sessionExpires = timestampMillis
                    + (Boolean.TRUE.equals(entity.isRememberMe()) && realm.getSsoSessionMaxLifespanRememberMe() > 0
                    ? TimeAdapter.fromSecondsToMilliseconds(realm.getSsoSessionMaxLifespanRememberMe())
                    : TimeAdapter.fromSecondsToMilliseconds(realm.getSsoSessionMaxLifespan()));

            long clientSessionMaxLifespan = TimeAdapter.fromSecondsToMilliseconds(realm.getClientSessionMaxLifespan());

            if (clientSessionMaxLifespan > 0) {
                long clientSessionMaxExpiration = timestampMillis + clientSessionMaxLifespan;
                sessionExpires = Math.min(sessionExpires, clientSessionMaxExpiration);
            }

            long expiration = lastSessionRefreshMillis + (Boolean.TRUE.equals(entity.isRememberMe()) && realm.getSsoSessionIdleTimeoutRememberMe() > 0
                    ? TimeAdapter.fromSecondsToMilliseconds(realm.getSsoSessionIdleTimeoutRememberMe())
                    : TimeAdapter.fromSecondsToMilliseconds(realm.getSsoSessionIdleTimeout()));

            long clientSessionIdleTimeout = TimeAdapter.fromSecondsToMilliseconds(realm.getClientSessionIdleTimeout());

            if (clientSessionIdleTimeout > 0) {
                long clientSessionIdleExpiration = lastSessionRefreshMillis + clientSessionIdleTimeout;
                expiration = Math.min(expiration, clientSessionIdleExpiration);
            }

            entity.setExpiration(Math.min(expiration, sessionExpires));
        }
    }
}
