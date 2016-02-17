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

package org.keycloak.testsuite.federation.ldap.base;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.testsuite.federation.ldap.FederationTestUtils;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.common.util.Time;

import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPSyncTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static UserFederationProviderModel ldapModel = null;

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            // Other tests may left Time offset uncleared, which could cause issues
            Time.setOffset(0);

            Map<String,String> ldapConfig = ldapRule.getConfig();
            ldapConfig.put(LDAPConstants.SYNC_REGISTRATIONS, "false");
            ldapConfig.put(LDAPConstants.EDIT_MODE, UserFederationProvider.EditMode.WRITABLE.toString());
            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "test-ldap",
                    -1, -1, 0);

            FederationTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);

            // Delete all LDAP users and add 5 new users for testing
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            FederationTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            for (int i=1 ; i<=5 ; i++) {
                LDAPObject ldapUser = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "user" + i, "User" + i + "FN", "User" + i + "LN", "user" + i + "@email.org", null, "12" + i);
                FederationTestUtils.updateLDAPPassword(ldapFedProvider, ldapUser, "Password1");
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
        UsersSyncManager usersSyncManager = new UsersSyncManager();

        // wait a bit
        sleep(ldapRule.getSleepTime());

        KeycloakSession session = keycloakRule.startSession();
        try {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            UserFederationSyncResult syncResult = usersSyncManager.syncAllUsers(sessionFactory, "test", ldapModel);
            FederationTestUtils.assertSyncEquals(syncResult, 5, 0, 0, 0);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userStorage();
            // Assert users imported
            FederationTestUtils.assertUserImported(userProvider, testRealm, "user1", "User1FN", "User1LN", "user1@email.org", "121");
            FederationTestUtils.assertUserImported(userProvider, testRealm, "user2", "User2FN", "User2LN", "user2@email.org", "122");
            FederationTestUtils.assertUserImported(userProvider, testRealm, "user3", "User3FN", "User3LN", "user3@email.org", "123");
            FederationTestUtils.assertUserImported(userProvider, testRealm, "user4", "User4FN", "User4LN", "user4@email.org", "124");
            FederationTestUtils.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org", "125");

            // Assert lastSync time updated
            Assert.assertTrue(ldapModel.getLastSync() > 0);
            for (UserFederationProviderModel persistentFedModel : testRealm.getUserFederationProviders()) {
                if (LDAPFederationProviderFactory.PROVIDER_NAME.equals(persistentFedModel.getProviderName())) {
                    Assert.assertTrue(persistentFedModel.getLastSync() > 0);
                } else {
                    // Dummy provider has still 0
                    Assert.assertEquals(0, persistentFedModel.getLastSync());
                }
            }

            // wait a bit
            sleep(ldapRule.getSleepTime());

            // Add user to LDAP and update 'user5' in LDAP
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            FederationTestUtils.addLDAPUser(ldapFedProvider, testRealm, "user6", "User6FN", "User6LN", "user6@email.org", null, "126");
            LDAPObject ldapUser5 = ldapFedProvider.loadLDAPUserByUsername(testRealm, "user5");
            // NOTE: Changing LDAP attributes directly here
            ldapUser5.setSingleAttribute(LDAPConstants.EMAIL, "user5Updated@email.org");
            ldapUser5.setSingleAttribute(LDAPConstants.POSTAL_CODE, "521");
            ldapFedProvider.getLdapIdentityStore().update(ldapUser5);

            // Assert still old users in local provider
            FederationTestUtils.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org", "125");
            Assert.assertNull(userProvider.getUserByUsername("user6", testRealm));

            // Trigger partial sync
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            UserFederationSyncResult syncResult = usersSyncManager.syncChangedUsers(sessionFactory, "test", ldapModel);
            FederationTestUtils.assertSyncEquals(syncResult, 1, 1, 0, 0);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userStorage();
            // Assert users updated in local provider
            FederationTestUtils.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5updated@email.org", "521");
            FederationTestUtils.assertUserImported(userProvider, testRealm, "user6", "User6FN", "User6LN", "user6@email.org", "126");
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

            FederationTestUtils.addLocalUser(session, testRealm, "user7", "user7@email.org", "password");
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

            // Add user to LDAP with duplicated username "user7"
            duplicatedLdapUser = FederationTestUtils.addLDAPUser(ldapFedProvider, testRealm, "user7", "User7FN", "User7LN", "user7-something@email.org", null, "126");

        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Assert syncing from LDAP fails due to duplicated username
            UserFederationSyncResult result = new UsersSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), "test", ldapModel);
            Assert.assertEquals(1, result.getFailed());

            // Remove "user7" from LDAP
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            ldapFedProvider.getLdapIdentityStore().remove(duplicatedLdapUser);

            // Add user to LDAP with duplicated email "user7@email.org"
            duplicatedLdapUser = FederationTestUtils.addLDAPUser(ldapFedProvider, testRealm, "user7-something", "User7FNN", "User7LNL", "user7@email.org", null, "126");
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Assert syncing from LDAP fails due to duplicated email
            UserFederationSyncResult result = new UsersSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), "test", ldapModel);
            Assert.assertEquals(1, result.getFailed());
            Assert.assertNull(session.userStorage().getUserByUsername("user7-something", testRealm));

            // Update LDAP user to avoid duplicated email
            duplicatedLdapUser.setSingleAttribute(LDAPConstants.EMAIL, "user7-changed@email.org");
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            ldapFedProvider.getLdapIdentityStore().update(duplicatedLdapUser);

            // Assert user successfully synced now
            result = new UsersSyncManager().syncAllUsers(session.getKeycloakSessionFactory(), "test", ldapModel);
            Assert.assertEquals(0, result.getFailed());
        } finally {
            keycloakRule.stopSession(session, true);
        }

        // Assert user imported in another transaction
        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            FederationTestUtils.assertUserImported(session.userStorage(), testRealm, "user7-something", "User7FNN", "User7LNL", "user7-changed@email.org", "126");
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
            for (UserModel user : session.userStorage().getUsers(testRealm, true)) {
                session.userStorage().removeUser(testRealm, user);
            }

            UserFederationProviderModel providerModel = KeycloakModelUtils.findUserFederationProviderByDisplayName(ldapModel.getDisplayName(), testRealm);

            // Change name of UUID attribute to same like usernameAttribute
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            String uidAttrName = ldapFedProvider.getLdapIdentityStore().getConfig().getUsernameLdapAttribute();
            origUuidAttrName = providerModel.getConfig().get(LDAPConstants.UUID_LDAP_ATTRIBUTE);
            providerModel.getConfig().put(LDAPConstants.UUID_LDAP_ATTRIBUTE, uidAttrName);

            // Need to change this due to ApacheDS pagination bug (For other LDAP servers, pagination works fine) TODO: Remove once ApacheDS upgraded and pagination is fixed
            providerModel.getConfig().put(LDAPConstants.BATCH_SIZE_FOR_SYNC, "10");
            testRealm.updateUserFederationProvider(providerModel);

        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserFederationProviderModel providerModel = KeycloakModelUtils.findUserFederationProviderByDisplayName(ldapModel.getDisplayName(), testRealm);

            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            UserFederationSyncResult syncResult = new UsersSyncManager().syncAllUsers(sessionFactory, "test", providerModel);
            Assert.assertEquals(0, syncResult.getFailed());

        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Assert users imported with correct LDAP_ID
            FederationTestUtils.assertUserImported(session.users(), testRealm, "user1", "User1FN", "User1LN", "user1@email.org", "121");
            FederationTestUtils.assertUserImported(session.users(), testRealm, "user2", "User2FN", "User2LN", "user2@email.org", "122");
            UserModel user1 = session.users().getUserByUsername("user1", testRealm);
            Assert.assertEquals("user1", user1.getFirstAttribute(LDAPConstants.LDAP_ID));

            // Revert config changes
            UserFederationProviderModel providerModel = KeycloakModelUtils.findUserFederationProviderByDisplayName(ldapModel.getDisplayName(), testRealm);
            providerModel.getConfig().put(LDAPConstants.UUID_LDAP_ATTRIBUTE, origUuidAttrName);
            testRealm.updateUserFederationProvider(providerModel);
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
            for (UserModel user : session.userStorage().getUsers(testRealm, true)) {
                session.userStorage().removeUser(testRealm, user);
            }

            UserFederationProviderModel providerModel = KeycloakModelUtils.findUserFederationProviderByDisplayName(ldapModel.getDisplayName(), testRealm);

            // Add street mapper and add some user including street
            UserFederationMapperModel streetMapper = FederationTestUtils.addUserAttributeMapper(testRealm, ldapModel, "streetMapper", "street", LDAPConstants.STREET);
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject streetUser = FederationTestUtils.addLDAPUser(ldapFedProvider, testRealm, "user8", "User8FN", "User8LN", "user8@email.org", "user8street", "126");

            // Change name of username attribute name to street
            origUsernameAttrName = providerModel.getConfig().get(LDAPConstants.USERNAME_LDAP_ATTRIBUTE);
            providerModel.getConfig().put(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, "street");

            // Need to change this due to ApacheDS pagination bug (For other LDAP servers, pagination works fine) TODO: Remove once ApacheDS upgraded and pagination is fixed
            providerModel.getConfig().put(LDAPConstants.BATCH_SIZE_FOR_SYNC, "10");
            testRealm.updateUserFederationProvider(providerModel);

        } finally {
            keycloakRule.stopSession(session, true);
        }

        // Just user8 synced. All others failed to sync
        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserFederationProviderModel providerModel = KeycloakModelUtils.findUserFederationProviderByDisplayName(ldapModel.getDisplayName(), testRealm);

            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            UserFederationSyncResult syncResult = new UsersSyncManager().syncAllUsers(sessionFactory, "test", providerModel);
            Assert.assertEquals(1, syncResult.getAdded());
            Assert.assertTrue(syncResult.getFailed() > 0);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");

            // Revert config changes
            UserFederationProviderModel providerModel = KeycloakModelUtils.findUserFederationProviderByDisplayName(ldapModel.getDisplayName(), testRealm);
            providerModel.getConfig().put(LDAPConstants.USERNAME_LDAP_ATTRIBUTE, origUsernameAttrName);
            testRealm.updateUserFederationProvider(providerModel);
            UserFederationMapperModel streetMapper = testRealm.getUserFederationMapperByName(providerModel.getId(), "streetMapper");
            testRealm.removeUserFederationMapper(streetMapper);
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
