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
package org.keycloak.testsuite.actions;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.actiontoken.updateemail.UpdateEmailActionToken;
import org.keycloak.authentication.requiredactions.UpdateEmail;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class RequiredActionUpdateEmailTestWithVerificationTest extends AbstractRequiredActionUpdateEmailTest {

	@Rule
	public GreenMailRule greenMail = new GreenMailRule();

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

	@Page
	private InfoPage infoPage;

	@Page
	private ErrorPage errorPage;

	protected void prepareUser(UserRepresentation user){
		user.setEmailVerified(true);
	}

	@Override
	public void configureTestRealm(RealmRepresentation testRealm) {
		testRealm.setVerifyEmail(true);
	}

	@Override
	protected void changeEmailUsingRequiredAction(String newEmail, boolean logoutOtherSessions, boolean newEmailAsUsername) throws Exception {
		String redirectUri = OAuthClient.APP_ROOT + "/auth?nonce=" + UUID.randomUUID();
		oauth.redirectUri(redirectUri);
		loginPage.open();

		loginPage.login("test-user@localhost", "password");
        updateEmailPage.assertCurrent();

        if (logoutOtherSessions) {
            updateEmailPage.checkLogoutSessions();
        }

        Assert.assertEquals(logoutOtherSessions, updateEmailPage.isLogoutSessionsChecked());
        updateEmailPage.changeEmail(newEmail);

		events.expect(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, newEmail).assertEvent();
		UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		assertEquals("test-user@localhost", user.getEmail());
		assertTrue(user.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_EMAIL.name()));
        assertNotEquals(newEmail, user.getEmail());
        assertTrue(user.isEmailVerified());
        Map<String, List<String>> attributes = user.getAttributes();
        assertNotNull(attributes.get(UserModel.EMAIL_PENDING));
        assertEquals(1, attributes.get(UserModel.EMAIL_PENDING).size());
        assertEquals(newEmail, attributes.get(UserModel.EMAIL_PENDING).get(0));

		driver.navigate().to(fetchEmailConfirmationLink(newEmail));

		infoPage.assertCurrent();
		assertEquals("The account email has been successfully updated to new@localhost.", infoPage.getInfo());
		infoPage.clickBackToApplicationLink();
		WaitUtils.waitForPageToLoad();
		assertEquals(redirectUri, driver.getCurrentUrl());

        if (newEmailAsUsername) {
            user = ActionUtil.findUserWithAdminClient(adminClient, newEmail);
        } else {
            user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        }
        attributes = user.getAttributes();
        assertTrue(attributes == null || !attributes.containsKey(UserModel.EMAIL_PENDING));
        assertEquals(newEmail, user.getEmail());
        assertTrue(user.isEmailVerified());
	}

	private void updateEmail(boolean logoutOtherSessions) throws Exception {
		// login using another session
		configureRequiredActionsToUser("test-user@localhost");
		UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
		OAuthClient oauth2 = oauth.newConfig().driver(driver2);
		oauth2.doLogin("test-user@localhost", "password");
		EventRepresentation event1 = events.expectLogin().assertEvent();
		assertEquals(1, testUser.getUserSessions().size());

		// add action and change email
		configureRequiredActionsToUser("test-user@localhost", UserModel.RequiredAction.UPDATE_EMAIL.name());
		changeEmailUsingRequiredAction("new@localhost", logoutOtherSessions, false);

		if (logoutOtherSessions) {
			events.expectLogout(event1.getSessionId())
					.detail(Details.REDIRECT_URI,  getAuthServerContextRoot() + "/auth/realms/test/account/")
					.detail(Details.LOGOUT_TRIGGERED_BY_ACTION_TOKEN, UpdateEmailActionToken.TOKEN_TYPE)
					.assertEvent();
		}

		events.expect(EventType.UPDATE_EMAIL)
				.detail(Details.PREVIOUS_EMAIL, "test-user@localhost")
				.detail(Details.UPDATED_EMAIL, "new@localhost")
				.assertEvent();

		List<UserSessionRepresentation> sessions = testUser.getUserSessions();
		if (logoutOtherSessions) {
			assertEquals(0, sessions.size());
		} else {
			assertEquals(1, sessions.size());
			assertEquals(event1.getSessionId(), sessions.iterator().next().getId());
		}

		UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		assertEquals("new@localhost", user.getEmail());
		assertFalse(user.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_EMAIL.name()));
	}

    @Test
    public void updateEmailLogoutSessionsChecked() throws Exception {
            updateEmail(true);
    }

    @Test
    public void updateEmailLogoutSessionsNotChecked() throws Exception {
            updateEmail(false);
    }

    @Test
    public void pendingVerificationIsNotDisplayedOnFirstVisit() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateEmailPage.assertCurrent();

        // Verify no pending verification message is shown on first visit
        assertThat(updateEmailPage.getInfo(), not(containsString("A verification email was sent to")));
    }

	@Test
	public void confirmEmailUpdateAfterThirdPartyEmailUpdate() throws MessagingException, IOException {
		loginPage.open();
		loginPage.login("test-user@localhost", "password");

		updateEmailPage.assertCurrent();
		updateEmailPage.changeEmail("new@localhost");

		String confirmationLink = fetchEmailConfirmationLink("new@localhost");

		UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		user.setEmail("very-new@localhost");
		user.setEmailVerified(true);
		testRealm().users().get(user.getId()).update(user);

		driver.navigate().to(confirmationLink);

		errorPage.assertCurrent();
		assertEquals("The link you clicked is an old stale link and is no longer valid. Maybe you have already verified your email.", errorPage.getError());
		assertTrue(ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost").getRequiredActions().contains(UserModel.RequiredAction.UPDATE_EMAIL.name()));
	}

    @Test
    public void testSkipHeadRequestWhenFollowingVerificationLink() throws MessagingException, IOException {
        oauth.openLoginForm();
        loginPage.login("test-user@localhost", "password");

        updateEmailPage.assertCurrent();
        updateEmailPage.changeEmail("new@localhost");

        String confirmationLink = fetchEmailConfirmationLink("new@localhost");

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            try (SimpleHttpResponse response = SimpleHttpDefault.doHead(confirmationLink, httpClient).asResponse()) {
                assertEquals(Status.OK.getStatusCode(), response.getStatus());
            }
        }

        driver.navigate().to(confirmationLink);
        infoPage.assertCurrent();
    }

    @Test
    public void testForceEmailVerification() throws MessagingException, IOException {
        // disables verify email at the realm level
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setVerifyEmail(false);
        testRealm().update(realm);

        // force email verification when updating the email
        AuthenticationManagementResource authMgt = testRealm().flows();
        RequiredActionProviderRepresentation requiredAction = authMgt.getRequiredActions().stream()
                .filter(action -> RequiredAction.UPDATE_EMAIL.name().equals(action.getAlias()))
                .findAny().get();
        requiredAction.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.TRUE.toString());
        authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);

        try {
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            updateEmailPage.assertCurrent();
            updateEmailPage.changeEmail("new@localhost");
            String confirmationLink = fetchEmailConfirmationLink("new@localhost");
            driver.navigate().to(confirmationLink);
            infoPage.assertCurrent();
        } finally {
            realm.setVerifyEmail(true);
            testRealm().update(realm);
            requiredAction.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.FALSE.toString());
            authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);
        }
    }

    @Test
    public void testForceEmailVerificationWhenUpdatingEmailInUpdateProfileContext() throws MessagingException, IOException {
        // disables verify email at the realm level
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setVerifyEmail(false);
        testRealm().update(realm);

        // force email verification when updating the email
        AuthenticationManagementResource authMgt = testRealm().flows();
        RequiredActionProviderRepresentation requiredAction = authMgt.getRequiredActions().stream()
                .filter(action -> RequiredAction.UPDATE_EMAIL.name().equals(action.getAlias()))
                .findAny().get();
        requiredAction.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.TRUE.toString());
        authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);

        try {
            UserRepresentation user = testRealm().users().search("test-user@localhost").get(0);
            user.setEmail("");
            user.setRequiredActions(List.of(RequiredAction.UPDATE_PROFILE.name()));
            testRealm().users().get(user.getId()).update(user);

            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            updateProfilePage.assertCurrent();
            updateProfilePage.update("f", "l", "new-email@localhost");
            String confirmationLink = fetchEmailConfirmationLink("new-email@localhost");
            driver.navigate().to(confirmationLink);
            infoPage.assertCurrent();

            user = testRealm().users().search("test-user@localhost").get(0);
            assertEquals("new-email@localhost", user.getEmail());
        } finally {
            realm.setVerifyEmail(true);
            testRealm().update(realm);
            requiredAction.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.FALSE.toString());
            authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);
        }
    }

    @Test
    public void testForceEmailVerificationWhenUpdatingEmailInUpdateProfileContextIfEmailNotConfirmed() throws MessagingException, IOException {
        // disables verify email at the realm level
        RealmRepresentation realm = testRealm().toRepresentation();
        realm.setVerifyEmail(false);
        testRealm().update(realm);

        // force email verification when updating the email
        AuthenticationManagementResource authMgt = testRealm().flows();
        RequiredActionProviderRepresentation requiredAction = authMgt.getRequiredActions().stream()
                .filter(action -> RequiredAction.UPDATE_EMAIL.name().equals(action.getAlias()))
                .findAny().get();
        requiredAction.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.TRUE.toString());
        authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);

        try {
            UserRepresentation user = testRealm().users().search("test-user@localhost").get(0);
            user.setEmail("");
            user.setRequiredActions(List.of(RequiredAction.UPDATE_PROFILE.name()));
            testRealm().users().get(user.getId()).update(user);

            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            updateProfilePage.assertCurrent();
            updateProfilePage.update("f", "l", "new-email@localhost");
            String confirmationLink = fetchEmailConfirmationLink("new-email@localhost");
            assertNotNull(confirmationLink);
            // logout to check if update email required action will be executed
            testRealm().users().get(user.getId()).logout();
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            // user is forced to update the email because it was not yet confirmed
            // The pending verification message takes precedence and is more informative
            assertThat(updateEmailPage.getInfo(), containsString("A verification email was sent to new-email@localhost"));
            updateEmailPage.clickSubmitAction();
            confirmationLink = fetchEmailConfirmationLink("new-email@localhost", greenMail.getLastReceivedMessage());
            driver.navigate().to(confirmationLink);
            infoPage.assertCurrent();

            user = testRealm().users().search("test-user@localhost").get(0);
            assertEquals("new-email@localhost", user.getEmail());
        } finally {
            realm.setVerifyEmail(true);
            testRealm().update(realm);
            requiredAction.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.FALSE.toString());
            authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);
        }
    }

    @Test
    public void testForceEmailVerificationWhenUpdatingEmailInUpdateProfileContextWhenVerifyEmailEnabled() {
        // force email verification when updating the email
        AuthenticationManagementResource authMgt = testRealm().flows();
        RequiredActionProviderRepresentation requiredAction = authMgt.getRequiredActions().stream()
                .filter(action -> RequiredAction.UPDATE_EMAIL.name().equals(action.getAlias()))
                .findAny().get();
        requiredAction.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.TRUE.toString());
        authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);

        try {
            UserRepresentation user = testRealm().users().search("test-user@localhost").get(0);
            user.setEmail("");
            user.setRequiredActions(List.of(RequiredAction.UPDATE_PROFILE.name()));
            testRealm().users().get(user.getId()).update(user);

            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            updateProfilePage.assertCurrent();
            updateProfilePage.update("f", "l", "new-email@localhost");

            user = testRealm().users().search("test-user@localhost").get(0);
            assertEquals(1, user.getRequiredActions().size());
            // When UPDATE_EMAIL is configured with forced verification, it takes precedence over VERIFY_EMAIL
            assertEquals(RequiredAction.UPDATE_EMAIL.name(), user.getRequiredActions().get(0));
        } finally {
            requiredAction.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.FALSE.toString());
            authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);
        }
    }

	@Test
	public void confirmEmailAfterDuplicateEmailSetForThirdPartyAccount() throws MessagingException, IOException {
		loginPage.open();
		loginPage.login("test-user@localhost", "password");

		updateEmailPage.assertCurrent();
		updateEmailPage.changeEmail("new@localhost");

		String confirmationLink = fetchEmailConfirmationLink("new@localhost");

		UserRepresentation otherUser = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost");
		otherUser.setEmail("new@localhost");
		otherUser.setEmailVerified(true);
		testRealm().users().get(otherUser.getId()).update(otherUser);

		driver.navigate().to(confirmationLink);

		errorPage.assertCurrent();
		assertEquals("Email already exists.", errorPage.getError());
		assertTrue(ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost").getRequiredActions().contains(UserModel.RequiredAction.UPDATE_EMAIL.name()));
	}

    @Test
    public void testForceEmailVerificationWithUpdateProfileAndExistingEmail() {
        // Save original configuration to restore later
        RequiredActionConfigRepresentation originalConfig = testRealm().flows().getRequiredActionConfig(UserModel.RequiredAction.UPDATE_EMAIL.name());

        try {
            // Configure UPDATE_EMAIL to force email verification
            RequiredActionConfigRepresentation config = new RequiredActionConfigRepresentation();
            config.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.TRUE.toString());
            testRealm().flows().updateRequiredActionConfig(UserModel.RequiredAction.UPDATE_EMAIL.name(), config);

            // Create user with UPDATE_PROFILE required action
            UserRepresentation user = UserBuilder.create()
                    .enabled(true)
                    .username("profile-test-user@localhost")
                    .email("profile-test-user@localhost")
                    .firstName("Tom")
                    .lastName("Brady")
                    .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name())
                    .build();
            ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");

            // Login and update profile (first and last name only, no email)
            loginPage.open();
            loginPage.login("profile-test-user@localhost", "password");

            updateProfilePage.update("Updated", "Name");

            if (errorPage.isCurrent()) {
                fail("There should not be an exception thrown. Error: " + errorPage.getError());
            }
        } finally {
            // Always restore original configuration
            testRealm().flows().updateRequiredActionConfig(UserModel.RequiredAction.UPDATE_EMAIL.name(), originalConfig);
            events.clear();
            ApiUtil.removeUserByUsername(testRealm(), "profile-test-user@localhost");
        }
    }

	private String fetchEmailConfirmationLink(String emailRecipient) throws MessagingException, IOException {
		MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		assertEquals(1, receivedMessages.length);
		MimeMessage message = receivedMessages[0];
        return fetchEmailConfirmationLink(emailRecipient, message);
    }

    private String fetchEmailConfirmationLink(String emailRecipient, MimeMessage message) throws MessagingException, IOException {
        Address[] recipients = message.getRecipients(Message.RecipientType.TO);
        assertTrue(recipients.length >= 1);
        assertEquals(emailRecipient, recipients[0].toString());

        return MailUtils.getPasswordResetEmailLink(message).trim();
    }

    @Test
    public void testEmailVerificationPendingMessageOnReAuthentication() throws MessagingException, IOException {
        // Save original configuration to restore later
        RequiredActionConfigRepresentation originalConfig = testRealm().flows().getRequiredActionConfig(UserModel.RequiredAction.UPDATE_EMAIL.name());
        
        try {
            // Configure UPDATE_EMAIL to force email verification
            RequiredActionConfigRepresentation config = new RequiredActionConfigRepresentation();
            config.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.TRUE.toString());
            testRealm().flows().updateRequiredActionConfig(UserModel.RequiredAction.UPDATE_EMAIL.name(), config);

            // Create user with empty email and UPDATE_PROFILE required action
            UserRepresentation user = UserBuilder.create()
                    .enabled(true)
                    .username("pendinguser")
                    .email("") // Start with empty email
                    .firstName("John")
                    .lastName("Doe")
                    .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name())
                    .build();
            ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");

            loginPage.open();
            loginPage.login("pendinguser", "password");
            updateProfilePage.assertCurrent();
            updateProfilePage.update("John", "Doe", "pending@localhost");

            // Verification email should be sent and email should be set
            UserRepresentation updatedUser = testRealm().users().get(findUser("pendinguser").getId()).toRepresentation();
            assertNull("Email should be not set immediately", updatedUser.getEmail());

            assertTrue("User should have UPDATE_EMAIL required action", 
                      updatedUser.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_EMAIL.name()));

            infoPage.assertCurrent();
            
            // Check that the email confirmation sent message is displayed
            assertEquals("A confirmation email has been sent to pending@localhost. You must follow the instructions of the former to complete the email update.", 
                        infoPage.getInfo());

            loginPage.open();
            loginPage.login("pendinguser", "password");

            // Should be on UPDATE_EMAIL page with pending verification message
            updateEmailPage.assertCurrent();
            
            // Check that the pending verification message is displayed
            assertThat("Should show pending verification message", 
                      updateEmailPage.getInfo(), containsString("A verification email was sent to pending@localhost"));
            
            // Check that the email field is pre-filled with the pending email, not the old email
            assertEquals("Email field should be pre-filled with pending email", 
                        "pending@localhost", updateEmailPage.getEmail());

            updateEmailPage.changeEmail("pending@localhost"); // Same email to resend
            
            // Should send verification email
            String confirmationLink = fetchEmailConfirmationLink("pending@localhost", greenMail.getLastReceivedMessage());
            assertNotNull("Should have received verification email", confirmationLink);

        } finally {
            // Always restore original configuration and clean up
            testRealm().flows().updateRequiredActionConfig(UserModel.RequiredAction.UPDATE_EMAIL.name(), originalConfig);
            events.clear();
            ApiUtil.removeUserByUsername(testRealm(), "pendinguser");
        }
    }

    @Test
    public void testPendingVerificationMessageWithRealmVerificationEnabled() throws MessagingException, IOException {
        try {
            // Create user with verified email and UPDATE_EMAIL required action
            UserRepresentation user = UserBuilder.create()
                    .enabled(true)
                    .username("realmverifyuser")
                    .email("realmverifyuser@localhost")
                    .firstName("John")
                    .lastName("Doe")
                    .emailVerified(true)
                    .requiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name())
                    .build();
            ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");

            // Step 1: Login and change email (triggers verification due to realm verification setting)
            loginPage.open();
            loginPage.login("realmverifyuser", "password");
            updateEmailPage.assertCurrent();
            
            // Verify no pending message on first visit
            assertThat("Should not show pending message on first visit", 
                      updateEmailPage.getInfo(), not(containsString("A verification email was sent to")));
            
            updateEmailPage.changeEmail("realmverify@localhost");

            // Should send verification email and show confirmation
            events.expect(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, "realmverify@localhost").user(findUser("realmverifyuser").getId()).assertEvent();
            String confirmationLink = fetchEmailConfirmationLink("realmverify@localhost");
            assertNotNull("Should have received verification email", confirmationLink);

            // Step 2: Logout and login again (should show pending verification message)
            testRealm().users().get(findUser("realmverifyuser").getId()).logout();
            loginPage.open();
            loginPage.login("realmverifyuser", "password");

            // Should be on UPDATE_EMAIL page with pending verification message
            updateEmailPage.assertCurrent();
            
            // Check that the pending verification message is displayed
            assertThat("Should show pending verification message with realm verification enabled", 
                      updateEmailPage.getInfo(), containsString("A verification email was sent to realmverify@localhost"));

            // Step 3: Complete verification to ensure cache is cleared
            driver.navigate().to(confirmationLink);
            infoPage.assertCurrent();

        } finally {
            // Clean up
            events.clear();
            ApiUtil.removeUserByUsername(testRealm(), "realmverifyuser");
        }
    }

    @Test
    public void testUpdateProfileWithVerificationWhenEmailIsNotSetAndIsWritable() throws MessagingException, IOException {
        configureRequiredActionsToUser("test-user@localhost", RequiredAction.UPDATE_PROFILE.name());
        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        assertEquals(1, testUser.toRepresentation().getRequiredActions().size());
        UserRepresentation rep = testUser.toRepresentation();
        rep.setEmail("");
        testUser.update(rep);

        // login and update profile, email is empty and writable, so email input should be present
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        updateProfilePage.assertCurrent();
        assertTrue(updateProfilePage.isEmailInputPresent());
        updateProfilePage.update("Tom", "Brady", "test-user@localhost");

        // Should send verification email and show pending verification message
        assertThat("Should show pending verification message with realm verification enabled",
                driver.getPageSource(), containsString("A confirmation email has been sent to test-user@localhost."));
        String confirmationLink = fetchEmailConfirmationLink("test-user@localhost");
        rep = testUser.toRepresentation();
        assertEquals(1, rep.getRequiredActions().size());
        assertEquals(RequiredAction.UPDATE_EMAIL.name(), rep.getRequiredActions().get(0));
        assertEquals("test-user@localhost", testUser.toRepresentation().getAttributes().get(UserModel.EMAIL_PENDING).get(0));
        assertNull(testUser.toRepresentation().getEmail());

        // confirm the email and authenticate to the app
        driver.navigate().to(confirmationLink);
        infoPage.assertCurrent();
        infoPage.clickBackToApplicationLink();
        appPage.assertCurrent();
    }

    @Test
    public void testEmailVerificationCancelledByAdmin() throws Exception {
        configureRequiredActionsToUser("test-user@localhost", UserModel.RequiredAction.UPDATE_EMAIL.name());

        loginPage.open();

        loginPage.login("test-user@localhost", "password");
        updateEmailPage.assertCurrent();
        updateEmailPage.changeEmail("new@localhost");

        events.expect(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, "new@localhost").assertEvent();
        
        // Verify EMAIL_PENDING attribute is set
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Map<String, List<String>> attributes = user.getAttributes();
        assertEquals("EMAIL_PENDING should contain new email", "new@localhost", attributes.get(UserModel.EMAIL_PENDING).get(0));
        assertTrue("User should have UPDATE_EMAIL required action", user.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_EMAIL.name()));

        String confirmationLink = fetchEmailConfirmationLink("new@localhost");
        assertNotNull("Should have received verification email", confirmationLink);
        
        // Admin sets EMAIL_PENDING to empty string (simulating admin UI removal)
        user.singleAttribute(UserModel.EMAIL_PENDING, "");
        testRealm().users().get(user.getId()).update(user);

        driver.navigate().to(confirmationLink);

        errorPage.assertCurrent();
        assertEquals("This email verification has been cancelled by an administrator.", errorPage.getError());
    }

    @Test
    public void testUpdateEmailVerificationResendTooFast() throws Exception {
        UserRepresentation testUser = testRealm().users().search("test-user@localhost").get(0);
        
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        updateEmailPage.assertCurrent();
        updateEmailPage.changeEmail("newemail@localhost");
        
        // First email should be sent
        assertEquals(1, greenMail.getReceivedMessages().length);
        assertThat("Should show pending verification message",
                driver.getPageSource(), containsString("A confirmation email has been sent to newemail@localhost."));

        // Logout and login again to get back to update email page for resend
        testRealm().users().get(testUser.getId()).logout();
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        updateEmailPage.assertCurrent();

        // Try to resend immediately - should be blocked by cooldown
        updateEmailPage.changeEmail("newemail@localhost");
        assertThat("Should show cooldown error message", 
                driver.getPageSource(), containsString("You must wait"));
        assertEquals("Email should not be sent again due to cooldown", 1, greenMail.getReceivedMessages().length);
        
        // Check that email field is pre-filled with the pending email after cooldown error
        assertEquals("Email field should be pre-filled with pending email during cooldown", 
                "newemail@localhost", updateEmailPage.getEmail());

        try {
            // Move time forward beyond cooldown period (default 30 seconds)
            setTimeOffset(40);
            
            // Logout and login again to retry after cooldown
            testRealm().users().get(testUser.getId()).logout();
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            updateEmailPage.assertCurrent();
            
            // Now resend should work
            updateEmailPage.changeEmail("newemail@localhost");
            assertEquals("Second email should be sent after cooldown expires", 2, greenMail.getReceivedMessages().length);
        } finally {
            setTimeOffset(0);
        }
    }
}
