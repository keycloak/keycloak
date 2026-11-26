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
package org.keycloak.tests.oidc;

import java.net.URI;
import java.util.Objects;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.AuthorizationResponseToken;
import org.keycloak.representations.IDToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.common.BasicRealmWithUserConfig;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class AuthorizationTokenResponseModeTest extends AbstractOIDCScopeTest {
    private static final String USERNAME =  "basic-user";
    private static final String PASSWORD = "password";
    private static final String STATE = "OpenIdConnect.AuthenticationProperties=2302984sdlk";

    @InjectRealm(config = BasicRealmWithUserConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @Test
    public void authorizationRequestQueryJWTResponseMode() {
        oauth.responseType(OAuth2Constants.CODE);
        oauth.responseMode(OIDCResponseMode.QUERY_JWT.value());
        oauth.loginForm().state(STATE).open();

        authenticatePassword(USERNAME, PASSWORD);
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals(oauth.getClientId(), responseToken.getAudience()[0]);
        Assertions.assertNotNull(responseToken.getOtherClaims().get("code"));
        assertEquals(STATE, responseToken.getOtherClaims().get("state"));
        Assertions.assertNull(responseToken.getOtherClaims().get("error"));

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    public void authorizationRequestJWTResponseMode() throws Exception {
        // jwt response_mode. It should fallback to query.jwt
        oauth.responseType(OAuth2Constants.CODE);
        oauth.responseMode("jwt");
        oauth.loginForm().state(STATE).open();

        authenticatePassword(USERNAME, PASSWORD);
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals(oauth.getClientId(), responseToken.getAudience()[0]);
        Assertions.assertNotNull(responseToken.getOtherClaims().get("code"));
        // should not return code when response_type not 'token'
        assertFalse(responseToken.getOtherClaims().containsKey(OAuth2Constants.SCOPE));
        assertEquals(STATE, responseToken.getOtherClaims().get("state"));
        Assertions.assertNull(responseToken.getOtherClaims().get("error"));
        URI currentUri = new URI(Objects.requireNonNull(oauth.getDriver().getCurrentUrl()));
        Assertions.assertNotNull(currentUri.getRawQuery());
        Assertions.assertNull(currentUri.getRawFragment());

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    public void authorizationRequestFragmentJWTResponseMode() throws Exception {
        oauth.responseType(OAuth2Constants.CODE);
        oauth.responseMode(OIDCResponseMode.FRAGMENT_JWT.value());
        oauth.loginForm().state(STATE).open();

        authenticatePassword(USERNAME, PASSWORD);
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals(oauth.getClientId(), responseToken.getAudience()[0]);
        Assertions.assertNotNull(responseToken.getOtherClaims().get("code"));
        assertEquals(STATE, responseToken.getOtherClaims().get("state"));
        Assertions.assertNull(responseToken.getOtherClaims().get("error"));

        URI currentUri = new URI(Objects.requireNonNull(oauth.getDriver().getCurrentUrl()));
        Assertions.assertNull(currentUri.getRawQuery());
        Assertions.assertNotNull(currentUri.getRawFragment());

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    public void authorizationRequestFormPostJWTResponseMode() {
        oauth.responseType(OAuth2Constants.CODE);
        oauth.responseMode(OIDCResponseMode.FORM_POST_JWT.value());
        oauth.loginForm().state(STATE).open();

        authenticatePassword(USERNAME, PASSWORD);

        String responseTokenEncoded = oauth.getDriver().findElement(By.id("response")).getText();
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(responseTokenEncoded);

        assertEquals(oauth.getClientId(), responseToken.getAudience()[0]);
        Assertions.assertNotNull(responseToken.getOtherClaims().get("code"));
        assertEquals(STATE, responseToken.getOtherClaims().get("state"));
        Assertions.assertNull(responseToken.getOtherClaims().get("error"));

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    public void authorizationRequestJWTResponseModeIdTokenResponseType() throws Exception {
        managedRealm.updateClientWithCleanup(oauth.getClientId(), c -> c.implicitFlowEnabled(true));
        // jwt response_mode. It should fallback to fragment.jwt when its hybrid flow
        oauth.responseMode("jwt");
        oauth.responseType("code id_token");
        oauth.loginForm().state(STATE).nonce("123456").open();

        authenticatePassword(USERNAME, PASSWORD);

        AuthorizationEndpointResponse response = oauth.parseLoginResponse();

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals(oauth.getClientId(), responseToken.getAudience()[0]);
        Assertions.assertNotNull(responseToken.getOtherClaims().get("code"));
        assertEquals(STATE, responseToken.getOtherClaims().get("state"));
        Assertions.assertNull(responseToken.getOtherClaims().get("error"));

        Assertions.assertNotNull(responseToken.getOtherClaims().get("id_token"));
        String idTokenEncoded = (String) responseToken.getOtherClaims().get("id_token");
        IDToken idToken = oauth.verifyIDToken(idTokenEncoded);
        assertEquals("123456", idToken.getNonce());

        URI currentUri = new URI(Objects.requireNonNull(oauth.getDriver().getCurrentUrl()));
        Assertions.assertNull(currentUri.getRawQuery());
        Assertions.assertNotNull(currentUri.getRawFragment());

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    public void authorizationRequestJWTResponseModeAccessTokenResponseType() throws Exception {
        managedRealm.updateClientWithCleanup(oauth.getClientId(), c -> c.implicitFlowEnabled(true));
        // jwt response_mode. It should fallback to fragment.jwt when its hybrid flow
        oauth.responseMode("jwt");
        oauth.responseType("token id_token");
        oauth.loginForm().state(STATE).nonce("123456").open();

        authenticatePassword(USERNAME, PASSWORD);

        AuthorizationEndpointResponse response = oauth.parseLoginResponse();

        assertTrue(response.isRedirected());
        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(response.getResponse());

        assertEquals(oauth.getClientId(), responseToken.getAudience()[0]);
        Assertions.assertNull(responseToken.getOtherClaims().get("code"));
        assertEquals(STATE, responseToken.getOtherClaims().get("state"));
        Assertions.assertNull(responseToken.getOtherClaims().get("error"));

        Assertions.assertNotNull(responseToken.getOtherClaims().get("id_token"));
        String idTokenEncoded = (String) responseToken.getOtherClaims().get("id_token");
        IDToken idToken = oauth.verifyIDToken(idTokenEncoded);
        assertEquals("123456", idToken.getNonce());

        Assertions.assertNotNull(responseToken.getOtherClaims().get("access_token"));
        String accessTokenEncoded = (String) responseToken.getOtherClaims().get("access_token");
        AccessToken accessToken = oauth.verifyToken(accessTokenEncoded);
        assertNull(accessToken.getNonce());

        URI currentUri = new URI(Objects.requireNonNull(oauth.getDriver().getCurrentUrl()));
        Assertions.assertNull(currentUri.getRawQuery());
        Assertions.assertNotNull(currentUri.getRawFragment());

        EventAssertion.expectLoginSuccess(events.poll());
    }

    @Test
    public void authorizationRequestFailInvalidResponseModeQueryJWT() {
        managedRealm.updateClientWithCleanup(oauth.getClientId(), c -> c.implicitFlowEnabled(true));
        oauth.responseMode("query.jwt");
        oauth.responseType("code id_token");
        oauth.loginForm().state(STATE).nonce("123456").open();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();

        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(errorResponse.getResponse());
        Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, responseToken.getOtherClaims().get("error"));
        Assertions.assertEquals("Response_mode 'query.jwt' is allowed only when the authorization response token is encrypted", responseToken.getOtherClaims().get("error_description"));

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REQUEST).userId(null).sessionId(null);
    }

    @Test
    public void testErrorObjectExpectedClaims() {
        managedRealm.updateClientWithCleanup(oauth.getClientId(), c -> c.implicitFlowEnabled(true));
        oauth.responseMode("query.jwt");
        oauth.responseType("code id_token");
        oauth.loginForm().state(STATE).nonce("123456").open();

        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();

        AuthorizationResponseToken responseToken = oauth.verifyAuthorizationResponseToken(errorResponse.getResponse());

        assertNotNull(responseToken.getIssuer());
        assertNotNull(responseToken.getExp());
        assertNotNull(responseToken.getAudience());
        assertNotEquals(0, responseToken.getAudience().length);
        assertTrue(responseToken.getOtherClaims().containsKey("error"));
        assertTrue(responseToken.getOtherClaims().containsKey("error_description"));

        EventAssertion.assertError(events.poll()).type(EventType.LOGIN_ERROR).error(Errors.INVALID_REQUEST).userId(null).sessionId(null);
    }
}
