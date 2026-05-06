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

package org.keycloak.tests.actions;

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
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.webdriver.BrowserType;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.utils.PasswordGenerateUtil;
import org.keycloak.userprofile.UserProfileContext;
import org.keycloak.utils.StringUtil;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.ElementClickInterceptedException;

import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_ADMIN;
import static org.keycloak.userprofile.config.UPConfigUtils.ROLE_USER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;

@KeycloakIntegrationTest
@DatabaseTest
public class RequiredActionUpdateProfileTest {

    private static final String PASSWORD = PasswordGenerateUtil.generatePassword();

    @InjectRealm(config = RequiredActionUpdateProfileRealmConfig.class)
    ManagedRealm realm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectUser(ref = "testUser", config = TestUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser testUser;

    @InjectUser(ref = "johnDoh", config = JohnDohUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser johnDohUser;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected LoginUpdateProfilePage updateProfilePage;

    @InjectPage
    protected ErrorPage errorPage;

    @Test
    public void updateProfile() {
        oauth.openLoginForm();

        loginPage.fillLogin("test-user@localhost", PASSWORD);
        loginPage.submit();

        updateProfilePage.assertCurrent();
        assertFalse(updateProfilePage.isCancelDisplayed());

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("new@email.com").submit();

        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PROFILE).details(Details.PREVIOUS_FIRST_NAME, "Tom").details(Details.UPDATED_FIRST_NAME, "New first")
                .details(Details.PREVIOUS_LAST_NAME, "Brady").details(Details.UPDATED_LAST_NAME, "New last")
                .details(Details.PREVIOUS_EMAIL, "test-user@localhost").details(Details.UPDATED_EMAIL, "new@email.com");

        EventAssertion.expectLoginSuccess(events.poll());

        // assert user is really updated in persistent store
        UserRepresentation user = testUser.admin().toRepresentation();
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

        loginPage.fillLogin("john-doh@localhost", PASSWORD);
        loginPage.submit();

        String userId = johnDohUser.getId();

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("new").firstName("New first").lastName("New last").email("john-doh@localhost").submit();

        Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.UPDATE_PROFILE)
                .hasCodeId()
                .sessionId(null)
                .userId(userId)
                .details(Details.USERNAME, "john-doh@localhost")
                .details(Details.UPDATED_FIRST_NAME, "New first")
                .details(Details.UPDATED_LAST_NAME, "New last")
                .withoutDetails(Details.CONSENT);

        EventAssertion.expectLoginSuccess(events.poll()).details(Details.USERNAME, "john-doh@localhost").userId(userId);

        // assert user is really updated in persistent store
        UserRepresentation user = johnDohUser.admin().toRepresentation();
        Assertions.assertEquals("New first", user.getFirstName());
        Assertions.assertEquals("New last", user.getLastName());
        Assertions.assertEquals("john-doh@localhost", user.getEmail());
        Assertions.assertEquals("new", user.getUsername());
        // email not changed so verify that emailVerified flag is NOT reset
        Assertions.assertEquals(true, user.isEmailVerified());
    }

    @Test
    public void updateProfileMissingFirstName() {
        oauth.openLoginForm();

        loginPage.fillLogin("test-user@localhost", PASSWORD);
        loginPage.submit();

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
        oauth.openLoginForm();

        loginPage.fillLogin("test-user@localhost", PASSWORD);
        loginPage.submit();

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
        oauth.openLoginForm();

        loginPage.fillLogin("test-user@localhost", PASSWORD);
        loginPage.submit();

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

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileInvalidEmail() {
        oauth.openLoginForm();

        loginPage.fillLogin("test-user@localhost", PASSWORD);
        loginPage.submit();

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("invalid").firstName("New first").lastName("New last")
                .email("invalidemail").submit();

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
        oauth.openLoginForm();

        loginPage.fillLogin("john-doh@localhost", PASSWORD);
        loginPage.submit();

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
        oauth.openLoginForm();

        loginPage.fillLogin("john-doh@localhost", PASSWORD);
        loginPage.submit();

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
        oauth.openLoginForm();

        loginPage.fillLogin("test-user@localhost", PASSWORD);
        loginPage.submit();

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last")
                .email("keycloak-user@localhost").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("keycloak-user@localhost", updateProfilePage.getEmail());

        Assertions.assertEquals("Email already exists.", updateProfilePage.getInputErrors().getEmailError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileDuplicateUsernameWithEmail() {
        UserRepresentation user = UserBuilder.create("user1@local.com")
                .name("user1", "user1").email("user1@local.org").emailVerified(true).password("password").build();

        String userId = ApiUtil.getCreatedId(realm.admin().users().create(user));
        realm.cleanup().add(r -> r.users().get(userId).remove());

        oauth.openLoginForm();

        loginPage.fillLogin("john-doh@localhost", PASSWORD);
        loginPage.submit();

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("user1@local.org").firstName("New first").lastName("New last").email("new@email.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("new@email.com", updateProfilePage.getEmail());
        Assertions.assertEquals("user1@local.org", updateProfilePage.getUsername());

        Assertions.assertEquals("Username already exists.", updateProfilePage.getInputErrors().getUsernameError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileDuplicatedEmailWithUsername() {
        UserRepresentation user = UserBuilder.create("user1@local.com")
                .name("user1", "user1").email("user1@local.org").emailVerified(true).password("password").build();

        String userId = ApiUtil.getCreatedId(realm.admin().users().create(user));
        realm.cleanup().add(r -> r.users().get(userId).remove());

        oauth.openLoginForm();

        loginPage.fillLogin("test-user@localhost", PASSWORD);
        loginPage.submit();

        updateProfilePage.assertCurrent();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last")
                .email("user1@local.com").submit();

        updateProfilePage.assertCurrent();

        // assert that form holds submitted values during validation error
        Assertions.assertEquals("New first", updateProfilePage.getFirstName());
        Assertions.assertEquals("New last", updateProfilePage.getLastName());
        Assertions.assertEquals("user1@local.com", updateProfilePage.getEmail());

        Assertions.assertEquals("Email already exists.", updateProfilePage.getInputErrors().getEmailError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateProfileExpiredCookies() {
        oauth.openLoginForm();
        loginPage.fillLogin("john-doh@localhost", PASSWORD);
        loginPage.submit();

        updateProfilePage.assertCurrent();

        // Expire cookies and assert the page with "back to application" link present
        driver.cookies().deleteAll();

        updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("keycloak-user@localhost").submit();
        errorPage.assertCurrent();
    }

    @Test
    public void updateProfileWithoutRemoveCustomAttributes() {
        UserProfileResource upResource = realm.admin().users().userProfile();
        UPConfig upConfig = upResource.getConfiguration();
        upConfig.setUnmanagedAttributePolicy(UPConfig.UnmanagedAttributePolicy.ADMIN_EDIT);
        upResource.update(upConfig);

        try {
            UserResource user = testUser.admin();
            UserRepresentation userRep = user.toRepresentation();

            userRep.setAttributes(new HashMap<>());
            userRep.getAttributes().put("custom", Arrays.asList("custom"));

            user.update(userRep);

            oauth.openLoginForm();

            loginPage.fillLogin("test-user@localhost", PASSWORD);
            loginPage.submit();

            updateProfilePage.assertCurrent();
            assertFalse(updateProfilePage.isCancelDisplayed());

            updateProfilePage.prepareUpdate().username("test-user@localhost").firstName("New first").lastName("New last").email("new@email.com").submit();

            Assertions.assertNotNull(oauth.parseLoginResponse().getCode());

            EventAssertion.expectRequiredAction(events.poll()).type(EventType.UPDATE_PROFILE).details(Details.CONTEXT, UserProfileContext.UPDATE_PROFILE.name()).details(Details.PREVIOUS_EMAIL, "test-user@localhost").details(Details.UPDATED_EMAIL, "new@email.com");

            EventAssertion.expectLoginSuccess(events.poll());

            // assert user is really updated in persistent store
            userRep = testUser.admin().toRepresentation();
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
    public void testMultivaluedAttributes() {
        Assumptions.assumeFalse(driver.getBrowserType().equals(BrowserType.HTML_UNIT)); // we can't yet run modern JavaScript using HtmlUnit

        UserProfileResource userProfile = realm.admin().users().userProfile();
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
            loginPage.fillLogin("john-doh@localhost", PASSWORD);
            loginPage.submit();
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
                driver.waiting().waitForOAuthCallback();
                UserRepresentation userRep = johnDohUser.admin().toRepresentation();
                assertThat(userRep.getAttributes().get(attribute), Matchers.containsInAnyOrder(values.toArray()));

                // make sure multiple values are properly rendered
                userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PROFILE.name()));
                johnDohUser.admin().update(userRep);
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
                driver.waiting().waitForOAuthCallback();
                userRep = johnDohUser.admin().toRepresentation();
                assertThat(userRep.getAttributes().get(attribute), Matchers.hasSize(1));
                assertThat(userRep.getAttributes().get(attribute).get(0), Matchers.in(values));

                // make sure adding/removing within the same context works
                userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PROFILE.name()));
                johnDohUser.admin().update(userRep);
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
                driver.waiting().waitForOAuthCallback();
                userRep = johnDohUser.admin().toRepresentation();
                assertThat(userRep.getAttributes().get(attribute), Matchers.hasSize(1));
                assertThat(userRep.getAttributes().get(attribute).get(0), Matchers.in(values));

                // at the end the attribute is set with multiple values
                userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PROFILE.name()));
                johnDohUser.admin().update(userRep);
                oauth.openLoginForm();
                for (String value : values) {
                    String elementId = attribute + "-" + value;
                    updateProfilePage.setAttribute(elementId, value);
                    updateProfilePage.clickAddAttributeValue(elementId);
                }
                updateProfilePage.update("f", "l", "e@keycloak.org");
                driver.waiting().waitForOAuthCallback();

                // restart the update profile flow
                userRep = johnDohUser.admin().toRepresentation();
                userRep.setRequiredActions(List.of(UserModel.RequiredAction.UPDATE_PROFILE.name()));
                johnDohUser.admin().update(userRep);
                oauth.openLoginForm();
            }

            UserRepresentation userRep = johnDohUser.admin().toRepresentation();

            // all attributes should be set with multiple values
            for (String attribute : attributes) {
                assertThat(userRep.getAttributes().get(attribute), Matchers.containsInAnyOrder(values.toArray()));
            }
        } finally {
            userProfile.update(configuration);
        }
    }

    public static class RequiredActionUpdateProfileRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.editUsernameAllowed(true);
            realm.users(UserBuilder.create("keycloak-user@localhost")
                    .email("keycloak-user@localhost")
                    .name("keycloak", "User")
                    .password(PASSWORD)
                    .emailVerified(true));
            return realm;
        }
    }

    public static class TestUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("test-user@localhost")
                    .email("test-user@localhost")
                    .name("Tom", "Brady")
                    .password(PASSWORD)
                    .emailVerified(true)
                    .requiredActions(UserModel.RequiredAction.UPDATE_PROFILE.name());
        }
    }

    public static class JohnDohUserConfig implements UserConfig {

        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("john-doh@localhost")
                    .email("john-doh@localhost")
                    .name("John", "Doh")
                    .password(PASSWORD)
                    .emailVerified(true)
                    .requiredActions(UserModel.RequiredAction.UPDATE_PROFILE.name());
        }
    }

}
