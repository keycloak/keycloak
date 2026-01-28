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

package org.keycloak.testsuite.forms;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.ClientData;
import org.keycloak.protocol.RestartLoginCookie;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractChangeImportedUserPasswordsTest;
import org.keycloak.testsuite.ActionURIUtils;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InfoPage;
import org.keycloak.testsuite.pages.LoginExpiredPage;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.pages.LoginPasswordResetPage;
import org.keycloak.testsuite.pages.LoginPasswordUpdatePage;
import org.keycloak.testsuite.pages.LoginUpdateProfilePage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.pages.RegisterPage;
import org.keycloak.testsuite.pages.VerifyEmailPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.BrowserTabUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.GreenMailRule;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.util.TokenUtil;

import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;

import static org.keycloak.models.Constants.CLIENT_DATA;
import static org.keycloak.testsuite.AssertEvents.DEFAULT_REDIRECT_URI;
import static org.keycloak.testsuite.util.ServerURLs.getAuthServerContextRoot;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;

import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tries to simulate testing with multiple browser tabs
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class MultipleTabsLoginTest extends AbstractChangeImportedUserPasswordsTest {

    private String userId;

    @Override
    protected boolean modifyRealmForSSL() {
        return true;
    }

    @Before
    public void setup() {
        UserRepresentation user = UserBuilder.create()
                .username("login-test")
                .email("login@test.com")
                .enabled(true)
                .requiredAction(UserModel.RequiredAction.UPDATE_PROFILE.toString())
                .requiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString())
                .build();

        userId = ApiUtil.createUserAndResetPasswordWithAdminClient(testRealm(), user, generatePassword("login-test"), true);
        getCleanup().addUserId(userId);

        oauth.clientId("test-app");
    }

    @Rule
    public GreenMailRule greenMail = new GreenMailRule();

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Page
    protected AppPage appPage;

    @Page
    protected LoginPage loginPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected InfoPage infoPage;

    @Page
    protected VerifyEmailPage verifyEmailPage;

    @Page
    protected LoginPasswordResetPage resetPasswordPage;

    @Page
    protected LoginPasswordUpdatePage updatePasswordPage;

    @Page
    protected LoginUpdateProfilePage updateProfilePage;

    @Page
    protected LoginExpiredPage loginExpiredPage;

    @Page
    protected RegisterPage registerPage;

    @Page
    protected OAuthGrantPage grantPage;

    @Rule
    public AssertEvents events = new AssertEvents(this);


    @Test
    public void multipleTabsParallelLoginTest() {
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));
            oauth.openLoginForm();
            loginPage.assertCurrent();

            loginPage.login("login-test", getPassword("login-test"));
            updatePasswordPage.assertCurrent();

            // Simulate login in different browser tab tab2. I will be on loginPage again.
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));

            oauth.openLoginForm();
            loginPage.assertCurrent();

            // Login in tab2
            loginSuccessAndDoRequiredActions();

            // Try to go back to tab 1. We should be logged-in automatically
            tabUtil.closeTab(1);
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));

            // Should be back on tab1
            waitForAppPage(() -> driver.navigate().refresh());
            appPage.assertCurrent();
        }
    }

    // Simulating scenario described in https://github.com/keycloak/keycloak/issues/24112
    @Test
    public void multipleTabsParallelLoginTestWithAuthSessionExpiredInTheMiddle() {
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            multipleTabsParallelLogin(tabUtil);

            waitForAppPage(() -> loginPage.login("login-test", getPassword("login-test")));
            assertOnAppPageWithAlreadyLoggedInError(EventType.LOGIN);
        }
    }

    @Test
    public void testWithAuthSessionExpiredInTheMiddle_badRedirectUri() throws Exception {
        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Simulate incorrect login attempt to make sure that URL is on LoginActionsService URL
        loginPage.login("invalid", "invalid");
        String loginUrl = driver.getCurrentUrl();
        Assert.assertTrue(UriUtils.decodeQueryString(new URL(loginUrl).getQuery()).containsKey(CLIENT_DATA));
        getLogger().info("URL in tab1: " + driver.getCurrentUrl());

        oauth.openLoginForm();
        loginPage.assertCurrent();

        // Wait until authentication session expires
        setTimeOffset(7200000);

        loginPage.login("login-test", getPassword("login-test"));
        loginPage.assertCurrent();
        Assert.assertEquals(loginPage.getError(), "Your login attempt timed out. Login will start from the beginning.");
        events.clear();

        loginSuccessAndDoRequiredActions();

        // Remove redirectUri from the client "test-app"
        try (ClientAttributeUpdater cap = ClientAttributeUpdater.forClient(adminClient, "test", "test-app")
                .setRedirectUris(List.of("https://foo"))
                .update()) {

            events.clear();

            // Delete cookie and go to original loginURL. Restore from client_data parameter should fail due the incorrect redirectUri
            driver.manage().deleteCookieNamed(RestartLoginCookie.KC_RESTART);

            driver.navigate().to(loginUrl);
            errorPage.assertCurrent();
            Assert.assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

            events.expectLogin().user((String) null).session((String) null)
                    .error(Errors.INVALID_REDIRECT_URI)
                    .detail(Details.RESPONSE_TYPE, OIDCResponseType.CODE)
                    .detail(Details.RESPONSE_MODE, OIDCResponseMode.QUERY.value())
                    .removeDetail(Details.CONSENT)
                    .removeDetail(Details.CODE_ID)
                    .assertEvent(true);
        }
    }

    private void multipleTabsParallelLogin(BrowserTabUtil tabUtil) {
        assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));
        oauth.openLoginForm();
        loginPage.assertCurrent();
        getLogger().info("URL in tab1: " + driver.getCurrentUrl());
        // Open new tab 2
        tabUtil.newTab(oauth.loginForm().build());
        assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));
        loginPage.assertCurrent();
        getLogger().info("URL in tab2: " + driver.getCurrentUrl());
        // Wait until authentication session expires
        setTimeOffset(7200000);

        //triggers the postponed function in authChecker.js to check if the auth session cookie has changed
        WaitUtils.pause(2000);

        // Try to login in tab2. After fill login form, the login will be restarted (due KC_RESTART cookie). User can continue login
        loginPage.login("login-test", getPassword("login-test"));
        loginPage.assertCurrent();
        Assert.assertEquals(loginPage.getError(), "Your login attempt timed out. Login will start from the beginning.");
        events.clear();
        loginSuccessAndDoRequiredActions();

        // Go back to tab1. Usually should be automatically authenticated here (previously it showed "You are already logged-in")
        tabUtil.closeTab(1);
        assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));
    }

    private void loginSuccessAndDoRequiredActions() {
        loginPage.login("login-test", getPassword("login-test"));
        updatePasswordPage.changePassword(getPassword("login-test"), getPassword("login-test"));
        updateProfilePage.prepareUpdate().firstName("John").lastName("Doe3")
                .email("john@doe3.com").submit();
        appPage.assertCurrent();
    }

    // Assert browser was redirected to the appPage with "error=temporarily_unavailable" and error_description corresponding to Constants.AUTHENTICATION_EXPIRED_MESSAGE
    private void assertOnAppPageWithAlreadyLoggedInError(EventType expectedEventType) {
        if (!(driver instanceof HtmlUnitDriver)) {
            // In case of real browsers, the "tab2" is automatically refreshed when tab1 finish authentication. This is done by invoking LoginActionsService.restartSession endpoint by JS.
            // Hence event type is always RESTART_AUTHENTICATION
            expectedEventType = EventType.RESTART_AUTHENTICATION;
        }

        events.expect(expectedEventType)
                .user((String) null).error(Errors.ALREADY_LOGGED_IN)
                .detail(Details.REDIRECT_URI, Matchers.equalTo(DEFAULT_REDIRECT_URI))
                .detail(Details.REDIRECTED_TO_CLIENT, "true")
                .detail(Details.RESPONSE_TYPE, OIDCResponseType.CODE)
                .detail(Details.RESPONSE_MODE, OIDCResponseMode.QUERY.value())
                .assertEvent(true);
        appPage.assertCurrent(); // Page "You are already logged in." should not be here
        AuthorizationEndpointResponse authzResponse = oauth.parseLoginResponse();
        Assert.assertEquals(OAuthErrorException.TEMPORARILY_UNAVAILABLE, authzResponse.getError());
        Assert.assertEquals(Constants.AUTHENTICATION_EXPIRED_MESSAGE, authzResponse.getErrorDescription());
    }

    @Test
    public void multipleTabsParallelLoginTestWithAuthSessionExpiredAndRegisterClick() {
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            multipleTabsParallelLogin(tabUtil);

            waitForAppPage(() -> loginPage.clickRegister());
            assertOnAppPageWithAlreadyLoggedInError(EventType.REGISTER);
        }
    }

    @Test
    public void multipleTabsParallelLoginTestWithAuthSessionExpiredAndResetPasswordClick() {
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            multipleTabsParallelLogin(tabUtil);

            waitForAppPage(() -> loginPage.resetPassword());
            assertOnAppPageWithAlreadyLoggedInError(EventType.RESET_PASSWORD);
        }
    }

    @Test
    public void multipleTabsParallelLoginTestWithAuthSessionExpiredAndRequiredAction() {
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            // Go through login in tab1 until required actions are shown
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));
            oauth.openLoginForm();
            loginPage.assertCurrent();
            loginPage.login("login-test", getPassword("login-test"));
            updatePasswordPage.assertCurrent();
            getLogger().info("URL in tab1: " + driver.getCurrentUrl());

            // Open new tab 2
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));
            loginPage.assertCurrent();
            getLogger().info("URL in tab2: " + driver.getCurrentUrl());

            // Wait until authentication session expires
            setTimeOffset(7200000);

            //triggers the postponed function in authChecker.js to check if the auth session cookie has changed
            WaitUtils.pause(2000);

            // Try to login in tab2. After fill login form, the login will be restarted (due KC_RESTART cookie). User can continue login
            loginPage.login("login-test", getPassword("login-test"));
            loginPage.assertCurrent();
            Assert.assertEquals(loginPage.getError(), "Your login attempt timed out. Login will start from the beginning.");

            loginSuccessAndDoRequiredActions();

            // Go back to tab1. Usually should be automatically authenticated here (previously it showed "You are already logged-in")
            tabUtil.closeTab(1);
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));

            waitForAppPage(() -> updatePasswordPage.changePassword(getPassword("login-test"), getPassword("login-test")));
            assertOnAppPageWithAlreadyLoggedInError(EventType.CUSTOM_REQUIRED_ACTION);
        }
    }

    @Test
    public void multipleTabsParallelLoginTestWithAuthSessionExpiredAndRefreshInTab1() {
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            // Go through login in tab1 and do unsuccessful login attempt (to make sure that "action URL" is shown in browser URL instead of OIDC authentication request URL)
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));
            oauth.openLoginForm();
            loginPage.assertCurrent();
            loginPage.login("login-test", "bad-password");
            loginPage.assertCurrent();
            getLogger().info("URL in tab1: " + driver.getCurrentUrl());

            // Open new tab 2
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));
            loginPage.assertCurrent();
            getLogger().info("URL in tab2: " + driver.getCurrentUrl());

            // Wait until authentication session expires
            setTimeOffset(7200000);

            //triggers the postponed function in authChecker.js to check if the auth session cookie has changed
            WaitUtils.pause(2000);

            // Try to login in tab2. After fill login form, the login will be restarted (due KC_RESTART cookie). User can continue login
            loginPage.login("login-test", getPassword("login-test"));
            loginPage.assertCurrent();
            Assert.assertEquals(loginPage.getError(), "Your login attempt timed out. Login will start from the beginning.");

            loginSuccessAndDoRequiredActions();

            // Go back to tab1 and refresh the page. Should be automatically authenticated here (previously it showed "You are already logged-in")
            tabUtil.closeTab(1);
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));

            waitForAppPage(() -> {
                events.clear();
                driver.navigate().refresh();
            });
            assertOnAppPageWithAlreadyLoggedInError(EventType.LOGIN);
        }
    }

    @Test
    public void testLoginAfterLogoutFromDifferentTab() {
        try (BrowserTabUtil util = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            // login in the first tab
            oauth.openLoginForm();
            String tab1WindowHandle = util.getActualWindowHandle();
            loginSuccessAndDoRequiredActions();
            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
            AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());

            // seamless login in the second tab, user already authenticated
            util.newTab(oauth.loginForm().build());
            oauth.openLoginForm();
            appPage.assertCurrent();
            events.clear();
            // logout in the second tab
            oauth.logoutForm().idTokenHint(tokenResponse.getIdToken()).withRedirect().open();
            events.expectLogout(accessToken.getSessionState()).user(userId).session(accessToken.getSessionState()).assertEvent();
            // re-login in the second tab
            oauth.openLoginForm();
            loginPage.login("login-test", getPassword("login-test"));
            appPage.assertCurrent();

            // seamless authentication in the first tab
            util.switchToTab(tab1WindowHandle);
            oauth.openLoginForm();
            appPage.assertCurrent();
        }
    }

    @Test
    public void multipleTabsLoginAndPassiveCheck() throws MalformedURLException {
        try (BrowserTabUtil util = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            oauth.openLoginForm();
            loginPage.assertCurrent();
            String originalTab = util.getActualWindowHandle();

            // open a new tab performing the passive check
            String passiveCheckUrl = oauth.responseType("none").loginForm().prompt("none").build();
            util.newTab(passiveCheckUrl);
            MatcherAssert.assertThat(new URL(oauth.getDriver().getCurrentUrl()).getQuery(), Matchers.containsString("error=login_required"));

            // continue with the login in the first tab
            util.switchToTab(originalTab);
            loginPage.login("login-test", getPassword("login-test"));
            updatePasswordPage.assertCurrent();
        }
    }


    @Test
    public void expiredAuthenticationAction_currentCodeExpiredExecution() {
        // Simulate to open login form in 2 tabs
        oauth.openLoginForm();
        loginPage.assertCurrent();
        String actionUrl1 = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());

        // Click "register" in tab2
        loginPage.clickRegister();
        registerPage.assertCurrent();

        // Simulate going back to tab1 and confirm login form. Page "showExpired" should be shown (NOTE: WebDriver does it with GET, when real browser would do it with POST. Improve test if needed...)
        driver.navigate().to(actionUrl1);
        loginExpiredPage.assertCurrent();

        // Click on continue and assert I am on "register" form
        loginExpiredPage.clickLoginContinueLink();
        registerPage.assertCurrent();

        // Finally click "Back to login" and authenticate
        registerPage.clickBackToLogin();
        loginPage.assertCurrent();

        // Login success now
        loginSuccessAndDoRequiredActions();
    }


    @Test
    public void expiredAuthenticationAction_expiredCodeCurrentExecution() {
        // Simulate to open login form in 2 tabs
        oauth.openLoginForm();
        loginPage.assertCurrent();
        String actionUrl1 = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());

        loginPage.login("invalid", "invalid");
        loginPage.assertCurrent();
        Assert.assertEquals("Invalid username or password.", loginPage.getInputError());

        // Simulate going back to tab1 and confirm login form. Login page with "action expired" message should be shown (NOTE: WebDriver does it with GET, when real browser would do it with POST. Improve test if needed...)
        driver.navigate().to(actionUrl1);
        loginPage.assertCurrent();
        Assert.assertEquals("Action expired. Please continue with login now.", loginPage.getError());

        // Login success now
        loginSuccessAndDoRequiredActions();
    }


    @Test
    public void expiredAuthenticationAction_expiredCodeExpiredExecution() {
        // Open tab1
        oauth.openLoginForm();
        loginPage.assertCurrent();
        String actionUrl1 = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());

        // Authenticate in tab2
        loginPage.login("login-test", getPassword("login-test"));
        updatePasswordPage.assertCurrent();

        // Simulate going back to tab1 and confirm login form. Page "Page expired" should be shown (NOTE: WebDriver does it with GET, when real browser would do it with POST. Improve test if needed...)
        driver.navigate().to(actionUrl1);
        loginExpiredPage.assertCurrent();

        // Finish login
        loginExpiredPage.clickLoginContinueLink();
        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword(getPassword("login-test"), getPassword("login-test"));
        updateProfilePage.prepareUpdate().firstName("John").lastName("Doe3")
                .email("john@doe3.com").submit();
        appPage.assertCurrent();
    }


    @Test
    public void loginActionWithoutExecution() throws Exception {
        oauth.openLoginForm();

        // Manually remove execution from the URL and try to simulate the request just with "code" parameter
        String actionUrl = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());
        actionUrl = ActionURIUtils.removeQueryParamFromURI(actionUrl, Constants.EXECUTION);

        driver.navigate().to(actionUrl);

        loginExpiredPage.assertCurrent();
    }


    // Same like "loginActionWithoutExecution", but AuthenticationSession is in REQUIRED_ACTIONS action
    @Test
    public void loginActionWithoutExecutionInRequiredActions() throws Exception {
        oauth.openLoginForm();
        loginPage.assertCurrent();

        loginPage.login("login-test", getPassword("login-test"));
        updatePasswordPage.assertCurrent();

        // Manually remove execution from the URL and try to simulate the request just with "code" parameter
        String actionUrl = ActionURIUtils.getActionURIFromPageSource(driver.getPageSource());
        actionUrl = ActionURIUtils.removeQueryParamFromURI(actionUrl, Constants.EXECUTION);

        driver.navigate().to(actionUrl);

        // Back on updatePasswordPage now
        updatePasswordPage.assertCurrent();

        updatePasswordPage.changePassword(getPassword("login-test"), getPassword("login-test"));
        updateProfilePage.prepareUpdate().firstName("John").lastName("Doe3")
                .email("john@doe3.com").submit();
        appPage.assertCurrent();
    }


    // KEYCLOAK-5797
    @Test
    public void loginWithDifferentClients() throws Exception {
       String redirectUri = String.format("%s/foo/bar/baz", getAuthServerContextRoot());
       // Open tab1 and start login here
       oauth.openLoginForm();
       loginPage.assertCurrent();
       loginPage.login("login-test", "bad-password");
       String tab1Url = driver.getCurrentUrl();

       // Go to tab2 and start login with different client "root-url-client"
       oauth.clientId("root-url-client");
        oauth.redirectUri(redirectUri);
        oauth.openLoginForm();
        loginPage.assertCurrent();
        String tab2Url = driver.getCurrentUrl();

        // Go back to tab1 and finish login here
        driver.navigate().to(tab1Url);
        loginSuccessAndDoRequiredActions();

        // Go back to tab2 and finish login here. Should be on the root-url-client page
        driver.navigate().to(tab2Url);
        assertCurrentUrlStartsWith(redirectUri);
    }


    // KEYCLOAK-5938
    @Test
    public void loginWithSameClientDifferentStatesLoginInTab1() throws Exception {
        String redirectUri1 = String.format("%s/auth/realms/master/app/auth/suffix1", getAuthServerContextRoot());
        String redirectUri2 = String.format("%s/auth/realms/master/app/auth/suffix2", getAuthServerContextRoot());
        // Open tab1 and start login here
        oauth.redirectUri(redirectUri1);
        oauth.loginForm().state("state1").open();
        loginPage.assertCurrent();
        loginPage.login("login-test", "bad-password");
        String tab1Url = driver.getCurrentUrl();

        // Go to tab2 and start login with different client "root-url-client"
        oauth.redirectUri(redirectUri2);
        oauth.loginForm().state("state2").open();
        loginPage.assertCurrent();
        String tab2Url = driver.getCurrentUrl();

        // Go back to tab1 and finish login here
        driver.navigate().to(tab1Url);
        loginSuccessAndDoRequiredActions();

        // Assert I am redirected to the appPage in tab1 and have state corresponding to tab1
        appPage.assertCurrent();
        String currentUrl = driver.getCurrentUrl();
        assertCurrentUrlStartsWith(redirectUri1);
        Assert.assertTrue(currentUrl.contains("state1"));
    }


    // KEYCLOAK-5938
    @Test
    public void loginWithSameClientDifferentStatesLoginInTab2() throws Exception {
        String redirectUri1 = String.format("%s/auth/realms/master/app/auth/suffix1", getAuthServerContextRoot());
        String redirectUri2 = String.format("%s/auth/realms/master/app/auth/suffix2", getAuthServerContextRoot());
        // Open tab1 and start login here
        oauth.redirectUri(redirectUri1);
        oauth.loginForm().state("state1").open();
        loginPage.assertCurrent();
        loginPage.login("login-test", "bad-password");
        String tab1Url = driver.getCurrentUrl();

        // Go to tab2 and start login with different client "root-url-client"
        oauth.redirectUri(redirectUri2);
        oauth.loginForm().state("state2").open();
        loginPage.assertCurrent();
        String tab2Url = driver.getCurrentUrl();

        // Continue in tab2 and finish login here
        loginSuccessAndDoRequiredActions();

        // Assert I am redirected to the appPage in tab2 and have state corresponding to tab2
        appPage.assertCurrent();
        String currentUrl = driver.getCurrentUrl();
        assertCurrentUrlStartsWith(redirectUri2);
        Assert.assertTrue(currentUrl.contains("state2"));
    }

    // KEYCLOAK-12161
    @Test
    public void testEmptyBaseUrl() throws Exception {
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));

            String clientUuid = KeycloakModelUtils.generateId();
            ClientRepresentation emptyBaseclient = ClientBuilder.create()
                    .clientId("empty-baseurl-client")
                    .id(clientUuid)
                    .enabled(true)
                    .baseUrl("")
                    .addRedirectUri("*")
                    .secret("password")
                    .build();
            testRealm().clients().create(emptyBaseclient);
            getCleanup().addClientUuid(clientUuid);

            oauth.clientId("empty-baseurl-client");
            oauth.openLoginForm();
            loginPage.assertCurrent();

            loginPage.login("login-test", getPassword("login-test"));
            updatePasswordPage.assertCurrent();

            String tab1Url = driver.getCurrentUrl();

            // Simulate login in different browser tab tab2. I will be on loginPage again.
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));

            loginPage.assertCurrent();

            // Login in tab2
            loginSuccessAndDoRequiredActions();

            // Try to go back to tab 1. We should be logged-in automatically
            tabUtil.closeTab(1);
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));

            waitForAppPage(() -> driver.navigate().refresh());
            appPage.assertCurrent();
        }
    }

   @Test
    public void testLoginPageRefresh() {

       try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
           assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));
           oauth.openLoginForm();
           loginPage.assertCurrent();
           getLogger().info("URL in tab1: " + driver.getCurrentUrl());

           //delete cookie to be recreated in tab 2
           driver.manage().deleteCookieNamed("AUTH_SESSION_ID");

           // Open new tab 2
           tabUtil.newTab(oauth.loginForm().build());
           assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));
           loginPage.assertCurrent();
           getLogger().info("URL in tab2: " + driver.getCurrentUrl());

           tabUtil.switchToTab(0);
           loginPage.assertCurrent();

           //wait for the refresh in the first tab
           WaitUtils.pause(2000);

           if (driver instanceof HtmlUnitDriver) {
               // authChecker.js javascript does not work with HtmlUnitDriver. So need to "refresh" the current browser tab by running the last action in order to simulate "already_logged_in"
               // error and being redirected to client
               driver.navigate().refresh();
           }

           loginSuccessAndDoRequiredActions();

           tabUtil.switchToTab(1);

           waitForAppPage(() -> loginPage.login("login-test", getPassword("login-test")));
       }
    }

    @Test
    public void testRedirectToCorrectUrlAfterAuthSessionExpiration() {

        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {

            String redirectUri1 = String.format("%s/auth/realms/master/app/auth/suffix1", getAuthServerContextRoot());
            String redirectUri2 = String.format("%s/auth/realms/master/app/auth/suffix2", getAuthServerContextRoot());

            //open tab 1 with redirect uri 1
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));
            oauth.redirectUri(redirectUri1);
            oauth.openLoginForm();
            loginPage.assertCurrent();
            getLogger().info("URL in tab1: " + driver.getCurrentUrl());

            //open tab 2 with redirect uri 2
            oauth.redirectUri(redirectUri2);
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));
            loginPage.assertCurrent();
            getLogger().info("URL in tab2: " + driver.getCurrentUrl());

            // Wait until authentication session expires
            setTimeOffset(7200000);

            //triggers the postponed function in authChecker.js to check if the auth session cookie has changed
            WaitUtils.pause(2000);

            // Go back to tab1
            tabUtil.closeTab(1);
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));

            getLogger().info("URL in tab1 after close: " + driver.getCurrentUrl());

            // Try to login in tab2. After fill login form, the login will be restarted (due KC_RESTART cookie). User can continue login
            loginPage.login("login-test", getPassword("login-test"));
            loginPage.assertCurrent();
            Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());
            events.clear();
            loginSuccessAndDoRequiredActions();

            getLogger().info("URL in after: " + driver.getCurrentUrl());

            //redirected url should be the redirect uri 1
            Assert.assertTrue(driver.getCurrentUrl().startsWith(redirectUri1));
        }
    }

    @Test
    public void testRestartFailureWithDifferentClientAfterAuthSessionExpiration() {

        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {

            String redirectUri1 = String.format("%s/auth/realms/master/app/auth/suffix1", getAuthServerContextRoot());
            String redirectUri2 = String.format("%s/foo/bar/baz", getAuthServerContextRoot());

            //open tab 1 with redirect uri 1
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));
            oauth.redirectUri(redirectUri1);
            oauth.openLoginForm();
            loginPage.assertCurrent();
            getLogger().info("URL in tab1: " + driver.getCurrentUrl());

            //open tab 2 with redirect uri 2 and different client
            oauth.client("root-url-client");
            oauth.redirectUri(redirectUri2);
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));
            loginPage.assertCurrent();
            getLogger().info("URL in tab2: " + driver.getCurrentUrl());
            // Wait until authentication session expires
            setTimeOffset(7200000);

            //triggers the postponed function in authChecker.js to check if the auth session cookie has changed
            WaitUtils.pause(2000);

            // Go back to tab1
            tabUtil.closeTab(1);
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));

            // Try to login in tab1.
            loginPage.login("login-test", getPassword("login-test"));

            //assert cookie not found
            events.expect(EventType.LOGIN_ERROR)
                    .user(new UserRepresentation())
                    .error(Errors.INVALID_CODE)
                    .assertEvent();

            events.expect(EventType.LOGIN_ERROR)
                    .user(new UserRepresentation())
                    .error(Errors.COOKIE_NOT_FOUND)
                    .assertEvent();
        }
    }

    @Test
    public void testInjectRedirectUriInClientDataAfterAuthSessionExpiration() throws IOException {

        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {

            String redirectUri1 = String.format("%s/auth/realms/master/app/auth/suffix1", getAuthServerContextRoot());
            String redirectUri2 = String.format("%s/auth/realms/master/app/auth/suffix2", getAuthServerContextRoot());
            String redirectUriInject = String.format("%s/auth/realms/master/app/authFake/suffix1", getAuthServerContextRoot());

            //open tab 1 with redirect uri 1
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));
            oauth.redirectUri(redirectUri1);
            oauth.openLoginForm();
            loginPage.assertCurrent();
            getLogger().info("URL in tab1: " + driver.getCurrentUrl());

            //login with wrong credentials to move to authenticate page with clientData param
            loginPage.login("wrong", "wrong");

            //open tab 2
            oauth.redirectUri(redirectUri2);
            tabUtil.newTab(oauth.loginForm().build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(2));
            loginPage.assertCurrent();
            getLogger().info("URL in tab2: " + driver.getCurrentUrl());

            // Wait until authentication session expires
            setTimeOffset(7200000);

            //triggers the postponed function in authChecker.js to check if the auth session cookie has changed
            WaitUtils.pause(2000);

            // Go back to tab1
            tabUtil.closeTab(1);
            assertThat(tabUtil.getCountOfTabs(), Matchers.equalTo(1));

            //replace clientData param injecting a different redirect uri
            String currentClientDataString = ActionURIUtils.parseQueryParamsFromActionURI(oauth.getDriver().getCurrentUrl()).get(CLIENT_DATA);
            ClientData clientData = ClientData.decodeClientDataFromParameter(currentClientDataString);
            clientData.setRedirectUri(redirectUriInject);

            String injectedUrl = UriBuilder.fromUri(oauth.getDriver().getCurrentUrl())
                    .replaceQueryParam(CLIENT_DATA, clientData.encode())
                    .build().toString();

            oauth.getDriver().navigate().to(injectedUrl);

            loginPage.assertCurrent();
            Assert.assertEquals("Your login attempt timed out. Login will start from the beginning.", loginPage.getError());
            events.clear();

            loginPage.assertCurrent();
            loginSuccessAndDoRequiredActions();

            //injected redirected url should be ignored
            Assert.assertTrue(driver.getCurrentUrl().startsWith(redirectUri2));
        }
    }

    @Test
    public void testLogoutDifferentBrowserWithAuthenticationSessionStillPresent() throws Exception {
        try (BrowserTabUtil tabUtil = BrowserTabUtil.getInstanceAndSetEnv(driver)) {
            // start login with the test-app
            oauth.client("test-app").openLoginForm();
            String tab1WindowHandle = tabUtil.getActualWindowHandle();
            loginPage.assertCurrent();

            // create a second tab to initiate another login to the account-console
            tabUtil.newTab(oauth.client(Constants.ACCOUNT_CONSOLE_CLIENT_ID)
                    .redirectUri(OAuthClient.AUTH_SERVER_ROOT + "/realms/" + TEST_REALM_NAME + "/account")
                    .loginForm()
                    .codeChallenge(PkceGenerator.s256())
                    .build());
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(2));
            loginPage.assertCurrent();
            tabUtil.switchToTab(tab1WindowHandle);
            tabUtil.closeTab(1);

            // perform an online login to create the online session, auth session is maintained a short time because the other tab
            assertThat(tabUtil.getCountOfTabs(), Matchers.is(1));
            oauth.client("test-app", "password").redirectUri(OAuthClient.APP_ROOT + "/auth");
            loginPage.assertCurrent();
            loginPage.login("test-user@localhost", getPassword("test-user@localhost"));
            appPage.assertCurrent();
            AccessTokenResponse responseOnline = oauth.accessTokenRequest(oauth.parseLoginResponse().getCode()).send();
            Assert.assertNull(responseOnline.getError());
            RefreshToken onlineRefreshToken = oauth.parseRefreshToken(responseOnline.getRefreshToken());
            Assert.assertEquals(TokenUtil.TOKEN_TYPE_REFRESH, onlineRefreshToken.getType());
            Assert.assertEquals("test-user@localhost", oauth.verifyToken(responseOnline.getAccessToken()).getPreferredUsername());

            // perform an offline request for the client, automatic login
            oauth.scope("openid offline_access");
            oauth.openLoginForm();
            appPage.assertCurrent();
            AccessTokenResponse responseOffline = oauth.accessTokenRequest(oauth.parseLoginResponse().getCode()).send();
            Assert.assertNull(responseOffline.getError());
            RefreshToken offlineRefreshToken = oauth.parseRefreshToken(responseOffline.getRefreshToken());
            Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineRefreshToken.getType());
            Assert.assertEquals("test-user@localhost", oauth.verifyToken(responseOffline.getAccessToken()).getPreferredUsername());
            Assert.assertEquals(onlineRefreshToken.getSessionId(), offlineRefreshToken.getSessionId());

            // remove the online session using logout but not having the cookies (different browser)
            HttpPost logoutPost = new HttpPost(OAuthClient.AUTH_SERVER_ROOT + "/realms/" + TEST_REALM_NAME + "/protocol/openid-connect/logout");
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(
                    List.of(new BasicNameValuePair(OAuth2Constants.ID_TOKEN_HINT, responseOnline.getIdToken())),
                    StandardCharsets.UTF_8);
            logoutPost.setEntity(formEntity);
            try (CloseableHttpClient httpClient = HttpClientBuilder.create().build();
                    CloseableHttpResponse logoutResponse = httpClient.execute(logoutPost)) {
                Assert.assertEquals(Response.Status.OK.getStatusCode(), logoutResponse.getStatusLine().getStatusCode());
            }

            // perform a second offline login after logoput with another user, auth session should be different
            oauth.openLoginForm();
            loginPage.assertCurrent();
            loginPage.login("non-duplicate-email-user", getPassword("non-duplicate-email-user"));
            appPage.assertCurrent();
            responseOffline = oauth.accessTokenRequest(oauth.parseLoginResponse().getCode()).send();
            Assert.assertNull(responseOffline.getError());
            offlineRefreshToken = oauth.parseRefreshToken(responseOffline.getRefreshToken());
            System.err.println(responseOffline.getRefreshToken());
            Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, offlineRefreshToken.getType());
            Assert.assertEquals("non-duplicate-email-user", oauth.verifyToken(responseOffline.getAccessToken()).getPreferredUsername());
            Assert.assertNotEquals(onlineRefreshToken.getSessionId(), offlineRefreshToken.getSessionId());

            // refresh the token and check everything is correct
            responseOffline = oauth.doRefreshTokenRequest(responseOffline.getRefreshToken());
            Assert.assertNull(responseOffline.getError());
            Assert.assertEquals(TokenUtil.TOKEN_TYPE_OFFLINE, oauth.parseRefreshToken(responseOffline.getRefreshToken()).getType());
            Assert.assertEquals("non-duplicate-email-user", oauth.verifyToken(responseOffline.getAccessToken()).getPreferredUsername());
        }
    }

    private void waitForAppPage(Runnable htmlUnitAction) {
        if (driver instanceof HtmlUnitDriver) {
            // authChecker.js javascript does not work with HtmlUnitDriver. So need to "refresh" the current browser tab by running the last action in order to simulate "already_logged_in"
            // error and being redirected to client
            htmlUnitAction.run();
        }

        // Should be back on tab1 and logged-in automatically here
        WaitUtils.waitUntilElement(appPage.getAccountLink()).is().clickable();
    }
}
