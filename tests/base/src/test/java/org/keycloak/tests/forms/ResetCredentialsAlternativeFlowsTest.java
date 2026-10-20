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

package org.keycloak.tests.forms;

import java.net.URI;
import java.util.Arrays;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestCleanup;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.PasswordPage;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.runonserver.RunHelpers;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test for the various alternatives of reset-credentials flow or browser flow (non-default setup of the  flows)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 * @author <a href="mailto:jlieskov@redhat.com">Jan Lieskovsky</a>
 */
@KeycloakIntegrationTest
public class ResetCredentialsAlternativeFlowsTest extends AbstractResetCredentialsAlternativeFlowsTest {

    @InjectUser(ref="differentUser", config = ResetCredentialsAlternativeFlowsTest.DifferentUser.class)
    ManagedUser differentUser;

    @InjectMailServer
    MailServer mail;

    @InjectPage
    protected LoginUsernamePage loginUsernameOnlyPage;

    @InjectPage
    protected PasswordPage passwordPage;

    @InjectPage
    protected LoginPasswordUpdatePage updatePasswordPage;

    @InjectPage
    protected ErrorPage errorPage;

    @TestSetup
    public void setup() {
        runOnServer.run(RunHelpers.configureBrowserFlowWithAlternativeCredentials());
    }

    @TestCleanup
    public void cleanUp() {
        runOnServer.run(session -> {
            AuthenticationFlowModel alternativeBrowserFlow = session.getContext().getRealm().getBrowserFlow();
            FlowUtil.inCurrentRealm(session).selectFlow(DefaultAuthenticationFlows.BROWSER_FLOW).defineAsBrowserFlow();
            session.getContext().getRealm().removeAuthenticationFlow(alternativeBrowserFlow);
        });
    }

    @Override
    protected String getAiaAction() {
        return null; // noop
    }

    // Test with default reset-credentials flow and alternative browser flow with separate username and password screen.
    //
    // Provide username and click "Forget password" on browser flow. Then provide non-existing username in reset-credentials 1st screen.
    // User should be cleared from authentication context and no email should be sent
    @Test
    public void testNotExistingUserProvidedInResetCredentialsFlow() {
        // Provide username and then click "Forget password"
        provideUsernameAndClickResetPassword(managedUser.getUsername());

        // Provide non-existent username after "login-test" user already set in the context by browser flow
        resetPasswordPage.changePassword("non-existent");

        loginUsernameOnlyPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginUsernameOnlyPage.getSuccessMessage().orElse(null));

        // Assert no email was sent as user was cleared
        assertEquals(0, mail.getReceivedMessages().length);
    }


    // Test with default reset-credentials flow and alternative browser flow with separate username and password screen.
    //
    // Provide username and click "Forget password" on browser flow. Then provide different username in reset-credentials 1st screen than provided earlier
    // on browser flow username screen. There should be an error and no email should be sent
    @Test
    public void testDifferentUserProvidedInResetCredentialsFlow() {
        // Provide username and then click "Forget password"
        provideUsernameAndClickResetPassword(differentUser.getUsername());

        // Provide existing username "test-user@localhost" for different user than "login-test", which was set earlier by browser flow
        resetPasswordPage.changePassword(managedUser.getUsername());

        // Should be on error page
        errorPage.assertCurrent();

        // Assert no email was sent
        assertEquals(0, mail.getReceivedMessages().length);
    }


    // Test with default reset-credentials flow and alternative browser flow with separate username and password screen.
    //
    // Provide username and click "Forget password" on browser flow. Then provide same username in reset-credentials 1st screen than provided earlier
    // on browser flow username screen. There should be an email successfully sent.
    @Test
    public void testSameUserProvidedInResetCredentialsFlow() {
        // Provide username and then click "Forget password"
        provideUsernameAndClickResetPassword(managedUser.getUsername());

        // Provide same username "login-test" as earlier in browser flow
        resetPasswordPage.changePassword(managedUser.getUsername());

        loginUsernameOnlyPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginUsernameOnlyPage.getSuccessMessage().orElse(null));

        // Assert email was sent
        assertEquals(1, mail.getReceivedMessages().length);
    }


    // Test with alternative reset-credentials flow with removed ResetCredentialChooseUser authenticator and with alternative browser
    // flow with separate username and password screen.
    //
    // Provide username and click "Forget password" on browser flow. Then provide same username in reset-credentials 1st screen than provided earlier
    // on browser flow username screen. There should be an email successfully sent.
    @Test
    public void testResetCredentialsFlowWithUsernameProvidedFromBrowserFlow() throws Exception {
        try {
            final String newFlowAlias = "resetcred - alternative";
            // Configure reset-credentials flow without ResetCredentialsChooseUser authenticator
            configureResetCredentialsRemoveExecutionsAndBindTheFlow(
                    newFlowAlias,
                    Arrays.asList("reset-credentials-choose-user")
            );

            // provides username
            oauth.openLoginForm();
            loginUsernameOnlyPage.fillLoginWithUsernameOnly(managedUser.getUsername());
            loginUsernameOnlyPage.submit();

            passwordPage.assertCurrent();

            // Click "Forget password"
            passwordPage.clickResetPassword();

            // Should be directly back on the loginPage with the message about sent email
            loginUsernameOnlyPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginUsernameOnlyPage.getSuccessMessage().orElse(null));

            // Assert email was sent
            assertEquals(1, mail.getReceivedMessages().length);

            // Successfully reset password
            MimeMessage message = mail.getLastReceivedMessage();

            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);

            driver.open(changePasswordUrl.trim());

            updatePasswordPage.assertCurrent();
            updatePasswordPage.changePassword("resetPassword", "resetPassword");

            // Assert user authenticated
            assertThat(driver.driver().getPageSource(), Matchers.containsString("kc-info-message"));
            assertThat(driver.driver().getPageSource(), Matchers.containsString("Your account has been updated."));
        } finally {
            revertResetCredentialsFlows();
        }
    }

    private void provideUsernameAndClickResetPassword(String username) {
        // provides username
        oauth.openLoginForm();
        loginUsernameOnlyPage.fillLoginWithUsernameOnly(username);
        loginUsernameOnlyPage.submit();

        passwordPage.assertCurrent();

        // Click "Forget password"
        passwordPage.clickResetPassword();

        // Assert switched to the "reset-credentials" flow, but button "back" not available
        resetPasswordPage.assertCurrent();
        Assertions.assertTrue(URI.create(driver.getCurrentUrl()).getPath().endsWith("/login-actions/reset-credentials"));
    }

    private static class DifferentUser implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user
                    .username("login-test")
                    .email("login@test.com")
                    .password("password")
                    .name("Jane", "Doe")
                    .enabled(true);

        }
    }
}
