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

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.MASTER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.keycloak.models.AdminRoles.ADMIN;
import static org.keycloak.testsuite.Constants.AUTH_SERVER_ROOT;

import java.util.List;

import org.jboss.logging.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.RuleChain;
import org.junit.rules.TestRule;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.LDAPStorageProviderFactory;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPAttributeMapper;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPAttributeMapperFactory;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPRoleStorageMapper;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPRoleStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.LDAPRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPProvidersIntegrationTest {

    private static final Logger log = Logger.getLogger(LDAPProvidersIntegrationTest.class);

    private static LDAPRule ldapRule = new LDAPRule();

    private static ComponentModel ldapModel = null;


    private static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            LDAPTestUtils.addLocalUser(manager.getSession(), appRealm, "marykeycloak", "mary@test.com", "password-app");

            MultivaluedHashMap<String,String> ldapConfig = LDAPTestUtils.getLdapRuleConfig(ldapRule);
            ldapConfig.putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ldapConfig.putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            UserStorageProviderModel model = new UserStorageProviderModel();
            model.setLastSync(0);
            model.setChangedSyncPeriod(-1);
            model.setFullSyncPeriod(-1);
            model.setName("test-ldap");
            model.setPriority(0);
            model.setProviderId(LDAPStorageProviderFactory.PROVIDER_NAME);
            model.setConfig(ldapConfig);

            ldapModel = appRealm.addComponentModel(model);
            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);

            // Delete all LDAP users and add some new for testing
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.removeAllLDAPUsers(ldapFedProvider, appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, john, "Password1");

            LDAPObject existing = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "existing", "Existing", "Foo", "existing@email.org", null, "5678");

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
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

    @WebResource
    protected OAuthGrantPage grantPage;

//    @Test
//    @Ignore
//    public void runit() throws Exception {
//        Thread.sleep(10000000);
//
//    }

    /**
     * KEYCLOAK-3986
     *
     */
    @Test
    public void testSyncRegistrationOff() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");
            UserStorageProviderModel newModel = new UserStorageProviderModel(ldapModel);
            newModel.getConfig().putSingle(LDAPConstants.SYNC_REGISTRATIONS, "false");
            appRealm.updateComponent(newModel);
            UserModel newUser1 = session.users().addUser(appRealm, "newUser1");
            Assert.assertNull(newUser1.getFederationLink());
        } finally {
            keycloakRule.stopSession(session, false);
        }


    }


    private Keycloak adminClient;

    @Before
    public void onBefore() {
        adminClient = Keycloak.getInstance(AUTH_SERVER_ROOT, MASTER, ADMIN, ADMIN, Constants.ADMIN_CLI_CLIENT_ID);
    }

    @Test
    public void testRemoveImportedUsers() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");
            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertEquals(ldapModel.getId(), user.getFederationLink());
        } finally {
            keycloakRule.stopSession(session, true);
        }

        adminClient.realm("test").userStorage().removeImportedUsers(ldapModel.getId());

        session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");
            UserModel user = session.userLocalStorage().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNull(user);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    // test name prefixed with zz to make sure it runs last as we are unlinking imported users
    @Test
    public void zzTestUnlinkUsers() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");
            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertEquals(ldapModel.getId(), user.getFederationLink());
        } finally {
            keycloakRule.stopSession(session, true);
        }

        adminClient.realm("test").userStorage().unlink(ldapModel.getId());

        session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");
            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNull(user.getFederationLink());

        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void caseInSensitiveImport() {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject jbrown2 = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "JBrown2", "John", "Brown2", "jbrown2@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, jbrown2, "Password1");
            LDAPObject jbrown3 = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "jbrown3", "John", "Brown3", "JBrown3@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, jbrown3, "Password1");
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
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject jbrown4 = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "JBrown4", "John", "Brown4", "jbrown4@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, jbrown4, "Password1");
            LDAPObject jbrown5 = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "jbrown5", "John", "Brown5", "JBrown5@Email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ldapFedProvider, jbrown5, "Password1");
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
    public void deleteFederationLink() throws Exception {
        // KEYCLOAK-4789: Login in client, which requires consent
        oauth.clientId("third-party");
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");

        grantPage.assertCurrent();
        grantPage.accept();

        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        {
            KeycloakSession session = keycloakRule.startSession();
            try {
                RealmManager manager = new RealmManager(session);

                RealmModel appRealm = manager.getRealm("test");
                appRealm.removeComponent(ldapModel);
                Assert.assertEquals(0, appRealm.getComponents(appRealm.getId(), UserStorageProvider.class.getName()).size());
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
                ldapModel.setId(null);
                ldapModel = appRealm.addComponentModel(ldapModel);
                LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ldapModel);
            } finally {
                keycloakRule.stopSession(session, true);
            }
        }

        oauth.clientId("test-app");

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
  
    
   
    //
    // KEYCLOAK-4533
    //
    @Test
    public void testLDAPUserDeletionImport() {
       
    	KeycloakSession session = keycloakRule.startSession();
        RealmModel appRealm = new RealmManager(session).getRealmByName("test");
        LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);        	
      	LDAPConfig config = ldapProvider.getLdapIdentityStore().getConfig();      
      
      	// Make sure mary is gone
      	LDAPTestUtils.removeLDAPUserByUsername(ldapProvider, appRealm, config, "maryjane");
      	
     // Create the user in LDAP and register him

       LDAPObject mary = LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "maryjane", "mary", "yram", "mj@testing.redhat.cz", null, "12398");
       LDAPTestUtils.updateLDAPPassword(ldapProvider, mary, "Password1");
        
        try {
        	
        	// Log in and out of the user
         	loginSuccessAndLogout("maryjane", "Password1");  
           
         	// Delete LDAP User
        	LDAPTestUtils.removeLDAPUserByUsername(ldapProvider, appRealm, config, "maryjane");
   
        	// Make sure the deletion took place. 
        	List<UserModel> deletedUsers = session.users().searchForUser("mary yram", appRealm);
            Assert.assertTrue(deletedUsers.isEmpty());
                  
        } finally {
            keycloakRule.stopSession(session, false);
        }
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
            Assert.assertEquals("registerusersuccess2", user.getUsername());
            Assert.assertEquals("firstName", user.getFirstName());
            Assert.assertEquals("lastName", user.getLastName());
            Assert.assertTrue(user.isEnabled());
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void testCaseSensitiveAttributeName() {
        KeycloakSession session = keycloakRule.startSession();

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject johnZip = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johnzip", "John", "Zip", "johnzip@email.org", null, "12398");

            // Remove default zipcode mapper and add the mapper for "POstalCode" to test case sensitivity
            ComponentModel currentZipMapper = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "zipCodeMapper");
            appRealm.removeComponent(currentZipMapper);
            LDAPTestUtils.addUserAttributeMapper(appRealm, ldapModel, "zipCodeMapper-cs", "postal_code", "POstalCode");

            // Fetch user from LDAP and check that postalCode is filled
            UserModel user = session.users().getUserByUsername("johnzip", appRealm);
            String postalCode = user.getFirstAttribute("postal_code");
            Assert.assertEquals("12398", postalCode);

        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void testCommaInUsername() {
        KeycloakSession session = keycloakRule.startSession();
        boolean skip = false;

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            // Workaround as comma is not allowed in sAMAccountName on active directory. So we will skip the test for this configuration
            LDAPConfig config = ldapFedProvider.getLdapIdentityStore().getConfig();
            if (config.isActiveDirectory() && config.getUsernameLdapAttribute().equals(LDAPConstants.SAM_ACCOUNT_NAME)) {
                skip = true;
            }

            if (!skip) {
                LDAPObject johnComma = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "john,comma", "John", "Comma", "johncomma@email.org", null, "12387");
                LDAPTestUtils.updateLDAPPassword(ldapFedProvider, johnComma, "Password1");

                LDAPObject johnPlus = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "john+plus,comma", "John", "Plus", "johnplus@email.org", null, "12387");
                LDAPTestUtils.updateLDAPPassword(ldapFedProvider, johnPlus, "Password1");
            }
        } finally {
            keycloakRule.stopSession(session, false);
        }

        if (!skip) {
            // Try to import the user with comma in username into Keycloak
            loginSuccessAndLogout("john,comma", "Password1");
            loginSuccessAndLogout("john+plus,comma", "Password1");
        }
    }

    //@Test // don't think we should support this, bburke
    public void testDirectLDAPUpdate() {
        KeycloakSession session = keycloakRule.startSession();

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPObject johnDirect = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "johndirect", "John", "Direct", "johndirect@email.org", null, "12399");

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
            ComponentModel zipMapper = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "zipCodeMapper");
            zipMapper.getConfig().putSingle(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true");
            appRealm.updateComponent(zipMapper);

            // Update lastName mapper to read the value from Keycloak DB
            ComponentModel lastNameMapper = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "last name");
            lastNameMapper.getConfig().putSingle(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "false");
            appRealm.updateComponent(lastNameMapper);

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


    // TODO: Rather separate test for fullNameMapper to better test all the possibilities
    @Test
    public void testFullNameMapper() {
        KeycloakSession session = keycloakRule.startSession();
        ComponentModel firstNameMapper = null;

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            // assert that user "fullnameUser" is not in local DB
            Assert.assertNull(session.users().getUserByUsername("fullname", appRealm));

            // Add the user with some fullName into LDAP directly. Ensure that fullName is saved into "cn" attribute in LDAP (currently mapped to model firstName)
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "fullname", "James Dee", "Dee", "fullname@email.org", null, "4578");

            // add fullname mapper to the provider and remove "firstNameMapper". For this test, we will simply map full name to the LDAP attribute, which was before firstName ( "givenName" on active directory, "cn" on other LDAP servers)
            firstNameMapper =  LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "first name");
            String ldapFirstNameAttributeName = firstNameMapper.getConfig().getFirst(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE);
            appRealm.removeComponent(firstNameMapper);

            ComponentModel fullNameMapperModel = KeycloakModelUtils.createComponentModel("full name", ldapModel.getId(), FullNameLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE, ldapFirstNameAttributeName,
                    FullNameLDAPStorageMapper.READ_ONLY, "false");
            appRealm.addComponentModel(fullNameMapperModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            LDAPTestUtils.assertUserImported(session.users(), appRealm, "fullname", "James", "Dee", "fullname@email.org", "4578");

            // change mapper to writeOnly
            ComponentModel fullNameMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "full name");
            fullNameMapperModel.getConfig().putSingle(FullNameLDAPStorageMapper.WRITE_ONLY, "true");
            appRealm.updateComponent(fullNameMapperModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }


        // Assert changing user in Keycloak will change him in LDAP too...
        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            UserModel fullnameUser = session.users().getUserByUsername("fullname", appRealm);
            fullnameUser.setFirstName("James2");
            fullnameUser.setLastName("Dee2");
        } finally {
            keycloakRule.stopSession(session, true);
        }


        // Assert changed user available in Keycloak
        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            LDAPTestUtils.assertUserImported(session.users(), appRealm, "fullname", "James2", "Dee2", "fullname@email.org", "4578");

            // Remove "fullnameUser" to assert he is removed from LDAP. Revert mappers to previous state
            UserModel fullnameUser = session.users().getUserByUsername("fullname", appRealm);
            session.users().removeUser(appRealm, fullnameUser);

            // Revert mappers
            ComponentModel fullNameMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "full name");
            appRealm.removeComponent(fullNameMapperModel);

            firstNameMapper.setId(null);
            appRealm.addComponentModel(firstNameMapper);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    @Test
    public void testHardcodedAttributeMapperTest() throws Exception {
        // Create hardcoded mapper for "description"
        KeycloakSession session = keycloakRule.startSession();
        ComponentModel hardcodedMapperModel = null;

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            hardcodedMapperModel = KeycloakModelUtils.createComponentModel("hardcodedAttr-description", ldapModel.getId(), HardcodedLDAPAttributeMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_NAME, "description",
                    HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_VALUE, "some-${RANDOM}");
            appRealm.addComponentModel(hardcodedMapperModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        // Register new user
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "email34@check.cz", "register123", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());



        session = keycloakRule.startSession();
        ComponentModel userAttrMapper = null;
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            // See that user don't yet have any description
            UserModel user = LDAPTestUtils.assertUserImported(session.users(), appRealm, "register123", "firstName", "lastName", "email34@check.cz", null);
            Assert.assertNull(user.getFirstAttribute("desc"));
            Assert.assertNull(user.getFirstAttribute("description"));

            // Remove hardcoded mapper for "description" and create regular userAttribute mapper for description
            appRealm.removeComponent(hardcodedMapperModel);

            userAttrMapper = LDAPTestUtils.addUserAttributeMapper(appRealm, ldapModel, "desc-attribute-mapper", "desc", "description");
            userAttrMapper.put(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true");
            appRealm.updateComponent(userAttrMapper);
        } finally {
            keycloakRule.stopSession(session, true);
        }



        // Check that user has description on him now
        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");

            session.userCache().evict(appRealm, session.users().getUserByUsername("register123", appRealm));

            // See that user don't yet have any description
            UserModel user = session.users().getUserByUsername("register123", appRealm);
            Assert.assertNull(user.getFirstAttribute("description"));
            Assert.assertNotNull(user.getFirstAttribute("desc"));
            String desc = user.getFirstAttribute("desc");
            Assert.assertTrue(desc.startsWith("some-"));
            Assert.assertEquals(35, desc.length());

            // Remove mapper for "description"
            appRealm.removeComponent(userAttrMapper);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }


    @Test
    public void testHardcodedRoleMapper() {
        KeycloakSession session = keycloakRule.startSession();
        ComponentModel firstNameMapper = null;

        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");
            RoleModel hardcodedRole = appRealm.addRole("hardcoded-role");

            // assert that user "johnkeycloak" doesn't have hardcoded role
            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertFalse(john.hasRole(hardcodedRole));

            ComponentModel hardcodedMapperModel = KeycloakModelUtils.createComponentModel("hardcoded role", ldapModel.getId(), HardcodedLDAPRoleStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    HardcodedLDAPRoleStorageMapper.ROLE, "hardcoded-role");
            appRealm.addComponentModel(hardcodedMapperModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");
            RoleModel hardcodedRole = appRealm.getRole("hardcoded-role");

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertTrue(john.hasRole(hardcodedRole));

            // Can't remove user from hardcoded role
            try {
                john.deleteRoleMapping(hardcodedRole);
                Assert.fail("Didn't expected to remove role mapping");
            } catch (ModelException expected) {
            }

            // Revert mappers
            ComponentModel hardcodedMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "hardcoded role");
            appRealm.removeComponent(hardcodedMapperModel);
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
                LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
                LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "marykeycloak", "Mary1", "Kelly1", "mary1@email.org", null, "123");
                LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "mary-duplicatemail", "Mary2", "Kelly2", "mary@test.com", null, "123");
                LDAPObject marynoemail = LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "marynoemail", "Mary1", "Kelly1", null, null, "123");
                LDAPTestUtils.updateLDAPPassword(ldapFedProvider, marynoemail, "Password1");
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

            UserStorageProviderModel model = new UserStorageProviderModel(ldapModel);
            model.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.toString());
            appRealm.updateComponent(model);
            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ldapModel.getId());
            try {
                user.setEmail("error@error.com");
                Assert.fail("should fail");
            } catch (ReadOnlyException e) {

            }
            try {
                user.setLastName("Berk");
                Assert.fail("should fail");
            } catch (ReadOnlyException e) {

            }
            try {
                user.setFirstName("Bilbo");
                Assert.fail("should fail");
            } catch (ReadOnlyException e) {

            }
            try {
                UserCredentialModel cred = UserCredentialModel.password("PoopyPoop1", true);
                session.userCredentialManager().updateCredential(appRealm, user, cred);
                Assert.fail("should fail");
            } catch (ReadOnlyException e) {

            }

            Assert.assertTrue(session.users().removeUser(appRealm, user));
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            Assert.assertEquals(UserStorageProvider.EditMode.WRITABLE.toString(), appRealm.getComponent(ldapModel.getId()).getConfig().getFirst(LDAPConstants.EDIT_MODE));
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }

    @Test
    public void testRemoveFederatedUser() {
        /*
        {
            KeycloakSession session = keycloakRule.startSession();
            RealmModel appRealm = session.realms().getRealmByName("test");
            UserModel user = session.users().getUserByUsername("registerUserSuccess2", appRealm);
            keycloakRule.stopSession(session, true);
            if (user == null) {
                registerUserLdapSuccess();
            }
        }
        */
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
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username1", "John1", "Doel1", "user1@email.org", null, "121");
            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username2", "John2", "Doel2", "user2@email.org", null, "122");
            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username3", "John3", "Doel3", "user3@email.org", null, "123");
            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username4", "John4", "Doel4", "user4@email.org", null, "124");

            // Users are not at local store at this moment
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username1", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username2", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username3", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username4", appRealm));

            // search by username
            session.users().searchForUser("username1", appRealm);
            LDAPTestUtils.assertUserImported(session.userLocalStorage(), appRealm, "username1", "John1", "Doel1", "user1@email.org", "121");

            // search by email
            session.users().searchForUser("user2@email.org", appRealm);
            LDAPTestUtils.assertUserImported(session.userLocalStorage(), appRealm, "username2", "John2", "Doel2", "user2@email.org", "122");

            // search by lastName
            session.users().searchForUser("Doel3", appRealm);
            LDAPTestUtils.assertUserImported(session.userLocalStorage(), appRealm, "username3", "John3", "Doel3", "user3@email.org", "123");

            // search by firstName + lastName
            session.users().searchForUser("John4 Doel4", appRealm);
            LDAPTestUtils.assertUserImported(session.userLocalStorage(), appRealm, "username4", "John4", "Doel4", "user4@email.org", "124");
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void testSearchWithCustomLDAPFilter() {
        // Add custom filter for searching users
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            ldapModel.getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(|(mail=user5@email.org)(mail=user6@email.org))");
            appRealm.updateComponent(ldapModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }

        session = keycloakRule.startSession();
        try {
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            RealmModel appRealm = session.realms().getRealmByName("test");

            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username5", "John5", "Doel5", "user5@email.org", null, "125");
            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username6", "John6", "Doel6", "user6@email.org", null, "126");
            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username7", "John7", "Doel7", "user7@email.org", null, "127");

            // search by email
            List<UserModel> list = session.users().searchForUser("user5@email.org", appRealm);
            LDAPTestUtils.assertUserImported(session.userLocalStorage(), appRealm, "username5", "John5", "Doel5", "user5@email.org", "125");

            session.users().searchForUser("John6 Doel6", appRealm);
            LDAPTestUtils.assertUserImported(session.userLocalStorage(), appRealm, "username6", "John6", "Doel6", "user6@email.org", "126");

            session.users().searchForUser("user7@email.org", appRealm);
            session.users().searchForUser("John7 Doel7", appRealm);
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username7", appRealm));

            // Remove custom filter
            ldapModel.getConfig().remove(LDAPConstants.CUSTOM_USER_SEARCH_FILTER);
            appRealm.updateComponent(ldapModel);
        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

    @Test
    public void testUnsynced() throws Exception {
        KeycloakSession session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");

            UserStorageProviderModel model = new UserStorageProviderModel(ldapModel);
            model.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.toString());
            appRealm.updateComponent(model);
            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ldapModel.getId());

            UserCredentialModel cred = UserCredentialModel.password("Candycand1", true);
            session.userCredentialManager().updateCredential(appRealm, user, cred);
            CredentialModel userCredentialValueModel = session.userCredentialManager().getStoredCredentialsByType(appRealm, user, CredentialModel.PASSWORD).get(0);
            Assert.assertEquals(UserCredentialModel.PASSWORD, userCredentialValueModel.getType());
            Assert.assertTrue(session.userCredentialManager().isValid(appRealm, user, cred));

            // LDAP password is still unchanged
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, model);
            LDAPObject ldapUser = ldapProvider.loadLDAPUserByUsername(appRealm, "johnkeycloak");
            ldapProvider.getLdapIdentityStore().validatePassword(ldapUser, "Password1");

            // User is deleted just locally
            Assert.assertTrue(session.users().removeUser(appRealm, user));

            // Assert user not available locally, but will be reimported from LDAP once searched
            Assert.assertNull(session.userLocalStorage().getUserByUsername("johnkeycloak", appRealm));
            Assert.assertNotNull(session.users().getUserByUsername("johnkeycloak", appRealm));
        } finally {
            keycloakRule.stopSession(session, false);
        }

        session = keycloakRule.startSession();
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            Assert.assertEquals(UserStorageProvider.EditMode.WRITABLE.toString(),  appRealm.getComponent(ldapModel.getId()).getConfig().getFirst(LDAPConstants.EDIT_MODE));
        } finally {
            keycloakRule.stopSession(session, false);
        }
    }
    
    
    @Test
    public void testSearchByAttributes() {
        KeycloakSession session = keycloakRule.startSession();
        final String ATTRIBUTE = "postal_code";
        final String ATTRIBUTE_VALUE = "80330340";
        try {
            RealmModel appRealm = session.realms().getRealmByName("test");
            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);

            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username8", "John8", "Doel8", "user8@email.org", null, ATTRIBUTE_VALUE);
            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username9", "John9", "Doel9", "user9@email.org", null, ATTRIBUTE_VALUE);
            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "username10", "John10", "Doel10", "user10@email.org", null, "1210");
            
            // Users are not at local store at this moment
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username8", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username9", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username10", appRealm));

            // search for user by attribute
            List<UserModel> users = ldapProvider.searchForUserByUserAttribute(ATTRIBUTE, ATTRIBUTE_VALUE, appRealm);
            assertEquals(2, users.size());
            assertNotNull(users.get(0).getAttribute(ATTRIBUTE));
            assertEquals(1, users.get(0).getAttribute(ATTRIBUTE).size());
            assertEquals(ATTRIBUTE_VALUE, users.get(0).getAttribute(ATTRIBUTE).get(0));

            assertNotNull(users.get(1).getAttribute(ATTRIBUTE));
            assertEquals(1, users.get(1).getAttribute(ATTRIBUTE).size());
            assertEquals(ATTRIBUTE_VALUE, users.get(1).getAttribute(ATTRIBUTE).get(0));

	    // user are now imported to local store
            LDAPTestUtils.assertUserImported(session.userLocalStorage(), appRealm, "username8", "John8", "Doel8", "user8@email.org", ATTRIBUTE_VALUE);
            LDAPTestUtils.assertUserImported(session.userLocalStorage(), appRealm, "username9", "John9", "Doel9", "user9@email.org", ATTRIBUTE_VALUE);
            // but the one not looked up is not
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username10", appRealm));

        } finally {
            keycloakRule.stopSession(session, true);
        }
    }

}
