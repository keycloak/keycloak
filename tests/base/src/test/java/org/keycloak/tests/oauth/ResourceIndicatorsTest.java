package org.keycloak.tests.oauth;

import org.keycloak.common.Profile;
import org.keycloak.representations.AccessToken;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ClientConfigBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuthErrorException.INVALID_TARGET;
import static org.keycloak.protocol.oidc.token.ResourceIndicatorsPostProcessor.ERROR_INVALID_RESOURCE;
import static org.keycloak.protocol.oidc.token.ResourceIndicatorsPostProcessor.ERROR_NOT_MATCHING;

@KeycloakIntegrationTest(config = ResourceIndicatorsTest.ResourceIndicatorServerConfig.class)
public class ResourceIndicatorsTest {

    @InjectRealm(config = ResourceIndicatorsRealm.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = OAuthClientConfig.class)
    OAuthClient oauth;

    @TestSetup
    public void loginUser() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("urn:client:theservice").doLogin("user", "pass");
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());
    }

    @Test
    public void testValidResourceByClientUrn() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("urn:client:theservice").send();
        assertValidResponse(tokenResponse, "theservice");
    }

    @Test
    public void testValidResourceByUrl() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("https://theservice").send();
        assertValidResponse(tokenResponse, "https://theservice");
    }

    @Test
    public void testInvalidResourceByClientUrn() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("urn:client:somethingelse").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testInvalidResourceByUrl() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("https://theservice2").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testAuthzResourceInTokenRequest() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("urn:client:theservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("urn:client:theservice").send();
        assertValidResponse(accessTokenResponse, "theservice");
    }

    @Test
    public void testAuthzNoResourceInTokenRequest() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("urn:client:theservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).send();
        assertValidResponse(accessTokenResponse, "theservice");
    }

    @Test
    public void testAuthzDifferentResource() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("urn:client:otherservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("urn:client:theservice").send();
        assertErrorResponse(accessTokenResponse, INVALID_TARGET, ERROR_NOT_MATCHING);
    }

    @Test
    public void testRefreshWithNoResource() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("urn:client:theservice").send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).send();
        assertValidResponse(refreshResponse, "theservice");
    }

    @Test
    public void testRefreshWithResourceByClientUrl() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("urn:client:theservice").send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).resource("urn:client:theservice").send();
        assertValidResponse(refreshResponse,  "theservice");
    }

    @Test
    public void testRefreshWithDifferentResource() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("https://theservice").send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).resource("https://otherservice").send();
        assertErrorResponse(refreshResponse,  INVALID_TARGET, ERROR_NOT_MATCHING);
    }

    private static final class ResourceIndicatorsRealm implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.addClient("theservice").attribute("resource_url", "https://theservice");
            realm.clientRoles("theservice", "myrole");

            realm.addClient("otherservice").attribute("resource_url", "https://otherservice");
            realm.clientRoles("otherservice", "myrole");

            realm.addClient("serviceWithoutResource");
            realm.clientRoles("serviceWithoutResource", "myrole");

            realm.addUser("user").firstName("user").lastName("user").password("pass").email("the@email.localhost")
                    .clientRoles("theservice", "myrole")
                    .clientRoles("otherservice", "myrole")
                    .clientRoles("serviceWithoutResource", "myrole");

            return realm;
        }
    }

    private static final class OAuthClientConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientConfigBuilder configure(ClientConfigBuilder client) {
            return super.configure(client).fullScopeEnabled(true);
        }
    }

    protected static final class ResourceIndicatorServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.RESOURCE_INDICATORS);
        }
    }

    private void assertValidResponse(AccessTokenResponse response, String... expectedAudience) {
        Assertions.assertTrue(response.isSuccess());

        AccessToken accessToken = oauth.parseToken(response.getAccessToken(), AccessToken.class);
        MatcherAssert.assertThat(accessToken.getAudience(), Matchers.arrayContainingInAnyOrder(expectedAudience));
    }

    private void assertErrorResponse(AccessTokenResponse response, String expectedError, String expectedErrorDescription) {
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(expectedError, response.getError());
        Assertions.assertEquals(expectedErrorDescription, response.getErrorDescription());
    }

}
