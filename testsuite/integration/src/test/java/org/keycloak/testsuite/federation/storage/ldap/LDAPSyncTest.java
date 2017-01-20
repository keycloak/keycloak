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

package org.keycloak.testsuite.federation.storage.ldap;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.common.util.Time;
import org.keycloak.component.ComponentModel;
import org.keycloak.services.managers.UserStorageSyncManager;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.UserCache;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.user.SynchronizationResult;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPSyncTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static UserStorageProviderModel ldapModel = null;

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            // Other tests may left Time offset uncleared, which could cause issues
            Time.setOffset(0);

            MultivaluedHashMap<String,String> ldapConfig = LDAPTestUtils.getLdapRuleConfig(ldapRule);
            ldapConfig.putSingle(LDAPConstants.SYNC_REGISTRATIONS, "false");
            ldapConfig.putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setLastSync(0);
            model.setChangedSyncPeriod(-1);
            model.setFullSyncPeriod(-1);
            model.setName("test-ldap");
            model.setPriority(0);
            model.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
            model.setConfig(ldapConfig);

            ldapModel = new UserStorageProviderModel(appRealm.addComponentModel(model));


            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);

            // Delete all LDAP users and add 5 new users for testing
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            for (int i=1 ; i<=5 ; i++) {
                LDAPObject ldapUser = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "user" + i, "User" + i + "FN", "User" + i + "LN", "user" + i + "@email.org", null, "12" + i);
                LDAPTestUtils.updateLDAPPassword(ldapFedProvider, ldapUser, "Password1");
            }
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

//    @Test
//    public void test01runit() throws Exception {
//        Thread.sleep(10000000);
//    }

    @Test
    public void test01LDAPSync() {
        UserStorageSyncManager usersSyncManager = new UserStorageSyncManager();

        // wait a bit
        sleep(ldapRule.getSleepTime());

        KeycloakSession session = keycloakRule.startSession();
        try {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = usersSyncManager.syncAllUsers(sessionFactory, "test", ldapModel);
            LDAPTestUtils.assertSyncEquals(syncResult, 5, 0, 0, 0);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userLocalStorage();
            // Assert users imported
            LDAPTestUtils.assertUserImported(userProvider, testRealm, "user1", "User1FN", "User1LN", "user1@email.org", "121");
            LDAPTestUtils.assertUserImported(userProvider, testRealm, "user2", "User2FN", "User2LN", "user2@email.org", "122");
            LDAPTestUtils.assertUserImported(userProvider, testRealm, "user3", "User3FN", "User3LN", "user3@email.org", "123");
            LDAPTestUtils.assertUserImported(userProvider, testRealm, "user4", "User4FN", "User4LN", "user4@email.org", "124");
            LDAPTestUtils.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org", "125");

            // Assert lastSync time updated
            Assert.assertTrue(ldapModel.getLastSync() > 0);
            for (UserStorageProviderModel persistentFedModel : testRealm.getUserStorageProviders()) {
                if (LDAPStorageProviderFactory.PROVIDER_NAME.equals(persistentFedModel.getProviderId())) {
                    Assert.assertTrue(persistentFedModel.getLastSync() > 0);
                } else {
                    // Dummy provider has still 0
                    Assert.assertEquals(0, persistentFedModel.getLastSync());
                }
            }

            // wait a bit
            sleep(ldapRule.getSleepTime());

            // Add user to LDAP and update 'user5' in LDAP
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapFedProvider, testRealm, "user6", "User6FN", "User6LN", "user6@email.org", null, "126");
            LDAPObject ldapUser5 = ldapFedProvider.loadLDAPUserByUsername(testRealm, "user5");
            // NOTE: Changing LDAP attributes directly here
            ldapUser5.setSingleAttribute(LDAPConstants.EMAIL, "user5Updated@email.org");
            ldapUser5.setSingleAttribute(LDAPConstants.POSTAL_CODE, "521");
            ldapFedProvider.getLdapIdentityStore().update(ldapUser5);

            // Assert still old users in local provider
            LDAPTestUtils.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org", "125");
            Assert.assertNull(userProvider.getUserByUsername("user6", testRealm));

            // Trigger partial sync
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = usersSyncManager.syncChangedUsers(sessionFactory, "test", ldapModel);
            LDAPTestUtils.assertSyncEquals(syncResult, 1, 1, 0, 0);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userLocalStorage();
            // Assert users updated in local provider
            LDAPTestUtils.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5updated@email.org", "521");
            LDAPTestUtils.assertUserImported(userProvider, testRealm, "user6", "User6FN", "User6LN", "user6@email.org", "126");
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void test02duplicateUsernameAndEmailSync() {
        LDAPObject duplicatedLdapUser;

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");

            LDAPTestUtils.addLocalUser(session, testRealm, "user7", "user7@email.org", "password");
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            // Add user to LDAP with duplicated username "user7"
            duplicatedLdapUser = LDAPTestUtils.addLDAPUser(ldapFedProvider, testRealm, "user7", "User7FN", "User7LN", "user7-something@email.org", null, "126");

        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Assert syncing from LDAP fails due to duplicated username
            SynchronizationResult result = new UserStorageSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), "test", ldapModel);
            Assert.assertEquals(1, result.getFailed());

            // Remove "user7" from LDAP
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            ldapFedProvider.getLdapIdentityStore().remove(duplicatedLdapUser);

            // Add user to LDAP with duplicated email "user7@email.org"
            duplicatedLdapUser = LDAPTestUtils.addLDAPUser(ldapFedProvider, testRealm, "user7-something", "User7FNN", "User7LNL", "user7@email.org", null, "126");
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Assert syncing from LDAP fails due to duplicated email
            SynchronizationResult result = new UserStorageSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), "test", ldapModel);
            Assert.assertEquals(1, result.getFailed());
            Assert.assertNull(session.userLocalStorage().getUserByUsername("user7-something", testRealm));

            // Update LDAP user to avoid duplicated email
            duplicatedLdapUser.setSingleAttribute(LDAPConstants.EMAIL, "user7-changed@email.org");
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            ldapFedProvider.getLdapIdentityStore().update(duplicatedLdapUser);

            // Assert user successfully synced now
            result = new UserStorageSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), "test", ldapModel);
            Assert.assertEquals(0, result.getFailed());
        } finally {
            keycloakRule.stopSession(session, true);
        }

        // Assert user imported in another transaction
        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            LDAPTestUtils.assertUserImported(session.userLocalStorage(), testRealm, "user7-something", "User7FNN", "User7LNL", "user7-changed@email.org", "126");
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    // KEYCLOAK-1571
    @Test
    public void test03SameUUIDAndUsernameSync() {
        KeycloakSession session = keycloakRule.startSession();
        String origUuidAttrName;

        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Remove all users from model
            for (UserModel user : session.userLocalStorage().getUsers(testRealm, true)) {
                session.userLocalStorage().removeUser(testRealm, user);
            }

            UserStorageProviderModel providerModel = KeycloakModelUtils.findUserStorageProviderByName(ldapModel.getName(), testRealm);

            // Change name of UUID attribute to same like usernameAttribute
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            String uidAttrName = ldapFedProvider.getLdapIdentityStore().getConfig().getUsernameLdapAttribute();
            origUuidAttrName = providerModel.getConfig().getFirst(LDAPConstants.UUID_LDAP_ATTRIBUTE);
            providerModel.getConfig().putSingle(LDAPConstants.UUID_LDAP_ATTRIBUTE, uidAttrName);

            // Need to change this due to ApacheDS pagination bug (For other LDAP servers, pagination works fine) TODO: Remove once ApacheDS upgraded and pagination is fixed
            providerModel.getConfig().putSingle(LDAPConstants.BATCH_SIZE_FOR_SYNC, "10");
            testRealm.updateComponent(providerModel);

        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserStorageProviderModel providerModel = KeycloakModelUtils.findUserStorageProviderByName(ldapModel.getName(), testRealm);

            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = new UserStorageSyncManager().syncAllUsers(sessionFactory, "test", providerModel);
            Assert.assertEquals(0, syncResult.getFailed());

        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Assert users imported with correct LDAP_ID
            LDAPTestUtils.assertUserImported(session.users(), testRealm, "user1", "User1FN", "User1LN", "user1@email.org", "121");
            LDAPTestUtils.assertUserImported(session.users(), testRealm, "user2", "User2FN", "User2LN", "user2@email.org", "122");
            UserModel user1 = session.users().getUserByUsername("user1", testRealm);
            Assert.assertEquals("user1", user1.getFirstAttribute(LDAPConstants.LDAP_ID));

            // Revert config changes
            UserStorageProviderModel providerModel = KeycloakModelUtils.findUserStorageProviderByName(ldapModel.getName(), testRealm);
            providerModel.getConfig().putSingle(LDAPConstants.UUID_LDAP_ATTRIBUTE, origUuidAttrName);
            testRealm.updateComponent(providerModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    // KEYCLOAK-1728
    @Test
    public void test04MissingLDAPUsernameSync() {
        KeycloakSession session = keycloakRule.startSession();
        String origUsernameAttrName;

        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Remove all users from model
            for (UserModel user : session.userLocalStorage().getUsers(testRealm, true)) {
                System.out.println("trying to delete user: " + user.getUsername());
                UserCache userCache = session.userCache();
                if (userCache != null) {
                    userCache.evict(testRealm, user);
                }
                session.userLocalStorage().removeUser(testRealm, user);
            }

            UserStorageProviderModel providerModel = KeycloakModelUtils.findUserStorageProviderByName(ldapModel.getName(), testRealm);

            // Add street mapper and add some user including street
            ComponentModel streetMapper = LDAPTestUtils.addUserAttributeMapper(testRealm, ldapModel, "streetMapper", "street", LDAPConstants.STREET);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject streetUser = LDAPTestUtils.addLDAPUser(ldapFedProvider, testRealm, "user8", "User8FN", "User8LN", "user8@email.org", "user8street", "126");

            // Change name of username attribute name to street
            origUsernameAttrName = providerModel.getConfig().getFirst(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
            providerModel.getConfig().putSingle(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, "street");

            // Need to change this due to ApacheDS pagination bug (For other LDAP servers, pagination works fine) TODO: Remove once ApacheDS upgraded and pagination is fixed
            providerModel.getConfig().putSingle(LDAPConstants.BATCH_SIZE_FOR_SYNC, "10");
            testRealm.updateComponent(providerModel);

        } finally {
            keycloakRule.stopSession(session, true);
        }

        // Just user8 synced. All others failed to sync
        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserStorageProviderModel providerModel = KeycloakModelUtils.findUserStorageProviderByName(ldapModel.getName(), testRealm);

            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            SynchronizationResult syncResult = new UserStorageSyncManager().syncAllUsers(sessionFactory, "test", providerModel);
            Assert.assertEquals(1, syncResult.getAdded());
            Assert.assertTrue(syncResult.getFailed() > 0);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Revert config changes
            UserStorageProviderModel providerModel = KeycloakModelUtils.findUserStorageProviderByName(ldapModel.getName(), testRealm);
            providerModel.getConfig().putSingle(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, origUsernameAttrName);
            testRealm.updateComponent(providerModel);
            ComponentModel streetMapper = LDAPTestUtils.getSubcomponentByName(testRealm, providerModel, "streetMapper");
            testRealm.removeComponent(streetMapper);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }
}
