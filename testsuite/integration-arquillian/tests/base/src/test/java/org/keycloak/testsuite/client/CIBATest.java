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
package org.keycloak.testsuite.client;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;

import javax.ws.rs.core.Response.Status;

import org.apache.http.client.methods.CloseableHttpResponse;

import static org.junit.Assert.assertThat;
import static org.keycloak.protocol.oidc.grants.ciba.endpoints.BackchannelAuthenticationCallbackEndpoint.SUCCEEDED;
import static org.keycloak.protocol.oidc.grants.ciba.endpoints.BackchannelAuthenticationCallbackEndpoint.UNAUTHORIZED;
import static org.keycloak.protocol.oidc.grants.ciba.endpoints.BackchannelAuthenticationCallbackEndpoint.CANCELLED;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.QUARKUS;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.CibaConfig;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelRequest;
import org.keycloak.protocol.oidc.grants.ciba.channel.HttpAuthenticationChannelProviderFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableCiba;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.representation.TestAuthenticationChannelRequest;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.WaitUtils;
import org.keycloak.testsuite.util.OAuthClient.AuthenticationRequestAcknowledgement;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableCiba
@EnableFeature(value = Profile.Feature.CIBA, skipRestart = true)
@AuthServerContainerExclude({REMOTE, QUARKUS})
public class CIBATest extends AbstractTestRealmKeycloakTest {

    private final String AUTHENTICATION_CHANNEL_SERVER_NAME = "authentication-channel-server";
    private final String AUTHENTICATION_CHANNEL_SERVER_PASSWORD = "passwort-authentication-channel-server";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {

        UserRepresentation user = UserBuilder.create()
                .username("nutzername-schwarz")
                .email("schwarz@test.example.com")
                .enabled(true)
                .password("passwort-schwarz")
                .addRoles("user", "offline_access")
                .build();
        testRealm.getUsers().add(user);

        user = UserBuilder.create()
                .username("nutzername-rot")
                .email("rot@test.example.com")
                .enabled(true)
                .password("passwort-rot")
                .addRoles("user", "offline_access")
                .build();
        testRealm.getUsers().add(user);

        user = UserBuilder.create()
                .username("nutzername-gelb")
                .email("gelb@test.example.com")
                .enabled(true)
                .password("passwort-gelb")
                .addRoles("user", "offline_access")
                .build();
        testRealm.getUsers().add(user);

        user = UserBuilder.create()
                .username("nutzername-deaktiviert")
                .email("deaktiviert@test.example.com")
                .enabled(false)
                .password("passwort-deaktiviert")
                .addRoles("user", "offline_access")
                .build();
        testRealm.getUsers().add(user);

        ClientRepresentation confApp = KeycloakModelUtils.createClient(testRealm, AUTHENTICATION_CHANNEL_SERVER_NAME);
        confApp.setSecret(AUTHENTICATION_CHANNEL_SERVER_PASSWORD);
        confApp.setServiceAccountsEnabled(Boolean.TRUE);

    }

    @BeforeClass
    public static void setAuthenticationChannelRequestUri() {
        System.setProperty("keycloak.ciba.auth.channel.provider", HttpAuthenticationChannelProviderFactory.PROVIDER_ID);
        System.setProperty("keycloak.ciba.http.auth.channel.uri", TestApplicationResourceUrls.clientAuthenticationChannelRequestUri());
    }

    private String cibaBackchannelTokenDeliveryMode;
    private Integer cibaExpiresIn;
    private Integer cibaInterval;
    private String cibaAuthRequestedUserHint;

    private final String TEST_REALM_NAME = "test";
    private final String TEST_CLIENT_NAME = "test-app";
    private final String TEST_CLIENT_PASSWORD = "password";

    private AuthenticationRequestAcknowledgement doBackchannelAuthenticationRequest(String username) throws Exception {
        return doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, null);
    }

    private AuthenticationRequestAcknowledgement doBackchannelAuthenticationRequest(String clientId, String clientSecret, String username, String bindingMessage) throws Exception {
        AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(clientId, clientSecret, username, bindingMessage);
        Assert.assertThat(response.getStatusCode(), is(equalTo(200)));
        Assert.assertNotNull(response.getAuthReqId());
        return response;
    }

    private TestAuthenticationChannelRequest doAuthenticationChannelRequest() {
        // get Authentication Channel Request keycloak has done on Backchannel Authentication Endpoint from the FIFO queue of testing Authentication Channel Request API
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        TestAuthenticationChannelRequest authenticationChannelReq = oidcClientEndpointsResource.getAuthenticationChannel();
        return authenticationChannelReq;
    }

    private EventRepresentation doAuthenticationChannelCallback(String authenticationChannelReq, String authenticationChannelStatus, String username) throws Exception {
        return doAuthenticationChannelCallback(TEST_CLIENT_NAME, authenticationChannelReq, authenticationChannelStatus, username);
    }

    private EventRepresentation doAuthenticationChannelCallback(String clientIdAsConsumerDevice, String authenticationChannelReq, String authenticationChannelStatus, String username) throws Exception {
        int statusCode = oauth.doAuthenticationChannelCallback(authenticationChannelReq, authenticationChannelStatus);
        Assert.assertThat(statusCode, is(equalTo(200)));
        // check login event : ignore user id and other details except for username
        EventRepresentation representation = new EventRepresentation();

        representation.setDetails(Collections.emptyMap());

        return representation;
    }

    private EventRepresentation doAuthenticationChannelCallback(String clientId, TestAuthenticationChannelRequest testRequest, String authenticationChannelStatus, String username, String error) throws Exception {
        int statusCode = oauth.doAuthenticationChannelCallback(testRequest.getBearerToken(), authenticationChannelStatus);
        Assert.assertThat(statusCode, is(equalTo(200)));
        return events.expect(EventType.LOGIN_ERROR).clearDetails().client(clientId).error(error).user((String)null).session(CoreMatchers.nullValue(String.class)).assertEvent();
    }

    private EventRepresentation doAuthenticationChannelCallbackError(Status status, String clientId, TestAuthenticationChannelRequest testRequest, String authenticationChannelStatus, String username, String error) throws Exception {
        int statusCode = oauth.doAuthenticationChannelCallback(testRequest.getBearerToken(), authenticationChannelStatus);
        Assert.assertThat(statusCode, is(equalTo(status.getStatusCode())));
        return events.expect(EventType.LOGIN_ERROR).clearDetails().client(clientId).error(error).user((String)null).session(CoreMatchers.nullValue(String.class)).assertEvent();
    }

    private OAuthClient.AccessTokenResponse doBackchannelAuthenticationTokenRequest(String codeId, String sessionId, String username, String authReqId, boolean isOfflineAccess) throws Exception {
        return doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, codeId, sessionId, username, authReqId, isOfflineAccess);
    }

    private OAuthClient.AccessTokenResponse doBackchannelAuthenticationTokenRequest(String clientId, String clientSecret, String codeId, String sessionId, String username, String authReqId, boolean isOfflineAccess) throws Exception {
        OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(clientId, clientSecret, authReqId);
        Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(200)));
        EventRepresentation event = events.expectAuthReqIdToToken(codeId, sessionId).clearDetails().user(AssertEvents.isUUID()).client(clientId).assertEvent();

        AccessToken accessToken = oauth.verifyToken(tokenRes.getAccessToken());
        Assert.assertThat(accessToken.getIssuedFor(), is(equalTo(clientId)));

        RefreshToken refreshToken = oauth.parseRefreshToken(tokenRes.getRefreshToken());
        Assert.assertThat(refreshToken.getIssuedFor(), is(equalTo(clientId)));
        Assert.assertThat(refreshToken.getAudience()[0], is(equalTo(refreshToken.getIssuer())));

        IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
        Assert.assertThat(idToken.getPreferredUsername(), is(equalTo(username)));
        Assert.assertThat(idToken.getIssuedFor(), is(equalTo(clientId)));
        Assert.assertThat(idToken.getAudience()[0], is(equalTo(idToken.getIssuedFor())));

        return tokenRes;
    }

    private String doIntrospectAccessTokenWithClientCredential(OAuthClient.AccessTokenResponse tokenRes, String username) throws IOException {
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, tokenRes.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);
        Assert.assertThat(jsonNode.get("active").asBoolean(), is(equalTo(true)));
        Assert.assertThat(jsonNode.get("username").asText(), is(equalTo(username)));
        Assert.assertThat(jsonNode.get("client_id").asText(), is(equalTo(TEST_CLIENT_NAME)));
        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);
        Assert.assertThat(rep.isActive(), is(equalTo(true)));
        Assert.assertThat(rep.getClientId(), is(equalTo(TEST_CLIENT_NAME)));
        Assert.assertThat(rep.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        events.expect(EventType.INTROSPECT_TOKEN).user((String)null).clearDetails().assertEvent();

        tokenResponse = oauth.introspectAccessTokenWithClientCredential(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, tokenRes.getRefreshToken());
        jsonNode = objectMapper.readTree(tokenResponse);
        Assert.assertThat(jsonNode.get("active").asBoolean(), is(equalTo(true)));
        Assert.assertThat(jsonNode.get("client_id").asText(), is(equalTo(TEST_CLIENT_NAME)));
        rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);
        Assert.assertThat(rep.isActive(), is(equalTo(true)));
        Assert.assertThat(rep.getClientId(), is(equalTo(TEST_CLIENT_NAME)));
        Assert.assertThat(rep.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        Assert.assertThat(rep.getAudience()[0], is(equalTo(rep.getIssuer())));
        events.expect(EventType.INTROSPECT_TOKEN).user((String)null).clearDetails().assertEvent();

        tokenResponse = oauth.introspectAccessTokenWithClientCredential(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, tokenRes.getIdToken());
        jsonNode = objectMapper.readTree(tokenResponse);
        Assert.assertThat(jsonNode.get("active").asBoolean(), is(equalTo(true)));
        Assert.assertThat(jsonNode.get("client_id").asText(), is(equalTo(TEST_CLIENT_NAME)));
        rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);
        Assert.assertThat(rep.isActive(), is(equalTo(true)));
        Assert.assertThat(rep.getUserName(), is(equalTo(username)));
        Assert.assertThat(rep.getClientId(), is(equalTo(TEST_CLIENT_NAME)));
        Assert.assertThat(rep.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        Assert.assertThat(rep.getPreferredUsername(), is(equalTo(username)));
        Assert.assertThat(rep.getAudience()[0], is(equalTo(rep.getIssuedFor())));
        events.expect(EventType.INTROSPECT_TOKEN).user((String)null).clearDetails().assertEvent();

        return tokenResponse;
    }

    private OAuthClient.AccessTokenResponse doRefreshTokenRequest(String oldRefreshToken, String username, String sessionId, boolean isOfflineAccess) {
        OAuthClient.AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(oldRefreshToken, TEST_CLIENT_PASSWORD);
        Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(200)));

        AccessToken accessToken = oauth.verifyToken(tokenRes.getAccessToken());
        Assert.assertThat(accessToken.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        Assert.assertThat(accessToken.getExp().longValue(), is(equalTo(accessToken.getIat().longValue() + tokenRes.getExpiresIn())));

        RefreshToken refreshToken = oauth.parseRefreshToken(tokenRes.getRefreshToken());
        Assert.assertThat(refreshToken.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        Assert.assertThat(refreshToken.getAudience()[0], is(equalTo(refreshToken.getIssuer())));
        if(!isOfflineAccess) Assert.assertThat(refreshToken.getExp().longValue(), is(equalTo(refreshToken.getIat().longValue() + tokenRes.getRefreshExpiresIn())));

        IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
        Assert.assertThat(idToken.getPreferredUsername(), is(equalTo(username)));
        Assert.assertThat(idToken.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        Assert.assertThat(idToken.getAudience()[0], is(equalTo(idToken.getIssuedFor())));
        Assert.assertThat(idToken.getExp().longValue(), is(equalTo(idToken.getIat().longValue() + tokenRes.getExpiresIn())));

        events.expectRefresh(tokenRes.getRefreshToken(), sessionId).session(CoreMatchers.notNullValue(String.class)).user(AssertEvents.isUUID()).clearDetails().assertEvent();

        return tokenRes;
    }

    private EventRepresentation doLogoutByRefreshToken(String refreshToken, String sessionId, String userId, boolean isOfflineAccess) throws IOException{
        try (CloseableHttpResponse res = oauth.doLogout(refreshToken, TEST_CLIENT_PASSWORD)) {
            assertThat(res, Matchers.statusCodeIsHC(Status.NO_CONTENT));
        }

        // confirm logged out
        OAuthClient.AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(refreshToken, TEST_CLIENT_PASSWORD);
        Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
        Assert.assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.INVALID_GRANT)));
        if (isOfflineAccess) Assert.assertThat(tokenRes.getErrorDescription(), is(equalTo("Offline user session not found")));
        else Assert.assertThat(tokenRes.getErrorDescription(), is(equalTo("Session not active")));

        return events.expectLogout(sessionId).client(TEST_CLIENT_NAME).user(AssertEvents.isUUID()).session(AssertEvents.isUUID()).clearDetails().assertEvent();
    }

    private EventRepresentation doTokenRevokeByRefreshToken(String refreshToken, String sessionId, String userId, boolean isOfflineAccess) throws IOException{
        try (CloseableHttpResponse res = oauth.doTokenRevoke(refreshToken, "refresh_token", TEST_CLIENT_PASSWORD)) {
            assertThat(res, Matchers.statusCodeIsHC(Status.OK));
        }

        // confirm revocation
        OAuthClient.AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(refreshToken, TEST_CLIENT_PASSWORD);
        Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
        Assert.assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.INVALID_GRANT)));
        if (isOfflineAccess) Assert.assertThat(tokenRes.getErrorDescription(), is(equalTo("Offline user session not found")));
        else Assert.assertThat(tokenRes.getErrorDescription(), is(equalTo("Session not active")));

        return events.expect(EventType.REVOKE_GRANT).clearDetails().client(TEST_CLIENT_NAME).user(AssertEvents.isUUID()).assertEvent();
    }

    private void testBackchannelAuthenticationFlow(boolean isOfflineAccess) throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            if(isOfflineAccess) oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
            Assert.assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));
            if (isOfflineAccess) Assert.assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.OFFLINE_ACCESS)));
            Assert.assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.SCOPE_OPENID)));

            // user Authentication Channel completed
            EventRepresentation loginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String userId = loginEvent.getUserId();

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(codeId, sessionId, username, response.getAuthReqId(), isOfflineAccess);

            // token introspection
            String tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // token refresh
            tokenRes = doRefreshTokenRequest(tokenRes.getRefreshToken(), username, sessionId, isOfflineAccess);

            // token introspection after token refresh
            tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // logout by refresh token
            EventRepresentation logoutEvent = doLogoutByRefreshToken(tokenRes.getRefreshToken(), sessionId, userId, isOfflineAccess);

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testAttackerClientUseVictimAuthReqIdAttack() throws Exception {
        ClientResource victimClientResource = null;
        ClientResource attackerClientResource = null;
        ClientRepresentation victimClientRep = null;
        ClientRepresentation attackerClientRep = null;
        try {
            final String username = "nutzername-gelb";
            final String victimClientName = "test-app-scope";
            final String attackerClientName = TEST_CLIENT_NAME;
            final String victimClientPassword = "password";
            final String attackerClientPassword = TEST_CLIENT_PASSWORD;
            String victimClientAuthReqId = null;
            victimClientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), victimClientName);
            victimClientRep = victimClientResource.toRepresentation();
            prepareCIBASettings(victimClientResource, victimClientRep);
            attackerClientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), attackerClientName);
            attackerClientRep = attackerClientResource.toRepresentation();
            prepareCIBASettings(attackerClientResource, attackerClientRep);

            // victim client Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(victimClientName, victimClientPassword, username, "asdfghjkl");
            victimClientAuthReqId = response.getAuthReqId();

            // victim client Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest victimClientAuthenticationChannelReq = testRequest.getRequest();

            // victim client Authentication Channel completed
            EventRepresentation victimClientloginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);

            // attacker client Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(attackerClientName, attackerClientPassword, victimClientAuthReqId);
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.INVALID_GRANT)));
            Assert.assertThat(tokenRes.getErrorDescription(), is(equalTo("unauthorized client")));
        } finally {
            revertCIBASettings(victimClientResource, victimClientRep);
            revertCIBASettings(attackerClientResource, attackerClientRep);
        }
    }

    @Test
    public void testAuthenticationChannelUnexpectedError() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";
            final String signal = "GODOWN";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, signal);
            Assert.assertThat(response.getStatusCode(), is(equalTo(503)));

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.INVALID_GRANT)));
            Assert.assertThat(tokenRes.getErrorDescription(), is(equalTo("Invalid Auth Req ID")));
        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testBackchannelAuthnReqWithDeactivatedUser() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-deaktiviert";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, null);
            Assert.assertThat(response.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testBackchannelAuthnReqWithUnknownUser() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "Unbekannt";
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage);
            Assert.assertThat(response.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testBackchannelAuthnReqWithoutLoginHint() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = null;
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage);
            Assert.assertThat(response.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testLoginHintTokenRequiredButNotSend() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-schwarz";
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT, CibaGrantType.LOGIN_HINT_TOKEN);
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, null);
            Assert.assertThat(response.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
        } finally {
            revertCIBASettings(clientResource, clientRep);
            restoreCIBAPolicy();
        }
    }

//    public void testDifferentUserAuthenticated() throws Exception {
//        ClientResource clientResource = null;
//        ClientRepresentation clientRep = null;
//        try {
//            final String usernameToBeAuthenticated = "nutzername-rot";
//            final String usernameAuthenticated = "nutzername-gelb";
//            final String bindingMessage = "BASTION";
//
//            // prepare CIBA settings
//            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
//            clientRep = clientResource.toRepresentation();
//            prepareCIBASettings(clientResource, clientRep);
//            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);
//
//            // user Backchannel Authentication Request
//            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, usernameToBeAuthenticated, bindingMessage);
//
//            // user Authentication Channel Request
//            AuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest();
//            Assert.assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));
//            Assert.assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.OFFLINE_ACCESS)));
//
//            // different user Authentication Channel completed
//            oauth.doAuthenticationChannelCallback(AUTHENTICATION_CHANNEL_SERVER_NAME, AUTHENTICATION_CHANNEL_SERVER_PASSWORD, usernameAuthenticated, authenticationChannelReq.getAuthenticationChannelId(), SUCCEEDED);
//
//            // user Token Request
//            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
//            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
//            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.INVALID_GRANT));
//        } finally {
//            revertCIBASettings(clientResource, clientRep);
//        }
//    }

    @Test
    public void testTokenRevocation() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
            Assert.assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));
            Assert.assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.OFFLINE_ACCESS)));

            // user Authentication Channel completed
            EventRepresentation loginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String userId = loginEvent.getUserId();

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(codeId, sessionId, username, response.getAuthReqId(), true);

            // token introspection
            String tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // token refresh
            tokenRes = doRefreshTokenRequest(tokenRes.getRefreshToken(), username, sessionId, true);

            // token introspection after token refresh
            tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // revoke by refresh token
            EventRepresentation logoutEvent = doTokenRevokeByRefreshToken(tokenRes.getRefreshToken(), sessionId, userId, true);

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testChangeInterval() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String firstUsername = "nutzername-schwarz";
            final String secondUsername = "nutzername-rot";
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // first user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(firstUsername);
            Assert.assertThat(response.getInterval(), is(equalTo(5)));
            // dequeue user Authentication Channel Request by first user to revert the initial setting of the queue
            doAuthenticationChannelRequest();

            // set interval
            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(1200));
            attrMap.put(CibaConfig.CIBA_INTERVAL, String.valueOf(10));
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            // first user Token Request
            // second user Backchannel Authentication Request
            response = doBackchannelAuthenticationRequest(secondUsername);
            Assert.assertThat(response.getInterval(), is(equalTo(10)));
            // dequeue user Authentication Channel Request by second user to revert the initial setting of the queue
            doAuthenticationChannelRequest();
        } finally {
            revertCIBASettings(clientResource, clientRep);
            restoreCIBAPolicy();
        }
    }

    @Test
    public void testAccessThrottling() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_INTERVAL, String.valueOf(3));
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
            Assert.assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.AUTHORIZATION_PENDING)); // 10+5+5 sec

            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.SLOW_DOWN)); // 10+5+5 sec

            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.SLOW_DOWN)); // 10+5+5+5 sec

            // user Authentication Channel completed
            EventRepresentation loginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String userId = loginEvent.getUserId();

            WaitUtils.pause(3000);

            tokenRes = doBackchannelAuthenticationTokenRequest(codeId, sessionId, username, response.getAuthReqId(), false);

            // token introspection
            String tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // token refresh
            tokenRes = doRefreshTokenRequest(tokenRes.getRefreshToken(), username, sessionId, false);

            // token introspection after token refresh
            tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // revoke by refresh token
            EventRepresentation logoutEvent = doTokenRevokeByRefreshToken(tokenRes.getRefreshToken(), sessionId, userId, false);

        } finally {
            revertCIBASettings(clientResource, clientRep);
            restoreCIBAPolicy();
        }
    }

    @Test
    public void testTokenRequestAfterIntervalButNotYetAuthenticated() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_INTERVAL, String.valueOf(5));
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage);

            // user Token Request but not yet user being authenticated
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.AUTHORIZATION_PENDING));

            // user Token Request but not yet user being authenticated
            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.SLOW_DOWN));

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
            Assert.assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));

            // user Authentication Channel completed
            EventRepresentation loginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String userId = loginEvent.getUserId();

            WaitUtils.pause(5000);

            // user Token Request again
            tokenRes = doBackchannelAuthenticationTokenRequest(codeId, sessionId, username, response.getAuthReqId(), false);

            // token introspection
            String tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // token refresh
            tokenRes = doRefreshTokenRequest(tokenRes.getRefreshToken(), username, sessionId, false);

            // token introspection after token refresh
            tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // revoke by refresh token
            EventRepresentation logoutEvent = doTokenRevokeByRefreshToken(tokenRes.getRefreshToken(), sessionId, userId, false);

        } finally {
            revertCIBASettings(clientResource, clientRep);
            restoreCIBAPolicy();
        }
    }

    private RealmRepresentation backupCIBAPolicy() {
        RealmRepresentation rep = testRealm().toRepresentation();
        Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
        cibaBackchannelTokenDeliveryMode = attrMap.get(CibaConfig.CIBA_BACKCHANNEL_TOKENDELIVERY_MODE);
        cibaExpiresIn = Integer.parseInt(attrMap.get(CibaConfig.CIBA_EXPIRES_IN));
        cibaInterval = Integer.parseInt(attrMap.get(CibaConfig.CIBA_INTERVAL));
        cibaAuthRequestedUserHint = attrMap.get(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT);
        return rep;
    }

    private void restoreCIBAPolicy() {
        RealmRepresentation rep = testRealm().toRepresentation();
        Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
        attrMap.put(CibaConfig.CIBA_BACKCHANNEL_TOKENDELIVERY_MODE, cibaBackchannelTokenDeliveryMode);
        attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(cibaExpiresIn));
        attrMap.put(CibaConfig.CIBA_INTERVAL, String.valueOf(cibaInterval));
        attrMap.put(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT, cibaAuthRequestedUserHint);
        rep.setAttributes(attrMap);
        testRealm().update(rep);
    }

    @Test
    public void testCIBAPolicy() {
        try {
            // null input - default values used
            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_BACKCHANNEL_TOKENDELIVERY_MODE, null);
            attrMap.put(CibaConfig.CIBA_EXPIRES_IN, null);
            attrMap.put(CibaConfig.CIBA_INTERVAL, null);
            attrMap.put(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT, null);
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            rep = testRealm().toRepresentation();
            attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            Assert.assertThat(attrMap.get(CibaConfig.CIBA_BACKCHANNEL_TOKENDELIVERY_MODE), is(equalTo("poll")));
            Assert.assertThat(Integer.parseInt(attrMap.get(CibaConfig.CIBA_EXPIRES_IN)), is(equalTo(120)));
            Assert.assertThat(Integer.parseInt(attrMap.get(CibaConfig.CIBA_INTERVAL)), is(equalTo(5)));
            Assert.assertThat(attrMap.get(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT), is(equalTo("login_hint")));

            // valid input
            rep = backupCIBAPolicy();
            attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_BACKCHANNEL_TOKENDELIVERY_MODE, "poll");
            attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(736));
            attrMap.put(CibaConfig.CIBA_INTERVAL, String.valueOf(7));
            attrMap.put(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT, "login_hint");
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            rep = testRealm().toRepresentation();
            Assert.assertThat(attrMap.get(CibaConfig.CIBA_BACKCHANNEL_TOKENDELIVERY_MODE), is(equalTo("poll")));
            Assert.assertThat(Integer.parseInt(attrMap.get(CibaConfig.CIBA_EXPIRES_IN)), is(equalTo(736)));
            Assert.assertThat(Integer.parseInt(attrMap.get(CibaConfig.CIBA_INTERVAL)), is(equalTo(7)));
            Assert.assertThat(attrMap.get(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT), is(equalTo("login_hint")));
        } finally {
            restoreCIBAPolicy();
        }
    }

    @Test
    public void testBackchannelAuthenticationFlow() throws Exception {
        testBackchannelAuthenticationFlow(false);
    }

    @Test
    public void testBackchannelAuthenticationFlowOfflineAccess() throws Exception {
        testBackchannelAuthenticationFlow(true);
    }

    private void prepareCIBASettings(ClientResource clientResource, ClientRepresentation clientRep) {
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setBackchannelTokenDeliveryMode("poll");
        clientResource.update(clientRep);
    }

    private void revertCIBASettings(ClientResource clientResource, ClientRepresentation clientRep) {
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setBackchannelTokenDeliveryMode(null);
        clientResource.update(clientRep);
    }

    @Test
    public void testMultipleUsersBackchannelAuthenticationFlows() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String firstUsername = "nutzername-schwarz";
            final String secondUsername = "nutzername-rot";
            String firstUserAuthReqId = null;
            String secondUserAuthReqId = null;
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // first user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(firstUsername);
            firstUserAuthReqId = response.getAuthReqId();

            // first user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest firstUserAuthenticationChannelReq = testRequest.getRequest();

            // first user Authentication Channel completed
            EventRepresentation firstUserloginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, firstUsername);

            String firstUserSessionId = firstUserloginEvent.getSessionId();
            String firstUserSessionCodeId = firstUserloginEvent.getDetails().get(Details.CODE_ID);

            // second user Backchannel Authentication Request
            response = doBackchannelAuthenticationRequest(secondUsername);
            secondUserAuthReqId = response.getAuthReqId();

            // second user Authentication Channel Request
            testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest secondUserAuthenticationChannelReq = testRequest.getRequest();

            // second user Authentication Channel completed
            EventRepresentation secondUserloginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, secondUsername);
            String secondUserSessionId = secondUserloginEvent.getSessionId();
            String secondUserSessionCodeId = secondUserloginEvent.getDetails().get(Details.CODE_ID);

            // second user Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(secondUserSessionCodeId, secondUserSessionId, secondUsername, secondUserAuthReqId, false);

            // first user Token Request
            tokenRes = doBackchannelAuthenticationTokenRequest(firstUserSessionCodeId, firstUserSessionId, firstUsername, firstUserAuthReqId, false);

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testExplicitConsentRequiredBackchannelAuthenticationFlows() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-gelb";
            final String clientName = "third-party";  // see testrealm.json : "consentRequired": true
            final String clientPassword = "password";
            String clientAuthReqId = null;
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), clientName);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // client Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(clientName, clientPassword, username, "asdfghjkl");
            clientAuthReqId = response.getAuthReqId();

            // client Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest clientAuthenticationChannelReq = testRequest.getRequest();
            Assert.assertTrue(clientAuthenticationChannelReq.getConsentRequired());
            Assert.assertThat(clientAuthenticationChannelReq.getScope(), is(containsString(OAuth2Constants.SCOPE_OPENID)));
            Assert.assertThat(clientAuthenticationChannelReq.getScope(), is(containsString("email")));
            Assert.assertThat(clientAuthenticationChannelReq.getScope(), is(containsString("profile")));
            Assert.assertThat(clientAuthenticationChannelReq.getScope(), is(containsString("roles")));

            // client Authentication Channel completed
            EventRepresentation clientloginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);
            String clientSessionId = clientloginEvent.getSessionId();
            String clientSessionCodeId = clientloginEvent.getDetails().get(Details.CODE_ID);

            // client Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(clientName, clientPassword, clientSessionCodeId, clientSessionId, username, clientAuthReqId, false);

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testMultipleClientsBackchannelAuthenticationFlows() throws Exception {
        ClientResource firstClientResource = null;
        ClientResource secondClientResource = null;
        ClientRepresentation firstClientRep = null;
        ClientRepresentation secondClientRep = null;
        try {
            final String username = "nutzername-gelb";
            final String firstClientName = "test-app-scope"; // see testrealm.json
            final String secondClientName = TEST_CLIENT_NAME;
            final String firstClientPassword = "password";
            final String secondClientPassword = TEST_CLIENT_PASSWORD;
            String firstClientAuthReqId = null;
            String secondClientAuthReqId = null;
            firstClientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), firstClientName);
            firstClientRep = firstClientResource.toRepresentation();
            prepareCIBASettings(firstClientResource, firstClientRep);
            secondClientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), secondClientName);
            secondClientRep = secondClientResource.toRepresentation();
            prepareCIBASettings(secondClientResource, secondClientRep);

            // first client Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(firstClientName, firstClientPassword, username, "asdfghjkl");
            firstClientAuthReqId = response.getAuthReqId();

            // first client Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest firstClientAuthenticationChannelReq = testRequest.getRequest();

            // first client Authentication Channel completed
            EventRepresentation firstClientloginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);
            String firstClientSessionId = null;
            String firstClientSessionCodeId = null;

            // second client Backchannel Authentication Request
            response = doBackchannelAuthenticationRequest(secondClientName, secondClientPassword, username, "qwertyui");
            secondClientAuthReqId = response.getAuthReqId();

            // second client Authentication Channel Request
            testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest secondClientAuthenticationChannelReq = testRequest.getRequest();

            // second client Authentication Channel completed
            EventRepresentation secondClientloginEvent = doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);
            String secondClientSessionId = null;
            String secondClientSessionCodeId = null;

            // second client Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(secondClientName, secondClientPassword, secondClientSessionCodeId, secondClientSessionId, username, secondClientAuthReqId, false);

            // first client Token Request
            tokenRes = doBackchannelAuthenticationTokenRequest(firstClientName, firstClientPassword, firstClientSessionCodeId, firstClientSessionId, username, firstClientAuthReqId, false);

        } finally {
            revertCIBASettings(firstClientResource, firstClientRep);
            revertCIBASettings(secondClientResource, secondClientRep);
        }
    }

    @Test
    public void testRequestTokenBeforeAuthenticationNotCompleted() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(username);

            // user Token Request before Authentication Channel completion
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.AUTHORIZATION_PENDING));

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();

            // user Authentication Channel completed
            doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);

            WaitUtils.pause(6000);

            // user Token Request after Authentication Channel completion
            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(200)));

            IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
            Assert.assertThat(idToken.getPreferredUsername(), is(equalTo(username)));

            AccessToken accessToken = oauth.verifyToken(tokenRes.getAccessToken());

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testRequestTokenAfterAuthReqIdExpired() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(60));
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(username);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();

            // user Authentication Channel completed
            doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);

            setTimeOffset(70);

            // user Token Request before Authentication Channel completion
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.EXPIRED_TOKEN));

        } finally {
            revertCIBASettings(clientResource, clientRep);
            restoreCIBAPolicy();
        }
    }

    @Test
    public void testDuplicatedTokenRequestWithSameAuthReqId() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-gelb";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(username);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();

            // user Authentication Channel completed
            doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(200)));

            IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
            Assert.assertThat(idToken.getPreferredUsername(), is(equalTo(username)));

            AccessToken accessToken = oauth.verifyToken(tokenRes.getAccessToken());

            // duplicate user Token Request
            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.INVALID_GRANT));

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testOtherClientSendTokenRequest() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(username);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();

            // user Authentication Channel completed
            doAuthenticationChannelCallback(testRequest.getBearerToken(), SUCCEEDED, username);

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(AUTHENTICATION_CHANNEL_SERVER_NAME, AUTHENTICATION_CHANNEL_SERVER_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            Assert.assertThat(tokenRes.getError(), is(OAuthErrorException.INVALID_GRANT));

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testAuthenticationChannelUnauthorized() throws Exception {
        testAuthenticationChannelErrorCase(Status.FORBIDDEN, UNAUTHORIZED, OAuthErrorException.ACCESS_DENIED, Errors.CONSENT_DENIED);
    }

    @Test
    public void testAuthenticationChannelCancelled() throws Exception {
        testAuthenticationChannelErrorCase(Status.FORBIDDEN, CANCELLED, OAuthErrorException.ACCESS_DENIED, Errors.NOT_ALLOWED);
    }

    private void testAuthenticationChannelErrorCase(Status status, String authnResult, String error, String errorEvent) throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(username);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest();
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();

            // user Authentication Channel completed
            doAuthenticationChannelCallbackError(Status.BAD_REQUEST, TEST_CLIENT_NAME, testRequest, authnResult, username, errorEvent);

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            Assert.assertThat(tokenRes.getStatusCode(), is(equalTo(status.getStatusCode())));
            Assert.assertThat(tokenRes.getError(), is(error));

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    protected AuthenticationChannelRequest toAccessToken(String rpt) {
        AuthenticationChannelRequest accessToken;

        try {
            accessToken = new JWSInput(rpt).readJsonContent(AuthenticationChannelRequest.class);
        } catch (JWSInputException cause) {
            throw new RuntimeException("Failed to deserialize RPT", cause);
        }
        return accessToken;
    }
}
