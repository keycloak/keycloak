/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.testsuite.oidc;

import java.io.IOException;
import java.net.URI;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class AuthorizationTokenResponseModeTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Test
    public void authorizationRequestQueryJWTResponseMode() throws Exception {
        oauth.responseMode(OIDCResponseMode.QUERY_JWT.value());

        AuthorizationEndpointResponse response = oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals("test-app", responseToken.getAudience()[0]);
        Assert.assertNotNull(responseToken.getOtherClaims().get("code"));
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", responseToken.getOtherClaims().get("state"));
        Assert.assertNull(responseToken.getOtherClaims().get("error"));

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestJWTResponseMode() throws Exception {
        // jwt response_mode. It should fallback to query.jwt
        oauth.responseMode("jwt");

        AuthorizationEndpointResponse response = oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals("test-app", responseToken.getAudience()[0]);
        Assert.assertNotNull(responseToken.getOtherClaims().get("code"));
        // should not return code when response_type not 'token'
        assertFalse(responseToken.getOtherClaims().containsKey(OAuth2Constants.SCOPE));
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", responseToken.getOtherClaims().get("state"));
        Assert.assertNull(responseToken.getOtherClaims().get("error"));

        URI currentUri = new URI(driver.getCurrentUrl());
        Assert.assertNotNull(currentUri.getRawQuery());
        Assert.assertNull(currentUri.getRawFragment());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestFragmentJWTResponseMode() throws Exception {
        oauth.responseMode(OIDCResponseMode.FRAGMENT_JWT.value());

        AuthorizationEndpointResponse response = oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals("test-app", responseToken.getAudience()[0]);
        Assert.assertNotNull(responseToken.getOtherClaims().get("code"));
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", responseToken.getOtherClaims().get("state"));
        Assert.assertNull(responseToken.getOtherClaims().get("error"));

        URI currentUri = new URI(driver.getCurrentUrl());
        Assert.assertNull(currentUri.getRawQuery());
        Assert.assertNotNull(currentUri.getRawFragment());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestFormPostJWTResponseMode() throws IOException {
        oauth.responseMode(OIDCResponseMode.FORM_POST_JWT.value());
        oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").doLogin("test-user@localhost", "password");

        String sources = driver.getPageSource();
        System.out.println(sources);

        String responseTokenEncoded = driver.findElement(By.id("response")).getText();

        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(responseTokenEncoded);

        assertEquals("test-app", responseToken.getAudience()[0]);
        Assert.assertNotNull(responseToken.getOtherClaims().get("code"));
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", responseToken.getOtherClaims().get("state"));
        Assert.assertNull(responseToken.getOtherClaims().get("error"));

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestJWTResponseModeIdTokenResponseType() throws Exception {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").implicitFlow(true);
        // jwt response_mode. It should fallback to fragment.jwt when its hybrid flow
        oauth.responseMode("jwt");
        oauth.responseType("code id_token");

        AuthorizationEndpointResponse response = oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").nonce("123456").doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals("test-app", responseToken.getAudience()[0]);
        Assert.assertNotNull(responseToken.getOtherClaims().get("code"));
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", responseToken.getOtherClaims().get("state"));
        Assert.assertNull(responseToken.getOtherClaims().get("error"));

        Assert.assertNotNull(responseToken.getOtherClaims().get("id_token"));
        String idTokenEncoded = (String) responseToken.getOtherClaims().get("id_token");
        IDToken idToken = oauth.verifyIDToken(idTokenEncoded);
        assertEquals("123456", idToken.getNonce());

        URI currentUri = new URI(driver.getCurrentUrl());
        Assert.assertNull(currentUri.getRawQuery());
        Assert.assertNotNull(currentUri.getRawFragment());

        String codeId = events.expectLogin().assertEvent().getDetails().get(Details.CODE_ID);
    }

    @Test
    public void authorizationRequestJWTResponseModeAccessTokenResponseType() throws Exception {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").implicitFlow(true);
        // jwt response_mode. It should fallback to fragment.jwt when its hybrid flow
        oauth.responseMode("jwt");
        oauth.responseType("token id_token");

        AuthorizationEndpointResponse response = oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").nonce("123456").doLogin("test-user@localhost", "password");

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals("test-app", responseToken.getAudience()[0]);
        Assert.assertNull(responseToken.getOtherClaims().get("code"));
        assertEquals("OpenIdConnect.AuthenticationProperties=2302984sdlk", responseToken.getOtherClaims().get("state"));
        Assert.assertNull(responseToken.getOtherClaims().get("error"));

        Assert.assertNotNull(responseToken.getOtherClaims().get("id_token"));
        String idTokenEncoded = (String) responseToken.getOtherClaims().get("id_token");
        IDToken idToken = oauth.verifyIDToken(idTokenEncoded);
        assertEquals("123456", idToken.getNonce());

        Assert.assertNotNull(responseToken.getOtherClaims().get("access_token"));
        String accessTokenEncoded = (String) responseToken.getOtherClaims().get("access_token");
        AccessToken accessToken = oauth.verifyToken(accessTokenEncoded);
        assertNull(accessToken.getNonce());

        URI currentUri = new URI(driver.getCurrentUrl());
        Assert.assertNull(currentUri.getRawQuery());
        Assert.assertNotNull(currentUri.getRawFragment());
    }

    @Test
    public void authorizationRequestFailInvalidResponseModeQueryJWT() throws Exception {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").implicitFlow(true);
        oauth.responseMode("query.jwt");
        oauth.responseType("code id_token");
        oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").nonce("123456").open();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(errorResponse.getResponse());
        Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, responseToken.getOtherClaims().get("error"));
        Assert.assertEquals("Response_mode 'query.jwt' is allowed only when the authorization response token is encrypted", responseToken.getOtherClaims().get("error_description"));

        events.expectLogin().error(Errors.INVALID_REQUEST).user((String) null).session((String) null).clearDetails().assertEvent();
    }

    @Test
    public void testErrorObjectExpectedClaims() throws Exception {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").implicitFlow(true);
        oauth.responseMode("query.jwt");
        oauth.responseType("code id_token");
        oauth.loginForm().state("OpenIdConnect.AuthenticationProperties=2302984sdlk").nonce("123456").open();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(errorResponse.getResponse());

        assertNotNull(responseToken.getIssuer());
        assertNotNull(responseToken.getExp());
        assertNotNull(responseToken.getAudience());
        assertNotEquals(0, responseToken.getAudience().length);
        assertTrue(responseToken.getOtherClaims().containsKey("error"));
        assertTrue(responseToken.getOtherClaims().containsKey("error_description"));
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }
}
