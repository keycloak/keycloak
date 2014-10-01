package org.keycloak.testsuite.forms;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.managers.UsersSyncManager;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testutils.DummyUserFederationProviderFactory;
import org.keycloak.testutils.LDAPEmbeddedServer;
import org.keycloak.timer.TimerProvider;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.basic.User;

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
            LDAPEmbeddedServer ldapServer = ldapRule.getEmbeddedServer();
            Map<String,String> ldapConfig = ldapServer.getLDAPConfig();
            ldapConfig.put(LDAPFederationProvider.SYNC_REGISTRATIONS, "false");
            ldapConfig.put(LDAPFederationProvider.EDIT_MODE, UserFederationProvider.EditMode.UNSYNCED.toString());

            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "test-ldap",
                    -1, -1, 0);

            // Delete all LDAP users and add 5 new users for testing
            PartitionManager partitionManager = FederationProvidersIntegrationTest.getPartitionManager(manager.getSession(), ldapModel);
            LDAPUtils.removeAllUsers(partitionManager);

            User user1 = LDAPUtils.addUser(partitionManager, "user1", "User1FN", "User1LN", "user1@email.org");
            LDAPUtils.updatePassword(partitionManager, user1, "Password1");
            User user2 = LDAPUtils.addUser(partitionManager, "user2", "User2FN", "User2LN", "user2@email.org");
            LDAPUtils.updatePassword(partitionManager, user2, "Password2");
            User user3 = LDAPUtils.addUser(partitionManager, "user3", "User3FN", "User3LN", "user3@email.org");
            LDAPUtils.updatePassword(partitionManager, user3, "Password3");
            User user4 = LDAPUtils.addUser(partitionManager, "user4", "User4FN", "User4LN", "user4@email.org");
            LDAPUtils.updatePassword(partitionManager, user4, "Password4");
            User user5 = LDAPUtils.addUser(partitionManager, "user5", "User5FN", "User5LN", "user5@email.org");
            LDAPUtils.updatePassword(partitionManager, user5, "Password5");

            // Add properties provider
            dummyModel = appRealm.addUserFederationProvider(DummyUserFederationProviderFactory.PROVIDER_NAME, new HashMap<String, String>(), 1, "test-dummy", -1, 1, 0);
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

    @Test
    public void testLDAPSync() {
        UsersSyncManager usersSyncManager = new UsersSyncManager();

        // wait a bit
        sleep(1000);

        KeycloakSession session = keycloakRule.startSession();
        try {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            usersSyncManager.syncAllUsers(sessionFactory, "test", ldapModel);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userStorage();
            // Assert users imported
            assertUserImported(userProvider, testRealm, "user1", "User1FN", "User1LN", "user1@email.org");
            assertUserImported(userProvider, testRealm, "user2", "User2FN", "User2LN", "user2@email.org");
            assertUserImported(userProvider, testRealm, "user3", "User3FN", "User3LN", "user3@email.org");
            assertUserImported(userProvider, testRealm, "user4", "User4FN", "User4LN", "user4@email.org");
            assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org");

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
            PartitionManager partitionManager = FederationProvidersIntegrationTest.getPartitionManager(session, ldapModel);
            LDAPUtils.addUser(partitionManager, "user6", "User6FN", "User6LN", "user6@email.org");
            LDAPUtils.updateUser(partitionManager, "user5", "User5FNUpdated", "User5LNUpdated", "user5Updated@email.org");

            // Assert still old users in local provider
            assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org");
            Assert.assertNull(userProvider.getUserByUsername("user6", testRealm));

            // Trigger partial sync
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            usersSyncManager.syncChangedUsers(sessionFactory, "test", ldapModel);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userStorage();
            // Assert users updated in local provider
            assertUserImported(userProvider, testRealm, "user5", "User5FNUpdated", "User5LNUpdated", "user5Updated@email.org");
            assertUserImported(userProvider, testRealm, "user6", "User6FN", "User6LN", "user6@email.org");
        } finally {
            keycloakRule.stopSession(session, false);
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

    public static void assertUserImported(UserProvider userProvider, RealmModel realm, String username, String expectedFirstName, String expectedLastName, String expectedEmail) {
        UserModel user = userProvider.getUserByUsername(username, realm);
        Assert.assertNotNull(user);
        Assert.assertEquals(expectedFirstName, user.getFirstName());
        Assert.assertEquals(expectedLastName, user.getLastName());
        Assert.assertEquals(expectedEmail, user.getEmail());
    }
}
