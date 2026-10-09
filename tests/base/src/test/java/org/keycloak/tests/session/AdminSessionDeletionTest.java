/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.tests.session;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.AuthenticationSessionManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.admin.AdminClientFactory;
import org.keycloak.testframework.annotations.InjectAdminClientFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class AdminSessionDeletionTest {

    private static final String BACKEND = "backend";
    private static final String SECRET = "secret";
    private static final String USERNAME = "alice";
    private static final String PASSWORD = "password";

    @InjectRealm(config = TestRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @InjectOAuthClient(config = PublicClientConfig.class)
    OAuthClient oauth;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectPage
    LoginPage loginPage;

    @InjectAdminClientFactory
    AdminClientFactory adminClientFactory;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    public void deleteSessionWithUnfinishedLoginTab(boolean useRefreshTokens) {
        // The default client-credentials grant has no sid. Also cover the stateful variant.
        var backend = realm.admin().clients().findByClientId(BACKEND).get(0);
        backend.getAttributes().put("client_credentials.use_refresh_token", Boolean.toString(useRefreshTokens));
        realm.admin().clients().get(backend.getId()).update(backend);

        try (Keycloak admin = adminClientFactory.create()
                .realm(realm.getName()).clientId(BACKEND).clientSecret(SECRET)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS).build()) {
            AccessToken adminToken = oauth.parseToken(admin.tokenManager().getAccessTokenString(), AccessToken.class);
            assertEquals(useRefreshTokens, adminToken.getSessionId() != null);

            oauth.openLoginForm();
            loginPage.assertCurrent();
            driver.tabs().newTab("about:blank");
            oauth.openLoginForm();
            loginPage.assertCurrent();
            loginPage.fillLogin(USERNAME, PASSWORD);
            loginPage.submit();
            String oldSid = currentSessionId();
            assertNotNull(oldSid);

            // The other tab must keep this root alive; without it the regression is not exercised.
            runOnServer.run(session -> {
                RealmModel currentRealm = session.getContext().getRealm();
                RootAuthenticationSessionModel root = session.authenticationSessions()
                        .getRootAuthenticationSession(currentRealm, oldSid);
                assertNotNull(root);
                assertEquals(1, root.getAuthenticationSessions().size());
            });

            admin.realm(realm.getName()).deleteSession(oldSid, false);

            runOnServer.run(session -> {
                RealmModel currentRealm = session.getContext().getRealm();
                assertNull(session.sessions().getUserSession(currentRealm, oldSid));
                assertNull(session.authenticationSessions().getRootAuthenticationSession(currentRealm, oldSid),
                        "The deleted user's root authentication session must also be removed");
            });

            // Keep all browser cookies, including AUTH_SESSION_ID, across deletion and the next login.
            oauth.doLogin(USERNAME, PASSWORD);
            String newSid = currentSessionId();
            assertNotNull(newSid);
            assertNotEquals(oldSid, newSid);
        }
    }

    @Test
    public void logoutSessionForAnotherUserSessionRemainsIsolated() {
        // Preserve the protection introduced by #49949 for nested logouts (#32124).
        runOnServer.run(session -> {
            RealmModel currentRealm = session.getContext().getRealm();
            ClientModel client = currentRealm.getClientByClientId(BACKEND);
            UserModel user = session.users().getUserByUsername(currentRealm, USERNAME);
            UserSessionModel first = session.sessions().createUserSession(null, currentRealm, user,
                    USERNAME, "127.0.0.1", "form", false, null, null,
                    UserSessionModel.SessionPersistenceState.PERSISTENT);
            UserSessionModel second = session.sessions().createUserSession(null, currentRealm, user,
                    USERNAME, "127.0.0.1", "form", false, null, null,
                    UserSessionModel.SessionPersistenceState.PERSISTENT);
            session.getContext().setClient(client);
            session.getContext().setAuthenticationSession(null);
            AuthenticationSessionManager manager = new AuthenticationSessionManager(session);

            AuthenticationSessionModel firstLogout = AuthenticationManager.createOrJoinLogoutSession(
                    session, currentRealm, manager, first, false, true);
            assertEquals(first.getId(), firstLogout.getParentSession().getId());

            AuthenticationSessionModel secondLogout = AuthenticationManager.createOrJoinLogoutSession(
                    session, currentRealm, manager, second, false, true);
            assertNotEquals(firstLogout.getParentSession().getId(), secondLogout.getParentSession().getId());
            assertNotEquals(second.getId(), secondLogout.getParentSession().getId());
            assertEquals(AuthenticationSessionModel.Action.LOGGING_OUT.name(), secondLogout.getAction());

            session.authenticationSessions().removeRootAuthenticationSession(currentRealm, secondLogout.getParentSession());
            assertNotNull(session.authenticationSessions().getRootAuthenticationSession(currentRealm, first.getId()));
            assertEquals(AuthenticationSessionModel.Action.LOGGING_OUT.name(), firstLogout.getAction());
        });
    }

    private String currentSessionId() {
        var response = oauth.parseLoginResponse();
        assertTrue(response.isSuccess());
        AccessTokenResponse tokens = oauth.doAccessTokenRequest(response.getCode());
        assertEquals(200, tokens.getStatusCode());
        return oauth.parseToken(tokens.getAccessToken(), AccessToken.class).getSessionId();
    }

    public static class PublicClientConfig implements ClientConfig {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("app").publicClient(true);
        }
    }

    public static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.clients(ClientBuilder.create(BACKEND).secret(SECRET).serviceAccountsEnabled(true)
                            .attribute("client_credentials.use_refresh_token", "false"))
                    .users(UserBuilder.create(USERNAME).password(PASSWORD)
                                    .name("Alice", "Tester").email("alice@example.org").emailVerified(true),
                            UserBuilder.create(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + BACKEND)
                                    .serviceAccountId(BACKEND)
                                    .clientRoles(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.MANAGE_USERS));
        }
    }
}
