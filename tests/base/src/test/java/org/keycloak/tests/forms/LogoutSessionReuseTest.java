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
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import jakarta.ws.rs.core.NewCookie;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.cookie.CookieType;
import org.keycloak.http.HttpResponse;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.RealmModel;
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
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.realm.UserConfigBuilder;
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
        public UserConfigBuilder configure(UserConfigBuilder user) {
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
