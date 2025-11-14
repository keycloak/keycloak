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
package org.keycloak.testsuite.actions;

import java.util.List;

import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.credential.OTPCredentialModel;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginConfigTotpPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.pages.VerifyProfilePage;
import org.keycloak.testsuite.util.GreenMailRule;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.actions.RequiredActionEmailVerificationTest.getEmailLink;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author <a href="mailto:wadahiro@gmail.com">Hiroyuki Wada</a>
 */
public class RequiredActionPriorityTest extends AbstractTestRealmKeycloakTest {

    private static final String EMAIL = "test-user@localhost";
    private static final String USERNAME = EMAIL;
    private static final String PASSWORD = "password";
    private static final String NEW_EMAIL = "new@email.com";
    private static final String NEW_FIRST_NAME = "New first";
    private static final String NEW_LAST_NAME = "New last";
    private static final String NEW_PASSWORD = "new-password";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;
    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    @Page
    protected LoginPasswordUpdatePage changePasswordPage;

    @Page
    protected LoginUpdateProfilePage updateProfilePage;

    @Page
    protected VerifyProfilePage verifyProfilePage;

    @Page
    protected TermsAndConditionsPage termsPage;

    @Page
    protected LoginConfigTotpPage totpPage;

    private String testUserId;

    @Before
    public void beforeEach() {
        setRequiredActionEnabled(TEST_REALM_NAME, TermsAndConditions.PROVIDER_ID, true, false);

        testUserId = ApiUtil.findUserByUsernameId(testRealm(), USERNAME).toRepresentation().getId();
    }

    @Override
    public void configureTestRealm(final RealmRepresentation testRealm) {
        testRealm.setResetPasswordAllowed(true);
    }

    @Override
    protected boolean isImportAfterEachMethod() {
        return true;
    }

    @Override
    protected boolean removeVerifyProfileAtImport() {
        return false;
    }

    @Test
    public void executeRequiredActionsWithDefaultPriority() {
        // Default priority is alphabetical order:
        // TermsAndConditions -> UpdatePassword -> UpdateProfile
        enableRequiredActionForUser(RequiredAction.UPDATE_PASSWORD);
        enableRequiredActionForUser(RequiredAction.UPDATE_PROFILE);
        enableRequiredActionForUser(RequiredAction.TERMS_AND_CONDITIONS);

        // Login
        loginPage.open();
        loginPage.login(USERNAME, PASSWORD);

        // First, accept terms
        termsPage.assertCurrent();
        termsPage.acceptTerms();
        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).removeDetail(Details.REDIRECT_URI)
                .detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID).assertEvent();

        // Second, change password
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword(NEW_PASSWORD, NEW_PASSWORD);
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();

        // Finally, update profile
        updateProfilePage.assertCurrent();
        updateProfilePage.prepareUpdate().firstName(NEW_FIRST_NAME).lastName(NEW_LAST_NAME)
                .email(NEW_EMAIL).submit();
        events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.UPDATED_FIRST_NAME, NEW_FIRST_NAME)
                .detail(Details.UPDATED_LAST_NAME, NEW_LAST_NAME)
                .detail(Details.PREVIOUS_EMAIL, EMAIL)
                .detail(Details.UPDATED_EMAIL, NEW_EMAIL)
                .assertEvent();

        // Logged in
        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().assertEvent();
    }

    @Test
    public void executeRequiredActionsWithCustomPriority() {
        // Default priority is alphabetical order:
        // TermsAndConditions -> UpdatePassword -> UpdateProfile

        // After Changing the priority, the order will be:
        // UpdatePassword -> UpdateProfile -> TermsAndConditions
        final var requiredActionsCustomOrdered = List.of(
                RequiredAction.UPDATE_PASSWORD,
                RequiredAction.UPDATE_PROFILE,
                RequiredAction.TERMS_AND_CONDITIONS
        );
        ApiUtil.updateRequiredActionsOrder(testRealm(), requiredActionsCustomOrdered);

        enableRequiredActionForUser(RequiredAction.UPDATE_PASSWORD);
        enableRequiredActionForUser(RequiredAction.UPDATE_PROFILE);
        enableRequiredActionForUser(RequiredAction.TERMS_AND_CONDITIONS);

        // Login
        loginPage.open();
        loginPage.login(USERNAME, PASSWORD);

        // First, change password
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword(NEW_PASSWORD, NEW_PASSWORD);
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();

        // Second, update profile
        updateProfilePage.assertCurrent();
        updateProfilePage.prepareUpdate().firstName(NEW_FIRST_NAME).lastName(NEW_LAST_NAME)
                .email(NEW_EMAIL).submit();
        events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.UPDATED_FIRST_NAME, NEW_FIRST_NAME)
                .detail(Details.UPDATED_LAST_NAME, NEW_LAST_NAME)
                .detail(Details.PREVIOUS_EMAIL, EMAIL)
                .detail(Details.UPDATED_EMAIL, NEW_EMAIL)
                .assertEvent();

        // Finally, accept terms
        termsPage.assertCurrent();
        termsPage.acceptTerms();
        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).removeDetail(Details.REDIRECT_URI)
                .detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID).assertEvent();

        // Logged in
        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().assertEvent();
    }

    @Test
    public void executeRequiredActionsWithCustomPriorityAppliesSamePriorityToUserActionsAndKcActionParam() {
        // Default priority is alphabetical order:
        // TermsAndConditions -> UpdatePassword -> UpdateProfile

        // After Changing the priority, the order will be:
        // UpdatePassword -> UpdateProfile -> TermsAndConditions
        final var requiredActionsCustomOrdered = List.of(
                RequiredAction.UPDATE_PASSWORD,
                RequiredAction.UPDATE_PROFILE,
                RequiredAction.TERMS_AND_CONDITIONS
        );
        ApiUtil.updateRequiredActionsOrder(testRealm(), requiredActionsCustomOrdered);

        enableRequiredActionForUser(RequiredAction.UPDATE_PASSWORD);
        // we don't enable UPDATE_PROFILE for the user, we set this as kc_action param instead
        enableRequiredActionForUser(RequiredAction.TERMS_AND_CONDITIONS);

        // Login with kc_action=UPDATE_PROFILE
        oauth.loginForm().kcAction(RequiredAction.UPDATE_PROFILE.name()).open();
        loginPage.assertCurrent(TEST_REALM_NAME);
        loginPage.login(USERNAME, PASSWORD);

        // First, change password
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword(NEW_PASSWORD, NEW_PASSWORD);
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).assertEvent();

        // Second, accept terms
        termsPage.assertCurrent();
        termsPage.acceptTerms();
        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).removeDetail(Details.REDIRECT_URI)
                .detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID).assertEvent();

        // Finally, update profile. Action specified by "kc_action" should be always triggered last
        updateProfilePage.assertCurrent();
        updateProfilePage.prepareUpdate().firstName(NEW_FIRST_NAME).lastName(NEW_LAST_NAME)
                .email(NEW_EMAIL).submit();
        events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.UPDATED_FIRST_NAME, NEW_FIRST_NAME)
                .detail(Details.UPDATED_LAST_NAME, NEW_LAST_NAME)
                .detail(Details.PREVIOUS_EMAIL, EMAIL)
                .detail(Details.UPDATED_EMAIL, NEW_EMAIL)
                .assertEvent();

        // Logged in
        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().assertEvent();
    }


    @Test
    public void executeLoginActionWithCustomPriorityAppliesSamePriorityToSessionAndUserActions()
            throws Exception {
        // Default priority is alphabetical order:
        // TermsAndConditions -> UpdatePassword -> UpdateProfile

        // After Changing the priority, the order will be:
        // UpdatePassword -> UpdateProfile -> TermsAndConditions
        final var requiredActionsCustomOrdered = List.of(
                RequiredAction.UPDATE_PASSWORD,
                RequiredAction.UPDATE_PROFILE,
                RequiredAction.TERMS_AND_CONDITIONS
        );
        ApiUtil.updateRequiredActionsOrder(testRealm(), requiredActionsCustomOrdered);

        // NOTE: we don't configure UPDATE_PASSWORD on the user - it's set on the session by the reset-password flow
        enableRequiredActionForUser(RequiredAction.UPDATE_PROFILE);
        enableRequiredActionForUser(RequiredAction.TERMS_AND_CONDITIONS);

        // Get a password reset link
        loginPage.open();
        loginPage.assertCurrent(TEST_REALM_NAME);
        loginPage.resetPassword();

        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(USERNAME);
        events.expectRequiredAction(EventType.SEND_RESET_PASSWORD).assertEvent();
        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

        assertEquals(1, greenMail.getReceivedMessages().length);
        final var message = greenMail.getLastReceivedMessage();
        final var resetUrl = getEmailLink(message);
        assertNotNull(resetUrl);
        driver.navigate().to(resetUrl);

        // First, change password
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword(NEW_PASSWORD, NEW_PASSWORD);
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();

        // Second, update profile
        updateProfilePage.assertCurrent();
        updateProfilePage.prepareUpdate().firstName(NEW_FIRST_NAME).lastName(NEW_LAST_NAME)
                .email(NEW_EMAIL).submit();
        events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.UPDATED_FIRST_NAME, NEW_FIRST_NAME)
                .detail(Details.UPDATED_LAST_NAME, NEW_LAST_NAME)
                .detail(Details.PREVIOUS_EMAIL, EMAIL)
                .detail(Details.UPDATED_EMAIL, NEW_EMAIL)
                .assertEvent();

        // Finally, accept terms
        termsPage.assertCurrent();
        termsPage.acceptTerms();
        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).removeDetail(Details.REDIRECT_URI)
                .detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID).assertEvent();

        // Logged in
        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().assertEvent();
    }

    @Test
    public void executeRequiredActionWithCustomPriorityAppliesSamePriorityToSessionAndUserActions() {
        // Default priority is alphabetical order:
        // TermsAndConditions -> VerifyProfile

        // After Changing the priority, the order will be:
        // VerifyProfile -> TermsAndConditions
        final var requiredActionsCustomOrdered = List.of(
                RequiredAction.VERIFY_PROFILE,
                RequiredAction.TERMS_AND_CONDITIONS
        );
        ApiUtil.updateRequiredActionsOrder(testRealm(), requiredActionsCustomOrdered);

        // make user profile invalid by setting lastName to empty
        final var userResource = testRealm().users().get(testUserId);
        final var user = userResource.toRepresentation();
        user.setLastName("");
        userResource.update(user);

        /* NOTE: we don't configure VERIFY_PROFILE on the user - it's set on the session because the profile is incomplete */
        enableRequiredActionForUser(RequiredAction.TERMS_AND_CONDITIONS);

        // Get a password reset link
        loginPage.open();
        loginPage.assertCurrent(TEST_REALM_NAME);
        loginPage.login(USERNAME, PASSWORD);

        // Second, complete the profile
        verifyProfilePage.assertCurrent();
        events.expectRequiredAction(EventType.VERIFY_PROFILE)
                .user(testUserId)
                .detail(Details.FIELDS_TO_UPDATE, UserModel.LAST_NAME)
                .assertEvent();

        verifyProfilePage.update(NEW_FIRST_NAME, NEW_LAST_NAME);
        events.expectRequiredAction(EventType.UPDATE_PROFILE)
                .user(testUserId)
                .detail(Details.UPDATED_FIRST_NAME, NEW_FIRST_NAME)
                .detail(Details.UPDATED_LAST_NAME, NEW_LAST_NAME)
                .assertEvent();

        // Finally, accept terms
        termsPage.assertCurrent();
        termsPage.acceptTerms();
        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).removeDetail(Details.REDIRECT_URI)
                .detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID).assertEvent();

        // Logged in
        appPage.assertCurrent();
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().assertEvent();
    }

    @Test
    public void setupTotpAfterUpdatePassword() {
        enableRequiredActionForUser(RequiredAction.CONFIGURE_TOTP);
        enableRequiredActionForUser(RequiredAction.UPDATE_PASSWORD);

        // move UPDATE_PASSWORD before top
        final var requiredActionsCustomOrdered = List.of(
                RequiredAction.UPDATE_PASSWORD,
                RequiredAction.CONFIGURE_TOTP
        );
        ApiUtil.updateRequiredActionsOrder(testRealm(), requiredActionsCustomOrdered);

        // Login
        loginPage.open();
        loginPage.login(USERNAME, PASSWORD);

        // change password
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword(NEW_PASSWORD, NEW_PASSWORD);
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, PasswordCredentialModel.TYPE).assertEvent();

        // CONFIGURE_TOTP
        totpPage.assertCurrent();

        totpPage.clickManual();
        String pageSource = driver.getPageSource();
        assertThat(pageSource, not(containsString("Unable to scan?")));
        assertThat(pageSource, containsString("Scan barcode?"));

        TimeBasedOTP totp = new TimeBasedOTP();
        totpPage.configure(totp.generateTOTP(totpPage.getTotpSecret()), "userLabel");
        events.expectRequiredAction(EventType.UPDATE_TOTP).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();
        events.expectRequiredAction(EventType.UPDATE_CREDENTIAL).detail(Details.CREDENTIAL_TYPE, OTPCredentialModel.TYPE).assertEvent();

        // Logged in
        appPage.assertCurrent();
        assertThat(appPage.getRequestType(), is(RequestType.AUTH_RESPONSE));
        events.expectLogin().assertEvent();

    }

    @Test
    public void skipToNextRequiredActionWithCustomPriority() {
        enableRequiredActionForUser(RequiredAction.VERIFY_EMAIL);
        enableRequiredActionForUser(RequiredAction.UPDATE_PASSWORD);

        RealmRepresentation realmRep = testRealm().toRepresentation();
        realmRep.setVerifyEmail(true);
        testRealm().update(realmRep);

        final var userResource = testRealm().users().get(testUserId);
        final var user = userResource.toRepresentation();
        user.setEmailVerified(true);
        userResource.update(user);

        final var requiredActionsCustomOrdered = List.of(
                RequiredAction.VERIFY_EMAIL,
                RequiredAction.UPDATE_PASSWORD
        );
        ApiUtil.updateRequiredActionsOrder(testRealm(), requiredActionsCustomOrdered);

        // Login
        loginPage.open();
        loginPage.login(USERNAME, PASSWORD);
        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).assertEvent();

        // change password
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword(NEW_PASSWORD, NEW_PASSWORD);
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        appPage.assertCurrent();
        assertThat(appPage.getRequestType(), is(RequestType.AUTH_RESPONSE));
        events.expect(EventType.UPDATE_CREDENTIAL).assertEvent();
    }

    private void enableRequiredActionForUser(final RequiredAction requiredAction) {
        setRequiredActionEnabled(TEST_REALM_NAME, testUserId, requiredAction.name(), true);
    }

}
