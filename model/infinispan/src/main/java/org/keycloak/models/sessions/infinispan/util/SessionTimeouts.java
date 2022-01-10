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

import org.keycloak.common.util.Time;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.LoginFailureEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.SessionTimeoutHelper;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionTimeouts {

    /**
     * This indicates that entry is already expired and should be removed from the cache
     */
    public static final long ENTRY_EXPIRED_FLAG = -2l;

    /**
     * This is used just if timeouts are not set on the realm (usually happens just during tests when realm is created manually with the model API)
     */
    public static final int MINIMAL_EXPIRATION_SEC = 300;

    /**
     * Get the maximum lifespan, which this userSession can remain in the infinispan cache.
     * Returned value will be used as "lifespan" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param userSessionEntity
     * @return
     */
    public static long getUserSessionLifespanMs(RealmModel realm, UserSessionEntity userSessionEntity) {
        int timeSinceSessionStart = Time.currentTime() - userSessionEntity.getStarted();

        int sessionMaxLifespan = Math.max(realm.getSsoSessionMaxLifespan(), MINIMAL_EXPIRATION_SEC);
        if (userSessionEntity.isRememberMe()) {
            sessionMaxLifespan = Math.max(realm.getSsoSessionMaxLifespanRememberMe(), sessionMaxLifespan);
        }

        long timeToExpire = sessionMaxLifespan - timeSinceSessionStart;

        // Indication that entry should be expired
        if (timeToExpire <=0) {
            return ENTRY_EXPIRED_FLAG;
        }

        return Time.toMillis(timeToExpire);
    }


    /**
     * Get the maximum idle time for this userSession.
     * Returned value will be used when as "maxIdleTime" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param userSessionEntity
     * @return
     */
    public static long getUserSessionMaxIdleMs(RealmModel realm, UserSessionEntity userSessionEntity) {
        int timeSinceLastRefresh = Time.currentTime() - userSessionEntity.getLastSessionRefresh();

        int sessionIdleMs = Math.max(realm.getSsoSessionIdleTimeout(), MINIMAL_EXPIRATION_SEC);
        if (userSessionEntity.isRememberMe()) {
            sessionIdleMs = Math.max(realm.getSsoSessionIdleTimeoutRememberMe(), sessionIdleMs);
        }

        long maxIdleTime = sessionIdleMs - timeSinceLastRefresh + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;

        // Indication that entry should be expired
        if (maxIdleTime <=0) {
            return ENTRY_EXPIRED_FLAG;
        }

        return Time.toMillis(maxIdleTime);
    }


    /**
     * Get the maximum lifespan, which this clientSession can remain in the infinispan cache.
     * Returned value will be used as "lifespan" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param clientSessionEntity
     * @return
     */
    public static long getClientSessionLifespanMs(RealmModel realm, AuthenticatedClientSessionEntity clientSessionEntity) {
        int timeSinceTimestampUpdate = Time.currentTime() - clientSessionEntity.getTimestamp();

        int sessionMaxLifespan = Math.max(realm.getSsoSessionMaxLifespan(), realm.getSsoSessionMaxLifespanRememberMe());

        // clientSession max lifespan has preference if set
        if (realm.getClientSessionMaxLifespan() > 0) {
            sessionMaxLifespan = realm.getClientSessionMaxLifespan();
        }

        sessionMaxLifespan = Math.max(sessionMaxLifespan, MINIMAL_EXPIRATION_SEC);

        long timeToExpire = sessionMaxLifespan - timeSinceTimestampUpdate;

        // Indication that entry should be expired
        if (timeToExpire <=0) {
            return ENTRY_EXPIRED_FLAG;
        }

        return Time.toMillis(timeToExpire);
    }


    /**
     * Get the maxIdle, which this clientSession will use.
     * Returned value will be used as "maxIdle" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param clientSessionEntity
     * @return
     */
    public static long getClientSessionMaxIdleMs(RealmModel realm, AuthenticatedClientSessionEntity clientSessionEntity) {
        int timeSinceTimestampUpdate = Time.currentTime() - clientSessionEntity.getTimestamp();

        int sessionIdleTimeout = Math.max(realm.getSsoSessionIdleTimeout(), realm.getSsoSessionIdleTimeoutRememberMe());

        // clientSession idle timeout has preference if set
        if (realm.getClientSessionIdleTimeout() > 0) {
            sessionIdleTimeout = realm.getClientSessionIdleTimeout();
        }

        sessionIdleTimeout = Math.max(sessionIdleTimeout, MINIMAL_EXPIRATION_SEC);

        long timeToExpire = sessionIdleTimeout - timeSinceTimestampUpdate + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;

        // Indication that entry should be expired
        if (timeToExpire <=0) {
            return ENTRY_EXPIRED_FLAG;
        }

        return Time.toMillis(timeToExpire);
    }


    /**
     * Get the maximum lifespan, which this offline userSession can remain in the infinispan cache.
     * Returned value will be used as "lifespan" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param userSessionEntity
     * @return
     */
    public static long getOfflineSessionLifespanMs(RealmModel realm, UserSessionEntity userSessionEntity) {
        // By default, this is disabled, so offlineSessions have just "maxIdle"
        if (!realm.isOfflineSessionMaxLifespanEnabled()) return -1l;

        int timeSinceSessionStart = Time.currentTime() - userSessionEntity.getStarted();

        int sessionMaxLifespan = Math.max(realm.getOfflineSessionMaxLifespan(), MINIMAL_EXPIRATION_SEC);

        long timeToExpire = sessionMaxLifespan - timeSinceSessionStart;

        // Indication that entry should be expired
        if (timeToExpire <=0) {
            return ENTRY_EXPIRED_FLAG;
        }

        return Time.toMillis(timeToExpire);
    }


    /**
     * Get the maximum idle time for this offline userSession.
     * Returned value will be used when as "maxIdleTime" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param userSessionEntity
     * @return
     */
    public static long getOfflineSessionMaxIdleMs(RealmModel realm, UserSessionEntity userSessionEntity) {
        int timeSinceLastRefresh = Time.currentTime() - userSessionEntity.getLastSessionRefresh();

        int sessionIdle = Math.max(realm.getOfflineSessionIdleTimeout(), MINIMAL_EXPIRATION_SEC);

        long maxIdleTime = sessionIdle - timeSinceLastRefresh + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;

        // Indication that entry should be expired
        if (maxIdleTime <=0) {
            return ENTRY_EXPIRED_FLAG;
        }

        return Time.toMillis(maxIdleTime);
    }

    /**
     * Get the maximum lifespan, which this offline clientSession can remain in the infinispan cache.
     * Returned value will be used as "lifespan" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param authenticatedClientSessionEntity
     * @return
     */
    public static long getOfflineClientSessionLifespanMs(RealmModel realm, AuthenticatedClientSessionEntity authenticatedClientSessionEntity) {
        // By default, this is disabled, so offlineSessions have just "maxIdle"
        if (!realm.isOfflineSessionMaxLifespanEnabled() && realm.getClientOfflineSessionMaxLifespan() <= 0) return -1l;

        int timeSinceTimestamp = Time.currentTime() - authenticatedClientSessionEntity.getTimestamp();

        int sessionMaxLifespan = Math.max(realm.getOfflineSessionMaxLifespan(), MINIMAL_EXPIRATION_SEC);

        // clientSession max lifespan has preference if set
        if (realm.getClientOfflineSessionMaxLifespan() > 0) {
            sessionMaxLifespan = realm.getClientOfflineSessionMaxLifespan();
        }

        long timeToExpire = sessionMaxLifespan - timeSinceTimestamp;

        // Indication that entry should be expired
        if (timeToExpire <=0) {
            return ENTRY_EXPIRED_FLAG;
        }

        return Time.toMillis(timeToExpire);
    }

    /**
     * Get the maxIdle, which this offline clientSession will use.
     * Returned value will be used as "maxIdle" when calling put/replace operation in the infinispan cache for this entity
     *
     * @param realm
     * @param authenticatedClientSessionEntity
     * @return
     */
    public static long getOfflineClientSessionMaxIdleMs(RealmModel realm, AuthenticatedClientSessionEntity authenticatedClientSessionEntity) {
        int timeSinceLastRefresh = Time.currentTime() - authenticatedClientSessionEntity.getTimestamp();

        int sessionIdle = Math.max(realm.getOfflineSessionIdleTimeout(), MINIMAL_EXPIRATION_SEC);

        // clientSession idle timeout has preference if set
        if (realm.getClientOfflineSessionIdleTimeout() > 0) {
            sessionIdle = realm.getClientOfflineSessionIdleTimeout();
        }

        long maxIdleTime = sessionIdle - timeSinceLastRefresh + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS;

        // Indication that entry should be expired
        if (maxIdleTime <=0) {
            return ENTRY_EXPIRED_FLAG;
        }

        return Time.toMillis(maxIdleTime);
    }


    /**
     * Not using lifespan for detached login failure  (backwards compatibility with the background cleaner threads, which were used for cleanup of detached login failures)
     *
     * @param realm
     * @param loginFailureEntity
     * @return
     */
    public static long getLoginFailuresLifespanMs(RealmModel realm, LoginFailureEntity loginFailureEntity) {
        return -1l;
    }


    /**
     * Not using maxIdle for detached login failure  (backwards compatibility with the background cleaner threads, which were used for cleanup of detached login failures)
     *
     * @param realm
     * @param loginFailureEntity
     * @return
     */
    public static long getLoginFailuresMaxIdleMs(RealmModel realm, LoginFailureEntity loginFailureEntity) {
        return -1l;
    }


}
