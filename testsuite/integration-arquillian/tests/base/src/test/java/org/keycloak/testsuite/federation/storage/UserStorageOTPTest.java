/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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
 *
 */

package org.keycloak.testsuite.federation.storage;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;


import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.DummyUserFederationProvider;
import org.keycloak.testsuite.federation.DummyUserFederationProviderFactory;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.UserBuilder;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;
import static org.keycloak.testsuite.federation.storage.UserStorageTest.addComponent;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UserStorageOTPTest extends AbstractTestRealmKeycloakTest {


    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LoginConfigTotpPage loginConfigTotpPage;

    @Page
    protected AppPage appPage;

    protected TimeBasedOTP totp = new TimeBasedOTP();



    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

    }

    @Before
    public void addProvidersBeforeTest() throws URISyntaxException, IOException {
        ComponentRepresentation dummyProvider = new ComponentRepresentation();
        dummyProvider.setName("dummy");
        dummyProvider.setId(DummyUserFederationProviderFactory.PROVIDER_NAME);
        dummyProvider.setProviderId(DummyUserFederationProviderFactory.PROVIDER_NAME);
        dummyProvider.setProviderType(UserStorageProvider.class.getName());
        dummyProvider.setConfig(new MultivaluedHashMap<>());
        dummyProvider.getConfig().putSingle("priority", Integer.toString(0));
        dummyProvider.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));

        addComponent(testRealm(), getCleanup(), dummyProvider);

        UserRepresentation user = UserBuilder.create()
                .username("test-user")
                .email("test-user@something.org")
                .build();
        String testUserId = ApiUtil.createUserWithAdminClient(testRealm(), user);

        getCleanup().addUserId(testUserId);
    }


    @Test
    public void testCredentialsThroughRESTAPI() {
        // Test that test-user has federation link on him
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user");
        Assert.assertEquals(DummyUserFederationProviderFactory.PROVIDER_NAME, user.toRepresentation().getFederationLink());

        // Test that both "password" and "otp" are configured for the test-user
        List<String> userStorageCredentialTypes = user.getConfiguredUserStorageCredentialTypes();
        Assert.assertNames(userStorageCredentialTypes, PasswordCredentialModel.TYPE, OTPCredentialModel.TYPE);
    }


    @Test
    public void testAuthentication() {
        // Test that user is required to provide OTP credential during authentication
        loginPage.open();
        loginPage.login("test-user", DummyUserFederationProvider.HARDCODED_PASSWORD);

        loginTotpPage.assertCurrent();

        loginTotpPage.login("654321");
        loginTotpPage.assertCurrent();
        Assert.assertEquals("Invalid authenticator code.", loginPage.getError());

        loginTotpPage.login(DummyUserFederationProvider.HARDCODED_OTP);

        appPage.assertCurrent();
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
    }


    @Test
    public void testUpdateOTP() {
        // Add requiredAction to the user for update OTP
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user");
        UserRepresentation userRep = user.toRepresentation();
        userRep.setRequiredActions(Collections.singletonList(UserModel.RequiredAction.CONFIGURE_TOTP.toString()));
        user.update(userRep);

        // Authenticate as the user
        loginPage.open();
        loginPage.login("test-user", DummyUserFederationProvider.HARDCODED_PASSWORD);
        loginTotpPage.assertCurrent();
        loginTotpPage.login(DummyUserFederationProvider.HARDCODED_OTP);

        // User should be required to update OTP
        loginConfigTotpPage.assertCurrent();

        // Dummy OTP code won't work when configure new OTP
        loginConfigTotpPage.configure(DummyUserFederationProvider.HARDCODED_OTP);
        Assert.assertEquals("Invalid authenticator code.", loginConfigTotpPage.getError());

        // This will save the credential to the local DB
        String totpSecret = loginConfigTotpPage.getTotpSecret();
        log.infof("Totp Secret: %s", totpSecret);
        String totpCode = totp.generateTOTP(totpSecret);
        loginConfigTotpPage.configure(totpCode);

        appPage.assertCurrent();

        // Logout
        appPage.logout();

        // Authenticate as the user again with the dummy OTP should still work
        loginPage.open();
        loginPage.login("test-user", DummyUserFederationProvider.HARDCODED_PASSWORD);
        loginTotpPage.assertCurrent();
        loginTotpPage.login(DummyUserFederationProvider.HARDCODED_OTP);

        appPage.assertCurrent();
        appPage.logout();

        // Authenticate with the new OTP code should work as well
        loginPage.open();
        loginPage.login("test-user", DummyUserFederationProvider.HARDCODED_PASSWORD);
        loginTotpPage.assertCurrent();
        loginTotpPage.login(totp.generateTOTP(totpSecret));

        appPage.assertCurrent();
        appPage.logout();
    }

    @Test
    public void testNormalUser() {
        // Add some other user to the KEycloak
        UserRepresentation user = UserBuilder.create()
                .username("test-user2")
                .email("test-user2@something.org")
                .build();
        String testUserId = ApiUtil.createUserWithAdminClient(testRealm(), user);
        getCleanup().addUserId(testUserId);

        // Assert he has federation link on him
        UserResource userResource = ApiUtil.findUserByUsernameId(testRealm(), "test-user2");
        Assert.assertEquals(DummyUserFederationProviderFactory.PROVIDER_NAME, userResource.toRepresentation().getFederationLink());

        // Assert no userStorage supported credentials shown through admin REST API for that user. For this user, the validation of password and OTP is not delegated
        // to the dummy user storage provider
        Assert.assertTrue(userResource.getConfiguredUserStorageCredentialTypes().isEmpty());

        // Update password
        ApiUtil.resetUserPassword(userResource, "pass", false);

        // Authenticate as the user. Only the password will be required for him
        loginPage.open();
        loginPage.login("test-user2", "pass");

        appPage.assertCurrent();
        Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
    }





}
