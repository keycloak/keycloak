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

import jakarta.mail.Address;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AppInitiatedActionUpdateEmailWithVerificationTest extends AbstractAppInitiatedActionUpdateEmailTest {

	@Rule
	public GreenMailRule greenMail = new GreenMailRule();

	@Page
	protected InfoPage infoPage;

	@Page
	protected ErrorPage errorPage;

	@Override
	public void configureTestRealm(RealmRepresentation testRealm) {
		testRealm.setVerifyEmail(true);
	}

	@Override
	protected void prepareUser(UserRepresentation user) {
		user.setEmailVerified(true);
	}

	@Override
	protected void changeEmailUsingAIA(String newEmail) throws Exception {
		doAIA();
		loginPage.login("test-user@localhost", "password");

		emailUpdatePage.assertCurrent();
		assertTrue(emailUpdatePage.isCancelDisplayed());
		emailUpdatePage.changeEmail(newEmail);

		events.expect(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, newEmail).assertEvent();
		Assert.assertEquals("test-user@localhost", ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost").getEmail());

		driver.navigate().to(fetchEmailConfirmationLink(newEmail));

		infoPage.assertCurrent();
		assertEquals(String.format("The account email has been successfully updated to %s.", newEmail), infoPage.getInfo());
	}

	@Test
	public void updateEmail() throws Exception {
		changeEmailUsingAIA("new@localhost");

		events.expect(EventType.UPDATE_EMAIL)
				.detail(Details.PREVIOUS_EMAIL, "test-user@localhost")
				.detail(Details.UPDATED_EMAIL, "new@localhost");

		Assert.assertEquals("new@localhost", ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost").getEmail());
	}

	@Test
	public void confirmEmailUpdateAfterThirdPartyEmailUpdate() throws MessagingException, IOException {
		doAIA();
		loginPage.login("test-user@localhost", "password");

		emailUpdatePage.assertCurrent();
		emailUpdatePage.changeEmail("new@localhost");

		String confirmationLink = fetchEmailConfirmationLink("new@localhost");

		UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		user.setEmail("very-new@localhost");
		user.setEmailVerified(true);
		testRealm().users().get(user.getId()).update(user);

		driver.navigate().to(confirmationLink);

		errorPage.assertCurrent();
		assertEquals("The link you clicked is an old stale link and is no longer valid. Maybe you have already verified your email.", errorPage.getError());
	}

	@Test
	public void confirmEmailAfterDuplicateEmailSetForThirdPartyAccount() throws MessagingException, IOException {
		doAIA();
		loginPage.login("test-user@localhost", "password");

		emailUpdatePage.assertCurrent();
		emailUpdatePage.changeEmail("new@localhost");

		String confirmationLink = fetchEmailConfirmationLink("new@localhost");

		UserRepresentation otherUser = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost");
		otherUser.setEmail("new@localhost");
		otherUser.setEmailVerified(true);
		testRealm().users().get(otherUser.getId()).update(otherUser);

		driver.navigate().to(confirmationLink);

		errorPage.assertCurrent();
		assertEquals("Email already exists.", errorPage.getError());
	}

	private String fetchEmailConfirmationLink(String emailRecipient) throws MessagingException, IOException {
		MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		Assert.assertEquals(1, receivedMessages.length);
		MimeMessage message = receivedMessages[0];
		Address[] recipients = message.getRecipients(Message.RecipientType.TO);
		Assert.assertTrue(recipients.length >= 1);
		assertEquals(emailRecipient, recipients[0].toString());

		return MailUtils.getPasswordResetEmailLink(message).trim();
	}

	@Test
	public void updateEmailWithRedirect() throws Exception {
		doAIA();
		loginPage.login("test-user@localhost", "password");

		emailUpdatePage.assertCurrent();
		assertTrue(emailUpdatePage.isCancelDisplayed());
		emailUpdatePage.changeEmail("new@localhost");

		events.expect(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, "new@localhost").assertEvent();
		Assert.assertEquals("test-user@localhost", ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost").getEmail());
		String link = fetchEmailConfirmationLink("new@localhost");
		String token = link.substring(link.indexOf("key=") + "key=".length()).split("&")[0];
		try {
			final AccessToken actionTokenVerifyEmail = TokenVerifier.create(token, AccessToken.class).getToken();
			//Issue #14860
			assertEquals("test-app", actionTokenVerifyEmail.getIssuedFor());
		} catch (VerificationException e) {
			throw new IOException(e);
		}
		driver.navigate().to(link);

		infoPage.assertCurrent();
		assertEquals(String.format("The account email has been successfully updated to %s.", "new@localhost"), infoPage.getInfo());
		//Issue #15136
		final WebElement backToApplicationLink = driver.findElement(By.linkText("« Back to Application"));
		assertThat(backToApplicationLink.getDomAttribute("href"), Matchers.containsString("/auth/realms/master/app/auth"));

		events.expect(EventType.UPDATE_EMAIL)
				.detail(Details.PREVIOUS_EMAIL, "test-user@localhost")
				.detail(Details.UPDATED_EMAIL, "new@localhost");
		Assert.assertEquals("new@localhost", ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost").getEmail());
	}
}
