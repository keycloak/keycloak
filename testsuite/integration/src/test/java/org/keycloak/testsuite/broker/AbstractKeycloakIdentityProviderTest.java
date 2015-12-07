package org.keycloak.testsuite.broker;

import java.io.IOException;
import java.net.URI;
import java.util.Set;

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

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.testsuite.MailUtil;
import org.keycloak.testsuite.broker.util.UserSessionStatusServlet;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author pedroigor
 */
public abstract class AbstractKeycloakIdentityProviderTest extends AbstractIdentityProviderTest {

    @Test
    public void testSuccessfulAuthentication() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_ON);

        UserModel user = assertSuccessfulAuthentication(identityProviderModel, "test-user", "new@email.com", true);
        Assert.assertEquals("617-666-7777", user.getFirstAttribute("mobile"));
    }

    @Test
    public void testSuccessfulAuthenticationUpdateProfileOnMissing_nothingMissing() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_MISSING);

        assertSuccessfulAuthentication(identityProviderModel, "test-user", "test-user@localhost", false);
    }

    @Test
    public void testSuccessfulAuthenticationUpdateProfileOnMissing_missingEmail() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_MISSING);

        assertSuccessfulAuthentication(identityProviderModel, "test-user-noemail", "new@email.com", true);
    }

    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile() {
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_OFF);

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
        RealmModel realm = getRealm();
        realm.setVerifyEmail(true);
        setUpdateProfileFirstLogin(realm, IdentityProviderRepresentation.UPFLM_OFF);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        try {
            identityProviderModel.setTrustEmail(false);

            UserModel federatedUser = assertSuccessfulAuthenticationWithEmailVerification(identityProviderModel, "test-user", "test-user@localhost", false);

            // email is verified now
            assertFalse(federatedUser.getRequiredActions().contains(UserModel.RequiredAction.VERIFY_EMAIL.name()));

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
        RealmModel realm = getRealm();
        realm.setVerifyEmail(true);
        setUpdateProfileFirstLogin(realm, IdentityProviderRepresentation.UPFLM_OFF);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        try {
            IdentityProviderModel identityProviderModel = getIdentityProviderModel();

            UserModel federatedUser = assertSuccessfulAuthentication(identityProviderModel, "test-user-noemail", null, false);

            assertTrue(federatedUser.getRequiredActions().contains(UserModel.RequiredAction.VERIFY_EMAIL.name()));

        } finally {
            getRealm().setVerifyEmail(false);
        }
    }

    /**
     * Test for KEYCLOAK-1372 - verify email action is not performed if email is provided but email trust is enabled for the provider
     */
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailProvided_emailVerifyEnabled_emailTrustEnabled() {
        RealmModel realmWithBroker = getRealm();
        realmWithBroker.setVerifyEmail(true);
        setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_OFF);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        try {
            identityProviderModel.setTrustEmail(true);

            UserModel federatedUser = assertSuccessfulAuthentication(identityProviderModel, "test-user", "test-user@localhost", false);

            assertFalse(federatedUser.getRequiredActions().contains(UserModel.RequiredAction.VERIFY_EMAIL.name()));

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
        RealmModel realm = getRealm();
        realm.setVerifyEmail(true);
        setUpdateProfileFirstLogin(realm, IdentityProviderRepresentation.UPFLM_ON);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        try {
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
        RealmModel realm = getRealm();
        realm.setRegistrationEmailAsUsername(true);
        setUpdateProfileFirstLogin(realm, IdentityProviderRepresentation.UPFLM_OFF);

        brokerServerRule.stopSession(this.session, true);
        this.session = brokerServerRule.startSession();

        try {
            IdentityProviderModel identityProviderModel = getIdentityProviderModel();

            authenticateWithIdentityProvider(identityProviderModel, "test-user", false);

            // authenticated and redirected to app
            assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));

            brokerServerRule.stopSession(session, true);
            session = brokerServerRule.startSession();

            // check correct user is created with email as username and bound to correct federated identity
            realm = getRealm();

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
    public void testAccountManagementLinkIdentity() {
        // Login as pedroigor to account management
        accountFederatedIdentityPage.realm("realm-with-broker");
        accountFederatedIdentityPage.open();
        assertTrue(driver.getTitle().equals("Log in to realm-with-broker"));
        loginPage.login("pedroigor", "password");
        assertTrue(accountFederatedIdentityPage.isCurrent());

        // Link my "pedroigor" identity with "test-user" from brokered Keycloak
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_ON);
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


    // KEYCLOAK-1822
    @Test
    public void testAccountManagementLinkedIdentityAlreadyExists() {
        // Login as "test-user" through broker
        IdentityProviderModel identityProvider = getIdentityProviderModel();
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_OFF);

        assertSuccessfulAuthentication(identityProvider, "test-user", "test-user@localhost", false);

        // Login as pedroigor to account management
        accountFederatedIdentityPage.realm("realm-with-broker");
        accountFederatedIdentityPage.open();
        assertTrue(driver.getTitle().equals("Log in to realm-with-broker"));
        loginPage.login("pedroigor", "password");
        assertTrue(accountFederatedIdentityPage.isCurrent());

        // Try to link my "pedroigor" identity with "test-user" from brokered Keycloak.
        accountFederatedIdentityPage.clickAddProvider(identityProvider.getAlias());

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));
        this.loginPage.login("test-user", "password");
        doAfterProviderAuthentication();

        // Error is displayed in account management because federated identity"test-user" already linked to local account "test-user"
        assertTrue(accountFederatedIdentityPage.isCurrent());
        assertEquals("Federated identity returned by " + getProviderId() + " is already linked to another user.", accountFederatedIdentityPage.getError());
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
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_ON);
        IdentityProviderModel identityProviderModel = getIdentityProviderModel();

        identityProviderModel.setStoreToken(true);

        authenticateWithIdentityProvider(identityProviderModel, "test-user", true);

        brokerServerRule.stopSession(session, true);
        session = brokerServerRule.startSession();

        UserModel federatedUser = getFederatedUser();
        RealmModel realm = getRealm();
        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realm);

        assertFalse(federatedIdentities.isEmpty());
        assertEquals(1, federatedIdentities.size());

        FederatedIdentityModel identityModel = federatedIdentities.iterator().next();

        assertNotNull(identityModel.getToken());

        UserSessionStatusServlet.UserSessionStatus userSessionStatus = retrieveSessionStatus();
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
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
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

        assertEquals(Response.Status.FORBIDDEN.getStatusCode(), response.getStatus());

        revokeGrant();
        driver.navigate().to("http://localhost:8081/test-app/logout");
        driver.navigate().to("http://localhost:8081/test-app");

        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/auth/realms/realm-with-broker/protocol/openid-connect/auth"));
    }

    protected abstract void doAssertTokenRetrieval(String pageSource);

}
