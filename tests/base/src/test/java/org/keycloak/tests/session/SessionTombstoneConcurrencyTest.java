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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.keycloak.common.util.MultiSiteUtils;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.infinispan.util.InfinispanUtils;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
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
@KeycloakIntegrationTest
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
    public void assumePersistentUserSessionsWithEmbeddedCaches() {
        boolean supported = runOnServer.fetch(session -> MultiSiteUtils.isPersistentSessionsEnabled() && InfinispanUtils.isEmbeddedInfinispan(), Boolean.class);
        Assumptions.assumeTrue(supported, "Requires persistent user sessions with embedded Infinispan caches");
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
            String realmName = realm.getName();
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

            // 7. Verify: admin API should show 0 sessions for this user
            List<UserSessionRepresentation> sessions = user.admin().getUserSessions();
            assertEquals(0, sessions.size(),
                    "Admin API should show 0 sessions after logout — session must not be resurrected");
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

        // Inject a tombstone into the user session cache slot
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            SessionEntityWrapper<UserSessionEntity> existing = cache.get(sessionId);
            assertNotNull(existing, "User session should exist in cache");
            cache.put(sessionId, existing.asTombstone(), 60, TimeUnit.SECONDS);
            LOG.debugf("Injected tombstone for user session %s", sessionId);
        });

        // Refresh should fail: the tombstone makes get() return null
        assertEquals(400, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh must fail when user session cache slot contains a tombstone");
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

        // Inject a tombstone into the client session cache slot (user session remains valid)
        String realmName = realm.getName();
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            String clientUUID = realmModel.getClientByClientId("tombstone-test-client").getId();

            EmbeddedClientSessionKey key = new EmbeddedClientSessionKey(sessionId, clientUUID);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            SessionEntityWrapper<AuthenticatedClientSessionEntity> existing = cache.get(key);
            assertNotNull(existing, "Client session should exist in cache");
            cache.put(key, existing.asTombstone(), 60, TimeUnit.SECONDS);
            LOG.debugf("Injected tombstone for client session %s/%s", sessionId, clientUUID);
        });

        // Refresh should fail: the tombstone in the client session cache triggers a DB load,
        // but importSession() finds the tombstone via putIfAbsent and isTombstoneBlockingImportOf()
        // returns true (same timestamp), so the import is blocked.
        assertEquals(400, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh must fail when client session cache slot contains a tombstone");
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
