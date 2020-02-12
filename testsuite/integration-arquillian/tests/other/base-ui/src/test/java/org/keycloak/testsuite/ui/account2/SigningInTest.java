/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.ui.account2;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.WebAuthnAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.WebAuthnPasswordlessAuthenticatorFactory;
import org.keycloak.authentication.requiredactions.WebAuthnPasswordlessRegisterFactory;
import org.keycloak.authentication.requiredactions.WebAuthnRegisterFactory;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.credential.WebAuthnCredentialModel;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.AuthenticationExecutionRepresentation;
import org.keycloak.representations.idm.AuthenticationFlowRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderSimpleRepresentation;
import org.keycloak.testsuite.WebAuthnAssume;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.auth.page.login.OTPSetup;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import org.keycloak.testsuite.pages.webauthn.WebAuthnRegisterPage;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.SigningInPage;

import java.time.LocalDateTime;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.models.AuthenticationExecutionModel.Requirement.REQUIRED;
import static org.keycloak.models.UserModel.RequiredAction.CONFIGURE_TOTP;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;
import static org.keycloak.testsuite.util.WaitUtils.pause;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SigningInTest extends BaseAccountPageTest {
    public static final String PASSWORD_LABEL = "My Password";
    public static final String WEBAUTHN_FLOW_ID = "75e2390e-f296-49e6-acf8-6d21071d7e10";

    @Page
    private SigningInPage signingInPage;

    @Page
    private UpdatePassword updatePasswordPage;

    @Page
    private OTPSetup otpSetupPage;

    @Page
    private WebAuthnRegisterPage webAuthnRegisterPage;

    private SigningInPage.CredentialType passwordCredentialType;
    private SigningInPage.CredentialType otpCredentialType;
    private SigningInPage.CredentialType webAuthnCredentialType;
    private SigningInPage.CredentialType webAuthnPwdlessCredentialType;
    private TimeBasedOTP otpGenerator;

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return signingInPage;
    }

    @Override
    protected void afterAbstractKeycloakTestRealmImport() {
        super.afterAbstractKeycloakTestRealmImport();

        // configure WebAuthn
        // we can't do this during the realm import because we'd need to specify all built-in flows as well

        AuthenticationFlowRepresentation flow = new AuthenticationFlowRepresentation();
        flow.setId(WEBAUTHN_FLOW_ID);
        flow.setAlias("webauthn flow");
        flow.setProviderId("basic-flow");
        flow.setBuiltIn(false);
        flow.setTopLevel(true);
        testRealmResource().flows().createFlow(flow);

        AuthenticationExecutionRepresentation execution = new AuthenticationExecutionRepresentation();
        execution.setAuthenticator(WebAuthnAuthenticatorFactory.PROVIDER_ID);
        execution.setPriority(10);
        execution.setRequirement(REQUIRED.toString());
        execution.setParentFlow(WEBAUTHN_FLOW_ID);
        testRealmResource().flows().addExecution(execution);

        execution.setAuthenticator(WebAuthnPasswordlessAuthenticatorFactory.PROVIDER_ID);
        testRealmResource().flows().addExecution(execution);

        RequiredActionProviderSimpleRepresentation requiredAction = new RequiredActionProviderSimpleRepresentation();
        requiredAction.setProviderId(WebAuthnRegisterFactory.PROVIDER_ID);
        requiredAction.setName("blahblah");
        testRealmResource().flows().registerRequiredAction(requiredAction);

        requiredAction.setProviderId(WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
        testRealmResource().flows().registerRequiredAction(requiredAction);

        // no need to actually configure the authentication, in Account Console tests we just verify the registration
    }

    @Override
    public void setDefaultPageUriParameters() {
        super.setDefaultPageUriParameters();
        updatePasswordPage.setAuthRealm(TEST);
        otpSetupPage.setAuthRealm(TEST);
    }

    @Before
    public void beforeSigningInTest() {
        passwordCredentialType = signingInPage.getCredentialType(PasswordCredentialModel.TYPE);
        otpCredentialType = signingInPage.getCredentialType(OTPCredentialModel.TYPE);
        webAuthnCredentialType = signingInPage.getCredentialType(WebAuthnCredentialModel.TYPE_TWOFACTOR);
        webAuthnPwdlessCredentialType = signingInPage.getCredentialType(WebAuthnCredentialModel.TYPE_PASSWORDLESS);

        RealmRepresentation realm = testRealmResource().toRepresentation();
        otpGenerator = new TimeBasedOTP(realm.getOtpPolicyAlgorithm(), realm.getOtpPolicyDigits(), realm.getOtpPolicyPeriod(), 0);
    }

    @Test
    public void categoriesTest() {
        testContext.setTestRealmReps(emptyList()); // reimport realm after this test

        assertEquals(3, signingInPage.getCategoriesCount());

        assertEquals("Basic Authentication", signingInPage.getCategoryTitle("basic-authentication"));
        assertEquals("Two-Factor Authentication", signingInPage.getCategoryTitle("two-factor"));
        assertEquals("Passwordless", signingInPage.getCategoryTitle("passwordless"));

        // Delete WebAuthn flow ==> Passwordless category should disappear
        testRealmResource().flows().deleteFlow(WEBAUTHN_FLOW_ID);
        refreshPageAndWaitForLoad();
        assertEquals(2, signingInPage.getCategoriesCount());
    }

    @Test
    public void updatePasswordTest() {
        SigningInPage.UserCredential passwordCred =
                passwordCredentialType.getUserCredential(testUserResource().credentials().get(0).getId());

        assertFalse(passwordCredentialType.isSetUpLinkVisible());
        assertTrue(passwordCredentialType.isSetUp());
        assertUserCredential(PASSWORD_LABEL, false, passwordCred);

        LocalDateTime previousCreatedAt = passwordCred.getCreatedAt();
        log.info("Waiting one minute to ensure createdAt will change");
        pause(60000);

        final String newPwd = "Keycloak is the best!";
        passwordCred.clickUpdateBtn();
        updatePasswordPage.assertCurrent();
        updatePasswordPage.updatePasswords(newPwd, newPwd);
        // TODO uncomment this once KEYCLOAK-12852 is resolved
        // signingInPage.assertCurrent();

        assertUserCredential(PASSWORD_LABEL, false, passwordCred);
        assertNotEquals(previousCreatedAt, passwordCred.getCreatedAt());
    }

    @Test
    public void updatePasswordTestForUserWithoutPassword() {
            // Remove password from the user through admin REST API
            String passwordId = testUserResource().credentials().get(0).getId();
            testUserResource().removeCredential(passwordId);

            // Refresh the page
            refreshPageAndWaitForLoad();

            // Test user doesn't have password set
            assertTrue(passwordCredentialType.isSetUpLinkVisible());
            assertFalse(passwordCredentialType.isSetUp());

            // Set password
            passwordCredentialType.clickSetUpLink();
            updatePasswordPage.assertCurrent();
            String originalPassword = Users.getPasswordOf(testUser);
            updatePasswordPage.updatePasswords(originalPassword, originalPassword);
            // TODO uncomment this once KEYCLOAK-12852 is resolved
            // signingInPage.assertCurrent();

            // Credential set-up now
            assertFalse(passwordCredentialType.isSetUpLinkVisible());
            assertTrue(passwordCredentialType.isSetUp());
            SigningInPage.UserCredential passwordCred =
                    passwordCredentialType.getUserCredential(testUserResource().credentials().get(0).getId());
            assertUserCredential(PASSWORD_LABEL, false, passwordCred);
    }

    @Test
    public void otpTest() {
        assertFalse(otpCredentialType.isSetUp());
        otpCredentialType.clickSetUpLink();
        otpSetupPage.cancel();
        // TODO uncomment this once KEYCLOAK-12852 is resolved
        // signingInPage.assertCurrent();
        assertFalse(otpCredentialType.isSetUp());

        assertEquals("Authenticator Application", otpCredentialType.getTitle());

        final String label1 = "OTP is secure";
        final String label2 = "OTP is inconvenient";

        SigningInPage.UserCredential otp1 = addOtpCredential(label1);
        assertTrue(otpCredentialType.isSetUp());
        assertEquals(1, otpCredentialType.getUserCredentialsCount());
        assertUserCredential(label1, true, otp1);

        SigningInPage.UserCredential otp2 = addOtpCredential(label2);
        assertEquals(2, otpCredentialType.getUserCredentialsCount());
        assertUserCredential(label2, true, otp2);

        testRemoveCredential(otp1);
    }

    @Test
    public void twoFactorWebAuthnTest() {
        testWebAuthn(false);
    }

    @Test
    public void passwordlessWebAuthnTest() {
        testWebAuthn(true);
    }

    private void testWebAuthn(boolean passwordless) {
        WebAuthnAssume.assumeChrome(driver); // we need some special flags to be able to register security key

        SigningInPage.CredentialType credentialType;
        final String expectedHelpText;

        if (passwordless) {
            credentialType = webAuthnPwdlessCredentialType;
            expectedHelpText = "Use your security key for passwordless log in.";
        }
        else {
            credentialType = webAuthnCredentialType;
            expectedHelpText = "Use your security key to log in.";
        }

        assertFalse(credentialType.isSetUp());
        // no way to simulate registration cancellation

        assertEquals("Security Key", credentialType.getTitle());
        assertEquals(expectedHelpText, credentialType.getHelpText());

        final String label1 = "WebAuthn is convenient";
        final String label2 = "but not yet widely adopted";

        SigningInPage.UserCredential webAuthn1 = addWebAuthnCredential(label1, passwordless);
        assertTrue(credentialType.isSetUp());
        assertEquals(1, credentialType.getUserCredentialsCount());
        assertUserCredential(label1, true, webAuthn1);

        SigningInPage.UserCredential webAuthn2 = addWebAuthnCredential(label2, passwordless);
        assertEquals(2, credentialType.getUserCredentialsCount());
        assertUserCredential(label2, true, webAuthn2);

        testRemoveCredential(webAuthn1);
    }

    @Test
    public void setUpLinksTest() {
        testSetUpLink(otpCredentialType, CONFIGURE_TOTP.name());
        testSetUpLink(webAuthnCredentialType, WebAuthnRegisterFactory.PROVIDER_ID);
        testSetUpLink(webAuthnPwdlessCredentialType, WebAuthnPasswordlessRegisterFactory.PROVIDER_ID);
    }

    private void testSetUpLink(SigningInPage.CredentialType credentialType, String requiredActionProviderId) {
        assertTrue("Set up link is visible", credentialType.isSetUpLinkVisible());

        RequiredActionProviderRepresentation requiredAction = new RequiredActionProviderRepresentation();
        requiredAction.setEnabled(false);
        testRealmResource().flows().updateRequiredAction(requiredActionProviderId, requiredAction);

        refreshPageAndWaitForLoad();
        assertFalse("Set up link is not visible", credentialType.isSetUpLinkVisible());

        assertFalse("Credential type is not set up", credentialType.isSetUp()); // this also check the cred type is present
        assertNotNull("Title is present", credentialType.getTitle());
    }

    private SigningInPage.UserCredential addOtpCredential(String label) {
        otpCredentialType.clickSetUpLink();
        otpSetupPage.assertCurrent();
        otpSetupPage.clickManualMode();

        String secret = new String(Base32.decode(otpSetupPage.getSecretKey()));
        String code = otpGenerator.generateTOTP(secret);
        otpSetupPage.setTotp(code);
        otpSetupPage.setUserLabel(label);
        otpSetupPage.submit();
        // TODO uncomment this once KEYCLOAK-12852 is resolved
        // signingInPage.assertCurrent();

        return getNewestUserCredential(otpCredentialType);
    }

    private SigningInPage.UserCredential addWebAuthnCredential(String label, boolean passwordless) {
        SigningInPage.CredentialType credentialType = passwordless ? webAuthnPwdlessCredentialType : webAuthnCredentialType;

        credentialType.clickSetUpLink(true);
        webAuthnRegisterPage.registerWebAuthnCredential(label);
        waitForPageToLoad();
        // TODO uncomment this once KEYCLOAK-12852 is resolved
        // signingInPage.assertCurrent();

        return getNewestUserCredential(credentialType);
    }

    private SigningInPage.UserCredential getNewestUserCredential(SigningInPage.CredentialType credentialType) {
        List<CredentialRepresentation> credentials = testUserResource().credentials();
        SigningInPage.UserCredential userCredential =
                credentialType.getUserCredential(credentials.get(credentials.size() - 1).getId());
        assertTrue(userCredential.isPresent());
        return userCredential;
    }

    private void testRemoveCredential(SigningInPage.UserCredential userCredential) {
        int countBeforeRemove = userCredential.getCredentialType().getUserCredentialsCount();

        testModalDialog(userCredential::clickRemoveBtn, () -> {
            assertTrue(userCredential.isPresent());
            assertEquals(countBeforeRemove, userCredential.getCredentialType().getUserCredentialsCount());
        });

        assertFalse(userCredential.isPresent());
        assertEquals(countBeforeRemove - 1, userCredential.getCredentialType().getUserCredentialsCount());
        signingInPage.alert().assertSuccess();
    }

    private void assertUserCredential(String expectedUserLabel, boolean removable, SigningInPage.UserCredential userCredential) {
        assertEquals(expectedUserLabel, userCredential.getUserLabel());

        // we expect the credential was created/edited no longer that 2 minutes ago (1 minute might not be enough in some corner cases)
        LocalDateTime beforeNow = LocalDateTime.now().minusMinutes(2);
        LocalDateTime now = LocalDateTime.now();
        // createdAt doesn't specify seconds so it should be something like 12:47:00
        LocalDateTime createdAt = userCredential.getCreatedAt();

        assertTrue("Creation time should be after time before update", createdAt.isAfter(beforeNow));
        assertTrue("Creation time should be before now", createdAt.isBefore(now));

        assertEquals("Remove button visible", removable, userCredential.isRemoveBtnDisplayed());
        assertEquals("Update button visible", !removable, userCredential.isUpdateBtnDisplayed());
    }
}
