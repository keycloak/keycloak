package org.keycloak.models.sessions.infinispan.util;

import java.util.concurrent.TimeUnit;

import org.keycloak.models.AuthenticatedClientSessionModel;
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
 * @param realm            The {@link RealmModel} to fetch the max-idle and lifespan settings.
 * @param offline          Indicates whether the sessions are offline.
 * @param currentTimeMillis The current time in milliseconds.
 */
public record SessionExpirationPredicates(RealmModel realm, boolean offline, long currentTimeMillis) {

    public boolean isUserSessionExpired(UserSessionModel model) {
        return isUserSessionExpired(model.isRememberMe(),
                TimeUnit.SECONDS.toMillis(model.getStarted()),
                TimeUnit.SECONDS.toMillis(model.getLastSessionRefresh()));
    }

    public boolean isUserSessionExpired(UserSessionEntity entity) {
        return isUserSessionExpired(entity.isRememberMe(),
                TimeUnit.SECONDS.toMillis(entity.getStarted()),
                TimeUnit.SECONDS.toMillis(entity.getLastSessionRefresh()));
    }

    public boolean isUserSessionExpired(RemoteUserSessionEntity entity) {
        return isUserSessionExpired(entity.isRememberMe(),
                TimeUnit.SECONDS.toMillis(entity.getStarted()),
                TimeUnit.SECONDS.toMillis(entity.getLastSessionRefresh()));
    }

    public boolean isClientSessionExpired(AuthenticatedClientSessionModel model) {
        return isClientSessionExpired(model.getUserSession().isRememberMe(),
                TimeUnit.SECONDS.toMillis(model.getStarted()),
                TimeUnit.SECONDS.toMillis(model.getUserSessionStarted()),
                TimeUnit.SECONDS.toMillis(model.getTimestamp()),
                model.getClient());
    }

    public boolean isClientSessionExpired(AuthenticatedClientSessionEntity entity, boolean rememberMe, ClientModel client) {
        return isClientSessionExpired(rememberMe,
                TimeUnit.SECONDS.toMillis(entity.getStarted()),
                TimeUnit.SECONDS.toMillis(entity.getUserSessionStarted()),
                TimeUnit.SECONDS.toMillis(entity.getTimestamp()),
                client);
    }

    public boolean isClientSessionExpired(RemoteAuthenticatedClientSessionEntity entity, int userSessionStarted, boolean rememberMe, ClientModel client) {
        return isClientSessionExpired(rememberMe,
                TimeUnit.SECONDS.toMillis(entity.getStarted()),
                TimeUnit.SECONDS.toMillis(userSessionStarted),
                TimeUnit.SECONDS.toMillis(entity.getTimestamp()),
                client);
    }

    private boolean isUserSessionExpired(boolean rememberMe, long startedMs, long lastRefreshMs) {
        var lifespan = SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(offline, rememberMe, startedMs, realm);
        var maxIdle = SessionExpirationUtils.calculateUserSessionIdleTimestamp(offline, rememberMe, lastRefreshMs, realm);
        return isExpired(lifespan, maxIdle);
    }

    private boolean isClientSessionExpired(boolean rememberMe, long startedMs, long userSessionStartedMs, long lastRefreshMs, ClientModel client) {
        var lifespan = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(offline, rememberMe, startedMs, userSessionStartedMs, realm, client);
        var maxIdle = SessionExpirationUtils.calculateClientSessionIdleTimestamp(offline, rememberMe, lastRefreshMs, realm, client);
        return isExpired(lifespan, maxIdle);
    }

    private boolean isExpired(long lifespanTimestamp, long maxIdleTimestamp) {
        var maxIdleExpired = maxIdleTimestamp - currentTimeMillis <= 0;
        return lifespanTimestamp == -1 ?
                maxIdleExpired :
                maxIdleExpired || lifespanTimestamp - currentTimeMillis <= 0;
    }
}
