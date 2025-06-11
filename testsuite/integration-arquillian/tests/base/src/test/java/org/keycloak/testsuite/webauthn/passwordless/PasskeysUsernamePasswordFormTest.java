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
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.WebAuthnConstants;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.OrganizationDomainRepresentation;
import org.keycloak.representations.idm.OrganizationRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.pages.SelectOrganizationPage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.utils.PropertyRequirement;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 *
 * @author rmartinc
 */
@EnableFeature(value = Profile.Feature.PASSKEYS, skipRestart = true)
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class PasskeysUsernamePasswordFormTest extends AbstractWebAuthnVirtualTest {

    @Page
    protected SelectOrganizationPage selectOrganizationPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        makePasswordlessRequiredActionDefault(realmRepresentation);
        switchExecutionInBrowserFormToProvider(realmRepresentation, UsernamePasswordFormFactory.PROVIDER_ID);

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
                .setWebAuthnPolicyRequireResidentKey(PropertyRequirement.YES.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(WebAuthnConstants.OPTION_REQUIRED)
                .setWebAuthnPolicyPasskeysEnabled(Boolean.TRUE)
                .update()) {

            checkWebAuthnConfiguration(PropertyRequirement.YES.getValue(), WebAuthnConstants.OPTION_REQUIRED);

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
                .setWebAuthnPolicyRequireResidentKey(PropertyRequirement.NOT_SPECIFIED.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(WebAuthnConstants.OPTION_NOT_SPECIFIED)
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
            MatcherAssert.assertThat(loginPage.getPasswordInputError(), Matchers.nullValue());
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
                .setWebAuthnPolicyRequireResidentKey(PropertyRequirement.YES.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(WebAuthnConstants.OPTION_REQUIRED)
                .setWebAuthnPolicyPasskeysEnabled(Boolean.TRUE)
                .update()) {

            checkWebAuthnConfiguration(PropertyRequirement.YES.getValue(), WebAuthnConstants.OPTION_REQUIRED);

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

    @Test
    public void organizationLoginWithDiscoverableKey() throws Exception {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        // set passwordless policy for discoverable keys
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(PropertyRequirement.YES.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(WebAuthnConstants.OPTION_REQUIRED)
                .setWebAuthnPolicyPasskeysEnabled(Boolean.TRUE)
                .update();
             Closeable realmUpdate = new RealmAttributeUpdater(testRealm())
                .setOrganizationsEnabled(true)
                .update()) {

            OrganizationRepresentation orgRep = createRepresentation("testOrg", "email");
            testRealm().organizations().create(orgRep);

            checkWebAuthnConfiguration(PropertyRequirement.YES.getValue(), WebAuthnConstants.OPTION_REQUIRED);

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();
            events.clear();

            // login using the organization
            oauth.openLoginForm();
            WaitUtils.waitForPageToLoad();
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.isPasswordInputPresent(), Matchers.is(false));
            loginPage.loginUsername(USERNAME);

            // now the passkeys username password page should be presented with username selected and passkeys disabled
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getAttemptedUsername(), Matchers.is("userwebauthn"));
            Assert.assertThrows(NoSuchElementException.class, () -> driver.findElement(By.xpath("//form[@id='webauth']")));
            loginPage.login("invalid-password");
            loginPage.assertCurrent();
            MatcherAssert.assertThat(loginPage.getPasswordInputError(), Matchers.is("Invalid password."));
            events.expect(EventType.LOGIN_ERROR)
                    .error(Errors.INVALID_USER_CREDENTIALS)
                    .user(user.getId())
                    .assertEvent();

            // correct login now
            MatcherAssert.assertThat(loginPage.getAttemptedUsername(), Matchers.is("userwebauthn"));
            Assert.assertThrows(NoSuchElementException.class, () -> driver.findElement(By.xpath("//form[@id='webauth']")));
            loginPage.login(getPassword(USERNAME));
            appPage.assertCurrent();
            events.expectLogin()
                    .user(user.getId())
                    .detail(Details.USERNAME, "userwebauthn")
                    .detail(Details.CREDENTIAL_TYPE, Matchers.nullValue())
                    .assertEvent();
        }
    }

    private OrganizationRepresentation createRepresentation(String name, String... orgDomains) {
        OrganizationRepresentation org = new OrganizationRepresentation();
        org.setName(name);
        org.setAlias(name);
        org.setDescription(name + " is a test organization!");

        for (String orgDomain : orgDomains) {
            OrganizationDomainRepresentation domainRep = new OrganizationDomainRepresentation();
            domainRep.setName(orgDomain);
            org.addDomain(domainRep);
        }

        return org;
    }

}
