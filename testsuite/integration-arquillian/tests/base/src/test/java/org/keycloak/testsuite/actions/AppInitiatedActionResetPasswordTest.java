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

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.SecondBrowser;
import org.openqa.selenium.WebDriver;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Stan Silvert
 */
public class AppInitiatedActionResetPasswordTest extends AbstractAppInitiatedActionTest {

    @Override
    protected String getAiaAction() {
        return UserModel.RequiredAction.UPDATE_PASSWORD.name();
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        testRealm.setResetPasswordAllowed(Boolean.TRUE);
    }

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected LoginPasswordUpdatePage changePasswordPage;

    @Drone
    @SecondBrowser
    private WebDriver driver2;

    @After
    public void after() {
        ApiUtil.resetUserPassword(testRealm().users().get(findUser("test-user@localhost").getId()), "password", false);
    }

    @Test
    public void resetPassword() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        events.expectLogin().assertEvent();

        doAIA();

        changePasswordPage.assertCurrent();
        assertTrue(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        assertKcActionStatus(SUCCESS);

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        oauth.openLogout();

        events.expectLogout(loginEvent.getSessionId()).assertEvent();

        loginPage.open();
        loginPage.login("test-user@localhost", "new-password");

        events.expectLogin().assertEvent();
    }

    @Test
    public void resetPasswordRequiresReAuth() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        events.expectLogin().assertEvent();

        setTimeOffset(350);

        // Should prompt for re-authentication
        doAIA();

        loginPage.assertCurrent();
        Assert.assertEquals("test-user@localhost", loginPage.getAttemptedUsername());
        loginPage.login("password");

        changePasswordPage.assertCurrent();
        assertTrue(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        assertKcActionStatus(SUCCESS);
    }

    @Test
    public void cancelChangePassword() throws Exception {
        doAIA();
        
        loginPage.login("test-user@localhost", "password");
        
        changePasswordPage.assertCurrent();
        changePasswordPage.cancel();
        
        assertKcActionStatus(CANCELLED);
    }

    @Test
    public void resetPasswordUserHasUpdatePasswordRequiredAction() throws Exception {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        UserResource userResource = testRealm().users().get(findUser("test-user@localhost").getId());
        UserRepresentation userRep = userResource.toRepresentation();
        userRep.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.name());
        userResource.update(userRep);

        events.expectLogin().assertEvent();

        doAIA();

        changePasswordPage.assertCurrent();
        assertFalse(changePasswordPage.isCancelDisplayed());

        changePasswordPage.changePassword("new-password", "new-password");

        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        assertKcActionStatus(SUCCESS);
    }

    @Test
    public void checkLogoutSessions() {
        OAuthClient oauth2 = new OAuthClient();
        oauth2.init(driver2);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());
        List<UserSessionRepresentation> sessions = testUser.getUserSessions();
        assertEquals(1, sessions.size());
        final String firstSessionId = sessions.get(0).getId();

        oauth2.doLogin("test-user@localhost", "password");
        events.expectLogin().assertEvent();
        assertEquals(2, testUser.getUserSessions().size());

        doAIA();

        changePasswordPage.assertCurrent();
        assertTrue("Logout sessions is checked by default", changePasswordPage.isLogoutSessionsChecked());
        changePasswordPage.changePassword("All Right Then, Keep Your Secrets", "All Right Then, Keep Your Secrets");
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        assertKcActionStatus(SUCCESS);

        sessions = testUser.getUserSessions();
        assertEquals(1, sessions.size());
        assertEquals("Old session is still valid", firstSessionId, sessions.get(0).getId());
    }

    @Test
    public void uncheckLogoutSessions() {
        OAuthClient oauth2 = new OAuthClient();
        oauth2.init(driver2);

        UserResource testUser = testRealm().users().get(findUser("test-user@localhost").getId());

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        oauth2.doLogin("test-user@localhost", "password");
        events.expectLogin().assertEvent();
        assertEquals(2, testUser.getUserSessions().size());

        doAIA();

        changePasswordPage.assertCurrent();
        changePasswordPage.uncheckLogoutSessions();
        changePasswordPage.changePassword("All Right Then, Keep Your Secrets", "All Right Then, Keep Your Secrets");
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();
        assertKcActionStatus(SUCCESS);

        assertEquals(2, testUser.getUserSessions().size());
    }

}
