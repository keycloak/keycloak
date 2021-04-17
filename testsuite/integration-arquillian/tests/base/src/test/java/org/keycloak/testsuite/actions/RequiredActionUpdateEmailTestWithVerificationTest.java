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
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;

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

	@Test
	public void updateEmail() throws IOException, MessagingException {
		loginPage.open();

		loginPage.login("test-user@localhost", "password");

		updateEmailPage.assertCurrent();
		updateEmailPage.changeEmail("new@localhost");

		events.expect(EventType.SEND_VERIFY_EMAIL).detail(Details.EMAIL, "new@localhost").assertEvent();
		UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		assertEquals("test-user@localhost", user.getEmail());
		assertTrue(user.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_EMAIL.name()));

		driver.navigate().to(fetchEmailConfirmationLink("new@localhost"));

		infoPage.assertCurrent();
		assertEquals("The account email has been successfully updated to new@localhost.", infoPage.getInfo());

		events.expect(EventType.UPDATE_EMAIL)
				.detail(Details.PREVIOUS_EMAIL, "test-user@localhost")
				.detail(Details.UPDATED_EMAIL, "new@localhost");

		user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		assertEquals("new@localhost", user.getEmail());
		assertFalse(user.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_EMAIL.name()));
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

	private String fetchEmailConfirmationLink(String emailRecipient) throws MessagingException, IOException {
		MimeMessage[] receivedMessages = greenMail.getReceivedMessages();
		assertEquals(1, receivedMessages.length);
		MimeMessage message = receivedMessages[0];
		Address[] recipients = message.getRecipients(Message.RecipientType.TO);
		assertTrue(recipients.length >= 1);
		assertEquals(emailRecipient, recipients[0].toString());

		return MailUtils.getPasswordResetEmailLink(message).trim();
	}
}
