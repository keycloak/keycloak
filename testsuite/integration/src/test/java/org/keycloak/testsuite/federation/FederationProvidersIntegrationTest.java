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
import org.keycloak.federation.ldap.LDAPConfig;
import org.keycloak.federation.ldap.LDAPFederationProvider;
import org.keycloak.federation.ldap.LDAPFederationProviderFactory;
import org.keycloak.federation.ldap.idm.model.LDAPObject;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapper;
import org.keycloak.federation.ldap.mappers.FullNameLDAPFederationMapperFactory;
import org.keycloak.federation.ldap.mappers.UserAttributeLDAPFederationMapper;
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
import org.keycloak.representations.AccessToken;
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

import static org.junit.Assert.assertEquals;

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
            FederationTestUtils.addLocalUser(manager.getSession(), appRealm, "marykeycloak", "mary@test.com", "password-app");

            Map<String,String> ldapConfig = ldapRule.getConfig();
            ldapConfig.put(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ldapConfig.put(LDAPConstants.EDIT_MODE, UserFederationProvider.EditMode.WRITABLE.toString());

            ldapModel = appRealm.addUserFederationProvider(LDAPFederationProviderFactory.PROVIDER_NAME, ldapConfig, 0, "test-ldap", -1, -1, 0);
            FederationTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);

            // Delete all LDAP users and add some new for testing
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            FederationTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            LDAPObject john = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            ldapFedProvider.getLdapIdentityStore().updatePassword(john, "Password1");

            LDAPObject existing = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "existing", "Existing", "Foo", "existing@email.org", null, "5678");
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
    public void caseInSensitiveImport() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject jbrown2 = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "JBrown2", "John", "Brown2", "jbrown2@email.org", null, "1234");
            ldapFedProvider.getLdapIdentityStore().updatePassword(jbrown2, "Password1");
            LDAPObject jbrown3 = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "jbrown3", "John", "Brown3", "JBrown3@email.org", null, "1234");
            ldapFedProvider.getLdapIdentityStore().updatePassword(jbrown3, "Password1");
        } finally {
            keycloakRule.stopSession(session, true);
        }

        loginSuccessAndLogout("jbrown2", "Password1");
        loginSuccessAndLogout("JBrown2", "Password1");
        loginSuccessAndLogout("jbrown2@email.org", "Password1");
        loginSuccessAndLogout("JBrown2@email.org", "Password1");

        loginSuccessAndLogout("jbrown3", "Password1");
        loginSuccessAndLogout("JBrown3", "Password1");
        loginSuccessAndLogout("jbrown3@email.org", "Password1");
        loginSuccessAndLogout("JBrown3@email.org", "Password1");
    }

    private void loginSuccessAndLogout(String username, String password) {
        loginPage.open();
        loginPage.login(username, password);
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
        oauth.openLogout();
    }

    @Test
    public void caseInsensitiveSearch() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject jbrown2 = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "JBrown4", "John", "Brown4", "jbrown4@email.org", null, "1234");
            ldapFedProvider.getLdapIdentityStore().updatePassword(jbrown2, "Password1");
            LDAPObject jbrown3 = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "jbrown5", "John", "Brown5", "JBrown5@Email.org", null, "1234");
            ldapFedProvider.getLdapIdentityStore().updatePassword(jbrown3, "Password1");
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");

            // search by username
            List<UserModel> users = session.users().searchForUser("JBROwn4", appRealm);
            Assert.assertEquals(1, users.size());
            UserModel user4 = users.get(0);
            Assert.assertEquals("jbrown4", user4.getUsername());
            Assert.assertEquals("jbrown4@email.org", user4.getEmail());

            // search by email
            users = session.users().searchForUser("JBROwn5@eMAil.org", appRealm);
            Assert.assertEquals(1, users.size());
            UserModel user5 = users.get(0);
            Assert.assertEquals("jbrown5", user5.getUsername());
            Assert.assertEquals("jbrown5@email.org", user5.getEmail());
        } finally {
            keycloakRule.stopSession(session, true);
        }
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
        loginPage.login("marykeycloak", "password-app");

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
    }

    @Test
    public void loginLdapWithDirectGrant() throws Exception {
        OAuthClient.AccessTokenResponse response = oauth.doGrantAccessTokenRequest("password", "johnkeycloak", "Password1");
        assertEquals(200, response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

        response = oauth.doGrantAccessTokenRequest("password", "johnkeycloak", "");
        assertEquals(401, response.getStatusCode());
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

        registerPage.register("firstName", "lastName", "email2@check.cz", "registerUserSuccess2", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("registerUserSuccess2", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ldapModel.getId());
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void testCaseSensitiveAttributeName() {
        KeycloakSession session = keycloakRule.startSession();

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject johnZip = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnzip", "John", "Zip", "johnzip@email.org", null, "12398");

            // Remove default zipcode mapper and add the mapper for "POstalCode" to test case sensitivity
            UserFederationMapperModel currentZipMapper = appRealm.getUserFederationMapperByName(ldapModel.getId(), "zipCodeMapper");
            appRealm.removeUserFederationMapper(currentZipMapper);
            FederationTestUtils.addUserAttributeMapper(appRealm, ldapModel, "zipCodeMapper-cs", "postal_code", "POstalCode");

            // Fetch user from LDAP and check that postalCode is filled
            UserModel user = session.users().getUserByUsername("johnzip", appRealm);
            String postalCode = user.getFirstAttribute("postal_code");
            Assert.assertEquals("12398", postalCode);

        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void testDotInUsername() {
        KeycloakSession session = keycloakRule.startSession();
        boolean skip = false;

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");
            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);

            // Workaround as dot is not allowed in sAMAccountName on active directory. So we will skip the test for this configuration
            LDAPConfig config = ldapFedProvider.getLdapIdentityStore().getConfig();
            if (config.isActiveDirectory() && config.getUsernameLdapAttribute().equals(LDAPConstants.SAM_ACCOUNT_NAME)) {
                skip = true;
            }

            if (!skip) {
                LDAPObject johnDot = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "john,dot", "John", "Dot", "johndot@email.org", null, "12387");
                ldapFedProvider.getLdapIdentityStore().updatePassword(johnDot, "Password1");
            }
        } finally {
            keycloakRule.stopSession(session, false);
        }

        if (!skip) {
            // Try to import the user with dot in username into Keycloak
            loginSuccessAndLogout("john,dot", "Password1");
        }
    }

    @Test
    public void testDirectLDAPUpdate() {
        KeycloakSession session = keycloakRule.startSession();

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject johnDirect = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johndirect", "John", "Direct", "johndirect@email.org", null, "12399");

            // Fetch user from LDAP and check that postalCode is filled
            UserModel user = session.users().getUserByUsername("johndirect", appRealm);
            String postalCode = user.getFirstAttribute("postal_code");
            Assert.assertEquals("12399", postalCode);

            // Directly update user in LDAP
            johnDirect.setSingleAttribute(LDAPConstants.POSTAL_CODE, "12400");
            johnDirect.setSingleAttribute(LDAPConstants.SN, "DirectLDAPUpdated");
            ldapFedProvider.getLdapIdentityStore().update(johnDirect);

        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");
            UserModel user = session.users().getUserByUsername("johndirect", appRealm);

            // Verify that postalCode is still the same as we read it's value from Keycloak DB
            user = session.users().getUserByUsername("johndirect", appRealm);
            String postalCode = user.getFirstAttribute("postal_code");
            Assert.assertEquals("12399", postalCode);

            // Check user.getAttributes()
            postalCode = user.getAttributes().get("postal_code").get(0);
            Assert.assertEquals("12399", postalCode);

            // LastName is new as lastName mapper will read the value from LDAP
            String lastName = user.getLastName();
            Assert.assertEquals("DirectLDAPUpdated", lastName);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            // Update postalCode mapper to always read the value from LDAP
            UserFederationMapperModel zipMapper = appRealm.getUserFederationMapperByName(ldapModel.getId(), "zipCodeMapper");
            zipMapper.getConfig().put(UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true");
            appRealm.updateUserFederationMapper(zipMapper);

            // Update lastName mapper to read the value from Keycloak DB
            UserFederationMapperModel lastNameMapper = appRealm.getUserFederationMapperByName(ldapModel.getId(), "last name");
            lastNameMapper.getConfig().put(UserAttributeLDAPFederationMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false");
            appRealm.updateUserFederationMapper(lastNameMapper);

            // Verify that postalCode is read from LDAP now
            UserModel user = session.users().getUserByUsername("johndirect", appRealm);
            String postalCode = user.getFirstAttribute("postal_code");
            Assert.assertEquals("12400", postalCode);

            // Check user.getAttributes()
            postalCode = user.getAttributes().get("postal_code").get(0);
            Assert.assertEquals("12400", postalCode);

            Assert.assertFalse(user.getAttributes().containsKey(UserModel.LAST_NAME));

            // lastName is read from Keycloak DB now
            String lastName = user.getLastName();
            Assert.assertEquals("Direct", lastName);

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
            FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "fullname", "James Dee", "Dee", "fullname@email.org", null, "4578");

            // add fullname mapper to the provider and remove "firstNameMapper". For this test, we will simply map full name to the LDAP attribute, which was before firstName ( "givenName" on active directory, "cn" on other LDAP servers)
            firstNameMapper = appRealm.getUserFederationMapperByName(ldapModel.getId(), "first name");
            String ldapFirstNameAttributeName = firstNameMapper.getConfig().get(UserAttributeLDAPFederationMapper.LDAP_ATTRIBUTE);
            appRealm.removeUserFederationMapper(firstNameMapper);

            UserFederationMapperModel fullNameMapperModel = KeycloakModelUtils.createUserFederationMapperModel("full name", ldapModel.getId(), FullNameLDAPFederationMapperFactory.PROVIDER_ID,
                    FullNameLDAPFederationMapper.LDAP_FULL_NAME_ATTRIBUTE, ldapFirstNameAttributeName,
                    UserAttributeLDAPFederationMapper.READ_ONLY, "false");
            appRealm.addUserFederationMapper(fullNameMapperModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            FederationTestUtils.assertUserImported(session.users(), appRealm, "fullname", "James", "Dee", "fullname@email.org", "4578");

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
    public void testImportExistingUserFromLDAP() throws Exception {
        // Add LDAP user with same email like existing model user
        keycloakRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                LDAPFederationProvider ldapFedProvider = FederationTestUtils.getLdapProvider(session, ldapModel);
                FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "marykeycloak", "Mary1", "Kelly1", "mary1@email.org", null, "123");
                FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "mary-duplicatemail", "Mary2", "Kelly2", "mary@test.com", null, "123");
                LDAPObject marynoemail = FederationTestUtils.addLDAPUser(ldapFedProvider, appRealm, "marynoemail", "Mary1", "Kelly1", null, null, "123");
                ldapFedProvider.getLdapIdentityStore().updatePassword(marynoemail, "Password1");
            }

        });

        // Try to import the duplicated LDAP user into Keycloak
        loginPage.open();
        loginPage.login("mary-duplicatemail", "password");
        Assert.assertEquals("Email already exists.", loginPage.getError());

        loginPage.login("mary1@email.org", "password");
        Assert.assertEquals("Username already exists.", loginPage.getError());

        loginSuccessAndLogout("marynoemail", "Password1");
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

            FederationTestUtils.addLDAPUser(ldapProvider, appRealm, "username1", "John1", "Doel1", "user1@email.org", null, "121");
            FederationTestUtils.addLDAPUser(ldapProvider, appRealm, "username2", "John2", "Doel2", "user2@email.org", null, "122");
            FederationTestUtils.addLDAPUser(ldapProvider, appRealm, "username3", "John3", "Doel3", "user3@email.org", null, "123");
            FederationTestUtils.addLDAPUser(ldapProvider, appRealm, "username4", "John4", "Doel4", "user4@email.org", null, "124");

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
