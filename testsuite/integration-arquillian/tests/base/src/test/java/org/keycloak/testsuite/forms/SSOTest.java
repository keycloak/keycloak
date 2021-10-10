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
package org.keycloak.testsuite.forms;

import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.models.UserModel;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.drone.Different;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class SSOTest extends AbstractTestRealmKeycloakTest {

    @Drone
    @Different
    protected WebDriver driver2;

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected AccountUpdateProfilePage profilePage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void loginSuccess() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();

        IDToken idToken = sendTokenRequestAndGetIDToken(loginEvent);
        Assert.assertEquals("1", idToken.getAcr());
        Long authTime = idToken.getAuth_time();

        appPage.open();

        oauth.openLoginForm();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        loginEvent = events.expectLogin().removeDetail(Details.USERNAME).client("test-app").assertEvent();
        String sessionId2 = loginEvent.getSessionId();

        assertEquals(sessionId, sessionId2);

        // acr is 0 as we authenticated through SSO cookie
        idToken = sendTokenRequestAndGetIDToken(loginEvent);
        Assert.assertEquals("0", idToken.getAcr());
        // auth time hasn't changed as we authenticated through SSO cookie
        Assert.assertEquals(authTime, idToken.getAuth_time());

        profilePage.open();
        assertTrue(profilePage.isCurrent());

        // Expire session
        testingClient.testing().removeUserSession("test", sessionId);

        oauth.doLogin("test-user@localhost", "password");

        String sessionId4 = events.expectLogin().assertEvent().getSessionId();
        assertNotEquals(sessionId, sessionId4);

        events.clear();
    }


    @Test
    public void multipleSessions() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        EventRepresentation login1 = events.expectLogin().assertEvent();

        try {
            //OAuthClient oauth2 = new OAuthClient(driver2);
            OAuthClient oauth2 = new OAuthClient();
            oauth2.init(driver2);

            oauth2.doLogin("test-user@localhost", "password");

            EventRepresentation login2 = events.expectLogin().assertEvent();

            Assert.assertEquals(RequestType.AUTH_RESPONSE, RequestType.valueOf(driver2.getTitle()));
            Assert.assertNotNull(oauth2.getCurrentQuery().get(OAuth2Constants.CODE));

            assertNotEquals(login1.getSessionId(), login2.getSessionId());

            oauth.openLogout();
            events.expectLogout(login1.getSessionId()).assertEvent();

            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());

            oauth2.openLoginForm();

            events.expectLogin().session(login2.getSessionId()).removeDetail(Details.USERNAME).assertEvent();
            Assert.assertEquals(RequestType.AUTH_RESPONSE, RequestType.valueOf(driver2.getTitle()));
            Assert.assertNotNull(oauth2.getCurrentQuery().get(OAuth2Constants.CODE));

            oauth2.openLogout();
            events.expectLogout(login2.getSessionId()).assertEvent();

            oauth2.openLoginForm();

            assertTrue(driver2.getTitle().equals("Sign in to test"));
        } finally {
            driver2.close();
        }
    }


    @Test
    public void loginWithRequiredActionAddedInTheMeantime() {
        // SSO login
        loginPage.open();
        loginPage.login("test-user@localhost", "password");

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();

        // Add update-profile required action to user now
        UserRepresentation user = testRealm().users().get(loginEvent.getUserId()).toRepresentation();
        user.getRequiredActions().add(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        testRealm().users().get(loginEvent.getUserId()).update(user);

        // Attempt SSO login. update-password form is shown
        oauth.openLoginForm();
        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword("password", "password");
        events.expectRequiredAction(EventType.UPDATE_PASSWORD).assertEvent();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        loginEvent = events.expectLogin().removeDetail(Details.USERNAME).client("test-app").assertEvent();
        String sessionId2 = loginEvent.getSessionId();
        assertEquals(sessionId, sessionId2);


    }

}
