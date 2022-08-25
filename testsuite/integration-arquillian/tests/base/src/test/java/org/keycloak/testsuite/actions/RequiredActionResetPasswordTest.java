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

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.SecondBrowser;
import org.openqa.selenium.WebDriver;

import java.util.LinkedList;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class RequiredActionResetPasswordTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setResetPasswordAllowed(Boolean.TRUE);
    }

    @Drone
    @SecondBrowser
    private WebDriver driver2;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected LoginPasswordUpdatePage changePasswordPage;

    @After
    public void after() {
        ApiUtil.resetUserPassword(testRealm().users().get(findUser("test-user@localhost").getId()), "password", false);
    }

    @Test
    public void tempPassword() throws Exception {
        requireUpdatePassword();
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        changePasswordPage.assertCurrent();
        assertFalse(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        OAuthClient.AccessTokenResponse tokenResponse = sendTokenRequestAndGetResponse(loginEvent);
        oauth.idTokenHint(tokenResponse.getIdToken()).openLogout();

        events.expectLogout(loginEvent.getSessionId()).assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "new-password");

        events.expectLogin().assertEvent();
    }

    @Test
    public void logoutSessionsCheckboxNotPresent() {
        OAuthClient oauth2 = new OAuthClient();
        oauth2.init(driver2);

        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());

        oauth2.doLogin("test-user@localhost", "password");
        events.expectLogin().assertEvent();
        assertEquals(1, testUser.getUserSessions().size());

        requireUpdatePassword();

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        changePasswordPage.assertCurrent();
        assertFalse(changePasswordPage.isLogoutSessionDisplayed());
        changePasswordPage.changePassword("All Right Then, Keep Your Secrets", "All Right Then, Keep Your Secrets");
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        events.expectLogin().assertEvent();

        assertEquals("All sessions are still active", 2, testUser.getUserSessions().size());
    }

    private void requireUpdatePassword() {
        UserRepresentation userRep = findUser("test-user@localhost");
        if (userRep.getRequiredActions() == null) {
            userRep.setRequiredActions(new LinkedList<>());
        }
        userRep.getRequiredActions().add(RequiredAction.UPDATE_PASSWORD.name());
        testRealm().users().get(userRep.getId()).update(userRep);
    }

}
