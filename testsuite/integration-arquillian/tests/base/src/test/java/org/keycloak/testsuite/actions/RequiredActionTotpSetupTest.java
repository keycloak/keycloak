/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.actions;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.pages.AccountTotpPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LanguageComboboxAwarePage;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginTotpPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.By;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
public class RequiredActionTotpSetupTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        RequiredActionProviderRepresentation requiredAction = new RequiredActionProviderRepresentation();
        requiredAction.setAlias(UserModel.RequiredAction.CONFIGURE_TOTP.name());
        requiredAction.setProviderId(UserModel.RequiredAction.CONFIGURE_TOTP.name());
        requiredAction.setName("Configure Totp");
        requiredAction.setEnabled(true);
        requiredAction.setDefaultAction(true);

        List<RequiredActionProviderRepresentation> requiredActions = new LinkedList<>();
        requiredActions.add(requiredAction);
        testRealm.setRequiredActions(requiredActions);
        testRealm.setResetPasswordAllowed(Boolean.TRUE);
    }

    @Before
    public void setOTPAuthRequired() {

        adminClient.realm("test").flows().getExecutions("browser").
                stream().filter(execution -> execution.getDisplayName().equals("Browser - Conditional OTP"))
                .forEach(execution ->
                {execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED.name());
                adminClient.realm("test").flows().updateExecutions("browser", execution);});

        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost")
                .firstName("Tom")
                .lastName("Brady")
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name()).build();
        ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
    }


    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginTotpPage loginTotpPage;

    @Page
    protected LoginConfigTotpPage totpPage;

    @Page
    protected AccountTotpPage accountTotpPage;

    @Page
    protected RegisterPage registerPage;

    protected TimeBasedOTP totp = new TimeBasedOTP();

    @Test
    public void setupTotpRegister() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName", "lastName", "email@mail.com", "setupTotp", "password", "password");

        String userId = events.expectRegister("setupTotp", "email@mail.com").assertEvent().getUserId();

        assertTrue(totpPage.isCurrent());
        assertFalse(totpPage.isCancelDisplayed());

        // assert attempted-username not shown when setup TOTP
        LanguageComboboxAwarePage.assertAttemptedUsernameAvailability(driver, false);

        // KEYCLOAK-11753 - Verify OTP label element present on "Configure OTP" required action form
        driver.findElement(By.id("userLabel"));

        totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()));

        String authSessionId = events.expectRequiredAction(EventType.UPDATE_TOTP).user(userId).detail(Details.USERNAME, "setuptotp").assertEvent()
                .getDetails().get(Details.CODE_ID);

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).session(authSessionId).detail(Details.USERNAME, "setuptotp").assertEvent();
    }

    @Test
    public void setupTotpRegisterManual() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName", "lastName", "checkQrCode@mail.com", "checkQrCode", "password", "password");

        String pageSource = driver.getPageSource();

        assertTrue(pageSource.contains("Install one of the following applications on your mobile"));
        assertTrue(pageSource.contains("FreeOTP"));
        assertTrue(pageSource.contains("Google Authenticator"));

        assertTrue(pageSource.contains("Open the application and scan the barcode"));
        assertFalse(pageSource.contains("Open the application and enter the key"));

        assertTrue(pageSource.contains("Unable to scan?"));
        assertFalse(pageSource.contains("Scan barcode?"));

        totpPage.clickManual();

        pageSource = driver.getPageSource();

        assertTrue(pageSource.contains("Install one of the following applications on your mobile"));
        assertTrue(pageSource.contains("FreeOTP"));
        assertTrue(pageSource.contains("Google Authenticator"));

        assertFalse(pageSource.contains("Open the application and scan the barcode"));
        assertTrue(pageSource.contains("Open the application and enter the key"));

        assertFalse(pageSource.contains("Unable to scan?"));
        assertTrue(pageSource.contains("Scan barcode?"));

        assertTrue(driver.findElement(By.id("kc-totp-secret-key")).getText().matches("[\\w]{4}( [\\w]{4}){7}"));

        assertEquals("Type: Time-based", driver.findElement(By.id("kc-totp-type")).getText());
        assertEquals("Algorithm: SHA1", driver.findElement(By.id("kc-totp-algorithm")).getText());
        assertEquals("Digits: 6", driver.findElement(By.id("kc-totp-digits")).getText());
        assertEquals("Interval: 30", driver.findElement(By.id("kc-totp-period")).getText());

        totpPage.clickBarcode();

        pageSource = driver.getPageSource();

        assertTrue(pageSource.contains("Install one of the following applications on your mobile"));
        assertTrue(pageSource.contains("FreeOTP"));
        assertTrue(pageSource.contains("Google Authenticator"));

        assertTrue(pageSource.contains("Open the application and scan the barcode"));
        assertFalse(pageSource.contains("Open the application and enter the key"));

        assertTrue(pageSource.contains("Unable to scan?"));
        assertFalse(pageSource.contains("Scan barcode?"));

        // KEYCLOAK-11753 - Verify OTP label element present on "Configure OTP" required action form
        driver.findElement(By.id("userLabel"));
    }

    // KEYCLOAK-7081
    @Test
    public void setupTotpRegisterManualModeSwitchesOnBadSubmit() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName", "lastName", "setupTotpRegisterManualModeSwitchesOnBadSubmit@mail.com", "setupTotpRegisterManualModeSwitchesOnBadSubmit", "password", "password");

        String pageSource = driver.getPageSource();

        assertTrue(pageSource.contains("Unable to scan?"));
        assertFalse(pageSource.contains("Scan barcode?"));

        totpPage.clickManual();

        pageSource = driver.getPageSource();

        assertFalse(pageSource.contains("Unable to scan?"));
        assertTrue(pageSource.contains("Scan barcode?"));

        totpPage.submit();

        pageSource = driver.getPageSource();

        assertFalse(pageSource.contains("Unable to scan?"));
        assertTrue(pageSource.contains("Scan barcode?"));

        assertEquals("Please specify authenticator code.", totpPage.getInputCodeError());
    }

    // KEYCLOAK-7081
    @Test
    public void setupTotpRegisterBarcodeModeSwitchesOnBadSubmit() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName", "lastName", "setupTotpRegisterBarcodeModeSwitchesOnBadSubmit@mail.com", "setupTotpRegisterBarcodeModeSwitchesOnBadSubmit", "password", "password");

        String pageSource = driver.getPageSource();

        assertTrue(pageSource.contains("Unable to scan?"));
        assertFalse(pageSource.contains("Scan barcode?"));

        totpPage.submit();

        pageSource = driver.getPageSource();

        assertTrue(pageSource.contains("Unable to scan?"));
        assertFalse(pageSource.contains("Scan barcode?"));

        assertEquals("Please specify authenticator code.", totpPage.getInputCodeError());

        totpPage.clickManual();

        pageSource = driver.getPageSource();

        assertFalse(pageSource.contains("Unable to scan?"));
        assertTrue(pageSource.contains("Scan barcode?"));
    }

    @Test
    public void setupTotpRegisterVerifyCustomOtpLabelSetProperly() {
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName", "lastName", "setupTotpRegister@mail.com", "setupTotpRegister", "password", "password");

        String userId = events.expectRegister("setupTotpRegister", "setupTotpRegister@mail.com").assertEvent().getUserId();

        assertTrue(totpPage.isCurrent());

        // KEYCLOAK-11753 - Verify OTP label element present on "Configure OTP" required action form
        driver.findElement(By.id("userLabel"));

        String customOtpLabel = "my-custom-otp-label";

        // Set OTP label to a custom value
        totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), customOtpLabel);

        // Open account page & verify OTP authenticator with requested label was created
        accountTotpPage.open();
        accountTotpPage.assertCurrent();

        String pageSource = driver.getPageSource();
        assertTrue(pageSource.contains(customOtpLabel));
    }

    @Test
    public void setupTotpModifiedPolicy() {
        RealmResource realm = testRealm();
        RealmRepresentation rep = realm.toRepresentation();
        rep.setOtpPolicyDigits(8);
        rep.setOtpPolicyType("hotp");
        rep.setOtpPolicyAlgorithm("HmacSHA256");
        realm.update(rep);
        try {
            loginPage.open();
            loginPage.clickRegister();
            registerPage.register("firstName", "lastName", "setupTotpModifiedPolicy@mail.com", "setupTotpModifiedPolicy", "password", "password");

            String pageSource = driver.getPageSource();

            assertTrue(pageSource.contains("FreeOTP"));
            assertFalse(pageSource.contains("Google Authenticator"));

            totpPage.clickManual();

            assertEquals("Type: Counter-based", driver.findElement(By.id("kc-totp-type")).getText());
            assertEquals("Algorithm: SHA256", driver.findElement(By.id("kc-totp-algorithm")).getText());
            assertEquals("Digits: 8", driver.findElement(By.id("kc-totp-digits")).getText());
            assertEquals("Counter: 0", driver.findElement(By.id("kc-totp-counter")).getText());
        } finally {
            rep.setOtpPolicyDigits(6);
            rep.setOtpPolicyType("totp");
            rep.setOtpPolicyAlgorithm("HmacSHA1");
            realm.update(rep);
        }
    }

    @Test
    public void setupTotpExisting() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        totpPage.assertCurrent();

        String totpSecret = totpPage.getTotpSecret();

        totpPage.configure(totp.generateTOTP(totpSecret));

        String authSessionId = events.expectRequiredAction(EventType.UPDATE_TOTP).assertEvent()
                .getDetails().get(Details.CODE_ID);

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().session(authSessionId).assertEvent();

        OAuthClient.AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();

        events.expectLogout(authSessionId).assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        String src = driver.getPageSource();
        loginTotpPage.login(totp.generateTOTP(totpSecret));

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();
    }

    //KEYCLOAK-15511
    @Test
    public void setupTotpEnforcedBySessionNotForUserInGeneral() {
        String username = "test-user@localhost";
        String configureTotp = UserModel.RequiredAction.CONFIGURE_TOTP.name();

        // Remove required action from the user
        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), username);
        UserRepresentation userRepresentation = user.toRepresentation();
        userRepresentation.getRequiredActions().remove(configureTotp);
        user.update(userRepresentation);

        // login
        loginPage.open();
        loginPage.login(username, "password");

        // ensure TOTP configuration is enforced for current authentication session
        totpPage.assertCurrent();

        // ensure TOTP configuration it is not enforced for the user in general
        userRepresentation = user.toRepresentation();
        assertFalse(userRepresentation.getRequiredActions().contains(configureTotp));
    }

    @Test
    public void setupTotpRegisteredAfterTotpRemoval() {
        // Register new user
        loginPage.open();
        loginPage.clickRegister();
        registerPage.register("firstName2", "lastName2", "email2@mail.com", "setupTotp2", "password2", "password2");

        String userId = events.expectRegister("setupTotp2", "email2@mail.com").assertEvent().getUserId();

        // Configure totp
        totpPage.assertCurrent();

        String totpCode = totpPage.getTotpSecret();
        totpPage.configure(totp.generateTOTP(totpCode));

        // After totp config, user should be on the app page
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectRequiredAction(EventType.UPDATE_TOTP).user(userId).detail(Details.USERNAME, "setuptotp2").assertEvent();

        EventRepresentation loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, "setuptotp2").assertEvent();

        // Logout
        OAuthClient.AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();
        events.expectLogout(loginEvent.getSessionId()).user(userId).assertEvent();

        // Try to login after logout
        loginPage.open();
        loginPage.login("setupTotp2", "password2");

        // Totp is already configured, thus one-time password is needed, login page should be loaded
        String uri = driver.getCurrentUrl();
        String src = driver.getPageSource();
        assertTrue(loginPage.isCurrent());
        Assert.assertFalse(totpPage.isCurrent());

        // Login with one-time password
        loginTotpPage.login(totp.generateTOTP(totpCode));

        loginEvent = events.expectLogin().user(userId).detail(Details.USERNAME, "setupTotp2").assertEvent();

        // Open account page
        accountTotpPage.open();
        accountTotpPage.assertCurrent();

        // Remove google authentificator
        accountTotpPage.removeTotp();

        events.expectAccount(EventType.REMOVE_TOTP).user(userId).assertEvent();

        // Logout
        accountTotpPage.logout();
        events.expectLogout(loginEvent.getSessionId()).user(userId).detail(Details.REDIRECT_URI, oauth.AUTH_SERVER_ROOT + "/realms/test/account/totp").assertEvent();

        // Try to login
        loginPage.open();
        loginPage.login("setupTotp2", "password2");

        // Since the authentificator was removed, it has to be set up again
        totpPage.assertCurrent();
        totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()));

        String sessionId = events.expectRequiredAction(EventType.UPDATE_TOTP).user(userId).detail(Details.USERNAME, "setupTotp2").assertEvent()
                .getDetails().get(Details.CODE_ID);

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().user(userId).session(sessionId).detail(Details.USERNAME, "setupTotp2").assertEvent();
    }

    @Test
    public void setupOtpPolicyChangedTotp8Digits() {
        // set policy to 8 digits
        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        RealmBuilder.edit(realmRep)
                    .otpLookAheadWindow(1)
                    .otpDigits(8)
                    .otpPeriod(30)
                    .otpType(OTPCredentialModel.TOTP)
                    .otpAlgorithm(HmacOTP.HMAC_SHA1)
                    .otpInitialCounter(0);
        adminClient.realm("test").update(realmRep);


        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        totpPage.assertCurrent();

        String totpSecret = totpPage.getTotpSecret();

        TimeBasedOTP timeBased = new TimeBasedOTP(HmacOTP.HMAC_SHA1, 8, 30, 1);
        totpPage.configure(timeBased.generateTOTP(totpSecret));

        String sessionId = events.expectRequiredAction(EventType.UPDATE_TOTP).assertEvent()
                .getDetails().get(Details.CODE_ID);

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().session(sessionId).assertEvent();

        OAuthClient.AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();

        events.expectLogout(loginEvent.getSessionId()).assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        String src = driver.getPageSource();
        String token = timeBased.generateTOTP(totpSecret);
        assertEquals(8, token.length());
        loginTotpPage.login(token);

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();

        // Revert
        realmRep = adminClient.realm("test").toRepresentation();
        RealmBuilder.edit(realmRep)
                .otpDigits(6);
        adminClient.realm("test").update(realmRep);
    }

    @Test
    public void setupOtpPolicyChangedHotp() {
        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        RealmBuilder.edit(realmRep)
                    .otpLookAheadWindow(0)
                    .otpDigits(6)
                    .otpPeriod(30)
                    .otpType(OTPCredentialModel.HOTP)
                    .otpAlgorithm(HmacOTP.HMAC_SHA1)
                    .otpInitialCounter(0);
        adminClient.realm("test").update(realmRep);


        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        totpPage.assertCurrent();

        String totpSecret = totpPage.getTotpSecret();

        HmacOTP otpgen = new HmacOTP(6, HmacOTP.HMAC_SHA1, 1);
        totpPage.configure(otpgen.generateHOTP(totpSecret, 0));
        String uri = driver.getCurrentUrl();
        String sessionId = events.expectRequiredAction(EventType.UPDATE_TOTP).assertEvent()
            .getDetails().get(Details.CODE_ID);

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().session(sessionId).assertEvent();

        OAuthClient.AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();

        events.expectLogout(loginEvent.getSessionId()).assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        loginTotpPage.assertCurrent();
        loginTotpPage.login(otpgen.generateHOTP(totpSecret, 1));


        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        loginEvent = events.expectLogin().assertEvent();

        tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();
        events.expectLogout(null).session(AssertEvents.isUUID()).assertEvent();

        // test lookAheadWindow
        realmRep = adminClient.realm("test").toRepresentation();
        RealmBuilder.edit(realmRep)
                    .otpLookAheadWindow(5)
                    .otpDigits(6)
                    .otpPeriod(30)
                    .otpType(OTPCredentialModel.HOTP)
                    .otpAlgorithm(HmacOTP.HMAC_SHA1)
                    .otpInitialCounter(0);
        adminClient.realm("test").update(realmRep);


        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        loginTotpPage.assertCurrent();
        loginTotpPage.login(otpgen.generateHOTP(totpSecret, 2));

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();

        // Revert
        realmRep = adminClient.realm("test").toRepresentation();
        RealmBuilder.edit(realmRep)
                .otpLookAheadWindow(1)
                .otpDigits(6)
                .otpPeriod(30)
                .otpType(OTPCredentialModel.TOTP)
                .otpAlgorithm(HmacOTP.HMAC_SHA1)
                .otpInitialCounter(0);
        adminClient.realm("test").update(realmRep);

    }

}
