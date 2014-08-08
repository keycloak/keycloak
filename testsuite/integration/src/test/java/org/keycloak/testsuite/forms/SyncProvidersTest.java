package org.keycloak.testsuite.forms;

import java.util.Date;
import java.util.Map;

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
import org.keycloak.models.UserFederationProviderFactory;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserProvider;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testutils.LDAPEmbeddedServer;
import org.picketlink.idm.PartitionManager;
import org.picketlink.idm.model.basic.User;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SyncProvidersTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static UserFederationProviderModel ldapModel = null;

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            LDAPEmbeddedServer ldapServer = ldapRule.getEmbeddedServer();
            Map<String,String> ldapConfig = ldapServer.getLDAPConfig();
            ldapConfig.put(LDAPFederationProvider.SYNC_REGISTRATIONS, "false");
            ldapConfig.put(LDAPFederationProvider.EDIT_MODE, UserFederationProvider.EditMode.UNSYNCED.toString());

            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "test-ldap");

            // Delete all LDAP users and add 5 new users for testing
            PartitionManager partitionManager = FederationProvidersIntegrationTest.getPartitionManager(manager.getSession(), ldapModel);
            LDAPUtils.removeAllUsers(partitionManager);

            User user1 = LDAPUtils.addUser(partitionManager, "user1", "User1FN", "User1LN", "user1@email.org");
            LDAPUtils.updatePassword(partitionManager, user1, "password1");
            User user2 = LDAPUtils.addUser(partitionManager, "user2", "User2FN", "User2LN", "user2@email.org");
            LDAPUtils.updatePassword(partitionManager, user2, "password2");
            User user3 = LDAPUtils.addUser(partitionManager, "user3", "User3FN", "User3LN", "user3@email.org");
            LDAPUtils.updatePassword(partitionManager, user3, "password3");
            User user4 = LDAPUtils.addUser(partitionManager, "user4", "User4FN", "User4LN", "user4@email.org");
            LDAPUtils.updatePassword(partitionManager, user4, "password4");
            User user5 = LDAPUtils.addUser(partitionManager, "user5", "User5FN", "User5LN", "user5@email.org");
            LDAPUtils.updatePassword(partitionManager, user5, "password5");

            // Add properties provider
//            Map<String,String> filePropertiesConfig = new HashMap<String, String>();
//            filePropertiesConfig.put("path", );
//            appRealm.addUserFederationProvider(FilePropertiesFederationFactory.PROVIDER_NAME, filePropertiesConfig, 1, "test-fileProps");
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

    @Test
    public void testLDAPSync() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            UserFederationProviderFactory ldapFedFactory = (UserFederationProviderFactory)sessionFactory.getProviderFactory(UserFederationProvider.class, LDAPFederationProviderFactory.PROVIDER_NAME);
            ldapFedFactory.syncAllUsers(sessionFactory, "test", ldapModel);
        } finally {
            keycloakRule.stopSession(session, false);
        }

        // Assert users imported (model test)
        session = keycloakRule.startSession();
        try {
            RealmModel testRealm = session.realms().getRealm("test");
            UserProvider userProvider = session.userStorage();
            assertUserImported(userProvider, testRealm, "user1", "User1FN", "User1LN", "user1@email.org");
            assertUserImported(userProvider, testRealm, "user2", "User2FN", "User2LN", "user2@email.org");
            assertUserImported(userProvider, testRealm, "user3", "User3FN", "User3LN", "user3@email.org");
            assertUserImported(userProvider, testRealm, "user4", "User4FN", "User4LN", "user4@email.org");
            assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org");

            // wait a bit
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ie) {
                throw new RuntimeException(ie);
            }
            Date beforeLDAPUpdate = new Date();

            // Add user to LDAP and update 'user5' in LDAP
            PartitionManager partitionManager = FederationProvidersIntegrationTest.getPartitionManager(session, ldapModel);
            LDAPUtils.addUser(partitionManager, "user6", "User6FN", "User6LN", "user6@email.org");
            LDAPUtils.updateUser(partitionManager, "user5", "User5FNUpdated", "User5LNUpdated", "user5Updated@email.org");

            // Assert still old users in local provider
            assertUserImported(userProvider, testRealm, "user5", "User5FN", "User5LN", "user5@email.org");
            Assert.assertNull(userProvider.getUserByUsername("user6", testRealm));

            // Trigger partial sync
            KeycloakSessionFactory sessionFactory = session.getKeycloakSessionFactory();
            UserFederationProviderFactory ldapFedFactory = (UserFederationProviderFactory)sessionFactory.getProviderFactory(UserFederationProvider.class, LDAPFederationProviderFactory.PROVIDER_NAME);
            ldapFedFactory.syncChangedUsers(sessionFactory, "test", ldapModel, beforeLDAPUpdate);
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

    private void assertUserImported(UserProvider userProvider, RealmModel realm, String username, String expectedFirstName, String expectedLastName, String expectedEmail) {
        UserModel user = userProvider.getUserByUsername(username, realm);
        Assert.assertNotNull(user);
        Assert.assertEquals(expectedFirstName, user.getFirstName());
        Assert.assertEquals(expectedLastName, user.getLastName());
        Assert.assertEquals(expectedEmail, user.getEmail());
    }
}
