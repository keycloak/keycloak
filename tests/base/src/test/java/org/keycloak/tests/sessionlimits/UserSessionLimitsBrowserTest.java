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

import org.keycloak.authentication.authenticators.browser.CookieAuthenticatorFactory;
import org.keycloak.authentication.authenticators.browser.UsernamePasswordFormFactory;
import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
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
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.FlowUtil;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.BEHAVIOR;
import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION;
import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT;
import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.ERROR_TO_DISPLAY;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.configureSessionLimits;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.deleteAllCookiesForRealm;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


@KeycloakIntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserSessionLimitsBrowserTest {

    @InjectRealm(config = SessionLimitsRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    protected LoginPage loginPage;

    @InjectPage
    protected ErrorPage errorPage;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectEvents
    protected Events events;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    private String realmName;
    private static final String username = "test-user@localhost";
    private static final String password = "password";
    private static String defaultRedirectUri = null;
    private static boolean flowsConfigured = false;

    @BeforeEach
    public void setup() {
        // Clear browser cookies
        deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

        // Clear server-side user sessions
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeUserSessions(realm);
        });

        if (defaultRedirectUri == null) {
            defaultRedirectUri = oauth.getRedirectUri();
        }

        oauth.redirectUri(defaultRedirectUri);
        this.realmName = managedRealm.getName();

        if (!flowsConfigured) {
            setUpFlows();
        }
        events.clear();
    }

    private void setUpFlows() {
         runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();

            // Enable direct grants for test-app
            org.keycloak.models.ClientModel testApp = realm.getClientByClientId("test-app");
            if (testApp != null) {
             testApp.setDirectAccessGrantsEnabled(true);
            }

            // Configure session limits on browser flow
            AuthenticationFlowModel browserFlow = realm.getBrowserFlow();
            configureUsernamePassword(realm, browserFlow);
            configureSessionLimits(realm, browserFlow, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");
         });
         flowsConfigured = true;
    }

    @Test
    public void testClientSessionCountExceededAndNewSessionDeniedBrowserFlow() {

        oauth.openLoginForm();
        loginPage.fillLogin(username, password);
        loginPage.submit();
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);

        runOnServer.run(assertSessionCount(realmName, username, 1));

        // Delete the cookies, while maintaining the server side session active
        deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

        // First login creates session (count=1), second login should be DENIED because count >= limit
        oauth.openLoginForm();
        loginPage.fillLogin(username, password);
        loginPage.submit();
        
        EventRepresentation errorEvent = events.poll();

        final String realm = realmName;
        final String clientId = "test-app";
        runOnServer.run(session -> {
            org.keycloak.models.RealmModel realmModel = session.realms().getRealmByName(realm);
            org.keycloak.models.UserModel userModel = session.users().getUserByUsername(realmModel, username);
            org.keycloak.models.ClientModel clientModel = realmModel.getClientByClientId(clientId);
        });
        
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
        loginPage.fillLogin(username, password);
        loginPage.submit();
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

        oauth.loginForm().prompt("login").open();
        loginPage.fillLogin(username, password);
        loginPage.submit();
        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedBrowserFlow() {
        try {
            setAuthenticatorConfigItem(BEHAVIOR,
                    UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);

            // Login and verify login was successful
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventRepresentation initialLoginEvent = events.poll();
            EventAssertion.assertSuccess(initialLoginEvent).type(EventType.LOGIN);
            String userId = initialLoginEvent.getUserId();
            String initialLoginSessionID = initialLoginEvent.getSessionId();

            deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
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
            setAuthenticatorConfigItem(BEHAVIOR, DENY_NEW_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedBrowserFlow() {
        try {
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "0");
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);

            deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

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
            setAuthenticatorConfigItem(BEHAVIOR, DENY_NEW_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountExceededAndOldestSessionRemovedBrowserFlow()  {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "0");
            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventRepresentation initialLoginEvent = events.poll();
            EventAssertion.assertSuccess(initialLoginEvent).type(EventType.LOGIN);
            String userId = initialLoginEvent.getUserId();
            String initialLoginSessionID = initialLoginEvent.getSessionId();

            deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
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
            setAuthenticatorConfigItem(BEHAVIOR, DENY_NEW_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "1");
        }
    }

    // Issue 17374
    @Test
    public void testSSOLogin() {
        try {
            runOnServer.run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow("browser-session-limits"));
            runOnServer.run(session -> FlowUtil.inCurrentRealm(session)
                    .selectFlow("browser-session-limits")
                    .clear()
                    .addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE,
                            CookieAuthenticatorFactory.PROVIDER_ID)
                    .addSubFlowExecution(AuthenticationExecutionModel.Requirement.ALTERNATIVE, subFlow -> {
                        subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED,
                                UsernamePasswordFormFactory.PROVIDER_ID);
                        subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED,
                                UserSessionLimitsAuthenticatorFactory.USER_SESSION_LIMITS,
                                config -> {
                                    config.getConfig().put(UserSessionLimitsAuthenticatorFactory.BEHAVIOR,
                                            UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION);
                                    config.getConfig().put(UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT, "1");
                                    config.getConfig().put(UserSessionLimitsAuthenticatorFactory.ERROR_MESSAGE,
                                            "There are too many sessions");
                                });
                    })
                    .defineAsBrowserFlow()
            );

            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
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

            deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

            oauth.openLoginForm();
            loginPage.fillLogin(username, password);
            loginPage.submit();
            EventRepresentation errorEvent = events.poll();
            EventAssertion.assertError(errorEvent)
                    .type(EventType.LOGIN_ERROR)
                    .error(Errors.GENERIC_AUTHENTICATION_ERROR);

            errorPage.assertCurrent();
            assertEquals("There are too many sessions", errorPage.getError());

            restoreAndRemoveFlow(realmName, username);

        } finally {
            // Ensure original browser flow is restored even if test fails
            restoreAndRemoveFlow(realmName, username);
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

    private void restoreAndRemoveFlow(String realmName, String username) {
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

    private void setAuthenticatorConfigItem(String key, String value) {
        String currentRealm = this.realmName;
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(currentRealm);
            AuthenticationFlowModel browserFlow = realm.getBrowserFlow();
            String configAlias = "user-session-limits-" + browserFlow.getId();
            AuthenticatorConfigModel configModel = realm.getAuthenticatorConfigByAlias(configAlias);
            if (configModel == null) {
                throw new RuntimeException("Config not found: " + configAlias +
                        " in flow: " + "custom-direct-grant-with-limits");
            }
            configModel.getConfig().put(key, value);
            realm.updateAuthenticatorConfig(configModel);
        });
    }

    public static class SessionLimitsRealmConfig implements RealmConfig {
        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addUser(username)
                    .email(username)
                    .firstName("Test")
                    .lastName("User")
                    .emailVerified(true)
                    .password(password)
                    .enabled(true);
            realm.name("session-limits-test-realm");
            return realm;
        }
    }
}
