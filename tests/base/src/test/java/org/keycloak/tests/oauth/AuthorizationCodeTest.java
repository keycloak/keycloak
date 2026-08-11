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
import java.util.Arrays;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.UriBuilder;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectHttpClient;
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
import org.keycloak.testframework.ui.page.InstalledAppRedirectPage;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.tests.suites.DatabaseTest;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
@KeycloakIntegrationTest
public class AuthorizationCodeTest {

    @InjectRealm(config =  AuthzCodeRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectWebDriver
    ManagedWebDriver driver;

    @InjectOAuthClient(lifecycle = LifeCycle.METHOD)
    OAuthClient oauth;

    @InjectEvents
    Events events;

    @InjectPage
    InstalledAppRedirectPage installedAppPage;

    @InjectPage
    ErrorPage errorPage;

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectKeycloakUrls
    KeycloakUrls keycloakUrls;

    @Test
    public void authorizationRequest() throws IOException {
        AuthorizationEndpointResponse response = oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        Assertions.assertNotNull(response.getCode());
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", response.getState());
        Assertions.assertNull(response.getError());
        assertEquals(keycloakUrls.getBase() + "/realms/test", response.getIssuer());

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    @DatabaseTest
    public void authorizationRequestInstalledApp() {
        ClientRepresentation clientRep = oauth.clientResource().toRepresentation();
        clientRep.setRedirectUris(List.of(Constants.INSTALLED_APP_URN));
        oauth.clientResource().update(clientRep);
        oauth.redirectUri(Constants.INSTALLED_APP_URN);

        oauth.doLogin("test-user@localhost", "password");

        installedAppPage.getSuccessCode();

        EventAssertion.expectLoginSuccess(events.poll()).details(Details.REDIRECT_URI, keycloakUrls.getBase() + "/realms/test/protocol/openid-connect/oauth/oob");
    }

    @Test
    public void authorizationRequestInstalledAppErrors() {
        String error = "<p><a href=\"javascript&amp;colon;alert(document.domain);\">Back to application</a></p>";
        installedAppPage.open(keycloakUrls.getBase(), "test", null, error, null);

        // Assert text escaped
        installedAppPage.assertCurrent();
        driver.driver().manage().timeouts().implicitlyWait(Duration.ofMillis(1));
        installedAppPage.assertLinkBackToApplicationNotPresent();
        Assertions.assertEquals("Error code: <p><a href=\"javascript&amp;colon;alert(document.domain);\">Back to application</a></p>", installedAppPage.getPageTitleText());
        driver.driver().manage().timeouts().implicitlyWait(Duration.ofSeconds((5)));

        error = "<p><a href=\"http://foo.com\">Back to application</a></p>";
        installedAppPage.open(keycloakUrls.getBase(), "test", null, error, null);

        // Assert text is escaped
        installedAppPage.assertCurrent();
        driver.driver().manage().timeouts().implicitlyWait(Duration.ofMillis(1));
        installedAppPage.assertLinkBackToApplicationNotPresent();
        Assertions.assertEquals("Error code: <p><a href=\"http://foo.com\">Back to application</a></p>", installedAppPage.getPageTitleText());
        driver.driver().manage().timeouts().implicitlyWait(Duration.ofSeconds((5)));
    }

    @Test
    @DatabaseTest
    public void authorizationValidRedirectUri() {
        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        Assertions.assertNotNull(response.getCode());

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    @DatabaseTest
    public void testInvalidRedirectUri() {
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
    public void testInvalidNULCharacterClientId() {
        oauth.client("%00");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertEquals("An internal server error has occurred", errorPage.getError());
    }

    @Test
    @DatabaseTest
    public void testInvalidESCCharacterClientId() {
        oauth.client("%1B");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertEquals("An internal server error has occurred", errorPage.getError());
    }

    @Test
    @DatabaseTest
    public void authorizationRequestNoState() {
        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        Assertions.assertNotNull(response.getCode());
        Assertions.assertNull(response.getState());
        Assertions.assertNull(response.getError());
        assertEquals(keycloakUrls.getBase() + "/realms/test", response.getIssuer());

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    public void authorizationRequestInvalidResponseType() {
        oauth.responseType("tokenn");
        oauth.openLoginForm();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        assertTrue(errorResponse.isRedirected());
        Assertions.assertEquals(errorResponse.getError(), OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE);
        Assertions.assertEquals(keycloakUrls.getBase() + "/realms/test", errorResponse.getIssuer());

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REQUEST).userId(null).sessionId(null).details(Details.RESPONSE_TYPE, "tokenn");
    }

    // Issue 29866
    @Test
    public void authorizationRequestInvalidResponseType_testHeaders() throws IOException {
        oauth.responseType("tokenn");
        HttpGet getRequest = new HttpGet(oauth.loginForm().build());
        CloseableHttpResponse response = httpClient.execute(getRequest);

        assertThat(response.getStatusLine().getStatusCode(), is(equalTo(302)));
        String cacheControl = Arrays.toString(response.getHeaders(HttpHeaders.CACHE_CONTROL));
        Assertions.assertNotNull(cacheControl);
        MatcherAssert.assertThat(cacheControl, containsString("no-store"));
        MatcherAssert.assertThat(cacheControl, containsString("must-revalidate"));
    }

    @Test
    public void authorizationRequestFormPostResponseModeInvalidResponseType() throws IOException {
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.responseType("tokenn");
        oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").open();

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REQUEST)
                .details(Details.REASON, "Unsupported response_type")
                .details(Details.RESPONSE_TYPE, "tokenn");
        String error = driver.findElement(By.id("error")).getText();
        String state = driver.findElement(By.id("state")).getText();

        assertEquals(OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, error);
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", state);
    }

    @Test
    public void authorizationRequestFormPostResponseModeWithoutResponseType() {
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.responseType(null);
        oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").open();

        String error = driver.findElement(By.id("error")).getText();
        String errorDescription = driver.findElement(By.id("error_description")).getText();
        String state = driver.findElement(By.id("state")).getText();

        assertEquals(OAuthErrorException.INVALID_REQUEST, error);
        assertEquals("Missing parameter: response_type", errorDescription);
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", state);
    }

    // KEYCLOAK-3281
    @Test
    public void authorizationRequestFormPostResponseMode() {
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").doLogin("test-user@localhost", "password");

        String sources = driver.driver().getPageSource();
        System.out.println(sources);

        String code = driver.findElement(By.id("code")).getText();
        String state = driver.findElement(By.id("state")).getText();

        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", state);

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    @DatabaseTest
    public void authorizationRequestFormPostResponseModeInvalidRedirectUri() {
        ClientRepresentation clientRep = oauth.clientResource().toRepresentation();
        clientRep.setRedirectUris(List.of("*"));
        oauth.clientResource().update(clientRep);
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.responseType(OAuth2Constants.CODE);
        oauth.redirectUri("javascript:alert('XSS')");
        oauth.openLoginForm();

        errorPage.assertCurrent();
        assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REDIRECT_URI).userId(null).sessionId(null);
    }

    @Test
    @DatabaseTest
    public void authorizationRequestFormPostResponseModeHTMLEntitiesRedirectUri() {
        ClientRepresentation clientRep = oauth.clientResource().toRepresentation();
        clientRep.setRedirectUris(List.of("*"));
        oauth.clientResource().update(clientRep);
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        final String redirectUri = oauth.getRedirectUri() + "?p=&gt;"; // set HTML entity &gt;
        oauth.redirectUri(redirectUri);

        String requestState = "authorizationRequestFormPostResponseModeHTMLEntitiesRedirectUri";

        oauth.loginForm().state(requestState).doLogin("test-user@localhost", "password");

        // if not properly encoded %3E would be received instead of &gt;
        Assertions.assertEquals(redirectUri, oauth.getDriver().getCurrentUrl(), "Redirect page was not encoded");
        String state = driver.findElement(By.id("state")).getText();
        Assertions.assertEquals(requestState, state);
        Assertions.assertNotNull(driver.findElement(By.id("code")).getText());

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN)
                .hasUserId()
                .hasSessionId()
                .details(Details.USERNAME, "test-user@localhost")
                .details(OIDCLoginProtocol.RESPONSE_MODE_PARAM, OIDCResponseMode.FORM_POST.name().toLowerCase())
                .details(OAuth2Constants.REDIRECT_URI, redirectUri);
    }

    @Test
    @DatabaseTest
    public void authorizationRequestFormPostJwtResponseModeHTMLEntitiesRedirectUri() {
        ClientRepresentation clientRep = oauth.clientResource().toRepresentation();
        clientRep.setRedirectUris(List.of("*"));
        oauth.clientResource().update(clientRep);
        oauth.responseMode(OIDCResponseMode.FORM_POST_JWT.value());
        final String redirectUri = oauth.getRedirectUri() + "?p=&gt;"; // set HTML entity &gt;
        oauth.redirectUri(redirectUri);

        String requestState = "authorizationRequestFormPostJwtResponseModeHTMLEntitiesRedirectUri";
        oauth.loginForm().state(requestState).doLogin("test-user@localhost", "password");

        // if not properly encoded %3E would be received instead of &gt;
        Assertions.assertEquals(redirectUri, oauth.getDriver().getCurrentUrl(), "Redirect page was not encoded");
        String responseTokenEncoded = driver.findElement(By.id("response")).getText();
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(responseTokenEncoded);
        assertEquals("test-app", responseToken.getAudience()[0]);
        Assertions.assertNotNull(responseToken.getOtherClaims().get("code"));
        Assertions.assertNull(responseToken.getOtherClaims().get("error"));
        Assertions.assertEquals(requestState, responseToken.getOtherClaims().get("state"));
        Assertions.assertNotNull(responseToken.getOtherClaims().get("code"));

        EventAssertion.assertSuccess(events.poll()).type(EventType.LOGIN)
                .hasUserId()
                .sessionId((String) responseToken.getOtherClaims().get("session_state"))
                .details(Details.USERNAME, "test-user@localhost")
                .details(OIDCLoginProtocol.RESPONSE_MODE_PARAM, OIDCResponseMode.FORM_POST_JWT.name().toLowerCase())
                .details(OAuth2Constants.REDIRECT_URI, redirectUri);
    }

    @Test
    public void authorizationRequestFormPostResponseModeWithCustomState() {
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.loginForm().state("\"><foo>bar_baz(2)far</foo>").doLogin("test-user@localhost", "password");

        String sources = driver.driver().getPageSource();
        System.out.println(sources);

        String code = driver.findElement(By.id("code")).getText();
        String state = driver.findElement(By.id("state")).getText();

        assertEquals("\"><foo>bar_baz(2)far</foo>", state);

        EventAssertion.expectLoginSuccess(events.poll());
    }


    @Test
    public void authorizationRequestFragmentResponseModeNotKept() throws Exception {
        // Set response_mode=fragment and login
        oauth.responseMode(OIDCResponseMode.FRAGMENT.value());
        AuthorizationEndpointResponse response = oauth.loginForm().state("authorizationRequestFragmentResponseModeNotKept").doLogin("test-user@localhost", "password");

        Assertions.assertNotNull(response.getCode());
        Assertions.assertNotNull(response.getState());

        URI currentUri = new URI(driver.getCurrentUrl());
        Assertions.assertNull(currentUri.getRawQuery());
        Assertions.assertNotNull(currentUri.getRawFragment());

        // Unset response_mode. The initial OIDC AuthenticationRequest won't contain "response_mode" parameter now and hence it should fallback to "query".
        oauth.responseMode(null);
        oauth.loginForm().state("authorizationRequestFragmentResponseModeNotKept2").open();
        response = oauth.parseLoginResponse();

        Assertions.assertNotNull(response.getCode());
        Assertions.assertNotNull(response.getState());
        Assertions.assertEquals(keycloakUrls.getBase() + "/realms/test", response.getIssuer());

        currentUri = new URI(driver.getCurrentUrl());
        Assertions.assertNotNull(currentUri.getRawQuery());
        Assertions.assertNull(currentUri.getRawFragment());
    }

    @Test
    public void authorizationRequestParamsMoreThanOnce() {
        String logoutUrl = UriBuilder.fromUri(oauth.loginForm().build()).queryParam(OAuth2Constants.SCOPE, "read_write")
                .queryParam(OAuth2Constants.STATE, "abcdefg")
                .queryParam(OAuth2Constants.SCOPE, "pop push").build().toString();

        driver.driver().navigate().to(logoutUrl);

        AuthorizationEndpointResponse response = oauth.parseLoginResponse();

        assertEquals("invalid_request", response.getError());
        assertEquals("duplicated parameter", response.getErrorDescription());

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REQUEST).userId(null).sessionId(null);
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

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REQUEST).userId(null).sessionId(null).clientId(null);
    }

    private static class AuthzCodeRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realmBuilder) {
            return realmBuilder.name("test")
                    .users(UserBuilder.create("test-user@localhost")
                            .name("test", "user")
                            .email("test-user@localhost")
                            .emailVerified(true)
                            .password("password")
                            .realmRoles("user", "offline_access").build());
        }
    }

}
