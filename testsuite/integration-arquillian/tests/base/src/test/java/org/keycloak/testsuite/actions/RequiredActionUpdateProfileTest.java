/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
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

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;
import org.keycloak.testsuite.util.UserBuilder;

import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.HashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionUpdateProfileTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginUpdateProfileEditUsernameAllowedPage updateProfilePage;

    @Page
    protected ErrorPage errorPage;
    
    protected boolean isDynamicForm() {
        return false;
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        ActionUtil.addRequiredActionForUser(testRealm, "test-user@localhost", UserModel.RequiredAction.UPDATE_PROFILE.name());
        ActionUtil.addRequiredActionForUser(testRealm, "john-doh@localhost", UserModel.RequiredAction.UPDATE_PROFILE.name());
    }

    @Before
    public void beforeTest() {
        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost")
                .firstName("Tom")
                .lastName("Brady")
                .emailVerified(true)
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name()).build();
        ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");

        ApiUtil.removeUserByUsername(testRealm(), "john-doh@localhost");
        user = UserBuilder.create().enabled(true)
                .username("john-doh@localhost")
                .email("john-doh@localhost")
                .firstName("John")
                .lastName("Doh")
                .emailVerified(true)
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.name()).build();
        ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");
    }

    @Test
    public void updateProfile() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();
        assertFalse(updateProfilePage.isCancelDisplayed());

        updateProfilePage.update("New first", "New last", "new@email.com", "test-user@localhost");

        events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.PREVIOUS_FIRST_NAME, "Tom").detail(Details.UPDATED_FIRST_NAME, "New first")
                .detail(Details.PREVIOUS_LAST_NAME, "Brady").detail(Details.UPDATED_LAST_NAME, "New last")
                .detail(Details.PREVIOUS_EMAIL, "test-user@localhost").detail(Details.UPDATED_EMAIL, "new@email.com")
                .detail(Details.PREVIOUS_EMAIL, "test-user@localhost").detail(Details.UPDATED_EMAIL, "new@email.com")
                .assertEvent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Assert.assertEquals("New first", user.getFirstName());
        Assert.assertEquals("New last", user.getLastName());
        Assert.assertEquals("new@email.com", user.getEmail());
        Assert.assertEquals("test-user@localhost", user.getUsername());
        // email changed so verify that emailVerified flag is reset
        Assert.assertEquals(false, user.isEmailVerified());
    }

    @Test
    public void updateUsername() {
        loginPage.open();

        loginPage.login("john-doh@localhost", "password");

        String userId = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost").getId();

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "john-doh@localhost", "new");

        events.expectLogin().event(EventType.UPDATE_PROFILE).detail(Details.UPDATED_FIRST_NAME, "New first").user(userId).session(Matchers.nullValue(String.class)).removeDetail(Details.CONSENT)
                .detail(Details.UPDATED_LAST_NAME, "New last").user(userId).session(Matchers.nullValue(String.class)).removeDetail(Details.CONSENT)
                .detail(Details.USERNAME, "john-doh@localhost")
                .assertEvent();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().detail(Details.USERNAME, "john-doh@localhost").user(userId).assertEvent();

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "new");
        Assert.assertEquals("New first", user.getFirstName());
        Assert.assertEquals("New last", user.getLastName());
        Assert.assertEquals("john-doh@localhost", user.getEmail());
        Assert.assertEquals("new", user.getUsername());
        // email not changed so verify that emailVerified flag is NOT reset
        Assert.assertEquals(true, user.isEmailVerified());
        getCleanup().addUserId(user.getId());
    }

    @Test
    public void updateProfileMissingFirstName() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("", "New last", "new@email.com", "new");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("new@email.com", updateProfilePage.getEmail());

        if(isDynamicForm())
            Assert.assertEquals("Please specify this field.", updateProfilePage.getInputErrors().getFirstNameError());
        else
            Assert.assertEquals("Please specify first name.", updateProfilePage.getInputErrors().getFirstNameError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileMissingLastName() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "", "new@email.com", "new");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("", updateProfilePage.getLastName());
        Assert.assertEquals("new@email.com", updateProfilePage.getEmail());

        if(isDynamicForm())
            Assert.assertEquals("Please specify this field.", updateProfilePage.getInputErrors().getLastNameError());
        else
            Assert.assertEquals("Please specify last name.", updateProfilePage.getInputErrors().getLastNameError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileMissingEmail() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "", "new");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("", updateProfilePage.getEmail());

        Assert.assertEquals("Please specify email.", updateProfilePage.getInputErrors().getEmailError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileInvalidEmail() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "invalidemail", "invalid");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("invalidemail", updateProfilePage.getEmail());

        Assert.assertEquals("Invalid email address.", updateProfilePage.getInputErrors().getEmailError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileMissingUsername() {
        loginPage.open();

        loginPage.login("john-doh@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "new@email.com", "");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assert.assertEquals("", updateProfilePage.getUsername());

        Assert.assertEquals("Please specify username.", updateProfilePage.getInputErrors().getUsernameError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileDuplicateUsername() {
        loginPage.open();

        loginPage.login("john-doh@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "new@email.com", "test-user@localhost");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assert.assertEquals("test-user@localhost", updateProfilePage.getUsername());

        Assert.assertEquals("Username already exists.", updateProfilePage.getInputErrors().getUsernameError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileDuplicatedEmail() {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.update("New first", "New last", "keycloak-user@localhost", "test-user@localhost");

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assert.assertEquals("New first", updateProfilePage.getFirstName());
        Assert.assertEquals("New last", updateProfilePage.getLastName());
        Assert.assertEquals("keycloak-user@localhost", updateProfilePage.getEmail());

        Assert.assertEquals("Email already exists.", updateProfilePage.getInputErrors().getEmailError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileExpiredCookies() {
        loginPage.open();
        loginPage.login("john-doh@localhost", "password");

        updateProfilePage.assertCurrent();

        // Expire cookies and assert the page with "back to application" link present
        driver.manage().deleteAllCookies();

        updateProfilePage.update("New first", "New last", "keycloak-user@localhost", "test-user@localhost");
        errorPage.assertCurrent();

        String backToAppLink = errorPage.getBackToApplicationLink();

        ClientRepresentation client = ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app").toRepresentation();
        Assert.assertEquals(backToAppLink, client.getBaseUrl());
    }

    @Test
    public void updateProfileWithoutRemoveCustomAttributes() {
        UserRepresentation userRep = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        UserResource user = adminClient.realm("test").users().get(userRep.getId());

        userRep.setAttributes(new HashMap<>());
        userRep.getAttributes().put("custom", Arrays.asList("custom"));

        user.update(userRep);

        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();
        assertFalse(updateProfilePage.isCancelDisplayed());

        updateProfilePage.update("New first", "New last", "new@email.com", "test-user@localhost");

        events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.PREVIOUS_EMAIL, "test-user@localhost").detail(Details.UPDATED_EMAIL, "new@email.com").assertEvent();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();

        // assert user is really updated in persistent store
        userRep = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Assert.assertEquals("New first", userRep.getFirstName());
        Assert.assertEquals("New last", userRep.getLastName());
        Assert.assertEquals("new@email.com", userRep.getEmail());
        Assert.assertEquals("test-user@localhost", userRep.getUsername());
        Assert.assertNotNull(userRep.getAttributes());
        Assert.assertTrue(userRep.getAttributes().containsKey("custom"));
    }

}
