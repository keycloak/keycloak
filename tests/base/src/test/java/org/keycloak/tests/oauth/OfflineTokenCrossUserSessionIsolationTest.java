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
package org.keycloak.tests.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Regression test for https://github.com/keycloak/keycloak/issues/46239
 *
 * Verifies that the root authentication session is removed together with the
 * online user session on the FIRST_OFFLINE_ACCESS code path, preventing a
 * second user from inheriting the first user's session via a stale
 * AUTH_SESSION_ID cookie.
 *
 * Note: this test is a true regression detector only when run with
 * {@code KC_TEST_SERVER=embedded}. In distribution mode the external
 * Infinispan cache removes the root auth session independently, so the
 * test passes regardless of the fix.
 */
@KeycloakIntegrationTest
public class OfflineTokenCrossUserSessionIsolationTest {

    @InjectRealm(config = OfflineTokenRealmConfig.class)
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectPage
    LoginPage loginPage;

    @InjectRunOnServer(permittedPackages = "org.keycloak.tests.oauth")
    RunOnServerClient runOnServer;

    public static class OfflineTokenRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.eventsEnabled(true);

            realm.addClient("offline-client")
                    .secret("secret1")
                    .redirectUris("*")
                    .directAccessGrantsEnabled(true);

            realm.addUser("test-user@localhost")
                    .name("Test", "User")
                    .email("test-user@localhost")
                    .emailVerified(true)
                    .password("password")
                    .roles("offline_access");

            realm.addUser("offline-user2@localhost")
                    .name("Offline", "User2")
                    .email("offline-user2@localhost")
                    .emailVerified(true)
                    .password("password")
                    .roles("offline_access");

            return realm;
        }
    }

    // https://github.com/keycloak/keycloak/issues/46239
    @Test
    public void offlineTokenCrossUserSessionIsolation() {
        oauth.client("offline-client", "secret1").scope(OAuth2Constants.OFFLINE_ACCESS);

        // User A logs in
        AuthorizationEndpointResponse loginResponseA = oauth.doLogin("test-user@localhost", "password");
        String codeA = loginResponseA.getCode();
        String sessionStateA = loginResponseA.getSessionState();

        // Re-create the root auth session WITH a tab to simulate production (Infinispan)
        // where the root auth session survives after the auth flow.
        // The embedded store auto-removes root sessions when the last tab is consumed,
        // so we must add a tab to keep it alive and make this test a genuine regression detector.
        final String realmName = realm.getName();
        final String sessionId = sessionStateA;
        final String clientId = "offline-client";
        runOnServer.run(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var rootAuthSession = session.authenticationSessions().getRootAuthenticationSession(testRealm, sessionId);
            if (rootAuthSession == null) {
                rootAuthSession = session.authenticationSessions().createRootAuthenticationSession(testRealm, sessionId);
            }
            var client = testRealm.getClientByClientId(clientId);
            rootAuthSession.createAuthenticationSession(client);
        });

        // Verify root auth session exists before code exchange
        Boolean rootAuthSessionExists = runOnServer.fetch(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            var ras = session.authenticationSessions().getRootAuthenticationSession(testRealm, sessionId);
            return ras != null && !ras.getAuthenticationSessions().isEmpty();
        }, Boolean.class);
        Assertions.assertTrue(rootAuthSessionExists,
                "Root auth session with tab must exist before code exchange");

        // Exchange code — FIRST_OFFLINE_ACCESS removes user session and root auth session
        AccessTokenResponse tokenResponseA = oauth.doAccessTokenRequest(codeA);
        Assertions.assertTrue(tokenResponseA.isSuccess(), "Token request for user A should succeed");
        RefreshToken offlineTokenA = oauth.parseToken(tokenResponseA.getRefreshToken(), RefreshToken.class);

        // Assert root auth session is gone
        Boolean rootAuthSessionRemoved = runOnServer.fetch(session -> {
            var testRealm = session.realms().getRealmByName(realmName);
            return session.authenticationSessions().getRootAuthenticationSession(testRealm, sessionId) == null;
        }, Boolean.class);
        Assertions.assertTrue(rootAuthSessionRemoved,
                "Root auth session must be removed after first offline_access token exchange");

        // User B logs in from the same browser (stale AUTH_SESSION_ID cookie)
        AuthorizationEndpointResponse loginResponseB = oauth.doLogin("offline-user2@localhost", "password");
        String codeB = loginResponseB.getCode();

        AccessTokenResponse tokenResponseB = oauth.doAccessTokenRequest(codeB);
        Assertions.assertTrue(tokenResponseB.isSuccess(), "Token request for user B should succeed");
        RefreshToken offlineTokenB = oauth.parseToken(tokenResponseB.getRefreshToken(), RefreshToken.class);

        // User B must not inherit User A's session
        Assertions.assertNotEquals(offlineTokenA.getSessionId(), offlineTokenB.getSessionId(),
                "User B must get a different offline session than User A");

        // Refresh must return User B's identity
        AccessTokenResponse refreshResponseB = oauth.doRefreshTokenRequest(tokenResponseB.getRefreshToken());
        Assertions.assertTrue(refreshResponseB.isSuccess(), "Refresh for user B should succeed");
        AccessToken refreshedTokenB = oauth.verifyToken(refreshResponseB.getAccessToken(), AccessToken.class);

        String userBId = realm.admin().users().search("offline-user2@localhost", true).get(0).getId();
        Assertions.assertEquals(userBId, refreshedTokenB.getSubject(),
                "Refreshed token must belong to user B");
    }
}
