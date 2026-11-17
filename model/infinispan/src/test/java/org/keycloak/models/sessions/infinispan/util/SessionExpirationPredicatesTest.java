/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.sessions.infinispan.util;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SessionExpirationPredicates}, verifying that seconds-valued entity fields
 * are correctly converted to milliseconds before comparison.
 */
public class SessionExpirationPredicatesTest {

    private static final int SSO_MAX_LIFESPAN = 600;
    private static final int SSO_IDLE_TIMEOUT = 300;

    private final Map<String, Object> realmMap = new HashMap<>();
    private final Map<String, String> clientAttributes = new HashMap<>();
    private RealmModel realm;
    private ClientModel client;

    @Before
    public void setUp() {
        realm = createRealm();
        client = createClient();
        resetRealm();
    }

    @Test
    public void testUserSessionEntityNotExpired() {
        int createdSeconds = 1_000_000;
        int lastRefreshSeconds = createdSeconds + 100;
        long nowMillis = TimeUnit.SECONDS.toMillis(lastRefreshSeconds) + TimeUnit.SECONDS.toMillis(SSO_IDLE_TIMEOUT) - 1000;

        var entity = createUserSessionEntity(createdSeconds, lastRefreshSeconds, false);
        var predicates = new SessionExpirationPredicates(realm, false, nowMillis);

        Assert.assertFalse("Session should not be expired", predicates.isUserSessionExpired(entity));
    }

    @Test
    public void testUserSessionEntityExpiredByLifespan() {
        int createdSeconds = 1_000_000;
        long nowMillis = TimeUnit.SECONDS.toMillis(createdSeconds) + TimeUnit.SECONDS.toMillis(SSO_MAX_LIFESPAN) + 1000;

        var entity = createUserSessionEntity(createdSeconds, createdSeconds, false);
        var predicates = new SessionExpirationPredicates(realm, false, nowMillis);

        Assert.assertTrue("Session should be expired by lifespan", predicates.isUserSessionExpired(entity));
    }

    @Test
    public void testUserSessionEntityExpiredByIdle() {
        int createdSeconds = 1_000_000;
        int lastRefreshSeconds = createdSeconds + 100;
        long nowMillis = TimeUnit.SECONDS.toMillis(lastRefreshSeconds) + TimeUnit.SECONDS.toMillis(SSO_IDLE_TIMEOUT) + 1000;

        var entity = createUserSessionEntity(createdSeconds, lastRefreshSeconds, false);
        var predicates = new SessionExpirationPredicates(realm, false, nowMillis);

        Assert.assertTrue("Session should be expired by idle timeout", predicates.isUserSessionExpired(entity));
    }

    @Test
    public void testClientSessionEntityNotExpired() {
        int createdSeconds = 1_000_000;
        int lastRefreshSeconds = createdSeconds + 100;
        long nowMillis = TimeUnit.SECONDS.toMillis(lastRefreshSeconds) + TimeUnit.SECONDS.toMillis(SSO_IDLE_TIMEOUT) - 1000;

        var entity = createClientSessionEntity(createdSeconds, createdSeconds, lastRefreshSeconds);
        var predicates = new SessionExpirationPredicates(realm, false, nowMillis);

        Assert.assertFalse("Client session should not be expired",
                predicates.isClientSessionExpired(entity, false, client));
    }

    @Test
    public void testClientSessionEntityExpiredByLifespan() {
        int createdSeconds = 1_000_000;
        long nowMillis = TimeUnit.SECONDS.toMillis(createdSeconds) + TimeUnit.SECONDS.toMillis(SSO_MAX_LIFESPAN) + 1000;

        var entity = createClientSessionEntity(createdSeconds, createdSeconds, createdSeconds);
        var predicates = new SessionExpirationPredicates(realm, false, nowMillis);

        Assert.assertTrue("Client session should be expired by lifespan",
                predicates.isClientSessionExpired(entity, false, client));
    }

    @Test
    public void testOfflineUserSessionUnlimitedLifespan() {
        int createdSeconds = 1_000_000;
        int lastRefreshSeconds = createdSeconds;
        long nowMillis = TimeUnit.SECONDS.toMillis(createdSeconds) + TimeUnit.SECONDS.toMillis(100_000);

        var entity = createUserSessionEntity(createdSeconds, lastRefreshSeconds, false);
        var predicates = new SessionExpirationPredicates(realm, true, nowMillis);

        // Offline lifespan is unlimited (-1) by default; only idle matters.
        // lastRefresh + offlineIdleTimeout (default 2592000s = 30d) is far in the future.
        Assert.assertFalse("Offline session should not be expired with default idle timeout",
                predicates.isUserSessionExpired(entity));
    }

    @Test
    public void testSecondsAreNotComparedDirectlyAgainstMilliseconds() {
        // This test guards against the original bug: if seconds are compared against
        // milliseconds, the subtraction (ms_timestamp - seconds_current) is always positive,
        // and expired sessions are never detected.
        int createdSeconds = 1_000_000;
        long nowMillis = TimeUnit.SECONDS.toMillis(createdSeconds) + TimeUnit.SECONDS.toMillis(SSO_MAX_LIFESPAN) + 1000;

        var entity = createUserSessionEntity(createdSeconds, createdSeconds, false);

        // Correct: currentTimeMillis in milliseconds
        var correctPredicates = new SessionExpirationPredicates(realm, false, nowMillis);
        Assert.assertTrue("Should detect expiration with millisecond time",
                correctPredicates.isUserSessionExpired(entity));

        // Wrong: if someone passed seconds instead, the subtraction would be
        // ms_timestamp - seconds ≈ ms_timestamp (always positive), never detecting expiration.
        long wrongTimeInSeconds = nowMillis / 1000;
        var wrongPredicates = new SessionExpirationPredicates(realm, false, wrongTimeInSeconds);
        // With seconds as currentTimeMillis, the session appears non-expired (the original bug)
        Assert.assertFalse("Seconds-as-millis should fail to detect expiration (demonstrating the bug)",
                wrongPredicates.isUserSessionExpired(entity));
    }

    private void resetRealm() {
        realmMap.clear();
        clientAttributes.clear();
        realmMap.put("isOfflineSessionMaxLifespanEnabled", false);
        realmMap.put("getOfflineSessionMaxLifespan", 0);
        realmMap.put("getOfflineSessionIdleTimeout", 0);
        realmMap.put("getClientOfflineSessionMaxLifespan", 0);
        realmMap.put("getClientOfflineSessionIdleTimeout", 0);
        realmMap.put("getSsoSessionMaxLifespan", SSO_MAX_LIFESPAN);
        realmMap.put("getSsoSessionIdleTimeout", SSO_IDLE_TIMEOUT);
        realmMap.put("getClientSessionMaxLifespan", 0);
        realmMap.put("getClientSessionIdleTimeout", 0);
        realmMap.put("getSsoSessionMaxLifespanRememberMe", 0);
        realmMap.put("getSsoSessionIdleTimeoutRememberMe", 0);
    }

    private RealmModel createRealm() {
        return (RealmModel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{RealmModel.class}, (proxy, method, args) -> {
                    Object result = realmMap.get(method.getName());
                    if (result != null) {
                        return result;
                    }
                    throw new UnsupportedOperationException("Realm method not in map: " + method.getName());
                });
    }

    private ClientModel createClient() {
        return (ClientModel) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class[]{ClientModel.class}, (proxy, method, args) -> {
                    if ("getAttribute".equals(method.getName()) && args.length == 1) {
                        return clientAttributes.get((String) args[0]);
                    }
                    throw new UnsupportedOperationException("Client method not in map: " + method.getName());
                });
    }

    private static UserSessionEntity createUserSessionEntity(int startedSeconds, int lastRefreshSeconds, boolean rememberMe) {
        var entity = new UserSessionEntity("test-session");
        entity.setStarted(startedSeconds);
        entity.setLastSessionRefresh(lastRefreshSeconds);
        entity.setRememberMe(rememberMe);
        return entity;
    }

    private static AuthenticatedClientSessionEntity createClientSessionEntity(int startedSeconds, int userSessionStartedSeconds, int timestampSeconds) {
        var entity = new AuthenticatedClientSessionEntity();
        entity.setTimestamp(timestampSeconds);
        entity.getNotes().put(AuthenticatedClientSessionModel.STARTED_AT_NOTE, String.valueOf(startedSeconds));
        entity.getNotes().put(AuthenticatedClientSessionModel.USER_SESSION_STARTED_AT_NOTE, String.valueOf(userSessionStartedSeconds));
        return entity;
    }
}
