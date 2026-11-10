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

import org.keycloak.common.util.Time;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ResetTimeOffsetEvent;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.testframework.remote.providers.timeoffset.InfinispanTimeUtil;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Checks with the Hibernate statistics which queries the persistent user session expiration runs.
 * <p>
 * The statistics are shared with the periodic expiration task running in the server. That task only runs every few
 * minutes, so the assertions look at the executed query kinds and not at exact counts.
 */
@KeycloakIntegrationTest
public class UserSessionExpirationQueryCountTest {

    private static final String REALM_NAME = "expiration-query-count";
    private static final int ASSERTION_ATTEMPTS = 3;

    @InjectRealm(config = ExpirationRealmConfig.class)
    ManagedRealm realm;

    @TestOnServer
    public void emptyRealmSkipsBucketQueries(KeycloakSession session) {
        RealmModel realmModel = session.realms().getRealmByName(REALM_NAME);

        // the realm has no sessions, so there is no bucket query
        assertWithRetries(() -> {
            String[] queries = executedSessionQueries(session, realmModel);
            assertTrue(Arrays.stream(queries).noneMatch(q -> q.contains("sessionBucket")),
                    "No bucket query is expected for a realm without sessions: " + Arrays.toString(queries));
        });
    }

    @TestOnServer
    public void sessionInPreviouslyEmptyRealmIsStillExpired(KeycloakSession session) {
        RealmModel realmModel = session.realms().getRealmByName(REALM_NAME);
        InfinispanTimeUtil.enableTestingTimeService(session);
        try {
            executedSessionQueries(session, realmModel);

            String[] sessionId = new String[1];
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session1 -> {
                session1.getContext().setRealm(realmModel);
                var user = session1.users().addUser(realmModel, "expiring-user");
                sessionId[0] = session1.sessions().createUserSession(null, realmModel, user, "expiring-user", "127.0.0.1", "form", false, null, null, UserSessionModel.SessionPersistenceState.PERSISTENT).getId();
            });

            // the realm has a session now, so the probe finds it and the bucket queries run
            assertWithRetries(() -> {
                String[] queries = executedSessionQueries(session, realmModel);
                assertTrue(Arrays.stream(queries).anyMatch(q -> q.contains("sessionBucket")),
                        "Bucket queries are expected for a realm with sessions: " + Arrays.toString(queries));
            });

            Time.setOffset(realmModel.getSsoSessionIdleTimeout() + SessionTimeoutHelper.PERIODIC_CLEANER_IDLE_TIMEOUT_WINDOW_SECONDS + 60);
            executedSessionQueries(session, realmModel);

            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session1 -> {
                session1.getContext().setRealm(realmModel);
                assertNull(session1.sessions().getUserSession(realmModel, sessionId[0]));
                var user = session1.users().getUserByUsername(realmModel, "expiring-user");
                assertNotNull(user);
                session1.users().removeUser(realmModel, user);
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

    // runs the expiration and returns the HQL of the executed queries on the user session entity
    private static String[] executedSessionQueries(KeycloakSession session, RealmModel realm) {
        Statistics stats = session.getProvider(JpaConnectionProvider.class)
                .getEntityManager()
                .getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        boolean wasEnabled = stats.isStatisticsEnabled();
        stats.setStatisticsEnabled(true);
        stats.clear();
        try {
            KeycloakModelUtils.runJobInTransaction(session.getKeycloakSessionFactory(), session1 -> {
                session1.getContext().setRealm(realm);
                session1.getProvider(UserSessionPersisterProvider.class).removeExpired(realm);
            });
            return Arrays.stream(stats.getQueries())
                    .filter(q -> q.contains("PersistentUserSessionEntity"))
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
