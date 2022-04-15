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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.AuthenticationFlow;
import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.authentication.forms.RegistrationPassword;
import org.keycloak.authentication.forms.RegistrationProfile;
import org.keycloak.authentication.forms.RegistrationRecaptcha;
import org.keycloak.authentication.forms.RegistrationUserCreation;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserBuilder;

import javax.mail.internet.MimeMessage;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.jgroups.util.Util.assertTrue;
import static org.junit.Assert.assertEquals;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class RegisterTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected AccountUpdateProfilePage accountPage;

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void registerExistingUsernameForbidden() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerExistingUser@email", "roleRichUser", "password", "password");

        registerPage.assertCurrent();
        assertEquals("Username already exists.", registerPage.getInputAccountErrors().getUsernameError());

        // assert form keeps form fields on error
        assertEquals("firstName", registerPage.getFirstName());
        assertEquals("lastName", registerPage.getLastName());
        assertEquals("registerExistingUser@email", registerPage.getEmail());
        assertEquals("roleRichUser", registerPage.getUsername());
        assertEquals("", registerPage.getPassword());
        assertEquals("", registerPage.getPasswordConfirm());

        events.expectRegister("rolerichuser", "registerExistingUser@email")
                .removeDetail(Details.EMAIL)
                .user((String) null).error("username_in_use").assertEvent();
    }
 
    @Test
    public void registerExistingEmailForbidden() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "test-user@localhost", "registerExistingUser", "password", "password");

        registerPage.assertCurrent();
        assertEquals("Email already exists.", registerPage.getInputAccountErrors().getEmailError());

        // assert form keeps form fields on error
        assertEquals("firstName", registerPage.getFirstName());
        assertEquals("lastName", registerPage.getLastName());
        assertEquals("test-user@localhost", registerPage.getEmail());
        assertEquals("registerExistingUser", registerPage.getUsername());
        assertEquals("", registerPage.getPassword());
        assertEquals("", registerPage.getPasswordConfirm());

        events.expectRegister("registerexistinguser", "registerExistingUser@email")
                .removeDetail(Details.EMAIL)
                .user((String) null).error("email_in_use").assertEvent();
    }
 
    @Test
    public void registerExistingEmailAllowed() throws IOException {
        try (RealmAttributeUpdater rau = setDuplicateEmailsAllowed(true).update()) {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "test-user@localhost", "registerExistingEmailUser", "password", "password");

            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = events.expectRegister("registerExistingEmailUser", "test-user@localhost").assertEvent().getUserId();
            events.expectLogin().detail("username", "registerexistingemailuser").user(userId).assertEvent();

            assertUserBasicRegisterAttributes(userId, "registerexistingemailuser", "test-user@localhost", "firstName", "lastName");

            testRealm().users().get(userId).remove();
        }
    }

    @Test
    public void registerUpperCaseEmail() throws IOException {
        String userId = registerUpperCaseAndGetUserId(false);
        assertThat(userId, notNullValue());
        testRealm().users().get(userId).remove();
    }

    @Test
    public void registerUpperCaseEmailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            String userId = registerUpperCaseAndGetUserId(true);
            assertThat(userId, notNullValue());
            testRealm().users().get(userId).remove();
        }
    }

    @Test
    public void registerUpperCaseEmailWithChangedEmailAsUsername() throws IOException {
        Assume.assumeTrue("See https://github.com/keycloak/keycloak/issues/10245", isUserCacheEnabled());

        String userId = registerUpperCaseAndGetUserId(false);
        assertThat(userId, notNullValue());
        oauth.openLogout();
        events.clear();

        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            loginPage.open();
            loginPage.assertCurrent();

            final String EMAIL = "TEST@localhost";
            loginPage.login(EMAIL, "password");
            assertThat(RequestType.AUTH_RESPONSE, is(appPage.getRequestType()));

            events.expectLogin()
                    .detail("username", EMAIL)
                    .user(userId)
                    .assertEvent();
        } finally {
            assertThat(userId, notNullValue());
            testRealm().users().get(userId).remove();
        }
    }

    @Test
    public void registerUserInvalidPasswordConfirm() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserInvalidPasswordConfirm@email", "registerUserInvalidPasswordConfirm", "password", "invalid");

        registerPage.assertCurrent();
        assertEquals("Password confirmation doesn't match.", registerPage.getInputPasswordErrors().getPasswordConfirmError());

        // assert form keeps form fields on error
        assertEquals("firstName", registerPage.getFirstName());
        assertEquals("lastName", registerPage.getLastName());
        assertEquals("registerUserInvalidPasswordConfirm@email", registerPage.getEmail());
        assertEquals("registerUserInvalidPasswordConfirm", registerPage.getUsername());
        assertEquals("", registerPage.getPassword());
        assertEquals("", registerPage.getPasswordConfirm());

        events.expectRegister("registerUserInvalidPasswordConfirm", "registerUserInvalidPasswordConfirm@email")
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.EMAIL)
                .user((String) null).error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserMissingPassword() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserMissingPassword@email", "registerUserMissingPassword", null, null);

        registerPage.assertCurrent();
        assertEquals("Please specify password.", registerPage.getInputPasswordErrors().getPasswordError());

        events.expectRegister("registerUserMissingPassword", "registerUserMissingPassword@email")
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.EMAIL)
                .user((String) null).error("invalid_registration").assertEvent();
    }

    @Test
    public void registerPasswordPolicy() throws IOException {
        try (RealmAttributeUpdater rau = getRealmAttributeUpdater().setPasswordPolicy("length").update()) {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerPasswordPolicy@email", "registerPasswordPolicy", "pass", "pass");

            registerPage.assertCurrent();
            assertEquals("Invalid password: minimum length 8.", registerPage.getInputPasswordErrors().getPasswordError());

            events.expectRegister("registerPasswordPolicy", "registerPasswordPolicy@email")
                    .removeDetail(Details.USERNAME)
                    .removeDetail(Details.EMAIL)
                    .user((String) null).error("invalid_registration").assertEvent();

            registerPage.register("firstName", "lastName", "registerPasswordPolicy@email", "registerPasswordPolicy", "password", "password");
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = events.expectRegister("registerPasswordPolicy", "registerPasswordPolicy@email").assertEvent().getUserId();

            events.expectLogin().user(userId).detail(Details.USERNAME, "registerpasswordpolicy").assertEvent();
        }
    }

    @Test
    public void registerUserMissingUsername() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserMissingUsername@email", null, "password", "password");

        registerPage.assertCurrent();
        assertEquals("Please specify username.", registerPage.getInputAccountErrors().getUsernameError());

        events.expectRegister(null, "registerUserMissingUsername@email")
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.EMAIL)
                .error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserManyErrors() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register(null, null, null, null, null, null);

        registerPage.assertCurrent();

        assertEquals("Please specify username.", registerPage.getInputAccountErrors().getUsernameError());
        assertThat(registerPage.getInputAccountErrors().getFirstNameError(), anyOf(
                containsString("Please specify first name"),
                containsString("Please specify this field")
        ));
        assertThat(registerPage.getInputAccountErrors().getLastNameError(), anyOf(
                containsString("Please specify last name"),
                containsString("Please specify this field")
        ));
        assertThat(registerPage.getInputAccountErrors().getEmailError(), anyOf(
                containsString("Please specify email"),
                containsString("Please specify this field")
        ));

        assertThat(registerPage.getInputPasswordErrors().getPasswordError(), is("Please specify password."));

        events.expectRegister(null, "registerUserMissingUsername@email")
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.EMAIL)
                .error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserMissingEmail() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", null, "registerUserMissingEmail", "password", "password");
        registerPage.assertCurrent();
        assertEquals("Please specify email.", registerPage.getInputAccountErrors().getEmailError());
        events.expectRegister("registerusermissingemail", null)
                .removeDetail("email")
                .error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserInvalidEmail() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("firstName", "lastName", "registerUserInvalidEmailemail", "registerUserInvalidEmail", "password", "password");
        registerPage.assertCurrent();
        assertEquals("registerUserInvalidEmailemail", registerPage.getEmail());
        assertEquals("Invalid email address.", registerPage.getInputAccountErrors().getEmailError());
        events.expectRegister("registeruserinvalidemail", "registerUserInvalidEmailemail")
                .error("invalid_registration").assertEvent();
    }

    @Test
    public void registerUserSuccess() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        //contains few special characters we want to be sure they are allowed in username
        String username = "register.U-se@rS_uccess";
        
        registerPage.register("firstName", "lastName", "registerUserSuccess@email", username, "password", "password");

        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expectRegister(username, "registerUserSuccess@email").assertEvent().getUserId();
        assertUserRegistered(userId, username.toLowerCase(), "registerusersuccess@email");
    }

    private void assertUserRegistered(String userId, String username, String email) {
        events.expectLogin().detail("username", username.toLowerCase()).user(userId).assertEvent();

        UserRepresentation user = getUser(userId);
        Assert.assertNotNull(user);
        Assert.assertNotNull(user.getCreatedTimestamp());
        // test that timestamp is current with 10s tollerance
        Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);
        assertUserBasicRegisterAttributes(userId, username, email, "firstName", "lastName");
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // GreenMailRule is not working atm
    public void registerUserSuccessWithEmailVerification() throws Exception {
        try (RealmAttributeUpdater rau = setVerifyEmail(true).update()) {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerUserSuccessWithEmailVerification@email", "registerUserSuccessWithEmailVerification", "password", "password");
            verifyEmailPage.assertCurrent();

            String userId = events.expectRegister("registerUserSuccessWithEmailVerification", "registerUserSuccessWithEmailVerification@email").assertEvent().getUserId();

            {
                assertTrue("Expecting verify email", greenMail.waitForIncomingEmail(1000, 1));

                events.expect(EventType.SEND_VERIFY_EMAIL)
                  .detail(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
                  .user(userId)
                  .assertEvent();

                MimeMessage message = greenMail.getLastReceivedMessage();
                String link = MailUtils.getPasswordResetEmailLink(message);

                driver.navigate().to(link);
            }

            events.expectRequiredAction(EventType.VERIFY_EMAIL)
              .detail(Details.EMAIL, "registerUserSuccessWithEmailVerification@email".toLowerCase())
              .user(userId)
              .assertEvent();

            assertUserRegistered(userId, "registerUserSuccessWithEmailVerification", "registerUserSuccessWithEmailVerification@email");

            appPage.assertCurrent();
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            // test that timestamp is current with 10s tollerance
            // test user info is set from form
        }
    }

    @Test
    @AuthServerContainerExclude(AuthServer.REMOTE) // GreenMailRule is not working atm
    public void registerUserSuccessWithEmailVerificationWithResend() throws Exception {
        try (RealmAttributeUpdater rau = setVerifyEmail(true).update()) {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerUserSuccessWithEmailVerificationWithResend@email", "registerUserSuccessWithEmailVerificationWithResend", "password", "password");
            verifyEmailPage.assertCurrent();

            String userId = events.expectRegister("registerUserSuccessWithEmailVerificationWithResend", "registerUserSuccessWithEmailVerificationWithResend@email").assertEvent().getUserId();

            {
                assertTrue("Expecting verify email", greenMail.waitForIncomingEmail(1000, 1));

                events.expect(EventType.SEND_VERIFY_EMAIL)
                  .detail(Details.EMAIL, "registerUserSuccessWithEmailVerificationWithResend@email".toLowerCase())
                  .user(userId)
                  .assertEvent();

                verifyEmailPage.clickResendEmail();
                verifyEmailPage.assertCurrent();

                assertTrue("Expecting second verify email", greenMail.waitForIncomingEmail(1000, 1));

                events.expect(EventType.SEND_VERIFY_EMAIL)
                  .detail(Details.EMAIL, "registerUserSuccessWithEmailVerificationWithResend@email".toLowerCase())
                  .user(userId)
                  .assertEvent();

                MimeMessage message = greenMail.getLastReceivedMessage();
                String link = MailUtils.getPasswordResetEmailLink(message);

                driver.navigate().to(link);
            }

            events.expectRequiredAction(EventType.VERIFY_EMAIL)
              .detail(Details.EMAIL, "registerUserSuccessWithEmailVerificationWithResend@email".toLowerCase())
              .user(userId)
              .assertEvent();

            assertUserRegistered(userId, "registerUserSuccessWithEmailVerificationWithResend", "registerUserSuccessWithEmailVerificationWithResend@email");

            appPage.assertCurrent();
            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            // test that timestamp is current with 10s tollerance
            // test user info is set from form
        }
    }

    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void registerUserUmlats() {
        loginPage.open();

        assertTrue(loginPage.isCurrent());

        loginPage.clickRegister();
        registerPage.assertCurrent();

        registerPage.register("Äǜṳǚǘǖ", "Öṏṏ", "registeruserumlats@email", "registeruserumlats", "password", "password");

        String userId = events.expectRegister("registeruserumlats", "registeruserumlats@email").assertEvent().getUserId();
        events.expectLogin().detail("username", "registeruserumlats").user(userId).assertEvent();

        accountPage.open();
        assertTrue(accountPage.isCurrent());

        UserRepresentation user = getUser(userId);
        Assert.assertNotNull(user);
        assertEquals("Äǜṳǚǘǖ", user.getFirstName());
        assertEquals("Öṏṏ", user.getLastName());

        assertEquals("Äǜṳǚǘǖ", accountPage.getFirstName());
        assertEquals("Öṏṏ", accountPage.getLastName());
    }

    // KEYCLOAK-3266
    @Test
    public void registerUserNotUsernamePasswordPolicy() throws IOException {
        try (RealmAttributeUpdater rau = getRealmAttributeUpdater().setPasswordPolicy("notUsername").update()) {
            loginPage.open();

            assertTrue(loginPage.isCurrent());

            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("firstName", "lastName", "registerUserNotUsername@email", "registerUserNotUsername", "registerUserNotUsername", "registerUserNotUsername");

            assertTrue(registerPage.isCurrent());
            assertEquals("Invalid password: must not be equal to the username.", registerPage.getInputPasswordErrors().getPasswordError());

            adminClient.realm("test").users().create(UserBuilder.create().username("registerUserNotUsername").build());

            registerPage.register("firstName", "lastName", "registerUserNotUsername@email", "registerUserNotUsername", "registerUserNotUsername", "registerUserNotUsername");

            assertTrue(registerPage.isCurrent());
            assertEquals("Username already exists.", registerPage.getInputAccountErrors().getUsernameError());

            registerPage.register("firstName", "lastName", "registerUserNotUsername@email", null, "password", "password");

            assertTrue(registerPage.isCurrent());
            assertEquals("Please specify username.", registerPage.getInputAccountErrors().getUsernameError());
        }
    }

    // KEYCLOAK-12729
    @Test
    public void registerUserNotEmailPasswordPolicy() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true)
                .setPasswordPolicy("notEmail").update()) {

            loginPage.open();

            assertTrue(loginPage.isCurrent());

            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "registerUserNotEmail@email", "registerUserNotEmail@email", "registerUserNotEmail@email");

            assertTrue(registerPage.isCurrent());
            assertEquals("Invalid password: must not be equal to the email.", registerPage.getInputPasswordErrors().getPasswordError());
        }
    }

    protected UserRepresentation getUser(String userId) {
        return testRealm().users().get(userId).toRepresentation();
    }

    @Test
    public void registerExistingUser_emailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "test-user@localhost", "password", "password");

            registerPage.assertCurrent();
            assertEquals("Email already exists.", registerPage.getInputAccountErrors().getEmailError());

            events.expectRegister("test-user@localhost", "test-user@localhost").user((String) null).error("email_in_use").assertEvent();
        }
    }

    @Test
    public void registerUserMissingOrInvalidEmail_emailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", null, "password", "password");
            registerPage.assertCurrent();
            assertEquals("Please specify email.", registerPage.getInputAccountErrors().getEmailError());
            events.expectRegister(null, null).removeDetail("username").removeDetail("email").error("invalid_registration").assertEvent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "registerUserInvalidEmailemail", "password", "password");
            registerPage.assertCurrent();
            assertEquals("Invalid email address.", registerPage.getInputAccountErrors().getEmailError());
            events.expectRegister("registerUserInvalidEmailemail", "registerUserInvalidEmailemail").error("invalid_registration").assertEvent();
        }
    }

    @Test
    public void registerUserSuccess_emailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.registerWithEmailAsUsername("firstName", "lastName", "registerUserSuccessE@email", "password", "password");

            assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            String userId = events.expectRegister("registerUserSuccessE@email", "registerUserSuccessE@email").assertEvent().getUserId();
            events.expectLogin().detail("username", "registerusersuccesse@email").user(userId).assertEvent();

            UserRepresentation user = getUser(userId);
            Assert.assertNotNull(user);
            Assert.assertNotNull(user.getCreatedTimestamp());
            // test that timestamp is current with 10s tollerance
            Assert.assertTrue((System.currentTimeMillis() - user.getCreatedTimestamp()) < 10000);
        }
    }

    @Test
    public void registerWithLoginHint() throws IOException {

        registerPage.openWithLoginHint("username_test");

        assertEquals("username_test", registerPage.getUsername());
    }

    @Test
    public void registerWithLoginHint_emailAsUsername() throws IOException {
        try (RealmAttributeUpdater rau = configureRealmRegistrationEmailAsUsername(true).update()) {
            registerPage.openWithLoginHint("test@test.com");

            assertEquals("test@test.com", registerPage.getEmail());
        }
    }

    //KEYCLOAK-14161
    @Test
    public void customRegistrationPageFormTest() {
        String newFlowAlias = "register - custom";
        configureRegistrationFlowWithCustomRegistrationPageForm(newFlowAlias);

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();
    }

    protected RealmAttributeUpdater configureRealmRegistrationEmailAsUsername(final boolean value) {
        return getRealmAttributeUpdater().setRegistrationEmailAsUsername(value);
    }

    protected RealmAttributeUpdater setDuplicateEmailsAllowed(boolean allowed) {
        return getRealmAttributeUpdater().setDuplicateEmailsAllowed(allowed);
    }

    protected RealmAttributeUpdater setVerifyEmail(boolean value) {
        return getRealmAttributeUpdater().setVerifyEmail(value);
    }

    private RealmAttributeUpdater getRealmAttributeUpdater() {
        return new RealmAttributeUpdater(testRealm());
    }

    /**
     * Helper method for registering user with upper case email
     *
     * @param emailAsUsername is flag `Email as username` enabled
     * @return user ID
     */
    private String registerUpperCaseAndGetUserId(boolean emailAsUsername) {
        final String EMAIL = "TEST@localhost";
        final String USERNAME = "UPPERCASE";
        final String EMAIL_OR_USERNAME = emailAsUsername ? EMAIL : USERNAME;

        loginPage.open();
        loginPage.clickRegister();
        registerPage.assertCurrent();

        if (emailAsUsername) {
            registerPage.registerWithEmailAsUsername("firstName", "lastName", EMAIL, "password", "password");
        } else {
            registerPage.register("firstName", "lastName", EMAIL, USERNAME, "password", "password");
        }

        String userId = events.expectRegister(EMAIL_OR_USERNAME, EMAIL)
                .assertEvent()
                .getUserId();

        EventRepresentation loginEvent = events.expectLogin()
                .detail("username", EMAIL_OR_USERNAME.toLowerCase())
                .user(userId)
                .assertEvent();
        OAuthClient.AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken());
        assertUserBasicRegisterAttributes(userId, emailAsUsername ? null : USERNAME, EMAIL, "firstName", "lastName");

        return userId;
    }

    private void assertUserBasicRegisterAttributes(String userId, String username, String email, String firstName, String lastName) {
        UserRepresentation user = getUser(userId);
        assertThat(user, notNullValue());

        if (username != null) {
            assertThat(username.toLowerCase(), is(user.getUsername()));
        }
        assertThat(email.toLowerCase(), is(user.getEmail()));
        assertThat(firstName, is(user.getFirstName()));
        assertThat(lastName, is(user.getLastName()));
    }

    private void configureRegistrationFlowWithCustomRegistrationPageForm(String newFlowAlias) {
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session).copyRegistrationFlow(newFlowAlias));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                        .clear()
                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, CookieAuthenticatorFactory.PROVIDER_ID)
                        .addSubFlowExecution("Sub Flow", AuthenticationFlow.BASIC_FLOW, AuthenticationExecutionModel.Requirement.ALTERNATIVE, subflow -> subflow
                                .addSubFlowExecution("Sub sub Form Flow", AuthenticationFlow.FORM_FLOW, AuthenticationExecutionModel.Requirement.REQUIRED, subsubflow -> subsubflow
                                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, RegistrationUserCreation.PROVIDER_ID)
                                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, RegistrationProfile.PROVIDER_ID)
                                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, RegistrationPassword.PROVIDER_ID)
                                        .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.DISABLED, RegistrationRecaptcha.PROVIDER_ID)
                                )
                        )
                .defineAsRegistrationFlow() // Activate this new flow
        );
    }

}
