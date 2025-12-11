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

import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.pages.SelectOrganizationPage;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.firefox.FirefoxDriver;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author rmartinc
 */
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class PasskeysUsernamePasswordFormTest extends AbstractWebAuthnVirtualTest {

    @Page
    protected SelectOrganizationPage selectOrganizationPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        makePasswordlessRequiredActionDefault(realmRepresentation);
        switchExecutionInBrowserFormToProvider(realmRepresentation, UsernamePasswordFormFactory.PROVIDER_ID);

        configureTestRealm(realmRepresentation);
        testRealms.add(realmRepresentation);
    }

    @Override
    public boolean isPasswordless() {
        return true;
    }

    @Test
    public void webauthnLoginWithDiscoverableKey() throws Exception {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        // set passwordless policy for discoverable keys
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(null)
                .setWebAuthnPolicyUserVerificationRequirement(null)
                .setWebAuthnPolicyPasskeysEnabled(Boolean.TRUE)
                .update()) {

            checkWebAuthnConfiguration(Constants.WEBAUTHN_POLICY_OPTION_YES, Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();
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
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // invalid login first
            loginPage.login(USERNAME, "invalid-password");
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameInputError(), Matchers.is("Invalid username or password."));
            MatcherAssert.assertThat(loginPage.getPasswordInputError(), nullValue());
            events.expect(EventType.LOGIN_ERROR)
                    .detail(Details.USERNAME, USERNAME)
                    .error(Errors.INVALID_USER_CREDENTIALS)
                    .user(user.getId())
                    .assertEvent();

            // login OK now
            loginPage.login(USERNAME, getPassword(USERNAME));
            appPage.assertCurrent();
            events.expectLogin()
                    .user(user.getId())
                    .detail(Details.USERNAME, USERNAME)
                    .detail(Details.CREDENTIAL_TYPE, nullValue())
                    .assertEvent();

            logout();
        }
    }

    @Test
    public void passwordLoginWithExternalKey() throws Exception {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // set passwordless policy for discoverable keys
        try (Closeable c = setPasswordlessPolicyForExternalKey()) {

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


    // Test users is able to authenticate with passkey during re-authentication (for example when OIDC parameter prompt=login is used)
    @Test
    public void webauthnLoginWithExternalKey_reauthentication() throws Exception {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // set passwordless policy for discoverable keys
        try (Closeable c = setPasswordlessPolicyForExternalKey()) {

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

            // Re-authentication now with prompt=login. Passkeys login should be possible.
            oauth.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();
            WaitUtils.waitForPageToLoad();

            loginPage.assertCurrent();
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());
            assertEquals("Please re-authenticate to continue", loginPage.getInfoMessage());

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


    // Test user re-authentication with password when passkeys feature enabled, but passkeys is not enabled for the realm. Passkeys should not be shown during re-authentication
    @Test
    public void reauthenticationOfUserWithoutPasskey() throws Exception {
        // set passwordless policy for discoverable keys
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyPasskeysEnabled(Boolean.FALSE)
                .update()) {

            // Login with password
            oauth.openLoginForm();
            WaitUtils.waitForPageToLoad();

            // WebAuthn elements not available
            loginPage.assertCurrent();
            Assert.assertThrows(NoSuchElementException.class, () -> driver.findElement(By.xpath("//form[@id='webauth']")));

            // Login with password
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
            appPage.assertCurrent();

            events.clear();

            // Re-authentication now with prompt=login. Passkeys login should not be available on the page as this user does not have passkey
            oauth.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();
            WaitUtils.waitForPageToLoad();

            loginPage.assertCurrent();
            assertEquals("Please re-authenticate to continue", loginPage.getInfoMessage());
            Assert.assertThrows(NoSuchElementException.class, () -> driver.findElement(By.xpath("//form[@id='webauth']")));

            // Login with password
            loginPage.login(getPassword("test-user@localhost"));
            appPage.assertCurrent();

            UserRepresentation testUser = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost").toRepresentation();

            events.expectLogin()
                    .user(testUser.getId())
                    .detail(Details.USERNAME, testUser.getUsername())
                    .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, nullValue())
                    .assertEvent();

            logout();
        }
    }


    // Test user, which has both passkey and password, is able to re-authenticate with any of those. Also checks that re-authentication works after failed login (incorrect password)
    @Test
    public void webauthnLoginWithExternalKey_reauthenticationWithPasswordOrPasskey() throws Exception {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // set passwordless policy for discoverable keys
        try (Closeable c = setPasswordlessPolicyForExternalKey()) {

            checkWebAuthnConfiguration(Constants.WEBAUTHN_POLICY_OPTION_YES, Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();

            // open login page, the key is not internal so not opened by default
            oauth.openLoginForm();
            WaitUtils.waitForPageToLoad();

            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // force login using webauthn link
            webAuthnLoginPage.clickAuthenticate();
            appPage.assertCurrent();

            // Re-authentication now with prompt=login. Passkeys login should be possible.
            oauth.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();
            WaitUtils.waitForPageToLoad();

            loginPage.assertCurrent();
            assertEquals("Please re-authenticate to continue", loginPage.getInfoMessage());
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // incorrect password (password of different user)
            loginPage.login(getPassword("test-user@localhost"));
            Assert.assertEquals("Invalid password.", loginPage.getInputError());

            // Check that passkeys elements still available for this user
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            events.clear();

            // re-authenticate using passkey credential
            webAuthnLoginPage.clickAuthenticate();
            appPage.assertCurrent();

            // Successful event - passkey login
            events.expectLogin()
                    .user(user.getId())
                    .detail(Details.USERNAME, user.getUsername())
                    .detail(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true")
                    .assertEvent();

            // Re-authenticate again
            oauth.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();
            WaitUtils.waitForPageToLoad();

            // incorrect password (password of different user)
            loginPage.login(getPassword("test-user@localhost"));
            Assert.assertEquals("Invalid password.", loginPage.getInputError());

            events.clear();

            // re-authenticate using password now
            loginPage.login(getPassword(USERNAME));
            appPage.assertCurrent();

            // Succesful event - password login
            events.expectLogin()
                    .user(user.getId())
                    .detail(Details.USERNAME, user.getUsername())
                    .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, nullValue())
                    .assertEvent();

            logout();
        }
    }

    private Closeable setPasswordlessPolicyForExternalKey() {
        return getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                .setWebAuthnPolicyUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                .setWebAuthnPolicyPasskeysEnabled(Boolean.TRUE)
                .update();
    }

}
