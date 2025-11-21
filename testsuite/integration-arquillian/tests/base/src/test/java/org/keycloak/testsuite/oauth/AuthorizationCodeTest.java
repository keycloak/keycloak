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
package org.keycloak.testsuite.oauth;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
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
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.InstalledAppRedirectPage;
import org.keycloak.testsuite.updaters.ClientAttributeUpdater;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationCodeTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    private ErrorPage errorPage;

    @Page
    private InstalledAppRedirectPage installedAppPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    @Before
    public void clientConfiguration() {
        oauth.responseType(OAuth2Constants.CODE);
        oauth.responseMode(null);
    }

    @Test
    public void authorizationRequest() throws IOException {
        AuthorizationEndpointResponse response = oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", response.getState());
        Assert.assertNull(response.getError());
        assertEquals(oauth.AUTH_SERVER_ROOT + "/realms/test", response.getIssuer());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestInstalledApp() throws IOException {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris(Constants.INSTALLED_APP_URN);
        oauth.redirectUri(Constants.INSTALLED_APP_URN);

        oauth.doLogin("test-user@localhost", "password");

        installedAppPage.getSuccessCode();

        events.expectLogin().detail(Details.REDIRECT_URI, oauth.AUTH_SERVER_ROOT + "/realms/test/protocol/openid-connect/oauth/oob").assertEvent().getDetails().get(Details.CODE_ID);

        ClientManager.realm(adminClient.realm("test")).clientId("test-app").removeRedirectUris(Constants.INSTALLED_APP_URN);
    }

    @Test
    public void authorizationRequestInstalledAppErrors() throws IOException {
        String error = "<p><a href=\"javascript&amp;colon;alert(document.domain);\">Back to application</a></p>";
        installedAppPage.open("test", null, error, null);

        // Assert text escaped and not "a" link present
        installedAppPage.assertLinkBackToApplicationNotPresent();
        Assert.assertEquals("Error code: <p>Back to application</p>", installedAppPage.getPageTitleText());

        error = "<p><a href=\"http://foo.com\">Back to application</a></p>";
        installedAppPage.open("test", null, error, null);

        // In this case, link is not sanitized as it is valid link, however it is escaped and not shown as a link
        installedAppPage.assertLinkBackToApplicationNotPresent();
        Assert.assertEquals("Error code: <p><a href=\"http://foo.com\" rel=\"nofollow\">Back to application</a></p>", installedAppPage.getPageTitleText());
    }

    @Test
    public void authorizationValidRedirectUri() throws IOException {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris(oauth.getRedirectUri());

        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void testInvalidRedirectUri() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris(oauth.getRedirectUri());

        oauth.redirectUri(oauth.getRedirectUri() + "%20test");
        oauth.openLoginForm();

        assertTrue(errorPage.isCurrent());
        assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

        oauth.redirectUri("ZAP%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%25n%25s%0A");
        oauth.openLoginForm();

        assertTrue(errorPage.isCurrent());
        assertEquals("Invalid parameter: redirect_uri", errorPage.getError());
    }

    @Test
    public void testInvalidNULCharacterClientId() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris(oauth.getRedirectUri());

        oauth.client("%00");
        oauth.openLoginForm();

        assertTrue(errorPage.isCurrent());
        assertEquals("An internal server error has occurred", errorPage.getError());
    }

    @Test
    public void authorizationRequestNoState() throws IOException {
        AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        Assert.assertNull(response.getState());
        Assert.assertNull(response.getError());
        assertEquals(oauth.AUTH_SERVER_ROOT + "/realms/test", response.getIssuer());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestInvalidResponseType() throws IOException {
        oauth.responseType("tokenn");
        oauth.openLoginForm();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE);
        Assert.assertEquals(oauth.AUTH_SERVER_ROOT + "/realms/test", errorResponse.getIssuer());

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().detail(Details.RESPONSE_TYPE, "tokenn").assertEvent();
    }

    // Issue 29866
    @Test
    public void authorizationRequestInvalidResponseType_testHeaders() throws IOException {
        oauth.responseType("tokenn");
        Client client = AdminClientUtil.createResteasyClient();
        Response response = client.target(oauth.loginForm().build()).request().get();

        assertThat(response.getStatus(), is(equalTo(302)));
        String cacheControl = response.getHeaderString(HttpHeaders.CACHE_CONTROL);
        Assert.assertNotNull(cacheControl);
        Assert.assertThat(cacheControl, containsString("no-store"));
        Assert.assertThat(cacheControl, containsString("must-revalidate"));
    }

    @Test
    public void authorizationRequestFormPostResponseModeInvalidResponseType() throws IOException {
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.responseType("tokenn");
        oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").open();

        String error = driver.findElement(By.id("error")).getText();
        String state = driver.findElement(By.id("state")).getText();

        assertEquals(OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE, error);
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", state);

    }

    @Test
    public void authorizationRequestFormPostResponseModeWithoutResponseType() throws IOException {
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
    public void authorizationRequestFormPostResponseMode() throws IOException {
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").doLogin("test-user@localhost", "password");

        String sources = driver.getPageSource();
        System.out.println(sources);

        String code = driver.findElement(By.id("code")).getText();
        String state = driver.findElement(By.id("state")).getText();

        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", state);

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestFormPostResponseModeInvalidRedirectUri() throws IOException {
        try (var c = ClientAttributeUpdater.forClient(adminClient, "test", "test-app")
                .setRedirectUris(Collections.singletonList("*"))
                .update()) {
            oauth.responseMode(OIDCResponseMode.FORM_POST.value());
            oauth.responseType(OAuth2Constants.CODE);
            oauth.redirectUri("javascript:alert('XSS')");
            oauth.openLoginForm();

            errorPage.assertCurrent();
            assertEquals("Invalid parameter: redirect_uri", errorPage.getError());

            events.expectLogin().error(Errors.INVALID_REDIRECT_URI).user((String) null).session((String) null).clearDetails().assertEvent();
        }
    }

    @Test
    public void authorizationRequestFormPostResponseModeHTMLEntitiesRedirectUri() throws IOException {
        try (var c = ClientAttributeUpdater.forClient(adminClient, "test", "test-app")
                .setRedirectUris(Collections.singletonList("*"))
                .update()) {
            oauth.responseMode(OIDCResponseMode.FORM_POST.value());
            oauth.responseType(OAuth2Constants.CODE);
            final String redirectUri = oauth.getRedirectUri() + "?p=&gt;"; // set HTML entity &gt;
            oauth.redirectUri(redirectUri);

            String requestState = "authorizationRequestFormPostResponseModeHTMLEntitiesRedirectUri";

            oauth.loginForm().state(requestState).doLogin("test-user@localhost", "password");

            WaitUtils.waitForPageToLoad();
            // if not properly encoded %3E would be received instead of &gt;
            Assert.assertEquals("Redirect page was not encoded", redirectUri, oauth.getDriver().getCurrentUrl());
            String state = driver.findElement(By.id("state")).getText();
            Assert.assertEquals(requestState, state);
            Assert.assertNotNull(driver.findElement(By.id("code")).getText());

            events.expect(EventType.LOGIN)
                    .user(AssertEvents.isUUID())
                    .session(AssertEvents.isSessionId())
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(OIDCLoginProtocol.RESPONSE_MODE_PARAM, OIDCResponseMode.FORM_POST.name().toLowerCase())
                    .detail(OAuth2Constants.REDIRECT_URI, redirectUri)
                    .assertEvent();
        }
    }

    @Test
    public void authorizationRequestFormPostJwtResponseModeHTMLEntitiesRedirectUri() throws IOException {
        try (var c = ClientAttributeUpdater.forClient(adminClient, "test", "test-app")
                .setRedirectUris(Collections.singletonList("*"))
                .update()) {
            oauth.responseMode(OIDCResponseMode.FORM_POST_JWT.value());
            oauth.responseType(OAuth2Constants.CODE);
            final String redirectUri = oauth.getRedirectUri() + "?p=&gt;"; // set HTML entity &gt;
            oauth.redirectUri(redirectUri);

            String requestState = "authorizationRequestFormPostJwtResponseModeHTMLEntitiesRedirectUri";
            oauth.loginForm().state(requestState).doLogin("test-user@localhost", "password");

            WaitUtils.waitForPageToLoad();
            // if not properly encoded %3E would be received instead of &gt;
            Assert.assertEquals("Redirect page was not encoded", redirectUri, oauth.getDriver().getCurrentUrl());
            String responseTokenEncoded = driver.findElement(By.id("response")).getText();
            AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(responseTokenEncoded);
            assertEquals("test-app", responseToken.getAudience()[0]);
            Assert.assertNotNull(responseToken.getOtherClaims().get("code"));
            Assert.assertNull(responseToken.getOtherClaims().get("error"));
            Assert.assertEquals(requestState, responseToken.getOtherClaims().get("state"));
            Assert.assertNotNull(responseToken.getOtherClaims().get("code"));

            events.expect(EventType.LOGIN)
                    .user(AssertEvents.isUUID())
                    .session((String) responseToken.getOtherClaims().get("session_state"))
                    .detail(Details.USERNAME, "test-user@localhost")
                    .detail(OIDCLoginProtocol.RESPONSE_MODE_PARAM, OIDCResponseMode.FORM_POST_JWT.name().toLowerCase())
                    .detail(OAuth2Constants.REDIRECT_URI, redirectUri)
                    .assertEvent();
        }
    }

    @Test
    public void authorizationRequestFormPostResponseModeWithCustomState() throws IOException {
        oauth.responseMode(OIDCResponseMode.FORM_POST.value());
        oauth.loginForm().state("\"><foo>bar_baz(2)far</foo>").doLogin("test-user@localhost", "password");

        String sources = driver.getPageSource();
        System.out.println(sources);

        String code = driver.findElement(By.id("code")).getText();
        String state = driver.findElement(By.id("state")).getText();

        assertEquals("\"><foo>bar_baz(2)far</foo>", state);

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }


    @Test
    public void authorizationRequestFragmentResponseModeNotKept() throws Exception {
        // Set response_mode=fragment and login
        oauth.responseMode(OIDCResponseMode.FRAGMENT.value());
        AuthorizationEndpointResponse response = oauth.loginForm().state("authorizationRequestFragmentResponseModeNotKept").doLogin("test-user@localhost", "password");

        Assert.assertNotNull(response.getCode());
        Assert.assertNotNull(response.getState());

        URI currentUri = new URI(driver.getCurrentUrl());
        Assert.assertNull(currentUri.getRawQuery());
        Assert.assertNotNull(currentUri.getRawFragment());

        // Unset response_mode. The initial OIDC AuthenticationRequest won't contain "response_mode" parameter now and hence it should fallback to "query".
        oauth.responseMode(null);
        oauth.loginForm().state("authorizationRequestFragmentResponseModeNotKept2").open();
        response = oauth.parseLoginResponse();

        Assert.assertNotNull(response.getCode());
        Assert.assertNotNull(response.getState());
        Assert.assertEquals(oauth.AUTH_SERVER_ROOT + "/realms/test", response.getIssuer());

        currentUri = new URI(driver.getCurrentUrl());
        Assert.assertNotNull(currentUri.getRawQuery());
        Assert.assertNull(currentUri.getRawFragment());
    }

    @Test
    public void authorizationRequestParamsMoreThanOnce() throws IOException {
        String logoutUrl = UriBuilder.fromUri(oauth.loginForm().build()).queryParam(OAuth2Constants.SCOPE, "read_write")
                .queryParam(OAuth2Constants.STATE, "abcdefg")
                .queryParam(OAuth2Constants.SCOPE, "pop push").build().toString();

        driver.navigate().to(logoutUrl);

        AuthorizationEndpointResponse response = oauth.parseLoginResponse();

        assertEquals("invalid_request", response.getError());
        assertEquals("duplicated parameter", response.getErrorDescription());

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();
    }

    @Test
    public void authorizationRequestClientParamsMoreThanOnce() throws IOException {
        String logoutUrl = UriBuilder.fromUri(oauth.loginForm().build()).queryParam(OAuth2Constants.SCOPE, "read_write")
                .queryParam(OAuth2Constants.CLIENT_ID, "client2client")
                .queryParam(OAuth2Constants.REDIRECT_URI, "https://www.example.com")
                .queryParam(OAuth2Constants.STATE, "abcdefg")
                .queryParam(OAuth2Constants.SCOPE, "pop push").build().toString();

        driver.navigate().to(logoutUrl);

        assertTrue(errorPage.isCurrent());
        assertEquals("Invalid Request", errorPage.getError());

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).client((String) null).clearDetails().assertEvent();
    }

}
