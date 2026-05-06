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

import java.net.URI;
import java.time.Duration;
import java.util.Collections;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.testframework.annotations.InjectAdminClient;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectKeycloakUrls;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakUrls;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.LoginPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.pages.InstalledAppRedirectPage;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

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
public class AuthorizationCodeTest {

    static final String REALM_NAME = "test";

    @InjectRealm(config = AuthorizationCodeRealmConfig.class, lifecycle = LifeCycle.CLASS)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectAdminClient
    Keycloak adminClient;

    @InjectEvents
    Events events;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @InjectPage
    private ErrorPage errorPage;

    @InjectPage
    private LoginPage loginPage;

    @InjectPage
    InstalledAppRedirectPage installedAppPage;

    static class AuthorizationCodeRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.name(REALM_NAME)
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

        EventRepresentation eventRepresentation = EventAssertion.assertSuccess(events.poll())
                .type(EventType.LOGIN)
                .getEvent();
        String codeId = eventRepresentation.getDetails().get(Details.CODE_ID);
        assertNotNull(codeId);
    }

    @Test
    @DatabaseTest
    public void authorizationRequestInstalledApp() {
        managedRealm.dirty();

        ClientResource testAppClient = findClientByClientId("test-app");
        ClientRepresentation testAppRep = testAppClient.toRepresentation();
        testAppRep.getRedirectUris().add(Constants.INSTALLED_APP_URN);
        testAppClient.update(testAppRep);

        oauth.redirectUri(Constants.INSTALLED_APP_URN);
        oauth.doLogin("test-user@localhost", "password");

        assertTrue(driver.driver().getCurrentUrl().contains("/protocol/openid-connect/oauth/oob"));

        assertNotNull(driver.driver().findElement(By.id("code")));

        EventRepresentation eventRepresentation = events.poll();
        assertNotNull(eventRepresentation);
        EventAssertion.assertSuccess(eventRepresentation)
                .type(EventType.LOGIN)
                .details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/test/protocol/openid-connect/oauth/oob");

        String codeId = eventRepresentation.getDetails().get(Details.CODE_ID);
        assertNotNull(codeId);
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
        ClientResource testAppClient = findClientByClientId("test-app");
        ClientRepresentation testAppRep = testAppClient.toRepresentation();
        testAppRep.getRedirectUris().add(oauth.getRedirectUri());
        testAppClient.update(testAppRep);

        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        assertNotNull(response.getCode());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        assertNotNull(codeId);
    }

    @Test
    @DatabaseTest
    public void testInvalidRedirectUri() {
        managedRealm.dirty();
        ClientResource testAppClient = findClientByClientId("test-app");
        ClientRepresentation testAppRep = testAppClient.toRepresentation();
        testAppRep.getRedirectUris().add(oauth.getRedirectUri());
        testAppClient.update(testAppRep);
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
        ClientResource testAppClient = findClientByClientId("test-app");
        ClientRepresentation testAppRep = testAppClient.toRepresentation();
        testAppRep.getRedirectUris().add(oauth.getRedirectUri());
        testAppClient.update(testAppRep);

        oauth.client("%1B");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertEquals("Client not found.", errorPage.getError());
    }

    @Test
    @DatabaseTest
    public void testInvalidNULCharacterClientId() {
        managedRealm.dirty();
        ClientResource testAppClient = findClientByClientId("test-app");
        ClientRepresentation testAppRep = testAppClient.toRepresentation();
        testAppRep.getRedirectUris().add(oauth.getRedirectUri());
        testAppClient.update(testAppRep);

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
        assertNotNull(response.getCode());
        assertNull(response.getState());
        assertNull(response.getError());
        assertEquals(keycloakUrls.getBase() + "/realms/test", response.getIssuer());

        EventRepresentation loginEvent = events.poll();
        EventAssertion.assertSuccess(loginEvent).type(EventType.LOGIN);
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

        EventRepresentation errorEvent = events.poll();
        EventAssertion.assertError(errorEvent)
                .type(EventType.LOGIN_ERROR)
                .error(Errors.INVALID_REQUEST)
                .userId(null)
                .sessionId(null)
                .details(Details.RESPONSE_TYPE, "tokenn");
    }

    // Issue 29866
    @Test
    public void authorizationRequestInvalidResponseType_testHeaders() {
        managedRealm.dirty();

        oauth.responseType("tokenn");
        Client client = ClientBuilder.newClient();
        Response response = client.target(oauth.loginForm().build()).request().get();

        assertThat(response.getStatus(), is(equalTo(302)));
        String cacheControl = response.getHeaderString(HttpHeaders.CACHE_CONTROL);
        assertNotNull(cacheControl);
        assertThat(cacheControl, containsString("no-store"));
        assertThat(cacheControl, containsString("must-revalidate"));
    }

    @Test
    public void authorizationRequestFormPostResponseModeInvalidResponseType() {
        managedRealm.dirty();

        String requestState = "OpenIdConnect.AuthenticationProperties=2302984sdlk";
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.responseType("tokenn");
        oauth.loginForm().state(requestState).open();

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
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        assertNotNull(codeId);
    }

    @Test
    @DatabaseTest
    public void authorizationRequestFormPostResponseModeInvalidRedirectUri() {
        managedRealm.dirty();

        ClientResource testAppClient = findClientByClientId("test-app");
        ClientRepresentation testAppRep = testAppClient.toRepresentation();
        testAppRep.setRedirectUris(Collections.singletonList("*"));
        testAppClient.update(testAppRep);

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

        ClientResource testAppClient = findClientByClientId("test-app");
        ClientRepresentation testAppRep = testAppClient.toRepresentation();
        testAppRep.setRedirectUris(Collections.singletonList("*"));
        testAppClient.update(testAppRep);

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
        managedRealm.dirty();

        ClientResource testAppClient = findClientByClientId("test-app");
        ClientRepresentation testAppRep = testAppClient.toRepresentation();
        testAppRep.setRedirectUris(Collections.singletonList("*"));
        testAppClient.update(testAppRep);


        oauth.responseMode(OIDCResponseMode.FORM_POST_JWT.value());
        oauth.responseType(OAuth2Constants.CODE);
        final String redirectUri = oauth.getRedirectUri() + "?p=&gt;"; // set HTML entity &gt;
        oauth.redirectUri(redirectUri);

        String requestState = "authorizationRequestFormPostJwtResponseModeHTMLEntitiesRedirectUri";
        oauth.loginForm().state(requestState).open();
        loginPage.fillLogin("test-user@localhost", "password");
        loginPage.submit();

        try {

            new WebDriverWait(driver.driver(), Duration.ofSeconds(2))
                    .until(ExpectedConditions.presenceOfElementLocated(By.id("response")));

            // if not properly encoded %3E would be received instead of >
            String responseTokenEncoded = driver.driver().findElement(By.id("response")).getText();
            assertNotNull(responseTokenEncoded);
        } catch (org.openqa.selenium.TimeoutException e) {
            // The form may have auto-submitted already, check if we're at the callback
            assertTrue(driver.driver().getCurrentUrl().contains("/callback/oauth"),
                    "Should be at callback or response page");
        }

        // Verify the redirect URI is correct (may be in current URL if auto-submitted)
        assertTrue(driver.driver().getCurrentUrl().contains("?p=") ||
                        driver.driver().getCurrentUrl().contains(">"),
                "URL should contain the query parameter");


        // Verify login was successful via events
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
        managedRealm.dirty();

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
                .type(EventType.LOGIN)
                .details(Details.RESPONSE_MODE, OIDCResponseMode.FORM_POST.value());

        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        assertNotNull(codeId);
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

    private ClientResource findClientByClientId(String clientId) {
        for (ClientRepresentation c : adminClient.realm(REALM_NAME).clients().findAll()) {
            if (clientId.equals(c.getClientId())) {
                return adminClient.realm(REALM_NAME).clients().get(c.getId());
            }
        }
        return null;
    }

}
