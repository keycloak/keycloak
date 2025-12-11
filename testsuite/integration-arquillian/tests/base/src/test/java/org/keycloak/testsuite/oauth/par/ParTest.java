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

package org.keycloak.testsuite.oauth.par;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.ParConfig;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.policies.AbstractClientPoliciesTest;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.services.clientpolicy.executor.TestRaiseExceptionExecutorFactory;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.RoleBuilder;
import org.keycloak.testsuite.util.oauth.AbstractHttpResponse;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.Assert;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createTestRaiseExeptionExecutorConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ParTest extends AbstractClientPoliciesTest {

    // defined in testrealm.json
    private static final String TEST_USER_NAME = "test-user@localhost";
    private static final String TEST_USER_PASSWORD = "password";
    private static final String TEST_USER2_NAME = "john-doh@localhost";
    private static final String TEST_USER2_PASSWORD = "password";

    private static final String CLIENT_NAME = "Zahlungs-App";
    private static final String CLIENT_REDIRECT_URI = "https://localhost:8543/auth/realms/test/app/auth/cb";
    private static final String IMAGINARY_REQUEST_URI = "urn:ietf:params:oauth:request_uri:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA";
    private static final int DEFAULT_REQUEST_URI_LIFESPAN = 60;

    private static final String VALID_CORS_URL = "http://localtest.me:8180";
    private static final String INVALID_CORS_URL = "http://invalid.localtest.me:8180";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        List<UserRepresentation> users = realm.getUsers();

        LinkedList<CredentialRepresentation> credentials = new LinkedList<>();
        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue("password");
        credentials.add(password);

        UserRepresentation user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("manage-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.MANAGE_CLIENTS)));

        users.add(user);

        user = new UserRepresentation();
        user.setEnabled(true);
        user.setUsername("create-clients");
        user.setCredentials(credentials);
        user.setClientRoles(Collections.singletonMap(Constants.REALM_MANAGEMENT_CLIENT_ID, Collections.singletonList(AdminRoles.CREATE_CLIENT)));
        user.setGroups(Arrays.asList("topGroup")); // defined in testrealm.json

        users.add(user);

        realm.setUsers(users);

        realm.getClients().add(ClientBuilder.create().redirectUris(VALID_CORS_URL + "/realms/master/app")
                .addWebOrigin(VALID_CORS_URL).clientId("test-app2").publicClient().directAccessGrants().build());

        testRealms.add(realm);
    }

    // success with one client conducting one authz request
    @Test
    public void testSuccessfulSinglePar() throws Exception {
        try {
            // setup PAR realm settings
            int requestUriLifespan = 45;
            setParRealmSettings(requestUriLifespan);

            // create client dynamically
            String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
                clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
            });
            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

            // Pushed Authorization Request
            oauth.client(clientId, clientSecret);
            oauth.redirectUri(CLIENT_REDIRECT_URI);
            ParResponse pResp = oauth.doPushedAuthorizationRequest();
            assertEquals(201, pResp.getStatusCode());
            String requestUri = pResp.getRequestUri();
            assertEquals(requestUriLifespan, pResp.getExpiresIn());

            // Authorization Request with request_uri of PAR
            // remove parameters as query strings of uri
            oauth.redirectUri(null);
            oauth.scope(null);
            oauth.responseType(null);
            String state = "testSuccessfulSinglePar";
            oauth.loginForm().requestUri(requestUri).state(state).open();
            assertThat(driver.getCurrentUrl(), startsWith(OAuthClient.AUTH_SERVER_ROOT + "/realms/" + oauth.getRealm() + "/login-actions/authenticate"));
            driver.navigate().refresh(); // ensure PAR survives browser page reload
            driver.navigate().refresh();
            oauth.fillLoginForm(TEST_USER_NAME, TEST_USER_PASSWORD);
            AuthorizationEndpointResponse loginResponse = oauth.parseLoginResponse();
            assertEquals(state, loginResponse.getState());
            String code = loginResponse.getCode();
            String sessionId =loginResponse.getSessionState();

            // Token Request
            oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
            AccessTokenResponse res = oauth.doAccessTokenRequest(code);
            assertEquals(200, res.getStatusCode());

            AccessToken token = oauth.verifyToken(res.getAccessToken());
            String userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId();
            assertEquals(userId, token.getSubject());
            assertEquals(sessionId, token.getSessionState());
            // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
            // Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
            assertEquals(clientId, token.getIssuedFor());

            // Token Refresh
            String refreshTokenString = res.getRefreshToken();
            RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);
            assertEquals(sessionId, refreshToken.getSessionState());
            assertEquals(clientId, refreshToken.getIssuedFor());

            AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshTokenString);
            assertEquals(200, refreshResponse.getStatusCode());

            AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());
            RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshResponse.getRefreshToken());
            assertEquals(sessionId, refreshedToken.getSessionState());
            assertEquals(sessionId, refreshedRefreshToken.getSessionState());
            assertEquals(findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId(), refreshedToken.getSubject());

            // Logout
            oauth.doLogout(refreshResponse.getRefreshToken());
            refreshResponse = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken());
            assertEquals(400, refreshResponse.getStatusCode());

        } finally {
            restoreParRealmSettings();
        }
    }

    // success with one public client conducting one authz request
    @Test
    public void testSuccessfulSingleParPublicClient() throws Exception {
        try {
            // setup PAR realm settings
            int requestUriLifespan = 45;
            setParRealmSettings(requestUriLifespan);

            // create client dynamically
            String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setTokenEndpointAuthMethod("none"); // Public Client
                clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
                clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
            });
            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals("none", oidcCRep.getTokenEndpointAuthMethod()); // Public Client

            // Pushed Authorization Request
            oauth.client(clientId, clientSecret);
            oauth.redirectUri(CLIENT_REDIRECT_URI);
            ParResponse pResp = oauth.doPushedAuthorizationRequest();
            assertEquals(201, pResp.getStatusCode());
            String requestUri = pResp.getRequestUri();
            assertEquals(requestUriLifespan, pResp.getExpiresIn());

            // Authorization Request with request_uri of PAR
            // remove parameters as query strings of uri
            oauth.redirectUri(null);
            oauth.scope(null);
            oauth.responseType(null);
            String state = "testSuccessfulSingleParPublicClient";
            AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUri).state(state).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            assertEquals(state, loginResponse.getState());
            String code = loginResponse.getCode();
            String sessionId =loginResponse.getSessionState();

            // Token Request
            oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
            AccessTokenResponse res = oauth.doAccessTokenRequest(code);
            assertEquals(200, res.getStatusCode());

            AccessToken token = oauth.verifyToken(res.getAccessToken());
            String userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId();
            assertEquals(userId, token.getSubject());
            assertEquals(sessionId, token.getSessionState());
            // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
            // Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
            assertEquals(clientId, token.getIssuedFor());

            // Token Refresh
            String refreshTokenString = res.getRefreshToken();
            RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);
            assertEquals(sessionId, refreshToken.getSessionState());
            assertEquals(clientId, refreshToken.getIssuedFor());

            AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshTokenString);
            assertEquals(200, refreshResponse.getStatusCode());

            AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());
            RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshResponse.getRefreshToken());
            assertEquals(sessionId, refreshedToken.getSessionState());
            assertEquals(sessionId, refreshedRefreshToken.getSessionState());
            assertEquals(findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId(), refreshedToken.getSubject());

            // Logout
            oauth.doLogout(refreshResponse.getRefreshToken());
            refreshResponse = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken());
            assertEquals(400, refreshResponse.getStatusCode());

        } finally {
            restoreParRealmSettings();
        }
    }

    @Test
    public void testWrongSigningAlgorithmForRequestObject() throws Exception {
        try {
            // setup PAR realm settings
            int requestUriLifespan = 45;
            setParRealmSettings(requestUriLifespan);

            // create client dynamically
            String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME),
                    (OIDCClientRepresentation clientRep) -> {
                        clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
                        clientRep.setRedirectUris(new ArrayList<>(Arrays.asList(CLIENT_REDIRECT_URI)));
                        clientRep.setRequestObjectSigningAlg(Algorithm.PS256);
                    });

            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

            oauth.client(clientId, clientSecret);

            TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = new TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject();
            requestObject.id(KeycloakModelUtils.generateId());
            requestObject.iat(Long.valueOf(Time.currentTime()));
            requestObject.exp(requestObject.getIat() + Long.valueOf(300));
            requestObject.nbf(requestObject.getIat());
            requestObject.setClientId(oauth.getClientId());
            requestObject.setResponseType("code");
            requestObject.setRedirectUriParam(CLIENT_REDIRECT_URI);
            requestObject.setScope("openid");
            requestObject.setNonce(KeycloakModelUtils.generateId());

            byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
            String encodedRequestObject = Base64Url.encode(contentBytes);
            TestOIDCEndpointsApplicationResource client = testingClient.testApp().oidcClientEndpoints();

            // use and set jwks_url
            ClientResource clientResource = ApiUtil
                    .findClientByClientId(adminClient.realm(oauth.getRealm()), oauth.getClientId());
            ClientRepresentation clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep)
                    .setJwksUrl(TestApplicationResourceUrls.clientJwksUri());
            clientResource.update(clientRep);
            client.generateKeys(org.keycloak.crypto.Algorithm.RS256);
            client.registerOIDCRequest(encodedRequestObject, org.keycloak.crypto.Algorithm.RS256);

            // do not send any other parameter but the request request parameter
            oauth.responseType(null);
            oauth.redirectUri(null);
            oauth.scope(null);
            ParResponse pResp = oauth.pushedAuthorizationRequest().request(client.getOIDCRequest()).send();
            assertEquals(400, pResp.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, pResp.getError());
        } finally {
            restoreParRealmSettings();
        }
    }

    @Test
    public void testSuccessfulUsingRequestParameter() throws Exception {
        try {
            // setup PAR realm settings
            int requestUriLifespan = 45;
            setParRealmSettings(requestUriLifespan);

            // create client dynamically
            String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
                clientRep.setRedirectUris(new ArrayList<>(Arrays.asList(CLIENT_REDIRECT_URI)));
            });

            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

            oauth.client(clientId, clientSecret);

            TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = new TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject();
            requestObject.id(KeycloakModelUtils.generateId());
            requestObject.iat(Long.valueOf(Time.currentTime()));
            requestObject.exp(requestObject.getIat() + Long.valueOf(300));
            requestObject.nbf(requestObject.getIat());
            requestObject.setClientId(oauth.getClientId());
            requestObject.setResponseType("code");
            requestObject.setRedirectUriParam(CLIENT_REDIRECT_URI);
            requestObject.setScope("openid");
            requestObject.setNonce(KeycloakModelUtils.generateId());

            byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
            String encodedRequestObject = Base64Url.encode(contentBytes);
            TestOIDCEndpointsApplicationResource client = testingClient.testApp().oidcClientEndpoints();

            // use and set jwks_url
            ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(oauth.getRealm()), oauth.getClientId());
            ClientRepresentation clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(TestApplicationResourceUrls.clientJwksUri());
            clientResource.update(clientRep);
            client.generateKeys(org.keycloak.crypto.Algorithm.RS256);
            client.registerOIDCRequest(encodedRequestObject, org.keycloak.crypto.Algorithm.RS256);

            // do not send any other parameter but the request request parameter
            oauth.responseType(null);
            oauth.redirectUri(null);
            oauth.scope(null);
            ParResponse pResp = oauth.pushedAuthorizationRequest().request(client.getOIDCRequest()).send();
            assertEquals(201, pResp.getStatusCode());
            String requestUri = pResp.getRequestUri();
            assertEquals(requestUriLifespan, pResp.getExpiresIn());

            // Authorization Request with request_uri of PAR
            // remove parameters as query strings of uri
            oauth.redirectUri(null);
            oauth.scope(null);
            oauth.responseType(null);
            AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUri).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

            // Token Request
            oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
            AccessTokenResponse res = oauth.doAccessTokenRequest(loginResponse.getCode());
            assertEquals(200, res.getStatusCode());

            oauth.verifyToken(res.getAccessToken());
            IDToken idToken = oauth.verifyIDToken(res.getIdToken());
            assertEquals(requestObject.getNonce(), idToken.getNonce());
        } finally {
            restoreParRealmSettings();
        }
    }

    @Test
    public void testRequestParameterPrecedenceOverOtherParameters() throws Exception {
        try {
            // setup PAR realm settings
            int requestUriLifespan = 45;
            setParRealmSettings(requestUriLifespan);

            // create client dynamically
            String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
                clientRep.setRedirectUris(new ArrayList<>(Arrays.asList(CLIENT_REDIRECT_URI)));
            });

            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

            oauth.client(clientId, clientSecret);

            TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = new TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject();
            requestObject.id(KeycloakModelUtils.generateId());
            requestObject.iat(Long.valueOf(Time.currentTime()));
            requestObject.exp(requestObject.getIat() + Long.valueOf(300));
            requestObject.nbf(requestObject.getIat());
            requestObject.setClientId(oauth.getClientId());
            requestObject.setResponseType("code");
            requestObject.setRedirectUriParam(CLIENT_REDIRECT_URI);
            requestObject.setScope("openid");
            requestObject.setNonce(KeycloakModelUtils.generateId());
            requestObject.setState("testRequestParameterPrecedenceOverOtherParameters");


            byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
            String encodedRequestObject = Base64Url.encode(contentBytes);
            TestOIDCEndpointsApplicationResource client = testingClient.testApp().oidcClientEndpoints();

            // use and set jwks_url
            ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(oauth.getRealm()), oauth.getClientId());
            ClientRepresentation clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(TestApplicationResourceUrls.clientJwksUri());
            clientResource.update(clientRep);
            client.generateKeys(org.keycloak.crypto.Algorithm.RS256);
            client.registerOIDCRequest(encodedRequestObject, org.keycloak.crypto.Algorithm.RS256);

            // do not send any other parameter but the request request parameter
            oauth.responseType("code id_token");
            oauth.redirectUri("http://invalid");
            oauth.scope(null);
            ParResponse pResp = oauth.pushedAuthorizationRequest().nonce("12345").request(client.getOIDCRequest()).send();
            assertEquals(201, pResp.getStatusCode());
            String requestUri = pResp.getRequestUri();
            assertEquals(requestUriLifespan, pResp.getExpiresIn());

            oauth.scope("invalid");
            oauth.redirectUri("http://invalid");
            oauth.responseType("invalid");
            oauth.redirectUri(null);
            String wrongState = "wrongState";
            AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUri).state(wrongState).nonce("12345").doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            assertEquals(requestObject.getState(), loginResponse.getState());
            assertNotEquals(requestObject.getState(), wrongState);

            // Token Request
            oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
            AccessTokenResponse res = oauth.doAccessTokenRequest(loginResponse.getCode());
            assertEquals(200, res.getStatusCode());

            oauth.verifyToken(res.getAccessToken());
            IDToken idToken = oauth.verifyIDToken(res.getIdToken());
            assertEquals(requestObject.getNonce(), idToken.getNonce());
        } finally {
            restoreParRealmSettings();
        }
    }

    @Test
    public void testIgnoreParameterIfNotSetinRequestObject() throws Exception {
        try {
            // setup PAR realm settings
            int requestUriLifespan = 45;
            setParRealmSettings(requestUriLifespan);

            // create client dynamically
            String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
                clientRep.setRedirectUris(new ArrayList<>(Arrays.asList(CLIENT_REDIRECT_URI)));
            });

            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

            oauth.client(clientId, clientSecret);

            TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = new TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject();
            requestObject.id(KeycloakModelUtils.generateId());
            requestObject.iat(Long.valueOf(Time.currentTime()));
            requestObject.exp(requestObject.getIat() + Long.valueOf(300));
            requestObject.nbf(requestObject.getIat());
            requestObject.setClientId(oauth.getClientId());
            requestObject.setResponseType("code");
            requestObject.setRedirectUriParam(CLIENT_REDIRECT_URI);
            requestObject.setScope("openid");
            requestObject.setNonce(KeycloakModelUtils.generateId());

            byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
            String encodedRequestObject = Base64Url.encode(contentBytes);
            TestOIDCEndpointsApplicationResource client = testingClient.testApp().oidcClientEndpoints();

            // use and set jwks_url
            ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(oauth.getRealm()), oauth.getClientId());
            ClientRepresentation clientRep = clientResource.toRepresentation();
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(TestApplicationResourceUrls.clientJwksUri());
            clientResource.update(clientRep);
            client.generateKeys(org.keycloak.crypto.Algorithm.RS256);
            client.registerOIDCRequest(encodedRequestObject, org.keycloak.crypto.Algorithm.RS256);

            // do not send any other parameter but the request request parameter
            oauth.responseType("code id_token");
            oauth.redirectUri("http://invalid");
            oauth.scope(null);
            ParResponse pResp = oauth.pushedAuthorizationRequest().nonce("12345").request(client.getOIDCRequest()).send();
            assertEquals(201, pResp.getStatusCode());
            String requestUri = pResp.getRequestUri();
            assertEquals(requestUriLifespan, pResp.getExpiresIn());

            oauth.scope("invalid");
            oauth.redirectUri("http://invalid");
            oauth.responseType("invalid");
            oauth.redirectUri(null);
            String wrongState = "wrongState";
            AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUri).state(wrongState).nonce("12345").doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            assertNull(loginResponse.getState());
            assertNotEquals(requestObject.getState(), wrongState);

            // Token Request
            oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
            AccessTokenResponse res = oauth.doAccessTokenRequest(loginResponse.getCode());
            assertEquals(200, res.getStatusCode());

            oauth.verifyToken(res.getAccessToken());
            IDToken idToken = oauth.verifyIDToken(res.getIdToken());
            assertEquals(requestObject.getNonce(), idToken.getNonce());
        } finally {
            restoreParRealmSettings();
        }
    }

    // success with the same client conducting multiple authz requests + PAR simultaneously
    @Test
    public void testSuccessfulMultipleParBySameClient() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.FALSE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

        // Pushed Authorization Request #1
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUriOne = pResp.getRequestUri();

        // Pushed Authorization Request #2
        oauth.client(clientId, clientSecret);
        oauth.scope("microprofile-jwt" + " " + "profile");
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUriTwo = pResp.getRequestUri();

        // Authorization Request with request_uri of PAR #2
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        String state = "testSuccessfulMultipleParBySameClient";
        AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUriTwo).state(state).doLogin(TEST_USER2_NAME, TEST_USER2_PASSWORD);
        assertEquals(state, loginResponse.getState());
        String code = loginResponse.getCode();
        String sessionId =loginResponse.getSessionState();

        // Token Request #2
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        String userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER2_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals(TEST_USER2_NAME, token.getSubject());
        assertEquals(clientId, token.getIssuedFor());
        assertTrue(token.getScope().contains("openid"));
        assertTrue(token.getScope().contains("microprofile-jwt"));
        assertTrue(token.getScope().contains("profile"));

        // Logout
        oauth.doLogout(res.getRefreshToken()); // same oauth instance is used so that this logout is needed to send authz request consecutively.

        // Authorization Request with request_uri of PAR #1
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        state = "testSuccessfulMultipleParBySameClient2";
        loginResponse = oauth.loginForm().state(state).requestUri(requestUriOne).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(state, loginResponse.getState());
        code = loginResponse.getCode();
        sessionId =loginResponse.getSessionState();

        // Token Request #1
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());

        token = oauth.verifyToken(res.getAccessToken());
        userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
        assertEquals(clientId, token.getIssuedFor());
        assertFalse(token.getScope().contains("microprofile-jwt"));
        assertTrue(token.getScope().contains("openid"));
    }

    // success with several clients conducting multiple authz requests + PAR simultaneously
    @Test
    public void testSuccessfulMultipleParByMultipleClients() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.FALSE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

        authManageClients(); // call it when several clients are created consecutively.

        String client2Id = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcC2Rep = getClientDynamically(client2Id);
        String client2Secret = oidcC2Rep.getClientSecret();
        assertEquals(Boolean.TRUE, oidcC2Rep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcC2Rep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcC2Rep.getTokenEndpointAuthMethod());

        // Pushed Authorization Request #1
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUriOne = pResp.getRequestUri();

        // Pushed Authorization Request #2
        oauth.client(client2Id, client2Secret);
        oauth.scope("microprofile-jwt" + " " + "profile");
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUriTwo = pResp.getRequestUri();

        // Authorization Request with request_uri of PAR #2
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        String state = "testSuccessfulMultipleParByMultipleClients";
        AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUriTwo).state(state).doLogin(TEST_USER2_NAME, TEST_USER2_PASSWORD);
        assertEquals(state, loginResponse.getState());
        String code = loginResponse.getCode();
        String sessionId =loginResponse.getSessionState();

        // Token Request #2
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        String userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER2_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
        assertEquals(client2Id, token.getIssuedFor());
        assertTrue(token.getScope().contains("openid"));
        assertTrue(token.getScope().contains("microprofile-jwt"));
        assertTrue(token.getScope().contains("profile"));

        // Logout
        oauth.doLogout(res.getRefreshToken()); // same oauth instance is used so that this logout is needed to send authz request consecutively.

        // Authorization Request with request_uri of PAR #1
        // remove parameters as query strings of uri
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        state = "testSuccessfulMultipleParByMultipleClients2";
        loginResponse = oauth.loginForm().state(state).requestUri(requestUriOne).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(state, loginResponse.getState());
        code = loginResponse.getCode();
        sessionId =loginResponse.getSessionState();

        // Token Request #1
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());

        token = oauth.verifyToken(res.getAccessToken());
        userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
        assertEquals(clientId, token.getIssuedFor());
        assertFalse(token.getScope().contains("microprofile-jwt"));
        assertTrue(token.getScope().contains("openid"));
    }

    // not issued PAR request_uri used
    @Test
    public void testFailureNotIssuedParUsed() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

        // Pushed Authorization Request
        // but not use issued request_uri
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());

        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        // use not issued request_uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        String state = "testFailureNotIssuedParUsed";
        oauth.loginForm().requestUri(IMAGINARY_REQUEST_URI).state(state).open();
        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        Assert.assertFalse(errorResponse.isRedirected());
    }

    // PAR request_uri used twice
    @Test
    public void testFailureParUsedTwice() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        String state = "testFailureParUsedTwice";
        AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUri).state(state).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(state, loginResponse.getState());
        String code = loginResponse.getCode();

        // Token Request
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());

        // Authorization Request with request_uri of PAR
        // use same redirect_uri
        state = "testFailureParUsedTwice2";
        oauth.loginForm().requestUri(requestUri).state(state).open();
        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        Assert.assertFalse(errorResponse.isRedirected());
    }

    // PAR request_uri used by other client
    @Test
    public void testFailureParUsedByOtherClient() throws Exception {
        // create client dynamically
        String victimClientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.FALSE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation victimOidcCRep = getClientDynamically(victimClientId);
        String victimClientSecret = victimOidcCRep.getClientSecret();
        assertEquals(Boolean.FALSE, victimOidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(victimOidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, victimOidcCRep.getTokenEndpointAuthMethod());

        authManageClients();

        String attackerClientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation attackerOidcCRep = getClientDynamically(attackerClientId);
        assertEquals(Boolean.TRUE, attackerOidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(attackerOidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, attackerOidcCRep.getTokenEndpointAuthMethod());

        // Pushed Authorization Request
        oauth.client(victimClientId, victimClientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        // used by other client
        oauth.client(attackerClientId);
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        String state = "testFailureParUsedByOtherClient";
        oauth.loginForm().state(state).requestUri(requestUri).open();
        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        Assert.assertFalse(errorResponse.isRedirected());
    }

    // not PAR by PAR required client
    @Test
    public void testFailureNotParByParRequiredCilent() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());

        oauth.client(clientId, clientSecret);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.parseLoginResponse().getError());
        assertEquals("Pushed Authorization Request is only allowed.", oauth.parseLoginResponse().getErrorDescription());

        updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.FALSE);
        });

        AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String code = loginResponse.getCode();

        // Token Request
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());
    }

    // expired PAR used
    @Test
    public void testFailureParExpired() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();
        int expiresIn = pResp.getExpiresIn();

        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        // PAR expired
        setTimeOffset(expiresIn + 5);
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        String state = "testFailureParExpired";
        oauth.loginForm().state(state).requestUri(requestUri).open();
        AuthorizationEndpointResponse errorResponse = oauth.parseLoginResponse();
        Assert.assertFalse(errorResponse.isRedirected());
    }

    // client authentication failed
    @Test
    public void testFailureClientAuthnFailed() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret + "abc");
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(401, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());
        assertEquals("Authentication failed.", pResp.getErrorDescription());
    }

    // PAR including request_uri
    @Test
    public void testFailureParIncludesRequestUri() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.FALSE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.pushedAuthorizationRequest().requestUri(IMAGINARY_REQUEST_URI).send();
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());
        assertEquals("It is not allowed to include request_uri to PAR.", pResp.getErrorDescription());
    }

    // invalid PAR
    @Test
    public void testFailureInvalidPar() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        updateClientByAdmin(clientId, (ClientRepresentation cRep)->{
            OIDCAdvancedConfigWrapper.fromClientRepresentation(cRep).setRequestObjectRequired(OIDCConfigAttributes.REQUEST_OBJECT_REQUIRED_REQUEST);
        });

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());
    }

    // PAR including invalid redirect_uri
    @Test
    public void testFailureParIncludesInvalidRedirectUri() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(INVALID_CORS_URL);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());
        assertEquals("Invalid parameter: redirect_uri", pResp.getErrorDescription());
    }

    // PAR including invalid response_type
    @Test
    public void testFailureParIncludesInvalidResponseType() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.FALSE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        oauth.responseType(null);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());
        assertEquals("Missing parameter: response_type", pResp.getErrorDescription());
    }

    // PAR including invalid scope
    @Test
    public void testFailureParIncludesInvalidScope() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        oauth.scope("not_registered_scope");
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());
        assertEquals("Invalid scopes: openid not_registered_scope", pResp.getErrorDescription());
    }

    // PAR invalid PKCE setting
    @Test
    public void testFailureParInvalidPkceSetting() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        updateClientByAdmin(clientId, (ClientRepresentation cRep)->{
            OIDCAdvancedConfigWrapper.fromClientRepresentation(cRep).setPkceCodeChallengeMethod("S256");
        });

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest();
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());
        assertEquals("Missing parameter: code_challenge_method", pResp.getErrorDescription());
    }

    // CORS test
    @Test
    public void testParCorsRequestWithValidUrl() throws Exception {
        try {
            // create client dynamically
            String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setRequirePushedAuthorizationRequests(Boolean.FALSE);
                clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI, VALID_CORS_URL + "/realms/master/app")));
            });
            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

            updateClientByAdmin(clientId, (ClientRepresentation cRep)->{
                cRep.setOrigin(VALID_CORS_URL);
            });

            // Pushed Authorization Request
            oauth.client(clientId, clientSecret);
            oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");
            oauth.origin(VALID_CORS_URL);
            ParResponse pResp = oauth.doPushedAuthorizationRequest();
            assertEquals(201, pResp.getStatusCode());
            assertCors(pResp);
            String requestUri = pResp.getRequestUri();

            doNormalAuthzProcess(requestUri, VALID_CORS_URL + "/realms/master/app", clientId, clientSecret);
        } finally {
            oauth.origin(null);
        }
    }

    // CORS test
    @Test
    public void testParCorsRequestWithInvalidUrlShouldFail() throws Exception {
        try {
            // create client dynamically
            String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
                clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI, VALID_CORS_URL + "/realms/master/app")));
            });
            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.FALSE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

            updateClientByAdmin(clientId, (ClientRepresentation cRep)->{
                cRep.setOrigin(VALID_CORS_URL);
            });

            // Pushed Authorization Request
            oauth.client(clientId, clientSecret);
            oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");
            oauth.origin(INVALID_CORS_URL);
            ParResponse pResp = oauth.doPushedAuthorizationRequest();
            assertEquals(201, pResp.getStatusCode());
            assertNotCors(pResp);
            String requestUri = pResp.getRequestUri();

            doNormalAuthzProcess(requestUri, VALID_CORS_URL + "/realms/master/app", clientId, clientSecret);

        } finally {
            oauth.origin(null);
        }
    }

    @Test
    public void testExtendedClientPolicyIntefacesForPar() throws Exception {
        // create client dynamically
        String clientId = createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
            clientRep.setRedirectUris(new ArrayList<String>(Arrays.asList(CLIENT_REDIRECT_URI)));
        });
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                    .addExecutor(TestRaiseExceptionExecutorFactory.PROVIDER_ID,
                            createTestRaiseExeptionExecutorConfig(Arrays.asList(ClientPolicyEvent.PUSHED_AUTHORIZATION_REQUEST)))
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register role policy
        String roleName = "sample-client-role-alpha";
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forste Politikken", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(Arrays.asList(roleName)))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Add role to the client
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        clientResource.roles().create(RoleBuilder.create().name(roleName).build());

        // Pushed Authorization Request
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse response = oauth.doPushedAuthorizationRequest();
        assertEquals(400, response.getStatusCode());
        assertEquals(ClientPolicyEvent.PUSHED_AUTHORIZATION_REQUEST.toString(), response.getError());
        assertEquals("Exception thrown intentionally", response.getErrorDescription());
    }

    private void doNormalAuthzProcess(String requestUri, String redirectUrl, String clientId, String clientSecret) {
        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        String state = "doNormalAuthzProcess";
        AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUri).state(state).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(state, loginResponse.getState());
        String code = loginResponse.getCode();
        String sessionId =loginResponse.getSessionState();

        // Token Request
        oauth.redirectUri(redirectUrl); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        String userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
        assertEquals(clientId, token.getIssuedFor());

        // Token Refresh
        String refreshTokenString = res.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);
        assertEquals(sessionId, refreshToken.getSessionState());
        assertEquals(clientId, refreshToken.getIssuedFor());

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshTokenString);
        assertEquals(200, refreshResponse.getStatusCode());

        AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshResponse.getRefreshToken());
        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());
        assertEquals(findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId(), refreshedToken.getSubject());

        // Logout
        oauth.doLogout(refreshResponse.getRefreshToken());
        refreshResponse = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken());
        assertEquals(400, refreshResponse.getStatusCode());
    }

    private void setParRealmSettings(int requestUriLifespan) {
        RealmRepresentation rep = adminClient.realm(REALM_NAME).toRepresentation();
        Map<String, String> attributes = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
        attributes.put(ParConfig.PAR_REQUEST_URI_LIFESPAN, String.valueOf(requestUriLifespan));
        rep.setAttributes(attributes);
        adminClient.realm(REALM_NAME).update(rep);
    }

    private void restoreParRealmSettings() {
        setParRealmSettings(DEFAULT_REQUEST_URI_LIFESPAN);
    }

    private static void assertCors(AbstractHttpResponse response) {
        assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
        assertEquals(VALID_CORS_URL, response.getHeader("Access-Control-Allow-Origin"));
        assertEquals("Access-Control-Allow-Methods", response.getHeader("Access-Control-Expose-Headers"));
    }

    private static void assertNotCors(AbstractHttpResponse response) {
        assertNull(response.getHeader("Access-Control-Allow-Credentials"));
        assertNull(response.getHeader("Access-Control-Allow-Origin"));
        assertNull(response.getHeader("Access-Control-Expose-Headers"));
    }

}
