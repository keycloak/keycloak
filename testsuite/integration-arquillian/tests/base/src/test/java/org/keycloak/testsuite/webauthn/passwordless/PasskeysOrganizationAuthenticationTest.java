/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.organization.authentication.authenticators.browser.OrganizationAuthenticatorFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.AuthenticatorConfigRepresentation;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.openqa.selenium.By;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 *
 * @author rmartinc
 */
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class PasskeysOrganizationAuthenticationTest extends AbstractWebAuthnVirtualTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        makePasswordlessRequiredActionDefault(realmRepresentation);
        switchExecutionInBrowserFormToProvider(realmRepresentation, UsernamePasswordFormFactory.PROVIDER_ID);
        realmRepresentation.setOrganizationsEnabled(Boolean.TRUE);

        OrganizationRepresentation emailOrg = new OrganizationRepresentation();
        emailOrg.setName("email");
        emailOrg.setAlias("email");
        OrganizationDomainRepresentation domainRep = new OrganizationDomainRepresentation();
        domainRep.setName("email");
        emailOrg.addDomain(domainRep);
        realmRepresentation.addOrganization(emailOrg);

        testRealms.add(realmRepresentation);
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
    public void webauthnLoginWithDiscoverableKeyRequiresMembership() throws IOException {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        // enable organization configuration
        AuthenticationExecutionInfoRepresentation organizationExec = testRealm().flows().getExecutions("browser-webauthn-conditional-organization").stream()
                .filter(exec -> OrganizationAuthenticatorFactory.ID.equals(exec.getProviderId()))
                .findAny()
                .orElse(null);
        Assert.assertNotNull("Organization execution not found", organizationExec);

        AuthenticatorConfigRepresentation config = new AuthenticatorConfigRepresentation();
        config.setAlias(KeycloakModelUtils.generateId());
        config.getConfig().put(OrganizationAuthenticatorFactory.REQUIRES_USER_MEMBERSHIP, Boolean.TRUE.toString());
        getCleanup().addAuthenticationConfigId(ApiUtil.getCreatedId(testRealm().flows().newExecutionConfig(organizationExec.getId(), config)));

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

            // the user should be automatically logged in using the discoverable key but error because no org
            oauth.openLoginForm();
            WaitUtils.waitForPageToLoad();

            errorPage.assertCurrent();
            MatcherAssert.assertThat(errorPage.getError(), Matchers.containsString("User is not a member of the organization"));
        }
    }

    @Test
    public void passwordLoginWithNonDiscoverableKey() throws Exception {
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

            // access login page, key is not discoverable so webauthn should be enabled but login should be manual
            oauth.openLoginForm();
            WaitUtils.waitForPageToLoad();
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
            MatcherAssert.assertThat(loginPage.isPasswordInputPresent(), Matchers.is(false));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());
            loginPage.loginUsername(USERNAME);

            // now the passkeys username password page should be presented with username selected. Passkeys still enabled
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getAttemptedUsername(), Matchers.is("userwebauthn"));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());
            loginPage.login("invalid-password");
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getPasswordInputError(), Matchers.is("Invalid password."));
            events.expect(EventType.LOGIN_ERROR)
                    .error(Errors.INVALID_USER_CREDENTIALS)
                    .user(user.getId())
                    .assertEvent();

            // correct login now
            MatcherAssert.assertThat(loginPage.getAttemptedUsername(), Matchers.is("userwebauthn"));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());
            loginPage.login(getPassword(USERNAME));
            appPage.assertCurrent();
            events.expectLogin()
                    .user(user.getId())
                    .detail(Details.USERNAME, "userwebauthn")
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

    // Test users is able to authenticate with passkey during re-authentication (for example when OIDC parameter prompt=login is used)
    @Test
    public void webauthnLoginWithDiscoverableKey_reauthentication() throws IOException {
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

            // Re-authentication now with prompt=login. Passkeys login should be possible.
            oauth.loginForm()
                    .prompt(OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                    .open();
            WaitUtils.waitForPageToLoad();

            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.isPasswordInputPresent(), Matchers.is(true));
            MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());
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
