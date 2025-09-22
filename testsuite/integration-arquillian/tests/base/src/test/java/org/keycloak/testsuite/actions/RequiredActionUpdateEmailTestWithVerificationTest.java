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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import jakarta.ws.rs.core.Response.Status;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.actiontoken.updateemail.UpdateEmailActionToken;
import org.keycloak.authentication.requiredactions.UpdateEmail;
import org.keycloak.broker.provider.util.SimpleHttp.Response;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
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
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.WaitUtils;

public class RequiredActionUpdateEmailTestWithVerificationTest extends AbstractRequiredActionUpdateEmailTest {

	@Rule
	public GreenMailRule greenMail = new GreenMailRule();

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
	protected void changeEmailUsingRequiredAction(String newEmail, boolean logoutOtherSessions) throws Exception {
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

		driver.navigate().to(fetchEmailConfirmationLink(newEmail));

		infoPage.assertCurrent();
		assertEquals("The account email has been successfully updated to new@localhost.", infoPage.getInfo());
		infoPage.clickBackToApplicationLink();
		WaitUtils.waitForPageToLoad();
		assertEquals(redirectUri, driver.getCurrentUrl());
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
		changeEmailUsingRequiredAction("new@localhost", logoutOtherSessions);

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
            try (Response response = SimpleHttpDefault.doHead(confirmationLink, httpClient).asResponse()) {
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
            assertTrue(driver.getPageSource().contains("You need to update your email address to activate your account."));
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
            assertEquals(RequiredAction.VERIFY_EMAIL.name(), user.getRequiredActions().get(0));
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
}
