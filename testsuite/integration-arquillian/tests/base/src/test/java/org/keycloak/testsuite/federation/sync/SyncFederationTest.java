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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StoreSyncEvent;
import org.keycloak.storage.UserStoragePrivateUtil;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.UserStorageProviderModel.SyncMode;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.auth.page.AuthRealm;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;

import org.jboss.logging.Logger;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runners.MethodSorters;

/**
 * Test with Dummy providers
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
            sleep(1800);

            // Cancel timer
            StoreSyncEvent.fire(session, appRealm, dummyModel, true);
            log.infof("Notified sync manager about cancel periodic sync");

            // This sync is here just to ensure that we have lock (doublecheck that periodic sync, which was possibly triggered before canceling timer is finished too)
            while (true) {
                SynchronizationResult result = UserStoragePrivateUtil.runPeriodicSync(session.getKeycloakSessionFactory(), dummyModel);
                if (result.isIgnored()) {
                    log.infof("Still waiting for lock before periodic sync is finished: %s", result.toString());
                    sleep(1000);
                } else {
                    break;
                }
            }

            int full = state.get("full");
            int changed = state.get("changed");

            // Assert that DummyUserFederationProviderFactory.syncChangedUsers was invoked at least 2 times (once periodically and once for us)
            int newChanged = dummyFedFactory.getChangedSyncCounter();
            Assertions.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            Assertions.assertTrue(newChanged > (changed + 1), "Assertion failed. newChanged=" + newChanged + ", changed=" + changed);

            // Assert that dummy provider won't be invoked anymore
            sleep(1800);
            Assertions.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            int newestChanged = dummyFedFactory.getChangedSyncCounter();
            Assertions.assertEquals(newChanged, newestChanged, "Assertion failed. newChanged=" + newChanged + ", newestChanged=" + newestChanged);
        });

        // remove dummyProvider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            appRealm.removeComponent(dummyModel);
        });
    }

    @Test
    public void testForceManualSync() {
        // Enable timer for SyncDummyUserFederationProvider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel model = new UserStorageProviderModel();

            model.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
            model.setPriority(1);
            model.setName("test-sync-dummy");
            model.setFullSyncPeriod(1);
            model.setLastSync(0);

            appRealm.addComponentModel(model);
        });

        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

            sleep(dummyModel.getFullSyncPeriod() * 2L);
            session.getContext().setRealm(appRealm);
            SynchronizationResult result = UserStoragePrivateUtil.runFullSync(sessionFactory, dummyModel);
            Assertions.assertFalse(result.isIgnored());
        });

        // remove dummyProvider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            session.getContext().setRealm(appRealm);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            appRealm.removeComponent(dummyModel);
        });
    }

    private static UserStorageProviderModel findDummyProviderModel(RealmModel realm) {
        return findDummyProviderModel(realm, "test-sync-dummy");
    }

    private static UserStorageProviderModel findDummyProviderModel(RealmModel realm, String name) {
        return realm.getComponentsStream()
                .filter(component -> Objects.equals(component.getName(), name))
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

            // Assert that dummy provider wasn't invoked anymore
            sleep(1800);

            int full = state.get("full");
            int changed = state.get("changed");

            Assertions.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            int newChanged = dummyFedFactory.getChangedSyncCounter();
            Assertions.assertEquals(changed, newChanged, "Assertion failed. changed=" + changed + ", newChanged=" + newChanged);
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
            sleep(1800);

            // Cancel timer
            StoreSyncEvent.fire(session, appRealm, dummyModel, true);
            log.infof("Notified sync manager about cancel periodic sync");

            // This sync is here just to ensure that we have lock (doublecheck that periodic sync, which was possibly triggered before canceling timer is finished too)
            while (true) {
                SynchronizationResult result = UserStoragePrivateUtil.runPeriodicSync(session.getKeycloakSessionFactory(), dummyModel);
                if (result.isIgnored()) {
                    log.infof("Still waiting for lock before periodic sync is finished: %s", result.toString());
                    sleep(1000);
                } else {
                    break;
                }
            }

            int full = state.get("full");
            int changed = state.get("changed");

            // Assert that DummyUserFederationProviderFactory.syncChangedUsers was invoked at least 1 time
            int newChanged = dummyFedFactory.getChangedSyncCounter();
            Assertions.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            log.info("Asserting. newChanged=" + newChanged + " > changed=" + changed);
            Assertions.assertTrue(newChanged > (changed + 1), "Assertion failed. newChanged=" + newChanged + ", changed=" + changed);

            // Assert that dummy provider won't be invoked anymore
            sleep(1800);
            Assertions.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            int newestChanged = dummyFedFactory.getChangedSyncCounter();
            Assertions.assertEquals(newChanged, newestChanged, "Assertion failed. newChanged=" + newChanged + ", newestChanged=" + newestChanged);
        });


        // remove dummyProvider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            appRealm.removeComponent(dummyModel);
        });
    }


    @Test
    public void test03ConcurrentSync() {
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
            model.getConfig().putSingle(SyncDummyUserFederationProviderFactory.WAIT_TIME, "20");
            ComponentModel dummyModel = new UserStorageProviderModel(appRealm.addComponentModel(model));
        });


        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

            // Wait and then trigger sync manually. Assert it will be ignored
            try {
                SyncDummyUserFederationProviderFactory.latchStarted.await(20000, TimeUnit.MILLISECONDS);
                SynchronizationResult syncResult = UserStoragePrivateUtil.runPeriodicSync(sessionFactory, dummyModel);
                Assertions.assertTrue(syncResult.isIgnored());

                // Cancel timer
                StoreSyncEvent.fire(session, appRealm, dummyModel, true);

                // Signal to factory to finish waiting
                SyncDummyUserFederationProviderFactory.latchWait.countDown();

                // wait the task to be finished
                SyncDummyUserFederationProviderFactory.latchFinished.await(20000, TimeUnit.MILLISECONDS);

                // This sync is here just to ensure that we have lock (doublecheck that periodic sync, which was possibly triggered before canceling timer is finished too)
                while (true) {
                    SynchronizationResult result = UserStoragePrivateUtil.runPeriodicSync(session.getKeycloakSessionFactory(), dummyModel);
                    if (result.isIgnored()) {
                        log.infof("Still waiting for lock before periodic sync is finished: %s", result.toString());
                        sleep(1000);
                    } else {
                        break;
                    }
                }
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

    @Test
    public void test04IgnoredSync() throws Exception {
        // Add IgnoredDummyUserFederationProviderFactory provider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setProviderId(IgnoredDummyUserFederationProviderFactory.IGNORED_PROVIDER_ID);
            model.setPriority(1);
            model.setName("test-sync-dummy");
            model.setFullSyncPeriod(-1);
            model.setChangedSyncPeriod(-1);
            model.setLastSync(0);
            appRealm.addComponentModel(model);
        });

        // run both sync methods that will be ignored
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = UserStoragePrivateUtil.runFullSync(sessionFactory, dummyModel);
            Assertions.assertTrue(syncResult.isIgnored());
            syncResult = UserStoragePrivateUtil.runPeriodicSync(sessionFactory, dummyModel);
            Assertions.assertTrue(syncResult.isIgnored());
        });

        // assert the last sync is not updated
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            Assertions.assertEquals(0, dummyModel.getLastSync());
        });

        // remove provider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            appRealm.removeComponent(dummyModel);
        });
    }

    @Test
    public void test05LongSync() throws Exception {
        // Add IgnoredDummyUserFederationProviderFactory provider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setProviderId(IgnoredDummyUserFederationProviderFactory.IGNORED_PROVIDER_ID);
            model.setPriority(1);
            model.setName("test-sync-dummy");
            model.setFullSyncPeriod(2147484);
            model.setChangedSyncPeriod(2147484);
            model.setLastSync(0);
            appRealm.addComponentModel(model);
        });

        // run both sync methods that will be ignored
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = UserStoragePrivateUtil.runFullSync(sessionFactory, dummyModel);
            Assertions.assertTrue(syncResult.isIgnored());
            syncResult = UserStoragePrivateUtil.runPeriodicSync(sessionFactory, dummyModel);
            Assertions.assertTrue(syncResult.isIgnored());
        });

        // assert the last sync is not updated
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            Assertions.assertEquals(0, dummyModel.getLastSync());
        });

        // remove provider
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
            UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm);
            appRealm.removeComponent(dummyModel);
        });
    }

    /**
     * The periodic gate (UserStorageSyncTask.isSyncPeriod) measures the time since the last sync,
     * and the timer schedules each periodic run 'period' seconds after the previous run started.
     * The gate must therefore measure from the start of the previous run: if the finish time of a
     * run longer than one second is recorded instead, the next timer tick is skipped and the
     * effective sync period doubles.
     *
     * See https://github.com/keycloak/keycloak/issues/53930
     */
    @Test
    public void test06PeriodicSyncRunsAtConfiguredPeriodWhenSyncTakesLongerThanASecond() {
        final String providerName = "test-sync-dummy-slow";

        // Enable the timer for the dummy provider, with a full sync that takes 1.2 seconds
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);

            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
            model.setPriority(1);
            model.setName(providerName);
            model.setFullSyncPeriod(2);
            model.setChangedSyncPeriod(-1);
            model.setLastSync(0);
            model.getConfig().putSingle(DummyUserFederationProviderFactory.SYNC_DELAY_MS_CONFIG, "1200");

            appRealm.addComponentModel(model);
        });

        try {
            int baseline = testingClient.server().fetch(session -> {
                KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
                return ((DummyUserFederationProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME)).getFullSyncCounter();
            }, Integer.class);

            // The first periodic run starts one period after the component is added and takes 1.2
            // seconds. Wait for it to finish so that the start of a run and the recorded lastSync
            // stamp can be compared. The timer keeps running afterwards.
            Map<String, Integer> firstRun = waitForFinishedRun(providerName, 15000);
            int runStart = firstRun.get("runStart");
            int lastSyncStamp = firstRun.get("lastSync");

            // lastSync must record when the sync run started, not when it finished. The observed
            // run took 1.2 seconds, so a stamp after the start of the run means the finish time
            // was recorded and the next timer tick will be skipped.
            Assertions.assertTrue(lastSyncStamp <= runStart,
                    "lastSync should record the start of the sync run, but the recorded time was " + lastSyncStamp + " while the run started at " + runStart);

            // A run taking 1.2 seconds must not postpone the following timer ticks. Within the
            // window below the timer ticks three times after the first run started, so at least
            // three runs in total should have started, one per configured period. With the finish
            // time recorded instead, every other tick would be skipped.
            sleep(5500);

            int totalRuns = testingClient.server().fetch(session -> {
                KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
                return ((DummyUserFederationProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME)).getFullSyncCounter();
            }, Integer.class) - baseline;

            Assertions.assertTrue(totalRuns >= 3, "Periodic sync should start once per period, but only " + totalRuns + " runs started");
        } finally {
            // Cancel the timer and remove the provider
            testingClient.server().run(session -> {
                RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
                UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm, providerName);

                if (dummyModel != null) {
                    StoreSyncEvent.fire(session, appRealm, dummyModel, true);
                    appRealm.removeComponent(dummyModel);
                }
            });
        }
    }

    private Map<String, Integer> waitForFinishedRun(String providerName, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;

        while (System.currentTimeMillis() < deadline) {
            Map<String, Integer> state = testingClient.server().fetch(session -> {
                KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
                DummyUserFederationProviderFactory dummyFedFactory = (DummyUserFederationProviderFactory) sessionFactory.getProviderFactory(UserStorageProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);
                RealmModel appRealm = session.realms().getRealmByName(AuthRealm.TEST);
                UserStorageProviderModel dummyModel = findDummyProviderModel(appRealm, providerName);

                Map<String, Integer> state1 = new HashMap<>();
                state1.put("lastSync", dummyModel.getLastSync(SyncMode.FULL));
                state1.put("runStart", dummyFedFactory.getLastFullSyncStartEpochSec());
                state1.put("inFlight", dummyFedFactory.isFullSyncInFlight() ? 1 : 0);
                return state1;
            }, Map.class);

            if (state.get("lastSync") > 0 && state.get("runStart") > 0 && state.get("inFlight") == 0) {
                return state;
            }

            sleep(500);
        }

        throw new AssertionError("No periodic sync run finished within " + timeoutMs + " ms");
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
