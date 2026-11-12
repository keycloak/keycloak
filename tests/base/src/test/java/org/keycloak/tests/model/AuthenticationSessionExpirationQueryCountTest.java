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

package org.keycloak.tests.model;

import java.util.Arrays;

import org.keycloak.authentication.jpa.AuthenticationSessionExpirationAction;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.models.utils.SessionExpiration;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.remote.providers.timeoffset.InfinispanTimeUtil;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Checks with the Hibernate statistics which queries the authentication session expiration runs.
 * <p>
 * The statistics are shared with the periodic expiration task running in the server. That task only runs every few
 * minutes, so the assertions look at the executed query kinds and not at exact counts, and they are retried.
 */
@KeycloakIntegrationTest
public class AuthenticationSessionExpirationQueryCountTest {

    private static final String REALM_NAME = "auth-expiration-query-count";
    private static final int ASSERTION_ATTEMPTS = 3;

    @InjectRealm(config = ExpirationRealmConfig.class)
    ManagedRealm realm;

    @TestOnServer
    public void emptyRealmSkipsBucketQueries(KeycloakSession session) {
        assumeTrue(Profile.isFeatureEnabled(Profile.Feature.STATELESS));
        String realmId = session.realms().getRealmByName(REALM_NAME).getId();

        // the realm has no sessions, so there is no bucket query
        assertWithRetries(() -> {
            String[] queries = executedSessionQueries(session, realmId);
            assertTrue(Arrays.stream(queries).noneMatch(q -> q.contains("sessionBucket")),
                    "No bucket query is expected for a realm without sessions: " + Arrays.toString(queries));
        });
    }

    @TestOnServer
    public void sessionInPreviouslyEmptyRealmIsStillExpired(KeycloakSession session) {
        assumeTrue(Profile.isFeatureEnabled(Profile.Feature.STATELESS));
        RealmModel realmModel = session.realms().getRealmByName(REALM_NAME);
        String realmId = realmModel.getId();
        InfinispanTimeUtil.enableTestingTimeService(session);
        try {
            executedSessionQueries(session, realmId);

            String[] sessionId = new String[1];
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session1 -> {
                RealmModel r = session1.realms().getRealm(realmId);
                sessionId[0] = session1.authenticationSessions().createRootAuthenticationSession(r).getId();
            });

            // the realm has a session now, so the probe finds it and the bucket queries run
            assertWithRetries(() -> {
                String[] queries = executedSessionQueries(session, realmId);
                assertTrue(Arrays.stream(queries).anyMatch(q -> q.contains("sessionBucket")),
                        "Bucket queries are expected for a realm with sessions: " + Arrays.toString(queries));
            });

            Time.setOffset(SessionExpiration.getAuthSessionLifespan(realmModel) + 60);
            executedSessionQueries(session, realmId);

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session1 -> {
                assertNull(session1.getProvider(JpaConnectionProvider.class).getEntityManager()
                        .find(org.keycloak.authentication.jpa.RootAuthenticationSessionEntity.class, sessionId[0]));
            });
        } finally {
            Time.setOffset(0);
            session.getKeycloakSessionFactory().publish(new ResetTimeOffsetEvent());
            InfinispanTimeUtil.disableTestingTimeService(session);
        }
    }

    // The statistics are shared with the periodic expiration task, which can add queries for other realms to a measurement.
    // A regression fails every attempt, so retrying only filters out that noise.
    private static void assertWithRetries(Runnable assertion) {
        for (int attempt = 1; ; attempt++) {
            try {
                assertion.run();
                return;
            } catch (AssertionError e) {
                if (attempt >= ASSERTION_ATTEMPTS) {
                    throw e;
                }
            }
        }
    }

    // runs the expiration and returns the HQL of the executed queries on the root authentication session entity
    private static String[] executedSessionQueries(KeycloakSession session, String realmId) {
        Statistics stats = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        boolean wasEnabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session1 ->
                    AuthenticationSessionExpirationAction.INSTANCE.removeExpired(session1, realmId, Time.currentTime(), Integer.MAX_VALUE, removed -> {
                    }));
            return Arrays.stream(stats.getQueries())
                    .filter(q -> q.contains("RootAuthenticationSessionEntity"))
                    .toArray(String[]::new);
        } finally {
            stats.setStatisticsEnabled(wasEnabled);
        }
    }

    private static class ExpirationRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.name(REALM_NAME);
        }
    }
}
