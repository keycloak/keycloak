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
package org.keycloak.tests.forms;

import java.nio.charset.StandardCharsets;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.cookie.CookieType;
import org.keycloak.events.Errors;
import org.keycloak.http.simple.SimpleHttp;
import org.keycloak.http.simple.SimpleHttpResponse;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.services.managers.UserSessionManager;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectSimpleHttp;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

/**
 * Regression tests for CVE-2026-107124.
 *
 * <h2>Attack scenarios</h2>
 *
 * <h3>Session fixation via stale {@code id_token_hint}</h3>
 * <p>
 * An attacker holds a stale ID token {@code IDT1(iat=T, sid=S1)} and navigates
 * the victim's browser (fresh, no cookies) to
 * {@code /logout?id_token_hint=IDT1}.  Without the fix,
 * {@code createOrJoinLogoutSession()} called {@code setAuthSessionCookie(S1)} —
 * planting {@code Set-Cookie: AUTH_SESSION_ID=S1} even on the error path.  When
 * the victim subsequently logs in their login is fixated onto root session S1,
 * enabling account takeover.
 *
 * <h2>Fix</h2>
 * <ul>
 *   <li>When the user session is resolved solely from {@code id_token_hint} (no identity
 *       cookie), {@code LogoutEndpoint.logout()} validates the hint against the user session
 *       before creating any logout (root auth) session or writing any {@code AUTH_SESSION_ID}
 *       cookie.</li>
 *   <li>The logout root auth session id is never derived from the hint's session id, and the
 *       {@code AUTH_SESSION_ID} cookie is only written when the confirmation screen is shown.</li>
 *   <li>{@code doBrowserLogout} removes the logout root auth session on every failure path.</li>
 *   <li>Wherever a user session is removed (logout, token revocation, expired identity-cookie
 *       sessions, account client revocation, offline session removal, etc.) the root auth
 *       session sharing its id is removed too, so the id cannot become free for reuse as a user
 *       session id while a root auth session with that id is still alive.</li>
 * </ul>
 */
@KeycloakIntegrationTest
public class StaleAuthSessionLogoutTest {

    @InjectRealm(config = StaleAuthSessionRealmConfig.class)
    ManagedRealm realm;

    @InjectSimpleHttp
    SimpleHttp simpleHttp;

    // ── Browser A: attacker ───────────────────────────────────────────────────
    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    // ── Browser B: victim (separate cookie jar / web driver) ─────────────────
    @InjectOAuthClient(ref = "oauth-victim", webDriverRef = "driver-victim", lifecycle = LifeCycle.METHOD)
    OAuthClient oauthVictim;

    @InjectWebDriver(ref = "driver-victim", lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driverVictim;

    @InjectPage(webDriverRef = "driver-victim")
    LogoutConfirmPage victimLogoutConfirmPage;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.keycloak.admin"})
    RunOnServerClient runOnServer;

    @InjectEvents
    Events events;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectUser(ref = "user-a", config = UserAConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser userA;

    @InjectUser(ref = "user-b", config = UserBConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedUser userB;

    // -------------------------------------------------------------------------
    // Realm / user configuration
    // -------------------------------------------------------------------------

    public static class StaleAuthSessionRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm
                    .eventsEnabled(true)
                    .clients(ClientBuilder.create("stale-session-second-client")
                            .directAccessGrantsEnabled()
                            .secret("secret"));
        }
    }

    public static class UserAConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("stale-session-attacker")
                    .name("Stale", "Attacker")
                    .password("password-a")
                    .email("stale-session-attacker@localhost")
                    .emailVerified(true);
        }
    }

    public static class UserBConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("stale-session-victim")
                    .name("Stale", "Victim")
                    .password("password-b")
                    .email("stale-session-victim@localhost")
                    .emailVerified(true);
        }
    }

    // -------------------------------------------------------------------------
    // Test: End-to-end session-fixation / account-takeover
    // -------------------------------------------------------------------------

    /**
     * End-to-end reproduction of the CVE-2026-107124 session-fixation attack.
     *
     * <ol>
     *   <li>Attacker logs in → {@code IDT1(iat=T, sid=S1)}.</li>
     *   <li>Server time is advanced 100 s and the existing user session is restarted in-place,
     *       so its {@code started} timestamp becomes {@code T+100}, making {@code IDT1} genuinely stale
     *       ({@code iat+1 < started}).  The session id {@code S1} is kept so the stale
     *       token still references a live session — the condition the attacker exploits.</li>
     *   <li>A tab is parked in root {@code S1} (keeps it alive in auth-session storage).</li>
     *   <li>Victim's browser (separate, empty cookie jar) navigates to
     *       {@code /logout?id_token_hint=IDT1}.
     *       <ul>
     *         <li>Without fix: {@code createOrJoinLogoutSession} calls
     *             {@code setAuthSessionCookie(S1)} even on the error path.
     *             Victim's browser is fixated onto {@code S1}.</li>
     *         <li>With fix: the stale {@code id_token_hint} is rejected before
     *             {@code createOrJoinLogoutSession} runs, so no cookie is planted.</li>
     *       </ul></li>
     *   <li>User session {@code S1} is deleted (root {@code S1} kept alive by the
     *       parked tab, so the id is free for reuse as a user session id).</li>
     *   <li>Victim logs in (browser B) with victim credentials.</li>
     *   <li><b>Key assertion</b>: the victim's user session id must differ from
     *       {@code S1}.  Without the fix, the victim's browser carries
     *       {@code AUTH_SESSION_ID=S1}, so its login lands in root {@code S1} and
     *       the resulting user session id equals {@code S1}.</li>
     * </ol>
     *
     * <p>Assertions:
     * <ul>
     *   <li>The poisoned logout must produce a {@code LOGOUT_ERROR / invalid_token} event.</li>
     *   <li>After the victim logs in, the victim's session id must differ from {@code S1}.</li>
     * </ul>
     */
    @Test
    void sessionFixationViaStaleLogoutPlantsAttackerCookie() {

        // ── Step 1: Attacker logs in (browser A) ─────────────────────────────
        AuthorizationEndpointResponse attackerLogin1 = oauth.doLogin(userA.getUsername(), userA.getPassword());
        Assertions.assertTrue(attackerLogin1.isSuccess(), "[1] Attacker first login must succeed");

        AccessTokenResponse attackerTokens1 = oauth.doAccessTokenRequest(attackerLogin1.getCode());
        Assertions.assertTrue(attackerTokens1.isSuccess(), "[1] Attacker first token exchange must succeed");

        final String staleIdToken  = attackerTokens1.getIdToken();
        final String sessionIdS1   = oauth.verifyIDToken(staleIdToken).getSessionId();

        // ── Step 2: Make IDT1 stale by advancing started to T+100 ────────────
        // Advance the clock 100 s, then restart the existing session in-place so that
        // its started timestamp advances and iat(IDT1)+1 < started(S1).
        //
        // We restart the session in-place rather than removing and re-creating it
        // because remove-then-create in one transaction triggers a JPA conflict: the
        // persister sees the new session (started=T+100) conflict with the recently
        // removed one (started=T) and throws "Session has already aged".
        timeOffSet.set(100);
        final String realmName = realm.getName();
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, sessionIdS1);
            userSession.restartSession(realmModel, userSession.getUser(), userSession.getLoginUsername(),
                    userSession.getIpAddress(), userSession.getAuthMethod(), userSession.isRememberMe(),
                    userSession.getBrokerSessionId(), userSession.getBrokerUserId());
        });

        // ── Step 3: Park a tab in root S1 ────────────────────────────────────
        // Keeps the root auth session alive so the id is free as a user session id
        // once the user session is removed in step 5.
        parkRootAuthenticationSession(sessionIdS1);

        // ── Step 4: Victim's browser navigates to the poisoned logout URL ─────
        // The victim browser (browser B) has no Keycloak cookies.
        // With the fix: stale id_token_hint is rejected before createOrJoinLogoutSession
        // runs → no AUTH_SESSION_ID cookie is planted in the victim's browser.
        events.clear();
        oauthVictim.logoutForm()
                .idTokenHint(staleIdToken)
                .open();

        // The poisoned logout must produce LOGOUT_ERROR / invalid_token.
        EventRepresentation logoutErrorEvent = events.poll();
        logoutErrorEvent.setClientId(null); // expectLogoutError asserts clientId == null
        EventAssertion.expectLogoutError(logoutErrorEvent)
                .error(Errors.INVALID_TOKEN);

        // ── Step 5: Remove the user session so S1 is free for reuse ──────────
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserSessionModel s1 = session.sessions().getUserSession(realmModel, sessionIdS1);
            if (s1 != null) {
                session.sessions().removeUserSession(realmModel, s1);
            }
        });
        Assertions.assertTrue(rootAuthenticationSessionExists(sessionIdS1),
                "[5] Root auth session S1 must still be alive for the test to be meaningful");

        // ── Step 6: Victim logs in with victim credentials (browser B) ────────
        events.clear();
        AuthorizationEndpointResponse victimLogin = oauthVictim.doLogin(
                userB.getUsername(), userB.getPassword());
        Assertions.assertTrue(victimLogin.isSuccess(), "[6] Victim login must succeed");

        AccessTokenResponse victimTokens = oauthVictim.doAccessTokenRequest(victimLogin.getCode());
        Assertions.assertTrue(victimTokens.isSuccess(), "[6] Victim token exchange must succeed");

        // ── Step 7: Key assertion – victim's session must not be fixated onto S1 ─
        // Without the fix: AUTH_SESSION_ID=S1 was planted in the victim's browser at
        // step 4, so the victim's login used root S1 and the resulting user session id
        // equals S1.  With the fix: no cookie was planted, the victim gets a fresh root
        // session, and victimSessionId != sessionIdS1.
        String victimSessionId = oauthVictim.verifyIDToken(victimTokens.getIdToken()).getSessionId();
        Assertions.assertNotEquals(sessionIdS1, victimSessionId,
                "[7] EXPLOIT SUCCEEDED: victim's user session id equals the attacker's root "
                        + "session id S1 (" + sessionIdS1 + "). The poisoned logout URL planted "
                        + "AUTH_SESSION_ID=S1 in the victim's browser, fixating the victim's "
                        + "login onto the attacker's session.");
    }

    // -------------------------------------------------------------------------
    // Test: failed logout removes only its own tab, not the whole root
    // -------------------------------------------------------------------------

    /**
     * When {@code doBrowserLogout} fails, the logout tab must be removed from the root auth session
     * it joined, but the root itself must only go away if no other tabs remain.
     *
     * <p>The victim's browser has no identity cookie but has a root auth session (R) from a login
     * form opened in another tab.  The logout confirmation screen (forced because the hint's client no
     * longer exists) joins R through the {@code AUTH_SESSION_ID} cookie.  The hint's user session no
     * longer exists, so confirming the logout fails in {@code doBrowserLogout}.
     * <ul>
     *   <li>Removing the whole root would lose the login tab in the other browser tab.</li>
     *   <li>Removing nothing would leave the logout tab behind (checked by the failure path itself).</li>
     * </ul>
     * Removing only the logout tab id leaves exactly the login tab in R.
     */
    @Test
    void failedLogoutRemovesOnlyLogoutTabFromRoot() {
        final String doomedClientId = "stale-session-doomed-client-tab";
        realm.cleanup().add(r -> r.clients().findByClientId(doomedClientId)
                .forEach(c -> r.clients().get(c.getId()).remove()));

        ClientRepresentation doomedClient = ClientBuilder.create(doomedClientId)
                .publicClient()
                .redirectUris(oauth.config().getRedirectUri())
                .build();
        final String doomedClientUuid = ApiUtil.getCreatedId(realm.admin().clients().create(doomedClient));

        // ── Step 1: Obtain a validly signed id token, then remove its client and user session ──
        oauth.client(doomedClientId);
        AuthorizationEndpointResponse login = oauth.doLogin(userA.getUsername(), userA.getPassword());
        Assertions.assertTrue(login.isSuccess(), "[1] Login must succeed");
        AccessTokenResponse tokens = oauth.doAccessTokenRequest(login.getCode());
        Assertions.assertTrue(tokens.isSuccess(), "[1] Token exchange must succeed");
        final String hintIdToken = tokens.getIdToken();
        final String sessionId = oauth.verifyIDToken(hintIdToken).getSessionId();

        realm.admin().clients().get(doomedClientUuid).remove();
        realm.admin().deleteSession(sessionId, false);
        Assertions.assertTrue(userA.admin().getUserSessions().isEmpty(), "[1] User session must be gone");

        // ── Step 2: Victim's browser has a login tab open in root R (no identity cookie) ──
        oauthVictim.openLoginForm();
        final String rootId = authSessionIdFromCookie(driverVictim);
        Assertions.assertEquals(1, rootAuthenticationSessionTabCount(rootId),
                "[2] Root R must contain only the login tab");

        // ── Step 3: Logout confirmation joins root R via the AUTH_SESSION_ID cookie ──
        oauthVictim.logoutForm()
                .idTokenHint(hintIdToken)
                .open();
        victimLogoutConfirmPage.assertCurrent();
        Assertions.assertEquals(rootId, authSessionIdFromCookie(driverVictim),
                "[3] The logout session must join the existing root R");
        Assertions.assertEquals(2, rootAuthenticationSessionTabCount(rootId),
                "[3] Root R must contain the login tab and the logout tab");

        // ── Step 4: Confirming fails in doBrowserLogout since the hint's session is gone ──
        events.clear();
        victimLogoutConfirmPage.confirmLogout();
        EventRepresentation logoutErrorEvent = events.poll();
        Assertions.assertEquals(Errors.SESSION_EXPIRED, logoutErrorEvent.getError());

        // ── Step 5: Key assertions ───────────────────────────────────────────
        Assertions.assertTrue(rootAuthenticationSessionExists(rootId),
                "[5] Root R must not be removed unconditionally: it still holds the login tab");
        Assertions.assertEquals(1, rootAuthenticationSessionTabCount(rootId),
                "[5] Only the logout tab must have been removed from root R");
    }

    // -------------------------------------------------------------------------
    // Test: revoking offline session while online session coexists must preserve root
    // -------------------------------------------------------------------------

    /**
     * Offline sessions share the same ID as the online session they were created from (the
     * provider copies the online ID).  When {@link UserSessionManager#revokeOfflineUserSession}
     * is called it must NOT remove the root authentication session if an online session with
     * the same ID still exists — doing so would break in-progress browser tabs backed by that
     * online session.
     *
     * <p>Steps:
     * <ol>
     *   <li>Create an online user session S1 on the server.</li>
     *   <li>Create an offline session with the same ID S1 (as the provider does when promoting
     *       an online session to offline access).</li>
     *   <li>Park a root authentication session at S1 to simulate an open browser tab.</li>
     *   <li>Revoke the offline session via {@link UserSessionManager#revokeOfflineUserSession}.</li>
     *   <li><b>Key assertion</b>: the root auth session for S1 must still exist because the
     *       online session S1 is still alive.</li>
     *   <li>Remove the online session and its root.</li>
     *   <li>Assert that the root auth session is now gone.</li>
     * </ol>
     */
    @Test
    void revokeOfflineUserSessionPreservesRootWhenOnlineSessionCoexists() {
        final String realmName = realm.getName();
        final String username = userA.getUsername();

        // ── Step 1+2: Create coexisting online and offline sessions with the same ID ──
        final String sessionId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realmModel, username);
            ClientModel client = realmModel.getClientByClientId("test-app");

            UserSessionModel onlineSession = session.sessions().createUserSession(
                    null, realmModel, user, username, "127.0.0.1",
                    OIDCLoginProtocol.LOGIN_PROTOCOL, false, null, null,
                    UserSessionModel.SessionPersistenceState.PERSISTENT);
            AuthenticatedClientSessionModel clientSession = session.sessions().createClientSession(realmModel, client, onlineSession);

            session.sessions().createOfflineUserSession(onlineSession);
            UserSessionModel offlineSession = session.sessions().getOfflineUserSession(realmModel, onlineSession.getId());
            session.sessions().createOfflineClientSession(clientSession, offlineSession);

            return onlineSession.getId();
        }, String.class);

        // ── Step 3: Park a root auth session at S1 ───────────────────────────
        parkRootAuthenticationSession(sessionId);
        Assertions.assertTrue(rootAuthenticationSessionExists(sessionId),
                "[3] Root auth session must exist before offline session revocation");

        // ── Step 4: Revoke the offline session ───────────────────────────────
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserSessionModel offlineSession = session.sessions().getOfflineUserSession(realmModel, sessionId);
            new UserSessionManager(session).revokeOfflineUserSession(offlineSession);
        });

        // ── Step 5: Key assertion – root must still exist ────────────────────
        Assertions.assertTrue(rootAuthenticationSessionExists(sessionId),
                "REGRESSION: revokeOfflineUserSession removed the root auth session even though "
                        + "an online session with the same ID (" + sessionId + ") was still alive. "
                        + "This would disrupt in-progress browser tabs backed by that online session "
                        + "and create a precondition for session-fixation attacks.");

        // ── Step 6+7: Remove the online session and verify root is gone ───────
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserSessionModel onlineSession = session.sessions().getUserSession(realmModel, sessionId);
            session.sessions().removeUserSession(realmModel, onlineSession);
            session.authenticationSessions().removeRootAuthenticationSession(realmModel, sessionId);
        });
        Assertions.assertFalse(rootAuthenticationSessionExists(sessionId),
                "[7] Root auth session must be gone after the online session is also removed");
    }

    /**
     * Same coexistence guard exercised via the account REST endpoint
     * {@code DELETE /account/applications/{clientId}/sessions}.
     *
     * <p>When a client is revoked and the offline session for that client becomes empty
     * while an online session with the same ID still has other clients attached, the root
     * authentication session must NOT be removed.
     *
     * <p>Steps:
     * <ol>
     *   <li>Create an online session S1 with two clients: {@code test-app} and
     *       {@code stale-session-second-client}.</li>
     *   <li>Create an offline session with the same ID S1 that has only
     *       {@code stale-session-second-client}.</li>
     *   <li>Park a root authentication session at S1.</li>
     *   <li>Revoke {@code stale-session-second-client} via the account REST endpoint.</li>
     *   <li><b>Key assertion</b>: root S1 must still exist because the online session still
     *       has {@code test-app} attached.</li>
     * </ol>
     */
    @Test
    void revokeApplicationSessionPreservesRootWhenOnlineSessionCoexists() throws Exception {
        final String realmName = realm.getName();
        final String username = userA.getUsername();

        // ── Step 1+2: Create the coexistence state on the server ─────────────
        final String sessionId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserModel user = session.users().getUserByUsername(realmModel, username);
            ClientModel testApp = realmModel.getClientByClientId("test-app");
            ClientModel secondClient = realmModel.getClientByClientId("stale-session-second-client");

            // Online session with both clients
            UserSessionModel onlineSession = session.sessions().createUserSession(
                    null, realmModel, user, username, "127.0.0.1",
                    OIDCLoginProtocol.LOGIN_PROTOCOL, false, null, null,
                    UserSessionModel.SessionPersistenceState.PERSISTENT);
            session.sessions().createClientSession(realmModel, testApp, onlineSession);
            AuthenticatedClientSessionModel cs2 = session.sessions().createClientSession(realmModel, secondClient, onlineSession);

            // Offline session with same ID, only stale-session-second-client
            session.sessions().createOfflineUserSession(onlineSession);
            UserSessionModel offlineSession = session.sessions().getOfflineUserSession(realmModel, onlineSession.getId());
            session.sessions().createOfflineClientSession(cs2, offlineSession);

            return onlineSession.getId();
        }, String.class);

        // ── Step 3: Park a root auth session at S1 ───────────────────────────
        parkRootAuthenticationSession(sessionId);
        Assertions.assertTrue(rootAuthenticationSessionExists(sessionId),
                "[3] Root auth session must exist before revocation");

        // ── Step 4: Revoke stale-session-second-client via the account REST endpoint ──
        // Obtain an account-management token via the real auth endpoint so that the
        // security context is wired up correctly for the account REST resource.
        oauth.scope(null);
        String accountToken = oauth.client("test-app", "test-secret")
                .doPasswordGrantRequest(userA.getUsername(), userA.getPassword())
                .getAccessToken();

        String revokeUrl = realm.getBaseUrl() + "/account/applications/stale-session-second-client/sessions";
        try (SimpleHttpResponse response = simpleHttp.doDelete(revokeUrl)
                .header("Accept", "application/json")
                .auth(accountToken)
                .asResponse()) {
            Assertions.assertEquals(204, response.getStatus());
        }

        // ── Step 5: Key assertion – root must still exist ────────────────────
        Assertions.assertTrue(rootAuthenticationSessionExists(sessionId),
                "REGRESSION: revoking the offline application session removed the root auth "
                        + "session even though an online session with the same ID (" + sessionId + ") "
                        + "was still alive (it still had test-app attached). This would disrupt "
                        + "in-progress browser tabs and create a precondition for session-fixation attacks.");

        // Cleanup
        realm.admin().logoutAll();
    }


    // -------------------------------------------------------------------------
    // Test: revoking a refresh token must not leave the root auth session behind
    // -------------------------------------------------------------------------

    /**
     * Revoking the last refresh token of a user session removes the user session. The root
     * authentication session sharing its id must be removed too, otherwise the id is free as
     * a user session id while a root session with that id is still alive — the precondition
     * for the session-fixation attack.
     */
    @Test
    void revokeTokenRemovesRootAuthenticationSession() {
        AuthorizationEndpointResponse login = oauth.doLogin(userA.getUsername(), userA.getPassword());
        Assertions.assertTrue(login.isSuccess(), "Login must succeed");
        AccessTokenResponse tokens = oauth.doAccessTokenRequest(login.getCode());
        Assertions.assertTrue(tokens.isSuccess(), "Token exchange must succeed");
        final String sessionId = oauth.verifyIDToken(tokens.getIdToken()).getSessionId();

        parkRootAuthenticationSession(sessionId);
        Assertions.assertTrue(rootAuthenticationSessionExists(sessionId), "Root auth session must exist before revoke");

        Assertions.assertEquals(200, oauth.doTokenRevoke(tokens.getRefreshToken()).getStatusCode());

        Assertions.assertTrue(userA.admin().getUserSessions().isEmpty(), "User session must be removed");
        Assertions.assertFalse(rootAuthenticationSessionExists(sessionId),
                "Root auth session with the same id as the removed user session must be removed too");
    }

    /**
     * Same as {@link #revokeTokenRemovesRootAuthenticationSession()} for an offline token. Because
     * the login only requested offline access, the online session is removed when the offline
     * session is created ({@code FIRST_OFFLINE_ACCESS}), so only the root session needs cleaning up
     * when the offline session loses its last client session.
     */
    @Test
    void revokeOfflineTokenRemovesRootAuthenticationSession() {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AuthorizationEndpointResponse login = oauth.doLogin(userA.getUsername(), userA.getPassword());
        Assertions.assertTrue(login.isSuccess(), "Login must succeed");
        AccessTokenResponse tokens = oauth.doAccessTokenRequest(login.getCode());
        Assertions.assertTrue(tokens.isSuccess(), "Token exchange must succeed");
        final String sessionId = oauth.parseRefreshToken(tokens.getRefreshToken()).getSessionId();

        parkRootAuthenticationSession(sessionId);
        Assertions.assertTrue(rootAuthenticationSessionExists(sessionId), "Root auth session must exist before revoke");

        Assertions.assertEquals(200, oauth.doTokenRevoke(tokens.getRefreshToken()).getStatusCode());

        Assertions.assertFalse(rootAuthenticationSessionExists(sessionId),
                "Root auth session with the same id as the removed offline session must be removed too");
    }

    // -------------------------------------------------------------------------
    // Test: expired session removes root auth session (identity-cookie path)
    // -------------------------------------------------------------------------

    /**
     * When the identity cookie references a user session that is still present in the store but
     * has exceeded its SSO idle timeout, {@code authenticateIdentityCookie} invokes
     * {@code invalidUserSessionCallback}.  The callback backchannel-logs out the session in an
     * inner transaction and then removes it from the outer session.  Without the fix, the root
     * authentication session sharing the same id was <em>not</em> removed on the outer session,
     * so the id became free for reuse as a user session id while a root auth session with that id
     * was still alive — the precondition for the session-fixation attack.
     *
     * <p>Steps:
     * <ol>
     *   <li>Attacker logs in via browser → gets user session {@code S1} and an identity cookie.</li>
     *   <li>A tab is parked in root {@code S1} to keep the root auth session alive
     *       independently of the user session.</li>
     *   <li>The SSO idle timeout is set to 1 s and the clock is advanced past it, so
     *       {@code isSessionValid} returns {@code false} while the session record still
     *       exists in the store (the store has not yet garbage-collected it).</li>
     *   <li>The attacker's browser visits the authorization endpoint again.  Because the
     *       identity cookie is still present, {@code authenticateIdentityCookie} is called,
     *       the expired session is detected, and {@code invalidUserSessionCallback} fires.</li>
     *   <li><b>Key assertion</b>: the root auth session for {@code S1} must have been removed
     *       by the callback.  Without the fix it survived, so a later login could reuse the id
     *       for a new user session.</li>
     * </ol>
     */
    @Test
    void expiredSessionRemovesRootAuthenticationSession() {
        // ── Step 1: Log in (browser A) ────────────────────────────────────────
        AuthorizationEndpointResponse login = oauth.doLogin(userA.getUsername(), userA.getPassword());
        Assertions.assertTrue(login.isSuccess(), "[1] Login must succeed");
        AccessTokenResponse tokens = oauth.doAccessTokenRequest(login.getCode());
        Assertions.assertTrue(tokens.isSuccess(), "[1] Token exchange must succeed");
        final String sessionId = oauth.verifyIDToken(tokens.getIdToken()).getSessionId();

        // ── Step 2: Park a tab in root S1 ────────────────────────────────────
        parkRootAuthenticationSession(sessionId);
        Assertions.assertTrue(rootAuthenticationSessionExists(sessionId),
                "[2] Root auth session must exist before session expiry");

        // ── Step 3: Make the user session appear expired ───────────────────
        // Set the realm SSO idle timeout to 1 s, then advance the clock by enough
        // that isSessionValid() returns false while the session entity remains in
        // the store (the store's own expiry window is wider than the realm setting).
        realm.updateWithCleanup(r -> r.ssoSessionIdleTimeout(1));
        timeOffSet.set(100);

        // ── Step 4: Trigger authenticateIdentityCookie via the auth endpoint ──
        // The attacker's browser still carries the identity cookie for S1.
        // The identity cookie is valid JWT-wise (checkActive=false for cookie),
        // but the user session is idle-expired → invalidUserSessionCallback fires.
        oauth.openLoginForm();

        // ── Step 5: Key assertion ─────────────────────────────────────────────
        Assertions.assertFalse(rootAuthenticationSessionExists(sessionId),
                "EXPLOIT PRECONDITION: root auth session with the same id as the expired user "
                        + "session S1 (" + sessionId + ") was not removed when the session expired. "
                        + "An attacker keeping a tab open in root S1 can have that id reused for a new "
                        + "user session once the expired one is gone, enabling session fixation.");

        // ── Step 6: The login that triggered the cleanup must still be completable ──
        // The authorization endpoint added the login tab to the cookie's root S1, which the cleanup removed. The
        // flow has to be restarted in a fresh root, otherwise the form shown here refers to a deleted
        // authentication session and submitting the credentials fails.
        oauth.fillLoginForm(userA.getUsername(), userA.getPassword());
        AuthorizationEndpointResponse secondLogin = oauth.parseLoginResponse();
        Assertions.assertTrue(secondLogin.isSuccess(),
                "[6] Submitting credentials after the expired session was cleaned up must complete the login");
        AccessTokenResponse secondTokens = oauth.doAccessTokenRequest(secondLogin.getCode());
        Assertions.assertTrue(secondTokens.isSuccess(), "[6] Token exchange must succeed");
        Assertions.assertNotEquals(sessionId, oauth.verifyIDToken(secondTokens.getIdToken()).getSessionId(),
                "[6] The new login must not reuse the id of the expired user session");
    }

    // -------------------------------------------------------------------------
    // Test: confirmation screen must not plant the hint's session id as cookie
    // -------------------------------------------------------------------------

    /**
     * Verifies that when the logout confirmation screen is shown for a session resolved solely
     * from {@code id_token_hint} (no browser identity cookie), the {@code AUTH_SESSION_ID}
     * cookie written for the confirmation POST-back does not carry the hint's session id.
     *
     * <p>The scenario forces the confirmation screen by supplying a {@code client_id} that
     * matches the hint's {@code azp} but no longer exists in the realm.  The victim's browser
     * has no identity cookie, so the user session is resolved only from the hint.  Without the
     * fix, {@code createOrJoinLogoutSession} used the hint's session id as the root auth
     * session id and therefore as the {@code AUTH_SESSION_ID} cookie value, enabling the
     * attacker to fixate the victim's next login onto that session.
     */
    @Test
    void confirmationScreenForHintOnlySessionDoesNotPlantHintSessionIdCookie() {
        final String doomedClientId = "stale-session-doomed-client";
        realm.cleanup().add(r -> r.clients().findByClientId(doomedClientId)
                .forEach(c -> r.clients().get(c.getId()).remove()));

        ClientRepresentation doomedClient = ClientBuilder.create(doomedClientId)
                .publicClient()
                .redirectUris(oauth.config().getRedirectUri())
                .build();
        final String doomedClientUuid = ApiUtil.getCreatedId(realm.admin().clients().create(doomedClient));

        // ── Step 1: Attacker logs in, then obtains a token for the doomed client ──
        AuthorizationEndpointResponse attackerLogin = oauth.doLogin(userA.getUsername(), userA.getPassword());
        Assertions.assertTrue(attackerLogin.isSuccess(), "[1] Attacker login must succeed");

        oauth.client(doomedClientId);
        oauth.openLoginForm();
        AuthorizationEndpointResponse doomedLogin = oauth.parseLoginResponse();
        Assertions.assertTrue(doomedLogin.isSuccess(), "[2] SSO login for the doomed client must succeed");
        AccessTokenResponse doomedTokens = oauth.doAccessTokenRequest(doomedLogin.getCode());
        Assertions.assertTrue(doomedTokens.isSuccess(), "[2] Token exchange for the doomed client must succeed");

        final String hintIdToken = doomedTokens.getIdToken();
        final String sessionIdS1 = oauth.verifyIDToken(hintIdToken).getSessionId();

        // ── Step 2: Remove the doomed client ─────────────────────────────────
        // The ID token keeps its valid realm signature; S1 stays alive.
        realm.admin().clients().get(doomedClientUuid).remove();
        Assertions.assertEquals(1, userA.admin().getUserSessions().size(),
                "[2] Attacker user session S1 must still exist after the client is removed");

        // ── Step 3: Victim's browser opens the poisoned logout URL ────────────
        // client_id == azp (no INVALID_TOKEN error) but the client is gone, which
        // triggers forcedConfirmation while userSession comes from id_token_hint only.
        oauthVictim.logoutForm()
                .idTokenHint(hintIdToken)
                .param("client_id", doomedClientId)
                .open();

        victimLogoutConfirmPage.assertCurrent();

        // ── Step 4: Key assertion ─────────────────────────────────────────────
        // The AUTH_SESSION_ID cookie must be present (needed to POST back) …
        Cookie authSessionCookie = driverVictim.driver().manage()
                .getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        Assertions.assertNotNull(authSessionCookie,
                "[4] The confirmation screen needs an AUTH_SESSION_ID cookie to POST back");

        // … but its encoded session id must NOT equal the hint's session id S1.
        String encodedSessionId = authSessionCookie.getValue();
        int routeSeparator = encodedSessionId.indexOf('.');
        if (routeSeparator != -1) {
            encodedSessionId = encodedSessionId.substring(0, routeSeparator);
        }
        String signedSessionId = new String(Base64Url.decode(encodedSessionId), StandardCharsets.UTF_8);
        String cookieSessionId = signedSessionId.substring(0, signedSessionId.lastIndexOf('.'));
        Assertions.assertNotEquals(sessionIdS1, cookieSessionId,
                "[4] EXPLOIT SUCCEEDED: the logout confirmation screen planted AUTH_SESSION_ID=" + sessionIdS1
                        + " derived from the attacker-controlled id_token_hint in the victim's browser, "
                        + "enabling session fixation.");

        Assertions.assertEquals(1, userA.admin().getUserSessions().size(),
                "[4] Session S1 must not be terminated before the user confirms the logout");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * (Re-)creates root auth session {@code id} holding one unfinished tab.
     * This simulates an attacker keeping a root auth session alive (e.g. a parked
     * browser tab) so that when the matching user session is deleted the id becomes
     * free for reuse — the precondition for the session-fixation attack.
     */
    private void parkRootAuthenticationSession(String id) {
        final String realmName = realm.getName();
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            RootAuthenticationSessionModel root = session.authenticationSessions().getRootAuthenticationSession(realmModel, id);
            if (root == null) {
                root = session.authenticationSessions().createRootAuthenticationSession(realmModel, id);
            }
            root.createAuthenticationSession(realmModel.getClientByClientId("test-app"));
        });
    }

    private String authSessionIdFromCookie(ManagedWebDriver webDriver) {
        Cookie cookie = webDriver.driver().manage().getCookieNamed(CookieType.AUTH_SESSION_ID.getName());
        Assertions.assertNotNull(cookie, "AUTH_SESSION_ID cookie must be present");
        String encoded = cookie.getValue();
        int routeSeparator = encoded.indexOf('.');
        if (routeSeparator != -1) {
            encoded = encoded.substring(0, routeSeparator);
        }
        String signed = new String(Base64Url.decode(encoded), StandardCharsets.UTF_8);
        return signed.substring(0, signed.lastIndexOf('.'));
    }

    private int rootAuthenticationSessionTabCount(String id) {
        final String realmName = realm.getName();
        return runOnServer.fetch(session -> {
            RootAuthenticationSessionModel root = session.authenticationSessions()
                    .getRootAuthenticationSession(session.realms().getRealmByName(realmName), id);
            return root == null ? -1 : root.getAuthenticationSessions().size();
        }, Integer.class);
    }

    private boolean rootAuthenticationSessionExists(String id) {
        final String realmName = realm.getName();
        return runOnServer.fetch(session -> session.authenticationSessions()
                .getRootAuthenticationSession(session.realms().getRealmByName(realmName), id) != null, Boolean.class);
    }
}
