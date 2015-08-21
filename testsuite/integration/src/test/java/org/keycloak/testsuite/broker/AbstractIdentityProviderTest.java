/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.broker;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.testsuite.MailUtil;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.broker.util.UserSessionStatusServlet.UserSessionStatus;
import org.keycloak.testsuite.pages.AccountFederatedIdentityPage;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.rule.GreenMailRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author pedroigor
 */
public abstract class AbstractIdentityProviderTest {

    private static final URI BASE_URI = UriBuilder.fromUri("http://localhost:8081/auth").build();

    @ClassRule
    public static BrokerKeyCloakRule brokerServerRule = new BrokerKeyCloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    private LoginPage loginPage;

    @WebResource
    private LoginUpdateProfilePage updateProfilePage;


    @WebResource
    protected VerifyEmailPage verifyEmailPage;

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected OAuthGrantPage grantPage;

    @WebResource
    protected AccountPasswordPage changePasswordPage;

    @WebResource
    protected AccountFederatedIdentityPage accountFederatedIdentityPage;

    private KeycloakSession session;

    @Before
    public void onBefore() {
        this.session = brokerServerRule.startSession();
        removeTestUsers();
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();
        assertNotNull(getIdentityProviderModel());
    }

    @After
    public void onAfter() {
        revokeGrant();
        brokerServerRule.stopSession(this.session, true);
    }

    @Test
    public void testSuccessfulAuthentication() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_ON);

        UserModel user = assertSuccessfulAuthentication(identityProviderModel, "test-user", "new@email.com", true);
        Assert.assertEquals("617-666-7777", user.getFirstAttribute("mobile"));
    }

    @Test
    public void testSuccessfulAuthenticationUpdateProfileOnMissing_nothingMissing() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_MISSING);

        assertSuccessfulAuthentication(identityProviderModel, "test-user", "test-user@localhost", false);
    }

    @Test
    public void testSuccessfulAuthenticationUpdateProfileOnMissing_missingEmail() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_MISSING);

        assertSuccessfulAuthentication(identityProviderModel, "test-user-noemail", "new@email.com", true);
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_OFF);

        assertSuccessfulAuthentication(identityProviderModel, "test-user", "test-user@localhost", false);
    }

    /**
     * Test that verify email action is performed if email is provided and email trust is not enabled for the provider
     * 
     * @throws MessagingException
     * @throws IOException
     */
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailProvided_emailVerifyEnabled() throws IOException, MessagingException {
        getRealm().setVerifyEmail(true);
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        try {
            identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_OFF);
            identityProviderModel.setTrustEmail(false);

            UserModel federatedUser = assertSuccessfulAuthenticationWithEmailVerification(identityProviderModel, "test-user", "test-user@localhost", false);

            // email is verified now
            assertFalse(federatedUser.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL.name()));

        } finally {
            getRealm().setVerifyEmail(false);
        }
    }

    private UserModel assertSuccessfulAuthenticationWithEmailVerification(IdentityProviderModel identityProviderModel, String username, String expectedEmail,
            boolean isProfileUpdateExpected)
            throws IOException, MessagingException {
        authenticateWithIdentityProvider(identityProviderModel, username, isProfileUpdateExpected);

        // verify email is sent
        Assert.assertTrue(verifyEmailPage.isCurrent());

        // read email to take verification link from
        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String verificationUrl = getVerificationEmailLink(message);

        driver.navigate().to(verificationUrl.trim());

        // authenticated and redirected to app
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);

        doAssertFederatedUser(federatedUser, identityProviderModel, expectedEmail, isProfileUpdateExpected);

        brokerServerRule.stopSession(session, true);
        session = brokerServerRule.startSession();

        RealmModel realm = getRealm();

        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

        assertEquals(1, federatedIdentities.size());

        FederatedIdentityModel federatedIdentityModel = federatedIdentities.iterator().next();

        assertEquals(getProviderId(), federatedIdentityModel.getIdentityProvider());
        assertEquals(federatedUser.getUsername(), federatedIdentityModel.getIdentityProvider() + "." + federatedIdentityModel.getUserName());

        driver.navigate().to("http://localhost:8081/test-app/logout");
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));
        return federatedUser;
    }

    /**
     * Test for KEYCLOAK-1053 - verify email action is not performed if email is not provided, login is normal, but action stays in set to be performed later
     */
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailNotProvided_emailVerifyEnabled() {
        getRealm().setVerifyEmail(true);
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        try {
            IdentityProviderModel identityProviderModel = getIdentityProviderModel();
            identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_OFF);

            UserModel federatedUser = assertSuccessfulAuthentication(identityProviderModel, "test-user-noemail", null, false);

            assertTrue(federatedUser.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL.name()));

        } finally {
            getRealm().setVerifyEmail(false);
        }
    }

    /**
     * Test for KEYCLOAK-1372 - verify email action is not performed if email is provided but email trust is enabled for the provider
     */
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailProvided_emailVerifyEnabled_emailTrustEnabled() {
        getRealm().setVerifyEmail(true);
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        try {
            identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_OFF);
            identityProviderModel.setTrustEmail(true);

            UserModel federatedUser = assertSuccessfulAuthentication(identityProviderModel, "test-user", "test-user@localhost", false);

            assertFalse(federatedUser.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL.name()));

        } finally {
            identityProviderModel.setTrustEmail(false);
            getRealm().setVerifyEmail(false);
        }
    }

    /**
     * Test for KEYCLOAK-1372 - verify email action is performed if email is provided and email trust is enabled for the provider, but email is changed on First login update profile page
     * 
     * @throws MessagingException
     * @throws IOException
     */
    @Test
    public void testSuccessfulAuthentication_emailTrustEnabled_emailVerifyEnabled_emailUpdatedOnFirstLogin() throws IOException, MessagingException {
        getRealm().setVerifyEmail(true);
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        try {
            identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_ON);
            identityProviderModel.setTrustEmail(true);

            UserModel user = assertSuccessfulAuthenticationWithEmailVerification(identityProviderModel, "test-user", "new@email.com", true);
            Assert.assertEquals("617-666-7777", user.getFirstAttribute("mobile"));
        } finally {
            identityProviderModel.setTrustEmail(false);
            getRealm().setVerifyEmail(false);
        }
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername() {

        getRealm().setRegistrationEmailAsUsername(true);
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        try {
            IdentityProviderModel identityProviderModel = getIdentityProviderModel();
            identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_OFF);

            authenticateWithIdentityProvider(identityProviderModel, "test-user", false);

            // authenticated and redirected to app
            assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

            brokerServerRule.stopSession(session, true);
            session = brokerServerRule.startSession();

            // check correct user is created with email as username and bound to correct federated identity
            RealmModel realm = getRealm();

            UserModel federatedUser = session.users().getUserByUsername("test-user@localhost", realm);

            assertNotNull(federatedUser);

            assertEquals("test-user@localhost", federatedUser.getUsername());

            doAssertFederatedUser(federatedUser, identityProviderModel, "test-user@localhost", false);

            Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

            assertEquals(1, federatedIdentities.size());

            FederatedIdentityModel federatedIdentityModel = federatedIdentities.iterator().next();

            assertEquals(getProviderId(), federatedIdentityModel.getIdentityProvider());

            driver.navigate().to("http://localhost:8081/test-app/logout");
            driver.navigate().to("http://localhost:8081/test-app");

            assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        } finally {
            getRealm().setRegistrationEmailAsUsername(false);
        }
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername_emailNotProvided() {

        getRealm().setRegistrationEmailAsUsername(true);
        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        try {
            IdentityProviderModel identityProviderModel = getIdentityProviderModel();
            identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_OFF);

            authenticateWithIdentityProvider(identityProviderModel, "test-user-noemail", false);

            brokerServerRule.stopSession(session, true);
            session = brokerServerRule.startSession();

            // check correct user is created with username from provider as email is not available
            RealmModel realm = getRealm();
            UserModel federatedUser = getFederatedUser();
            assertNotNull(federatedUser);

            doAssertFederatedUserNoEmail(federatedUser);

            Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

            assertEquals(1, federatedIdentities.size());

            FederatedIdentityModel federatedIdentityModel = federatedIdentities.iterator().next();

            assertEquals(getProviderId(), federatedIdentityModel.getIdentityProvider());
            revokeGrant();

            driver.navigate().to("http://localhost:8081/test-app/logout");
            driver.navigate().to("http://localhost:8081/test-app");

            assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        } finally {
            getRealm().setRegistrationEmailAsUsername(false);
        }
    }

    protected void doAssertFederatedUserNoEmail(UserModel federatedUser) {
        assertEquals("kc-oidc-idp.test-user-noemail", federatedUser.getUsername());
        assertEquals(null, federatedUser.getEmail());
        assertEquals("Test", federatedUser.getFirstName());
        assertEquals("User", federatedUser.getLastName());
    }

    @Test
    public void testDisabled() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setEnabled(false);

        this.driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        try {
            this.driver.findElement(By.className(getProviderId()));
            fail("Provider [" + getProviderId() + "] not disabled.");
        } catch (NoSuchElementException nsee) {

        }
    }

    @Test
    public void testProviderOnLoginPage() {
        // Provider button is available on login page
        this.driver.navigate().to("http://localhost:8081/test-app/");
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));
        loginPage.findSocialButton(getProviderId());
     }

    @Test
    public void testUserAlreadyExistsWhenUpdatingProfile() {
        this.driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        // choose the identity provider
        this.loginPage.clickSocial(getProviderId());

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // log in to identity provider
        this.loginPage.login("test-user", "password");

        doAfterProviderAuthentication();

        this.updateProfilePage.assertCurrent();
        this.updateProfilePage.update("Test", "User", "psilva@redhat.com");

        WebElement element = this.driver.findElement(By.className("kc-feedback-text"));

        assertNotNull(element);

        assertEquals("Email already exists.", element.getText());

        this.updateProfilePage.assertCurrent();
        this.updateProfilePage.update("Test", "User", "test-user@redhat.com");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);
    }

    @Test
    public void testUserAlreadyExistsWhenNotUpdatingProfile() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_OFF);

        this.driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        // choose the identity provider
        this.loginPage.clickSocial(getProviderId());

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // log in to identity provider
        this.loginPage.login("pedroigor", "password");

        doAfterProviderAuthentication();

        WebElement element = this.driver.findElement(By.className("kc-feedback-text"));

        assertNotNull(element);

        assertEquals("User with email already exists. Please login to account management to link the account.", element.getText());
    }

    @Test
    public void testAccountManagementLinkIdentity() {
        // Login as pedroigor to account management
        accountFederatedIdentityPage.realm("realm-with-broker");
        accountFederatedIdentityPage.open();
        assertTrue(driver.getTitle().equals("Log in to realm-with-broker"));
        loginPage.login("pedroigor", "password");
        assertTrue(accountFederatedIdentityPage.isCurrent());

        // Link my "pedroigor" identity with "test-user" from brokered Keycloak
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        accountFederatedIdentityPage.clickAddProvider(identityProviderModel.getAlias());

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));
        this.loginPage.login("test-user", "password");
        doAfterProviderAuthentication();

        // Assert identity linked in account management
        assertTrue(accountFederatedIdentityPage.isCurrent());
        assertTrue(driver.getPageSource().contains("id=\"remove-" + identityProviderModel.getAlias() + "\""));

        // Revoke grant in account mgmt
        revokeGrant();

        // Logout from account management
        accountFederatedIdentityPage.logout();
        assertTrue(driver.getTitle().equals("Log in to realm-with-broker"));

        // Assert I am logged immediately to account management due to previously linked "test-user" identity
        loginPage.clickSocial(identityProviderModel.getAlias());
        doAfterProviderAuthentication();
        assertTrue(accountFederatedIdentityPage.isCurrent());
        assertTrue(driver.getPageSource().contains("id=\"remove-" + identityProviderModel.getAlias() + "\""));

        // Unlink my "test-user"
        accountFederatedIdentityPage.clickRemoveProvider(identityProviderModel.getAlias());
        assertTrue(driver.getPageSource().contains("id=\"add-" + identityProviderModel.getAlias() + "\""));

        // Revoke grant in account mgmt
        revokeGrant();

        // Logout from account management
        System.out.println("*** logout from account management");
        accountFederatedIdentityPage.logout();
        assertTrue(driver.getTitle().equals("Log in to realm-with-broker"));
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        // Try to login. Previous link is not valid anymore, so now it should try to register new user
        this.loginPage.clickSocial(identityProviderModel.getAlias());
        this.loginPage.login("test-user", "password");
        doAfterProviderAuthentication();
        this.updateProfilePage.assertCurrent();
    }

    @Test(expected = NoSuchElementException.class)
    public void testIdentityProviderNotAllowed() {
        this.driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        driver.findElement(By.className("model-oidc-idp"));
    }

    protected void configureClientRetrieveToken(String clientId) {
        RealmModel realm = getRealm();
        RoleModel readTokenRole = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID).getRole(Constants.READ_TOKEN_ROLE);
        ClientModel client = realm.getClientByClientId(clientId);
        if (!client.hasScope(readTokenRole)) client.addScopeMapping(readTokenRole);

        brokerServerRule.stopSession(session, true);
        session = brokerServerRule.startSession();

    }

    protected void configureUserRetrieveToken(String username) {
        RealmModel realm = getRealm();
        UserModel user = session.users().getUserByUsername(username, realm);
        RoleModel readTokenRole = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID).getRole(Constants.READ_TOKEN_ROLE);
        if (user != null && !user.hasRole(readTokenRole)) {
            user.grantRole(readTokenRole);
        }
        brokerServerRule.stopSession(session, true);
        session = brokerServerRule.startSession();

    }

    protected void unconfigureClientRetrieveToken(String clientId) {
        RealmModel realm = getRealm();
        RoleModel readTokenRole = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID).getRole(Constants.READ_TOKEN_ROLE);
        ClientModel client = realm.getClientByClientId(clientId);
        if (client.hasScope(readTokenRole)) client.deleteScopeMapping(readTokenRole);

        brokerServerRule.stopSession(session, true);
        session = brokerServerRule.startSession();

    }

    protected void unconfigureUserRetrieveToken(String username) {
        RealmModel realm = getRealm();
        UserModel user = session.users().getUserByUsername(username, realm);
        RoleModel readTokenRole = realm.getClientByClientId(Constants.BROKER_SERVICE_CLIENT_ID).getRole(Constants.READ_TOKEN_ROLE);
        if (user != null && user.hasRole(readTokenRole)) {
            user.deleteRoleMapping(readTokenRole);
        }
        brokerServerRule.stopSession(session, true);
        session = brokerServerRule.startSession();

    }

    @Test
    public void testTokenStorageAndRetrievalByApplication() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setStoreToken(true);

        authenticateWithIdentityProvider(identityProviderModel, "test-user", true);

        UserModel federatedUser = getFederatedUser();
        RealmModel realm = getRealm();
        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

        assertFalse(federatedIdentities.isEmpty());
        assertEquals(1, federatedIdentities.size());

        FederatedIdentityModel identityModel = federatedIdentities.iterator().next();

        assertNotNull(identityModel.getToken());

        UserSessionStatus userSessionStatus = retrieveSessionStatus();
        String accessToken = userSessionStatus.getAccessTokenString();
        URI tokenEndpointUrl = Urls.identityProviderRetrieveToken(BASE_URI, getProviderId(), realm.getName());
        final String authHeader = "Bearer " + accessToken;
        ClientRequestFilter authFilter = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
            }
        };
        Client client = ClientBuilder.newBuilder().register(authFilter).build();
        WebTarget tokenEndpoint = client.target(tokenEndpointUrl);
        Response response = tokenEndpoint.request().get();
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.readEntity(String.class));
        revokeGrant();


        driver.navigate().to("http://localhost:8081/test-app/logout");
        String currentUrl = this.driver.getCurrentUrl();
        System.out.println("after logout currentUrl: " + currentUrl);
        assertTrue(currentUrl.startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        unconfigureUserRetrieveToken(getProviderId() + ".test-user");
        loginIDP("test-user");
        //authenticateWithIdentityProvider(identityProviderModel, "test-user");
        assertEquals("http://localhost:8081/test-app", driver.getCurrentUrl());

        userSessionStatus = retrieveSessionStatus();
        accessToken = userSessionStatus.getAccessTokenString();
        final String authHeader2 = "Bearer " + accessToken;
        ClientRequestFilter authFilter2 = new ClientRequestFilter() {
            @Override
            public void filter(ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().add(HttpHeaders.AUTHORIZATION, authHeader2);
            }
        };
        client = ClientBuilder.newBuilder().register(authFilter2).build();
        tokenEndpoint = client.target(tokenEndpointUrl);
        response = tokenEndpoint.request().get();

        assertEquals(Status.FORBIDDEN.getStatusCode(), response.getStatus());

        revokeGrant();
        driver.navigate().to("http://localhost:8081/test-app/logout");
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));
    }

    protected abstract void doAssertTokenRetrieval(String pageSource);

    private UserModel assertSuccessfulAuthentication(IdentityProviderModel identityProviderModel, String username, String expectedEmail, boolean isProfileUpdateExpected) {
        authenticateWithIdentityProvider(identityProviderModel, username, isProfileUpdateExpected);

        // authenticated and redirected to app
        assertTrue("Bad current URL " + this.driver.getCurrentUrl() + " and page source: " + this.driver.getPageSource(),
                this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);
        assertNotNull(federatedUser.getCreatedTimestamp());
        // test that timestamp is current with 10s tollerance
        Assert.assertTrue((System.currentTimeMillis() - federatedUser.getCreatedTimestamp()) < 10000);

        doAssertFederatedUser(federatedUser, identityProviderModel, expectedEmail, isProfileUpdateExpected);

        brokerServerRule.stopSession(session, true);
        session = brokerServerRule.startSession();

        RealmModel realm = getRealm();

        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

        assertEquals(1, federatedIdentities.size());

        FederatedIdentityModel federatedIdentityModel = federatedIdentities.iterator().next();

        assertEquals(getProviderId(), federatedIdentityModel.getIdentityProvider());
        assertEquals(federatedUser.getUsername(), federatedIdentityModel.getIdentityProvider() + "." + federatedIdentityModel.getUserName());

        driver.navigate().to("http://localhost:8081/test-app/logout");
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));
        return federatedUser;
    }

    private void authenticateWithIdentityProvider(IdentityProviderModel identityProviderModel, String username, boolean isProfileUpdateExpected) {
        loginIDP(username);


        if (isProfileUpdateExpected) {
            String userEmail = "new@email.com";
            String userFirstName = "New first";
            String userLastName = "New last";

            // update profile
            this.updateProfilePage.assertCurrent();
            this.updateProfilePage.update(userFirstName, userLastName, userEmail);
        }

    }

    private void loginIDP(String username) {
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        // choose the identity provider
        this.loginPage.clickSocial(getProviderId());

        String currentUrl = this.driver.getCurrentUrl();
        assertTrue(currentUrl.startsWith("http://localhost:8082/auth/"));
        System.out.println(this.driver.getCurrentUrl());
        // log in to identity provider
        this.loginPage.login(username, "password");
        doAfterProviderAuthentication();
    }

    protected UserModel getFederatedUser() {
        UserSessionStatus userSessionStatus = retrieveSessionStatus();
        IDToken idToken = userSessionStatus.getIdToken();
        KeycloakSession samlServerSession = brokerServerRule.startSession();
        try {
            RealmModel brokerRealm = samlServerSession.realms().getRealm("realm-with-broker");
            return samlServerSession.users().getUserById(idToken.getSubject(), brokerRealm);
        } finally {
            brokerServerRule.stopSession(samlServerSession, false);
        }
    }

    protected void doAfterProviderAuthentication() {

    }

    protected void revokeGrant() {

    }

    protected abstract String getProviderId();

    protected IdentityProviderModel getIdentityProviderModel() {
        IdentityProviderModel identityProviderModel = getRealm().getIdentityProviderByAlias(getProviderId());

        assertNotNull(identityProviderModel);

        identityProviderModel.setUpdateProfileFirstLoginMode(IdentityProviderRepresentation.UPFLM_ON);
        identityProviderModel.setEnabled(true);

        return identityProviderModel;
    }

    private RealmModel getRealm() {
        return this.session.realms().getRealm("realm-with-broker");
    }

    protected void doAssertFederatedUser(UserModel federatedUser, IdentityProviderModel identityProviderModel, String expectedEmail, boolean isProfileUpdateExpected) {
        if (isProfileUpdateExpected) {
            String userFirstName = "New first";
            String userLastName = "New last";

            assertEquals(expectedEmail, federatedUser.getEmail());
            assertEquals(userFirstName, federatedUser.getFirstName());
            assertEquals(userLastName, federatedUser.getLastName());
        } else {
            assertEquals(expectedEmail, federatedUser.getEmail());
            assertEquals("Test", federatedUser.getFirstName());
            assertEquals("User", federatedUser.getLastName());
        }
    }

    private UserSessionStatus retrieveSessionStatus() {
        UserSessionStatus sessionStatus = null;

        try {
            String pageSource = this.driver.getPageSource();

            sessionStatus = JsonSerialization.readValue(pageSource.getBytes(), UserSessionStatus.class);
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }

        return sessionStatus;
    }

    private void removeTestUsers() {
        RealmModel realm = getRealm();
        List<UserModel> users = this.session.users().getUsers(realm, true);

        for (UserModel user : users) {
            Set<FederatedIdentityModel> identities = this.session.users().getFederatedIdentities(user, realm);

            for (FederatedIdentityModel fedIdentity : identities) {
                this.session.users().removeFederatedIdentity(realm, user, fedIdentity.getIdentityProvider());
            }

            if (!"pedroigor".equals(user.getUsername())) {
                this.session.users().removeUser(realm, user);
            }
        }
    }
    
    private String getVerificationEmailLink(MimeMessage message) throws IOException, MessagingException {
    	Multipart multipart = (Multipart) message.getContent();
    	
        final String textContentType = multipart.getBodyPart(0).getContentType();
        
        assertEquals("text/plain; charset=UTF-8", textContentType);
        
        final String textBody = (String) multipart.getBodyPart(0).getContent();
        final String textVerificationUrl = MailUtil.getLink(textBody);
    	
        final String htmlContentType = multipart.getBodyPart(1).getContentType();
        
        assertEquals("text/html; charset=UTF-8", htmlContentType);
        
        final String htmlBody = (String) multipart.getBodyPart(1).getContent();
        final String htmlVerificationUrl = MailUtil.getLink(htmlBody);
        
        assertEquals(htmlVerificationUrl, textVerificationUrl);

        return htmlVerificationUrl;
    }
}
