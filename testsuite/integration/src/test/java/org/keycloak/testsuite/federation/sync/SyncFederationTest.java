/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
import java.util.concurrent.TimeUnit;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.testsuite.DummyUserFederationProviderFactory;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.timer.TimerProvider;

/**
 * Test with Dummy providers (For LDAP see {@link org.keycloak.testsuite.federation.ldap.base.LDAPSyncTest}
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SyncFederationTest {

    private static final Logger log = Logger.getLogger(SyncFederationTest.class);

    private static UserFederationProviderModel dummyModel = null;

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            // Other tests may left Time offset uncleared, which could cause issues
            Time.setOffset(0);
        }
    });

    @Test
    public void test01PeriodicSync() {

        // Enable timer for SyncDummyUserFederationProvider
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                dummyModel = appRealm.addUserFederationProvider(DummyUserFederationProviderFactory.PROVIDER_NAME, new HashMap<String, String>(), 1, "test-sync-dummy", -1, 1, 0);
            }

        });

        KeycloakSession session = keycloakRule.startSession();
        try {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            DummyUserFederationProviderFactory dummyFedFactory = (DummyUserFederationProviderFactory)sessionFactory.getProviderFactory(UserFederationProvider.class, DummyUserFederationProviderFactory.PROVIDER_NAME);
            int full = dummyFedFactory.getFullSyncCounter();
            int changed = dummyFedFactory.getChangedSyncCounter();

            // Assert that after some period was DummyUserFederationProvider triggered
            UsersSyncManager usersSyncManager = new UsersSyncManager();
            usersSyncManager.bootstrapPeriodic(sessionFactory, session.getProvider(TimerProvider.class));
            sleep(1800);

            // Cancel timer
            RealmModel appRealm = session.realms().getRealmByName("test");
            usersSyncManager.notifyToRefreshPeriodicSync(session, appRealm, dummyModel, true);
            log.infof("Notified sync manager about cancel periodic sync");

            // This sync is here just to ensure that we have lock (doublecheck that periodic sync, which was possibly triggered before canceling timer is finished too)
            while (true) {
                UserFederationSyncResult result = usersSyncManager.syncChangedUsers(session.getKeycloakSessionFactory(), appRealm.getId(), dummyModel);
                if (result.isIgnored()) {
                    log.infof("Still waiting for lock before periodic sync is finished", result.toString());
                    sleep(1000);
                } else {
                    break;
                }
            }

            // Assert that DummyUserFederationProviderFactory.syncChangedUsers was invoked at least 2 times (once periodically and once for us)
            int newChanged = dummyFedFactory.getChangedSyncCounter();
            Assert.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            Assert.assertTrue("Assertion failed. newChanged=" + newChanged + ", changed=" + changed, newChanged > (changed + 1));

            // Assert that dummy provider won't be invoked anymore
            sleep(1800);
            Assert.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            int newestChanged =  dummyFedFactory.getChangedSyncCounter();
            Assert.assertEquals("Assertion failed. newChanged=" + newChanged + ", newestChanged="  + newestChanged, newChanged, newestChanged);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        // remove dummyProvider
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.removeUserFederationProvider(dummyModel);
            }

        });
    }

    @Test
    public void test02ConcurrentSync() throws Exception {
        SyncDummyUserFederationProviderFactory.restartLatches();

        // Enable timer for SyncDummyUserFederationProvider
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                Map<String, String> config = new HashMap<>();
                config.put(SyncDummyUserFederationProviderFactory.WAIT_TIME, "2000");
                dummyModel = appRealm.addUserFederationProvider(SyncDummyUserFederationProviderFactory.SYNC_PROVIDER_ID, config, 1, "test-sync-dummy", -1, 1, 0);
            }

        });

        KeycloakSession session = keycloakRule.startSession();
        try {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();

            // bootstrap periodic sync
            UsersSyncManager usersSyncManager = new UsersSyncManager();
            usersSyncManager.bootstrapPeriodic(sessionFactory, session.getProvider(TimerProvider.class));

            // Wait and then trigger sync manually. Assert it will be ignored
            sleep(1800);
            RealmModel realm = session.realms().getRealm("test");
            UserFederationSyncResult syncResult = usersSyncManager.syncChangedUsers(sessionFactory, realm.getId(), dummyModel);
            Assert.assertTrue(syncResult.isIgnored());

            // Cancel timer
            usersSyncManager.notifyToRefreshPeriodicSync(session, realm, dummyModel, true);

            // Signal to factory to finish waiting
            SyncDummyUserFederationProviderFactory.latch1.countDown();

        } finally {
            keycloakRule.stopSession(session, true);
        }

        SyncDummyUserFederationProviderFactory.latch2.await(20000, TimeUnit.MILLISECONDS);

        // remove provider
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.removeUserFederationProvider(dummyModel);
            }

        });
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }
}
