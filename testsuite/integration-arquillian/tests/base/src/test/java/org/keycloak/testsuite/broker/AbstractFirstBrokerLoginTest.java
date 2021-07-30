package org.keycloak.testsuite.broker;

import java.util.List;

import javax.mail.internet.MimeMessage;
import javax.ws.rs.core.Response;

import com.google.common.collect.ImmutableMap;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.broker.provider.HardcodedUserSessionAttributeMapper;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderSyncMode;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.IdentityProviderMapperRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.forms.VerifyProfileTest;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.MailServer;
import org.keycloak.testsuite.util.MailServerConfiguration;
import org.keycloak.testsuite.util.SecondBrowser;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.support.PageFactory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.assertHardCodedSessionNote;
import static org.keycloak.testsuite.broker.BrokerRunOnServerUtil.configureAutoLinkFlow;
import static org.keycloak.testsuite.broker.BrokerTestConstants.USER_EMAIL;
import static org.keycloak.testsuite.broker.BrokerTestTools.getConsumerRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.getProviderRoot;
import static org.keycloak.testsuite.broker.BrokerTestTools.waitForPage;
import static org.keycloak.testsuite.util.MailAssert.assertEmailAndGetUrl;

/**
 * Test of various scenarios related to first broker login.
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractFirstBrokerLoginTest extends AbstractInitializedBaseBrokerTest {

    @Drone
    @SecondBrowser
    protected WebDriver driver2;
    
    protected void enableDynamicUserProfile() {
        
        RealmResource rr = adminClient.realm(bc.consumerRealmName());
        
        RealmRepresentation testRealm = rr.toRepresentation();
        
        VerifyProfileTest.enableDynamicUserProfile(testRealm);

        rr.update(testRealm);
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractFirstBrokerLoginTest#testErrorPageWhenDuplicationNotAllowed_updateProfileOn
     */
    @Test
    public void testErrorExistingUserWithUpdateProfile() {
        createUser("consumer");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

        assertNumFederatedIdentities(existingUser, 1);
    }

    /**
     * KEYCLOAK-12870
     */
    @Test
    public void testLinkAccountByReauthenticationResetPassword() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);
        String existingUser = createUser("consumer");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

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
        assertTrue(driver.getPageSource().contains("You are already logged in."));
        driver.navigate().forward(); // here a new execution ID is added to the URL using JS, see below
        idpConfirmLinkPage.assertCurrent();

        // Click browser 'back' on review profile page
        idpConfirmLinkPage.clickReviewProfile();
        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        driver.navigate().back();
        // JS-capable browsers (i.e. all except HtmlUnit) add a new execution ID to the URL which then causes the login expire page to appear (because the old ID and new ID don't match)
        if (!(driver instanceof HtmlUnitDriver)) {
            loginExpiredPage.assertCurrent();
            loginExpiredPage.clickLoginContinueLink();
        }
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
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

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

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
            loginPage.login(bc.getUserLogin(), bc.getUserPassword());
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        Assert.assertTrue(updateAccountInformationPage.isCurrent());
        Assert.assertTrue("We must be on correct realm right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.consumerRealmName() + "/"));

        log.debug("Updating info on updateAccount page");
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        waitForPage(driver, "update password", false);
        updatePasswordPage.updatePasswords("password", "password");
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
        Assert.assertEquals("FirstName", accountUpdateProfilePage.getFirstName());
        Assert.assertEquals("LastName", accountUpdateProfilePage.getLastName());
        Assert.assertEquals("test@localhost.com", accountUpdateProfilePage.getEmail());
        Assert.assertEquals("test", accountUpdateProfilePage.getUsername());
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));

        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

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
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
        assertEquals(accountPage.buildUri().toASCIIString().replace("master", "consumer") + "/", driver.getCurrentUrl());

        //test if the user has verified email
        assertTrue(realm.users().get(linkedUserId).toRepresentation().isEmailVerified());
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        verifyEmailPage.assertCurrent();

        String verificationUrl = assertEmailAndGetUrl(MailServerConfiguration.FROM, USER_EMAIL,
                "verify your email address", false);

        driver.navigate().to(verificationUrl.trim());
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");
        loginPage.login("no-email", "password");

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

        List<UserRepresentation> users = realm.users().search("no-email");
        assertEquals(1, users.size());
        List<String> requiredActions = users.get(0).getRequiredActions();
        assertEquals(1, requiredActions.size());
        assertEquals(UserModel.RequiredAction.VERIFY_EMAIL.name(), requiredActions.get(0));

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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("FirstName", "LastName");

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        waitForPage(driver, "update account information", false);
        updateAccountInformationPage.assertCurrent();
        updateAccountInformationPage.updateAccountInformation("changed@localhost.com", "FirstName", "LastName");

        verifyEmailPage.assertCurrent();

        String verificationUrl = assertEmailAndGetUrl(MailServerConfiguration.FROM, "changed@localhost.com",
                "verify your email address", false);

        driver.navigate().to(verificationUrl.trim());
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();

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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
        assertEquals(accountPage.buildUri().toASCIIString().replace("master", "consumer") + "/", driver.getCurrentUrl());
        //test if the user has verified email
        assertTrue(adminClient.realm(bc.consumerRealmName()).users().get(consumerUser.getId()).toRepresentation().isEmailVerified());

        driver.navigate().to(url);
        waitForPage(driver, "you are already logged in.", false);
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());

        driver.navigate().to(url);
        waitForPage(driver, "confirm linking the account testuser of identity provider " + bc.getIDPAlias() + " with your account.", false);
        proceedPage.clickProceedLink();
        waitForPage(driver, "you successfully verified your email. please go back to your original browser and continue there with the login.", false);
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

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest.testSuccessfulAuthenticationUpdateProfileOnMissing_missingEmail
     */
    @Test
    public void testUpdateProfileIfMissingInformation() {
        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);

        createUser(bc.providerRealmName(), "no-first-name", "password", null, "LastName", "no-first-name@localhost.com");
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
        Assert.assertEquals("FirstName", accountUpdateProfilePage.getFirstName());
        Assert.assertEquals("LastName", accountUpdateProfilePage.getLastName());
        Assert.assertEquals("no-first-name@localhost.com", accountUpdateProfilePage.getEmail());
        Assert.assertEquals("no-first-name", accountUpdateProfilePage.getUsername());


        logoutFromRealm(getProviderRoot(), bc.providerRealmName());
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
        createUser(bc.providerRealmName(), "no-last-name", "password", "FirstName", null, "no-last-name@localhost.com");
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
        Assert.assertEquals("FirstName", accountUpdateProfilePage.getFirstName());
        Assert.assertEquals("LastName", accountUpdateProfilePage.getLastName());
        Assert.assertEquals("no-last-name@localhost.com", accountUpdateProfilePage.getEmail());
        Assert.assertEquals("no-last-name", accountUpdateProfilePage.getUsername());

        logoutFromRealm(getProviderRoot(), bc.providerRealmName());
        logoutFromRealm(getConsumerRoot(), bc.consumerRealmName());
        createUser(bc.providerRealmName(), "no-email", "password", "FirstName", "LastName", null);
        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
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

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
        Assert.assertEquals("FirstName", accountUpdateProfilePage.getFirstName());
        Assert.assertEquals("LastName", accountUpdateProfilePage.getLastName());
        Assert.assertEquals("no-email@localhost.com", accountUpdateProfilePage.getEmail());
        Assert.assertEquals("no-email", accountUpdateProfilePage.getUsername());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest.testSuccessfulAuthenticationUpdateProfileOnMissing_nothingMissing
     */
    @Test
    public void testUpdateProfileIfNotMissingInformation() {
        updateExecutions(AbstractBrokerTest::setUpMissingUpdateProfileOnFirstLogin);
        createUser(bc.providerRealmName(), "all-info-set", "password", "FirstName", "LastName", "all-info-set@localhost.com");

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        log.debug("Clicking social " + bc.getIDPAlias());
        loginPage.clickSocial(bc.getIDPAlias());
        waitForPage(driver, "sign in to", true);
        Assert.assertTrue("Driver should be on the provider realm page right now",
                driver.getCurrentUrl().contains("/auth/realms/" + bc.providerRealmName() + "/"));
        log.debug("Logging in");
        loginPage.login("all-info-set", "password");

        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
        Assert.assertEquals("FirstName", accountUpdateProfilePage.getFirstName());
        Assert.assertEquals("LastName", accountUpdateProfilePage.getLastName());
        Assert.assertEquals("all-info-set@localhost.com", accountUpdateProfilePage.getEmail());
        Assert.assertEquals("all-info-set", accountUpdateProfilePage.getUsername());
    }


    /**
     * Refers to in old test suite: org.keycloak.testsuite.broker.AbstractKeycloakIdentityProviderTest.testSuccessfulAuthenticationWithoutUpdateProfile
     */
    @Test
    public void testWithoutUpdateProfile() {
        updateExecutions(AbstractBrokerTest::disableUpdateProfileOnFirstLogin);

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);
        waitForAccountManagementTitle();
        accountUpdateProfilePage.assertCurrent();
        Assert.assertEquals("", accountUpdateProfilePage.getFirstName());
        Assert.assertEquals("", accountUpdateProfilePage.getLastName());
        Assert.assertEquals(bc.getUserEmail(), accountUpdateProfilePage.getEmail());
        Assert.assertEquals(bc.getUserLogin(), accountUpdateProfilePage.getUsername());
    }


    /**
     * Tests that user can link federated identity with existing brokered
     * account without prompt (KEYCLOAK-7270).
     */
    @Test
    public void testAutoLinkAccountWithBroker() {
        testingClient.server(bc.consumerRealmName()).run(configureAutoLinkFlow(bc.getIDPAlias()));

        driver.navigate().to(getAccountUrl(getConsumerRoot(), bc.consumerRealmName()));
        logInWithBroker(bc);

        RealmResource realm = adminClient.realm(bc.consumerRealmName());

        assertNumFederatedIdentities(realm.users().search(bc.getUserLogin()).get(0).getId(), 1);
    }

}
