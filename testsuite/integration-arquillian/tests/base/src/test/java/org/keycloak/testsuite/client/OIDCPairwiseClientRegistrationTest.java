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

package org.keycloak.testsuite.client;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.client.registration.HttpErrorException;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.UserManager;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class OIDCPairwiseClientRegistrationTest extends AbstractClientRegistrationTest {

    @Before
    public void before() throws Exception {
        super.before();

        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    private OIDCClientRepresentation createRep() {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("RegistrationAccessTokenTest");
        client.setClientUri(OAuthClient.APP_ROOT);
        client.setRedirectUris(Collections.singletonList(oauth.getRedirectUri()));
        return client;
    }

    public OIDCClientRepresentation create() throws ClientRegistrationException {
        OIDCClientRepresentation client = createRep();

        OIDCClientRepresentation response = reg.oidc().create(client);

        return response;
    }

    public OIDCClientRepresentation createPairwise() throws ClientRegistrationException {
        // Create pairwise client
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");
        OIDCClientRepresentation pairwiseClient = reg.oidc().create(clientRep);
        return pairwiseClient;
    }


    private void assertCreateFail(OIDCClientRepresentation client, int expectedStatusCode, String expectedErrorContains) {
        try {
            reg.oidc().create(client);
            Assert.fail("Not expected to successfuly register client");
        } catch (ClientRegistrationException expected) {
            HttpErrorException httpEx = (HttpErrorException) expected.getCause();
            Assert.assertEquals(expectedStatusCode, httpEx.getStatusLine().getStatusCode());
            if (expectedErrorContains != null) {
                assertTrue("Error response doesn't contain expected text", httpEx.getErrorResponse().contains(expectedErrorContains));
            }
        }
    }

    @Test
    public void createPairwiseClient() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals("pairwise", response.getSubjectType());
    }

    @Test
    public void updateClientToPairwise() throws Exception {
        OIDCClientRepresentation response = create();
        Assert.assertEquals("public", response.getSubjectType());

        reg.auth(Auth.token(response));
        response.setSubjectType("pairwise");
        OIDCClientRepresentation updated = reg.oidc().update(response);

        Assert.assertEquals("pairwise", updated.getSubjectType());
    }

    @Test
    public void updateSectorIdentifierUri() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals("pairwise", response.getSubjectType());
        Assert.assertNull(response.getSectorIdentifierUri());

        reg.auth(Auth.token(response));

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.addAll(response.getRedirectUris());
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setSectorIdentifierRedirectUris(sectorRedirects);

        response.setSectorIdentifierUri(TestApplicationResourceUrls.pairwiseSectorIdentifierUri());

        OIDCClientRepresentation updated = reg.oidc().update(response);

        Assert.assertEquals("pairwise", updated.getSubjectType());
        Assert.assertEquals(TestApplicationResourceUrls.pairwiseSectorIdentifierUri(), updated.getSectorIdentifierUri());

    }

    @Test
    public void updateToPairwiseThroughAdminRESTSuccess() throws Exception {
        OIDCClientRepresentation response = create();
        Assert.assertEquals("public", response.getSubjectType());
        Assert.assertNull(response.getSectorIdentifierUri());

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.addAll(response.getRedirectUris());
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setSectorIdentifierRedirectUris(sectorRedirects);

        String sectorIdentifierUri = TestApplicationResourceUrls.pairwiseSectorIdentifierUri();

        // Add protocolMapper through admin REST endpoint
        String clientId = response.getClientId();
        ProtocolMapperRepresentation pairwiseProtMapper = SHA256PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri, null);
        RealmResource realmResource = realmsResouce().realm("test");
        ClientManager.realm(realmResource).clientId(clientId).addProtocolMapper(pairwiseProtMapper);

        reg.auth(Auth.token(response));
        OIDCClientRepresentation rep = reg.oidc().get(response.getClientId());
        Assert.assertEquals("pairwise", rep.getSubjectType());
        Assert.assertEquals(sectorIdentifierUri, rep.getSectorIdentifierUri());

    }

    @Test
    public void updateToPairwiseThroughAdminRESTFailure() throws Exception {
        OIDCClientRepresentation response = create();
        Assert.assertEquals("public", response.getSubjectType());
        Assert.assertNull(response.getSectorIdentifierUri());

        // Push empty list to the sector identifier URI
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setSectorIdentifierRedirectUris(new ArrayList<>());

        String sectorIdentifierUri = TestApplicationResourceUrls.pairwiseSectorIdentifierUri();

        // Add protocolMapper through admin REST endpoint
        String clientId = response.getClientId();
        ProtocolMapperRepresentation pairwiseProtMapper = SHA256PairwiseSubMapper.createPairwiseMapper(sectorIdentifierUri, null);
        RealmResource realmResource = realmsResouce().realm("test");
        ClientResource clientResource = ApiUtil.findClientByClientId(realmsResouce().realm("test"), clientId);
        Response resp = clientResource.getProtocolMappers().createMapper(pairwiseProtMapper);
        Assert.assertEquals(400, resp.getStatus());

        // Assert still public
        reg.auth(Auth.token(response));
        OIDCClientRepresentation rep = reg.oidc().get(response.getClientId());
        Assert.assertEquals("public", rep.getSubjectType());
        Assert.assertNull(rep.getSectorIdentifierUri());
    }

    @Test
    public void createPairwiseClientWithSectorIdentifierURI() throws Exception {
        OIDCClientRepresentation clientRep = createRep();

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.addAll(clientRep.getRedirectUris());
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setSectorIdentifierRedirectUris(sectorRedirects);

        clientRep.setSubjectType("pairwise");
        clientRep.setSectorIdentifierUri(TestApplicationResourceUrls.pairwiseSectorIdentifierUri());

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals("pairwise", response.getSubjectType());
        Assert.assertEquals(TestApplicationResourceUrls.pairwiseSectorIdentifierUri(), response.getSectorIdentifierUri());
    }

    @Test
    public void createPairwiseClientWithRedirectsToMultipleHostsWithoutSectorIdentifierURI() throws Exception {
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
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setSectorIdentifierRedirectUris(redirects);

        clientRep.setSubjectType("pairwise");
        clientRep.setSectorIdentifierUri(TestApplicationResourceUrls.pairwiseSectorIdentifierUri());
        clientRep.setRedirectUris(redirects);

        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        Assert.assertEquals("pairwise", response.getSubjectType());
        Assert.assertEquals(TestApplicationResourceUrls.pairwiseSectorIdentifierUri(), response.getSectorIdentifierUri());
        Assert.assertNames(response.getRedirectUris(), "http://redirect1", "http://redirect2");
    }

    @Test
    public void createPairwiseClientWithSectorIdentifierURIContainingMismatchedRedirects() throws Exception {
        OIDCClientRepresentation clientRep = createRep();

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.add("http://someotherredirect");
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setSectorIdentifierRedirectUris(sectorRedirects);

        clientRep.setSubjectType("pairwise");
        clientRep.setSectorIdentifierUri(TestApplicationResourceUrls.pairwiseSectorIdentifierUri());

        assertCreateFail(clientRep, 400, "Client redirect URIs does not match redirect URIs fetched from the Sector Identifier URI.");
    }

    @Test
    public void createPairwiseClientWithSectorIdentifierURIContainingMismatchedRedirectsPublicSubject() throws Exception {
        OIDCClientRepresentation clientRep = createRep();

        // Push redirect uris to the sector identifier URI
        List<String> sectorRedirects = new ArrayList<>();
        sectorRedirects.add("http://someotherredirect");
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.setSectorIdentifierRedirectUris(sectorRedirects);

        clientRep.setSubjectType("public");
        clientRep.setSectorIdentifierUri(TestApplicationResourceUrls.pairwiseSectorIdentifierUri());

        assertCreateFail(clientRep, 400, "Client redirect URIs does not match redirect URIs fetched from the Sector Identifier URI.");
    }

    @Test
    public void createPairwiseClientWithInvalidSectorIdentifierURI() throws Exception {
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");
        clientRep.setSectorIdentifierUri("malformed");
        assertCreateFail(clientRep, 400, "Invalid Sector Identifier URI.");
    }

    @Test
    public void createPairwiseClientWithUnreachableSectorIdentifierURI() throws Exception {
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
        oauth.clientId(publicClient.getClientId());
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin("test-user@localhost", "password");
        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(loginResponse.getCode(), publicClient.getClientSecret());
        AccessToken accessToken = oauth.verifyToken(accessTokenResponse.getAccessToken());
        Assert.assertEquals("test-user", accessToken.getPreferredUsername());
        Assert.assertEquals("test-user@localhost", accessToken.getEmail());
        String tokenUserId = accessToken.getSubject();

        // Assert public client has same subject like userId
        UserRepresentation user = realmsResouce().realm("test").users().search("test-user", 0, 1).get(0);
        Assert.assertEquals(user.getId(), tokenUserId);

        // Create pairwise client
        OIDCClientRepresentation clientRep = createRep();
        clientRep.setSubjectType("pairwise");
        OIDCClientRepresentation pairwiseClient = reg.oidc().create(clientRep);
        Assert.assertEquals("pairwise", pairwiseClient.getSubjectType());

        // Login to pairwise client
        oauth.clientId(pairwiseClient.getClientId());
        oauth.openLoginForm();
        loginResponse = new OAuthClient.AuthorizationEndpointResponse(oauth);
        accessTokenResponse = oauth.doAccessTokenRequest(loginResponse.getCode(), pairwiseClient.getClientSecret());

        // Assert token payloads don't contain more than one "sub"
        String accessTokenPayload = getPayload(accessTokenResponse.getAccessToken());
        Assert.assertEquals(1, StringUtils.countMatches(accessTokenPayload, "\"sub\""));
        String idTokenPayload = getPayload(accessTokenResponse.getIdToken());
        Assert.assertEquals(1, StringUtils.countMatches(idTokenPayload, "\"sub\""));
        String refreshTokenPayload = getPayload(accessTokenResponse.getRefreshToken());
        Assert.assertEquals(1, StringUtils.countMatches(refreshTokenPayload, "\"sub\""));

        accessToken = oauth.verifyToken(accessTokenResponse.getAccessToken());
        Assert.assertEquals("test-user", accessToken.getPreferredUsername());
        Assert.assertEquals("test-user@localhost", accessToken.getEmail());

        // Assert pairwise client has different subject than userId
        String pairwiseUserId = accessToken.getSubject();
        Assert.assertNotEquals(pairwiseUserId, user.getId());

        // Send request to userInfo endpoint
        Client jaxrsClient = AdminClientUtil.createResteasyClient();
        try {
            // Check that userInfo contains pairwise subjectId as well
            Response userInfoResponse = UserInfoClientUtil.executeUserInfoRequest_getMethod(jaxrsClient, accessTokenResponse.getAccessToken());
            UserInfo userInfo = UserInfoClientUtil.testSuccessfulUserInfoResponse(userInfoResponse, "test-user", "test-user@localhost");
            String userInfoSubId = userInfo.getSubject();
            Assert.assertEquals(pairwiseUserId, userInfoSubId);
        } finally {
            jaxrsClient.close();
        }
    }

    @Test
    public void refreshPairwiseToken() throws Exception {
        // Create pairwise client
        OIDCClientRepresentation pairwiseClient = createPairwise();

        // Login to pairwise client
        OAuthClient.AccessTokenResponse accessTokenResponse = login(pairwiseClient, "test-user@localhost", "password");

        // Verify tokens
        oauth.parseRefreshToken(accessTokenResponse.getAccessToken());
        IDToken idToken = oauth.verifyIDToken(accessTokenResponse.getIdToken());
        oauth.parseRefreshToken(accessTokenResponse.getRefreshToken());

        // Refresh token
        OAuthClient.AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken(), pairwiseClient.getClientSecret());

        // Verify refreshed tokens
        oauth.verifyToken(refreshTokenResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshTokenResponse.getRefreshToken());
        IDToken refreshedIdToken = oauth.verifyIDToken(refreshTokenResponse.getIdToken());

        // If an ID Token is returned as a result of a token refresh request, the following requirements apply:
        // its iss Claim Value MUST be the same as in the ID Token issued when the original authentication occurred
        Assert.assertEquals(idToken.getIssuer(), refreshedRefreshToken.getIssuer());

        // its sub Claim Value MUST be the same as in the ID Token issued when the original authentication occurred
        Assert.assertEquals(idToken.getSubject(), refreshedRefreshToken.getSubject());

        // its iat Claim MUST represent the time that the new ID Token is issued
        Assert.assertEquals(refreshedIdToken.getIssuedAt(), refreshedRefreshToken.getIssuedAt());

        // if the ID Token contains an auth_time Claim, its value MUST represent the time of the original authentication
        // - not the time that the new ID token is issued
        Assert.assertEquals(idToken.getAuthTime(), refreshedIdToken.getAuthTime());

        // its azp Claim Value MUST be the same as in the ID Token issued when the original authentication occurred; if
        // no azp Claim was present in the original ID Token, one MUST NOT be present in the new ID Token
        Assert.assertEquals(idToken.getIssuedFor(), refreshedIdToken.getIssuedFor());
    }

    @Test
    public void introspectPairwiseAccessToken() throws Exception {
        // Create a pairwise client
        OIDCClientRepresentation pairwiseClient = createPairwise();

        // Login to pairwise client
        OAuthClient.AccessTokenResponse accessTokenResponse = login(pairwiseClient, "test-user@localhost", "password");

        String introspectionResponse = oauth.introspectAccessTokenWithClientCredential(pairwiseClient.getClientId(), pairwiseClient.getClientSecret(), accessTokenResponse.getAccessToken());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(introspectionResponse);
        Assert.assertEquals(true, jsonNode.get("active").asBoolean());
        Assert.assertEquals("test-user@localhost", jsonNode.get("email").asText());
    }

    @Test
    public void refreshPairwiseTokenDeletedUser() throws Exception {
        String userId = createUser(REALM_NAME, "delete-me@localhost", "password");

        // Create pairwise client
        OIDCClientRepresentation pairwiseClient = createPairwise();

        // Login to pairwise client
        oauth.clientId(pairwiseClient.getClientId());
        oauth.clientId(pairwiseClient.getClientId());
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin("delete-me@localhost", "password");
        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(loginResponse.getCode(), pairwiseClient.getClientSecret());

        assertEquals(200, accessTokenResponse.getStatusCode());

        // Delete user
        adminClient.realm(REALM_NAME).users().delete(userId);

        OAuthClient.AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken(), pairwiseClient.getClientSecret());
        assertEquals(400, refreshTokenResponse.getStatusCode());
        assertEquals("invalid_grant", refreshTokenResponse.getError());
        assertNull(refreshTokenResponse.getAccessToken());
        assertNull(refreshTokenResponse.getIdToken());
        assertNull(refreshTokenResponse.getRefreshToken());
    }

    @Test
    public void refreshPairwiseTokenDisabledUser() throws Exception {
        createUser(REALM_NAME, "disable-me@localhost", "password");

        // Create pairwise client
        OIDCClientRepresentation pairwiseClient = createPairwise();

        // Login to pairwise client
        oauth.clientId(pairwiseClient.getClientId());
        oauth.clientId(pairwiseClient.getClientId());
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin("disable-me@localhost", "password");
        OAuthClient.AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(loginResponse.getCode(), pairwiseClient.getClientSecret());
        assertEquals(200, accessTokenResponse.getStatusCode());

        try {
            UserManager.realm(adminClient.realm(REALM_NAME)).username("disable-me@localhost").enabled(false);

            OAuthClient.AccessTokenResponse refreshTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken(), pairwiseClient.getClientSecret());
            assertEquals(400, refreshTokenResponse.getStatusCode());
            assertEquals("invalid_grant", refreshTokenResponse.getError());
            assertNull(refreshTokenResponse.getAccessToken());
            assertNull(refreshTokenResponse.getIdToken());
            assertNull(refreshTokenResponse.getRefreshToken());
        } finally {
            UserManager.realm(adminClient.realm(REALM_NAME)).username("disable-me@localhost").enabled(true);
        }
    }

    private OAuthClient.AccessTokenResponse login(OIDCClientRepresentation client, String username, String password) {
        oauth.clientId(client.getClientId());
        OAuthClient.AuthorizationEndpointResponse loginResponse = oauth.doLogin(username, password);
        return oauth.doAccessTokenRequest(loginResponse.getCode(), client.getClientSecret());
    }

    private String getPayload(String token) {
        String payloadBase64 = token.split("\\.")[1];
        return new String(Base64.getDecoder().decode(payloadBase64));
    }
}