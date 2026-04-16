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

import org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.AuthenticatorConfigModel;
import org.keycloak.models.RealmModel;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.BEHAVIOR;
import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION;
import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.USER_CLIENT_LIMIT;
import static org.keycloak.authentication.authenticators.sessionlimits.UserSessionLimitsAuthenticatorFactory.USER_REALM_LIMIT;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.ERROR_TO_DISPLAY;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertClientSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.assertSessionCount;
import static org.keycloak.tests.sessionlimits.UserSessionLimitsUtil.configureSessionLimits;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class UserSessionLimitsDirectGrantTest {

    @InjectRealm(config = SessionLimitsRealmConfig.class)
    ManagedRealm managedRealm;

    @InjectOAuthClient
    protected OAuthClient oauth;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    protected LoginPage loginPage;

    @InjectWebDriver
    protected ManagedWebDriver driver;

    @InjectEvents
    protected Events events;

    private String realmName;
    private String defaultRedirectUri = null;
    private static boolean flowsConfigured = false;

    private static final String username = "test-user@localhost";
    private static final String password = "password";

    @BeforeEach
    public void setUp() {
        
        driver.cookies().deleteAll();
        this.realmName = managedRealm.getName();

        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeUserSessions(realm);
        });

        if (defaultRedirectUri == null) {
            defaultRedirectUri = oauth.getRedirectUri();
        }
        oauth.redirectUri(defaultRedirectUri);

        if (!flowsConfigured) {
            setupFlows();
        }

        resetToDefaultConfig();
        events.clear();
    }

    private void setupFlows() {
        runOnServer.run(session -> {
            RealmModel realm = session.getContext().getRealm();
            session.sessions().removeUserSessions(realm);

            AuthenticationFlowModel directGrant = realm.getDirectGrantFlow();
            configureSessionLimits(realm, directGrant, UserSessionLimitsAuthenticatorFactory.DENY_NEW_SESSION, "0", "1");
         });
         flowsConfigured = true;
    }

    @Test
    public void testClientSessionCountExceededAndNewSessionDeniedDirectGrantFlow() {
        AccessTokenResponse response = oauth.doPasswordGrantRequest(username, password);
        assertEquals(200, response.getStatusCode());

        response = oauth.doPasswordGrantRequest(username, password);
        assertEquals(403, response.getStatusCode());
        assertEquals(ERROR_TO_DISPLAY, response.getError());
    }

    @Test
    public void testRealmSessionCountAndClientSessionCountExceededAndOldestClientSessionShouldBePrioritized() {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "1");

            AccessTokenResponse response =  oauth.client("direct-grant-1", password)
                    .doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response =  oauth.client("direct-grant-2", password)
                    .doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());
            awaitTwoSessionsOnePerClient();

            response =  oauth.client("direct-grant-2", password)
                    .doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());
            awaitTwoSessionsOnePerClient();
        } finally {
            resetToDefaultConfig();
        }
    }

    // intentionally removed?
    public void testRealmSessionCountAndClientSessionCountExceededAndDecreaseLimitsAfterActiveSessionsAreCreated() {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "4");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "2");

            AccessTokenResponse response =  oauth.client("direct-grant-1", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response =  oauth.client("direct-grant-2", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response =  oauth.client("direct-grant-1", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response =  oauth.client("direct-grant-2", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 4));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 2));

            response =  oauth.client("direct-grant-2", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 4));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 2));

            setAuthenticatorConfigItem(USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "1");

            response =  oauth.client("direct-grant-2", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));
        } finally {
            resetToDefaultConfig();
        }
    }

    @Test
    public void testClientSessionCountExceededAndOldestSessionRemovedDirectGrantFlow() {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "2");
            for (int i = 0; i < 2; ++i) {
                AccessTokenResponse response = oauth.doPasswordGrantRequest(username, password);
                assertEquals(200, response.getStatusCode());
            }
            runOnServer.run(assertSessionCount(realmName, username, 2));

            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "1");
            AccessTokenResponse response = oauth.doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());
            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            resetToDefaultConfig();
        }
    }

    @Test
    public void testRealmSessionCountExceededAndNewSessionDeniedDirectGrantFlow() {
        try {
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "0");
            AccessTokenResponse response = oauth.doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response = oauth.doPasswordGrantRequest(username, password);
            assertEquals(403, response.getStatusCode());
            assertEquals(ERROR_TO_DISPLAY, response.getError());
        } finally {
            resetToDefaultConfig();
        }
    }

    @Test  // Issue 46879
    public void testRealmLimitExceededWithMultipleClientsAndClientLimitHigherDirectGrantFlow() {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "3");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "10");

            AccessTokenResponse response = oauth.client("direct-grant-1", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response = oauth.client("direct-grant-1", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response = oauth.client("direct-grant-2", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 3));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));

            response = oauth.client("direct-grant-2", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 3));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }finally {
            resetToDefaultConfig();
        }
    }

    @Test
    public void testRealmSessionCountExceededAndOldestSessionRemovedDirectGrantFlow() {
        try {
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "1");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "0");
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);

            AccessTokenResponse response = oauth.doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response = oauth.doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());
            runOnServer.run(assertSessionCount(realmName, username, 1));
        } finally {
            resetToDefaultConfig();
        }
    }

    @Test
    public void testComparatorPrioritizesCurrentClientSessionsForRemoval() {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "3");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "0");

            AccessTokenResponse response = oauth.client("direct-grant-1", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());
            response = oauth.client("direct-grant-1", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response = oauth.client("direct-grant-2", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 3));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));

            // The comparator should prioritize removing a direct-grant-2 session even though direct-grant-1 sessions older
            response = oauth.client("direct-grant-2", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 3));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
        } finally {
            resetToDefaultConfig();
        }
    }

    @Test
    public void testComparatorFallsBackToAgeWhenNoCurrentClientSessions() {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "0");

            AccessTokenResponse response = oauth.client("direct-grant-1", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            try {
                Thread.sleep(1100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            response = oauth.client("direct-grant-2", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));

            // comparator should fall back to age and remove the oldest session (direct-grant-1)
            response = oauth.client("direct-grant-3", password).doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 0));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-3", 1));
        } finally {
            resetToDefaultConfig();
        }
    }

    @Test
    public void testRealmLimitExceededWithNewClientAndOldestSessionRemovedFromOtherClients() {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "2");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "2");

            AccessTokenResponse response = oauth.client("direct-grant-1", password)
                    .doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            response = oauth.client("direct-grant-2", password)
                    .doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));

            // Login to client-3 - should remove oldest session from other clients
            response = oauth.client("direct-grant-3", password)
                    .doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-3", 1));
        } finally {
            resetToDefaultConfig();
        }
    }

    @Test
    public void testRealmAndClientLimitExceededAndClientSessionRemovalSufficient() {
        try {
            setAuthenticatorConfigItem(BEHAVIOR, UserSessionLimitsAuthenticatorFactory.TERMINATE_OLDEST_SESSION);
            setAuthenticatorConfigItem(USER_REALM_LIMIT, "4");
            setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "2");

            for (int i = 0; i < 2; i++) {
                AccessTokenResponse response = oauth.client("direct-grant-1", password)
                        .doPasswordGrantRequest(username, password);
                assertEquals(200, response.getStatusCode());
            }

            for (int i = 0; i < 2; i++) {
                AccessTokenResponse response = oauth.client("direct-grant-2", password)
                        .doPasswordGrantRequest(username, password);
                assertEquals(200, response.getStatusCode());
            }

            runOnServer.run(assertSessionCount(realmName, username, 4));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 2));

            // Login to client-1 again - should remove oldest client-1 session only
            AccessTokenResponse response = oauth.client("direct-grant-1", password)
                    .doPasswordGrantRequest(username, password);
            assertEquals(200, response.getStatusCode());

            runOnServer.run(assertSessionCount(realmName, username, 4));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 2));
            runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 2));
        } finally {
            resetToDefaultConfig();
        }
    }

    private void awaitTwoSessionsOnePerClient() {
        Awaitility.await()
                .pollInterval(java.time.Duration.ofMillis(200))
                .atMost(java.time.Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    runOnServer.run(assertSessionCount(realmName, username, 2));
                    runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-1", 1));
                    runOnServer.run(assertClientSessionCount(realmName, username, "direct-grant-2", 1));
                });
    }

    private void resetToDefaultConfig() {
        setAuthenticatorConfigItem(BEHAVIOR, DENY_NEW_SESSION);
        setAuthenticatorConfigItem(USER_REALM_LIMIT, "0");
        setAuthenticatorConfigItem(USER_CLIENT_LIMIT, "1");
    }

    private void setAuthenticatorConfigItem(String key, String value) {
        String currentRealm = this.realmName;
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(currentRealm);
            AuthenticationFlowModel flow = realm.getDirectGrantFlow();
            String configAlias = "user-session-limits-" + flow.getId();
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

            realm.addClient("direct-grant-1")
                    .secret(password)
                    .directAccessGrantsEnabled(true);

            realm.addClient("direct-grant-2")
                    .secret(password)
                    .directAccessGrantsEnabled(true);

            realm.addClient("direct-grant-3")
                    .secret(password)
                    .directAccessGrantsEnabled(true);

            realm.name("session-limits-test-realm");
            return realm;
        }
    }
}
