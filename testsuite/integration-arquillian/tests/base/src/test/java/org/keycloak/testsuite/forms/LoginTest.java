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

import java.net.MalformedURLException;
import java.net.URI;
import org.jboss.arquillian.drone.api.annotation.Drone;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.util.Retry;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.AuthServerTestEnricher;
import org.keycloak.testsuite.console.page.AdminConsole;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.AppPage.RequestType;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.util.ContainerAssume;
import org.keycloak.testsuite.util.DroneUtils;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.RandomStringUtils;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.ApiUtil.findClientByClientId;
import static org.keycloak.testsuite.util.OAuthClient.AUTH_SERVER_ROOT;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWithLoginUrlOf;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class LoginTest extends AbstractTestRealmKeycloakTest {

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserRepresentation user = UserBuilder.create()
                                             .id("login-test")
                                             .username("login-test")
                                             .email("login@test.com")
                                             .enabled(true)
                                             .password("password")
                                             .build();
        userId = user.getId();

        UserRepresentation user2 = UserBuilder.create()
                                              .id("login-test2")
                                              .username("login-test2")
                                              .email("login2@test.com")
                                              .enabled(true)
                                              .password("password")
                                              .build();
        user2Id = user2.getId();

        UserRepresentation admin = UserBuilder.create()
                .username("admin")
                .password("admin")
                .enabled(true)
                .build();
        HashMap<String, List<String>> clientRoles = new HashMap<>();
        clientRoles.put("realm-management", Arrays.asList("realm-admin"));
        admin.setClientRoles(clientRoles);

        RealmBuilder.edit(testRealm)
                    .user(user)
                    .user(user2)
                    .user(admin);
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    protected AppPage appPage;

    @Page
    @JavascriptBrowser
    protected AdminConsole jsAdminConsole;

    @Drone
    @JavascriptBrowser
    protected WebDriver jsDriver;

    @Page
    protected LoginPage loginPage;

    @Page
    @JavascriptBrowser
    protected LoginPage jsLoginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    private static String userId;

    private static String user2Id;

    @Test
    public void testBrowserSecurityHeaders() {
        Client client = ClientBuilder.newClient();
        Response response = client.target(oauth.getLoginFormUrl()).request().get();
        Assert.assertThat(response.getStatus(), is(equalTo(200)));
        for (Map.Entry<String, String> entry : BrowserSecurityHeaders.defaultHeaders.entrySet()) {
            String headerName = BrowserSecurityHeaders.headerAttributeMap.get(entry.getKey());
            String headerValue = response.getHeaderString(headerName);
            if (entry.getValue().isEmpty()) {
                Assert.assertNull(headerValue);
            } else {
                Assert.assertNotNull(headerValue);
                Assert.assertThat(headerValue, is(equalTo(entry.getValue())));
            }
        }
        response.close();
        client.close();
    }

    @Test
    public void testContentSecurityPolicyReportOnlyBrowserSecurityHeader() {
        final String expectedCspReportOnlyValue = "default-src 'none'";
        final String cspReportOnlyAttr = "contentSecurityPolicyReportOnly";
        final String cspReportOnlyHeader = "Content-Security-Policy-Report-Only";

        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        final String defaultContentSecurityPolicyReportOnly = realmRep.getBrowserSecurityHeaders().get(cspReportOnlyAttr);
        realmRep.getBrowserSecurityHeaders().put(cspReportOnlyAttr, expectedCspReportOnlyValue);
        adminClient.realm("test").update(realmRep);

        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(oauth.getLoginFormUrl()).request().get();
            String headerValue = response.getHeaderString(cspReportOnlyHeader);
            Assert.assertThat(headerValue, is(equalTo(expectedCspReportOnlyValue)));
            response.close();
            client.close();
        } finally {
            realmRep.getBrowserSecurityHeaders().put(cspReportOnlyAttr, defaultContentSecurityPolicyReportOnly);
            adminClient.realm("test").update(realmRep);
        }
    }

    //KEYCLOAK-5556
    @Test
    public void testPOSTAuthenticationRequest() {
        Client client = ClientBuilder.newClient();

        //POST request to http://localhost:8180/auth/realms/test/protocol/openid-connect/auth;
        UriBuilder b = OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(AUTH_SERVER_ROOT));
        Response response = client.target(b.build(oauth.getRealm())).request().post(oauth.getLoginEntityForPOST());
        
        Assert.assertThat(response.getStatus(), is(equalTo(200)));
        Assert.assertThat(response, Matchers.body(containsString("Log In")));

        response.close();
        client.close();
    }

    @Test
    public void loginWithLongRedirectUri() throws Exception {
        try (AutoCloseable c = new RealmAttributeUpdater(adminClient.realm("test"))
                .updateWith(r -> r.setEventsEnabled(true)).update()) {
            String randomLongString = RandomStringUtils.random(2500, true, true);
            String longRedirectUri = oauth.getRedirectUri() + "?longQueryParameterValue=" + randomLongString;
            UriBuilder longLoginUri = UriBuilder.fromUri(oauth.getLoginFormUrl()).replaceQueryParam(OAuth2Constants.REDIRECT_URI, longRedirectUri);

            DroneUtils.getCurrentDriver().navigate().to(longLoginUri.build().toString());

            loginPage.assertCurrent();
            loginPage.login("login-test", "password");

            events.expectLogin().user(userId).detail(OAuth2Constants.REDIRECT_URI, longRedirectUri).assertEvent();
        }
    }

    @Test
    public void loginChangeUserAfterInvalidPassword() {
        loginPage.open();
        loginPage.login("login-test2", "invalid");

        loginPage.assertCurrent();

        Assert.assertEquals("login-test2", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user(user2Id).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test2")
                .removeDetail(Details.CONSENT)
                .assertEvent();

        loginPage.login("login-test", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginInvalidPassword() {
        loginPage.open();
        loginPage.login("login-test", "invalid");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("login-test", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    public void loginMissingPassword() {
        loginPage.open();
        loginPage.missingPassword("login-test");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("login-test", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                .detail(Details.USERNAME, "login-test")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    private void setUserEnabled(String userName, boolean enabled) {
        UserRepresentation rep = adminClient.realm("test").users().get(userName).toRepresentation();
        rep.setEnabled(enabled);
        adminClient.realm("test").users().get(userName).update(rep);
    }

    @Test
    public void loginInvalidPasswordDisabledUser() {
        setUserEnabled("login-test", false);

        try {
            loginPage.open();
            loginPage.login("login-test", "invalid");

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assert.assertEquals("login-test", loginPage.getUsername());
            Assert.assertEquals("", loginPage.getPassword());

            // KEYCLOAK-2024
            Assert.assertEquals("Invalid username or password.", loginPage.getError());

            events.expectLogin().user(userId).session((String) null).error("invalid_user_credentials")
                    .detail(Details.USERNAME, "login-test")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            setUserEnabled("login-test", true);
        }
    }

    @Test
    public void loginDisabledUser() {
        setUserEnabled("login-test", false);

        try {
            loginPage.open();
            loginPage.login("login-test", "password");

            loginPage.assertCurrent();

            // KEYCLOAK-1741 - assert form field values kept
            Assert.assertEquals("login-test", loginPage.getUsername());
            Assert.assertEquals("", loginPage.getPassword());

            // KEYCLOAK-2024
            Assert.assertEquals("Account is disabled, contact your administrator.", loginPage.getError());

            events.expectLogin().user(userId).session((String) null).error("user_disabled")
                    .detail(Details.USERNAME, "login-test")
                    .removeDetail(Details.CONSENT)
                    .assertEvent();
        } finally {
            setUserEnabled("login-test", true);
        }
    }

    @Test
    public void loginInvalidUsername() {
        loginPage.open();
        loginPage.login("invalid", "password");

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        Assert.assertEquals("invalid", loginPage.getUsername());
        Assert.assertEquals("", loginPage.getPassword());

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error("user_not_found")
                .detail(Details.USERNAME, "invalid")
                .removeDetail(Details.CONSENT)
                .assertEvent();

        loginPage.login("login-test", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginMissingUsername() {
        loginPage.open();
        loginPage.missingUsername();

        loginPage.assertCurrent();

        Assert.assertEquals("Invalid username or password.", loginPage.getError());

        events.expectLogin().user((String) null).session((String) null).error("user_not_found")
                .removeDetail(Details.CONSENT)
                .assertEvent();
    }

    @Test
    // KEYCLOAK-2557
    public void loginUserWithEmailAsUsername() {
        loginPage.open();
        loginPage.login("login@test.com", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login@test.com").assertEvent();
    }

    @Test
    public void loginSuccess() {
        loginPage.open();
        loginPage.login("login-test", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginSuccessRealmSigningAlgorithms() throws JWSInputException {
        ContainerAssume.assumeAuthServerSSL();

        loginPage.open();
        loginPage.login("login-test", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

        driver.navigate().to(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth/realms/test/");
        String keycloakIdentity = driver.manage().getCookieNamed("KEYCLOAK_IDENTITY").getValue();

        // Check identity cookie is signed with HS256
        String algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
        assertEquals("HS256", algorithm);

        try {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.ES256);

            oauth.openLoginForm();
            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());

            driver.navigate().to(AuthServerTestEnricher.getAuthServerContextRoot() + "/auth/realms/test/");
            keycloakIdentity = driver.manage().getCookieNamed("KEYCLOAK_IDENTITY").getValue();

            // Check identity cookie is still signed with HS256
            algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
            assertEquals("HS256", algorithm);

            // Check identity cookie still works
            oauth.openLoginForm();
            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
        }
    }

    @Test
    public void loginWithWhitespaceSuccess() {
        loginPage.open();
        loginPage.login(" login-test \t ", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginWithEmailWhitespaceSuccess() {
        loginPage.open();
        loginPage.login("    login@test.com    ", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).assertEvent();
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = adminClient.realm("test").toRepresentation();
        realmRep.setPasswordPolicy(policy);
        adminClient.realm("test").update(realmRep);
    }

    @Test
    public void loginWithForcePasswordChangePolicy() {
        setPasswordPolicy("forceExpiredPasswordChange(1)");

        try {
            // Setting offset to more than one day to force password update
            // elapsedTime > timeToExpire
            setTimeOffset(86405);

            loginPage.open();

            loginPage.login("login-test", "password");

            updatePasswordPage.assertCurrent();

            updatePasswordPage.changePassword("updatedPassword", "updatedPassword");

            setTimeOffset(0);

            events.expectRequiredAction(EventType.UPDATE_PASSWORD).user(userId).detail(Details.USERNAME, "login-test").assertEvent();

            String currentUrl = driver.getCurrentUrl();
            String pageSource = driver.getPageSource();
            assertEquals("bad expectation, on page: " + currentUrl, RequestType.AUTH_RESPONSE, appPage.getRequestType());

            events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();

        } finally {
            setPasswordPolicy(null);
            UserResource userRsc = adminClient.realm("test").users().get("login-test");
            ApiUtil.resetUserPassword(userRsc, "password", false);
        }
    }

    @Test
    public void loginWithoutForcePasswordChangePolicy() {
        setPasswordPolicy("forceExpiredPasswordChange(1)");

        try {
            // Setting offset to less than one day to avoid forced password update
            // elapsedTime < timeToExpire
            setTimeOffset(86205);

            loginPage.open();

            loginPage.login("login-test", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

            setTimeOffset(0);

            events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
        } finally {
            setPasswordPolicy(null);
        }
    }

    @Test
    public void loginNoTimeoutWithLongWait() {
        loginPage.open();

        setTimeOffset(1700);

        loginPage.login("login-test", "password");

        setTimeOffset(0);

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent().getSessionId();
    }



    @Test
    public void loginLoginHint() {
        String loginFormUrl = oauth.getLoginFormUrl() + "&login_hint=login-test";
        driver.navigate().to(loginFormUrl);

        Assert.assertEquals("login-test", loginPage.getUsername());
        loginPage.login("password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).detail(Details.USERNAME, "login-test").assertEvent();
    }

    @Test
    public void loginWithEmailSuccess() {
        loginPage.open();
        loginPage.login("login@test.com", "password");

        Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
        Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));

        events.expectLogin().user(userId).assertEvent();
    }

    private void setRememberMe(boolean enabled) {
        this.setRememberMe(enabled, null, null);
    }

    private void setRememberMe(boolean enabled, Integer idleTimeout, Integer maxLifespan) {
        RealmRepresentation rep = adminClient.realm("test").toRepresentation();
        rep.setRememberMe(enabled);
        rep.setSsoSessionIdleTimeoutRememberMe(idleTimeout);
        rep.setSsoSessionMaxLifespanRememberMe(maxLifespan);
        adminClient.realm("test").update(rep);
    }

    @Test
    public void loginWithRememberMe() {
        setRememberMe(true);

        try {
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login-test", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
            EventRepresentation loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login-test")
                                                   .detail(Details.REMEMBER_ME, "true")
                                                   .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            Assert.assertEquals("login-test", loginPage.getUsername());

            loginPage.setRememberMe(false);
        } finally {
            setRememberMe(false);
        }
    }

    //KEYCLOAK-2741
    @Test
    public void loginAgainWithoutRememberMe() {
        setRememberMe(true);

        try {
            //login with remember me
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login-test", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
            EventRepresentation loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login-test")
                                                   .detail(Details.REMEMBER_ME, "true")
                                                   .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            Assert.assertEquals("login-test", loginPage.getUsername());

            //login without remember me
            loginPage.setRememberMe(false);
            loginPage.login("login-test", "password");
            
            // Expire session
            loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login-test")
                                                   .assertEvent();
            sessionId = loginEvent.getSessionId();
            testingClient.testing().removeUserSession("test", sessionId);
            
            // Assert rememberMe not checked nor username/email prefilled
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            assertNotEquals("login-test", loginPage.getUsername());
        } finally {
            setRememberMe(false);
        }
    }
    
    @Test
    // KEYCLOAK-3181
    public void loginWithEmailUserAndRememberMe() {
        setRememberMe(true);

        try {
            loginPage.open();
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("login@test.com", "password");

            Assert.assertEquals(RequestType.AUTH_RESPONSE, appPage.getRequestType());
            Assert.assertNotNull(oauth.getCurrentQuery().get(OAuth2Constants.CODE));
            EventRepresentation loginEvent = events.expectLogin().user(userId)
                                                   .detail(Details.USERNAME, "login@test.com")
                                                   .detail(Details.REMEMBER_ME, "true")
                                                   .assertEvent();
            String sessionId = loginEvent.getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            
            Assert.assertEquals("login@test.com", loginPage.getUsername());

            loginPage.setRememberMe(false);
        } finally {
            setRememberMe(false);
        }
    }


    // Login timeout scenarios

    // KEYCLOAK-1037
    @Test
    public void loginExpiredCode() {
        loginPage.open();
        setTimeOffset(5000);
        // No explicitly call "removeExpired". Hence authSession will still exists, but will be expired
        //testingClient.testing().removeExpired("test");

        loginPage.login("login@test.com", "password");
        loginPage.assertCurrent();

        Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());
        setTimeOffset(0);

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .assertEvent();
    }

    // KEYCLOAK-1037
    @Test
    public void loginExpiredCodeWithExplicitRemoveExpired() {
        loginPage.open();
        setTimeOffset(5000);
        // Explicitly call "removeExpired". Hence authSession won't exist, but will be restarted from the KC_RESTART
        testingClient.testing().removeExpired("test");

        loginPage.login("login@test.com", "password");

        //loginPage.assertCurrent();
        loginPage.assertCurrent();

        Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());
        setTimeOffset(0);

        events.expectLogin().user((String) null).session((String) null).error(Errors.EXPIRED_CODE).clearDetails()
                .detail(Details.RESTART_AFTER_TIMEOUT, "true")
                .client((String) null)
                .assertEvent();
    }

    @Test
    public void loginAfterExpiredTimeout() throws Exception {
        try (AutoCloseable c = new RealmAttributeUpdater(adminClient.realm("test"))
                .updateWith(r -> {
                    r.setSsoSessionMaxLifespan(5);
                })
                .update()) {

            DroneUtils.addWebDriver(jsDriver);

            jsAdminConsole.setAdminRealm(testRealm().toRepresentation().getRealm());

            jsAdminConsole.navigateTo();
            assertCurrentUrlStartsWithLoginUrlOf(jsAdminConsole);

            // login for the first time
            jsLoginPage.login("admin", "admin");

            // wait for a timeout
            TimeUnit.SECONDS.sleep(5);
            Retry.execute(() -> jsLoginPage.assertCurrent(), 20, 500);

            // try to re-login immediately, it should be successful i.e without "You took too long to login. Login process starting from beginning." message
            jsLoginPage.login("admin", "admin");

            assertFalse(jsLoginPage.isCurrent());
        }
    }


    @Test
    public void loginExpiredCodeAndExpiredCookies() {
        loginPage.open();

        driver.manage().deleteAllCookies();

        // Cookies are expired including KC_RESTART. No way to continue login. Error page must be shown with the "back to application" link
        loginPage.login("login@test.com", "password");
        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();

        ClientRepresentation thirdParty = findClientByClientId(adminClient.realm("test"), "third-party").toRepresentation();
        Assert.assertNotNull(link, thirdParty.getBaseUrl());
    }



    @Test
    public void openLoginFormWithDifferentApplication() throws Exception {
        oauth.clientId("root-url-client");
        oauth.redirectUri("http://localhost:8180/foo/bar/");
        oauth.openLoginForm();

        // Login form shown after redirect from app
        oauth.clientId("test-app");
        oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
        oauth.openLoginForm();

        assertTrue(loginPage.isCurrent());
        loginPage.login("test-user@localhost", "password");
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void openLoginFormAfterExpiredCode() throws Exception {
        oauth.openLoginForm();

        setTimeOffset(5000);

        oauth.openLoginForm();

        loginPage.assertCurrent();
        try {
            String loginError = loginPage.getError();
            Assert.fail("Not expected to have error on loginForm. Error is: " + loginError);
        } catch (NoSuchElementException nsee) {
            // Expected
        }

        loginPage.login("test-user@localhost", "password");
        appPage.assertCurrent();

        events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
    }

    @Test
    public void loginRememberMeExpiredIdle() throws Exception {
        setRememberMe(true, 1, null);

        try {
            // login form shown after redirect from app
            oauth.clientId("test-app");
            oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());
            loginPage.setRememberMe(true);
            loginPage.login("test-user@localhost", "password");

            // sucessful login - app page should be on display.
            events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
            appPage.assertCurrent();

            // expire idle timeout using the timeout window.
            setTimeOffset(2 + SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS);

            // trying to open the account page with an expired idle timeout should redirect back to the login page.
            appPage.openAccount();
            loginPage.assertCurrent();
        } finally {
            setRememberMe(false);
        }
    }

    @Test
    public void loginRememberMeExpiredMaxLifespan() throws Exception {
        setRememberMe(true, null, 1);

        try {
            // login form shown after redirect from app
            oauth.clientId("test-app");
            oauth.redirectUri(OAuthClient.APP_ROOT + "/auth");
            oauth.openLoginForm();

            assertTrue(loginPage.isCurrent());
            loginPage.setRememberMe(true);
            loginPage.login("test-user@localhost", "password");

            // sucessful login - app page should be on display.
            events.expectLogin().detail(Details.USERNAME, "test-user@localhost").assertEvent();
            appPage.assertCurrent();

            // expire the max lifespan.
            setTimeOffset(2);

            // trying to open the account page with an expired lifespan should redirect back to the login page.
            appPage.openAccount();
            loginPage.assertCurrent();
        } finally {
            setRememberMe(false);
        }
    }

}
