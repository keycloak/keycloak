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
            usersSyncManager.removePeriodicSyncForProvider(session.getProvider(TimerProvider.class), dummyModel);

            // Assert that DummyUserFederationProviderFactory.syncChangedUsers was invoked
            int newChanged = dummyFedFactory.getChangedSyncCounter();
            Assert.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            Assert.assertTrue(newChanged > changed);

            // Assert that dummy provider won't be invoked anymore
            sleep(1800);
            Assert.assertEquals(full, dummyFedFactory.getFullSyncCounter());
            Assert.assertEquals(newChanged, dummyFedFactory.getChangedSyncCounter());
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
    public void test02ConcurrentSync() {
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
            usersSyncManager.removePeriodicSyncForProvider(session.getProvider(TimerProvider.class), dummyModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }

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
