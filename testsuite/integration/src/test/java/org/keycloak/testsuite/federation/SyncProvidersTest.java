package org.keycloak.testsuite.federation;

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
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserFederationSyncResult;
import org.keycloak.models.UserProvider;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testsuite.DummyUserFederationProviderFactory;
import org.keycloak.timer.TimerProvider;
import org.keycloak.util.Time;

import java.util.HashMap;
import java.util.Map;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SyncProvidersTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static UserFederationProviderModel ldapModel = null;
    private static UserFederationProviderModel dummyModel = null;

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
                ldapFedProvider.getLdapIdentityStore().updatePassword(ldapUser, "Password1");
            }

            // Add dummy provider
            dummyModel = appRealm.addUserFederationProvider(DummyUserFederationProviderFactory.PROVIDER_NAME, new HashMap<String, String>(), 1, "test-dummy", -1, 1, 0);
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
        sleep(1000);

        KeycloakSession session = keycloakRule.startSession();
        try {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            UserFederationSyncResult syncResult = usersSyncManager.syncAllUsers(sessionFactory, "test", ldapModel);
            assertSyncEquals(syncResult, 5, 0, 0, 0);
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
            sleep(1000);

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
            assertSyncEquals(syncResult, 1, 1, 0, 0);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userStorage();
            // Assert users updated in local provider
            FederationTestUtils.assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5Updated@email.org", "521");
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
            FederationTestUtils.assertUserImported(session.userStorage(), testRealm, "user7-something", "User7FNN", "User7LNL", "user7-changed@email.org", "126");
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void testPeriodicSync() {
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
            keycloakRule.stopSession(session, false);
        }
    }

    private void sleep(int time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException ie) {
            throw new RuntimeException(ie);
        }
    }

    private void assertSyncEquals(UserFederationSyncResult syncResult, int expectedAdded, int expectedUpdated, int expectedRemoved, int expectedFailed) {
        Assert.assertEquals(syncResult.getAdded(), expectedAdded);
        Assert.assertEquals(syncResult.getUpdated(), expectedUpdated);
        Assert.assertEquals(syncResult.getRemoved(), expectedRemoved);
        Assert.assertEquals(syncResult.getFailed(), expectedFailed);
    }
}
