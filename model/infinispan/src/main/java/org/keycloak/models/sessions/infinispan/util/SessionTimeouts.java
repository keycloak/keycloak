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
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
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
        long lifespan = SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(false, userSessionEntity.isRememberMe(),
                TimeUnit.SECONDS.toMillis(userSessionEntity.getStarted()), realm);
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
        long idle = SessionExpirationUtils.calculateUserSessionIdleTimestamp(false, userSessionEntity.isRememberMe(),
                TimeUnit.SECONDS.toMillis(userSessionEntity.getLastSessionRefresh()), realm);
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
        long lifespan = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(false, clientSessionEntity.isUserSessionRememberMe(),
                TimeUnit.SECONDS.toMillis(clientSessionEntity.getStarted()), TimeUnit.SECONDS.toMillis(clientSessionEntity.getUserSessionStarted()),
                realm, client);
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
        long idle = SessionExpirationUtils.calculateClientSessionIdleTimestamp(false, clientSessionEntity.isUserSessionRememberMe(),
                TimeUnit.SECONDS.toMillis(clientSessionEntity.getTimestamp()), realm, client);
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
        long lifespan = SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(true, userSessionEntity.isRememberMe(),
                TimeUnit.SECONDS.toMillis(userSessionEntity.getStarted()), realm);
        if (lifespan == -1L) {
            return lifespan;
        }
        lifespan = lifespan - Time.currentTimeMillis();
        if (lifespan <= 0) {
            return ENTRY_EXPIRED_FLAG;
        }
        return lifespan;
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
        long idle = SessionExpirationUtils.calculateUserSessionIdleTimestamp(true, userSessionEntity.isRememberMe(),
                TimeUnit.SECONDS.toMillis(userSessionEntity.getLastSessionRefresh()), realm);
        idle = idle - Time.currentTimeMillis();
        if (idle <= 0) {
            return ENTRY_EXPIRED_FLAG;
        }
        return idle;
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
        long lifespan = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(true, authenticatedClientSessionEntity.isUserSessionRememberMe(),
                TimeUnit.SECONDS.toMillis(authenticatedClientSessionEntity.getStarted()), TimeUnit.SECONDS.toMillis(authenticatedClientSessionEntity.getUserSessionStarted()),
                realm, client);
        if (lifespan == -1L) {
            return lifespan;
        }
        lifespan = lifespan - Time.currentTimeMillis();
        if (lifespan <= 0) {
            return ENTRY_EXPIRED_FLAG;
        }
        return lifespan;
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
        long idle = SessionExpirationUtils.calculateClientSessionIdleTimestamp(true, authenticatedClientSessionEntity.isUserSessionRememberMe(),
                TimeUnit.SECONDS.toMillis(authenticatedClientSessionEntity.getTimestamp()), realm, client);
        idle = idle - Time.currentTimeMillis();
        if (idle <= 0) {
            return ENTRY_EXPIRED_FLAG;
        }
        return idle;
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
}
