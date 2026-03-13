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

package org.keycloak.models.redis.loginFailure;

import org.keycloak.models.redis.RedisConnectionProvider;
import org.keycloak.models.redis.entities.*;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserLoginFailureModel;
import org.keycloak.models.UserLoginFailureProvider;
import org.keycloak.models.redis.entities.RedisLoginFailureEntity;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of UserLoginFailureProvider for brute force protection.
 */
public class RedisUserLoginFailureProvider implements UserLoginFailureProvider {

    private static final Logger logger = Logger.getLogger(RedisUserLoginFailureProvider.class);

    private final KeycloakSession session;
    private final RedisConnectionProvider redis;
    private final long failureLifespan;

    public RedisUserLoginFailureProvider(KeycloakSession session, RedisConnectionProvider redis, long failureLifespan) {
        this.session = session;
        this.redis = redis;
        this.failureLifespan = failureLifespan;
    }

    @Override
    public UserLoginFailureModel getUserLoginFailure(RealmModel realm, String odString) {
        String key = RedisLoginFailureEntity.createKey(realm.getId(), odString);
        RedisLoginFailureEntity entity = redis.get(
                RedisConnectionProvider.CACHE_LOGIN_FAILURES,
                key,
                RedisLoginFailureEntity.class
        );

        if (entity == null) {
            return null;
        }

        return new RedisUserLoginFailureAdapter(session, realm, entity, redis, failureLifespan);
    }

    @Override
    public UserLoginFailureModel addUserLoginFailure(RealmModel realm, String odString) {
        String key = RedisLoginFailureEntity.createKey(realm.getId(), odString);

        RedisLoginFailureEntity entity = redis.get(
                RedisConnectionProvider.CACHE_LOGIN_FAILURES,
                key,
                RedisLoginFailureEntity.class
        );

        if (entity == null) {
            entity = new RedisLoginFailureEntity(realm.getId(), odString);
            redis.put(RedisConnectionProvider.CACHE_LOGIN_FAILURES, key, entity, failureLifespan, TimeUnit.SECONDS);
        }

        return new RedisUserLoginFailureAdapter(session, realm, entity, redis, failureLifespan);
    }

    @Override
    public void removeUserLoginFailure(RealmModel realm, String odString) {
        String key = RedisLoginFailureEntity.createKey(realm.getId(), odString);
        redis.delete(RedisConnectionProvider.CACHE_LOGIN_FAILURES, key);
    }

    @Override
    public void removeAllUserLoginFailures(RealmModel realm) {
        // Would need to scan all keys - for now, let them expire
        logger.debugf("removeAllUserLoginFailures called for realm %s - entries will expire via TTL", realm.getId());
    }

    @Override
    public void close() {
        // Nothing to close
    }

    /**
     * Adapter for UserLoginFailureModel backed by Redis.
     */
    private static class RedisUserLoginFailureAdapter implements UserLoginFailureModel {

        private final KeycloakSession session;
        private final RealmModel realm;
        private final RedisLoginFailureEntity entity;
        private final RedisConnectionProvider redis;
        private final long lifespan;

        public RedisUserLoginFailureAdapter(KeycloakSession session, RealmModel realm,
                                            RedisLoginFailureEntity entity,
                                            RedisConnectionProvider redis, long lifespan) {
            this.session = session;
            this.realm = realm;
            this.entity = entity;
            this.redis = redis;
            this.lifespan = lifespan;
        }

        @Override
        public String getId() {
            return RedisLoginFailureEntity.createKey(entity.getRealmId(), entity.getUserId());
        }

        @Override
        public String getUserId() {
            return entity.getUserId();
        }

        @Override
        public int getFailedLoginNotBefore() {
            return entity.getFailedLoginNotBefore();
        }

        @Override
        public void setFailedLoginNotBefore(int notBefore) {
            entity.setFailedLoginNotBefore(notBefore);
            save();
        }

        @Override
        public int getNumFailures() {
            return entity.getNumFailures();
        }

        @Override
        public void incrementFailures() {
            entity.setNumFailures(entity.getNumFailures() + 1);
            save();
        }

        @Override
        public void clearFailures() {
            entity.clearFailures();
            save();
        }

        @Override
        public long getLastFailure() {
            return entity.getLastFailure();
        }

        @Override
        public void setLastFailure(long lastFailure) {
            entity.setLastFailure(lastFailure);
            save();
        }

        @Override
        public String getLastIPFailure() {
            return entity.getLastIPFailure();
        }

        @Override
        public void setLastIPFailure(String ip) {
            entity.setLastIPFailure(ip);
            save();
        }

        @Override
        public int getNumTemporaryLockouts() {
            return entity.getNumTemporaryLockouts();
        }

        @Override
        public void incrementTemporaryLockouts() {
            entity.setNumTemporaryLockouts(entity.getNumTemporaryLockouts() + 1);
            save();
        }

        private void save() {
            String key = RedisLoginFailureEntity.createKey(entity.getRealmId(), entity.getUserId());
            redis.put(RedisConnectionProvider.CACHE_LOGIN_FAILURES, key, entity, lifespan, TimeUnit.SECONDS);
        }
    }
}
