/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.tests.oidc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.constants.ServiceAccountConstants;
import org.keycloak.common.util.MultivaluedHashMap;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.encode.AccessTokenContext;
import org.keycloak.protocol.oidc.grants.AuthorizationCodeGrantTypeFactory;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.RoleNameMapper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.protocol.oidc.mappers.SessionStateMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ComponentExportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.userprofile.config.UPConfig;
import org.keycloak.representations.userprofile.config.UPConfig.UnmanagedAttributePolicy;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.UseLightweightAccessTokenExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.GroupBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.tests.client.policies.AbstractClientPoliciesTest;
import org.keycloak.tests.utils.admin.AdminApiUtil;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;
import org.keycloak.testsuite.util.runonserver.RunHelpers;
import org.keycloak.userprofile.UserProfileProvider;
import org.keycloak.userprofile.config.UPConfigUtils;
import org.keycloak.util.JsonSerialization;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.protocol.ProtocolMapperUtils.USER_SESSION_NOTE;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.ACR;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.ACR_SCOPE;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.ADDRESS;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.ALLOWED_WEB_ORIGINS;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.AUDIENCE_RESOLVE;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.CLIENT_ROLES;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.EMAIL;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.EMAIL_VERIFIED;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.FAMILY_NAME;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.FULL_NAME;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.GIVEN_NAME;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.PROFILE_CLAIM;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.REALM_ROLES;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.ROLES_SCOPE;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.USERNAME;
import static org.keycloak.protocol.oidc.OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE;
import static org.keycloak.protocol.oidc.mappers.AbstractPairwiseSubMapper.PROVIDER_ID_SUFFIX;
import static org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper.INCLUDED_CLIENT_AUDIENCE;
import static org.keycloak.protocol.oidc.mappers.HardcodedClaim.CLAIM_VALUE;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_INTROSPECTION;
import static org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper.INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN;
import static org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT;
import static org.keycloak.protocol.oidc.mappers.RoleNameMapper.NEW_ROLE_NAME;
import static org.keycloak.protocol.oidc.mappers.RoleNameMapper.ROLE_CONFIG;
import static org.keycloak.userprofile.DeclarativeUserProfileProvider.UP_COMPONENT_CONFIG_KEY;

import static org.junit.jupiter.api.Assertions.assertEquals;

@KeycloakIntegrationTest
public class LightWeightAccessTokenTest extends AbstractClientPoliciesTest {

    @InjectRealm(config = LightweightRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm managedRealm;

    @InjectRealm(ref = "master", attachTo = "master")
    ManagedRealm managedMasterRealm;

    @InjectRunOnServer(ref = "master", realmRef = "master")
    RunOnServerClient runOnServerMaster;

    @InjectOAuthClient(ref = "lightweight")
    OAuthClient oauth;


    private static final Logger logger = Logger.getLogger(LightWeightAccessTokenTest.class);

    private static final String RESOURCE_SERVER_CLIENT_ID = "resource-server";
    private static final String RESOURCE_SERVER_CLIENT_PASSWORD = "password";
    private static final String RESOURCE_SERVER_ROLE = "rs-role";


    private ClientResource testClient() {
        return AdminApiUtil.findClientByClientId(managedRealm.admin(), TEST_CLIENT);
    }

    private void setClientAttribute(String key, String value) {
        managedRealm.updateClientWithCleanup(TEST_CLIENT, client -> client.attribute(key, value));
    }


    @Test
    public void accessTokenFalseIntrospectionTrueTest() throws IOException {
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            oauth.scope("address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, false, false);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String tokenResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void accessTokenTrueIntrospectionFalseTest() throws IOException {
        ProtocolMappersResource protocolMappers = setProtocolMappers(true, false, true);
        try {
            oauth.scope("address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, true, false);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String tokenResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + tokenResponse);
            // Most of the claims should not be included in introspectionResponse as introspectionMapper was disabled
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, false, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void accessTokenTrueIntrospectionTrueTest() throws IOException {
        ProtocolMappersResource protocolMappers = setProtocolMappers(true, true, true);
        try {
            oauth.scope("address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, true, false);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String tokenResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void accessTokenTrueIntrospectionReturnedAsJwt() throws IOException {
        ProtocolMappersResource protocolMappers = setProtocolMappers(true, true, true);
        try {
            oauth.scope("address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, true, false);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);

            String tokenResponse = oauth.introspectionRequest(accessToken).tokenTypeHint("access_token").jwtResponse().send().getRaw();
            logger.debug("tokenResponse:" + tokenResponse);
            AccessToken introspectionResult = JsonSerialization.readValue(tokenResponse, AccessToken.class);
            assertTokenIntrospectionResponse(introspectionResult, true, true, false);

            Assertions.assertNotNull(introspectionResult.getOtherClaims().get("jwt"));
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void offlineTokenTest() throws IOException {
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            oauth.scope("openid address offline_access");

            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            TokenResponseContext ctx = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            AccessTokenResponse response = ctx.tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);
            logger.debug("idtoken:" + response.getIdToken());
            assertAccessToken(oauth.verifyToken(accessToken), true, false, false);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            removeSession(ctx.userSessionId);
            String tokenResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void clientCredentialTest() throws Exception {
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, false);
        try {
            oauth.scope("address");

            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), false, false, false);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String tokenResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), false, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void exchangeTest() throws Exception {
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            oauth.scope("address");

            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, false, false);
            response = oauth.doTokenExchange(accessToken);
            String exchangedTokenString = response.getAccessToken();
            logger.debug("exchangedTokenString:" + exchangedTokenString);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String tokenResponse = oauth.doIntrospectionAccessTokenRequest(exchangedTokenString).getRaw();
            logger.debug("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void testPolicyLightWeightFalseTest() throws Exception {
        setUseLightweightAccessTokenExecutor();
        ProtocolMappersResource protocolMappers = setProtocolMappers(true, true, false, false);
        try {
            oauth.scope("address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);

            AuthorizationEndpointResponse authsEndpointResponse = oauth.loginForm().nonce("123456").doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode());
            String accessToken = tokenResponse.getAccessToken();
            assertAccessToken(oauth.verifyToken(accessToken), true, false, true);
            logger.debug("lightweight access token:" + accessToken);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String introspectResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            assertTokenIntrospectionResponse(JsonSerialization.readValue(introspectResponse, AccessToken.class), true, true, false);
            logger.debug("tokenResponse:" + introspectResponse);

            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            managedRealm.updateClientPolicy(Collections.emptyList());
            oauth.doLogout(tokenResponse.getRefreshToken());

            authsEndpointResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode());
            accessToken = tokenResponse.getAccessToken();
            logger.debug("access token:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void testPolicyLightWeightTrueTest() throws Exception {
        setUseLightweightAccessTokenExecutor();
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true, false);
        try {
            oauth.scope("address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);

            AuthorizationEndpointResponse authsEndpointResponse = oauth.loginForm().nonce("123456").doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode());
            String accessToken = tokenResponse.getAccessToken();
            logger.debug("access token:" + accessToken);
            AccessToken token = oauth.verifyToken(accessToken);
            assertAccessToken(token, true, true, true);

            assertTokenContext(token.getId(), AccessTokenContext.SessionType.ONLINE, AccessTokenContext.TokenType.LIGHTWEIGHT, AuthorizationCodeGrantTypeFactory.GRANT_SHORTCUT);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String introspectResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + introspectResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(introspectResponse, AccessToken.class), true, true, false);

        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void testAlwaysUseLightWeightFalseTest() throws Exception {
        alwaysUseLightWeightAccessToken(true);
        ProtocolMappersResource protocolMappers = setProtocolMappers(true, true, false, false);
        try {
            oauth.scope("address");

            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);

            AuthorizationEndpointResponse authsEndpointResponse = oauth.loginForm().nonce("123456").doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode());
            String accessToken = tokenResponse.getAccessToken();
            assertAccessToken(oauth.verifyToken(accessToken), true, false, true);
            logger.debug("lightweight access token:" + accessToken);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String introspectResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            assertTokenIntrospectionResponse(JsonSerialization.readValue(introspectResponse, AccessToken.class), true, true, false);
            logger.debug("tokenResponse:" + introspectResponse);

            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            alwaysUseLightWeightAccessToken(false);
            oauth.doLogout(tokenResponse.getRefreshToken());

            authsEndpointResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode());
            accessToken = tokenResponse.getAccessToken();
            logger.debug("access token:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void testAlwaysUseLightWeightTrueTest() throws Exception {
        alwaysUseLightWeightAccessToken(true);
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true, false);
        try {
            oauth.scope("address");

            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);

            AuthorizationEndpointResponse authsEndpointResponse = oauth.loginForm().nonce("123456").doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode());
            String accessToken = tokenResponse.getAccessToken();
            logger.debug("access token:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, true, true);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String introspectResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + introspectResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(introspectResponse, AccessToken.class), true, true, false);

        } finally {
            deleteProtocolMappers(protocolMappers);
            alwaysUseLightWeightAccessToken(false);
        }
    }

    @Test
    public void testWithoutBasicClaim() throws Exception {
        alwaysUseLightWeightAccessToken(true);
        removeDefaultBasicClientScope();
        ProtocolMappersResource protocolMappers = setProtocolMappers(true, true, false, false);
        try {
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            oauth.scope("address");

            AuthorizationEndpointResponse authsEndpointResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode());
            String accessToken = tokenResponse.getAccessToken();
            logger.debug("access token:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true,  false,true);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String introspectResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + introspectResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(introspectResponse, AccessToken.class), true, true, true);

            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            alwaysUseLightWeightAccessToken(false);
            oauth.doLogout(tokenResponse.getRefreshToken());


            authsEndpointResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
            tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode());
            accessToken = tokenResponse.getAccessToken();
            logger.debug("access token:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, true,  true);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            introspectResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + introspectResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(introspectResponse, AccessToken.class), true, true, true);

        } finally {
            deleteProtocolMappers(protocolMappers);
            addDefaultBasicClientScope();
        }
    }

    @Test
    public void clientCredentialWithoutBasicClaims() throws Exception {
        removeDefaultBasicClientScope();
        alwaysUseLightWeightAccessToken(true);
        try {
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), false,  false,false);

            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String tokenResponse = oauth.doIntrospectionAccessTokenRequest(accessToken).getRaw();
            logger.debug("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), false, true, false);
        } finally {
            addDefaultBasicClientScope();
            alwaysUseLightWeightAccessToken(false);
        }
    }

    @Test
    public void testAdminConsoleClientWithLightweightAccessToken() {
        clearKeycloakSsoCookies();
        oauth.realm("master");
        oauth.client(Constants.ADMIN_CONSOLE_CLIENT_ID, TEST_CLIENT_SECRET);
        oauth.redirectUri(oauth.getBaseUrl() + "/admin/master/console");
        PkceGenerator pkce = PkceGenerator.s256();

        AuthorizationEndpointResponse authsEndpointResponse = oauth.loginForm().codeChallenge(pkce).doLogin("admin", "admin");
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authsEndpointResponse.getCode()).codeVerifier(pkce).send();
        String accessToken = tokenResponse.getAccessToken();
        logger.debug("access token:" + accessToken);
        assertBasicClaims(oauth.verifyToken(accessToken), true, true);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(oauth.getBaseUrl() + "/admin/realms/master");
            get.setHeader("Authorization", "Bearer " + accessToken);
            try (CloseableHttpResponse response = client.execute(get)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                RealmRepresentation realmRepresentation = JsonSerialization.readValue(response.getEntity().getContent(), RealmRepresentation.class);
                Assertions.assertEquals("master", realmRepresentation.getRealm());
            }
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testAdminConsoleClientWithLightweightAccessTokenTransientSessionParameterizedScopes() throws Exception {
        setClientAttribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, Boolean.TRUE.toString());
        oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
        AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest();
        String accessToken = response.getAccessToken();
        logger.debug("access token:" + accessToken);
        assertBasicClaims(oauth.verifyToken(accessToken), false, false);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpPost post = new HttpPost(oauth.getBaseUrl() + "/admin/realms");
            post.setHeader("Authorization", "Bearer " + accessToken);
            post.setEntity(new StringEntity("{\"realm\":\"invalid\",\"enabled\":true}", ContentType.APPLICATION_JSON));
            try (CloseableHttpResponse resp = client.execute(post)) {
                Assertions.assertEquals(Response.Status.FORBIDDEN.getStatusCode(), resp.getStatusLine().getStatusCode());
            }
        }
    }

    @Test
    public void testAdminApiWithLightweightAccessTokenAndTransientSession() {
        RealmResource masterRealm = managedMasterRealm.admin();
        ClientRepresentation transientClientRep = ClientBuilder.create("transient_client")
                .secret("password")
                .serviceAccountsEnabled(true)
                .attribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, String.valueOf(true))
                .build();
        String transientClientUuid = ApiUtil.getCreatedId(masterRealm.clients().create(transientClientRep));
        managedMasterRealm.cleanup().add(r -> r.clients().get(transientClientUuid).remove());

        String serviceAccountUserId = masterRealm.clients().get(transientClientUuid).toRepresentation().getServiceAccountUserId();
        masterRealm.users().get(serviceAccountUserId).roles().realmLevel().add(Collections.singletonList(masterRealm.roles().get(AdminRoles.ADMIN).toRepresentation()));
        try {
            oauth.realm("master");
            oauth.client(transientClientRep.getClientId(), transientClientRep.getSecret());
            AccessTokenResponse tokenResponse = oauth.doClientCredentialsGrantAccessTokenRequest();
            String accessTokenString = tokenResponse.getAccessToken();
            Assertions.assertNull(tokenResponse.getRefreshToken());
            AccessToken accessToken = oauth.verifyToken(accessTokenString);
            Assertions.assertNotNull(accessToken.getSubject());
            Assertions.assertNull(accessToken.getSessionId());

            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpGet get = new HttpGet(oauth.getBaseUrl() + "/admin/realms/master");
            get.setHeader("Authorization", "Bearer " + accessTokenString);
            CloseableHttpResponse response = client.execute(get);
            Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
            RealmRepresentation realmRepresentation = JsonSerialization.readValue(response.getEntity().getContent(), RealmRepresentation.class);
            Assertions.assertEquals("master", realmRepresentation.getRealm());

        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testAdminApiWithLightweightAccessAndSubClaim() {
        // The admin console token is issued by the master realm, so the "sub" mapper must be changed there
        setScopeProtocolMapper(managedMasterRealm.admin(), OIDCLoginProtocolFactory.BASIC_SCOPE, "sub", true, false, true);
        managedMasterRealm.cleanup().add(r ->
                setScopeProtocolMapper(r, OIDCLoginProtocolFactory.BASIC_SCOPE, "sub", true, false, false));

        clearKeycloakSsoCookies();
        oauth.realm("master");
        oauth.client(Constants.ADMIN_CONSOLE_CLIENT_ID, TEST_CLIENT_SECRET);
        oauth.redirectUri(oauth.getBaseUrl() + "/admin/master/console");
        PkceGenerator pkce = PkceGenerator.s256();

        AuthorizationEndpointResponse authsEndpointResponse = oauth.loginForm().codeChallenge(pkce).doLogin("admin", "admin");
        AccessTokenResponse tokenResponse = oauth.accessTokenRequest(authsEndpointResponse.getCode()).codeVerifier(pkce).send();
        String accessToken = tokenResponse.getAccessToken();
        logger.debug("access token:" + accessToken);
        assertBasicClaims(oauth.verifyToken(accessToken), false, false);

        try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet(oauth.getBaseUrl() + "/admin/realms/master");
            get.setHeader("Authorization", "Bearer " + accessToken);
            try (CloseableHttpResponse response = client.execute(get)) {
                Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
                RealmRepresentation realmRepresentation = JsonSerialization.readValue(response.getEntity().getContent(), RealmRepresentation.class);
                Assertions.assertEquals("master", realmRepresentation.getRealm());
            }
        } catch (Exception e) {
            Assertions.fail(e.getMessage());
        }
    }

    @Test
    public void testPublicClientWithLightweightAccessTokenViaDirectGrant() throws Exception {
        // Setup client policy with client-access-type=public + lightweight token executor
        setupPolicy(managedRealm, UseLightweightAccessTokenExecutorFactory.PROVIDER_ID, null,
                ClientAccessTypeConditionFactory.PROVIDER_ID,
                ClientPolicyBuilder.clientAccessTypeCondition(false, ClientAccessTypeConditionFactory.TYPE_PUBLIC));

        // Create public client with direct access grants
        String publicClientId = generateSuffixedName("public-direct-grant-client");
        createClientByAdmin(managedRealm, publicClientId, OIDCLoginProtocol.LOGIN_PROTOCOL, (ClientRepresentation clientRep) -> {
            clientRep.setPublicClient(Boolean.TRUE);
            clientRep.setServiceAccountsEnabled(Boolean.FALSE);
            clientRep.setDirectAccessGrantsEnabled(Boolean.TRUE);
        });

        // Perform direct access grant (POST with username/password)
        oauth.client(publicClientId);
        AccessTokenResponse response = oauth.doPasswordGrantRequest(TEST_USER_NAME, TEST_USER_PASSWORD);

        // Verify lightweight token
        AccessToken token = oauth.verifyToken(response.getAccessToken());
        assertTokenContext(token.getId(), null, AccessTokenContext.TokenType.LIGHTWEIGHT, "ro");
    }

    /**
     * The token id is encoded as sessionType(2) + tokenType(2) + grantType(2) + ':' + rawId,
     * see DefaultTokenContextEncoderProvider. Pass null to skip checking the session type or grant type.
     */
    private void assertTokenContext(String tokenId, AccessTokenContext.SessionType sessionType, AccessTokenContext.TokenType tokenType, String grantShortcut) {
        int indexOf = tokenId.indexOf(':');
        Assertions.assertEquals(6, indexOf, "Unexpected token id format: " + tokenId);
        if (sessionType != null) {
            Assertions.assertEquals(sessionType.getShortcut(), tokenId.substring(0, 2));
        }
        Assertions.assertEquals(tokenType.getShortcut(), tokenId.substring(2, 4));
        if (grantShortcut != null) {
            Assertions.assertEquals(grantShortcut, tokenId.substring(4, 6));
        }
    }

    private void removeSession(final String sessionId) {
        try {
            managedRealm.admin().deleteSession(sessionId, false);
        } catch (NotFoundException nfe) {
            // Ignore
        }
    }

    private void assertMapperClaims(AccessToken token, boolean isAddMapperResponseFlag, boolean isAuthCodeFlow) {
        if (isAddMapperResponseFlag) {
            if (isAuthCodeFlow) {
                Assertions.assertNotNull(token.getName());
                Assertions.assertNotNull(token.getGivenName());
                Assertions.assertNotNull(token.getFamilyName());
                Assertions.assertNotNull(token.getAddress());
                Assertions.assertNotNull(token.getEmail());
                Assertions.assertNotNull(token.getOtherClaims().get("user-session-note"));
                Assertions.assertNotNull(token.getOtherClaims().get("test-claim"));
                Assertions.assertNotNull(token.getOtherClaims().get("group-name"));
                Assertions.assertNotNull(token.getOtherClaims().get(IDToken.SESSION_STATE));
            }
            Assertions.assertNotNull(token.getAudience());
            Assertions.assertNotNull(token.getAcr());
            Assertions.assertNotNull(token.getAllowedOrigins());
            Assertions.assertNotNull(token.getRealmAccess());
            Assertions.assertNotNull(token.getResourceAccess());
            Assertions.assertNotNull(token.getEmailVerified());
            Assertions.assertNotNull(token.getPreferredUsername());
        } else {
            if (isAuthCodeFlow) {
                Assertions.assertNull(token.getName());
                Assertions.assertNull(token.getGivenName());
                Assertions.assertNull(token.getFamilyName());
                Assertions.assertNull(token.getAddress());
                Assertions.assertNull(token.getEmail());
                Assertions.assertNull(token.getOtherClaims().get("user-session-note"));
                Assertions.assertNull(token.getOtherClaims().get("test-claim"));
                Assertions.assertNull(token.getOtherClaims().get("group-name"));
                Assertions.assertNull(token.getOtherClaims().get(IDToken.SESSION_STATE));
            }
            Assertions.assertNull(token.getAcr());
            Assertions.assertNull(token.getAllowedOrigins());
            Assertions.assertNull(token.getRealmAccess());
            Assertions.assertTrue(token.getResourceAccess().isEmpty());
            Assertions.assertNull(token.getEmailVerified());
            Assertions.assertNull(token.getPreferredUsername());
        }
    }

    private void assertInitClaims(AccessToken token, boolean isAuthCodeFlow) {
        Assertions.assertNotNull(token.getExp());
        Assertions.assertNotNull(token.getIat());
        Assertions.assertNotNull(token.getId());
        Assertions.assertNotNull(token.getType());
        Assertions.assertNotNull(token.getIssuedFor());
        Assertions.assertNotNull(token.getScope());
        Assertions.assertNotNull(token.getIssuer());
        if (isAuthCodeFlow) {
            Assertions.assertNotNull(token.getSessionId());
        } else {
            Assertions.assertNull(token.getSessionId());
        }
    }

    private void assertBasicClaims(AccessToken token, boolean isAuthCodeFlow, boolean missing) {
        if (missing) {
            Assertions.assertNull(token.getAuth_time());
            Assertions.assertNull(token.getSubject());
        } else {
            Assertions.assertNotNull(token.getSubject());
            if (isAuthCodeFlow) {
                Assertions.assertNotNull(token.getAuth_time());
            } else {
                Assertions.assertNull(token.getAuth_time());
            }
        }
    }

    private void assertIntrospectClaims(AccessToken token) {
        Assertions.assertNotNull(token.getOtherClaims().get("client_id"));
        Assertions.assertNotNull(token.getOtherClaims().get("active"));
        Assertions.assertNotNull(token.getOtherClaims().get("token_type"));
    }

    private void assertAccessToken(AccessToken token, boolean isAuthCodeFlow, boolean isAddToAccessToken, boolean missingBasicClaims) {
        Assertions.assertNull(token.getNonce());
        assertMapperClaims(token, isAddToAccessToken, isAuthCodeFlow);
        assertInitClaims(token, isAuthCodeFlow);
        assertBasicClaims(token, isAuthCodeFlow, missingBasicClaims);
    }

    private void assertTokenIntrospectionResponse(AccessToken token, boolean isAuthCodeFlow, boolean isAddToIntrospect, boolean missingBasicClaims) {
        Assertions.assertNull(token.getNonce());
        assertMapperClaims(token, isAddToIntrospect, isAuthCodeFlow);
        assertInitClaims(token, isAuthCodeFlow);
        assertIntrospectClaims(token);
        assertBasicClaims(token, isAuthCodeFlow, missingBasicClaims);
    }

    public void addDefaultBasicClientScope() {
        managedRealm.admin().getDefaultDefaultClientScopes()
                .stream()
                .filter(scope-> scope.getName().equals(OIDCLoginProtocolFactory.BASIC_SCOPE))
                .findFirst()
                .ifPresent(scope-> {
                    testClient().addDefaultClientScope(scope.getId());
                });
    }

    public void removeDefaultBasicClientScope() {
        managedRealm.admin().getDefaultDefaultClientScopes()
                .stream()
                .filter(scope-> scope.getName().equals(OIDCLoginProtocolFactory.BASIC_SCOPE))
                .findFirst()
                .ifPresent(scope-> {
                    testClient().removeDefaultClientScope(scope.getId());
                });
    }

    private void setScopeProtocolMappers(boolean isIncludeAccessToken, boolean isIncludeIntrospection, boolean isIncludeLightweightAccessToken) {
        setScopeProtocolMapper(ACR_SCOPE, ACR, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(PROFILE_CLAIM, FULL_NAME, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(EMAIL, EMAIL, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(EMAIL, EMAIL_VERIFIED, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(PROFILE_CLAIM, GIVEN_NAME, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(PROFILE_CLAIM, FAMILY_NAME, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(PROFILE_CLAIM, USERNAME, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(WEB_ORIGINS_SCOPE, ALLOWED_WEB_ORIGINS, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(ROLES_SCOPE, REALM_ROLES, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(ROLES_SCOPE, CLIENT_ROLES, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(ROLES_SCOPE, AUDIENCE_RESOLVE, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        setScopeProtocolMapper(ADDRESS, ADDRESS, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
    }

    private void setScopeProtocolMapper(RealmResource realm, String scopeName, String mapperName, boolean isIncludeAccessToken, boolean isIncludeIntrospection, boolean isIncludeLightweightAccessToken) {
        ClientScopeResource scope = AdminApiUtil.findClientScopeByName(realm, scopeName);
        ProtocolMapperRepresentation protocolMapper = AdminApiUtil.findProtocolMapperByName(scope, mapperName);
        Map<String, String> config = protocolMapper.getConfig();
        if (isIncludeAccessToken) {
            config.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        } else {
            config.put(INCLUDE_IN_ACCESS_TOKEN, "false");
        }
        if (isIncludeIntrospection) {
            config.put(INCLUDE_IN_INTROSPECTION, "true");
        } else {
            config.put(INCLUDE_IN_INTROSPECTION, "false");
        }
        if (isIncludeLightweightAccessToken) {
            config.put(INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN, "true");
        } else {
            config.put(INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN, "false");
        }
        scope.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);
    }

    private void setScopeProtocolMapper(String scopeName, String mapperName, boolean isIncludeAccessToken, boolean isIncludeIntrospection, boolean isIncludeLightweightAccessToken) {
        setScopeProtocolMapper(managedRealm.admin(), scopeName, mapperName, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
    }

    private void clearKeycloakSsoCookies() {
        oauth.getDriver().navigate().to(oauth.getBaseUrl() + "/realms/master");
        oauth.getDriver().manage().deleteAllCookies();
    }

    private void useConcreteRedirectUriForPairwise() {
        ClientResource client = testClient();
        ClientRepresentation rep = client.toRepresentation();
        rep.setRedirectUris(List.of(oauth.getRedirectUri()));
        client.update(rep);
    }

    private ProtocolMappersResource setProtocolMappers(boolean isIncludeAccessToken, boolean isIncludeIntrospection, boolean setPairWise) {
        if (setPairWise) {
            useConcreteRedirectUriForPairwise();
        }
        setScopeProtocolMappers(isIncludeAccessToken, isIncludeIntrospection, false);
        List<ProtocolMapperRepresentation> protocolMapperList = new ArrayList<>();
        setExistingProtocolMappers(protocolMapperList, isIncludeAccessToken, isIncludeIntrospection, false, setPairWise);
        ProtocolMappersResource protocolMappers = testClient().getProtocolMappers();
        protocolMappers.createMapper(protocolMapperList);
        return protocolMappers;
    }

    private ProtocolMappersResource setProtocolMappers(boolean isIncludeAccessToken, boolean isIncludeIntrospection, boolean isIncludeLightweightAccessToken, boolean setPairWise) {
        if (setPairWise) {
            useConcreteRedirectUriForPairwise();
        }
        setScopeProtocolMappers(isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken);
        List<ProtocolMapperRepresentation> protocolMapperList = new ArrayList<>();
        setExistingProtocolMappers(protocolMapperList, isIncludeAccessToken, isIncludeIntrospection, isIncludeLightweightAccessToken, setPairWise);
        ProtocolMappersResource protocolMappers = testClient().getProtocolMappers();
        protocolMappers.createMapper(protocolMapperList);
        return protocolMappers;
    }

    private void setExistingProtocolMappers(List<ProtocolMapperRepresentation> protocolMapperList, boolean isIncludeAccessToken, boolean isIncludeIntrospection, boolean isIncludeLightweightAccessToken, boolean setPairWise) {
        Map<String, String> config = new HashMap<>();
        if (isIncludeAccessToken) {
            config.put(INCLUDE_IN_ACCESS_TOKEN, "true");
        } else {
            config.put(INCLUDE_IN_ACCESS_TOKEN, "false");
        }
        if (isIncludeIntrospection) {
            config.put(INCLUDE_IN_INTROSPECTION, "true");
        } else {
            config.put(INCLUDE_IN_INTROSPECTION, "false");
        }
        if (isIncludeLightweightAccessToken) {
            config.put(INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN, "true");
        } else {
            config.put(INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN, "false");
        }

        ProtocolMapperRepresentation audienceProtocolMapper = createClaimMapper("audience", AudienceProtocolMapper.PROVIDER_ID, new HashMap<>(config) {{
            put(INCLUDED_CLIENT_AUDIENCE, "account-console");
        }});
        protocolMapperList.add(audienceProtocolMapper);
        // Always include resource-server in audience for introspection validation
        ProtocolMapperRepresentation resourceServerAudienceMapper = createClaimMapper("audience-resource-server", AudienceProtocolMapper.PROVIDER_ID, new HashMap<>() {{
            put(INCLUDE_IN_ACCESS_TOKEN, "true");
            put(INCLUDE_IN_INTROSPECTION, "true");
            put(INCLUDE_IN_LIGHTWEIGHT_ACCESS_TOKEN, String.valueOf(isIncludeLightweightAccessToken));
            put(INCLUDED_CLIENT_AUDIENCE, RESOURCE_SERVER_CLIENT_ID);
        }});
        protocolMapperList.add(resourceServerAudienceMapper);
        ProtocolMapperRepresentation roleNameMapper = createClaimMapper("role-name", RoleNameMapper.PROVIDER_ID, new HashMap<>(config) {{
            put(ROLE_CONFIG, "user");
            put(NEW_ROLE_NAME, "new-role");
        }});
        protocolMapperList.add(roleNameMapper);
        ProtocolMapperRepresentation groupMembershipMapper = createClaimMapper("group-member", GroupMembershipMapper.PROVIDER_ID, new HashMap<>(config) {{
            put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "group-name");
        }});
        protocolMapperList.add(groupMembershipMapper);
        ProtocolMapperRepresentation hardcodedClaim = createClaimMapper("hardcoded-claim", HardcodedClaim.PROVIDER_ID, new HashMap<>(config) {{
            put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "test-claim");
            put(CLAIM_VALUE, "test-value");
        }});
        protocolMapperList.add(hardcodedClaim);
        ProtocolMapperRepresentation hardcodedRole = createClaimMapper("hardcoded-role", HardcodedRole.PROVIDER_ID, new HashMap<>(config) {{
            put(ROLE_CONFIG, "hardcoded-role");
        }});
        protocolMapperList.add(hardcodedRole);
        ProtocolMapperRepresentation userSessionNoteMapper = createClaimMapper("user-session-note", UserSessionNoteMapper.PROVIDER_ID, new HashMap<>(config) {{
            put(OIDCAttributeMapperHelper.TOKEN_CLAIM_NAME, "user-session-note");
            put(USER_SESSION_NOTE, "AUTH_TIME");
        }});
        protocolMapperList.add(userSessionNoteMapper);
        if (setPairWise) {
            ProtocolMapperRepresentation pairwiseSubMapper = createClaimMapper("pairwise-sub-mapper", "oidc-" + SHA256PairwiseSubMapper.PROVIDER_ID + PROVIDER_ID_SUFFIX, new HashMap<>(config) {{
                put(PAIRWISE_SUB_ALGORITHM_SALT, "abc");
            }});
            protocolMapperList.add(pairwiseSubMapper);
        }
        ProtocolMapperRepresentation sessionStateMapper = createClaimMapper("session-state-mapper", SessionStateMapper.PROVIDER_ID, config);
        protocolMapperList.add(sessionStateMapper);
    }

    private static ProtocolMapperRepresentation createClaimMapper(String name, String providerId, Map<String, String> config) {
        ProtocolMapperModel mapper = new ProtocolMapperModel();
        mapper.setName(name);
        mapper.setProtocolMapper(providerId);
        mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        mapper.setConfig(config);
        return ModelToRepresentation.toRepresentation(mapper);
    }

    private void deleteProtocolMappers(ProtocolMappersResource protocolMappers) {
        List<String> mapperNames = new ArrayList<>(Arrays.asList("reference", "audience", "audience-resource-server", "role-name", "group-member", "hardcoded-claim", "hardcoded-role", "user-session-note", "pairwise-sub-mapper", "session-state-mapper"));
        List<ProtocolMapperRepresentation> mappers = new ArrayList<>();
        for (String mapperName : mapperNames) {
            mappers.add(ProtocolMapperUtil.getMapperByNameAndProtocol(protocolMappers, OIDCLoginProtocol.LOGIN_PROTOCOL, mapperName));
        }

        for (ProtocolMapperRepresentation mapper : mappers) {
            if (mapper != null) {
                protocolMappers.delete(mapper.getId());
            }
        }
    }

    private TokenResponseContext browserLogin(String username, String password) {
        AuthorizationEndpointResponse authsEndpointResponse = oauth.loginForm().nonce("123456").doLogin(username, password);
        String userSessionId = authsEndpointResponse.getSessionState();
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode());
        return new TokenResponseContext(userSessionId, tokenResponse);
    }

    private class TokenResponseContext {

        private final String userSessionId;
        private final AccessTokenResponse tokenResponse;

        public TokenResponseContext(String userSessionId, AccessTokenResponse tokenResponse) {
            this.userSessionId = userSessionId;
            this.tokenResponse = tokenResponse;
        }
    }

    private void setUseLightweightAccessTokenExecutor() throws Exception {
        setupPolicy(managedRealm, UseLightweightAccessTokenExecutorFactory.PROVIDER_ID, null,
                AnyClientConditionFactory.PROVIDER_ID, null);
    }

    private void alwaysUseLightWeightAccessToken(boolean enable){
        setClientAttribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, String.valueOf(enable));
    }

    private void allowUserinfoWithLightweightAccessToken(boolean allow) {
        runOnServerMaster.run(RunHelpers.setSystemPropertyOnServer("oidc.allow-userinfo-with-lightweight-access-token", String.valueOf(allow)));
        runOnServerMaster.run(RunHelpers.reinitializeProviderFactoryWithSystemPropertiesScope(org.keycloak.protocol.LoginProtocol.class.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL, "oidc."));
    }

    @Test
    public void testUserInfoWithLightweightTokenRejected() {
        alwaysUseLightWeightAccessToken(true);
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            oauth.scope("openid address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);

            UserInfoResponse userInfoResponse = oauth.userInfoRequest(accessToken).send();
            assertEquals(401, userInfoResponse.getStatusCode());
        } finally {
            deleteProtocolMappers(protocolMappers);
            alwaysUseLightWeightAccessToken(false);
        }
    }

    @Test
    public void testUserInfoWithLightweightTokenAllowedPerClient() {
        alwaysUseLightWeightAccessToken(true);
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            // Enable per-client option
            setClientAttribute(OIDCConfigAttributes.ALLOW_USERINFO_WITH_LIGHTWEIGHT_ACCESS_TOKEN, "true");

            oauth.scope("openid address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);

            // UserInfo request should succeed with per-client option enabled
            UserInfoResponse userInfoResponse = oauth.userInfoRequest(accessToken).send();
            assertEquals(200, userInfoResponse.getStatusCode());
            assertEquals("test-user@localhost", userInfoResponse.getUserInfo().getPreferredUsername());

        } finally {
            // Disable per-client option
            setClientAttribute(OIDCConfigAttributes.ALLOW_USERINFO_WITH_LIGHTWEIGHT_ACCESS_TOKEN, "false");
            deleteProtocolMappers(protocolMappers);
            alwaysUseLightWeightAccessToken(false);
        }
    }

    @Test
    public void testUserInfoWithLightweightTokenAllowedServerWide() throws Exception {
        alwaysUseLightWeightAccessToken(true);
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            // Enable server-wide option
            allowUserinfoWithLightweightAccessToken(true);

            oauth.scope("openid address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);

            // UserInfo request should succeed with server-wide option enabled
            UserInfoResponse userInfoResponse = oauth.userInfoRequest(accessToken).send();
            assertEquals(200, userInfoResponse.getStatusCode());
            assertEquals("test-user@localhost", userInfoResponse.getUserInfo().getPreferredUsername());

        } finally {
            // Disable server-wide option
            allowUserinfoWithLightweightAccessToken(false);
            deleteProtocolMappers(protocolMappers);
            alwaysUseLightWeightAccessToken(false);
        }
    }

    @Test
    public void testUserInfoWithLightweightTokenServerWideOverridesClient() throws Exception {
        alwaysUseLightWeightAccessToken(true);
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            // Enable server-wide option
            allowUserinfoWithLightweightAccessToken(true);

            // Explicitly disable per-client option
            setClientAttribute(OIDCConfigAttributes.ALLOW_USERINFO_WITH_LIGHTWEIGHT_ACCESS_TOKEN, "false");

            oauth.scope("openid address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            String accessToken = response.getAccessToken();
            logger.debug("accessToken:" + accessToken);

            // Should succeed - server-wide overrides per-client false
            UserInfoResponse userInfoResponse = oauth.userInfoRequest(accessToken).send();
            assertEquals(200, userInfoResponse.getStatusCode());
            assertEquals("test-user@localhost", userInfoResponse.getUserInfo().getPreferredUsername());

            // Remove per-client option (not set)
            setClientAttribute(OIDCConfigAttributes.ALLOW_USERINFO_WITH_LIGHTWEIGHT_ACCESS_TOKEN, null);

            // Reuse same token - server-wide should still allow
            userInfoResponse = oauth.userInfoRequest(accessToken).send();
            assertEquals(200, userInfoResponse.getStatusCode());
            assertEquals("test-user@localhost", userInfoResponse.getUserInfo().getPreferredUsername());

        } finally {
            allowUserinfoWithLightweightAccessToken(false);
            setClientAttribute(OIDCConfigAttributes.ALLOW_USERINFO_WITH_LIGHTWEIGHT_ACCESS_TOKEN, "false");
            deleteProtocolMappers(protocolMappers);
            alwaysUseLightWeightAccessToken(false);
        }
    }

    @Test
    public void testPolicyLightWeightTokenExchangeTest() throws Exception {
        setUseLightweightAccessTokenExecutor();

        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true, false);
        try {
            setClientAttribute(Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, String.valueOf(false));
            ClientResource clientResource = testClient();
            ClientRepresentation clientRep = clientResource.toRepresentation();
            if (clientRep.getAttributes() == null) clientRep.setAttributes(new java.util.HashMap<>());
            clientRep.getAttributes().put("standard.token.exchange.enabled", "true");
            clientResource.update(clientRep);
            oauth.scope("address");
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET);
            AccessTokenResponse response = browserLogin(TEST_USER_NAME, TEST_USER_PASSWORD).tokenResponse;
            assertAccessToken(oauth.verifyToken(response.getAccessToken()), true, true, true);
            AccessTokenResponse exchangeResponse = oauth.tokenExchangeRequest(response.getAccessToken()).send();
            String exchangedTokenString = exchangeResponse.getAccessToken();
            Assertions.assertNotNull(exchangedTokenString, "Token exchange response should contain access token");
            assertTokenContext(oauth.verifyToken(exchangedTokenString).getId(), null, AccessTokenContext.TokenType.LIGHTWEIGHT, null);
            oauth.client(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_CLIENT_PASSWORD);
            String tokenResponse = oauth.doIntrospectionAccessTokenRequest(exchangedTokenString).getRaw();
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, true, false);
        } finally {
            try {
                ClientResource cr = testClient();
                ClientRepresentation crRep = cr.toRepresentation();
                if (crRep.getAttributes() != null) {
                    crRep.getAttributes().remove("standard.token.exchange.enabled");
                    cr.update(crRep);
                }
            } catch (Exception ignored) {
            }
            deleteProtocolMappers(protocolMappers);
        }
    }


    private static class LightweightRealmConfig implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.groups(GroupBuilder.create("topGroup").subGroups("level2group"));

            realm.clients(ClientBuilder.create().clientId(TEST_CLIENT)
                    .secret(TEST_CLIENT_SECRET)
                    .redirectUris("*")
                    .webOrigins("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    // Replaces @EnableFeature(TOKEN_EXCHANGE): standard token exchange v2 is enabled by default but opt-in per client
                    .attribute(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED, "true"));

            realm.clients(ClientBuilder.create().clientId(RESOURCE_SERVER_CLIENT_ID)
                    .secret(RESOURCE_SERVER_CLIENT_PASSWORD)
                    .redirectUris("*")
                    .directAccessGrantsEnabled(true)
                    .serviceAccountsEnabled(true)
                    .attribute(Constants.SUPPORT_JWT_CLAIM_IN_INTROSPECTION_RESPONSE_ENABLED, "true"));

            realm.clientRoles(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_ROLE);

            realm.users(UserBuilder.create().username(TEST_USER_NAME)
                    .email(TEST_USER_NAME)
                    .name("Tom", "Brady")
                    .password(TEST_USER_PASSWORD)
                    .groups("/topGroup/level2group")
                    .attribute("street", "1 My Street")
                    .attribute("locality", "Cardiff")
                    .attribute("region", "Cardiff")
                    .attribute("postal_code", "CF104RA")
                    .realmRoles("user", "offline_access")
                    .clientRoles(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_ROLE));

            realm.users(UserBuilder.create().username(ServiceAccountConstants.SERVICE_ACCOUNT_USER_PREFIX + TEST_CLIENT)
                    .serviceAccountId(TEST_CLIENT)
                    .realmRoles("offline_access")
                    .clientRoles(RESOURCE_SERVER_CLIENT_ID, RESOURCE_SERVER_ROLE));

            return realm.update(rep -> {
                UPConfig upConfig = UPConfigUtils.parseSystemDefaultConfig();
                upConfig.setUnmanagedAttributePolicy(UnmanagedAttributePolicy.ENABLED);

                ComponentExportRepresentation userProfile = new ComponentExportRepresentation();
                userProfile.setId(UUID.randomUUID().toString());
                userProfile.setProviderId("declarative-user-profile");
                userProfile.setConfig(new MultivaluedHashMap<>(
                        Map.of(UP_COMPONENT_CONFIG_KEY, List.of(JsonSerialization.valueAsString(upConfig)))));

                MultivaluedHashMap<String, ComponentExportRepresentation> components = new MultivaluedHashMap<>();
                components.add(UserProfileProvider.class.getName(), userProfile);
                rep.setComponents(components);
            });
        }
    }

}
