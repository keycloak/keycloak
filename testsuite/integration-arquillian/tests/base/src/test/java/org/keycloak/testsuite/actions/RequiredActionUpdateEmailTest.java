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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.admin.client.resource.AuthenticationManagementResource;
import org.keycloak.admin.client.resource.UserProfileResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.requiredactions.UpdateEmail;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RequiredActionProviderRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.representations.userprofile.config.UPAttribute;
import org.keycloak.representations.userprofile.config.UPAttributePermissions;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.util.oauth.OAuthClient;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.userprofile.UserProfileConstants.ROLE_ADMIN;
import static org.keycloak.userprofile.UserProfileConstants.ROLE_USER;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class RequiredActionUpdateEmailTest extends AbstractRequiredActionUpdateEmailTest {

    @Override
    protected void changeEmailUsingRequiredAction(String newEmail, boolean logoutOtherSessions, boolean newEmailAsUsername) {
        loginPage.open();

        loginPage.login("test-user@localhost", "password");
        updateEmailPage.assertCurrent();
        if (logoutOtherSessions) {
            updateEmailPage.checkLogoutSessions();
        }
        Assert.assertEquals(logoutOtherSessions, updateEmailPage.isLogoutSessionsChecked());

        updateEmailPage.changeEmail(newEmail);
    }

    private void updateEmail(boolean logoutOtherSessions) {
        // login using another session
        configureRequiredActionsToUser("test-user@localhost");
        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        OAuthClient oauth2 = oauth.newConfig().driver(driver2);;
        oauth2.doLogin("test-user@localhost", "password");
        EventRepresentation event1 = events.expectLogin().assertEvent();
        assertEquals(1, testUser.getUserSessions().size());

        // add the action and change it
        configureRequiredActionsToUser("test-user@localhost", UserModel.RequiredAction.UPDATE_EMAIL.name());
        changeEmailUsingRequiredAction("new@localhost", logoutOtherSessions, false);

        if (logoutOtherSessions) {
            events.expectLogout(event1.getSessionId())
                    .detail(Details.LOGOUT_TRIGGERED_BY_REQUIRED_ACTION, UserModel.RequiredAction.UPDATE_EMAIL.name())
                    .assertEvent();
        }

        events.expectRequiredAction(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, "test-user@localhost")
                .detail(Details.UPDATED_EMAIL, "new@localhost").assertEvent();
        assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation event2 = events.expectLogin().assertEvent();
        List<UserSessionRepresentation> sessions = testUser.getUserSessions();
        if (logoutOtherSessions) {
            assertEquals(1, sessions.size());
            assertEquals(event2.getSessionId(), sessions.iterator().next().getId());
        } else {
            assertEquals(2, sessions.size());
            MatcherAssert.assertThat(sessions.stream().map(UserSessionRepresentation::getId).collect(Collectors.toList()),
                    Matchers.containsInAnyOrder(event1.getSessionId(), event2.getSessionId()));
        }

        // assert user is really updated in persistent store
        UserRepresentation user = ActionUtil.findUserWithAdminClient(adminClient, "test-user@localhost");
        assertEquals("new@localhost", user.getEmail());
        assertEquals("Tom", user.getFirstName());
        assertEquals("Brady", user.getLastName());
        assertFalse(user.getRequiredActions().contains(UserModel.RequiredAction.UPDATE_EMAIL.name()));
    }

    @Test
    public void updateEmailLogoutSessionsChecked() {
        updateEmail(true);
    }

    @Test
    public void updateEmailLogoutSessionsNotChecked() {
        updateEmail(false);
    }

    @Test
    public void updateEmailRequiredActionWhenEmailIsReadonly() {
        UserProfileResource userProfile = testRealm().users().userProfile();
        UPConfig upConfigOld = userProfile.getConfiguration();
        UPConfig upConfig = userProfile.getConfiguration();
        upConfig.addOrReplaceAttribute((new UPAttribute(UserModel.EMAIL, new UPAttributePermissions(Set.of(ROLE_USER, ROLE_ADMIN), Set.of(ROLE_ADMIN)))));
        getCleanup().addCleanup(() -> {
            userProfile.update(upConfigOld);
        });
        userProfile.update(upConfig);

        configureRequiredActionsToUser("test-user@localhost", UserModel.RequiredAction.UPDATE_EMAIL.name());

        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        assertEquals(1, testUser.toRepresentation().getRequiredActions().size());

        loginPage.open();

        loginPage.login("test-user@localhost", "password");

        // UPDATE_EMAIL required action is skipped and cleared
        appPage.assertCurrent();

        assertEquals(0, testUser.toRepresentation().getRequiredActions().size());
    }

    @Test
    public void testUpdateProfileWhenEmailIsSetAndIsWritable() {
        configureRequiredActionsToUser("test-user@localhost", RequiredAction.UPDATE_PROFILE.name());
        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        assertEquals(1, testUser.toRepresentation().getRequiredActions().size());

        // login and update profile, email is already set and should not be visible
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        updateProfilePage.assertCurrent();
        assertFalse(updateProfilePage.isEmailInputPresent());
        updateProfilePage.update("Tom", "Brady");

        // successfully update the profile without providing the email
        appPage.assertCurrent();
        assertEquals(0, testUser.toRepresentation().getRequiredActions().size());
    }

    @Test
    public void testUpdateProfileWhenEmailNotSetAndIsWritable() {
        configureRequiredActionsToUser("test-user@localhost", RequiredAction.UPDATE_PROFILE.name());
        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        assertEquals(1, testUser.toRepresentation().getRequiredActions().size());
        UserRepresentation rep = testUser.toRepresentation();
        rep.setEmail("");
        testUser.update(rep);

        // login and update profile, including the email
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        updateProfilePage.assertCurrent();
        assertTrue(updateProfilePage.isEmailInputPresent());
        updateProfilePage.update("Tom", "Brady", "test-user@localhost");

        appPage.assertCurrent();
        rep = testUser.toRepresentation();
        assertEquals(0, rep.getRequiredActions().size());
        assertNull(Optional.ofNullable(rep.getAttributes()).orElse(Map.of()).get(UserModel.EMAIL_PENDING));
        assertEquals("test-user@localhost", rep.getEmail());
    }

    @Test
    public void testUpdateProfileWhenEmailNotSetAndIsNotWritable() {
        UPConfig upConfig = testRealm().users().userProfile().getConfiguration();
        upConfig.getAttribute(UserModel.EMAIL).setPermissions(new UPAttributePermissions(Set.of(ROLE_USER, ROLE_ADMIN), Set.of(ROLE_ADMIN)));
        testRealm().users().userProfile().update(upConfig);
        getCleanup().addCleanup(() -> {
            upConfig.getAttribute(UserModel.EMAIL).setPermissions(new UPAttributePermissions(Set.of(ROLE_USER, ROLE_ADMIN), Set.of(ROLE_USER, ROLE_ADMIN)));
            testRealm().users().userProfile().update(upConfig);
        });
        configureRequiredActionsToUser("test-user@localhost", RequiredAction.UPDATE_PROFILE.name());
        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        assertEquals(1, testUser.toRepresentation().getRequiredActions().size());
        UserRepresentation rep = testUser.toRepresentation();
        rep.setEmail("");
        testUser.update(rep);

        // login and update profile, email is readonly for users and should not be visible
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        updateProfilePage.assertCurrent();
        assertFalse(updateProfilePage.isEmailInputPresent());
        updateProfilePage.update("Tom", "Brady");

        appPage.assertCurrent();
        rep = testUser.toRepresentation();
        assertEquals(0, rep.getRequiredActions().size());
        assertNull(Optional.ofNullable(rep.getAttributes()).orElse(Map.of()).get(UserModel.EMAIL_PENDING));
        assertNull(rep.getEmail());
    }

    @Test
    public void testFailWhenSendingVerificationEmail() {
        AuthenticationManagementResource authMgt = testRealm().flows();
        RequiredActionProviderRepresentation requiredAction = authMgt.getRequiredActions().stream()
                .filter(action -> RequiredAction.UPDATE_EMAIL.name().equals(action.getAlias()))
                .findAny().get();
        requiredAction.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.TRUE.toString());
        authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);
        getCleanup().addCleanup(() -> {
            requiredAction.getConfig().remove(UpdateEmail.CONFIG_VERIFY_EMAIL);
            authMgt.updateRequiredAction(requiredAction.getAlias(), requiredAction);
        });

        configureRequiredActionsToUser("test-user@localhost", RequiredAction.UPDATE_PROFILE.name());
        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        assertEquals(1, testUser.toRepresentation().getRequiredActions().size());
        UserRepresentation rep = testUser.toRepresentation();
        rep.setEmail("");
        testUser.update(rep);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        updateProfilePage.assertCurrent();
        assertTrue(updateProfilePage.isEmailInputPresent());
        updateProfilePage.update("Tom", "Brady", "test-user@localhost");
        errorPage.assertCurrent();
        assertEquals("Failed to send email, please try again later.", errorPage.getError());
        rep = testUser.toRepresentation();
        assertEquals(1, rep.getRequiredActions().size());
        assertEquals(RequiredAction.UPDATE_EMAIL.name(), rep.getRequiredActions().get(0));
        assertNull(Optional.ofNullable(rep.getAttributes()).orElse(Map.of()).get(UserModel.EMAIL_PENDING));
        assertNull(rep.getEmail());
    }
}
