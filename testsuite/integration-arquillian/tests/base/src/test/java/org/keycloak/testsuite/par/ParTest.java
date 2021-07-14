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

package org.keycloak.testsuite.par;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.core.UriBuilder;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Assert;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
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
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.AbstractClientPoliciesTest;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.services.clientpolicy.executor.TestRaiseExeptionExecutorFactory;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.OAuthClient.ParResponse;
import org.keycloak.util.JsonSerialization;

import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.QUARKUS;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;

@EnableFeature(value = Profile.Feature.PAR, skipRestart = true)
@AuthServerContainerExclude({REMOTE, QUARKUS})
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
            oauth.clientId(clientId);
            oauth.redirectUri(CLIENT_REDIRECT_URI);
            ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
            assertEquals(201, pResp.getStatusCode());
            String requestUri = pResp.getRequestUri();
            assertEquals(requestUriLifespan, pResp.getExpiresIn());

            // Authorization Request with request_uri of PAR
            // remove parameters as query strings of uri
            oauth.redirectUri(null);
            oauth.scope(null);
            oauth.responseType(null);
            oauth.requestUri(requestUri);
            String state = oauth.stateParamRandom().getState();
            oauth.stateParamHardcoded(state);
            OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            assertEquals(state, loginResponse.getState());
            String code = loginResponse.getCode();
            String sessionId =loginResponse.getSessionState();

            // Token Request
            oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
            OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
            assertEquals(200, res.getStatusCode());

            AccessToken token = oauth.verifyToken(res.getAccessToken());
            String userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId();
            assertEquals(userId, token.getSubject());
            assertEquals(sessionId, token.getSessionState());
            Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
            assertEquals(clientId, token.getIssuedFor());

            // Token Refresh
            String refreshTokenString = res.getRefreshToken();
            RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);
            assertEquals(sessionId, refreshToken.getSessionState());
            assertEquals(clientId, refreshToken.getIssuedFor());

            OAuthClient.AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshTokenString, clientSecret);
            assertEquals(200, refreshResponse.getStatusCode());

            AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());
            RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshResponse.getRefreshToken());
            assertEquals(sessionId, refreshedToken.getSessionState());
            assertEquals(sessionId, refreshedRefreshToken.getSessionState());
            assertEquals(findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId(), refreshedToken.getSubject());

            // Logout
            oauth.doLogout(refreshResponse.getRefreshToken(), clientSecret);
            refreshResponse = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken(), clientSecret);
            assertEquals(400, refreshResponse.getStatusCode());

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

            oauth.clientId(clientId);

            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

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
            oauth.request(client.getOIDCRequest());
            oauth.responseType(null);
            oauth.redirectUri(null);
            oauth.scope(null);
            ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
            assertEquals(201, pResp.getStatusCode());
            String requestUri = pResp.getRequestUri();
            assertEquals(requestUriLifespan, pResp.getExpiresIn());

            // Authorization Request with request_uri of PAR
            // remove parameters as query strings of uri
            oauth.redirectUri(null);
            oauth.scope(null);
            oauth.responseType(null);
            oauth.request(null);
            oauth.requestUri(requestUri);
            OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

            // Token Request
            oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
            OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(loginResponse.getCode(), clientSecret);
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

            oauth.clientId(clientId);

            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

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
            requestObject.setState(oauth.stateParamRandom().getState());


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
            oauth.request(client.getOIDCRequest());
            oauth.responseType("code id_token");
            oauth.redirectUri("http://invalid");
            oauth.scope(null);
            oauth.nonce("12345");
            ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
            assertEquals(201, pResp.getStatusCode());
            String requestUri = pResp.getRequestUri();
            assertEquals(requestUriLifespan, pResp.getExpiresIn());

            oauth.scope("invalid");
            oauth.redirectUri("http://invalid");
            oauth.responseType("invalid");
            oauth.redirectUri(null);
            oauth.nonce("12345");
            oauth.request(null);
            oauth.requestUri(requestUri);
            String wrongState = oauth.stateParamRandom().getState();
            oauth.stateParamHardcoded(wrongState);
            OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            assertEquals(requestObject.getState(), loginResponse.getState());
            assertNotEquals(requestObject.getState(), wrongState);

            // Token Request
            oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
            OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(loginResponse.getCode(), clientSecret);
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

            oauth.clientId(clientId);

            OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
            String clientSecret = oidcCRep.getClientSecret();
            assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
            assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
            assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());

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
            oauth.request(client.getOIDCRequest());
            oauth.responseType("code id_token");
            oauth.redirectUri("http://invalid");
            oauth.scope(null);
            oauth.nonce("12345");
            ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
            assertEquals(201, pResp.getStatusCode());
            String requestUri = pResp.getRequestUri();
            assertEquals(requestUriLifespan, pResp.getExpiresIn());

            oauth.scope("invalid");
            oauth.redirectUri("http://invalid");
            oauth.responseType("invalid");
            oauth.redirectUri(null);
            oauth.nonce("12345");
            oauth.request(null);
            oauth.requestUri(requestUri);
            String wrongState = oauth.stateParamRandom().getState();
            oauth.stateParamHardcoded(wrongState);
            OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            assertNull(loginResponse.getState());
            assertNotEquals(requestObject.getState(), wrongState);

            // Token Request
            oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
            OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(loginResponse.getCode(), clientSecret);
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        assertEquals(201, pResp.getStatusCode());
        String requestUriOne = pResp.getRequestUri();

        // Pushed Authorization Request #2
        oauth.clientId(clientId);
        oauth.scope("microprofile-jwt" + " " + "profile");
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        assertEquals(201, pResp.getStatusCode());
        String requestUriTwo = pResp.getRequestUri();

        // Authorization Request with request_uri of PAR #2
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        oauth.requestUri(requestUriTwo);
        String state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER2_NAME, TEST_USER2_PASSWORD);
        assertEquals(state, loginResponse.getState());
        String code = loginResponse.getCode();
        String sessionId =loginResponse.getSessionState();

        // Token Request #2
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        String userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER2_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        Assert.assertNotEquals(TEST_USER2_NAME, token.getSubject());
        assertEquals(clientId, token.getIssuedFor());
        assertTrue(token.getScope().contains("openid"));
        assertTrue(token.getScope().contains("microprofile-jwt"));
        assertTrue(token.getScope().contains("profile"));

        // Logout
        oauth.doLogout(res.getRefreshToken(), clientSecret); // same oauth instance is used so that this logout is needed to send authz request consecutively.

        // Authorization Request with request_uri of PAR #1
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        oauth.requestUri(requestUriOne);
        state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(state, loginResponse.getState());
        code = loginResponse.getCode();
        sessionId =loginResponse.getSessionState();

        // Token Request #1
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());

        token = oauth.verifyToken(res.getAccessToken());
        userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        assertEquals(201, pResp.getStatusCode());
        String requestUriOne = pResp.getRequestUri();

        // Pushed Authorization Request #2
        oauth.clientId(client2Id);
        oauth.scope("microprofile-jwt" + " " + "profile");
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        pResp = oauth.doPushedAuthorizationRequest(client2Id, client2Secret);
        assertEquals(201, pResp.getStatusCode());
        String requestUriTwo = pResp.getRequestUri();

        // Authorization Request with request_uri of PAR #2
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        oauth.requestUri(requestUriTwo);
        String state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER2_NAME, TEST_USER2_PASSWORD);
        assertEquals(state, loginResponse.getState());
        String code = loginResponse.getCode();
        String sessionId =loginResponse.getSessionState();

        // Token Request #2
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, client2Secret);
        assertEquals(200, res.getStatusCode());

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        String userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER2_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        Assert.assertNotEquals(TEST_USER2_NAME, token.getSubject());
        assertEquals(client2Id, token.getIssuedFor());
        assertTrue(token.getScope().contains("openid"));
        assertTrue(token.getScope().contains("microprofile-jwt"));
        assertTrue(token.getScope().contains("profile"));

        // Logout
        oauth.doLogout(res.getRefreshToken(), client2Secret); // same oauth instance is used so that this logout is needed to send authz request consecutively.

        // Authorization Request with request_uri of PAR #1
        // remove parameters as query strings of uri
        oauth.clientId(clientId);
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        oauth.requestUri(requestUriOne);
        state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(state, loginResponse.getState());
        code = loginResponse.getCode();
        sessionId =loginResponse.getSessionState();

        // Token Request #1
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());

        token = oauth.verifyToken(res.getAccessToken());
        userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        assertEquals(201, pResp.getStatusCode());

        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        // use not issued request_uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        oauth.requestUri(IMAGINARY_REQUEST_URI);
        String state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        driver.navigate().to(b.build().toURL());
        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        oauth.requestUri(requestUri);
        String state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(state, loginResponse.getState());
        String code = loginResponse.getCode();

        // Token Request
        oauth.redirectUri(CLIENT_REDIRECT_URI); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());

        // Authorization Request with request_uri of PAR
        // use same redirect_uri
        state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        driver.navigate().to(b.build().toURL());
        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);
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
        oauth.clientId(victimClientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(victimClientId, victimClientSecret);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        // used by other client
        oauth.clientId(attackerClientId);
        oauth.redirectUri(null);
        oauth.scope(null);
        oauth.responseType(null);
        oauth.requestUri(requestUri);
        String state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        driver.navigate().to(b.build().toURL());
        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);
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

        oauth.clientId(clientId);
        oauth.openLoginForm();
        assertEquals(OAuthErrorException.INVALID_REQUEST, oauth.getCurrentQuery().get(OAuth2Constants.ERROR));
        assertEquals("Pushed Authorization Request is only allowed.", oauth.getCurrentQuery().get(OAuth2Constants.ERROR_DESCRIPTION));

        updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.FALSE);
        });

        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String code = loginResponse.getCode();

        // Token Request
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
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
        oauth.requestUri(requestUri);
        String state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        UriBuilder b = UriBuilder.fromUri(oauth.getLoginFormUrl());
        driver.navigate().to(b.build().toURL());
        OAuthClient.AuthorizationEndpointResponse errorResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret + "abc");
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        oauth.requestUri(IMAGINARY_REQUEST_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
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
        oauth.clientId(clientId);
        oauth.redirectUri(INVALID_CORS_URL);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        oauth.responseType(null);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        oauth.scope("not_registered_scope");
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
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
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
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
            oauth.clientId(clientId);
            oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");
            oauth.origin(VALID_CORS_URL);
            ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret, (CloseableHttpResponse c)->{
                assertCors(c);
            });
            assertEquals(201, pResp.getStatusCode());
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
            oauth.clientId(clientId);
            oauth.redirectUri(VALID_CORS_URL + "/realms/master/app");
            oauth.origin(INVALID_CORS_URL);
            ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, clientSecret, (CloseableHttpResponse c)->{
                assertNotCors(c);
            });
            assertEquals(201, pResp.getStatusCode());
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
                    .addExecutor(TestRaiseExeptionExecutorFactory.PROVIDER_ID, null)
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        // Pushed Authorization Request
        oauth.clientId(clientId);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        ParResponse response = oauth.doPushedAuthorizationRequest(clientId, clientSecret);
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
        oauth.requestUri(requestUri);
        String state = oauth.stateParamRandom().getState();
        oauth.stateParamHardcoded(state);
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        assertEquals(state, loginResponse.getState());
        String code = loginResponse.getCode();
        String sessionId =loginResponse.getSessionState();

        // Token Request
        oauth.redirectUri(redirectUrl); // get tokens, it needed. https://datatracker.ietf.org/doc/html/rfc6749#section-4.1.3
        OAuthClient.AccessTokenResponse res = oauth.doAccessTokenRequest(code, clientSecret);
        assertEquals(200, res.getStatusCode());

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        String userId = findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId();
        assertEquals(userId, token.getSubject());
        assertEquals(sessionId, token.getSessionState());
        Assert.assertNotEquals(TEST_USER_NAME, token.getSubject());
        assertEquals(clientId, token.getIssuedFor());

        // Token Refresh
        String refreshTokenString = res.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);
        assertEquals(sessionId, refreshToken.getSessionState());
        assertEquals(clientId, refreshToken.getIssuedFor());

        OAuthClient.AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshTokenString, clientSecret);
        assertEquals(200, refreshResponse.getStatusCode());

        AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshResponse.getRefreshToken());
        assertEquals(sessionId, refreshedToken.getSessionState());
        assertEquals(sessionId, refreshedRefreshToken.getSessionState());
        assertEquals(findUserByUsername(adminClient.realm(REALM_NAME), TEST_USER_NAME).getId(), refreshedToken.getSubject());

        // Logout
        oauth.doLogout(refreshResponse.getRefreshToken(), clientSecret);
        refreshResponse = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken(), clientSecret);
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

    private static void assertCors(CloseableHttpResponse response) {
        assertEquals("true", response.getHeaders("Access-Control-Allow-Credentials")[0].getValue());
        assertEquals(VALID_CORS_URL, response.getHeaders("Access-Control-Allow-Origin")[0].getValue());
        assertEquals("Access-Control-Allow-Methods", response.getHeaders("Access-Control-Expose-Headers")[0].getValue());
    }

    private static void assertNotCors(CloseableHttpResponse response) {
        assertEquals(0, response.getHeaders("Access-Control-Allow-Credentials").length);
        assertEquals(0, response.getHeaders("Access-Control-Allow-Origin").length);
        assertEquals(0, response.getHeaders("Access-Control-Expose-Headers").length);
    }

}