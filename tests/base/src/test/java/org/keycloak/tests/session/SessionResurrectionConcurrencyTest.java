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

import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.UserSessionProvider;
import org.keycloak.models.sessions.infinispan.InfinispanUserSessionProviderFactory;
import org.keycloak.models.sessions.infinispan.changes.SessionEntityWrapper;
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

            readyLatch.await(10, TimeUnit.SECONDS);

            runOnServer.run(session -> {
                Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                        session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
                cache.remove(sessionId);
                LOG.debugf("Evicted user session %s from cache to force DB loads", sessionId);
            });

            LogoutResponse logoutResponse = oauth.doLogout(refreshToken);
            assertTrue(logoutResponse.isSuccess(), "Logout should succeed");
            stopFlag.set(true);

            doneLatch.await(30, TimeUnit.SECONDS);
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

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
        }
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

        runOnServer.run(session -> {
            Cache<String, SessionEntityWrapper<UserSessionEntity>> cache =
                    session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.USER_SESSION_CACHE_NAME);
            UserSessionEntity entity = new UserSessionEntity(sessionId);
            entity.setRealmId("dummy");
            SessionEntityWrapper<UserSessionEntity> marker = SessionEntityWrapper.createLoadingMarker(entity);
            cache.put(sessionId, marker, 30, TimeUnit.SECONDS);
            LOG.debugf("Injected loading marker for user session %s", sessionId);
        });

        assertEquals(200, oauth.doRefreshTokenRequest(refreshToken).getStatusCode(),
                "Refresh should succeed — loading marker must be treated as cache miss, session loaded from DB");

        oauth.doLogout(refreshToken);
    }

    public static class SessionCachingServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.spiOption("user-sessions", "infinispan", "use-caches", "true");
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
