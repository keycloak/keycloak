/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates
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
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.authentication.requiredactions.TermsAndConditions;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfileEditUsernameAllowedPage;
import org.keycloak.testsuite.pages.TermsAndConditionsPage;
import org.keycloak.testsuite.util.UserBuilder;

/**
 * @author <a href="mailto:wadahiro@gmail.com">Hiroyuki Wada</a>
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class RequiredActionPriorityTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginPasswordUpdatePage changePasswordPage;

    @Page
    protected LoginUpdateProfileEditUsernameAllowedPage updateProfilePage;

    @Page
    protected TermsAndConditionsPage termsPage;

    @Before
    public void setupRequiredActions() {
        setRequiredActionEnabled("test", TermsAndConditions.PROVIDER_ID, true, false);

        // Because of changing the password in test case, we need to re-create the user.
        ApiUtil.removeUserByUsername(testRealm(), "test-user@localhost");
        UserRepresentation user = UserBuilder.create().enabled(true).username("test-user@localhost")
                .email("test-user@localhost").build();
        String testUserId = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, "password");

        setRequiredActionEnabled("test", testUserId, RequiredAction.UPDATE_PASSWORD.name(), true);
        setRequiredActionEnabled("test", testUserId, RequiredAction.UPDATE_PROFILE.name(), true);
        setRequiredActionEnabled("test", testUserId, TermsAndConditions.PROVIDER_ID, true);
    }

    @Test
    public void executeRequiredActionsWithDefaultPriority() throws Exception {
        // Default priority is alphabetical order:
        // TermsAndConditions -> UpdatePassword -> UpdateProfile

        // Login
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        // First, accept terms
        termsPage.assertCurrent();
        termsPage.acceptTerms();
        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).removeDetail(Details.REDIRECT_URI)
                .detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID).assertEvent();

        // Second, change password
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("new-password", "new-password");
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        // Finally, update profile
        updateProfilePage.assertCurrent();
        updateProfilePage.update("New first", "New last", "new@email.com", "test-user@localhost");
        events.expectRequiredAction(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, "test-user@localhost")
                .detail(Details.UPDATED_EMAIL, "new@email.com").assertEvent();
        events.expectRequiredAction(EventType.UPDATE_PROFILE).assertEvent();

        // Logined
        appPage.assertCurrent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().assertEvent();
    }

    @Test
    public void executeRequiredActionsWithCustomPriority() throws Exception {
        // Default priority is alphabetical order:
        // TermsAndConditions -> UpdatePassword -> UpdateProfile

        // After Changing the priority, the order will be:
        // UpdatePassword -> UpdateProfile -> TermsAndConditions
        testRealm().flows().raiseRequiredActionPriority(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        testRealm().flows().lowerRequiredActionPriority("terms_and_conditions");

        // Login
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        // First, change password
        changePasswordPage.assertCurrent();
        changePasswordPage.changePassword("new-password", "new-password");
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        // Second, update profile
        updateProfilePage.assertCurrent();
        updateProfilePage.update("New first", "New last", "new@email.com", "test-user@localhost");
        events.expectRequiredAction(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, "test-user@localhost")
                .detail(Details.UPDATED_EMAIL, "new@email.com").assertEvent();
        events.expectRequiredAction(EventType.UPDATE_PROFILE).assertEvent();

        // Finally, accept terms
        termsPage.assertCurrent();
        termsPage.acceptTerms();
        events.expectRequiredAction(EventType.CUSTOM_REQUIRED_ACTION).removeDetail(Details.REDIRECT_URI)
                .detail(Details.CUSTOM_REQUIRED_ACTION, TermsAndConditions.PROVIDER_ID).assertEvent();

        // Logined
        appPage.assertCurrent();
        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        events.expectLogin().assertEvent();
    }
}
