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

import org.jboss.arquillian.graphene.page.Page;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.PageUtils;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.openqa.selenium.By;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class AuthorizationCodeTest extends AbstractKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Page
    private ErrorPage errorPage;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realmRepresentation);
    }

    @Before
    public void clientConfiguration() {
        oauth.responseType(OAuth2Constants.CODE);
        oauth.responseMode(null);
        oauth.stateParamRandom();
    }

    @Test
    public void authorizationRequest() throws IOException {
        oauth.stateParamHardcoded("OpenIdConnect.AuthenticationProperties=2302984sdlk");

        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", response.getState());
        Assert.assertNull(response.getError());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestInstalledApp() throws IOException {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris(Constants.INSTALLED_APP_URN);
        oauth.redirectUri(Constants.INSTALLED_APP_URN);

        oauth.doLogin("test-user@localhost", "password");

        String title = PageUtils.getPageTitle(driver);
        Assert.assertEquals("Success code", title);

        driver.findElement(By.id(OAuth2Constants.CODE)).getAttribute("value");

        events.expectLogin().detail(Details.REDIRECT_URI, oauth.AUTH_SERVER_ROOT + "/realms/test/protocol/openid-connect/oauth/oob").assertEvent().getDetails().get(Details.CODE_ID);

        ClientManager.realm(adminClient.realm("test")).clientId("test-app").removeRedirectUris(Constants.INSTALLED_APP_URN);
    }

    @Test
    public void authorizationValidRedirectUri() throws IOException {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").addRedirectUris(oauth.getRedirectUri());

        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

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
    public void authorizationRequestNoState() throws IOException {
        oauth.stateParamHardcoded(null);

        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        Assert.assertNotNull(response.getCode());
        Assert.assertNull(response.getState());
        Assert.assertNull(response.getError());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestInvalidResponseType() throws IOException {
        oauth.responseType("tokenn");
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        driver.navigate().to(b.build().toURL());

        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);
        assertTrue(errorResponse.isRedirected());
        Assert.assertEquals(errorResponse.getError(), OAuthErrorException.UNSUPPORTED_RESPONSE_TYPE);

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().detail(Details.RESPONSE_TYPE, "tokenn").assertEvent();
    }

    // KEYCLOAK-3281
    @Test
    public void authorizationRequestFormPostResponseMode() throws IOException {
        oauth.responseMode(OIDCResponseMode.FORM_POST.toString().toLowerCase());
        oauth.stateParamHardcoded("OpenIdConnect.AuthenticationProperties=2302984sdlk");
        oauth.doLoginGrant("test-user@localhost", "password");

        String sources = driver.getPageSource();
        System.out.println(sources);

        String code = driver.findElement(By.id("code")).getText();
        String state = driver.findElement(By.id("state")).getText();

        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", state);

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }


    @Test
    public void authorizationRequestFormPostResponseModeWithCustomState() throws IOException {
        oauth.responseMode(OIDCResponseMode.FORM_POST.toString().toLowerCase());
        oauth.stateParamHardcoded("\"><foo>bar_baz(2)far</foo>");
        oauth.doLoginGrant("test-user@localhost", "password");

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
        oauth.responseMode(OIDCResponseMode.FRAGMENT.toString().toLowerCase());
        OAuthClient.AuthorizationEndpointResponse response = oauth.doLogin("test-user@localhost", "password");

        Assert.assertNotNull(response.getCode());
        Assert.assertNotNull(response.getState());

        URI currentUri = new URI(driver.getCurrentUrl());
        Assert.assertNull(currentUri.getRawQuery());
        Assert.assertNotNull(currentUri.getRawFragment());

        // Unset response_mode. The initial OIDC AuthenticationRequest won't contain "response_mode" parameter now and hence it should fallback to "query".
        oauth.responseMode(null);
        oauth.openLoginForm();
        response = new OAuthClient.AuthorizationEndpointResponse(oauth);

        Assert.assertNotNull(response.getCode());
        Assert.assertNotNull(response.getState());

        currentUri = new URI(driver.getCurrentUrl());
        Assert.assertNotNull(currentUri.getRawQuery());
        Assert.assertNull(currentUri.getRawFragment());
    }

    @Test
    public void authorizationRequestParamsMoreThanOnce() throws IOException {
        oauth.stateParamHardcoded("OpenIdConnect.AuthenticationProperties=2302984sdlk");
        Map<String, String> extraParams = new HashMap<>();

        oauth.addCustomerParameter(OAuth2Constants.SCOPE, "read_write")
            .addCustomerParameter(OAuth2Constants.STATE, "abcdefg")
            .addCustomerParameter(OAuth2Constants.SCOPE, "pop push");

        oauth.openLoginForm();

        assertEquals("invalid_request", oauth.getCurrentQuery().get("error"));
        assertEquals("duplicated parameter", oauth.getCurrentQuery().get("error_description"));

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();
    }

    @Test
    public void authorizationRequestClientParamsMoreThanOnce() throws IOException {
        oauth.stateParamHardcoded("OpenIdConnect.AuthenticationProperties=2302984sdlk");

        oauth.addCustomerParameter(OAuth2Constants.SCOPE, "read_write")
                .addCustomerParameter(OAuth2Constants.CLIENT_ID, "client2client")
                .addCustomerParameter(OAuth2Constants.REDIRECT_URI, "https://www.example.com")
                .addCustomerParameter(OAuth2Constants.STATE, "abcdefg")
                .addCustomerParameter(OAuth2Constants.SCOPE, "pop push");

        oauth.openLoginForm();

        assertTrue(errorPage.isCurrent());
        assertEquals("Invalid Request", errorPage.getError());

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).client((String) null).clearDetails().assertEvent();
    }
    
}
