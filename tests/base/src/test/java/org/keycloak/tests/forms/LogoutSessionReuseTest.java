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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.NewCookie;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.cookie.CookieType;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.http.HttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.sessions.RootAuthenticationSessionModel;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.TestApp;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectTestApp;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RepresentationUtils;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Cookie;

/**
 * Tests the logout (root) authentication session that
 * {@code AuthenticationManager.createOrJoinLogoutSession} creates or joins.
 *
 * <p>The logout session is not keyed to the user session id (that would be predictable, see
 * CVE-2026-107124). Instead it is bound to the user session through the
 * {@code LOGOUT_USER_SESSION_ID} auth note. These tests protect the two behaviours that used to rely on
 * the root id being equal to the user session id:
 * <ul>
 *   <li>A browser logout that is split over several calls (the endpoint creates the logout session and stores
 *       {@code post_logout_redirect_uri} and {@code state} in it, {@code browserLogout} then needs to find the
 *       same session again), including the case where the user session is resolved only from
 *       {@code id_token_hint}.</li>
 *   <li>A backchannel logout of a user session while a root auth session that has the same id as the user
 *       session exists (for example re-created by the authorization endpoint when the browser comes back with
 *       only the identity cookie). That root must not be left behind.</li>
 * </ul>
 */
@KeycloakIntegrationTest
public class LogoutSessionReuseTest {

    private static final String STATE = "hint-only-logout-state";

    @InjectRealm
    ManagedRealm realm;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectUser(config = LogoutSessionReuseUserConfig.class)
    ManagedUser user;

    @InjectPage
    LogoutConfirmPage logoutConfirmPage;

    @InjectTestApp
    TestApp testApp;

    @InjectRunOnServer(permittedPackages = {"org.keycloak.tests", "org.keycloak.admin"})
    RunOnServerClient runOnServer;

    public static class LogoutSessionReuseUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("logout-session-reuse-user")
                    .name("Logout", "Reuse")
                    .password("password")
                    .email("logout-session-reuse-user@localhost")
                    .emailVerified(true);
        }
    }

    /**
     * The user session is resolved only from {@code id_token_hint}: the browser has the {@code KEYCLOAK_SESSION}
     * cookie but no identity cookie.
     *
     * <p>{@code LogoutEndpoint.logout} must then create the logout session without a user session (a session
     * derived from the hint alone must not influence the id of a root session that is exposed to the browser).
     * Once {@code doBrowserLogout} confirms the user session through the session cookie, it has to bind the
     * logout session to it. Otherwise {@code browserLogout} creates a second, empty logout session and
     * {@code post_logout_redirect_uri} and {@code state} are lost.
     */
    @Test
    void hintOnlyLogoutKeepsRedirectUriAndState() {
        final String postLogoutRedirectUri = testApp.getRedirectionUri();
        updateClient(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, postLogoutRedirectUri));

        AccessTokenResponse tokens = login();

        // Drop the identity cookie only. The cookies are scoped to the realm path, so the browser has to be on it.
        driver.open(oauth.getEndpoints().getJwks());
        driver.driver().manage().deleteCookieNamed(CookieType.IDENTITY.getName());
        Assertions.assertNull(driver.driver().manage().getCookieNamed(CookieType.IDENTITY.getName()),
                "The identity cookie must be gone for the test to be meaningful");
        Assertions.assertNotNull(driver.driver().manage().getCookieNamed(CookieType.SESSION.getName()),
                "The session cookie must be present so that the id_token_hint is matched to the browser session");

        oauth.logoutForm()
                .idTokenHint(tokens.getIdToken())
                .postLogoutRedirectUri(postLogoutRedirectUri)
                .state(STATE)
                .open();

        String currentUrl = driver.getCurrentUrl();
        Assertions.assertTrue(currentUrl.startsWith(postLogoutRedirectUri),
                "The browser must be redirected to post_logout_redirect_uri, but was: " + currentUrl);
        Assertions.assertTrue(currentUrl.contains("state=" + STATE),
                "The state parameter must be attached to the redirect, but the url was: " + currentUrl);
        Assertions.assertTrue(user.admin().getUserSessions().isEmpty(), "The user session must be logged out");
    }

    /**
     * The user session is resolved only from {@code id_token_hint}: the browser has neither the identity cookie nor
     * the {@code KEYCLOAK_SESSION} cookie, so {@code doBrowserLogout} cannot confirm the session against the browser
     * and falls back to a non-browser (backchannel) logout.
     *
     * <p>The logout session created by {@code LogoutEndpoint.logout} is bound to the user session from the hint.
     * {@code backchannelLogout} then joins that same logout session and removes its root in a {@code finally} block.
     * The test asserts that the user session is still logged out and that {@code post_logout_redirect_uri} and
     * {@code state} survive the removal of the logout session.
     */
    @Test
    void hintOnlyLogoutWithoutBrowserSessionKeepsRedirectUriAndState() {
        final String postLogoutRedirectUri = testApp.getRedirectionUri();
        updateClient(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, postLogoutRedirectUri));

        AccessTokenResponse tokens = login();

        // Drop both cookies. The cookies are scoped to the realm path, so the browser has to be on it.
        driver.open(oauth.getEndpoints().getJwks());
        driver.driver().manage().deleteCookieNamed(CookieType.IDENTITY.getName());
        driver.driver().manage().deleteCookieNamed(CookieType.SESSION.getName());
        Assertions.assertNull(driver.driver().manage().getCookieNamed(CookieType.IDENTITY.getName()),
                "The identity cookie must be gone for the test to be meaningful");
        Assertions.assertNull(driver.driver().manage().getCookieNamed(CookieType.SESSION.getName()),
                "The session cookie must be gone so that the logout is not matched to the browser session");

        oauth.logoutForm()
                .idTokenHint(tokens.getIdToken())
                .postLogoutRedirectUri(postLogoutRedirectUri)
                .state(STATE)
                .open();

        String currentUrl = driver.getCurrentUrl();
        Assertions.assertTrue(currentUrl.startsWith(postLogoutRedirectUri),
                "The browser must be redirected to post_logout_redirect_uri, but was: " + currentUrl);
        Assertions.assertTrue(currentUrl.contains("state=" + STATE),
                "The state parameter must be attached to the redirect, but the url was: " + currentUrl);
        Assertions.assertTrue(user.admin().getUserSessions().isEmpty(), "The user session must be logged out");
    }

    /**
     * A logout confirmation screen is shown and abandoned, the logout session stays in the root that the
     * {@code AUTH_SESSION_ID} cookie points at. A second logout request from the same browser joins that logout
     * session. Its {@code post_logout_redirect_uri} and {@code state} must not leak into the second request, which
     * did not send them.
     */
    @Test
    void abandonedLogoutDoesNotLeakRedirectUriAndStateIntoNextLogout() {
        final String postLogoutRedirectUri = testApp.getRedirectionUri();
        updateClient(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, postLogoutRedirectUri));

        login();

        // Without id_token_hint the confirmation screen is shown. Abandon it.
        oauth.logoutForm()
                .withClientId()
                .postLogoutRedirectUri(postLogoutRedirectUri)
                .state(STATE)
                .open();
        logoutConfirmPage.assertCurrent();

        // Same browser, so the second request finds the first logout session through the AUTH_SESSION_ID cookie
        oauth.logoutForm()
                .withClientId()
                .open();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        String currentUrl = driver.getCurrentUrl();
        Assertions.assertFalse(currentUrl.startsWith(postLogoutRedirectUri),
                "post_logout_redirect_uri of the abandoned request must not be used, but the url was: " + currentUrl);
        Assertions.assertFalse(currentUrl.contains(STATE),
                "state of the abandoned request must not be used, but the url was: " + currentUrl);
        Assertions.assertTrue(user.admin().getUserSessions().isEmpty(), "The user session must be logged out");
    }

    /**
     * An administrator logs the session out while a root auth session with the same id as the user session
     * exists. That is the root that the authorization endpoint re-creates for a browser that comes back with
     * only the identity cookie, and the one that the logout session used to join.
     *
     * <p>The root is simulated with {@link #parkRootAuthenticationSession(String)}, as the root created by the
     * authorization endpoint is removed again as soon as the SSO login completes.
     *
     * <p>The backchannel logout runs in a logout session of its own now, so the root that shares the user
     * session id is no longer cleaned up as a side effect of the logout session being removed. The test asserts
     * that the logout is still delivered to the client and that the root is removed with the user session.
     */
    @Test
    void adminLogoutCleansUpRootAuthSessionSharingUserSessionId() throws Exception {
        try (BackchannelLogoutReceiver receiver = new BackchannelLogoutReceiver()) {
            updateClient(Map.of(
                    OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, receiver.url(),
                    OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true"));

            final String sessionId = oauth.verifyIDToken(login().getIdToken()).getSessionId();

            parkRootAuthenticationSession(sessionId);
            Assertions.assertTrue(rootAuthenticationSessionExists(sessionId),
                    "The root auth session sharing the user session id must exist before the logout");

            realm.admin().deleteSession(sessionId, false);

            LogoutToken logoutToken = receiver.poll();
            Assertions.assertNotNull(logoutToken, "The backchannel logout must be delivered to the client");
            Assertions.assertEquals(sessionId, logoutToken.getSid());
            Assertions.assertTrue(user.admin().getUserSessions().isEmpty(), "The user session must be removed");
            Assertions.assertFalse(rootAuthenticationSessionExists(sessionId),
                    "The root auth session with the same id as the removed user session must be removed too");
        }
    }

    /**
     * A broker logout response (for example a cross-site POST from a SAML identity provider) can come back without
     * the {@code AUTH_SESSION_ID} cookie, as it is not sent with SameSite=Lax. The logout session that holds
     * {@code post_logout_redirect_uri} and {@code state} has to be found again from the user session alone.
     *
     * <p>Each {@code runOnServer} call is a separate request without any cookie, which is the situation of the
     * broker callback. The first call creates the browser-bound logout session as {@code browserLogout} does and
     * stores a value in it, the second call must join the very same logout session.
     */
    @Test
    void browserBoundLogoutSessionIsFoundWithoutAuthSessionCookie() {
        login();
        final String realmName = realm.getName();
        final String userSessionId = user.admin().getUserSessions().get(0).getId();
        final String noteName = "logout-session-reuse-note";

        String firstRootId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, userSessionId);
            AuthenticationSessionModel logoutSession = AuthenticationManager.createOrJoinLogoutSession(session, realmModel, userSession, true, false);
            logoutSession.setAuthNote(noteName, "kept");
            return logoutSession.getParentSession().getId();
        }, String.class);

        String secondRootId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, userSessionId);
            AuthenticationSessionModel logoutSession = AuthenticationManager.createOrJoinLogoutSession(session, realmModel, userSession, true, false);
            Assertions.assertEquals("kept", logoutSession.getAuthNote(noteName),
                    "The values saved in the logout session must survive a callback without the AUTH_SESSION_ID cookie");
            return logoutSession.getParentSession().getId();
        }, String.class);

        Assertions.assertEquals(firstRootId, secondRootId, "The same logout root must be used");
        Assertions.assertNotEquals(userSessionId, firstRootId, "The logout root id must not be derived from the user session id");

        // A request that starts a logout (for example one that resolves the user session only from an attacker
        // supplied id_token_hint) must never be pointed at the recorded root: it would plant a root id that the
        // owner of the user session knows in the browser of the victim.
        String initiatedRootId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, userSessionId);
            return AuthenticationManager.createOrJoinLogoutSession(session, realmModel, userSession, true, true)
                    .getParentSession().getId();
        }, String.class);
        Assertions.assertNotEquals(firstRootId, initiatedRootId,
                "A request that initiates a logout must not join the recorded logout root");

        // Don't leave the browser logged in for the other tests
        user.admin().logout();
    }

    /**
     * The {@code AUTH_SESSION_ID} cookie must never be written for a logout root that was recovered from the user
     * session, not even when the recovered logout session is joined a second time in the same request
     * ({@code browserLogout} followed by {@code finishBrowserLogout}).
     */
    @Test
    void recoveredLogoutRootIsNeverWrittenToCookie() {
        login();
        final String realmName = realm.getName();
        final String userSessionId = user.admin().getUserSessions().get(0).getId();

        // Starts the browser-bound logout, recording the root id in the user session
        String recordedRootId = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, userSessionId);
            return AuthenticationManager.createOrJoinLogoutSession(session, realmModel, userSession, true, false)
                    .getParentSession().getId();
        }, String.class);

        // A request without the cookie recovers the root and joins its logout session twice
        String recoveredCookies = runOnServer.fetch(session -> {
            List<String> cookieNames = new ArrayList<>();
            session.getContext().setHttpResponse(new CookieRecordingHttpResponse(cookieNames));
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserSessionModel userSession = session.sessions().getUserSession(realmModel, userSessionId);
            String first = AuthenticationManager.createOrJoinLogoutSession(session, realmModel, userSession, true, false)
                    .getParentSession().getId();
            String second = AuthenticationManager.createOrJoinLogoutSession(session, realmModel, userSession, true, false)
                    .getParentSession().getId();
            Assertions.assertEquals(first, second, "Both calls must use the same logout root");
            return first + "," + String.join(",", cookieNames);
        }, String.class);

        Assertions.assertEquals(recordedRootId + ",", recoveredCookies,
                "The recovered logout root must be joined, and no cookie may be written for it");

        // Don't leave the browser logged in for the other tests
        user.admin().logout();
    }

    /**
     * The browser has an unfinished login in root R, and neither the identity cookie nor the {@code KEYCLOAK_SESSION}
     * cookie, when it is sent to the logout endpoint with a valid {@code id_token_hint}. The logout session joins R
     * through the {@code AUTH_SESSION_ID} cookie, and as the user session cannot be confirmed against the browser it
     * is logged out by {@code backchannelLogout}. That must only remove the logout tab from R, not the login tab.
     */
    @Test
    void backchannelLogoutKeepsOtherTabsOfSharedRoot() {
        final String postLogoutRedirectUri = testApp.getRedirectionUri();
        updateClient(Map.of(OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, postLogoutRedirectUri));
        resetBrowser();

        String rootId = null;
        try {
            AccessTokenResponse tokens = login();
            dropCookies(CookieType.IDENTITY, CookieType.SESSION, CookieType.AUTH_SESSION_ID);

            // An unfinished login, for example in another tab of the same browser
            oauth.openLoginForm();
            rootId = authSessionIdFromCookie();
            Assertions.assertEquals(1, rootAuthenticationSessionTabCount(rootId), "Root R must hold only the login tab");

            oauth.logoutForm()
                    .idTokenHint(tokens.getIdToken())
                    .postLogoutRedirectUri(postLogoutRedirectUri)
                    .open();

            String currentUrl = driver.getCurrentUrl();
            Assertions.assertTrue(currentUrl.startsWith(postLogoutRedirectUri),
                    "The browser must be redirected to post_logout_redirect_uri, but was: " + currentUrl);
            Assertions.assertTrue(user.admin().getUserSessions().isEmpty(), "The user session must be logged out");
            Assertions.assertEquals(1, rootAuthenticationSessionTabCount(rootId),
                    "Only the logout tab may be removed from root R, the login tab must be kept");
        } finally {
            if (rootId != null) {
                removeRootAuthenticationSession(rootId);
            }
            resetBrowser();
            user.admin().logout();
        }
    }

    /**
     * A logout of user session A is left unfinished in logout root L after it already logged out a client. A logout
     * of user session B, for the same client and from the same browser, then finds the logout session of A in L. It
     * must not reuse it: the logout state that A recorded for the client would suppress the logout of B from that
     * client.
     *
     * <p>Here the browser has no session cookies anymore, so B is logged out by {@code backchannelLogout}.
     */
    @Test
    void logoutOfAnotherUserSessionDoesNotReuseItsClientLogoutStates() throws Exception {
        assertLogoutOfAnotherUserSessionIsDelivered(false);
    }

    /**
     * Same as {@link #logoutOfAnotherUserSessionDoesNotReuseItsClientLogoutStates()}, but the browser is still logged
     * in to user session A. The logout request with the {@code id_token_hint} of B then joins the logout session of A
     * and shows a confirmation screen. Once confirmed, the logout session is bound to B and B is logged out by
     * {@code browserLogout}.
     */
    @Test
    void confirmedLogoutOfAnotherUserSessionDoesNotReuseItsClientLogoutStates() throws Exception {
        assertLogoutOfAnotherUserSessionIsDelivered(true);
    }

    /**
     * The unfinished logout of user session A is simulated by recording the client as logged out in its logout
     * session, as {@code backchannelLogoutClientSession} does.
     */
    private void assertLogoutOfAnotherUserSessionIsDelivered(boolean keepBrowserSessionOfA) throws Exception {
        try (BackchannelLogoutReceiver receiver = new BackchannelLogoutReceiver()) {
            final String postLogoutRedirectUri = testApp.getRedirectionUri();
            updateClient(Map.of(
                    OIDCConfigAttributes.BACKCHANNEL_LOGOUT_URL, receiver.url(),
                    OIDCConfigAttributes.BACKCHANNEL_LOGOUT_SESSION_REQUIRED, "true",
                    OIDCConfigAttributes.POST_LOGOUT_REDIRECT_URIS, postLogoutRedirectUri));
            resetBrowser();

            try {
                // User session B, then user session A in the same browser
                final String idTokenB = login().getIdToken();
                final String sessionB = oauth.verifyIDToken(idTokenB).getSessionId();
                dropCookies(CookieType.IDENTITY, CookieType.SESSION, CookieType.AUTH_SESSION_ID);
                final String sessionA = oauth.verifyIDToken(login().getIdToken()).getSessionId();
                Assertions.assertNotEquals(sessionA, sessionB, "Two different user sessions are needed");

                // Start the logout of A and leave it unfinished
                oauth.logoutForm()
                        .withClientId()
                        .open();
                logoutConfirmPage.assertCurrent();
                recordClientLoggedOutInLogoutSession(authSessionIdFromCookie(), sessionA, oauth.getClientId());

                // Log out B from the same browser
                if (!keepBrowserSessionOfA) {
                    dropCookies(CookieType.IDENTITY, CookieType.SESSION);
                }
                oauth.logoutForm()
                        .idTokenHint(idTokenB)
                        .postLogoutRedirectUri(postLogoutRedirectUri)
                        .open();
                if (keepBrowserSessionOfA) {
                    logoutConfirmPage.assertCurrent();
                    logoutConfirmPage.confirmLogout();
                }

                String currentUrl = driver.getCurrentUrl();
                Assertions.assertTrue(currentUrl.startsWith(postLogoutRedirectUri),
                        "The browser must be redirected to post_logout_redirect_uri, but was: " + currentUrl);
                LogoutToken logoutToken = receiver.poll();
                Assertions.assertNotNull(logoutToken, "The logout of user session B must be delivered to the client");
                Assertions.assertEquals(sessionB, logoutToken.getSid());
                Assertions.assertEquals(List.of(sessionA), user.admin().getUserSessions().stream().map(s -> s.getId()).toList(),
                        "Only user session B must be logged out");
            } finally {
                resetBrowser();
                user.admin().logout();
            }
        }
    }

    /**
     * A logout confirmation creates a new logout root and points the {@code AUTH_SESSION_ID} cookie to it. The
     * {@code KC_AUTH_SESSION_HASH} cookie must point to it as well. Otherwise, after the confirmation is abandoned,
     * a login that joins the logout root (for example with {@code prompt=login}) renders the hash of that root while
     * the cookie still holds the hash of the previous root, and authChecker.js reloads the login page until the
     * cookie expires.
     */
    @Test
    void logoutRootUpdatesAuthSessionHashCookie() {
        resetBrowser();
        try {
            login();
            driver.open(oauth.getEndpoints().getJwks());
            final String loginRootHash = authSessionIdHashFromCookie();
            Assertions.assertNotNull(loginRootHash, "The login must set the KC_AUTH_SESSION_HASH cookie");

            oauth.logoutForm()
                    .withClientId()
                    .open();
            logoutConfirmPage.assertCurrent();

            final String logoutRootHash = authSessionIdHash(authSessionIdFromCookie());
            Assertions.assertNotEquals(loginRootHash, logoutRootHash, "The logout must use a new root");
            Assertions.assertEquals(logoutRootHash, authSessionIdHashFromCookie(),
                    "The KC_AUTH_SESSION_HASH cookie must hold the hash of the logout root");
        } finally {
            resetBrowser();
            user.admin().logout();
        }
    }

    /**
     * Binding the logout session of user session A to user session B removes the logout states recorded for the clients
     * of both user sessions, so that no state of A can suppress the logout of B. That includes a client that only A has,
     * as B can get a session for that client before its logout reaches it.
     */
    @Test
    void bindingLogoutSessionToAnotherUserSessionRemovesClientLogoutStates() {
        final String realmName = realm.getName();
        final String userId = user.getId();
        final String sharedClientId = oauth.getClientId();
        final String clientIdOfA = Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;

        // Created in a request of their own, so that the binding finds them in the session store
        String sessionIds = runOnServer.fetch(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            UserModel userModel = session.users().getUserById(realmModel, userId);
            UserSessionModel sessionA = session.sessions().createUserSession(realmModel, userModel, userModel.getUsername(), "127.0.0.1", "form", false, null, null);
            session.sessions().createClientSession(realmModel, realmModel.getClientByClientId(sharedClientId), sessionA);
            session.sessions().createClientSession(realmModel, realmModel.getClientByClientId(clientIdOfA), sessionA);
            UserSessionModel sessionB = session.sessions().createUserSession(realmModel, userModel, userModel.getUsername(), "127.0.0.1", "form", false, null, null);
            session.sessions().createClientSession(realmModel, realmModel.getClientByClientId(sharedClientId), sessionB);
            return sessionA.getId() + "," + sessionB.getId();
        }, String.class);
        final String sessionIdA = sessionIds.split(",")[0];
        final String sessionIdB = sessionIds.split(",")[1];

        try {
            String boundState = runOnServer.fetch(session -> {
                RealmModel realmModel = session.realms().getRealmByName(realmName);
                ClientModel sharedClient = realmModel.getClientByClientId(sharedClientId);
                ClientModel clientOfA = realmModel.getClientByClientId(clientIdOfA);
                RootAuthenticationSessionModel root = session.authenticationSessions().createRootAuthenticationSession(realmModel);
                AuthenticationSessionModel logoutSession = root.createAuthenticationSession(sharedClient);
                logoutSession.setAction(AuthenticationSessionModel.Action.LOGGING_OUT.name());
                AuthenticationManager.bindLogoutSessionToUserSession(session, logoutSession, session.sessions().getUserSession(realmModel, sessionIdA));
                AuthenticationManager.setClientLogoutAction(logoutSession, sharedClient.getId(), AuthenticationSessionModel.Action.LOGGED_OUT);
                AuthenticationManager.setClientLogoutAction(logoutSession, clientOfA.getId(), AuthenticationSessionModel.Action.LOGGED_OUT);

                AuthenticationManager.bindLogoutSessionToUserSession(session, logoutSession, session.sessions().getUserSession(realmModel, sessionIdB));

                String result = logoutSession.getAuthNote(AuthenticationManager.LOGOUT_USER_SESSION_ID) + ","
                        + AuthenticationManager.getClientLogoutAction(logoutSession, sharedClient.getId()) + ","
                        + AuthenticationManager.getClientLogoutAction(logoutSession, clientOfA.getId());
                session.authenticationSessions().removeRootAuthenticationSession(realmModel, root);
                return result;
            }, String.class);

            Assertions.assertEquals(sessionIdB + ",null,null", boundState,
                    "The logout session must be bound to B, without the logout states recorded for A");
        } finally {
            user.admin().logout();
        }
    }

    /** Records the names of the cookies that are written during a request. Static so it does not capture the test instance. */
    private static class CookieRecordingHttpResponse implements HttpResponse {

        private final List<String> cookieNames;

        CookieRecordingHttpResponse(List<String> cookieNames) {
            this.cookieNames = cookieNames;
        }

        @Override
        public int getStatus() {
            return 200;
        }

        @Override
        public void setStatus(int statusCode) {
        }

        @Override
        public void addHeader(String name, String value) {
        }

        @Override
        public void setHeader(String name, String value) {
        }

        @Override
        public void setCookieIfAbsent(NewCookie cookie) {
            cookieNames.add(cookie.getName());
        }
    }

    private AccessTokenResponse login() {
        AuthorizationEndpointResponse login = oauth.doLogin(user.getUsername(), user.getPassword());
        Assertions.assertTrue(login.isSuccess(), "Login must succeed");
        AccessTokenResponse tokens = oauth.doAccessTokenRequest(login.getCode());
        Assertions.assertTrue(tokens.isSuccess(), "Token exchange must succeed");
        return tokens;
    }

    private void updateClient(Map<String, String> attributes) {
        ClientResource clientResource = AdminApiUtil.findClientByClientId(realm.admin(), oauth.getClientId());
        ClientRepresentation original = clientResource.toRepresentation();
        realm.cleanup().add(r -> r.clients().get(original.getId()).update(original));
        ClientRepresentation updated = RepresentationUtils.clone(original);
        attributes.forEach(updated.getAttributes()::put);
        clientResource.update(updated);
    }

    /**
     * (Re-)creates root auth session {@code id} holding one unfinished tab.
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

    private boolean rootAuthenticationSessionExists(String id) {
        final String realmName = realm.getName();
        return runOnServer.fetch(session -> session.authenticationSessions()
                .getRootAuthenticationSession(session.realms().getRealmByName(realmName), id) != null, Boolean.class);
    }

    private int rootAuthenticationSessionTabCount(String id) {
        final String realmName = realm.getName();
        return runOnServer.fetch(session -> {
            RootAuthenticationSessionModel root = session.authenticationSessions()
                    .getRootAuthenticationSession(session.realms().getRealmByName(realmName), id);
            return root == null ? -1 : root.getAuthenticationSessions().size();
        }, Integer.class);
    }

    /**
     * Records {@code clientId} as logged out in the logout session of {@code userSessionId} in root {@code rootId}.
     */
    private void recordClientLoggedOutInLogoutSession(String rootId, String userSessionId, String clientId) {
        final String realmName = realm.getName();
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            RootAuthenticationSessionModel root = session.authenticationSessions().getRootAuthenticationSession(realmModel, rootId);
            Assertions.assertNotNull(root, "The logout root must exist");
            AuthenticationSessionModel logoutSession = root.getAuthenticationSessions().values().stream()
                    .filter(authSession -> AuthenticationSessionModel.Action.LOGGING_OUT.name().equals(authSession.getAction()))
                    .findFirst()
                    .orElse(null);
            Assertions.assertNotNull(logoutSession, "The logout root must hold a logout session");
            Assertions.assertEquals(userSessionId, logoutSession.getAuthNote(AuthenticationManager.LOGOUT_USER_SESSION_ID));
            AuthenticationManager.setClientLogoutAction(logoutSession, realmModel.getClientByClientId(clientId).getId(),
                    AuthenticationSessionModel.Action.LOGGED_OUT);
        });
    }

    private void removeRootAuthenticationSession(String id) {
        final String realmName = realm.getName();
        runOnServer.run(session -> {
            RealmModel realmModel = session.realms().getRealmByName(realmName);
            RootAuthenticationSessionModel root = session.authenticationSessions().getRootAuthenticationSession(realmModel, id);
            if (root != null) {
                session.authenticationSessions().removeRootAuthenticationSession(realmModel, root);
            }
        });
    }

    /**
     * Drops the session and authentication session cookies, so that a test does not depend on the browser state that
     * other tests leave behind (the browser is shared).
     */
    private void resetBrowser() {
        dropCookies(CookieType.IDENTITY, CookieType.SESSION, CookieType.AUTH_SESSION_ID, CookieType.AUTH_SESSION_ID_HASH);
    }

    /**
     * Deletes the given cookies from the browser. The cookies are scoped to the realm path, so the browser has to be on it.
     */
    private void dropCookies(CookieType... cookieTypes) {
        driver.open(oauth.getEndpoints().getJwks());
        for (CookieType cookieType : cookieTypes) {
            driver.driver().manage().deleteCookieNamed(cookieType.getName());
            Assertions.assertNull(driver.cookies().get(cookieType), "The " + cookieType.getName() + " cookie must be gone");
        }
    }

    private String authSessionIdFromCookie() {
        Cookie cookie = driver.cookies().get(CookieType.AUTH_SESSION_ID);
        Assertions.assertNotNull(cookie, "AUTH_SESSION_ID cookie must be present");
        String encoded = cookie.getValue();
        int routeSeparator = encoded.indexOf('.');
        if (routeSeparator != -1) {
            encoded = encoded.substring(0, routeSeparator);
        }
        String signed = new String(Base64Url.decode(encoded), StandardCharsets.UTF_8);
        return signed.substring(0, signed.lastIndexOf('.'));
    }

    /**
     * The value of the {@code KC_AUTH_SESSION_HASH} cookie, or {@code null} if there is none. The value may be quoted,
     * the quotes are removed as authChecker.js does.
     */
    private String authSessionIdHashFromCookie() {
        Cookie cookie = driver.cookies().get(CookieType.AUTH_SESSION_ID_HASH);
        if (cookie == null) {
            return null;
        }
        String value = cookie.getValue();
        return value.length() > 1 && value.startsWith("\"") && value.endsWith("\"") ? value.substring(1, value.length() - 1) : value;
    }

    /** The value of the {@code KC_AUTH_SESSION_HASH} cookie for root {@code rootId}. */
    private static String authSessionIdHash(String rootId) {
        return Base64.getEncoder().withoutPadding()
                .encodeToString(HashUtils.hash(JavaAlgorithm.SHA384, rootId.getBytes(StandardCharsets.UTF_8)));
    }

    /**
     * Minimal OIDC backchannel logout endpoint that records the received logout tokens.
     */
    private static class BackchannelLogoutReceiver implements AutoCloseable {

        private static final String PATH = "/backchannel-logout";
        private static final String LOGOUT_TOKEN_PARAM = "logout_token=";

        private final HttpServer server;
        private final BlockingQueue<LogoutToken> tokens = new LinkedBlockingQueue<>();

        BackchannelLogoutReceiver() throws IOException {
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext(PATH, exchange -> {
                try (exchange) {
                    String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                    for (String parameter : body.split("&")) {
                        if (parameter.startsWith(LOGOUT_TOKEN_PARAM)) {
                            String jwt = URLDecoder.decode(parameter.substring(LOGOUT_TOKEN_PARAM.length()), StandardCharsets.UTF_8);
                            tokens.add(new JWSInput(jwt).readJsonContent(LogoutToken.class));
                        }
                    }
                    exchange.sendResponseHeaders(200, -1);
                } catch (JWSInputException e) {
                    throw new IOException(e);
                }
            });
            server.start();
        }

        String url() {
            return "http://" + server.getAddress().getAddress().getHostAddress() + ":" + server.getAddress().getPort() + PATH;
        }

        LogoutToken poll() throws InterruptedException {
            return tokens.poll(10, TimeUnit.SECONDS);
        }

        @Override
        public void close() {
            server.stop(0);
        }
    }
}
