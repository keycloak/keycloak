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
package org.keycloak.tests.oauth;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.keycloak.OAuthErrorException;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.AbstractKeycloakTransaction;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.refresh.AbstractRefreshTokenProvider;
import org.keycloak.protocol.oidc.refresh.DefaultRefreshTokenProvider;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.common.TestRealmUserConfig;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest(config = RefreshTokenReuseRollbackTest.AfterCompletionDatabaseWrites.class)
public class RefreshTokenReuseRollbackTest {

    public static class AfterCompletionDatabaseWrites implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.spiOption("infinispan-transactions", "default",
                    "write-session-updates-in-main-transaction", "false");
        }
    }

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectRealm(config = RefreshTokenTest.RefreshTokenTestRealmConfig.class)
    ManagedRealm realm;

    @InjectUser(config = TestRealmUserConfig.class)
    ManagedUser user;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.oauth")
    RunOnServerClient runOnServer;

    @Test
    public void rollbackRestoresRefreshTokenReuseCount() {
        realm.updateWithCleanup(r -> r.revokeRefreshToken(true).refreshTokenMaxReuse(1));

        String code = loginAndGetCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertEquals(200, response.getStatusCode());

        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        String realmName = realm.getName();
        String userSessionId = refreshToken.getSessionId();
        String reuseKey = new TokenManager().getReuseIdKey(refreshToken);

        runOnServer.run(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            assertEquals(0, clientSession.getRefreshTokenUseCount(reuseKey));

            invokeReuseCounterHelper("incrementTokenReuseCount", session, clientSession, reuseKey);
            invokeReuseCounterHelper("incrementTokenReuseCount", session, clientSession, reuseKey);
            assertEquals(2, clientSession.getRefreshTokenUseCount(reuseKey));
            session.getTransactionManager().setRollbackOnly();
        });

        Integer countAfterRollback = runOnServer.fetch(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            return clientSession.getRefreshTokenUseCount(reuseKey);
        }, Integer.class);

        assertEquals(0, countAfterRollback,
                "A rolled-back refresh must not consume token reuse allowance");

        // A new token id resets the counter during validation. Rollback must restore the
        // value from before validation, even if the same transaction changes it again.
        runOnServer.run(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            clientSession.setRefreshTokenUseCount(reuseKey, 1);
            clientSession.setRefreshToken(reuseKey, "previous-token-id");
        });

        runOnServer.run(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            assertEquals(1, clientSession.getRefreshTokenUseCount(reuseKey));

            invokeReuseCounterHelper("captureTokenReuseState", session, clientSession, reuseKey);
            var nextToken = new RefreshToken(new AccessToken(), null, null);
            nextToken.id("next-token-id");
            nextToken.iat(System.currentTimeMillis() / 1000 + 3600);
            nextToken.getOtherClaims().put(Constants.REUSE_ID, reuseKey);
            try {
                new TokenManager().validateTokenReuse(session, testRealm, nextToken, clientSession, true);
            } catch (OAuthErrorException e) {
                throw new AssertionError("Fresh token should reset the prior reuse count", e);
            }
            assertEquals(0, clientSession.getRefreshTokenUseCount(reuseKey));
            invokeReuseCounterHelper("incrementTokenReuseCount", session, clientSession, reuseKey);
            clientSession.setRefreshTokenUseCount(reuseKey, 2);
            invokeReuseCounterHelper("recordTokenReuseState", session, clientSession, reuseKey);
            session.getTransactionManager().setRollbackOnly();
        });

        countAfterRollback = runOnServer.fetch(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            return clientSession.getRefreshTokenUseCount(reuseKey);
        }, Integer.class);

        assertEquals(1, countAfterRollback,
                "Rollback must restore the count from before validation reset it");
    }

    @Test
    public void rollbackRestoresRotationStateBeforeRetry() {
        realm.updateWithCleanup(r -> r.revokeRefreshToken(true).refreshTokenMaxReuse(0));

        String code = loginAndGetCode();
        AccessTokenResponse initial = oauth.doAccessTokenRequest(code);
        assertEquals(200, initial.getStatusCode());
        RefreshToken rt0 = oauth.parseRefreshToken(initial.getRefreshToken());
        AccessTokenResponse firstRefresh = oauth.doRefreshTokenRequest(initial.getRefreshToken());
        assertEquals(200, firstRefresh.getStatusCode());
        RefreshToken rt1 = oauth.parseRefreshToken(firstRefresh.getRefreshToken());

        String realmName = realm.getName();
        String userSessionId = rt0.getSessionId();
        String reuseKey = new TokenManager().getReuseIdKey(rt1);
        String rt0Id = rt0.getId();
        String rt1Id = rt1.getId();
        int rt1IssuedAt = rt1.getIat().intValue();

        runOnServer.run(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            assertEquals(rt0Id, clientSession.getRefreshToken(reuseKey));
            assertEquals(1, clientSession.getRefreshTokenUseCount(reuseKey));
            assertEquals(rt1Id, clientSession.getLatestGeneratedRefreshToken(reuseKey));
            assertEquals(rt1IssuedAt, clientSession.getRefreshTokenLastRefresh(reuseKey));

            invokeReuseCounterHelper("captureTokenReuseState", session, clientSession, reuseKey);
            var retryToken = new RefreshToken(new AccessToken(), null, null);
            retryToken.id(rt1Id);
            retryToken.iat((long) rt1IssuedAt);
            retryToken.getOtherClaims().put(Constants.REUSE_ID, reuseKey);
            try {
                new TokenManager().validateTokenReuse(session, testRealm, retryToken, clientSession, true);
            } catch (OAuthErrorException e) {
                throw new AssertionError("The first RT1 refresh should be valid", e);
            }
            invokeReuseCounterHelper("incrementTokenReuseCount", session, clientSession, reuseKey);
            clientSession.setLatestGeneratedRefreshToken(reuseKey, "synthetic-rt2");
            clientSession.setRefreshTokenLastRefresh(reuseKey, rt1IssuedAt + 1);
            invokeReuseCounterHelper("recordTokenReuseState", session, clientSession, reuseKey);
            session.getTransactionManager().setRollbackOnly();
        });

        String stateAfterRollback = runOnServer.fetch(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            return String.join("|", clientSession.getRefreshToken(reuseKey),
                    String.valueOf(clientSession.getRefreshTokenUseCount(reuseKey)),
                    clientSession.getLatestGeneratedRefreshToken(reuseKey),
                    String.valueOf(clientSession.getRefreshTokenLastRefresh(reuseKey)));
        }, String.class);
        AccessTokenResponse retry = oauth.doRefreshTokenRequest(firstRefresh.getRefreshToken());
        assertEquals(200, retry.getStatusCode(), "RT1 must remain retryable after rollback");
        assertEquals(String.join("|", rt0Id, "1", rt1Id, String.valueOf(rt1IssuedAt)),
                stateAfterRollback, "Rollback must restore the complete refresh-token rotation state");
    }

    @Test
    @DatabaseTest
    public void postgresCommitFailureRestoresRotationStateBeforeRetry() {
        String databaseProduct = runOnServer.fetch(session -> session.getProvider(JpaConnectionProvider.class)
                .getEntityManager().unwrap(org.hibernate.Session.class)
                .doReturningWork(connection -> connection.getMetaData().getDatabaseProductName()), String.class);
        Assumptions.assumeTrue("PostgreSQL".equals(databaseProduct));
        Boolean writesInMainTransaction = runOnServer.fetch(session -> org.keycloak.Config
                .scope("infinispanTransactions", "default")
                .getBoolean("writeSessionUpdatesInMainTransaction", true), Boolean.class);
        assertEquals(false, writesInMainTransaction,
                "This test must exercise DB writes in the after-completion phase");

        realm.updateWithCleanup(r -> r.revokeRefreshToken(true).refreshTokenMaxReuse(0));
        String code = loginAndGetCode();
        AccessTokenResponse initial = oauth.doAccessTokenRequest(code);
        assertEquals(200, initial.getStatusCode());
        RefreshToken rt0 = oauth.parseRefreshToken(initial.getRefreshToken());
        AccessTokenResponse firstRefresh = oauth.doRefreshTokenRequest(initial.getRefreshToken());
        assertEquals(200, firstRefresh.getStatusCode());
        RefreshToken rt1 = oauth.parseRefreshToken(firstRefresh.getRefreshToken());

        String userSessionId = rt0.getSessionId();
        if (!userSessionId.matches("[A-Za-z0-9_-]{20,64}")) {
            throw new AssertionError("Unexpected session ID format for SQL trigger test");
        }
        String reuseKey = new TokenManager().getReuseIdKey(rt1);
        String rt0Id = rt0.getId();
        String rt1Id = rt1.getId();
        int rt1IssuedAt = rt1.getIat().intValue();
        String expectedState = String.join("|", rt0Id, "1", rt1Id, String.valueOf(rt1IssuedAt));

        String databaseBefore = readDatabaseClientSession(userSessionId);
        assertEquals(expectedState, readRotationState(realm.getName(), userSessionId, reuseKey));

        runOnServer.run(session -> {
            var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            em.createNativeQuery("CREATE OR REPLACE FUNCTION kc_51749_fail_client_session_update() "
                    + "RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN "
                    + "RAISE EXCEPTION 'KC51749 injected PostgreSQL write failure'; END $$").executeUpdate();
            em.createNativeQuery("CREATE TRIGGER kc_51749_fail_client_session_update "
                    + "BEFORE UPDATE ON OFFLINE_CLIENT_SESSION FOR EACH ROW "
                    + "WHEN (NEW.USER_SESSION_ID = '" + userSessionId + "' "
                    + "AND NEW.DATA IS DISTINCT FROM OLD.DATA) "
                    + "EXECUTE FUNCTION kc_51749_fail_client_session_update()").executeUpdate();
        });
        AccessTokenResponse failedRefresh;
        try {
            failedRefresh = oauth.doRefreshTokenRequest(firstRefresh.getRefreshToken());
        } finally {
            runOnServer.run(session -> {
                var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                em.createNativeQuery("DROP TRIGGER IF EXISTS kc_51749_fail_client_session_update "
                        + "ON OFFLINE_CLIENT_SESSION").executeUpdate();
                em.createNativeQuery("DROP FUNCTION IF EXISTS kc_51749_fail_client_session_update()").executeUpdate();
            });
        }

        assertEquals(500, failedRefresh.getStatusCode());
        assertEquals(databaseBefore, readDatabaseClientSession(userSessionId),
                "The failed PostgreSQL update must not leave a partial durable session");
        assertEquals(expectedState, readRotationState(realm.getName(), userSessionId, reuseKey),
                "The cache must match the durable rotation state after DB commit failure");
        AccessTokenResponse retry = oauth.doRefreshTokenRequest(firstRefresh.getRefreshToken());
        assertEquals(200, retry.getStatusCode(), "RT1 must remain retryable after DB commit failure");
    }

    @Test
    public void afterCompletionFailurePreservesUnrelatedCommittedCallback() {
        String realmName = realm.getName();
        String mainMarker = "kc51749-main-committed";
        String callbackMarker = "kc51749-unrelated-callback";

        runOnServer.run(session -> commitWithFailingAfterCompletion(session, realmName, mainMarker, callbackMarker));

        String observed = runOnServer.fetch(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            return testRealm.getAttribute(mainMarker) + "|" + testRealm.getAttribute(callbackMarker);
        }, String.class);
        assertEquals("committed|committed", observed,
                "An after-completion failure must not roll back unrelated post-commit callbacks");
    }

    private static void commitWithFailingAfterCompletion(KeycloakSession session, String realmName,
                                                          String mainMarker, String callbackMarker) {
        session.realms().getRealmByName(realmName).setAttribute(mainMarker, "committed");
        var factory = session.getKeycloakSessionFactory();
        session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
            @Override
            protected void commitImpl() {
                throw new IllegalStateException("KC51749 injected after-completion failure");
            }

            @Override
            protected void rollbackImpl() {
            }
        });
        session.getTransactionManager().enlistAfterCompletion(new AbstractKeycloakTransaction() {
            @Override
            protected void commitImpl() {
                KeycloakModelUtils.runJobInTransaction(factory, next -> next.realms()
                        .getRealmByName(realmName).setAttribute(callbackMarker, "committed"));
            }

            @Override
            protected void rollbackImpl() {
                KeycloakModelUtils.runJobInTransaction(factory, next -> next.realms()
                        .getRealmByName(realmName).setAttribute(callbackMarker, "rolled-back"));
            }
        });

        try {
            session.getTransactionManager().commit();
            throw new AssertionError("The injected after-completion failure must propagate");
        } catch (IllegalStateException expected) {
            assertEquals("KC51749 injected after-completion failure", expected.getMessage());
        }
    }

    @Test
    public void expiredRefreshLockDoesNotLetDelayedRollbackOverwriteNewState() throws Exception {
        realm.updateWithCleanup(r -> r.revokeRefreshToken(true).refreshTokenMaxReuse(1));

        String code = loginAndGetCode();
        AccessTokenResponse initial = oauth.doAccessTokenRequest(code);
        assertEquals(200, initial.getStatusCode());
        AccessTokenResponse firstRefresh = oauth.doRefreshTokenRequest(initial.getRefreshToken());
        assertEquals(200, firstRefresh.getStatusCode());
        RefreshToken rt1 = oauth.parseRefreshToken(firstRefresh.getRefreshToken());

        String realmName = realm.getName();
        String userSessionId = rt1.getSessionId();
        String reuseKey = new TokenManager().getReuseIdKey(rt1);
        String lockId = "refreshLock:" + userSessionId + ":" + reuseKey;
        String marker = UUID.randomUUID().toString();
        String readyKey = "kc51749-ready:" + marker;
        String releaseKey = "kc51749-release:" + marker;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> delayedRollback = executor.submit(() -> runOnServer.run(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            invokeRefreshLockHelper(session, testRealm, userSessionId, reuseKey);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);

            invokeReuseCounterHelper("captureTokenReuseState", session, clientSession, reuseKey);
            invokeReuseCounterHelper("incrementTokenReuseCount", session, clientSession, reuseKey);
            assertTrue(session.singleUseObjects().putIfAbsent(readyKey, 60));

            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(50);
            while (!session.singleUseObjects().contains(releaseKey)) {
                if (System.nanoTime() > deadline) {
                    throw new AssertionError("Timed out waiting to roll back the older refresh state");
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Interrupted before delayed rollback", e);
                }
            }
            session.getTransactionManager().setRollbackOnly();
        }));

        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!runOnServer.fetch(session -> session.singleUseObjects().contains(readyKey), Boolean.class)) {
                assertTrue(System.nanoTime() < deadline, "Older refresh state did not reach the controlled wait");
                Thread.sleep(50);
            }

            Thread.sleep(17_000);
            AccessTokenResponse newerRefresh = oauth.doRefreshTokenRequest(firstRefresh.getRefreshToken());
            assertEquals(200, newerRefresh.getStatusCode(), "Refresh after the 15-second lock expiry must succeed");
            String newerState = readRotationState(realmName, userSessionId, reuseKey);

            assertTrue(runOnServer.fetch(session -> session.singleUseObjects()
                    .putIfAbsent(releaseKey, 60), Boolean.class));
            delayedRollback.get(30, TimeUnit.SECONDS);

            assertEquals(newerState, readRotationState(realmName, userSessionId, reuseKey),
                    "Late compensation must not overwrite rotation state committed by a newer refresh");
        } finally {
            runOnServer.run(session -> {
                session.singleUseObjects().putIfAbsent(releaseKey, 60);
                session.singleUseObjects().remove(readyKey);
                session.singleUseObjects().remove(lockId);
            });
            executor.shutdownNow();
        }
    }

    @Test
    public void expiredRefreshLockDoesNotReleaseNewHolder() throws Exception {
        String realmName = realm.getName();
        String userSessionId = UUID.randomUUID().toString();
        String reuseKey = UUID.randomUUID().toString();
        String lockId = "refreshLock:" + userSessionId + ":" + reuseKey;
        String marker = UUID.randomUUID().toString();
        String readyKey = "kc51749-ready:" + marker;
        String releaseKey = "kc51749-release:" + marker;

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> oldCompletion = executor.submit(() -> runOnServer.run(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            invokeRefreshLockHelper(session, testRealm, userSessionId, reuseKey);
            assertTrue(session.singleUseObjects().putIfAbsent(readyKey, 60));
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(50);
            while (!session.singleUseObjects().contains(releaseKey)) {
                if (System.nanoTime() > deadline) {
                    throw new AssertionError("Timed out waiting for old lock completion");
                }
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AssertionError("Interrupted before old lock completion", e);
                }
            }
        }));

        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (!runOnServer.fetch(session -> session.singleUseObjects().contains(readyKey), Boolean.class)) {
                assertTrue(System.nanoTime() < deadline, "Old lock was not acquired");
                Thread.sleep(50);
            }
            Thread.sleep(17_000);
            assertTrue(runOnServer.fetch(session -> session.singleUseObjects()
                    .putIfAbsent(lockId, 30), Boolean.class), "A later holder must acquire the expired lock");
            assertTrue(runOnServer.fetch(session -> session.singleUseObjects()
                    .putIfAbsent(releaseKey, 60), Boolean.class));
            oldCompletion.get(30, TimeUnit.SECONDS);
            assertTrue(runOnServer.fetch(session -> session.singleUseObjects().contains(lockId), Boolean.class),
                    "The old completion must not release the later holder's lock");
        } finally {
            runOnServer.run(session -> {
                session.singleUseObjects().putIfAbsent(releaseKey, 60);
                session.singleUseObjects().remove(readyKey);
                session.singleUseObjects().remove(lockId);
            });
            executor.shutdownNow();
        }
    }

    private String readDatabaseClientSession(String userSessionId) {
        return runOnServer.fetch(session -> (String) session.getProvider(JpaConnectionProvider.class)
                .getEntityManager().createNativeQuery("SELECT DATA FROM OFFLINE_CLIENT_SESSION "
                        + "WHERE USER_SESSION_ID = :sessionId")
                .setParameter("sessionId", userSessionId).getSingleResult(), String.class);
    }

    private String readRotationState(String realmName, String userSessionId, String reuseKey) {
        return runOnServer.fetch(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var userSession = session.sessions().getUserSession(testRealm, userSessionId);
            assertNotNull(userSession);
            var client = testRealm.getClientByClientId("test-app");
            assertNotNull(client);
            var clientSession = userSession.getAuthenticatedClientSessionByClient(client.getId());
            assertNotNull(clientSession);
            return String.join("|", clientSession.getRefreshToken(reuseKey),
                    String.valueOf(clientSession.getRefreshTokenUseCount(reuseKey)),
                    clientSession.getLatestGeneratedRefreshToken(reuseKey),
                    String.valueOf(clientSession.getRefreshTokenLastRefresh(reuseKey)));
        }, String.class);
    }

    private String loginAndGetCode() {
        driver.driver().manage().deleteAllCookies();
        oauth.loginForm().prompt("login").open();
        oauth.fillLoginForm(user.getUsername(), "password");
        return oauth.parseLoginResponse().getCode();
    }

    private static void invokeReuseCounterHelper(String name, KeycloakSession session,
                                                 AuthenticatedClientSessionModel clientSession, String reuseKey) {
        try {
            var method = AbstractRefreshTokenProvider.class.getDeclaredMethod(name,
                    KeycloakSession.class, AuthenticatedClientSessionModel.class, String.class);
            method.setAccessible(true);
            method.invoke(null, session, clientSession, reuseKey);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to invoke refresh-token reuse helper " + name, e);
        }
    }

    private static void invokeRefreshLockHelper(KeycloakSession session, RealmModel realm,
                                                String userSessionId, String reuseKey) {
        AccessToken base = new AccessToken();
        base.setSessionId(userSessionId);
        RefreshToken token = new RefreshToken(base, null, null);
        token.getOtherClaims().put(Constants.REUSE_ID, reuseKey);
        try {
            var method = AbstractRefreshTokenProvider.class.getDeclaredMethod(
                    "createTemporaryExclusiveLockForTokenRefreshOperation", KeycloakSession.class,
                    RealmModel.class, RefreshToken.class, TokenManager.class);
            method.setAccessible(true);
            method.invoke(new DefaultRefreshTokenProvider(session), session, realm, token, new TokenManager());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Unable to acquire the production refresh lock", e);
        }
    }

}
