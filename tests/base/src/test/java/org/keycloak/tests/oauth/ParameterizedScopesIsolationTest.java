package org.keycloak.tests.oauth;

import java.util.List;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oidc.grants.ciba.channel.AuthenticationChannelResponse;
import org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.ClaimType;
import org.keycloak.protocol.oidc.mappers.ParameterizedScopeMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.CibaProvider;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectCibaProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.realm.UserConfig;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.ciba.AuthenticationRequestAcknowledgement;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.IncludeIn.ACCESS_TOKEN;
import static org.keycloak.protocol.oidc.mappers.OIDCProtocolMapperBuilder.IncludeIn.INTROSPECTION;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproducer for https://github.com/keycloak/keycloak/issues/12223
 *
 * Verifies that sequential authorization code requests with different parameterized
 * scope parameters produce tokens with the correct, non-mixed scopes — both
 * at code exchange and after refresh.
 */
@KeycloakIntegrationTest(config = ParameterizedScopesIsolationTest.ParameterizedScopesServerConfig.class)
public class ParameterizedScopesIsolationTest {

    private static final String SCOPE_NAME = "dynamic";
    private static final String VALUE_A = "valueA";
    private static final String VALUE_B = "valueB";
    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "password";

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm realm;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectUser(config = TargetUserConfig.class)
    ManagedUser targetUser;

    @InjectOAuthClient(config = TestOAuthClientConfig.class)
    OAuthClient oauth;

    @InjectCibaProvider
    CibaProvider ciba;

    @Test
    public void isolationAcrossCodeExchangeAndRefresh() {
        createAndAssignParameterizedScope(SCOPE_NAME, "string");

        String scopeA = SCOPE_NAME + ":" + VALUE_A;
        String scopeB = SCOPE_NAME + ":" + VALUE_B;

        // 1. First authz request with dynamic:valueA — user authenticates
        AuthorizationEndpointResponse authResponse1 = oauth.loginForm()
                .scope(scopeA)
                .doLogin(USERNAME, PASSWORD);
        assertTrue(authResponse1.isRedirected());
        String code1 = authResponse1.getCode();
        assertNotNull(code1);

        // 2. Second authz request with dynamic:valueB — SSO, no credentials needed
        //    This overwrites the client session notes with the new scope
        AuthorizationEndpointResponse authResponse2 = oauth.loginForm()
                .scope(scopeB)
                .doLoginWithCookie();
        assertTrue(authResponse2.isRedirected());
        String code2 = authResponse2.getCode();
        assertNotNull(code2);

        // 3. Exchange code1 — token must have dynamic:valueA (not valueB)
        AccessTokenResponse tokenResponse1 = oauth.doAccessTokenRequest(code1);
        assertTrue(tokenResponse1.isSuccess());
        assertTokenScope(tokenResponse1, scopeA, scopeB);

        // 4. Exchange code2 — token must have dynamic:valueB (not valueA)
        AccessTokenResponse tokenResponse2 = oauth.doAccessTokenRequest(code2);
        assertTrue(tokenResponse2.isSuccess());
        assertTokenScope(tokenResponse2, scopeB, scopeA);

        // 5. Refresh token1 — must still carry dynamic:valueA
        AccessTokenResponse refreshResponse1 = oauth.doRefreshTokenRequest(tokenResponse1.getRefreshToken());
        assertTrue(refreshResponse1.isSuccess());
        assertTokenScope(refreshResponse1, scopeA, scopeB);

        // Verify the new refresh token also preserves the original scope
        RefreshToken newRefreshToken1 = oauth.parseToken(refreshResponse1.getRefreshToken(), RefreshToken.class);
        assertScopeContains(newRefreshToken1.getScope(), scopeA);
        assertScopeNotContains(newRefreshToken1.getScope(), scopeB);

        // 6. Refresh token2 — must still carry dynamic:valueB
        AccessTokenResponse refreshResponse2 = oauth.doRefreshTokenRequest(tokenResponse2.getRefreshToken());
        assertTrue(refreshResponse2.isSuccess());
        assertTokenScope(refreshResponse2, scopeB, scopeA);
    }

    @Test
    public void usernameScopeTypeResolvesUserByEmailWhenLoginWithEmailAllowed() {
        realm.updateWithCleanup(r -> r.loginWithEmailAllowed(true));

        String scopeName = "email-scope";
        String requestedScope = scopeName + ":target@localhost";
        createAndAssignParameterizedScope(scopeName, "username");

        AuthorizationEndpointResponse authResponse = oauth.loginForm()
                .scope(requestedScope)
                .doLogin(USERNAME, PASSWORD);
        assertTrue(authResponse.isRedirected());

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authResponse.getCode());
        assertTrue(tokenResponse.isSuccess());
        assertScopeContains(tokenResponse.getScope(), requestedScope);
    }

    @Test
    public void usernameScopeTypeRejectsEmailWhenLoginWithEmailNotAllowed() {
        realm.updateWithCleanup(r -> r.loginWithEmailAllowed(false));

        String scopeName = "no-email-scope";
        String requestedScope = scopeName + ":target@localhost";
        createAndAssignParameterizedScope(scopeName, "username");

        AuthorizationEndpointResponse authResponse = oauth.loginForm()
                .scope(requestedScope)
                .doLogin(USERNAME, PASSWORD);
        assertTrue(authResponse.isRedirected());

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authResponse.getCode());
        assertTrue(tokenResponse.isSuccess());
        assertScopeNotContains(tokenResponse.getScope(), requestedScope);
    }

    @Test
    public void usernameScopeTypeDoesNotLeakUserExistence() {
        String scopeName = "user-check";
        String requestedScope = scopeName + ":nonexistent-user";
        createAndAssignParameterizedScope(scopeName, "username");

        AuthorizationEndpointResponse authResponse = oauth.loginForm()
                .scope(requestedScope)
                .doLogin(USERNAME, PASSWORD);
        assertTrue(authResponse.isRedirected());

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authResponse.getCode());
        assertTrue(tokenResponse.isSuccess());
        assertScopeNotContains(tokenResponse.getScope(), requestedScope);
    }

    @Test
    public void cibaUsernameScopeTypeDoesNotLeakUserExistence() throws Exception {
        String scopeName = "ciba-user-check";
        String requestedScope = scopeName + ":nonexistent-user";
        createAndAssignParameterizedScope(scopeName, "username");

        oauth.scope(requestedScope);
        AuthenticationRequestAcknowledgement response = oauth.ciba().backchannelAuthenticationRequest(USERNAME)
                .bindingMessage("ciba-binding-msg")
                .clientNotificationToken("ciba-notification-token")
                .send();
        assertTrue(response.isSuccess());
        assertNotNull(response.getAuthReqId());

        CibaProvider.CibaAuthenticationChannelRequest authChannelReq = ciba.getAuthChannel("ciba-binding-msg");
        assertEquals(Response.Status.OK.getStatusCode(),
                oauth.ciba().doAuthenticationChannelCallback(authChannelReq.getBearerToken(), AuthenticationChannelResponse.Status.SUCCEED));

        ciba.getPushedCibaClientNotification("ciba-notification-token");

        AccessTokenResponse tokenResponse = oauth.ciba().doBackchannelAuthenticationTokenRequest(response.getAuthReqId());
        assertTrue(tokenResponse.isSuccess());
        assertScopeNotContains(tokenResponse.getScope(), requestedScope);
    }

    @Test
    public void usernameScopeTypeDropsScopeWhenTargetUserDisabled() {
        String scopeName = "user-scope";
        String requestedScope = scopeName + ":" + targetUser.getUsername();

        createAndAssignParameterizedScope(scopeName, "username");

        // 1. Auth request — user authenticates, scope is granted
        AuthorizationEndpointResponse authResponse = oauth.loginForm()
                .scope(requestedScope)
                .doLogin(USERNAME, PASSWORD);
        assertTrue(authResponse.isRedirected());
        String code = authResponse.getCode();
        assertNotNull(code);

        // 2. Code to token — scope should be present
        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code);
        assertTrue(tokenResponse.isSuccess());
        assertScopeContains(tokenResponse.getScope(), requestedScope);

        // 3. Refresh with no scopes (uses initial scopes) — scope should still be present
        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertTrue(refreshResponse.isSuccess());
        assertScopeContains(refreshResponse.getScope(), requestedScope);

        // 4. Disable the target user
        targetUser.updateWithCleanup(u -> u.enabled(false));

        // 5. Refresh with no scopes — succeeds but scope is no longer assigned
        AccessTokenResponse refreshResponse2 = oauth.doRefreshTokenRequest(refreshResponse.getRefreshToken());
        assertTrue(refreshResponse2.isSuccess());
        assertScopeNotContains(refreshResponse2.getScope(), requestedScope);
    }

    @Test
    public void longestPrefixScopeMatchWins() {
        createParameterizedScopeWithMapper("read", "read_param");
        createParameterizedScopeWithMapper("read:account", "read_account_param");

        // "read:account:victim" should bind to "read:account" (longest prefix), not "read"
        AuthorizationEndpointResponse authResponse = oauth.loginForm()
                .scope("read:account:victim")
                .doLogin(USERNAME, PASSWORD);
        assertTrue(authResponse.isRedirected());

        AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(authResponse.getCode());
        assertTrue(tokenResponse.isSuccess());

        AccessToken token = oauth.parseToken(tokenResponse.getAccessToken(), AccessToken.class);
        assertScopeContains(token.getScope(), "read:account:victim");
        Assertions.assertEquals(List.of("victim"), token.getOtherClaims().get("read_account_param"),
                "Longest prefix scope 'read:account' should match with parameter 'victim'");
        Assertions.assertNull(token.getOtherClaims().get("read_param"),
                "Shorter prefix scope 'read' should not match when a longer prefix exists");

        // "read:service:payment" has no "read:service" scope, so it should bind to "read"
        AuthorizationEndpointResponse authResponse2 = oauth.loginForm()
                .scope("read:service:payment")
                .doLoginWithCookie();
        assertTrue(authResponse2.isRedirected());

        AccessTokenResponse tokenResponse2 = oauth.doAccessTokenRequest(authResponse2.getCode());
        assertTrue(tokenResponse2.isSuccess());

        AccessToken token2 = oauth.parseToken(tokenResponse2.getAccessToken(), AccessToken.class);
        assertScopeContains(token2.getScope(), "read:service:payment");
        Assertions.assertEquals(List.of("service:payment"), token2.getOtherClaims().get("read_param"),
                "Only matching prefix scope 'read' should match with parameter 'service:payment'");
        Assertions.assertNull(token2.getOtherClaims().get("read_account_param"),
                "Non-matching scope 'read:account' should not match 'read:service:payment'");
    }

    private void createParameterizedScopeWithMapper(String name, String claimName) {
        ClientScopeRepresentation scopeRep = ParameterizedScopeBuilder.create(name)
                .parameterizedScopeType("string")
                .build();

        String scopeId;
        try (Response response = realm.admin().clientScopes().create(scopeRep)) {
            assertEquals(201, response.getStatus(), "Parameterized scope creation should succeed");
            scopeId = ApiUtil.getCreatedId(response);
        }

        ProtocolMapperModel mapper = ParameterizedScopeMapper.builder(name + "-param-mapper")
                .claimName(claimName).type(ClaimType.STRING).includeIn(ACCESS_TOKEN, INTROSPECTION).build();
        try (Response response = realm.admin().clientScopes().get(scopeId).getProtocolMappers()
                .createMapper(ModelToRepresentation.toRepresentation(mapper))) {
            assertEquals(201, response.getStatus(), "Mapper creation should succeed");
        }

        String clientId = realm.admin().clients().findByClientId(oauth.getClientId()).get(0).getId();
        realm.cleanup().add(r -> r.clientScopes().get(scopeId).remove());
        realm.admin().clients().get(clientId).addOptionalClientScope(scopeId);
    }

    private static void assertScopeContains(String scopeString, String expectedScope) {
        assertNotNull(scopeString, "Scope string should not be null");
        assertTrue(scopeString.contains(expectedScope),
                "Scope '" + scopeString + "' should contain '" + expectedScope + "'");
    }

    private static void assertScopeNotContains(String scopeString, String notExpectedScope) {
        assertNotNull(scopeString, "Scope string should not be null");
        assertFalse(scopeString.contains(notExpectedScope),
                "Scope '" + scopeString + "' should NOT contain '" + notExpectedScope + "'");
    }

    private void assertTokenScope(AccessTokenResponse response, String expectedScope, String notExpectedScope) {
        AccessToken token = oauth.parseToken(response.getAccessToken(), AccessToken.class);
        assertScopeContains(token.getScope(), expectedScope);
        assertScopeNotContains(token.getScope(), notExpectedScope);
    }

    private void createAndAssignParameterizedScope(String name, String type) {
        ClientScopeRepresentation scopeRep = ParameterizedScopeBuilder.create(name)
                .parameterizedScopeType(type)
                .build();

        String scopeId;
        try (Response response = realm.admin().clientScopes().create(scopeRep)) {
            assertEquals(201, response.getStatus(), "Parameterized scope creation should succeed");
            scopeId = ApiUtil.getCreatedId(response);
        }

        String clientId = realm.admin().clients().findByClientId(oauth.getClientId()).get(0).getId();
        realm.cleanup().add(r -> r.clientScopes().get(scopeId).remove());
        realm.admin().clients().get(clientId).addOptionalClientScope(scopeId);
    }

    static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create(USERNAME).password(PASSWORD)
                    .email("test@localhost").firstName("Test").lastName("User"));
        }
    }

    static class TargetUserConfig implements UserConfig {
        @Override
        public UserBuilder configure(UserBuilder user) {
            return user.username("target-user")
                    .password(PASSWORD)
                    .email("target@localhost")
                    .firstName("Target")
                    .lastName("User");
        }
    }

    static class TestOAuthClientConfig extends DefaultOAuthClientConfiguration {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client)
                    .consentRequired(false)
                    .attribute(CibaConfig.CIBA_BACKCHANNEL_TOKEN_DELIVERY_MODE_PER_CLIENT, "ping")
                    .attribute(CibaConfig.CIBA_BACKCHANNEL_CLIENT_NOTIFICATION_ENDPOINT, "http://localhost:8500/ciba/push-ciba-client-notification")
                    .attribute(CibaConfig.OIDC_CIBA_GRANT_ENABLED, Boolean.TRUE.toString());
        }
    }

    public static class ParameterizedScopesServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.PARAMETERIZED_SCOPES)
                    .option("spi-ciba-auth-channel-ciba-http-auth-channel-http-authentication-channel-uri",
                            "http://localhost:8500/ciba/request-authentication-channel");
        }
    }
}
