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

import org.junit.Assert;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.pages.AccountUpdateProfilePage;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SSOTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule();

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected AccountUpdateProfilePage profilePage;

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @Test
    public void loginSuccess() {
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        
        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        String sessionId = events.expectLogin().assertEvent().getSessionId();

        appPage.open();

        oauth.openLoginForm();

        assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        profilePage.open();

        assertTrue(profilePage.isCurrent());

        String sessionId2 = events.expectLogin().removeDetail(Details.USERNAME).client("test-app").assertEvent().getSessionId();

        assertEquals(sessionId, sessionId2);

        // Expire session
        keycloakRule.removeUserSession(sessionId);

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

        Event login1 = events.expectLogin().assertEvent();

        WebDriver driver2 = WebRule.createWebDriver();
        try {
            OAuthClient oauth2 = new OAuthClient(driver2);
            oauth2.state("mystate");
            oauth2.doLogin("test-user@localhost", "password");

            Event login2 = events.expectLogin().assertEvent();

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

            assertTrue(driver2.getTitle().equals("Log in to test"));
        } finally {
            driver2.close();
        }
    }

}
