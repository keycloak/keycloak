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
package org.keycloak.testsuite.sessionlimits;

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
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.forms.BrowserFlowTest;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.MailUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.sessionlimits.UserSessionLimitsUtil.ERROR_TO_DISPLAY;
import static org.keycloak.testsuite.sessionlimits.UserSessionLimitsUtil.assertClientSessionCount;
import static org.keycloak.testsuite.sessionlimits.UserSessionLimitsUtil.assertSessionCount;
import static org.keycloak.testsuite.sessionlimits.UserSessionLimitsUtil.configureSessionLimits;

import static org.junit.Assert.assertEquals;

public class UserSessionLimitsTest extends AbstractTestRealmKeycloakTest {
    private String realmName = "test";
    private String username = "test-user@localhost";

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        findTestApp(testRealm).setDirectAccessGrantsEnabled(true);
    }

    @Before
    public void setupFlows() {
        // Do this just once per class
        if (testContext.isInitialized()) {
            return;
        }
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");

            AuthenticationFlowModel browser = realm.getBrowserFlow();
            configureUsernamePassword(realm, browser);
            configureSessionLimits(realm, browser, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");

            AuthenticationFlowModel directGrant = realm.getDirectGrantFlow();
            configureSessionLimits(realm, directGrant, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");

            AuthenticationFlowModel resetPasswordFlow = realm.getResetCredentialsFlow();
            configureSessionLimits(realm, resetPasswordFlow, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");

            var directClient1 = realm.addClient("direct-grant-1");
            directClient1.setName("direct-grant-1");
            directClient1.setSecret("password");
            directClient1.setDirectAccessGrantsEnabled(true);
            directClient1.setEnabled(true);

            var directClient2 = realm.addClient("direct-grant-2");
            directClient2.setName("direct-grant-2");
            directClient2.setSecret("password");
            directClient2.setDirectAccessGrantsEnabled(true);
            directClient2.setEnabled(true);
        });
        testContext.setInitialized(true);
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

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Page
    protected AppPage appPage;

    @Test
    public void testClientSessionCountExceededAndNewSessionDeniedBrowserFlow() throws Exception {
        // Login and verify login was successful
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        // Delete the cookies, while maintaining the server side session active
        super.deleteCookies();
        
        // Login the same user again and verify the configured error message is shown
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        events.expect(EventType.LOGIN_ERROR).user((String) null).error(Errors.GENERIC_AUTHENTICATION_ERROR).assertEvent();
        errorPage.assertCurrent();
        assertEquals(ERROR_TO_DISPLAY, errorPage.getError());
    }

    @Test
    public void testClientSessionCountNotExceededOnReAuthentication() throws Exception {
        // Login and verify login was successful
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        // Re-authenticate the user with prompt=login
        oauth.loginForm().prompt("login").open();
        loginPage.login("test-user@localhost", "password");
        events.expectLogin().assertEvent();
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedBrowserFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);

            // Login and verify login was successful
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            events.expectLogin().assertEvent();

            // Delete the cookies, while maintaining the server side session active
            super.deleteCookies();

            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            events.expectLogin().assertEvent();
            testingClient.server(realmName).run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedBrowserFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            events.expectLogin().assertEvent();

            // Delete the cookies, while maintaining the server side session active
            super.deleteCookies();

            // Login the same user again and verify the configured error message is shown
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            events.expect(EventType.LOGIN_ERROR).user((String) null).error(Errors.GENERIC_AUTHENTICATION_ERROR).assertEvent();
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
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            events.expectLogin().assertEvent();

            // Delete the cookies, while maintaining the server side session active
            super.deleteCookies();

            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            events.expectLogin().assertEvent();
            testingClient.server(realmName).run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.BROWSER_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testClientSessionCountExceededAndNewSessionDeniedDirectGrantFlow() throws Exception {
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

            response =  oauth.client("direct-grant-2", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            testingClient.server(realmName).run(assertSessionCount(realmName, username, 2));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));

            response =  oauth.client("direct-grant-2", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            testingClient.server(realmName).run(assertSessionCount(realmName, username, 2));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));
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

            AccessTokenResponse response =  oauth.client("direct-grant-1", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            response =  oauth.client("direct-grant-2", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            response =  oauth.client("direct-grant-1", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            response =  oauth.client("direct-grant-2", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            testingClient.server(realmName).run(assertSessionCount(realmName, username, 4));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-2", 2));

            response =  oauth.client("direct-grant-2", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            testingClient.server(realmName).run(assertSessionCount(realmName, username, 4));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-2", 2));

            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");

            response =  oauth.client("direct-grant-2", "password")
                    .doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            testingClient.server(realmName).run(assertSessionCount(realmName, username, 2));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
            testingClient.server(realmName).run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));
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
            }
            testingClient.server(realmName).run(assertSessionCount(realmName, username, 2));

            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            testingClient.server(realmName).run(assertSessionCount(realmName, username, 1));
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
    public void testRealmSessionCountExceededAndOldestSessionRemovedDirectGrantFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.DIRECT_GRANT_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());

            response = oauth.doPasswordGrantRequest("test-user@localhost", "password");
            assertEquals(200, response.getStatusCode());
            testingClient.server(realmName).run(assertSessionCount(realmName, username, 1));
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
            String redirect_uri = oauth.AUTH_SERVER_ROOT + "/realms/test/account";
            oauth.clientId("account");
            oauth.redirectUri(redirect_uri);
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().client("account").detail(Details.REDIRECT_URI, redirect_uri).assertEvent();

            // Delete the cookies, while maintaining the server side session active
            super.deleteCookies();

            String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
            driver.navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .user(loginEvent.getUserId())
                    .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                    .client("account")
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.EMAIL, "test-user@localhost")
                    .session((String)null)
                    .assertEvent();
            assertEquals(1, greenMail.getReceivedMessages().length);

            MimeMessage message = greenMail.getReceivedMessages()[0];
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.navigate().to(changePasswordUrl.trim());

            events.expect(EventType.RESET_PASSWORD_ERROR).client("account").error(Errors.GENERIC_AUTHENTICATION_ERROR).assertEvent();
        } finally {
            testRealm().clients().findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID).get(0).setDirectAccessGrantsEnabled(false);
            ApiUtil.resetUserPassword(testRealm().users().get(findUser("test-user@localhost").getId()), "password", false);
        }
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedResetPasswordFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);

            // Login and verify login was successful
            String redirect_uri = oauth.AUTH_SERVER_ROOT + "/realms/test/account";
            oauth.clientId("account");
            oauth.redirectUri(redirect_uri);
            oauth.doLogin("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().client("account").detail(Details.REDIRECT_URI, redirect_uri).assertEvent();

            // Delete the cookies, while maintaining the server side session active
            super.deleteCookies();

            String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
            driver.navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .user(loginEvent.getUserId())
                    .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                    .client("account")
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.EMAIL, "test-user@localhost")
                    .session((String)null)
                    .assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getReceivedMessages()[0];
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.navigate().to(changePasswordUrl.trim());
            updatePasswordPage.assertCurrent();
            updatePasswordPage.changePassword("resetPassword", "resetPassword");

            testingClient.server(realmName).run(assertSessionCount(realmName, username, 1));
        } finally {
            ApiUtil.resetUserPassword(testRealm().users().get(findUser("test-user@localhost").getId()), "password", false);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedResetPasswordFlow() throws Exception {
        try {
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "0");

            // Login and verify login was successful
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            // Delete the cookies, while maintaining the server side session active
            super.deleteCookies();

            String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
            driver.navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .user(loginEvent.getUserId())
                    .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                    .client("account")
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.EMAIL, "test-user@localhost")
                    .session((String)null)
                    .assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getReceivedMessages()[0];
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.navigate().to(changePasswordUrl.trim());

            events.expect(EventType.RESET_PASSWORD_ERROR).client("account").error(Errors.GENERIC_AUTHENTICATION_ERROR).assertEvent();
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
            loginPage.open();
            loginPage.login("test-user@localhost", "password");
            EventRepresentation loginEvent = events.expectLogin().assertEvent();

            // Delete the cookies, while maintaining the server side session active
            super.deleteCookies();

            String resetUri = oauth.AUTH_SERVER_ROOT + "/realms/test/login-actions/reset-credentials";
            driver.navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword("test-user@localhost");
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            events.expectRequiredAction(EventType.SEND_RESET_PASSWORD)
                    .user(loginEvent.getUserId())
                    .detail(Details.REDIRECT_URI,  oauth.AUTH_SERVER_ROOT + "/realms/test/account/")
                    .client("account")
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(Details.EMAIL, "test-user@localhost")
                    .session((String)null)
                    .assertEvent();

            assertEquals(1, greenMail.getReceivedMessages().length);
            MimeMessage message = greenMail.getReceivedMessages()[0];
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.navigate().to(changePasswordUrl.trim());
            updatePasswordPage.assertCurrent();
            updatePasswordPage.changePassword("resetPassword", "resetPassword");

            testingClient.server(realmName).run(assertSessionCount(realmName, username, 1));
        } finally {
            ApiUtil.resetUserPassword(testRealm().users().get(findUser("test-user@localhost").getId()), "password", false);

            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.BEHAVIOR, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(DefaultAuthenticationFlows.RESET_CREDENTIALS_FLOW, UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT, "1");
        }
    }

    // Issue 17374
    @Test
    public void testSSOLogin() throws Exception {
        // Setup authentication flow
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session). copyBrowserFlow("browser-session-limits"));
        testingClient.server("test").run(session -> FlowUtil.inCurrentRealm(session)
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
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId1 = loginEvent.getSessionId();

        // SSO login in browser1. Should be still OK (Login won't be denied even if session limit is set to 1 because we are login in same browser for SSO login)
        oauth.openLoginForm();
        assertEquals(AppPage.RequestType.AUTH_RESPONSE, appPage.getRequestType());
        loginEvent = events.expectLogin().removeDetail(Details.USERNAME).client("test-app").assertEvent();
        String sessionId2 = loginEvent.getSessionId();
        assertEquals(sessionId1, sessionId2);

        // Delete cookies to emulate login in new browser
        super.deleteCookies();

        // New login should fail due the sessions limit
        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        events.expect(EventType.LOGIN_ERROR).user((String) null).error(Errors.GENERIC_AUTHENTICATION_ERROR).assertEvent();
        errorPage.assertCurrent();
        assertEquals("There are too many sessions", errorPage.getError()); // Default error message

        // Revert config of authenticators
        BrowserFlowTest.revertFlows(adminClient.realm("test"), "browser-session-limits");
    }

    private void setAuthenticatorConfigItem(String alias, String key, String value) {
        testingClient.server().run(session -> {
            RealmModel realm = session.realms().getRealmByName("test");
            AuthenticationFlowModel flow = realm.getFlowByAlias(alias);
            AuthenticatorConfigModel configModel = realm.getAuthenticatorConfigByAlias("user-session-limits-" + flow.getId());
            configModel.getConfig().put(key, value);
            realm.updateAuthenticatorConfig(configModel);
        });
    }
}
