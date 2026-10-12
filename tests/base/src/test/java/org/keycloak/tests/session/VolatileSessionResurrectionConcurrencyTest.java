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
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
import org.keycloak.models.sessions.infinispan.entities.AuthenticatedClientSessionEntity;
import org.keycloak.models.sessions.infinispan.entities.EmbeddedClientSessionKey;
import org.keycloak.models.sessions.infinispan.entities.UserSessionEntity;
import org.keycloak.representations.RefreshToken;
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
import org.keycloak.testframework.remote.providers.runonserver.FetchOnServer;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;
import org.keycloak.util.TokenUtil;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the loading marker + CAS fix for GH issue #51127 in the volatile session path
 * (persistent user sessions disabled). The volatile path falls back to the database for
 * offline sessions, which has the same resurrection race as the persistent path.
 */
@KeycloakIntegrationTest(config = VolatileSessionResurrectionConcurrencyTest.VolatileSessionServerConfig.class)
public class VolatileSessionResurrectionConcurrencyTest {

    private static final Logger LOG = Logger.getLogger(VolatileSessionResurrectionConcurrencyTest.class);

    private static final int CONCURRENT_THREADS = 8;
    private static final int ITERATIONS = 5;

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = TestUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient(config = OfflineGrantClientConfig.class)
    OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    /**
     * Smoke test: regular (non-offline) login and logout work with persistent sessions disabled.
     */
    @Test
    public void regularLoginLogoutWorks() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode(), "Password grant should succeed");
        assertNotNull(tokenResponse.getAccessToken());
        assertNotNull(tokenResponse.getRefreshToken());

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(200, refreshResponse.getStatusCode(), "Refresh should succeed");

        LogoutResponse logoutResponse = oauth.doLogout(refreshResponse.getRefreshToken());
        assertTrue(logoutResponse.isSuccess(), "Logout should succeed");

        AccessTokenResponse afterLogout = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken());
        assertEquals(400, afterLogout.getStatusCode(), "Refresh after logout should fail");
    }

    /**
     * Smoke test: offline token flow works with persistent sessions disabled.
     */
    @Test
    public void offlineTokenFlowWorks() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode(), "Offline password grant should succeed");

        RefreshToken parsedRefresh = oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, parsedRefresh.getType(), "Should be an offline token");

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(200, refreshResponse.getStatusCode(), "Offline refresh should succeed");

        LogoutResponse logoutResponse = oauth.doLogout(refreshResponse.getRefreshToken());
        assertTrue(logoutResponse.isSuccess(), "Logout should succeed");

        AccessTokenResponse afterLogout = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken());
        assertEquals(400, afterLogout.getStatusCode(), "Offline refresh after logout should fail");

        oauth.scope(null);
    }

    /**
     * Core race condition test for the volatile path: evict the offline session from cache,
     * then fire concurrent offline session lookups (each triggering a DB-load + import path)
     * while the main thread logs out. Without the fix, putIfAbsent would resurrect the
     * deleted session. Uses server-side getOfflineUserSession() to exercise the actual
     * offline DB fallback path with loading markers.
     */
    @Test
    public void logoutDuringConcurrentOfflineRefreshShouldNotResurrectSession() throws Exception {
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            LOG.infof("=== Iteration %d/%d ===", iteration + 1, ITERATIONS);

            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
            assertEquals(200, tokenResponse.getStatusCode(), "Password grant should succeed");
            String refreshToken = tokenResponse.getRefreshToken();
            assertNotNull(refreshToken);

            RefreshToken parsedRefresh = oauth.parseRefreshToken(refreshToken);
            String sessionId = parsedRefresh.getSessionId();
            assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, parsedRefresh.getType());

            String realmName = realm.getName();
            ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
            AtomicBoolean stopFlag = new AtomicBoolean(false);
            AtomicInteger successfulLookups = new AtomicInteger(0);
            CountDownLatch readyLatch = new CountDownLatch(CONCURRENT_THREADS);
            CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);
            List<Throwable> errors = new CopyOnWriteArrayList<>();

            for (int t = 0; t < CONCURRENT_THREADS; t++) {
                executor.submit(() -> {
                    try {
                        readyLatch.countDown();
                        while (!stopFlag.get()) {
                            try {
                                String found = runOnServer.fetchString((FetchOnServer) session -> {
                                    var realmModel = session.realms().getRealmByName(realmName);
                                    return session.sessions().getOfflineUserSession(realmModel, sessionId) != null ? "1" : null;
                                });
                                if ("1".equals(found)) {
                                    successfulLookups.incrementAndGet();
                                }
                            } catch (Exception e) {
                                // May fail after logout — expected
                            }
                        }
                        for (int i = 0; i < 3; i++) {
                            try {
                                runOnServer.run(session -> {
                                    var realmModel = session.realms().getRealmByName(realmName);
                                    session.sessions().getOfflineUserSession(realmModel, sessionId);
                                });
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

            assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Workers must be running before logout");
            // Give workers time to perform at least one successful lookup before cache eviction
            Thread.sleep(200);

            try {
                runOnServer.run(session -> {
                    Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                            session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
                    cache.remove(sessionId);
                    LOG.debugf("Evicted offline user session %s from cache to force DB loads", sessionId);
                });

                LogoutResponse logoutResponse = oauth.doLogout(refreshToken);
                assertTrue(logoutResponse.isSuccess(), "Logout should succeed");

                if (!errors.isEmpty()) {
                    errors.forEach(e -> LOG.error("Thread error", e));
                    assertTrue(errors.isEmpty(), "Unexpected errors in worker threads");
                }

                Thread.sleep(500);

                AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
                assertEquals(400, refreshResponse.getStatusCode(),
                        "Offline refresh should fail after logout — session must not be resurrected in cache");
            } finally {
                stopFlag.set(true);
                boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
                executor.shutdown();
                boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
                assertTrue(completed, "Workers should complete within timeout");
                assertTrue(terminated, "Executor should terminate");
            }

            assertTrue(successfulLookups.get() > 0,
                    "At least one worker must have performed a successful lookup before logout");
            LOG.infof("Successful lookups before/during logout: %d", successfulLookups.get());

            oauth.scope(null);
        }
    }

    /**
     * Verifies that a loading marker injected into the offline session cache is treated
     * as a cache miss, not as a real session. The offline session should still be loadable
     * from the database.
     */
    @Test
    public void loadingMarkerInOfflineCacheIsTreatedAsCacheMiss() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();

        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Offline refresh should succeed before injecting marker");

        String realmName = realm.getName();
        runOnServer.run(session -> {
            String realmId = session.realms().getRealmByName(realmName).getId();
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            UserSessionEntity entity = new UserSessionEntity(sessionId);
            entity.setRealmId(realmId);
            SessionEntityWrapper<UserSessionEntity> marker = SessionEntityWrapper.createLoadingMarker(entity);
            cache.put(sessionId, marker, 30, TimeUnit.SECONDS);
            LOG.debugf("Injected loading marker for offline user session %s", sessionId);
        });

        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Offline refresh should succeed — loading marker must be treated as cache miss, session loaded from DB");

        oauth.doLogout(refreshToken);
        oauth.scope(null);
    }

    /**
     * Verifies that a session note change on an offline session in the volatile path
     * is persisted to the database. After setting a note and clearing the cache,
     * reloading the session from DB should still have the note.
     */
    @Test
    public void offlineSessionNotePersistedToDatabase() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        // Set a session note on the offline session
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getOfflineUserSession(realmModel, sessionId);
            if (userSession == null) {
                throw new AssertionError("Offline user session should exist: " + sessionId);
            }
            userSession.setNote("test-note", "test-value");
        });

        // Clear the offline session cache to force DB load on next access
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class)
                            .getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
            LOG.debugf("Evicted offline user session %s from cache", sessionId);
        });

        // Reload the session — should come from DB. Check if the note survived.
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getOfflineUserSession(realmModel, sessionId);
            if (userSession == null) {
                throw new AssertionError("Offline user session should be loadable from DB after cache eviction: " + sessionId);
            }
            String noteValue = userSession.getNote("test-note");
            if (!"test-value".equals(noteValue)) {
                throw new AssertionError("Session note should be persisted to DB and survive cache eviction. " +
                        "Expected 'test-value', got: " + noteValue);
            }
        });

        oauth.doLogout(refreshToken);
        oauth.scope(null);
    }

    /**
     * When multiple readers hit a cache miss simultaneously (without any delete),
     * all should succeed. One reader's CAS replace succeeds; others find the data
     * already cached or fall back to loading without caching.
     */
    @Test
    public void concurrentOfflineReadersWithoutDeleteShouldAllSucceed() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        // Evict offline session from cache
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
            LOG.debugf("Evicted offline user session %s from cache", sessionId);
        });

        // Concurrent offline session lookups via independent server-side sessions
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(CONCURRENT_THREADS);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await(10, TimeUnit.SECONDS);
                    runOnServer.run(session -> {
                        var realmModel = session.realms().getRealmByName(realmName);
                        var offlineSession = session.sessions().getOfflineUserSession(realmModel, sessionId);
                        if (offlineSession == null) {
                            throw new AssertionError("Offline session should be loadable from DB: " + sessionId);
                        }
                    });
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(completed, "Workers should complete within timeout");
        assertTrue(terminated, "Executor should terminate");

        assertTrue(errors.isEmpty(), "All concurrent offline session lookups should succeed");

        // Session should still be usable
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, refreshResponse.getStatusCode(),
                "Offline refresh should succeed after concurrent reads");

        oauth.doLogout(refreshResponse.getRefreshToken());
        oauth.scope(null);
    }

    /**
     * Concurrent imports of the same offline session should not leave orphaned loading
     * markers in the client session cache. This exercises the client marker cleanup path
     * in importUserSession() when a user session CAS fails.
     */
    @Test
    public void noOrphanedClientSessionMarkersAfterConcurrentImport() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        // Evict offline user session and its client sessions from cache to force DB reimport
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> userCache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            userCache.remove(sessionId);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientCache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);
            clientCache.entrySet().removeIf(entry -> sessionId.equals(entry.getKey().userSessionId()));
            LOG.debugf("Evicted offline user session %s and its client sessions from cache", sessionId);
        });

        // Concurrent offline session lookups — each triggers importUserSession with client session markers
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(CONCURRENT_THREADS);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await(10, TimeUnit.SECONDS);
                    runOnServer.run(session -> {
                        var realmModel = session.realms().getRealmByName(realmName);
                        session.sessions().getOfflineUserSession(realmModel, sessionId);
                    });
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(completed, "Workers should complete within timeout");
        assertTrue(terminated, "Executor should terminate");

        assertTrue(errors.isEmpty(), "Concurrent imports should not error");

        // Verify no orphaned loading markers in the offline client session cache
        runOnServer.run(session -> {
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_CLIENT_SESSION_CACHE_NAME);
            for (var entry : cache.entrySet()) {
                if (entry.getKey().userSessionId().equals(sessionId) && entry.getValue().isLoadingMarker()) {
                    throw new AssertionError("Orphaned loading marker found for client session: " + entry.getKey());
                }
            }
        });

        // Session should still be usable
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, refreshResponse.getStatusCode());

        oauth.doLogout(refreshResponse.getRefreshToken());
        oauth.scope(null);
    }

    /**
     * Triggers a bulk admin API query for offline sessions while another thread is importing
     * the same sessions. Exercises the getOfflineUserSessionsStream() path with marker contention
     * from concurrent getUserSessionEntityFromCacheOrImportIfNecessary() calls.
     */
    @Test
    public void bulkOfflineSessionQueryWithConcurrentImport() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();
        String oauthClientId = "volatile-marker-test-client";

        // Get the client UUID for the admin API call
        String clientUUID = runOnServer.fetch(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            return realmModel.getClientByClientId(oauthClientId).getId();
        }, String.class);

        // Evict offline user session from cache
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
            LOG.debugf("Evicted offline user session %s from cache", sessionId);
        });

        // Fire concurrent bulk queries and direct lookups
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(CONCURRENT_THREADS);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            final int thread = t;
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await(10, TimeUnit.SECONDS);
                    if (thread % 2 == 0) {
                        user.admin().getOfflineSessions(clientUUID);
                    } else {
                        runOnServer.run(session -> {
                            var realmModel = session.realms().getRealmByName(realmName);
                            session.sessions().getOfflineUserSession(realmModel, sessionId);
                        });
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        boolean completed = doneLatch.await(30, TimeUnit.SECONDS);
        executor.shutdown();
        boolean terminated = executor.awaitTermination(10, TimeUnit.SECONDS);
        assertTrue(completed, "Workers should complete within timeout");
        assertTrue(terminated, "Executor should terminate");

        assertTrue(errors.isEmpty(), "Concurrent bulk queries and imports should not error");

        // Session should still be usable
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, refreshResponse.getStatusCode());

        oauth.doLogout(refreshResponse.getRefreshToken());
        oauth.scope(null);
    }

    /**
     * Bulk-query streams (getOfflineUserSessionsStream by user) bind sessions to the
     * transaction without importing into the Infinispan cache, to prevent resurrection
     * of concurrently deleted sessions. Changes made to such transaction-bound sessions
     * (e.g. setNote) must still be persisted to the database.
     */
    @Test
    public void bulkQueryBindsToTransactionAndPersistsChanges() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        // Evict the offline session from cache so the bulk query must load from DB
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class)
                            .getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
            LOG.debugf("Evicted offline user session %s from cache for bulk-query test", sessionId);
        });

        // Use the bulk-query stream to find the session and set a note on it
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var user = session.users().getUserByUsername(realmModel, "volatile-marker-test-user");
            var userSession = session.sessions().getOfflineUserSessionsStream(realmModel, user)
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("Bulk query should return the offline session: " + sessionId));
            userSession.setNote("bulk-query-note", "persisted-value");
        });

        // Evict again, then reload by ID to verify the note was persisted to the DB
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class)
                            .getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
        });

        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getOfflineUserSession(realmModel, sessionId);
            if (userSession == null) {
                throw new AssertionError("Offline session should be loadable from DB after cache eviction: " + sessionId);
            }
            String noteValue = userSession.getNote("bulk-query-note");
            if (!"persisted-value".equals(noteValue)) {
                throw new AssertionError("Note set via bulk-query stream should be persisted to DB. " +
                        "Expected 'persisted-value', got: " + noteValue);
            }
        });

        oauth.doLogout(refreshToken);
        oauth.scope(null);
    }

    /**
     * Place a loading marker with a very short TTL, wait for it to expire, then access
     * the session. The expired marker means the cache slot is empty, so a new reader can
     * place a fresh marker and load from DB. This simulates recovery after a reader crashes
     * without consuming its marker.
     */
    @Test
    public void loadingMarkerExpiryRecovery() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        // Place a loading marker with a very short TTL (1 second)
        runOnServer.run(session -> {
            String realmId = session.realms().getRealmByName(realmName).getId();
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.OFFLINE_USER_SESSION_CACHE_NAME);
            UserSessionEntity entity = new UserSessionEntity(sessionId);
            entity.setRealmId(realmId);
            SessionEntityWrapper<UserSessionEntity> marker = SessionEntityWrapper.createLoadingMarker(entity);
            cache.put(sessionId, marker, 1, TimeUnit.SECONDS);
            LOG.debugf("Injected loading marker with 1s TTL for offline session %s", sessionId);
        });

        // Wait for the marker to expire
        Thread.sleep(1500);

        // Session should still be loadable — expired marker means cache slot is empty,
        // new reader can place a fresh marker and load from DB
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, refreshResponse.getStatusCode(),
                "Offline refresh should succeed — expired marker should not prevent session recovery");

        oauth.doLogout(refreshResponse.getRefreshToken());
        oauth.scope(null);
    }

    public static class VolatileSessionServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.featuresDisabled(Profile.Feature.PERSISTENT_USER_SESSIONS);
        }
    }

    public static class TestUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("volatile-marker-test-user")
                    .password("password")
                    .email("volatile-marker-test@localhost")
                    .name("Volatile", "Test");
        }
    }

    public static class OfflineGrantClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("volatile-marker-test-client")
                    .secret("secret")
                    .directAccessGrantsEnabled(true)
                    .redirectUris("http://localhost:8080/*");
        }
    }
}
