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
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.events.Event;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.managers.ClientManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.OAuthClient;
import org.keycloak.testsuite.OAuthClient.AccessTokenResponse;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.rule.KeycloakRule;
import org.keycloak.testsuite.rule.WebResource;
import org.keycloak.testsuite.rule.WebRule;
import org.openqa.selenium.WebDriver;

import static org.junit.Assert.*;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class TokenIntrospectionTest {

    protected static Keycloak keycloak;

    @ClassRule
    public static KeycloakRule keycloakRule = new KeycloakRule(new KeycloakRule.KeycloakSetup() {

        @Override
        public void config(RealmManager manager, RealmModel adminstrationRealm, RealmModel appRealm) {
            appRealm.getClientByClientId("test-app").setDirectAccessGrantsEnabled(true);
            ClientModel confApp = KeycloakModelUtils.createClient(appRealm, "confidential-cli");
            confApp.setSecret("secret1");
            new ClientManager(manager).enableServiceAccount(confApp);
            ClientModel pubApp = KeycloakModelUtils.createClient(appRealm, "public-cli");
            pubApp.setPublicClient(true);
            {
                UserModel user = manager.getSession().users().addUser(appRealm, KeycloakModelUtils.generateId(), "no-permissions", false, false);
                user.updateCredential(UserCredentialModel.password("password"));
                user.setEnabled(true);
                RoleModel role = appRealm.getRole("user");
                user.grantRole(role);
            }

            keycloak = Keycloak.getInstance("http://localhost:8081/auth", "master", "admin", "admin", Constants.ADMIN_CLI_CLIENT_ID);
        }

    });

    @Rule
    public WebRule webRule = new WebRule(this);

    @WebResource
    protected WebDriver driver;

    @WebResource
    protected OAuthClient oauth;

    @WebResource
    protected LoginPage loginPage;

    @Rule
    public AssertEvents events = new AssertEvents(keycloakRule);

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

        events.clear();
    }

    @Test
    public void testInvalidClientCredentials() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "bad_credential", accessTokenResponse.getAccessToken());

        assertEquals("{\"error_description\":\"Authentication failed.\",\"error\":\"invalid_request\"}", tokenResponse);

        events.clear();
    }

    @Test
    public void testIntrospectRefreshToken() throws Exception {
        oauth.doLogin("test-user@localhost", "password");
        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        Event loginEvent = events.expectLogin().assertEvent();
        String sessionId = loginEvent.getSessionId();
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("confidential-cli", "secret1", accessTokenResponse.getAccessToken());
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

        events.clear();
    }

    @Test
    public void testPublicClientCredentialsNotAllowed() throws Exception {
        oauth.doLogin("test-user@localhost", "password");

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        AccessTokenResponse accessTokenResponse = oauth.doAccessTokenRequest(code, "password");
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential("public-cli", "it_doesnt_matter", accessTokenResponse.getAccessToken());

        assertEquals("{\"error_description\":\"Client not allowed.\",\"error\":\"invalid_request\"}", tokenResponse);

        events.clear();
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

        events.clear();
    }
}
