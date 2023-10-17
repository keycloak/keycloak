package org.keycloak.testsuite.oidc;

import jakarta.ws.rs.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.admin.client.resource.ClientScopeResource;
import org.keycloak.admin.client.resource.ProtocolMappersResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.common.Profile;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.GroupMembershipMapper;
import org.keycloak.protocol.oidc.mappers.HardcodedClaim;
import org.keycloak.protocol.oidc.mappers.HardcodedRole;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.mappers.RoleNameMapper;
import org.keycloak.protocol.oidc.mappers.SHA256PairwiseSubMapper;
import org.keycloak.protocol.oidc.mappers.UserSessionNoteMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.KeycloakModelUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ProtocolMapperUtil;
import org.keycloak.util.JsonSerialization;
import org.wildfly.common.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import static org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper.PAIRWISE_SUB_ALGORITHM_SALT;
import static org.keycloak.protocol.oidc.mappers.RoleNameMapper.NEW_ROLE_NAME;
import static org.keycloak.protocol.oidc.mappers.RoleNameMapper.ROLE_CONFIG;
import static org.keycloak.testsuite.admin.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

@EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
public class LightWeightAccessTokenTest extends AbstractKeycloakTest {

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true).setServiceAccountsEnabled(true);
        ClientManager.realm(adminClient.realm("test")).clientId("resource-server").directAccessGrant(true);
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        UserRepresentation user = findUser(realm, "test-user@localhost");
        Map<String, List<String>> attributes = new HashMap<>(){{
            put("street", Arrays.asList("1 My Street"));
            put("locality", Arrays.asList("Cardiff"));
            put("region", Arrays.asList("Cardiff"));
            put("postal_code", Arrays.asList("CF104RA"));
        }};
        user.setAttributes(attributes);
        user.setGroups(Arrays.asList("/topGroup/level2group"));
        ClientRepresentation confApp = KeycloakModelUtils.createClient(realm, "resource-server");
        confApp.setSecret("password");
        confApp.setServiceAccountsEnabled(Boolean.TRUE);
        testRealms.add(realm);
    }

    private UserRepresentation findUser(RealmRepresentation testRealm, String userName) {
        for (UserRepresentation user : testRealm.getUsers()) {
            if (user.getUsername().equals(userName)) return user;
        }

        return null;
    }

    @Test
    public void accessTokenFalseIntrospectionTrueTest() throws IOException {
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            oauth.nonce("123456");
            oauth.scope("address");
            oauth.clientId("test-app");
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password").tokenResponse;
            String accessToken = response.getAccessToken();
            System.out.println("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, false);

            oauth.clientId("resource-server");
            String tokenResponse = oauth.introspectAccessTokenWithClientCredential("resource-server", "password", accessToken);
            System.out.println("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void accessTokenTrueIntrospectionFalseTest() throws IOException {
        ProtocolMappersResource protocolMappers = setProtocolMappers(true, false, true);
        try {
            oauth.nonce("123456");
            oauth.scope("address");
            oauth.clientId("test-app");
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password").tokenResponse;
            String accessToken = response.getAccessToken();
            System.out.println("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, true);

            oauth.clientId("resource-server");
            String tokenResponse = oauth.introspectAccessTokenWithClientCredential("resource-server", "password", accessToken);
            System.out.println("tokenResponse:" + tokenResponse);
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
            oauth.nonce("123456");
            oauth.scope("address");
            oauth.clientId("test-app");
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password").tokenResponse;
            String accessToken = response.getAccessToken();
            System.out.println("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, true);

            oauth.clientId("resource-server");
            String tokenResponse = oauth.introspectAccessTokenWithClientCredential("resource-server", "password", accessToken);
            System.out.println("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void offlineTokenTest() throws IOException {
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            oauth.nonce("123456");
            oauth.scope("openid address offline_access");

            oauth.clientId("test-app");
            TokenResponseContext ctx = browserLogin("password", "test-user@localhost", "password");
            OAuthClient.AccessTokenResponse response = ctx.tokenResponse;
            String accessToken = response.getAccessToken();
            System.out.println("accessToken:" + accessToken);
            System.out.println("idtoken:" + response.getIdToken());
            assertAccessToken(oauth.verifyToken(accessToken), true, false);

            oauth.clientId("resource-server");
            removeSession(ctx.userSessionId);
            String tokenResponse = oauth.introspectAccessTokenWithClientCredential("resource-server", "password", accessToken);
            System.out.println("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, true, false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    private void removeSession(final String sessionId) {
        testingClient.testing().removeExpired("test");
        try {
            testingClient.testing().removeUserSession("test", sessionId);
        } catch (NotFoundException nfe) {
            // Ignore
        }
    }

    @Test
    public void clientCredentialTest() throws Exception {
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, false);
        try {
            oauth.nonce("123456");
            oauth.scope("address");

            oauth.clientId("test-app");
            OAuthClient.AccessTokenResponse response = oauth.doClientCredentialsGrantAccessTokenRequest("password");
            String accessToken = response.getAccessToken();
            System.out.println("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), false, false);

            oauth.clientId("resource-server");
            String tokenResponse = oauth.introspectAccessTokenWithClientCredential("resource-server", "password", accessToken);
            System.out.println("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), false);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    @Test
    public void exchangeTest() throws Exception {
        ProtocolMappersResource protocolMappers = setProtocolMappers(false, true, true);
        try {
            oauth.nonce("123456");
            oauth.scope("address");

            oauth.clientId("test-app");
            OAuthClient.AccessTokenResponse response = browserLogin("password", "test-user@localhost", "password").tokenResponse;
            String accessToken = response.getAccessToken();
            System.out.println("accessToken:" + accessToken);
            assertAccessToken(oauth.verifyToken(accessToken), true, false);
            response = oauth.doTokenExchange(TEST, accessToken, null, "test-app", "password");
            String exchangedTokenString = response.getAccessToken();
            System.out.println("exchangedTokenString:" + exchangedTokenString);

            oauth.clientId("resource-server");
            String tokenResponse = oauth.introspectAccessTokenWithClientCredential("resource-server", "password", exchangedTokenString);
            System.out.println("tokenResponse:" + tokenResponse);
            assertTokenIntrospectionResponse(JsonSerialization.readValue(tokenResponse, AccessToken.class), true, true, true);
        } finally {
            deleteProtocolMappers(protocolMappers);
        }
    }

    private void assertMapperClaims(AccessToken token, boolean isAddMapperResponseFlag, boolean isAuthCodeFlow) {
        if (isAddMapperResponseFlag) {
            if (isAuthCodeFlow) {
                Assert.assertNotNull(token.getName());
                Assert.assertNotNull(token.getGivenName());
                Assert.assertNotNull(token.getFamilyName());
                Assert.assertNotNull(token.getAddress());
                Assert.assertNotNull(token.getEmail());
                Assert.assertNotNull(token.getOtherClaims().get("user-session-note"));
                Assert.assertNotNull(token.getOtherClaims().get("test-claim"));
                Assert.assertNotNull(token.getOtherClaims().get("group-name"));
            }
            Assert.assertNotNull(token.getAudience());
            Assert.assertNotNull(token.getAcr());
            Assert.assertNotNull(token.getAllowedOrigins());
            Assert.assertNotNull(token.getRealmAccess());
            Assert.assertNotNull(token.getResourceAccess());
            Assert.assertNotNull(token.getEmailVerified());
            Assert.assertNotNull(token.getPreferredUsername());
        } else {
            if (isAuthCodeFlow) {
                Assert.assertTrue(token.getName() == null);
                Assert.assertTrue(token.getGivenName() == null);
                Assert.assertTrue(token.getFamilyName() == null);
                Assert.assertTrue(token.getAddress() == null);
                Assert.assertTrue(token.getEmail() == null);
                Assert.assertTrue(token.getOtherClaims().get("user-session-note") == null);
                Assert.assertTrue(token.getOtherClaims().get("test-claim") == null);
                Assert.assertTrue(token.getOtherClaims().get("group-name") == null);
            }
            Assert.assertTrue(token.getAcr() == null);
            Assert.assertTrue(token.getAllowedOrigins() == null);
            Assert.assertTrue(token.getRealmAccess() == null);
            Assert.assertTrue(token.getResourceAccess().isEmpty());
            Assert.assertTrue(token.getEmailVerified() == null);
            Assert.assertTrue(token.getPreferredUsername() == null);
        }
    }

    private void assertInitClaims(AccessToken token, boolean isAuthCodeFlow) {
        Assert.assertNotNull(token.getExp());
        Assert.assertNotNull(token.getIat());
        Assert.assertNotNull(token.getId());
        Assert.assertNotNull(token.getType());
        if (isAuthCodeFlow) {
            Assert.assertNotNull(token.getSessionId());
            Assert.assertNotNull(token.getAuth_time());
        } else {
            Assert.assertTrue(token.getSessionId() == null);
            Assert.assertTrue(token.getAuth_time() == null);
        }
        Assert.assertNotNull(token.getIssuedFor());
        Assert.assertNotNull(token.getScope());
        Assert.assertNotNull(token.getIssuer());
    }

    private void assertIntrospectClaims(AccessToken token) {
        Assert.assertNotNull(token.getOtherClaims().get("client_id"));
        Assert.assertNotNull(token.getOtherClaims().get("active"));
        Assert.assertNotNull(token.getOtherClaims().get("token_type"));
    }

    private void assertNonce(AccessToken token, boolean isAuthCodeFlow, boolean exchangeToken) {
        if (isAuthCodeFlow && !exchangeToken) {
            Assert.assertNotNull(token.getNonce());
        } else {
            Assert.assertTrue(token.getNonce() == null);
        }
    }

    private void assertAccessToken(AccessToken token, boolean isAuthCodeFlow, boolean isAddToAccessToken) {
        assertNonce(token, isAuthCodeFlow, false);
        assertMapperClaims(token, isAddToAccessToken, isAuthCodeFlow);
        assertInitClaims(token, isAuthCodeFlow);
    }

    private void assertTokenIntrospectionResponse(AccessToken token, boolean isAuthCodeFlow) {
        assertTokenIntrospectionResponse(token, isAuthCodeFlow, true, false);
    }

    private void assertTokenIntrospectionResponse(AccessToken token, boolean isAuthCodeFlow, boolean isAddToIntrospect, boolean exchangeToken) {
        assertNonce(token, isAuthCodeFlow, exchangeToken);
        assertMapperClaims(token, isAddToIntrospect, isAuthCodeFlow);
        assertInitClaims(token, isAuthCodeFlow);
        assertIntrospectClaims(token);
    }

    protected RealmResource testRealm() {
        return adminClient.realm("test");
    }

    private void setScopeProtocolMappers(boolean isIncludeAccessToken, boolean isIncludeIntrospection) {
        setScopeProtocolMapper(ACR_SCOPE, ACR, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(PROFILE_CLAIM, FULL_NAME, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(EMAIL, EMAIL, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(EMAIL, EMAIL_VERIFIED, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(PROFILE_CLAIM, GIVEN_NAME, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(PROFILE_CLAIM, FAMILY_NAME, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(PROFILE_CLAIM, USERNAME, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(WEB_ORIGINS_SCOPE, ALLOWED_WEB_ORIGINS, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(ROLES_SCOPE, REALM_ROLES, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(ROLES_SCOPE, CLIENT_ROLES, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(ROLES_SCOPE, AUDIENCE_RESOLVE, isIncludeAccessToken, isIncludeIntrospection);
        setScopeProtocolMapper(ADDRESS, ADDRESS, isIncludeAccessToken, isIncludeIntrospection);
    }

    private void setScopeProtocolMapper(String scopeName, String mapperName, boolean isIncludeAccessToken, boolean isIncludeIntrospection) {
        ClientScopeResource scope = ApiUtil.findClientScopeByName(testRealm(), scopeName);
        ProtocolMapperRepresentation protocolMapper = ApiUtil.findProtocolMapperByName(scope, mapperName);
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
        scope.getProtocolMappers().update(protocolMapper.getId(), protocolMapper);
    }

    private ProtocolMappersResource setProtocolMappers(boolean isIncludeAccessToken, boolean isIncludeIntrospection, boolean setPairWise) {
        setScopeProtocolMappers(isIncludeAccessToken, isIncludeIntrospection);
        List<ProtocolMapperRepresentation> protocolMapperList = new ArrayList<>();
        setExistingProtocolMappers(protocolMapperList, isIncludeAccessToken, isIncludeIntrospection, setPairWise);
        ProtocolMappersResource protocolMappers = ApiUtil.findClientResourceByClientId(adminClient.realm("test"), "test-app").getProtocolMappers();
        protocolMappers.createMapper(protocolMapperList);
        return protocolMappers;
    }

    private void setExistingProtocolMappers(List<ProtocolMapperRepresentation> protocolMapperList, boolean isIncludeAccessToken, boolean isIncludeIntrospection, boolean setPairWise) {
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

        ProtocolMapperRepresentation audienceProtocolMapper = createClaimMapper("audience", AudienceProtocolMapper.PROVIDER_ID, new HashMap<>(config) {{
            put(INCLUDED_CLIENT_AUDIENCE, "account-console");
        }});
        protocolMapperList.add(audienceProtocolMapper);
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
        List<String> mapperNames = new ArrayList<>(Arrays.asList("reference", "audience", "role-name", "group-member", "hardcoded-claim", "hardcoded-role", "user-session-note", "pairwise-sub-mapper"));
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

    private TokenResponseContext browserLogin(String clientSecret, String username, String password) {
        OAuthClient.AuthorizationEndpointResponse authsEndpointResponse = oauth.doLogin(username, password);
        String userSessionId = authsEndpointResponse.getSessionState();
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authsEndpointResponse.getCode(), clientSecret);
        return new TokenResponseContext(userSessionId, tokenResponse);
    }

    private class TokenResponseContext {

        private final String userSessionId;
        private final OAuthClient.AccessTokenResponse tokenResponse;

        public TokenResponseContext(String userSessionId, OAuthClient.AccessTokenResponse tokenResponse) {
            this.userSessionId = userSessionId;
            this.tokenResponse = tokenResponse;
        }
    }
}
