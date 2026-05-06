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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.AdminApiUtil;
import org.keycloak.testsuite.arquillian.annotation.IgnoreBrowserDriver;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.utils.StringUtil;

import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionUpdateProfileTest extends AbstractChangeImportedUserPasswordsTest {

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

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        super.configureTestRealm(testRealm);
        ActionUtil.addRequiredActionForUser(testRealm, "test-user@localhost", UserModel.RequiredAction.UPDATE_PROFILE.name());
        ActionUtil.addRequiredActionForUser(testRealm, "john-doh@localhost", UserModel.RequiredAction.UPDATE_PROFILE.name());
    }

    @Before
    public void beforeTest() {
        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost")
                .firstName("Tom")
                .lastName("Brady")
                .emailVerified(true)
                .requiredActions(UserModel.RequiredAction.UPDATE_PROFILE.name()).build();
        AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, generatePassword("test-user@localhost"));

        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "john-doh@localhost");
        user = UserBuilder.create().enabled(true)
                .username("john-doh@localhost")
                .email("john-doh@localhost")
                .firstName("John")
                .lastName("Doh")
                .emailVerified(true)
                .requiredActions(UserModel.RequiredAction.UPDATE_PROFILE.name()).build();
        AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, generatePassword("john-doh@localhost"));
    }

    @Test
    public void updateProfile() {
        oauth.openLoginForm();
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        user.setEmailVerified(true);
        adminClient.realm("test").users().get(user.getId()).update(user);
        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        updateProfilePage.assertCurrent();
        assertFalse(updateProfilePage.isCancelDisplayed());

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("new@email.com").submit();

        events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.PREVIOUS_FIRST_NAME, "Tom").detail(Details.UPDATED_FIRST_NAME, "New first")
                .detail(Details.PREVIOUS_LAST_NAME, "Brady").detail(Details.UPDATED_LAST_NAME, "New last")
                .detail(Details.PREVIOUS_EMAIL, "test-user@localhost").detail(Details.UPDATED_EMAIL, "new@email.com")
                .assertEvent();
        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventAssertion.expectLoginSuccess(events.poll());

        // assert user is really updated in persistent store
        user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        Assertions.assertEquals("New first", user.getFirstName());
        Assertions.assertEquals("New last", user.getLastName());
        Assertions.assertEquals("new@email.com", user.getEmail());
        Assertions.assertEquals("test-user@localhost", user.getUsername());
        // email changed so verify that emailVerified flag is reset
        Assertions.assertEquals(false, user.isEmailVerified());
    }

    @Test
    public void updateUsername() {
        oauth.openLoginForm();

        loginPage.login("john-doh@localhost", getPassword("john-doh@localhost"));

        String userId = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost").getId();

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("new").firstName("New first").lastName("New last").email("john-doh@localhost").submit();

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.UPDATE_PROFILE)
                .isCodeId()
                .sessionId(null)
                .userId(userId)
                .details(Details.USERNAME, "john-doh@localhost")
                .details(Details.UPDATED_FIRST_NAME, "New first")
                .details(Details.UPDATED_LAST_NAME, "New last")
                .withoutDetails(Details.CONSENT);

        Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, "john-doh@localhost").userId(userId);

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "new");
        Assertions.assertEquals("New first", user.getFirstName());
        Assertions.assertEquals("New last", user.getLastName());
        Assertions.assertEquals("john-doh@localhost", user.getEmail());
        Assertions.assertEquals("new", user.getUsername());
        // email not changed so verify that emailVerified flag is NOT reset
        Assertions.assertEquals(true, user.isEmailVerified());
        getCleanup().addUserId(user.getId());
    }

    @Test
    public void updateProfileMissingFirstName() {
        oauth.openLoginForm();

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("new").firstName("").lastName("New last").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assertions.assertEquals("Please specify this field.", updateProfilePage.getInputErrors().getFirstNameError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileMissingLastName() {
        oauth.openLoginForm();

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("new").firstName("New first").lastName("").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());

        Assertions.assertEquals("Please specify this field.", updateProfilePage.getInputErrors().getLastNameError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileMissingEmail() {
        oauth.openLoginForm();

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("new").firstName("New first").lastName("New last")
                .email("").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("", updateProfilePage.getEmail());

        assertThat(updateProfilePage.getInputErrors().getEmailError(), anyOf(
                containsString("Please specify email"),
                containsString("Please specify this field")
        ));

        events.assertEmpty();
    }

    @Test
    public void updateProfileInvalidEmail() {
        oauth.openLoginForm();

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("invalid").firstName("New first").lastName("New last")
                        .email("invalidemail").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("invalidemail", updateProfilePage.getEmail());

        Assertions.assertEquals("Invalid email address.", updateProfilePage.getInputErrors().getEmailError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileMissingUsername() {
        oauth.openLoginForm();

        loginPage.login("john-doh@localhost", getPassword("john-doh@localhost"));

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("").firstName("New first").lastName("New last").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assertions.assertEquals("", updateProfilePage.getUsername());

        Assertions.assertEquals("Please specify username.", updateProfilePage.getInputErrors().getUsernameError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileDuplicateUsername() {
        oauth.openLoginForm();

        loginPage.login("john-doh@localhost", getPassword("john-doh@localhost"));

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assertions.assertEquals("test-user@localhost", updateProfilePage.getUsername());

        Assertions.assertEquals("Username already exists.", updateProfilePage.getInputErrors().getUsernameError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileDuplicatedEmail() {
        oauth.openLoginForm();

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last")
                .email("keycloak-user@localhost").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("keycloak-user@localhost", updateProfilePage.getEmail());

        Assertions.assertEquals("Email already exists.", updateProfilePage.getInputErrors().getEmailError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileDuplicateUsernameWithEmail() {
        getCleanup().addUserId(createUser(TEST_REALM_NAME, "user1@local.com", generatePassword("user1@local.com"), "user1", "user1", "user1@local.org"));

        oauth.openLoginForm();

        loginPage.login("john-doh@localhost", getPassword("john-doh@localhost"));

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("user1@local.org").firstName("New first").lastName("New last").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assertions.assertEquals("user1@local.org", updateProfilePage.getUsername());

        Assertions.assertEquals("Username already exists.", updateProfilePage.getInputErrors().getUsernameError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileDuplicatedEmailWithUsername() {
        getCleanup().addUserId(createUser(TEST_REALM_NAME, "user1@local.com", generatePassword("user1@local.com"), "user1", "user1", "user1@local.org"));

        oauth.openLoginForm();

        loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last")
                .email("user1@local.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("user1@local.com", updateProfilePage.getEmail());

        Assertions.assertEquals("Email already exists.", updateProfilePage.getInputErrors().getEmailError());

        events.assertEmpty();
    }

    @Test
    public void updateProfileExpiredCookies() {
        oauth.openLoginForm();
        loginPage.login("john-doh@localhost", getPassword("john-doh@localhost"));

        updateProfilePage.assertCurrent();

        // Expire cookies and assert the page with "back to application" link present
        driver.manage().deleteAllCookies();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("keycloak-user@localhost").submit();
        errorPage.assertCurrent();

        String backToAppLink = errorPage.getBackToApplicationLink();

        ClientRepresentation client = AdminApiUtil.findClientByClientId(adminClient.realm("test"), "test-app").toRepresentation();
        Assertions.assertEquals(backToAppLink, client.getBaseUrl());
    }

    @Test
    public void updateProfileWithoutRemoveCustomAttributes() {
        UserProfileResource upResource = adminClient.realm("test").users().userProfile();
        UPConfig upConfig = upResource.getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ADMIN_EDIT);
        upResource.update(upConfig);

        try {
            UserRepresentation userRep = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
            UserResource user = adminClient.realm("test").users().get(userRep.getId());

            userRep.setAttributes(new HashMap<>());
            userRep.getAttributes().put("custom", Arrays.asList("custom"));

            user.update(userRep);

            oauth.openLoginForm();

            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));

            updateProfilePage.assertCurrent();
            assertFalse(updateProfilePage.isCancelDisplayed());

            updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("new@email.com").submit();

            events.expectRequiredAction(EventType.UPDATE_PROFILE).detail(Details.CONTEXT, UserProfileContext.UPDATE_PROFILE.name()).detail(Details.PREVIOUS_EMAIL, "test-user@localhost").detail(Details.UPDATED_EMAIL, "new@email.com").assertEvent();

            Assertions.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            EventAssertion.expectLoginSuccess(events.poll());

            // assert user is really updated in persistent store
            userRep = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
            Assertions.assertEquals("New first", userRep.getFirstName());
            Assertions.assertEquals("New last", userRep.getLastName());
            Assertions.assertEquals("new@email.com", userRep.getEmail());
            Assertions.assertEquals("test-user@localhost", userRep.getUsername());
            Assertions.assertNotNull(userRep.getAttributes());
            Assertions.assertTrue(userRep.getAttributes().containsKey("custom"));
        } finally {
            upConfig.setUnmanagedAttributePolicy(null);
            upResource.update(upConfig);
        }
    }

    @Test
    @IgnoreBrowserDriver(HtmlUnitDriver.class) // we can't yet run modern JavaScript using HtmlUnit
    public void testMultivaluedAttributes() {
        UserProfileResource userProfile = managedRealm.admin().users().userProfile();
        UPConfig configuration = userProfile.getConfiguration();

        try {
            UPConfig testUpConfig = configuration.clone();
            List<String> attributes = List.of("foo", "bar", "zar");
            List<String> values = IntStream.range(0, 5).mapToObj(Integer::toString).collect(Collectors.toList());
            Set<String> valuesSet = new HashSet<>(values);

            for (String attribute : attributes) {
                testUpConfig.addOrReplaceAttribute(
                        new UPAttribute(attribute, true, new UPAttributePermissions(Set.of(), Set.of(ROLE_USER, ROLE_ADMIN)))
                );
            }

            userProfile.update(testUpConfig);

            oauth.openLoginForm();
            loginPage.login("john-doh@localhost", getPassword("john-doh@localhost"));
            updateProfilePage.assertCurrent();

            for (String attribute : attributes) {
                updateProfilePage.assertCurrent();

                // add multiple values
                for (String value : values) {
                    String elementId = attribute + "-" + value;
                    updateProfilePage.setAttribute(elementId, value);
                    updateProfilePage.clickAddAttributeValue(elementId);
                }
                updateProfilePage.update("f", "l", "e@keycloak.org");
                UserRepresentation userRep = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost");
                assertThat(userRep.getAttributes().get(attribute), Matchers.containsInAnyOrder(values.toArray()));

                // make sure multiple values are properly rendered
                userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PROFILE.name()));
                managedRealm.admin().users().get(userRep.getId()).update(userRep);
                oauth.openLoginForm();
                assertThat(IntStream.range(0, 5).mapToObj(value -> updateProfilePage.getAttribute(attribute + "-" + value)).collect(Collectors.toSet()), Matchers.equalTo(valuesSet));

                final String lastValue = values.get(values.size() - 1);

                // remove multiple values, only the last value should be kept as you can't remove the last one
                for (String value : values) {
                    try {
                        updateProfilePage.clickRemoveAttributeValue(attribute + "-0");
                    } catch (ElementClickInterceptedException e) {
                        if (! lastValue.equals(value)) {    // Ignore that the last value cannot be clicked / removed - this is by design
                            throw e;
                        }
                    }
                }
                updateProfilePage.update("f", "l", "e@keycloak.org");
                userRep = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost");
                assertThat(userRep.getAttributes().get(attribute), Matchers.hasSize(1));
                assertThat(userRep.getAttributes().get(attribute).get(0), Matchers.in(values));

                // make sure adding/removing within the same context works
                userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PROFILE.name()));
                managedRealm.admin().users().get(userRep.getId()).update(userRep);
                oauth.openLoginForm();
                for (String value : values) {
                    String elementId = attribute + "-" + value;
                    updateProfilePage.setAttribute(elementId, value);
                    updateProfilePage.clickAddAttributeValue(elementId);
                }

                assertThat(IntStream.range(0, 5).mapToObj(value -> updateProfilePage.getAttribute(attribute + "-" + value)).collect(Collectors.toSet()), Matchers.equalTo(valuesSet));

                for (String value : values) {
                    try {
                        updateProfilePage.clickRemoveAttributeValue(attribute + "-0");
                    } catch (ElementClickInterceptedException e) {
                        if (! lastValue.equals(value)) {    // Ignore that the last value cannot be clicked / removed - this is by design
                            throw e;
                        }
                    }
                }
                // make sure the last attribute is set with a value
                if (StringUtil.isBlank(updateProfilePage.getAttribute(attribute + "-0"))) {
                    updateProfilePage.setAttribute(attribute + "-0", lastValue);
                }
                updateProfilePage.update("f", "l", "e@keycloak.org");
                userRep = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost");
                assertThat(userRep.getAttributes().get(attribute), Matchers.hasSize(1));
                assertThat(userRep.getAttributes().get(attribute).get(0), Matchers.in(values));

                // at the end the attribute is set with multiple values
                userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PROFILE.name()));
                managedRealm.admin().users().get(userRep.getId()).update(userRep);
                oauth.openLoginForm();
                for (String value : values) {
                    String elementId = attribute + "-" + value;
                    updateProfilePage.setAttribute(elementId, value);
                    updateProfilePage.clickAddAttributeValue(elementId);
                }
                updateProfilePage.update("f", "l", "e@keycloak.org");

                // restart the update profile flow
                userRep = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost");
                userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PROFILE.name()));
                managedRealm.admin().users().get(userRep.getId()).update(userRep);
                oauth.openLoginForm();
            }

            UserRepresentation userRep = ActionUtil.findUserWithAdminClient(adminClient, "john-doh@localhost");

            // all attributes should be set with multiple values
            for (String attribute : attributes) {
                assertThat(userRep.getAttributes().get(attribute), Matchers.containsInAnyOrder(values.toArray()));
            }
        } finally {
            userProfile.update(configuration);
        }
    }

}
