/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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
 *
 */
package org.keycloak.testsuite.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.hamcrest.MatcherAssert;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.LogoutToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginPage;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlEquals;

import org.keycloak.testsuite.auth.page.account.AccountManagement;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.PageUtils;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.updaters.RealmAttributeUpdater;
import org.keycloak.testsuite.updaters.UserAttributeUpdater;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.URLUtils;
import org.keycloak.testsuite.util.WaitUtils;
import org.openqa.selenium.NoSuchElementException;

/**
 * Test for OIDC RP-Initiated Logout - https://openid.net/specs/openid-connect-rpinitiated-1_0.html
 * <p>
 * This is handled on server-side by the LogoutEndpoint.logout method
 *
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class RPInitiatedLogoutTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    @Page
    protected InfoPage infoPage;

    @Page
    protected AccountManagement accountManagementPage;

    @Page
    private ErrorPage errorPage;

    private String APP_REDIRECT_URI;

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Before
    public void clientConfiguration() {
        APP_REDIRECT_URI = oauth.APP_AUTH_ROOT;
    }

    @Test
    public void logoutRedirect() {

        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String sessionId = tokenResponse.getSessionState();

        String redirectUri = APP_REDIRECT_URI + "?logout";

        String idTokenString = tokenResponse.getIdToken();

        String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(redirectUri).idTokenHint(idTokenString).build();
        driver.navigate().to(logoutUrl);

        events.expectLogout(sessionId).detail(Details.REDIRECT_URI, redirectUri).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(sessionId)));

        assertCurrentUrlEquals(redirectUri);

        tokenResponse = loginUser();
        String sessionId2 = tokenResponse.getSessionState();
        idTokenString = tokenResponse.getIdToken();
        assertNotEquals(sessionId, sessionId2);

        // Test also "state" parameter is included in the URL after logout. Make sure to use idTokenHint from the last login to match with current browser session
        logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(redirectUri).idTokenHint(idTokenString).state("something").build();
        driver.navigate().to(logoutUrl);
        events.expectLogout(sessionId2).detail(Details.REDIRECT_URI, redirectUri).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(sessionId2)));
        assertCurrentUrlEquals(redirectUri + "&state=something");
    }

    @Test
    public void postLogoutRedirect() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String sessionId = tokenResponse.getSessionState();

        String redirectUri = APP_REDIRECT_URI + "?post_logout";

        List<String> postLogoutRedirectUris = Collections.singletonList(redirectUri);
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").setPostLogoutRedirectUri(postLogoutRedirectUris);

        String idTokenString = tokenResponse.getIdToken();

        try {
            String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(redirectUri).idTokenHint(idTokenString).build();
            driver.navigate().to(logoutUrl);

            events.expectLogout(sessionId).detail(Details.REDIRECT_URI, redirectUri).assertEvent();
            MatcherAssert.assertThat(false, is(isSessionActive(sessionId)));

            assertCurrentUrlEquals(redirectUri);

            tokenResponse = loginUser();
            String sessionId2 = tokenResponse.getSessionState();
            idTokenString = tokenResponse.getIdToken();
            assertNotEquals(sessionId, sessionId2);

            // Test also "state" parameter is included in the URL after logout. Make sure to use idTokenHint from the last login to match with current browser session
            logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(redirectUri).idTokenHint(idTokenString).state("something").build();
            driver.navigate().to(logoutUrl);
            events.expectLogout(sessionId2).detail(Details.REDIRECT_URI, redirectUri).assertEvent();
            MatcherAssert.assertThat(false, is(isSessionActive(sessionId2)));
            assertCurrentUrlEquals(redirectUri + "&state=something");
        } finally {
            postLogoutRedirectUris = Collections.singletonList("+");
            ClientManager.realm(adminClient.realm("test")).clientId("test-app").setPostLogoutRedirectUri(postLogoutRedirectUris);
        }
    }

    @Test
    public void logoutRedirectWithIdTokenHintPointToDifferentSession() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String sessionId = tokenResponse.getSessionState();

        String redirectUri = APP_REDIRECT_URI + "?logout";

        String idTokenString = tokenResponse.getIdToken();

        String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(redirectUri).idTokenHint(idTokenString).build();
        driver.navigate().to(logoutUrl);

        events.expectLogout(sessionId).detail(Details.REDIRECT_URI, redirectUri).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(sessionId)));

        assertCurrentUrlEquals(redirectUri);

        loginPage.open();
        loginPage.login("test-user@localhost", "password");
        assertTrue(appPage.isCurrent());

        String sessionId2 = events.expectLogin().assertEvent().getSessionId();
        assertNotEquals(sessionId, sessionId2);

        // Using idTokenHint of the 1st session. Logout confirmation is needed in such case. Test also "state" parameter is included in the URL after logout
        logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(redirectUri).idTokenHint(idTokenString).state("something").build();
        driver.navigate().to(logoutUrl);
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();
        events.expectLogoutError(Errors.SESSION_EXPIRED);
        MatcherAssert.assertThat(false, is(isSessionActive(sessionId2)));
        assertCurrentUrlEquals(redirectUri + "&state=something");
    }


    @Test
    public void logoutWithExpiredSession() throws Exception {
        try (AutoCloseable c = new RealmAttributeUpdater(adminClient.realm("test"))
                .updateWith(r -> r.setSsoSessionMaxLifespan(20))
                .update()) {

            OAuthClient.AccessTokenResponse tokenResponse = loginUser();
            String idTokenString = tokenResponse.getIdToken();

            // expire online user session
            setTimeOffset(9999);

            String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).idTokenHint(idTokenString).build();
            driver.navigate().to(logoutUrl);

            // should not throw an internal server error. But no logout event is sent as nothing was logged-out
            appPage.assertCurrent();
            events.expectLogoutError(Errors.SESSION_EXPIRED);
            MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));

            // check if the back channel logout succeeded
            driver.navigate().to(oauth.getLoginFormUrl());
            WaitUtils.waitForPageToLoad();
            loginPage.assertCurrent();
        }
    }


    //KEYCLOAK-2741
    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void logoutWithRememberMe() throws IOException {
        try (RealmAttributeUpdater update = new RealmAttributeUpdater(testRealm()).setRememberMe(true).update()) {
            loginPage.open();
            assertFalse(loginPage.isRememberMeChecked());
            loginPage.setRememberMe(true);
            assertTrue(loginPage.isRememberMeChecked());
            loginPage.login("test-user@localhost", "password");

            String sessionId = events.expectLogin().assertEvent().getSessionId();

            // Expire session
            testingClient.testing().removeUserSession("test", sessionId);

            // Assert rememberMe checked and username/email prefilled
            loginPage.open();
            assertTrue(loginPage.isRememberMeChecked());
            assertEquals("test-user@localhost", loginPage.getUsername());

            loginPage.login("test-user@localhost", "password");

            //log out
            appPage.openAccount();
            accountManagementPage.signOut();
            // Assert rememberMe not checked nor username/email prefilled
            assertTrue(loginPage.isCurrent());
            assertFalse(loginPage.isRememberMeChecked());
            assertNotEquals("test-user@localhost", loginPage.getUsername());
        }
    }


    @Test
    public void logoutSessionWhenLoggedOutByAdmin() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String sessionId = tokenResponse.getSessionState();
        String idTokenString = tokenResponse.getIdToken();

        adminClient.realm("test").logoutAll();
        MatcherAssert.assertThat(false, is(isSessionActive(sessionId)));

        // Try logout even if user already logged-out by admin. Should redirect back to the application, but no logout-event should be triggered
        String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).idTokenHint(idTokenString).build();
        driver.navigate().to(logoutUrl);
        events.expectLogoutError(Errors.SESSION_EXPIRED);
        assertCurrentUrlEquals(APP_REDIRECT_URI);

        // Login again in the browser. Ensure to use newest idTokenHint after logout
        tokenResponse = loginUser();
        String sessionId2 = tokenResponse.getSessionState();
        idTokenString = tokenResponse.getIdToken();
        assertNotEquals(sessionId, sessionId2);
        logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).idTokenHint(idTokenString).build();

        driver.navigate().to(logoutUrl);
        events.expectLogout(sessionId2).detail(Details.REDIRECT_URI, APP_REDIRECT_URI).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(sessionId2)));
    }


    // KEYCLOAK-5982
    @Test
    public void testLogoutWhenAccountClientRenamed() throws IOException {
        // Temporarily rename client "account" . Revert it back after the test
        try (Closeable accountClientUpdater = ClientAttributeUpdater.forClient(adminClient, "test", Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                .setClientId("account-changed")
                .update()) {

            // Assert logout works
            logoutRedirect();
        }
    }

    @Test
    public void browserLogoutWithAccessToken() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String accessToken = tokenResponse.getAccessToken();

        driver.navigate().to(oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).idTokenHint(accessToken).build());

        events.expectLogoutError(OAuthErrorException.INVALID_TOKEN).assertEvent();

        // Session still authenticated
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
    }

    @Test
    public void logoutWithExpiredIdToken() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String idTokenString = tokenResponse.getIdToken();

        // Logout should succeed with expired ID token, see KEYCLOAK-3399
        setTimeOffset(60 * 60 * 24);

        String logoutUrl = oauth.getLogoutUrl()
                .idTokenHint(idTokenString)
                .postLogoutRedirectUri(APP_REDIRECT_URI)
                .build();

        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
             CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(APP_REDIRECT_URI));
        }
        events.expectLogoutError(Errors.SESSION_EXPIRED);

        MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));
    }

    @Test
    public void logoutWithValidIdTokenWhenLoggedOutByAdmin() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String idTokenString = tokenResponse.getIdToken();

        adminClient.realm("test").logoutAll();

        // Logout with HTTP client. Logout should succeed with user already logged out, see KEYCLOAK-3399. But no logout event should be present
        String logoutUrl = oauth.getLogoutUrl()
                .idTokenHint(idTokenString)
                .postLogoutRedirectUri(APP_REDIRECT_URI)
                .build();

        try (CloseableHttpClient c = HttpClientBuilder.create().disableRedirectHandling().build();
             CloseableHttpResponse response = c.execute(new HttpGet(logoutUrl))) {
            assertThat(response, Matchers.statusCodeIsHC(Response.Status.FOUND));
            assertThat(response.getFirstHeader(HttpHeaders.LOCATION).getValue(), is(APP_REDIRECT_URI));
        }
        events.expectLogoutError(Errors.SESSION_EXPIRED);

        MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));
    }


    // Parameter "redirect_uri" is not valid in logoutRequest (See LegacyLogoutTest for the scenario with "redirect_uri" allowed by backwards compatibility switch)
    @Test
    public void logoutWithRedirectUriParameterShouldFail() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String idTokenString = tokenResponse.getIdToken();

        // Logout with "redirect_uri" parameter alone should fail
        String logoutUrl = oauth.getLogoutUrl().redirectUri(APP_REDIRECT_URI).build();
        driver.navigate().to(logoutUrl);
        errorPage.assertCurrent();
        events.expectLogoutError(OAuthErrorException.INVALID_REQUEST).assertEvent();

        // Logout with "redirect_uri" parameter and with "id_token_hint" should fail
        oauth.getLogoutUrl().idTokenHint(idTokenString).redirectUri(APP_REDIRECT_URI).build();
        driver.navigate().to(logoutUrl);
        errorPage.assertCurrent();
        events.expectLogoutError(OAuthErrorException.INVALID_REQUEST).assertEvent();

        // Assert user still authenticated
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
    }


    // Test with "post_logout_redirect_uri" without "id_token_hint" should fail
    @Test
    public void logoutWithPostLogoutUriWithoutIdTokenHintShouldFail() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();

        // Logout with "redirect_uri" parameter alone should fail
        String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).build();
        driver.navigate().to(logoutUrl);
        errorPage.assertCurrent();
        events.expectLogoutError(OAuthErrorException.INVALID_REQUEST).assertEvent();

        // Assert user still authenticated
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
    }


    @Test
    public void logoutWithInvalidPostLogoutRedirectUri() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String idTokenString = tokenResponse.getIdToken();

        // Completely invalid redirect uri
        driver.navigate().to(oauth.getLogoutUrl().postLogoutRedirectUri("https://invalid").idTokenHint(idTokenString).build());
        errorPage.assertCurrent();
        events.expectLogoutError(OAuthErrorException.INVALID_REDIRECT_URI).detail(Details.REDIRECT_URI, "https://invalid").assertEvent();

        // Redirect uri of different client in the realm should fail as well
        String rootUrlClientRedirectUri = UriUtils.getOrigin(APP_REDIRECT_URI) + "/foo/bar";
        driver.navigate().to(oauth.getLogoutUrl().postLogoutRedirectUri(rootUrlClientRedirectUri).idTokenHint(idTokenString).build());
        errorPage.assertCurrent();
        events.expectLogoutError(OAuthErrorException.INVALID_REDIRECT_URI).detail(Details.REDIRECT_URI, rootUrlClientRedirectUri).assertEvent();

        // Session still authenticated
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
    }


    @Test
    public void logoutWithInvalidIdTokenHint() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String idTokenString = tokenResponse.getIdToken();

        // Removed signature from id_token_hint
        String idTokenHint = idTokenString.substring(0, idTokenString.lastIndexOf("."));
        driver.navigate().to(oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).idTokenHint(idTokenHint).build());
        errorPage.assertCurrent();
        events.expectLogoutError(OAuthErrorException.INVALID_TOKEN).removeDetail(Details.REDIRECT_URI).assertEvent();

        // Invalid signature
        idTokenHint = idTokenHint + ".something";
        driver.navigate().to(oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).idTokenHint(idTokenHint).build());
        errorPage.assertCurrent();
        events.expectLogoutError(OAuthErrorException.INVALID_TOKEN).removeDetail(Details.REDIRECT_URI).assertEvent();

        // Session still authenticated
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
    }


    // Test without "id_token_hint" and without "post_logout_redirect_uri" . User should confirm logout
    @Test
    public void logoutWithoutIdTokenHintWithoutPostLogoutRedirectUri() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();

        driver.navigate().to(oauth.getLogoutUrl().build());

        // Assert logout confirmation page. Session still exists
        logoutConfirmPage.assertCurrent();
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
        events.assertEmpty();
        logoutConfirmPage.confirmLogout();

        // Info page present. No link "back to the application"
        infoPage.assertCurrent();
        Assert.assertEquals("You are logged out", infoPage.getInfo());
        try {
            logoutConfirmPage.clickBackToApplicationLink();
            fail();
        } catch (NoSuchElementException ex) {
            // expected
        }

        events.expectLogout(tokenResponse.getSessionState()).removeDetail(Details.REDIRECT_URI).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));
    }


    // Test with "id_token_hint" and without "post_logout_redirect_uri" . User should see "You were logged-out" at the end of logout
    @Test
    public void logoutWithIdTokenHintWithoutPostLogoutRedirectUri() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();

        driver.navigate().to(oauth.getLogoutUrl().idTokenHint(tokenResponse.getIdToken()).build());

        // Info page present. Link "back to the application" present
        infoPage.assertCurrent();
        Assert.assertEquals("You are logged out", infoPage.getInfo());

        events.expectLogout(tokenResponse.getSessionState()).removeDetail(Details.REDIRECT_URI).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));

        infoPage.clickBackToApplicationLink();
        WaitUtils.waitForPageToLoad();
        MatcherAssert.assertThat(driver.getCurrentUrl(), endsWith("/app/auth"));
    }


    // Test for the scenario when "action" inside authentication session is expired
    @Test
    public void logoutExpiredConfirmationAction() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();

        driver.navigate().to(oauth.getLogoutUrl().build());

        // Assert logout confirmation page. Session still exists
        logoutConfirmPage.assertCurrent();
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
        events.assertEmpty();

        // Set time offset to expire "action" inside logoutSession
        setTimeOffset(310);
        logoutConfirmPage.confirmLogout();

        errorPage.assertCurrent();
        Assert.assertEquals("Logout failed", errorPage.getError());

        events.expectLogoutError(Errors.EXPIRED_CODE).assertEvent();
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));

        // Link not present
        try {
            errorPage.clickBackToApplication();
            fail();
        } catch (NoSuchElementException ex) {
            // expected
        }
    }

    // Test for the scenario when "authenticationSession" itself is expired
    @Test
    public void logoutExpiredConfirmationAuthSession() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();

        driver.navigate().to(oauth.getLogoutUrl().build());

        // Assert logout confirmation page. Session still exists
        logoutConfirmPage.assertCurrent();
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
        events.assertEmpty();

        // Set time offset to expire "action" inside logoutSession
        setTimeOffset(1810);
        logoutConfirmPage.confirmLogout();

        errorPage.assertCurrent();
        Assert.assertEquals("Logout failed", errorPage.getError());

        events.expectLogoutError(Errors.SESSION_EXPIRED).assertEvent();
    }

    // Test logout with "consentRequired" . All of "post_logout_redirect_uri", "id_token_hint" and "state" parameters are present in the logout request
    @Test
    public void logoutConsentRequired() {
        oauth.clientId("third-party");
        OAuthClient.AccessTokenResponse tokenResponse = loginUser(true);
        String idTokenString = tokenResponse.getIdToken();

        String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).idTokenHint(idTokenString).state("somethingg").build();
        driver.navigate().to(logoutUrl);

        // Logout confirmation page not shown as id_token_hint was included.
        // Redirected back to the application with expected "state"
        events.expectLogout(tokenResponse.getSessionState()).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));
        assertCurrentUrlEquals(APP_REDIRECT_URI + "?state=somethingg");

        UserResource user = ApiUtil.findUserByUsernameId(testRealm(), "test-user@localhost");
        user.revokeConsent("third-party");
    }


    // Test logout request with only "client_id" parameter. Also test "ui_locales" parameter works as expected
    @Test
    public void logoutWithUiLocalesAndClientIdParameter() throws IOException {
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(testRealm()).addSupportedLocale("cs").update()) {
            OAuthClient.AccessTokenResponse tokenResponse = loginUser(false);

            String logoutUrl = oauth.getLogoutUrl().clientId("test-app").uiLocales("cs").build();
            driver.navigate().to(logoutUrl);

            // Assert logout confirmation page. Session still exists. Assert czech language on logout page
            Assert.assertEquals("Odhlašování", PageUtils.getPageTitle(driver)); // Logging out
            Assert.assertEquals("Čeština", logoutConfirmPage.getLanguageDropdownText());
            MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
            events.assertEmpty();
            logoutConfirmPage.confirmLogout();

            // Info page present with the link "Back to application"
            events.expectLogout(tokenResponse.getSessionState()).removeDetail(Details.REDIRECT_URI).assertEvent();
            MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));

            infoPage.assertCurrent();
            Assert.assertEquals("Odhlášení bylo úspěšné", infoPage.getInfo()); // Logout success message
            infoPage.clickBackToApplicationLinkCs();
            WaitUtils.waitForPageToLoad();
            MatcherAssert.assertThat(driver.getCurrentUrl(), endsWith("/app/auth"));
        }
    }

    @Test
    public void logoutWithClientIdAndExpiredCode() throws IOException {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String idTokenString = tokenResponse.getIdToken();

        driver.navigate().to(oauth.getLogoutUrl().clientId("test-app").build());

        // Assert logout confirmation page. Session still exists
        logoutConfirmPage.assertCurrent();
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
        events.assertEmpty();

        // Set time offset to expire "action" inside logoutSession
        setTimeOffset(310);
        logoutConfirmPage.confirmLogout();

        errorPage.assertCurrent();
        Assert.assertEquals("Logout failed", errorPage.getError());

        events.expectLogoutError(Errors.EXPIRED_CODE).assertEvent();
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));

        // Link "Back to application" present
        errorPage.clickBackToApplication();
        MatcherAssert.assertThat(driver.getCurrentUrl(), endsWith("/app/auth"));
    }


    @Test
    public void logoutWithClientIdAndWithoutIdTokenHint() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();

        String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).clientId("test-app").state("somethingg").build();
        driver.navigate().to(logoutUrl);

        // Assert logout confirmation page as id_token_hint was not sent. Session still exists. Assert default language on logout page (English)
        logoutConfirmPage.assertCurrent();
        Assert.assertEquals("English", logoutConfirmPage.getLanguageDropdownText());
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
        events.assertEmpty();
        logoutConfirmPage.confirmLogout();

        // Redirected back to the application with expected "state"
        events.expectLogout(tokenResponse.getSessionState()).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));
        assertCurrentUrlEquals(APP_REDIRECT_URI + "?state=somethingg");
    }


    @Test
    public void logoutWithClientIdIdTokenHintAndPostLogoutRedirectUri() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();

        // Test logout with all of "client_id", "id_token_hint" and "post_logout_redirect_uri". Logout should work without confirmation
        String logoutUrl = oauth.getLogoutUrl()
                .postLogoutRedirectUri(APP_REDIRECT_URI)
                .clientId("test-app")
                .idTokenHint(tokenResponse.getIdToken())
                .state("somethingg").build();
        driver.navigate().to(logoutUrl);

        // Logout done and redirected back to the application with expected "state"
        events.expectLogout(tokenResponse.getSessionState()).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));
        assertCurrentUrlEquals(APP_REDIRECT_URI + "?state=somethingg");

        // Test logout only with "client_id" and "post_logout_redirect_uri". Should automatically redirect as there is no logout (No active browser session)
        logoutUrl = oauth.getLogoutUrl()
                .postLogoutRedirectUri(APP_REDIRECT_URI)
                .clientId("test-app")
                .state("something2").build();
        driver.navigate().to(logoutUrl);

        events.assertEmpty();
        assertCurrentUrlEquals(APP_REDIRECT_URI + "?state=something2");
    }


    @Test
    public void logoutWithBadClientId() {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();

        // Case when client_id points to different client than ID Token.
        String logoutUrl = oauth.getLogoutUrl()
                .postLogoutRedirectUri(APP_REDIRECT_URI)
                .clientId("third-party")
                .idTokenHint(tokenResponse.getIdToken()).build();
        driver.navigate().to(logoutUrl);

        errorPage.assertCurrent();
        Assert.assertEquals("Invalid parameter: id_token_hint", errorPage.getError());

        events.expectLogoutError(Errors.INVALID_TOKEN).client("third-party").assertEvent();
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));

        // Case when client_id is non-existing client and redirect uri of different client is used
        logoutUrl = oauth.getLogoutUrl()
                .postLogoutRedirectUri(APP_REDIRECT_URI)
                .clientId("non-existing").build();
        driver.navigate().to(logoutUrl);

        errorPage.assertCurrent();
        Assert.assertEquals("Invalid redirect uri", errorPage.getError());

        events.expectLogoutError(Errors.INVALID_REDIRECT_URI).assertEvent();
        MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));

        // Case when client_id is non-existing client. Confirmation is needed.
        logoutUrl = oauth.getLogoutUrl()
                .clientId("non-existing").build();
        driver.navigate().to(logoutUrl);

        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        // Info page present. No link "back to the application"
        infoPage.assertCurrent();
        Assert.assertEquals("You are logged out", infoPage.getInfo());
        try {
            logoutConfirmPage.clickBackToApplicationLink();
            fail();
        } catch (NoSuchElementException ex) {
            // expected
        }

        events.expectLogout(tokenResponse.getSessionState()).removeDetail(Details.REDIRECT_URI).assertEvent();
        MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));
    }


    // Calling RP-Initiated Logout endpoint with POST request. This must be supported according to specification
    @Test
    public void logoutWithPostRequest() throws IOException {
        try (RealmAttributeUpdater updater = new RealmAttributeUpdater(testRealm()).addSupportedLocale("cs").update()) {
            OAuthClient.AccessTokenResponse tokenResponse = loginUser();

            // Logout with POST request and automatic redirect after logout
            String redirectUri = APP_REDIRECT_URI + "?logout";

            String idTokenString = tokenResponse.getIdToken();
            String sessionId = tokenResponse.getSessionState();

            Map<String, String> postParams = new HashMap<>();
            postParams.put(OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM, redirectUri);
            postParams.put(OIDCLoginProtocol.ID_TOKEN_HINT, idTokenString);
            postParams.put(OAuth2Constants.STATE, "my-state");
            URLUtils.sendPOSTRequestWithWebDriver(oauth.getLogoutUrl().build(), postParams);

            events.expectLogout(tokenResponse.getSessionState()).detail(Details.REDIRECT_URI, redirectUri).assertEvent();
            MatcherAssert.assertThat(false, is(isSessionActive(sessionId)));
            assertCurrentUrlEquals(redirectUri + "&state=my-state");

            // Logout with showing confirmation screen
            tokenResponse = loginUser();
            sessionId = tokenResponse.getSessionState();

            postParams.clear();
            postParams.put(OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM, redirectUri);
            postParams.put(OAuth2Constants.CLIENT_ID, "test-app");
            postParams.put(OAuth2Constants.STATE, "my-state-2");
            postParams.put(OIDCLoginProtocol.UI_LOCALES_PARAM, "cs");
            URLUtils.sendPOSTRequestWithWebDriver(oauth.getLogoutUrl().build(), postParams);

            Assert.assertEquals("Odhlašování", PageUtils.getPageTitle(driver)); // Logging out
            Assert.assertEquals("Čeština", logoutConfirmPage.getLanguageDropdownText());
            logoutConfirmPage.confirmLogout();

            WaitUtils.waitForPageToLoad();
            events.expectLogout(tokenResponse.getSessionState()).detail(Details.REDIRECT_URI, redirectUri).assertEvent();
            MatcherAssert.assertThat(false, is(isSessionActive(sessionId)));
            assertCurrentUrlEquals(redirectUri + "&state=my-state-2");
        }
    }


    @Test
    public void testLocalizationPreferenceDuringLogout() throws IOException {
        try (RealmAttributeUpdater realmUpdater = new RealmAttributeUpdater(testRealm()).addSupportedLocale("cs").update()) {
            OAuthClient.AccessTokenResponse tokenResponse = loginUser();

            // Set localization to the user account to "cs". Ensure that it is shown
            try (UserAttributeUpdater userUpdater = UserAttributeUpdater.forUserByUsername(testRealm(), "test-user@localhost").setAttribute(UserModel.LOCALE, "cs").update()) {
                driver.navigate().to(oauth.getLogoutUrl().build());
                Assert.assertEquals("Odhlašování", PageUtils.getPageTitle(driver)); // Logging out
                Assert.assertEquals("Čeština", logoutConfirmPage.getLanguageDropdownText());

                // Set localization together with ui_locales param. User localization should have preference
                driver.navigate().to(oauth.getLogoutUrl().uiLocales("de").build());
                Assert.assertEquals("Odhlašování", PageUtils.getPageTitle(driver)); // Logging out
                Assert.assertEquals("Čeština", logoutConfirmPage.getLanguageDropdownText());
            }

            UserAttributeUpdater.forUserByUsername(testRealm(), "test-user@localhost").removeAttribute(UserModel.LOCALE).update();

            // Removed localization from user account. Now localization set by ui_locales parameter should be used
            driver.navigate().to(oauth.getLogoutUrl().uiLocales("de").build());
            Assert.assertEquals("Abmelden", PageUtils.getPageTitle(driver)); // Logging out
            Assert.assertEquals("Deutsch", logoutConfirmPage.getLanguageDropdownText());
            logoutConfirmPage.confirmLogout();
            WaitUtils.waitForPageToLoad();
            events.expectLogout(tokenResponse.getSessionState()).removeDetail(Details.REDIRECT_URI).assertEvent();

            // Remove ui_locales from logout request. Default locale should be set
            tokenResponse = loginUser();
            driver.navigate().to(oauth.getLogoutUrl().build());
            Assert.assertEquals("Logging out", PageUtils.getPageTitle(driver));
            Assert.assertEquals("English", logoutConfirmPage.getLanguageDropdownText());
            logoutConfirmPage.confirmLogout();
            WaitUtils.waitForPageToLoad();
            events.expectLogout(tokenResponse.getSessionState()).removeDetail(Details.REDIRECT_URI).assertEvent();
        }
    }


    @Test
    public void testLocalizationDuringLogout() throws IOException {
        try (RealmAttributeUpdater realmUpdater = new RealmAttributeUpdater(testRealm()).addSupportedLocale("cs").update()) {
            OAuthClient.AccessTokenResponse tokenResponse = loginUser();

            // Display the logout page. Then change the localization to Czech, then back to english and then and logout
            driver.navigate().to(oauth.getLogoutUrl().build());

            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.openLanguage("Čeština");

            Assert.assertEquals("Odhlašování", PageUtils.getPageTitle(driver)); // Logging out
            Assert.assertEquals("Čeština", logoutConfirmPage.getLanguageDropdownText());

            logoutConfirmPage.openLanguage("English");

            Assert.assertEquals("Logging out", PageUtils.getPageTitle(driver));
            Assert.assertEquals("English", logoutConfirmPage.getLanguageDropdownText());

            // Logout
            logoutConfirmPage.confirmLogout();
            infoPage.assertCurrent();
            Assert.assertEquals("You are logged out", infoPage.getInfo());
            try {
                logoutConfirmPage.clickBackToApplicationLink();
                fail();
            } catch (NoSuchElementException ex) {
                // expected
            }

            // Display logout with ui_locales parameter set to "de"
            tokenResponse = loginUser();
            driver.navigate().to(oauth.getLogoutUrl()
                    .clientId("test-app")
                    .uiLocales("de")
                    .build());

            Assert.assertEquals("Abmelden", PageUtils.getPageTitle(driver)); // Logging out
            Assert.assertEquals("Deutsch", logoutConfirmPage.getLanguageDropdownText());

            // Change locale. It should have preference over the "de" set by ui_locales
            logoutConfirmPage.openLanguage("Čeština");
            Assert.assertEquals("Odhlašování", PageUtils.getPageTitle(driver)); // Logging out
            Assert.assertEquals("Čeština", logoutConfirmPage.getLanguageDropdownText());

            // Logout
            logoutConfirmPage.confirmLogout();

            infoPage.assertCurrent();
            Assert.assertEquals("Odhlášení bylo úspěšné", infoPage.getInfo()); // Logout success message
            infoPage.clickBackToApplicationLinkCs();
            WaitUtils.waitForPageToLoad();
            MatcherAssert.assertThat(driver.getCurrentUrl(), endsWith("/app/auth"));
        }
    }


    @Test
    public void testIncorrectChangingParameters() throws IOException {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();

        // Display the logout page. Then change the localization to Czech and logout
        driver.navigate().to(oauth.getLogoutUrl().uiLocales("de").build());

        Assert.assertEquals("Abmelden", PageUtils.getPageTitle(driver)); // Logging out
        logoutConfirmPage.openLanguage("English");

        // Try to manually change value of parameter tab_id to some incorrect value. Error should be shown in this case
        String currentUrl = driver.getCurrentUrl();
        String changedUrl = UriBuilder.fromUri(currentUrl)
                .replaceQueryParam(Constants.TAB_ID, "invalid")
                .build().toString();

        driver.navigate().to(changedUrl);
        WaitUtils.waitForPageToLoad();

        errorPage.assertCurrent();
        Assert.assertEquals("Logout failed", errorPage.getError());

        events.expectLogoutError(Errors.LOGOUT_FAILED).assertEvent();
    }


    @Test
    public void testFrontChannelLogoutWithPostLogoutRedirectUri() throws Exception {
        ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
        ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);
        rep.setFrontchannelLogout(true);
        rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, oauth.APP_ROOT + "/admin/frontchannelLogout");
        clients.get(rep.getId()).update(rep);
        try {
            oauth.clientSessionState("client-session");
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
            String idTokenString = tokenResponse.getIdToken();
            String logoutUrl = oauth.getLogoutUrl().idTokenHint(idTokenString)
                    .postLogoutRedirectUri(oauth.APP_AUTH_ROOT).build();
            driver.navigate().to(logoutUrl);
            LogoutToken logoutToken = testingClient.testApp().getFrontChannelLogoutToken();
            Assert.assertNotNull(logoutToken);

            IDToken idToken = new JWSInput(idTokenString).readJsonContent(IDToken.class);

            Assert.assertEquals(logoutToken.getIssuer(), idToken.getIssuer());
            Assert.assertEquals(logoutToken.getSid(), idToken.getSessionId());
        } finally {
            rep.setFrontchannelLogout(false);
            rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "");
            clients.get(rep.getId()).update(rep);
        }
    }

    @Test
    public void testFrontChannelLogoutWithoutSessionRequired() throws Exception {
        ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
        ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);
        rep.setFrontchannelLogout(true);
        rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, oauth.APP_ROOT + "/admin/frontchannelLogout");
        rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED, "false");
        clients.get(rep.getId()).update(rep);
        try {
            oauth.clientSessionState("client-session");
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
            String idTokenString = tokenResponse.getIdToken();
            String logoutUrl = oauth.getLogoutUrl().idTokenHint(idTokenString)
                    .postLogoutRedirectUri(oauth.APP_AUTH_ROOT).build();
            driver.navigate().to(logoutUrl);
            LogoutToken logoutToken = testingClient.testApp().getFrontChannelLogoutToken();
            Assert.assertNotNull(logoutToken);

            Assert.assertNull(logoutToken.getIssuer());
            Assert.assertNull(logoutToken.getSid());
        } finally {
            rep.setFrontchannelLogout(false);
            rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "");
            rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_SESSION_REQUIRED, "true");
            clients.get(rep.getId()).update(rep);
        }
    }

    @Test
    public void testFrontChannelLogout() throws Exception {
        ClientsResource clients = adminClient.realm(oauth.getRealm()).clients();
        ClientRepresentation rep = clients.findByClientId(oauth.getClientId()).get(0);
        rep.setName("My Testing App");
        rep.setFrontchannelLogout(true);
        rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, oauth.APP_ROOT + "/admin/frontchannelLogout");
        clients.get(rep.getId()).update(rep);
        try {
            oauth.clientSessionState("client-session");
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
            String idTokenString = tokenResponse.getIdToken();
            String logoutUrl = oauth.getLogoutUrl().idTokenHint(idTokenString).build();
            driver.navigate().to(logoutUrl);
            LogoutToken logoutToken = testingClient.testApp().getFrontChannelLogoutToken();
            Assert.assertNotNull(logoutToken);
            IDToken idToken = new JWSInput(idTokenString).readJsonContent(IDToken.class);
            Assert.assertEquals(logoutToken.getIssuer(), idToken.getIssuer());
            Assert.assertEquals(logoutToken.getSid(), idToken.getSessionId());
            assertTrue(driver.getTitle().equals("Logging out"));
            assertTrue(driver.getPageSource().contains("You are logging out from following apps"));
            assertTrue(driver.getPageSource().contains("My Testing App"));
        } finally {
            rep.setFrontchannelLogout(false);
            rep.getAttributes().put(OIDCConfigAttributes.FRONT_CHANNEL_LOGOUT_URI, "");
            clients.get(rep.getId()).update(rep);
        }
    }

    @Test
    public void logoutWithIdTokenAndDisabledClientMustWork() throws Exception {
        OAuthClient.AccessTokenResponse tokenResponse = loginUser();


        try (Closeable accountClientUpdater = ClientAttributeUpdater.forClient(adminClient, "test", oauth.getClientId())
                .setEnabled(false).update()) {

            String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).clientId("test-app").build();
            driver.navigate().to(logoutUrl);
            MatcherAssert.assertThat(true, is(isSessionActive(tokenResponse.getSessionState())));
            events.assertEmpty();

            logoutConfirmPage.confirmLogout();
            events.expectLogout(tokenResponse.getSessionState()).assertEvent();
            MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));
        }

    }

    //Login and logout with account client disabled after login
    @Test
    public void testLogoutWhenAccountClientIsDisabled() throws IOException {

        OAuthClient.AccessTokenResponse tokenResponse = loginUser();
        String sessionId = tokenResponse.getSessionState();

        try (Closeable accountClientUpdater = ClientAttributeUpdater.forClient(adminClient, "test", Constants.ACCOUNT_MANAGEMENT_CLIENT_ID)
                .setEnabled(false)
                .update()) {
            String logoutUrl = oauth.getLogoutUrl().build();
            driver.navigate().to(logoutUrl);

            events.assertEmpty();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            MatcherAssert.assertThat(false, is(isSessionActive(sessionId)));
            MatcherAssert.assertThat(false, is(isSessionActive(tokenResponse.getSessionState())));
        }
    }

    @Test
    public void logoutWithIdTokenAndRemovedClient() throws Exception {
        ClientRepresentation clientRep = ClientBuilder.create()
                .clientId("my-foo-client")
                .enabled(true)
                .baseUrl("https://foo/bar")
                .addRedirectUri(APP_REDIRECT_URI)
                .secret("password")
                .build();
        try (Response response = testRealm().clients().create(clientRep)) {
            String uuid = ApiUtil.getCreatedId(response);
            oauth.clientId("my-foo-client");

            OAuthClient.AccessTokenResponse tokenResponse = loginUser();

            // Remove client after login of user
            testRealm().clients().get(uuid).remove();

            String logoutUrl = oauth.getLogoutUrl().postLogoutRedirectUri(APP_REDIRECT_URI).idTokenHint(tokenResponse.getIdToken()).build();
            driver.navigate().to(logoutUrl);

            // Invalid redirect URI page is shown. It was not possible to verify post_logout_redirect_uri due the client was removed
            errorPage.assertCurrent();
            events.expectLogoutError(OAuthErrorException.INVALID_REDIRECT_URI).detail(Details.REDIRECT_URI, APP_REDIRECT_URI).assertEvent();
        }
    }

    // SUPPORT METHODS
    private OAuthClient.AccessTokenResponse loginUser() {
        return loginUser(false);
    }

    private OAuthClient.AccessTokenResponse loginUser(boolean consentRequired) {
        oauth.doLogin("test-user@localhost", "password");

        if (consentRequired) {
            grantPage.assertCurrent();
            grantPage.accept();
        }

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);

        oauth.clientSessionState("client-session");
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        events.clear();
        return tokenResponse;
    }

    private boolean isSessionActive(String sessionId) {
        try {
            testingClient.testing().getClientSessionsCountInUserSession("test", sessionId);
            return true;
        } catch (NotFoundException nfe) {
            return false;
        }
    }
}
