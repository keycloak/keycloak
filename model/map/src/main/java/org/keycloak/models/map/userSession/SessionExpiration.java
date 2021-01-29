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
import org.keycloak.protocol.oidc.OIDCConfigAttributes;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class SessionExpiration {

    public static void setClientSessionExpiration(MapAuthenticatedClientSessionEntity entity, RealmModel realm, ClientModel client) {
        if (entity.isOffline()) {
            long sessionExpires = entity.getTimestamp() + realm.getOfflineSessionIdleTimeout();
            if (realm.isOfflineSessionMaxLifespanEnabled()) {
                sessionExpires = entity.getTimestamp() + realm.getOfflineSessionMaxLifespan();

                long clientOfflineSessionMaxLifespan;
                String clientOfflineSessionMaxLifespanPerClient = client.getAttribute(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN);
                if (clientOfflineSessionMaxLifespanPerClient != null && !clientOfflineSessionMaxLifespanPerClient.trim().isEmpty()) {
                    clientOfflineSessionMaxLifespan = Long.parseLong(clientOfflineSessionMaxLifespanPerClient);
                } else {
                    clientOfflineSessionMaxLifespan = realm.getClientOfflineSessionMaxLifespan();
                }

                if (clientOfflineSessionMaxLifespan > 0) {
                    long clientOfflineSessionMaxExpiration = entity.getTimestamp() + clientOfflineSessionMaxLifespan;
                    sessionExpires = Math.min(sessionExpires, clientOfflineSessionMaxExpiration);
                }
            }

            long expiration = entity.getTimestamp() + realm.getOfflineSessionIdleTimeout();

            long clientOfflineSessionIdleTimeout;
            String clientOfflineSessionIdleTimeoutPerClient = client.getAttribute(OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT);
            if (clientOfflineSessionIdleTimeoutPerClient != null && !clientOfflineSessionIdleTimeoutPerClient.trim().isEmpty()) {
                clientOfflineSessionIdleTimeout = Long.parseLong(clientOfflineSessionIdleTimeoutPerClient);
            } else {
                clientOfflineSessionIdleTimeout = realm.getClientOfflineSessionIdleTimeout();
            }

            if (clientOfflineSessionIdleTimeout > 0) {
                long clientOfflineSessionIdleExpiration = entity.getTimestamp() + clientOfflineSessionIdleTimeout;
                expiration = Math.min(expiration, clientOfflineSessionIdleExpiration);
            }

            entity.setExpiration(Math.min(expiration, sessionExpires));
        } else {
            long sessionExpires = (long) entity.getTimestamp() + (realm.getSsoSessionMaxLifespanRememberMe() > 0
                    ? realm.getSsoSessionMaxLifespanRememberMe() : realm.getSsoSessionMaxLifespan());

            long clientSessionMaxLifespan;
            String clientSessionMaxLifespanPerClient = client.getAttribute(OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN);
            if (clientSessionMaxLifespanPerClient != null && !clientSessionMaxLifespanPerClient.trim().isEmpty()) {
                clientSessionMaxLifespan = Long.parseLong(clientSessionMaxLifespanPerClient);
            } else {
                clientSessionMaxLifespan = realm.getClientSessionMaxLifespan();
            }

            if (clientSessionMaxLifespan > 0) {
                long clientSessionMaxExpiration = entity.getTimestamp() + clientSessionMaxLifespan;
                sessionExpires = Math.min(sessionExpires, clientSessionMaxExpiration);
            }

            long expiration = (long) entity.getTimestamp() + (realm.getSsoSessionIdleTimeoutRememberMe() > 0
                    ? realm.getSsoSessionIdleTimeoutRememberMe() : realm.getSsoSessionIdleTimeout());

            long clientSessionIdleTimeout;
            String clientSessionIdleTimeoutPerClient = client.getAttribute(OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT);
            if (clientSessionIdleTimeoutPerClient != null && !clientSessionIdleTimeoutPerClient.trim().isEmpty()) {
                clientSessionIdleTimeout = Long.parseLong(clientSessionIdleTimeoutPerClient);
            } else {
                clientSessionIdleTimeout = realm.getClientSessionIdleTimeout();
            }

            if (clientSessionIdleTimeout > 0) {
                long clientSessionIdleExpiration = entity.getTimestamp() + clientSessionIdleTimeout;
                expiration = Math.min(expiration, clientSessionIdleExpiration);
            }

            entity.setExpiration(Math.min(expiration, sessionExpires));
        }
    }

    public static void setUserSessionExpiration(MapUserSessionEntity entity, RealmModel realm) {
        if (entity.isOffline()) {
            long sessionExpires = entity.getLastSessionRefresh() + realm.getOfflineSessionIdleTimeout();
            if (realm.isOfflineSessionMaxLifespanEnabled()) {
                sessionExpires = entity.getStarted() + realm.getOfflineSessionMaxLifespan();

                long clientOfflineSessionMaxLifespan = realm.getClientOfflineSessionMaxLifespan();

                if (clientOfflineSessionMaxLifespan > 0) {
                    long clientOfflineSessionMaxExpiration = entity.getStarted() + clientOfflineSessionMaxLifespan;
                    sessionExpires = Math.min(sessionExpires, clientOfflineSessionMaxExpiration);
                }
            }

            long expiration = entity.getLastSessionRefresh() + realm.getOfflineSessionIdleTimeout();

            long clientOfflineSessionIdleTimeout = realm.getClientOfflineSessionIdleTimeout();

            if (clientOfflineSessionIdleTimeout > 0) {
                long clientOfflineSessionIdleExpiration = Time.currentTime() + clientOfflineSessionIdleTimeout;
                expiration = Math.min(expiration, clientOfflineSessionIdleExpiration);
            }

            entity.setExpiration(Math.min(expiration, sessionExpires));
        } else {
            long sessionExpires = (long) entity.getStarted()
                    + (entity.isRememberMe() && realm.getSsoSessionMaxLifespanRememberMe() > 0
                    ? realm.getSsoSessionMaxLifespanRememberMe()
                    : realm.getSsoSessionMaxLifespan());

            long clientSessionMaxLifespan = realm.getClientSessionMaxLifespan();

            if (clientSessionMaxLifespan > 0) {
                long clientSessionMaxExpiration = entity.getStarted() + clientSessionMaxLifespan;
                sessionExpires = Math.min(sessionExpires, clientSessionMaxExpiration);
            }

            long expiration = (long) entity.getLastSessionRefresh() + (entity.isRememberMe() && realm.getSsoSessionIdleTimeoutRememberMe() > 0
                    ? realm.getSsoSessionIdleTimeoutRememberMe()
                    : realm.getSsoSessionIdleTimeout());

            long clientSessionIdleTimeout = realm.getClientSessionIdleTimeout();

            if (clientSessionIdleTimeout > 0) {
                long clientSessionIdleExpiration = entity.getLastSessionRefresh() + clientSessionIdleTimeout;
                expiration = Math.min(expiration, clientSessionIdleExpiration);
            }

            entity.setExpiration(Math.min(expiration, sessionExpires));
        }
    }
}
