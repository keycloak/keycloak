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

import org.keycloak.models.redis.RedisConnectionProvider;
import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.redis.entities.RedisAuthenticationSessionEntity;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Adapter for RootAuthenticationSessionModel backed by Redis.
 * Uses deferred writes to batch multiple updates into a single Redis PSETEX call.
 */
public class RedisRootAuthenticationSessionAdapter implements RootAuthenticationSessionModel {

    private static final Logger logger = Logger.getLogger(RedisRootAuthenticationSessionAdapter.class);

    private final KeycloakSession session;
    private final RealmModel realm;
    private final RedisAuthenticationSessionEntity entity;
    private final RedisConnectionProvider redis;
    private final int lifespan;
    private boolean modified = false;

    public RedisRootAuthenticationSessionAdapter(KeycloakSession session, RealmModel realm,
                                                 RedisAuthenticationSessionEntity entity,
                                                 RedisConnectionProvider redis, int lifespan) {
        this.session = session;
        this.realm = realm;
        this.entity = entity;
        this.redis = redis;
        this.lifespan = lifespan;
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
    public int getTimestamp() {
        return entity.getTimestamp();
    }

    @Override
    public void setTimestamp(int timestamp) {
        logger.debugf("setTimestamp called for root authentication session %s with timestamp %d", getId(), timestamp);
        entity.setTimestamp(timestamp);
        markModified();
    }

    @Override
    public Map<String, AuthenticationSessionModel> getAuthenticationSessions() {
        Map<String, AuthenticationSessionModel> result = new HashMap<>();
        for (Map.Entry<String, RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity> entry : entity.getAuthenticationSessions().entrySet()) {
            ClientModel client = realm.getClientById(entry.getValue().getClientUUID());
            if (client != null) {
                result.put(entry.getKey(), new RedisAuthenticationSessionAdapter(session, this, client, entry.getValue()));
            }
        }
        return result;
    }

    @Override
    public AuthenticationSessionModel getAuthenticationSession(ClientModel client, String tabId) {
        if (client == null || tabId == null) return null;
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab = entity.getAuthenticationSession(tabId);
        if (tab == null || !tab.getClientUUID().equals(client.getId())) return null;
        return new RedisAuthenticationSessionAdapter(session, this, client, tab);
    }

    @Override
    public AuthenticationSessionModel createAuthenticationSession(ClientModel client) {
        String tabId = org.keycloak.models.utils.KeycloakModelUtils.generateId();
        RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity tab =
                new RedisAuthenticationSessionEntity.RedisAuthenticationTabEntity(tabId, client.getId());
        entity.setAuthenticationSession(tabId, tab);
        logger.debugf("createAuthenticationSession called for root authentication session %s with tabId %s and client %s", getId(), tabId, client.getId());
        markModified();
        return new RedisAuthenticationSessionAdapter(session, this, client, tab);
    }

    @Override
    public void removeAuthenticationSessionByTabId(String tabId) {
        logger.debugf("removeAuthenticationSessionByTabId called for root authentication session %s with tabId %s", getId(), tabId);
        entity.removeAuthenticationSession(tabId);
        markModified();
    }

    @Override
    public void restartSession(RealmModel realm) {
        logger.debugf("restartSession called for root authentication session %s", getId());
        entity.getAuthenticationSessions().clear();
        entity.setTimestamp(Time.currentTime());
        markModified();
    }

    /**
     * Marks this session as modified and registers for transaction commit.
     * This allows multiple updates to be batched into a single Redis PSETEX call.
     */
    void markModified() {
        if (!modified) {
            modified = true;
            // Register for update on transaction commit
            session.getTransactionManager().enlistAfterCompletion(
                    new AbstractRedisPersistenceTransaction(flag -> modified = flag, () -> modified) {
                        @Override
                        protected void persist() {
                            RedisRootAuthenticationSessionAdapter.this.persist();
                        }
                    });
        }
    }

    /**
     * Persist changes to Redis.
     * All accumulated changes are written in a single PSETEX call.
     */
    private void persist() {
        logger.debugf("persist called for root authentication session %s", getId());
        if (modified) {
            redis.put(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS, entity.getId(), entity, lifespan, TimeUnit.SECONDS);
            modified = false;
        }
    }
}
