/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.keycloak.testsuite.social;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.events.Details;
import org.keycloak.events.Event;
import org.keycloak.events.EventType;
import org.keycloak.models.RealmModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.DummySocialServlet;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.KeycloakRule.KeycloakSetup;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.HashMap;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class SocialLoginTest {

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakSetup() {
        @Override
        public void config(RealmManager manager, RealmModel defaultRealm, RealmModel appRealm) {
            appRealm.setSocial(true);
            appRealm.setUpdateProfileOnInitialSocialLogin(false);

            HashMap<String, String> socialConfig = new HashMap<String, String>();
            socialConfig.put("dummy.key", "1234");
            socialConfig.put("dummy.secret", "1234");
            appRealm.setSocialConfig(socialConfig);
        }
    });

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected AppPage appPage;

    @WebResource
    protected LoginPage loginPage;

    @WebResource
    protected LoginUpdateProfilePage profilePage;

    @WebResource
    protected OAuthClient oauth;

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

    @BeforeClass
    public static void before() {
        keycloakRule.deployServlet("dummy-social", "/dummy-social", DummySocialServlet.class);
    }

    @Test
    public void loginSuccess() throws Exception {
        loginPage.open();

        loginPage.clickSocial("dummy");

        driver.findElement(By.id("id")).sendKeys("1");
        driver.findElement(By.id("username")).sendKeys("dummy-user1");
        driver.findElement(By.id("firstname")).sendKeys("Bob");
        driver.findElement(By.id("lastname")).sendKeys("Builder");
        driver.findElement(By.id("email")).sendKeys("bob@builder.com");
        driver.findElement(By.id("login")).click();

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        String userId = events.expect(EventType.REGISTER)
                .user(AssertEvents.isUUID())
                .detail(Details.EMAIL, "bob@builder.com")
                .detail(Details.REGISTER_METHOD, "social@dummy")
                .detail(Details.REDIRECT_URI, AssertEvents.DEFAULT_REDIRECT_URI)
                .detail(Details.USERNAME, "1@dummy")
                .session((String) null)
                .assertEvent().getUserId();

        Event loginEvent = events.expectSocialLogin()
                .user(userId)
                .detail(Details.USERNAME, "1@dummy")
                .detail(Details.AUTH_METHOD, "social@dummy")
                .assertEvent();

        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);

        AccessTokenResponse response = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "password");

        events.expectCodeToToken(codeId, sessionId).user(userId).assertEvent();

        AccessToken token = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals(36, token.getSubject().length());
        Assert.assertEquals(sessionId, token.getSessionState());

        UserRepresentation profile = keycloakRule.getUserById("test", token.getSubject());
        Assert.assertEquals(36, profile.getUsername().length());

        Assert.assertEquals("Bob", profile.getFirstName());
        Assert.assertEquals("Builder", profile.getLastName());
        Assert.assertEquals("bob@builder.com", profile.getEmail());

        oauth.openLogout();

        events.expectLogout(sessionId).user(userId).assertEvent();

        loginPage.open();

        loginPage.clickSocial("dummy");

        driver.findElement(By.id("id")).sendKeys("1");
        driver.findElement(By.id("username")).sendKeys("dummy-user1");
        driver.findElement(By.id("login")).click();

        events.expectSocialLogin().user(userId).detail(Details.USERNAME, "1@dummy").detail(Details.AUTH_METHOD, "social@dummy").assertEvent();
    }

    @Test
    public void loginEmailExists() throws Exception {
        loginPage.open();
        loginPage.clickSocial("dummy");

        driver.findElement(By.id("id")).sendKeys("loginEmailExists1");
        driver.findElement(By.id("username")).sendKeys("dummy-user1");
        driver.findElement(By.id("firstname")).sendKeys("Bob");
        driver.findElement(By.id("lastname")).sendKeys("Builder");
        driver.findElement(By.id("email")).sendKeys("loginEmailExists@builder.com");
        driver.findElement(By.id("login")).click();

        oauth.openLogout();
        events.clear();

        loginPage.open();

        loginPage.clickSocial("dummy");

        driver.findElement(By.id("id")).sendKeys("loginEmailExists2");
        driver.findElement(By.id("username")).sendKeys("dummy-user2");
        driver.findElement(By.id("firstname")).sendKeys("Bob2");
        driver.findElement(By.id("lastname")).sendKeys("Builder2");
        driver.findElement(By.id("email")).sendKeys("loginEmailExists@builder.com");
        driver.findElement(By.id("login")).click();

        Assert.assertTrue(loginPage.isCurrent());
        Assert.assertEquals("User with email already exists. Please login to account management to link the account.", loginPage.getError());

        events.clear();
    }

    @Test
    public void loginCancelled() throws Exception {
        loginPage.open();

        loginPage.clickSocial("dummy");

        driver.findElement(By.id("cancel")).click();

        Assert.assertTrue(loginPage.isCurrent());
        Assert.assertEquals("Access denied", loginPage.getWarning());

        events.expectSocialLogin().error("rejected_by_user").user((String) null).session((String) null).detail(Details.AUTH_METHOD, "social@dummy").removeDetail(Details.USERNAME).removeDetail(Details.CODE_ID).assertEvent();

        String src = driver.getPageSource();
        loginPage.login("test-user@localhost", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

        events.expectLogin().assertEvent();
    }

    @Test
    public void profileUpdateRequired() {
        keycloakRule.configure(new KeycloakSetup() {
            @Override
            public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                appRealm.setUpdateProfileOnInitialSocialLogin(true);
            }
        });

        try {
            loginPage.open();

            loginPage.clickSocial("dummy");

            driver.findElement(By.id("id")).sendKeys("2");
            driver.findElement(By.id("username")).sendKeys("dummy-user2");
            driver.findElement(By.id("firstname")).sendKeys("Bob");
            driver.findElement(By.id("lastname")).sendKeys("Builder");
            driver.findElement(By.id("email")).sendKeys("bob@builder.com");
            driver.findElement(By.id("login")).click();

            profilePage.isCurrent();

            Assert.assertEquals("Bob", profilePage.getFirstName());
            Assert.assertEquals("Builder", profilePage.getLastName());
            Assert.assertEquals("bob@builder.com", profilePage.getEmail());

            String userId = events.expect(EventType.REGISTER)
                    .user(AssertEvents.isUUID())
                    .detail(Details.EMAIL, "bob@builder.com")
                    .detail(Details.REGISTER_METHOD, "social@dummy")
                    .detail(Details.REDIRECT_URI, AssertEvents.DEFAULT_REDIRECT_URI)
                    .detail(Details.USERNAME, "2@dummy")
                    .assertEvent().getUserId();

            profilePage.update("Dummy", "User", "dummy-user-reg@dummy-social");

            events.expectRequiredAction(EventType.UPDATE_PROFILE).user(userId).detail(Details.AUTH_METHOD, "social@dummy").detail(Details.USERNAME, "2@dummy").assertEvent();
            events.expectRequiredAction(EventType.UPDATE_EMAIL).user(userId).detail(Details.AUTH_METHOD, "social@dummy").detail(Details.USERNAME, "2@dummy").detail(Details.PREVIOUS_EMAIL, "bob@builder.com").detail(Details.UPDATED_EMAIL, "dummy-user-reg@dummy-social").assertEvent();

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            Event loginEvent = events.expectLogin().user(userId).removeDetail(Details.USERNAME).detail(Details.AUTH_METHOD, "social@dummy").detail(Details.USERNAME, "2@dummy").assertEvent();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            AccessTokenResponse response = oauth.doAccessTokenRequest(oauth.getCurrentQuery().get(OAuth2Constants.CODE), "password");
            AccessToken token = oauth.verifyToken(response.getAccessToken());

            events.expectCodeToToken(codeId, loginEvent.getSessionId()).user(userId).assertEvent();

            UserRepresentation profile = keycloakRule.getUserById("test", token.getSubject());

            Assert.assertEquals("Dummy", profile.getFirstName());
            Assert.assertEquals("User", profile.getLastName());
            Assert.assertEquals("dummy-user-reg@dummy-social", profile.getEmail());
        } finally {
            keycloakRule.configure(new KeycloakSetup() {
                @Override
                public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
                    appRealm.setUpdateProfileOnInitialSocialLogin(false);
                }
            });
        }
    }

}
