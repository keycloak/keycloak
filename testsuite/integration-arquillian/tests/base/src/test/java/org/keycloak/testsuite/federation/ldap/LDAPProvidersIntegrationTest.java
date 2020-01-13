/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.federation.ldap;

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialModel;
import org.keycloak.models.GroupModel;
import org.keycloak.models.LDAPConstants;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.cache.CachedUserModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.storage.ReadOnlyException;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.UserStorageProviderModel;
import org.keycloak.storage.ldap.LDAPConfig;
import org.keycloak.storage.ldap.LDAPStorageProvider;
import org.keycloak.storage.ldap.idm.model.LDAPObject;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.FullNameLDAPStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPAttributeMapper;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPAttributeMapperFactory;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPGroupStorageMapper;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPGroupStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPRoleStorageMapper;
import org.keycloak.storage.ldap.mappers.HardcodedLDAPRoleStorageMapperFactory;
import org.keycloak.storage.ldap.mappers.LDAPStorageMapper;
import org.keycloak.storage.ldap.mappers.UserAttributeLDAPStorageMapper;
import org.keycloak.testsuite.AbstractAuthTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.LDAPRule;
import org.keycloak.testsuite.util.LDAPTestUtils;
import org.keycloak.testsuite.util.OAuthClient;

import javax.naming.AuthenticationException;
import javax.ws.rs.core.Response;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LDAPProvidersIntegrationTest extends AbstractLDAPTest {

    @ClassRule
    public static LDAPRule ldapRule = new LDAPRule();

    @Override
    protected LDAPRule getLDAPRule() {
        return ldapRule;
    }

    @Override
    protected void afterImportTestRealm() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLocalUser(session, appRealm, "marykeycloak", "mary@test.com", "password-app");

            LDAPTestUtils.addZipCodeLDAPMapper(appRealm, ctx.getLdapModel());

            // Delete all LDAP users and add some new for testing
            LDAPTestUtils.removeAllLDAPUsers(ctx.getLdapProvider(), appRealm);

            LDAPObject john = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "johnkeycloak", "John", "Doe", "john@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), john, "Password1");

            LDAPObject existing = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "existing", "Existing", "Foo", "existing@email.org", null, "5678");

            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);

        });
    }



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
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().put(LDAPConstants.SYNC_REGISTRATIONS, "false");
            ctx.getRealm().updateComponent(ctx.getLdapModel());
        });

        UserRepresentation newUser1 = AbstractAuthTest.createUserRepresentation("newUser1", "newUser1@email.cz", null, null, true);
        Response resp = testRealm().users().create(newUser1);
        String userId = ApiUtil.getCreatedId(resp);
        resp.close();

        testRealm().users().get(userId).toRepresentation();
        Assert.assertTrue(StorageId.isLocalStorage(userId));
        Assert.assertNull(newUser1.getFederationLink());

        // Revert
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.SYNC_REGISTRATIONS, "true");
            ctx.getRealm().updateComponent(ctx.getLdapModel());
        });
    }


    @Test
    public void testRemoveImportedUsers() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            UserModel user = session.users().getUserByUsername("johnkeycloak", ctx.getRealm());
            Assert.assertEquals(ctx.getLdapModel().getId(), user.getFederationLink());
        });

        adminClient.realm("test").userStorage().removeImportedUsers(ldapModelId);

        testingClient.server().run(session -> {
            RealmManager manager = new RealmManager(session);
            RealmModel appRealm = manager.getRealm("test");
            UserModel user = session.userLocalStorage().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNull(user);
        });
    }

    // test name prefixed with zz to make sure it runs last as we are unlinking imported users
    @Test
    public void zzTestUnlinkUsers() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            UserModel user = session.users().getUserByUsername("johnkeycloak", ctx.getRealm());
            Assert.assertEquals(ctx.getLdapModel().getId(), user.getFederationLink());
        });

        adminClient.realm("test").userStorage().unlink(ldapModelId);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            UserModel user = session.users().getUserByUsername("johnkeycloak", ctx.getRealm());
            Assert.assertNotNull(user);
            Assert.assertNull(user.getFederationLink());
        });
    }

    @Test
    public void caseInSensitiveImport() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPObject jbrown2 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "JBrown2", "John", "Brown2", "jbrown2@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), jbrown2, "Password1");
            LDAPObject jbrown3 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "jbrown3", "John", "Brown3", "JBrown3@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), jbrown3, "Password1");
        });

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
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            LDAPObject jbrown4 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "JBrown4", "John", "Brown4", "jbrown4@email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), jbrown4, "Password1");
            LDAPObject jbrown5 = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "jbrown5", "John", "Brown5", "JBrown5@Email.org", null, "1234");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), jbrown5, "Password1");
        });

        // search by username
        List<UserRepresentation> users = testRealm().users().search("JBROwn4", 0, 10);
        UserRepresentation user4 = users.get(0);
        Assert.assertEquals("jbrown4", user4.getUsername());
        Assert.assertEquals("jbrown4@email.org", user4.getEmail());

        // search by email
        users = testRealm().users().search("JBROwn5@eMAil.org", 0, 10);
        Assert.assertEquals(1, users.size());
        UserRepresentation user5 = users.get(0);
        Assert.assertEquals("jbrown5", user5.getUsername());
        Assert.assertEquals("jbrown5@email.org", user5.getEmail());
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

        ComponentRepresentation ldapRep = testRealm().components().component(ldapModelId).toRepresentation();
        testRealm().components().component(ldapModelId).remove();

        // User not available once LDAP provider was removed
        loginPage.open();
        loginPage.login("johnkeycloak", "Password1");
        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        // Re-add LDAP provider
        Map<String, String> cfg = getLDAPRule().getConfig();
        ldapModelId = testingClient.testing().ldap(TEST_REALM_NAME).createLDAPProvider(cfg, isImportEnabled());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            LDAPTestUtils.addZipCodeLDAPMapper(ctx.getRealm(), ctx.getLdapModel());
        });

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
        Assert.assertEquals(200, response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

        response = oauth.doGrantAccessTokenRequest("password", "johnkeycloak", "");
        Assert.assertEquals(401, response.getStatusCode());
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
    public void ldapPasswordChangeWithAccountConsole() throws Exception {
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


    // KEYCLOAK-12340
    @Test
    public void ldapPasswordChangeWithAdminEndpointAndRequiredAction() throws Exception {
        String username = "adminEndpointAndRequiredActionTest";
        String email = username + "@email.cz";

        // Register new LDAP user with password, logout user
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();
        registerPage.register("firstName", "lastName", email,
                username, "Password1", "Password1");


        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        appPage.logout();

        // Test admin endpoint. Assert federated endpoint returns password in LDAP "supportedCredentials", but there is no stored password
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), username);
        assertPasswordConfiguredThroughLDAPOnly(user);

        // Update password through admin REST endpoint. Assert user can authenticate with the new password
        ApiUtil.resetUserPassword(user, "Password1-updated1", false);

        loginPage.open();

        loginSuccessAndLogout(username, "Password1-updated1");

        // Test admin endpoint. Assert federated endpoint returns password in LDAP "supportedCredentials", but there is no stored password
        assertPasswordConfiguredThroughLDAPOnly(user);

        // Test this just for the import mode. No-import mode doesn't support requiredActions right now
        if (isImportEnabled()) {
            // Update password through required action.
            UserRepresentation user2 = user.toRepresentation();
            user2.setRequiredActions(Arrays.asList(UserModel.RequiredAction.UPDATE_PASSWORD.toString()));
            user.update(user2);

            loginPage.open();
            loginPage.login(username, "Password1-updated1");
            requiredActionChangePasswordPage.assertCurrent();

            requiredActionChangePasswordPage.changePassword("Password1-updated2", "Password1-updated2");

            appPage.assertCurrent();
            appPage.logout();

            // Assert user can authenticate with the new password
            loginSuccessAndLogout(username, "Password1-updated2");

            // Test admin endpoint. Assert federated endpoint returns password in LDAP "supportedCredentials", but there is no stored password
            assertPasswordConfiguredThroughLDAPOnly(user);
        }
    }


    // Use admin REST endpoints
    private void assertPasswordConfiguredThroughLDAPOnly(UserResource user) {
        // Assert password not stored locally
        List<CredentialRepresentation> storedCredentials = user.credentials();
        for (CredentialRepresentation credential : storedCredentials) {
            Assert.assertFalse(PasswordCredentialModel.TYPE.equals(credential.getType()));
        }

        // Assert password is stored in the LDAP
        List<String> userStorageCredentials = user.getConfiguredUserStorageCredentialTypes();
        Assert.assertTrue(userStorageCredentials.contains(PasswordCredentialModel.TYPE));
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
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPConfig config = ctx.getLdapProvider().getLdapIdentityStore().getConfig();

            // Make sure mary is gone
            LDAPTestUtils.removeLDAPUserByUsername(ctx.getLdapProvider(), ctx.getRealm(), config, "maryjane");

            // Create the user in LDAP and register him

            LDAPObject mary = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "maryjane", "mary", "yram", "mj@testing.redhat.cz", null, "12398");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), mary, "Password1");

        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            LDAPConfig config = ctx.getLdapProvider().getLdapIdentityStore().getConfig();

            // Delete LDAP User
            LDAPTestUtils.removeLDAPUserByUsername(ctx.getLdapProvider(), ctx.getRealm(), config, "maryjane");

            // Make sure the deletion took place.
            List<UserModel> deletedUsers = session.users().searchForUser("mary yram", ctx.getRealm());
            Assert.assertTrue(deletedUsers.isEmpty());

        });
    }


    @Test
    public void registerUserLdapSuccess() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "email2@check.cz", "registerUserSuccess2", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(),"registerUserSuccess2");
        Assert.assertNotNull(user);
        assertFederatedUserLink(user);
        Assert.assertEquals("registerusersuccess2", user.getUsername());
        Assert.assertEquals("firstName", user.getFirstName());
        Assert.assertEquals("lastName", user.getLastName());
        Assert.assertTrue(user.isEnabled());
    }


    protected void assertFederatedUserLink(UserRepresentation user) {
        Assert.assertTrue(StorageId.isLocalStorage(user.getId()));
        Assert.assertNotNull(user.getFederationLink());
        Assert.assertEquals(user.getFederationLink(), ldapModelId);
    }


    @Test
    public void testCaseSensitiveAttributeName() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(session, appRealm);
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

        });
    }

    @Test
    public void testCommaInUsername() {
        Boolean skipTest = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            boolean skip = false;

            // Workaround as comma is not allowed in sAMAccountName on active directory. So we will skip the test for this configuration
            LDAPConfig config = ctx.getLdapProvider().getLdapIdentityStore().getConfig();
            if (config.isActiveDirectory() && config.getUsernameLdapAttribute().equals(LDAPConstants.SAM_ACCOUNT_NAME)) {
                skip = true;
            }

            if (!skip) {
                LDAPObject johnComma = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "john,comma", "John", "Comma", "johncomma@email.org", null, "12387");
                LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), johnComma, "Password1");

                LDAPObject johnPlus = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "john+plus,comma", "John", "Plus", "johnplus@email.org", null, "12387");
                LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), johnPlus, "Password1");
            }

            return skip;

        }, Boolean.class);

        if (!skipTest) {
            // Try to import the user with comma in username into Keycloak
            loginSuccessAndLogout("john,comma", "Password1");
            loginSuccessAndLogout("john+plus,comma", "Password1");
        }
    }


    // TODO: Rather separate test class for fullNameMapper to better test all the possibilities
    @Test
    public void testFullNameMapper() {

        ComponentRepresentation firstNameMapperRep = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // assert that user "fullnameUser" is not in local DB
            Assert.assertNull(session.users().getUserByUsername("fullname", appRealm));

            // Add the user with some fullName into LDAP directly. Ensure that fullName is saved into "cn" attribute in LDAP (currently mapped to model firstName)
            ComponentModel ldapModel = LDAPTestUtils.getLdapProviderModel(session, appRealm);
            LDAPStorageProvider ldapFedProvider = LDAPTestUtils.getLdapProvider(session, ldapModel);
            LDAPTestUtils.addLDAPUser(ldapFedProvider, appRealm, "fullname", "James Dee", "Dee", "fullname@email.org", null, "4578");

            // add fullname mapper to the provider and remove "firstNameMapper". For this test, we will simply map full name to the LDAP attribute, which was before firstName ( "givenName" on active directory, "cn" on other LDAP servers)
            ComponentModel firstNameMapper =  LDAPTestUtils.getSubcomponentByName(appRealm, ldapModel, "first name");
            String ldapFirstNameAttributeName = firstNameMapper.getConfig().getFirst(UserAttributeLDAPStorageMapper.LDAP_ATTRIBUTE);
            appRealm.removeComponent(firstNameMapper);

            ComponentRepresentation firstNameMapperRepp = ModelToRepresentation.toRepresentation(session, firstNameMapper, true);

            ComponentModel fullNameMapperModel = KeycloakModelUtils.createComponentModel("full name", ldapModel.getId(), FullNameLDAPStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    FullNameLDAPStorageMapper.LDAP_FULL_NAME_ATTRIBUTE, ldapFirstNameAttributeName,
                    FullNameLDAPStorageMapper.READ_ONLY, "false");
            appRealm.addComponentModel(fullNameMapperModel);

            return firstNameMapperRepp;
        }, ComponentRepresentation.class);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            LDAPTestAsserts.assertUserImported(session.users(), appRealm, "fullname", "James", "Dee", "fullname@email.org", "4578");
        });

        // Assert user will be changed in LDAP too
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel fullnameUser = session.users().getUserByUsername("fullname", appRealm);
            fullnameUser.setFirstName("James2");
            fullnameUser.setLastName("Dee2");
        });


        // Assert changed user available in Keycloak
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            LDAPTestAsserts.assertUserImported(session.users(), appRealm, "fullname", "James2", "Dee2", "fullname@email.org", "4578");

            // Remove "fullnameUser" to assert he is removed from LDAP. Revert mappers to previous state
            UserModel fullnameUser = session.users().getUserByUsername("fullname", appRealm);
            session.users().removeUser(appRealm, fullnameUser);

            // Revert mappers
            ComponentModel fullNameMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "full name");
            appRealm.removeComponent(fullNameMapperModel);
        });

        firstNameMapperRep.setId(null);
        Response response = testRealm().components().add(firstNameMapperRep);
        Assert.assertEquals(201, response.getStatus());
        response.close();
    }


    @Test
    public void testHardcodedAttributeMapperTest() throws Exception {
        // Create hardcoded mapper for "description"
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            ComponentModel hardcodedMapperModel = KeycloakModelUtils.createComponentModel("hardcodedAttr-description", ctx.getLdapModel().getId(), HardcodedLDAPAttributeMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_NAME, "description",
                    HardcodedLDAPAttributeMapper.LDAP_ATTRIBUTE_VALUE, "some-${RANDOM}");
            ctx.getRealm().addComponentModel(hardcodedMapperModel);
        });

        // Register new user
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "email34@check.cz", "register123", "Password1", "Password1");
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            // See that user don't yet have any description
            UserModel user = LDAPTestAsserts.assertUserImported(session.users(), appRealm, "register123", "firstName", "lastName", "email34@check.cz", null);
            Assert.assertNull(user.getFirstAttribute("desc"));
            Assert.assertNull(user.getFirstAttribute("description"));

            // Remove hardcoded mapper for "description" and create regular userAttribute mapper for description
            ComponentModel hardcodedMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "hardcodedAttr-description");
            appRealm.removeComponent(hardcodedMapperModel);

            ComponentModel userAttrMapper = LDAPTestUtils.addUserAttributeMapper(appRealm, ctx.getLdapModel(), "desc-attribute-mapper", "desc", "description");
            userAttrMapper.put(UserAttributeLDAPStorageMapper.ALWAYS_READ_VALUE_FROM_LDAP, "true");
            appRealm.updateComponent(userAttrMapper);
        });

        // Check that user has description on him now
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            session.userCache().evict(appRealm, session.users().getUserByUsername("register123", appRealm));

            // See that user don't yet have any description
            UserModel user = session.users().getUserByUsername("register123", appRealm);
            Assert.assertNull(user.getFirstAttribute("description"));
            Assert.assertNotNull(user.getFirstAttribute("desc"));
            String desc = user.getFirstAttribute("desc");
            Assert.assertTrue(desc.startsWith("some-"));
            Assert.assertEquals(35, desc.length());

            // Remove mapper for "description"
            ComponentModel userAttrMapper = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "desc-attribute-mapper");
            appRealm.removeComponent(userAttrMapper);
        });
    }


    @Test
    public void testHardcodedRoleMapper() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            RoleModel hardcodedRole = appRealm.addRole("hardcoded-role");

            // assert that user "johnkeycloak" doesn't have hardcoded role
            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertFalse(john.hasRole(hardcodedRole));

            ComponentModel hardcodedMapperModel = KeycloakModelUtils.createComponentModel("hardcoded role", ctx.getLdapModel().getId(),
                    HardcodedLDAPRoleStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    HardcodedLDAPRoleStorageMapper.ROLE, "hardcoded-role");
            appRealm.addComponentModel(hardcodedMapperModel);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

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
            ComponentModel hardcodedMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "hardcoded role");
            appRealm.removeComponent(hardcodedMapperModel);
        });
    }

    @Test
    public void testHardcodedGroupMapper() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            GroupModel hardcodedGroup = appRealm.createGroup("hardcoded-group", "hardcoded-group");

            // assert that user "johnkeycloak" doesn't have hardcoded group
            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertFalse(john.isMemberOf(hardcodedGroup));

            ComponentModel hardcodedMapperModel = KeycloakModelUtils.createComponentModel("hardcoded group",
                    ctx.getLdapModel().getId(), HardcodedLDAPGroupStorageMapperFactory.PROVIDER_ID, LDAPStorageMapper.class.getName(),
                    HardcodedLDAPGroupStorageMapper.GROUP, "hardcoded-group");
            appRealm.addComponentModel(hardcodedMapperModel);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            GroupModel hardcodedGroup = appRealm.getGroupById("hardcoded-group");

            // Assert user is successfully imported in Keycloak DB now with correct firstName and lastName
            UserModel john = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertTrue(john.isMemberOf(hardcodedGroup));

            // Can't remove user from hardcoded role
            try {
                john.leaveGroup(hardcodedGroup);
                Assert.fail("Didn't expected to leave group");
            } catch (ModelException expected) {
            }

            // Revert mappers
            ComponentModel hardcodedMapperModel = LDAPTestUtils.getSubcomponentByName(appRealm, ctx.getLdapModel(), "hardcoded group");
            appRealm.removeComponent(hardcodedMapperModel);
        });
    }

    @Test
    public void testImportExistingUserFromLDAP() throws Exception {
        // Add LDAP user with same email like existing model user
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "marykeycloak", "Mary1", "Kelly1", "mary1@email.org", null, "123");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "mary-duplicatemail", "Mary2", "Kelly2", "mary@test.com", null, "123");
            LDAPObject marynoemail = LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "marynoemail", "Mary1", "Kelly1", null, null, "123");
            LDAPTestUtils.updateLDAPPassword(ctx.getLdapProvider(), marynoemail, "Password1");
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
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.READ_ONLY.toString());
            appRealm.updateComponent(ctx.getLdapModel());
        });

        UserRepresentation userRep = ApiUtil.findUserByUsername(testRealm(), "johnkeycloak");
        assertFederatedUserLink(userRep);

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(user);
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
        });

        // Revert
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().put(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());
            appRealm.updateComponent(ctx.getLdapModel());

            Assert.assertEquals(UserStorageProvider.EditMode.WRITABLE.toString(),
                    appRealm.getComponent(ctx.getLdapModel().getId()).getConfig().getFirst(LDAPConstants.EDIT_MODE));
        });
    }

    @Test
    public void testRemoveFederatedUser() {
        UserRepresentation user = ApiUtil.findUserByUsername(testRealm(), "registerusersuccess2");

        // Case when this test was executed "alone" (User "registerusersuccess2" is registered inside registerUserLdapSuccess)
        if (user == null) {
            registerUserLdapSuccess();
            user = ApiUtil.findUserByUsername(testRealm(), "registerusersuccess2");
        }

        assertFederatedUserLink(user);
        testRealm().users().get(user.getId()).remove();
        user = ApiUtil.findUserByUsername(testRealm(), "registerusersuccess2");
        Assert.assertNull(user);
    }

    @Test
    public void testSearch() {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username1", "John1", "Doel1", "user1@email.org", null, "121");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username2", "John2", "Doel2", "user2@email.org", null, "122");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username3", "John3", "Doel3", "user3@email.org", null, "123");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username4", "John4", "Doel4", "user4@email.org", null, "124");

            // Users are not at local store at this moment
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username1", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username2", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username3", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username4", appRealm));

            // search by username
            session.users().searchForUser("username1", appRealm);
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), appRealm, "username1", "John1", "Doel1", "user1@email.org", "121");

            // search by email
            session.users().searchForUser("user2@email.org", appRealm);
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), appRealm, "username2", "John2", "Doel2", "user2@email.org", "122");

            // search by lastName
            session.users().searchForUser("Doel3", appRealm);
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), appRealm, "username3", "John3", "Doel3", "user3@email.org", "123");

            // search by firstName + lastName
            session.users().searchForUser("John4 Doel4", appRealm);
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), appRealm, "username4", "John4", "Doel4", "user4@email.org", "124");
        });
    }

    @Test
    public void testSearchWithCustomLDAPFilter() {
        // Add custom filter for searching users
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.CUSTOM_USER_SEARCH_FILTER, "(|(mail=user5@email.org)(mail=user6@email.org))");
            appRealm.updateComponent(ctx.getLdapModel());
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username5", "John5", "Doel5", "user5@email.org", null, "125");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username6", "John6", "Doel6", "user6@email.org", null, "126");
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username7", "John7", "Doel7", "user7@email.org", null, "127");

            // search by email
            List<UserModel> list = session.users().searchForUser("user5@email.org", appRealm);
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), appRealm, "username5", "John5", "Doel5", "user5@email.org", "125");

            session.users().searchForUser("John6 Doel6", appRealm);
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), appRealm, "username6", "John6", "Doel6", "user6@email.org", "126");

            session.users().searchForUser("user7@email.org", appRealm);
            session.users().searchForUser("John7 Doel7", appRealm);
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username7", appRealm));

            // Remove custom filter
            ctx.getLdapModel().getConfig().remove(LDAPConstants.CUSTOM_USER_SEARCH_FILTER);
            appRealm.updateComponent(ctx.getLdapModel());
        });
    }

    @Test
    public void testUnsynced() throws Exception {
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserStorageProviderModel model = new UserStorageProviderModel(ctx.getLdapModel());
            model.getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.UNSYNCED.toString());
            appRealm.updateComponent(model);
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getFederationLink());
            Assert.assertEquals(user.getFederationLink(), ctx.getLdapModel().getId());

            UserCredentialModel cred = UserCredentialModel.password("Candycand1", true);
            session.userCredentialManager().updateCredential(appRealm, user, cred);
            CredentialModel userCredentialValueModel = session.userCredentialManager().getStoredCredentialsByType(appRealm, user, PasswordCredentialModel.TYPE).get(0);
            Assert.assertEquals(PasswordCredentialModel.TYPE, userCredentialValueModel.getType());
            Assert.assertTrue(session.userCredentialManager().isValid(appRealm, user, cred));

            // LDAP password is still unchanged
            try {
                LDAPObject ldapUser = ctx.getLdapProvider().loadLDAPUserByUsername(appRealm, "johnkeycloak");
                ctx.getLdapProvider().getLdapIdentityStore().validatePassword(ldapUser, "Password1");
            } catch (AuthenticationException ex) {
                throw new RuntimeException(ex);
            }
        });

        // Test admin REST endpoints
        UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), "johnkeycloak");

        // Assert password is stored locally
        List<String> storedCredentials = userResource.credentials().stream()
                .map(CredentialRepresentation::getType)
                .collect(Collectors.toList());
        Assert.assertTrue(storedCredentials.contains(PasswordCredentialModel.TYPE));

        // Assert password is supported in the LDAP too.
        List<String> userStorageCredentials = userResource.getConfiguredUserStorageCredentialTypes();
        Assert.assertTrue(userStorageCredentials.contains(PasswordCredentialModel.TYPE));

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);

            // User is deleted just locally
            Assert.assertTrue(session.users().removeUser(appRealm, user));

            // Assert user not available locally, but will be reimported from LDAP once searched
            Assert.assertNull(session.userLocalStorage().getUserByUsername("johnkeycloak", appRealm));
            Assert.assertNotNull(session.users().getUserByUsername("johnkeycloak", appRealm));
        });

        // Revert
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            ctx.getLdapModel().getConfig().putSingle(LDAPConstants.EDIT_MODE, UserStorageProvider.EditMode.WRITABLE.toString());

            appRealm.updateComponent(ctx.getLdapModel());

            Assert.assertEquals(UserStorageProvider.EditMode.WRITABLE.toString(), appRealm.getComponent(ctx.getLdapModel().getId()).getConfig().getFirst(LDAPConstants.EDIT_MODE));
        });
    }


    @Test
    public void testSearchByAttributes() {
        testingClient.server().run(session -> {
            final String ATTRIBUTE = "postal_code";
            final String ATTRIBUTE_VALUE = "80330340";

            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username8", "John8", "Doel8", "user8@email.org", null, ATTRIBUTE_VALUE);
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username9", "John9", "Doel9", "user9@email.org", null, ATTRIBUTE_VALUE);
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), appRealm, "username10", "John10", "Doel10", "user10@email.org", null, "1210");

            // Users are not at local store at this moment
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username8", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username9", appRealm));
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username10", appRealm));

            // search for user by attribute
            List<UserModel> users = ctx.getLdapProvider().searchForUserByUserAttribute(ATTRIBUTE, ATTRIBUTE_VALUE, appRealm);
            assertEquals(2, users.size());
            assertNotNull(users.get(0).getAttribute(ATTRIBUTE));
            assertEquals(1, users.get(0).getAttribute(ATTRIBUTE).size());
            assertEquals(ATTRIBUTE_VALUE, users.get(0).getAttribute(ATTRIBUTE).get(0));

            assertNotNull(users.get(1).getAttribute(ATTRIBUTE));
            assertEquals(1, users.get(1).getAttribute(ATTRIBUTE).size());
            assertEquals(ATTRIBUTE_VALUE, users.get(1).getAttribute(ATTRIBUTE).get(0));

	        // user are now imported to local store
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), appRealm, "username8", "John8", "Doel8", "user8@email.org", ATTRIBUTE_VALUE);
            LDAPTestAsserts.assertUserImported(session.userLocalStorage(), appRealm, "username9", "John9", "Doel9", "user9@email.org", ATTRIBUTE_VALUE);
            // but the one not looked up is not
            Assert.assertNull(session.userLocalStorage().getUserByUsername("username10", appRealm));

        });
    }


    // KEYCLOAK-9002
    @Test
    public void testSearchWithPartiallyCachedUser() {
        testingClient.server().run(session -> {
            session.userCache().clear();
        });


        // This will load user from LDAP and partially cache him (including attributes)
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();
            UserModel user = session.users().getUserByUsername("johnkeycloak", appRealm);
            Assert.assertNotNull(user);

            user.getAttributes();
        });


        // Assert search without arguments won't blow up with StackOverflowError
        adminClient.realm("test").users().search(null, 0, 10, false);

        List<UserRepresentation> users = adminClient.realm("test").users().search("johnkeycloak", 0, 10, false);
        Assert.assertTrue(users.stream().anyMatch(userRepresentation -> "johnkeycloak".equals(userRepresentation.getUsername())));
    }


    @Test
    public void testLDAPUserRefreshCache() {
        testingClient.server().run(session -> {
            session.userCache().clear();
        });

        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            RealmModel appRealm = ctx.getRealm();

            LDAPStorageProvider ldapProvider = LDAPTestUtils.getLdapProvider(session, ctx.getLdapModel());
            LDAPTestUtils.addLDAPUser(ldapProvider, appRealm, "johndirect", "John", "Direct", "johndirect@email.org", null, "1234");

            // Fetch user from LDAP and check that postalCode is filled
            UserModel user = session.users().getUserByUsername("johndirect", appRealm);
            String postalCode = user.getFirstAttribute("postal_code");
            Assert.assertEquals("1234", postalCode);

            LDAPTestUtils.removeLDAPUserByUsername(ldapProvider, appRealm, ldapProvider.getLdapIdentityStore().getConfig(), "johndirect");
        });

        setTimeOffset(60 * 5); // 5 minutes in future, user should be cached still

        testingClient.server().run(session -> {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");
            CachedUserModel user = (CachedUserModel) session.users().getUserByUsername("johndirect", appRealm);
            String postalCode = user.getFirstAttribute("postal_code");
            String email = user.getEmail();
            Assert.assertEquals("1234", postalCode);
            Assert.assertEquals("johndirect@email.org", email);
        });

        setTimeOffset(60 * 20); // 20 minutes into future, cache will be invalidated

        testingClient.server().run(session -> {
            RealmModel appRealm = new RealmManager(session).getRealmByName("test");
            UserModel user = session.users().getUserByUsername("johndirect", appRealm);
            Assert.assertNull(user);
        });

        setTimeOffset(0);
    }

    @Test
    public void testCacheUser() {
        String userId = testingClient.server().fetch(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().setCachePolicy(UserStorageProviderModel.CachePolicy.NO_CACHE);
            ctx.getRealm().updateComponent(ctx.getLdapModel());

            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "testCacheUser", "John", "Cached", "johndirect@test.com", null, "1234");

            // Fetch user from LDAP and check that postalCode is filled
            UserModel testedUser = session.users().getUserByUsername("testCacheUser", ctx.getRealm());

            String usserId = testedUser.getId();
            Assert.assertNotNull(usserId);
            Assert.assertFalse(usserId.isEmpty());

            return usserId;
        }, String.class);

        testingClient.server().run(session -> {

            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            UserModel testedUser = session.users().getUserById(userId, appRealm);
            Assert.assertFalse(testedUser instanceof CachedUserModel);
        });

        // restore default cache policy
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);

            ctx.getLdapModel().setCachePolicy(UserStorageProviderModel.CachePolicy.MAX_LIFESPAN);
            ctx.getLdapModel().setMaxLifespan(600000); // Lifetime is 10 minutes
            ctx.getRealm().updateComponent(ctx.getLdapModel());
        });


        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            UserModel testedUser = session.users().getUserById(userId, appRealm);
            Assert.assertTrue(testedUser instanceof CachedUserModel);
        });

        setTimeOffset(60 * 5); // 5 minutes in future, should be cached still
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            UserModel testedUser = session.users().getUserById(userId, appRealm);
            Assert.assertTrue(testedUser instanceof CachedUserModel);
        });

        setTimeOffset(60 * 10); // 10 minutes into future, cache will be invalidated
        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            UserModel testedUser = session.users().getUserByUsername("thor", appRealm);
            Assert.assertFalse(testedUser instanceof CachedUserModel);
        });

        setTimeOffset(0);
    }

    @Test
    public void testEmailVerifiedFromImport(){

        // Test trusted email option
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().put(LDAPConstants.TRUST_EMAIL, "true");
            ctx.getRealm().updateComponent(ctx.getLdapModel());
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "testUserVerified", "John", "Email", "john@test.com", null, "1234");
        });
        loginPage.open();
        loginPage.login("testuserVerified", "password");

        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            List<UserModel> userVerified = session.users().searchForUser("john@test.com", appRealm);
            Assert.assertTrue(userVerified.get(0).isEmailVerified());
        });

        //Test untrusted email option 
        testingClient.server().run(session -> {
            LDAPTestContext ctx = LDAPTestContext.init(session);
            ctx.getLdapModel().put(LDAPConstants.TRUST_EMAIL, "false");
            ctx.getRealm().updateComponent(ctx.getLdapModel());
            LDAPTestUtils.addLDAPUser(ctx.getLdapProvider(), ctx.getRealm(), "testUserNotVerified", "John", "Email", "john2@test.com", null, "1234");
        });

        loginPage.open();
        loginPage.login("testuserNotVerified", "password");

        testingClient.server().run(session -> {
            RealmModel appRealm = session.realms().getRealmByName(TEST_REALM_NAME);
            List<UserModel> userNotVerified = session.users().searchForUser("john2@test.com", appRealm);
            Assert.assertFalse(userNotVerified.get(0).isEmailVerified());
        });
    }
}
