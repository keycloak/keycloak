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

package org.keycloak.testsuite.forms;

import java.util.Arrays;
import java.util.List;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.models.UserManager;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAuthenticationTest;
import org.keycloak.testsuite.actions.AbstractAppInitiatedActionTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.LoginUsernameOnlyPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.PasswordPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.util.AccountHelper;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Test for the various alternatives of reset-credentials flow or browser flow (non-default setup of the  flows)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:jlieskov@redhat.com">Jan Lieskovsky</a>
 */
public class ResetCredentialsAlternativeFlowsTest extends AbstractAppInitiatedActionTest {

    private String userId;
    private String password;

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginUsernameOnlyPage loginUsernameOnlyPage;

    @Page
    protected PasswordPage passwordPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Page
    protected LoginConfigTotpPage totpPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected AppPage appPage;

    protected TimeBasedOTP totp = new TimeBasedOTP();

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void setup() {
        log.info("Adding login-test user");
        UserRepresentation user = UserBuilder.create()
                .username("login-test")
                .email("login@test.com")
                .enabled(true)
                .build();

        password = generatePassword();
        userId = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, password);
        getCleanup().addUserId(userId);
    }

    @Override
    public String getAiaAction() {
        return UserModel.RequiredAction.CONFIGURE_TOTP.name();
    }


    // Test with default reset-credentials flow and alternative browser flow with separate username and password screen.
    //
    // Provide username and click "Forget password" on browser flow. Then provide non-existing username in reset-credentials 1st screen.
    // User should be cleared from authentication context and no email should be sent
    @Test
    public void testNotExistingUserProvidedInResetCredentialsFlow() {
        try {
            MultiFactorAuthenticationTest.configureBrowserFlowWithAlternativeCredentials(testingClient);

            // Provide username and then click "Forget password"
            provideUsernameAndClickResetPassword("login-test");

            // Provide non-existent username after "login-test" user already set in the context by browser flow
            resetPasswordPage.changePassword("non-existent");

            loginUsernameOnlyPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginUsernameOnlyPage.getSuccessMessage());

            // Assert no email was sent as user was cleared
            assertEquals(0, greenMail.getReceivedMessages().length);

        } finally {
            revertFlows();
        }
    }


    // Test with default reset-credentials flow and alternative browser flow with separate username and password screen.
    //
    // Provide username and click "Forget password" on browser flow. Then provide different username in reset-credentials 1st screen than provided earlier
    // on browser flow username screen. There should be an error and no email should be sent
    @Test
    public void testDifferentUserProvidedInResetCredentialsFlow() {
        try {
            MultiFactorAuthenticationTest.configureBrowserFlowWithAlternativeCredentials(testingClient);

            // Provide username and then click "Forget password"
            provideUsernameAndClickResetPassword("login-test");

            // Provide existing username "test-user@localhost" for different user than "login-test", which was set earlier by browser flow
            resetPasswordPage.changePassword("test-user@localhost");

            // Should be on error page
            errorPage.assertCurrent();

            // Assert no email was sent
            assertEquals(0, greenMail.getReceivedMessages().length);
        } finally {
            revertFlows();
        }
    }


    // Test with default reset-credentials flow and alternative browser flow with separate username and password screen.
    //
    // Provide username and click "Forget password" on browser flow. Then provide same username in reset-credentials 1st screen than provided earlier
    // on browser flow username screen. There should be an email successfully sent.
    @Test
    public void testSameUserProvidedInResetCredentialsFlow() {
        try {
            MultiFactorAuthenticationTest.configureBrowserFlowWithAlternativeCredentials(testingClient);

            // Provide username and then click "Forget password"
            provideUsernameAndClickResetPassword("login-test");

            // Provide same username "login-test" as earlier in browser flow
            resetPasswordPage.changePassword("login-test");

            loginUsernameOnlyPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginUsernameOnlyPage.getSuccessMessage());

            // Assert email was sent
            assertEquals(1, greenMail.getReceivedMessages().length);
        } finally {
            revertFlows();
        }
    }


    // Test with alternative reset-credentials flow with removed ResetCredentialChooseUser authenticator and with alternative browser
    // flow with separate username and password screen.
    //
    // Provide username and click "Forget password" on browser flow. Then provide same username in reset-credentials 1st screen than provided earlier
    // on browser flow username screen. There should be an email successfully sent.
    @Test
    public void testResetCredentialsFlowWithUsernameProvidedFromBrowserFlow() throws Exception {
        try {
            MultiFactorAuthenticationTest.configureBrowserFlowWithAlternativeCredentials(testingClient);
            final String newFlowAlias = "resetcred - alternative";
            // Configure reset-credentials flow without ResetCredentialsChooseUser authenticator
            configureResetCredentialsRemoveExecutionsAndBindTheFlow(
                    newFlowAlias,
                    Arrays.asList("reset-credentials-choose-user")
            );

            // provides username
            loginUsernameOnlyPage.open();
            loginUsernameOnlyPage.login("login-test");

            passwordPage.assertCurrent();

            // Click "Forget password"
            passwordPage.clickResetPassword();

            // Should be directly back on the loginPage with the message about sent email
            loginUsernameOnlyPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginUsernameOnlyPage.getSuccessMessage());

            // Assert email was sent
            assertEquals(1, greenMail.getReceivedMessages().length);

            // Successfully reset password
            MimeMessage message = greenMail.getReceivedMessages()[0];

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            driver.navigate().to(changePasswordUrl.trim());

            updatePasswordPage.assertCurrent();
            updatePasswordPage.changePassword("resetPassword", "resetPassword");

            // Assert user authenticated
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.parseLoginResponse().getCode());
        } finally {
            revertFlows();
        }
    }


    private void provideUsernameAndClickResetPassword(String username) {
        // provides username
        loginUsernameOnlyPage.open();
        loginUsernameOnlyPage.login(username);

        passwordPage.assertCurrent();

        // Click "Forget password"
        passwordPage.clickResetPassword();

        // Assert switched to the "reset-credentials" flow, but button "back" not available
        resetPasswordPage.assertCurrent();
        Assert.assertTrue(URLUtils.currentUrlMatches("/login-actions/reset-credentials"));
    }


    private void revertFlows() {
        List<AuthenticationFlowRepresentation> flows = testRealm().flows().getFlows();

        // Set default flows
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setBrowserFlow(DefaultAuthenticationFlows.BROWSER_FLOW);
        realm.setResetCredentialsFlow(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW);
        testRealm().update(realm);

        // Delete flows previously created within various tests
        final List<String> aliasesOfExistingFlows = Arrays.asList(
                "browser - alternative",
                "resetcred - alternative",
                "resetcred - KEYCLOAK-11753 - test"
        );

        for(String existingFlowAlias : aliasesOfExistingFlows) {
            AuthenticationFlowRepresentation flowRepresentation = AbstractAuthenticationTest.findFlowByAlias(existingFlowAlias, flows);
            if (flowRepresentation != null) {
                testRealm().flows().deleteFlow(flowRepresentation.getId());
            }
        }
    }


    // Create a copy of the default reset credentials flow with the specified flow alias if it doesn't exist yet
    // Remove execution(s), specified by (a list of) providerId(s) from the flow
    // Finally bind / define the flow as the reset credential one
    private void configureResetCredentialsRemoveExecutionsAndBindTheFlow(String newFlowAlias, List<String> providerIdsToRemove) {
        testingClient.server("test").run(session -> {
            // Create a copy of the default reset credentials flow with the specified flow alias if it doesn't exist yet
            if(session.getContext().getRealm().getFlowByAlias(newFlowAlias) == null) {
                FlowUtil.inCurrentRealm(session).copyResetCredentialsFlow(newFlowAlias);
            }
        });

        for(String providerId : providerIdsToRemove) {
            // For each execution to be removed its index within the flow based on providerId
            int executionIndex = realmsResouce().realm("test").flows().getExecutions(newFlowAlias)
                    .stream()
                    .filter(e -> e.getProviderId().equals(providerId))
                    .mapToInt(e -> e.getIndex())
                    .findFirst()
                    .getAsInt();

            // Remove the execution(s)
            testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                    .selectFlow(newFlowAlias)
                    .removeExecution(executionIndex)
            );
        }

        // Bind the flow as the reset-credentials one
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow(newFlowAlias)
                .defineAsResetCredentialsFlow()
        );
    }


    @Test
    public void resetCredentialsVerifyCustomOtpLabelSetProperly() {
        try {
            // Make a copy of the default Reset Credentials flow, but:
            // * Without 'Send Reset Email' authenticator,
            // * Without 'Reset Password' authenticator
            final String newFlowAlias = "resetcred - KEYCLOAK-11753 - test";
            configureResetCredentialsRemoveExecutionsAndBindTheFlow(
                    newFlowAlias,
                    Arrays.asList("reset-credential-email", "reset-password")
            );

            // Login & set up the initial OTP code for the user
            loginPage.open();
            loginPage.login("login-test", password);
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);

            String customOtpLabel = "my-original-otp-label";

            // Setup OTP
            doAIA();
            totpPage.assertCurrent();
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), customOtpLabel);
            assertKcActionStatus(SUCCESS);

            // Logout
            oauth.logoutForm().idTokenHint(response.getIdToken()).open();

            // Go to login page & click "Forgot password" link to perform the custom 'Reset Credential' flow
            loginPage.open();
            loginPage.resetPassword();

            // Should be on reset password page now. Provide email of the user & click Submit button
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("login-test");

            // Provide updated form of the OTP label, to be used within 'Reset OTP' (next) step
            customOtpLabel = "my-reset-otp-label";

            // Reset OTP label to a custom value as part of Reset Credentials flow
            AccountHelper.updateTotpUserLabel(testRealm(), "login-test", customOtpLabel);

            // Open OTP Authenticator account page
            // Check if OTP credential is present
            Assert.assertTrue(AccountHelper.totpUserLabelComparator(testRealm(), "login-test", customOtpLabel));

        // Undo setup changes performed within the test
        } finally {
            revertFlows();
        }
    }


    // KEYCLOAK-12168 Verify the 'Device Name' label is optional for the first OTP credential created
    // (either via Account page or by registering new user), but required for each next created OTP credential
    @Test
    public void deviceNameOptionalForFirstOTPCredentialButRequiredForEachNextOne() {
        // Enable 'Default Action' on 'Configure OTP' RA for the 'test' realm
        RequiredActionProviderRepresentation otpRequiredAction = testRealm().flows().getRequiredAction("CONFIGURE_TOTP");
        otpRequiredAction.setDefaultAction(true);
        testRealm().flows().updateRequiredAction("CONFIGURE_TOTP", otpRequiredAction);

        try {
            // Make a copy of the default Reset Credentials flow, but:
            // * Without 'Send Reset Email' authenticator,
            // * Without 'Reset Password' authenticator
            final String newFlowAlias = "resetcred - KEYCLOAK-12168 - firstOTP - account - test";
            configureResetCredentialsRemoveExecutionsAndBindTheFlow(
                    newFlowAlias,
                    Arrays.asList("reset-credential-email", "reset-password")
            );

            /* Verify the 'Device Name' is optional when creating new OTP credential via the Account page */

            // Login & set up the initial OTP code for the user
            loginPage.open();
            loginPage.login("login@test.com", password);

            // Create OTP credential with empty label
            final String emptyOtpLabel = "";

            // Setup OTP
            doAIA();
            totpPage.assertCurrent();
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), emptyOtpLabel);
            assertKcActionStatus(SUCCESS);

            Assert.assertTrue(AccountHelper.deleteTotpAuthentication(testRealm(), "login-test"));

            // Logout
            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            /* Verify the 'Device Name' is optional when creating the first OTP credential via the login config TOTP page */

            // Register new user
            loginPage.open();
            loginPage.clickRegister();
            registerPage.assertCurrent();

            registerPage.register("Bruce", "Wilson", "bwilson@keycloak.org", "bwilson", generatePassword());
            totpPage.assertCurrent();

            // Create OTP credential with empty label

            // Setup OTP
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), "");

            // Assert user authenticated
            appPage.assertCurrent();
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.parseLoginResponse().getCode());

            Assert.assertTrue(AccountHelper.isTotpPresent(testRealm(), "bwilson"));
            Assert.assertTrue(AccountHelper.totpUserLabelComparator(testRealm(), "bwilson", ""));

            // Logout
            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            /* Verify the 'Device Name' is required for each next OTP credential created via the login config TOTP page */

            // Click "Forgot password" to define another OTP credential
            loginPage.open();
            loginPage.resetPassword();

            // Should be on reset password page now. Provide email of previously registered user & click Submit button
            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("bwilson@keycloak.org");

            // Try to create another OTP credential with empty label again. This
            // should fail with error since OTP label is required in this case already
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), "");
            Assert.assertTrue(AccountHelper.totpCountEquals(testRealm(), "bwilson", 1));
            // Create 2nd OTP credential with valid (non-empty) Device Name label. This should pass
            final String secondOtpLabel = "My 2nd OTP device";
            totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), secondOtpLabel);

            // Assert user authenticated
            appPage.assertCurrent();
            Assert.assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.parseLoginResponse().getCode());

            // Verify 2nd OTP credential was successfully created too
            Assert.assertTrue(AccountHelper.totpUserLabelComparator(testRealm(), "bwilson", secondOtpLabel));

            // Remove both OTP credentials
            Assert.assertTrue(AccountHelper.deleteTotpAuthentication(testRealm(), "bwilson"));
            Assert.assertTrue(AccountHelper.deleteTotpAuthentication(testRealm(), "bwilson"));

            // Logout
            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

        // Undo setup changes performed within the test
        } finally {
            revertFlows();
            // Disable 'Default Action' on 'Configure OTP' RA for the 'test' realm
            otpRequiredAction.setDefaultAction(false);
            testRealm().flows().updateRequiredAction("CONFIGURE_TOTP", otpRequiredAction);
            // Remove the within test registered 'bwilson' user
            testingClient.server("test").run(session -> {
                UserManager um = new UserManager(session);
                UserModel user = session.users().getUserByUsername(session.getContext().getRealm(), "bwilson");
                if (user != null) {
                    um.removeUser(session.getContext().getRealm(), user);
                }
            });
        }
    }
}
