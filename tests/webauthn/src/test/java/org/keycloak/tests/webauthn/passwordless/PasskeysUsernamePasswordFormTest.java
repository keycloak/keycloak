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

package org.keycloak.tests.webauthn.passwordless;

import org.keycloak.WebAuthnConstants;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.tests.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.tests.webauthn.authenticators.DefaultVirtualAuthOptions;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;

/**
 *
 * @author rmartinc
 */
public class PasskeysUsernamePasswordFormTest extends AbstractWebAuthnVirtualTest {

    @Override
    protected void switchExecutionInBrowserFormToPasswordless() {
        managedRealm.updateWithCleanup(r -> r.browserFlow(DefaultAuthenticationFlows.BROWSER_FLOW));
        UserRepresentation user = AdminApiUtil.findUserByUsername(managedRealm.admin(), USERNAME);
        if (user != null) {
            managedRealm.admin().users().delete(user.getId());
        }
    }

    @Override
    public boolean isPasswordless() {
        return true;
    }

    @ParameterizedTest
    @ValueSource(strings = {"conditional", "optional"})
    public void webauthnLoginWithDiscoverableKey(String mediation) {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        // set passwordless policy for discoverable keys
        {
            managedRealm.updateWithCleanup(r -> r.webAuthnPolicyPasswordlessRpEntityName("localhost")
                    .webAuthnPolicyPasswordlessRequireResidentKey(null)
                    .webAuthnPolicyPasswordlessUserVerificationRequirement(null)
                    .webAuthnPolicyPasswordlessPasskeysEnabled(Boolean.TRUE)
                    .webAuthnPolicyPasswordlessMediation(mediation));

            checkWebAuthnConfiguration(Constants.WEBAUTHN_POLICY_OPTION_YES, Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();
            events.clear();

            // the user should be automatically logged in using the discoverable key
            oAuthClient.openLoginForm();

            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, user.getUsername())
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true");

            logout();
        }
    }

    @Test
    public void passwordLoginWithNonDiscoverableKey() {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        // set passwordless policy not specified, key will not be discoverable
        {
            managedRealm.updateWithCleanup(r -> r.webAuthnPolicyPasswordlessRpEntityName("localhost")
                    .webAuthnPolicyPasswordlessRequireResidentKey(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED)
                    .webAuthnPolicyPasswordlessUserVerificationRequirement(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED)
                    .webAuthnPolicyPasswordlessPasskeysEnabled(Boolean.TRUE));

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();

            events.clear();

            // login should be done manually but webauthn is enabled
            oAuthClient.openLoginForm();
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // invalid login first
            loginPage.fillLogin(USERNAME, "invalid-password");
            loginPage.submit();
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameInputError(), Matchers.is("Invalid username or password."));
            Assertions.assertTrue(loginPage.getPasswordInputError().isEmpty());
            EventAssertion.assertError(events.poll())
                    .type(EventType.LOGIN_ERROR)
                    .isCodeId()
                    .userId(user.getId())
                    .details(Details.USERNAME, USERNAME)
                    .error(Errors.INVALID_USER_CREDENTIALS);

            // login OK now
            loginPage.fillLogin(USERNAME, PASSWORD);
            loginPage.submit();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, USERNAME)
                    .withoutDetails(Details.CREDENTIAL_TYPE);

            logout();
        }
    }

    @Test
    public void passwordLoginWithExternalKey() {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // set passwordless policy for discoverable keys
        {
            managedRealm.updateWithCleanup(r -> r.webAuthnPolicyPasswordlessRpEntityName("localhost")
                    .webAuthnPolicyPasswordlessRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                    .webAuthnPolicyPasswordlessUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                    .webAuthnPolicyPasswordlessPasskeysEnabled(Boolean.TRUE));

            checkWebAuthnConfiguration(Constants.WEBAUTHN_POLICY_OPTION_YES, Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();
            events.clear();

            // open login page, the key is not internal so not opened by default
            oAuthClient.openLoginForm();

            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // force login using webauthn link
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, user.getUsername())
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true");

            logout();
            events.clear();

            // set authenticatorAttachment to platform with modal mediation;
            // no platform authenticator is available so the modal must not be shown
            managedRealm.updateWithCleanup(r -> r.webAuthnPolicyPasswordlessAuthenticatorAttachment("platform")
                    .webAuthnPolicyPasswordlessMediation("optional"));

            oAuthClient.openLoginForm();

            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // no modal was shown, force login using webauthn link
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, user.getUsername())
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true");

            logout();
        }
    }

    // Test users is able to authenticate with passkey during re-authentication (for example when OIDC parameter prompt=login is used)
    @Test
    public void webauthnLoginWithExternalKey_reauthentication() {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // set passwordless policy for discoverable keys
        {
            managedRealm.updateWithCleanup(r -> r.webAuthnPolicyPasswordlessRpEntityName("localhost")
                    .webAuthnPolicyPasswordlessRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                    .webAuthnPolicyPasswordlessUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                    .webAuthnPolicyPasswordlessPasskeysEnabled(Boolean.TRUE));

            checkWebAuthnConfiguration(Constants.WEBAUTHN_POLICY_OPTION_YES, Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();
            events.clear();

            // open login page, the key is not internal so not opened by default
            oAuthClient.openLoginForm();

            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // force login using webauthn link
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, user.getUsername())
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true");

            // Re-authentication now with prompt=login. Passkeys login should be possible.
            oAuthClient.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();

            loginPage.assertCurrent();
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());
            Assertions.assertEquals("Please re-authenticate to continue", loginPage.getInfoMessage().orElse(null));

            // force login using webauthn link
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, user.getUsername())
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true");

            logout();
        }
    }

    // Test user re-authentication with password when passkeys feature enabled, but passkeys is not enabled for the realm. Passkeys should not be shown during re-authentication
    @Test
    public void reauthenticationOfUserWithoutPasskey() {
        // set passwordless policy for discoverable keys
        {
            managedRealm.updateWithCleanup(r -> r.webAuthnPolicyPasswordlessPasskeysEnabled(Boolean.FALSE));

            // Login with password
            oAuthClient.openLoginForm();

            // WebAuthn elements not available
            loginPage.assertCurrent();
            Assertions.assertThrows(NoSuchElementException.class, () -> driver.findElement(By.xpath("//form[@id='webauth']")));

            // Login with password
            loginPage.fillLogin("test-user@localhost", PASSWORD);
            loginPage.submit();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            events.clear();

            // Re-authentication now with prompt=login. Passkeys login should not be available on the page as this user does not have passkey
            oAuthClient.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();

            loginPage.assertCurrent();
            Assertions.assertEquals("Please re-authenticate to continue", loginPage.getInfoMessage().orElse(null));
            Assertions.assertThrows(NoSuchElementException.class, () -> driver.findElement(By.xpath("//form[@id='webauth']")));

            // Login with password
            loginPage.fillPassword(PASSWORD);
            loginPage.submit();

            UserRepresentation testUser = AdminApiUtil.findUserByUsername(managedRealm.admin(), "test-user@localhost");

            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(testUser.getId())
                    .isCodeId()
                    .details(Details.USERNAME, testUser.getUsername())
                    .withoutDetails(Details.CREDENTIAL_TYPE, WebAuthnConstants.USER_VERIFICATION_CHECKED);

            logout();
        }
    }

    // Test user, which has both passkey and password, is able to re-authenticate with any of those. Also checks that re-authentication works after failed login (incorrect password)
    @Test
    public void webauthnLoginWithExternalKey_reauthenticationWithPasswordOrPasskey() {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // set passwordless policy for discoverable keys
        {
            managedRealm.updateWithCleanup(r -> r.webAuthnPolicyPasswordlessRpEntityName("localhost")
                    .webAuthnPolicyPasswordlessRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                    .webAuthnPolicyPasswordlessUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                    .webAuthnPolicyPasswordlessPasskeysEnabled(Boolean.TRUE));

            checkWebAuthnConfiguration(Constants.WEBAUTHN_POLICY_OPTION_YES, Constants.WEBAUTHN_POLICY_OPTION_REQUIRED);

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();

            // open login page, the key is not internal so not opened by default
            oAuthClient.openLoginForm();

            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // force login using webauthn link
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            // Re-authentication now with prompt=login. Passkeys login should be possible.
            oAuthClient.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();

            loginPage.assertCurrent();
            Assertions.assertEquals("Please re-authenticate to continue", loginPage.getInfoMessage().orElse(null));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // incorrect password (password of different user)
            loginPage.fillPassword("invalid-password");
            loginPage.submit();
            Assertions.assertEquals("Invalid username or password.", loginPage.getPasswordInputError().orElse(null));

            // Check that passkeys elements still available for this user
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            events.clear();

            // re-authenticate using passkey credential
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            // Successful event - passkey login
            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, user.getUsername())
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true");

            // Re-authenticate again
            oAuthClient.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();

            // incorrect password (password of different user)
            loginPage.fillPassword("invalid-password");
            loginPage.submit();
            Assertions.assertEquals("Invalid username or password.", loginPage.getPasswordInputError().orElse(null));

            events.clear();

            // re-authenticate using password now
            loginPage.fillPassword(PASSWORD);
            loginPage.submit();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            // Succesful event - password login
            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, user.getUsername())
                    .withoutDetails(Details.CREDENTIAL_TYPE, WebAuthnConstants.USER_VERIFICATION_CHECKED);

            logout();
        }
    }

    @Test
    public void passwordLoginWithExternalKeyAndRememberMe() {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // set passwordless policy for discoverable keys and enable remember me
        {
            managedRealm.updateWithCleanup(r -> r.webAuthnPolicyPasswordlessRpEntityName("localhost")
                    .webAuthnPolicyPasswordlessRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                    .webAuthnPolicyPasswordlessUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                    .webAuthnPolicyPasswordlessPasskeysEnabled(Boolean.TRUE)
                    .setRememberMe(Boolean.TRUE));

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();

            events.clear();

            // login should be done manually but webauthn is enabled
            oAuthClient.openLoginForm();
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // force login using webauthn link
            loginPage.rememberMe(true);
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion loginEvent = EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, user.getUsername())
                    .details(Details.REMEMBER_ME, "true")
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true");

            // clear the session and check remember me is present
            managedRealm.admin().deleteSession(loginEvent.getEvent().getSessionId(), false);
            oAuthClient.openLoginForm();
            loginPage.assertCurrent();
            Assertions.assertEquals(user.getUsername(), loginPage.getUsername());
            Assertions.assertTrue(loginPage.isRememberMe());

            // uncheck remember me and process normally
            loginPage.rememberMe(false);
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, user.getUsername())
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true")
                    .withoutDetails(Details.REMEMBER_ME);
        }
    }
}
