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
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.OAuth2Constants;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.PersistentUserSessionProvider;
import org.keycloak.models.sessions.infinispan.UserSessionAdapter;
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
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.providers.session.BlockingUserSessionPersisterProvider;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for GH issue #51127: concurrent cache reads during logout can
 * resurrect a deleted user session via putIfAbsent. The loading marker + CAS fix
 * prevents this by ensuring a reader's CAS replace fails when a concurrent delete
 * has removed the marker from the cache.
 */
@KeycloakIntegrationTest(config = SessionResurrectionConcurrencyTest.SessionCachingServerConfig.class)
@DatabaseTest
public class SessionResurrectionConcurrencyTest {

    private static final Logger LOG = Logger.getLogger(SessionResurrectionConcurrencyTest.class);

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

    /**
     * Core race condition test: evict the session from cache, then fire concurrent
     * userinfo requests (each triggering a DB-load + putIfAbsent path) while the main
     * thread logs out. Without the fix, putIfAbsent would resurrect the deleted session.
     */
    @Test
    public void logoutDuringConcurrentUserInfoShouldNotResurrectSession() throws Exception {
        for (int iteration = 0; iteration < ITERATIONS; iteration++) {
            LOG.infof("=== Iteration %d/%d ===", iteration + 1, ITERATIONS);

            AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
            assertEquals(200, tokenResponse.getStatusCode(), "Password grant should succeed");
            String accessToken = tokenResponse.getAccessToken();
            String refreshToken = tokenResponse.getRefreshToken();
            assertNotNull(accessToken);
            assertNotNull(refreshToken);

            RefreshToken parsedRefresh = oauth.parseRefreshToken(refreshToken);
            String sessionId = parsedRefresh.getSessionId();

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
                                if (oauth.doUserInfoRequest(accessToken).isSuccess()) {
                                    successfulLookups.incrementAndGet();
                                }
                            } catch (Exception e) {
                                // UserInfo may fail after logout — expected
                            }
                        }
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

            assertTrue(readyLatch.await(10, TimeUnit.SECONDS), "Workers must be running before logout");
            // Give workers time to perform at least one successful lookup before cache eviction
            Thread.sleep(200);

            try {
                runOnServer.run(session -> {
                    Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                            session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
                    cache.remove(sessionId);
                    LOG.debugf("Evicted user session %s from cache to force DB loads", sessionId);
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
                        "Refresh should fail after logout — session must not be resurrected in cache");

                List<UserSessionRepresentation> sessions = user.admin().getUserSessions();
                boolean sessionStillExists = sessions.stream().anyMatch(s -> s.getId().equals(sessionId));
                assertFalse(sessionStillExists,
                        "Session " + sessionId + " must not be resurrected after logout");
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
        }
    }

    @Test
    public void staleDatabaseLoadCannotResurrectSessionAfterLogout() throws Exception {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode(), "Password grant should succeed");
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();

        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = session.getProvider(InfinispanConnectionProvider.class)
                    .getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
            BlockingUserSessionPersisterProvider.blockAfterLoading(sessionId);
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> inFlightRequest = executor.submit(() -> oauth.doUserInfoRequest(accessToken).isSuccess());
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean loaded;
            do {
                loaded = runOnServer.fetch(session -> BlockingUserSessionPersisterProvider.hasLoaded(), Boolean.class);
                if (!loaded) {
                    Thread.sleep(10);
                }
            } while (!loaded && System.nanoTime() < deadline);
            assertTrue(loaded, "UserInfo must pause after loading the stale database snapshot");

            LogoutResponse logoutResponse = oauth.doLogout(refreshToken);
            assertTrue(logoutResponse.isSuccess(), "Logout should complete while the stale reader is paused");

            runOnServer.run(session -> BlockingUserSessionPersisterProvider.release());
            inFlightRequest.get(10, TimeUnit.SECONDS);

            AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(400, refreshResponse.getStatusCode(),
                    "A request starting after logout must not observe a resurrected session");

            runOnServer.run(session -> {
                Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = session.getProvider(InfinispanConnectionProvider.class)
                        .getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
                SessionEntityWrapper<UserSessionEntity> cached = cache.get(sessionId);
                if (cached != null) {
                    throw new AssertionError("The stale database snapshot was published after logout: " + cached);
                }
            });
        } finally {
            runOnServer.run(session -> BlockingUserSessionPersisterProvider.release());
            executor.shutdownNow();
        }
    }

    @Test
    public void clientUpdateRevokesConcurrentParentLoad() throws Exception {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode(), "Password grant should succeed");
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        runOnServer.run(session -> {
            var infinispan = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> userCache =
                    infinispan.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientCache =
                    infinispan.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            var client = session.realms().getRealmByName(realmName).getClientByClientId("marker-test-client");
            userCache.remove(sessionId);
            clientCache.remove(new EmbeddedClientSessionKey(sessionId, client.getId()));
            BlockingUserSessionPersisterProvider.blockAfterLoading(sessionId);
        });

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<Boolean> inFlightRequest = executor.submit(() -> oauth.doUserInfoRequest(accessToken).isSuccess());
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            boolean loaded;
            do {
                loaded = runOnServer.fetch(session -> BlockingUserSessionPersisterProvider.hasLoaded(), Boolean.class);
                if (!loaded) {
                    Thread.sleep(10);
                }
            } while (!loaded && System.nanoTime() < deadline);
            assertTrue(loaded, "UserInfo must pause after loading the parent and client database snapshots");

            // Client-session timestamps have second precision. Ensure refresh produces an
            // effective client mutation instead of being coalesced with session creation.
            Thread.sleep(1_100);
            AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
            assertEquals(200, refreshResponse.getStatusCode(), "Concurrent refresh should succeed");

            assertUserSessionNotCached(sessionId,
                    "The committed client update must revoke the concurrent parent loading marker");

            runOnServer.run(session -> BlockingUserSessionPersisterProvider.release());
            inFlightRequest.get(10, TimeUnit.SECONDS);

            assertUserSessionNotCached(sessionId,
                    "The stale parent snapshot must remain request-local after losing publication ownership");
            runOnServer.run(session -> {
                var infinispan = session.getProvider(InfinispanConnectionProvider.class);
                Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientCache =
                        infinispan.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
                var client = session.realms().getRealmByName(realmName).getClientByClientId("marker-test-client");
                var cached = clientCache.get(new EmbeddedClientSessionKey(sessionId, client.getId()));
                if (cached != null) {
                    throw new AssertionError("The stale preloaded client snapshot was published after the parent lost ownership: " + cached);
                }
            });
            assertEquals(200, oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken()).getStatusCode(),
                    "A subsequent request must reconstruct the session from committed database state");
        } finally {
            runOnServer.run(session -> BlockingUserSessionPersisterProvider.release());
            executor.shutdownNow();
        }
    }

    @Test
    public void databaseBackedCreationPublishesWarmCacheEntries() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode(), "Password grant should succeed");
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        runOnServer.run(session -> {
            var infinispan = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> userCache =
                    infinispan.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            SessionEntityWrapper<UserSessionEntity> userSession = userCache.get(sessionId);
            if (userSession == null || userSession.isLoadingMarker()) {
                throw new AssertionError("DB-backed user-session creation did not publish a complete cache entry");
            }

            var client = session.realms().getRealmByName(realmName).getClientByClientId("marker-test-client");
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientCache =
                    infinispan.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            SessionEntityWrapper<AuthenticatedClientSessionEntity> clientSession =
                    clientCache.get(new EmbeddedClientSessionKey(sessionId, client.getId()));
            if (clientSession == null || clientSession.isLoadingMarker()) {
                throw new AssertionError("DB-backed client-session creation did not publish a complete cache entry");
            }
        });

        oauth.doLogout(refreshToken);
    }

    @Test
    public void databaseLoadPublishesWarmCacheEntriesWhenPersistenceContextIsEligible() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode(), "Password grant should succeed");
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        runOnServer.run(session -> {
            var infinispan = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> userCache =
                    infinispan.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientCache =
                    infinispan.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            var client = session.realms().getRealmByName(realmName).getClientByClientId("marker-test-client");
            userCache.remove(sessionId);
            clientCache.remove(new EmbeddedClientSessionKey(sessionId, client.getId()));
        });

        assertTrue(oauth.doUserInfoRequest(accessToken).isSuccess(),
                "UserInfo should reload the evicted session from the database");

        runOnServer.run(session -> {
            var infinispan = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> userCache =
                    infinispan.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            SessionEntityWrapper<UserSessionEntity> userSession = userCache.get(sessionId);
            if (userSession == null || userSession.isLoadingMarker()) {
                throw new AssertionError("Eligible database load did not publish a complete user-session cache entry");
            }

            var client = session.realms().getRealmByName(realmName).getClientByClientId("marker-test-client");
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientCache =
                    infinispan.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            SessionEntityWrapper<AuthenticatedClientSessionEntity> clientSession =
                    clientCache.get(new EmbeddedClientSessionKey(sessionId, client.getId()));
            if (clientSession == null || clientSession.isLoadingMarker()) {
                throw new AssertionError("Eligible database load did not publish a complete client-session cache entry");
            }
        });

        oauth.doLogout(refreshToken);
    }

    private void assertUserSessionNotCached(String sessionId, String message) {
        boolean cached = runOnServer.fetch(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = session.getProvider(InfinispanConnectionProvider.class)
                    .getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            return cache.containsKey(sessionId);
        }, Boolean.class);
        assertFalse(cached, message);
    }

    @Test
    public void newSessionsAreCachedAfterCreation() {
        assumePersistentSessions();
        AccessTokenResponse response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        String sessionId = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String realmName = realm.getName();

        runOnServer.run(session -> {
            var infinispan = session.getProvider(InfinispanConnectionProvider.class);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> userCache = infinispan.getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            var parent = userCache.get(sessionId);
            if (parent == null || parent.isLoadingMarker()) {
                throw new AssertionError("A newly inserted user session must be cached after commit");
            }
            var client = session.realms().getRealmByName(realmName).getClientByClientId("marker-test-client");
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> clientCache = infinispan.getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            var child = clientCache.get(new EmbeddedClientSessionKey(sessionId, client.getId()));
            if (child == null || child.isLoadingMarker()) {
                throw new AssertionError("A newly inserted client session must be cached after commit");
            }
        });
        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void duplicateCreationWithOwnedMarkerDoesNotCacheIncompleteSession() {
        assumePersistentSessions();
        AccessTokenResponse response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        String sessionId = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String realmName = realm.getName();

        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            session.sessions().getUserSession(realmModel, sessionId).setNote("original-note", "retained");
        });

        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var persisted = session.getProvider(UserSessionPersisterProvider.class).loadUserSession(realmModel, sessionId, false);
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache = session.getProvider(InfinispanConnectionProvider.class)
                    .getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
            // Model a delayed parallel login after eviction of the first login's cache entry.
            var duplicate = session.sessions().createUserSession(sessionId, realmModel, persisted.getUser(), persisted.getLoginUsername(),
                    persisted.getIpAddress(), persisted.getAuthMethod(), persisted.isRememberMe(), null, null,
                    UserSessionModel.SessionPersistenceState.PERSISTENT);
            // Preserve the original login's start time without depending on test execution speed.
            ((UserSessionAdapter<?>) duplicate).getEntity().setStarted(persisted.getStarted());
            duplicate.setNote("duplicate-note", "added");
            var marker = cache.get(sessionId);
            if (marker == null || !marker.isLoadingMarker()) {
                throw new AssertionError("Duplicate creation must own a creation marker to exercise this race");
            }
        });

        assertUserSessionNotCached(sessionId, "Creation merged into an existing row must invalidate even its own marker");
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var loaded = session.sessions().getUserSession(realmModel, sessionId);
            if (!"retained".equals(loaded.getNote("original-note")) || !"added".equals(loaded.getNote("duplicate-note"))) {
                throw new AssertionError("Reload must retain both the original and merged login notes");
            }
            var client = realmModel.getClientByClientId("marker-test-client");
            if (loaded.getAuthenticatedClientSessionByClient(client.getId()) == null) {
                throw new AssertionError("Reload must retain the original login's client association");
            }
        });
        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void requestLocalParentLoadPreservesPendingClientUpdate() {
        assertRequestLocalParentLoadPreservesPendingClientTask(false);
    }

    @Test
    public void requestLocalParentLoadPreservesPendingClientRemoval() {
        assertRequestLocalParentLoadPreservesPendingClientTask(true);
    }

    private void assertRequestLocalParentLoadPreservesPendingClientTask(boolean remove) {
        assumePersistentSessions();
        AccessTokenResponse response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        String sessionId = oauth.parseRefreshToken(response.getRefreshToken()).getSessionId();
        String realmName = realm.getName();

        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var client = realmModel.getClientByClientId("marker-test-client");
            var persisted = session.getProvider(UserSessionPersisterProvider.class).loadUserSession(realmModel, sessionId, false);
            // Load and mutate the child before the parent is bound to the session transaction.
            var child = session.sessions().getClientSession(persisted, client, false);
            if (remove) {
                child.detachFromUserSession();
            } else {
                child.setNote("pending-note", "retained");
            }
            session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME).remove(sessionId);
            // The managed parent makes this a request-local load, including its preloaded children.
            var parent = session.sessions().getUserSession(realmModel, sessionId);
            var reloadedChild = session.sessions().getClientSession(parent, client, false);
            if (remove ? reloadedChild != null : reloadedChild == null || !"retained".equals(reloadedChild.getNote("pending-note"))) {
                throw new AssertionError("Binding a preloaded child must preserve its pending update or removal");
            }
        });

        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var client = realmModel.getClientByClientId("marker-test-client");
            var persister = session.getProvider(UserSessionPersisterProvider.class);
            var parent = persister.loadUserSession(realmModel, sessionId, false);
            var child = persister.loadClientSession(realmModel, client, parent, false);
            if (remove ? child != null : child == null || !"retained".equals(child.getNote("pending-note"))) {
                throw new AssertionError("The preserved child task must be committed to the database");
            }
        });
        oauth.doLogout(response.getRefreshToken());
    }

    private void assumePersistentSessions() {
        Assumptions.assumeTrue(runOnServer.fetch(session -> session.sessions() instanceof PersistentUserSessionProvider, Boolean.class),
                "Requires persistent user sessions");
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void realmCleanupRemovesUnlistedChildrenAndFencesPublication(boolean offline) {
        assumePersistentSessions();
        runOnServer.run(new RealmSessionCacheRemoval(offline, true));
    }

    /**
     * Verifies that a loading marker in the user session cache is treated as a cache miss,
     * not as a real session. The session should still be loadable from the database.
     */
    @Test
    public void loadingMarkerInCacheIsTreatedAsCacheMiss() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();

        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh should succeed before injecting marker");

        String realmName = realm.getName();
        runOnServer.run(session -> {
            String realmId = session.realms().getRealmByName(realmName).getId();
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            UserSessionEntity entity = new UserSessionEntity(sessionId);
            entity.setRealmId(realmId);
            SessionEntityWrapper<UserSessionEntity> marker = SessionEntityWrapper.createLoadingMarker(entity);
            cache.put(sessionId, marker, 30, TimeUnit.SECONDS);
            LOG.debugf("Injected loading marker for user session %s", sessionId);
        });

        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh should succeed — loading marker must be treated as cache miss, session loaded from DB");

        oauth.doLogout(refreshToken);
    }

    /**
     * When multiple readers hit a cache miss simultaneously (no delete happening),
     * all should succeed. One reader's CAS replace succeeds; others find the data
     * already cached and use it.
     */
    @Test
    public void concurrentReadersWithoutDeleteShouldAllSucceed() throws Exception {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String accessToken = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();

        // Evict user session to force all readers to hit DB
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
            LOG.debugf("Evicted user session %s from cache", sessionId);
        });

        // Fire concurrent userinfo requests — all hit cache miss simultaneously
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
        CountDownLatch startLatch = new CountDownLatch(CONCURRENT_THREADS);
        CountDownLatch doneLatch = new CountDownLatch(CONCURRENT_THREADS);
        List<Throwable> errors = new CopyOnWriteArrayList<>();

        for (int t = 0; t < CONCURRENT_THREADS; t++) {
            executor.submit(() -> {
                try {
                    startLatch.countDown();
                    startLatch.await(10, TimeUnit.SECONDS);
                    assertTrue(oauth.doUserInfoRequest(accessToken).isSuccess(),
                            "UserInfo should succeed during concurrent reads");
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

        assertTrue(errors.isEmpty(), "No errors expected during concurrent reads");

        // Session should still work after concurrent reads
        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh should succeed — session must be correctly recovered from DB");

        oauth.doLogout(refreshToken);
    }

    /**
     * Verifies that a session note update is persisted to the database.
     * After setting a note and clearing the cache, reloading the session
     * from DB should still have the note.
     */
    @Test
    public void sessionNoteUpdatePersistedToDatabase() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        // Set a session note
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(realmModel, sessionId);
            if (userSession == null) {
                throw new AssertionError("User session should exist: " + sessionId);
            }
            userSession.setNote("test-note", "test-value");
        });

        // Evict from cache to force DB load
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            cache.remove(sessionId);
            LOG.debugf("Evicted user session %s from cache", sessionId);
        });

        // Reload session — should come from DB with the note
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(realmModel, sessionId);
            if (userSession == null) {
                throw new AssertionError("User session should be loadable from DB after cache eviction: " + sessionId);
            }
            String noteValue = userSession.getNote("test-note");
            if (!"test-value".equals(noteValue)) {
                throw new AssertionError("Session note should be persisted to DB and survive cache eviction. " +
                        "Expected 'test-value', got: " + noteValue);
            }
        });

        oauth.doLogout(refreshToken);
    }

    /**
     * Verifies that evicting a client session from cache doesn't break the session.
     * The client session should be recoverable from the database.
     */
    @Test
    public void clientSessionEvictionRecovery() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();

        // Verify refresh works before eviction
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, refreshResponse.getStatusCode(), "Refresh should succeed before eviction");
        refreshToken = refreshResponse.getRefreshToken();

        // Evict the client session from cache
        String realmName = realm.getName();
        String oauthClientId = "marker-test-client";
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var client = realmModel.getClientByClientId(oauthClientId);
            Cache<EmbeddedClientSessionKey, SessionEntityWrapper<AuthenticatedClientSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.CLIENT_SESSION_CACHE_NAME);
            EmbeddedClientSessionKey key = new EmbeddedClientSessionKey(sessionId, client.getId());
            cache.remove(key);
            LOG.debugf("Evicted client session for userSession=%s client=%s", sessionId, client.getId());
        });

        // Refresh should still work — client session recovered from DB
        refreshResponse = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(200, refreshResponse.getStatusCode(),
                "Refresh should succeed — client session must be recoverable from DB after cache eviction");

        oauth.doLogout(refreshResponse.getRefreshToken());
    }

    /**
     * Queues a session REPLACE update, then injects a loading marker into the cache.
     * When the transaction commits, the REPLACE CAS fails (version mismatch with marker),
     * and post-commit conflict handling invalidates the key unconditionally to prevent
     * the marker owner from publishing stale pre-update data.
     */
    @Test
    public void replaceOnLoadingMarkerInvalidatesMarker() {
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, tokenResponse.getStatusCode());
        String refreshToken = tokenResponse.getRefreshToken();
        String sessionId = oauth.parseRefreshToken(refreshToken).getSessionId();
        String realmName = realm.getName();

        // Inside a single server-side transaction:
        // 1. Look up the session (adds to transaction with version V1)
        // 2. Set a note (queues a REPLACE update task)
        // 3. Overwrite the cache entry with a loading marker (version V2)
        // When the transaction commits, the REPLACE CAS fails (V1 != V2),
        // post-commit publication detects the changed UUID and invalidates it.
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(realmModel, sessionId);
            if (userSession == null) {
                throw new AssertionError("Session should exist: " + sessionId);
            }
            userSession.setNote("test-replace", "value");

            String realmId = realmModel.getId();
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            UserSessionEntity entity = new UserSessionEntity(sessionId);
            entity.setRealmId(realmId);
            SessionEntityWrapper<UserSessionEntity> marker = SessionEntityWrapper.createLoadingMarker(entity);
            cache.put(sessionId, marker, 30, TimeUnit.SECONDS);
            LOG.debugf("Injected loading marker for session %s before transaction commit", sessionId);
        });

        // Verify the marker was invalidated instead of receiving the committed update.
        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> verifyCache =
                    session.getProvider(InfinispanConnectionProvider.class)
                            .getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            SessionEntityWrapper<UserSessionEntity> cached = verifyCache.get(sessionId);
            if (cached != null && cached.isLoadingMarker()) {
                throw new AssertionError("Loading marker should have been invalidated by REPLACE, but it is still in cache");
            }
        });

        // Session should still be loadable from DB after marker invalidation
        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh should succeed — session is reloaded from DB after marker invalidation");

        oauth.doLogout(refreshToken);
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
            var sessionUser = session.users().getUserByUsername(realmModel, "marker-test-user");
            var userSession = session.sessions().getOfflineUserSessionsStream(realmModel, sessionUser)
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

    public static class SessionCachingServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.spiOption("user-sessions", "infinispan", "use-caches", "true")
                    .dependency("org.keycloak.tests", "keycloak-tests-custom-providers", true)
                    .option("spi-user-session-persister--provider", "blocking-jpa");
        }
    }

    public static class TestUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("marker-test-user")
                    .password("password")
                    .email("marker-test@localhost")
                    .name("Marker", "Test");
        }
    }

    public static class DirectGrantClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("marker-test-client")
                    .secret("secret")
                    .directAccessGrantsEnabled(true)
                    .redirectUris("http://localhost:8080/*");
        }
    }
}
