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

package org.keycloak.tests.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conditions.DisabledForDatabases;
import org.keycloak.testframework.infinispan.CacheType;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.tests.suites.DatabaseTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Concurrent first inserts of the same key through the {@code insert ... on conflict} upserts must not fail with a
 * unique constraint violation. On Oracle and SQL Server these upserts are emulated.
 * <p>
 * Disabled on H2 (development only): it emulates the upsert with a MERGE that is not race-free.
 */
@KeycloakIntegrationTest(config = ConcurrentUpsertTest.ServerConfig.class)
@DatabaseTest
@DisabledForDatabases({ "dev-mem", "dev-file" })
public class ConcurrentUpsertTest {

    private static final int ROUNDS = 30;
    private static final int THREADS = 4;

    @TestOnServer
    public void testConcurrentFirstLoginFailure(KeycloakSession session) throws Exception {
        String realmId = session.realms().getRealmByName("master").getId();
        List<String> userIds = Collections.synchronizedList(new ArrayList<>());
        try {
            List<String> errors = new ArrayList<>();
            race(session, errors, (s, userId) -> {
                userIds.add(userId);
                var failure = s.loginFailures().addUserLoginFailure(s.realms().getRealm(realmId), userId);
                // as DefaultBruteForceProtector does; an entry with lastFailure == 0 counts as expired
                failure.setLastFailure(Time.currentTimeMillis());
                failure.incrementFailures();
                return 1;
            });
            assertEquals(List.of(), errors, "concurrent first failures must not fail");
            int counted = KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), s ->
                    userIds.stream().distinct()
                            .mapToInt(userId -> s.loginFailures().getUserLoginFailure(s.realms().getRealm(realmId), userId).getNumFailures())
                            .sum());
            assertEquals(ROUNDS * THREADS, counted, "every failure must be counted");
        } finally {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), s -> userIds.stream().distinct()
                    .forEach(userId -> s.loginFailures().removeUserLoginFailure(s.realms().getRealm(realmId), userId)));
        }
    }

    @TestOnServer
    public void testConcurrentPutIfAbsent(KeycloakSession session) throws Exception {
        List<String> keys = Collections.synchronizedList(new ArrayList<>());
        try {
            List<String> errors = new ArrayList<>();
            List<Integer> results = race(session, errors, (s, key) -> {
                keys.add(key);
                return s.singleUseObjects().putIfAbsent(key, 60) ? 1 : 0;
            });
            assertEquals(List.of(), errors, "concurrent putIfAbsent must not fail");
            assertEquals(ROUNDS, results.stream().mapToInt(Integer::intValue).sum(), "exactly one putIfAbsent per key must succeed");
        } finally {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(),
                    s -> keys.stream().distinct().forEach(key -> s.singleUseObjects().remove(key)));
        }
    }

    @TestOnServer
    public void testConcurrentPutIfAbsentReplacesExpiredRow(KeycloakSession session) throws Exception {
        List<String> keys = newKeys();
        int originalTimeOffset = Time.getOffset();
        try {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), s -> keys.forEach(key ->
                    assertTrue(s.singleUseObjects().putIfAbsent(key, 1), "the initial single-use object must be inserted")));
            Time.setOffset(originalTimeOffset + 2);

            List<String> errors = new ArrayList<>();
            List<Integer> results = race(session, errors, keys,
                    (s, key) -> s.singleUseObjects().putIfAbsent(key, 60) ? 1 : 0);
            assertEquals(List.of(), errors, "concurrent replacement of expired single-use objects must not fail");
            assertEquals(ROUNDS, results.stream().mapToInt(Integer::intValue).sum(),
                    "exactly one putIfAbsent per expired key must succeed");
            int present = KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), s ->
                    (int) keys.stream().filter(key -> s.singleUseObjects().contains(key)).count());
            assertEquals(ROUNDS, present, "every expired single-use object must be replaced");
        } finally {
            Time.setOffset(originalTimeOffset);
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(),
                    s -> keys.forEach(key -> s.singleUseObjects().remove(key)));
        }
    }

    @TestOnServer
    public void testConcurrentRevokeToken(KeycloakSession session) throws Exception {
        List<String> errors = new ArrayList<>();
        List<Integer> results = race(session, errors, (s, id) -> s.revokedTokens().put(id, 60) ? 1 : 0);
        assertEquals(List.of(), errors, "concurrent revocations must not fail");
        assertEquals(ROUNDS, results.stream().mapToInt(Integer::intValue).sum(), "exactly one revocation per token must be new");
    }

    @TestOnServer
    public void testConcurrentRevokeTokenReplacesExpiredRow(KeycloakSession session) throws Exception {
        List<String> ids = newKeys();
        int originalTimeOffset = Time.getOffset();
        try {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), s -> ids.forEach(id ->
                    assertTrue(s.revokedTokens().put(id, 1), "the initial token revocation must be inserted")));
            Time.setOffset(originalTimeOffset + 2);

            List<String> errors = new ArrayList<>();
            List<Integer> results = race(session, errors, ids,
                    (s, id) -> s.revokedTokens().put(id, 60) ? 1 : 0);
            assertEquals(List.of(), errors, "concurrent replacement of expired token revocations must not fail");
            assertEquals(ROUNDS, results.stream().mapToInt(Integer::intValue).sum(),
                    "exactly one revocation per expired token must succeed");
            int present = KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), s ->
                    (int) ids.stream().filter(id -> s.revokedTokens().contains(id)).count());
            assertEquals(ROUNDS, present, "every expired token revocation must be replaced");
        } finally {
            Time.setOffset(originalTimeOffset);
        }
    }

    @TestOnServer
    public void testConcurrentRootAuthenticationSessionWithSameId(KeycloakSession session) throws Exception {
        String realmId = session.realms().getRealmByName("master").getId();
        List<String> ids = Collections.synchronizedList(new ArrayList<>());
        try {
            List<String> errors = new ArrayList<>();
            race(session, errors, (s, id) -> {
                ids.add(id);
                s.authenticationSessions().createRootAuthenticationSession(s.realms().getRealm(realmId), id);
                return 1;
            });
            assertEquals(List.of(), errors, "concurrent creation with the same id must not fail");
        } finally {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), s -> {
                var realm = s.realms().getRealm(realmId);
                ids.stream().distinct().forEach(id -> {
                    var rootSession = s.authenticationSessions().getRootAuthenticationSession(realm, id);
                    if (rootSession != null) {
                        s.authenticationSessions().removeRootAuthenticationSession(realm, rootSession);
                    }
                });
            });
        }
    }

    /**
     * Runs the task in {@link #THREADS} concurrent transactions per round, all with the same new key. Returns the
     * results of the committed transactions and adds the root cause of each failed one to {@code errors}.
     */
    private static List<Integer> race(KeycloakSession session, List<String> errors,
                                      BiFunction<KeycloakSession, String, Integer> task) throws Exception {
        return race(session, errors, newKeys(), task);
    }

    private static List<String> newKeys() {
        List<String> keys = new ArrayList<>(ROUNDS);
        for (int round = 0; round < ROUNDS; round++) {
            // a plain UUID fits every key column, e.g. ROOT_AUTHENTICATION_SESSION.ID is VARCHAR(36)
            keys.add(UUID.randomUUID().toString());
        }
        return keys;
    }

    private static List<Integer> race(KeycloakSession session, List<String> errors, List<String> keys,
                                      BiFunction<KeycloakSession, String, Integer> task) throws Exception {
        List<Integer> results = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            for (String key : keys) {
                CyclicBarrier barrier = new CyclicBarrier(THREADS);
                List<Future<Integer>> futures = new ArrayList<>();
                for (int i = 0; i < THREADS; i++) {
                    futures.add(executor.submit(() -> KeycloakModelUtils.runJobInTransactionWithResult(session.getKeycloakSessionFactory(), s -> {
                        try {
                            barrier.await(30, TimeUnit.SECONDS);
                        } catch (Exception e) {
                            throw new IllegalStateException(e);
                        }
                        return task.apply(s, key);
                    })));
                }
                for (Future<Integer> future : futures) {
                    try {
                        results.add(future.get(60, TimeUnit.SECONDS));
                    } catch (Exception e) {
                        Throwable cause = e;
                        while (cause.getCause() != null && cause.getCause() != cause) {
                            cause = cause.getCause();
                        }
                        errors.add(cause.getClass().getSimpleName() + ": " + cause.getMessage());
                    }
                }
            }
        } finally {
            executor.shutdownNow();
        }
        return results;
    }

    public static class ServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.STATELESS)
                    .cache(CacheType.ISPN)
                    .cacheEmbeddedClusterName("C1");
        }
    }
}
