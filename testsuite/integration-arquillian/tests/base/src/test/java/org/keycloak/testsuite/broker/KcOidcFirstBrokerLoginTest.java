package org.keycloak.testsuite.broker;

import org.apache.commons.lang3.StringUtils;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.broker.oidc.TestKeycloakOidcIdentityProviderFactory;
import org.keycloak.testsuite.forms.RegisterWithUserProfileTest;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.removeUserByUsername;
import static org.keycloak.testsuite.broker.BrokerTestConstants.IDP_OIDC_ALIAS;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.forms.VerifyProfileTest.ATTRIBUTE_DEPARTMENT;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ADMIN_EDITABLE;
import static org.keycloak.testsuite.forms.VerifyProfileTest.PERMISSIONS_ALL;

import java.util.List;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KcOidcFirstBrokerLoginTest extends AbstractFirstBrokerLoginTest {

    @Page
    protected LoginUpdateProfilePage loginUpdateProfilePage;

    @Page
    protected AppPage appPage;

    @Page
    protected RegisterPage registerPage;

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return new KcOidcBrokerConfiguration() {
            @Override
            public IdentityProviderRepresentation setUpIdentityProvider(IdentityProviderSyncMode syncMode) {
                IdentityProviderRepresentation idp = createIdentityProvider(IDP_OIDC_ALIAS, TestKeycloakOidcIdentityProviderFactory.ID);
                applyDefaultConfiguration(idp.getConfig(), syncMode);
                return idp;
            }
        };
    }

    /**
     * Tests the scenario where a OIDC IDP sends the refresh token only on first login (e.g. Google). In this case, subsequent
     * logins that end up triggering the update of the federated user should not rewrite the token (access token response)
     * without updating it first with the stored refresh token.
     *
     * Github issue reference: #25815
     *
     * @throws Exception if an error occurs while running the test.
     */
    @Test
    public void testRefreshTokenSentOnlyOnFirstLogin() throws Exception {
        IdentityProviderResource idp = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation representation = idp.toRepresentation();
        representation.setStoreToken(true);
        // enable refresh tokens only for the first login (test broker mimics behavior of idps that operate like this).
        representation.getConfig().put(TestKeycloakOidcIdentityProviderFactory.USE_SINGLE_REFRESH_TOKEN, "true");
        idp.update(representation);

        // create a test user in the provider realm.
        createUser(bc.providerRealmName(), "brucewayne", BrokerTestConstants.USER_PASSWORD, "Bruce", "Wayne", "brucewayne@gotham.com");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithIdp(bc.getIDPAlias(), "brucewayne", BrokerTestConstants.USER_PASSWORD);

        // obtain the stored token from the federated identity.
        String storedToken = testingClient.server(bc.consumerRealmName()).fetchString(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            UserModel userModel = session.users().getUserByUsername(realmModel, "brucewayne");
            FederatedIdentityModel fedIdentity = session.users().getFederatedIdentitiesStream(realmModel, userModel).findFirst().orElse(null);
            return fedIdentity != null ? fedIdentity.getToken() : null;
        });
        assertThat(storedToken, not(nullValue()));

        // convert the stored token into an access response for easier retrieval of both access and refresh tokens.
        AccessTokenResponse tokenResponse = JsonSerialization.readValue(storedToken.substring(1, storedToken.length() - 1).replace("\\", ""), AccessTokenResponse.class);
        String firstLoginAccessToken = tokenResponse.getToken();
        assertThat(firstLoginAccessToken, not(nullValue()));
        String firstLoginRefreshToken = tokenResponse.getRefreshToken();
        assertThat(firstLoginRefreshToken, not(nullValue()));

        // logout and then log back in.
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "brucewayne");

        loginPage.open(bc.consumerRealmName());
        logInWithIdp(bc.getIDPAlias(), "brucewayne", BrokerTestConstants.USER_PASSWORD);

        // fetch the stored token - access token should have been updated, but the refresh token should remain the same.
        storedToken = testingClient.server(bc.consumerRealmName()).fetchString(session -> {
            RealmModel realmModel = session.getContext().getRealm();
            UserModel userModel = session.users().getUserByUsername(realmModel, "brucewayne");
            FederatedIdentityModel fedIdentity = session.users().getFederatedIdentitiesStream(realmModel, userModel).findFirst().orElse(null);
            return fedIdentity != null ? fedIdentity.getToken() : null;
        });

        tokenResponse = JsonSerialization.readValue(storedToken.substring(1, storedToken.length() - 1).replace("\\", ""), AccessTokenResponse.class);
        String secondLoginAccessToken = tokenResponse.getToken();
        assertThat(secondLoginAccessToken, not(nullValue()));
        String secondLoginRefreshToken = tokenResponse.getRefreshToken();
        assertThat(secondLoginRefreshToken, not(nullValue()));

        assertThat(firstLoginAccessToken, not(equalTo(secondLoginAccessToken)));
        assertThat(firstLoginRefreshToken, is(equalTo(secondLoginRefreshToken)));
    }

    /**
     * KEYCLOAK-10932
     */
    @Test
    public void loginWithFirstnameLastnamePopulatedFromClaims() {

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        String firstname = "Firstname";
        String lastname = "Lastname";
        String username = "firstandlastname";
        createUser(bc.providerRealmName(), username, BrokerTestConstants.USER_PASSWORD, firstname, lastname, "firstnamelastname@example.org");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithIdp(bc.getIDPAlias(), username, BrokerTestConstants.USER_PASSWORD);

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), username);

        assertEquals(username, userRepresentation.getUsername());
        assertEquals(firstname, userRepresentation.getFirstName());
        assertEquals(lastname, userRepresentation.getLastName());
    }

    /**
     * Tests that duplication is detected and user wants to link federatedIdentity with existing account. He will confirm link by reauthentication
     * with different broker already linked to his account
     */
    @Test
    public void testLinkAccountByReauthenticationWithDifferentBroker() {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(samlBrokerConfig);
            Assert.assertTrue(appPage.isCurrent());
            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
            AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            assertTrue(idpConfirmLinkPage.isCurrent());
            assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
            idpConfirmLinkPage.clickLinkAccount();

            assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

            try {
                this.loginPage.findSocialButton(bc.getIDPAlias());
                org.junit.Assert.fail("Not expected to see social button with " + samlBrokerConfig.getIDPAlias());
            } catch (NoSuchElementException expected) {
            }

            log.debug("Clicking social " + samlBrokerConfig.getIDPAlias());
            loginPage.clickSocial(samlBrokerConfig.getIDPAlias());

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

    @Test
    public void testFilterMultipleBrokerWhenReauthenticating() {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        // create another oidc broker
        KcOidcBrokerConfiguration oidcBrokerConfig = KcOidcBrokerConfiguration.INSTANCE;
        ClientRepresentation oidcClient = oidcBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation oidcBroker = oidcBrokerConfig.setUpIdentityProvider();
        oidcBroker.setAlias("kc-oidc-idp2");
        oidcBroker.setDisplayName("kc-oidc-idp2");

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            adminClient.realm(bc.providerRealmName()).clients().create(oidcClient);
            consumerRealm.identityProviders().create(samlBroker);
            consumerRealm.identityProviders().create(oidcBroker);

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(samlBrokerConfig);
            appPage.assertCurrent();
            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
            AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            assertTrue(idpConfirmLinkPage.isCurrent());
            assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
            idpConfirmLinkPage.clickLinkAccount();

            assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

            // There have to be two idp showed on login page
            // kc-saml-idp and kc-oidc-idp2 must be present but not kc-oidc-idp
            this.loginPage.findSocialButton(samlBroker.getAlias());
            this.loginPage.findSocialButton(oidcBroker.getAlias());

            try {
                this.loginPage.findSocialButton(bc.getIDPAlias());
                org.junit.Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
            } catch (NoSuchElementException expected) {
            }

            log.debug("Clicking social " + samlBrokerConfig.getIDPAlias());
            loginPage.clickSocial(samlBrokerConfig.getIDPAlias());

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 2);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

    /**
     * Tests that nested first broker flows are not allowed. The user wants to link federatedIdentity with existing account. He will try link by reauthentication
     * with different broker not linked to his account. Error message should be shown, and reauthentication should be resumed.
     */
    @Test
    public void testNestedFirstBrokerFlow() {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            createUser(bc.getUserLogin());

            logInWithBroker(bc);

            waitForPage(driver, "account already exists", false);
            assertTrue(idpConfirmLinkPage.isCurrent());
            assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
            idpConfirmLinkPage.clickLinkAccount();

            assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

            try {
                this.loginPage.findSocialButton(bc.getIDPAlias());
                org.junit.Assert.fail("Not expected to see social button with " + samlBrokerConfig.getIDPAlias());
            } catch (NoSuchElementException expected) {
            }

            log.debug("Clicking social " + samlBrokerConfig.getIDPAlias());
            loginPage.clickSocial(samlBrokerConfig.getIDPAlias());
            assertEquals(String.format("The %s user %s is not linked to any known user.", samlBrokerConfig.getIDPAlias(), samlBrokerConfig.getUserLogin()), loginPage.getError());

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 0);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

    /**
     * Refers to in old test suite: OIDCFirstBrokerLoginTest#testMoreIdpAndBackButtonWhenLinkingAccount
     */
    @Test
    public void testLoginWithDifferentBrokerWhenUpdatingProfile() {
        KcSamlBrokerConfiguration samlBrokerConfig = KcSamlBrokerConfiguration.INSTANCE;
        ClientRepresentation samlClient = samlBrokerConfig.createProviderClients().get(0);
        IdentityProviderRepresentation samlBroker = samlBrokerConfig.setUpIdentityProvider();
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        try {
            updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
            adminClient.realm(bc.providerRealmName()).clients().create(samlClient);
            consumerRealm.identityProviders().create(samlBroker);

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(samlBrokerConfig);
            waitForPage(driver, "update account information", false);
            updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");
            AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
            AccountHelper.logout(adminClient.realm(bc.providerRealmName()), bc.getUserLogin());

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(bc);

            // User doesn't want to continue linking account. He rather wants to revert and try the other broker. Click browser "back" 3 times now
            driver.navigate().back();
            driver.navigate().back();

            // User is federated after log in with the original broker
            log.debug("Clicking social " + samlBrokerConfig.getIDPAlias());
            loginPage.clickSocial(samlBrokerConfig.getIDPAlias());

            assertNumFederatedIdentities(consumerRealm.users().search(samlBrokerConfig.getUserLogin()).get(0).getId(), 1);
        } finally {
            updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
            removeUserByUsername(consumerRealm, "consumer");
        }
    }

    @Test
    public void testEditUsername() {
        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        createUser(bc.providerRealmName(), "no-first-name", "password", null,
                "LastName", "no-first-name@localhost.com");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");
        loginPage.login("no-first-name", "password");

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("", "no-first-name@localhost.com", "FirstName", "LastName");
        updateAccountInformationPage.assertCurrent();

        assertEquals("Please specify username.", loginUpdateProfilePage.getInputErrors().getUsernameError());
        
        updateAccountInformationPage.updateAccountInformation("new-username", "no-first-name@localhost.com", "First Name", "Last Name");

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), "new-username");

        Assert.assertEquals("First Name", userRepresentation.getFirstName());
        Assert.assertEquals("Last Name", userRepresentation.getLastName());
        Assert.assertEquals("no-first-name@localhost.com", userRepresentation.getEmail());

    }

    @Test
    public void shouldOfferOidcOptionOnLoginPageAfterUserTriedToLogInButDecidedNotTo() {
        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        final var realmRepresentation = adminClient.realm(bc.consumerRealmName()).toRepresentation();
        realmRepresentation.setRegistrationAllowed(true);
        adminClient.realm(bc.consumerRealmName()).update(realmRepresentation);

        createUser(bc.providerRealmName(), "idp-cancel-test", "password", "IDP", "Cancel", "idp-cancel@localhost.com");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        loginPage.clickRegister();
        registerPage.clickBackToLogin();

        String urlWhenBackFromRegistrationPage = driver.getCurrentUrl();

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
            driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");
        loginPage.login("idp-cancel-test", "password");

        waitForPage(driver, "update account information", false);
        driver.navigate().back();
        driver.navigate().back();
        log.debug("Went back to the login screen.");
        String urlWhenWentBackFromIdpLogin = driver.getCurrentUrl();

        assertEquals(urlWhenBackFromRegistrationPage, urlWhenWentBackFromIdpLogin);

        log.debug("Should not fail here... We're still not logged in, so the IDP should be shown on the login page.");
        assertTrue("We should be on the login page.", driver.getPageSource().contains("Sign in to your account"));
        final var socialButton = this.loginPage.findSocialButton(bc.getIDPAlias());
    }


    @Test
    public void testDisplayName() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\",\"displayName\":\"${firstName}\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", \"displayName\" : \"Department\", " + PERMISSIONS_ALL + ", \"required\":{}}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        //assert field names
        // i18n replaced
        org.junit.Assert.assertEquals("First name", updateAccountInformationPage.getLabelForField("firstName"));
        // attribute name used if no display name set
        org.junit.Assert.assertEquals("lastName", updateAccountInformationPage.getLabelForField("lastName"));
        // direct value in display name
        org.junit.Assert.assertEquals("Department", updateAccountInformationPage.getLabelForField("department"));
    }

    @Test
    public void testAttributeGrouping() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"username\", " + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}, \"group\": \"company\"},"
                + "{\"name\": \"email\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"group\": \"contact\"}"
                + "], \"groups\": ["
                + "{\"name\": \"company\", \"displayDescription\": \"Company field desc\" },"
                + "{\"name\": \"contact\" }"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        //assert fields location in form
        String htmlFormId = "kc-idp-review-profile-form";

        //assert fields and groups location in form, attributes without a group appear first
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(1) > div:nth-child(2) > input#lastName")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(2) > div:nth-child(2) > input#username")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(3) > div:nth-child(2) > input#firstName")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(4) > div:nth-child(1) > label#header-company")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(4) > div:nth-child(2) > label#description-company")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(5) > div:nth-child(2) > input#department")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(6) > div:nth-child(1) > label#header-contact")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(7) > div:nth-child(2) > input#email")
                ).isDisplayed()
        );
    }

    @Test
    public void testAttributeGuiOrder() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"lastName\"," + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\":{}},"
                + "{\"name\": \"username\", " + VerifyProfileTest.PERMISSIONS_ALL + "},"
                + "{\"name\": \"firstName\"," + VerifyProfileTest.PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"email\", " + VerifyProfileTest.PERMISSIONS_ALL + "}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        //assert fields location in form
        String htmlFormId = "kc-idp-review-profile-form";
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(1) > div:nth-child(2) > input#lastName")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(2) > div:nth-child(2) > input#department")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(3) > div:nth-child(2) > input#username")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(4) > div:nth-child(2) > input#firstName")
                ).isDisplayed()
        );
        org.junit.Assert.assertTrue(
                driver.findElement(
                        By.cssSelector("form#"+htmlFormId+" > div:nth-child(5) > div:nth-child(2) > input#email")
                ).isDisplayed()
        );
    }

    @Test
    public void testAttributeInputTypes() {
        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + RegisterWithUserProfileTest.UP_CONFIG_PART_INPUT_TYPES
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        RegisterWithUserProfileTest.assertFieldTypes(driver);
    }

    @Test
    public void testDynamicUserProfileReviewWhenMissing_requiredReadOnlyAttributeDoesnotForceUpdate() {

        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);
    }

    @Test
    public void testDynamicUserProfileReviewWhenMissing_requiredButNotSelectedByScopeAttributeDoesnotForceUpdate() {

        addDepartmentScopeIntoRealm();

        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"department\"]}}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);
    }

    @Test
    public void testDynamicUserProfileReviewWhenMissing_requiredAndSelectedByScopeAttributeForcesUpdate() {

        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        //we use 'profile' scope which is requested by default
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"profile\"]}}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
    }

    @Test
    public void testDynamicUserProfileReview_requiredReadOnlyAttributeNotRenderedAndNotBlockingProcess() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\", " + PERMISSIONS_ADMIN_EDITABLE + ", \"required\":{}}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        assertFalse(updateAccountInformationPage.isDepartmentPresent());

        updateAccountInformationPage.updateAccountInformation( "requiredReadOnlyAttributeNotRenderedAndNotBlockingRegistration", "requiredReadOnlyAttributeNotRenderedAndNotBlockingRegistration@email", "FirstAA", "LastAA");
    }

    @Test
    public void testDynamicUserProfileReview_attributeRequiredAndSelectedByScopeMustBeSet() {
        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        //we use 'profile' scope which is requested by default
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + "},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"profile\"]}}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        //check required validation works
        updateAccountInformationPage.updateAccountInformation( "attributeRequiredAndSelectedByScopeMustBeSet", "attributeRequiredAndSelectedByScopeMustBeSet@email", "FirstAA", "LastAA", "");
        updateAccountInformationPage.assertCurrent();

        updateAccountInformationPage.updateAccountInformation( "attributeRequiredAndSelectedByScopeMustBeSet", "attributeRequiredAndSelectedByScopeMustBeSet@email", "FirstAA", "LastAA", "DepartmentAA");

        UserRepresentation user = VerifyProfileTest.getUserByUsername(testRealm(),"attributeRequiredAndSelectedByScopeMustBeSet");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("DepartmentAA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testDynamicUserProfileReview_attributeNotRequiredAndSelectedByScopeCanBeIgnored() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        //we use 'profile' scope which is requested by default
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\"profile\"]}}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        org.junit.Assert.assertTrue(updateAccountInformationPage.isDepartmentPresent());
        updateAccountInformationPage.updateAccountInformation( "attributeNotRequiredAndSelectedByScopeCanBeIgnored", "attributeNotRequiredAndSelectedByScopeCanBeIgnored@email", "FirstAA", "LastAA");

        UserRepresentation user = VerifyProfileTest.getUserByUsername(testRealm(),"attributeNotRequiredAndSelectedByScopeCanBeIgnored");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertThat(StringUtils.isEmpty(user.firstAttribute(ATTRIBUTE_DEPARTMENT)), is(true));
    }

    @Test
    public void testDynamicUserProfileReview_attributeNotRequiredAndSelectedByScopeCanBeSet() {

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        //we use 'profile' scope which is requested by default
        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"selector\":{\"scopes\":[\"profile\"]}}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        org.junit.Assert.assertTrue(updateAccountInformationPage.isDepartmentPresent());
        updateAccountInformationPage.updateAccountInformation( "attributeNotRequiredAndSelectedByScopeCanBeSet", "attributeNotRequiredAndSelectedByScopeCanBeSet@email", "FirstAA", "LastAA","Department AA");

        UserRepresentation user = VerifyProfileTest.getUserByUsername(testRealm(),"attributeNotRequiredAndSelectedByScopeCanBeSet");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals("Department AA", user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testDynamicUserProfileReview_attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingProcess() {

        addDepartmentScopeIntoRealm();

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        setUserProfileConfiguration("{\"attributes\": ["
                + "{\"name\": \"firstName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"lastName\"," + PERMISSIONS_ALL + ", \"required\": {}},"
                + "{\"name\": \"department\"," + PERMISSIONS_ALL + ", \"required\":{}, \"selector\":{\"scopes\":[\"department\"]}}"
                + "]}");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();

        assertFalse(updateAccountInformationPage.isDepartmentPresent());
        updateAccountInformationPage.updateAccountInformation( "attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingRegistration", "attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingRegistration@email", "FirstAA", "LastAA");

        UserRepresentation user = VerifyProfileTest.getUserByUsername(testRealm(),"attributeRequiredButNotSelectedByScopeIsNotRenderedAndNotBlockingRegistration");
        assertEquals("FirstAA", user.getFirstName());
        assertEquals("LastAA", user.getLastName());
        assertEquals(null, user.firstAttribute(ATTRIBUTE_DEPARTMENT));
    }

    @Test
    public void testFederatedIdentityCaseSensitiveOriginalUsername() {
        String expectedBrokeredUserName = "camelCase";
        IdentityProviderResource idp = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation representation = idp.toRepresentation();
        representation.getConfig().put(TestKeycloakOidcIdentityProviderFactory.PREFERRED_USERNAME, expectedBrokeredUserName);
        representation.getConfig().put(IdentityProviderModel.CASE_SENSITIVE_ORIGINAL_USERNAME, Boolean.TRUE.toString());
        idp.update(representation);
        createUser(bc.providerRealmName(), expectedBrokeredUserName, BrokerTestConstants.USER_PASSWORD, "f", "l", "fl@example.org");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        // the username is stored as lower-case in the provider realm local database
        logInWithIdp(bc.getIDPAlias(), expectedBrokeredUserName.toLowerCase(), BrokerTestConstants.USER_PASSWORD);

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(realm, expectedBrokeredUserName.toLowerCase());
        // the username is in lower case in the local database
        assertEquals(userRepresentation.getUsername(), expectedBrokeredUserName.toLowerCase());

        // the original username is preserved
        List<FederatedIdentityRepresentation> federatedIdentities = realm.users().get(userRepresentation.getId()).getFederatedIdentity();
        assertFalse(federatedIdentities.isEmpty());
        FederatedIdentityRepresentation federatedIdentity = federatedIdentities.get(0);
        assertEquals(expectedBrokeredUserName, federatedIdentity.getUserName());
    }

    @Test
    public void testFederatedIdentityCaseInsensitiveOriginalUsername() {
        String expectedBrokeredUserName = "camelCase";
        IdentityProviderResource idp = realmsResouce().realm(bc.consumerRealmName()).identityProviders().get(bc.getIDPAlias());
        IdentityProviderRepresentation representation = idp.toRepresentation();
        representation.getConfig().put(TestKeycloakOidcIdentityProviderFactory.PREFERRED_USERNAME, expectedBrokeredUserName);
        idp.update(representation);
        createUser(bc.providerRealmName(), expectedBrokeredUserName, BrokerTestConstants.USER_PASSWORD, "f", "l", "fl@example.org");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        // the username is stored as lower-case in the provider realm local database
        logInWithIdp(bc.getIDPAlias(), expectedBrokeredUserName.toLowerCase(), BrokerTestConstants.USER_PASSWORD);

        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(realm, expectedBrokeredUserName.toLowerCase());
        // the username is in lower case in the local database
        assertEquals(userRepresentation.getUsername(), expectedBrokeredUserName.toLowerCase());

        // the original username is preserved
        List<FederatedIdentityRepresentation> federatedIdentities = realm.users().get(userRepresentation.getId()).getFederatedIdentity();
        assertFalse(federatedIdentities.isEmpty());
        FederatedIdentityRepresentation federatedIdentity = federatedIdentities.get(0);
        assertEquals(expectedBrokeredUserName.toLowerCase(), federatedIdentity.getUserName());
    }

    public void addDepartmentScopeIntoRealm() {
        testRealm().clientScopes().create(ClientScopeBuilder.create().name("department").protocol("openid-connect").build());
    }

    protected void setUserProfileConfiguration(String configuration) {
        VerifyProfileTest.setUserProfileConfiguration(testRealm(), configuration);
    }

    private RealmResource testRealm() {
        return adminClient.realm(bc.consumerRealmName());
    }
}
