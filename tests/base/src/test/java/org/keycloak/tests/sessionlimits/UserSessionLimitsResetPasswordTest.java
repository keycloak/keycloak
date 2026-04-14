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

import java.time.Duration;

import jakarta.mail.internet.MimeMessage;

import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
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
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.page.LoginPasswordResetPage;
import org.keycloak.testframework.ui.page.LoginPasswordUpdatePage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.MailUtils;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.BEHAVIOR;
import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION;
import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT;
import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.configureSessionLimits;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.deleteAllCookiesForRealm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;

@TestInstance(PER_CLASS)
@KeycloakIntegrationTest
public class UserSessionLimitsResetPasswordTest {

    @InjectRealm(config = SessionLimitsRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    protected LoginPage loginPage;

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
    private String defaultRedirectUri = null;
    private boolean flowsConfigured = false;

    private static final String username = "test-user@localhost";

    @BeforeEach
    public void setUp() {

        this.realmName = managedRealm.getName();

        // Clear browser cookies
        deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

        // Clear server-side user sessions
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeUserSessions(realm);
        });

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeUserSessions(realm);

            // Reset password
            org.keycloak.models.UserModel user = session.users().getUserByUsername(realm, username);
            user.credentialManager().updateCredential(UserCredentialModel.password("password"));
        });

        if (defaultRedirectUri == null) {
            defaultRedirectUri = oauth.getRedirectUri();
        }
        oauth.redirectUri(defaultRedirectUri);

        if (!flowsConfigured) {
            setupFlows();
        }

        events.clear();
    }

    public void setupFlows() {

        runOnServer.run(session -> {

            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeUserSessions(realm);

            // Configure session limits on reset password flow
            AuthenticationFlowModel resetPasswordFlow = realm.getResetCredentialsFlow();
            configureSessionLimits(realm, resetPasswordFlow, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");
        });
        flowsConfigured = true;
    }

    @AfterEach
    public void tearDown() {
        // Restore account client to match realm configuration
        try {
            var accountClients = managedRealm.admin().clients()
                    .findByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
            if (!accountClients.isEmpty()) {
                accountClients.get(0).setDirectAccessGrantsEnabled(true); // Match realm config
            }
        } catch (Exception e) {
            System.err.println("Failed to restore account client state: " + e.getMessage());
        }
    }

    @Test
    public void testClientSessionCountExceededAndNewSessionDeniedResetPasswordFlow() throws Exception  {

        String redirect_uri = keycloakUrls.getBase() + "/realms/" + realmName + "/account";
        oauth.client("account");
        oauth.redirectUri(redirect_uri);
        oauth.doLogin(username, "password");
        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .clientId("account")
                .details(Details.REDIRECT_URI, redirect_uri);

        // Delete the cookies, while maintaining the server side session active
        deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

        String resetUri = keycloakUrls.getBase() + "/realms/" + realmName + "/login-actions/reset-credentials";
        driver.driver().navigate().to(resetUri);

        resetPasswordPage.assertCurrent();
        resetPasswordPage.changePassword(username);
        loginPage.assertCurrent();
        assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
        EventRepresentation resetEvent = events.poll();
        EventAssertion.assertSuccess(resetEvent)
                .type(EventType.SEND_RESET_PASSWORD)
                .userId(loginEvent.getUserId())
                .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/" + realmName + "/account/")
                .clientId("account")
                .details(Details.USERNAME, username)
                .details(Details.EMAIL, username)
                .sessionId(null)
                .withoutDetails(Details.CONSENT);

        Awaitility.await()
            .atMost(Duration.ofSeconds(5))
            .untilAsserted(() -> assertEquals(1, mailServer.getReceivedMessages().length));

        MimeMessage message = mailServer.getLastReceivedMessage();
        String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
        driver.open(changePasswordUrl.trim());

        EventRepresentation event = events.poll();
        EventAssertion.assertError(event)
                .type(EventType.RESET_PASSWORD_ERROR)
                .clientId("account")
                .error(Errors.GENERIC_AUTHENTICATION_ERROR);
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedResetPasswordFlow() throws Exception  {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);

            String redirect_uri = keycloakUrls.getBase()  + "/realms/" + realmName + "/account";
            oauth.client("account");
            oauth.redirectUri(redirect_uri);
            oauth.doLogin(username, "password");
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent)
                    .type(EventType.LOGIN)
                    .clientId("account")
                    .details(Details.REDIRECT_URI, redirect_uri);

            // Delete the cookies, while maintaining the server side session active
            deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

            String resetUri = keycloakUrls.getBase() + "/realms/" + realmName + "/login-actions/reset-credentials";
            driver.driver().navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword(username);
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/" + realmName + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, username)
                    .details(Details.EMAIL, username)
                    .sessionId(null)
                    .withoutDetails(Details.CONSENT);

            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> assertEquals(1, mailServer.getReceivedMessages().length));

            MimeMessage message = mailServer.getLastReceivedMessage();
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.open(changePasswordUrl.trim());
            updatePasswordPage.assertCurrent();
            updatePasswordPage.changePassword("resetPassword", "resetPassword");

            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(BEHAVIOR, DENY_NEW_SESSION);
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedResetPasswordFlow() throws Exception  {
        try {
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "0");

            // Login and verify login was successful
            oauth.openLoginForm();
            loginPage.fillLogin(username, "password");
            loginPage.submit();
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);

            // Delete the cookies, while maintaining the server side session active=
            String loginUrl = oauth.loginForm().build();
            String realmUrl = loginUrl.substring(0, loginUrl.indexOf("/protocol/openid-connect/auth"));
            deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

            // Navigate directly to reset credentials URL
            String resetUri = realmUrl + "/login-actions/reset-credentials";
            driver.driver().navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword(username);
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/" + realmName + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, username)
                    .details(Details.EMAIL, username)
                    .sessionId(null)
                    .withoutDetails(Details.CONSENT);

            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> assertEquals(1, mailServer.getReceivedMessages().length));
            MimeMessage message = mailServer.getLastReceivedMessage();
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.open(changePasswordUrl.trim());

            EventRepresentation errorEvent = events.poll();
            EventAssertion.assertError(errorEvent)
                    .type(EventType.RESET_PASSWORD_ERROR)
                    .clientId("account")
                    .error(Errors.GENERIC_AUTHENTICATION_ERROR);
        } finally {
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem( USER_CLIENT_LIMIT, "1");
        }
    }

    @Test
    public void testRealmSessionCountExceededAndOldestSessionRemovedResetPasswordFlow() throws Exception  {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "0");

            oauth.openLoginForm();
            loginPage.fillLogin(username, "password");
            loginPage.submit();
            EventRepresentation loginEvent = events.poll();
            EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);

            // Delete the cookies, while maintaining the server side session active
            deleteAllCookiesForRealm(driver, keycloakUrls, realmName);

            String resetUri = keycloakUrls.getBase() + "/realms/" + realmName + "/login-actions/reset-credentials";
            driver.driver().navigate().to(resetUri);

            resetPasswordPage.assertCurrent();
            resetPasswordPage.changePassword(username);
            loginPage.assertCurrent();
            assertEquals("You should receive an email shortly with further instructions.", loginPage.getSuccessMessage());
            EventRepresentation resetEvent = events.poll();
            EventAssertion.assertSuccess(resetEvent)
                    .type(EventType.SEND_RESET_PASSWORD)
                    .userId(loginEvent.getUserId())
                    .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/" + realmName + "/account/")
                    .clientId("account")
                    .details(Details.USERNAME, username)
                    .details(Details.EMAIL, username)
                    .sessionId(null)
                    .withoutDetails(Details.CONSENT);

            Awaitility.await()
                    .atMost(Duration.ofSeconds(5))
                    .untilAsserted(() -> assertEquals(1, mailServer.getReceivedMessages().length));
            MimeMessage message = mailServer.getLastReceivedMessage();
            String changePasswordUrl = MailUtils.getPasswordResetEmailLink(message);
            driver.open(changePasswordUrl.trim());
            updatePasswordPage.assertCurrent();
            updatePasswordPage.changePassword("resetPassword", "resetPassword");

            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            setAuthenticatorConfigItem(BEHAVIOR, DENY_NEW_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "0");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "1");
        }
    }

    private void setAuthenticatorConfigItem(String key, String value) {
        String currentRealm = this.realmName;
        runOnServer.run(session -> {

            RealmModel realm = session.realms().getRealmByName(currentRealm);
            AuthenticationFlowModel resetPasswordFlow = realm.getResetCredentialsFlow();
            String configAlias = "user-session-limits-" + resetPasswordFlow.getId();
            AuthenticatorConfigModel configModel = realm.getAuthenticatorConfigByAlias(configAlias);
            if (configModel == null) {
                throw new RuntimeException("Config not found: " + configAlias +
                        " in flow: " + resetPasswordFlow.getAlias());
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
                    .password("password")
                    .enabled(true);

            realm.addClient(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                    .secret("password")
                    .directAccessGrantsEnabled(true)
                    .redirectUris("*");

            realm.name("session-limits-test-realm");
            realm.resetPasswordAllowed(true);
            return realm;
        }
    }
}
