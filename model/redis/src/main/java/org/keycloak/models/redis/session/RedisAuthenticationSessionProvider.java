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
import org.keycloak.models.redis.entities.*;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.redis.entities.RedisAuthenticationSessionEntity;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.sessions.AuthenticationSessionCompoundId;
import org.keycloak.sessions.AuthenticationSessionProvider;
import org.keycloak.sessions.RootAuthenticationSessionModel;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based implementation of AuthenticationSessionProvider.
 *
 * Auth session TTL is computed dynamically per-realm using
 * {@link SessionExpiration#getAuthSessionLifespan(RealmModel)}, which returns
 * {@code max(accessCodeLifespanLogin, accessCodeLifespanUserAction, accessCodeLifespan)}.
 * This matches the behavior of Keycloak's built-in Infinispan provider.
 */
public class RedisAuthenticationSessionProvider implements AuthenticationSessionProvider {

    private static final Logger logger = Logger.getLogger(RedisAuthenticationSessionProvider.class);

    /** Fallback TTL if realm config returns 0 or negative (defensive). */
    public static final int FALLBACK_AUTH_SESSION_LIFESPAN = 300;

    private final KeycloakSession session;
    private final RedisConnectionProvider redis;

    public RedisAuthenticationSessionProvider(KeycloakSession session, RedisConnectionProvider redis) {
        this.session = session;
        this.redis = redis;
    }

    /**
     * Computes the auth session lifespan for the given realm by delegating to
     * {@link SessionExpiration#getAuthSessionLifespan(RealmModel)}.
     *
     * @param realm the realm whose config determines the TTL
     * @return lifespan in seconds, guaranteed &gt; 0
     */
    int calculateAuthSessionLifespan(RealmModel realm) {
        int realmTtl = SessionExpiration.getAuthSessionLifespan(realm);
        if (realmTtl <= 0) {
            logger.warnf("Auth session TTL for realm '%s' was %d, using fallback: %d seconds",
                    realm.getId(), realmTtl, FALLBACK_AUTH_SESSION_LIFESPAN);
            return FALLBACK_AUTH_SESSION_LIFESPAN;
        }
        return realmTtl;
    }

    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm) {
        return createRootAuthenticationSession(realm, null);
    }

    @Override
    public RootAuthenticationSessionModel createRootAuthenticationSession(RealmModel realm, String id) {
        String sessionId = id != null ? id : org.keycloak.models.utils.KeycloakModelUtils.generateId();
        int timestamp = Time.currentTime();
        int lifespan = calculateAuthSessionLifespan(realm);

        // Adding this log for now to help debug potential issues with Authentication Sessions not being put in Redis
        logger.debugf("Creating root authentication session with ID: %s for realm: %s (TTL: %ds)...", sessionId, realm.getId(), lifespan);

        RedisAuthenticationSessionEntity entity = new RedisAuthenticationSessionEntity(sessionId, realm.getId(), timestamp);

        redis.put(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS, sessionId, entity, lifespan, TimeUnit.SECONDS);

        logger.debugf("Created root authentication session: %s for realm: %s", sessionId, realm.getId());
        return new RedisRootAuthenticationSessionAdapter(session, realm, entity, redis, lifespan);
    }

    @Override
    public RootAuthenticationSessionModel getRootAuthenticationSession(RealmModel realm, String authenticationSessionId) {
        if (authenticationSessionId == null) return null;

        RedisAuthenticationSessionEntity entity = redis.get(
                RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS,
                authenticationSessionId,
                RedisAuthenticationSessionEntity.class
        );

        if (entity == null || !entity.getRealmId().equals(realm.getId())) {
            return null;
        }

        int lifespan = calculateAuthSessionLifespan(realm);
        logger.debugf("Retrieved root authentication session with ID: %s for realm: %s (TTL: %ds)", authenticationSessionId, realm.getId(), lifespan);

        return new RedisRootAuthenticationSessionAdapter(session, realm, entity, redis, lifespan);
    }

    @Override
    public void removeRootAuthenticationSession(RealmModel realm, RootAuthenticationSessionModel authenticationSession) {
        if (authenticationSession == null) return;

        redis.delete(RedisConnectionProvider.CACHE_AUTHENTICATION_SESSIONS, authenticationSession.getId());
        logger.debugf("Removed root authentication session: %s", authenticationSession.getId());
    }

    @Override
    public void removeAllExpired() {
        logger.debug("no-op: removeAllExpired called - Redis handles this via TTL");
    }

    @Override
    public void removeExpired(RealmModel realm) {
        logger.debugf("no-op: removeExpired called for realm %s - Redis handles this via TTL", realm.getId());
    }

    @Override
    public void onRealmRemoved(RealmModel realm) {
        logger.debugf("no-op: onRealmRemoved called for realm %s", realm.getId());
    }

    @Override
    public void onClientRemoved(RealmModel realm, ClientModel client) {
        logger.debugf("no-op: onClientRemoved called for client %s", client.getClientId());
    }

    @Override
    public void updateNonlocalSessionAuthNotes(AuthenticationSessionCompoundId compoundId, Map<String, String> authNotesFragment) {
        logger.debugf("no-op: updateNonlocalSessionAuthNotes called for compoundId %s", compoundId);
    }

    @Override
    public void close() {
    }
}
