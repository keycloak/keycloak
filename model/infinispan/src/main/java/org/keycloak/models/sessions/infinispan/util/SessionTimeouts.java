/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.models.sessions.infinispan.util;

import java.util.concurrent.TimeUnit;

import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.RootAuthenticationSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.models.utils.SessionExpirationUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionTimeouts {

    /**
     * This indicates that entry is already expired and should be removed from the cache
     */
    public static final long ENTRY_EXPIRED_FLAG = -2;

    private static final long IMMORTAL_FLAG = -1;

    /**
     * Get the maximum lifespan, which this userSession can remain in the infinispan cache.
     * Returned value will be used as "lifespan" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param client
     * @param userSessionEntity
     * @return
     */
    public static long getUserSessionLifespanMs(RealmModel realm, ClientModel client, UserSessionEntity userSessionEntity) {
        return getUserSessionLifespanMs(realm, false, userSessionEntity.isRememberMe(), userSessionEntity.getStarted());
    }

    public static long getUserSessionLifespanMs(RealmModel realm, boolean offline, boolean rememberMe, int started) {
        long lifespan = SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(offline, rememberMe,
                TimeUnit.SECONDS.toMillis(started), realm);
        if (offline && lifespan == IMMORTAL_FLAG) {
            return IMMORTAL_FLAG;
        }
        lifespan = lifespan - Time.currentTimeMillis();
        if (lifespan <= 0) {
            return ENTRY_EXPIRED_FLAG;
        }
        return lifespan;
    }

    /**
     * Get the maximum idle time for this userSession.
     * Returned value will be used when as "maxIdleTime" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param client
     * @param userSessionEntity
     * @return
     */
    public static long getUserSessionMaxIdleMs(RealmModel realm, ClientModel client, UserSessionEntity userSessionEntity) {
        return getUserSessionMaxIdleMs(realm, false, userSessionEntity.isRememberMe(), userSessionEntity.getLastSessionRefresh());
    }

    public static long getUserSessionMaxIdleMs(RealmModel realm, boolean offline, boolean rememberMe, int lastSessionRefresh) {
        long idle = SessionExpirationUtils.calculateUserSessionIdleTimestamp(offline, rememberMe, TimeUnit.SECONDS.toMillis(lastSessionRefresh), realm);
        idle = idle - Time.currentTimeMillis();
        if (idle <= 0) {
            return ENTRY_EXPIRED_FLAG;
        }
        return idle;
    }


    /**
     * Get the maximum lifespan, which this clientSession can remain in the infinispan cache.
     * Returned value will be used as "lifespan" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param client
     * @param clientSessionEntity
     * @return
     */
    public static long getClientSessionLifespanMs(RealmModel realm, ClientModel client, AuthenticatedClientSessionEntity clientSessionEntity) {
        return getClientSessionLifespanMs(realm, client, false, clientSessionEntity.isUserSessionRememberMe(), clientSessionEntity.getStarted(), clientSessionEntity.getUserSessionStarted());
    }

    public static long getClientSessionLifespanMs(RealmModel realm, ClientModel client, boolean offline, boolean isUserSessionRememberMe, int started, int userSessionStarted) {
        long lifespan = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(offline, isUserSessionRememberMe,
                TimeUnit.SECONDS.toMillis(started), TimeUnit.SECONDS.toMillis(userSessionStarted), realm, client);
        if (offline && lifespan == IMMORTAL_FLAG) {
            return IMMORTAL_FLAG;
        }
        lifespan = lifespan - Time.currentTimeMillis();
        if (lifespan <= 0) {
            return ENTRY_EXPIRED_FLAG;
        }
        return lifespan;
    }


    /**
     * Get the maxIdle, which this clientSession will use.
     * Returned value will be used as "maxIdle" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param client
     * @param clientSessionEntity
     * @return
     */
    public static long getClientSessionMaxIdleMs(RealmModel realm, ClientModel client, AuthenticatedClientSessionEntity clientSessionEntity) {
        return getClientSessionMaxIdleMs(realm, client, false, clientSessionEntity.isUserSessionRememberMe(), clientSessionEntity.getTimestamp());
    }

    public static long getClientSessionMaxIdleMs(RealmModel realm, ClientModel client, boolean offline, boolean isUserSessionRememberMe, int timestamp) {
        long idle = SessionExpirationUtils.calculateClientSessionIdleTimestamp(offline, isUserSessionRememberMe,
                TimeUnit.SECONDS.toMillis(timestamp), realm, client);
        idle = idle - Time.currentTimeMillis();
        if (idle <= 0) {
            return ENTRY_EXPIRED_FLAG;
        }
        return idle;
    }


    /**
     * Get the maximum lifespan, which this offline userSession can remain in the infinispan cache.
     * Returned value will be used as "lifespan" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param client
     * @param userSessionEntity
     * @return
     */
    public static long getOfflineSessionLifespanMs(RealmModel realm, ClientModel client, UserSessionEntity userSessionEntity) {
        return getUserSessionLifespanMs(realm, true, userSessionEntity.isRememberMe(), userSessionEntity.getStarted());
    }


    /**
     * Get the maximum idle time for this offline userSession.
     * Returned value will be used when as "maxIdleTime" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param client
     * @param userSessionEntity
     * @return
     */
    public static long getOfflineSessionMaxIdleMs(RealmModel realm, ClientModel client, UserSessionEntity userSessionEntity) {
        return getUserSessionMaxIdleMs(realm, true, userSessionEntity.isRememberMe(), userSessionEntity.getLastSessionRefresh());
    }

    /**
     * Get the maximum lifespan, which this offline clientSession can remain in the infinispan cache.
     * Returned value will be used as "lifespan" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param client
     * @param authenticatedClientSessionEntity
     * @return
     */
    public static long getOfflineClientSessionLifespanMs(RealmModel realm, ClientModel client, AuthenticatedClientSessionEntity authenticatedClientSessionEntity) {
        return getClientSessionLifespanMs(realm, client, true, authenticatedClientSessionEntity.isUserSessionRememberMe(), authenticatedClientSessionEntity.getStarted(), authenticatedClientSessionEntity.getUserSessionStarted());
    }

    /**
     * Get the maxIdle, which this offline clientSession will use.
     * Returned value will be used as "maxIdle" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param client
     * @param authenticatedClientSessionEntity
     * @return
     */
    public static long getOfflineClientSessionMaxIdleMs(RealmModel realm, ClientModel client, AuthenticatedClientSessionEntity authenticatedClientSessionEntity) {
        return getClientSessionMaxIdleMs(realm, client, true, authenticatedClientSessionEntity.isUserSessionRememberMe(), authenticatedClientSessionEntity.getTimestamp());
    }


    /**
     * Not using lifespan for detached login failure  (backwards compatibility with the background cleaner threads, which were used for cleanup of detached login failures)
     *
     * @param realm
     * @param client
     * @param loginFailureEntity
     * @return
     */
    public static long getLoginFailuresLifespanMs(RealmModel realm, ClientModel client, LoginFailureEntity loginFailureEntity) {
        return IMMORTAL_FLAG;
    }


    /**
     * Not using maxIdle for detached login failure  (backwards compatibility with the background cleaner threads, which were used for cleanup of detached login failures)
     *
     * @param realm
     * @param client
     * @param loginFailureEntity
     * @return
     */
    public static long getLoginFailuresMaxIdleMs(RealmModel realm, ClientModel client, LoginFailureEntity loginFailureEntity) {
        return IMMORTAL_FLAG;
    }

    public static long getAuthSessionLifespanMS(RealmModel realm, ClientModel client, RootAuthenticationSessionEntity entity) {
        return (entity.getTimestamp() - Time.currentTime() + SessionExpiration.getAuthSessionLifespan(realm)) * 1000L;
    }

    public static long getAuthSessionMaxIdleMS(RealmModel realm, ClientModel client, RootAuthenticationSessionEntity entity) {
        return IMMORTAL_FLAG;
    }
}
