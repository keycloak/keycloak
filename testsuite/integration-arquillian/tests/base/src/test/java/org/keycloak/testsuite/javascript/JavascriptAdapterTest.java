package org.keycloak.testsuite.javascript;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.Retry;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.ProfileAssume;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.auth.page.account.Applications;
import org.keycloak.testsuite.auth.page.login.OAuthGrant;
import org.keycloak.testsuite.util.JavascriptBrowser;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.javascript.JSObjectBuilder;
import org.keycloak.testsuite.util.javascript.JavascriptTestExecutor;
import org.keycloak.testsuite.util.javascript.XMLHttpRequest;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
import static org.junit.Assert.fail;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;
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

    @Override
    protected RealmRepresentation updateRealm(RealmBuilder builder) {
        return builder.accessTokenLifespan(30 + TOKEN_LIFESPAN_LEEWAY).build();
    }

    @Before
    public void setDefaultEnvironment() {
        testAppUrl = authServerContextRootPage.toString().replace("localhost", NIP_IO_URL) + JAVASCRIPT_URL + "/index.html";

        jsDriverTestRealmLoginPage.setAuthRealm(REALM_NAME);
        oAuthGrantPage.setAuthRealm(REALM_NAME);
        applicationsPage.setAuthRealm(REALM_NAME);

        jsDriver.navigate().to(oauth.getLoginFormUrl());
        waitForPageToLoad();
        events.poll();
        jsDriver.manage().deleteAllCookies();

        jsDriver.navigate().to(testAppUrl);

        waitUntilElement(outputArea).is().present();
        assertCurrentUrlStartsWith(testAppUrl, jsDriver);
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
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn)
                .logout(this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertInitNotAuth);
    }

    @Test
    public void testLoginWithPkceS256() {
        JSObjectBuilder pkceS256 = defaultArguments().pkceS256();
        testExecutor.init(pkceS256, this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(pkceS256, this::assertSuccessfullyLoggedIn)
                .logout(this::assertOnTestAppUrl)
                .init(pkceS256, this::assertInitNotAuth);
    }

    @Test
    public void testSilentCheckSso() {
        JSObjectBuilder checkSSO = defaultArguments().checkSSOOnLoad();
        testExecutor.init(checkSSO, this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(checkSSO, this::assertSuccessfullyLoggedIn)
                .refresh()
                .init(checkSSO
                        .add("silentCheckSsoRedirectUri", authServerContextRootPage.toString().replace("localhost", NIP_IO_URL) + JAVASCRIPT_URL + "/silent-check-sso.html")
                        , this::assertSuccessfullyLoggedIn);
    }

    @Test
    public void testSilentCheckSsoLoginWithLoginIframeDisabled() {
        JSObjectBuilder checkSSO = defaultArguments().checkSSOOnLoad();
        testExecutor.init(checkSSO, this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(checkSSO, this::assertSuccessfullyLoggedIn)
                .refresh()
                .init(checkSSO
                        .disableCheckLoginIframe()
                        .add("silentCheckSsoRedirectUri", authServerContextRootPage.toString().replace("localhost", NIP_IO_URL) + JAVASCRIPT_URL + "/silent-check-sso.html")
                        , this::assertSuccessfullyLoggedIn);
    }

    @Test
    public void testSilentCheckSsoWithoutRedirectUri() {
        JSObjectBuilder checkSSO = defaultArguments().checkSSOOnLoad();
        try {
            testExecutor.init(checkSSO, this::assertInitNotAuth)
                    .login(this::assertOnLoginPage)
                    .loginForm(testUser, this::assertOnTestAppUrl)
                    .init(checkSSO, this::assertSuccessfullyLoggedIn)
                    .refresh()
                    .init(checkSSO);
            fail();
        } catch (WebDriverException e) {
            // should happen
        }
    }

    @Test
    public void testSilentCheckSsoNotAuthenticated() {
        JSObjectBuilder checkSSO = defaultArguments().checkSSOOnLoad();
        testExecutor.init(checkSSO
                .add("checkLoginIframe", false)
                .add("silentCheckSsoRedirectUri", authServerContextRootPage.toString().replace("localhost", NIP_IO_URL) + JAVASCRIPT_URL + "/silent-check-sso.html")
                , this::assertInitNotAuth);
    }

    @Test
    public void testRefreshToken() {
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .refreshToken(9999, assertOutputContains("Failed to refresh token"))
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn)
                .refreshToken(9999, assertEventsContains("Auth Refresh Success"));
    }

    @Test
    public void testRefreshTokenIfUnder30s() {
        testExecutor.init(defaultArguments(), this::assertInitNotAuth)
                .login(this::assertOnLoginPage)
                .loginForm(testUser, this::assertOnTestAppUrl)
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn)
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
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn)
                .getProfile((driver1, output, events) -> Assert.assertThat((Map<String, String>) output, hasEntry("username", testUser.getUsername())));
    }

    @Test
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

            testExecutor.init(defaultArguments(), this::assertSuccessfullyLoggedIn);

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
                .init(defaultArguments().implicitFlow(), this::assertSuccessfullyLoggedIn);

    }

    @Test
    public void testCertEndpoint() {
        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertSuccessfullyLoggedIn)
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
        testExecutor.logInAndInit(defaultArguments().implicitFlow(), testUser, this::assertSuccessfullyLoggedIn)
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
            testExecutor.logInAndInit(defaultArguments().implicitFlow(), testUser, this::assertSuccessfullyLoggedIn)
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
        testExecutor.logInAndInit(defaultArguments().implicitFlow(), testUser, this::assertSuccessfullyLoggedIn)
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
            testExecutor.logInAndInit(defaultArguments(), unauthorizedUser, this::assertSuccessfullyLoggedIn)
                    .sendXMLHttpRequest(request, output -> Assert.assertThat(output, hasEntry("status", 403L)))
                    .logout(this::assertOnTestAppUrl)
                    .refresh();
        }
        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertSuccessfullyLoggedIn)
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
                .init(defaultArguments(), this::assertSuccessfullyLoggedIn);
    }

    @Test
    public void testUpdateToken() {
        XMLHttpRequest request = XMLHttpRequest.create()
                .url(authServerContextRootPage + "/auth/admin/realms/" + REALM_NAME + "/roles")
                .method("GET")
                .addHeader("Accept", "application/json")
                .addHeader("Authorization", "Bearer ' + keycloak.token + '");

        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertSuccessfullyLoggedIn)
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
        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertSuccessfullyLoggedIn)
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

        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertSuccessfullyLoggedIn)
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

        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertSuccessfullyLoggedIn)
                .sendXMLHttpRequest(request, response -> {
                            List<UserRepresentation> users = adminClient.realm(REALM_NAME).users().search("mhajas", 0, 1);
                            assertEquals("There should be created user mhajas", 1, users.size());

                            assertThat(((String) response.get("responseHeaders")).toLowerCase(), containsString("location: " + authServerContextRootPage.toString() + "/auth/admin/realms/" + REALM_NAME + "/users/" + users.get(0).getId()));
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
                  .init(defaultArguments(), this::assertSuccessfullyLoggedIn);
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
                , this::assertSuccessfullyLoggedIn)
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
                , this::assertSuccessfullyLoggedIn)
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
        testExecutor.logInAndInit(defaultArguments(), testUser, this::assertSuccessfullyLoggedIn)
                .executeAsyncScript(
                        "var callback = arguments[arguments.length - 1];" +
                        "keycloak.updateToken(60).success(function () {" +
                        "       event(\"First callback\");" +
                        "       keycloak.updateToken(60).success(function () {" +
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
                    assertSuccessfullyLoggedIn(driver1, output, events1);
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
                    assertSuccessfullyLoggedIn(driver1, output, events1);
                    assertThat(driver1.getCurrentUrl(), containsString("#fragmentPart"));
                });
    }


}
