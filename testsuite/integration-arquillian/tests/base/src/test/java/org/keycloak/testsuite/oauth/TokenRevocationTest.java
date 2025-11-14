/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.http.simple.SimpleHttpRequest;
import org.keycloak.representations.idm.OAuth2ErrorRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.pages.LoginPage;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.UserInfoClientUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.OAuthClient;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.AssertEvents.isTokenId;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:yoshiyuki.tabata.jy@hitachi.com">Yoshiyuki Tabata</a>
 */
public class TokenRevocationTest extends AbstractKeycloakTest {

    private RealmResource realm;

    private Client userInfoClient;
    private CloseableHttpClient restHttpClient;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realmRepresentation = loadJson(getClass().getResourceAsStream("/testrealm.json"),
            RealmRepresentation.class);
        RealmBuilder realm = RealmBuilder.edit(realmRepresentation).testEventListener();

        testRealms.add(realm.build());
    }

    @Before
    public void beforeTest() {
        // Create client configuration
        realm = adminClient.realm("test");
        ClientManager.realm(realm).clientId("test-app").directAccessGrant(true);
        ClientManager.realm(realm).clientId("test-app-scope").directAccessGrant(true);

        // Create clients
        userInfoClient = AdminClientUtil.createResteasyClient();
        restHttpClient = HttpClientBuilder.create().build();
    }

    @After
    public void afterTest() throws IOException {
        userInfoClient.close();
        restHttpClient.close();
    }

    @Page
    protected LoginPage loginPage;

    @Test
    public void testRevokeToken() throws Exception {
        AccessTokenResponse tokenResponse1 = login("test-app", "test-user@localhost", "password");
        AccessTokenResponse tokenResponse2 = login("test-app-scope", "test-user@localhost", "password");

        UserResource testUser = realm.users().get(realm.users().search("test-user@localhost").get(0).getId());
        List<UserSessionRepresentation> userSessions = testUser.getUserSessions();
        assertEquals(1, userSessions.size());
        Map<String, String> clients = userSessions.get(0).getClients();
        assertEquals("test-app", clients.get(realm.clients().findByClientId("test-app").get(0).getId()));
        assertEquals("test-app-scope", clients.get(realm.clients().findByClientId("test-app-scope").get(0).getId()));

        isTokenEnabled(tokenResponse1, "test-app");
        isTokenEnabled(tokenResponse2, "test-app-scope");

        oauth.client("test-app", "password");
        assertTrue(oauth.tokenRevocationRequest(tokenResponse1.getRefreshToken()).refreshToken().send().isSuccess());

        userSessions = testUser.getUserSessions();
        assertEquals(1, userSessions.size());
        clients = userSessions.get(0).getClients();
        assertNull(clients.get(realm.clients().findByClientId("test-app").get(0).getId()));
        assertEquals("test-app-scope", clients.get(realm.clients().findByClientId("test-app-scope").get(0).getId()));

        isTokenDisabled(tokenResponse1, "test-app");
        isTokenEnabled(tokenResponse2, "test-app-scope");

        // Revoke second token and assert no sessions for testUser
        assertTrue(oauth.tokenRevocationRequest(tokenResponse2.getRefreshToken()).refreshToken().send().isSuccess());

        userSessions = testUser.getUserSessions();
        assertEquals(0, userSessions.size());

    }

    @Test
    public void testRevokeAccessToken() throws Exception {
        oauth.client("test-app", "password");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost",
            "password");

        isTokenEnabled(tokenResponse, "test-app");

        assertTrue(oauth.tokenRevocationRequest(tokenResponse.getAccessToken()).accessToken().send().isSuccess());

        isAccessTokenDisabled(tokenResponse.getAccessToken(), "test-app");
    }

    @Test
    public void testRevokedAccessTokenCacheLifespan() throws Exception {
        oauth.client("test-app", "password");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        isTokenEnabled(tokenResponse, "test-app");

        assertTrue(oauth.tokenRevocationRequest(tokenResponse.getAccessToken()).accessToken().send().isSuccess());

        setTimeOffset(adminClient.realm(oauth.getRealm()).toRepresentation().getAccessTokenLifespan());

        isAccessTokenDisabled(tokenResponse.getAccessToken(), "test-app");

        setTimeOffset(0);
    }

    @Test
    public void testRevokeOfflineToken() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        oauth.client("test-app", "password");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost",
            "password");

        isTokenEnabled(tokenResponse, "test-app");

        assertTrue(oauth.tokenRevocationRequest(tokenResponse.getRefreshToken()).refreshToken().send().isSuccess());

        isTokenDisabled(tokenResponse, "test-app");
    }

    @Test
    public void testRevokeOfflineTokenWithOnlineSSOSession() throws Exception {
        AccessTokenResponse tokenResponse1 = login("test-app", "test-user@localhost", "password");

        // Offline login of same client in same SSO session as previous login
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        AccessTokenResponse tokenResponse2 = login("test-app", "test-user@localhost", "password");

        // Session IDs of "offline" and online session are same for now. This may change in the future
        Assert.assertEquals(tokenResponse1.getSessionState(), tokenResponse2.getSessionState());

        isTokenEnabled(tokenResponse2, "test-app");

        // Disable both offline and refresh
        assertTrue(oauth.tokenRevocationRequest(tokenResponse2.getRefreshToken()).refreshToken().send().isSuccess());

        isTokenDisabled(tokenResponse2, "test-app");
    }

    @Test
    public void testTokenTypeHint() throws Exception {
        // different token_type_hint
        oauth.client("test-app", "password");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost",
            "password");

        isTokenEnabled(tokenResponse, "test-app");

        assertTrue(oauth.tokenRevocationRequest(tokenResponse.getRefreshToken()).accessToken().send().isSuccess());

        isTokenDisabled(tokenResponse, "test-app");

        // invalid token_type_hint
        oauth.client("test-app", "password");
        tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        isTokenEnabled(tokenResponse, "test-app");

        assertTrue(oauth.tokenRevocationRequest(tokenResponse.getRefreshToken()).tokenTypeHint("invalid_token_type_hint").send().isSuccess());

        isTokenDisabled(tokenResponse, "test-app");
    }

    @Test
    public void testRevokeTokenFromDifferentClient() throws Exception {
        oauth.client("test-app", "password");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost",
            "password");

        isTokenEnabled(tokenResponse, "test-app");

        oauth.client("test-app-scope", "password");
        assertEquals(400, oauth.tokenRevocationRequest(tokenResponse.getRefreshToken()).refreshToken().send().getStatusCode());

        isTokenEnabled(tokenResponse, "test-app");
    }

    @Test
    public void testRevokeAlreadyRevokedToken() throws Exception {
        oauth.client("test-app", "password");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost",
            "password");

        isTokenEnabled(tokenResponse, "test-app");

        oauth.doLogout(tokenResponse.getRefreshToken());

        isTokenDisabled(tokenResponse, "test-app");

        assertTrue(oauth.tokenRevocationRequest(tokenResponse.getRefreshToken()).refreshToken().send().isSuccess());

        isTokenDisabled(tokenResponse, "test-app");
    }

    // KEYCLOAK-17300
    @Test
    public void testRevokeRequestParamsMoreThanOnce() throws Exception {
        oauth.client("test-app", "password");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost",
            "password");

        isTokenEnabled(tokenResponse, "test-app");

        String revokeResponse = doTokenRevokeWithDuplicateParams(tokenResponse.getRefreshToken(), "refresh_token", "password");

        OAuth2ErrorRepresentation errorRep = JsonSerialization.readValue(revokeResponse, OAuth2ErrorRepresentation.class);
        assertEquals("duplicated parameter", errorRep.getErrorDescription());
        assertEquals(OAuthErrorException.INVALID_REQUEST, errorRep.getError());
    }

    @Test
    public void testRevokeSingleNormalSession() throws Exception {
        testRevokeSingleSession(TokenUtil.TOKEN_TYPE_REFRESH);
    }

    @Test
    public void testRevokeSingleOfflineSession() throws Exception {
        oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
        testRevokeSingleSession(TokenUtil.TOKEN_TYPE_OFFLINE);
    }

    private void testRevokeSingleSession(String expectedTokenType) throws Exception {
        oauth.client("test-app", "password");
        AccessTokenResponse tokenResponse = oauth.doPasswordGrantRequest("test-user@localhost",
                "password");
        AccessTokenResponse tokenResponse2 = oauth.doPasswordGrantRequest("test-user@localhost",
                "password");

        isTokenEnabled(tokenResponse, "test-app");
        isTokenEnabled(tokenResponse2, "test-app");

        assertTrue(oauth.tokenRevocationRequest(tokenResponse.getRefreshToken()).refreshToken().send().isSuccess());

        events.expect(EventType.REVOKE_GRANT)
                .session(tokenResponse.getSessionState())
                .detail(Details.REFRESH_TOKEN_ID, isTokenId())
                .detail(Details.REFRESH_TOKEN_TYPE, expectedTokenType)
                .client("test-app")
                .assertEvent(true);

        isTokenDisabled(tokenResponse, "test-app");
        isTokenEnabled(tokenResponse2, "test-app");
    }

    private AccessTokenResponse login(String clientId, String username, String password) {
        oauth.client(clientId, "password");
        oauth.openLoginForm();
        if (loginPage.isCurrent()) {
            loginPage.login(username, password);
        }
        String code = oauth.parseLoginResponse().getCode();
        return oauth.doAccessTokenRequest(code);
    }

    private void isTokenEnabled(AccessTokenResponse tokenResponse, String clientId) throws IOException {
        oauth.client(clientId, "password");
        TokenMetadataRepresentation rep = oauth.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken()).asTokenMetadata();
        assertTrue(rep.isActive());

        AccessTokenResponse tokenRefreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(Status.OK.getStatusCode(), tokenRefreshResponse.getStatusCode());
    }

    private void isTokenDisabled(AccessTokenResponse tokenResponse, String clientId) throws IOException {
        isAccessTokenDisabled(tokenResponse.getAccessToken(), clientId);

        oauth.client(clientId, "password");
        AccessTokenResponse tokenRefreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), tokenRefreshResponse.getStatusCode());
    }

    private void isAccessTokenDisabled(String accessTokenString, String clientId) throws IOException {
        // Test introspection endpoint not possible
        oauth.client(clientId, "password");
        TokenMetadataRepresentation rep = oauth.doIntrospectionAccessTokenRequest(accessTokenString).asTokenMetadata();
        assertFalse(rep.isActive());

        // Test userInfo endpoint not possible
        Response response = UserInfoClientUtil.executeUserInfoRequest_getMethod(userInfoClient, accessTokenString);
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), response.getStatus());

        // Test account REST not possible
        String accountUrl = OAuthClient.AUTH_SERVER_ROOT + "/realms/test/account";
        SimpleHttpRequest accountRequest = SimpleHttpDefault.doGet(accountUrl, restHttpClient)
                .auth(accessTokenString)
                .acceptJson();
        assertEquals(Status.UNAUTHORIZED.getStatusCode(), accountRequest.asStatus());

        // Test admin REST not possible
        try (Keycloak adminClient = Keycloak.getInstance(OAuthClient.AUTH_SERVER_ROOT, "test", "test-app", accessTokenString, AdminClientUtil.getSSLContextWithTruststore())) {
            try {
                adminClient.realms().realm("test").toRepresentation();
                Assert.fail("Not expected to obtain realm");
            } catch (NotAuthorizedException nae) {
                // Expected
            }
        }
    }

    private String doTokenRevokeWithDuplicateParams(String token, String tokenTypeHint, String clientSecret)
        throws IOException {
        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(oauth.getEndpoints().getRevocation());

            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair("token", token));
            parameters.add(new BasicNameValuePair("token", "foo"));
            parameters.add(new BasicNameValuePair("token_type_hint", tokenTypeHint));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, oauth.getClientId()));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, clientSecret));

            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            client.execute(post).getEntity().writeTo(out);
            return new String(out.toByteArray());
        }
    }
}
