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

import java.util.concurrent.atomic.AtomicReference;

import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.storage.UserStorageEventListener;
import org.keycloak.storage.UserStorageProviderClusterEvent;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageProviderModel.SyncMode;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.remote.annotations.TestOnServer;
import org.keycloak.tests.common.CustomProvidersServerConfig;
import org.keycloak.tests.providers.federation.DummyUserFederationProviderFactory;
import org.keycloak.timer.TimerProvider;

import org.junit.jupiter.api.Assertions;

/**
 * Regression tests for the periodic user storage sync tasks when the realm providing them is removed:
 * both the full and the changed sync timers must be cancelled once the realm removal commits, they
 * must be preserved when the removal is rolled back, and the removal handling must not fail when the
 * realm is already gone, e.g. when it was deleted on another cluster node.
 */
@KeycloakIntegrationTest(config = CustomProvidersServerConfig.class)
public class UserStorageSyncRealmRemovalTest {

    private static final String REALM_NAME = "storage-sync-removal";
    private static final int PERIOD = 60;

    @InjectRealm(attachTo = "master")
    ManagedRealm masterRealm;

    @TestOnServer
    public void testBothSyncTimersCancelledAfterRealmRemovalCommit(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        TimerProvider timer = session.getProvider(TimerProvider.class);
        AtomicReference<String> realmId = new AtomicReference<>();
        AtomicReference<String> componentId = new AtomicReference<>();

        createRealmWithSyncProvider(sessionFactory, realmId, componentId);
        String fullTaskName = taskName(componentId.get(), SyncMode.FULL);
        String changedTaskName = taskName(componentId.get(), SyncMode.CHANGED);

        Assertions.assertTrue(timer.getTasks().containsKey(fullTaskName), "Full sync task should be scheduled");
        Assertions.assertTrue(timer.getTasks().containsKey(changedTaskName), "Changed sync task should be scheduled");

        try {
            removeRealmCommitted(sessionFactory, realmId.get());

            // after the removal is committed both periodic tasks must be cancelled, otherwise they keep
            // failing once per period for the now non-existent realm
            Assertions.assertFalse(timer.getTasks().containsKey(fullTaskName),
                    "Full sync task must be cancelled when the realm is removed");
            Assertions.assertFalse(timer.getTasks().containsKey(changedTaskName),
                    "Changed sync task must be cancelled when the realm is removed");
        } finally {
            removeRealmIfExists(sessionFactory, realmId.get());
        }
    }

    @TestOnServer
    public void testBothSyncTimersPreservedWhenRealmRemovalIsRolledBack(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        TimerProvider timer = session.getProvider(TimerProvider.class);
        AtomicReference<String> realmId = new AtomicReference<>();
        AtomicReference<String> componentId = new AtomicReference<>();

        createRealmWithSyncProvider(sessionFactory, realmId, componentId);
        String fullTaskName = taskName(componentId.get(), SyncMode.FULL);
        String changedTaskName = taskName(componentId.get(), SyncMode.CHANGED);

        Assertions.assertTrue(timer.getTasks().containsKey(fullTaskName), "Full sync task should be scheduled");
        Assertions.assertTrue(timer.getTasks().containsKey(changedTaskName), "Changed sync task should be scheduled");

        try {
            // remove the realm in a transaction that fails and rolls back - the tasks must be preserved
            Assertions.assertThrows(RuntimeException.class, () -> KeycloakModelUtils.runJobInTransaction(sessionFactory, s -> {
                RealmModel realm = s.realms().getRealm(realmId.get());
                s.getContext().setRealm(realm);
                s.realms().removeRealm(realmId.get());
                throw new RuntimeException("Simulating a failure to roll the realm removal back");
            }));

            Assertions.assertNotNull(session.realms().getRealm(realmId.get()), "The realm should still exist after the rollback");
            Assertions.assertTrue(timer.getTasks().containsKey(fullTaskName),
                    "Full sync task must be preserved when the realm removal is rolled back");
            Assertions.assertTrue(timer.getTasks().containsKey(changedTaskName),
                    "Changed sync task must be preserved when the realm removal is rolled back");
        } finally {
            removeRealmIfExists(sessionFactory, realmId.get());
        }
    }

    @TestOnServer
    public void testRemovalEventForMissingRealmStillCancelsDanglingTimers(KeycloakSession session) {
        KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
        TimerProvider timer = session.getProvider(TimerProvider.class);
        AtomicReference<String> realmId = new AtomicReference<>();
        AtomicReference<String> componentId = new AtomicReference<>();

        createRealmWithSyncProvider(sessionFactory, realmId, componentId);

        try {
            // remove the realm directly and register the task names as they would remain dangling on a node
            // that has not processed the removal yet, e.g. when the realm was deleted on another node
            removeRealmCommitted(sessionFactory, realmId.get());

            String fullTaskName = taskName(componentId.get(), SyncMode.FULL);
            String changedTaskName = taskName(componentId.get(), SyncMode.CHANGED);
            timer.schedule(() -> { }, PERIOD * 1000L, fullTaskName);
            timer.schedule(() -> { }, PERIOD * 1000L, changedTaskName);
            Assertions.assertTrue(timer.getTasks().containsKey(fullTaskName), "Dangling full sync task should be registered");
            Assertions.assertTrue(timer.getTasks().containsKey(changedTaskName), "Dangling changed sync task should be registered");

            // deliver the removal event as the cluster listener would when the realm is already gone;
            // this must cancel the tasks and must not fail with a missing realm error
            UserStorageProviderModel provider = new UserStorageProviderModel();
            provider.setId(componentId.get());
            provider.setParentId(realmId.get());
            provider.setName("removal-dummy");
            provider.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
            provider.setFullSyncPeriod(PERIOD);
            provider.setChangedSyncPeriod(PERIOD);

            UserStorageProviderClusterEvent event = UserStorageProviderClusterEvent.createEvent(true, realmId.get(), provider);
            new UserStorageEventListener(sessionFactory).eventReceived(event);

            Assertions.assertFalse(timer.getTasks().containsKey(fullTaskName),
                    "Dangling full sync task must be cancelled when the removal event arrives for a missing realm");
            Assertions.assertFalse(timer.getTasks().containsKey(changedTaskName),
                    "Dangling changed sync task must be cancelled when the removal event arrives for a missing realm");
        } finally {
            removeRealmIfExists(sessionFactory, realmId.get());
        }
    }

    private void createRealmWithSyncProvider(KeycloakSessionFactory sessionFactory, AtomicReference<String> realmId,
            AtomicReference<String> componentId) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, s -> {
            RealmModel realm = s.realms().createRealm(KeycloakModelUtils.generateId(), REALM_NAME + "-" + KeycloakModelUtils.generateId());
            realm.setDefaultRole(s.roles().addRealmRole(realm, Constants.DEFAULT_ROLES_ROLE_PREFIX + "-" + realm.getName()));
            s.getContext().setRealm(realm);
            realmId.set(realm.getId());

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setName("removal-dummy");
            model.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
            model.setParentId(realm.getId());
            model.setImportEnabled(true);
            model.setFullSyncPeriod(PERIOD);
            model.setChangedSyncPeriod(PERIOD);
            componentId.set(realm.addComponentModel(model).getId());
        });
    }

    private void removeRealmIfExists(KeycloakSessionFactory sessionFactory, String realmId) {
        if (realmId == null) {
            return;
        }
        KeycloakModelUtils.runJobInTransaction(sessionFactory, s -> {
            RealmModel realm = s.realms().getRealm(realmId);
            if (realm != null) {
                s.getContext().setRealm(realm);
                s.realms().removeRealm(realmId);
            }
        });
    }

    private static void removeRealmCommitted(KeycloakSessionFactory sessionFactory, String realmId) {
        KeycloakModelUtils.runJobInTransaction(sessionFactory, s -> {
            RealmModel realm = s.realms().getRealm(realmId);
            s.getContext().setRealm(realm);
            s.realms().removeRealm(realmId);
        });
    }

    // mirrors UserStorageSyncTask#getTaskName(), which is "UserStorageSyncTask-<providerId>-<syncMode>"
    private static String taskName(String componentId, SyncMode syncMode) {
        return "UserStorageSyncTask-" + componentId + "-" + syncMode;
    }
}
