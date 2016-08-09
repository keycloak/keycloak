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
import org.keycloak.testsuite.TestRealmKeycloakTest;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.OAuthClient.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 * @author Stan Silvert ssilvert@redhat.com (C) 2016 Red Hat Inc.
 */
public class TokenIntrospectionTest extends TestRealmKeycloakTest {

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
        String inactiveAccessToken = "eyJhbGciOiJSUzI1NiJ9.eyJub25jZSI6IjczMGZjNjQ1LTBlMDQtNDE3Yi04MDY0LTkyYWIyY2RjM2QwZSIsImp0aSI6ImU5ZGU1NjU2LWUzMjctNDkxNC1hNjBmLTI1MzJlYjBiNDk4OCIsImV4cCI6MTQ1MjI4MTAwMCwibmJmIjowLCJpYXQiOjE0NTIyODA3MDAsImlzcyI6Imh0dHA6Ly9sb2NhbGhvc3Q6ODA4MC9hdXRoL3JlYWxtcy9leGFtcGxlIiwiYXVkIjoianMtY29uc29sZSIsInN1YiI6IjFkNzQ0MDY5LWYyOTgtNGU3Yy1hNzNiLTU1YzlhZjgzYTY4NyIsInR5cCI6IkJlYXJlciIsImF6cCI6ImpzLWNvbnNvbGUiLCJzZXNzaW9uX3N0YXRlIjoiNzc2YTA0OTktODNjNC00MDhkLWE5YjctYTZiYzQ5YmQ3MThjIiwiY2xpZW50X3Nlc3Npb24iOiJjN2Y5ODczOC05MDhlLTQxOWYtYTdkNC1kODYxYjRhYTI3NjkiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsidXNlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJ2aWV3LXByb2ZpbGUiXX19LCJuYW1lIjoiU2FtcGxlIFVzZXIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ1c2VyIiwiZ2l2ZW5fbmFtZSI6IlNhbXBsZSIsImZhbWlseV9uYW1lIjoiVXNlciIsImVtYWlsIjoic2FtcGxlLXVzZXJAZXhhbXBsZSJ9.YyPV74j9CqOG2Jmq692ZZpqycjNpUgtYVRfQJccS_FU84tGVXoKKsXKYeY2UJ1Y_bPiYG1I1J6JSXC8XqgQijCG7Nh7oK0yN74JbRN58HG75fvg6K9BjR6hgJ8mHT8qPrCux2svFucIMIZ180eoBoRvRstkidOhl_mtjT_i31fU";
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
