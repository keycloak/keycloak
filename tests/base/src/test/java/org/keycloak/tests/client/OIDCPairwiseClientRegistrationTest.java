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

package org.keycloak.tests.client;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.SectorIdentifierRedirectUrisProvider;
import org.keycloak.testframework.oauth.annotations.InjectSectorIdentifierRedirectUrisProvider;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.tests.common.BasicUserConfig;
import org.keycloak.tests.utils.Assert;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@KeycloakIntegrationTest
public class OIDCPairwiseClientRegistrationTest extends AbstractClientRegistrationTest {

    @InjectSectorIdentifierRedirectUrisProvider
    private SectorIdentifierRedirectUrisProvider sectorIdentifierRedirectUrisProvider;

    @InjectUser(config = BasicUserConfig.class)
    ManagedUser managedUser;

    @BeforeEach
    @Override
    public void before() throws Exception {
        super.before();
        oauth.getDriver().navigate().to(managedRealm.getBaseUrl());
        oauth.getDriver().manage().deleteAllCookies();

        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    private OIDCClientRepresentation createRep() {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("RegistrationAccessTokenTest");
        client.setClientUri(oauth.getRedirectUri());
        client.setRedirectUris(Collections.singletonList(oauth.getRedirectUri()));
        return client;
    }

    public OIDCClientRepresentation create() throws ClientRegistrationException {
        OIDCClientRepresentation client = createRep();

        OIDCClientRepresentation response = reg.oidc().create(client);

        // Add audience mapper so the client can introspect its own tokens
        String clientId = response.getClientId();
        ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
        audienceMapper.setName("audience-mapper");
        audienceMapper.setProtocol("openid-connect");
        audienceMapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);

        Map<String, String> config = new HashMap<>();
        config.put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, clientId);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        audienceMapper.setConfig(config);

        managedRealm.updateClientWithCleanup(clientId, c -> c.protocolMappers(audienceMapper));

        return response;
    }

    public OIDCClientRepresentation createPairwise() throws ClientRegistrationException {
        // Create pairwise client
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");
        OIDCClientRepresentation pairwiseClient = reg.oidc().create(clientRep);

        // Add audience mapper so the client can introspect its own tokens
        String clientId = pairwiseClient.getClientId();
        ProtocolMapperRepresentation audienceMapper = new ProtocolMapperRepresentation();
        audienceMapper.setName("audience-mapper");
        audienceMapper.setProtocol("openid-connect");
        audienceMapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);

        java.util.Map<String, String> config = new java.util.HashMap<>();
        config.put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, clientId);
        config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
        audienceMapper.setConfig(config);

        managedRealm.updateClientWithCleanup(clientId, c -> c.protocolMappers(audienceMapper));

        return pairwiseClient;
    }


    private void assertCreateFail(OIDCClientRepresentation client, int expectedStatusCode, String expectedErrorContains) {
        try {
            reg.oidc().create(client);
            Assertions.fail("Not expected to successfuly register client");
        } catch (ClientRegistrationException expected) {
            HttpErrorException httpEx = (HttpErrorException) expected.getCause();
            Assertions.assertEquals(expectedStatusCode, httpEx.getStatusLine().getStatusCode());
            if (expectedErrorContains != null) {
                assertTrue(httpEx.getErrorResponse().contains(expectedErrorContains), "Error response doesn't contain expected text");
            }
        }
    }

    @Test
    public void createPairwiseClient() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assertions.assertEquals("pairwise", response.getSubjectType());
    }

    @Test
    public void updateClientToPairwise() throws Exception {
        OIDCClientRepresentation response = create();
        Assertions.assertEquals("public", response.getSubjectType());

        reg.auth(Auth.token(response));
        response.setSubjectType("pairwise");
        OIDCClientRepresentation updated = reg.oidc().update(response);

        Assertions.assertEquals("pairwise", updated.getSubjectType());
    }

    @Test
    public void updateSectorIdentifierUri() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assertions.assertEquals("pairwise", response.getSubjectType());
        Assertions.assertNull(response.getSectorIdentifierUri());

        reg.auth(Auth.token(response));

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.addAll(response.getRedirectUris());
        sectorIdentifierRedirectUrisProvider.setSectorIdentifierRedirectUris(sectorRedirects);

        response.setSectorIdentifierUri(sectorIdentifierRedirectUrisProvider.getUri());

        OIDCClientRepresentation updated = reg.oidc().update(response);

        Assertions.assertEquals("pairwise", updated.getSubjectType());
        Assertions.assertEquals(sectorIdentifierRedirectUrisProvider.getUri(), updated.getSectorIdentifierUri());

    }

    @Test
    public void updateToPairwiseThroughAdminRESTSuccess() throws Exception {
        OIDCClientRepresentation response = create();
        Assertions.assertEquals("public", response.getSubjectType());
        Assertions.assertNull(response.getSectorIdentifierUri());

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.addAll(response.getRedirectUris());
        sectorIdentifierRedirectUrisProvider.setSectorIdentifierRedirectUris(sectorRedirects);

        String sectorIdentifierUri = sectorIdentifierRedirectUrisProvider.getUri();

        // Add protocolMapper through admin REST endpoint
        String clientId = response.getClientId();
        ProtocolMapperRepresentation pairwiseProtMapper = SHA256PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri, null);
        managedRealm.updateClientWithCleanup(clientId, c -> c.protocolMappers(pairwiseProtMapper));

        reg.auth(Auth.token(response));
        OIDCClientRepresentation rep = reg.oidc().get(response.getClientId());
        Assertions.assertEquals("pairwise", rep.getSubjectType());
        Assertions.assertEquals(sectorIdentifierUri, rep.getSectorIdentifierUri());

    }

    @Test
    public void updateToPairwiseThroughAdminRESTFailure() throws Exception {
        OIDCClientRepresentation response = create();
        Assertions.assertEquals("public", response.getSubjectType());
        Assertions.assertNull(response.getSectorIdentifierUri());

        // Push empty list to the sector identifier URI
        sectorIdentifierRedirectUrisProvider.setSectorIdentifierRedirectUris(new ArrayList<>());

        String sectorIdentifierUri = sectorIdentifierRedirectUrisProvider.getUri();

        // Add protocolMapper through admin REST endpoint
        String clientId = response.getClientId();
        ProtocolMapperRepresentation pairwiseProtMapper = SHA256PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri, null);
        ClientResource clientResource = AdminApiUtil.findClientByClientId(managedRealm.admin(), clientId);
        try (Response resp = clientResource.getProtocolMappers().createMapper(pairwiseProtMapper)) {
            Assertions.assertEquals(400, resp.getStatus());
        }

        // Assert still public
        reg.auth(Auth.token(response));
        OIDCClientRepresentation rep = reg.oidc().get(response.getClientId());
        Assertions.assertEquals("public", rep.getSubjectType());
        Assertions.assertNull(rep.getSectorIdentifierUri());
    }

    @Test
    public void createPairwiseClientWithSectorIdentifierURI() throws Exception {
        OIDCClientRepresentation clientRep = createRep();

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.addAll(clientRep.getRedirectUris());
        sectorIdentifierRedirectUrisProvider.setSectorIdentifierRedirectUris(sectorRedirects);

        clientRep.setSubjectType("pairwise");
        clientRep.setSectorIdentifierUri(sectorIdentifierRedirectUrisProvider.getUri());

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assertions.assertEquals("pairwise", response.getSubjectType());
        Assertions.assertEquals(sectorIdentifierRedirectUrisProvider.getUri(), response.getSectorIdentifierUri());
    }

    @Test
    public void createPairwiseClientWithRedirectsToMultipleHostsWithoutSectorIdentifierURI() {
        OIDCClientRepresentation clientRep = createRep();

        List<String> redirects = new ArrayList<>();
        redirects.add("http://redirect1");
        redirects.add("http://redirect2");

        clientRep.setSubjectType("pairwise");
        clientRep.setRedirectUris(redirects);

        assertCreateFail(clientRep, 400, "Without a configured Sector Identifier URI, client redirect URIs must not contain multiple host components.");
    }

    @Test
    public void createPairwiseClientWithRedirectsToMultipleHosts() throws Exception {
        OIDCClientRepresentation clientRep = createRep();

        // Push redirect URIs to the sector identifier URI
        List<String> redirects = new ArrayList<>();
        redirects.add("http://redirect1");
        redirects.add("http://redirect2");
        sectorIdentifierRedirectUrisProvider.setSectorIdentifierRedirectUris(redirects);

        clientRep.setSubjectType("pairwise");
        clientRep.setSectorIdentifierUri(sectorIdentifierRedirectUrisProvider.getUri());
        clientRep.setRedirectUris(redirects);

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assertions.assertEquals("pairwise", response.getSubjectType());
        Assertions.assertEquals(sectorIdentifierRedirectUrisProvider.getUri(), response.getSectorIdentifierUri());
        Assert.assertNames(response.getRedirectUris(), "http://redirect1", "http://redirect2");
    }

    @Test
    public void createPairwiseClientWithSectorIdentifierURIContainingMismatchedRedirects() {
        OIDCClientRepresentation clientRep = createRep();

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.add("http://someotherredirect");
        sectorIdentifierRedirectUrisProvider.setSectorIdentifierRedirectUris(sectorRedirects);

        clientRep.setSubjectType("pairwise");
        clientRep.setSectorIdentifierUri(sectorIdentifierRedirectUrisProvider.getUri());

        assertCreateFail(clientRep, 400, "Client redirect URIs does not match redirect URIs fetched from the Sector Identifier URI.");
    }

    @Test
    public void createPairwiseClientWithSectorIdentifierURIContainingMismatchedRedirectsPublicSubject() {
        OIDCClientRepresentation clientRep = createRep();

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.add("http://someotherredirect");
        sectorIdentifierRedirectUrisProvider.setSectorIdentifierRedirectUris(sectorRedirects);

        clientRep.setSubjectType("public");
        clientRep.setSectorIdentifierUri(sectorIdentifierRedirectUrisProvider.getUri());

        assertCreateFail(clientRep, 400, "Client redirect URIs does not match redirect URIs fetched from the Sector Identifier URI.");
    }

    @Test
    public void createPairwiseClientWithInvalidSectorIdentifierURI() {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");
        clientRep.setSectorIdentifierUri("malformed");
        assertCreateFail(clientRep, 400, "Invalid Sector Identifier URI.");
    }

    @Test
    public void createPairwiseClientWithUnreachableSectorIdentifierURI() {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");
        clientRep.setSectorIdentifierUri("http://localhost/dummy");
        assertCreateFail(clientRep, 400, "Failed to get redirect URIs from the Sector Identifier URI.");
    }

    @Test
    public void loginUserToPairwiseClient() throws Exception {
        // Create public client
        OIDCClientRepresentation publicClient = create();

        // Login to public client
        oauth.client(publicClient.getClientId(), publicClient.getClientSecret());
        AuthorizationEndpointResponse loginResponse = oauth.doLogin(managedUser.getEmail(), managedUser.getPassword());
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(loginResponse.getCode());
        AccessToken accessToken = oauth.verifyToken(accessTokenResponse.getAccessToken());
        Assertions.assertEquals(managedUser.getUsername(), accessToken.getPreferredUsername());
        Assertions.assertEquals(managedUser.getEmail(), accessToken.getEmail());
        String tokenUserId = accessToken.getSubject();

        // Assert public client has same subject like userId
        UserRepresentation user = managedRealm.admin().users().search(managedUser.getUsername(), 0, 1).get(0);
        Assertions.assertEquals(user.getId(), tokenUserId);

        // Create pairwise client
        OIDCClientRepresentation pairwiseClient = createPairwise();
        Assertions.assertEquals("pairwise", pairwiseClient.getSubjectType());
        // Login to pairwise client
        oauth.client(pairwiseClient.getClientId(), pairwiseClient.getClientSecret());
        oauth.openLoginForm();
        loginResponse = oauth.parseLoginResponse();
        accessTokenResponse = oauth.doAccessTokenRequest(loginResponse.getCode());

        // Assert token payloads don't contain more than one "sub"
        String accessTokenPayload = getPayload(accessTokenResponse.getAccessToken());
        Assertions.assertEquals(1, StringUtils.countMatches(accessTokenPayload, "\"sub\""));
        String idTokenPayload = getPayload(accessTokenResponse.getIdToken());
        Assertions.assertEquals(1, StringUtils.countMatches(idTokenPayload, "\"sub\""));
        String refreshTokenPayload = getPayload(accessTokenResponse.getRefreshToken());
        Assertions.assertEquals(1, StringUtils.countMatches(refreshTokenPayload, "\"sub\""));

        accessToken = oauth.verifyToken(accessTokenResponse.getAccessToken());
        Assertions.assertEquals(managedUser.getUsername(), accessToken.getPreferredUsername());
        Assertions.assertEquals(managedUser.getEmail(), accessToken.getEmail());

        // Assert pairwise client has different subject than userId
        String pairwiseUserId = accessToken.getSubject();
        Assertions.assertNotEquals(pairwiseUserId, user.getId());

        // Check that userInfo contains pairwise subjectId as well
        UserInfoResponse userInfoResponse = oauth.doUserInfoRequest(accessTokenResponse.getAccessToken());
        Assertions.assertEquals(200, userInfoResponse.getStatusCode());
        UserInfo userInfo = userInfoResponse.getUserInfo();
        Assertions.assertEquals(managedUser.getUsername(), userInfo.getPreferredUsername());
        Assertions.assertEquals(managedUser.getEmail(), userInfo.getEmail());
        Assertions.assertEquals(pairwiseUserId, userInfo.getSubject());
    }

    @Test
    public void refreshPairwiseToken() throws Exception {
        // Create pairwise client
        OIDCClientRepresentation pairwiseClient = createPairwise();
        // Login to pairwise client
        AccessTokenResponse accessTokenResponse = login(pairwiseClient, managedUser.getEmail(),managedUser.getPassword());

        // Verify tokens
        oauth.verifyToken(accessTokenResponse.getAccessToken());
        IDToken idToken = oauth.verifyIDToken(accessTokenResponse.getIdToken());
        oauth.parseRefreshToken(accessTokenResponse.getRefreshToken());

        // Refresh token
        AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());

        // Verify refreshed tokens
        oauth.verifyToken(refreshTokenResponse.getAccessToken());
        oauth.parseRefreshToken(refreshTokenResponse.getRefreshToken());
        IDToken refreshedIdToken = oauth.verifyIDToken(refreshTokenResponse.getIdToken());

        // If an ID Token is returned as a result of a token refresh request, the following requirements apply:
        // its iss Claim Value MUST be the same as in the ID Token issued when the original authentication occurred
        Assertions.assertEquals(idToken.getIssuer(), refreshedIdToken.getIssuer());

        // its sub Claim Value MUST be the same as in the ID Token issued when the original authentication occurred
        Assertions.assertEquals(idToken.getSubject(), refreshedIdToken.getSubject());

        // its iat Claim MUST represent the time that the new ID Token is issued
        Assertions.assertTrue(refreshedIdToken.getIat() >= idToken.getIat());

        // if the ID Token contains an auth_time Claim, its value MUST represent the time of the original authentication
        // - not the time that the new ID token is issued
        Assertions.assertEquals(idToken.getAuth_time(), refreshedIdToken.getAuth_time());

        // its azp Claim Value MUST be the same as in the ID Token issued when the original authentication occurred; if
        // no azp Claim was present in the original ID Token, one MUST NOT be present in the new ID Token
        Assertions.assertEquals(idToken.getIssuedFor(), refreshedIdToken.getIssuedFor());
    }

    @Test
    public void introspectPairwiseAccessToken() throws Exception {
        // Create a pairwise client
        OIDCClientRepresentation pairwiseClient = createPairwise();

        // Login to pairwise client
        AccessTokenResponse accessTokenResponse = login(pairwiseClient, managedUser.getEmail(), managedUser.getPassword());

        JsonNode jsonNode = oauth.client(pairwiseClient.getClientId(), pairwiseClient.getClientSecret()).doIntrospectionAccessTokenRequest(accessTokenResponse.getAccessToken()).asJsonNode();
        Assertions.assertEquals(true, jsonNode.get("active").asBoolean());
        Assertions.assertEquals(managedUser.getEmail(), jsonNode.get("email").asText());
    }

    @Test
    public void refreshPairwiseTokenDeletedUser() throws Exception {
        String userId = createUser(REALM_NAME, "delete-me@localhost", "password");

        // Create pairwise client
        OIDCClientRepresentation pairwiseClient = createPairwise();

        // Login to pairwise client
        oauth.client(pairwiseClient.getClientId(), pairwiseClient.getClientSecret());
        AuthorizationEndpointResponse loginResponse = oauth.doLogin("delete-me@localhost", "password");
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(loginResponse.getCode());

        assertEquals(200, accessTokenResponse.getStatusCode());

        // Delete user
        try (Response response = adminClient.realm(REALM_NAME).users().delete(userId)) {
            Assertions.assertEquals(204, response.getStatus());
        }

        AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
        assertEquals(400, refreshTokenResponse.getStatusCode());
        assertEquals("invalid_grant", refreshTokenResponse.getError());
        assertNull(refreshTokenResponse.getAccessToken());
        assertNull(refreshTokenResponse.getIdToken());
        assertNull(refreshTokenResponse.getRefreshToken());
    }

    @Test
    public void refreshPairwiseTokenDisabledUser() throws Exception {
        String userId = createUser(REALM_NAME, "disable-me@localhost", "password");

        // Create pairwise client
        OIDCClientRepresentation pairwiseClient = createPairwise();

        // Login to pairwise client
        oauth.client(pairwiseClient.getClientId(), pairwiseClient.getClientSecret());
        AuthorizationEndpointResponse loginResponse = oauth.doLogin("disable-me@localhost", "password");
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(loginResponse.getCode());
        assertEquals(200, accessTokenResponse.getStatusCode());

        UserRepresentation userRep = managedRealm.admin().users().get(userId).toRepresentation();
        userRep.setEnabled(false);
        managedRealm.admin().users().get(userId).update(userRep);

            AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
            assertEquals(400, refreshTokenResponse.getStatusCode());
            assertEquals("invalid_grant", refreshTokenResponse.getError());
            assertNull(refreshTokenResponse.getAccessToken());
            assertNull(refreshTokenResponse.getIdToken());
            assertNull(refreshTokenResponse.getRefreshToken());
    }

    private AccessTokenResponse login(OIDCClientRepresentation client, String username, String password) {
        oauth.client(client.getClientId(), client.getClientSecret());
        AuthorizationEndpointResponse loginResponse = oauth.doLogin(username, password);
        return oauth.doAccessTokenRequest(loginResponse.getCode());
    }

    private String getPayload(String token) {
        String payloadBase64 = token.split("\\.")[1];
        return new String(Base64.getUrlDecoder().decode(payloadBase64), StandardCharsets.UTF_8);
    }

}
