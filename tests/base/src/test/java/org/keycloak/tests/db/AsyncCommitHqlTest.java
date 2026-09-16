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

import java.util.UUID;

import jakarta.persistence.EntityManager;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.Profile;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.connections.jpa.support.EntityManagerProxy;
import org.keycloak.events.Event;
import org.keycloak.events.EventStoreProvider;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.models.SingleUseObjectProvider;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;

import org.hibernate.Session;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the {@link EntityManagerProxy} Query wrapper marks the Hibernate session
 * for synchronous commit when an HQL {@code executeUpdate()} modifies rows.
 * <p>
 * Requires the stateless feature, which uses JPA-backed providers for single-use objects
 * and revoked tokens. Without stateless, these providers use Infinispan and never issue HQL.
 * <p>
 * On databases that support async commit (PostgreSQL, SQL Server, Oracle), the flag
 * must be set. On unsupported databases (H2, MySQL, MariaDB), async commit is not enabled,
 * so no wrapping occurs and the flag must not be set.
 */
@KeycloakIntegrationTest
public class AsyncCommitHqlTest {

    @InjectRealm
    ManagedRealm managedRealm;

    @InjectAdminClient(mode = InjectAdminClient.Mode.BOOTSTRAP)
    Keycloak adminClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @Test
    public void putIfAbsentSetsSyncCommitFlag() {
        Assumptions.assumeTrue(isStatelessFeatureEnabled(),
                "Test requires stateless feature (JPA-backed SingleUseObjectProvider)");
        runOnServer.run(session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            var dialect = em.getEntityManagerFactory()
                    .unwrap(SessionFactoryImplementor.class).getJdbcServices().getDialect();
            // Coarsely check if the DB at hand is expected to support async commit in our setup.
            // This avoids replicating the full logic in Keycloak to avoid duplication and out-of-sync code
            // based on the assumption that for the example the PostgreSQL database we provide for testing
            // will always support async commits (and is not an Aurora DB with logical replication enabled).
            // If the async detection logic will break for any of those databases in the Keycloak main code,
            // this test will rightfully fail. 
            // For this reason, it avoids the `EntityManagerProxy.isAsyncCommitEnabled(em)` that is used
            // in the other tests in this class.
            boolean databaseSupportsAsyncCommit = dialect instanceof PostgreSQLDialect
                    || dialect instanceof SQLServerDialect
                    || dialect instanceof OracleDialect;

            assertEquals(databaseSupportsAsyncCommit, EntityManagerProxy.isAsyncCommitEnabled(em),
                    "Async commit enablement must match database support (" + dialect.getClass().getSimpleName() + ")");

            String key = "async-commit-test-" + UUID.randomUUID();
            boolean inserted = session.getProvider(SingleUseObjectProvider.class).putIfAbsent(key, 300);
            assertTrue(inserted);

            boolean flagSet = Boolean.TRUE.equals(
                    em.unwrap(Session.class).getProperties().get(EntityManagerProxy.SYNC_COMMIT_REQUIRED));

            if (databaseSupportsAsyncCommit) {
                assertTrue(flagSet,
                        "On " + dialect.getClass().getSimpleName() + ", the sync flag must be set after executeUpdate modifies rows");
            } else {
                assertFalse(flagSet,
                        "On " + dialect.getClass().getSimpleName() + ", no Query wrapping occurs");
            }
        });
    }

    @Test
    public void executeUpdateWithZeroRowsDoesNotSetFlag() {
        runOnServer.run(session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            Assumptions.assumeTrue(EntityManagerProxy.isAsyncCommitEnabled(em),
                    "Only meaningful when async commit is enabled (Query wrapping active)");

            int rows = em.createQuery("UPDATE RealmEntity r SET r.displayName = r.displayName WHERE r.id = :id")
                    .setParameter("id", "non-existent-realm-id")
                    .executeUpdate();
            assertEquals(0, rows, "Update targeting non-existent realm must affect zero rows");

            boolean flagSet = Boolean.TRUE.equals(
                    em.unwrap(Session.class).getProperties().get(EntityManagerProxy.SYNC_COMMIT_REQUIRED));
            assertFalse(flagSet,
                    "sync_commit_required flag must not be set when executeUpdate modifies zero rows");
        });
    }

    @Test
    public void selectQueryDoesNotSetFlag() {
        String realmName = managedRealm.getName();

        runOnServer.run(session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            Assumptions.assumeTrue(EntityManagerProxy.isAsyncCommitEnabled(em),
                    "Only meaningful when async commit is enabled (Query wrapping active)");

            RealmModel realm = session.realms().getRealmByName(realmName);

            em.createQuery("SELECT r FROM RealmEntity r WHERE r.id = :id")
                    .setParameter("id", realm.getId())
                    .getResultList();

            boolean flagSet = Boolean.TRUE.equals(
                    em.unwrap(Session.class).getProperties().get(EntityManagerProxy.SYNC_COMMIT_REQUIRED));
            assertFalse(flagSet,
                    "sync_commit_required flag must not be set for SELECT queries");
        });
    }

    @Test
    public void eventEntityAllowsAsyncCommit() {
        String realmName = managedRealm.getName();

        runOnServer.run(session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            Assumptions.assumeTrue(EntityManagerProxy.isAsyncCommitEnabled(em),
                    "Only meaningful when async commit is enabled");

            RealmModel realm = session.realms().getRealmByName(realmName);

            Event event = new Event();
            event.setId(UUID.randomUUID().toString());
            event.setRealmId(realm.getId());
            event.setType(EventType.LOGIN);
            event.setTime(System.currentTimeMillis());
            session.getProvider(EventStoreProvider.class).onEvent(event);
            em.flush();

            boolean flagSet = Boolean.TRUE.equals(
                    em.unwrap(Session.class).getProperties().get(EntityManagerProxy.SYNC_COMMIT_REQUIRED));
            assertFalse(flagSet,
                    "Events implement AsynchronousCommitAllowed — sync flag must not be set");
        });
    }

    @Test
    public void authenticationSessionAllowsAsyncCommit() {
        String realmName = managedRealm.getName();

        runOnServer.run(session -> {
            EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
            Assumptions.assumeTrue(EntityManagerProxy.isAsyncCommitEnabled(em),
                    "Only meaningful when async commit is enabled");

            RealmModel realm = session.realms().getRealmByName(realmName);
            session.authenticationSessions().createRootAuthenticationSession(realm, UUID.randomUUID().toString());

            boolean flagSet = Boolean.TRUE.equals(
                    em.unwrap(Session.class).getProperties().get(EntityManagerProxy.SYNC_COMMIT_REQUIRED));
            assertFalse(flagSet,
                    "Authentication session HQL insert uses ASYNC_COMMIT_ALLOWED hint — sync flag must not be set");
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
}
