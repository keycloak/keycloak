/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.login;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.HmacOTP;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.auth.page.login.LoginError;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.auth.page.login.OTPSetup;
import org.keycloak.testsuite.auth.page.login.OneTimeCode;
import org.keycloak.testsuite.auth.page.login.RequiredActions;
import org.keycloak.testsuite.auth.page.login.TermsAndConditions;
import org.keycloak.testsuite.auth.page.login.UpdateAccount;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import org.keycloak.testsuite.auth.page.login.VerifyEmail;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.zxing.BarcodeFormat.QR_CODE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.keycloak.models.ClientScopeModel.CONSENT_SCREEN_TEXT;
import static org.keycloak.models.ClientScopeModel.DISPLAY_ON_CONSENT_SCREEN;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;
import static org.keycloak.representations.idm.CredentialRepresentation.PASSWORD;
import static org.keycloak.testsuite.admin.ApiUtil.createUserAndResetPasswordWithAdminClient;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class RequiredActionsTest extends AbstractLoginTest {
    public static final String GRANT_REALM = "grant-realm";
    public static final String CONSENT_TEXT = "Příliš žluťoučký kůň úpěl ďábelské ódy";

    private UserRepresentation grantRealmUser = createUserRepresentation("test", PASSWORD);

    public static final String TOTP = "totp";
    public static final String HOTP = "hotp";
    
    @Page
    private TermsAndConditions termsAndConditionsPage;

    @Page
    private UpdatePassword updatePasswordPage;

    @Page
    private UpdateAccount updateAccountPage;

    @Page
    private VerifyEmail verifyEmailPage;

    @Page
    private OTPSetup otpSetupPage;

    @Page
    private OneTimeCode oneTimeCodePage;

    @Page
    private OAuthGrant oAuthGrantPage;

    @Page
    private LoginError loginErrorPage;

    private TimeBasedOTP otpGenerator = new TimeBasedOTP();

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        termsAndConditionsPage.setAuthRealm(TEST);
        updatePasswordPage.setAuthRealm(TEST);
        updateAccountPage.setAuthRealm(TEST);
        verifyEmailPage.setAuthRealm(TEST);
        otpSetupPage.setAuthRealm(TEST);
        oneTimeCodePage.setAuthRealm(TEST);
        oAuthGrantPage.setAuthRealm(GRANT_REALM);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        super.addTestRealms(testRealms);

        RealmRepresentation testRealmRep = new RealmRepresentation();
        testRealmRep.setId(GRANT_REALM);
        testRealmRep.setRealm(GRANT_REALM);
        configureInternationalizationForRealm(testRealmRep);
        testRealmRep.setEnabled(true);

        testRealms.add(testRealmRep);
    }

    // Some actions we need to do after the realm is created and configured
    @Override
    protected void afterAbstractKeycloakTestRealmImport() {
        super.afterAbstractKeycloakTestRealmImport();

        // create test user
        createUserAndResetPasswordWithAdminClient(adminClient.realm(GRANT_REALM), grantRealmUser, PASSWORD);
    }

    @Test
    public void termsAndConditions() {
        RequiredActionProviderRepresentation termsAndCondRep = testRealmResource().flows().getRequiredAction(termsAndConditionsPage.getActionId());
        termsAndCondRep.setEnabled(true);
        testRealmResource().flows().updateRequiredAction(termsAndConditionsPage.getActionId(), termsAndCondRep);

        initiateRequiredAction(termsAndConditionsPage);

        termsAndConditionsPage.localeDropdown().selectAndAssert(CUSTOM_LOCALE_NAME);

        termsAndConditionsPage.acceptTerms();
        assertLoginSuccessful();

        deleteAllSessionsInTestRealm();
        initiateRequiredAction(termsAndConditionsPage);
        assertEquals("[TEST LOCALE] souhlas s podmínkami", termsAndConditionsPage.getText());
        termsAndConditionsPage.declineTerms();
        loginErrorPage.assertCurrent();
        assertNoAccess();
    }

    @Test
    public void updatePassword() {
        initiateRequiredAction(updatePasswordPage);

        updatePasswordPage.localeDropdown().selectAndAssert(CUSTOM_LOCALE_NAME);
        assertTrue(updatePasswordPage.feedbackMessage().isWarning());
        assertEquals("You need to change your password to activate your account.", updatePasswordPage.feedbackMessage().getText());
        assertEquals("New Password", updatePasswordPage.fields().getNewPasswordLabel());
        assertEquals("Confirm password", updatePasswordPage.fields().getConfirmPasswordLabel());

        updatePasswordPage.updatePasswords("some wrong", "password");
        assertTrue(updatePasswordPage.feedbackMessage().isError());
        assertEquals("[TEST LOCALE] hesla se neshodují", updatePasswordPage.feedbackMessage().getText());

        updatePasswordPage.localeDropdown().selectAndAssert(ENGLISH_LOCALE_NAME);
        updatePasswordPage.updatePasswords("matchingPassword", "matchingPassword");
        assertLoginSuccessful();
    }

    @Test
    public void updateProfile() {
        initiateRequiredAction(updateAccountPage);

        // prefilled profile
        assertTrue(updateAccountPage.feedbackMessage().isWarning());
        updateAccountPage.localeDropdown().selectAndAssert(CUSTOM_LOCALE_NAME);
        assertEquals("[TEST LOCALE] aktualizovat profil", updateAccountPage.feedbackMessage().getText());
        updateAccountPage.localeDropdown().selectAndAssert(ENGLISH_LOCALE_NAME);
        assertFalse(updateAccountPage.fields().isUsernamePresent());
        assertEquals("Email", updateAccountPage.fields().getEmailLabel());
        assertEquals("First name", updateAccountPage.fields().getFirstNameLabel());
        assertEquals("Last name", updateAccountPage.fields().getLastNameLabel());
        assertFalse(updateAccountPage.fields().hasEmailError());
        assertFalse(updateAccountPage.fields().hasFirstNameError());
        assertFalse(updateAccountPage.fields().hasLastNameError());
        assertEquals(testUser.getEmail(), updateAccountPage.fields().getEmail());
        assertEquals(testUser.getFirstName(), updateAccountPage.fields().getFirstName());
        assertEquals(testUser.getLastName(), updateAccountPage.fields().getLastName());
        updateAccountPage.localeDropdown().selectAndAssert(CUSTOM_LOCALE_NAME);

        // empty form
        updateAccountPage.updateAccount(null, null, null);
        assertTrue(updateAccountPage.feedbackMessage().isError());
        String errorMsg = updateAccountPage.feedbackMessage().getText();
        assertTrue(errorMsg.contains("first name") && errorMsg.contains("last name") && errorMsg.contains("email"));
        assertTrue(updateAccountPage.fields().hasEmailError());
        assertTrue(updateAccountPage.fields().hasFirstNameError());
        assertTrue(updateAccountPage.fields().hasLastNameError());

        final String email = "vmuzikar@redhat.com";
        final String firstName = "Vaclav";
        final String lastName = "Muzikar";

        // email filled in
        updateAccountPage.fields().setEmail(email);
        updateAccountPage.submit();
        assertTrue(updateAccountPage.feedbackMessage().isError());
        errorMsg = updateAccountPage.feedbackMessage().getText();
        assertTrue(errorMsg.contains("first name") && errorMsg.contains("last name") && !errorMsg.contains("email"));
        assertFalse(updateAccountPage.fields().hasEmailError());
        assertTrue(updateAccountPage.fields().hasFirstNameError());
        assertTrue(updateAccountPage.fields().hasLastNameError());
        assertEquals(email, updateAccountPage.fields().getEmail());

        // first name filled in
        updateAccountPage.fields().setFirstName(firstName);
        updateAccountPage.submit();
        assertTrue(updateAccountPage.feedbackMessage().isError());
        errorMsg = updateAccountPage.feedbackMessage().getText();
        assertTrue(!errorMsg.contains("first name") && errorMsg.contains("last name") && !errorMsg.contains("email"));
        assertFalse(updateAccountPage.fields().hasEmailError());
        assertFalse(updateAccountPage.fields().hasFirstNameError());
        assertTrue(updateAccountPage.fields().hasLastNameError());
        assertEquals(email, updateAccountPage.fields().getEmail());
        assertEquals(firstName, updateAccountPage.fields().getFirstName());

        // last name filled in
        updateAccountPage.fields().setFirstName(null);
        updateAccountPage.fields().setLastName(lastName);
        updateAccountPage.submit();
        assertTrue(updateAccountPage.feedbackMessage().isError());
        errorMsg = updateAccountPage.feedbackMessage().getText();
        assertTrue(errorMsg.contains("first name") && !errorMsg.contains("last name") && !errorMsg.contains("email"));
        assertFalse(updateAccountPage.fields().hasEmailError());
        assertTrue(updateAccountPage.fields().hasFirstNameError());
        assertFalse(updateAccountPage.fields().hasLastNameError());
        assertEquals(email, updateAccountPage.fields().getEmail());
        assertEquals(lastName, updateAccountPage.fields().getLastName());

        // success
        assertEquals("[TEST LOCALE] křestní jméno", updateAccountPage.fields().getFirstNameLabel());
        updateAccountPage.updateAccount(email, firstName, lastName);
        assertLoginSuccessful();
    }

    @Test
    public void verifyEmail() {
        initiateRequiredAction(verifyEmailPage);

        verifyEmailPage.localeDropdown().selectAndAssert(CUSTOM_LOCALE_NAME);

        boolean firstAttempt = true;
        while (true) {
            assertTrue(verifyEmailPage.feedbackMessage().isWarning());
            assertEquals("[TEST LOCALE] je třeba ověřit emailovou adresu", verifyEmailPage.feedbackMessage().getText());
            assertEquals("An email with instructions to verify your email address has been sent to your address test@email.test.", verifyEmailPage.getInstructionMessage());

            if (firstAttempt) {
                verifyEmailPage.clickResend();
                firstAttempt = false;
            }
            else {
                break;
            }
        }
    }

    @Test
    public void configureManualTotp() {
        setRealmOtpType(TOTP);
        testManualOtp();
    }

    @Test
    public void configureManualHotp() {
        setRealmOtpType(HOTP);
        testManualOtp();
    }

    @Test
    public void configureBarcodeTotp() throws Exception {
        setRealmOtpType(TOTP);
        testBarcodeOtp();
    }

    @Test
    public void configureBarcodeHotp() throws Exception {
        setRealmOtpType(HOTP);
        testBarcodeOtp();
    }

    @Test
    public void clientConsent() {
        testRealmPage.setAuthRealm(GRANT_REALM);
        testRealmAccountPage.setAuthRealm(GRANT_REALM);
        testRealmLoginPage.setAuthRealm(GRANT_REALM);

        final List<String> defaultClientScopesToApprove = Arrays.asList("Email address", "User profile");

        // custom consent text
        initiateClientScopesConsent(true, CONSENT_TEXT);
        oAuthGrantPage.localeDropdown().selectAndAssert(CUSTOM_LOCALE_NAME);
        List<String> clientScopesToApprove = new LinkedList<>(defaultClientScopesToApprove);
        clientScopesToApprove.add(CONSENT_TEXT);
        oAuthGrantPage.assertClientScopes(clientScopesToApprove);

        // default consent text
        initiateClientScopesConsent(true, null);
        clientScopesToApprove = new LinkedList<>(defaultClientScopesToApprove);
        clientScopesToApprove.add("Account");
        oAuthGrantPage.assertClientScopes(clientScopesToApprove);

        // consent with missing client
        initiateClientScopesConsent(false, CONSENT_TEXT);
        oAuthGrantPage.assertClientScopes(defaultClientScopesToApprove);

        // test buttons
        oAuthGrantPage.cancel();
        assertNoAccess();
        testRealmLoginPage.form().login(grantRealmUser);
        assertEquals("[TEST LOCALE] Udělit přístup Account", oAuthGrantPage.getTitleText());
        oAuthGrantPage.accept();
        assertLoginSuccessful();
    }

    private void testManualOtp() {
        initiateRequiredAction(otpSetupPage);

        otpSetupPage.localeDropdown().selectAndAssert(CUSTOM_LOCALE_NAME);

        otpSetupPage.clickManualMode();
        assertFalse(otpSetupPage.isBarcodePresent());
        assertTrue(otpSetupPage.feedbackMessage().isWarning());
        assertEquals("You need to set up Mobile Authenticator to activate your account.", otpSetupPage.feedbackMessage().getText());

        // empty input
        otpSetupPage.submit();
        assertTrue(otpSetupPage.feedbackMessage().isError());
        assertEquals("Please specify authenticator code.", otpSetupPage.feedbackMessage().getText());

        final String replacePattern = "^.+: ";
        
        // extract data
        String type = otpSetupPage.getOtpType().replaceAll(replacePattern, "");
        if (type.equals("Time-based")) type = TOTP;
        else if (type.equals("Counter-based")) type = HOTP;
        String secret = otpSetupPage.getSecretKey();
        int digits = Integer.parseInt(otpSetupPage.getOtpDigits().replaceAll(replacePattern, ""));
        String algorithm = otpSetupPage.getOtpAlgorithm().replaceAll(replacePattern, "");
        Integer period = type.equals(TOTP) ? Integer.parseInt(otpSetupPage.getOtpPeriod().replaceAll(replacePattern, "")) : null;
        Integer counter = type.equals(HOTP) ? Integer.parseInt(otpSetupPage.getOtpCounter().replaceAll(replacePattern, "")) : null;

        // the actual test
        testOtp(type, algorithm, digits, period, counter, secret);
    }

    private void testBarcodeOtp() throws Exception {
        assumeFalse(driver instanceof HtmlUnitDriver); // HtmlUnit browser cannot take screenshots
        TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
        QRCodeReader qrCodeReader = new QRCodeReader();

        initiateRequiredAction(otpSetupPage);

        otpSetupPage.localeDropdown().selectAndAssert(CUSTOM_LOCALE_NAME);

        otpSetupPage.clickManualMode();
        otpSetupPage.clickBarcodeMode();

        assertTrue(otpSetupPage.isBarcodePresent());
        assertFalse(otpSetupPage.isSecretKeyPresent());
        assertTrue(otpSetupPage.feedbackMessage().isWarning());
        assertEquals("You need to set up Mobile Authenticator to activate your account.", otpSetupPage.feedbackMessage().getText());

        // empty input
        otpSetupPage.submit();
        assertTrue(otpSetupPage.feedbackMessage().isError());
        assertEquals("Please specify authenticator code.", otpSetupPage.feedbackMessage().getText());

        // take a screenshot of the QR code
        byte[] screenshot = screenshotDriver.getScreenshotAs(OutputType.BYTES);
        BufferedImage screenshotImg = ImageIO.read(new ByteArrayInputStream(screenshot));
        BinaryBitmap screenshotBinaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(screenshotImg)));
        Result qrCode = qrCodeReader.decode(screenshotBinaryBitmap);

        // parse the QR code string
        Pattern qrUriPattern = Pattern.compile("^otpauth:\\/\\/(?<type>.+)\\/(?<realm>.+):(?<user>.+)\\?secret=(?<secret>.+)&digits=(?<digits>.+)&algorithm=(?<algorithm>.+)&issuer=(?<issuer>.+)&(?:period=(?<period>.+)|counter=(?<counter>.+))$");
        Matcher qrUriMatcher = qrUriPattern.matcher(qrCode.getText());
        assertTrue(qrUriMatcher.find());

        // extract data
        String type = qrUriMatcher.group("type");
        String realm = qrUriMatcher.group("realm");
        String user = qrUriMatcher.group("user");
        String secret = qrUriMatcher.group("secret");
        int digits = Integer.parseInt(qrUriMatcher.group("digits"));
        String algorithm = qrUriMatcher.group("algorithm");
        String issuer = qrUriMatcher.group("issuer");
        Integer period = type.equals(TOTP) ? Integer.parseInt(qrUriMatcher.group("period")) : null;
        Integer counter = type.equals(HOTP) ? Integer.parseInt(qrUriMatcher.group("counter")) : null;

        RealmRepresentation realmRep = testRealmResource().toRepresentation();
        String expectedRealmName = realmRep.getDisplayName() != null && !realmRep.getDisplayName().isEmpty() ? realmRep.getDisplayName() : realmRep.getRealm();

        // basic assertations
        assertEquals(QR_CODE, qrCode.getBarcodeFormat());
        assertEquals(expectedRealmName, realm);
        assertEquals(expectedRealmName, issuer);
        assertEquals(testUser.getUsername(), user);

        // the actual test
        testOtp(type, algorithm, digits, period, counter, secret);
    }

    private void testOtp(String type, String algorithm, int digits, Integer period, Integer counter, String secret) {
        switch (algorithm) {
            case "SHA1":
                algorithm = TimeBasedOTP.HMAC_SHA1;
                break;
            case "SHA256":
                algorithm = TimeBasedOTP.HMAC_SHA256;
                break;
            case "SHA512":
                algorithm = TimeBasedOTP.HMAC_SHA512;
                break;
            default:
                throw new AssertionError("Wrong algorithm type");
        }

        HmacOTP otpGenerator;
        String secretDecoded = new String(Base32.decode(secret));
        String code;

        switch (type) {
            case TOTP:
                otpGenerator = new TimeBasedOTP(algorithm, digits, period, 0);
                code = ((TimeBasedOTP) otpGenerator).generateTOTP(secretDecoded);
                break;
            case HOTP:
                otpGenerator = new HmacOTP(digits, algorithm, 0);
                code = otpGenerator.generateHOTP(secretDecoded, counter);
                break;
            default:
                throw new AssertionError("Wrong OTP type");
        }

        // fill in the form
        otpSetupPage.setTotp(code);
        otpSetupPage.submit();
        assertLoginSuccessful();

        // try the code is working
        deleteAllSessionsInTestRealm();
        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(testUser);
        oneTimeCodePage.assertCurrent();
        //assertEquals("One-time code", oneTimeCodePage.getTotpLabel());

        // bad attempt
        oneTimeCodePage.submit();
        assertTrue(oneTimeCodePage.feedbackMessage().isError());
        assertEquals("[TEST LOCALE] vložen chybný kód", oneTimeCodePage.feedbackMessage().getText());
        oneTimeCodePage.sendCode("XXXXXX");
        assertTrue(oneTimeCodePage.feedbackMessage().isError());
        assertEquals("[TEST LOCALE] vložen chybný kód", oneTimeCodePage.feedbackMessage().getText());

        // generate new code
        code = type.equals(TOTP) ? ((TimeBasedOTP) otpGenerator).generateTOTP(secretDecoded) : otpGenerator.generateHOTP(secretDecoded, ++counter);
        oneTimeCodePage.sendCode(code);
        assertLoginSuccessful();
    }

    private void setRealmOtpType(String otpType) {
        RealmRepresentation realmRep = testRealmResource().toRepresentation();
        realmRep.setOtpPolicyType(otpType);
        testRealmResource().update(realmRep);
    }

    private void initiateRequiredAction(RequiredActions requiredActionPage) {
        testUser.setRequiredActions(Collections.singletonList(requiredActionPage.getActionId()));
        testUserResource().update(testUser);

        testRealmAccountPage.navigateTo();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmAccountPage);

        testRealmLoginPage.form().login(testUser);
        requiredActionPage.assertCurrent();
    }

    private void initiateClientScopesConsent(boolean displayOnConsentScreen, String consentScreenText) {
        ClientRepresentation accountClientRep = testRealmResource().clients().findByClientId(ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);
        ClientResource accountClient = testRealmResource().clients().get(accountClientRep.getId());
        accountClientRep.setConsentRequired(true);
        accountClientRep.getAttributes().put(DISPLAY_ON_CONSENT_SCREEN, String.valueOf(displayOnConsentScreen));
        accountClientRep.getAttributes().put(CONSENT_SCREEN_TEXT, consentScreenText);
        accountClient.update(accountClientRep);

        testRealmAccountPage.navigateTo();
        testRealmLoginPage.form().login(grantRealmUser);
        oAuthGrantPage.assertCurrent();
    }

    private void assertNoAccess() {
        assertEquals("No access", loginErrorPage.getErrorMessage());
        loginErrorPage.backToApplication();
        assertCurrentUrlStartsWithLoginUrlOf(testRealmLoginPage);
    }
}
