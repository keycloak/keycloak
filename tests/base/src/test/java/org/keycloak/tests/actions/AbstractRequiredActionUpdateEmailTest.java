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
package org.keycloak.tests.actions;

import java.util.Arrays;
import java.util.HashMap;

import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.requiredactions.UpdateEmail;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.RequiredActionConfigRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginUpdateProfilePage;
import org.keycloak.testframework.ui.page.UpdateEmailPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.admin.AdminApiUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public abstract class AbstractRequiredActionUpdateEmailTest extends AbstractActionsTest {

    @InjectEvents
    protected Events events;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected UpdateEmailPage updateEmailPage;

    @InjectPage
    protected LoginUpdateProfilePage updateProfilePage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectWebDriver(ref = "secondDriver")
    protected ManagedWebDriver driver2;

    @BeforeEach
    public void beforeTest() {
        AdminApiUtil.enableRequiredAction(managedRealm.admin(), RequiredAction.UPDATE_EMAIL, true);
        RequiredActionConfigRepresentation config = new RequiredActionConfigRepresentation();
        config.setConfig(new HashMap<>());
        config.getConfig().put(UpdateEmail.CONFIG_VERIFY_EMAIL, Boolean.FALSE.toString());
        managedRealm.admin().flows().updateRequiredActionConfig(UserModel.RequiredAction.UPDATE_EMAIL.name(), config);

        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true)
                .username("test-user@localhost")
                .email("test-user@localhost")
                .firstName("Tom")
                .lastName("Brady")
                .requiredActions(UserModel.RequiredAction.UPDATE_EMAIL.name()).build();
        prepareUser(user);
        AdminApiUtil.createUserAndResetPasswordWithAdminClient(managedRealm.admin(), user, "password");

        AdminApiUtil.removeUserByUsername(managedRealm.admin(), "john-doh@localhost");
        user = UserBuilder.create().enabled(true)
                .username("john-doh@localhost")
                .email("john-doh@localhost")
                .firstName("John")
                .lastName("Doh")
                .requiredActions(UserModel.RequiredAction.UPDATE_EMAIL.name()).build();
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

        Assertions.assertEquals("", updateEmailPage.getEmail());
        Assertions.assertTrue(updateEmailPage.getEmailInputError().contains("Please specify email."));

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateEmailDuplicate() {
        oauth.openLoginForm();

        loginPage.login("john-doh@localhost", "password");

        updateEmailPage.assertCurrent();
        updateEmailPage.changeEmail("test-user@localhost");
        updateEmailPage.assertCurrent();

        Assertions.assertEquals("test-user@localhost", updateEmailPage.getEmail());
        Assertions.assertEquals("Email already exists.", updateEmailPage.getEmailInputError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateEmailInvalid() {
        oauth.openLoginForm();

        loginPage.login("test-user@localhost", "password");

        updateEmailPage.assertCurrent();
        updateEmailPage.changeEmail("invalid");
        updateEmailPage.assertCurrent();

        Assertions.assertEquals("invalid", updateEmailPage.getEmail());
        Assertions.assertEquals("Invalid email address.", updateEmailPage.getEmailInputError());

        Assertions.assertNull(events.poll());
    }

    @Test
    public void updateEmailWithEmailAsUsernameEnabled() throws Exception {
        Boolean genuineRegistrationEmailAsUsername = managedRealm.admin()
                .toRepresentation()
                .isRegistrationEmailAsUsername();

        setRegistrationEmailAsUsername(managedRealm.admin(), true);
        try {
            UserRepresentation user = ActionUtil.findUserWithAdminClient(managedRealm.admin(), "test-user@localhost");
            String firstName = user.getFirstName();
            String lastName = user.getLastName();
            assertNotNull(firstName);
            assertNotNull(lastName);
            changeEmailUsingRequiredAction("new@localhost", true, true);
            user = ActionUtil.findUserWithAdminClient(managedRealm.admin(), "new@localhost");
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
