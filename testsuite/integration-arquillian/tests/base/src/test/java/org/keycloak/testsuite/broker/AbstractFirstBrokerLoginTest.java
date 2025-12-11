package org.keycloak.testsuite.broker;

import java.util.List;

import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.broker.IdpReviewProfileAuthenticatorFactory;
import org.keycloak.broker.provider.HardcodedUserSessionAttributeMapper;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.ComponentRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.federation.UserMapStorageFactory;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FederatedIdentityBuilder;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.userprofile.UserProfileContext;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.PageFactory;

import static org.keycloak.storage.UserStorageProviderModel.IMPORT_ENABLED;
import static org.keycloak.testsuite.admin.ApiUtil.removeUserByUsername;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.assertHardCodedSessionNote;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.configureAutoLinkFlow;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.configureConfirmOverrideLinkFlow;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.grantReadTokenRole;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test of various scenarios related to first broker login.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractFirstBrokerLoginTest extends AbstractInitializedBaseBrokerTest {

    @Drone
    @SecondBrowser
    protected WebDriver driver2;

    @Rule
    public AssertEvents events = new AssertEvents(this);


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testErrorPageWhenDuplicationNotAllowed_updateProfileOn
     */
    @Test
    public void testErrorExistingUserWithUpdateProfile() {
        createUser("consumer");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("consumer", "consumer-user@redhat.com", "FirstName", "LastName");

        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
        assertEquals("User with username consumer already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testLinkAccountByReauthenticationWithPassword
     */
    @Test
    public void testLinkAccountByReauthenticationWithPassword() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        String existingUser = createUser("consumer");

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
            Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
        } catch (NoSuchElementException expected) {
        }

        try {
            this.loginPage.clickRegister();
            Assert.fail("Not expected to see register link");
        } catch (NoSuchElementException expected) {
        }

        loginPage.login("password");
        Assert.assertTrue(appPage.isCurrent());

        assertNumFederatedIdentities(existingUser, 1);
    }

    /**
     * KEYCLOAK-12870
     */
    @Test
    public void testLinkAccountByReauthenticationWithUsernameAndPassword() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        String existingUser = createUser("consumer");
        String anotherUser = createUser("foobar", "foo@bar.baz");

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
            Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
        } catch (NoSuchElementException expected) {
        }

        try {
            this.loginPage.clickRegister();
            Assert.fail("Not expected to see register link");
        } catch (NoSuchElementException expected) {
        }

        loginPage.login("foobar", "password");
        Assert.assertTrue(appPage.isCurrent());

        assertNumFederatedIdentities(existingUser, 0);
        assertNumFederatedIdentities(anotherUser, 1);
    }

    /**
     * KEYCLOAK-12870
     */
    @Test
    public void testLinkAccountByReauthenticationNoExistingUser() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        updateExecutions(AbstractBrokerTest::disableExistingUser);
        String existingUser = createUser("consumer");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

        try {
            this.loginPage.findSocialButton(bc.getIDPAlias());
            Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
        } catch (NoSuchElementException expected) {
        }

        try {
            this.loginPage.clickRegister();
            Assert.fail("Not expected to see register link");
        } catch (NoSuchElementException expected) {
        }

        loginPage.login("consumer", "password");
        Assert.assertTrue(appPage.isCurrent());

        assertNumFederatedIdentities(existingUser, 1);
    }

    @Test
    public void testLinkAccountByReauthenticationWithWrongPassword() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        updateExecutions(AbstractBrokerTest::disableExistingUser);

        Runnable revertRegistrationAllowedModification = toggleRegistrationAllowed(bc.consumerRealmName(), true);
        try {
            String existingUser = createUser("consumer");

            oauth.clientId("broker-app");
            loginPage.open(bc.consumerRealmName());

            logInWithBroker(bc);

            assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

            try {
                this.loginPage.findSocialButton(bc.getIDPAlias());
                Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
            } catch (NoSuchElementException expected) {
            }

            try {
                this.loginPage.clickRegister();
                Assert.fail("Not expected to see register link");
            } catch (NoSuchElementException expected) {
            }

            loginPage.login("consumer", "wrongpassword");
            Assert.assertTrue(loginPage.isCurrent(bc.consumerRealmName()));

            assertNumFederatedIdentities(existingUser, 0);

            assertEquals("Invalid username or password.", loginPage.getInputError());

            try {
                this.loginPage.clickRegister();
                Assert.fail("Not expected to see register link");
            } catch (NoSuchElementException expected) {
            }
        } finally {
            revertRegistrationAllowedModification.run();
        }
    }

    /**
     * KEYCLOAK-12870
     */
    @Test
    public void testLinkAccountByReauthenticationResetPassword() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        String existingUser = createUser("consumer");

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
            Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
        } catch (NoSuchElementException expected) {
        }

        try {
            this.loginPage.clickRegister();
            Assert.fail("Not expected to see register link");
        } catch (NoSuchElementException expected) {
        }

        loginPage.resetPassword();
        loginPasswordResetPage.assertCurrent();
        assertEquals("consumer", loginPasswordResetPage.getUsername());
    }

    /**
     * KEYCLOAK-12870
     */
    @Test
    public void testLinkAccountByReauthenticationResetPasswordNoExistingUser() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        updateExecutions(AbstractBrokerTest::disableExistingUser);
        String existingUser = createUser("consumer");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

        try {
            this.loginPage.findSocialButton(bc.getIDPAlias());
            Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
        } catch (NoSuchElementException expected) {
        }

        try {
            this.loginPage.clickRegister();
            Assert.fail("Not expected to see register link");
        } catch (NoSuchElementException expected) {
        }

        loginPage.resetPassword();
        loginPasswordResetPage.assertCurrent();
        assertTrue(loginPasswordResetPage.getUsername().isEmpty());
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testLinkAccountByReauthenticationWithPassword_browserButtons
     */
    @Test
    public void testLinkAccountByLogInAsUserUsingBrowserButtons() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        String userId = createUser("consumer");
        UserResource providerUser = adminClient.realm(bc.providerRealmName()).users().get(this.userId);
        UserRepresentation userResource = providerUser.toRepresentation();

        userResource.setEmail(USER_EMAIL);
        userResource.setFirstName("FirstName");
        userResource.setLastName("LastName");

        providerUser.update(userResource);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");

        // we need to force a login failure in order to be able to use back button to go back to login page at the provider
        loginPage.login("invalid", bc.getUserPassword());
        loginPage.login(bc.getUserLogin(), bc.getUserPassword());

        waitForPage(driver, "account already exists", false);

        // Click browser 'back' and then 'forward' and then continue
        driver.navigate().back();
        loginExpiredPage.assertCurrent();
        driver.navigate().forward(); // here a new execution ID is added to the URL using JS, see below
        idpConfirmLinkPage.assertCurrent();

        // Click browser 'back' on review profile page
        idpConfirmLinkPage.clickReviewProfile();
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        driver.navigate().back();

        loginExpiredPage.assertCurrent();
        loginExpiredPage.clickLoginContinueLink();
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation(bc.getUserEmail(), "FirstName", "LastName");

        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();

        // Login screen shown. Username is prefilled. Registration link and social buttons are not shown
        assertEquals("consumer", loginPage.getUsername());
        assertTrue(loginPage.isUsernameInputEnabled());

        assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), this.loginPage.getInfoMessage());

        try {
            loginPage.findSocialButton(bc.getIDPAlias());
            Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
        } catch (NoSuchElementException expected) {
        }

        // Use correct password now
        loginPage.login("password");
        appPage.assertCurrent();
        assertNumFederatedIdentities(userId, 1);
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testLinkAccountByReauthentication_forgetPassword
     */
    @Test
    public void testLinkAccountByLogInAsUserAfterResettingPassword() throws InterruptedException {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = realm.toRepresentation();

        realmRep.setResetPasswordAllowed(true);

        realm.update(realmRep);

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        String existingUser = createUser("consumer");
        UserResource providerUser = adminClient.realm(bc.providerRealmName()).users().get(userId);
        UserRepresentation userResource = providerUser.toRepresentation();

        userResource.setEmail(USER_EMAIL);
        userResource.setFirstName("FirstName");
        userResource.setLastName("LastName");

        providerUser.update(userResource);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();

        configureSMTPServer();

        this.loginPage.resetPassword();
        this.loginPasswordResetPage.assertCurrent();
        this.loginPasswordResetPage.changePassword();
        assertEquals("You should receive an email shortly with further instructions.", this.loginPage.getSuccessMessage());
        assertEquals(1, MailServer.getReceivedMessages().length);
        MimeMessage message = MailServer.getLastReceivedMessage();
        String linkFromMail = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "credentials", false);

        driver.navigate().to(linkFromMail.trim());

        // Need to update password now
        this.passwordUpdatePage.assertCurrent();
        this.passwordUpdatePage.changePassword("password", "password");

        Assert.assertTrue(appPage.isCurrent());
        assertNumFederatedIdentities(existingUser, 1);
    }

    /**
     * Reset password during first broker login should work without `AbstractIdpAuthenticator.EXISTING_USER_INFO` set.
     *
     * This session note is only set by {@link org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticator}
     * or {@link org.keycloak.authentication.authenticators.broker.IdpDetectExistingBrokerUserAuthenticator}. However,
     * the reset password feature should work without them.
     *
     * For more info see https://github.com/keycloak/keycloak/issues/26323 .
     */
    @Test
    public void testResetPasswordDuringFirstBrokerFlowWithoutExistingUserAuthenticator() throws InterruptedException {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = realm.toRepresentation();

        realmRep.setResetPasswordAllowed(true);

        realm.update(realmRep);

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        updateExecutions(AbstractBrokerTest::disableExistingUser);
        String existingUser = createUser("consumer");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

        try {
            this.loginPage.findSocialButton(bc.getIDPAlias());
            Assert.fail("Not expected to see social button with " + bc.getIDPAlias());
        } catch (NoSuchElementException expected) {
        }

        try {
            this.loginPage.clickRegister();
            Assert.fail("Not expected to see register link");
        } catch (NoSuchElementException expected) {
        }

        configureSMTPServer();

        this.loginPage.resetPassword();
        this.loginPasswordResetPage.assertCurrent();
        this.loginPasswordResetPage.changePassword("consumer");
        assertEquals("You should receive an email shortly with further instructions.", this.loginPage.getSuccessMessage());
        assertEquals(1, MailServer.getReceivedMessages().length);
        MimeMessage message = MailServer.getLastReceivedMessage();
        String linkFromMail = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "credentials", false);

        driver.navigate().to(linkFromMail.trim());

        // Need to update password now
        this.passwordUpdatePage.assertCurrent();
        this.passwordUpdatePage.changePassword("password", "password");

        Assert.assertTrue(appPage.isCurrent());
        assertNumFederatedIdentities(existingUser, 1);
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testLinkAccountByReauthentication_forgetPassword_differentBrowser
     */
    @Test
    public void testLinkAccountByLogInAsUserAfterResettingPasswordUsingDifferentBrowsers() throws InterruptedException {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = realm.toRepresentation();

        realmRep.setResetPasswordAllowed(true);

        realm.update(realmRep);

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        String existingUser = createUser("consumer");
        UserResource providerUser = adminClient.realm(bc.providerRealmName()).users().get(userId);
        UserRepresentation userResource = providerUser.toRepresentation();

        userResource.setEmail(USER_EMAIL);
        userResource.setFirstName("FirstName");
        userResource.setLastName("LastName");

        providerUser.update(userResource);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();

        configureSMTPServer();

        this.loginPage.resetPassword();
        this.loginPasswordResetPage.assertCurrent();
        this.loginPasswordResetPage.changePassword();
        assertEquals("You should receive an email shortly with further instructions.", this.loginPage.getSuccessMessage());
        assertEquals(1, MailServer.getReceivedMessages().length);
        MimeMessage message = MailServer.getLastReceivedMessage();
        String linkFromMail = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "credentials", false);

        driver2.navigate().to(linkFromMail.trim());
        removeSMTPConfiguration(realm);

        // Need to update password now
        LoginPasswordUpdatePage passwordUpdatePage = PageFactory.initElements(driver2, LoginPasswordUpdatePage.class);
        passwordUpdatePage.changePassword("password", "password");

        assertNumFederatedIdentities(existingUser, 0);

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());

        try {
            waitForPage(driver, "account already exists", false);
        } catch (Exception e) {
            // this is a workaround to make this test work for both oidc and saml. when doing oidc the browser is redirected to the login page to finish the linking
            loginPage.login(bc.getUserPassword());
        }

        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        idpConfirmLinkPage.clickLinkAccount();

        loginPage.login("password");
        assertNumFederatedIdentities(existingUser, 1);
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testErrorPageWhenDuplicationNotAllowed_updateProfileOff
     */
    @Test
    public void testUserExistsFirstBrokerLoginFlowUpdateProfileOff() {
        UserResource userResource = adminClient.realm(bc.consumerRealmName()).users().get(createUser("consumer"));
        UserRepresentation consumerUser = userResource.toRepresentation();

        consumerUser.setEmail(bc.getUserEmail());
        userResource.update(consumerUser);

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testErrorPageWhenDuplicationNotAllowed_updateProfileOff
     */
    @Test
    public void testUserExistsFirstBrokerLoginFlowUpdateProfileOn() {
        createUser("consumer");

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("consumer", "consumer-user@redhat.com", "FirstName", "LastName");

        waitForPage(driver, "we are sorry...", false);
        assertEquals("User with username consumer already exists. Please login to account management to link the account.", errorPage.getError());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testRegistrationWithPasswordUpdateRequired
     */
    @Test
    public void testRequiredUpdatedPassword() {
        updateExecutions(AbstractBrokerTest::enableRequirePassword);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        waitForPage(driver, "update password", false);
        updatePasswordPage.updatePasswords("password", "password");

        Assert.assertTrue(appPage.isCurrent());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testFixDuplicationsByReviewProfile
     */
    @Test
    public void testFixDuplicationsByReviewProfile() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        UserResource userResource = realm.users().get(createUser("consumer"));
        UserRepresentation consumerUser = userResource.toRepresentation();

        consumerUser.setEmail(bc.getUserEmail());
        userResource.update(consumerUser);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
        assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
        idpConfirmLinkPage.clickReviewProfile();

        waitForPage(driver, "update account information", false);
        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        updateAccountInformationPage.updateAccountInformation("consumer", "test@localhost.com", "FirstName", "LastName");

        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
        assertEquals("User with username consumer already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
        idpConfirmLinkPage.clickReviewProfile();

        waitForPage(driver, "update account information", false);
        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        updateAccountInformationPage.updateAccountInformation("test", "test@localhost.com", "FirstName", "LastName");

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), "test");

        Assert.assertEquals("FirstName", userRepresentation.getFirstName());
        Assert.assertEquals("LastName", userRepresentation.getLastName());
        Assert.assertEquals("test@localhost.com", userRepresentation.getEmail());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testHardcodedUserSessionNoteIsSetAfterFristBrokerLogin()
     */
    @Test
    public void testHardcodedUserSessionNoteIsSetAfterFirstBrokerLogin() {
        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);

        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        IdentityProviderResource idpResource = realm.identityProviders().get(bc.getIDPAlias());
        IdentityProviderMapperRepresentation hardCodedSessionNoteMapper = new IdentityProviderMapperRepresentation();

        hardCodedSessionNoteMapper.setName("static-session-note");
        hardCodedSessionNoteMapper.setIdentityProviderAlias(bc.getIDPAlias());
        hardCodedSessionNoteMapper.setIdentityProviderMapper(HardcodedUserSessionAttributeMapper.PROVIDER_ID);
        hardCodedSessionNoteMapper.setConfig(ImmutableMap.<String, String>builder()
                .put(IdentityProviderMapperModel.SYNC_MODE, IdentityProviderSyncMode.IMPORT.toString())
                .put(HardcodedUserSessionAttributeMapper.ATTRIBUTE_VALUE, "sessionvalue")
                .put(HardcodedUserSessionAttributeMapper.ATTRIBUTE, "user-session-attr")
                .build());

        Response response = idpResource.addMapper(hardCodedSessionNoteMapper);
        response.close();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        testingClient.server().run(assertHardCodedSessionNote());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testRegistrationWithEmailAsUsername
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testSuccessfulAuthenticationWithoutUpdateProfile_newUser_emailAsUsername()
     */
    @Test
    public void testRequiredRegistrationEmailAsUserName() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = realm.toRepresentation();

        updateExecutions(AbstractBrokerTest::enableUpdateProfileOnFirstLogin);
        realmRep.setRegistrationEmailAsUsername(true);
        realm.update(realmRep);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        try {
            updateAccountInformationPage.updateAccountInformation("test", "test@redhat.com", "FirstName", "LastName");
            Assert.fail("It is not expected to see username field");
        } catch (NoSuchElementException ignore) {
        }

        updateAccountInformationPage.updateAccountInformation("test@redhat.com", "FirstName", "LastName");

        assertEquals(1, realm.users().search("test@redhat.com").size());
    }


    // KEYCLOAK-2957
    @Test
    public void testLinkAccountWithEmailVerified() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        configureSMTPServer();

        //create user on consumer's site who should be linked later
        String linkedUserId = createUser("consumer");

        //test
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("Firstname", "Lastname");

        //link account by email
        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.clickLinkAccount();

        String url = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "Someone wants to link your ", false);

        log.info("navigating to url from email: " + url);
        driver.navigate().to(url);

        //test if user is logged in
        assertTrue(driver.getCurrentUrl().startsWith(getConsumerRoot() + "/auth/realms/master/app/"));

        //test if the user has verified email
        assertTrue(realm.users().get(linkedUserId).toRepresentation().isEmailVerified());
    }

    @Test
    public void testLinkAccountModifyingEmailLinkingByEmailNotAllowed() {
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());

        configureSMTPServer();

        // change provider user email to changed@localhost.com
        UserRepresentation userProvider = ApiUtil.findUserByUsername(providerRealm, bc.getUserLogin());
        userProvider.setEmail("changed@localhost.com");
        providerRealm.users().get(userProvider.getId()).update(userProvider);

        //create user on consumer's site with the correct email
        final String linkedUserId = createUser(bc.getUserLogin());

        //test
        oauth.config().clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        // update the email to the correct one
        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation(USER_EMAIL, "Firstname", "Lastname");

        //link account
        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.clickLinkAccount();

        //it should start the link using username and password as email has been changed
        assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

        loginPage.login("password");
        Assert.assertTrue(appPage.isCurrent());

        assertNumFederatedIdentities(linkedUserId, 1);
    }

    @Test
    public void testLinkAccountReviewDisabled() throws Exception {
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        // disable the idp review step
        AuthenticationExecutionInfoRepresentation idpReviewProfileExec = consumerRealm.flows().getExecutions("first broker login").stream()
                .filter(execution -> IdpReviewProfileAuthenticatorFactory.PROVIDER_ID.equals(execution.getProviderId()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull("IdpReviewProfileAuthenticator execution not found", idpReviewProfileExec);
        idpReviewProfileExec.setRequirement(AuthenticationExecutionModel.Requirement.DISABLED.name());
        consumerRealm.flows().updateExecutions("first broker login", idpReviewProfileExec);

        //create user on consumer's site with the correct email
        final String linkedUserId = createUser(bc.getUserLogin());

        //test
        oauth.config().clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        // no review displayed and button in link not available
        waitForPage(driver, "account already exists", false);
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));
        Assert.assertFalse("Review Profile button is displayed", idpConfirmLinkPage.isReviewProfileDisplayed());
        idpConfirmLinkPage.clickLinkAccount();

        //linking the account using password as email not configured
        assertEquals("Authenticate to link your account with " + bc.getIDPAlias(), loginPage.getInfoMessage());

        loginPage.login("password");
        Assert.assertTrue(appPage.isCurrent());

        assertNumFederatedIdentities(linkedUserId, 1);
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testSuccessfulAuthenticationWithoutUpdateProfile_emailProvided_emailVerifyEnabled
     */
    @Test
    public void testLinkAccountWithUntrustedEmailVerified() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = realm.toRepresentation();

        realmRep.setVerifyEmail(true);

        realm.update(realmRep);

        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();

        idpRep.setTrustEmail(false);

        identityProviderResource.update(idpRep);

        configureSMTPServer();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        verifyEmailPage.assertCurrent();

        String verificationUrl = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "verify your email address", false);

        driver.navigate().to(verificationUrl.trim());
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testSuccessfulAuthenticationWithoutUpdateProfile_emailNotProvided_emailVerifyEnabled
     *
     */
    @Test
    public void testSuccessfulAuthenticationWithoutUpdateProfile_emailNotProvided_emailVerifyEnabled() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = realm.toRepresentation();

        realmRep.setVerifyEmail(true);

        realm.update(realmRep);

        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        createUser(bc.providerRealmName(), "no-email", "password", "FirstName", "LastName", null);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");
        loginPage.login("no-email", "password");

        List<UserRepresentation> users = realm.users().search("no-email");
        assertEquals(1, users.size());

    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testSuccessfulAuthenticationWithoutUpdateProfile_emailProvided_emailVerifyEnabled_emailTrustEnabled
     */
    @Test
    public void testVerifyEmailNotRequiredActionWhenEmailIsTrustedByProvider() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = realm.toRepresentation();

        realmRep.setVerifyEmail(true);

        realm.update(realmRep);

        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();

        idpRep.setTrustEmail(true);

        identityProviderResource.update(idpRep);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        List<UserRepresentation> users = realm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        List<String> requiredActions = users.get(0).getRequiredActions();
        assertEquals(0, requiredActions.size());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest#testSuccessfulAuthentication_emailTrustEnabled_emailVerifyEnabled_emailUpdatedOnFirstLogin
     */
    @Test
    public void testVerifyEmailRequiredActionWhenChangingEmailDuringFirstLogin() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());
        RealmRepresentation realmRep = realm.toRepresentation();

        realmRep.setVerifyEmail(true);

        realm.update(realmRep);

        IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();

        idpRep.setTrustEmail(true);

        identityProviderResource.update(idpRep);

        configureSMTPServer();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("changed@localhost.com", "FirstName", "LastName");

        verifyEmailPage.assertCurrent();

        String verificationUrl = assertEmailAndGetUrl(MailServerConfiguration.FROM, "changed@localhost.com",
                "verify your email address", false);

        driver.navigate().to(verificationUrl.trim());

        List<UserRepresentation> users = realm.users().search(bc.getUserLogin());
        assertEquals(1, users.size());
        List<String> requiredActions = users.get(0).getRequiredActions();
        assertEquals(0, requiredActions.size());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testLinkAccountByEmailVerificationTwice
     */
    @Test
    public void testLinkAccountByEmailVerificationTwice() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        UserResource userResource = realm.users().get(createUser("consumer"));
        UserRepresentation consumerUser = userResource.toRepresentation();

        consumerUser.setEmail(bc.getUserEmail());
        userResource.update(consumerUser);
        configureSMTPServer();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        //link account by email
        waitForPage(driver, "update account information", false);
        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");
        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
        assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
        idpConfirmLinkPage.clickLinkAccount();

        String url = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "Someone wants to link your ", false);
        driver.navigate().to(url);
        //test if user is logged in
        assertTrue(driver.getCurrentUrl().startsWith(getConsumerRoot() + "/auth/realms/master/app/"));
        //test if the user has verified email
        assertTrue(adminClient.realm(bc.consumerRealmName()).users().get(consumerUser.getId()).toRepresentation().isEmailVerified());

        driver.navigate().to(url);
        waitForPage(driver, "your email address has been verified already.", false);
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "consumer");

        driver.navigate().to(url);
        waitForPage(driver, "your email address has been verified already.", false);

        driver2.navigate().to(url);
        waitForPage(driver, "your email address has been verified already.", false);
    }

    @Test
    public void testLinkAccountByEmailVerificationInAnotherBrowser() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        UserResource userResource = realm.users().get(createUser("consumer"));
        UserRepresentation consumerUser = userResource.toRepresentation();

        consumerUser.setEmail(bc.getUserEmail());
        consumerUser.setEmailVerified(true);
        userResource.update(consumerUser);
        configureSMTPServer();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        //link account by email
        waitForPage(driver, "update account information", false);
        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");
        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
        assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
        idpConfirmLinkPage.clickLinkAccount();
        idpLinkEmailPage.assertCurrent();

        String url = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "Someone wants to link your ", false);

        // in the second browser confirm the mail
        driver2.navigate().to(url);
        assertThat(driver2.findElement(By.id("kc-page-title")).getText(), startsWith("Confirm linking the account"));
        assertThat(driver2.findElement(By.className("instruction")).getText(), startsWith("If you link the account, you will also be able to login using account"));
        driver2.findElement(By.linkText("» Click here to proceed")).click();
        assertThat(driver2.findElement(By.className("instruction")).getText(), startsWith("You successfully verified your email."));

        idpLinkEmailPage.continueLink();

        //test if user is logged in
        assertTrue(driver.getCurrentUrl().startsWith(getConsumerRoot() + "/auth/realms/master/app/"));
        // check user is linked
        List<FederatedIdentityRepresentation> identities = userResource.getFederatedIdentity();
        assertEquals(1, identities.size());
        assertEquals(bc.getIDPAlias(), identities.iterator().next().getIdentityProvider());
        assertEquals(bc.getUserLogin(), identities.iterator().next().getUserName());
    }

    @Test
    public void testLinkAccountByEmailVerificationToEmailVerifiedUser() {
        // set up a user with verified email
        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        UserResource userResource = realm.users().get(createUser("consumer"));
        UserRepresentation consumerUser = userResource.toRepresentation();

        consumerUser.setEmail(bc.getUserEmail());
        consumerUser.setEmailVerified(true);
        userResource.update(consumerUser);
        configureSMTPServer();

        // begin login with idp
        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());
        logInWithBroker(bc);

        // update account profile
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        // idp confirm link
        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.assertCurrent();
        assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
        idpConfirmLinkPage.clickLinkAccount();

        String url = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "Someone wants to link your ", false);
        driver.navigate().to(url);

        assertTrue(driver.getCurrentUrl().startsWith(getConsumerRoot() + "/auth/realms/master/app/"));
        assertTrue(adminClient.realm(bc.consumerRealmName()).users().get(consumerUser.getId()).toRepresentation().isEmailVerified());
        assertNumFederatedIdentities(consumerUser.getId(), 1);
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testLinkAccountByEmailVerificationResendEmail
     */
    @Test
    public void testLinkAccountByEmailVerificationResendEmail() {
        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        UserResource userResource = realm.users().get(createUser("consumer"));
        UserRepresentation consumerUser = userResource.toRepresentation();

        consumerUser.setEmail(bc.getUserEmail());
        userResource.update(consumerUser);
        configureSMTPServer();

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        //link account by email
        waitForPage(driver, "update account information", false);
        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");
        waitForPage(driver, "account already exists", false);
        assertTrue(idpConfirmLinkPage.isCurrent());
        assertEquals("User with email user@localhost.com already exists. How do you want to continue?", idpConfirmLinkPage.getMessage());
        idpConfirmLinkPage.clickLinkAccount();


        waitForPage(driver, "link " + bc.getIDPAlias(), false);
        assertEquals("an email with instructions to link " + bc.getIDPAlias() + " account testuser with your consumer account has been sent to you.", idpLinkEmailPage.getMessage().toLowerCase());
        idpLinkEmailPage.resendEmail();

        assertEquals(2, MailServer.getReceivedMessages().length);
    }


    @Test
    public void testVerifyEmailInNewBrowserWithPreserveClient() {
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());

        configureSMTPServer();

        //create user on consumer's site who should be linked later
        String linkedUserId = createUser("consumer");

        driver.navigate().to(getLoginUrl(getConsumerRoot(), bc.consumerRealmName(), "broker-app"));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("Firstname", "Lastname");

        //link account by email
        waitForPage(driver, "account already exists", false);
        idpConfirmLinkPage.clickLinkAccount();

        String url = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "Someone wants to link your ", false);

        log.info("navigating to url from email in second browser: " + url);

        // navigate to url in the second browser
        driver2.navigate().to(url);

        final WebElement proceedLink = driver2.findElement(By.linkText("» Click here to proceed"));
        MatcherAssert.assertThat(proceedLink, Matchers.notNullValue());

        // check if the initial client is preserved
        String link = proceedLink.getAttribute("href");
        MatcherAssert.assertThat(link, Matchers.containsString("client_id=broker-app"));
        proceedLink.click();

        assertThat(driver2.getPageSource(), Matchers.containsString("You successfully verified your email. Please go back to your original browser and continue there with the login."));

        //test if the user has verified email
        assertTrue(consumerRealm.users().get(linkedUserId).toRepresentation().isEmailVerified());
    }

    @Test
    public void testEventsOnUpdateProfileNoEmailChange() {
        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        createUser(bc.providerRealmName(), "no-first-name", "password", null, "LastName", "no-first-name@localhost.com");

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
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), "no-first-name");

        Assert.assertEquals("FirstName", userRepresentation.getFirstName());
        Assert.assertEquals("LastName", userRepresentation.getLastName());
        Assert.assertEquals("no-first-name@localhost.com", userRepresentation.getEmail());

        RealmRepresentation consumerRealmRep = adminClient.realm(bc.consumerRealmName()).toRepresentation();

        events.expectAccount(EventType.IDENTITY_PROVIDER_FIRST_LOGIN).client("broker-app")
                .realm(consumerRealmRep).user((String)null)
                .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, "no-first-name")
                .assertEvent(getFirstConsumerEvent());

        events.expectAccount(EventType.UPDATE_PROFILE).client("broker-app")
                .realm(consumerRealmRep).user((String)null)
                .detail(Details.CONTEXT, UserProfileContext.IDP_REVIEW.name())
                .assertEvent(getFirstConsumerEvent());

        events.expectAccount(EventType.REGISTER).client("broker-app")
                .realm(consumerRealmRep).user(Matchers.any(String.class)).session((String) null)
                .detail(Details.IDENTITY_PROVIDER_USERNAME, "no-first-name")
                .detail(Details.REGISTER_METHOD, "broker")
                .assertEvent(getFirstConsumerEvent());

        events.expectAccount(EventType.LOGIN).client("broker-app")
                .realm(consumerRealmRep).user(Matchers.any(String.class)).session(Matchers.any(String.class))
                .detail(Details.IDENTITY_PROVIDER_USERNAME, "no-first-name")
                .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
                .assertEvent(getFirstConsumerEvent());
    }

    @Test
    public void testEventsOnUpdateProfileWithEmailChange() {
        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        createUser(bc.providerRealmName(), "no-first-name", "password", null, "LastName", "no-first-name@localhost.com");

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
        updateAccountInformationPage.updateAccountInformation("new-email@localhost.com","FirstName", "LastName");

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), "no-first-name");

        Assert.assertEquals("FirstName", userRepresentation.getFirstName());
        Assert.assertEquals("LastName", userRepresentation.getLastName());
        Assert.assertEquals("new-email@localhost.com", userRepresentation.getEmail());
        Assert.assertEquals("no-first-name", userRepresentation.getUsername());

        RealmRepresentation consumerRealmRep = adminClient.realm(bc.consumerRealmName()).toRepresentation();

        events.expectAccount(EventType.IDENTITY_PROVIDER_FIRST_LOGIN).client("broker-app")
                .realm(consumerRealmRep).user((String)null)
                .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, "no-first-name")
                .assertEvent(getFirstConsumerEvent());

        events.expectAccount(EventType.UPDATE_EMAIL).client("broker-app")
                .realm(consumerRealmRep).user((String)null).session((String) null)
            .detail(Details.CONTEXT, UserProfileContext.IDP_REVIEW.name())
            .detail(Details.IDENTITY_PROVIDER_USERNAME, "no-first-name")
            .detail(Details.PREVIOUS_EMAIL, "no-first-name@localhost.com")
            .detail(Details.UPDATED_EMAIL, "new-email@localhost.com")
            .assertEvent(getFirstConsumerEvent());

        events.expectAccount(EventType.UPDATE_PROFILE).client("broker-app")
                .realm(consumerRealmRep).user((String)null)
                .detail(Details.CONTEXT, UserProfileContext.IDP_REVIEW.name())
                .assertEvent(getFirstConsumerEvent());

        events.expectAccount(EventType.REGISTER).client("broker-app")
                .realm(consumerRealmRep).user(Matchers.any(String.class)).session((String) null)
            .detail(Details.IDENTITY_PROVIDER_USERNAME, "no-first-name")
            .detail(Details.REGISTER_METHOD, "broker")
            .assertEvent(events.poll());

        events.expectAccount(EventType.LOGIN).client("broker-app")
                .realm(consumerRealmRep).user(Matchers.any(String.class)).session(Matchers.any(String.class))
            .detail(Details.IDENTITY_PROVIDER_USERNAME, "no-first-name")
            .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
            .assertEvent(events.poll());
    }

    protected EventRepresentation getFirstConsumerEvent() {
        String providerRealmId = adminClient.realm(bc.providerRealmName()).toRepresentation().getId();
        EventRepresentation er = events.poll();
        while(er != null && providerRealmId.equals(er.getRealmId())) {
            er = events.poll();
        }
        return er;
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest.testSuccessfulAuthenticationUpdateProfileOnMissing_missingEmail
     */
    @Test
    public void testUpdateProfileIfMissingInformation() {
        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        createUser(bc.providerRealmName(), "no-first-name", "password", null, "LastName", "no-first-name@localhost.com");

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
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), "no-first-name");

        Assert.assertEquals("FirstName", userRepresentation.getFirstName());
        Assert.assertEquals("LastName", userRepresentation.getLastName());
        Assert.assertEquals("no-first-name@localhost.com", userRepresentation.getEmail());

        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), "no-first-name");
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "no-first-name");
        createUser(bc.providerRealmName(), "no-last-name", "password", "FirstName", null, "no-last-name@localhost.com");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");
        loginPage.login("no-last-name", "password");

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), "no-last-name");

        Assert.assertEquals("FirstName", userRepresentation.getFirstName());
        Assert.assertEquals("LastName", userRepresentation.getLastName());
        Assert.assertEquals("no-last-name@localhost.com", userRepresentation.getEmail());

        AccountHelper.logout(adminClient.realm(bc.providerRealmName()), "no-last-name");
        AccountHelper.logout(adminClient.realm(bc.consumerRealmName()), "no-last-name");

        createUser(bc.providerRealmName(), "no-email", "password", "FirstName", "LastName", null);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");
        loginPage.login("no-email", "password");

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("no-email@localhost.com", "FirstName", "LastName");

        userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), "no-email");

        Assert.assertEquals("FirstName", userRepresentation.getFirstName());
        Assert.assertEquals("LastName", userRepresentation.getLastName());
        Assert.assertEquals("no-email@localhost.com", userRepresentation.getEmail());
    }

    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest.testSuccessfulAuthenticationUpdateProfileOnMissing_nothingMissing
     */
    @Test
    public void testUpdateProfileIfNotMissingInformation() {
        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
        createUser(bc.providerRealmName(), "all-info-set", "password", "FirstName", "LastName", "all-info-set@localhost.com");

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");
        loginPage.login("all-info-set", "password");

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.providerRealmName()), "all-info-set");

        Assert.assertEquals("FirstName", userRepresentation.getFirstName());
        Assert.assertEquals("LastName", userRepresentation.getLastName());
        Assert.assertEquals("all-info-set@localhost.com", userRepresentation.getEmail());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest.testSuccessfulAuthenticationWithoutUpdateProfile
     */
    @Test
    public void testWithoutUpdateProfile() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        UserRepresentation userRepresentation = AccountHelper.getUserRepresentation(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());

        Assert.assertNull(userRepresentation.getFirstName());
        Assert.assertNull(userRepresentation.getLastName());
        Assert.assertEquals(bc.getUserEmail(), userRepresentation.getEmail());
    }


    /**
     * Tests that user can link federated identity with existing brokered
     * account without prompt (KEYCLOAK-7270).
     */
    @Test
    public void testAutoLinkAccountWithBroker() {
        testingClient.server(bc.consumerRealmName()).run(configureAutoLinkFlow(bc.getIDPAlias()));

        oauth.clientId("broker-app");
        loginPage.open(bc.consumerRealmName());

        logInWithBroker(bc);

        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        assertNumFederatedIdentities(realm.users().search(bc.getUserLogin()).get(0).getId(), 1);
    }

    /*
     * test linking the user with an existing read-token role from the federation provider
     * when AddReadTokenRoleOnCreate was enabled for the IdP.
     */
    @Test
    public void testDuplicatedGrantReadTokenRoleWithUserFederationProvider() {
        try {
            // setup federation provider
            ComponentRepresentation component = new ComponentRepresentation();
            component.setName("memory");
            component.setProviderId(UserMapStorageFactory.PROVIDER_ID);
            component.setProviderType(UserStorageProvider.class.getName());
            component.setConfig(new MultivaluedHashMap<>());
            component.getConfig().putSingle("priority", Integer.toString(0));
            component.getConfig().putSingle(IMPORT_ENABLED, Boolean.toString(false));
            adminClient.realm(bc.consumerRealmName()).components().add(component);

            // grant read-token role first
            String username = bc.getUserLogin();
            String createdId = createUser(username);
            testingClient.server(bc.consumerRealmName()).run(grantReadTokenRole(username));

            // enable read token role on create
            IdentityProviderRepresentation idpRep = identityProviderResource.toRepresentation();
            idpRep.setAddReadTokenRoleOnCreate(true);
            identityProviderResource.update(idpRep);

            // auto link when first broker login flow
            testingClient.server(bc.consumerRealmName()).run(configureAutoLinkFlow(bc.getIDPAlias()));
            logInAsUserInIDP();
            assertNumFederatedIdentities(createdId, 1);
        } finally {
            removeUserByUsername(adminClient.realm(bc.consumerRealmName()), bc.getUserLogin());
        }
    }

    @Test
    public void testConfirmOverrideLink() {
        RealmResource consumerRealm = adminClient.realm(bc.consumerRealmName());
        RealmResource providerRealm = adminClient.realm(bc.providerRealmName());

        testingClient.server(bc.consumerRealmName())
                .run(configureConfirmOverrideLinkFlow(bc.getIDPAlias()));

        // create a user with existing federated identity
        String createdUser = createUser(bc.getUserLogin());

        FederatedIdentityRepresentation identity = FederatedIdentityBuilder.create()
                .userId("id")
                .userName("username")
                .identityProvider(bc.getIDPAlias())
                .build();

        try (Response response = consumerRealm.users().get(createdUser)
                .addFederatedIdentity(bc.getIDPAlias(), identity)) {
            assertEquals("status", 204, response.getStatus());
        }

        // login with the same username user but different user id from provider
        logInAsUserInIDP();

        idpConfirmOverrideLinkPage.assertCurrent();
        String expectMessage = "You are trying to link your account testuser with the " + bc.getIDPAlias() + " account testuser. " +
                "But your account is already linked with different " + bc.getIDPAlias() + " account username. " +
                "Can you confirm if you want to replace the existing link with the new account?";
        assertEquals(expectMessage, idpConfirmOverrideLinkPage.getMessage());
        idpConfirmOverrideLinkPage.clickConfirmOverride();

        // assert federated identity override
        UserRepresentation user = ApiUtil.findUserByUsername(providerRealm, bc.getUserLogin());
        String providerUserId = user.getId();
        List<FederatedIdentityRepresentation> federatedIdentities = consumerRealm.users().get(createdUser).getFederatedIdentity();
        assertEquals(1, federatedIdentities.size());
        FederatedIdentityRepresentation actual = federatedIdentities.get(0);
        assertEquals(bc.getIDPAlias(), actual.getIdentityProvider());
        assertEquals(bc.getUserLogin(), actual.getUserName());
        if (this instanceof KcSamlFirstBrokerLoginTest) {
            // for SAML, the userID is username
            assertEquals(bc.getUserLogin(), actual.getUserId());
        } else {
            // for OIDC, the userID is id
            assertEquals(providerUserId, actual.getUserId());
        }

        RealmRepresentation consumerRealmRep = consumerRealm.toRepresentation();

        // one for showing the confirm page
        events.expectIdentityProviderFirstLogin(consumerRealmRep, bc.getIDPAlias(), bc.getUserLogin())
                .assertEvent(getFirstConsumerEvent());
        // one for submitting the confirmAction
        events.expectIdentityProviderFirstLogin(consumerRealmRep, bc.getIDPAlias(), bc.getUserLogin())
                .assertEvent(getFirstConsumerEvent());

        events.expect(EventType.FEDERATED_IDENTITY_OVERRIDE_LINK)
                .client("broker-app")
                .realm(consumerRealmRep)
                .user(createdUser)
                .detail(Details.IDENTITY_PROVIDER, bc.getIDPAlias())
                .detail(Details.IDENTITY_PROVIDER_USERNAME, bc.getUserLogin())
                .detail(Details.PREF_PREVIOUS + Details.IDENTITY_PROVIDER_USERNAME, "username")
                .assertEvent(getFirstConsumerEvent());
    }

    private Runnable toggleRegistrationAllowed(String realmName, boolean registrationAllowed) {
        RealmResource consumerRealm = adminClient.realm(realmName);
        RealmRepresentation realmRepresentation = consumerRealm.toRepresentation();
        boolean genuineValue = realmRepresentation.isRegistrationAllowed();
        realmRepresentation.setRegistrationAllowed(registrationAllowed);
        consumerRealm.update(realmRepresentation);

        return () -> toggleRegistrationAllowed(realmName, genuineValue);
    }
}
