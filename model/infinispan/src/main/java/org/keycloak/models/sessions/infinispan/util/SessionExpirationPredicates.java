package org.keycloak.models.sessions.infinispan.util;

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
 * @param realm       The {@link RealmModel} to fetch the max-idle and lifespan settings.
 * @param offline     Indicates whether the sessions are offline.
 * @param currentTime The current time value.
 */
public record SessionExpirationPredicates(RealmModel realm, boolean offline, int currentTime) {

    public boolean isUserSessionExpired(UserSessionModel model) {
        return isUserSessionExpired(model.isRememberMe(), model.getStarted(), model.getLastSessionRefresh());
    }

    public boolean isUserSessionExpired(UserSessionEntity entity) {
        return isUserSessionExpired(entity.isRememberMe(), entity.getStarted(), entity.getLastSessionRefresh());
    }

    public boolean isUserSessionExpired(RemoteUserSessionEntity entity) {
        return isUserSessionExpired(entity.isRememberMe(), entity.getStarted(), entity.getLastSessionRefresh());
    }

    public boolean isClientSessionExpired(AuthenticatedClientSessionModel model) {
        return isClientSessionExpired(model.getUserSession().isRememberMe(), model.getStarted(), model.getUserSessionStarted(), model.getTimestamp(), model.getClient());
    }

    public boolean isClientSessionExpired(AuthenticatedClientSessionEntity entity, boolean rememberMe, ClientModel client) {
        return isClientSessionExpired(rememberMe, entity.getStarted(), entity.getUserSessionStarted(), entity.getTimestamp(), client);
    }

    public boolean isClientSessionExpired(RemoteAuthenticatedClientSessionEntity entity, int userSessionStarted, boolean rememberMe, ClientModel client) {
        return isClientSessionExpired(rememberMe, entity.getStarted(), userSessionStarted, entity.getTimestamp(), client);
    }

    private boolean isUserSessionExpired(boolean rememberMe, long started, long lastRefresh) {
        var lifespan = SessionExpirationUtils.calculateUserSessionMaxLifespanTimestamp(offline, rememberMe, started, realm);
        var maxIdle = SessionExpirationUtils.calculateUserSessionIdleTimestamp(offline, rememberMe, lastRefresh, realm);
        return isExpired(lifespan, maxIdle);
    }

    private boolean isClientSessionExpired(boolean rememberMe, long started, long userSessionStarted, long lastRefresh, ClientModel client) {
        var lifespan = SessionExpirationUtils.calculateClientSessionMaxLifespanTimestamp(offline, rememberMe, started, userSessionStarted, realm, client);
        var maxIdle = SessionExpirationUtils.calculateClientSessionIdleTimestamp(offline, rememberMe, lastRefresh, realm, client);
        return isExpired(lifespan, maxIdle);
    }

    private boolean isExpired(long lifespanTimestamp, long maxIdleTimestamp) {
        var maxIdleExpired = maxIdleTimestamp - currentTime <= 0;
        return lifespanTimestamp == -1 ?
                maxIdleExpired :
                maxIdleExpired || lifespanTimestamp - currentTime <= 0;
    }
}
