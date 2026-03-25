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

import jakarta.mail.internet.MimeMessage;

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
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.mail.MailServer;
import org.keycloak.testframework.mail.annotations.InjectMailServer;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakUrls;
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

import java.util.HashMap;
import java.util.Map;

import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.ERROR_TO_DISPLAY;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertClientSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertSessionCount;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class UserSessionLimitsTest {

    @InjectRealm(config = SessionLimitsRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectPage
    protected LoginPasswordResetPage resetPasswordPage;

    @InjectPage
    protected LoginPasswordUpdatePage updatePasswordPage;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectEvents
    protected Events events;

    @InjectMailServer
    MailServer mailServer;

    private String realmName;
    private final String username = "test-user@localhost";
    private static boolean isServerConfigured = false;
    private static String defaultRedirectUri = null;

    @BeforeEach
    public void setupFlows() {
        driver.cookies().deleteAll();

        if (defaultRedirectUri == null) {
            defaultRedirectUri = oauth.getRedirectUri();
        }

        oauth.client("test-app", "");
        oauth.redirectUri(defaultRedirectUri);

        String userId = managedRealm.admin().users().searchByUsername(username, true).get(0).getId();
        managedRealm.admin().users().get(userId).logout();

        // 1. Store the realm name in a LOCAL variable
        String localRealmName = managedRealm.getName();
        this.realmName = localRealmName;

        if (!isServerConfigured) {
            runOnServer.run(session -> {
                // DECLARE 'realm' FIRST! Use the LOCAL variable so it doesn't capture 'this'
                RealmModel realm = session.realms().getRealmByName(localRealmName);

                // Enable direct grants for test-app (needed for later tests)
                org.keycloak.models.ClientModel testApp = realm.getClientByClientId("test-app");
                if (testApp != null) {
                    testApp.setDirectAccessGrantsEnabled(true);
                }

                AuthenticationFlowModel browser = realm.getBrowserFlow();

                // 1. Wipe out the complex default ALTERNATIVE executions from the injected realm
                realm.getAuthenticationExecutionsStream(browser.getId())
                        .toList()
                        .forEach(realm::removeAuthenticatorExecution);

                // 2. Rebuild the linear, 2-step flow the test explicitly expects.
                // Call the static method on the class to prevent implicit 'this' capture.
                UserSessionLimitsTest.configureUsernamePassword(realm, browser);
                UserSessionLimitsUtil.configureSessionLimits(realm, browser, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");

                // Note: Direct Grant and Reset Password flows are linear by default,
                // so we can just append the limits to them without clearing them first.
                AuthenticationFlowModel directGrant = realm.getDirectGrantFlow();
                UserSessionLimitsUtil.configureSessionLimits(realm, directGrant, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");

                AuthenticationFlowModel resetPasswordFlow = realm.getResetCredentialsFlow();
                UserSessionLimitsUtil.configureSessionLimits(realm, resetPasswordFlow, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");
            });
            isServerConfigured = true;
        }
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
    public void testClientSessionCountExceededAndNewSessionDeniedBrowserFlow() {
        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

        driver.cookies().deleteAll();

        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", "password");
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
    public void testClientSessionCountNotExceededOnReAuthentication() {
        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

        oauth.loginForm().prompt("login").open();
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedBrowserFlow() {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);

            oauth.openLoginForm();
            loginPage.fillLogin("test-user@localhost", "password");
            loginPage.submit();
            EventRepresentation initialLoginEvent = events.poll();
            EventAssertion.assertSuccess(initialLoginEvent).type(EventType.LOGIN);
            String userId = initialLoginEvent.getUserId();
            String initialLoginSessionID = initialLoginEvent.getSessionId();

            driver.cookies().deleteAll();

            oauth.openLoginForm();
            loginPage.fillLogin("test-user@localhost", "password");
            loginPage.submit();
            EventRepresentation logoutEvent = events.poll();
            EventAssertion.assertSuccess(logoutEvent)
                    .type(EventType.LOGOUT)
                    .userId(userId)
                    .sessionId(initialLoginSessionID);
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN);
            String newSessionId = loginEvent.getSessionId();
            assertThat(newSessionId, Matchers.not(initialLoginSessionID));
            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedBrowserFlow() {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");
            oauth.openLoginForm();
            loginPage.fillLogin("test-user@localhost", "password");
            loginPage.submit();
            EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

            driver.cookies().deleteAll();

            oauth.openLoginForm();
            loginPage.fillLogin("test-user@localhost", "password");
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
    public void testRealmSessionCountExceededAndOldestSessionRemovedBrowserFlow()  {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");
            oauth.openLoginForm();
            loginPage.fillLogin("test-user@localhost", "password");
            loginPage.submit();
            EventRepresentation initialLoginEvent = events.poll();
            EventAssertion.assertSuccess(initialLoginEvent).type(EventType.LOGIN);
            String userId = initialLoginEvent.getUserId();
            String initialLoginSessionID = initialLoginEvent.getSessionId();

            driver.cookies().deleteAll();

            oauth.openLoginForm();
            loginPage.fillLogin("test-user@localhost", "password");
            loginPage.submit();
            EventRepresentation logoutEvent = events.poll();
            EventAssertion.assertSuccess(logoutEvent)
                    .type(EventType.LOGOUT)
                    .userId(userId)
                    .sessionId(initialLoginSessionID);
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
    public void testClientSessionCountExceededAndNewSessionDeniedDirectGrantFlow()  {
        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(200, response.getStatusCode());

        response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
        assertEquals(403, response.getStatusCode());
        assertEquals(ERROR_TO_DISPLAY, response.getError());
    }

    @Test
    public void testRealmSessionCountAndClientSessionCountExceededAndOldestClientSessionShouldBePrioritized() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");

            AccessTokenResponse response =  oauth.client("direct-grant-1", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            Thread.sleep(1100);

            response =  oauth.client("direct-grant-2", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));
            Thread.sleep(1100);

            response =  oauth.client("direct-grant-2", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountAndClientSessionCountExceededAndDecreaseLimitsAfterActiveSessionsAreCreated() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "4");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "2");

            AccessTokenResponse response =  oauth.client("direct-grant-1", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            Thread.sleep(1100);

            response =  oauth.client("direct-grant-2", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            Thread.sleep(1100);

            response =  oauth.client("direct-grant-1", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            Thread.sleep(1100);

            response =  oauth.client("direct-grant-2", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 4));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 2));

            Thread.sleep(1100);

            response =  oauth.client("direct-grant-2", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 4));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 2));

            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");

            Thread.sleep(1100);

            response =  oauth.client("direct-grant-2", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));
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
                AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
                assertEquals(200, response.getStatusCode());
                Thread.sleep(1100);
            }
            runOnServer.run(assertSessionCount(realmName, username, 2));

            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedDirectGrantFlow()  {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");
            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(403, response.getStatusCode());
            assertEquals(ERROR_TO_DISPLAY, response.getError());
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmLimitExceededWithMultipleClientsAndClientLimitHigherDirectGrantFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "3");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "10");

            AccessTokenResponse response = oauth.client("direct-grant-1", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            Thread.sleep(1100);

            response = oauth.client("direct-grant-1", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            Thread.sleep(1100);

            response = oauth.client("direct-grant-2", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 3));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));

            Thread.sleep(1100);

            response = oauth.client("direct-grant-2", "password").doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 3));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
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

            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            Thread.sleep(1100);

            response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testClientSessionCountExceededAndNewSessionDeniedResetPasswordFlow() throws Exception  {
        try {
            String redirect_uri = keycloakUrls.getBase() + "/realms/" + realmName + "/account";
            oauth.client("account");
            oauth.redirectUri(redirect_uri);
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("account")
                    .details(Details.REDIRECT_URI, redirect_uri);

            driver.cookies().deleteAll();

            String resetUri = keycloakUrls.getBase() + "/realms/" + realmName + "/login-actions/reset-credentials";
            driver.open(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/" + realmName + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, "test-user@localhost")
                    .details(Details.EMAIL, "test-user@localhost")
                    .sessionId(null);
            assertEquals(1, mailServer.getReceivedMessages().length);

            MimeMessage message = mailServer.getLastReceivedMessage();
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.open(changePasswordUrl.trim());

            EventRepresentation event = events.poll();
            EventAssertion.assertError(event)
                    .type(EventType.RESET_PASSWORD_ERROR)
                    .clientId("account")
                    .error(Errors.GENERIC_AUTHENTICATION_ERROR);
        } finally {
            managedRealm.admin().clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).setDirectAccessGrantsEnabled(false);
        }
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedResetPasswordFlow() throws Exception  {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);

            String redirect_uri = keycloakUrls.getBase()  + "/realms/" + realmName + "/account";
            oauth.client("account");
            oauth.redirectUri(redirect_uri);
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("account")
                    .details(Details.REDIRECT_URI, redirect_uri);

            driver.cookies().deleteAll();

            String resetUri = keycloakUrls.getBase()  + "/realms/" + realmName + "/login-actions/reset-credentials";
            driver.open(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/" + realmName + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, "test-user@localhost")
                    .details(Details.EMAIL, "test-user@localhost")
                    .sessionId(null);

            assertEquals(1, mailServer.getReceivedMessages().length);
            MimeMessage message = mailServer.getLastReceivedMessage();
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.open(changePasswordUrl.trim());
            updatePasswordPage.assertCurrent();
            updatePasswordPage.changePassword("resetPassword", "resetPassword");

            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW,
                    UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedResetPasswordFlow() throws Exception  {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");

            oauth.openLoginForm();
            loginPage.fillLogin("test-user@localhost", "password");
            loginPage.submit();
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);

            driver.cookies().deleteAll();

            String resetUri = keycloakUrls.getBase() + "/realms/" + realmName + "/login-actions/reset-credentials";
            driver.open(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/" + realmName + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, "test-user@localhost")
                    .details(Details.EMAIL, "test-user@localhost")
                    .sessionId(null);

            assertEquals(1, mailServer.getReceivedMessages().length);
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
    public void testRealmSessionCountExceededAndOldestSessionRemovedResetPasswordFlow() throws Exception  {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");

            oauth.openLoginForm();
            loginPage.fillLogin("test-user@localhost", "password");
            loginPage.submit();
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);

            driver.cookies().deleteAll();

            String resetUri = keycloakUrls.getBase() + "/realms/" + realmName + "/login-actions/reset-credentials";
            driver.open(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/" + realmName + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, "test-user@localhost")
                    .details(Details.EMAIL, "test-user@localhost")
                    .sessionId(null);

            assertEquals(1, mailServer.getReceivedMessages().length);
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
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow("browser-session-limits"));
        runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                .selectFlow("browser-session-limits")
                .clear()
                .defineAsBrowserFlow()
        );

        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();
        assertThat(driver.getCurrentUrl(), Matchers.startsWith(oauth.getRedirectUri()));
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
        String sessionId1 = loginEvent.getSessionId();

        oauth.openLoginForm();
        assertThat(driver.getCurrentUrl(), Matchers.startsWith(oauth.getRedirectUri()));
        loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("test-app");
        String sessionId2 = loginEvent.getSessionId();
        assertEquals(sessionId1, sessionId2);

        driver.cookies().deleteAll();

        oauth.openLoginForm();
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();
        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.LOGIN_ERROR)
                .error(Errors.GENERIC_AUTHENTICATION_ERROR);

        errorPage.assertCurrent();
        assertEquals("There are too many sessions", errorPage.getError());

        String currentRealm = this.realmName;
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(currentRealm);
            AuthenticationFlowModel flow = realm.getFlowByAlias("browser-session-limits");
            if (flow != null) {
                realm.removeAuthenticationFlow(flow);
            }
        });
    }

    // CRITICAL FIX: To force JPA to dirty check the config entity in JUnit 5, we must replace the map entirely.
    private void setAuthenticatorConfigItem(String alias, String key, String value) {
        String currentRealm = this.realmName;
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(currentRealm);
            AuthenticationFlowModel flow = realm.getFlowByAlias(alias);
            AuthenticatorConfigModel configModel = realm.getAuthenticatorConfigByAlias("user-session-limits-" + flow.getId());

            // Do not use configModel.getConfig().put(key, value) here! It fails silently.
            Map<String, String> newConfig = new HashMap<>(configModel.getConfig());
            newConfig.put(key, value);
            configModel.setConfig(newConfig);

            realm.updateAuthenticatorConfig(configModel);
        });
    }

    public static class SessionLimitsRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addUser("test-user@localhost")
                    .email("test-user@localhost")
                    .firstName("Test")
                    .lastName("User")
                    .emailVerified(true)
                    .password("password");

            // Define custom clients. We removed test-app to avoid the 409 database collision!
            realm.addClient("direct-grant-1")
                    .secret("password")
                    .directAccessGrantsEnabled(true);

            realm.addClient("direct-grant-2")
                    .secret("password")
                    .directAccessGrantsEnabled(true);

            return realm;
        }
    }
}
