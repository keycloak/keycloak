/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.utils;

import java.util.concurrent.TimeUnit;

import org.keycloak.models.RealmModel;

/**
 * A record with the {@link RealmModel} expiration settings.
 *
 * @param lifespan           The regular user session lifespan in seconds.
 * @param maxIdle            The regular user session max-idle in seconds.
 * @param offlineLifespan    The offline user session lifespan in seconds.
 * @param offlineMaxIdle     The offline user session max-idle in seconds.
 * @param rememberMeLifespan The regular user session, in seconds, when remember me is enabled for the session.
 * @param rememberMeMaxIdle  the regular user session, in seconds, when remember me is enabled for the session.
 */
public record RealmExpiration(int lifespan,
                              int maxIdle,
                              int offlineLifespan,
                              int offlineMaxIdle,
                              int rememberMeLifespan,
                              int rememberMeMaxIdle) {

    /**
     * Returns the lifespan for a regular session.
     *
     * @param rememberMe If the session has remember_me enabled.
     * @return The computed lifespan for a regular session, in seconds.
     */
    public int getLifespan(boolean rememberMe) {
        return rememberMe ? rememberMeLifespan : lifespan;
    }

    /**
     * Returns the max-idle for a regular session.
     *
     * @param rememberMe If the session has remember_me enabled.
     * @return The computed max-idle for a regular session, in seconds.
     */
    public int getMaxIdle(boolean rememberMe) {
        return rememberMe ? rememberMeMaxIdle : maxIdle;
    }

    /**
     * Computes the time, in milliseconds, in which the offline session is expired via max lifetime.
     *
     * @param created The timestamp, in milliseconds, when the session was created.
     * @return The timestamp, in milliseconds, since when this session is not longer valid.
     */
    public long calculateOfflineLifespanTimestamp(long created) {
        return offlineLifespan == -1 ? -1 : created + TimeUnit.SECONDS.toMillis(offlineLifespan);
    }

    /**
     * Computes the time, in milliseconds, in which the regular session is expired via max lifetime.
     *
     * @param created    The timestamp, in milliseconds, when the session was created.
     * @param rememberMe Set to {@code true} if the session has remember me enabled.
     * @return The timestamp, in milliseconds, since when this session is not longer valid.
     */
    public long calculateRegularLifespanTimestamp(long created, boolean rememberMe) {
        return created + TimeUnit.SECONDS.toMillis(getLifespan(rememberMe));
    }

    /**
     * Computes the time, in milliseconds, in which the offline session is expired via max idle.
     *
     * @param lastRefresh timestamp when the session was created
     * @return The timestamp, in milliseconds, since when this session is not long valid.
     */
    public long calculateOfflineMaxIdleTimestamp(long lastRefresh) {
        return lastRefresh + TimeUnit.SECONDS.toMillis(offlineMaxIdle);
    }

    /**
     * Computes the time, in milliseconds, in which the offline session is expired via max idle.
     *
     * @param lastRefresh timestamp when the session was created
     * @param rememberMe  Set to {@code true} if the session has remember me enabled.
     * @return The timestamp, in milliseconds, since when this session is not long valid.
     */
    public long calculateRegularMaxIdleTimestamp(long lastRefresh, boolean rememberMe) {
        return lastRefresh + TimeUnit.SECONDS.toMillis(getMaxIdle(rememberMe));
    }

    /**
     * Creates a new {@link RealmExpiration} instance from the {@link RealmModel} instance.
     *
     * @param realm The {@link RealmModel} instance to get the expiration settings.
     * @return A new {@link RealmExpiration}.
     */
    public static RealmExpiration fromRealm(RealmModel realm) {
        int offlineMaxIdle = SessionExpirationUtils.getOfflineSessionIdleTimeout(realm);
        int offlineLifespan = realm.isOfflineSessionMaxLifespanEnabled() ? SessionExpirationUtils.getOfflineSessionMaxLifespan(realm) : -1;
        int maxIdle = SessionExpirationUtils.getSsoSessionIdleTimeout(realm);
        int lifespan = SessionExpirationUtils.getSsoSessionMaxLifespan(realm);
        int maxIdleRememberMe = Math.max(maxIdle, realm.getSsoSessionIdleTimeoutRememberMe());
        int lifespanRememberMe = Math.max(lifespan, realm.getSsoSessionMaxLifespanRememberMe());
        return new RealmExpiration(lifespan, maxIdle, offlineLifespan, offlineMaxIdle, lifespanRememberMe, maxIdleRememberMe);
    }

}
