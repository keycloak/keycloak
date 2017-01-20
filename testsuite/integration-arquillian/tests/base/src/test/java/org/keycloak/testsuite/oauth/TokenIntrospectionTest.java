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
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
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
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import java.util.ArrayList;
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
        assertTrue(jsonNode.has("nbf"));
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
        assertEquals(jsonNode.get("nbf").asInt(), rep.getNotBefore());
        assertEquals(jsonNode.get("sub").asText(), rep.getSubject());
        assertEquals(jsonNode.get("aud").asText(), rep.getAudience()[0]);
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
        String tokenResponse = oauth.introspectRefreshTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);

        assertTrue(jsonNode.get("active").asBoolean());
        assertEquals(sessionId, jsonNode.get("session_state").asText());
        assertEquals("test-app", jsonNode.get("client_id").asText());
        assertTrue(jsonNode.has("exp"));
        assertTrue(jsonNode.has("iat"));
        assertTrue(jsonNode.has("nbf"));
        assertTrue(jsonNode.has("sub"));
        assertTrue(jsonNode.has("aud"));
        assertTrue(jsonNode.has("iss"));
        assertTrue(jsonNode.has("jti"));

        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);

        assertTrue(rep.isActive());
        assertEquals("test-app", rep.getClientId());
        assertEquals(jsonNode.get("session_state").asText(), rep.getSessionState());
        assertEquals(jsonNode.get("exp").asInt(), rep.getExpiration());
        assertEquals(jsonNode.get("iat").asInt(), rep.getIssuedAt());
        assertEquals(jsonNode.get("nbf").asInt(), rep.getNotBefore());
        assertEquals(jsonNode.get("iss").asText(), rep.getIssuer());
        assertEquals(jsonNode.get("jti").asText(), rep.getId());
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
}
