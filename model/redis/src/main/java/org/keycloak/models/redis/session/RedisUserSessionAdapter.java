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
import org.keycloak.models.redis.entities.*;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.redis.entities.RedisClientSessionEntity;
import org.keycloak.models.redis.entities.RedisUserSessionEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Adapter that wraps RedisUserSessionEntity to implement UserSessionModel.
 */
public class RedisUserSessionAdapter implements UserSessionModel {

    private static final Logger logger = Logger.getLogger(RedisUserSessionAdapter.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final UserModel user;
    private final RedisUserSessionEntity entity;
    private final RedisConnectionProvider redis;
    private final boolean offline;
    private boolean modified = false;
    private long version = 0; // Version for optimistic locking

    public RedisUserSessionAdapter(KeycloakSession session, RealmModel realm, UserModel user,
                                    RedisUserSessionEntity entity, RedisConnectionProvider redis,
                                    boolean offline, long initialVersion) {
        this.session = Objects.requireNonNull(session);
        this.realm = Objects.requireNonNull(realm);
        this.user = user;
        this.entity = Objects.requireNonNull(entity);
        this.redis = Objects.requireNonNull(redis);
        this.offline = offline;
        this.version = initialVersion;
    }

    @Override
    public String getId() {
        return entity.getId();
    }

    @Override
    public RealmModel getRealm() {
        return realm;
    }

    @Override
    public String getBrokerSessionId() {
        return entity.getBrokerSessionId();
    }

    @Override
    public String getBrokerUserId() {
        return entity.getBrokerUserId();
    }

    @Override
    public UserModel getUser() {
        return user;
    }

    @Override
    public String getLoginUsername() {
        return entity.getLoginUsername();
    }

    @Override
    public String getIpAddress() {
        return entity.getIpAddress();
    }

    @Override
    public String getAuthMethod() {
        return entity.getAuthMethod();
    }

    @Override
    public boolean isRememberMe() {
        return entity.isRememberMe();
    }

    @Override
    public int getStarted() {
        return entity.getStarted();
    }

    @Override
    public int getLastSessionRefresh() {
        return entity.getLastSessionRefresh();
    }

    @Override
    public void setLastSessionRefresh(int seconds) {
        entity.setLastSessionRefresh(seconds);
        markModified();
    }

    @Override
    public boolean isOffline() {
        return offline;
    }

    @Override
    public Map<String, AuthenticatedClientSessionModel> getAuthenticatedClientSessions() {
        Map<String, AuthenticatedClientSessionModel> result = new HashMap<>();
        
        String cacheName = offline ?
                RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME :
                RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME;
        
        // Use userSession index for O(1) lookup of client session keys
        String userSessionIndexKey = "userSession:" + entity.getId();
        List<String> clientSessionKeys = redis.getSortedSetMembers(cacheName, userSessionIndexKey);
        
        if (clientSessionKeys.isEmpty()) {
            return result;
        }
        
        // Batch load all client session entities (single Redis call)
        Map<String, RedisClientSessionEntity> entities = redis.getAll(cacheName, clientSessionKeys, RedisClientSessionEntity.class);
        
        for (Map.Entry<String, RedisClientSessionEntity> entry : entities.entrySet()) {
            RedisClientSessionEntity clientEntity = entry.getValue();
            String clientId = clientEntity.getClientId();
            
            ClientModel client = realm.getClientById(clientId);
            if (client == null) {
                // Client was deleted, skip (Redis TTL will clean up the session)
                continue;
            }
            
            result.put(clientId, new RedisClientSessionAdapter(
                    session, realm, client, this, clientEntity, redis, offline, 1));
        }
        
        return result;
    }

    @Override
    public AuthenticatedClientSessionModel getAuthenticatedClientSessionByClient(String clientUUID) {
        ClientModel client = realm.getClientById(clientUUID);
        if (client == null) {
            return null;
        }
        
        String key = entity.getId() + ":" + clientUUID;
        String cacheName = offline ?
                RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME :
                RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME;
        
        RedisClientSessionEntity clientEntity = redis.get(cacheName, key, RedisClientSessionEntity.class);
        if (clientEntity == null) {
            return null;
        }
        
        // Session was loaded from Redis, it has at least version 1
        return new RedisClientSessionAdapter(session, realm, client, this, clientEntity, redis, offline, 1);
    }

    @Override
    public void removeAuthenticatedClientSessions(java.util.Collection<String> removedClientUUIDS) {
        String cacheName = offline ?
                RedisConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME :
                RedisConnectionProvider.CLIENT_SESSION_CACHE_NAME;
        
        for (String clientId : removedClientUUIDS) {
            String key = entity.getId() + ":" + clientId;
            redis.delete(cacheName, key);
        }
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
    public State getState() {
        return entity.getState();
    }

    @Override
    public void setState(State state) {
        entity.setState(state);
        markModified();
    }

    @Override
    public void restartSession(RealmModel realm, UserModel user, String loginUsername,
                                String ipAddress, String authMethod, boolean rememberMe,
                                String brokerSessionId, String brokerUserId) {
        entity.restart(realm.getId(), user.getId(), loginUsername, ipAddress,
                authMethod, rememberMe, brokerSessionId, brokerUserId);
        markModified();
    }

    @Override
    public SessionPersistenceState getPersistenceState() {
        return SessionPersistenceState.PERSISTENT;
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
                            RedisUserSessionAdapter.this.persist();
                        }
                    });
        }
    }

    /**
     * Persist changes to Redis.
     */
    public void persist() {
        if (modified) {
            // Check if this session has been marked as deleted by the provider
            // This prevents re-creating sessions that were deleted during the same transaction
            RedisUserSessionProvider provider = (RedisUserSessionProvider) session.sessions();
            if (provider != null && provider.isUserSessionDeleted(entity.getId())) {
                logger.debugf("persist() skipped for userSession %s - session was marked as deleted", entity.getId());
                modified = false;
                return;
            }

            String cacheName = offline ?
                    RedisConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME :
                    RedisConnectionProvider.USER_SESSION_CACHE_NAME;

            long lifespan = offline ?
                    realm.getOfflineSessionIdleTimeout() :
                    realm.getSsoSessionMaxLifespan();

            redis.put(cacheName, entity.getId(), entity, lifespan, TimeUnit.SECONDS);
            modified = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RedisUserSessionAdapter that = (RedisUserSessionAdapter) o;
        return Objects.equals(entity.getId(), that.entity.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(entity.getId());
    }
}
