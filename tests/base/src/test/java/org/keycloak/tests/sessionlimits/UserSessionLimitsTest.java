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
package org.keycloak.tests.sessionlimits;

import java.util.List;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.MailUtils;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.ERROR_TO_DISPLAY;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertClientSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.configureSessionLimits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class UserSessionLimitsTest {

    @InjectRealm(config = UserSessionLimitsRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectEvents
    protected Events events;

    @InjectMailServer
    MailServer mailServer;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectPage
    protected LoginPasswordResetPage resetPasswordPage;

    @InjectPage
    protected LoginPasswordUpdatePage updatePasswordPage;
    
    private static String realmName = "test";
    private static final String username = "test-user@localhost";
    private static final String password = "password";
    private static final String directGrant1 = "direct-grant-1";
    private static final String directGrant2 = "direct-grant-2";
    private static String defaultRedirectUri = null;

    @BeforeEach
    public void setup() {
        // Clear browser cookies
        deleteAllCookiesForRealm(driver);

        // Clear server-side user sessions
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeUserSessions(realm);

             // Reset password
            org.keycloak.models.UserModel user = session.users().getUserByUsername(realm, username);
            user.credentialManager().updateCredential(UserCredentialModel.password(password));
        });

        if (defaultRedirectUri == null) {
            defaultRedirectUri = oauth.getRedirectUri();
        }
        oauth.redirectUri(defaultRedirectUri);

        events.clear();
    }

    @TestSetup
    public void setUpFlows() {

        // Configure auto-generated account client and update
        ClientRepresentation accountClient = managedRealm.admin().clients()
                .findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0);
        accountClient.setSecret(password);
        accountClient.setDirectAccessGrantsEnabled(true);
        accountClient.setRedirectUris(List.of("*"));
        managedRealm.admin().clients().get(accountClient.getId()).update(accountClient);

        runOnServer.run(session -> {

            RealmModel realm = session.getContext().getRealm();

            AuthenticationFlowModel browser = realm.getBrowserFlow();
            configureUsernamePassword(realm, browser);
            configureSessionLimits(realm, browser, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");

            AuthenticationFlowModel directGrant = realm.getDirectGrantFlow();
            configureSessionLimits(realm, directGrant, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");

            AuthenticationFlowModel resetPasswordFlow = realm.getResetCredentialsFlow();
            configureSessionLimits(realm, resetPasswordFlow, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");
       });
    }

    private static void configureUsernamePassword(RealmModel realm, AuthenticationFlowModel flow) {
        AuthenticationExecutionModel execution = new AuthenticationExecutionModel();
        execution.setParentFlow(flow.getId());
        execution.setRequirement(AuthenticationExecutionModel.Requirement.REQUIRED);
        execution.setAuthenticator(UsernamePasswordFormFactory.PROVIDER_ID);
        execution.setPriority(20);
        execution.setAuthenticatorFlow(false);
        realm.addAuthenticatorExecution(execution);
    }

    @Test
    public void testClientSessionCountExceededAndNewSessionDeniedBrowserFlow() throws Exception {
        // Login and verify login was successful
        oauth.openLoginForm();
        loginPage.fillLogin(username, password);
        loginPage.submit();
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
        runOnServer.run(assertSessionCount(realmName, username, 1));

        // Delete the cookies, while maintaining the server side session active
        deleteAllCookiesForRealm(driver);

        // Login the same user again and verify the configured error message is shown
        oauth.openLoginForm();
        loginPage.fillLogin(username, password);
        loginPage.submit();
        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.LOGIN_ERROR)
                .userId(null)
                .error(Errors.GENERIC_AUTHENTICATION_ERROR);
        errorPage.assertCurrent();
        assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
    }

    @Test
    public void testClientSessionCountNotExceededOnReAuthentication() throws Exception {
        // Login and verify login was successful
        oauth.openLoginForm();
        loginPage.fillLogin(username, password);
        loginPage.submit();
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

        // Re-authenticate the user with prompt=login
        oauth.loginForm().prompt("login").open();
        loginPage.fillLogin(username, password);
        loginPage.submit();
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedBrowserFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW,
                    UserSessionLimitsAuthenticatorFactory.BEHAVIOR,
                    UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);

            // Login and verify login was successful
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventRepresentation initialLoginEvent = events.poll();
            EventAssertion.assertSuccess(initialLoginEvent).type(EventType.LOGIN);
            String userId = initialLoginEvent.getUserId();
            String initialLoginSessionID = initialLoginEvent.getSessionId();

            // Delete the cookies, while maintaining the server side session active
            deleteAllCookiesForRealm(driver);

            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            // assert we have a logout session event, as the authenticator should have deleted the first session.
            EventRepresentation logoutEvent = events.poll();
            EventAssertion.assertSuccess(logoutEvent)
                    .type(EventType.LOGOUT)
                    .userId(userId)
                    .sessionId(initialLoginSessionID);
            // User is first logged out, then logged in with a fresh sessionId
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN);
            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW,
                    UserSessionLimitsAuthenticatorFactory.BEHAVIOR,
                    UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedBrowserFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

            // Delete the cookies, while maintaining the server side session active
            deleteAllCookiesForRealm(driver);

            // Login the same user again and verify the configured error message is shown
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventRepresentation errorEvent = events.poll();
            EventAssertion.assertError(errorEvent)
                    .type(EventType.LOGIN_ERROR)
                    .userId(null)
                    .error(Errors.GENERIC_AUTHENTICATION_ERROR);
            errorPage.assertCurrent();
            assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountExceededAndOldestSessionRemovedBrowserFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventRepresentation initialLoginEvent = events.poll();
            EventAssertion.assertSuccess(initialLoginEvent).type(EventType.LOGIN);
            String userId = initialLoginEvent.getUserId();
            String initialLoginSessionID = initialLoginEvent.getSessionId();

            // Delete the cookies, while maintaining the server side session active
            deleteAllCookiesForRealm(driver);

            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            // assert we have a logout session event, as the authenticator should have deleted the first session.
            EventRepresentation logoutEvent = events.poll();
            EventAssertion.assertSuccess(logoutEvent)
                    .type(EventType.LOGOUT)
                    .userId(userId)
                    .sessionId(initialLoginSessionID);
            // User is first logged out, then logged in with a fresh sessionId
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
            String newSessionId = loginEvent.getSessionId();
            assertThat(newSessionId, Matchers.not(initialLoginSessionID));
            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testClientSessionCountExceededAndNewSessionDeniedDirectGrantFlow() throws Exception {
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", password);
        assertEquals(200, response.getStatusCode());

        response = oauth.doPasswordGrantRequest("test-user@localhost", password);
        assertEquals(403, response.getStatusCode());
        assertEquals(ERROR_TO_DISPLAY, response.getError());
    }

    @Test
    public void testRealmSessionLimitExceededButClientLimitNotExceededShouldNotThrow() throws Exception {
        // Reproduces: realm limit exceeded on client-a, then login to client-b whose client
        // session count is below the client limit. Without the fix, logoutOldestSessions()
        // receives a negative count and Stream.limit() throws IllegalArgumentException.
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "3");

            AccessTokenResponse response = oauth.client(directGrant1, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            response = oauth.client(directGrant1, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            // realm limit reached (2 sessions on direct-grant-1); direct-grant-2 has 0 sessions,
            // below the client limit of 3. login must succeed and must not throw a 500.
            response = oauth.client(directGrant2, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant1, 1));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant2, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountAndClientSessionCountExceededAndOldestClientSessionShouldBePrioritized() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");

            AccessTokenResponse response =  oauth.client(directGrant1, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            response =  oauth.client(directGrant2, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant1, 1));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant2, 1));

            response =  oauth.client(directGrant2, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant1, 1));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant2, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    public void testRealmSessionCountAndClientSessionCountExceededAndDecreaseLimitsAfterActiveSessionsAreCreated() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "4");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "2");

            AccessTokenResponse response =  oauth.client(directGrant1, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            response =  oauth.client(directGrant2, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            response =  oauth.client(directGrant1, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            response =  oauth.client(directGrant2, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 4));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant1, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant2, 2));

            response =  oauth.client(directGrant2, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 4));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant1, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant2, 2));

            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");

            response =  oauth.client(directGrant2, password)
                    .doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant1, 1));
            runOnServer.run(assertClientSessionCount(realmName, username, directGrant2, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedDirectGrantFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "2");
            for (int i = 0; i < 2; ++i) {
                AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", password);
                assertEquals(200, response.getStatusCode());
            }
            runOnServer.run(assertSessionCount(realmName, username, 2));

            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());
            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedDirectGrantFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");
            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            response = oauth.doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(403, response.getStatusCode());
            assertEquals(ERROR_TO_DISPLAY, response.getError());
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountExceededAndOldestSessionRemovedDirectGrantFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());

            response = oauth.doPasswordGrantRequest("test-user@localhost", password);
            assertEquals(200, response.getStatusCode());
            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testClientSessionCountExceededAndNewSessionDeniedResetPasswordFlow() throws Exception {
        try {
            // Login and verify login was successful
            String redirect_uri = managedRealm.getBaseUrl() + "/account";
            oauth.client("account");
            oauth.redirectUri(redirect_uri);
            oauth.doLogin(username, password);
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("account")
                    .details(Details.REDIRECT_URI, redirect_uri);

            // Delete the cookies, while maintaining the server side session active
            deleteAllCookiesForRealm(driver);

            String resetUri = managedRealm.getBaseUrl() + "/login-actions/reset-credentials";
            driver.driver().navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());

            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, managedRealm.getBaseUrl() + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, username)
                    .details(Details.EMAIL, username)
                    .sessionId(null)
                    .withoutDetails(Details.CONSENT);
            mailServer.waitForIncomingEmail(1);

            MimeMessage message = mailServer.getLastReceivedMessage();
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.open(changePasswordUrl.trim());

            EventRepresentation event = events.poll();
            EventAssertion.assertError(event)
                    .type(EventType.RESET_PASSWORD_ERROR)
                    .clientId("account")
                    .error(Errors.GENERIC_AUTHENTICATION_ERROR);
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
        }
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedResetPasswordFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);

            // Login and verify login was successful
            String redirect_uri = managedRealm.getBaseUrl()  + "/realms/" + realmName + "/account";
            oauth.client("account");
            oauth.redirectUri(redirect_uri);
            oauth.doLogin(username, password);
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("account")
                    .details(Details.REDIRECT_URI, redirect_uri);

            // Delete the cookies, while maintaining the server side session active
            deleteAllCookiesForRealm(driver);

            String resetUri = managedRealm.getBaseUrl() + "/login-actions/reset-credentials";
            driver.driver().navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, managedRealm.getBaseUrl() + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, username)
                    .details(Details.EMAIL, username)
                    .sessionId(null)
                    .withoutDetails(Details.CONSENT);

            MimeMessage message = mailServer.getLastReceivedMessage();
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.open(changePasswordUrl.trim());
            updatePasswordPage.assertCurrent();
            updatePasswordPage.changePassword("resetPassword", "resetPassword");

            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedResetPasswordFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");

            // Login and verify login was successful
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);

            // Delete the cookies, while maintaining the server side session active
            deleteAllCookiesForRealm(driver);

            // Navigate directly to reset credentials URL
            String resetUri = managedRealm.getBaseUrl() + "/login-actions/reset-credentials";
            driver.driver().navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword(username);
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, managedRealm.getBaseUrl() + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, username)
                    .details(Details.EMAIL, username)
                    .sessionId(null)
                    .withoutDetails(Details.CONSENT);

            MimeMessage message = mailServer.getLastReceivedMessage();
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.open(changePasswordUrl.trim());

            EventRepresentation errorEvent = events.poll();
            EventAssertion.assertError(errorEvent)
                    .type(EventType.RESET_PASSWORD_ERROR)
                    .clientId("account")
                    .error(Errors.GENERIC_AUTHENTICATION_ERROR);
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountExceededAndOldestSessionRemovedResetPasswordFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");

            // Login and verify login was successful
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);

            // Delete the cookies, while maintaining the server side session active
            deleteAllCookiesForRealm(driver);

            String resetUri = managedRealm.getBaseUrl() + "/login-actions/reset-credentials";
            driver.driver().navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword(username);
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, managedRealm.getBaseUrl() + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, username)
                    .details(Details.EMAIL, username)
                    .sessionId(null)
                    .withoutDetails(Details.CONSENT);

            MimeMessage message = mailServer.getLastReceivedMessage();
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.open(changePasswordUrl.trim());
            updatePasswordPage.assertCurrent();
            updatePasswordPage.changePassword("resetPassword", "resetPassword");

            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    // Issue 17374
    @Test
    public void testSSOLogin() throws Exception {
        try {
            // Setup authentication flow
            runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow("browser-session-limits"));
            runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                    .selectFlow("browser-session-limits")
                    .clear()
                    .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, CookieAuthenticatorFactory.PROVIDER_ID)
                    .addSubFlowExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, subFlow -> {
                        subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernamePasswordFormFactory.PROVIDER_ID);
                        subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UserSessionLimitsAuthenticatorFactory.USER_SESSION_LIMITS,
                                config -> {
                                    config.getConfig().put(UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
                                    config.getConfig().put(UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
                                });
                    })
                    .defineAsBrowserFlow()
            );

            // Login in browser1
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            assertThat(driver.getCurrentUrl(), Matchers.startsWith(oauth.getRedirectUri()));
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
            String sessionId1 = loginEvent.getSessionId();

            // Cookie-based SSO login in same browser. Session limit should NOT apply because this reuses the existing
            // session cookie rather than creating a new session.
            oauth.openLoginForm();
            assertThat(driver.getCurrentUrl(), Matchers.startsWith(oauth.getRedirectUri()));
            loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("test-app");
            String sessionId2 = loginEvent.getSessionId();
            assertEquals(sessionId1, sessionId2);

            // verifying session count is still 1
            runOnServer.run(assertSessionCount(realmName, username, 1));

            // Delete cookies to emulate login in new browser
            deleteAllCookiesForRealm(driver);

            // New login should fail due the sessions limit
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventRepresentation errorEvent = events.poll();
            EventAssertion.assertError(errorEvent)
                    .type(EventType.LOGIN_ERROR)
                    .error(Errors.GENERIC_AUTHENTICATION_ERROR);

            errorPage.assertCurrent();
            assertEquals("There are too many sessions", errorPage.getError());

        } finally {
            // Revert config of authenticators
            restoreAndRemoveFlow(realmName);
        }
    }


    private void restoreAndRemoveFlow(String realmName) {
        // Cleanup: restore original browser flow and remove custom flow
        String currentRealm = this.realmName;
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(currentRealm);

            AuthenticationFlowModel originalBrowserFlow = realm.getFlowByAlias(DefaultAuthenticationFlows.BROWSER_FLOW);
            if (originalBrowserFlow != null) {
                realm.setBrowserFlow(originalBrowserFlow);
            }

            AuthenticationFlowModel customFlow = realm.getFlowByAlias("browser-session-limits");
            if (customFlow != null) {
                realm.removeAuthenticationFlow(customFlow);
            }
        });
    }

    private void setAuthenticatorConfigItem(String alias, String key, String value) {
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            if (realm != null) {
                AuthenticationFlowModel flow = realm.getFlowByAlias(alias);
                AuthenticatorConfigModel configModel = realm.getAuthenticatorConfigByAlias("user-session-limits-" + flow.getId());
                if (configModel != null) {
                    configModel.getConfig().put(key, value);
                    realm.updateAuthenticatorConfig(configModel);
                }
            }
        });
    }

    private void deleteAllCookiesForRealm(ManagedWebDriver driver) {
        // Navigate to a blank page in the realm to ensure cookies are properly scoped
        driver.driver().navigate().to(managedRealm.getBaseUrl());
        driver.cookies().deleteAll();
    }

    public static class UserSessionLimitsRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.addUser(username)
                    .email(username)
                    .name("Test", "User")
                    .emailVerified(true)
                    .password(password)
                    .enabled(true);

            realm.name(realmName);
            realm.addClient(directGrant1)
                    .secret(password)
                    .directAccessGrantsEnabled(true)
                    .redirectUris("*");

            realm.addClient(directGrant2)
                    .secret(password)
                    .directAccessGrantsEnabled(true)
                    .redirectUris("*");

            realm.resetPasswordAllowed(true);
            return realm;
        }
    }
}
