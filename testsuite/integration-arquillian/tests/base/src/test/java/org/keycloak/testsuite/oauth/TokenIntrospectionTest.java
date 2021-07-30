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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.TextNode;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.oidc.OIDCScopeTest;
import org.keycloak.testsuite.oidc.AbstractOIDCScopeTest;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.UriBuilder;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class TokenIntrospectionTest extends AbstractTestRealmKeycloakTest {

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        ClientRepresentation confApp = KeycloakModelUtils.createClient(testRealm, "confidential-cli");
        confApp.setSecret("secret1");
        confApp.setServiceAccountsEnabled(Boolean.TRUE);

        ClientRepresentation pubApp = KeycloakModelUtils.createClient(testRealm, "public-cli");
        pubApp.setPublicClient(Boolean.TRUE);

        ClientRepresentation samlApp = KeycloakModelUtils.createClient(testRealm, "saml-client");
        samlApp.setSecret("secret2");
        samlApp.setServiceAccountsEnabled(Boolean.TRUE);
        samlApp.setProtocol("saml");

        UserRepresentation user = new UserRepresentation();
        user.setUsername("no-permissions");
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType("password");
        credential.setValue("password");
        List<CredentialRepresentation> creds = new ArrayList<>();
        creds.add(credential);
        user.setCredentials(creds);
        user.setEnabled(Boolean.TRUE);
        List<String> realmRoles = new ArrayList<>();
        realmRoles.add("user");
        user.setRealmRoles(realmRoles);
        testRealm.getUsers().add(user);
    }

    @Test
    public void testConfidentialClientCredentialsBasicAuthentication() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);

        assertTrue(jsonNode.get("active").asBoolean());
        assertEquals("test-user@localhost", jsonNode.get("username").asText());
        assertEquals("test-app", jsonNode.get("client_id").asText());
        assertTrue(jsonNode.has("exp"));
        assertTrue(jsonNode.has("iat"));
        assertFalse(jsonNode.has("nbf"));
        assertTrue(jsonNode.has("sub"));
        assertTrue(jsonNode.has("aud"));
        assertTrue(jsonNode.has("iss"));
        assertTrue(jsonNode.has("jti"));

        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertTrue(rep.isActive());
        assertEquals("test-user@localhost", rep.getUserName());
        assertEquals("test-app", rep.getClientId());
        assertEquals(jsonNode.get("exp").asInt(), rep.getExpiration());
        assertEquals(jsonNode.get("iat").asInt(), rep.getIssuedAt());
        assertEquals(jsonNode.get("nbf"), rep.getNbf());
        assertEquals(jsonNode.get("sub").asText(), rep.getSubject());

        List<String> audiences = new ArrayList<>();

        // We have single audience in the token - hence it is simple string
        assertTrue(jsonNode.get("aud") instanceof TextNode);
        audiences.add(jsonNode.get("aud").asText());
        Assert.assertNames(audiences, rep.getAudience());

        assertEquals(jsonNode.get("iss").asText(), rep.getIssuer());
        assertEquals(jsonNode.get("jti").asText(), rep.getId());
    }

    @Test
    public void testInvalidClientCredentials() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "bad_credential", accessTokenResponse.getAccessToken());

        OAuth2ErrorRepresentation errorRep = JsonSerialization.readValue(tokenResponse, OAuth2ErrorRepresentation.class);
        Assert.assertEquals("Authentication failed.", errorRep.getErrorDescription());
        Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, errorRep.getError());
    }

    @Test
    public void testIntrospectRefreshToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String tokenResponse = oauth.introspectRefreshTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getRefreshToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);

        assertTrue(jsonNode.get("active").asBoolean());
        assertEquals(sessionId, jsonNode.get("session_state").asText());
        assertEquals("test-app", jsonNode.get("client_id").asText());
        assertTrue(jsonNode.has("exp"));
        assertTrue(jsonNode.has("iat"));
        assertFalse(jsonNode.has("nbf"));
        assertTrue(jsonNode.has("sub"));
        assertTrue(jsonNode.has("aud"));
        assertTrue(jsonNode.has("iss"));
        assertTrue(jsonNode.has("jti"));
        assertTrue(jsonNode.has("typ"));

        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertTrue(rep.isActive());
        assertEquals("test-app", rep.getClientId());
        assertEquals(jsonNode.get("session_state").asText(), rep.getSessionState());
        assertEquals(jsonNode.get("exp").asInt(), rep.getExpiration());
        assertEquals(jsonNode.get("iat").asInt(), rep.getIssuedAt());
        assertEquals(jsonNode.get("nbf"), rep.getNbf());
        assertEquals(jsonNode.get("iss").asText(), rep.getIssuer());
        assertEquals(jsonNode.get("jti").asText(), rep.getId());
        assertEquals(jsonNode.get("typ").asText(), "Refresh");
    }

    @Test
    public void testIntrospectRefreshTokenAfterUserSessionLogoutAndLoginAgain() throws Exception {
        AccessTokenResponse accessTokenResponse = loginAndForceNewLoginPage();
        String refreshToken1 = accessTokenResponse.getRefreshToken();

        oauth.doLogout(refreshToken1, "password");
        events.clear();

        setTimeOffset(2);

        oauth.fillLoginForm("test-user@localhost", "password");
        events.expectLogin().assertEvent();

        Assert.assertFalse(loginPage.isCurrent());

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse tokenResponse2 = oauth.doAccessTokenRequest(code, "password");

        String introspectResponse = oauth.introspectRefreshTokenWithClientCredential("confidential-cli", "secret1", tokenResponse2.getRefreshToken());

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(introspectResponse);
        assertTrue(jsonNode.get("active").asBoolean());

        introspectResponse = oauth.introspectRefreshTokenWithClientCredential("confidential-cli", "secret1", refreshToken1);

        jsonNode = objectMapper.readTree(introspectResponse);
        assertFalse(jsonNode.get("active").asBoolean());
    }

    @Test
    public void testPublicClientCredentialsNotAllowed() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("public-cli", "it_doesnt_matter", accessTokenResponse.getAccessToken());

        OAuth2ErrorRepresentation errorRep = JsonSerialization.readValue(tokenResponse, OAuth2ErrorRepresentation.class);
        Assert.assertEquals("Client not allowed.", errorRep.getErrorDescription());
        Assert.assertEquals(OAuthErrorException.INVALID_REQUEST, errorRep.getError());
    }

    @Test
    public void testInactiveAccessToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String inactiveAccessToken = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJGSjg2R2NGM2pUYk5MT2NvNE52WmtVQ0lVbWZZQ3FvcXRPUWVNZmJoTmxFIn0.eyJqdGkiOiI5NjgxZTRlOC01NzhlLTQ3M2ItOTIwNC0yZWE5OTdhYzMwMTgiLCJleHAiOjE0NzYxMDY4NDksIm5iZiI6MCwiaWF0IjoxNDc2MTA2NTQ5LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgxODAvYXV0aC9yZWFsbXMvdGVzdCIsImF1ZCI6InRlc3QtYXBwIiwic3ViIjoiZWYyYzk0NjAtZDRkYy00OTk5LWJlYmUtZWVmYWVkNmJmMGU3IiwidHlwIjoiQmVhcmVyIiwiYXpwIjoidGVzdC1hcHAiLCJhdXRoX3RpbWUiOjE0NzYxMDY1NDksInNlc3Npb25fc3RhdGUiOiI1OGY4M2MzMi03MDhkLTQzNjktODhhNC05YjI5OGRjMDY5NzgiLCJhY3IiOiIxIiwiY2xpZW50X3Nlc3Npb24iOiI2NTYyOTVkZC1kZWNkLTQyZDAtYWJmYy0zZGJjZjJlMDE3NzIiLCJhbGxvd2VkLW9yaWdpbnMiOlsiaHR0cDovL2xvY2FsaG9zdDo4MTgwIl0sInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ1c2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsidGVzdC1hcHAiOnsicm9sZXMiOlsiY3VzdG9tZXItdXNlciJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiJUb20gQnJhZHkiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0LXVzZXJAbG9jYWxob3N0IiwiZ2l2ZW5fbmFtZSI6IlRvbSIsImZhbWlseV9uYW1lIjoiQnJhZHkiLCJlbWFpbCI6InRlc3QtdXNlckBsb2NhbGhvc3QifQ.LYU7opqZsc9e-ZmdsIhcecjHL3kQkpP13VpwO4MHMqEVNeJsZI1WOkTM5HGVAihcPfQazhaYvcik0gFTF_6ZcKzDqanjx80TGhSIrV5FoCeUrbp7w_66VKDH7ImPc8T2kICQGHh2d521WFBnvXNifw7P6AR1rGg4qrUljHdf_KU";
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", inactiveAccessToken);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);

        assertFalse(jsonNode.get("active").asBoolean());

        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertFalse(rep.isActive());
        assertNull(rep.getUserName());
        assertNull(rep.getClientId());
        assertNull(rep.getSubject());
    }

    @Test
    public void testUnsupportedToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String inactiveAccessToken = "unsupported";
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", inactiveAccessToken);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);

        assertFalse(jsonNode.get("active").asBoolean());

        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertFalse(rep.isActive());
        assertNull(rep.getUserName());
        assertNull(rep.getClientId());
        assertNull(rep.getSubject());
    }

    @Test
    public void testIntrospectAccessToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        EventRepresentation loginEvent = events.expectLogin().assertEvent();
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());
        TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertTrue(rep.isActive());
        assertEquals("test-user@localhost", rep.getUserName());
        assertEquals("test-app", rep.getClientId());
        assertEquals(loginEvent.getUserId(), rep.getSubject());

        // Assert expected scope
        AbstractOIDCScopeTest.assertScopes("openid email profile", rep.getScope());
    }

    @Test
    public void testIntrospectAccessTokenES256() throws Exception {
        testIntrospectAccessToken(Algorithm.ES256);
    }

    @Test
    public void testIntrospectAccessTokenPS256() throws Exception {
        testIntrospectAccessToken(Algorithm.PS256);
    }

    private void testIntrospectAccessToken(String jwaAlgorithm) throws Exception {
        try {
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), jwaAlgorithm);

            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            EventRepresentation loginEvent = events.expectLogin().assertEvent();
            AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");

            assertEquals(jwaAlgorithm, new JWSInput(accessTokenResponse.getAccessToken()).getHeader().getAlgorithm().name());

            String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());

            TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);

            assertTrue(rep.isActive());
            assertEquals("test-user@localhost", rep.getUserName());
            assertEquals("test-app", rep.getClientId());
            assertEquals(loginEvent.getUserId(), rep.getSubject());

            // Assert expected scope
            OIDCScopeTest.assertScopes("openid email profile", rep.getScope());
        } finally {
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "test-app"), Algorithm.RS256);
        }
    }

    @Test
    public void testIntrospectAccessTokenSessionInvalid() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        oauth.doLogout(accessTokenResponse.getRefreshToken(), "password");

        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());
        TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertFalse(rep.isActive());
        assertNull(rep.getUserName());
        assertNull(rep.getClientId());
        assertNull(rep.getSubject());
    }

    // KEYCLOAK-4829
    @Test
    public void testIntrospectAccessTokenOfflineAccess() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");

        setTimeOffset(86400);

        // "Online" session still exists, but is invalid
        accessTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken(), "password");
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());
        TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertTrue(rep.isActive());
        assertEquals("test-user@localhost", rep.getUserName());
        assertEquals("test-app", rep.getClientId());

        // "Online" session doesn't even exists
        testingClient.testing().removeExpired("test");

        accessTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken(), "password");
        tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());
        rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertTrue(rep.isActive());
        assertEquals("test-user@localhost", rep.getUserName());
        assertEquals("test-app", rep.getClientId());
    }


    @Test
    public void testIntrospectAccessTokenUserDisabled() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");

        EventRepresentation loginEvent = events.expectLogin().assertEvent();

        UserRepresentation userRep = new UserRepresentation();
        try {
            userRep.setEnabled(false);
            adminClient.realm(oauth.getRealm()).users().get(loginEvent.getUserId()).update(userRep);

            String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());
            TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);

            assertFalse(rep.isActive());
            assertNull(rep.getUserName());
            assertNull(rep.getClientId());
            assertNull(rep.getSubject());
        } finally {
            userRep.setEnabled(true);
            adminClient.realm(oauth.getRealm()).users().get(loginEvent.getUserId()).update(userRep);
        }
    }

    @Test
    public void testIntrospectAccessTokenExpired() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");

        setTimeOffset(adminClient.realm(oauth.getRealm()).toRepresentation().getAccessTokenLifespan() + 1);
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());
        TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertFalse(rep.isActive());
        assertNull(rep.getUserName());
        assertNull(rep.getClientId());
        assertNull(rep.getSubject());
    }


    /**
     * Test covers the same scenario from different endpoints like TokenEndpoint and LogoutEndpoint.
     */
    @Test
    public void testIntrospectWithSamlClient() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        events.expectLogin().assertEvent();
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("saml-client", "secret2", accessTokenResponse.getAccessToken());
        TokenMetadataRepresentation rep = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertEquals(Errors.INVALID_CLIENT, rep.getOtherClaims().get("error"));
        assertNull(rep.getSubject());
    }

    private OAuthClient.AccessTokenResponse loginAndForceNewLoginPage() {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.clientSessionState("client-session");

        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, "password");

        setTimeOffset(1);

        String loginFormUri = UriBuilder.fromUri(oauth.getLoginFormUrl())
                .queryParam(OIDCLoginProtocol.PROMPT_PARAM, OIDCLoginProtocol.PROMPT_VALUE_LOGIN)
                .build().toString();
        driver.navigate().to(loginFormUri);

        loginPage.assertCurrent();

        return tokenResponse;
    }

    // KEYCLOAK-17259
    @Test
    public void testIntrospectionRequestParamsMoreThanOnce() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");

        accessTokenResponse = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken(), "password");
        String tokenResponse = introspectAccessTokenWithDuplicateParams("confidential-cli", "secret1", accessTokenResponse.getAccessToken());

        OAuth2ErrorRepresentation errorRep = JsonSerialization.readValue(tokenResponse, OAuth2ErrorRepresentation.class);
        assertEquals("duplicated parameter", errorRep.getErrorDescription());
        assertEquals(OAuthErrorException.INVALID_REQUEST, errorRep.getError());
    }

    @Test
    public void testIntrospectRevokeRefreshToken() throws Exception {
        RealmRepresentation realm = adminClient.realm(oauth.getRealm()).toRepresentation();
        realm.setRevokeRefreshToken(true);
        adminClient.realm(oauth.getRealm()).update(realm);
        try {
            JsonNode jsonNode = introspectRevokedToken();
            assertFalse(jsonNode.get("active").asBoolean());
        } finally {
            realm.setRevokeRefreshToken(false);
            adminClient.realm(oauth.getRealm()).update(realm);
        }
    }

    @Test
    public void testIntrospectRevokeOfflineToken() throws Exception {
        RealmRepresentation realm = adminClient.realm(oauth.getRealm()).toRepresentation();
        realm.setRevokeRefreshToken(true);
        adminClient.realm(oauth.getRealm()).update(realm);
        try {
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
            JsonNode jsonNode = introspectRevokedToken();
            assertFalse(jsonNode.get("active").asBoolean());
        } finally {
            realm.setRevokeRefreshToken(false);
            adminClient.realm(oauth.getRealm()).update(realm);
        }
    }

    @Test
    public void testIntrospectRefreshTokenAfterRefreshTokenRequest() throws Exception {
        RealmRepresentation realm = adminClient.realm(oauth.getRealm()).toRepresentation();
        realm.setRevokeRefreshToken(true);
        realm.setRefreshTokenMaxReuse(1);
        adminClient.realm(oauth.getRealm()).update(realm);
        try {
            oauth.doLogin("test-user@localhost", "password");
            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
            String oldRefreshToken = accessTokenResponse.getRefreshToken();

            setTimeOffset(1);

            accessTokenResponse = oauth.doRefreshTokenRequest(oldRefreshToken, "password");

            accessTokenResponse = oauth.doRefreshTokenRequest(oldRefreshToken, "password");
            String newRefreshToken = accessTokenResponse.getRefreshToken();
            String tokenResponse = oauth.introspectRefreshTokenWithClientCredential("confidential-cli", "secret1",
                newRefreshToken);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(tokenResponse);
            assertTrue(jsonNode.get("active").asBoolean());

            accessTokenResponse = oauth.doRefreshTokenRequest(newRefreshToken, "password");
            tokenResponse = oauth.introspectRefreshTokenWithClientCredential("confidential-cli", "secret1", oldRefreshToken);
            jsonNode = objectMapper.readTree(tokenResponse);
            assertFalse(jsonNode.get("active").asBoolean());
        } finally {
            realm.setRevokeRefreshToken(false);
            realm.setRefreshTokenMaxReuse(0);
            adminClient.realm(oauth.getRealm()).update(realm);
        }
    }

    private String introspectAccessTokenWithDuplicateParams(String clientId, String clientSecret, String tokenToIntrospect) {
        HttpPost post = new HttpPost(oauth.getTokenIntrospectionUrl());

        String authorization = BasicAuthHelper.createHeader(clientId, clientSecret);
        post.setHeader("Authorization", authorization);

        List<NameValuePair> parameters = new LinkedList<>();

        parameters.add(new BasicNameValuePair("token", tokenToIntrospect));
        parameters.add(new BasicNameValuePair("token", "foo"));
        parameters.add(new BasicNameValuePair("token_type_hint", "access_token"));

        UrlEncodedFormEntity formEntity;

        try {
            formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }

        post.setEntity(formEntity);

        try (CloseableHttpResponse response = HttpClientBuilder.create().build().execute(post)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            response.getEntity().writeTo(out);
            return new String(out.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("Failed to retrieve access token", e);
        }
    }

    private JsonNode introspectRevokedToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String stringRefreshToken = accessTokenResponse.getRefreshToken();

        accessTokenResponse = oauth.doRefreshTokenRequest(stringRefreshToken, "password");

        String tokenResponse = oauth.introspectRefreshTokenWithClientCredential("confidential-cli", "secret1",
            stringRefreshToken);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.readTree(tokenResponse);
    }
}
