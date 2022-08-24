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

import static org.junit.Assert.assertFalse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.auth.page.login.UpdateEmailPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.UserBuilder;

@EnableFeature(Profile.Feature.UPDATE_EMAIL)
public abstract class AbstractRequiredActionUpdateEmailTest extends AbstractTestRealmKeycloakTest {

	@Rule
	public AssertEvents events = new AssertEvents(this);

	@Page
	protected LoginPage loginPage;

	@Page
	protected UpdateEmailPage updateEmailPage;

	@Page
	protected AppPage appPage;

	@Before
	public void beforeTest() {
		ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
		UserRepresentation user = UserBuilder.create().enabled(true)
				.username("test-user@localhost")
				.email("test-user@localhost")
				.firstName("Tom")
				.lastName("Brady")
				.requiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).build();
		prepareUser(user);
		ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");

		ApiUtil.removeUserByUsername(testRealm(), "john-doh@localhost");
		user = UserBuilder.create().enabled(true)
				.username("john-doh@localhost")
				.email("john-doh@localhost")
				.firstName("John")
				.lastName("Doh")
				.requiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).build();
		prepareUser(user);
		ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
	}

	protected void prepareUser(UserRepresentation user){

	}

	@Override
	public void configureTestRealm(RealmRepresentation testRealm) {

	}

	@Test
	public void cancelIsNotDisplayed(){
		loginPage.open();

		loginPage.login("test-user@localhost", "password");

		updateEmailPage.assertCurrent();
		assertFalse(updateEmailPage.isCancelDisplayed());
	}

	@Test
	public void updateEmailMissing() {
		loginPage.open();

		loginPage.login("test-user@localhost", "password");

		updateEmailPage.assertCurrent();

		updateEmailPage.changeEmail("");

		updateEmailPage.assertCurrent();

		// assert that form holds submitted values during validation error
		Assert.assertEquals("", updateEmailPage.getEmail());

		Assert.assertEquals("Please specify email.", updateEmailPage.getEmailInputError());

		events.assertEmpty();
	}

	@Test
	public void updateEmailDuplicate() {
		loginPage.open();

		loginPage.login("john-doh@localhost", "password");

		updateEmailPage.assertCurrent();

		updateEmailPage.changeEmail("test-user@localhost");

		updateEmailPage.assertCurrent();

		// assert that form holds submitted values during validation error
		Assert.assertEquals("test-user@localhost", updateEmailPage.getEmail());

		Assert.assertEquals("Email already exists.", updateEmailPage.getEmailInputError());

		events.assertEmpty();
	}

	@Test
	public void updateEmailInvalid() {
		loginPage.open();

		loginPage.login("test-user@localhost", "password");

		updateEmailPage.assertCurrent();

		updateEmailPage.changeEmail("invalid");

		updateEmailPage.assertCurrent();

		// assert that form holds submitted values during validation error
		Assert.assertEquals("invalid", updateEmailPage.getEmail());

		Assert.assertEquals("Invalid email address.", updateEmailPage.getEmailInputError());

		events.assertEmpty();
	}
}
