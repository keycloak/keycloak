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

package org.keycloak.tests.loginfailures;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.loginfailures.jpa.LoginFailureExpirationAction;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.suites.DatabaseTest;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for LoginFailureExpirationAction to verify that expired login failure entries are properly removed from the
 * database.
 * <p>
 * This test only runs when login failures v2 is enabled.
 */
@KeycloakIntegrationTest
// The JPA expiration queries, result limiting, bulk deletion, and upsert behavior need verification on every database.
@DatabaseTest
public class LoginFailureExpirationTest {

    private static final int MAX_DELTA_TIME_SECONDS = 60; // 1 minute

    @InjectRealm(config = ConfigureMaxDeltaTimeout.class)
    ManagedRealm realm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectUser(ref = "user0")
    ManagedUser user0;

    @InjectUser(ref = "user1")
    ManagedUser user1;

    @InjectUser(ref = "user2")
    ManagedUser user2;

    @InjectUser(ref = "user3")
    ManagedUser user3;

    @InjectUser(ref = "user4")
    ManagedUser user4;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @AfterEach
    public void reset() {
        var state = createState();
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            for (var userId : state.allUsers()) {
                session.loginFailures().removeUserLoginFailure(realmModel, userId);
            }
        });
    }

    @Test
    public void testLoginFailureExpiration() {
        Assumptions.assumeTrue(isLoginFailuresV2Enabled(), "Test only runs with login failures v2 enabled");

        var state = createState();

        // Create login failures with different last failure times
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);

            // User 0: Recent failure (should NOT be expired)
            var failure0 = session.loginFailures().addUserLoginFailure(realmModel, state.user0);
            failure0.setLastFailure(Time.currentTimeMillis() - 10_000); // 10 seconds ago
            failure0.incrementFailures();

            // User 1: Failure within the safe window (should NOT be expired)
            var failure1 = session.loginFailures().addUserLoginFailure(realmModel, state.user1);
            failure1.setLastFailure(Time.currentTimeMillis() - ((MAX_DELTA_TIME_SECONDS - 10) * 1000)); // 10 seconds before max delta
            failure1.incrementFailures();

            // User 2: Old failure (should be expired)
            var failure2 = session.loginFailures().addUserLoginFailure(realmModel, state.user2);
            failure2.setLastFailure(Time.currentTimeMillis() - ((MAX_DELTA_TIME_SECONDS + 10) * 1000)); // 10 seconds past max delta
            failure2.incrementFailures();

            // User 3: Very old failure (should be expired)
            var failure3 = session.loginFailures().addUserLoginFailure(realmModel, state.user3);
            failure3.setLastFailure(Time.currentTimeMillis() - ((MAX_DELTA_TIME_SECONDS + 120) * 1000)); // 2 minutes past max delta
            failure3.incrementFailures();

            // User 4: Ancient failure (should be expired)
            var failure4 = session.loginFailures().addUserLoginFailure(realmModel, state.user4);
            failure4.setLastFailure(Time.currentTimeMillis() - ((MAX_DELTA_TIME_SECONDS + 300) * 1000)); // 5 minutes past max delta
            failure4.incrementFailures();
        });

        // Run the expiration action manually

        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var removedCount = new AtomicInteger(0);
            // Run expiration with a high maxRemoval to ensure all expired entries are removed
            var hasMore = LoginFailureExpirationAction.INSTANCE.removeExpired(
                    session,
                    realmModel.getId(),
                    Time.currentTime(),
                    100, // maxRemoval
                    removedCount::addAndGet
            );

            assertFalse(hasMore, "Should not have more entries to remove");
            // Verify that 3 entries were removed (users 2, 3, 4)
            assertEquals(3, removedCount.get(), "Should have removed 3 expired login failures");
        });

        // Verify the state after expiration
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);

            // User 0 and 1 should still exist (not expired)
            var failure0 = session.loginFailures().getUserLoginFailure(realmModel, state.user0);
            assertNotNull(failure0, "Recent login failure should still exist");
            assertEquals(1, failure0.getNumFailures());

            var failure1 = session.loginFailures().getUserLoginFailure(realmModel, state.user1);
            assertNotNull(failure1, "Login failure at edge should still exist");
            assertEquals(1, failure1.getNumFailures());

            // Users 2, 3, 4 should be removed (expired)
            var failure2 = session.loginFailures().getUserLoginFailure(realmModel, state.user2);
            assertNull(failure2, "Old login failure should be removed");

            var failure3 = session.loginFailures().getUserLoginFailure(realmModel, state.user3);
            assertNull(failure3, "Very old login failure should be removed");

            var failure4 = session.loginFailures().getUserLoginFailure(realmModel, state.user4);
            assertNull(failure4, "Ancient login failure should be removed");
        });
    }

    @Test
    public void testLoginFailureExpirationWithPermanentLockout() {
        Assumptions.assumeTrue(isLoginFailuresV2Enabled(), "Test only runs with login failures v2 enabled");

        var state = createState();

        // Configure realm for permanent lockout only
        realm.updateWithCleanup(r -> r.permanentLockout(true).maxTemporaryLockouts(0));

        // Create an expired login failure
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var failure = session.loginFailures().addUserLoginFailure(realmModel, state.user0);
            failure.setLastFailure(Time.currentTimeMillis() - ((MAX_DELTA_TIME_SECONDS + 60) * 1000));
            failure.incrementFailures();
        });

        // Run the expiration action
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var removedCount = new AtomicInteger(0);
            LoginFailureExpirationAction.INSTANCE.removeExpired(
                    session,
                    realmModel.getId(),
                    Time.currentTime(),
                    100,
                    removedCount::addAndGet
            );
            // With permanent lockout only, login failures should NOT expire
            assertEquals(0, removedCount.get(), "Should not remove any entries with permanent lockout only");
        });

        // Verify the login failure still exists
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var failure = session.loginFailures().getUserLoginFailure(realmModel, state.user0);
            assertNotNull(failure, "Login failure should still exist with permanent lockout");
        });
    }

    @Test
    public void testLoginFailureExpirationWithMaxRemovalLimit() {
        Assumptions.assumeTrue(isLoginFailuresV2Enabled(), "Test only runs with login failures v2 enabled");

        var state = createState();

        // Create multiple expired login failures
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            for (var userId : state.allUsers()) {
                var failure = session.loginFailures().addUserLoginFailure(realmModel, userId);
                failure.setLastFailure(Time.currentTimeMillis() - ((MAX_DELTA_TIME_SECONDS + 60) * 1000));
                failure.incrementFailures();
            }
        });

        // Run expiration with a limit of 2
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var removedCount = new AtomicInteger(0);
            var hasMore = LoginFailureExpirationAction.INSTANCE.removeExpired(
                    session,
                    realmModel.getId(),
                    Time.currentTime(),
                    2, // maxRemoval - limit to 2
                    removedCount::addAndGet
            );
            // Should indicate there are more entries to remove
            assertTrue(hasMore, "Should indicate more entries to remove");
            assertEquals(2, removedCount.get(), "Should have removed exactly 2 entries");
        });

        // Run again to remove remaining entries
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var removedCount = new AtomicInteger(0);
            var hasMore = LoginFailureExpirationAction.INSTANCE.removeExpired(
                    session,
                    realmModel.getId(),
                    Time.currentTime(),
                    100,
                    removedCount::addAndGet
            );
            assertFalse(hasMore, "Should not have more entries after second run");
            assertEquals(3, removedCount.get(), "Should have removed remaining 3 entries");
        });
    }

    @Test
    public void testAddingFailureResetsExpiredEntry() {
        Assumptions.assumeTrue(isLoginFailuresV2Enabled(), "Test only runs with login failures v2 enabled");

        var state = createState();

        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var failure = session.loginFailures().addUserLoginFailure(realmModel, state.user0);
            failure.setLastFailure(Time.currentTimeMillis() - ((MAX_DELTA_TIME_SECONDS + 60) * 1000));
            failure.incrementFailures();
            failure.incrementSecondaryAuthFailures();
            failure.incrementSecondaryAuthFailures();
        });

        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);

            assertNull(session.loginFailures().getUserLoginFailure(realmModel, state.user0), "Entry should be treated as expired");

            var failure = session.loginFailures().addUserLoginFailure(realmModel, state.user0);
            assertEquals(0, failure.getNumFailures());
            assertEquals(0, failure.getNumSecondaryAuthFailures(), "Secondary failures of an expired entry must be reset");
        });
    }

    @Test
    public void testEntryCommittedAfterSnapshotIsFound() {
        Assumptions.assumeTrue(isLoginFailuresV2Enabled(), "Test only runs with login failures v2 enabled");

        var state = createState();

        runOnServer.run(session -> {
            var factory = session.getKeycloakSessionFactory();
            var snapshotTaken = new CountDownLatch(1);
            var otherCommitted = new CountDownLatch(1);
            ExecutorService executor = Executors.newFixedThreadPool(2, runnable -> {
                var thread = new Thread(runnable, "login-failure-upsert");
                thread.setDaemon(true);
                return thread;
            });
            int expectedFailures;
            try {
                // Transaction A reads first (as DefaultBruteForceProtector.failure() does), which on MySQL/MariaDB
                // fixes its REPEATABLE READ snapshot, and only then adds the login failure.
                Future<?> first = executor.submit(() -> KeycloakModelUtils.runJobInTransaction(factory, staleSession -> {
                    var realmModel = staleSession.realms().getRealm(state.realmId);
                    assertNull(staleSession.loginFailures().getUserLoginFailure(realmModel, state.user0));
                    snapshotTaken.countDown();
                    await(otherCommitted);
                    var failure = staleSession.loginFailures().addUserLoginFailure(realmModel, state.user0);
                    failure.setLastFailure(Time.currentTimeMillis());
                    failure.incrementFailures();
                }));
                // Transaction B creates the entry and commits in between.
                Future<?> second = executor.submit(() -> KeycloakModelUtils.runJobInTransaction(factory, concurrentSession -> {
                    await(snapshotTaken);
                    var failure = concurrentSession.loginFailures().addUserLoginFailure(
                            concurrentSession.realms().getRealm(state.realmId), state.user0);
                    failure.setLastFailure(Time.currentTimeMillis());
                    failure.incrementFailures();
                }));
                second.get(30, TimeUnit.SECONDS);
                otherCommitted.countDown();

                try {
                    first.get(30, TimeUnit.SECONDS);
                    expectedFailures = 2;
                } catch (ExecutionException e) {
                    // MariaDB with innodb_snapshot_isolation=ON (the default in 11.8) rejects a locking read of a row
                    // that is newer than the snapshot. The transaction fails fast instead of looping, which is acceptable.
                    if (!isSnapshotIsolationConflict(e)) {
                        throw e;
                    }
                    expectedFailures = 1;
                }
            } catch (TimeoutException e) {
                fail("addUserLoginFailure did not return; it must find an entry committed after the snapshot was taken");
                return;
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                executor.shutdownNow();
            }

            int expected = expectedFailures;
            KeycloakModelUtils.runJobInTransaction(factory, verificationSession -> {
                var failure = verificationSession.loginFailures().getUserLoginFailure(
                        verificationSession.realms().getRealm(state.realmId), state.user0);
                assertNotNull(failure);
                assertEquals(expected, failure.getNumFailures());
            });
        });
    }

    private static boolean isSnapshotIsolationConflict(Throwable e) {
        for (Throwable t = e; t != null; t = t.getCause()) {
            if ("org.hibernate.exception.SnapshotIsolationException".equals(t.getClass().getName())) {
                return true;
            }
        }
        return false;
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(30, TimeUnit.SECONDS), "Timed out waiting for concurrent transaction");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private boolean isLoginFailuresV2Enabled() {
        var serverInfo = adminClient.serverInfo().getInfo();
        var feature = serverInfo.getFeatures().stream()
                .filter(feat -> Profile.Feature.LOGIN_FAILURES_V2.name().equals(feat.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Login failures v2 feature not found"));
        return feature.isEnabled();
    }

    public static class ConfigureMaxDeltaTimeout implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.maxTemporaryLockouts(1)
                    .permanentLockout(false)
                    .maxDeltaTimeSeconds(MAX_DELTA_TIME_SECONDS);
        }
    }

    private ServerState createState() {
        return new ServerState(realm.getId(), user0.getId(), user1.getId(), user2.getId(), user3.getId(), user4.getId());
    }

    // to be serialized with runOnServer
    public record ServerState(
            String realmId,
            String user0,
            String user1,
            String user2,
            String user3,
            String user4
    ) implements Serializable {

        List<String> allUsers() {
            return List.of(user0, user1, user2, user3, user4);
        }

    }
}
