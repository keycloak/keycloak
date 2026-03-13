/*
 * Copyright 2026 Capital One Financial Corporation and/or its affiliates
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

package org.keycloak.models.redis.session;

import org.jboss.logging.Logger;
import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.entities.RedisClientSessionEntity;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Adapter that wraps RedisClientSessionEntity to implement AuthenticatedClientSessionModel.
 */
public class RedisClientSessionAdapter implements AuthenticatedClientSessionModel {

    private static final Logger logger = Logger.getLogger(RedisClientSessionAdapter.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final ClientModel client;
    private final UserSessionModel userSession;
    private final RedisClientSessionEntity entity;
    private final RedisConnectionProvider redis;
    private final boolean offline;
    private boolean modified = false;
    private boolean detached = false; // Flag to prevent re-persisting after deletion
    private long version = 0; // Version for optimistic locking

    public RedisClientSessionAdapter(KeycloakSession session, RealmModel realm, ClientModel client,
                                      UserSessionModel userSession, RedisClientSessionEntity entity,
                                      RedisConnectionProvider redis, boolean offline, long initialVersion) {
        this.session = Objects.requireNonNull(session);
        this.realm = Objects.requireNonNull(realm);
        this.client = Objects.requireNonNull(client);
        this.userSession = Objects.requireNonNull(userSession);
        this.entity = Objects.requireNonNull(entity);
        this.redis = Objects.requireNonNull(redis);
        this.offline = offline;
        this.version = initialVersion;
    }

    @Override
    public String getId() {
        return entity.getUserSessionId() + ":" + entity.getClientId();
    }

    @Override
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        entity.setTimestamp(timestamp);
        markModified();
    }

    @Override
    public void detachFromUserSession() {
        String cacheName = offline ?
                RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME :
                RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME;
        detached = true; // Mark as detached to prevent re-persisting
        redis.delete(cacheName, entity.getKey());
    }

    @Override
    public UserSessionModel getUserSession() {
        return userSession;
    }

    @Override
    public String getCurrentRefreshToken() {
        return entity.getCurrentRefreshToken();
    }

    @Override
    public void setCurrentRefreshToken(String currentRefreshToken) {
        entity.setCurrentRefreshToken(currentRefreshToken);
        markModified();
    }

    @Override
    public int getCurrentRefreshTokenUseCount() {
        return entity.getCurrentRefreshTokenUseCount();
    }

    @Override
    public void setCurrentRefreshTokenUseCount(int currentRefreshTokenUseCount) {
        entity.setCurrentRefreshTokenUseCount(currentRefreshTokenUseCount);
        markModified();
    }

    @Override
    public String getNote(String name) {
        return entity.getNote(name);
    }

    @Override
    public void setNote(String name, String value) {
        entity.setNote(name, value);
        markModified();
    }

    @Override
    public void removeNote(String name) {
        entity.removeNote(name);
        markModified();
    }

    @Override
    public Map<String, String> getNotes() {
        Map<String, String> notes = entity.getNotes();
        return notes != null ? new HashMap<>(notes) : new HashMap<>();
    }

    @Override
    public String getRedirectUri() {
        return entity.getRedirectUri();
    }

    @Override
    public void setRedirectUri(String uri) {
        entity.setRedirectUri(uri);
        markModified();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public ClientModel getClient() {
        return client;
    }

    @Override
    public String getAction() {
        return entity.getAction();
    }

    @Override
    public void setAction(String action) {
        entity.setAction(action);
        markModified();
    }

    @Override
    public String getProtocol() {
        return entity.getAuthMethod();
    }

    @Override
    public void setProtocol(String method) {
        entity.setAuthMethod(method);
        markModified();
    }

    private void markModified() {
        if (!modified) {
            modified = true;
            // Register for update on transaction commit
            // IMPORTANT: Use enlist() instead of enlistAfterCompletion() to ensure persistence
            // happens DURING the transaction, before any session removal in the same transaction.
            // Using enlistAfterCompletion() caused a bug where persist() would run AFTER
            // removeUserSession(), effectively re-creating deleted sessions.
            session.getTransactionManager().enlist(
                    new AbstractRedisPersistenceTransaction(flag -> modified = flag, () -> modified) {
                        @Override
                        protected void persist() {
                            RedisClientSessionAdapter.this.persist();
                        }
                    });
        }
    }

    /**
     * Persist changes to Redis.
     */
    public void persist() {
        // Skip persist if the session has been detached (deleted)
        if (detached) {
            modified = false;
            return;
        }

        if (modified) {
            String cacheName = offline ?
                    RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME :
                    RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME;

            // Check if the parent user session still exists before persisting.
            // This prevents re-creating orphaned client sessions when the user session
            // was deleted (e.g., after offline token exchange removes the online session).
            String userSessionCacheName = offline ?
                    RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME :
                    RedisConnectionProvider.USER_SESSION_CACHE_NAME;
            boolean userSessionExists = redis.containsKey(userSessionCacheName, entity.getUserSessionId());

            if (!userSessionExists) {
                logger.debugf("persist() skipped for clientSession %s - parent userSession no longer exists",
                        entity.getKey());
                modified = false;
                return;
            }

            long lifespan = offline ?
                    realm.getOfflineSessionIdleTimeout() :
                    realm.getSsoSessionMaxLifespan();

            redis.put(cacheName, entity.getKey(), entity, lifespan, TimeUnit.SECONDS);
            modified = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisClientSessionAdapter that = (RedisClientSessionAdapter) o;
        return Objects.equals(entity.getKey(), that.entity.getKey());
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity.getKey());
    }
}
