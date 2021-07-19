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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.containsString;

import javax.ws.rs.core.Response.Status;

import org.apache.http.client.methods.CloseableHttpResponse;

import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status.CANCELLED;
import static org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status.SUCCEED;
import static org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse.Status.UNAUTHORIZED;
import static org.keycloak.testsuite.Assert.assertExpiration;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.QUARKUS;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateContextConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureCibaAuthenticationRequestSigningAlgorithmExecutorConfig;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.models.CibaConfig;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelRequest;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor.SecureCibaAuthenticationRequestSigningAlgorithmExecutor;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor.SecureCibaAuthenticationRequestSigningAlgorithmExecutorFactory;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor.SecureCibaSessionEnforceExecutorFactory;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor.SecureCibaSignedAuthenticationRequestExecutor;
import org.keycloak.protocol.oidc.grants.ciba.clientpolicy.executor.SecureCibaSignedAuthenticationRequestExecutorFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyEvent;
import org.keycloak.services.clientpolicy.ClientPoliciesUtil;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmExecutorFactory;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.representation.TestAuthenticationChannelRequest;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.services.clientpolicy.executor.TestRaiseExeptionExecutorFactory;
import org.keycloak.testsuite.util.InfinispanTestTimeServiceRule;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.Matchers;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.OAuthClient.AuthenticationRequestAcknowledgement;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableFeature(value = Profile.Feature.CIBA, skipRestart = true)
@AuthServerContainerExclude({REMOTE, QUARKUS})
public class CIBATest extends AbstractClientPoliciesTest {

    private static final String TEST_USER_NAME = "test-user@localhost";

    private final String SECOND_TEST_CLIENT_NAME = "test-second-client";
    private final String SECOND_TEST_CLIENT_SECRET = "passwort-test-second-client";
    private static final String ERR_MSG_CLIENT_REG_FAIL = "Failed to send request";

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Rule
    public InfinispanTestTimeServiceRule ispnTestTimeService = new InfinispanTestTimeServiceRule(this);

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);

        UserRepresentation user = UserBuilder.create()
                .username("nutzername-schwarz")
                .email("schwarz@test.example.com")
                .enabled(true)
                .password("passwort-schwarz")
                .addRoles("user", "offline_access")
                .build();
        realm.getUsers().add(user);

        user = UserBuilder.create()
                .username("nutzername-rot")
                .email("rot@test.example.com")
                .enabled(true)
                .password("passwort-rot")
                .addRoles("user", "offline_access")
                .build();
        realm.getUsers().add(user);

        user = UserBuilder.create()
                .username("nutzername-gelb")
                .email("gelb@test.example.com")
                .enabled(true)
                .password("passwort-gelb")
                .addRoles("user", "offline_access")
                .build();
        realm.getUsers().add(user);

        user = UserBuilder.create()
                .username("nutzername-deaktiviert")
                .email("deaktiviert@test.example.com")
                .enabled(false)
                .password("passwort-deaktiviert")
                .addRoles("user", "offline_access")
                .build();
        realm.getUsers().add(user);

        ClientRepresentation confApp = KeycloakModelUtils.createClient(realm, SECOND_TEST_CLIENT_NAME);
        confApp.setSecret(SECOND_TEST_CLIENT_SECRET);
        confApp.setServiceAccountsEnabled(Boolean.TRUE);

        testRealms.add(realm);
    }

    private String cibaBackchannelTokenDeliveryMode;
    private Integer cibaExpiresIn;
    private Integer cibaInterval;
    private String cibaAuthRequestedUserHint;

    private final String TEST_REALM_NAME = "test";
    private final String TEST_CLIENT_NAME = "test-app";
    private final String TEST_CLIENT_PASSWORD = "password";

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
            TestAuthenticationChannelRequest victimClientAuthenticationChannelReq = doAuthenticationChannelRequest("asdfghjkl");

            // victim client Authentication Channel completed
            doAuthenticationChannelCallback(victimClientAuthenticationChannelReq);

            // attacker client Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(attackerClientName, attackerClientPassword, victimClientAuthReqId);
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.INVALID_GRANT)));
            assertThat(tokenRes.getErrorDescription(), is(equalTo("unauthorized client")));
        } finally {
            revertCIBASettings(victimClientResource, victimClientRep);
            revertCIBASettings(attackerClientResource, attackerClientRep);
        }
    }

    // This tests that client should *not* be allowed to do whole CIBA flow by himself without any interaction from the user
    @Test
    public void testAttackerClientUseAuthReqIdInCallbackEndpoint() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // client sends Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage);

            // This request should not ever pass. Client should not be allowed to send the successfull "approve" request to the BackchannelAuthenticationCallbackEndpoint
            // with using the "authReqId" as a bearer token
            int statusCode = oauth.doAuthenticationChannelCallback(response.getAuthReqId(), SUCCEED);
            assertThat(statusCode, is(equalTo(403)));

            // client sends TokenRequest - This should not pass and should return 400
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.AUTHORIZATION_PENDING)));
        } finally {
            revertCIBASettings(clientResource, clientRep);
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
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, signal, null);
            assertThat(response.getStatusCode(), is(equalTo(503)));

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.INVALID_GRANT)));
            assertThat(tokenRes.getErrorDescription(), is(equalTo("Invalid Auth Req ID")));
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
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, null, "acr2");
            assertThat(response.getStatusCode(), is(equalTo(400)));
            assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
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
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage, "urn:mace:incommon:iap:silver urn:mace:incommon:iap:gold");
            assertThat(response.getStatusCode(), is(equalTo(400)));
            assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
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
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage, "ACR1");
            assertThat(response.getStatusCode(), is(equalTo(400)));
            assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
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
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, null, null);
            assertThat(response.getStatusCode(), is(equalTo(400)));
            assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
        } finally {
            revertCIBASettings(clientResource, clientRep);
            restoreCIBAPolicy();
        }
    }

    @Test
    @Ignore("Should never happen because the AD does not send any information about the user but only the status of the authentication")
    public void testDifferentUserAuthenticated() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String usernameToBeAuthenticated = "nutzername-rot";
            final String usernameAuthenticated = "nutzername-gelb";
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, usernameToBeAuthenticated, bindingMessage);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest(bindingMessage);
            assertThat(authenticationChannelReq.getRequest().getScope(), is(containsString(OAuth2Constants.OFFLINE_ACCESS)));

            // different user Authentication Channel completed
//            oauth.doAuthenticationChannelCallback(SECOND_TEST_CLIENT_NAME, SECOND_TEST_CLIENT_SECRET, usernameAuthenticated, authenticationChannelReq.getBearerToken(), SUCCEEDED);

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.INVALID_GRANT));
        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

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
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest(bindingMessage);
            assertThat(authenticationChannelReq.getRequest().getScope(), is(containsString(OAuth2Constants.OFFLINE_ACCESS)));

            // user Authentication Channel completed
            EventRepresentation loginEvent = doAuthenticationChannelCallback(authenticationChannelReq);
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String userId = loginEvent.getUserId();

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(username, response.getAuthReqId());

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
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, firstUsername, "lbies8e");
            assertThat(response.getInterval(), is(equalTo(5)));
            // dequeue user Authentication Channel Request by first user to revert the initial setting of the queue
            doAuthenticationChannelRequest("lbies8e");

            // set interval
            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(1200));
            attrMap.put(CibaConfig.CIBA_INTERVAL, String.valueOf(10));
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            // first user Token Request
            // second user Backchannel Authentication Request
            response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, secondUsername, "Keb9eser");
            assertThat(response.getInterval(), is(equalTo(10)));
            // dequeue user Authentication Channel Request by second user to revert the initial setting of the queue
            doAuthenticationChannelRequest("Keb9eser");
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
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest(bindingMessage);
            assertThat(authenticationChannelReq.getRequest().getBindingMessage(), is(equalTo(bindingMessage)));

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.AUTHORIZATION_PENDING)); // 10+5+5 sec

            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.SLOW_DOWN)); // 10+5+5 sec

            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.SLOW_DOWN)); // 10+5+5+5 sec

            // user Authentication Channel completed
            EventRepresentation loginEvent = doAuthenticationChannelCallback(authenticationChannelReq);
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String userId = loginEvent.getUserId();

            setTimeOffset(3);

            tokenRes = doBackchannelAuthenticationTokenRequest(username, response.getAuthReqId());

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
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.AUTHORIZATION_PENDING));

            // user Token Request but not yet user being authenticated
            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.SLOW_DOWN));

            // user Authentication Channel Request
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest(bindingMessage);
            assertThat(authenticationChannelReq.getRequest().getBindingMessage(), is(equalTo(bindingMessage)));

            // user Authentication Channel completed
            EventRepresentation loginEvent = doAuthenticationChannelCallback(authenticationChannelReq);
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String userId = loginEvent.getUserId();

            setTimeOffset(5);

            // user Token Request again
            tokenRes = doBackchannelAuthenticationTokenRequest(username, response.getAuthReqId());

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
    public void testCIBAPolicy() {
        try {
            // null input - default values used
            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE, null);
            attrMap.put(CibaConfig.CIBA_EXPIRES_IN, null);
            attrMap.put(CibaConfig.CIBA_INTERVAL, null);
            attrMap.put(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT, null);
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            rep = testRealm().toRepresentation();
            attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            assertThat(attrMap.get(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE), is(equalTo("poll")));
            assertThat(Integer.parseInt(attrMap.get(CibaConfig.CIBA_EXPIRES_IN)), is(equalTo(120)));
            assertThat(Integer.parseInt(attrMap.get(CibaConfig.CIBA_INTERVAL)), is(equalTo(5)));
            assertThat(attrMap.get(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT), is(equalTo("login_hint")));

            // valid input
            rep = backupCIBAPolicy();
            attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE, "poll");
            attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(736));
            attrMap.put(CibaConfig.CIBA_INTERVAL, String.valueOf(7));
            attrMap.put(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT, "login_hint");
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            rep = testRealm().toRepresentation();
            assertThat(attrMap.get(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE), is(equalTo("poll")));
            assertThat(Integer.parseInt(attrMap.get(CibaConfig.CIBA_EXPIRES_IN)), is(equalTo(736)));
            assertThat(Integer.parseInt(attrMap.get(CibaConfig.CIBA_INTERVAL)), is(equalTo(7)));
            assertThat(attrMap.get(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT), is(equalTo("login_hint")));
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

    @Test
    public void testBackchannelAuthenticationFlowWithoutBindingMessage() throws Exception {
        testBackchannelAuthenticationFlow(false, null);
    }

    @Test
    public void testBackchannelAuthenticationFlowOfflineAccessWithoutBindingMessage() throws Exception {
        testBackchannelAuthenticationFlow(true, null);
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
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, firstUsername, "Pjb9eD8w");
            firstUserAuthReqId = response.getAuthReqId();

            // first user Authentication Channel Request
            TestAuthenticationChannelRequest firstUserAuthenticationChannelReq = doAuthenticationChannelRequest("Pjb9eD8w");

            // first user Authentication Channel completed
            EventRepresentation firstUserloginEvent = doAuthenticationChannelCallback(firstUserAuthenticationChannelReq);
            String firstUserSessionId = firstUserloginEvent.getSessionId();
            String firstUserSessionCodeId = firstUserloginEvent.getDetails().get(Details.CODE_ID);

            // second user Backchannel Authentication Request
            response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, secondUsername, "dEkg8vDdsl");
            secondUserAuthReqId = response.getAuthReqId();

            // second user Authentication Channel Request
            TestAuthenticationChannelRequest secondUserAuthenticationChannelReq = doAuthenticationChannelRequest("dEkg8vDdsl");

            // second user Authentication Channel completed
            EventRepresentation secondUserloginEvent = doAuthenticationChannelCallback(secondUserAuthenticationChannelReq);
            String secondUserSessionId = secondUserloginEvent.getSessionId();
            String secondUserSessionCodeId = secondUserloginEvent.getDetails().get(Details.CODE_ID);

            // second user Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(secondUsername, secondUserAuthReqId);

            // first user Token Request
            tokenRes = doBackchannelAuthenticationTokenRequest(firstUsername, firstUserAuthReqId);

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
            TestAuthenticationChannelRequest clientAuthenticationChannelReq = doAuthenticationChannelRequest("asdfghjkl");
            Assert.assertTrue(clientAuthenticationChannelReq.getRequest().getConsentRequired());
            assertThat(clientAuthenticationChannelReq.getRequest().getScope(), is(containsString(OAuth2Constants.SCOPE_OPENID)));
            assertThat(clientAuthenticationChannelReq.getRequest().getScope(), is(containsString("email")));
            assertThat(clientAuthenticationChannelReq.getRequest().getScope(), is(containsString("profile")));
            assertThat(clientAuthenticationChannelReq.getRequest().getScope(), is(containsString("roles")));

            // client Authentication Channel completed
            EventRepresentation clientloginEvent = doAuthenticationChannelCallback(clientAuthenticationChannelReq);
            String clientSessionId = clientloginEvent.getSessionId();

            String clientSessionCodeId = clientloginEvent.getDetails().get(Details.CODE_ID);

            // client Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(clientName, clientPassword, username, clientAuthReqId);

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
            assertThat(firstClientResource, notNullValue());

            firstClientRep = firstClientResource.toRepresentation();
            prepareCIBASettings(firstClientResource, firstClientRep);

            secondClientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), secondClientName);
            assertThat(secondClientResource, notNullValue());

            secondClientRep = secondClientResource.toRepresentation();
            prepareCIBASettings(secondClientResource, secondClientRep);

            // first client Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(firstClientName, firstClientPassword, username, "asdfghjkl");
            firstClientAuthReqId = response.getAuthReqId();

            // first client Authentication Channel Request
            TestAuthenticationChannelRequest firstClientAuthenticationChannelReq = doAuthenticationChannelRequest("asdfghjkl");

            // first client Authentication Channel completed
            EventRepresentation firstClientloginEvent = doAuthenticationChannelCallback(firstClientAuthenticationChannelReq);

            // second client Backchannel Authentication Request
            response = doBackchannelAuthenticationRequest(secondClientName, secondClientPassword, username, "qwertyui");
            secondClientAuthReqId = response.getAuthReqId();

            // second client Authentication Channel Request
            TestAuthenticationChannelRequest secondClientAuthenticationChannelReq = doAuthenticationChannelRequest("qwertyui");

            // second client Authentication Channel completed
            EventRepresentation secondClientloginEvent = doAuthenticationChannelCallback(secondClientAuthenticationChannelReq);

            // second client Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(secondClientName, secondClientPassword, username, secondClientAuthReqId);

            // first client Token Request
            tokenRes = doBackchannelAuthenticationTokenRequest(firstClientName, firstClientPassword, username, firstClientAuthReqId);

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
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, "kvoDKw98");

            // user Token Request before Authentication Channel completion
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.AUTHORIZATION_PENDING));

            // user Authentication Channel Request
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest("kvoDKw98");

            // user Authentication Channel completed
            doAuthenticationChannelCallback(authenticationChannelReq);

            setTimeOffset(6);

            // user Token Request after Authentication Channel completion
            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(200)));

            IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
            assertThat(idToken.getPreferredUsername(), is(equalTo(username)));

            AccessToken accessToken = oauth.verifyToken(tokenRes.getAccessToken());
            assertThat(accessToken, notNullValue());

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
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(60));
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, "obkes8dke1");

            // user Authentication Channel Request
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest("obkes8dke1");

            // user Authentication Channel completed
            doAuthenticationChannelCallback(authenticationChannelReq);

            setTimeOffset(70);

            // user Token Request before Authentication Channel completion
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.EXPIRED_TOKEN));

        } finally {
            revertCIBASettings(clientResource, clientRep);
            restoreCIBAPolicy();
        }
    }

    @Test
    public void testCallbackAfterAuthenticationRequestExpired() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            RealmRepresentation rep = backupCIBAPolicy();
            Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
            attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(60));
            rep.setAttributes(attrMap);
            testRealm().update(rep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, "3FIekcs9");

            // user Authentication Channel Request
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest("3FIekcs9");

            setTimeOffset(70);

            int statusCode = oauth.doAuthenticationChannelCallback(authenticationChannelReq.getBearerToken(), SUCCEED);
            assertThat(statusCode, is(equalTo(Status.FORBIDDEN.getStatusCode())));
            events.expect(EventType.LOGIN_ERROR).clearDetails().client((String) null).error(Errors.INVALID_TOKEN).user((String) null).session(CoreMatchers.nullValue(String.class)).assertEvent();
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
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, "kciwje86");

            // user Authentication Channel Request
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest("kciwje86");

            // user Authentication Channel completed
            doAuthenticationChannelCallback(authenticationChannelReq);

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(200)));

            IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
            assertThat(idToken.getPreferredUsername(), is(equalTo(username)));

            AccessToken accessToken = oauth.verifyToken(tokenRes.getAccessToken());

            // duplicate user Token Request
            tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.INVALID_GRANT));

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
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, "ldkq366");

            // user Authentication Channel Request
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest("ldkq366");

            // user Authentication Channel completed
            doAuthenticationChannelCallback(authenticationChannelReq);

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(SECOND_TEST_CLIENT_NAME, SECOND_TEST_CLIENT_SECRET, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.INVALID_GRANT));

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testAuthenticationChannelUnauthorized() throws Exception {
        testAuthenticationChannelErrorCase(Status.OK, Status.FORBIDDEN, UNAUTHORIZED, OAuthErrorException.ACCESS_DENIED, Errors.CONSENT_DENIED);
    }

    @Test
    public void testAuthenticationChannelCancelled() throws Exception {
        testAuthenticationChannelErrorCase(Status.OK, Status.FORBIDDEN, CANCELLED, OAuthErrorException.ACCESS_DENIED, Errors.NOT_ALLOWED);
    }

    @Test
    public void testAuthenticationChannelUnknown() throws Exception {
        testAuthenticationChannelErrorCase(Status.BAD_REQUEST, Status.BAD_REQUEST,  null, OAuthErrorException.AUTHORIZATION_PENDING, Errors.INVALID_REQUEST);
    }

    @Test
    public void testInvalidConsumptionDeviceRegistration() throws Exception {
        try {
            createClientDynamically("invalid-CIBA-CD", (OIDCClientRepresentation clientRep) -> {
                clientRep.setBackchannelTokenDeliveryMode("pushpush");
            });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
    }

    @Test
    public void testCibaGrantDeactivated() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";

            // prepare CIBA settings with ciba grant deactivated
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
            attributes.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, "poll");
            attributes.put(CibaConfig.OIDC_CIBA_GRANT_ENABLED, null);
            attributes.put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, Algorithm.RS256);
            clientRep.setAttributes(attributes);
            clientResource.update(clientRep);
            //clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            Assert.assertNull(clientRep.getAttributes().get(CibaConfig.OIDC_CIBA_GRANT_ENABLED));
            Assert.assertThat(clientRep.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG), is(Algorithm.RS256));

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, "gilwekDe3", "acr2");
            assertThat(response.getStatusCode(), is(equalTo(400)));
            assertThat(response.getError(), is(OAuthErrorException.INVALID_GRANT));
            assertThat(response.getErrorDescription(), is("Client not allowed OIDC CIBA Grant"));

            // activate ciba grant
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            attributes = clientRep.getAttributes();
            attributes.put(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
            attributes.put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, Algorithm.ES256);
            clientRep.setAttributes(attributes);
            clientResource.update(clientRep);
            clientRep = clientResource.toRepresentation();
            Assert.assertThat(clientRep.getAttributes().get(CibaConfig.OIDC_CIBA_GRANT_ENABLED), is(Boolean.TRUE.toString()));
            Assert.assertThat(clientRep.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG), is(Algorithm.ES256));

            // user Backchannel Authentication Request
            response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, "Fkb4T3s");

            // user Authentication Channel Request
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest("Fkb4T3s");

            // user Authentication Channel completed
            doAuthenticationChannelCallback(authenticationChannelReq);

            // deactivate ciba grant
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            attributes = clientRep.getAttributes();
            attributes.put(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.FALSE.toString());
            attributes.put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, "none");
            clientRep.setAttributes(attributes);
            clientResource.update(clientRep);
            clientRep = clientResource.toRepresentation();
            Assert.assertThat(clientRep.getAttributes().get(CibaConfig.OIDC_CIBA_GRANT_ENABLED), is(Boolean.FALSE.toString()));
            Assert.assertThat(clientRep.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG), is("none"));

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(SECOND_TEST_CLIENT_NAME, SECOND_TEST_CLIENT_SECRET, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
            assertThat(tokenRes.getError(), is(OAuthErrorException.INVALID_GRANT));
            assertThat(tokenRes.getErrorDescription(), is("Client not allowed OIDC CIBA Grant"));
        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    @Test
    public void testCibaGrantSettingByDynamicClientRegistration() throws Exception {
        String clientId = createClientDynamically(generateSuffixedName("valid-CIBA-CD"), (OIDCClientRepresentation clientRep) -> {});
        OIDCClientRepresentation rep = getClientDynamically(clientId);
        Assert.assertFalse(rep.getGrantTypes().contains(OAuth2Constants.CIBA_GRANT_TYPE));
        Assert.assertNull(rep.getBackchannelAuthenticationRequestSigningAlg());
        updateClientDynamically(clientId, (OIDCClientRepresentation clientRep) -> {
            List<String> grantTypes = Optional.ofNullable(clientRep.getGrantTypes()).orElse(new ArrayList<>());
            grantTypes.add(OAuth2Constants.CIBA_GRANT_TYPE);
            clientRep.setGrantTypes(grantTypes);
            clientRep.setBackchannelAuthenticationRequestSigningAlg(Algorithm.PS256);
        });

        rep = getClientDynamically(clientId);
        Assert.assertTrue(rep.getGrantTypes().contains(OAuth2Constants.CIBA_GRANT_TYPE));
        Assert.assertThat(rep.getBackchannelAuthenticationRequestSigningAlg(), is(Algorithm.PS256));
    }

    @Test
    public void testBackchannelAuthenticationFlowWithSignedAuthenticationRequestParam() throws Exception {
        testBackchannelAuthenticationFlowWithSignedAuthenticationRequest(false, Algorithm.PS256);
    }

    @Test
    public void testBackchannelAuthenticationFlowWithSignedAuthenticationRequestUriParam() throws Exception {
        testBackchannelAuthenticationFlowWithSignedAuthenticationRequest(true, Algorithm.ES256);
    }

    @Test
    public void testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequestParam() throws Exception {
        testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequest(false, Algorithm.HS256, 400, "Signed algorithm is not allowed");
    }

    @Test
    public void testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequestUriParam() throws Exception {
        testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequest(true, "none", 400, "None signed algorithm is not allowed");
    }

    @Test
    public void testSecureCibaSessionEnforceExecutor() throws Exception {
        String clientId = createClientDynamically(generateSuffixedName("valid-CIBA-CD"), (OIDCClientRepresentation clientRep) -> {
            List<String> grantTypes = Optional.ofNullable(clientRep.getGrantTypes()).orElse(new ArrayList<>());
            grantTypes.add(OAuth2Constants.CIBA_GRANT_TYPE);
            clientRep.setGrantTypes(grantTypes);
        });
        OIDCClientRepresentation rep = getClientDynamically(clientId);
        String clientSecret = rep.getClientSecret();

        String username = "nutzername-rot";
        Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put("user_device", "mobile");

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                    .addExecutor(SecureCibaSessionEnforceExecutorFactory.PROVIDER_ID, null)
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, 
                        createAnyClientConditionConfig())
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        // user Backchannel Authentication Request
        AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(clientId, clientSecret, username, null, null, additionalParameters);
        assertThat(response.getStatusCode(), is(equalTo(400)));
        assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
        assertThat(response.getErrorDescription(), is("Missing parameter: binding_message"));
    }

    @Test
    public void testSecureCibaSessionEnforceExecutorWithSignedAuthenticationRequestParam() throws Exception {
        testSecureCibaSessionEnforceExecutor(false);
    }

    @Test
    public void testSecureCibaSessionEnforceExecutorWithSignedAuthenticationRequestUriParam() throws Exception {
        testSecureCibaSessionEnforceExecutor(true);
    }

    @Test
    public void testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequest() throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            boolean useRequestUri = false;
            String sigAlg = Algorithm.PS256;
            final String username = "nutzername-rot";
            String bindingMessage = "Flughafen-Frankfurt-am-Main";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // register profiles
            String json = (new ClientProfilesBuilder()).addProfile(
                    (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureCibaSignedAuthenticationRequestExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
                    ).toString();
            updateProfiles(json);

            // register policies
            json = (new ClientPoliciesBuilder()).addPolicy(
                    (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, 
                            createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
                    ).toString();
            updatePolicies(json);

            AuthorizationEndpointRequestObject requestObject = createPartialAuthorizationEndpointRequestObject(username, bindingMessage);
            requestObject.nbf(requestObject.getIat());

            registerSharedAuthenticationRequest(requestObject, TEST_CLIENT_NAME, sigAlg, useRequestUri);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage, null, null);
            assertThat(response.getStatusCode(), is(equalTo(400)));
            assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
            assertThat(response.getErrorDescription(), is("Missing parameter in the signed authentication request: exp"));

            useRequestUri = true;
            bindingMessage = "Flughafen-Wien-Schwechat";
            requestObject = createPartialAuthorizationEndpointRequestObject(username, bindingMessage);
            requestObject.exp(requestObject.getIat() + Long.valueOf(300));

            registerSharedAuthenticationRequest(requestObject, TEST_CLIENT_NAME, sigAlg, useRequestUri);

            // user Backchannel Authentication Request
            response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage, null, null);
            assertThat(response.getStatusCode(), is(equalTo(400)));
            assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
            assertThat(response.getErrorDescription(), is("Missing parameter in the signed authentication request: nbf"));

            useRequestUri = false;
            bindingMessage = "Stuttgart-Hauptbahnhof";
            requestObject = createPartialAuthorizationEndpointRequestObject(username, bindingMessage);
            requestObject.exp(requestObject.getIat() + SecureCibaSignedAuthenticationRequestExecutor.DEFAULT_AVAILABLE_PERIOD + 10);
            requestObject.nbf(requestObject.getIat());

            registerSharedAuthenticationRequest(requestObject, TEST_CLIENT_NAME, sigAlg, useRequestUri);

            // user Backchannel Authentication Request
            response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage, null, null);
            assertThat(response.getStatusCode(), is(equalTo(400)));
            assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
            assertThat(response.getErrorDescription(), is("signed authentication request's available period is long"));

            useRequestUri = true;
            bindingMessage = "Brno-hlavni-nadrazif";
            requestObject = createPartialAuthorizationEndpointRequestObject(username, bindingMessage);
            requestObject.exp(requestObject.getIat() + Long.valueOf(300));
            requestObject.nbf(requestObject.getIat());

            registerSharedAuthenticationRequest(requestObject, TEST_CLIENT_NAME, sigAlg, useRequestUri);

            // user Backchannel Authentication Request
            response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest(bindingMessage);
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
            assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));
            assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.SCOPE_OPENID)));

            // user Authentication Channel completed
            doAuthenticationChannelCallback(testRequest);

            // user Token Request
            doBackchannelAuthenticationTokenRequest(username, response.getAuthReqId());
        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    private AuthorizationEndpointRequestObject createPartialAuthorizationEndpointRequestObject(String username, String bindingMessage) throws Exception {
        AuthorizationEndpointRequestObject requestObject = new AuthorizationEndpointRequestObject();
        requestObject.id(org.keycloak.models.utils.KeycloakModelUtils.generateId());
        requestObject.iat(Long.valueOf(Time.currentTime()));
        requestObject.setScope("openid");
        requestObject.setMax_age(Integer.valueOf(600));
        requestObject.setOtherClaims("custom_claim_zwei", "gelb");
        requestObject.audience(Urls.realmIssuer(new URI(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"), TEST_REALM_NAME), "https://example.com");
        requestObject.setLoginHint(username);
        requestObject.setBindingMessage(bindingMessage);
        return requestObject;
    }

    private void testSecureCibaSessionEnforceExecutor(boolean useRequestUri) throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            String sigAlg = Algorithm.PS256;
            final String username = "nutzername-rot";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            AuthorizationEndpointRequestObject sharedAuthenticationRequest = createValidSharedAuthenticationRequest();
            sharedAuthenticationRequest.setLoginHint(username);
            registerSharedAuthenticationRequest(sharedAuthenticationRequest, TEST_CLIENT_NAME, sigAlg, useRequestUri);

            // register profiles
            String json = (new ClientProfilesBuilder()).addProfile(
                    (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(SecureCibaSessionEnforceExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
                    ).toString();
            updateProfiles(json);

            // register policies
            json = (new ClientPoliciesBuilder()).addPolicy(
                    (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, 
                            createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
                    ).toString();
            updatePolicies(json);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, null, null, null);
            assertThat(response.getStatusCode(), is(equalTo(400)));
            assertThat(response.getError(), is(OAuthErrorException.INVALID_REQUEST));
            assertThat(response.getErrorDescription(), is("Missing parameter: binding_message"));

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    private RealmResource testRealm() {
        return adminClient.realm(REALM_NAME);
    }

    @Test
    public void testBackchannelAuthenticationFlowRegisterDifferentSigAlgInAdvanceWithSignedAuthenticationRequestParam() throws Exception {
        testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequest(false, Algorithm.ES256, Algorithm.PS256, 400, OAuthErrorException.INVALID_REQUEST, "Client requested algorithm not registered in advance or request signed with different algorithm other than client requested algorithm", TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD);
    }

    @Test
    public void testBackchannelAuthenticationFlowRegisterDifferentSigAlgInAdvanceWithSignedAuthenticationRequestUriParam() throws Exception {
        testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequest(true, Algorithm.PS256, Algorithm.ES256, 400, OAuthErrorException.INVALID_REQUEST, "Client requested algorithm not registered in advance or request signed with different algorithm other than client requested algorithm", TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD);
    }

    @Test
    public void testBackchannelAuthenticationFlowNotRegisterSigAlgInAdvanceWithSignedAuthenticationRequestParam() throws Exception {
        testBackchannelAuthenticationFlowNotRegisterSigAlgInAdvanceWithSignedAuthentication("valid-CIBA-CD-Ein", false, null, Algorithm.ES256, 400, "Client requested algorithm not registered in advance or request signed with different algorithm other than client requested algorithm");
    }

    @Test
    public void testBackchannelAuthenticationFlowNotRegisterSigAlgInAdvanceWithSignedAuthenticationRequestUriParam() throws Exception {
        testBackchannelAuthenticationFlowNotRegisterSigAlgInAdvanceWithSignedAuthentication("valid-CIBA-CD-Zwei", true, null, Algorithm.PS256, 400, "Client requested algorithm not registered in advance or request signed with different algorithm other than client requested algorithm");
    }

    @Test
    public void testExtendedClientPolicyIntefacesForBackchannelAuthenticationRequest() throws Exception {
        String clientId = generateSuffixedName("confidential-app");
        String clientSecret = "app-secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
            clientRep.setBearerOnly(Boolean.FALSE);
            Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
            attributes.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, "poll");
            attributes.put(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
            clientRep.setAttributes(attributes);
        });

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                    .addExecutor(TestRaiseExeptionExecutorFactory.PROVIDER_ID, null)
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(clientId, clientSecret, TEST_USER_NAME, "Pjb9eD8w", null, null);
        assertEquals(400, response.getStatusCode());
        assertEquals(ClientPolicyEvent.BACKCHANNEL_AUTHENTICATION_REQUEST.toString(), response.getError());
        assertEquals("Exception thrown intentionally", response.getErrorDescription());
    }

    @Test
    public void testExtendedClientPolicyIntefacesForBackchannelTokenRequest() throws Exception {
        String clientId = generateSuffixedName("confidential-app");
        String clientSecret = "app-secret";
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setSecret(clientSecret);
            clientRep.setStandardFlowEnabled(Boolean.TRUE);
            clientRep.setImplicitFlowEnabled(Boolean.TRUE);
            clientRep.setPublicClient(Boolean.FALSE);
            clientRep.setBearerOnly(Boolean.FALSE);
            Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
            attributes.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, "poll");
            attributes.put(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
            clientRep.setAttributes(attributes);
        });

        final String bindingMessage = "BASTION";
        Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put("user_device", "mobile");

        // user Backchannel Authentication Request
        AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(clientId, clientSecret, TEST_USER_NAME, bindingMessage, null, additionalParameters);
        assertThat(response.getStatusCode(), is(equalTo(200)));
        Assert.assertNotNull(response.getAuthReqId());

        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        TestAuthenticationChannelRequest authenticationChannelReq = oidcClientEndpointsResource.getAuthenticationChannel(bindingMessage);
        int statusCode = oauth.doAuthenticationChannelCallback(authenticationChannelReq.getBearerToken(), SUCCEED);
        assertThat(statusCode, is(equalTo(200)));

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forste Profilen")
                    .addExecutor(TestRaiseExeptionExecutorFactory.PROVIDER_ID, null)
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                    .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(clientId, clientSecret, response.getAuthReqId());
        assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
        assertThat(tokenRes.getError(), is(OAuthErrorException.INVALID_GRANT));
        assertThat(tokenRes.getErrorDescription(), is("Exception thrown intentionally"));
    }

    @Test
    public void testSecureCibaAuthenticationRequestSigningAlgorithmEnforceExecutor() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                    .addExecutor(SecureCibaAuthenticationRequestSigningAlgorithmExecutorFactory.PROVIDER_ID, null)
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(

                (new ClientPolicyBuilder()).createPolicy(POLICY_NAME, "Den Forsta Policyn", Boolean.TRUE)
                    .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                        createClientUpdateContextConditionConfig(Arrays.asList(
                                ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER,
                                ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN,
                                ClientUpdaterContextConditionFactory.BY_REGISTRATION_ACCESS_TOKEN)))
                    .addProfile(PROFILE_NAME)
                    .toRepresentation()
                ).toString();
        updatePolicies(json);

        // create by Admin REST API - fail
        try {
            createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
                    clientRep.setSecret("secret");
                    clientRep.setAttributes(new HashMap<>());
                    clientRep.getAttributes().put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, "none");
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
        }

        // create by Admin REST API - success
        String cAppAdminId = createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, org.keycloak.crypto.Algorithm.ES256);
            });
        ClientRepresentation cRep = getClientByAdmin(cAppAdminId);
        assertEquals(org.keycloak.crypto.Algorithm.ES256, cRep.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG));

        // create by Admin REST API - success, PS256 enforced
        String cAppAdmin2Id = createClientByAdmin(generateSuffixedName("App-by-Admin2"), (ClientRepresentation client2Rep) -> {
            });
        ClientRepresentation cRep2 = getClientByAdmin(cAppAdmin2Id);
        assertEquals(org.keycloak.crypto.Algorithm.PS256, cRep2.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG));

        // update by Admin REST API - fail
        try {
            updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, org.keycloak.crypto.Algorithm.RS512);
            });
        } catch (ClientPolicyException cpe) {
            assertEquals(Errors.INVALID_REQUEST, cpe.getError());
        }
        cRep = getClientByAdmin(cAppAdminId);
        assertEquals(org.keycloak.crypto.Algorithm.ES256, cRep.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG));

        // update by Admin REST API - success
        updateClientByAdmin(cAppAdminId, (ClientRepresentation clientRep) -> {
                clientRep.setAttributes(new HashMap<>());
                clientRep.getAttributes().put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, org.keycloak.crypto.Algorithm.PS384);
        });
        cRep = getClientByAdmin(cAppAdminId);
        assertEquals(org.keycloak.crypto.Algorithm.PS384, cRep.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG));

        // update profiles, ES256 enforced
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                    .addExecutor(SecureCibaAuthenticationRequestSigningAlgorithmExecutorFactory.PROVIDER_ID,
                            createSecureCibaAuthenticationRequestSigningAlgorithmExecutorConfig(org.keycloak.crypto.Algorithm.ES256))
                    .toRepresentation()
                ).toString();

        updateProfiles(json);

        // update by Admin REST API - success
        updateClientByAdmin(cAppAdmin2Id, (ClientRepresentation client2Rep) -> {
                client2Rep.getAttributes().remove(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG);
        });
        cRep2 = getClientByAdmin(cAppAdmin2Id);
        assertEquals(org.keycloak.crypto.Algorithm.ES256, cRep2.getAttributes().get(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG));

        // update profiles, fall back to PS256
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                    .addExecutor(SecureCibaAuthenticationRequestSigningAlgorithmExecutorFactory.PROVIDER_ID,
                            createSecureCibaAuthenticationRequestSigningAlgorithmExecutorConfig(org.keycloak.crypto.Algorithm.RS512))
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // create dynamically - fail
        try {
            createClientByAdmin(generateSuffixedName("App-in-Dynamic"), (ClientRepresentation clientRep) -> {
                    clientRep.setSecret("secret");
                    clientRep.setAttributes(new HashMap<>());
                    clientRep.getAttributes().put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, org.keycloak.crypto.Algorithm.RS384);
                });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
        }

        // create dynamically - success
        String cAppDynamicClientId = createClientDynamically(generateSuffixedName("App-in-Dynamic"), (OIDCClientRepresentation clientRep) -> {
                clientRep.setBackchannelAuthenticationRequestSigningAlg(org.keycloak.crypto.Algorithm.ES256);
            });
        events.expect(EventType.CLIENT_REGISTER).client(cAppDynamicClientId).user(org.hamcrest.Matchers.isEmptyOrNullString()).assertEvent();

        // update dynamically - fail
        try {
            updateClientDynamically(cAppDynamicClientId, (OIDCClientRepresentation clientRep) -> {
                     clientRep.setBackchannelAuthenticationRequestSigningAlg(org.keycloak.crypto.Algorithm.RS256);
                 });
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }
        assertEquals(org.keycloak.crypto.Algorithm.ES256, getClientDynamically(cAppDynamicClientId).getBackchannelAuthenticationRequestSigningAlg());

        // update dynamically - success
        updateClientDynamically(cAppDynamicClientId, (OIDCClientRepresentation clientRep) -> {
                clientRep.setBackchannelAuthenticationRequestSigningAlg(org.keycloak.crypto.Algorithm.ES384);
            });
        assertEquals(org.keycloak.crypto.Algorithm.ES384, getClientDynamically(cAppDynamicClientId).getBackchannelAuthenticationRequestSigningAlg());

        // create dynamically - success, PS256 enforced
        restartAuthenticatedClientRegistrationSetting();
        String cAppDynamicClient2Id = createClientDynamically(generateSuffixedName("App-in-Dynamic"), (OIDCClientRepresentation client2Rep) -> {
            });
        OIDCClientRepresentation cAppDynamicClient2Rep = getClientDynamically(cAppDynamicClient2Id);
        assertEquals(org.keycloak.crypto.Algorithm.PS256, cAppDynamicClient2Rep.getBackchannelAuthenticationRequestSigningAlg());

        // update profiles, enforce ES256
        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(PROFILE_NAME, "Den Forsta Profilen")
                    .addExecutor(SecureCibaAuthenticationRequestSigningAlgorithmExecutorFactory.PROVIDER_ID,
                            createSecureCibaAuthenticationRequestSigningAlgorithmExecutorConfig(org.keycloak.crypto.Algorithm.ES256))
                    .toRepresentation()
                ).toString();
        updateProfiles(json);

        // update dynamically - success, ES256 enforced
        updateClientDynamically(cAppDynamicClient2Id, (OIDCClientRepresentation client2Rep) -> {
                client2Rep.setBackchannelAuthenticationRequestSigningAlg(null);
            });
        cAppDynamicClient2Rep = getClientDynamically(cAppDynamicClient2Id);
        assertEquals(org.keycloak.crypto.Algorithm.ES256, cAppDynamicClient2Rep.getBackchannelAuthenticationRequestSigningAlg());
    }

    private void testBackchannelAuthenticationFlowNotRegisterSigAlgInAdvanceWithSignedAuthentication(String clientName, boolean useRequestUri, String requestedSigAlg, String sigAlg, int statusCode, String errorDescription) throws Exception {
        String clientId = createClientDynamically(clientName, (OIDCClientRepresentation clientRep) -> {
            List<String> grantTypes = Optional.ofNullable(clientRep.getGrantTypes()).orElse(new ArrayList<>());
            grantTypes.add(OAuth2Constants.CIBA_GRANT_TYPE);
            clientRep.setGrantTypes(grantTypes);
        });
        OIDCClientRepresentation rep = getClientDynamically(clientId);
        String clientSecret = rep.getClientSecret();
        testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequest(useRequestUri, requestedSigAlg, sigAlg, statusCode, OAuthErrorException.INVALID_REQUEST, errorDescription, clientId, clientSecret);
    }

    private void testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequest(boolean useRequestUri, String sigAlg, int statusCode, String errorDescription) throws Exception {
        testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequest(useRequestUri, sigAlg, sigAlg, 400, OAuthErrorException.INVALID_REQUEST, errorDescription, TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD);
    }

    private void testBackchannelAuthenticationFlowWithInvalidSignedAuthenticationRequest(boolean useRequestUri, String requestedSigAlg, String sigAlg, int statusCode, String error, String errorDescription, String clientId, String clientSecret) throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), clientId);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            AuthorizationEndpointRequestObject sharedAuthenticationRequest = createValidSharedAuthenticationRequest();
            sharedAuthenticationRequest.setLoginHint(username);
            sharedAuthenticationRequest.setBindingMessage(bindingMessage);
            registerSharedAuthenticationRequest(sharedAuthenticationRequest, clientId, requestedSigAlg, sigAlg, useRequestUri, clientSecret);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(clientId, clientSecret, null, null, null);
            Assert.assertThat(response.getStatusCode(), is(equalTo(statusCode)));
            Assert.assertThat(response.getError(), is(error));
            Assert.assertThat(response.getErrorDescription(), is(errorDescription));
        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    protected void registerSharedInvalidAuthenticationRequest(AuthorizationEndpointRequestObject requestObject, String clientId, String sigAlg, boolean isUseRequestUri) throws URISyntaxException, IOException {
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // Set required signature for request_uri
        // use and set jwks_url
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        Map<String, String> attr = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
        attr.put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, sigAlg);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
        clientResource.update(clientRep);

        oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // register request object
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        oidcClientEndpointsResource.registerOIDCRequest(encodedRequestObject, sigAlg);

        if (isUseRequestUri) {
            oauth.request(null);
            oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        } else {
            oauth.requestUri(null);
            oauth.request(oidcClientEndpointsResource.getOIDCRequest());
        }
    }

    private void testBackchannelAuthenticationFlowWithSignedAuthenticationRequest(boolean useRequestUri, String sigAlg) throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";
            final String bindingMessage = "BASTION";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            AuthorizationEndpointRequestObject sharedAuthenticationRequest = createValidSharedAuthenticationRequest();
            sharedAuthenticationRequest.setLoginHint(username);
            sharedAuthenticationRequest.setBindingMessage(bindingMessage);
            registerSharedAuthenticationRequest(sharedAuthenticationRequest, TEST_CLIENT_NAME, sigAlg, useRequestUri);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, null, null);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest(bindingMessage);
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
            Assert.assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));
            Assert.assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.SCOPE_OPENID)));

            // user Authentication Channel completed
            EventRepresentation loginEvent = doAuthenticationChannelCallback(testRequest);
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String userId = loginEvent.getUserId();

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(username, response.getAuthReqId());

            // token introspection
            String tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // token refresh
            tokenRes = doRefreshTokenRequest(tokenRes.getRefreshToken(), username, sessionId, false);

            // token introspection after token refresh
            tokenResponse = doIntrospectAccessTokenWithClientCredential(tokenRes, username);

            // logout by refresh token
            EventRepresentation logoutEvent = doLogoutByRefreshToken(tokenRes.getRefreshToken(), sessionId, userId, false);

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    private AuthorizationEndpointRequestObject createValidSharedAuthenticationRequest() throws URISyntaxException {
        AuthorizationEndpointRequestObject requestObject = new AuthorizationEndpointRequestObject();
        requestObject.id(org.keycloak.models.utils.KeycloakModelUtils.generateId());
        requestObject.iat(Long.valueOf(Time.currentTime()));
        requestObject.exp(requestObject.getIat() + Long.valueOf(300));
        requestObject.nbf(requestObject.getIat());
        requestObject.setScope("openid");
        requestObject.setMax_age(Integer.valueOf(600));
        requestObject.setOtherClaims("custom_claim_zwei", "gelb");
        requestObject.audience(Urls.realmIssuer(new URI(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"), TEST_REALM_NAME), "https://example.com");
        return requestObject;
    }

    protected void registerSharedAuthenticationRequest(AuthorizationEndpointRequestObject requestObject, String clientId, String sigAlg, boolean isUseRequestUri) throws URISyntaxException, IOException {
        registerSharedAuthenticationRequest(requestObject, clientId, sigAlg, isUseRequestUri, null);
    }

    protected void registerSharedAuthenticationRequest(AuthorizationEndpointRequestObject requestObject, String clientId, String sigAlg, boolean isUseRequestUri, String clientSecret) throws URISyntaxException, IOException {
        registerSharedAuthenticationRequest(requestObject, clientId, sigAlg, sigAlg, isUseRequestUri, clientSecret);
    }

    private boolean isSymmetricSigAlg(String sigAlg) {
        if (Algorithm.HS256.equals(sigAlg)) return true;
        if (Algorithm.HS384.equals(sigAlg)) return true;
        if (Algorithm.HS512.equals(sigAlg)) return true;
        return false;
    }

    protected void registerSharedAuthenticationRequest(AuthorizationEndpointRequestObject requestObject, String clientId, String requestedSigAlg, String sigAlg, boolean isUseRequestUri, String clientSecret) throws URISyntaxException, IOException {
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // Set required signature for request_uri
        // use and set jwks_url
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        if (requestedSigAlg != null) {
            Map<String, String> attr = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
            attr.put(CibaConfig.CIBA_BACKCHANNEL_AUTH_REQUEST_SIGNING_ALG, requestedSigAlg);
            clientRep.setAttributes(attr);
        }
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
        clientResource.update(clientRep);

        oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // register request object
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        if (isSymmetricSigAlg(sigAlg)) {
            oidcClientEndpointsResource.registerOIDCRequestSymmetricSig(encodedRequestObject, sigAlg, clientSecret);
        } else {
            // generate and register client keypair
            if (!"none".equals(sigAlg)) oidcClientEndpointsResource.generateKeys(sigAlg);

            oidcClientEndpointsResource.registerOIDCRequest(encodedRequestObject, sigAlg);
        }

        if (isUseRequestUri) {
            oauth.request(null);
            oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        } else {
            oauth.requestUri(null);
            oauth.request(oidcClientEndpointsResource.getOIDCRequest());
        }
    }

    private void testAuthenticationChannelErrorCase(Status statusCallback, Status statusTokenEndpont, AuthenticationChannelResponse.Status authStatus, String error, String errorEvent) throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, "kwq26rfjs73");

            // user Authentication Channel Request
            TestAuthenticationChannelRequest authenticationChannelReq = doAuthenticationChannelRequest("kwq26rfjs73");

            // user Authentication Channel completed
            doAuthenticationChannelCallbackError(statusCallback, TEST_CLIENT_NAME, authenticationChannelReq, authStatus, username, errorEvent);

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(TEST_CLIENT_PASSWORD, response.getAuthReqId());
            assertThat(tokenRes.getStatusCode(), is(equalTo(statusTokenEndpont.getStatusCode())));
            assertThat(tokenRes.getError(), is(error));

        } finally {
            revertCIBASettings(clientResource, clientRep);
        }
    }

    private void prepareCIBASettings(ClientResource clientResource, ClientRepresentation clientRep) {
        Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
        attributes.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, "poll");
        attributes.put(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
        clientRep.setAttributes(attributes);
        List<String> requestUris = new ArrayList<>(OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).getRequestUris());
        requestUris.add(TestApplicationResourceUrls.clientRequestUri());
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(requestUris);
        clientResource.update(clientRep);
    }

    private void revertCIBASettings(ClientResource clientResource, ClientRepresentation clientRep) {
        Map<String, String> attributes = Optional.ofNullable(clientRep.getAttributes()).orElse(new HashMap<>());
        attributes.remove(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT);
        attributes.remove(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
        clientRep.setAttributes(attributes);
        List<String> requestUris = new ArrayList<>(OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).getRequestUris());
        requestUris.remove(TestApplicationResourceUrls.clientRequestUri());
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(requestUris);
        clientResource.update(clientRep);
    }

    private RealmRepresentation backupCIBAPolicy() {
        RealmRepresentation rep = testRealm().toRepresentation();
        Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
        cibaBackchannelTokenDeliveryMode = attrMap.get(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE);
        cibaExpiresIn = Integer.parseInt(attrMap.get(CibaConfig.CIBA_EXPIRES_IN));
        cibaInterval = Integer.parseInt(attrMap.get(CibaConfig.CIBA_INTERVAL));
        cibaAuthRequestedUserHint = attrMap.get(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT);
        return rep;
    }

    private void restoreCIBAPolicy() {
        RealmRepresentation rep = testRealm().toRepresentation();
        Map<String, String> attrMap = Optional.ofNullable(rep.getAttributes()).orElse(new HashMap<>());
        attrMap.put(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE, cibaBackchannelTokenDeliveryMode);
        attrMap.put(CibaConfig.CIBA_EXPIRES_IN, String.valueOf(cibaExpiresIn));
        attrMap.put(CibaConfig.CIBA_INTERVAL, String.valueOf(cibaInterval));
        attrMap.put(CibaConfig.CIBA_AUTH_REQUESTED_USER_HINT, cibaAuthRequestedUserHint);
        rep.setAttributes(attrMap);
        testRealm().update(rep);
    }

    private AuthenticationRequestAcknowledgement doBackchannelAuthenticationRequest(String clientId, String clientSecret, String username, String bindingMessage) throws Exception {
        return doBackchannelAuthenticationRequest(clientId, clientSecret, username, bindingMessage, null);
    }

    private AuthenticationRequestAcknowledgement doBackchannelAuthenticationRequest(String clientId, String clientSecret, String username, String bindingMessage, Map<String, String> additionalParameters) throws Exception {
        AuthenticationRequestAcknowledgement response = oauth.doBackchannelAuthenticationRequest(clientId, clientSecret, username, bindingMessage, null, additionalParameters);
        assertThat(response.getStatusCode(), is(equalTo(200)));
        Assert.assertNotNull(response.getAuthReqId());
        return response;
    }

    private TestAuthenticationChannelRequest doAuthenticationChannelRequest(String bindingMessage) {
        // get Authentication Channel Request keycloak has done on Backchannel Authentication Endpoint from the FIFO queue of testing Authentication Channel Request API
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        TestAuthenticationChannelRequest authenticationChannelReq = oidcClientEndpointsResource.getAuthenticationChannel(bindingMessage);
        return authenticationChannelReq;
    }

    private EventRepresentation doAuthenticationChannelCallback(TestAuthenticationChannelRequest request) throws Exception {
        int statusCode = oauth.doAuthenticationChannelCallback(request.getBearerToken(), SUCCEED);
        assertThat(statusCode, is(equalTo(200)));
        // check login event : ignore user id and other details except for username
        EventRepresentation representation = new EventRepresentation();

        representation.setDetails(Collections.emptyMap());

        return representation;
    }

    private EventRepresentation doAuthenticationChannelCallbackError(Status status, String clientId, TestAuthenticationChannelRequest authenticationChannelReq, AuthenticationChannelResponse.Status authStatus, String username, String error) throws Exception {
        int statusCode = oauth.doAuthenticationChannelCallback(authenticationChannelReq.getBearerToken(), authStatus);
        assertThat(statusCode, is(equalTo(status.getStatusCode())));
        return events.expect(EventType.LOGIN_ERROR).clearDetails().client(clientId).error(error).user((String)null).session(CoreMatchers.nullValue(String.class)).assertEvent();
    }

    private OAuthClient.AccessTokenResponse doBackchannelAuthenticationTokenRequest(String username, String authReqId) throws Exception {
        return doBackchannelAuthenticationTokenRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, authReqId);
    }

    private OAuthClient.AccessTokenResponse doBackchannelAuthenticationTokenRequest(String clientId, String clientSecret, String username, String authReqId) throws Exception {
        OAuthClient.AccessTokenResponse tokenRes = oauth.doBackchannelAuthenticationTokenRequest(clientId, clientSecret, authReqId);
        assertThat(tokenRes.getStatusCode(), is(equalTo(200)));
        EventRepresentation event = events.expectAuthReqIdToToken(null, null).clearDetails().user(AssertEvents.isUUID()).client(clientId).assertEvent();

        AccessToken accessToken = oauth.verifyToken(tokenRes.getAccessToken());
        assertThat(accessToken.getIssuedFor(), is(equalTo(clientId)));

        RefreshToken refreshToken = oauth.parseRefreshToken(tokenRes.getRefreshToken());
        assertThat(refreshToken.getIssuedFor(), is(equalTo(clientId)));
        assertThat(refreshToken.getAudience()[0], is(equalTo(refreshToken.getIssuer())));

        IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
        assertThat(idToken.getPreferredUsername(), is(equalTo(username)));
        assertThat(idToken.getIssuedFor(), is(equalTo(clientId)));
        assertThat(idToken.getAudience()[0], is(equalTo(idToken.getIssuedFor())));

        return tokenRes;
    }

    private String doIntrospectAccessTokenWithClientCredential(OAuthClient.AccessTokenResponse tokenRes, String username) throws IOException {
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, tokenRes.getAccessToken());
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);
        assertThat(jsonNode.get("active").asBoolean(), is(equalTo(true)));
        assertThat(jsonNode.get("username").asText(), is(equalTo(username)));
        assertThat(jsonNode.get("client_id").asText(), is(equalTo(TEST_CLIENT_NAME)));
        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);
        assertThat(rep.isActive(), is(equalTo(true)));
        assertThat(rep.getClientId(), is(equalTo(TEST_CLIENT_NAME)));
        assertThat(rep.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        events.expect(EventType.INTROSPECT_TOKEN).user((String) null).clearDetails().assertEvent();

        tokenResponse = oauth.introspectAccessTokenWithClientCredential(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, tokenRes.getRefreshToken());
        jsonNode = objectMapper.readTree(tokenResponse);
        assertThat(jsonNode.get("active").asBoolean(), is(equalTo(true)));
        assertThat(jsonNode.get("client_id").asText(), is(equalTo(TEST_CLIENT_NAME)));
        rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);
        assertThat(rep.isActive(), is(equalTo(true)));
        assertThat(rep.getClientId(), is(equalTo(TEST_CLIENT_NAME)));
        assertThat(rep.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        assertThat(rep.getAudience()[0], is(equalTo(rep.getIssuer())));
        events.expect(EventType.INTROSPECT_TOKEN).user((String) null).clearDetails().assertEvent();

        tokenResponse = oauth.introspectAccessTokenWithClientCredential(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, tokenRes.getIdToken());
        jsonNode = objectMapper.readTree(tokenResponse);
        assertThat(jsonNode.get("active").asBoolean(), is(equalTo(true)));
        assertThat(jsonNode.get("client_id").asText(), is(equalTo(TEST_CLIENT_NAME)));
        rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);
        assertThat(rep.isActive(), is(equalTo(true)));
        assertThat(rep.getUserName(), is(equalTo(username)));
        assertThat(rep.getClientId(), is(equalTo(TEST_CLIENT_NAME)));
        assertThat(rep.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        assertThat(rep.getPreferredUsername(), is(equalTo(username)));
        assertThat(rep.getAudience()[0], is(equalTo(rep.getIssuedFor())));
        events.expect(EventType.INTROSPECT_TOKEN).user((String) null).clearDetails().assertEvent();

        return tokenResponse;
    }

    private OAuthClient.AccessTokenResponse doRefreshTokenRequest(String oldRefreshToken, String username, String sessionId, boolean isOfflineAccess) {
        OAuthClient.AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(oldRefreshToken, TEST_CLIENT_PASSWORD);
        assertThat(tokenRes.getStatusCode(), is(equalTo(200)));

        AccessToken accessToken = oauth.verifyToken(tokenRes.getAccessToken());
        assertThat(accessToken.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        checkTokenExpiration(accessToken, tokenRes.getExpiresIn());

        RefreshToken refreshToken = oauth.parseRefreshToken(tokenRes.getRefreshToken());
        assertThat(refreshToken.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        assertThat(refreshToken.getAudience()[0], is(equalTo(refreshToken.getIssuer())));
        if (!isOfflineAccess) checkTokenExpiration(refreshToken, tokenRes.getRefreshExpiresIn());

        IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
        assertThat(idToken.getPreferredUsername(), is(equalTo(username)));
        assertThat(idToken.getIssuedFor(), is(equalTo(TEST_CLIENT_NAME)));
        assertThat(idToken.getAudience()[0], is(equalTo(idToken.getIssuedFor())));
        checkTokenExpiration(idToken, tokenRes.getExpiresIn());

        events.expectRefresh(tokenRes.getRefreshToken(), sessionId).session(CoreMatchers.notNullValue(String.class)).user(AssertEvents.isUUID()).clearDetails().assertEvent();

        return tokenRes;
    }

    // KEYCLOAK-18391
    private void checkTokenExpiration(JsonWebToken token, long expiresIn) {
        assertThat(token, notNullValue());

        final Long tokenExp = token.getExp();
        final Long tokenIat = token.getIat();

        assertThat(tokenExp, notNullValue());
        assertThat(tokenIat, notNullValue());

        assertExpiration(tokenExp, tokenIat + expiresIn);
    }

    private EventRepresentation doLogoutByRefreshToken(String refreshToken, String sessionId, String userId, boolean isOfflineAccess) throws IOException {
        try (CloseableHttpResponse res = oauth.doLogout(refreshToken, TEST_CLIENT_PASSWORD)) {
            assertThat(res, Matchers.statusCodeIsHC(Status.NO_CONTENT));
        }

        // confirm logged out
        OAuthClient.AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(refreshToken, TEST_CLIENT_PASSWORD);
        assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
        assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.INVALID_GRANT)));
        if (isOfflineAccess) assertThat(tokenRes.getErrorDescription(), is(equalTo("Offline user session not found")));
        else assertThat(tokenRes.getErrorDescription(), is(equalTo("Session not active")));

        return events.expectLogout(sessionId).client(TEST_CLIENT_NAME).user(AssertEvents.isUUID()).session(AssertEvents.isUUID()).clearDetails().assertEvent();
    }

    private EventRepresentation doTokenRevokeByRefreshToken(String refreshToken, String sessionId, String userId, boolean isOfflineAccess) throws IOException {
        try (CloseableHttpResponse res = oauth.doTokenRevoke(refreshToken, "refresh_token", TEST_CLIENT_PASSWORD)) {
            assertThat(res, Matchers.statusCodeIsHC(Status.OK));
        }

        // confirm revocation
        OAuthClient.AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(refreshToken, TEST_CLIENT_PASSWORD);
        assertThat(tokenRes.getStatusCode(), is(equalTo(400)));
        assertThat(tokenRes.getError(), is(equalTo(OAuthErrorException.INVALID_GRANT)));
        if (isOfflineAccess) assertThat(tokenRes.getErrorDescription(), is(equalTo("Offline user session not found")));
        else assertThat(tokenRes.getErrorDescription(), is(equalTo("Session not active")));

        return events.expect(EventType.REVOKE_GRANT).clearDetails().client(TEST_CLIENT_NAME).user(AssertEvents.isUUID()).assertEvent();
    }

    private void testBackchannelAuthenticationFlow(boolean isOfflineAccess) throws Exception {
        testBackchannelAuthenticationFlow(isOfflineAccess, "BASTION");
    }

    private void testBackchannelAuthenticationFlow(boolean isOfflineAccess, String bindingMessage) throws Exception {
        ClientResource clientResource = null;
        ClientRepresentation clientRep = null;
        try {
            final String username = "nutzername-rot";
            Map<String, String> additionalParameters = new HashMap<>();
            additionalParameters.put("user_device", "mobile");

            // prepare CIBA settings
            clientResource = ApiUtil.findClientByClientId(adminClient.realm(TEST_REALM_NAME), TEST_CLIENT_NAME);
            assertThat(clientResource, notNullValue());

            clientRep = clientResource.toRepresentation();
            prepareCIBASettings(clientResource, clientRep);
            if (isOfflineAccess) oauth.scope(OAuth2Constants.OFFLINE_ACCESS);

            long startTime = Time.currentTime();

            // user Backchannel Authentication Request
            AuthenticationRequestAcknowledgement response = doBackchannelAuthenticationRequest(TEST_CLIENT_NAME, TEST_CLIENT_PASSWORD, username, bindingMessage, additionalParameters);

            // user Authentication Channel Request
            TestAuthenticationChannelRequest testRequest = doAuthenticationChannelRequest(bindingMessage);
            AuthenticationChannelRequest authenticationChannelReq = testRequest.getRequest();
            assertThat(authenticationChannelReq.getBindingMessage(), is(equalTo(bindingMessage)));
            if (isOfflineAccess) assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.OFFLINE_ACCESS)));
            assertThat(authenticationChannelReq.getScope(), is(containsString(OAuth2Constants.SCOPE_OPENID)));
            assertThat(authenticationChannelReq.getAdditionalParameters().get("user_device"), is(equalTo("mobile")));

            // user Authentication Channel completed
            EventRepresentation loginEvent = doAuthenticationChannelCallback(testRequest);
            String sessionId = loginEvent.getSessionId();
            String codeId = loginEvent.getDetails().get(Details.CODE_ID);
            String userId = loginEvent.getUserId();

            // user Token Request
            OAuthClient.AccessTokenResponse tokenRes = doBackchannelAuthenticationTokenRequest(username, response.getAuthReqId());
            IDToken idToken = oauth.verifyIDToken(tokenRes.getIdToken());
            long currentTime = Time.currentTime();
            long authTime = idToken.getAuth_time().longValue();
            assertTrue(startTime -5 <= authTime);
            assertTrue(authTime <= currentTime + 5);

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
}
