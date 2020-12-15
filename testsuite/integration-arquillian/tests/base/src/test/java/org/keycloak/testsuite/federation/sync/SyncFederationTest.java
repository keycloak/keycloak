/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.sync;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.managers.UserStorageSyncManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;
import org.keycloak.timer.TimerProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * Test with Dummy providers
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@AuthServerContainerExclude(AuthServer.REMOTE)
public class SyncFederationTest extends AbstractAuthTest {

    private static final Logger log = Logger.getLogger(SyncFederationTest.class);

    /**
     * Test that period sync is triggered when creating a synchronized User Storage Provider
     *
     */
    @Test
    public void test01PeriodicSyncOnCreate() {

        final Map<String, Integer> state = testingClient.server().fetch(session -> {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            DummyUserFederationProviderFactory dummyFedFactory = (DummyUserFederationProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);

            int full = dummyFedFactory.getFullSyncCounter();
            int changed = dummyFedFactory.getChangedSyncCounter();

            Map<String, Integer> state1 = new HashMap<>();
            state1.put("full", full);
            state1.put("changed", changed);
            return state1;

        }, Map.class);

        // Enable timer for SyncDummyUserFederationProvider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
            model.setPriority(1);
            model.setName("test-sync-dummy");
            model.setFullSyncPeriod(-1);
            model.setChangedSyncPeriod(1);
            model.setLastSync(0);
            ComponentModel dummyModel = new UserStorageProviderModel(appRealm.addComponentModel(model));
        });

        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            DummyUserFederationProviderFactory dummyFedFactory = (DummyUserFederationProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);

            // Assert that after some period was DummyUserFederationProvider triggered
            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();
            sleep(1800);

            // Cancel timer
            usersSyncManager.notifyToRefreshPeriodicSync(session, appRealm, dummyModel, true);
            log.infof("Notified sync manager about cancel periodic sync");

            // This sync is here just to ensure that we have lock (doublecheck that periodic sync, which was possibly triggered before canceling timer is finished too)
            while (true) {
                SynchronizationResult result = usersSyncManager.syncChangedUsers(session.getKeycloakSessionFactory(), appRealm.getId(), dummyModel);
                if (result.isIgnored()) {
                    log.infof("Still waiting for lock before periodic sync is finished", result.toString());
                    sleep(1000);
                } else {
                    break;
                }
            }

            int full = state.get("full");
            int changed = state.get("changed");

            // Assert that DummyUserFederationProviderFactory.syncChangedUsers was invoked at least 2 times (once periodically and once for us)
            int newChanged = dummyFedFactory.getChangedSyncCounter();
            Assert.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            Assert.assertTrue("Assertion failed. newChanged=" + newChanged + ", changed=" + changed, newChanged > (changed + 1));

            // Assert that dummy provider won't be invoked anymore
            sleep(1800);
            Assert.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            int newestChanged = dummyFedFactory.getChangedSyncCounter();
            Assert.assertEquals("Assertion failed. newChanged=" + newChanged + ", newestChanged=" + newestChanged, newChanged, newestChanged);
        });

        // remove dummyProvider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            appRealm.removeComponent(dummyModel);
        });
    }


    private static final UserStorageProviderModel findDummyProviderModel(RealmModel realm) {
        return realm.getComponentsStream()
                .filter(component -> Objects.equals(component.getName(), "test-sync-dummy"))
                .map(UserStorageProviderModel::new)
                .findFirst()
                .orElse(null);
    }

    /**
     * Test that period sync is triggered when updating a synchronized User Storage Provider to have a non-negative sync period
     *
     */
    @Test
    public void test02PeriodicSyncOnUpdate() {

        final Map<String, Integer> state = testingClient.server().fetch(session -> {

            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            DummyUserFederationProviderFactory dummyFedFactory = (DummyUserFederationProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);

            int full = dummyFedFactory.getFullSyncCounter();
            int changed = dummyFedFactory.getChangedSyncCounter();

            Map<String, Integer> state1 = new HashMap<>();
            state1.put("full", full);
            state1.put("changed", changed);
            return state1;

        }, Map.class);


        // Configure sync without timer for SyncDummyUserFederationProvider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
            model.setPriority(1);
            model.setName("test-sync-dummy");
            model.setFullSyncPeriod(-1);
            model.setChangedSyncPeriod(-1);
            model.setLastSync(0);
            ComponentModel dummyModel = new UserStorageProviderModel(appRealm.addComponentModel(model));
        });

        testingClient.server().run(session -> {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            DummyUserFederationProviderFactory dummyFedFactory = (DummyUserFederationProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);

            // Assert that after some period was DummyUserFederationProvider triggered
            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();

            // Assert that dummy provider wasn't invoked anymore
            sleep(1800);

            int full = state.get("full");
            int changed = state.get("changed");

            Assert.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            int newChanged = dummyFedFactory.getChangedSyncCounter();
            Assert.assertEquals("Assertion failed. changed=" + changed + ", newChanged=" + newChanged, changed, newChanged);
        });

        // Re-enable periodic sync for changed users
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);

            dummyModel.setChangedSyncPeriod(1);
            appRealm.updateComponent(dummyModel);
        });


        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            DummyUserFederationProviderFactory dummyFedFactory = (DummyUserFederationProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);

            // Assert that after some period was DummyUserFederationProvider triggered
            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();
            sleep(1800);

            // Cancel timer
            usersSyncManager.notifyToRefreshPeriodicSync(session, appRealm, dummyModel, true);
            log.infof("Notified sync manager about cancel periodic sync");

            // This sync is here just to ensure that we have lock (doublecheck that periodic sync, which was possibly triggered before canceling timer is finished too)
            while (true) {
                SynchronizationResult result = usersSyncManager.syncChangedUsers(session.getKeycloakSessionFactory(), appRealm.getId(), dummyModel);
                if (result.isIgnored()) {
                    log.infof("Still waiting for lock before periodic sync is finished", result.toString());
                    sleep(1000);
                } else {
                    break;
                }
            }

            int full = state.get("full");
            int changed = state.get("changed");

            // Assert that DummyUserFederationProviderFactory.syncChangedUsers was invoked at least 1 time
            int newChanged = dummyFedFactory.getChangedSyncCounter();
            Assert.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            log.info("Asserting. newChanged=" + newChanged + " > changed=" + changed);
            Assert.assertTrue("Assertion failed. newChanged=" + newChanged + ", changed=" + changed, newChanged > (changed + 1));

            // Assert that dummy provider won't be invoked anymore
            sleep(1800);
            Assert.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            int newestChanged = dummyFedFactory.getChangedSyncCounter();
            Assert.assertEquals("Assertion failed. newChanged=" + newChanged + ", newestChanged=" + newestChanged, newChanged, newestChanged);
        });


        // remove dummyProvider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            appRealm.removeComponent(dummyModel);
        });
    }


    @Test
    public void test03ConcurrentSync() throws Exception {
        // Enable timer for SyncDummyUserFederationProvider
        testingClient.server().run(session -> {
            SyncDummyUserFederationProviderFactory.restartLatches();

            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setProviderId(SyncDummyUserFederationProviderFactory.SYNC_PROVIDER_ID);
            model.setPriority(1);
            model.setName("test-sync-dummy");
            model.setFullSyncPeriod(-1);
            model.setChangedSyncPeriod(1);
            model.setLastSync(0);
            model.getConfig().putSingle(SyncDummyUserFederationProviderFactory.WAIT_TIME, "2000");
            ComponentModel dummyModel = new UserStorageProviderModel(appRealm.addComponentModel(model));
        });


        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);

            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

            // bootstrap periodic sync
            UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();
            usersSyncManager.bootstrapPeriodic(sessionFactory, session.getProvider(TimerProvider.class));

            // Wait and then trigger sync manually. Assert it will be ignored
            sleep(1800);
            SynchronizationResult syncResult = usersSyncManager.syncChangedUsers(sessionFactory, appRealm.getId(), dummyModel);
            Assert.assertTrue(syncResult.isIgnored());

            // Cancel timer
            usersSyncManager.notifyToRefreshPeriodicSync(session, appRealm, dummyModel, true);

            // Signal to factory to finish waiting
            SyncDummyUserFederationProviderFactory.latch1.countDown();

            try {
                SyncDummyUserFederationProviderFactory.latch2.await(20000, TimeUnit.MILLISECONDS);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });

        // remove provider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            appRealm.removeComponent(dummyModel);
        });
    }


    private static void sleep(long ms) {
        try {
            log.infof("Sleeping for %d ms", ms);
            Thread.sleep(ms);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }
}
