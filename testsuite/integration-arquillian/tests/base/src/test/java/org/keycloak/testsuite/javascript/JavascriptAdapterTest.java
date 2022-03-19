package org.keycloak.testsuite.javascript;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Retry;
import org.keycloak.common.util.UriUtils;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.ClaimsRepresentation;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.SuiteContext;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
import org.keycloak.testsuite.arquillian.annotation.DisableFeature;
import org.keycloak.testsuite.auth.page.account.Applications;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.auth.page.login.UpdatePassword;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.javascript.JSObjectBuilder;
import org.keycloak.testsuite.util.javascript.JavascriptStateValidator;
import org.keycloak.testsuite.util.javascript.JavascriptTestExecutor;
import org.keycloak.testsuite.util.javascript.XMLHttpRequest;
import org.keycloak.util.JsonSerialization;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;

import static java.lang.Math.toIntExact;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.util.ServerURLs.AUTH_SERVER_HOST;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlDoesntStartWith;
import static org.keycloak.testsuite.util.URLAssert.assertCurrentUrlStartsWith;
import static org.keycloak.testsuite.util.WaitUtils.waitForPageToLoad;
import static org.keycloak.testsuite.util.WaitUtils.waitUntilElement;

/**
 * @author mhajas
 */
@AuthServerContainerExclude(AuthServer.REMOTE)
public class JavascriptAdapterTest extends AbstractJavascriptTest {

    private String testAppUrl;
    private String testAppWithInitInHeadUrl;
    protected JavascriptTestExecutor testExecutor;
    private static int TIME_SKEW_TOLERANCE = 3;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    @JavascriptBrowser
    private Applications applicationsPage;

    @Page
    @JavascriptBrowser
    private OAuthGrant oAuthGrantPage;

    @Page
    @JavascriptBrowser
    private UpdatePassword updatePasswordPage;

    @Override
    protected RealmRepresentation updateRealm(RealmBuilder builder) {
        return builder.accessTokenLifespan(30 + TOKEN_LIFESPAN_LEEWAY).build();
    }

    @Before
    public void setDefaultEnvironment() {
        String testAppRootUrl = authServerContextRootPage.toString().replace(AUTH_SERVER_HOST, JS_APP_HOST) + JAVASCRIPT_URL;
        testAppUrl = testAppRootUrl + "/index.html";
        testAppWithInitInHeadUrl = testAppRootUrl + "/init-in-head.html";

        jsDriverTestRealmLoginPage.setAuthRealm(REALM_NAME);
        oAuthGrantPage.setAuthRealm(REALM_NAME);
        applicationsPage.setAuthRealm(REALM_NAME);

        jsDriver.navigate().to(oauth.getLoginFormUrl());
        waitForPageToLoad();
        events.poll();
        jsDriver.manage().deleteAllCookies();

        navigateToTestApp(testAppUrl);

        testExecutor = JavascriptTestExecutor.create(jsDriver, jsDriverTestRealmLoginPage);

        jsDriver.manage().deleteAllCookies();

        setStandardFlowForClient();

        //tests cleanup
        oauth.setDriver(driver);
        setTimeOffset(0);
    }

    protected JSObjectBuilder defaultArguments() {
        return JSObjectBuilder.create().defaultSettings();
    }

    private void assertOnTestAppUrl(WebDriver jsDriver, Object output, WebElement events) {
        assertOnTestAppUrl(jsDriver, output, events, testAppUrl);
    }

    private void assertOnTestAppWithInitInHeadUrl(WebDriver jsDriver, Object output, WebElement events) {
        assertOnTestAppUrl(jsDriver, output, events, testAppWithInitInHeadUrl);
    }

    private void assertOnTestAppUrl(WebDriver jsDriver, Object output, WebElement events, String testAppUrl) {
        waitForPageToLoad();
        assertCurrentUrlStartsWith(testAppUrl, jsDriver);
    }

    @Test
    public void testJSConsoleAuth() {
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm( UserBuilder.create().username("user").password("invalid-password").build(),
                        (driver1, output, events) -> assertCurrentUrlDoesntStartWith(testAppUrl, driver1))
                .loginForm(UserBuilder.create().username("invalid-user").password("password").build(),
                        (driver1, output, events) -> assertCurrentUrlDoesntStartWith(testAppUrl, driver1))
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertInitAuth)
                .logout(this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertInitNotAuth);
    }

    @Test
    public void testLoginWithPkceS256() {
        JSObjectBuilder pkceS256 = defaultArguments().pkceS256();
        testExecutor.init(pkceS256, this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(pkceS256, this::assertInitAuth)
                .logout(this::assertOnTestAppUrl)
                .init(pkceS256, this::assertInitNotAuth);
    }

    @Test
    public void testSilentCheckSso() {
        JSObjectBuilder checkSSO = defaultArguments().checkSSOOnLoad()
                .add("silentCheckSsoRedirectUri", authServerContextRootPage.toString().replace(AUTH_SERVER_HOST, JS_APP_HOST) + JAVASCRIPT_URL + "/silent-check-sso.html");

        // when 3rd party cookies are disabled, the adapter has to do a full redirect to KC to check whether the user
        // is logged in or not – it can't rely on silent check-sso iframe
        testExecutor.init(checkSSO, this::assertInitNotAuth, SuiteContext.BROWSER_STRICT_COOKIES)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(checkSSO, this::assertInitAuth, false)
                .refresh()
                .init(checkSSO
                        , this::assertInitAuth, SuiteContext.BROWSER_STRICT_COOKIES);
    }

    @Test
    public void testSilentCheckSsoLoginWithLoginIframeDisabled() {
        JSObjectBuilder checkSSO = defaultArguments().checkSSOOnLoad()
                .add("silentCheckSsoRedirectUri", authServerContextRootPage.toString().replace(AUTH_SERVER_HOST, JS_APP_HOST) + JAVASCRIPT_URL + "/silent-check-sso.html");

        testExecutor.init(checkSSO, this::assertInitNotAuth, SuiteContext.BROWSER_STRICT_COOKIES)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(checkSSO, this::assertInitAuth, false)
                .refresh()
                .init(checkSSO
                        .disableCheckLoginIframe()
                        , this::assertInitAuth, SuiteContext.BROWSER_STRICT_COOKIES);
    }

    @Test
    public void testSilentCheckSsoWithFallbackDisabled() {
        JSObjectBuilder checkSSO = defaultArguments().checkSSOOnLoad().disableSilentCheckSSOFallback()
                .add("silentCheckSsoRedirectUri", authServerContextRootPage.toString().replace(AUTH_SERVER_HOST, JS_APP_HOST) + JAVASCRIPT_URL + "/silent-check-sso.html");

        testExecutor.init(checkSSO, this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(checkSSO, this::assertInitAuth)
                .refresh()
                .init(checkSSO
                        // with the fall back disabled, the adapter won't do full redirect to KC
                        , SuiteContext.BROWSER_STRICT_COOKIES ? this::assertInitNotAuth : this::assertInitAuth);
    }

    @Test
    public void testCheckSso() {
        JSObjectBuilder checkSSO = defaultArguments().checkSSOOnLoad();

        // when 3rd party cookies are disabled, the adapter has to do a full redirect to KC to check whether the user
        // is logged in or not – it can't rely on the login iframe
        testExecutor.init(checkSSO, this::assertInitNotAuth, SuiteContext.BROWSER_STRICT_COOKIES)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(checkSSO, this::assertInitAuth, false)
                .refresh()
                .init(checkSSO, this::assertInitAuth, true);
    }

    @Test
    public void testSilentCheckSsoNotAuthenticated() {
        JSObjectBuilder checkSSO = defaultArguments().checkSSOOnLoad()
                .add("checkLoginIframe", false)
                .add("silentCheckSsoRedirectUri", authServerContextRootPage.toString().replace(AUTH_SERVER_HOST, JS_APP_HOST) + JAVASCRIPT_URL + "/silent-check-sso.html");

        testExecutor.init(checkSSO
                , this::assertInitNotAuth, SuiteContext.BROWSER_STRICT_COOKIES);
    }

    @Test
    // KEYCLOAK-13206
    public void testIframeInit() {
        JSObjectBuilder iframeInterval = defaultArguments().setCheckLoginIframeIntervalTo1(); // to speed up the test a bit
        testExecutor.init(iframeInterval)
                .login()
                .loginForm(testUser)
                .init(iframeInterval)
                .wait(2000, (driver1, output, events) -> { // iframe is initialized after ~1 second, 2 seconds is just to be sure
                    assertAdapterIsLoggedIn(driver1, output, events);
                    final String logMsg = "3rd party cookies aren't supported by this browser.";
                    if (SuiteContext.BROWSER_STRICT_COOKIES) {
                        // this is here not really to test the log but also to make sure the browser is configured properly
                        // and cookies were blocked
                        assertEventsWebElementContains(logMsg, driver1, output, events);
                    }
                    else {
                        assertEventsWebElementDoesntContain(logMsg, driver1, output, events);
                    }
                });
    }

    @Test
    public void testRefreshToken() {
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .refreshToken(9999, assertOutputContains("Failed to refresh token"))
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertInitAuth)
                .refreshToken(9999, assertEventsContains("Auth Refresh Success"));
    }

    @Test
    public void testRefreshTokenIfUnder30s() {
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertInitAuth)
                .refreshToken(30, assertOutputContains("Token not refreshed, valid for"))
                .addTimeSkew(-5) // instead of wait move in time
                .refreshToken(30, assertEventsContains("Auth Refresh Success"));
    }

    @Test
    public void testGetProfile() {
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .getProfile(assertOutputContains("Failed to load profile"))
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertInitAuth)
                .getProfile((driver1, output, events) -> Assert.assertThat((Map<String, String>) output, hasEntry("username", testUser.getUsername())));
    }

    @Test
    @DisableFeature(value = Profile.Feature.ACCOUNT2, skipRestart = true) // TODO remove this (KEYCLOAK-16228)
    public void grantBrowserBasedApp() {
        Assume.assumeTrue("This test doesn't work with phantomjs", !"phantomjs".equals(System.getProperty("js.browser")));

        ClientResource clientResource = ApiUtil.findClientResourceByClientId(adminClient.realm(REALM_NAME), CLIENT_ID);
        ClientRepresentation client = clientResource.toRepresentation();
        try {
            client.setConsentRequired(true);
            clientResource.update(client);

            testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                  .login(this::assertOnLoginPage)
                  .loginForm(testUser, (driver1, output, events) -> assertTrue(oAuthGrantPage.isCurrent(driver1))
                        // I am not sure why is this driver1 argument to isCurrent necessary, but I got exception without it
                  );

            oAuthGrantPage.accept();

            EventRepresentation loginEvent = events.expectLogin()
                  .client(CLIENT_ID)
                  .detail(Details.CONSENT, Details.CONSENT_VALUE_CONSENT_GRANTED)
                  .detail(Details.REDIRECT_URI, testAppUrl)
                  .detail(Details.USERNAME, testUser.getUsername())
                  .assertEvent();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);

            testExecutor.init(defaultArguments(), this::assertInitAuth);

            applicationsPage.navigateTo();
            events.expectCodeToToken(codeId, loginEvent.getSessionId()).client(CLIENT_ID).assertEvent();

            applicationsPage.revokeGrantForApplication(CLIENT_ID);
            events.expect(EventType.REVOKE_GRANT)
                  .client("account")
                  .detail(Details.REVOKED_CLIENT, CLIENT_ID)
                  .assertEvent();

            jsDriver.navigate().to(testAppUrl);
            testExecutor.configure() // need to configure because we refreshed page
                  .init(defaultArguments(), this::assertInitNotAuth)
                  .login((driver1, output, events) -> assertTrue(oAuthGrantPage.isCurrent(driver1)));
        } finally {
            // Clean
            client.setConsentRequired(false);
            clientResource.update(client);
        }
    }

    @Test
    public void implicitFlowTest() {
        testExecutor.init(defaultArguments().implicitFlow(), this::assertInitNotAuth)
                .login(this::assertOnTestAppUrl)
                .errorResponse(assertOutputContains("Implicit flow is disabled for the client"));

        setImplicitFlowForClient();
        jsDriver.navigate().to(testAppUrl);

        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnTestAppUrl)
                .errorResponse(assertOutputContains("Standard flow is disabled for the client"));
        jsDriver.navigate().to(testAppUrl);

        testExecutor.init(defaultArguments().implicitFlow(), this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments().implicitFlow(), this::assertInitAuth);

    }

    @Test
    public void testCertEndpoint() {
        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertInitAuth)
                .sendXMLHttpRequest(XMLHttpRequest.create()
                                .url(authServerContextRootPage + "/auth/realms/" + REALM_NAME + "/protocol/openid-connect/certs")
                                .method("GET")
                                .addHeader("Accept", "application/json")
                                .addHeader("Authorization", "Bearer ' + keycloak.token + '"),
                        assertResponseStatus(200));
    }

    @Test
    public void implicitFlowQueryTest() {
        setImplicitFlowForClient();
        testExecutor.init(JSObjectBuilder.create().implicitFlow().queryResponse(), this::assertInitNotAuth)
                .login((driver1, output, events1) -> Retry.execute(
                        () -> assertThat(driver1.getCurrentUrl(), containsString("Response_mode+%27query%27+not+allowed")),
                        20, 50)
                );
    }

    @Test
    public void implicitFlowRefreshTokenTest() {
        setImplicitFlowForClient();
        testExecutor.logInAndInit(defaultArguments().implicitFlow(), testUser, this::assertInitAuth)
            .refreshToken(9999, assertOutputContains("Failed to refresh token"));
    }

    @Test
    public void implicitFlowOnTokenExpireTest() {
        RealmRepresentation realm = adminClient.realms().realm(REALM_NAME).toRepresentation();
        Integer storeAccesTokenLifespan = realm.getAccessTokenLifespanForImplicitFlow();
        try {
            realm.setAccessTokenLifespanForImplicitFlow(5);
            adminClient.realms().realm(REALM_NAME).update(realm);

            setImplicitFlowForClient();
            testExecutor.logInAndInit(defaultArguments().implicitFlow(), testUser, this::assertInitAuth)
                  .addTimeSkew(-5); // Move in time instead of wait

            waitUntilElement(eventsArea).text().contains("Access token expired");
        } finally {
            // Get to origin state
            realm.setAccessTokenLifespanForImplicitFlow(storeAccesTokenLifespan);
            adminClient.realms().realm(REALM_NAME).update(realm);
        }
    }

    @Test
    public void implicitFlowCertEndpoint() {
        setImplicitFlowForClient();
        testExecutor.logInAndInit(defaultArguments().implicitFlow(), testUser, this::assertInitAuth)
                .sendXMLHttpRequest(XMLHttpRequest.create()
                                .url(authServerContextRootPage + "/auth/realms/" + REALM_NAME + "/protocol/openid-connect/certs")
                                .method("GET")
                                .addHeader("Accept", "application/json")
                                .addHeader("Authorization", "Bearer ' + keycloak.token + '"),
                        assertResponseStatus(200));
    }

    @Test
    public void testBearerRequest() {
        XMLHttpRequest request = XMLHttpRequest.create()
                .url(authServerContextRootPage + "/auth/admin/realms/" + REALM_NAME + "/roles")
                .method("GET")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer ' + keycloak.token + '");

        testExecutor.init(defaultArguments())
                // Possibility of 0 and 401 is caused by this issue: https://issues.redhat.com/browse/KEYCLOAK-12686
                .sendXMLHttpRequest(request, response -> assertThat(response, hasEntry(is("status"), anyOf(is(0L), is(401L)))))
                .refresh();
        if (!"phantomjs".equals(System.getProperty("js.browser"))) {
            // I have no idea why, but this request doesn't work with phantomjs, it works in chrome
            testExecutor.logInAndInit(defaultArguments(), unauthorizedUser, this::assertInitAuth)
                    .sendXMLHttpRequest(request, output -> Assert.assertThat(output, hasEntry("status", 403L)))
                    .logout(this::assertOnTestAppUrl)
                    .refresh();
        }
        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertInitAuth)
                .sendXMLHttpRequest(request, assertResponseStatus(200));
    }

    @Test
    public void loginRequiredAction() {
        try {
            testExecutor.init(defaultArguments().loginRequiredOnLoad());
            // This throws exception because when JavascriptExecutor waits for AsyncScript to finish
            // it is redirected to login page and executor gets no response

            throw new RuntimeException("Probably the login-required OnLoad mode doesn't work, because testExecutor should fail with error that page was redirected.");
        } catch (WebDriverException ex) {
            // should happen
        }

        testExecutor.loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertInitAuth);
    }

    /**
     * Test for scope handling via {@code initOptions}: <pre>{@code
     * Keycloak keycloak = new Keycloak(); keycloak.init({.... scope: "profile email phone"})
     * }</pre>
     * See KEYCLOAK-14412
     */
    @Test
    public void testScopeInInitOptionsShouldBeConsideredByLoginUrl() {

        JSObjectBuilder initOptions = defaultArguments()
                .loginRequiredOnLoad()
                // phone is optional client scope
                .add("scope", "openid profile email phone");

        try {
            testExecutor.init(initOptions);
            // This throws exception because when JavascriptExecutor waits for AsyncScript to finish
            // it is redirected to login page and executor gets no response

            throw new RuntimeException("Probably the login-required OnLoad mode doesn't work, because testExecutor should fail with error that page was redirected.");
        } catch (WebDriverException ex) {
            // should happen
        }

        testExecutor.loginForm(testUser, this::assertOnTestAppUrl)
                    .init(initOptions, this::assertAdapterIsLoggedIn)
                    .executeScript("return window.keycloak.tokenParsed.scope", assertOutputContains("phone"));
    }

    /**
     * Test for scope handling via {@code loginOptions}: <pre>{@code
     * Keycloak keycloak = new Keycloak(); keycloak.login({.... scope: "profile email phone"})
     * }</pre>
     * See KEYCLOAK-14412
     */
    @Test
    public void testScopeInLoginOptionsShouldBeConsideredByLoginUrl() {

        testExecutor.configure().init(defaultArguments());

        JSObjectBuilder loginOptions = JSObjectBuilder.create().add("scope", "profile email phone");

        testExecutor.login(loginOptions, (JavascriptStateValidator) (driver, output, events) -> {
            assertThat(driver.getCurrentUrl(), containsString("&scope=openid%20profile%20email%20phone"));
        });
    }

    /**
     * Test for acr handling via {@code loginOptions}: <pre>{@code
     * Keycloak keycloak = new Keycloak(); keycloak.login({.... acr: { values: ["foo", "bar"], essential: false}})
     * }</pre>
     */
    @Test
    public void testAcrInLoginOptionsShouldBeConsideredByLoginUrl() {
        // Test when no "acr" option given. Claims parameter won't be passed to Keycloak server
        testExecutor.configure().init(defaultArguments());
        JSObjectBuilder loginOptions = JSObjectBuilder.create();

        testExecutor.login(loginOptions, (JavascriptStateValidator) (driver, output, events) -> {
            try {
                String queryString = new URL(driver.getCurrentUrl()).getQuery();
                String claimsParam = UriUtils.decodeQueryString(queryString).getFirst(OIDCLoginProtocol.CLAIMS_PARAM);
                Assert.assertNull(claimsParam);
            } catch (IOException ioe) {
                throw new AssertionError(ioe);
            }
        });

        // Test given "acr" option will be translated into the "claims" parameter passed to Keycloak server
        jsDriver.navigate().to(testAppUrl);
        testExecutor.configure().init(defaultArguments());

        JSObjectBuilder acr1 = JSObjectBuilder.create()
                .add("values", new String[] {"foo", "bar"})
                .add("essential", false);
        loginOptions = JSObjectBuilder.create().add("acr", acr1);

        testExecutor.login(loginOptions, (JavascriptStateValidator) (driver, output, events) -> {
            try {
                String queryString = new URL(driver.getCurrentUrl()).getQuery();
                String claimsParam = UriUtils.decodeQueryString(queryString).getFirst(OIDCLoginProtocol.CLAIMS_PARAM);
                Assert.assertNotNull(claimsParam);

                ClaimsRepresentation claimsRep = JsonSerialization.readValue(claimsParam, ClaimsRepresentation.class);
                ClaimsRepresentation.ClaimValue<String> claimValue = claimsRep.getClaimValue(IDToken.ACR, ClaimsRepresentation.ClaimContext.ID_TOKEN, String.class);
                Assert.assertNames(claimValue.getValues(), "foo", "bar");
                Assert.assertThat(claimValue.isEssential(), is(false));
            } catch (IOException ioe) {
                throw new AssertionError(ioe);
            }
        });
    }

    @Test
    public void testUpdateToken() {
        XMLHttpRequest request = XMLHttpRequest.create()
                .url(authServerContextRootPage + "/auth/admin/realms/" + REALM_NAME + "/roles")
                .method("GET")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer ' + keycloak.token + '");

        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertInitAuth)
                .addTimeSkew(-33);
        setTimeOffset(33);
        testExecutor.refreshToken(5, assertEventsContains("Auth Refresh Success"));

        setTimeOffset(67);
        testExecutor.addTimeSkew(-34)
                // Possibility of 0 and 401 is caused by this issue: https://issues.redhat.com/browse/KEYCLOAK-12686
                .sendXMLHttpRequest(request, response -> assertThat(response, hasEntry(is("status"), anyOf(is(0L), is(401L)))))
                .refreshToken(5, assertEventsContains("Auth Refresh Success"))
                .sendXMLHttpRequest(request, assertResponseStatus(200));
    }

    @Test
    public void timeSkewTest() {
        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertInitAuth)
                .checkTimeSkew((driver1, output, events) -> assertThat(toIntExact((long) output),
                        is(
                            both(greaterThan(0 - TIME_SKEW_TOLERANCE))
                            .and(lessThan(TIME_SKEW_TOLERANCE))
                        )
                ));

        setTimeOffset(40);

        testExecutor.refreshToken(9999, assertEventsContains("Auth Refresh Success"))
                .checkTimeSkew((driver1, output, events) -> assertThat(toIntExact((long) output),
                        is(
                            both(greaterThan(-40 - TIME_SKEW_TOLERANCE))
                            .and(lessThan(-40 + TIME_SKEW_TOLERANCE))
                        )
                ));
    }

    @Test
    public void testOneSecondTimeSkewTokenUpdate() {
        setTimeOffset(1);

        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertInitAuth)
                .refreshToken(9999, assertEventsContains("Auth Refresh Success"));

        try {
            // The events element should contain "Auth logout" but we need to wait for it
            // and text().not().contains() doesn't wait. With KEYCLOAK-4179 it took some time for "Auth Logout" to be present
            waitUntilElement(eventsArea).text().contains("Auth Logout");

            throw new RuntimeException("The events element shouldn't contain \"Auth Logout\" text");
        } catch (TimeoutException e) {
            // OK
        }
    }

    @Test
    public void testLocationHeaderInResponse() {
        XMLHttpRequest request = XMLHttpRequest.create()
                .url(authServerContextRootPage + "/auth/admin/realms/" + REALM_NAME + "/users")
                .method("POST")
                .content("JSON.stringify(JSON.parse('{\"emailVerified\" : false, \"enabled\" : true, \"username\": \"mhajas\", \"firstName\" :\"First\", \"lastName\":\"Last\",\"email\":\"email@redhat.com\", \"attributes\": {}}'))")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer ' + keycloak.token + '")
                .addHeader("Content-Type", "application/json; charset=UTF-8");

        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertInitAuth)
                .sendXMLHttpRequest(request, response -> {
                            List<UserRepresentation> users = adminClient.realm(REALM_NAME).users().search("mhajas", 0, 1);
                            assertEquals("There should be created user mhajas", 1, users.size());

                            assertThat(((String) response.get("responseHeaders")).toLowerCase(), containsString("location: " + authServerContextRootPage.toString() + "/auth/admin/realms/" + REALM_NAME + "/users/" + users.get(0).getId()));
                        });
    }

    @Test
    public void equalsSignInRedirectUrl() {
        testAppUrl = authServerContextRootPage.toString().replace(AUTH_SERVER_HOST, JS_APP_HOST) + JAVASCRIPT_URL + "/index.html?test=bla=bla&super=man";
        jsDriver.navigate().to(testAppUrl);

        JSObjectBuilder arguments = defaultArguments();

        testExecutor.init(arguments, this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(arguments, (driver1, output1, events2) -> {
                    assertTrue(driver1.getCurrentUrl().contains("bla=bla"));
                    assertInitAuth(driver1, output1, events2);
                });
    }

    @Test
    public void spaceInRealmNameTest() {
        // Unfortunately this test doesn't work on phantomjs
        // it looks like phantomjs double encode %20 => %25%20
        Assume.assumeTrue("This test doesn't work with phantomjs", !"phantomjs".equals(System.getProperty("js.browser")));

        try {
            adminClient.realm(REALM_NAME).update(RealmBuilder.edit(adminClient.realm(REALM_NAME).toRepresentation()).name(SPACE_REALM_NAME).build());

            JSObjectBuilder configuration = JSObjectBuilder.create()
                  .add("url", authServerContextRootPage + "/auth")
                  .add("realm", SPACE_REALM_NAME)
                  .add("clientId", CLIENT_ID);

            testAppUrl = authServerContextRootPage + JAVASCRIPT_ENCODED_SPACE_URL + "/index.html";
            jsDriver.navigate().to(testAppUrl);
            jsDriverTestRealmLoginPage.setAuthRealm(SPACE_REALM_NAME);

            testExecutor.configure(configuration)
                  .init(defaultArguments(), this::assertInitNotAuth)
                  .login(this::assertOnLoginPage)
                  .loginForm(testUser, this::assertOnTestAppUrl)
                  .configure(configuration)
                  .init(defaultArguments(), this::assertInitAuth);
        } finally {
            adminClient.realm(SPACE_REALM_NAME).update(RealmBuilder.edit(adminClient.realm(SPACE_REALM_NAME).toRepresentation()).name(REALM_NAME).build());
            jsDriverTestRealmLoginPage.setAuthRealm(REALM_NAME);
        }
    }

    @Test
    public void initializeWithTokenTest() {
        oauth.setDriver(jsDriver);

        oauth.realm(REALM_NAME);
        oauth.clientId(CLIENT_ID);
        oauth.redirectUri(testAppUrl);
        oauth.doLogin(testUser);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String token = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        testExecutor.init(JSObjectBuilder.create()
                    .add("token", token)
                    .add("refreshToken", refreshToken)
                , (driver1, output, events) -> {
                    assertInitAuth(driver1, output, events);
                    if (SuiteContext.BROWSER_STRICT_COOKIES) {
                        // iframe is unsupported so a token refresh had to be performed
                        assertEventsContains("Auth Refresh Success").validate(driver1, output, events);
                    }
                    else {
                        assertEventsDoesntContain("Auth Refresh Success").validate(driver1, output, events);
                    }
                })
                .refreshToken(9999, assertEventsContains("Auth Refresh Success"));
    }

    @Test
    public void initializeWithTimeSkew() {
        oauth.setDriver(jsDriver); // Oauth need to login with jsDriver

        // Get access token and refresh token to initialize with
        setTimeOffset(600);
        oauth.realm(REALM_NAME);
        oauth.clientId(CLIENT_ID);
        oauth.redirectUri(testAppUrl);
        oauth.doLogin(testUser);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String token = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        // Perform test
        testExecutor.init(JSObjectBuilder.create()
                        .add("token", token)
                        .add("refreshToken", refreshToken)
                        .add("timeSkew", -600)
                , this::assertInitAuth)
                .checkTimeSkew((driver1, output, events) -> assertThat((Long) output, is(
                                both(greaterThan(-600L - TIME_SKEW_TOLERANCE))
                                .and(lessThan(-600L + TIME_SKEW_TOLERANCE))
                        )))
                .refreshToken(9999, assertEventsContains("Auth Refresh Success"))
                .checkTimeSkew((driver1, output, events) -> assertThat((Long) output, is(
                                both(greaterThan(-600L - TIME_SKEW_TOLERANCE))
                                .and(lessThan(-600L + TIME_SKEW_TOLERANCE))
                )));
    }

    @Test
    // KEYCLOAK-4503
    public void initializeWithRefreshToken() {

        oauth.setDriver(jsDriver); // Oauth need to login with jsDriver

        oauth.realm(REALM_NAME);
        oauth.clientId(CLIENT_ID);
        oauth.redirectUri(testAppUrl);
        oauth.doLogin(testUser);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");
        String token = tokenResponse.getAccessToken();
        String refreshToken = tokenResponse.getRefreshToken();

        testExecutor.init(JSObjectBuilder.create()
                        .add("refreshToken", refreshToken)
                , (driver1, output, events) -> {
            assertInitNotAuth(driver1, output, events);
            waitUntilElement(events).text().not().contains("Auth Success");
        });
    }

    @Test
    public void reentrancyCallbackTest() {
        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertInitAuth)
            .executeAsyncScript(
                "var callback = arguments[arguments.length - 1];" +
                "keycloak.updateToken(60).then(function () {" +
                "       event(\"First callback\");" +
                "       keycloak.updateToken(60).then(function () {" +
                "          event(\"Second callback\");" +
                "          callback(\"Success\");" +
                "       });" +
                "    }" +
                ");"
                , (driver1, output, events) -> {
                    waitUntilElement(events).text().contains("First callback");
                    waitUntilElement(events).text().contains("Second callback");
                    waitUntilElement(events).text().not().contains("Auth Logout");
                }
            );
    }

    @Test
    public void fragmentInURLTest() {
        jsDriver.navigate().to(testAppUrl + "#fragmentPart");
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), (driver1, output, events1) -> {
                    assertInitAuth(driver1, output, events1);
                    assertThat(driver1.getCurrentUrl(), containsString("#fragmentPart"));
                });
    }

    @Test
    public void fragmentInLoginFunction() {
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(JSObjectBuilder.create()
                        .add("redirectUri", testAppUrl + "#fragmentPart")
                        .build(), this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), (driver1, output, events1) -> {
                    assertInitAuth(driver1, output, events1);
                    assertThat(driver1.getCurrentUrl(), containsString("#fragmentPart"));
                });
    }

    @Test
    public void testRefreshTokenWithDeprecatedPromiseHandles() {
        String refreshWithDeprecatedHandles = "var callback = arguments[arguments.length - 1];" +
                "   window.keycloak.updateToken(9999).success(function (refreshed) {" +
            "            callback('Success handle');" +
                "   }).error(function () {" +
                "       callback('Error handle');" +
                "   });";

        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .executeAsyncScript(refreshWithDeprecatedHandles, assertOutputContains("Error handle"))
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertInitAuth)
                .executeAsyncScript(refreshWithDeprecatedHandles, assertOutputContains("Success handle"));
    }

    @Test
    public void testAIAFromJavascriptAdapterSuccess() {
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(JSObjectBuilder.create()
                        .add("action", "UPDATE_PASSWORD")
                        .build(), this::assertOnLoginPage)
                .loginForm(testUser);

        updatePasswordPage.updatePasswords(USER_PASSWORD, USER_PASSWORD);

        testExecutor.init(defaultArguments(), (driver1, output, events1) -> {
            assertInitAuth(driver1, output, events1);
            waitUntilElement(events1).text().contains("AIA status: success");
        });
    }

    @Test
    public void testAIAFromJavascriptAdapterCancelled() {
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(JSObjectBuilder.create()
                        .add("action", "UPDATE_PASSWORD")
                        .build(), this::assertOnLoginPage)
                .loginForm(testUser);

        updatePasswordPage.cancel();

        testExecutor.init(defaultArguments(), (driver1, output, events1) -> {
            assertInitAuth(driver1, output, events1);
            waitUntilElement(events1).text().contains("AIA status: cancelled");
        });
    }

    @Test
    // KEYCLOAK-15158
    public void testInitInHead() {
        navigateToTestApp(testAppWithInitInHeadUrl);

        testExecutor.validateOutputField(this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppWithInitInHeadUrl)
                .validateOutputField(this::assertInitAuth);
    }

    @Test
    public void check3pCookiesMessageCallbackTest() {
        testExecutor.attachCheck3pCookiesIframeMutationObserver()
                .init(defaultArguments(), this::assertInitNotAuth);
    }

    // In case of incorrect/unavailable realm provided in KeycloakConfig,
    // JavaScript Adapter init() should fail-fast and reject Promise with KeycloakError.
    @Test
    public void checkInitWithInvalidRealm() {

        JSObjectBuilder keycloakConfig = JSObjectBuilder.create()
                .add("url", authServerContextRootPage + "/auth")
                .add("realm", "invalid-realm-name")
                .add("clientId", CLIENT_ID);

        JSObjectBuilder initOptions = defaultArguments().add("messageReceiveTimeout", 5000);

        testExecutor
                .configure(keycloakConfig)
                .init(initOptions, assertErrorResponse("Timeout when waiting for 3rd party check iframe message."));

    }

    // In case of unavailable Authorization Server due to network or other kind of problems,
    // JavaScript Adapter init() should fail-fast and reject Promise with KeycloakError.
    @Test
    public void checkInitWithUnavailableAuthServer() {

        JSObjectBuilder keycloakConfig = JSObjectBuilder.create()
                .add("url", "https://localhost:12345/auth")
                .add("realm", REALM_NAME)
                .add("clientId", CLIENT_ID);

        JSObjectBuilder initOptions = defaultArguments().add("messageReceiveTimeout", 5000);

        testExecutor
                .configure(keycloakConfig)
                .init(initOptions, assertErrorResponse("Timeout when waiting for 3rd party check iframe message."));

    }

    protected void assertAdapterIsLoggedIn(WebDriver driver1, Object output, WebElement events) {
        assertTrue(testExecutor.isLoggedIn());
    }

    protected void navigateToTestApp(final String testAppUrl) {
        jsDriver.navigate().to(testAppUrl);
        waitUntilElement(outputArea).is().present();
        assertCurrentUrlStartsWith(testAppUrl, jsDriver);
    }
}
