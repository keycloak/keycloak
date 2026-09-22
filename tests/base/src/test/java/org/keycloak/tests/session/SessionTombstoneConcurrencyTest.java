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

package org.keycloak.tests.session;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.SingleUseObjectValueEntity;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for GH issue #51127: concurrent cache reads during logout can
 * resurrect a deleted user session via putIfAbsent. The tombstone fix prevents this
 * by occupying the cache slot after deletion.
 */
@KeycloakIntegrationTest(config = SessionTombstoneConcurrencyTest.SessionCachingServerConfig.class)
@DatabaseTest
public class SessionTombstoneConcurrencyTest {

    private static final Logger LOG = Logger.getLogger(SessionTombstoneConcurrencyTest.class);

    private static final int CONCURRENT_THREADS = 8;
    private static final int ITERATIONS = 5;

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = TestUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient(config = DirectGrantClientConfig.class)
    OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @BeforeEach
    public void assumeSessionCachingEnabled() {
        boolean supported = runOnServer.fetch(session -> {
            var factory = (InfinispanUserSessionProviderFactory) session.getKeycloakSessionFactory().getProviderFactory(UserSessionProvider.class);
            return factory.useCaches();
        }, Boolean.class);
        Assumptions.assumeTrue(supported, "Requires session caching to be enabled (persistent sessions with embedded Infinispan caches)");
    }

    @Test
    public void logoutDuringConcurrentUserInfoShouldNotResurrectSession() throws Exception {
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            LOG.infof("=== Iteration %d/%d ===", iteration + 1, ITERATIONS);

            // 1. Login via password grant
            AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
            assertEquals(200, tokenResponse.getStatusCode(), "Password grant should succeed");
            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            assertNotNull(accessToken);
            assertNotNull(refreshToken);

            RefreshToken parsedRefresh = oauth.parseRefreshToken(refreshToken);
            String sessionId = parsedRefresh.getSessionId();

            // 2. Fire concurrent userinfo requests to trigger session cache reads,
            //    then logout on the main thread mid-flight.
            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            AtomicBoolean stopFlag = new AtomicBoolean(false);
            CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_THREADS);
            CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);
            List<Throwable> errors = new CopyOnWriteArrayList<>();

            for (int t = 0; t < CONCURRENT_THREADS; t++) {
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        while (!stopFlag.get()) {
                            try {
                                oauth.doUserInfoRequest(accessToken);
                            } catch (Exception e) {
                                // UserInfo may fail after logout — expected
                            }
                        }
                        // A few more requests after logout to widen the race window
                        for (int i = 0; i < 10; i++) {
                            try {
                                oauth.doUserInfoRequest(accessToken);
                            } catch (Exception e) {
                                // expected
                            }
                        }
                    } catch (Throwable e) {
                        errors.add(e);
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            // Wait for all threads to be ready
            readyLatch.await(10, TimeUnit.SECONDS);

            // 3. Evict user session from cache to force DB loads on concurrent requests.
            //    This makes the test more realistic: concurrent requests will now trigger
            //    the cache-miss → DB-load → putIfAbsent path (the exact race from #51127).
            runOnServer.run(session -> {
                Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                        session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
                cache.remove(sessionId);
                LOG.debugf("Evicted user session %s from cache to force DB loads", sessionId);
            });

            // 4. Logout while concurrent requests are doing DB loads
            LogoutResponse logoutResponse = oauth.doLogout(refreshToken);
            assertTrue(logoutResponse.isSuccess(), "Logout should succeed");
            stopFlag.set(true);

            doneLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

            if (!errors.isEmpty()) {
                errors.forEach(e -> LOG.error("Thread error", e));
            }

            // 5. Wait briefly for async cache operations to settle
            Thread.sleep(500);

            // 6. Verify: refresh token should be invalid (session was logged out)
            AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(400, refreshResponse.getStatusCode(),
                    "Refresh should fail after logout — session must not be resurrected in cache");

            // 7. Verify: this specific session should not exist
            List<UserSessionRepresentation> sessions = user.admin().getUserSessions();
            boolean sessionStillExists = sessions.stream().anyMatch(s -> s.getId().equals(sessionId));
            assertTrue(!sessionStillExists,
                    "Session " + sessionId + " must not be resurrected after logout");
        }
    }

    /**
     * Verifies that a tombstone in the user session cache prevents session lookup.
     * This directly exercises the isTombstone() check in UserSessionPersistentChangelogBasedTransaction.get().
     */
    @Test
    public void tombstonePreventsUserSessionLookup() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();

        // Verify the session works before tombstoning
        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh should succeed before tombstone");

        // Inject a tombstone into the user session cache slot.
        // We create the tombstone from scratch — the entity content doesn't matter,
        // only the "tombstone" marker in localMetadata is checked by isTombstone().
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            UserSessionEntity entity = new UserSessionEntity(sessionId);
            SessionEntityWrapper<UserSessionEntity> tombstone = new SessionEntityWrapper<>(entity).asTombstone();
            cache.put(sessionId, tombstone, 30, TimeUnit.SECONDS);
            LOG.debugf("Injected tombstone for user session %s", sessionId);
        });

        // Refresh should fail: the tombstone makes get() return null
        assertEquals(400, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh must fail when user session cache slot contains a tombstone");

        // Clean up: remove tombstone so the session can be properly logged out
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
        });
        oauth.doLogout(refreshToken);
    }

    /**
     * Verifies that a tombstone in the client session cache blocks re-import from the database.
     * This exercises the isTombstoneBlockingImportOf() check in PersistentSessionsChangelogBasedTransaction.importSession().
     */
    @Test
    public void tombstoneBlocksClientSessionImportFromDatabase() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();

        // Verify the session works before tombstoning
        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh should succeed before tombstone");

        // Inject a tombstone into the client session cache slot (user session remains valid).
        // We need the client session's timestamp so the tombstone blocks import of that exact session.
        String realmName = realm.getName();
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            session.getContext().setRealm(realmModel);
            var clientModel = realmModel.getClientByClientId("tombstone-test-client");
            String clientUUID = clientModel.getId();

            // Look up the client session to get the timestamp
            var userSession = session.sessions().getUserSession(realmModel, sessionId);
            assertNotNull(userSession, "User session should exist");
            var clientSession = session.sessions().getClientSession(userSession, clientModel, false);
            assertNotNull(clientSession, "Client session should exist");
            int timestamp = clientSession.getTimestamp();

            // Create tombstone with matching timestamp so isTombstoneBlockingImportOf() returns true
            AuthenticatedClientSessionEntity entity = new AuthenticatedClientSessionEntity();
            entity.setTimestamp(timestamp);
            SessionEntityWrapper<AuthenticatedClientSessionEntity> tombstone = new SessionEntityWrapper<>(entity).asTombstone();

            EmbeddedClientSessionKey key = new EmbeddedClientSessionKey(sessionId, clientUUID);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            cache.put(key, tombstone, 30, TimeUnit.SECONDS);
            LOG.debugf("Injected tombstone for client session %s/%s with timestamp %d", sessionId, clientUUID, timestamp);
        });

        // Refresh should fail: the tombstone in the client session cache triggers a DB load,
        // but importSession() finds the tombstone via putIfAbsent and isTombstoneBlockingImportOf()
        // returns true (same timestamp), so the import is blocked.
        assertEquals(400, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh must fail when client session cache slot contains a tombstone");

        // Clean up
        String cleanupRealmName = realm.getName();
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(cleanupRealmName);
            String clientUUID = realmModel.getClientByClientId("tombstone-test-client").getId();
            Cache<EmbeddedClientSessionKey, ?> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            cache.remove(new EmbeddedClientSessionKey(sessionId, clientUUID));
        });
        oauth.doLogout(refreshToken);
    }

    /**
     * Verifies that a backup tombstone in the action token cache blocks session import
     * even when no tombstone exists in the session cache (simulating eviction from bounded cache).
     */
    @Test
    public void backupTombstoneInActionTokenCacheBlocksImport() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();

        // Verify the session works
        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh should succeed before backup tombstone");

        // Inject a backup tombstone into the action token cache (no tombstone in session cache).
        // Then remove the session from the session cache to force a DB reload → putIfAbsent path.
        runOnServer.run(session -> {
            Cache<String, SingleUseObjectValueEntity> actionTokenCache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE);
            String backupKey = "tomb:" + InfinispanConnectionProvider.USER_SESSION_CACHE_NAME + ":" + sessionId;
            actionTokenCache.put(backupKey, new SingleUseObjectValueEntity(Map.of()), 30, TimeUnit.SECONDS);

            Cache<String, SessionEntityWrapper<UserSessionEntity>> sessionCache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            sessionCache.remove(sessionId);
            LOG.debugf("Injected backup tombstone for user session %s and evicted from session cache", sessionId);
        });

        // Refresh should fail: putIfAbsent succeeds (no tombstone in session cache),
        // but the backup check in the action token cache detects the deletion and removes the entry.
        assertEquals(400, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh must fail when backup tombstone exists in action token cache");

        // Clean up
        runOnServer.run(session -> {
            Cache<String, SingleUseObjectValueEntity> actionTokenCache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.ACTION_TOKEN_CACHE);
            actionTokenCache.remove("tomb:" + InfinispanConnectionProvider.USER_SESSION_CACHE_NAME + ":" + sessionId);
        });
        oauth.doLogout(refreshToken);
    }

    public static class SessionCachingServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            // Tombstones and concurrency issues only happen when caching is enabled.
            // In 26.8, caching is disabled by default.
            return config.spiOption("user-sessions", "infinispan", "use-caches", "true");
        }
    }

    public static class TestUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("tombstone-test-user")
                    .password("password")
                    .email("tombstone-test@localhost")
                    .name("Tombstone", "Test");
        }
    }

    public static class DirectGrantClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("tombstone-test-client")
                    .secret("secret")
                    .directAccessGrantsEnabled(true)
                    .redirectUris("http://localhost:8080/*");
        }
    }
}
