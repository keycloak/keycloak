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
import java.util.concurrent.atomic.AtomicInteger;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.loginfailures.jpa.LoginFailureExpirationAction;
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test for LoginFailureExpirationAction to verify that expired login failure entries are properly removed from the
 * database.
 * <p>
 * This test only runs when the stateless feature is enabled.
 */
@KeycloakIntegrationTest
public class LoginFailureExpirationTest {

    private static final int MAX_DELTA_TIME_SECONDS = 60; // 1 minute

    // Not important, just large enough to keep test timestamps positive. All APIs take the current time as a parameter,
    // so we avoid calling Time.currentTimeSeconds() in this test.
    private static final int CURRENT_TIME_SECONDS = 60 * 60 * 5;
    private static final long CURRENT_TIME_MILLIS = (long) CURRENT_TIME_SECONDS * 1000L;
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
        // Only run this test when stateless feature is enabled
        Assumptions.assumeTrue(isStatelessFeatureEnabled(), "Test only runs with stateless feature enabled");

        var state = createState();

        // Create login failures with different last failure times
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);

            // User 0: Recent failure (should NOT be expired)
            var failure0 = session.loginFailures().addUserLoginFailure(realmModel, state.user0);
            failure0.setLastFailure(CURRENT_TIME_MILLIS - 10_000); // 10 seconds ago
            failure0.incrementFailures();

            // User 1: Failure just at the edge (should NOT be expired)
            var failure1 = session.loginFailures().addUserLoginFailure(realmModel, state.user1);
            failure1.setLastFailure(CURRENT_TIME_MILLIS - (MAX_DELTA_TIME_SECONDS * 1000)); // exactly at max delta
            failure1.incrementFailures();

            // User 2: Old failure (should be expired)
            var failure2 = session.loginFailures().addUserLoginFailure(realmModel, state.user2);
            failure2.setLastFailure(CURRENT_TIME_MILLIS - ((MAX_DELTA_TIME_SECONDS + 10) * 1000)); // 10 seconds past max delta
            failure2.incrementFailures();

            // User 3: Very old failure (should be expired)
            var failure3 = session.loginFailures().addUserLoginFailure(realmModel, state.user3);
            failure3.setLastFailure(CURRENT_TIME_MILLIS - ((MAX_DELTA_TIME_SECONDS + 120) * 1000)); // 2 minutes past max delta
            failure3.incrementFailures();

            // User 4: Ancient failure (should be expired)
            var failure4 = session.loginFailures().addUserLoginFailure(realmModel, state.user4);
            failure4.setLastFailure(CURRENT_TIME_MILLIS - ((MAX_DELTA_TIME_SECONDS + 300) * 1000)); // 5 minutes past max delta
            failure4.incrementFailures();
        });

        // Verify all login failures exist before expiration
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);

            for (var userId : state.allUsers()) {
                var failure = session.loginFailures().getUserLoginFailure(realmModel, userId);
                assertNotNull(failure, "Login failure should exist for user " + userId);
                assertEquals(1, failure.getNumFailures(), "Login failure should have 1 failure");
            }
        });

        // Run the expiration action manually

        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var removedCount = new AtomicInteger(0);
            // Run expiration with a high maxRemoval to ensure all expired entries are removed
            var hasMore = LoginFailureExpirationAction.INSTANCE.removeExpired(
                    session,
                    realmModel.getId(),
                    CURRENT_TIME_SECONDS,
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
        // Only run this test when stateless feature is enabled
        Assumptions.assumeTrue(isStatelessFeatureEnabled(), "Test only runs with stateless feature enabled");

        var state = createState();

        // Configure realm for permanent lockout only
        realm.updateWithCleanup(r -> r.permanentLockout(true).maxTemporaryLockouts(0));

        // Create an expired login failure
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var failure = session.loginFailures().addUserLoginFailure(realmModel, state.user0);
            failure.setLastFailure(CURRENT_TIME_MILLIS - ((MAX_DELTA_TIME_SECONDS + 60) * 1000));
            failure.incrementFailures();
        });

        // Run the expiration action
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            var removedCount = new AtomicInteger(0);
            LoginFailureExpirationAction.INSTANCE.removeExpired(
                    session,
                    realmModel.getId(),
                    CURRENT_TIME_SECONDS,
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
        // Only run this test when stateless feature is enabled
        Assumptions.assumeTrue(isStatelessFeatureEnabled(), "Test only runs with stateless feature enabled");

        var state = createState();

        // Create multiple expired login failures
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealm(state.realmId);
            for (var userId : state.allUsers()) {
                var failure = session.loginFailures().addUserLoginFailure(realmModel, userId);
                failure.setLastFailure(CURRENT_TIME_MILLIS - ((MAX_DELTA_TIME_SECONDS + 60) * 1000));
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
                    CURRENT_TIME_SECONDS,
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
                    CURRENT_TIME_SECONDS,
                    100,
                    removedCount::addAndGet
            );
            assertFalse(hasMore, "Should not have more entries after second run");
            assertEquals(3, removedCount.get(), "Should have removed remaining 3 entries");
        });
    }

    private boolean isStatelessFeatureEnabled() {
        var serverInfo = adminClient.serverInfo().getInfo();
        var feature = serverInfo.getFeatures().stream()
                .filter(feat -> Profile.Feature.STATELESS.name().equals(feat.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Stateless feature not found"));
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
