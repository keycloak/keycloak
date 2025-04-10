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
import org.keycloak.authentication.authenticators.browser.PasskeysConditionalUIAuthenticatorFactory;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.representations.idm.AuthenticationExecutionExportRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.pages.PageUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;
import org.keycloak.testsuite.webauthn.utils.PropertyRequirement;
import org.keycloak.testsuite.webauthn.utils.WebAuthnRealmData;
import org.openqa.selenium.firefox.FirefoxDriver;

/**
 *
 * @author rmartinc
 */
@EnableFeature(value = Profile.Feature.PASSKEYS, skipRestart = true)
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class PasskeysConditionalUITest extends AbstractWebAuthnVirtualTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        makePasswordlessRequiredActionDefault(realmRepresentation);
        switchExecutionInBrowserFormToPasskeysConditionalUI(realmRepresentation);

        testRealms.add(realmRepresentation);
        configureTestRealm(realmRepresentation);
    }

    @Override
    public boolean isPasswordless() {
        return true;
    }

    /**
     * Creates the webauthn flow with the passkeys conditional UI authenticator.
     * @param realm The realm representation
     */
    protected void switchExecutionInBrowserFormToPasskeysConditionalUI(RealmRepresentation realm) {
        List<AuthenticationFlowRepresentation> flows = realm.getAuthenticationFlows();
        MatcherAssert.assertThat(flows, Matchers.notNullValue());

        AuthenticationFlowRepresentation browserForm = flows.stream()
                .filter(f -> f.getAlias().equals("browser-webauthn-forms"))
                .findFirst()
                .orElse(null);
        MatcherAssert.assertThat("Cannot find 'browser-webauthn-forms' flow", browserForm, Matchers.notNullValue());

        flows.removeIf(f -> f.getAlias().equals(browserForm.getAlias()));

        // set just one authenticator with the passkeys conditional UI
        AuthenticationExecutionExportRepresentation passkeysConditionalUI = new AuthenticationExecutionExportRepresentation();
        passkeysConditionalUI.setAuthenticator(PasskeysConditionalUIAuthenticatorFactory.PROVIDER_ID);
        passkeysConditionalUI.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
        passkeysConditionalUI.setPriority(10);
        passkeysConditionalUI.setAuthenticatorFlow(false);
        passkeysConditionalUI.setUserSetupAllowed(false);

        browserForm.setAuthenticationExecutions(List.of(passkeysConditionalUI));
        flows.add(browserForm);

        realm.setAuthenticationFlows(flows);
    }

    @Test
    public void successLoginWithDiscoverableKey() throws IOException {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        // set passwordless policy for discoverable keys
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(PropertyRequirement.YES.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(WebAuthnConstants.OPTION_REQUIRED)
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            MatcherAssert.assertThat(realmData, Matchers.notNullValue());
            MatcherAssert.assertThat(realmData.getRpEntityName(), Matchers.is("localhost"));
            MatcherAssert.assertThat(realmData.getRequireResidentKey(), Matchers.is(PropertyRequirement.YES.getValue()));
            MatcherAssert.assertThat(realmData.getUserVerificationRequirement(), Matchers.is(WebAuthnConstants.OPTION_REQUIRED));

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
                    .detail(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                    .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true")
                    .assertEvent();
        }
    }

    @Test
    public void failureWithNonDiscoverableKey() throws IOException {
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.PASSKEYS.getOptions());

        // set passwordless policy not specified, key will not be discoverable
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(PropertyRequirement.NOT_SPECIFIED.getValue())
                .setWebAuthnPolicyUserVerificationRequirement(WebAuthnConstants.OPTION_NOT_SPECIFIED)
                .update()) {

            WebAuthnRealmData realmData = new WebAuthnRealmData(testRealm().toRepresentation(), isPasswordless());
            MatcherAssert.assertThat(realmData, Matchers.notNullValue());
            MatcherAssert.assertThat(realmData.getRpEntityName(), Matchers.is("localhost"));
            MatcherAssert.assertThat(realmData.getRequireResidentKey(), Matchers.is(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));
            MatcherAssert.assertThat(realmData.getUserVerificationRequirement(), Matchers.is(Constants.DEFAULT_WEBAUTHN_POLICY_NOT_SPECIFIED));

            registerDefaultUser();

            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());

            logout();

            events.clear();

            // the key is not discoverable, therefore the login should not be done automatically
            oauth.openLoginForm();
            WaitUtils.waitForPageToLoad();
            loginPage.assertCurrent();
            MatcherAssert.assertThat(PageUtils.getPageTitle(driver), Matchers.is("Passkey login"));
        }
    }
}
