package org.keycloak.tests.oauth;

import java.util.HashMap;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectWebDriver;
import org.keycloak.testframework.ui.webdriver.ManagedWebDriver;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Reproducer for https://github.com/keycloak/keycloak/issues/12223
 *
 * Verifies that sequential authorization code requests with different dynamic
 * scope parameters produce tokens with the correct, non-mixed scopes — both
 * at code exchange and after refresh.
 */
@KeycloakIntegrationTest(config = DynamicScopesIsolationTest.DynamicScopesServerConfig.class)
public class DynamicScopesIsolationTest {

    private static final String SCOPE_NAME = "dynamic";
    private static final String VALUE_A = "valueA";
    private static final String VALUE_B = "valueB";
    private static final String USERNAME = "test-user";
    private static final String PASSWORD = "password";

    @InjectRealm(config = TestRealmConfig.class)
    ManagedRealm realm;

    @InjectWebDriver(lifecycle = LifeCycle.METHOD)
    ManagedWebDriver driver;

    @InjectOAuthClient(config = TestOAuthClientConfig.class)
    OAuthClient oauth;

    private String dynamicScopeId;

    @AfterEach
    public void cleanup() {
        if (dynamicScopeId != null) {
            ClientRepresentation client = realm.admin().clients().findByClientId(oauth.getClientId()).get(0);
            realm.admin().clients().get(client.getId()).removeOptionalClientScope(dynamicScopeId);
            realm.admin().clientScopes().get(dynamicScopeId).remove();
            dynamicScopeId = null;
        }
    }

    @Test
    public void testDynamicScopeIsolationAcrossCodeExchangeAndRefresh() {
        createAndAssignDynamicScope();

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

    private void assertTokenScope(AccessTokenResponse response, String expectedScope, String notExpectedScope) {
        AccessToken token = oauth.parseToken(response.getAccessToken(), AccessToken.class);
        assertScopeContains(token.getScope(), expectedScope);
        assertScopeNotContains(token.getScope(), notExpectedScope);
    }

    private void createAndAssignDynamicScope() {
        ClientScopeRepresentation scopeRep = new ClientScopeRepresentation();
        scopeRep.setName(SCOPE_NAME);
        scopeRep.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
        scopeRep.setAttributes(new HashMap<>() {{
            put(ClientScopeModel.IS_DYNAMIC_SCOPE, "true");
            put(ClientScopeModel.DYNAMIC_SCOPE_REGEXP, SCOPE_NAME + ":*");
        }});

        try (Response response = realm.admin().clientScopes().create(scopeRep)) {
            assertEquals(201, response.getStatus(), "Dynamic scope creation should succeed");
            dynamicScopeId = ApiUtil.getCreatedId(response);
        }

        ClientRepresentation client = realm.admin().clients().findByClientId(oauth.getClientId()).get(0);
        realm.admin().clients().get(client.getId()).addOptionalClientScope(dynamicScopeId);
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

    static class TestRealmConfig implements RealmConfig {
        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            return realm.users(UserBuilder.create(USERNAME).password(PASSWORD)
                    .email("test@localhost").firstName("Test").lastName("User"));
        }
    }

    static class TestOAuthClientConfig extends DefaultOAuthClientConfiguration {
        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client).consentRequired(false);
        }
    }

    public static class DynamicScopesServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.DYNAMIC_SCOPES);
        }
    }
}
