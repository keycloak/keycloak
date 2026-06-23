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
package org.keycloak.tests.forms;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginConfigTotpPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;

import static org.keycloak.models.utils.SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest(config = LoginTest.DynamicScopeServerConfig.class)
public class LoginTest {

    @InjectRealm(ref = "login-test", config = LoginRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient(realmRef = "login-test")
    OAuthClient oauth;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectEvents(realmRef = "login-test")
    Events events;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectPage
    protected LoginPasswordUpdatePage updatePasswordPage;

    @InjectPage
    protected LoginConfigTotpPage configTotpPage;

    @InjectTimeOffSet(enableForCaches = true)
    TimeOffSet timeOffSet;

    @InjectHttpClient
    CloseableHttpClient client;

    private static String userId;

    private static String user2Id;

    private static final Map<String, String> userPasswords = new HashMap<>();

    @BeforeEach
    public void setupTest() {
        userId = AdminApiUtil.findUserByUsername(managedRealm.admin(), "login-test").getId();
        user2Id = AdminApiUtil.findUserByUsername(managedRealm.admin(), "test-2-login").getId();

        // Configure test-app client to accept redirect URIs with query parameters (for loginWithLongRedirectUri test)
        ClientResource testAppClient = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        if (testAppClient != null) {
            ClientRepresentation testAppRep = testAppClient.toRepresentation();
            testAppRep.setRedirectUris(List.of("*"));
            testAppClient.update(testAppRep);
        }
    }

    @Test
    public void testBrowserSecurityHeaders() throws IOException {
        HttpGet request = new HttpGet(oauth.loginForm().build());
        CloseableHttpResponse response = client.execute(request);

        assertThat(response.getStatusLine().getStatusCode(), is(equalTo(200)));
        for (BrowserSecurityHeaders header : BrowserSecurityHeaders.values()) {
            Header headerValue = response.getFirstHeader(header.getHeaderName());
            String expectedValue = header.getDefaultValue();
            if (expectedValue.isEmpty()) {
                assertNull(headerValue);
            } else {
                assertNotNull(headerValue);
                assertThat(headerValue.getValue(), is(equalTo(expectedValue)));
            }
        }
        response.close();
    }

    @Test
    @DatabaseTest
    public void testContentSecurityPolicyReportOnlyBrowserSecurityHeader() throws IOException {
        final String expectedCspReportOnlyValue = "default-src 'none'";
        final String cspReportOnlyAttr = "contentSecurityPolicyReportOnly";
        final String cspReportOnlyHeader = "Content-Security-Policy-Report-Only";

        managedRealm.updateWithCleanup(realm ->
                realm.update(rep ->
                        rep.getBrowserSecurityHeaders().put(cspReportOnlyAttr, expectedCspReportOnlyValue)
                )
        );

        HttpGet request = new HttpGet(oauth.loginForm().build());
        try(CloseableHttpResponse response = client.execute(request)){
            Header headerValue = response.getFirstHeader(cspReportOnlyHeader);
            assertThat(headerValue.getValue(), is(equalTo(expectedCspReportOnlyValue)));
        }
    }

    //KEYCLOAK-5556
    @Test
    public void testPOSTAuthenticationRequest() throws IOException {
        HttpPost post = new HttpPost(oauth.getEndpoints().getAuthorization());

        List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
        params.add(new BasicNameValuePair(OAuth2Constants.RESPONSE_TYPE, "code"));
        params.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        params.add(new BasicNameValuePair(OAuth2Constants.STATE, "123456"));
        post.setEntity(new UrlEncodedFormEntity(params));

        //POST request to http://localhost:8180/auth/realms/test/protocol/openid-connect/auth;
        CloseableHttpResponse response = client.execute(post);
        assertThat(response.getStatusLine().getStatusCode(), is(equalTo(200)));
        String body = EntityUtils.toString(response.getEntity());
        assertThat(body, containsString("Sign in"));
        response.close();
    }

    @Test
    @DatabaseTest
    public void loginWithLongRedirectUri() {
        managedRealm.updateWithCleanup(realm ->
                realm.update(rep ->
                        rep.setEventsEnabled(true)
        ));

        String randomLongString = RandomStringUtils.secure().next(2500, true, true);
        String longRedirectUri = oauth.getRedirectUri() + "?longQueryParameterValue=" + randomLongString;
        oauth.loginForm().param(OAuth2Constants.REDIRECT_URI, longRedirectUri).open();

        loginPage.assertCurrent();
        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(OAuth2Constants.REDIRECT_URI, longRedirectUri);

    }

    @Test
    public void loginChangeUserAfterInvalidPassword() {
        oauth.openLoginForm();
        loginPage.fillLogin("test-2-login", "invalid");
        loginPage.submit();

        loginPage.assertCurrent();

        assertEquals("test-2-login", loginPage.getUsername());
        assertEquals("", driver.driver().findElement(By.id("password")).getDomProperty("value"));

        assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertTrue(loginPage.getPasswordInputError().isEmpty());

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(user2Id)
                .sessionId(null)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .details(Details.USERNAME, "test-2-login")
                .withoutDetails(Details.CONSENT);

        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginInvalidPassword() {
        oauth.openLoginForm();
        loginPage.fillLogin("login-test", "invalid");
        loginPage.submit();

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        assertEquals("login-test", loginPage.getUsername());
        assertEquals("", driver.driver().findElement(By.id("password")).getDomProperty("value"));

        assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertTrue(loginPage.getPasswordInputError().isEmpty());

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(userId)
                .sessionId(null)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .details(Details.USERNAME, "login-test")
                .withoutDetails(Details.CONSENT);
    }

    @Test
    public void loginMissingPassword() {
        oauth.openLoginForm();
        loginPage.fillLogin("login-test", "");
        loginPage.submit();

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        assertEquals("login-test", loginPage.getUsername());
        assertEquals("", driver.driver().findElement(By.id("password")).getDomProperty("value"));

        assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertTrue(loginPage.getPasswordInputError().isEmpty());

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(userId)
                .sessionId(null)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .details(Details.USERNAME, "login-test")
                .withoutDetails(Details.CONSENT);
    }

    @Test
    @DatabaseTest
    public void loginInvalidPasswordDisabledUser() {
        managedRealm.updateUserWithCleanup("login-test", user -> user.enabled(false));

        oauth.openLoginForm();
        loginPage.fillLogin("login-test", "invalid");
        loginPage.submit();

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        assertEquals("login-test", loginPage.getUsername());
        assertEquals("", driver.driver().findElement(By.id("password")).getDomProperty("value"));

        // KEYCLOAK-2024
        assertEquals("Invalid username or password.", loginPage.getUsernameInputError());

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(userId)
                .sessionId(null)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .details(Details.USERNAME, "login-test")
                .withoutDetails(Details.CONSENT);
    }

    @Test
    @DatabaseTest
    public void loginDisabledUser() {
        managedRealm.updateUserWithCleanup("login-test", user -> user.enabled(false));

        oauth.openLoginForm();
        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        assertEquals("login-test", loginPage.getUsername());
        assertEquals("", driver.driver().findElement(By.id("password")).getDomProperty("value"));

        // KEYCLOAK-2024
        assertEquals("Account is disabled, contact your administrator.", loginPage.getErrorMessage().orElse(null));

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(userId)
                .sessionId(null)
                .error(Errors.USER_DISABLED)
                .details(Details.USERNAME, "login-test")
                .withoutDetails(Details.CONSENT);

    }

    @Test
    @DatabaseTest
    public void loginDifferentUserAfterDisabledUserThrownOut() {
        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());
        managedRealm.updateUserWithCleanup("test-user@localhost", user -> user.enabled(false));

        oauth.openLoginForm();
        loginPage.assertCurrent();

        // try to log in as different user
        loginPage.fillLogin("keycloak-user@localhost", getPassword("keycloak-user@localhost"));
        loginPage.submit();

        // keycloak-user@localhost has UPDATE_PASSWORD required action, so should be on password update page
        updatePasswordPage.assertCurrent();
    }

    @Test
    public void loginInvalidUsername() {
        oauth.openLoginForm();
        loginPage.fillLogin("invalid", "invalid");
        loginPage.submit();

        loginPage.assertCurrent();

        // KEYCLOAK-1741 - assert form field values kept
        assertEquals("invalid", loginPage.getUsername());
        assertEquals("", driver.driver().findElement(By.id("password")).getDomProperty("value"));

        assertEquals("Invalid username or password.", loginPage.getUsernameInputError());

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(null)
                .sessionId(null)
                .error(Errors.USER_NOT_FOUND)
                .details(Details.USERNAME, "invalid")
                .withoutDetails(Details.CONSENT);

        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginMissingUsername() {
        oauth.openLoginForm();
        loginPage.fillLogin("", "");
        loginPage.submit();

        loginPage.assertCurrent();

        assertEquals("Invalid username or password.", loginPage.getUsernameInputError());

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(null)
                .sessionId(null)
                .error(Errors.USER_NOT_FOUND)
                .withoutDetails(Details.CONSENT);
    }

    @Test
    // KEYCLOAK-2557
    public void loginUserWithEmailAsUsername() {
        oauth.openLoginForm();
        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login@test.com");
    }

    @Test
    public void loginSuccess() {
        oauth.openLoginForm();
        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginWithWhitespaceSuccess() {
        oauth.openLoginForm();
        loginPage.fillLogin(" login-test \t ", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginWithEmailWhitespaceSuccess() {
        oauth.openLoginForm();
        loginPage.fillLogin("    login@test.com    ", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId);
    }

    @Test
    @DatabaseTest
    public void loginWithForcePasswordChangePolicy() {
        managedRealm.updateWithCleanup(realm -> realm.passwordPolicy("forceExpiredPasswordChange(1)"));

        // Setting offset to more than one day to force password update
        // elapsedTime > timeToExpire
        timeOffSet.set(86405);

        oauth.openLoginForm();

        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        updatePasswordPage.assertCurrent();

        final String newPwd = LoginRealmConfig.generatePassword("login-test");
        updatePasswordPage.changePassword(newPwd, newPwd);

        timeOffSet.set(0);

        events.poll(); // UPDATE_CREDENTIAL (or might be null)

        assertTrue(oauth.parseLoginResponse().isSuccess());

        // Assert LOGIN event if available
        EventRepresentation loginEvent = events.poll();
        if (loginEvent != null) {
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .userId(userId);
        }
    }

    @Test
    @DatabaseTest
    public void loginWithoutForcePasswordChangePolicy() {
         managedRealm.updateWithCleanup(realm -> realm.passwordPolicy("forceExpiredPasswordChange(1)"));

         // Setting offset to less than one day to avoid forced password update
         // elapsedTime < timeToExpire
         timeOffSet.set(86205);

         oauth.openLoginForm();

         loginPage.fillLogin("login-test", getPassword("login-test"));
         loginPage.submit();

         assertTrue(oauth.parseLoginResponse().isSuccess());

         EventAssertion.assertSuccess(events.poll())
                 .type(EventType.LOGIN)
                 .userId(userId)
                 .details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginNoTimeoutWithLongWait() {
        oauth.openLoginForm();

        timeOffSet.set(1700);

        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test")
                .getEvent();
    }

    @Test
    public void loginLoginHint() {
        oauth.loginForm().param("login_hint", "login-test").open();

        assertEquals("login-test", loginPage.getUsername());
        loginPage.fillPassword(getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginWithEmailSuccess() {
        oauth.openLoginForm();
        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId);
    }

    @Test
    @DatabaseTest
    public void loginWithRememberMe() {
        managedRealm.updateWithCleanup(realm -> realm.setRememberMe(true));

        oauth.openLoginForm();
        assertFalse(loginPage.isRememberMe());
        loginPage.rememberMe(true);
        assertTrue(loginPage.isRememberMe());
        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());
        String sessionId = EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test")
                .details(Details.REMEMBER_ME, "true")
                .getEvent()
                .getSessionId();

        // Expire session
        managedRealm.admin().deleteSession(sessionId, false);

        // Assert rememberMe checked and username/email prefilled
        oauth.openLoginForm();
        assertTrue(loginPage.isRememberMe());
        assertEquals("login-test", loginPage.getUsername());

        loginPage.rememberMe(false);
    }

    @Test
    public void loginWithRememberMeNotSet() {
        oauth.openLoginForm();
        assertFalse(loginPage.isRememberMeCheckboxPresent());
        // fake create the rememberme checkbox
        ((JavascriptExecutor) driver.driver()).executeScript(
                "var checkbox = document.createElement('input');" +
                        "checkbox.type = 'checkbox';" +
                        "checkbox.id = 'rememberMe';" +
                        "checkbox.name = 'rememberMe';" +
                        "document.getElementsByTagName('form')[0].appendChild(checkbox);");

        assertTrue(loginPage.isRememberMeCheckboxPresent());
        loginPage.rememberMe(true);
        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test");
        // check remember me is not set although it was sent in the form data
        assertNull(loginEvent.getDetails().get(Details.REMEMBER_ME));
    }

    //KEYCLOAK-2741
    @Test
    public void loginAgainWithoutRememberMe() {
        managedRealm.updateWithCleanup(realm -> realm.setRememberMe(true));

        //login with remember me
        oauth.openLoginForm();
        assertFalse(loginPage.isRememberMe());
        loginPage.rememberMe(true);
        assertTrue(loginPage.isRememberMe());
        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());
        String sessionId = EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test")
                .details(Details.REMEMBER_ME, "true")
                .getEvent()
                .getSessionId();

        // Expire session
        managedRealm.admin().deleteSession(sessionId, false);

        // Assert rememberMe checked and username/email prefilled
        oauth.openLoginForm();
        assertTrue(loginPage.isRememberMe());
        assertEquals("login-test", loginPage.getUsername());

        //login without remember me
        loginPage.rememberMe(false);
        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        // Expire session
        sessionId = EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test")
                .getEvent()
                .getSessionId();
        managedRealm.admin().deleteSession(sessionId, false);

        // Assert rememberMe not checked nor username/email prefilled
        oauth.openLoginForm();
        assertFalse(loginPage.isRememberMe());
        assertNotEquals("login-test", loginPage.getUsername());
    }

    @Test
    // KEYCLOAK-3181
    public void loginWithEmailUserAndRememberMe() {
        managedRealm.updateWithCleanup(realm -> realm.setRememberMe(true));

        oauth.openLoginForm();
        loginPage.rememberMe(true);
        assertTrue(loginPage.isRememberMe());
        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());
        String sessionId = EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login@test.com")
                .details(Details.REMEMBER_ME, "true")
                .getEvent()
                .getSessionId();

        // Expire session
        managedRealm.admin().deleteSession(sessionId, false);

        // Assert rememberMe checked and username/email prefilled
        oauth.openLoginForm();
        assertTrue(loginPage.isRememberMe());

        assertEquals("login@test.com", loginPage.getUsername());

        loginPage.rememberMe(false);
    }

    @Test
    @DatabaseTest
    public void testLoginAfterDisablingRememberMeInRealmSettings() {
        managedRealm.updateWithCleanup(realm ->  realm.setRememberMe(true));

        //login with remember me
        oauth.openLoginForm();
        loginPage.rememberMe(true);
        assertTrue(loginPage.isRememberMe());
        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login@test.com")
                .details(Details.REMEMBER_ME, "true");

        AccessTokenResponse response = oauth.accessTokenRequest(oauth.parseLoginResponse().getCode()).send();

        managedRealm.updateWithCleanup(realm -> realm.setRememberMe(false));

        //refresh fail
        response = oauth.refreshRequest(response.getRefreshToken()).send();
        assertNull(response.getAccessToken());
        assertNotNull(response.getError());
        assertEquals("Session not active", response.getErrorDescription());

        // Assert session removed
        oauth.openLoginForm();
        assertFalse(loginPage.isRememberMeCheckboxPresent());
        assertNotEquals("login-test", loginPage.getUsername());
    }

    // Login timeout scenarios
    // KEYCLOAK-1037
    @Test
    public void loginExpiredCode() {
        oauth.openLoginForm();
        // authSession expired and removed from the storage
        timeOffSet.set(5000);

        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();
        loginPage.assertCurrent();

        assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getErrorMessage().orElse(null));

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(null)
                .sessionId(null)
                .error(Errors.EXPIRED_CODE);
    }

    // KEYCLOAK-1037
    @Test
    public void loginExpiredCodeWithExplicitRemoveExpired() {
        oauth.openLoginForm();
        timeOffSet.set(5000);

        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();

        loginPage.assertCurrent();

        assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getErrorMessage().orElse(null));

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(null)
                .sessionId(null)
                .error(Errors.EXPIRED_CODE);
    }

    @Test
    @DatabaseTest
    public void loginAfterExpiredTimeout() {
        managedRealm.updateWithCleanup(realm -> realm.ssoSessionMaxLifespan(5));

        oauth.openLoginForm();
        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId);

        // wait for a timeout
        timeOffSet.set(6);

        oauth.openLoginForm();
        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId);
    }

    @Test
    public void loginExpiredCodeAndExpiredCookies() {
        oauth.client("third-party");
        oauth.openLoginForm();

        driver.cookies().deleteAll();
        // After deleting cookies, try to submit the form using JavaScript to bypass element not found issues
        ((JavascriptExecutor) driver.driver()).executeScript(
                "document.getElementById('username').value = 'login@test.com';" +
                        "document.getElementById('password').value = '" + getPassword("login-test") + "';" +
                        "document.querySelector('form').submit();"
        );

        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();

        ClientResource thirdParty = AdminApiUtil.findClientByClientId(managedRealm.admin(), "third-party");
        assert thirdParty != null;
        ClientRepresentation thirdPartyRep = thirdParty.toRepresentation();
        assertEquals(thirdPartyRep.getBaseUrl(), link);
    }

    @Test
    public void loginWithDisabledCookies() {
        oauth.client("test-app");
        oauth.openLoginForm();

        driver.cookies().deleteAll();

        // Cookie has been deleted or disabled, the error shown in the UI should be Errors.COOKIE_NOT_FOUND
        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .error(Errors.COOKIE_NOT_FOUND);

        errorPage.assertCurrent();
    }

    @Test
    @DatabaseTest
    public void loginWithClientDisabledInActiveAuthenticationSession() {        
        oauth.client("test-app");
        oauth.openLoginForm();
        loginPage.assertCurrent();

        managedRealm.updateClientWithCleanup("test-app", client -> client.enabled(false));
        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();

        errorPage.assertCurrent();
        assertEquals("Login requester not enabled", errorPage.getError());
        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .clientId("test-app")
                .userId(null)
                .sessionId(null)
                .error(Errors.CLIENT_DISABLED);
    }

    @Test
    public void openLoginFormWithDifferentApplication() {
        String serverRoot = keycloakUrls.getBase();
        oauth.client("root-url-client");
        oauth.redirectUri(serverRoot + "/foo/bar/");
        oauth.openLoginForm();

        // Login form shown after redirect from app
        oauth.client("test-app");
        oauth.redirectUri(oauth.getRedirectUri());
        oauth.openLoginForm();

        loginPage.assertCurrent();
        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .details(Details.USERNAME, "test-user@localhost");
    }

    @Test
    public void openLoginFormAfterExpiredCode() {
        oauth.openLoginForm();

        timeOffSet.set(5000);

        oauth.openLoginForm();

        loginPage.assertCurrent();
        assertNull(loginPage.getErrorMessage().orElse(null), "Not expected to have error on loginForm.");

        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .details(Details.USERNAME, "test-user@localhost");
    }

    @Test
    public void testAuthenticationSessionExpiresEarlyAfterAuthentication() {
        // Open login form and refresh right after. This simulates creating another "tab" in rootAuthenticationSession
        oauth.openLoginForm();
        driver.driver().navigate().refresh();

        // Assert authenticationSession in cache with 2 tabs
        String authSessionId = driver.driver().manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName()).getValue();
        assertEquals(2, getAuthenticationSessionTabsCount(authSessionId));

        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();

        assertTrue(oauth.parseLoginResponse().isSuccess());

        // authentication session should still exists with remaining browser tab
        assertEquals(1, getAuthenticationSessionTabsCount(authSessionId));

        // authentication session should be expired after 1 minute
        timeOffSet.set(300);
        assertEquals(0, getAuthenticationSessionTabsCount(authSessionId));
    }

    @Test
    public void loginRememberMeExpiredIdle() {
        managedRealm.updateWithCleanup(realm -> {
            realm.setRememberMe(true);
            realm.ssoSessionIdleTimeoutRememberMe(1);
            realm.ssoSessionIdleTimeout(1);
            return realm;
        });

        // login form shown after redirect from app
        oauth.client("test-app");
        oauth.redirectUri(oauth.getRedirectUri());
        oauth.openLoginForm();

        loginPage.assertCurrent();
        loginPage.rememberMe(true);
        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();

        // successful login - app page should be on display.
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .details(Details.USERNAME, "test-user@localhost");

        assertTrue(oauth.parseLoginResponse().isSuccess());

        // expire idle timeout using the timeout window.
        timeOffSet.set(2 + IDLE_TIMEOUT_WINDOW_SECONDS);

        // trying to open the account page with an expired idle timeout should redirect back to the login page.
        oauth.openLoginForm();
        loginPage.assertCurrent();
    }

    @Test
    @DatabaseTest
    public void loginRememberMeExpiredMaxLifespan() {
        managedRealm.updateWithCleanup(realm -> {
            realm.ssoSessionMaxLifespanRememberMe(1);
            realm.setRememberMe(true);
            return realm;
        });

        // login form shown after redirect from app
        oauth.client("test-app");
        oauth.redirectUri(oauth.getRedirectUri());
        oauth.openLoginForm();

        loginPage.assertCurrent();
        loginPage.rememberMe(true);
        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();

        // successful login - app page should be on display.
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .details(Details.USERNAME, "test-user@localhost");
        assertTrue(oauth.parseLoginResponse().isSuccess());

        // expire the max lifespan.
        timeOffSet.set(2);

        // trying to open the account page with an expired lifespan should redirect back to the login page.
        oauth.openLoginForm();
        loginPage.assertCurrent();
    }

    @Test
    @DatabaseTest
    public void loginSuccessfulWithDynamicScope() {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("dynamic");
        clientScope.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_PARAMETERIZED_SCOPE, "true");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_REGEXP, "dynamic:*");
            put(ClientScopeModel.PARAMETERIZED_SCOPE_TYPE, "string");
        }});
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = managedRealm.admin().clientScopes().create(clientScope);
        String scopeId = ApiUtil.getCreatedId(response);
        response.close();

        managedRealm.cleanup().add(realm -> realm.clientScopes().get(scopeId).remove());

        ClientResource testApp = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        oauth.scope("dynamic:scope");
        oauth.doLogin("login@test.com", getPassword("login-test"));
        EventAssertion.assertSuccess(events.poll())
                       .type(EventType.LOGIN)
                       .userId(userId);
    }

    @Test
    public void loginSuccessfulWithoutWebAuthn() {
        // This test verifies that basic username/password login works
        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN);
    }

    @Test
    @DatabaseTest
    public void testExecuteActionIfSessionExists() {
        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();
        assertTrue(oauth.parseLoginResponse().isSuccess());
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

        managedRealm.updateUserWithCleanup("test-user@localhost", user1 -> user1.requiredActions(RequiredAction.CONFIGURE_TOTP.name()));
        oauth.openLoginForm();

        // make sure the authentication session is no longer available
        for (Cookie cookie : driver.driver().manage().getCookies()) {
            if (cookie.getName().startsWith(CookieType.AUTH_SESSION_ID.getName())) {
                driver.driver().manage().deleteCookie(cookie);
            }
        }

        oauth.openLoginForm();
        configTotpPage.assertCurrent();
    }

    @Test
    public void testAuthSessionIdCookieFormat(){
        oauth.openLoginForm();
        String encodedBase64AuthSessionId = driver.driver().manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName()).getValue();
        String decodedAuthSessionId = new String(Base64Url.decode(encodedBase64AuthSessionId), StandardCharsets.UTF_8);
        assertTrue(decodedAuthSessionId.contains("."));
        String authSessionId = decodedAuthSessionId.substring(0, decodedAuthSessionId.indexOf("."));
        String signature = decodedAuthSessionId.substring(decodedAuthSessionId.indexOf(".") + 1);
        assertNotNull(authSessionId);
        // Validate session ID format
        assertTrue(authSessionId.length() >= 24);
        assertNotNull(signature);

        runOnServer.run(session-> assertNotNull(session.authenticationSessions().getRootAuthenticationSession(session.getContext().getRealm(), authSessionId)));
    }

    static class DynamicScopeServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES);
        }
    }

    public static class LoginRealmConfig implements RealmConfig {
        static int PASSWORD_LENGTH = 64;
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("test")
                    .eventsEnabled(true);


            // Add third-party client for loginExpiredCodeAndExpiredCookies test
            realm.clients(
                    ClientBuilder.create("third-party")
                                            .enabled(true)
                                            .secret("password")
                                            .baseUrl("http://localhost:8180/app")
                                            .redirectUris("http://localhost:8180/app/*")
                                            .directAccessGrantsEnabled(true),

                    // Add root-url-client for openLoginFormWithDifferentApplication test
                    ClientBuilder.create("root-url-client")
                                             .enabled(true)
                                             .secret("password")
                                             .redirectUris("http://localhost:8080/foo/bar/*", "https://localhost:8443/foo/bar/*", "https://localhost:8543/foo/bar/*")
                                             .directAccessGrantsEnabled(true)
            );

            realm.users(
                         UserBuilder.create("login-test")
                                    .email("login@test.com")
                                    .firstName("Login")
                                    .lastName("Test")
                                    .enabled(true)
                                    .password(generatePasswordForUser("login-test")),

                         UserBuilder.create("test-2-login")
                                   .email("login2@test.com")
                                   .firstName("Login2")
                                   .lastName("Test2")
                                   .enabled(true)
                                   .password(generatePasswordForUser("login2-test")),

                        UserBuilder.create("admin")
                                   .password(generatePasswordForUser("admin"))
                                   .enabled(true)
                                   .clientRoles("realm-management", "realm-admin"),

                        UserBuilder.create("test-user@localhost")
                                   .email("test-user@localhost")
                                   .firstName("Test")
                                   .lastName("User")
                                   .enabled(true)
                                   .password(generatePasswordForUser("test-user@localhost")),

                        UserBuilder.create("keycloak-user@localhost")
                                   .email("keycloak-user@localhost")
                                   .firstName("Keycloak")
                                   .lastName("User")
                                   .enabled(true)
                                   .password(generatePasswordForUser("keycloak-user@localhost"))
                                   .requiredActions("UPDATE_PASSWORD")
                    );
            return realm;
        }

        static String generatePasswordForUser(String username) {
            String pwd = generatePassword(username);
            userPasswords.put(username, pwd);
            return pwd;
        }

        static String generatePassword(String base) {
            return base + "-" + RandomStringUtils.secure().next(PASSWORD_LENGTH, true, true);
        }
    }

    private String getPassword(String username) {
        String password = userPasswords.get(username);
        if(!StringUtils.isBlank(password)) {
            return password;
        }
        throw new IllegalStateException("Password not found for user: " + username);
    }

    private int getAuthenticationSessionTabsCount(String authSessionId) {
        return Integer.parseInt(runOnServer.fetchString(session -> {
            RealmModel realmModel = session.realms().getRealm("test");
            session.getContext().setRealm(realmModel);

            AuthenticationSessionManager authenticationSessionManager = new AuthenticationSessionManager(session);
            String decodedBase64AndValidateSignature = authenticationSessionManager.decodeBase64AndValidateSignature(authSessionId);
            RootAuthenticationSessionModel rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(realmModel, decodedBase64AndValidateSignature);
            if (rootAuthSession == null) {
                return 0;
            }
            return rootAuthSession.getAuthenticationSessions().size();
        }));
    }
}
