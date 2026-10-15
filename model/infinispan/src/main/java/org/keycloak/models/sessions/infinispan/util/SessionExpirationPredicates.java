package org.keycloak.models.sessions.infinispan.util;

import java.util.function.LongSupplier;

import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RemoteAuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.RemoteUserSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.models.utils.SessionExpirationUtils;

/**
 * Utility record to check if a user or client session is expired. It handles all the current entities, from JPA or from
 * caching.
 *
 * @param realm       The {@link RealmModel} to fetch the max-idle and lifespan settings.
 * @param offline     Indicates whether the sessions are offline.
 * @param currentTime The current time value.
 */
public record SessionExpirationPredicates(RealmModel realm, boolean offline, int currentTime) {

    public boolean isUserSessionExpired(UserSessionModel model) {
        var lifespan = SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(model, realm);
        var maxIdle = SessionExpirationUtils.calculateUserSessionIdleTimestamp(model, realm);
        return isExpired(lifespan, maxIdle);
    }

    public boolean isUserSessionExpired(UserSessionEntity entity) {
        return isUserSessionExpired(entity.isRememberMe(), entity.getStarted(), entity.getLastSessionRefresh(),
                SessionExpirationUtils.upstreamSessionExpiry(offline, entity.getNotes()));
    }

    public boolean isUserSessionExpired(RemoteUserSessionEntity entity) {
        return isUserSessionExpired(entity.isRememberMe(), entity.getStarted(), entity.getLastSessionRefresh(),
                SessionExpirationUtils.upstreamSessionExpiry(offline, entity.getNotes()));
    }

    public boolean isClientSessionExpired(AuthenticatedClientSessionEntity entity, UserSessionModel userSession, ClientModel client) {
        return isClientSessionExpired(userSession.isRememberMe(), entity.getStarted(), entity.getUserSessionStarted(),
                entity.getTimestamp(), client, SessionExpirationUtils.upstreamSessionExpiry(offline, userSession.getNotes()));
    }

    public boolean isClientSessionExpired(RemoteAuthenticatedClientSessionEntity entity, UserSessionModel userSession, ClientModel client) {
        return isClientSessionExpired(userSession.isRememberMe(), entity.getStarted(), userSession.getStarted(),
                entity.getTimestamp(), client, SessionExpirationUtils.upstreamSessionExpiry(offline, userSession.getNotes()));
    }

    private boolean isUserSessionExpired(boolean rememberMe, long started, long lastRefresh, LongSupplier upstreamSessionExpiryMs) {
        var lifespan = SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(offline, rememberMe, started, realm, upstreamSessionExpiryMs);
        var maxIdle = SessionExpirationUtils.calculateUserSessionIdleTimestamp(offline, rememberMe, lastRefresh, realm, upstreamSessionExpiryMs);
        return isExpired(lifespan, maxIdle);
    }

    private boolean isClientSessionExpired(boolean rememberMe, long started, long userSessionStarted, long lastRefresh,
            ClientModel client, LongSupplier upstreamSessionExpiryMs) {
        var lifespan = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(offline, rememberMe, started, userSessionStarted, realm, client, upstreamSessionExpiryMs);
        var maxIdle = SessionExpirationUtils.calculateClientSessionIdleTimestamp(offline, rememberMe, lastRefresh, realm, client, upstreamSessionExpiryMs);
        return isExpired(lifespan, maxIdle);
    }

    private boolean isExpired(long lifespanTimestamp, long maxIdleTimestamp) {
        var maxIdleExpired = maxIdleTimestamp - currentTime <= 0;
        return lifespanTimestamp == -1 ?
                maxIdleExpired :
                maxIdleExpired || lifespanTimestamp - currentTime <= 0;
    }
}
