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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.pages.*;
import org.keycloak.testsuite.pages.AppPage.RequestType;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionMultipleActionsTest extends TestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        ActionUtil.addRequiredActionForUser(testRealm, "test-user@localhost", RequiredAction.UPDATE_PROFILE.name());
        ActionUtil.addRequiredActionForUser(testRealm, "test-user@localhost", RequiredAction.UPDATE_PASSWORD.name());
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

    @Test
    public void updateProfileAndPassword() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        String sessionId = null;
        if (changePasswordPage.isCurrent()) {
            sessionId = updatePassword(sessionId);

            updateProfilePage.assertCurrent();
            updateProfile(sessionId);
        } else if (updateProfilePage.isCurrent()) {
            sessionId = updateProfile(sessionId);

            changePasswordPage.assertCurrent();
            updatePassword(sessionId);
        } else {
            Assert.fail("Expected to update password and profile before login");
        }

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().session(sessionId).assertEvent();
    }

    public String updatePassword(String sessionId) {
        changePasswordPage.changePassword("new-password", "new-password");

        AssertEvents.ExpectedEvent expectedEvent = events.expectRequiredAction(EventType.UPDATE_PASSWORD);
        if (sessionId != null) {
            expectedEvent.session(sessionId);
        }
        return expectedEvent.assertEvent().getSessionId();
    }

    public String updateProfile(String sessionId) {
        updateProfilePage.update("New first", "New last", "new@email.com", "test-user@localhost");

        AssertEvents.ExpectedEvent expectedEvent = events.expectRequiredAction(EventType.UPDATE_EMAIL).detail(Details.PREVIOUS_EMAIL, "test-user@localhost").detail(Details.UPDATED_EMAIL, "new@email.com");
        if (sessionId != null) {
            expectedEvent.session(sessionId);
        }
        sessionId = expectedEvent.assertEvent().getSessionId();
        events.expectRequiredAction(EventType.UPDATE_PROFILE).session(sessionId).assertEvent();
        return sessionId;
    }

}
