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
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.Base32;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.testsuite.admin.Users;
import org.keycloak.testsuite.auth.page.login.OTPSetup;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import org.keycloak.testsuite.ui.account2.page.AbstractLoggedInPage;
import org.keycloak.testsuite.ui.account2.page.SigningInPage;
import org.keycloak.testsuite.ui.account2.page.utils.SigningInPageUtils;

import java.time.LocalDateTime;

import static java.util.Collections.emptyList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.keycloak.models.UserModel.RequiredAction.CONFIGURE_TOTP;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.ui.account2.page.utils.SigningInPageUtils.assertUserCredential;
import static org.keycloak.testsuite.ui.account2.page.utils.SigningInPageUtils.testSetUpLink;
import static org.keycloak.testsuite.util.UIUtils.refreshPageAndWaitForLoad;
import static org.keycloak.testsuite.util.WaitUtils.pause;

/**
 * @author Vaclav Muzikar <vmuzikar@redhat.com>
 */
public class SigningInTest extends BaseAccountPageTest {
    public static final String PASSWORD_LABEL = "My password";

    @Page
    private SigningInPage signingInPage;

    @Page
    private UpdatePassword updatePasswordPage;

    @Page
    private OTPSetup otpSetupPage;

    private SigningInPage.CredentialType passwordCredentialType;
    private SigningInPage.CredentialType otpCredentialType;
    private TimeBasedOTP otpGenerator;

    @Override
    protected AbstractLoggedInPage getAccountPage() {
        return signingInPage;
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

        RealmRepresentation realm = testRealmResource().toRepresentation();
        otpGenerator = new TimeBasedOTP(realm.getOtpPolicyAlgorithm(), realm.getOtpPolicyDigits(), realm.getOtpPolicyPeriod(), 0);
    }

    @Test
    public void categoriesTest() {
        testContext.setTestRealmReps(emptyList()); // reimport realm after this test

        assertThat(signingInPage.getCategoriesCount(), is(2));
        assertThat(signingInPage.getCategoryTitle("basic-authentication"), is("Basic authentication"));
        assertThat(signingInPage.getCategoryTitle("two-factor"), is("Two-factor authentication"));
    }

    @Test
    public void updatePasswordTest() {
        SigningInPage.UserCredential passwordCred = passwordCredentialType.getUserCredential(
                testUserResource()
                        .credentials()
                        .get(0)
                        .getId()
        );

        assertThat(passwordCredentialType.isSetUpLinkVisible(), is(false));
        assertThat(passwordCredentialType.isSetUp(), is(true));
        assertUserCredential(PASSWORD_LABEL, false, passwordCred);

        LocalDateTime previousCreatedAt = passwordCred.getCreatedAt();
        log.info("Waiting one minute to ensure createdAt will change");
        pause(60000);

        final String newPwd = "Keycloak is the best!";
        passwordCred.clickUpdateBtn();
        updatePasswordPage.assertCurrent();
        updatePasswordPage.updatePasswords(newPwd, newPwd);
        signingInPage.assertCurrent();

        assertUserCredential(PASSWORD_LABEL, false, passwordCred);
        assertThat(passwordCred.getCreatedAt(), is(not(previousCreatedAt)));
    }

    @Test
    public void updatePasswordTestForUserWithoutPassword() {
        // Remove password from the user through admin REST API
        String passwordId = testUserResource().credentials().get(0).getId();
        testUserResource().removeCredential(passwordId);

        // Refresh the page
        refreshPageAndWaitForLoad();

        // Test user doesn't have password set
        assertThat(passwordCredentialType.isSetUpLinkVisible(), is(true));
        assertThat(passwordCredentialType.isSetUp(), is(false));

        // Set password
        passwordCredentialType.clickSetUpLink();
        updatePasswordPage.assertCurrent();
        String originalPassword = Users.getPasswordOf(testUser);
        updatePasswordPage.updatePasswords(originalPassword, originalPassword);
        signingInPage.assertCurrent();

        // Credential set-up now
        assertThat(passwordCredentialType.isSetUpLinkVisible(), is(false));
        assertThat(passwordCredentialType.isSetUp(), is(true));
        SigningInPage.UserCredential passwordCred =
                passwordCredentialType.getUserCredential(testUserResource().credentials().get(0).getId());
        assertUserCredential(PASSWORD_LABEL, false, passwordCred);
    }

    @Test
    public void otpTest() {
        testContext.setTestRealmReps(emptyList());

        assertThat(otpCredentialType.isSetUp(), is(false));
        otpCredentialType.clickSetUpLink();
        otpSetupPage.cancel();

        signingInPage.assertCurrent();
        assertThat(otpCredentialType.isSetUp(), is(false));
        assertThat(otpCredentialType.getTitle(), is("authenticator application"));

        final String label1 = "OTP is secure";
        final String label2 = "OTP is inconvenient";

        SigningInPage.UserCredential otp1 = addOtpCredential(label1);
        assertThat(otpCredentialType.isSetUp(), is(true));
        assertThat(otpCredentialType.getUserCredentialsCount(), is(1));
        assertUserCredential(label1, true, otp1);

        SigningInPage.UserCredential otp2 = addOtpCredential(label2);
        assertThat(otpCredentialType.getUserCredentialsCount(), is(2));
        assertUserCredential(label2, true, otp2);

        assertThat("Set up link is not visible", otpCredentialType.isSetUpLinkVisible(), is(true));
        RequiredActionProviderRepresentation requiredAction = new RequiredActionProviderRepresentation();
        requiredAction.setEnabled(false);
        testRealmResource().flows().updateRequiredAction(CONFIGURE_TOTP.name(), requiredAction);

        refreshPageAndWaitForLoad();

        assertThat("Set up link for \"otp\" is visible", otpCredentialType.isSetUpLinkVisible(), is(false));
        assertThat("Not set up link for \"otp\" is visible", otpCredentialType.isNotSetUpLabelVisible(), is(false));
        assertThat("Title for \"otp\" is not visible", otpCredentialType.isTitleVisible(), is(true));
        assertThat(otpCredentialType.getUserCredentialsCount(), is(2));

        testRemoveCredential(otp1);
    }

    @Test
    public void setUpLinksTest() {
        testSetUpLink(testRealmResource(), otpCredentialType, CONFIGURE_TOTP.name());
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
        signingInPage.assertCurrent();

        return getNewestUserCredential(otpCredentialType);
    }

    private SigningInPage.UserCredential getNewestUserCredential(SigningInPage.CredentialType credentialType) {
        return SigningInPageUtils.getNewestUserCredential(testUserResource(), credentialType);
    }

    private void testRemoveCredential(SigningInPage.UserCredential userCredential) {
        SigningInPageUtils.testRemoveCredential(getAccountPage(), userCredential);
    }
}
