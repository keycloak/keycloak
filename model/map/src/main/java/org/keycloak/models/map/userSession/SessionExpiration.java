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
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.map.common.TimeAdapter;
import org.keycloak.models.utils.SessionExpirationUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;

/**
 * @author <a href="mailto:mkanis@redhat.com">Martin Kanis</a>
 */
public class SessionExpiration {

    private static long getTimestampNote(MapAuthenticatedClientSessionEntity entity, String name) {
        String value = entity.getNote(name);
        if (value == null) {
            // return timestamp if not found
            return entity.getTimestamp();
        }
        return TimeAdapter.fromSecondsToMilliseconds(Integer.parseInt(value));
    }

    public static void setClientSessionExpiration(MapAuthenticatedClientSessionEntity entity, RealmModel realm, ClientModel client) {
        long timestampMillis = entity.getTimestamp() != null ? entity.getTimestamp() : 0L;
        long clientSessionStartedAtMillis = getTimestampNote(entity, AuthenticatedClientSessionModel.STARTED_AT_NOTE);
        long userSessionStartedAtMillis = getTimestampNote(entity, AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE);
        boolean isRememberMe = Boolean.parseBoolean(entity.getNote(AuthenticatedClientSessionModel.USER_SESSION_REMEMBER_ME_NOTE));
        boolean isOffline = Boolean.TRUE.equals(entity.isOffline());

        long expiresbyLifespan = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(isOffline, isRememberMe,
                clientSessionStartedAtMillis, userSessionStartedAtMillis, realm, client);
        long expiresByIdle =SessionExpirationUtils.calculateClientSessionIdleTimestamp(isOffline, isRememberMe, timestampMillis, realm, client);

        if (expiresbyLifespan > 0) {
            entity.setExpiration(Math.min(expiresbyLifespan, expiresByIdle));
        } else {
            entity.setExpiration(expiresByIdle);
        }
    }

    public static void setUserSessionExpiration(MapUserSessionEntity entity, RealmModel realm) {
        long timestampMillis = entity.getTimestamp() != null ? entity.getTimestamp() : 0L;
        long lastSessionRefreshMillis = entity.getLastSessionRefresh() != null ? entity.getLastSessionRefresh() : 0L;
        boolean isRememberMe = Boolean.TRUE.equals(entity.isRememberMe());
        boolean isOffline = Boolean.TRUE.equals(entity.isOffline());

        long expiresByLifespan = SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(isOffline, isRememberMe, timestampMillis, realm);
        long expiresByIdle = SessionExpirationUtils.calculateUserSessionIdleTimestamp(isOffline, isRememberMe, lastSessionRefreshMillis, realm);

        if (expiresByLifespan > 0) {
            entity.setExpiration(Math.min(expiresByLifespan, expiresByIdle));
        } else {
            entity.setExpiration(expiresByIdle);
        }
    }
}
