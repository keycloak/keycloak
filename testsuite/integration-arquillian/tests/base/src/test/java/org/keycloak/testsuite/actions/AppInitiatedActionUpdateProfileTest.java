/*
 * Copyright 2019 Red Hat, Inc. and/or its affiliates
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

import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;

/**
 * Only covers basic use cases for App Initialized actions. Complete dynamic user profile behavior is tested in {@link RequiredActionUpdateProfileWithUserProfileTest} as it shares same code as the App initialized action.
 *
 * @author Stan Silvert
 */
public class AppInitiatedActionUpdateProfileTest extends AbstractAppInitiatedActionTest {

    @Override
    public String getAiaAction() {
        return UserModel.RequiredAction.UPDATE_PROFILE.name();
    }
    
    @Page
    protected LoginUpdateProfileEditUsernameAllowedPage updateProfilePage;

    @Page
    protected ErrorPage errorPage;
    
    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void beforeTest() {
        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost")
                .firstName("Tom")
                .lastName("Brady")
                .build();
        AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, "password");

        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "john-doh@localhost");
        user = UserBuilder.create().enabled(true)
                .username("john-doh@localhost")
                .email("john-doh@localhost")
                .firstName("John")
                .lastName("Doh")
                .build();
        AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, "password");
    }
  
    @Test
    public void updateProfile() {
        doAIA();

        loginPage.login("test-user@localhost", "password");
        
        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("new@email.com").submit();

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PROFILE).details(Details.PREVIOUS_FIRST_NAME, "Tom").details(Details.UPDATED_FIRST_NAME, "New first")
                .details(Details.PREVIOUS_LAST_NAME, "Brady").details(Details.UPDATED_LAST_NAME, "New last")
                .details(Details.PREVIOUS_EMAIL, "test-user@localhost").details(Details.UPDATED_EMAIL, "new@email.com");
        EventAssertion.expectLoginSuccess(events.poll());

        assertKcActionStatus(SUCCESS);

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Assertions.assertEquals("New first", user.getFirstName());
        Assertions.assertEquals("New last", user.getLastName());
        Assertions.assertEquals("new@email.com", user.getEmail());
        Assertions.assertEquals("test-user@localhost", user.getUsername());
    }
    
    @Test
    // This tests verifies that AIA still works if you call it after you are
    // already logged in.  The other main difference between this and all other
    // AIA tests is that the events are posted in a different order.
    public void updateProfileLoginFirst() {
        oauth.openLoginForm();
        loginPage.login("test-user@localhost", "password");
        
        doAIA();

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("new@email.com").submit();

        EventAssertion.expectLoginSuccess(events.poll());
        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PROFILE).details(Details.PREVIOUS_FIRST_NAME, "Tom").details(Details.UPDATED_FIRST_NAME, "New first")
                .details(Details.PREVIOUS_LAST_NAME, "Brady").details(Details.UPDATED_LAST_NAME, "New last")
                .details(Details.PREVIOUS_EMAIL, "test-user@localhost").details(Details.UPDATED_EMAIL, "new@email.com");
        EventAssertion.expectLoginSuccess(events.poll());

        assertKcActionStatus(SUCCESS);

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Assertions.assertEquals("New first", user.getFirstName());
        Assertions.assertEquals("New last", user.getLastName());
        Assertions.assertEquals("new@email.com", user.getEmail());
        Assertions.assertEquals("test-user@localhost", user.getUsername());
    }
    
    @Test
    public void cancelUpdateProfile() {
        doAIA();

        loginPage.login("test-user@localhost", "password");
        
        updateProfilePage.assertCurrent();
        updateProfilePage.cancel();

        assertKcActionStatus(CANCELLED);

        
        // assert nothing was updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Assertions.assertEquals("Tom", user.getFirstName());
        Assertions.assertEquals("Brady", user.getLastName());
        Assertions.assertEquals("test-user@localhost", user.getEmail());
        Assertions.assertEquals("test-user@localhost", user.getUsername());
    }
    

    @Test
    public void updateUsername() {
        doAIA();
        
        loginPage.login("john-doh@localhost", "password");

        String userId = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost").getId();

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("new").firstName("New first").lastName("New last").email("john-doh@localhost").submit();

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.UPDATE_PROFILE)
                .details(Details.PREVIOUS_FIRST_NAME, "John")
                .details(Details.UPDATED_FIRST_NAME, "New first")
                .details(Details.PREVIOUS_LAST_NAME, "Doh")
                .details(Details.UPDATED_LAST_NAME, "New last")
                .details(Details.USERNAME, "john-doh@localhost")
                .details(Details.REDIRECT_URI, oauth.getRedirectUri())
                .userId(userId).sessionId(null)
                .withoutDetails(Details.CONSENT);

        assertKcActionStatus(SUCCESS);

        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, "john-doh@localhost").userId(userId);

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "new");
        Assertions.assertEquals("New first", user.getFirstName());
        Assertions.assertEquals("New last", user.getLastName());
        Assertions.assertEquals("john-doh@localhost", user.getEmail());
        Assertions.assertEquals("new", user.getUsername());
        getCleanup().addUserId(user.getId());
    }

    @Test
    public void updateProfileMissingFirstName() {
        doAIA();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("new").firstName("").lastName("New last").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());

        Assertions.assertEquals("Please specify this field.", updateProfilePage.getInputErrors().getFirstNameError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileMissingLastName() {
        doAIA();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("new").firstName("New first").lastName("").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());

        Assertions.assertEquals("Please specify this field.", updateProfilePage.getInputErrors().getLastNameError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileMissingEmail() {
        doAIA();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("new").firstName("New first").lastName("New last").email("").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("", updateProfilePage.getEmail());

        assertThat(updateProfilePage.getInputErrors().getEmailError(), anyOf(
                containsString("Please specify email"),
                containsString("Please specify this field")
        ));

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileInvalidEmail() {
        doAIA();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("invalid").firstName("New first").lastName("New last").email("invalidemail").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("invalidemail", updateProfilePage.getEmail());

        Assertions.assertEquals("Invalid email address.", updateProfilePage.getInputErrors().getEmailError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileMissingUsername() {
        doAIA();

        loginPage.login("john-doh@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("").firstName("New first").lastName("New last").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assertions.assertEquals("", updateProfilePage.getUsername());

        Assertions.assertEquals("Please specify username.", updateProfilePage.getInputErrors().getUsernameError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileDuplicateUsername() {
        doAIA();

        loginPage.login("john-doh@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assertions.assertEquals("test-user@localhost", updateProfilePage.getUsername());

        Assertions.assertEquals("Username already exists.", updateProfilePage.getInputErrors().getUsernameError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileDuplicatedEmail() {
        doAIA();

        loginPage.login("test-user@localhost", "password");

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("keycloak-user@localhost").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("keycloak-user@localhost", updateProfilePage.getEmail());

        Assertions.assertEquals("Email already exists.", updateProfilePage.getInputErrors().getEmailError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileExpiredCookies() {
        doAIA();
        loginPage.login("john-doh@localhost", "password");

        updateProfilePage.assertCurrent();

        // Expire cookies and assert the page with "back to application" link present
        driver.manage().deleteAllCookies();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("keycloak-user@localhost").submit();
        errorPage.assertCurrent();

        String backToAppLink = errorPage.getBackToApplicationLink();

        ClientRepresentation client = AdminApiUtil.findClientByClientId(adminClient.realm("test"), "test-app").toRepresentation();
        Assertions.assertEquals(backToAppLink, client.getBaseUrl());
    }

}
