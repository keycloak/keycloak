/*
 * Copyright 2025 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.webauthn.passwordless;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 *
 * @author rmartinc
 */
@EnableFeature(value = Profile.Feature.PASSKEYS, skipRestart = true)
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class PasskeysUsernameFormTest extends AbstractWebAuthnVirtualTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        makePasswordlessRequiredActionDefault(realmRepresentation);
        switchExecutionInBrowser(realmRepresentation);

        testRealms.add(realmRepresentation);
    }

    private void switchExecutionInBrowser(RealmRepresentation realm) {
        List<AuthenticationFlowRepresentation> flows = realm.getAuthenticationFlows();
        MatcherAssert.assertThat(flows, Matchers.notNullValue());

        AuthenticationFlowRepresentation browserForm = flows.stream()
                .filter(f -> f.getAlias().equals("browser-webauthn-forms"))
                .findFirst()
                .orElse(null);
        MatcherAssert.assertThat("Cannot find 'browser-webauthn-forms' flow", browserForm, Matchers.notNullValue());

        flows.removeIf(f -> f.getAlias().equals(browserForm.getAlias()));

        // set first the username form authenticator
        AuthenticationExecutionExportRepresentation usernameForm = new AuthenticationExecutionExportRepresentation();
        usernameForm.setAuthenticator(UsernameFormFactory.PROVIDER_ID);
        usernameForm.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        usernameForm.setPriority(10);
        usernameForm.setAuthenticatorFlow(false);
        usernameForm.setUserSetupAllowed(false);

        // second the password form
        AuthenticationExecutionExportRepresentation passwordForm = new AuthenticationExecutionExportRepresentation();
        passwordForm.setAuthenticator(PasswordFormFactory.PROVIDER_ID);
        passwordForm.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        passwordForm.setPriority(20);
        passwordForm.setAuthenticatorFlow(false);
        passwordForm.setUserSetupAllowed(false);

        browserForm.setAuthenticationExecutions(List.of(usernameForm, passwordForm));
        flows.add(browserForm);

        realm.setAuthenticationFlows(flows);
    }

    @Override
    public boolean isPasswordless() {
        return true;
    }

    @Test
    public void webauthnLoginWithDiscoverableKey() throws IOException {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        // set passwordless policy for discoverable keys
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                .setWebAuthnPolicyUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                .setWebAuthnPolicyPasskeysEnabled(Boolean.TRUE)
                .update()) {

            checkWebAuthnConfiguration(Constants.WEBAUTHN_POLICY_OPTION_YES, Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();

            // remove the password, so passkeys are the only credential in the user
            final CredentialRepresentation passwordCredRep = userResource().credentials().stream()
                    .filter(cred -> PasswordCredentialModel.TYPE.equals(cred.getType()))
                    .findAny()
                    .orElse(null);
            Assert.assertNotNull("User has no password credential", passwordCredRep);
            userResource().removeCredential(passwordCredRep.getId());

            events.clear();

            // the user should be automatically logged in using the discoverable key
            oauth.openLoginForm();
            WaitUtils.waitForPageToLoad();

            appPage.assertCurrent();

            events.expectLogin()
                    .user(user.getId())
                    .detail(Details.USERNAME, user.getUsername())
                    .detail(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true")
                    .assertEvent();

            logout();
        }
    }

    @Test
    public void passwordLoginWithNonDiscoverableKey() throws IOException {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        // set passwordless policy not specified, key will not be discoverable
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED)
                .setWebAuthnPolicyUserVerificationRequirement(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED)
                .setWebAuthnPolicyPasskeysEnabled(Boolean.TRUE)
                .update()) {
            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();

            events.clear();

            // login should be done manually but webauthn is enabled
            oauth.openLoginForm();
            WaitUtils.waitForPageToLoad();
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(loginPage.isPasswordInputPresent(), Matchers.is(false));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // invalid login first
            loginPage.loginUsername("invalid-user");
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(loginPage.getUsernameInputError(), Matchers.is("Invalid username or email."));
            events.expect(EventType.LOGIN_ERROR)
                    .detail(Details.USERNAME, "invalid-user")
                    .error(Errors.USER_NOT_FOUND)
                    .user(Matchers.blankOrNullString())
                    .assertEvent();

            // login OK now
            loginPage.loginUsername(USERNAME);
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getAttemptedUsername(), Matchers.is(USERNAME));
            Assert.assertThrows(NoSuchElementException.class, () -> driver.findElement(By.xpath("//form[@id='webauth']")));
            loginPage.login(getPassword(USERNAME));
            appPage.assertCurrent();
            events.expectLogin()
                    .user(user.getId())
                    .detail(Details.USERNAME, USERNAME)
                    .detail(Details.CREDENTIAL_TYPE, Matchers.nullValue())
                    .assertEvent();
        }
    }

    @Test
    public void passwordLoginWithExternalKey() throws Exception {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // set passwordless policy for discoverable keys
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                .setWebAuthnPolicyUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                .setWebAuthnPolicyPasskeysEnabled(Boolean.TRUE)
                .update()) {

            checkWebAuthnConfiguration(Constants.WEBAUTHN_POLICY_OPTION_YES, Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();
            events.clear();

            // open login page, the key is not internal so not opened by default
            oauth.openLoginForm();
            WaitUtils.waitForPageToLoad();

            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(loginPage.isPasswordInputPresent(), Matchers.is(false));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // force login using webauthn link
            webAuthnLoginPage.clickAuthenticate();
            appPage.assertCurrent();

            events.expectLogin()
                    .user(user.getId())
                    .detail(Details.USERNAME, user.getUsername())
                    .detail(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true")
                    .assertEvent();
            logout();
        }
    }
}
