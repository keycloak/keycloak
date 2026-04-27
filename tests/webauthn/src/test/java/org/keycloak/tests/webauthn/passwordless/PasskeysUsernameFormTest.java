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
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginUsernamePage;
import org.keycloak.testframework.ui.page.PasswordPage;
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
public class PasskeysUsernameFormTest extends AbstractWebAuthnVirtualTest {

    @InjectPage
    protected LoginUsernamePage loginPage;

    @InjectPage
    protected PasswordPage passwordPage;

    @Override
    protected void switchExecutionInBrowserFormToPasswordless() {
        managedRealm.updateWithCleanup(r -> r.browserFlow("passkeys-username"));
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
                    .webAuthnPolicyPasswordlessRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                    .webAuthnPolicyPasswordlessUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                    .webAuthnPolicyPasswordlessPasskeysEnabled(Boolean.TRUE)
                    .webAuthnPolicyPasswordlessMediation(mediation));
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
            Assertions.assertNotNull(passwordCredRep, "User has no password credential");
            userResource().removeCredential(passwordCredRep.getId());

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
            loginPage.fillLoginWithUsernameOnly("invalid-user");
            loginPage.submit();
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(loginPage.getUsernameInputError(), Matchers.is("Invalid username or email."));
            EventAssertion.assertError(events.poll())
                    .type(EventType.LOGIN_ERROR)
                    .isCodeId()
                    .error(Errors.USER_NOT_FOUND)
                    .details(Details.USERNAME, "invalid-user");

            // login OK now
            loginPage.fillLoginWithUsernameOnly(USERNAME);
            loginPage.submit();
            passwordPage.assertCurrent();
            // Passkeys available on password-form as well. Allows to login only with the passkey of current user
            MatcherAssert.assertThat(loginPage.getAttemptedUsername(), Matchers.is(USERNAME));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());
            passwordPage.fillPassword(PASSWORD);
            passwordPage.submit();

            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, USERNAME)
                    .withoutDetails(Details.CREDENTIAL_TYPE);
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
        }
    }

    // Test that user is able to authenticate with passkeys even during re-authentication (For example when OIDC parameter prompt=login is used)
    @Test
    public void webauthnLoginWithDiscoverableKey_reauthentication() {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

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

            // remove the password, so passkeys are the only credential in the user
            final CredentialRepresentation passwordCredRep = userResource().credentials().stream()
                    .filter(cred -> PasswordCredentialModel.TYPE.equals(cred.getType()))
                    .findAny()
                    .orElse(null);
            Assertions.assertNotNull(passwordCredRep, "User has no password credential");
            userResource().removeCredential(passwordCredRep.getId());

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

            // Re-authentication now with prompt=login. Passkeys login should be possible.
            oAuthClient.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();

            passwordPage.assertCurrent();
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());
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

    @Test
    public void passwordLogin_reauthenticationOfUserWithoutPasskey() {
        // use a default resident key which is not shown in conditional UI
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // set passwordless policy for discoverable keys
        {
            managedRealm.updateWithCleanup(r -> r.webAuthnPolicyPasswordlessRpEntityName("localhost")
                    .webAuthnPolicyPasswordlessRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                    .webAuthnPolicyPasswordlessUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                    .webAuthnPolicyPasswordlessPasskeysEnabled(Boolean.TRUE));

            // Login with password
            oAuthClient.openLoginForm();

            // WebAuthn elements available, user is not yet known. Password not available as on username-form
            loginPage.assertCurrent();
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

            // Login with password. WebAuthn elements not available on password screen as user does not have passkeys
            loginPage.fillLoginWithUsernameOnly("test-user@localhost");
            loginPage.submit();
            passwordPage.assertCurrent();
            Assertions.assertThrows(NoSuchElementException.class, () -> driver.findElement(By.xpath("//form[@id='webauth']")));
            passwordPage.fillPassword(PASSWORD);
            passwordPage.submit();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            events.clear();

            // Re-authentication now with prompt=login. Passkeys login should not be available on the page as this user does not have passkey
            oAuthClient.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();

            passwordPage.assertCurrent();
            Assertions.assertEquals("Please re-authenticate to continue", passwordPage.getInfoMessage().orElse(null));
            Assertions.assertThrows(NoSuchElementException.class, () -> driver.findElement(By.xpath("//form[@id='webauth']")));

            // Incorrect password (password of different user)
            passwordPage.fillPassword("incorrect");
            passwordPage.submit();
            MatcherAssert.assertThat(passwordPage.getPasswordError(), Matchers.is("Invalid password."));

            events.clear();

            // Login with password
            passwordPage.fillPassword(PASSWORD);
            passwordPage.submit();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            UserRepresentation testUser = AdminApiUtil.findUserByUsername(managedRealm.admin(), "test-user@localhost");

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

    @Test
    public void passwordLoginWithExternalKeyAndRememberMeLoginAtUsername() {
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

    @Test
    public void passwordLoginWithExternalKeyAndRememberMeLoginAtPassword() {
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

            // force login using webauthn link at password
            loginPage.fillLoginWithUsernameOnly(USERNAME);
            loginPage.rememberMe(true);
            loginPage.submit();

            // login at password using webauthn
            passwordPage.assertCurrent();
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());
            EventAssertion loginEvent = EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, USERNAME)
                    .details(Details.REMEMBER_ME, "true")
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true");

            // clear the session and check remember me is present
            managedRealm.admin().deleteSession(loginEvent.getEvent().getSessionId(), false);
            oAuthClient.openLoginForm();
            loginPage.assertCurrent();
            Assertions.assertEquals(USERNAME, loginPage.getUsername());
            Assertions.assertTrue(loginPage.isRememberMe());

            // uncheck remember me and process normally using webauthn at password page
            loginPage.fillLoginWithUsernameOnly(USERNAME);
            loginPage.rememberMe(false);
            loginPage.submit();
            passwordPage.assertCurrent();
            webAuthnLoginPage.clickAuthenticate();
            Assertions.assertNotNull(oAuthClient.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .hasSessionId()
                    .userId(user.getId())
                    .isCodeId()
                    .details(Details.USERNAME, USERNAME)
                    .details(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .details(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true")
                    .withoutDetails(Details.REMEMBER_ME);
        }
    }
}
