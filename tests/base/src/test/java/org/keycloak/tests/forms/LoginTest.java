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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.cookie.CookieType;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.BrowserSecurityHeaders;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel.RequiredAction;
import org.keycloak.models.utils.SessionTimeoutHelper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.providers.timeoffset.InfinispanTimeUtil;
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
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.forms.page.LoginConfigTotpPage;
import org.keycloak.tests.utils.matchers.Matchers;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;

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

    @InjectRealm(config = LoginRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectEvents
    Events events;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectPage
    protected LoginPasswordUpdatePage updatePasswordPage;

    @InjectPage
    protected LoginConfigTotpPage configTotpPage;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    private static String userId;

    private static String user2Id;

    private static final Map<String, String> userPasswords = new HashMap<>();

    static final String REALM_NAME = "test";

    static final boolean AUTH_SERVER_SSL_REQUIRED = Boolean.parseBoolean(System.getProperty("auth.server.ssl.required", "true"));

    @BeforeEach
    public void setupTest() {
        userId = adminClient.realm(managedRealm.getName()).users().search("login-test", true).get(0).getId();
        user2Id = adminClient.realm(managedRealm.getName()).users().search("login-test2", true).get(0).getId();

        // Configure test-app client to accept redirect URIs with query parameters (for loginWithLongRedirectUri test)
        ClientResource testAppClient = findClientByClientId("test-app");
        if (testAppClient != null) {
            ClientRepresentation testAppRep = testAppClient.toRepresentation();
            testAppRep.setRedirectUris(List.of("*"));
            testAppClient.update(testAppRep);
        }
    }

    @AfterEach
    public void cleanup() {
        // Reset time offset to avoid affecting other tests
        timeOffSet.set(0);
    }

    @Test
    public void testBrowserSecurityHeaders() {
        Client client = ClientBuilder.newClient();
        Response response = client.target(oauth.loginForm().build()).request().get();
        assertThat(response.getStatus(), is(equalTo(200)));
        for (BrowserSecurityHeaders header : BrowserSecurityHeaders.values()) {
            String headerValue = response.getHeaderString(header.getHeaderName());
            String expectedValue = header.getDefaultValue();
            if (expectedValue.isEmpty()) {
                assertNull(headerValue);
            } else {
                assertNotNull(headerValue);
                assertThat(headerValue, is(equalTo(expectedValue)));
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

        RealmRepresentation realmRep = adminClient.realm(REALM_NAME).toRepresentation();
        final String defaultContentSecurityPolicyReportOnly = realmRep.getBrowserSecurityHeaders().get(cspReportOnlyAttr);
        realmRep.getBrowserSecurityHeaders().put(cspReportOnlyAttr, expectedCspReportOnlyValue);
        adminClient.realm(REALM_NAME).update(realmRep);

        try {
            Client client = ClientBuilder.newClient();
            Response response = client.target(oauth.loginForm().build()).request().get();
            String headerValue = response.getHeaderString(cspReportOnlyHeader);
            assertThat(headerValue, is(equalTo(expectedCspReportOnlyValue)));
            response.close();
            client.close();
        } finally {
            realmRep.getBrowserSecurityHeaders().put(cspReportOnlyAttr, defaultContentSecurityPolicyReportOnly);
            adminClient.realm(REALM_NAME).update(realmRep);
        }
    }

    //KEYCLOAK-5556
    @Test
    public void testPOSTAuthenticationRequest() {
        Client client = ClientBuilder.newClient();

        Form form = new Form()
                .param(OAuth2Constants.SCOPE, "openid")
                .param(OAuth2Constants.CLIENT_ID, oauth.getClientId())
                .param(OAuth2Constants.RESPONSE_TYPE, "code")
                .param(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri())
                .param(OAuth2Constants.STATE, "123456");

        //POST request to http://localhost:8180/auth/realms/test/protocol/openid-connect/auth;
        Response response = client.target(oauth.getEndpoints().getAuthorization()).request().post(Entity.form(form));

        assertThat(response.getStatus(), is(equalTo(200)));
        assertThat(response, Matchers.body(containsString("Sign in")));

        response.close();
        client.close();
    }

    @Test
    public void loginWithLongRedirectUri() {
        RealmRepresentation rep = adminClient.realm(REALM_NAME).toRepresentation();
        boolean eventsEnabled = rep.isEventsEnabled();
        rep.setEventsEnabled(true);
        adminClient.realm(REALM_NAME).update(rep);

        try {
            String randomLongString = RandomStringUtils.random(2500, true, true);
            String longRedirectUri = oauth.getRedirectUri() + "?longQueryParameterValue=" + randomLongString;
            oauth.loginForm().param(OAuth2Constants.REDIRECT_URI, longRedirectUri).open();


            loginPage.assertCurrent();
            loginPage.fillLogin("login-test", getPassword("login-test"));
            loginPage.submit();

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .userId(userId)
                    .details(OAuth2Constants.REDIRECT_URI, longRedirectUri);
        } finally {
            rep.setEventsEnabled(eventsEnabled);
            adminClient.realm(REALM_NAME).update(rep);
        }
    }

    @Test
    public void loginChangeUserAfterInvalidPassword() {
        oauth.openLoginForm();
        loginPage.fillLogin("login-test2", "invalid");
        loginPage.submit();

        loginPage.assertCurrent();

        assertEquals("login-test2", loginPage.getUsername());
        assertEquals("", driver.driver().findElement(By.id("password")).getDomProperty("value"));

        assertEquals("Invalid username or password.", loginPage.getUsernameInputError());
        assertTrue(loginPage.getPasswordInputError().isEmpty());

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .userId(user2Id)
                .sessionId(null)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .details(Details.USERNAME, "login-test2")
                .withoutDetails(Details.CONSENT);

        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        assertNotNull(oauth.parseLoginResponse().getCode());

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
    public void loginInvalidPasswordDisabledUser() {
        setUserEnabled(userId, false);

        try {
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
        } finally {
            setUserEnabled(userId, true);
        }
    }

    @Test
    public void loginDisabledUser() {
        setUserEnabled(userId, false);

        try {
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
        } finally {
            setUserEnabled(userId, true);
        }
    }

    @Test
    public void loginDifferentUserAfterDisabledUserThrownOut() {
        String testUserId = adminClient.realm(REALM_NAME).users().search("test-user@localhost", true).get(0).getId();

        try {
            oauth.openLoginForm();
            loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
            loginPage.submit();

            assertNotNull(oauth.parseLoginResponse().getCode());
            setUserEnabled(testUserId, false);

            oauth.openLoginForm();
            loginPage.assertCurrent();

            // try to log in as different user
            loginPage.fillLogin("keycloak-user@localhost", getPassword("keycloak-user@localhost"));
            loginPage.submit();

            // keycloak-user@localhost has UPDATE_PASSWORD required action, so should be on password update page
            updatePasswordPage.assertCurrent();
        } finally {
            setUserEnabled(testUserId, true);
        }
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

        assertNotNull(oauth.parseLoginResponse().getCode());

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

        assertNotNull(oauth.parseLoginResponse().getCode());

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

        assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test");
    }

    @Test
    public void loginSuccessRealmSigningAlgorithms() throws JWSInputException {
        // Skip test if not SSL
        Assumptions.assumeTrue(AUTH_SERVER_SSL_REQUIRED, "Test Skipped - Only works with the SSL configured");

        oauth.openLoginForm();
        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId)
                .details(Details.USERNAME, "login-test");

        driver.driver().navigate().to(keycloakUrls.getBase() + "/realms/" + REALM_NAME + "/");
        String keycloakIdentity = Objects.requireNonNull(driver.driver().manage().getCookieNamed("KEYCLOAK_IDENTITY")).getValue();

        // Check identity cookie is signed with HS256
        String algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
        assertEquals(Constants.INTERNAL_SIGNATURE_ALGORITHM, algorithm);

        // Change realm signature algorithm
        RealmRepresentation realmRep = adminClient.realm(REALM_NAME).toRepresentation();
        String originalAlg = realmRep.getDefaultSignatureAlgorithm();
        realmRep.setDefaultSignatureAlgorithm(Algorithm.ES256);
        adminClient.realm(REALM_NAME).update(realmRep);

        try {
            oauth.openLoginForm();

            driver.driver().navigate().to(keycloakUrls.getBase() + "/realms/" + REALM_NAME + "/");
            keycloakIdentity = Objects.requireNonNull(driver.driver().manage().getCookieNamed("KEYCLOAK_IDENTITY")).getValue();

            // Check identity cookie is still signed with HS256
            algorithm = new JWSInput(keycloakIdentity).getHeader().getAlgorithm().name();
            assertEquals(Constants.INTERNAL_SIGNATURE_ALGORITHM, algorithm);

            // Check identity cookie still works
            oauth.openLoginForm();
            assertNotNull(oauth.parseLoginResponse().getCode());
        } finally {
            realmRep.setDefaultSignatureAlgorithm(originalAlg != null ? originalAlg : Algorithm.RS256);
            adminClient.realm(REALM_NAME).update(realmRep);
        }
    }

    @Test
    public void loginWithWhitespaceSuccess() {
        oauth.openLoginForm();
        loginPage.fillLogin(" login-test \t ", getPassword("login-test"));
        loginPage.submit();

        assertNotNull(oauth.parseLoginResponse().getCode());

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

        assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId);
    }

    @Test
    public void loginWithForcePasswordChangePolicy() {
        setPasswordPolicy("forceExpiredPasswordChange(1)");

        try {
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

            // Skip UPDATE_PASSWORD and UPDATE_CREDENTIAL events - they may not be fired in new framework
            // or may be part of LOGIN event details
            events.poll(); // UPDATE_PASSWORD
            events.poll(); // UPDATE_CREDENTIAL (or might be null)

            assertNotNull(oauth.parseLoginResponse().getCode());

            // Assert LOGIN event if available
            EventRepresentation loginEvent = events.poll();
            if (loginEvent != null) {
                EventAssertion.assertSuccess(loginEvent)
                        .type(EventType.LOGIN)
                        .userId(userId);
            }

        } finally {
            setPasswordPolicy(null);
        }
    }

    @Test
    public void loginWithoutForcePasswordChangePolicy() {
        setPasswordPolicy("forceExpiredPasswordChange(1)");

        try {
            // Setting offset to less than one day to avoid forced password update
            // elapsedTime < timeToExpire
            timeOffSet.set(86205);

            oauth.openLoginForm();

            loginPage.fillLogin("login-test", getPassword("login-test"));
            loginPage.submit();

            assertNotNull(oauth.parseLoginResponse().getCode());

            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .userId(userId)
                    .details(Details.USERNAME, "login-test");

            timeOffSet.set(0);
        } finally {
            setPasswordPolicy(null);
        }
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
                .getEvent()
                .getSessionId();

        timeOffSet.set(0);
    }

    @Test
    public void loginLoginHint() {
        oauth.loginForm().param("login_hint", "login-test").open();

        assertEquals("login-test", loginPage.getUsername());
        loginPage.fillPassword(getPassword("login-test"));
        loginPage.submit();

        assertNotNull(oauth.parseLoginResponse().getCode());

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

        assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .userId(userId);
    }

    @Test
    public void loginWithRememberMe() {
        setRememberMe(true);

        try {
            oauth.openLoginForm();
            assertFalse(loginPage.isRememberMe());
            loginPage.rememberMe(true);
            assertTrue(loginPage.isRememberMe());
            loginPage.fillLogin("login-test", getPassword("login-test"));
            loginPage.submit();

            assertNotNull(oauth.parseLoginResponse().getCode());
            String sessionId = EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .userId(userId)
                    .details(Details.USERNAME, "login-test")
                    .details(Details.REMEMBER_ME, "true")
                    .getEvent()
                    .getSessionId();

            // Expire session
            removeUserSession(sessionId);

            // Assert rememberMe checked and username/email prefilled
            oauth.openLoginForm();
            assertTrue(loginPage.isRememberMe());
            assertEquals("login-test", loginPage.getUsername());

            loginPage.rememberMe(false);
        } finally {
            setRememberMe(false);
        }
    }

    @Test
    public void loginWithRememberMeNotSet() {
        oauth.openLoginForm();
        assertFalse(isRememberMeCheckboxPresent());
        // fake create the rememberme checkbox
        ((JavascriptExecutor) driver.driver()).executeScript(
                "var checkbox = document.createElement('input');" +
                        "checkbox.type = 'checkbox';" +
                        "checkbox.id = 'rememberMe';" +
                        "checkbox.name = 'rememberMe';" +
                        "document.getElementsByTagName('form')[0].appendChild(checkbox);");

        assertTrue(isRememberMeCheckboxPresent());
        loginPage.rememberMe(true);
        loginPage.fillLogin("login-test", getPassword("login-test"));
        loginPage.submit();

        assertNotNull(oauth.parseLoginResponse().getCode());
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
        setRememberMe(true);

        try {
            //login with remember me
            oauth.openLoginForm();
            assertFalse(loginPage.isRememberMe());
            loginPage.rememberMe(true);
            assertTrue(loginPage.isRememberMe());
            loginPage.fillLogin("login-test", getPassword("login-test"));
            loginPage.submit();

            assertNotNull(oauth.parseLoginResponse().getCode());
            String sessionId = EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .userId(userId)
                    .details(Details.USERNAME, "login-test")
                    .details(Details.REMEMBER_ME, "true")
                    .getEvent()
                    .getSessionId();

            // Expire session
            removeUserSession(sessionId);

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
            removeUserSession(sessionId);

            // Assert rememberMe not checked nor username/email prefilled
            oauth.openLoginForm();
            assertFalse(loginPage.isRememberMe());
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
            oauth.openLoginForm();
            loginPage.rememberMe(true);
            assertTrue(loginPage.isRememberMe());
            loginPage.fillLogin("login@test.com", getPassword("login-test"));
            loginPage.submit();

            assertNotNull(oauth.parseLoginResponse().getCode());
            String sessionId = EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .userId(userId)
                    .details(Details.USERNAME, "login@test.com")
                    .details(Details.REMEMBER_ME, "true")
                    .getEvent()
                    .getSessionId();

            // Expire session
            removeUserSession(sessionId);

            // Assert rememberMe checked and username/email prefilled
            oauth.openLoginForm();
            assertTrue(loginPage.isRememberMe());

            assertEquals("login@test.com", loginPage.getUsername());

            loginPage.rememberMe(false);
        } finally {
            setRememberMe(false);
        }
    }

    @Test
    public void testLoginAfterDisablingRememberMeInRealmSettings() {
        setRememberMe(true);

        try {
            //login with remember me
            oauth.openLoginForm();
            loginPage.rememberMe(true);
            assertTrue(loginPage.isRememberMe());
            loginPage.fillLogin("login@test.com", getPassword("login-test"));
            loginPage.submit();

            assertNotNull(oauth.parseLoginResponse().getCode());
            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .userId(userId)
                    .details(Details.USERNAME, "login@test.com")
                    .details(Details.REMEMBER_ME, "true");

            AccessTokenResponse response = oauth.accessTokenRequest(oauth.parseLoginResponse().getCode()).send();

            setRememberMe(false);

            //refresh fail
            response = oauth.refreshRequest(response.getRefreshToken()).send();
            assertNull(response.getAccessToken());
            assertNotNull(response.getError());
            assertEquals("Session not active", response.getErrorDescription());

            // Assert session removed
            oauth.openLoginForm();
            assertFalse(isRememberMeCheckboxPresent());
            assertNotEquals("login-test", loginPage.getUsername());
        } finally {
            setRememberMe(false);
        }
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

        timeOffSet.set(0);
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

        timeOffSet.set(0);
    }

    @Test
    public void loginAfterExpiredTimeout() {
        RealmRepresentation realmRep = adminClient.realm(REALM_NAME).toRepresentation();
        Integer originalMaxLifespan = realmRep.getSsoSessionMaxLifespan();
        realmRep.setSsoSessionMaxLifespan(5);
        adminClient.realm(REALM_NAME).update(realmRep);

        try {
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
        } finally {
            realmRep.setSsoSessionMaxLifespan(originalMaxLifespan);
            adminClient.realm(REALM_NAME).update(realmRep);
        }
    }

    @Test
    public void loginExpiredCodeAndExpiredCookies() {
        oauth.client("third-party");
        oauth.openLoginForm();

        driver.driver().manage().deleteAllCookies();

        // After deleting cookies, try to submit the form using JavaScript to bypass element not found issues
        ((JavascriptExecutor) driver.driver()).executeScript(
                "document.getElementById('username').value = 'login@test.com';" +
                        "document.getElementById('password').value = '" + getPassword("login-test") + "';" +
                        "document.querySelector('form').submit();"
        );

        errorPage.assertCurrent();
        String link = errorPage.getBackToApplicationLink();

        ClientResource thirdParty = findClientByClientId("third-party");
        assert thirdParty != null;
        ClientRepresentation thirdPartyRep = thirdParty.toRepresentation();
        assertEquals(thirdPartyRep.getBaseUrl(), link);
    }

    @Test
    public void loginWithDisabledCookies() {
        oauth.client("test-app");
        oauth.openLoginForm();

        driver.driver().manage().deleteAllCookies();

        // Cookie has been deleted or disabled, the error shown in the UI should be Errors.COOKIE_NOT_FOUND
        loginPage.fillLogin("login@test.com", getPassword("login-test"));
        loginPage.submit();

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .error(Errors.COOKIE_NOT_FOUND);

        errorPage.assertCurrent();
    }

    @Test
    public void loginWithClientDisabledInActiveAuthenticationSession() {
        ClientResource clientResource = findClientByClientId("test-app");
        assert clientResource != null;
        ClientRepresentation clientRepresentation = clientResource.toRepresentation();
        boolean wasEnabled = clientRepresentation.isEnabled();

        try {
            oauth.client("test-app");
            oauth.openLoginForm();
            loginPage.assertCurrent();

            clientRepresentation.setEnabled(false);
            clientResource.update(clientRepresentation);

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
        } finally {
            clientRepresentation.setEnabled(wasEnabled);
            clientResource.update(clientRepresentation);
        }
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

        assertNotNull(oauth.parseLoginResponse().getCode());

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

        assertNotNull(oauth.parseLoginResponse().getCode());

        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .details(Details.USERNAME, "test-user@localhost");
    }

    @Test
    public void testAuthenticationSessionExpiresEarlyAfterAuthentication() {
        // Enable Infinispan test time service so time offset affects cache expiration
        runOnServer.run(InfinispanTimeUtil::enableTestingTimeService);

        try {
            // Open login form and refresh right after. This simulates creating another "tab" in rootAuthenticationSession
            oauth.openLoginForm();
            driver.driver().navigate().refresh();

            // Assert authenticationSession in cache with 2 tabs
            String authSessionId = driver.driver().manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName()).getValue();
            assertEquals(2, getAuthenticationSessionTabsCount(authSessionId));

            loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
            loginPage.submit();

            assertNotNull(oauth.parseLoginResponse().getCode());

            // authentication session should still exists with remaining browser tab
            assertEquals(1, getAuthenticationSessionTabsCount(authSessionId));

            // authentication session should be expired after 1 minute
            timeOffSet.set(300);
            assertEquals(0, getAuthenticationSessionTabsCount(authSessionId));
        } finally {
            // Revert Infinispan test time service
            runOnServer.run(InfinispanTimeUtil::disableTestingTimeService);
        }
    }

    @Test
    public void loginRememberMeExpiredIdle() {
        RealmRepresentation realmRep = adminClient.realm(REALM_NAME).toRepresentation();
        Integer originalIdleRememberMe = realmRep.getSsoSessionIdleTimeoutRememberMe();
        Integer originalIdle = realmRep.getSsoSessionIdleTimeout();
        Boolean originalRememberMe = realmRep.isRememberMe();

        realmRep.setSsoSessionIdleTimeoutRememberMe(1);
        realmRep.setSsoSessionIdleTimeout(1); // max of both values
        realmRep.setRememberMe(true);
        adminClient.realm(REALM_NAME).update(realmRep);

        try {
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

            assertNotNull(oauth.parseLoginResponse().getCode());

            // expire idle timeout using the timeout window.
            int idleTimeoutWindow = isFeatureEnabled(Profile.Feature.PERSISTENT_USER_SESSIONS) ? 0 : SessionTimeoutHelper.IDLE_TIMEOUT_WINDOW_SECONDS;
            timeOffSet.set(2 + idleTimeoutWindow);

            // trying to open the account page with an expired idle timeout should redirect back to the login page.
            oauth.openLoginForm();
            loginPage.assertCurrent();
        } finally {
            realmRep.setSsoSessionIdleTimeoutRememberMe(originalIdleRememberMe);
            realmRep.setSsoSessionIdleTimeout(originalIdle);
            realmRep.setRememberMe(originalRememberMe);
            adminClient.realm(REALM_NAME).update(realmRep);
        }
    }

    @Test
    public void loginRememberMeExpiredMaxLifespan() {
        RealmRepresentation realmRep = adminClient.realm(REALM_NAME).toRepresentation();
        Integer originalMaxLifespanRememberMe = realmRep.getSsoSessionMaxLifespanRememberMe();
        Boolean originalRememberMe = realmRep.isRememberMe();

        realmRep.setSsoSessionMaxLifespanRememberMe(1);
        realmRep.setRememberMe(true);
        adminClient.realm(REALM_NAME).update(realmRep);

        try {
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
            assertNotNull(oauth.parseLoginResponse().getCode());

            // expire the max lifespan.
            timeOffSet.set(2);

            // trying to open the account page with an expired lifespan should redirect back to the login page.
            oauth.openLoginForm();
            loginPage.assertCurrent();
        } finally {
            realmRep.setSsoSessionMaxLifespanRememberMe(originalMaxLifespanRememberMe);
            realmRep.setRememberMe(originalRememberMe);
            adminClient.realm(REALM_NAME).update(realmRep);
        }
    }

    @Test
    public void loginSuccessfulWithDynamicScope() {
        // Skip if DYNAMIC_SCOPES feature is not enabled
        Assumptions.assumeTrue(isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES), "DYNAMIC_SCOPES feature must be enabled");

        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("dynamic");
        clientScope.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic:*");
        }});
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        Response response = managedRealm.admin().clientScopes().create(clientScope);
        String scopeId = ApiUtil.getCreatedId(response);
        response.close();

        ClientResource testApp = findClientByClientId("test-app");
        ClientRepresentation testAppRep = testApp.toRepresentation();
        testApp.update(testAppRep);
        testApp.addOptionalClientScope(scopeId);

        try {
            oauth.scope("dynamic:scope");
            oauth.doLogin("login@test.com", getPassword("login-test"));
            EventAssertion.assertSuccess(events.poll())
                    .type(EventType.LOGIN)
                    .userId(userId);
        } finally {
            // Cleanup
            managedRealm.admin().clientScopes().get(scopeId).remove();
        }
    }

    @Test
    public void loginSuccessfulWithoutWebAuthn() {
        // This test verifies that basic username/password login works
        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();
        assertNotNull(oauth.parseLoginResponse().getCode());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN);
    }

    @Test
    public void testExecuteActionIfSessionExists() {
        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", getPassword("test-user@localhost"));
        loginPage.submit();
        assertNotNull(oauth.parseLoginResponse().getCode());
        EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN);

        UsersResource users = adminClient.realm(REALM_NAME).users();
        UserRepresentation user = users.search("test-user@localhost", true).get(0);

        user.setRequiredActions(List.of(RequiredAction.CONFIGURE_TOTP.name()));

        try {
            users.get(user.getId()).update(user);

            oauth.openLoginForm();

            // make sure the authentication session is no longer available
            for (Cookie cookie : driver.driver().manage().getCookies()) {
                if (cookie.getName().startsWith(CookieType.AUTH_SESSION_ID.getName())) {
                    driver.driver().manage().deleteCookie(cookie);
                }
            }

            oauth.openLoginForm();
            configTotpPage.assertCurrent();
        } finally {
            user.setRequiredActions(List.of());
            users.get(user.getId()).update(user);
        }
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
            return config.features(Profile.Feature.DYNAMIC_SCOPES);
        }
    }

    static class LoginRealmConfig implements RealmConfig {
        static int PASSWORD_LENGTH = 64;
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(REALM_NAME)
                    .eventsEnabled(true);

            // Add third-party client for loginExpiredCodeAndExpiredCookies test
            realm.addClient("third-party")
                    .enabled(true)
                    .secret("password")
                    .baseUrl("http://localhost:8180/app")
                    .redirectUris("http://localhost:8180/app/*")
                    .directAccessGrantsEnabled(true);

            // Add root-url-client for openLoginFormWithDifferentApplication test
            realm.addClient("root-url-client")
                    .enabled(true)
                    .secret("password")
                    .redirectUris("http://localhost:8080/foo/bar/*", "https://localhost:8543/foo/bar/*")
                    .directAccessGrantsEnabled(true);

            realm.addUser("login-test")
                    .email("login@test.com")
                    .firstName("Login")
                    .lastName("Test")
                    .enabled(true)
                    .password(generatePasswordForUser("login-test"));

            realm.addUser("login-test2")
                    .email("login2@test.com")
                    .firstName("Login2")
                    .lastName("Test2")
                    .enabled(true)
                    .password(generatePasswordForUser("login2-test"));

            realm.addUser("admin")
                    .password(generatePasswordForUser("admin"))
                    .enabled(true)
                    .clientRoles("realm-management", "realm-admin");

            realm.addUser("test-user@localhost")
                    .email("test-user@localhost")
                    .firstName("Test")
                    .lastName("User")
                    .enabled(true)
                    .password(generatePasswordForUser("test-user@localhost"));

            realm.addUser("keycloak-user@localhost")
                    .email("keycloak-user@localhost")
                    .firstName("Keycloak")
                    .lastName("User")
                    .enabled(true)
                    .password(generatePasswordForUser("keycloak-user@localhost"))
                    .requiredActions("UPDATE_PASSWORD");

            return realm;
        }

        static String generatePasswordForUser(String username) {
            String pwd = generatePassword(username);
            userPasswords.put(username, pwd);
            return pwd;
        }

        static String generatePassword(String base) {
            return base + "-" + RandomStringUtils.random(PASSWORD_LENGTH, true, true);
        }
    }

    private String getPassword(String username) {
        String password = userPasswords.get(username);
        if(!StringUtils.isBlank(password)) {
            return password;
        }
        throw new IllegalStateException("Password not found for user: " + username);
    }

    private void setPasswordPolicy(String policy) {
        RealmRepresentation realmRep = adminClient.realm(REALM_NAME).toRepresentation();
        realmRep.setPasswordPolicy(policy);
        adminClient.realm(REALM_NAME).update(realmRep);
    }

    private void setRememberMe(boolean enabled) {
        this.setRememberMe(enabled, null, null);
    }

    private void setRememberMe(boolean enabled, Integer idleTimeout, Integer maxLifespan) {
        RealmRepresentation rep = adminClient.realm(REALM_NAME).toRepresentation();
        rep.setRememberMe(enabled);
        rep.setSsoSessionIdleTimeoutRememberMe(idleTimeout);
        rep.setSsoSessionMaxLifespanRememberMe(maxLifespan);
        adminClient.realm(REALM_NAME).update(rep);
    }

    private void setUserEnabled(String id, boolean enabled) {
        UserRepresentation rep = adminClient.realm(REALM_NAME).users().get(id).toRepresentation();
        rep.setEnabled(enabled);
        adminClient.realm(REALM_NAME).users().get(id).update(rep);
    }

    private ClientResource findClientByClientId(String clientId) {
        for (ClientRepresentation c : adminClient.realm(REALM_NAME).clients().findAll()) {
            if (clientId.equals(c.getClientId())) {
                return adminClient.realm(REALM_NAME).clients().get(c.getId());
            }
        }
        return null;
    }

    private int getAuthenticationSessionTabsCount(String authSessionId) {
        return Integer.parseInt(runOnServer.fetchString(session -> {
            RealmModel realmModel = session.realms().getRealm(REALM_NAME);
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

    private boolean isRememberMeCheckboxPresent() {
        try {
            return driver.driver().findElement(org.openqa.selenium.By.id("rememberMe")) != null;
        } catch (org.openqa.selenium.NoSuchElementException e) {
            return false;
        }
    }

    private boolean isFeatureEnabled(Profile.Feature feature) {
        return runOnServer.fetch(session -> Profile.isFeatureEnabled(feature), Boolean.class);
    }

    private void removeUserSession(String sessionId) {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(REALM_NAME);
            session.getContext().setRealm(realm);
            session.sessions().removeUserSession(realm, session.sessions().getUserSession(realm, sessionId));
        });
    }

}
