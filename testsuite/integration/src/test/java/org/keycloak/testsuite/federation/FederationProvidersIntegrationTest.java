package org.keycloak.testsuite.federation;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.LDAPUtils;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapperFactory;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelReadOnlyException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserCredentialValueModel;
import org.keycloak.models.UserFederationMapperModel;
import org.keycloak.models.UserFederationProvider;
import org.keycloak.models.UserFederationProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FederationProvidersIntegrationTest {

    private static LDAPRule ldapRule = new LDAPRule();

    private static UserFederationProviderModel ldapModel = null;

    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            FederationTestUtils.addLocalUser(manager.getSession(), appRealm, "mary", "mary@test.com", "password-app");

            Map<String,String> ldapConfig = ldapRule.getConfig();
            ldapConfig.put(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ldapConfig.put(LDAPConstants.EDIT_MODE, UserFederationProvider.EditMode.WRITABLE.toString());

            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "test-ldap", -1, -1, 0);
            FederationTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);

            // Delete all LDAP users and add some new for testing
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            LDAPUtils.removeAllUsers(ldapFedProvider, appRealm);

            LDAPObject john = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", "1234");
            ldapFedProvider.getLdapIdentityStore().updatePassword(john, "Password1");

            LDAPObject existing = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "existing", "Existing", "Foo", "existing@email.org", "5678");
        }
    });

    @ClassRule
    public static TestRule chain = RuleChain
            .outerRule(ldapRule)
            .around(keycloakRule);

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected RegisterPage registerPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected AccountUpdateProfilePage profilePage;

    @WebResource
    protected AccountPasswordPage changePasswordPage;

//    @Test
//    @Ignore
//    public void runit() throws Exception {
//        Thread.sleep(10000000);
//
//    }


    @Test
    public void caseSensitiveSearch() {
        loginPage.open();

        // This should fail for now due to case-sensitivity
        loginPage.login("johnKeycloak", "Password1");
        Assert.assertEquals("Invalid username or password.", loginPage.getError());
    }

    @Test
    public void deleteFederationLink() {
        loginLdap();
        {
            KeycloakSession session = keycloakRule.startSession();
            try {
                RealmManager manager = new RealmManager(session);

                RealmModel appRealm = manager.getRealm("test");
                appRealm.removeUserFederationProvider(ldapModel);
                Assert.assertEquals(0, appRealm.getUserFederationProviders().size());
            } finally {
                keycloakRule.stopSession(session, true);
            }
        }
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        {
            KeycloakSession session = keycloakRule.startSession();
            try {
                RealmManager manager = new RealmManager(session);

                RealmModel appRealm = manager.getRealm("test");
                ldapModel = appRealm.addUserFederationProvider(ldapModel.getProviderName(), ldapModel.getConfig(), ldapModel.getPriority(), ldapModel.getDisplayName(), -1, -1, 0);
                FederationTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);
            } finally {
                keycloakRule.stopSession(session, true);
            }
        }
        loginLdap();

    }

    @Test
    public void loginClassic() {
        loginPage.open();
        loginPage.login("mary", "password-app");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

    }

    @Test
    public void loginLdap() {
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        profilePage.open();
        Assert.assertEquals("John", profilePage.getFirstName());
        Assert.assertEquals("Doe", profilePage.getLastName());
        Assert.assertEquals("john@email.org", profilePage.getEmail());
        Assert.assertEquals("1234", profilePage.getPostalCode());
    }

    @Test
    public void loginLdapWithEmail() {
        loginPage.open();
        loginPage.login("john@email.org", "Password1");

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
    }

    @Test
    public void loginLdapWithoutPassword() {
        loginPage.open();
        loginPage.login("john@email.org", "");

        Assert.assertEquals("Invalid username or password.", loginPage.getError());
    }

    @Test
    public void passwordChangeLdap() throws Exception {
        changePasswordPage.open();
        loginPage.login("johnkeycloak", "Password1");
        changePasswordPage.changePassword("Password1", "New-password1", "New-password1");

        Assert.assertEquals("Your password has been updated.", profilePage.getSuccess());

        changePasswordPage.logout();

        loginPage.open();
        loginPage.login("johnkeycloak", "Bad-password1");
        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        loginPage.open();
        loginPage.login("johnkeycloak", "New-password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        // Change password back to previous value
        changePasswordPage.open();
        changePasswordPage.changePassword("New-password1", "Password1", "Password1");
        Assert.assertEquals("Your password has been updated.", profilePage.getSuccess());
    }

    @Test
    public void registerExistingLdapUser() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // check existing username
        registerPage.register("firstName", "lastName", "email@mail.cz", "existing", "Password1", "Password1");
        registerPage.assertCurrent();
        Assert.assertEquals("Username already exists.", registerPage.getError());

        // Check existing email
        registerPage.register("firstName", "lastName", "existing@email.org", "nonExisting", "Password1", "Password1");
        registerPage.assertCurrent();
        Assert.assertEquals("Email already exists.", registerPage.getError());
    }

    @Test
    public void registerUserLdapSuccess() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "email2@check.cz", "registerUserSuccess2", "Password1", "Password1", "non-LDAP-Mapped street", null, null, "78910", null);
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("registerUserSuccess2", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ldapModel.getId());
            Assert.assertEquals("78910", user.getAttribute("postal_code"));
            Assert.assertEquals("non-LDAP-Mapped street", user.getAttribute("street"));
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void testFullNameMapper() {
        KeycloakSession session = keycloakRule.startSession();
        UserFederationMapperModel firstNameMapper = null;

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            // assert that user "fullnameUser" is not in local DB
            Assert.assertNull(session.users().getUserByUsername("fullname", appRealm));

            // Add the user with some fullName into LDAP directly. Ensure that fullName is saved into "cn" attribute in LDAP (currently mapped to model firstName)
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "fullname", "James Dee", "Dee", "fullname@email.org", "4578");

            // add fullname mapper to the provider and remove "firstNameMapper"
            UserFederationMapperModel fullNameMapperModel = KeycloakModelUtils.createUserFederationMapperModel("full name", ldapModel.getId(), FullNameLDAPFederationMapperFactory.PROVIDER_ID,
                    FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, LDAPConstants.CN,
                    UserAttributeLDAPFederationMapper.READ_ONLY, "false");
            appRealm.addUserFederationMapper(fullNameMapperModel);

            firstNameMapper = appRealm.getUserFederationMapperByName(ldapModel.getId(), "first name");
            appRealm.removeUserFederationMapper(firstNameMapper);

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            FederationTestUtils.assertUserImported(session.users(), appRealm, "fullname", "James", "Dee", "fullname@email.org", "4578");
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            // Remove "fullnameUser" to assert he is removed from LDAP. Revert mappers to previous state
            UserModel fullnameUser = session.users().getUserByUsername("fullname", appRealm);
            session.users().removeUser(appRealm, fullnameUser);

            // Revert mappers
            UserFederationMapperModel fullNameMapperModel = appRealm.getUserFederationMapperByName(ldapModel.getId(), "full name");
            appRealm.removeUserFederationMapper(fullNameMapperModel);

            firstNameMapper.setId(null);
            appRealm.addUserFederationMapper(firstNameMapper);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void testReadonly() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");

            UserFederationProviderModel model = new UserFederationProviderModel(ldapModel.getId(), ldapModel.getProviderName(), ldapModel.getConfig(),
                    ldapModel.getPriority(), ldapModel.getDisplayName(), -1, -1, 0);
            model.getConfig().put(LDAPConstants.EDIT_MODE, UserFederationProvider.EditMode.READ_ONLY.toString());
            appRealm.updateUserFederationProvider(model);
            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ldapModel.getId());
            try {
                user.setEmail("error@error.com");
                Assert.fail("should fail");
            } catch (ModelReadOnlyException e) {

            }
            try {
                user.setLastName("Berk");
                Assert.fail("should fail");
            } catch (ModelReadOnlyException e) {

            }
            try {
                user.setFirstName("Bilbo");
                Assert.fail("should fail");
            } catch (ModelReadOnlyException e) {

            }
            try {
                UserCredentialModel cred = UserCredentialModel.password("PoopyPoop1");
                user.updateCredential(cred);
                Assert.fail("should fail");
            } catch (ModelReadOnlyException e) {

            }

            Assert.assertFalse(session.users().removeUser(appRealm, user));
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            Assert.assertEquals(UserFederationProvider.EditMode.WRITABLE.toString(), appRealm.getUserFederationProviders().get(0).getConfig().get(LDAPConstants.EDIT_MODE));
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void testRemoveFederatedUser() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("registerUserSuccess2", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ldapModel.getId());

            Assert.assertTrue(session.users().removeUser(appRealm, user));
            Assert.assertNull(session.users().getUserByUsername("registerUserSuccess2", appRealm));
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void testSearch() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

            FederationTestUtils.addLDAPUser(ldapProvider, appRealm, "username1", "John1", "Doel1", "user1@email.org", "121");
            FederationTestUtils.addLDAPUser(ldapProvider, appRealm, "username2", "John2", "Doel2", "user2@email.org", "122");
            FederationTestUtils.addLDAPUser(ldapProvider, appRealm, "username3", "John3", "Doel3", "user3@email.org", "123");
            FederationTestUtils.addLDAPUser(ldapProvider, appRealm, "username4", "John4", "Doel4", "user4@email.org", "124");

            // Users are not at local store at this moment
            Assert.assertNull(session.userStorage().getUserByUsername("username1", appRealm));
            Assert.assertNull(session.userStorage().getUserByUsername("username2", appRealm));
            Assert.assertNull(session.userStorage().getUserByUsername("username3", appRealm));
            Assert.assertNull(session.userStorage().getUserByUsername("username4", appRealm));

            // search by username
            session.users().searchForUser("username1", appRealm);
            FederationTestUtils.assertUserImported(session.userStorage(), appRealm, "username1", "John1", "Doel1", "user1@email.org", "121");

            // search by email
            session.users().searchForUser("user2@email.org", appRealm);
            FederationTestUtils.assertUserImported(session.userStorage(), appRealm, "username2", "John2", "Doel2", "user2@email.org", "122");

            // search by lastName
            session.users().searchForUser("Doel3", appRealm);
            FederationTestUtils.assertUserImported(session.userStorage(), appRealm, "username3", "John3", "Doel3", "user3@email.org", "123");

            // search by firstName + lastName
            session.users().searchForUser("John4 Doel4", appRealm);
            FederationTestUtils.assertUserImported(session.userStorage(), appRealm, "username4", "John4", "Doel4", "user4@email.org", "124");
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void testUnsynced() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");

            UserFederationProviderModel model = new UserFederationProviderModel(ldapModel.getId(), ldapModel.getProviderName(), ldapModel.getConfig(), ldapModel.getPriority(),
                    ldapModel.getDisplayName(), -1, -1, 0);
            model.getConfig().put(LDAPConstants.EDIT_MODE, UserFederationProvider.EditMode.UNSYNCED.toString());
            appRealm.updateUserFederationProvider(model);
            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ldapModel.getId());

            UserCredentialModel cred = UserCredentialModel.password("Candycand1");
            user.updateCredential(cred);
            UserCredentialValueModel userCredentialValueModel = user.getCredentialsDirectly().get(0);
            Assert.assertEquals(UserCredentialModel.PASSWORD, userCredentialValueModel.getType());
            Assert.assertTrue(session.users().validCredentials(appRealm, user, cred));

            // LDAP password is still unchanged
            LDAPFederationProvider ldapProvider = FederationTestUtils.getLdapProvider(session, model);
            LDAPObject ldapUser = ldapProvider.loadLDAPUserByUsername(appRealm, "johnkeycloak");
            ldapProvider.getLdapIdentityStore().validatePassword(ldapUser, "Password1");

            // ATM it's not permitted to delete user in unsynced mode. Should be user deleted just locally instead?
            Assert.assertFalse(session.users().removeUser(appRealm, user));
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            Assert.assertEquals(UserFederationProvider.EditMode.WRITABLE.toString(), appRealm.getUserFederationProviders().get(0).getConfig().get(LDAPConstants.EDIT_MODE));
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

}
