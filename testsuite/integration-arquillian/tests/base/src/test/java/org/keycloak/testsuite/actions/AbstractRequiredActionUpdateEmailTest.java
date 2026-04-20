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

import java.util.Arrays;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.auth.page.login.UpdateEmailPage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.util.SecondBrowser;
import org.keycloak.testsuite.util.UserBuilder;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.WebDriver;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@EnableFeature(Profile.Feature.UPDATE_EMAIL)
public abstract class AbstractRequiredActionUpdateEmailTest extends AbstractTestRealmKeycloakTest {

	@Rule
	public AssertEvents events = new AssertEvents(this);

	@Page
	protected LoginPage loginPage;

	@Page
	protected UpdateEmailPage updateEmailPage;

    @Page
    protected LoginUpdateProfilePage updateProfilePage;

    @Page
	protected AppPage appPage;

    @Page
    protected ErrorPage errorPage;

    @Drone
    @SecondBrowser
    protected WebDriver driver2;

	@Before
	public void beforeTest() {
        AdminApiUtil.enableRequiredAction(managedRealm.admin(), RequiredAction.UPDATE_EMAIL, true);
		AdminApiUtil.removeUserByUsername(managedRealm.admin(), "test-user@localhost");
		UserRepresentation user = UserBuilder.create().enabled(true)
				.username("test-user@localhost")
				.email("test-user@localhost")
				.firstName("Tom")
				.lastName("Brady")
				.requiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).build();
		prepareUser(user);
		AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, "password");

		AdminApiUtil.removeUserByUsername(managedRealm.admin(), "john-doh@localhost");
		user = UserBuilder.create().enabled(true)
				.username("john-doh@localhost")
				.email("john-doh@localhost")
				.firstName("John")
				.lastName("Doh")
				.requiredAction(UserModel.RequiredAction.UPDATE_EMAIL.name()).build();
		prepareUser(user);
		AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, "password");
	}

	private void setRegistrationEmailAsUsername(RealmResource realmResource, boolean enabled) {
		RealmRepresentation realmRepresentation = realmResource.toRepresentation();
		realmRepresentation.setRegistrationEmailAsUsername(enabled);
		realmResource.update(realmRepresentation);
	}

        protected void configureRequiredActionsToUser(String username, String... actions) {
                UserResource userResource = AdminApiUtil.findUserByUsernameId(managedRealm.admin(), username);
                UserRepresentation userRepresentation = userResource.toRepresentation();
                userRepresentation.setRequiredActions(Arrays.asList(actions));
                userResource.update(userRepresentation);
        }

	protected void prepareUser(UserRepresentation user) {

	}

	@Override
	public void configureTestRealm(RealmRepresentation testRealm) {

	}

	@Test
	public void cancelIsNotDisplayed() {
		oauth.openLoginForm();

		loginPage.login("test-user@localhost", "password");

		updateEmailPage.assertCurrent();
		assertFalse(updateEmailPage.isCancelDisplayed());
	}

	@Test
	public void updateEmailMissing() {
		oauth.openLoginForm();

		loginPage.login("test-user@localhost", "password");

		updateEmailPage.assertCurrent();

		updateEmailPage.changeEmail("");

		updateEmailPage.assertCurrent();

		// assert that form holds submitted values during validation error
		Assertions.assertEquals("", updateEmailPage.getEmail());

		Assertions.assertTrue(updateEmailPage.getEmailInputError().contains("Please specify email."));

		events.assertEmpty();
	}

	@Test
	public void updateEmailDuplicate() {
		oauth.openLoginForm();

		loginPage.login("john-doh@localhost", "password");

		updateEmailPage.assertCurrent();

		updateEmailPage.changeEmail("test-user@localhost");

		updateEmailPage.assertCurrent();

		// assert that form holds submitted values during validation error
		Assertions.assertEquals("test-user@localhost", updateEmailPage.getEmail());

		Assertions.assertEquals("Email already exists.", updateEmailPage.getEmailInputError());

		events.assertEmpty();
	}

	@Test
	public void updateEmailInvalid() {
		oauth.openLoginForm();

		loginPage.login("test-user@localhost", "password");

		updateEmailPage.assertCurrent();

		updateEmailPage.changeEmail("invalid");

		updateEmailPage.assertCurrent();

		// assert that form holds submitted values during validation error
		Assertions.assertEquals("invalid", updateEmailPage.getEmail());

		Assertions.assertEquals("Invalid email address.", updateEmailPage.getEmailInputError());

		events.assertEmpty();
	}

	@Test
	public void updateEmailWithEmailAsUsernameEnabled() throws Exception {
		Boolean genuineRegistrationEmailAsUsername = managedRealm.admin()
				.toRepresentation()
				.isRegistrationEmailAsUsername();

		setRegistrationEmailAsUsername(managedRealm.admin(), true);
		try {
			UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
			String firstName = user.getFirstName();
			String lastName = user.getLastName();
			assertNotNull(firstName);
			assertNotNull(lastName);
			changeEmailUsingRequiredAction("new@localhost", true, true);
			user = ActionUtil.findUserWithAdminClient(adminClient, "new@localhost");
			Assertions.assertNotNull(user);
			firstName = user.getFirstName();
			lastName = user.getLastName();
			assertNotNull(firstName);
			assertNotNull(lastName);
		} finally {
			setRegistrationEmailAsUsername(managedRealm.admin(), genuineRegistrationEmailAsUsername);
		}
	}

	protected abstract void changeEmailUsingRequiredAction(String newEmail, boolean logoutOtherSessions, boolean newEmailAsUsername) throws Exception;
}
