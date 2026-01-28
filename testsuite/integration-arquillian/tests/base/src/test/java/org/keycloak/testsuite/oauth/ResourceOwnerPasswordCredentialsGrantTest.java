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

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.Profile;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.TimeBasedOTP;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.RealmManager;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.UserManager;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:sthorger@redhat.com">Stian Thorgersen</a>
 */
public class ResourceOwnerPasswordCredentialsGrantTest extends AbstractKeycloakTest {

    private static String userId;

    private static String userId2;

    private static String userIdMultipleOTPs;

    private final TimeBasedOTP totp = new TimeBasedOTP();

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
        oauth.openid(false);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmBuilder realm = RealmBuilder.create().name("test")
                .testEventListener();


        ClientRepresentation app = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("resource-owner")
                .directAccessGrants()
                .secret("secret")
                .build();
        realm.client(app);

        ClientRepresentation app2 = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("resource-owner-public")
                .directAccessGrants()
                .publicClient()
                .build();
        realm.client(app2);

        ClientRepresentation app3 = ClientBuilder.create().id(KeycloakModelUtils.generateId())
            .clientId("resource-owner-refresh").directAccessGrants().secret("secret").build();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(app3).setUseRefreshToken(false);
        realm.client(app3);

        UserBuilder defaultUser = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("test-user@localhost")
                .password("password");
        realm.user(defaultUser);

        UserRepresentation user = UserBuilder.create()
                .username("direct-login")
                .email("direct-login@localhost")
                .password("password")
                .build();
        realm.user(user);

        UserRepresentation user2 = UserBuilder.create()
                .username("direct-login-otp")
                .password("password")
                .totpSecret("totpSecret")
                .build();
        realm.user(user2);

        UserBuilder userBuilderMultipleOTPs = UserBuilder.create()
                .id(userIdMultipleOTPs)
                .username("direct-login-multiple-otps")
                .password("password")
                .totpSecret("firstOTPIsPreferredCredential");
        for (int i = 2; i <= 10; i++) userBuilderMultipleOTPs.totpSecret(String.format("%s-th OTP authenticator", i));
        realm.user(userBuilderMultipleOTPs.build());

        testRealms.add(realm.build());
    }

    @Override
    public void importTestRealms() {
        super.importTestRealms();
        userIdMultipleOTPs = adminClient.realm("test").users().search("direct-login-multiple-otps", true).get(0).getId();
        userId = adminClient.realm("test").users().search("direct-login", true).get(0).getId();
        userId2 = adminClient.realm("test").users().search("direct-login-otp", true).get(0).getId();
    }

    @Test
    public void grantAccessTokenUsername() throws Exception {
        int authSessionsBefore = getAuthenticationSessionsCount();

        grantAccessToken("direct-login", "resource-owner");

        // Check that count of authSessions is same as before authentication (as authentication session was removed)
        Assert.assertEquals(authSessionsBefore, getAuthenticationSessionsCount());
    }

    @Test
    public void grantAccessTokenEmail() throws Exception {
        grantAccessToken("direct-login@localhost", "resource-owner");
    }

    @Test
    public void grantAccessTokenPublic() throws Exception {
        grantAccessToken("direct-login", "resource-owner-public");
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void grantAccessTokenWithDynamicScope() throws Exception {
        ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
        clientScope.setName("dynamic-scope");
        clientScope.setAttributes(new HashMap<String, String>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, "dynamic-scope:*");
        }});
        clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        RealmResource realmResource = adminClient.realm("test");
        try(Response response = realmResource.clientScopes().create(clientScope)) {
            String scopeId = ApiUtil.getCreatedId(response);
            getCleanup().addClientScopeId(scopeId);
            ClientResource resourceOwnerPublicClient = ApiUtil.findClientByClientId(realmResource, "resource-owner-public");
            ClientRepresentation testAppRep = resourceOwnerPublicClient.toRepresentation();
            resourceOwnerPublicClient.update(testAppRep);
            resourceOwnerPublicClient.addOptionalClientScope(scopeId);
        }

        oauth.scope("dynamic-scope:123");
        oauth.client("resource-owner-public");

        AccessTokenResponse response = oauth.doPasswordGrantRequest("direct-login", "password");

        assertTrue(response.getScope().contains("dynamic-scope:123"));

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        events.expectLogin()
                .client("resource-owner-public")
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, "direct-login")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        assertTrue(accessToken.getScope().contains("dynamic-scope:123"));
    }

    @Test
    @EnableFeature(value = Profile.Feature.DYNAMIC_SCOPES, skipRestart = true)
    public void grantAccessTokenWithUnassignedDynamicScope() throws Exception {
        oauth.scope("unknown-scope:123");
        oauth.client("resource-owner-public");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("direct-login", "password");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_scope", response.getError());
        assertEquals("Invalid scopes: unknown-scope:123", response.getErrorDescription());
    }

    @Test
    public void grantAccessTokenWithTotp() throws Exception {
        grantAccessToken(userId2, "direct-login-otp", "resource-owner", totp.generateTOTP("totpSecret"));
    }

    @Test
    public void grantAccessTokenWithMultipleTotp() throws Exception {
        // Confirm user can login with 1-th OTP since it's the preferred credential
        grantAccessToken(userIdMultipleOTPs, "direct-login-multiple-otps", "resource-owner", totp.generateTOTP("firstOTPIsPreferredCredential"));
        // For remaining OTP tokens HTTP 401 "Unauthorized" is the allowed / expected response
        oauth.client("resource-owner", "secret");
        for (int i = 2; i <= 10; i++) {
            String otp = totp.generateTOTP(String.format("%s-th OTP authenticator", i));
            AccessTokenResponse response = oauth.passwordGrantRequest("direct-login-multiple-otps", "password").otp(otp).send();
            assertEquals(401, response.getStatusCode());
        }
    }

    @Test
    public void grantAccessTokenMissingTotp() throws Exception {
        oauth.client("resource-owner", "secret");

        AccessTokenResponse response = oauth.doPasswordGrantRequest("direct-login-otp", "password");

        assertEquals(401, response.getStatusCode());

        assertEquals("invalid_grant", response.getError());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .clearDetails()
                .error(Errors.INVALID_USER_CREDENTIALS)
                .user(userId2)
                .assertEvent();
    }

    @Test
    public void grantAccessTokenInvalidTotp() throws Exception {
        int authSessionsBefore = getAuthenticationSessionsCount();

        oauth.client("resource-owner", "secret");

        AccessTokenResponse response = oauth.passwordGrantRequest("direct-login-otp", "password").otp(totp.generateTOTP("totpSecret2")).send();

        assertEquals(401, response.getStatusCode());

        assertEquals("invalid_grant", response.getError());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .clearDetails()
                .error(Errors.INVALID_USER_CREDENTIALS)
                .user(userId2)
                .assertEvent();

        // Check that count of authSessions is same as before authentication (as authentication session was removed)
        Assert.assertEquals(authSessionsBefore, getAuthenticationSessionsCount());
    }

    private void grantAccessToken(String login, String clientId) throws Exception {
        grantAccessToken(userId, login, clientId, null);
    }

    private void grantAccessToken(String userId, String login, String clientId, String otp) throws Exception {
        oauth.client(clientId, "secret");

        AccessTokenResponse response = oauth.passwordGrantRequest( login, "password").otp(otp).send();

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        events.expectLogin()
                .client(clientId)
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, login)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        Assert.assertTrue(login.equals(accessToken.getPreferredUsername()) || login.equals(accessToken.getEmail()));

        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());

        AccessTokenResponse refreshedResponse = oauth.doRefreshTokenRequest(response.getRefreshToken());

        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshedResponse.getRefreshToken());

        assertEquals(accessToken.getSessionState(), refreshedAccessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshedRefreshToken.getSessionState());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState()).user(userId).client(clientId).assertEvent();
    }

    @Test
    public void grantRequest_ClientES256_RealmPS256() throws Exception {
        conductGrantRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.ES256, Algorithm.PS256);
    }

    @Test
    public void grantRequest_ClientPS256_RealmES256() throws Exception {
        conductGrantRequest(Constants.INTERNAL_SIGNATURE_ALGORITHM, Algorithm.PS256, Algorithm.ES256);
    }

    private void conductGrantRequest(String expectedRefreshAlg, String expectedAccessAlg, String realmTokenAlg) throws Exception {
        try {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, realmTokenAlg);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "resource-owner"), expectedAccessAlg);
            grantRequest(expectedRefreshAlg, expectedAccessAlg);
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "resource-owner"), Algorithm.RS256);
        }
        return;
    }

    private void grantRequest(String expectedRefreshAlg, String expectedAccessAlg) throws Exception {
        String clientId = "resource-owner";
        String login = "direct-login";

        oauth.client(clientId, "secret");

        AccessTokenResponse response = oauth.doPasswordGrantRequest(login, "password");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        JWSHeader header = new JWSInput(response.getAccessToken()).getHeader();

        assertEquals(expectedAccessAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        header = new JWSInput(response.getRefreshToken()).getHeader();
        assertEquals(expectedRefreshAlg, header.getAlgorithm().name());
        assertEquals("JWT", header.getType());
        assertNull(header.getContentType());

        events.expectLogin()
                .client(clientId)
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, login)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .assertEvent();

        Assert.assertTrue(login.equals(accessToken.getPreferredUsername()) || login.equals(accessToken.getEmail()));

        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());

        AccessTokenResponse refreshedResponse = oauth.doRefreshTokenRequest(response.getRefreshToken());

        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshedResponse.getRefreshToken());

        assertEquals(accessToken.getSessionState(), refreshedAccessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshedRefreshToken.getSessionState());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState()).user(userId).client(clientId).assertEvent();
    }

    @Test
    public void grantAccessTokenLogout() throws Exception {
        oauth.client("resource-owner", "secret");

        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        events.expectLogin()
                .client("resource-owner")
                .session(accessToken.getSessionState())
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .assertEvent();

        LogoutResponse logoutResponse = oauth.doLogout(response.getRefreshToken());
        assertTrue(logoutResponse.isSuccess());
        events.expectLogout(accessToken.getSessionState()).client("resource-owner").removeDetail(Details.REDIRECT_URI).assertEvent();

        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState())
                .client("resource-owner")
                .user((String) null)
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .error(Errors.INVALID_TOKEN).assertEvent();
    }

    @Test
    public void grantAccessTokenInvalidClientCredentials() throws Exception {
        oauth.client("resource-owner", "invalid");

        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        assertEquals(401, response.getStatusCode());

        assertEquals("unauthorized_client", response.getError());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .clearDetails()
                .error(Errors.INVALID_CLIENT_CREDENTIALS)
                .user((String) null)
                .assertEvent();
    }

    @Test
    public void grantAccessTokenMissingClientCredentials() throws Exception {
        oauth.client("resource-owner", null);

        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        assertEquals(401, response.getStatusCode());

        assertEquals("unauthorized_client", response.getError());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .clearDetails()
                .error(Errors.INVALID_CLIENT_CREDENTIALS)
                .user((String) null)
                .assertEvent();
    }

    @Test
    public void grantAccessTokenClientNotAllowed() throws Exception {

        ClientManager.realm(adminClient.realm("test")).clientId("resource-owner").directAccessGrant(false);

        oauth.client("resource-owner", "secret");

        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        assertEquals(400, response.getStatusCode());

        assertEquals(OAuthErrorException.UNAUTHORIZED_CLIENT, response.getError());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .clearDetails()
                .error(Errors.NOT_ALLOWED)
                .user((String) null)
                .assertEvent();

        ClientManager.realm(adminClient.realm("test")).clientId("resource-owner").directAccessGrant(true);

    }

    @Test
    public void grantAccessTokenVerifyEmail() throws Exception {
        int authSessionsBefore = getAuthenticationSessionsCount();

        RealmResource realmResource = adminClient.realm("test");
        RealmManager.realm(realmResource).verifyEmail(true);

        oauth.client("resource-owner", "secret");

        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");

        assertEquals(400, response.getStatusCode());

        assertEquals("invalid_grant", response.getError());
        assertEquals("Account is not fully set up", response.getErrorDescription());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .clearDetails()
                .error(Errors.RESOLVE_REQUIRED_ACTIONS)
                .user((String) null)
                .assertEvent();

        RealmManager.realm(realmResource).verifyEmail(false);
        UserManager.realm(realmResource).username("test-user@localhost").removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.toString());

        // Check that count of authSessions is same as before authentication (as authentication session was removed)
        Assert.assertEquals(authSessionsBefore, getAuthenticationSessionsCount());
    }
    
    @Test
    public void grantAccessTokenVerifyEmailInvalidPassword() throws Exception {

        RealmResource realmResource = adminClient.realm("test");
        RealmManager.realm(realmResource).verifyEmail(true);

        oauth.client("resource-owner", "secret");

        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "bad-password");

        assertEquals(401, response.getStatusCode());

        assertEquals("invalid_grant", response.getError());
        assertEquals("Invalid user credentials", response.getErrorDescription());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .assertEvent();

        RealmManager.realm(realmResource).verifyEmail(false);
        UserManager.realm(realmResource).username("test-user@localhost").removeRequiredAction(UserModel.RequiredAction.VERIFY_EMAIL.toString());

    }

    @Test
    public void grantAccessTokenExpiredPassword() throws Exception {

        RealmResource realmResource = adminClient.realm("test");
        RealmManager.realm(realmResource).passwordPolicy("forceExpiredPasswordChange(1)");

        try {
            setTimeOffset(60 * 60 * 48);

            oauth.client("resource-owner", "secret");

            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "password");

            assertEquals(400, response.getStatusCode());

            assertEquals("invalid_grant", response.getError());
            assertEquals("Account is not fully set up", response.getErrorDescription());

            setTimeOffset(0);

            events.expectLogin()
                    .client("resource-owner")
                    .session((String) null)
                    .clearDetails()
                    .error(Errors.RESOLVE_REQUIRED_ACTIONS)
                    .user((String) null)
                    .assertEvent();
        } finally {
            RealmManager.realm(realmResource).passwordPolicy("");
            UserManager.realm(realmResource).username("test-user@localhost")
                    .removeRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        }
    }
    
    @Test
    public void grantAccessTokenExpiredPasswordInvalidPassword() throws Exception {

        RealmResource realmResource = adminClient.realm("test");
        RealmManager.realm(realmResource).passwordPolicy("forceExpiredPasswordChange(1)");

        try {
            setTimeOffset(60 * 60 * 48);

            oauth.client("resource-owner", "secret");

            AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "bad-password");

            assertEquals(401, response.getStatusCode());

            assertEquals("invalid_grant", response.getError());
            assertEquals("Invalid user credentials", response.getErrorDescription());

            events.expectLogin()
                    .client("resource-owner")
                    .session((String) null)
                    .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                    .removeDetail(Details.CODE_ID)
                    .removeDetail(Details.REDIRECT_URI)
                    .removeDetail(Details.CONSENT)
                    .error(Errors.INVALID_USER_CREDENTIALS)
                    .assertEvent();
        } finally {
            RealmManager.realm(realmResource).passwordPolicy("");
            UserManager.realm(realmResource).username("test-user@localhost")
                    .removeRequiredAction(UserModel.RequiredAction.UPDATE_PASSWORD.toString());
        }
    }

    @Test
    public void grantAccessTokenInvalidUserCredentialsPerf() throws Exception {
        int count = 5;

        // Measure the times when username exists, but password is invalid
        long sumInvalidPasswordMs = perfTest(count, "Invalid password", this::grantAccessTokenInvalidUserCredentials);

        // Measure the times when username does not exists
        long sumInvalidUsernameMs = perfTest(count, "User not found", this::grantAccessTokenUserNotFound);

        String errorMessage = String.format("Times in ms of %d attempts: For invalid password: %d. For invalid username: %d", count, sumInvalidPasswordMs, sumInvalidUsernameMs);

        // The times should be very similar. Using the bigger difference just to avoid flakiness (Before the fix, the difference was like 3 times shorter time for invalid-username, which allowed quite accurate username enumeration)
        Assert.assertTrue(errorMessage, sumInvalidUsernameMs * 2 > sumInvalidPasswordMs);
    }

    private long perfTest(int actionsCount, String actionMessage, RunnableWithException action) throws Exception {
        long sumTimeMs = 0;
        for (int i = 0 ; i < actionsCount ; i++) {
            long start = System.currentTimeMillis();
            action.run();
            long took = System.currentTimeMillis() - start;
            getLogger().infof("%s %d: %d ms", actionMessage, i + 1, took);
            sumTimeMs = sumTimeMs + took;
        }
        return sumTimeMs;
    }

    private interface RunnableWithException {
        void run() throws Exception;
    }

    @Test
    public void grantAccessTokenInvalidUserCredentials() throws Exception {
        oauth.client("resource-owner", "secret");

        AccessTokenResponse response = oauth.doPasswordGrantRequest("test-user@localhost", "invalid");

        assertEquals(401, response.getStatusCode());

        assertEquals("invalid_grant", response.getError());
        assertEquals("Invalid user credentials", response.getErrorDescription());

        events.expectLogin()
                .client("resource-owner")
                .session((String) null)
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .error(Errors.INVALID_USER_CREDENTIALS)
                .assertEvent();
    }

    @Test
    public void grantAccessTokenUserNotFound() throws Exception {
        oauth.client("resource-owner", "secret");

        AccessTokenResponse response = oauth.doPasswordGrantRequest("invalid", "invalid");

        assertEquals(401, response.getStatusCode());

        assertEquals("invalid_grant", response.getError());

        events.expectLogin()
                .client("resource-owner")
                .user((String) null)
                .session((String) null)
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.USERNAME, "invalid")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .error(Errors.USER_NOT_FOUND)
                .assertEvent();
    }

    @Test
    public void grantAccessTokenMissingGrantType() throws Exception {
        oauth.client("resource-owner", "secret");

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(oauth.getEndpoints().getToken());
            post.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
            AccessTokenResponse response = new AccessTokenResponse(client.execute(post));

            assertEquals(400, response.getStatusCode());

            assertEquals("invalid_request", response.getError());
            assertEquals("Missing form parameter: grant_type", response.getErrorDescription());
        }
    }

    @Test
    public void grantAccessTokenUnsupportedGrantType() throws Exception {
        oauth.client("resource-owner", "secret");

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(oauth.getEndpoints().getToken());
            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "unsupported_grant_type"));
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);
            AccessTokenResponse response = new AccessTokenResponse(client.execute(post));

            assertEquals(400, response.getStatusCode());

            assertEquals(OAuthErrorException.UNSUPPORTED_GRANT_TYPE, response.getError());
            assertEquals("Unsupported grant_type", response.getErrorDescription());
        }
    }

    @Test
    public void grantAccessTokenNoRefreshToken() throws Exception {
        oauth.client("resource-owner-refresh", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("direct-login", "password");

        assertEquals(200, response.getStatusCode());

        assertNotNull(response.getAccessToken());
        assertNull(response.getRefreshToken());
    }

    @Test
    public void grantAccessTokenServiceAccountUserOfOtherClient() throws Exception {
        ClientManager.realm(adminClient.realm("test")).clientId("resource-owner").setServiceAccountsEnabled(true);
        oauth.client("resource-owner-refresh", "secret");
        AccessTokenResponse response = oauth.doPasswordGrantRequest("service-account-resource-owner", "password");

        assertEquals(401, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());
        assertEquals("Invalid user credentials", response.getErrorDescription());

        events.expectLogin()
                .client("resource-owner-refresh")
                .session((String) null)
                .user((String) null)
                .detail(Details.GRANT_TYPE, OAuth2Constants.PASSWORD)
                .detail(Details.REASON, "User is a service account")
                .removeDetail(Details.CODE_ID)
                .removeDetail(Details.REDIRECT_URI)
                .removeDetail(Details.CONSENT)
                .error(Errors.INVALID_USER)
                .assertEvent();

        ClientManager.realm(adminClient.realm("test")).clientId("resource-owner").setServiceAccountsEnabled(false);
    }

    private int getAuthenticationSessionsCount() {
        return testingClient.testing().cache(InfinispanConnectionProvider.AUTHENTICATION_SESSIONS_CACHE_NAME).size();
    }
}
