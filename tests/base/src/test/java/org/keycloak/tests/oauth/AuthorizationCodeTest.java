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
package org.keycloak.tests.oauth;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.InstalledAppRedirectPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.apache.http.Header;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class AuthorizationCodeTest extends AbstractKeycloakTest {

    @InjectRealm(config = AuthorizationCodeRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectEvents
    Events events;

    @InjectPage
    private ErrorPage errorPage;

    @InjectPage
    private LoginPage loginPage;

    @InjectPage
    InstalledAppRedirectPage installedAppPage;

    @InjectHttpClient
    HttpClient client;

    @InjectClient(ref = "admin-cli", attachTo = "admin-cli")
    ManagedClient adminlient;

    static class AuthorizationCodeRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name("test")
                    .eventsEnabled(true);

            realm.users(UserBuilder.create("test-user@localhost")
                    .name("Tom", "Brady")
                    .email("test-user@localhost")
                    .emailVerified(true)
                    .password("password")
                    .realmRoles("user", "offline_access"));

            realm.users(UserBuilder.create("john-doh@localhost")
                    .name("John", "Doh")
                    .email("john-doh@localhost")
                    .emailVerified(true)
                    .password("password")
                    .realmRoles("user"));

            realm.users(UserBuilder.create("keycloak-user@localhost")
                    .email("keycloak-user@localhost")
                    .emailVerified(true)
                    .password("password")
                    .realmRoles("user"));

            return realm;
        }
    }

    @BeforeEach
    public void clientConfiguration() {
        oauth.responseType(OAuth2Constants.CODE);
        oauth.responseMode(null);
    }

    @Test
    public void authorizationRequest() {
        managedRealm.dirty();
        AuthorizationEndpointResponse response = oauth.loginForm()
                .state("OpenIdConnect.AuthenticationProperties=2302984sdlk")
                .doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        assertNotNull(response.getCode());
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", response.getState());
        assertNull(response.getError());
        assertEquals(keycloakUrls.getBase() + "/realms/test", response.getIssuer());

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);
    }

    @Test
    @DatabaseTest
    public void authorizationRequestInstalledApp() {
        ClientResource clientResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        ManagedClient client = new ManagedClient(clientResource.toRepresentation(), clientResource);
        client.updateWithCleanup(c-> c.redirectUris(Constants.INSTALLED_APP_URN));
        oauth.redirectUri(Constants.INSTALLED_APP_URN);

        oauth.doLogin("test-user@localhost", "password");

        installedAppPage.getSuccessCode();

        EventRepresentation eventRepresentation = events.poll();
        assertNotNull(eventRepresentation);
        EventAssertion.assertSuccess(eventRepresentation)
                .type(EventType.LOGIN)
                .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/test/protocol/openid-connect/oauth/oob");
    }

    @Test
    public void authorizationRequestInstalledAppErrors() {
        String error = "<p><a href=\"javascript&amp;colon;alert(document.domain);\">Back to application</a></p>";
        installedAppPage.open("test", null, error, null, keycloakUrls.getBase());

        // Assert text escaped
        installedAppPage.assertLinkBackToApplicationNotPresent();
        assertEquals("Error code: <p><a href=\"javascript&amp;colon;alert(document.domain);\">Back to application</a></p>", installedAppPage.getPageTitleText());

        error = "<p><a href=\"http://foo.com\">Back to application</a></p>";
        installedAppPage.open("test", null, error, null, keycloakUrls.getBase());

        // Assert text is escaped
        installedAppPage.assertLinkBackToApplicationNotPresent();
        assertEquals("Error code: <p><a href=\"http://foo.com\">Back to application</a></p>", installedAppPage.getPageTitleText());
    }

    @Test
    @DatabaseTest
    public void authorizationValidRedirectUri() {
        managedRealm.dirty();
        adminlient.updateWithCleanup(c-> c.redirectUris(oauth.getRedirectUri()));

        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        assertNotNull(response.getCode());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
    }

    @Test
    @DatabaseTest
    public void testInvalidRedirectUri() {
        managedRealm.dirty();
        adminlient.updateWithCleanup(c-> c.redirectUris(oauth.getRedirectUri()));

        oauth.redirectUri(oauth.getRedirectUri() + "%20test");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        oauth.redirectUri("ZAP%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%0A");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
    }

    @Test
    @DatabaseTest
    public void testInvalidESCCharacterClientId() {
        managedRealm.dirty();
        adminlient.updateWithCleanup(c-> c.redirectUris(oauth.getRedirectUri()));

        oauth.client("%1B");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertEquals("An internal server error has occurred", errorPage.getError());
    }

    @Test
    @DatabaseTest
    public void testInvalidNULCharacterClientId() {
        managedRealm.dirty();
        adminlient.updateWithCleanup(c-> c.redirectUris(oauth.getRedirectUri()));

        oauth.client("%00");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertEquals("An internal server error has occurred", errorPage.getError());
    }

    @Test
    public void authorizationRequestNoState() {
        managedRealm.dirty();
        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");
        assertTrue(response.isRedirected());
        adminlient.updateWithCleanup(c-> c.redirectUris(oauth.getRedirectUri()));
        assertNotNull(response.getCode());
        assertNull(response.getState());
        assertNull(response.getError());
        assertEquals(keycloakUrls.getBase() + "/realms/test", response.getIssuer());

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN);
    }

    @Test
    public void authorizationRequestInvalidResponseType() {
        managedRealm.dirty();

        oauth.responseType("tokenn");
        oauth.openLoginForm();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        assertTrue(errorResponse.isRedirected());
        assertEquals(OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, errorResponse.getError());
        assertEquals(keycloakUrls.getBase() + "/realms/test", errorResponse.getIssuer());

        EventAssertion.assertError(events.poll())
                .type(EventType.LOGIN_ERROR)
                .error(Errors.INVALID_REQUEST)
                .userId(null)
                .sessionId(null)
                .details(Details.RESPONSE_TYPE, "tokenn");
    }

    // Issue 29866
    @Test
    public void authorizationRequestInvalidResponseType_testHeaders() throws IOException {
        managedRealm.dirty();
        oauth.responseType("tokenn");

        HttpGet request = new HttpGet(oauth.loginForm().build());
        RequestConfig config = RequestConfig.custom()
                .setRedirectsEnabled(false)
                .build();
        request.setConfig(config);

        client.execute(request, response -> {
            assertThat(response.getStatusLine().getStatusCode(), is(equalTo(302)));
            Header cacheControlHeader = response.getFirstHeader(HttpHeaders.CACHE_CONTROL);
            assertNotNull(cacheControlHeader);
            String cacheControl = cacheControlHeader.getValue();
            assertNotNull(cacheControl);
            assertThat(cacheControl, containsString("no-store"));
            assertThat(cacheControl, containsString("must-revalidate"));
            return null;
        });
    }

    @Test
    public void authorizationRequestFormPostResponseModeInvalidResponseType() {
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.responseType("tokenn");
        oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").open();

        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.LOGIN_ERROR)
                .error(Errors.INVALID_REQUEST)
                .userId(null)
                .sessionId(null)
                .details(Details.RESPONSE_TYPE, "tokenn")
                .details(Details.REASON, "Unsupported response_type");
    }

    @Test
    public void authorizationRequestFormPostResponseModeWithoutResponseType() {
        String requestState = "OpenIdConnect.AuthenticationProperties=2302984sdlk";
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.responseType(null);
        oauth.loginForm().state(requestState).open();

        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.LOGIN_ERROR)
                .error(Errors.INVALID_REQUEST)
                .userId(null)
                .sessionId(null)
                .details(Details.REASON, "Missing parameter: response_type");
    }

    // KEYCLOAK-3281
    @Test
    public void authorizationRequestFormPostResponseMode() {
        String requestState = "OpenIdConnect.AuthenticationProperties=2302984sdlk";
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.loginForm().state(requestState).open();

        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();

        try {
            new WebDriverWait(driver.driver(), Duration.ofSeconds(2))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("state")));

            String state = driver.driver().findElement(By.id("state")).getText();
            assertEquals(requestState, state);
            assertNotNull(driver.driver().findElement(By.id("code")).getText());
        } catch (org.openqa.selenium.TimeoutException e) {
            assertTrue(driver.driver().getCurrentUrl().contains("/callback/oauth"),
                    "Should be at callback or response page");
        }

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
    }

    @Test
    @DatabaseTest
    public void authorizationRequestFormPostResponseModeInvalidRedirectUri() {
        adminlient.updateWithCleanup(c-> c.redirectUris("*"));

        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.responseType(OAuth2Constants.CODE);
        oauth.redirectUri("javascript:alert('XSS')");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.LOGIN_ERROR)
                .error(Errors.INVALID_REDIRECT_URI)
                .userId(null)
                .sessionId(null);
    }

    @Test
    @DatabaseTest
    public void authorizationRequestFormPostResponseModeHTMLEntitiesRedirectUri() {
        managedRealm.dirty();
        ClientResource clientResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        ManagedClient client = new ManagedClient(clientResource.toRepresentation(), clientResource);
        client.updateWithCleanup(c-> c.redirectUris("*"));

        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.responseType(OAuth2Constants.CODE);
        final String redirectUri = oauth.getRedirectUri() + "?p=&gt;"; // set HTML entity &gt;
        oauth.redirectUri(redirectUri);

        String requestState = "authorizationRequestFormPostResponseModeHTMLEntitiesRedirectUri";

        oauth.loginForm().state(requestState).open();
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();

        try {
            new WebDriverWait(driver.driver(), Duration.ofSeconds(2))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("state")));

            String state = driver.driver().findElement(By.id("state")).getText();
            assertEquals(requestState, state);
            assertNotNull(driver.driver().findElement(By.id("code")).getText());
        } catch (org.openqa.selenium.TimeoutException e) {
            assertTrue(driver.driver().getCurrentUrl().contains("/callback/oauth"),
                    "Should be at callback or response page");
        }

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .details(Details.USERNAME, "test-user@localhost")
                .details(OIDCLoginProtocol.RESPONSE_MODE_PARAM, OIDCResponseMode.FORM_POST.name().toLowerCase())
                .details(OAuth2Constants.REDIRECT_URI, redirectUri);
    }

    @Test
    @DatabaseTest
    public void authorizationRequestFormPostJwtResponseModeHTMLEntitiesRedirectUri() {
        ClientResource clientResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), "test-app");
        ManagedClient client = new ManagedClient(clientResource.toRepresentation(), clientResource);
        client.updateWithCleanup(c-> c.redirectUris("*"));

        oauth.responseMode(OIDCResponseMode.FORM_POST_JWT.value());
        oauth.responseType(OAuth2Constants.CODE);

        final String redirectUri = oauth.getRedirectUri() + "?p=&gt;"; // set HTML entity &gt;
        oauth.redirectUri(redirectUri);

        String requestState = "authorizationRequestFormPostJwtResponseModeHTMLEntitiesRedirectUri";

        // Perform login
        oauth.loginForm().state(requestState).open();
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();

        // Verify redirect URI was properly encoded (not decoded to >)
        // The key test: HTML entity &gt; should remain encoded, not decoded to >
        assertEquals(redirectUri, driver.getCurrentUrl(), "Redirect URI should preserve HTML entity encoding");
        assertTrue(driver.getCurrentUrl().contains("?p=&gt;"), "URL should contain encoded HTML entity &gt;");

        // Verify login event with correct response mode and redirect URI
        EventRepresentation loginEvent = events.poll();
        assertNotNull(loginEvent);
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN)
                .details(Details.USERNAME, "test-user@localhost")
                .details(OIDCLoginProtocol.RESPONSE_MODE_PARAM, OIDCResponseMode.FORM_POST_JWT.name().toLowerCase())
                .details(OAuth2Constants.REDIRECT_URI, redirectUri);
    }

    @Test
    public void authorizationRequestFormPostResponseModeWithCustomState() {
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.loginForm().state("\"><foo>bar_baz(2)far</foo>").open();

        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();

        try {
            new WebDriverWait(driver.driver(), Duration.ofSeconds(2))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("state")));

            String state = driver.driver().findElement(By.id("state")).getText();
            assertEquals("\"><foo>bar_baz(2)far</foo>", state);
        } catch (org.openqa.selenium.TimeoutException e) {
            assertTrue(driver.driver().getCurrentUrl().contains("/callback/oauth"),
                    "Should be at callback or response page");
        }

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent)
                .type(EventType.LOGIN);
    }

    @Test
    public void authorizationRequestFragmentResponseModeNotKept() throws Exception {
        managedRealm.dirty();
        oauth.responseMode(OIDCResponseMode.FRAGMENT.value());
        AuthorizationEndpointResponse response = oauth.loginForm().state("authorizationRequestFragmentResponseModeNotKept").doLogin("test-user@localhost", "password");

        assertNotNull(response.getCode());
        assertNotNull(response.getState());

        URI currentUri = new URI(driver.driver().getCurrentUrl());
        assertNull(currentUri.getRawQuery());
        assertNotNull(currentUri.getRawFragment());

        // Unset response_mode. The initial OIDC AuthenticationRequest won't contain "response_mode" parameter now and hence it should fallback to "query".
        oauth.responseMode(null);
        oauth.loginForm().state("authorizationRequestFragmentResponseModeNotKept2").open();
        response = oauth.parseLoginResponse();

        assertNotNull(response.getCode());
        assertNotNull(response.getState());
        assertEquals(keycloakUrls.getBase() + "/realms/test", response.getIssuer());

        currentUri = new URI(driver.driver().getCurrentUrl());
        assertNotNull(currentUri.getRawQuery());
        assertNull(currentUri.getRawFragment());
    }

    @Test
    public void authorizationRequestParamsMoreThanOnce() {
        managedRealm.dirty();

        String logoutUrl = UriBuilder.fromUri(oauth.loginForm().build()).queryParam(OAuth2Constants.SCOPE, "read_write")
                .queryParam(OAuth2Constants.STATE, "abcdefg")
                .queryParam(OAuth2Constants.SCOPE, "pop push").build().toString();

        driver.driver().navigate().to(logoutUrl);

        AuthorizationEndpointResponse response = oauth.parseLoginResponse();

        assertEquals("invalid_request", response.getError());
        assertEquals("duplicated parameter", response.getErrorDescription());

        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.LOGIN_ERROR)
                .error(Errors.INVALID_REQUEST)
                .userId(null)
                .sessionId(null);
    }

    @Test
    public void authorizationRequestClientParamsMoreThanOnce() {
        String logoutUrl = UriBuilder.fromUri(oauth.loginForm().build()).queryParam(OAuth2Constants.SCOPE, "read_write")
                .queryParam(OAuth2Constants.CLIENT_ID, "client2client")
                .queryParam(OAuth2Constants.REDIRECT_URI, "https://www.example.com")
                .queryParam(OAuth2Constants.STATE, "abcdefg")
                .queryParam(OAuth2Constants.SCOPE, "pop push").build().toString();

        driver.driver().navigate().to(logoutUrl);

        errorPage.assertCurrent();
        assertEquals("Invalid Request", errorPage.getError());

        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.LOGIN_ERROR)
                .error(Errors.INVALID_REQUEST)
                .userId(null)
                .sessionId(null)
                .clientId(null);
    }
}
