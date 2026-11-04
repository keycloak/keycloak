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

import java.util.UUID;
import java.util.function.Consumer;

import jakarta.persistence.EntityManager;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.RealmModel;
import org.keycloak.models.jpa.session.PersistentClientSessionEntity;
import org.keycloak.models.jpa.session.PersistentUserSessionEntity;
import org.keycloak.models.session.UserSessionPersisterProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.tests.suites.DatabaseTest;

import org.hibernate.SessionFactory;
import org.hibernate.Timeouts;
import org.hibernate.dialect.lock.spi.LockTimeoutType;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Preparation must lock fresh entities in one query and reject stale or locked rows. */
@DatabaseTest
@KeycloakIntegrationTest(config = SessionResurrectionConcurrencyTest.SessionCachingServerConfig.class)
public class SessionPreparationLockTest {

    @InjectRealm
    ManagedRealm realm;

    @InjectUser(config = SessionResurrectionConcurrencyTest.TestUserConfig.class)
    ManagedUser user;

    @InjectOAuthClient(config = SessionResurrectionConcurrencyTest.DirectGrantClientConfig.class)
    OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @ParameterizedTest
    @CsvSource({"false,false,false", "false,false,true", "false,true,false", "false,true,true",
            "true,false,false", "true,false,true", "true,true,false", "true,true,true"})
    public void preparationUsesOneQuery(boolean clientSession, boolean offline, boolean preloaded) {
        withSession(offline, sessionId -> {
            String realmName = realm.getName();
            runOnServer.run(session -> {
                var realmModel = session.realms().getRealmByName(realmName);
                var clientId = realmModel.getClientByClientId("marker-test-client").getId();
                var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                Object original = preloaded ? find(em, sessionId, clientId, offline, clientSession) : null;
                var persister = session.getProvider(UserSessionPersisterProvider.class);
                assertEquals(1, countStatements(em, () -> {
                    assertTrue(lock(persister, realmModel, sessionId, clientId, offline, clientSession, false));
                    Object managed = find(em, sessionId, clientId, offline, clientSession);
                    assertNotNull(managed);
                    if (preloaded) {
                        assertSame(original, managed, "Preparation must preserve the already managed entity");
                    }
                }), "Preparation should issue one locking SELECT, and subsequent access should reuse the entity");
            });
        });
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    public void previouslyLoadedVersionMustStillMatch(boolean clientSession, boolean offline) {
        withSession(offline, sessionId -> {
            String realmName = realm.getName();
            runOnServer.run(session -> {
                var realmModel = session.realms().getRealmByName(realmName);
                var clientId = realmModel.getClientByClientId("marker-test-client").getId();
                var em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
                Object original = find(em, sessionId, clientId, offline, clientSession);
                assertNotNull(original);
                var factory = session.getKeycloakSessionFactory();
                KeycloakModelUtils.suspendJtaTransaction(factory, () -> KeycloakModelUtils.runJobInTransaction(factory, other -> {
                    var otherEm = other.getProvider(JpaConnectionProvider.class).getEntityManager();
                    String entityType = clientSession ? "PersistentClientSessionEntity" : "PersistentUserSessionEntity";
                    var query = otherEm.createQuery("update " + entityType + " set version = version + 1 where userSessionId = :id and offline = :offline"
                            + (clientSession ? " and clientId = :clientId" : ""));
                    query.setParameter("id", sessionId).setParameter("offline", offline ? "1" : "0");
                    if (clientSession) {
                        query.setParameter("clientId", clientId);
                    }
                    assertEquals(1, query.executeUpdate());
                }));
                assertFalse(lock(session.getProvider(UserSessionPersisterProvider.class), realmModel, sessionId, clientId, offline, clientSession, false));
                assertSame(original, find(em, sessionId, clientId, offline, clientSession));
            });
        });
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    public void lockedRowsRequireFallbackIncludingDeletes(boolean clientSession, boolean offline) {
        boolean skipLockedSupported = runOnServer.fetch(session -> {
            var dialect = session.getProvider(JpaConnectionProvider.class).getEntityManager().getEntityManagerFactory()
                    .unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
            var support = dialect.getLockingSupport().getMetadata().getLockTimeoutType(Timeouts.SKIP_LOCKED);
            System.out.println("SESSION_PREPARATION_DIALECT " + dialect.getClass().getSimpleName()
                    + " version=" + dialect.getVersion() + " skipLocked=" + support);
            return support != LockTimeoutType.NONE;
        }, Boolean.class);
        Assumptions.assumeTrue(skipLockedSupported, "Requires SKIP LOCKED support in the configured Hibernate dialect");
        withSession(offline, sessionId -> {
            String realmName = realm.getName();
            runOnServer.run(session -> {
                var realmModel = session.realms().getRealmByName(realmName);
                var clientId = realmModel.getClientByClientId("marker-test-client").getId();
                assertTrue(lock(session.getProvider(UserSessionPersisterProvider.class), realmModel, sessionId, clientId, offline, clientSession, false));
                var factory = session.getKeycloakSessionFactory();
                KeycloakModelUtils.suspendJtaTransaction(factory, () -> KeycloakModelUtils.runJobInTransaction(factory, other -> {
                    var otherRealm = other.realms().getRealmByName(realmName);
                    var persister = other.getProvider(UserSessionPersisterProvider.class);
                    assertFalse(lock(persister, otherRealm, sessionId, clientId, offline, clientSession, false));
                    // An unlocked read still finds the row. The failed preparation skipped it rather
                    // than observing deletion; the next attempt also exercises the managed-entity path.
                    assertNotNull(find(other.getProvider(JpaConnectionProvider.class).getEntityManager(),
                            sessionId, clientId, offline, clientSession));
                    assertFalse(lock(persister, otherRealm, sessionId, clientId, offline, clientSession, true));
                }));
            });
        });
    }

    @ParameterizedTest
    @CsvSource({"false,false", "false,true", "true,false", "true,true"})
    public void absentRowsRequireFallbackIncludingDeletes(boolean clientSession, boolean offline) {
        String realmName = realm.getName();
        String absentId = UUID.randomUUID().toString();
        runOnServer.run(session -> {
            var realmModel = session.realms().getRealmByName(realmName);
            var clientId = realmModel.getClientByClientId("marker-test-client").getId();
            var persister = session.getProvider(UserSessionPersisterProvider.class);
            assertFalse(lock(persister, realmModel, absentId, clientId, offline, clientSession, false));
            assertFalse(lock(persister, realmModel, absentId, clientId, offline, clientSession, true));
        });
    }

    private void withSession(boolean offline, Consumer<String> test) {
        oauth.scope(offline ? "offline_access" : null);
        var response = oauth.doPasswordGrantRequest(user.getUsername(), "password");
        assertEquals(200, response.getStatusCode());
        try {
            test.accept(oauth.parseRefreshToken(response.getRefreshToken()).getSessionId());
        } finally {
            oauth.doLogout(response.getRefreshToken());
            oauth.scope(null);
        }
    }

    private static Object find(EntityManager em, String sessionId, String clientId, boolean offline, boolean clientSession) {
        String flag = offline ? "1" : "0";
        return clientSession
                ? em.find(PersistentClientSessionEntity.class, new PersistentClientSessionEntity.Key(sessionId, clientId,
                        PersistentClientSessionEntity.LOCAL, PersistentClientSessionEntity.LOCAL, flag))
                : em.find(PersistentUserSessionEntity.class, new PersistentUserSessionEntity.Key(sessionId, flag));
    }

    private static boolean lock(UserSessionPersisterProvider persister, RealmModel realm, String sessionId, String clientId,
                                boolean offline, boolean clientSession, boolean delete) {
        return clientSession ? persister.lockClientSession(realm, sessionId, clientId, offline, delete)
                : persister.lockUserSession(realm, sessionId, offline, delete);
    }

    private static long countStatements(EntityManager em, Runnable action) {
        var statistics = em.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        long before = statistics.getPrepareStatementCount();
        try {
            action.run();
            return statistics.getPrepareStatementCount() - before;
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }
}
