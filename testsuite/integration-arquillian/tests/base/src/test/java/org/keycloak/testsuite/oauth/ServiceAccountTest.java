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
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.RealmBuilder;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.util.UserBuilder;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ServiceAccountTest extends AbstractKeycloakTest {

    private static String userId;
    private static String userName;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public ExpectedException expectedException = ExpectedException.none();


    @Override
    public void beforeAbstractKeycloakTest() throws Exception {
        super.beforeAbstractKeycloakTest();
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmBuilder realm = RealmBuilder.create().name("test")
                .privateKey("MIICXAIBAAKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQABAoGAfmO8gVhyBxdqlxmIuglbz8bcjQbhXJLR2EoS8ngTXmN1bo2L90M0mUKSdc7qF10LgETBzqL8jYlQIbt+e6TH8fcEpKCjUlyq0Mf/vVbfZSNaVycY13nTzo27iPyWQHK5NLuJzn1xvxxrUeXI6A2WFpGEBLbHjwpx5WQG9A+2scECQQDvdn9NE75HPTVPxBqsEd2z10TKkl9CZxu10Qby3iQQmWLEJ9LNmy3acvKrE3gMiYNWb6xHPKiIqOR1as7L24aTAkEAtyvQOlCvr5kAjVqrEKXalj0Tzewjweuxc0pskvArTI2Oo070h65GpoIKLc9jf+UA69cRtquwP93aZKtW06U8dQJAF2Y44ks/mK5+eyDqik3koCI08qaC8HYq2wVl7G2QkJ6sbAaILtcvD92ToOvyGyeE0flvmDZxMYlvaZnaQ0lcSQJBAKZU6umJi3/xeEbkJqMfeLclD27XGEFoPeNrmdx0q10Azp4NfJAY+Z8KRyQCR2BEG+oNitBOZ+YXF9KCpH3cdmECQHEigJhYg+ykOvr1aiZUMFT72HU0jnmQe2FVekuG+LJUt2Tm7GtMjTFoGpf0JwrVuZN39fOYAlo+nTixgeW7X8Y=")
                .publicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCrVrCuTtArbgaZzL1hvh0xtL5mc7o0NqPVnYXkLvgcwiC3BjLGw1tGEGoJaXDuSaRllobm53JBhjx33UNv+5z/UMG4kytBWxheNVKnL6GgqlNabMaFfPLPCF8kAgKnsi79NMo+n6KnSY8YeUmec/p2vjO2NjsSAVcWEQMVhJ31LwIDAQAB")
                .testEventListener();

        ClientRepresentation enabledApp = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("service-account-cl-refresh-on")
                .secret("secret1")
                .serviceAccountsEnabled(true)
                .attribute(OIDCConfigAttributes.USE_REFRESH_TOKEN_FOR_CLIENT_CREDENTIALS_GRANT, "true")
                .build();

        realm.client(enabledApp);

        ClientRepresentation enabledAppWithSkipRefreshToken = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("service-account-cl")
                .secret("secret1")
                .serviceAccountsEnabled(true)
                .build();

        realm.client(enabledAppWithSkipRefreshToken);

        ClientRepresentation disabledApp = ClientBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .clientId("service-account-disabled")
                .secret("secret1")
                .build();

        realm.client(disabledApp);

        ClientRepresentation secretsWithSpecialCharacterClient = ClientBuilder.create()
            .id(KeycloakModelUtils.generateId())
            .clientId("service-account-cl-special-secrets")
            .secret("secret/with=special?character")
            .serviceAccountsEnabled(true)
            .build();

        realm.client(secretsWithSpecialCharacterClient);

        UserBuilder defaultUser = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("test-user@localhost");
        realm.user(defaultUser);

        userId = KeycloakModelUtils.generateId();
        userName = ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + enabledApp.getClientId();

        UserBuilder serviceAccountUser = UserBuilder.create()
                .id(userId)
                .username(userName)
                .serviceAccountId(enabledApp.getClientId());
        realm.user(serviceAccountUser);

        testRealms.add(realm.build());
    }

    @Test
    public void clientCredentialsAuthSuccess() throws Exception {
        oauth.clientId("service-account-cl-refresh-on");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        assertEquals(200, response.getStatusCode());

        // older clients which use client-credentials grant may create a refresh-token and session, see KEYCLOAK-9551.
        List<Map<String, String>> clientSessionStats = getAdminClient().realm(oauth.getRealm()).getClientSessionStats();
        assertThat(clientSessionStats, hasSize(1));
        Map<String, String> sessionStats = clientSessionStats.get(0);
        assertEquals(sessionStats.get("clientId"), oauth.getClientId());

        // Refresh token is for backwards compatibility only. It won't be in client credentials by default
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        events.expectClientLogin()
                .client("service-account-cl-refresh-on")
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, userName)
                .assertEvent();

        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());
        System.out.println("Access token other claims: " + accessToken.getOtherClaims());
        Assert.assertEquals("service-account-cl-refresh-on", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        Assert.assertTrue(accessToken.getOtherClaims().containsKey(ServiceAccountConstants.CLIENT_ADDRESS));
        Assert.assertTrue(accessToken.getOtherClaims().containsKey(ServiceAccountConstants.CLIENT_HOST));

        OAuthClient.AccessTokenResponse refreshedResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "secret1");

        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshedResponse.getRefreshToken());

        assertEquals(accessToken.getSessionState(), refreshedAccessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshedRefreshToken.getSessionState());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState()).user(userId).client("service-account-cl-refresh-on").assertEvent();
    }

    // This is for the backwards compatibility only. By default, there won't be refresh token and hence there won't be availability for the logout
    @Test
    public void clientCredentialsLogout() throws Exception {
        oauth.clientId("service-account-cl-refresh-on");
        events.clear();

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());

        events.expectClientLogin()
                .client("service-account-cl-refresh-on")
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, userName)
                .detail(Details.CLIENT_AUTH_METHOD, ClientIdAndSecretAuthenticator.PROVIDER_ID)
                .assertEvent();

        HttpResponse logoutResponse = oauth.doLogout(response.getRefreshToken(), "secret1");
        assertEquals(204, logoutResponse.getStatusLine().getStatusCode());
        events.expectLogout(accessToken.getSessionState())
                .client("service-account-cl-refresh-on")
                .user(userId)
                .removeDetail(Details.REDIRECT_URI)
                .assertEvent();

        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "secret1");
        assertEquals(400, response.getStatusCode());
        assertEquals("invalid_grant", response.getError());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState())
                .client("service-account-cl-refresh-on")
                .user(userId)
                .removeDetail(Details.TOKEN_ID)
                .removeDetail(Details.UPDATED_REFRESH_TOKEN_ID)
                .error(Errors.INVALID_TOKEN).assertEvent();
    }

    @Test
    public void clientCredentialsInvalidClientCredentials() throws Exception {
        oauth.clientId("service-account-cl");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret2");

        assertEquals(401, response.getStatusCode());

        assertEquals("unauthorized_client", response.getError());

        events.expectClientLogin()
                .client("service-account-cl")
                .session((String) null)
                .clearDetails()
                .error(Errors.INVALID_CLIENT_CREDENTIALS)
                .user((String) null)
                .assertEvent();
    }

    @Test
    public void clientCredentialsDisabledServiceAccount() throws Exception {
        oauth.clientId("service-account-disabled");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        assertEquals(401, response.getStatusCode());

        assertEquals("unauthorized_client", response.getError());

        events.expectClientLogin()
                .client("service-account-disabled")
                .user((String) null)
                .session((String) null)
                .removeDetail(Details.USERNAME)
                .removeDetail(Details.RESPONSE_TYPE)
                .error(Errors.INVALID_CLIENT)
                .assertEvent();

    }

    @Test
    public void changeClientIdTest() throws Exception {

        ClientManager.realm(adminClient.realm("test")).clientId("service-account-cl-refresh-on").renameTo("updated-client");

        oauth.clientId("updated-client");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        assertEquals(200, response.getStatusCode());

        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        Assert.assertEquals("updated-client", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));

        // Username updated after client ID changed
        events.expectClientLogin()
                .client("updated-client")
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "updated-client")
                .assertEvent();


        ClientManager.realm(adminClient.realm("test")).clientId("updated-client").renameTo("service-account-cl-refresh-on");

    }

    @Test
    public void refreshTokenRefreshForDisabledServiceAccount() throws Exception {
        try {
            oauth.clientId("service-account-cl");
            OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");
            assertEquals(200, response.getStatusCode());

            ClientManager.realm(adminClient.realm("test")).clientId("service-account-cl").setServiceAccountsEnabled(false);

            response = oauth.doRefreshTokenRequest(response.getRefreshToken(), "secret1");
            assertEquals(400, response.getStatusCode());
        }
        finally {
            ClientManager.realm(adminClient.realm("test")).clientId("service-account-cl").setServiceAccountsEnabled(true);
            UserRepresentation user = ClientManager.realm(adminClient.realm("test")).clientId("service-account-cl").getServiceAccountUser();
        }
    }

    @Test
    public void clientCredentialsAuthRequest_ClientES256_RealmPS256() throws Exception {
    	conductClientCredentialsAuthRequestWithRefreshToken(Algorithm.HS256, Algorithm.ES256, Algorithm.PS256);
    }

    @Test
    public void failManagePassword() {
        UserResource serviceAccount = adminClient.realm("test").users().get(userId);
        UserRepresentation representation = serviceAccount.toRepresentation();

        CredentialRepresentation password = new CredentialRepresentation();
        password.setValue("password");
        password.setType(CredentialRepresentation.PASSWORD);
        password.setTemporary(false);

        representation.setCredentials(Arrays.asList(password));

        this.expectedException.expect(Matchers.allOf(Matchers.instanceOf(ClientErrorException.class),
                Matchers.hasProperty("response", Matchers.hasProperty("status", Matchers.is(400)))));
        this.expectedException.reportMissingExceptionWithMessage("Should fail, should not be possible to manage credentials for service accounts");

        serviceAccount.update(representation);
    }

    /**
     * See KEYCLOAK-9551
     */
    @Test
    public void clientCredentialsAuthSuccessWithoutRefreshToken_revokeToken() throws Exception {
        String tokenString = clientCredentialsAuthSuccessWithoutRefreshTokenImpl();
        AccessToken accessToken = oauth.verifyToken(tokenString);

        // Revoke access token
        CloseableHttpResponse response1 = oauth.doTokenRevoke(tokenString, "access_token", "secret1");
        assertThat(response1, org.keycloak.testsuite.util.Matchers.statusCodeIsHC(Response.Status.OK));
        response1.close();

        events.expect(EventType.REVOKE_GRANT)
                .client("service-account-cl")
                .user(AssertEvents.isUUID())
                .session(Matchers.isEmptyOrNullString())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .assertEvent();

        // Check that it is not possible to introspect token anymore
        Assert.assertFalse(getIntrospectionResponse("service-account-cl", "secret1", tokenString));
        // TODO: This would be better to be "INTROSPECT_TOKEN_ERROR"
        events.expect(EventType.INTROSPECT_TOKEN)
                .client("service-account-cl")
                .user(Matchers.isEmptyOrNullString())
                .session(Matchers.isEmptyOrNullString())
                .assertEvent();
    }

    @Test
    public void clientCredentialsAuthSuccessWithoutRefreshToken_pairWiseSubject() throws Exception {
        // Add pairwise protocolMapper through admin REST endpoint
        ProtocolMapperRepresentation pairwiseProtMapper = SHA256PairwiseSubMapper.createPairwiseMapper(null, null);
        ClientManager.realm(adminClient.realm("test")).clientId("service-account-cl")
                .addRedirectUris(oauth.getRedirectUri())
                .addProtocolMapper(pairwiseProtMapper);

        clientCredentialsAuthSuccessWithoutRefreshTokenImpl();

        ClientManager.realm(adminClient.realm("test")).clientId("service-account-cl").removeProtocolMapper(pairwiseProtMapper.getName());
    }

    // Returns accessToken string
    private String clientCredentialsAuthSuccessWithoutRefreshTokenImpl() throws Exception {
        oauth.clientId("service-account-cl");
        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

        assertEquals(200, response.getStatusCode());
        String tokenString = response.getAccessToken();

        Assert.assertNotNull("Access-Token should be present", tokenString);
        AccessToken accessToken = oauth.verifyToken(tokenString);
        Assert.assertNull(accessToken.getSessionState());
        Assert.assertNull("Refresh-Token should not be present", response.getRefreshToken());

        events.expectClientLogin()
                .client("service-account-cl")
                .user(AssertEvents.isUUID())
                .session(AssertEvents.isUUID())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.USERNAME, ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + "service-account-cl")
                .assertEvent();

        // new clients which use client-credentials grant should NOT create a refresh-token or session, see KEYCLOAK-9551.
        List<Map<String, String>> clientSessionStats = getAdminClient().realm(oauth.getRealm()).getClientSessionStats();
        assertThat(clientSessionStats, empty());

        // Check that token is possible to introspect
        Assert.assertTrue(getIntrospectionResponse("service-account-cl", "secret1", tokenString));
        events.expect(EventType.INTROSPECT_TOKEN)
                .client("service-account-cl")
                .user(AssertEvents.isUUID())
                .user(Matchers.isEmptyOrNullString())
                .session(Matchers.isEmptyOrNullString())
                .assertEvent();

        return tokenString;
    }

    private boolean getIntrospectionResponse(String clientId, String clientSecret, String tokenString) throws IOException {
        String introspectionResponse = oauth.introspectAccessTokenWithClientCredential(clientId, clientSecret, tokenString);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(introspectionResponse);
        return jsonNode.get("active").asBoolean();
    }

    private void conductClientCredentialsAuthRequestWithRefreshToken(String expectedRefreshAlg, String expectedAccessAlg, String realmTokenAlg) throws Exception {
        try {
            /// Realm Setting is used for ID Token Signature Algorithm
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, realmTokenAlg);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "service-account-cl-refresh-on"), expectedAccessAlg);
            clientCredentialsAuthSuccessWithRefreshToken(expectedRefreshAlg, expectedAccessAlg);
        } finally {
            TokenSignatureUtil.changeRealmTokenSignatureProvider(adminClient, Algorithm.RS256);
            TokenSignatureUtil.changeClientAccessTokenSignatureProvider(ApiUtil.findClientByClientId(adminClient.realm("test"), "service-account-cl-refresh-on"), Algorithm.RS256);
        }
        return;
    }

    // Testing of refresh token is for backwards compatibility. By default, there won't be refresh token for the client credentials grant
    private void clientCredentialsAuthSuccessWithRefreshToken(String expectedRefreshAlg, String expectedAccessAlg) throws Exception {
        oauth.clientId("service-account-cl-refresh-on");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");

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

        events.expectClientLogin()
                .client("service-account-cl-refresh-on")
                .user(userId)
                .session(accessToken.getSessionState())
                .detail(Details.TOKEN_ID, accessToken.getId())
                .detail(Details.REFRESH_TOKEN_ID, refreshToken.getId())
                .detail(Details.USERNAME, userName)
                .assertEvent();

        assertEquals(accessToken.getSessionState(), refreshToken.getSessionState());
        System.out.println("Access token other claims: " + accessToken.getOtherClaims());
        Assert.assertEquals("service-account-cl-refresh-on", accessToken.getOtherClaims().get(ServiceAccountConstants.CLIENT_ID));
        Assert.assertTrue(accessToken.getOtherClaims().containsKey(ServiceAccountConstants.CLIENT_ADDRESS));
        Assert.assertTrue(accessToken.getOtherClaims().containsKey(ServiceAccountConstants.CLIENT_HOST));

        OAuthClient.AccessTokenResponse refreshedResponse = oauth.doRefreshTokenRequest(response.getRefreshToken(), "secret1");

        AccessToken refreshedAccessToken = oauth.verifyToken(refreshedResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshedResponse.getRefreshToken());

        assertEquals(accessToken.getSessionState(), refreshedAccessToken.getSessionState());
        assertEquals(accessToken.getSessionState(), refreshedRefreshToken.getSessionState());

        events.expectRefresh(refreshToken.getId(), refreshToken.getSessionState()).user(userId).client("service-account-cl-refresh-on").assertEvent();
    }

    @Test
    public void userInfoForServiceAccountWithoutRefreshTokenImpl() throws Exception {
        oauth.clientId("service-account-cl");
        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");
        assertEquals(200, response.getStatusCode());
        assertNull(response.getRefreshToken());

        UserInfo info = oauth.doUserInfoRequest(response.getAccessToken());
        assertEquals(200, response.getStatusCode());
        assertEquals("service-account-service-account-cl", info.getPreferredUsername());
    }

    @Test
    public void userInfoForServiceAccountWithRefreshTokenImpl() throws Exception {
        oauth.clientId("service-account-cl-refresh-on");
        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret1");
        assertEquals(200, response.getStatusCode());
        assertNotNull(response.getRefreshToken());

        UserInfo info = oauth.doUserInfoRequest(response.getAccessToken());
        assertEquals(200, response.getStatusCode());
        assertEquals("service-account-service-account-cl-refresh-on", info.getPreferredUsername());

        HttpResponse logoutResponse = oauth.doLogout(response.getRefreshToken(), "secret1");
        assertEquals(204, logoutResponse.getStatusLine().getStatusCode());
    }

    /**
     *  See KEYCLOAK-18704
     */
    @Test
    public void clientCredentialsAuthSuccessWithUrlEncodedSpecialCharactersSecret() throws Exception {
        oauth.clientId("service-account-cl-special-secrets");

        OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("secret/with=special?character");

        assertEquals(200, response.getStatusCode());
    }
}
