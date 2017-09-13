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

package org.keycloak.testsuite.broker;

import org.junit.Assert;
import org.junit.Test;
import org.keycloak.authentication.authenticators.broker.IdpCreateUserIfUniqueAuthenticatorFactory;
import org.keycloak.authentication.authenticators.broker.IdpEmailVerificationAuthenticatorFactory;
import org.keycloak.common.util.ObjectUtil;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.pages.IdpConfirmLinkPage;
import org.keycloak.testsuite.pages.IdpLinkEmailPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginExpiredPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;
import org.keycloak.testsuite.pages.ProceedPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import javax.mail.internet.MimeMessage;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hamcrest.Matchers;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractFirstBrokerLoginTest extends AbstractIdentityProviderTest {

    protected static final String APP_REALM_ID = "realm-with-broker";

    @WebResource
    protected LoginUpdateProfileEditUsernameAllowedPage updateProfileWithUsernamePage;

    @WebResource
    protected IdpConfirmLinkPage idpConfirmLinkPage;

    @WebResource
    protected IdpLinkEmailPage idpLinkEmailPage;

    @WebResource
    protected LoginPasswordUpdatePage passwordUpdatePage;

    @WebResource
    protected LoginExpiredPage loginExpiredPage;



    /**
     * Tests that if updateProfile is off and CreateUserIfUnique authenticator mandatory, error page will be shown if user with same email already exists
     */
    @Test
    public void testErrorPageWhenDuplicationNotAllowed_updateProfileOff() {
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED);
                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_OFF);
            }

        }, APP_REALM_ID);

        loginIDP("pedroigor");

        WebElement element = this.driver.findElement(By.className("instruction"));

        assertNotNull(element);

        assertEquals("User with email psilva@redhat.com already exists. Please login to account management to link the account.", element.getText());

        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            }

        }, APP_REALM_ID);
    }


    /**
     * Tests that if updateProfile is on and CreateUserIfUnique authenticator mandatory, error page will be shown if user with same email already exists
     */
    @Test
    public void testErrorPageWhenDuplicationNotAllowed_updateProfileOn() {
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.REQUIRED);
                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_ON);
            }

        }, APP_REALM_ID);

        loginIDP("test-user");

        this.updateProfileWithUsernamePage.assertCurrent();
        this.updateProfileWithUsernamePage.update("Test", "User", "test-user@redhat.com", "pedroigor");

        WebElement element = this.driver.findElement(By.className("instruction"));

        assertNotNull(element);

        assertEquals("User with username pedroigor already exists. Please login to account management to link the account.", element.getText());

        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW, IdpCreateUserIfUniqueAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.ALTERNATIVE);
            }

        }, APP_REALM_ID);
    }


    /**
     * Test user registers with IdentityProvider and needs to update password when it's required by IdpCreateUserIfUniqueAuthenticator
     */
    @Test
    public void testRegistrationWithPasswordUpdateRequired() {
        // Require updatePassword after user registered with broker
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                AuthenticatorConfigModel authenticatorConfig = realmWithBroker.getAuthenticatorConfigByAlias(DefaultAuthenticationFlows.IDP_CREATE_UNIQUE_USER_CONFIG_ALIAS);
                authenticatorConfig.getConfig().put(IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "true");
                realmWithBroker.updateAuthenticatorConfig(authenticatorConfig);

                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_MISSING);
            }

        }, APP_REALM_ID);

        loginIDP("pedroigor");
        this.updateProfileWithUsernamePage.assertCurrent();
        this.updateProfileWithUsernamePage.update("Test", "User", "some-user@redhat.com", "some-user");

        // Need to update password now
        this.passwordUpdatePage.assertCurrent();
        this.passwordUpdatePage.changePassword("password1", "password1");


        // assert authenticated
        assertFederatedUser("some-user", "some-user@redhat.com", "pedroigor");


        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                AuthenticatorConfigModel authenticatorConfig = realmWithBroker.getAuthenticatorConfigByAlias(DefaultAuthenticationFlows.IDP_CREATE_UNIQUE_USER_CONFIG_ALIAS);
                authenticatorConfig.getConfig().put(IdpCreateUserIfUniqueAuthenticatorFactory.REQUIRE_PASSWORD_UPDATE_AFTER_REGISTRATION, "false");
                realmWithBroker.updateAuthenticatorConfig(authenticatorConfig);
            }

        }, APP_REALM_ID);
    }


    /**
     * Test user registers with IdentityProvider with emailAsUsername
     */
    @Test
    public void testRegistrationWithEmailAsUsername() {
        // Require updatePassword after user registered with broker
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_ON);
                realmWithBroker.setRegistrationEmailAsUsername(true);
            }

        }, APP_REALM_ID);

        loginIDP("pedroigor");
        this.updateProfileWithUsernamePage.assertCurrent();

        try {
            this.updateProfileWithUsernamePage.update("Test", "User", "some-user@redhat.com", "some-user");
            Assert.fail("It is not expected to see username field");
        } catch (NoSuchElementException expected) {
        }

        this.updateProfileWithUsernamePage.update("Test", "User", "some-user@redhat.com");

        // assert authenticated
        assertFederatedUser("some-user@redhat.com", "some-user@redhat.com", "pedroigor");

        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_MISSING);
                realmWithBroker.setRegistrationEmailAsUsername(false);
            }

        }, APP_REALM_ID);
    }


    /**
     * Tests that duplication is detected, the confirmation page is displayed, user clicks on "Review profile" and goes back to updateProfile page and resolves duplication
     * by create new user
     */
    @Test
    public void testFixDuplicationsByReviewProfile() {
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_OFF);

        loginIDP("pedroigor");

        // There is user with same email. Update profile to use different email
        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickReviewProfile();

        this.updateProfileWithUsernamePage.assertCurrent();
        this.updateProfileWithUsernamePage.update("Test", "User", "testing-user@redhat.com", "pedroigor");

        // There is user with same username. Update profile to use different username
        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with username pedroigor already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickReviewProfile();

        this.updateProfileWithUsernamePage.assertCurrent();
        this.updateProfileWithUsernamePage.update("Test", "User", "testing-user@redhat.com", "testing-user");

        assertFederatedUser("testing-user", "testing-user@redhat.com", "pedroigor");
    }

    /**
     * Tests that duplication is detected and user wants to link federatedIdentity with existing account. He will confirm link by email
     */
    @Test
    public void testLinkAccountByEmailVerification() throws Exception {
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_OFF);

        loginIDP("pedroigor");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        // Confirm linking account by email
        this.idpLinkEmailPage.assertCurrent();
        Assert.assertEquals("An email with instructions to link " + ObjectUtil.capitalize(getProviderId()) + " account pedroigor with your " + APP_REALM_ID + " account has been sent to you.", this.idpLinkEmailPage.getMessage());

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String linkFromMail = getVerificationEmailLink(message);

        driver.navigate().to(linkFromMail.trim());

        // authenticated and redirected to app. User is linked with identity provider
        assertFederatedUser("pedroigor", "psilva@redhat.com", "pedroigor");

        // Assert user's email is verified now
        UserModel user = getFederatedUser();
        Assert.assertTrue(user.isEmailVerified());
    }

    /**
     * Tests that duplication is detected and user wants to link federatedIdentity with existing account. He will confirm link by email
     */
    @Test
    public void testLinkAccountByEmailVerificationTwice() throws Exception {
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_OFF);

        loginIDP("pedroigor");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        // Confirm linking account by email
        this.idpLinkEmailPage.assertCurrent();
        Assert.assertThat(
          this.idpLinkEmailPage.getMessage(),
          is("An email with instructions to link " + ObjectUtil.capitalize(getProviderId()) + " account pedroigor with your " + APP_REALM_ID + " account has been sent to you.")
        );

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String linkFromMail = getVerificationEmailLink(message);

        driver.navigate().to(linkFromMail.trim());

        // authenticated and redirected to app. User is linked with identity provider
        assertFederatedUser("pedroigor", "psilva@redhat.com", "pedroigor");

        // Assert user's email is verified now
        UserModel user = getFederatedUser();
        Assert.assertTrue(user.isEmailVerified());

        // Attempt to use the link for the second time
        driver.navigate().to(linkFromMail.trim());

        infoPage.assertCurrent();
        Assert.assertThat(infoPage.getInfo(), is("You are already logged in."));

        // Log out
        driver.navigate().to("http://localhost:8081/test-app/logout");

        // Go to the same link again
        driver.navigate().to(linkFromMail.trim());

        proceedPage.assertCurrent();
        Assert.assertThat(proceedPage.getInfo(), Matchers.containsString("Confirm linking the account"));
        proceedPage.clickProceedLink();
        infoPage.assertCurrent();
        Assert.assertThat(infoPage.getInfo(), startsWith("You successfully verified your email. Please go back to your original browser and continue there with the login."));
    }

    /**
     * Tests that duplication is detected and user wants to link federatedIdentity with existing account. He will confirm link by email
     */
    @Test
    public void testLinkAccountByEmailVerificationDifferentBrowser() throws Exception, Throwable {
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_OFF);

        loginIDP("pedroigor");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        // Confirm linking account by email
        this.idpLinkEmailPage.assertCurrent();
        Assert.assertThat(
          this.idpLinkEmailPage.getMessage(),
          is("An email with instructions to link " + ObjectUtil.capitalize(getProviderId()) + " account pedroigor with your " + APP_REALM_ID + " account has been sent to you.")
        );

        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String linkFromMail = getVerificationEmailLink(message);

        WebRule webRule2 = new WebRule(this);
        try {
            webRule2.initProperties();

            WebDriver driver2 = webRule2.getDriver();
            InfoPage infoPage2 = webRule2.getPage(InfoPage.class);
            ProceedPage proceedPage2 = webRule2.getPage(ProceedPage.class);

            driver2.navigate().to(linkFromMail.trim());

            // authenticated, but not redirected to app. Just seeing info page.
            proceedPage2.assertCurrent();
            Assert.assertThat(proceedPage2.getInfo(), Matchers.containsString("Confirm linking the account"));
            proceedPage2.clickProceedLink();
            infoPage2.assertCurrent();
            Assert.assertThat(infoPage2.getInfo(), startsWith("You successfully verified your email. Please go back to your original browser and continue there with the login."));
        } finally {
            // Revert everything
            webRule2.after();
        }

        this.idpLinkEmailPage.clickContinueFlowLink();

        // authenticated and redirected to app. User is linked with identity provider
        assertFederatedUser("pedroigor", "psilva@redhat.com", "pedroigor");

        // Assert user's email is verified now
        UserModel user = getFederatedUser();
        Assert.assertTrue(user.isEmailVerified());
    }

    @Test
    public void testLinkAccountByEmailVerificationResendEmail() throws Exception, Throwable {
        setUpdateProfileFirstLogin(IdentityProviderRepresentation.UPFLM_OFF);

        loginIDP("pedroigor");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        // Confirm linking account by email
        this.idpLinkEmailPage.assertCurrent();
        Assert.assertThat(
          this.idpLinkEmailPage.getMessage(),
          is("An email with instructions to link " + ObjectUtil.capitalize(getProviderId()) + " account pedroigor with your " + APP_REALM_ID + " account has been sent to you.")
        );

        this.idpLinkEmailPage.clickResendEmail();

        this.idpLinkEmailPage.assertCurrent();
        Assert.assertThat(
          this.idpLinkEmailPage.getMessage(),
          is("An email with instructions to link " + ObjectUtil.capitalize(getProviderId()) + " account pedroigor with your " + APP_REALM_ID + " account has been sent to you.")
        );

        Assert.assertEquals(2, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String linkFromMail = getVerificationEmailLink(message);

        driver.navigate().to(linkFromMail.trim());

        // authenticated and redirected to app. User is linked with identity provider
        assertFederatedUser("pedroigor", "psilva@redhat.com", "pedroigor");

        // Assert user's email is verified now
        UserModel user = getFederatedUser();
        Assert.assertTrue(user.isEmailVerified());
    }


    /**
     * Tests that duplication is detected and user wants to link federatedIdentity with existing account. He will confirm link by reauthentication (confirm password on login screen)
     */
    @Test
    public void testLinkAccountByReauthenticationWithPassword() throws Exception {
        // Remove smtp config. The reauthentication by username+password screen will be automatically used then
        final Map<String, String> smtpConfig = new HashMap<>();
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_OFF);
                smtpConfig.putAll(realmWithBroker.getSmtpConfig());
                realmWithBroker.setSmtpConfig(Collections.<String, String>emptyMap());
            }

        }, APP_REALM_ID);


        loginIDP("pedroigor");


        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        // Login screen shown. Username is prefilled and disabled. Registration link and social buttons are not shown
        Assert.assertEquals("Log in to " + APP_REALM_ID, this.driver.getTitle());
        Assert.assertEquals("pedroigor", this.loginPage.getUsername());
        Assert.assertFalse(this.loginPage.isUsernameInputEnabled());

        Assert.assertEquals("Authenticate as pedroigor to link your account with " + getProviderId(), this.loginPage.getInfoMessage());

        try {
            this.loginPage.findSocialButton(getProviderId());
            Assert.fail("Not expected to see social button with " + getProviderId());
        } catch (NoSuchElementException expected) {
        }

        try {
            this.loginPage.clickRegister();
            Assert.fail("Not expected to see register link");
        } catch (NoSuchElementException expected) {
        }

        // Use bad password first
        this.loginPage.login("password1");
        Assert.assertEquals("Invalid username or password.", this.loginPage.getError());

        // Use correct password now
        this.loginPage.login("password");

        // authenticated and redirected to app. User is linked with identity provider
        assertFederatedUser("pedroigor", "psilva@redhat.com", "pedroigor");


        // Restore smtp config
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                realmWithBroker.setSmtpConfig(smtpConfig);
            }

        }, APP_REALM_ID);
    }


    /**
     * Variation of previous test, which uses browser buttons (back, refresh etc)
     */
    @Test
    public void testLinkAccountByReauthenticationWithPassword_browserButtons() throws Exception {
        // Remove smtp config. The reauthentication by username+password screen will be automatically used then
        final Map<String, String> smtpConfig = new HashMap<>();
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_OFF);
                smtpConfig.putAll(realmWithBroker.getSmtpConfig());
                realmWithBroker.setSmtpConfig(Collections.<String, String>emptyMap());
            }

        }, APP_REALM_ID);


        // Use invalid username for the first time
        loginIDP("foo");
        assertTrue(driver.getCurrentUrl().startsWith("http://localhost:8082/auth/"));
        this.loginPage.login("pedroigor", "password");


        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());

        // Click browser 'back' and then 'forward' and then continue
        driver.navigate().back();
        Assert.assertTrue(driver.getPageSource().contains("You are already logged in."));
        driver.navigate().forward();
        this.idpConfirmLinkPage.assertCurrent();

        // Click browser 'back' on review profile page
        this.idpConfirmLinkPage.clickReviewProfile();
        this.updateProfilePage.assertCurrent();
        driver.navigate().back();
        this.updateProfilePage.assertCurrent();
        this.updateProfilePage.update("Pedro", "Igor", "psilva@redhat.com");

        this.idpConfirmLinkPage.assertCurrent();
        this.idpConfirmLinkPage.clickLinkAccount();

        // Login screen shown. Username is prefilled and disabled. Registration link and social buttons are not shown
        Assert.assertEquals("Log in to " + APP_REALM_ID, this.driver.getTitle());
        Assert.assertEquals("pedroigor", this.loginPage.getUsername());
        Assert.assertFalse(this.loginPage.isUsernameInputEnabled());

        Assert.assertEquals("Authenticate as pedroigor to link your account with " + getProviderId(), this.loginPage.getInfoMessage());

        try {
            this.loginPage.findSocialButton(getProviderId());
            Assert.fail("Not expected to see social button with " + getProviderId());
        } catch (NoSuchElementException expected) {
        }

        try {
            this.loginPage.clickRegister();
            Assert.fail("Not expected to see register link");
        } catch (NoSuchElementException expected) {
        }

        // Use bad password first
        this.loginPage.login("password1");
        Assert.assertEquals("Invalid username or password.", this.loginPage.getError());

        // Click browser 'back' and then continue
        this.driver.navigate().back();
        this.loginExpiredPage.assertCurrent();
        this.loginExpiredPage.clickLoginContinueLink();

        // Use correct password now
        this.loginPage.login("password");

        // authenticated and redirected to app. User is linked with identity provider
        assertFederatedUser("pedroigor", "psilva@redhat.com", "pedroigor");


        // Restore smtp config
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                realmWithBroker.setSmtpConfig(smtpConfig);
            }

        }, APP_REALM_ID);
    }


    /**
     * Tests that duplication is detected and user wants to link federatedIdentity with existing account. He will confirm link by reauthentication (confirm password on login screen)
     * and additionally he goes through "forget password"
     */
    @Test
    public void testLinkAccountByReauthentication_forgetPassword() throws Exception {
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_HANDLE_EXISTING_SUBFLOW,
                        IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.DISABLED);

                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_OFF);
            }

        }, APP_REALM_ID);

        loginIDP("pedroigor");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        // Click "Forget password" on login page. Email sent directly because username is known
        Assert.assertEquals("Log in to " + APP_REALM_ID, this.driver.getTitle());
        this.loginPage.resetPassword();

        Assert.assertEquals("Log in to " + APP_REALM_ID, this.driver.getTitle());
        Assert.assertEquals("You should receive an email shortly with further instructions.", this.loginPage.getSuccessMessage());

        // Click on link from email
        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String linkFromMail = getVerificationEmailLink(message);

        driver.navigate().to(linkFromMail.trim());

        // Need to update password now
        this.passwordUpdatePage.assertCurrent();
        this.passwordUpdatePage.changePassword("password", "password");

        // authenticated and redirected to app. User is linked with identity provider
        assertFederatedUser("pedroigor", "psilva@redhat.com", "pedroigor");

        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_HANDLE_EXISTING_SUBFLOW,
                        IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.ALTERNATIVE);

            }

        }, APP_REALM_ID);
    }


    /**
     * Same like above, but "forget password" link is opened in different browser
     */
    @Test
    public void testLinkAccountByReauthentication_forgetPassword_differentBrowser() throws Throwable {
        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_HANDLE_EXISTING_SUBFLOW,
                        IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.DISABLED);

                setUpdateProfileFirstLogin(realmWithBroker, IdentityProviderRepresentation.UPFLM_OFF);
            }

        }, APP_REALM_ID);

        loginIDP("pedroigor");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        // Click "Forget password" on login page. Email sent directly because username is known
        Assert.assertEquals("Log in to " + APP_REALM_ID, this.driver.getTitle());
        this.loginPage.resetPassword();

        Assert.assertEquals("Log in to " + APP_REALM_ID, this.driver.getTitle());
        Assert.assertEquals("You should receive an email shortly with further instructions.", this.loginPage.getSuccessMessage());

        // Click on link from email
        Assert.assertEquals(1, greenMail.getReceivedMessages().length);
        MimeMessage message = greenMail.getReceivedMessages()[0];
        String linkFromMail = getVerificationEmailLink(message);

        // Simulate 2nd browser
        WebRule webRule2 = new WebRule(this);
        try {
            webRule2.initProperties();

            WebDriver driver2 = webRule2.getDriver();
            LoginPasswordUpdatePage passwordUpdatePage2 = webRule2.getPage(LoginPasswordUpdatePage.class);
            InfoPage infoPage2 = webRule2.getPage(InfoPage.class);

            driver2.navigate().to(linkFromMail.trim());

            // Need to update password now
            passwordUpdatePage2.assertCurrent();
            passwordUpdatePage2.changePassword("password", "password");

            // authenticated, but not redirected to app. Just seeing info page.
            infoPage2.assertCurrent();
            Assert.assertEquals("Your account has been updated.", infoPage2.getInfo());
        } finally {
            // Revert everything
            webRule2.after();
        }

        // User is not yet linked with identity provider. He needs to authenticate again in 1st browser
        RealmModel realmWithBroker = getRealm();
        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(this.session.users().getUserByUsername("pedroigor", realmWithBroker), realmWithBroker);
        assertEquals(0, federatedIdentities.size());

        // Continue with 1st browser. Note that the user has already authenticated with brokered IdP in the beginning of this test
        // so entering their credentials there is now skipped.
        loginToIDPWhenAlreadyLoggedIntoProviderIdP("pedroigor");

        this.idpConfirmLinkPage.assertCurrent();
        Assert.assertEquals("User with email psilva@redhat.com already exists. How do you want to continue?", this.idpConfirmLinkPage.getMessage());
        this.idpConfirmLinkPage.clickLinkAccount();

        Assert.assertEquals("Log in to " + APP_REALM_ID, this.driver.getTitle());
        this.loginPage.login("password");

        // authenticated and redirected to app. User is linked with identity provider
        assertFederatedUser("pedroigor", "psilva@redhat.com", "pedroigor");

        brokerServerRule.update(new KeycloakRule.KeycloakSetup() {

            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel realmWithBroker) {
                setExecutionRequirement(realmWithBroker, DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_HANDLE_EXISTING_SUBFLOW,
                        IdpEmailVerificationAuthenticatorFactory.PROVIDER_ID, AuthenticationExecutionModel.Requirement.ALTERNATIVE);

            }

        }, APP_REALM_ID);
    }


    protected void assertFederatedUser(String expectedUsername, String expectedEmail, String expectedFederatedUsername) {
        assertTrue(this.driver.getCurrentUrl().startsWith("http://localhost:8081/test-app"));
        UserModel federatedUser = getFederatedUser();

        assertNotNull(federatedUser);
        assertEquals(expectedUsername, federatedUser.getUsername());
        assertEquals(expectedEmail, federatedUser.getEmail());

        RealmModel realmWithBroker = getRealm();
        Set<FederatedIdentityModel> federatedIdentities = this.session.users().getFederatedIdentities(federatedUser, realmWithBroker);
        assertEquals(1, federatedIdentities.size());

        FederatedIdentityModel federatedIdentityModel = federatedIdentities.iterator().next();

        assertEquals(getProviderId(), federatedIdentityModel.getIdentityProvider());
        assertEquals(expectedFederatedUsername, federatedIdentityModel.getUserName());
    }


    protected static void setExecutionRequirement(RealmModel realmWithBroker, String flowAlias, String authenticatorProvider, AuthenticationExecutionModel.Requirement requirement) {
        AuthenticationFlowModel flowModel = realmWithBroker.getFlowByAlias(flowAlias);
        List<AuthenticationExecutionModel> authExecutions = realmWithBroker.getAuthenticationExecutions(flowModel.getId());
        for (AuthenticationExecutionModel execution : authExecutions) {
            if (execution.getAuthenticator().equals(authenticatorProvider)) {
                execution.setRequirement(requirement);
                realmWithBroker.updateAuthenticatorExecution(execution);
                return;
            }
        }

        throw new IllegalStateException("Execution not found for flow " + flowAlias + " and authenticator " + authenticatorProvider);
    }

}
