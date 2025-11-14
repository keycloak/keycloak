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
import java.util.List;

import org.keycloak.WebAuthnConstants;
import org.keycloak.events.Details;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.webauthn.AbstractWebAuthnVirtualTest;
import org.keycloak.testsuite.webauthn.authenticators.DefaultVirtualAuthOptions;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.firefox.FirefoxDriver;

import static org.hamcrest.Matchers.nullValue;

/**
 *
 * @author rmartinc
 */
@IgnoreBrowserDriver(FirefoxDriver.class) // See https://github.com/keycloak/keycloak/issues/10368
public class PasskeysDefaultBrowserFlowTest extends AbstractWebAuthnVirtualTest {

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    private LoginConfigTotpPage loginConfigTotpPage;

    @Page
    protected OneTimeCode oneTimeCodePage;

    private final TimeBasedOTP totp = new TimeBasedOTP();

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/webauthn/testrealm-webauthn.json"), RealmRepresentation.class);

        // remove any flow to use defalt flows created by the realm
        realmRepresentation.setAuthenticationFlows(null);
        realmRepresentation.setBrowserFlow(null);
        makePasswordlessRequiredActionDefault(realmRepresentation);

        // configure otp to allow reusable
        realmRepresentation.setOtpPolicyAlgorithm("HmacSHA1");
        realmRepresentation.setOtpPolicyDigits(6);
        realmRepresentation.setOtpPolicyInitialCounter(0);
        realmRepresentation.setOtpPolicyLookAheadWindow(1);
        realmRepresentation.setOtpPolicyPeriod(30);
        realmRepresentation.setOtpPolicyType("totp");
        realmRepresentation.setOtpPolicyCodeReusable(Boolean.TRUE);

        configureTestRealm(realmRepresentation);
        testRealms.add(realmRepresentation);
    }

    @Override
    public boolean isPasswordless() {
        return true;
    }

    @Test
    public void testLoginWithExternalKey() throws Exception {
        // use a external key to not use conditional UI automatically
        getVirtualAuthManager().useAuthenticator(DefaultVirtualAuthOptions.DEFAULT_RESIDENT_KEY.getOptions());

        // enable passkeys in the policy
        try (Closeable c = getWebAuthnRealmUpdater()
                .setWebAuthnPolicyRpEntityName("localhost")
                .setWebAuthnPolicyRequireResidentKey(Constants.WEBAUTHN_POLICY_OPTION_YES)
                .setWebAuthnPolicyUserVerificationRequirement(Constants.WEBAUTHN_POLICY_OPTION_REQUIRED)
                .setWebAuthnPolicyPasskeysEnabled(Boolean.TRUE)
                .update()) {

            registerDefaultUser();
            UserRepresentation user = userResource().toRepresentation();
            MatcherAssert.assertThat(user, Matchers.notNullValue());
            logout();

            // test without otp
            testLoginUsingPasskey(user);
            testLoginUsingPassword(user);

            // configure otp for the user
            final String totpSecret = configureOtp(user);

            // test with otp enabled
            testLoginUsingPasskey(user);
            testLoginUsingPasswordAndOtp(user, totpSecret);
        }
    }

    private String configureOtp(UserRepresentation user) {
        user.setRequiredActions(List.of(UserModel.RequiredAction.CONFIGURE_TOTP.name()));
        userResource().update(user);

        oauth.openLoginForm();
        WaitUtils.waitForPageToLoad();
        loginPage.assertCurrent();
        webAuthnLoginPage.clickAuthenticate();
        loginConfigTotpPage.assertCurrent();
        final String totpSecret = loginConfigTotpPage.getTotpSecret();
        loginConfigTotpPage.configure(totp.generateTOTP(totpSecret), "totp");

        appPage.assertCurrent();

        logout();
        return totpSecret;
    }

    private void testLoginUsingPasskey(UserRepresentation user) {
        events.clear();

        oauth.openLoginForm();
        WaitUtils.waitForPageToLoad();

        // login page with passkeys activated
        loginPage.assertCurrent();
        MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
        MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

        // force login using webauthn link
        webAuthnLoginPage.clickAuthenticate();
        appPage.assertCurrent();

        // expect success login
        events.expectLogin()
                .user(user.getId())
                .detail(Details.USERNAME, user.getUsername())
                .detail(Details.CREDENTIAL_TYPE, WebAuthnCredentialModel.TYPE_PASSWORDLESS)
                .detail(WebAuthnConstants.USER_VERIFICATION_CHECKED, "true")
                .assertEvent();

        logout();
    }

    private void testLoginUsingPassword(UserRepresentation user) {
        events.clear();

        oauth.openLoginForm();
        WaitUtils.waitForPageToLoad();
        loginPage.assertCurrent();
        MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
        MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

        // login using password
        loginPage.login(USERNAME, getPassword(USERNAME));
        appPage.assertCurrent();
        events.expectLogin()
                .user(user.getId())
                .detail(Details.USERNAME, USERNAME)
                .detail(Details.CREDENTIAL_TYPE, nullValue())
                .assertEvent();

        logout();
    }

    private void testLoginUsingPasswordAndOtp(UserRepresentation user, String totpSecret) {
        events.clear();

        oauth.openLoginForm();
        WaitUtils.waitForPageToLoad();
        loginPage.assertCurrent();
        MatcherAssert.assertThat(loginPage.getUsernameAutocomplete(), Matchers.is("username webauthn"));
        MatcherAssert.assertThat(driver.findElement(By.xpath("//form[@id='webauth']")), Matchers.notNullValue());

        // login using password
        loginPage.login(USERNAME, getPassword(USERNAME));
        loginTotpPage.assertCurrent();

        // login using otp
        oneTimeCodePage.sendCode(new TimeBasedOTP().generateTOTP(totpSecret));
        appPage.assertCurrent();

        events.expectLogin()
                .user(user.getId())
                .detail(Details.USERNAME, USERNAME)
                .detail(Details.CREDENTIAL_TYPE, nullValue())
                .assertEvent();

        logout();
    }
}
