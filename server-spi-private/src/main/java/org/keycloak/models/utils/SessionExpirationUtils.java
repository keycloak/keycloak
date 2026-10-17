/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.utils.StringUtil;

import org.jboss.logging.Logger;

/**
 * <p>Shared methods to calculate the session expiration and idle.</p>
 *
 * @author rmartinc
 */
public class SessionExpirationUtils {

    private static final Logger logger = Logger.getLogger(SessionExpirationUtils.class);

    /**
     * Session note key for storing the upstream IdP's session expiry timestamp (in seconds).
     * Used by federated identity providers (e.g., SAML) to enforce the upstream session deadline.
     */
    public static final String FEDERATED_SESSION_NOT_ON_OR_AFTER = "FEDERATED_SESSION_NOT_ON_OR_AFTER";

    private static LongSupplier upstreamSessionExpiry(UserSessionModel userSession) {
        if (userSession == null || userSession.isOffline()) {
            return () -> -1L;
        }
        return () -> parseUpstreamSessionExpiry(userSession.getNote(FEDERATED_SESSION_NOT_ON_OR_AFTER));
    }

    /**
     * Creates a {@link LongSupplier} that lazily resolves the upstream (federated) session expiry
     * from a session notes map. Returns -1 if no upstream cap applies
     * (offline session, no notes, or no note present).
     */
    public static LongSupplier upstreamSessionExpiry(boolean offline, Map<String, String> notes) {
        if (offline || notes == null) {
            return () -> -1L;
        }
        return () -> parseUpstreamSessionExpiry(notes.get(FEDERATED_SESSION_NOT_ON_OR_AFTER));
    }

    /**
     * Calculates the time in which the session is expired via max lifetime
     * configuration.
     * @param offline is the session offline?
     * @param isRememberMe is the session remember me?
     * @param created timestamp when the session was created
     * @param realm The realm model
     * @param upstreamSessionExpiryMs supplier for the upstream session expiry in milliseconds, or null if not applicable
     * @return The time when the user session is expired or -1 if does not expire
     */
    public static long calculateUserSessionMaxLifespanTimestamp(boolean offline, boolean isRememberMe, long created,
            RealmModel realm, LongSupplier upstreamSessionExpiryMs) {
        RealmExpiration expiration = RealmExpiration.fromRealm(realm);
        long lifespan = offline ?
                expiration.calculateOfflineLifespanTimestamp(created) :
                expiration.calculateRegularLifespanTimestamp(created, isRememberMe);
        return capByUpstreamExpiry(lifespan, upstreamSessionExpiryMs);
    }

    /**
     * Calculates the time in which the session is expired via max lifetime configuration,
     * including any upstream (federated) session expiry cap.
     * @param userSession the user session
     * @param realm The realm model
     * @return The time when the user session is expired or -1 if does not expire
     */
    public static long calculateUserSessionMaxLifespanTimestamp(UserSessionModel userSession, RealmModel realm) {
        if (userSession == null) {
            return -1L;
        }
        return calculateUserSessionMaxLifespanTimestamp(
                userSession.isOffline(),
                userSession.isRememberMe(),
                TimeUnit.SECONDS.toMillis(userSession.getStarted()),
                realm,
                upstreamSessionExpiry(userSession));
    }

    /**
     * Calculates the time in which the user session is expired via the idle
     * configuration.
     * @param offline is the session offline?
     * @param isRememberMe is the session remember me?
     * @param lastRefreshed The last time the session was refreshed
     * @param realm The realm model
     * @param upstreamSessionExpiryMs supplier for the upstream session expiry in milliseconds, or null if not applicable
     * @return The time in which the user session is expired by idle timeout
     */
    public static long calculateUserSessionIdleTimestamp(boolean offline, boolean isRememberMe, long lastRefreshed,
            RealmModel realm, LongSupplier upstreamSessionExpiryMs) {
        RealmExpiration expiration = RealmExpiration.fromRealm(realm);
        long idle = offline ?
                expiration.calculateOfflineMaxIdleTimestamp(lastRefreshed) :
                expiration.calculateRegularMaxIdleTimestamp(lastRefreshed, isRememberMe);
        return capByUpstreamExpiry(idle, upstreamSessionExpiryMs);
    }

    /**
     * Calculates the time in which the user session is expired via the idle
     * configuration, including any upstream (federated) session expiry cap.
     * @param userSession the user session
     * @param realm The realm model
     * @return The time in which the user session is expired by idle timeout
     */
    public static long calculateUserSessionIdleTimestamp(UserSessionModel userSession, RealmModel realm) {
        if (userSession == null) {
            return -1L;
        }
        return calculateUserSessionIdleTimestamp(
                userSession.isOffline(),
                userSession.isRememberMe(),
                TimeUnit.SECONDS.toMillis(userSession.getLastSessionRefresh()),
                realm,
                upstreamSessionExpiry(userSession));
    }

    /**
     * Calculates the time in which the client session is expired via lifespan
     * configuration in the realm and client.
     * @param offline is the session offline?
     * @param isRememberMe is the session remember me?
     * @param clientSessionCreated timestamp when the client session was created
     * @param userSessionCreated timestamp when the user session was created
     * @param realm The realm model
     * @param client The client model
     * @param upstreamSessionExpiryMs supplier for the upstream session expiry in milliseconds, or null if not applicable
     * @return The time when the client session is expired or -1 if does not expire
     */
    public static long calculateClientSessionMaxLifespanTimestamp(boolean offline, boolean isRememberMe,
            long clientSessionCreated, long userSessionCreated, RealmModel realm, ClientModel client,
            LongSupplier upstreamSessionExpiryMs) {
        long timestamp = -1;
        if (offline) {
            long clientOfflineSessionMaxLifespan = getClientAttributeTimeout(client, OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_MAX_LIFESPAN);
            if (realm.isOfflineSessionMaxLifespanEnabled() || clientOfflineSessionMaxLifespan > 0) {
                if (clientOfflineSessionMaxLifespan > 0) {
                    clientOfflineSessionMaxLifespan = TimeUnit.SECONDS.toMillis(clientOfflineSessionMaxLifespan);
                } else if (realm.getClientOfflineSessionMaxLifespan() > 0) {
                    clientOfflineSessionMaxLifespan = TimeUnit.SECONDS.toMillis(realm.getClientOfflineSessionMaxLifespan());
                } else {
                    clientOfflineSessionMaxLifespan = TimeUnit.SECONDS.toMillis(getOfflineSessionMaxLifespan(realm));
                }
                timestamp = clientSessionCreated + clientOfflineSessionMaxLifespan;

                long userSessionExpires = calculateUserSessionMaxLifespanTimestamp(offline, isRememberMe,
                        userSessionCreated, realm, upstreamSessionExpiryMs);

                timestamp = userSessionExpires > 0? Math.min(timestamp, userSessionExpires) : timestamp;
            }
        } else {
            long clientSessionMaxLifespan = TimeUnit.SECONDS.toMillis(getSsoSessionMaxLifespan(realm));
            if (isRememberMe) {
                clientSessionMaxLifespan = Math.max(clientSessionMaxLifespan, TimeUnit.SECONDS.toMillis(realm.getSsoSessionMaxLifespanRememberMe()));
            }
            long clientSessionMaxLifespanPerClient = getClientAttributeTimeout(client, OIDCConfigAttributes.CLIENT_SESSION_MAX_LIFESPAN);
            if (clientSessionMaxLifespanPerClient > 0) {
                clientSessionMaxLifespan = TimeUnit.SECONDS.toMillis(clientSessionMaxLifespanPerClient);
            } else if (realm.getClientSessionMaxLifespan() > 0) {
                clientSessionMaxLifespan = TimeUnit.SECONDS.toMillis(realm.getClientSessionMaxLifespan());
            }

            timestamp = clientSessionCreated + clientSessionMaxLifespan;

            long userSessionExpires = calculateUserSessionMaxLifespanTimestamp(offline, isRememberMe,
                    userSessionCreated, realm, upstreamSessionExpiryMs);

            timestamp = Math.min(timestamp, userSessionExpires);
        }
        return timestamp;
    }

    /**
     * Calculates the time in which the user session is expired via the idle
     * configuration in the realm and client.
     * @param offline is the session offline?
     * @param isRememberMe is the session remember me?
     * @param lastRefreshed the last time the client session was refreshed
     * @param realm the realm model
     * @param client the client model
     * @param upstreamSessionExpiryMs supplier for the upstream session expiry in milliseconds, or null if not applicable
     * @return The time in which the client session is expired by idle timeout
     */
    public static long calculateClientSessionIdleTimestamp(boolean offline, boolean isRememberMe, long lastRefreshed,
            RealmModel realm, ClientModel client, LongSupplier upstreamSessionExpiryMs) {
        long timestamp;
        if (offline) {
            long clientOfflineSessionIdleTimeout = TimeUnit.SECONDS.toMillis(getOfflineSessionIdleTimeout(realm));
            long clientOfflineSessionIdleTimeoutPerClient = getClientAttributeTimeout(client, OIDCConfigAttributes.CLIENT_OFFLINE_SESSION_IDLE_TIMEOUT);
            if (clientOfflineSessionIdleTimeoutPerClient > 0) {
                clientOfflineSessionIdleTimeout = TimeUnit.SECONDS.toMillis(clientOfflineSessionIdleTimeoutPerClient);
            } else if (realm.getClientOfflineSessionIdleTimeout() > 0) {
                clientOfflineSessionIdleTimeout = TimeUnit.SECONDS.toMillis(realm.getClientOfflineSessionIdleTimeout());
            }

            timestamp = lastRefreshed + clientOfflineSessionIdleTimeout;
        } else {
            long clientSessionIdleTimeout = TimeUnit.SECONDS.toMillis(getSsoSessionIdleTimeout(realm));
            if (isRememberMe) {
                clientSessionIdleTimeout = Math.max(clientSessionIdleTimeout, TimeUnit.SECONDS.toMillis(realm.getSsoSessionIdleTimeoutRememberMe()));
            }
            long clientSessionIdleTimeoutPerClient = getClientAttributeTimeout(client, OIDCConfigAttributes.CLIENT_SESSION_IDLE_TIMEOUT);
            if (clientSessionIdleTimeoutPerClient > 0) {
                clientSessionIdleTimeout = TimeUnit.SECONDS.toMillis(clientSessionIdleTimeoutPerClient);
            } else if (realm.getClientSessionIdleTimeout() > 0){
                clientSessionIdleTimeout = TimeUnit.SECONDS.toMillis(realm.getClientSessionIdleTimeout());
            }

            timestamp = lastRefreshed + clientSessionIdleTimeout;
        }
        return capByUpstreamExpiry(timestamp, upstreamSessionExpiryMs);
    }

    /**
     * Calculates the time in which the client session is expired via lifespan
     * configuration, including any upstream (federated) session expiry cap.
     * @param userSession the user session
     * @param clientSession the client session
     * @param realm The realm model
     * @param client The client model
     * @return The time when the client session is expired or -1 if does not expire
     */
    public static long calculateClientSessionMaxLifespanTimestamp(UserSessionModel userSession,
            AuthenticatedClientSessionModel clientSession, RealmModel realm, ClientModel client) {
        if (userSession == null || clientSession == null) {
            return -1L;
        }
        return calculateClientSessionMaxLifespanTimestamp(
                userSession.isOffline(), userSession.isRememberMe(),
                TimeUnit.SECONDS.toMillis(clientSession.getStarted()),
                TimeUnit.SECONDS.toMillis(userSession.getStarted()),
                realm, client, upstreamSessionExpiry(userSession));
    }

    /**
     * Calculates the time in which the client session is expired via the idle
     * configuration, including any upstream (federated) session expiry cap.
     * @param userSession the user session
     * @param clientSession the client session
     * @param realm The realm model
     * @param client The client model
     * @return The time in which the client session is expired by idle timeout
     */
    public static long calculateClientSessionIdleTimestamp(UserSessionModel userSession,
            AuthenticatedClientSessionModel clientSession, RealmModel realm, ClientModel client) {
        if (userSession == null || clientSession == null) {
            return -1L;
        }
        return calculateClientSessionIdleTimestamp(
                userSession.isOffline(), userSession.isRememberMe(),
                TimeUnit.SECONDS.toMillis(clientSession.getTimestamp()),
                realm, client, upstreamSessionExpiry(userSession));
    }

    public static int getSsoSessionMaxLifespan(RealmModel realm) {
        int lifespan = realm.getSsoSessionMaxLifespan();
        if (lifespan <= 0) {
            lifespan = Constants.DEFAULT_SESSION_MAX_LIFESPAN;
        }
        return lifespan;
    }

    public static int getOfflineSessionMaxLifespan(RealmModel realm) {
        int lifespan = realm.getOfflineSessionMaxLifespan();
        if (lifespan <= 0) {
            lifespan = Constants.DEFAULT_OFFLINE_SESSION_MAX_LIFESPAN;
        }
        return lifespan;
    }

    public static int getSsoSessionIdleTimeout(RealmModel realm) {
        int idle = realm.getSsoSessionIdleTimeout();
        if (idle <= 0) {
            idle = Constants.DEFAULT_SESSION_IDLE_TIMEOUT;
        }
        return idle;
    }

    public static int getOfflineSessionIdleTimeout(RealmModel realm) {
        int idle = realm.getOfflineSessionIdleTimeout();
        if (idle <= 0) {
            idle = Constants.DEFAULT_OFFLINE_SESSION_IDLE_TIMEOUT;
        }
        return idle;
    }

    /**
     * Rounds {@code lastSessionRefresh} down to a coarse-grained boundary of {@code maxIdle / 2} seconds,
     * offset by {@code createdOn % granularity} so that sessions created at different times have staggered
     * boundaries and don't all become cleanup candidates at the same instant.
     */
    public static int computeLastSessionRefreshCoarse(int lastSessionRefresh, int maxIdle, int createdOn) {
        int granularity = Math.max(maxIdle / 2, 1);
        int offset = Math.floorMod(createdOn, granularity);
        int aligned = lastSessionRefresh - offset;
        return (aligned / granularity) * granularity + offset;
    }

    private static long capByUpstreamExpiry(long timestamp, LongSupplier upstreamSessionExpiryMs) {
        if (upstreamSessionExpiryMs == null) {
            return timestamp;
        }
        long upstream = upstreamSessionExpiryMs.getAsLong();
        if (upstream <= 0) {
            return timestamp;
        }
        if (timestamp == -1L) {
            return upstream;
        }
        return Math.min(timestamp, upstream);
    }

    private static long parseUpstreamSessionExpiry(String value) {
        if (value == null) {
            return -1L;
        }
        try {
            return TimeUnit.SECONDS.toMillis(Long.parseLong(value));
        } catch (NumberFormatException e) {
            logger.warnf("Invalid value for session note %s: %s", FEDERATED_SESSION_NOT_ON_OR_AFTER, value);
            return -1L;
        }
    }

    private static long getClientAttributeTimeout(ClientModel client, String attr) {
        if (client != null) {
            final String value = client.getAttribute(attr);
            if (StringUtil.isNotBlank(value)) {
                try {
                    return Long.parseLong(value);
                } catch (NumberFormatException e) {
                    // no-op
                }
            }
        }
        return -1;
    }
}
