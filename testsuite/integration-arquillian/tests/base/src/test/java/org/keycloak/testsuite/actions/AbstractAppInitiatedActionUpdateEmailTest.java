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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.common.Profile;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.pages.EmailUpdatePage;
import org.keycloak.testsuite.util.UserBuilder;

@EnableFeature(Profile.Feature.UPDATE_EMAIL)
public abstract class AbstractAppInitiatedActionUpdateEmailTest extends AbstractAppInitiatedActionTest {

	@Page
	protected EmailUpdatePage emailUpdatePage;

	@Override
	protected String getAiaAction() {
		return UserModel.RequiredAction.UPDATE_EMAIL.name();
	}

	@Override
	public void configureTestRealm(RealmRepresentation testRealm) {
	}

	@Before
	public void beforeTest() {
		ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
		UserRepresentation user = UserBuilder.create().enabled(true).username("test-user@localhost")
				.email("test-user@localhost").firstName("Tom").lastName("Brady").build();
		prepareUser(user);
		ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");

		ApiUtil.removeUserByUsername(testRealm(), "john-doh@localhost");
		user = UserBuilder.create().enabled(true).username("john-doh@localhost").email("john-doh@localhost").firstName("John")
				.lastName("Doh").build();
		prepareUser(user);
		ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
	}

	protected void prepareUser(UserRepresentation user){

	}

	@Test
	public void cancelUpdateEmail() {
		doAIA();

		loginPage.login("test-user@localhost", "password");

		emailUpdatePage.assertCurrent();
		emailUpdatePage.cancel();

		assertKcActionStatus("cancelled");

		// assert nothing was updated in persistent store
		UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		Assert.assertEquals("test-user@localhost", user.getEmail());
	}

	@Test
	public void updateToExistingEmail() {
		doAIA();

		loginPage.login("test-user@localhost", "password");

		emailUpdatePage.assertCurrent();
		emailUpdatePage.changeEmail("john-doh@localhost");
		emailUpdatePage.assertCurrent();

		Assert.assertEquals("Email already exists.", emailUpdatePage.getEmailError());

		UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		Assert.assertEquals("test-user@localhost", user.getEmail());
	}

	@Test
	public void updateToInvalidEmail(){
		doAIA();

		loginPage.login("test-user@localhost", "password");

		emailUpdatePage.assertCurrent();
		emailUpdatePage.changeEmail("invalidemail");
		emailUpdatePage.assertCurrent();

		Assert.assertEquals("Invalid email address.", emailUpdatePage.getEmailError());

		UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		Assert.assertEquals("test-user@localhost", user.getEmail());
	}

	@Test
	public void updateToBlankEmail(){
		doAIA();

		loginPage.login("test-user@localhost", "password");

		emailUpdatePage.assertCurrent();
		emailUpdatePage.changeEmail("");
		emailUpdatePage.assertCurrent();

		Assert.assertEquals("Please specify email.", emailUpdatePage.getEmailError());

		UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
		Assert.assertEquals("test-user@localhost", user.getEmail());
	}

}
