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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.models.ApplicationModel;
import org.keycloak.models.ClientIdentityProviderMappingModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.IDToken;
import org.keycloak.services.resources.flows.Urls;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.broker.util.UserSessionStatusServlet.UserSessionStatus;
import org.keycloak.testsuite.pages.AccountFederatedIdentityPage;
import org.keycloak.testsuite.pages.AccountPasswordPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.keycloak.testsuite.rule.WebRule.HtmlUnitDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.selenium.SeleneseTestBase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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
    private WebDriver driver;

    @WebResource
    private LoginPage loginPage;

    @WebResource
    private LoginUpdateProfilePage updateProfilePage;

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
        brokerServerRule.stopSession(this.session, true);
    }

    @Test
    public void testSuccessfulAuthentication() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        assertSuccessfulAuthentication(identityProviderModel, "test-user", "new@email.com");
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        identityProviderModel.setUpdateProfileFirstLogin(false);

        assertSuccessfulAuthentication(identityProviderModel, "test-user", "test-user@localhost");
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
            identityProviderModel.setUpdateProfileFirstLogin(false);

            UserModel federatedUser = assertSuccessfulAuthentication(identityProviderModel, "test-user-noemail", null);

            federatedUser.getRequiredActions().contains(RequiredAction.VERIFY_EMAIL);

        } finally {
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
            identityProviderModel.setUpdateProfileFirstLogin(false);

            authenticateWithIdentityProvider(identityProviderModel, "test-user");

            // authenticated and redirected to app
            assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

            // check correct user is created with email as username and bound to correct federated identity
            RealmModel realm = getRealm();

            UserModel federatedUser = session.users().getUserByUsername("test-user@localhost", realm);

            assertNotNull(federatedUser);

            assertEquals("test-user@localhost", federatedUser.getUsername());

            doAssertFederatedUser(federatedUser, identityProviderModel, "test-user@localhost");

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
            identityProviderModel.setUpdateProfileFirstLogin(false);

            authenticateWithIdentityProvider(identityProviderModel, "test-user-noemail");

            // check correct user is created with username from provider as email is not available
            RealmModel realm = getRealm();
            UserModel federatedUser = getFederatedUser();
            assertNotNull(federatedUser);

            doAssertFederatedUserNoEmail(federatedUser);

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

    protected void doAssertFederatedUserNoEmail(UserModel federatedUser) {
        assertEquals("test-user-noemail", federatedUser.getUsername());
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
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        RealmModel realm = getRealm();
        ApplicationModel applicationModel = realm.getApplicationByName("test-app");

        // This client doesn't have any specific identity providers settings
        ClientModel client2 = realm.findClient("test-app");
        assertEquals(0, client2.getIdentityProviders().size());

        // Provider button is available on login page
        this.driver.navigate().to("http://localhost:8081/test-app/");
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));
        loginPage.findSocialButton(getProviderId());

        // Add identityProvider to client model
        List<ClientIdentityProviderMappingModel> appIdentityProviders = new ArrayList<ClientIdentityProviderMappingModel>();
        ClientIdentityProviderMappingModel mapping = new ClientIdentityProviderMappingModel();
        mapping.setIdentityProvider(getProviderId());
        mapping.setRetrieveToken(true);
        appIdentityProviders.add(mapping);
        applicationModel.updateIdentityProviders(appIdentityProviders);

        // Provider button still available on login page
        this.driver.navigate().to("http://localhost:8081/test-app/");
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

        identityProviderModel.setUpdateProfileFirstLogin(false);

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

        // Logout from account management
        accountFederatedIdentityPage.logout();
        assertTrue(driver.getTitle().equals("Log in to realm-with-broker"));

        // Try to login. Previous link is not valid anymore, so now it should try to register new user
        this.loginPage.clickSocial(identityProviderModel.getAlias());
        doAfterProviderAuthentication();
        this.updateProfilePage.assertCurrent();
    }

    @Test(expected = NoSuchElementException.class)
    public void testIdentityProviderNotAllowed() {
        this.driver.navigate().to("http://localhost:8081/test-app/");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        driver.findElement(By.className("model-oidc-idp"));
    }

    @Test
    public void testTokenStorageAndRetrievalByApplication() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setStoreToken(true);

        authenticateWithIdentityProvider(identityProviderModel, "test-user");

        UserModel federatedUser = getFederatedUser();
        RealmModel realm = getRealm();
        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

        assertFalse(federatedIdentities.isEmpty());
        assertEquals(1, federatedIdentities.size());

        FederatedIdentityModel identityModel = federatedIdentities.iterator().next();

        assertNotNull(identityModel.getToken());

        configureRetrieveToken(realm.findClient("test-app"), getProviderId(), false);

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

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatus());

        configureRetrieveToken(getRealm().findClient("test-app"), getProviderId(), true);

        client = ClientBuilder.newBuilder().register(authFilter).build();
        tokenEndpoint = client.target(tokenEndpointUrl);
        response = tokenEndpoint.request().get();

        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        assertNotNull(response.readEntity(String.class));

        driver.navigate().to("http://localhost:8081/test-app/logout");
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));
    }

    @Test
    public void testTokenStorageAndRetrievalByOAuthClient() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setStoreToken(true);
        identityProviderModel.setUpdateProfileFirstLogin(false);

        driver.navigate().to("http://localhost:8081/test-app");

        // choose the identity provider
        this.loginPage.clickSocial(getProviderId());

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));

        // log in to identity provider
        this.loginPage.login("test-user", "password");

        doAfterProviderAuthentication();

        changePasswordPage.realm("realm-with-broker");
        changePasswordPage.open();
        changePasswordPage.changePassword("password", "password");

        driver.navigate().to("http://localhost:8081/test-app/logout");

        oauth.realm("realm-with-broker");
        oauth.redirectUri("http://localhost:8081/third-party");
        oauth.clientId("third-party");
        oauth.doLoginGrant("test-user@localhost", "password");

        grantPage.assertCurrent();
        grantPage.accept();

        assertTrue(oauth.getCurrentQuery().containsKey(OAuth2Constants.CODE));

        ClientModel clientModel = getRealm().findClient("third-party");
        assertEquals(0, clientModel.getIdentityProviders().size());

        configureRetrieveToken(clientModel, getProviderId(), true);

        AccessTokenResponse accessToken = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "password");
        URI tokenEndpointUrl = Urls.identityProviderRetrieveToken(BASE_URI, getProviderId(), getRealm().getName());
        String authHeader = "Bearer " + accessToken.getAccessToken();
        HtmlUnitDriver htmlUnitDriver = (WebRule.HtmlUnitDriver) this.driver;

        htmlUnitDriver.getWebClient().addRequestHeader(HttpHeaders.AUTHORIZATION, authHeader);

        htmlUnitDriver.navigate().to(tokenEndpointUrl.toString());

        grantPage.assertCurrent();
        grantPage.accept();

        assertNotNull(driver.getPageSource());

        doAssertTokenRetrieval(driver.getPageSource());
    }

    private void configureRetrieveToken(ClientModel clientModel, String providerId, boolean retrieveToken) {
        List<ClientIdentityProviderMappingModel> providerMappingModels = clientModel.getIdentityProviders();
        ClientIdentityProviderMappingModel providerMappingModel = null;

        // Check if provider is already linked with this client
        for (ClientIdentityProviderMappingModel current : providerMappingModels) {
            if (current.getIdentityProvider().equals(providerId)) {
                providerMappingModel = current;
                break;
            }
        }

        // Link provider with client if not linked yet
        if (providerMappingModel == null) {
            providerMappingModel = new ClientIdentityProviderMappingModel();
            providerMappingModel.setIdentityProvider(providerId);
            providerMappingModels.add(providerMappingModel);
        }

        providerMappingModel.setRetrieveToken(retrieveToken);

        clientModel.updateIdentityProviders(providerMappingModels);

        brokerServerRule.stopSession(session, true);
        session = brokerServerRule.startSession();
    }

    protected abstract void doAssertTokenRetrieval(String pageSource);

    private UserModel assertSuccessfulAuthentication(IdentityProviderModel identityProviderModel, String username, String expectedEmail) {
        authenticateWithIdentityProvider(identityProviderModel, username);

        // authenticated and redirected to app
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);

        doAssertFederatedUser(federatedUser, identityProviderModel, expectedEmail);

        RealmModel realm = getRealm();

        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

        assertEquals(1, federatedIdentities.size());

        FederatedIdentityModel federatedIdentityModel = federatedIdentities.iterator().next();

        assertEquals(getProviderId(), federatedIdentityModel.getIdentityProvider());
        assertEquals(federatedUser.getUsername(), federatedIdentityModel.getUserName());

        driver.navigate().to("http://localhost:8081/test-app/logout");
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));
        return federatedUser;
    }

    private void authenticateWithIdentityProvider(IdentityProviderModel identityProviderModel, String username) {
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));

        // choose the identity provider
        this.loginPage.clickSocial(getProviderId());

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));
        System.out.println(this.driver.getCurrentUrl());
        // log in to identity provider
        this.loginPage.login(username, "password");

        doAfterProviderAuthentication();

        if (identityProviderModel.isUpdateProfileFirstLogin()) {
            String userEmail = "new@email.com";
            String userFirstName = "New first";
            String userLastName = "New last";

            // update profile
            this.updateProfilePage.assertCurrent();
            this.updateProfilePage.update(userFirstName, userLastName, userEmail);
        }
    }

    protected UserModel getFederatedUser() {
        UserSessionStatus userSessionStatus = retrieveSessionStatus();
        IDToken idToken = userSessionStatus.getIdToken();
        KeycloakSession samlServerSession = brokerServerRule.startSession();
        RealmModel brokerRealm = samlServerSession.realms().getRealm("realm-with-broker");

        return samlServerSession.users().getUserById(idToken.getSubject(), brokerRealm);
    }

    protected void doAfterProviderAuthentication() {

    }

    protected abstract String getProviderId();

    protected IdentityProviderModel getIdentityProviderModel() {
        IdentityProviderModel identityProviderModel = getRealm().getIdentityProviderByAlias(getProviderId());

        assertNotNull(identityProviderModel);

        identityProviderModel.setUpdateProfileFirstLogin(true);
        identityProviderModel.setEnabled(true);

        return identityProviderModel;
    }

    private RealmModel getRealm() {
        return this.session.realms().getRealm("realm-with-broker");
    }

    protected void doAssertFederatedUser(UserModel federatedUser, IdentityProviderModel identityProviderModel, String expectedEmail) {
        if (identityProviderModel.isUpdateProfileFirstLogin()) {
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
            ObjectMapper objectMapper = new ObjectMapper();
            String pageSource = this.driver.getPageSource();

            sessionStatus = objectMapper.readValue(pageSource.getBytes(), UserSessionStatus.class);
        } catch (IOException ignore) {
            ignore.printStackTrace();
        }

        return sessionStatus;
    }

    private void removeTestUsers() {
        RealmModel realm = getRealm();
        List<UserModel> users = this.session.users().getUsers(realm);

        for (UserModel user : users) {
            Set<FederatedIdentityModel> identities = this.session.users().getFederatedIdentities(user, realm);

            for (FederatedIdentityModel fedIdentity : identities) {
                this.session.users().removeFederatedIdentity(realm, user, fedIdentity.getIdentityProvider());
            }

            if (!user.getUsername().equals("pedroigor")) {
                this.session.users().removeUser(realm, user);
            }
        }
    }
}
