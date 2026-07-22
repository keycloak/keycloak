package org.keycloak.tests.oauth;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.common.Profile;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
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
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuthErrorException.INVALID_TARGET;
import static org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants.ERROR_INVALID_RESOURCE;
import static org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants.ERROR_NOT_MATCHING;

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
    public void testInvalidResourceSyntax() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass").resource("/theservice2").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testAuthzInvalidResourceParam() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("/invalid").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());
        Assertions.assertEquals(authorizationEndpointResponse.getError(), INVALID_TARGET);
        Assertions.assertEquals(authorizationEndpointResponse.getErrorDescription(), ERROR_INVALID_RESOURCE);
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

    @Test
    public void testValidResourceByCustomAudience() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("https://custom-api").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("https://custom-api").send();
        assertValidResponse(accessTokenResponse, "https://custom-api");
    }

    @Test
    public void testValidResourceByOtherCustomAudience() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("https://custom-api-3").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("https://custom-api-3").send();
        assertValidResponse(accessTokenResponse, "https://custom-api-3");
    }

    @Test
    public void testInvalidResourceByCustomAudience() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("https://unknown-custom-api").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("https://unknown-custom-api").send();
        assertErrorResponse(accessTokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testRefreshWithCustomAudienceResource() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("https://custom-api-2").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("https://custom-api-2").send();
        Assertions.assertTrue(accessTokenResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.refreshRequest(accessTokenResponse.getRefreshToken()).send();
        assertValidResponse(refreshResponse, "https://custom-api-2");
    }

    @Test
    public void testRefreshWithCustomAudienceResourceExplicit() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("https://custom-api").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("https://custom-api").send();
        Assertions.assertTrue(accessTokenResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.refreshRequest(accessTokenResponse.getRefreshToken()).resource("https://custom-api").send();
        assertValidResponse(refreshResponse, "https://custom-api");
    }

    @Test
    public void testIntrospectionWithCustomAudienceResource() throws Exception {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().resource("https://custom-api").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).resource("https://custom-api").send();
        Assertions.assertTrue(accessTokenResponse.isSuccess());

        AccessToken accessToken = oauth.parseToken(accessTokenResponse.getAccessToken(), AccessToken.class);
        MatcherAssert.assertThat(accessToken.getAudience(), Matchers.hasItemInArray("https://custom-api"));

        IntrospectionResponse introspectionResponse = oauth.introspectionRequest(accessTokenResponse.getAccessToken())
                .tokenTypeHint("access_token")
                .client("https://custom-api", "introspection-secret")
                .send();
        Assertions.assertTrue(introspectionResponse.isSuccess());
        TokenMetadataRepresentation tokenMetadata = introspectionResponse.asTokenMetadata();
        MatcherAssert.assertThat(tokenMetadata.getAudience(), Matchers.hasItemInArray("https://custom-api"));
    }

    private static final class ResourceIndicatorsRealm implements RealmConfig {

        @Override
        public RealmBuilder configure(RealmBuilder realm) {
            realm.clients(ClientBuilder.create("theservice").attribute("resource_url", "https://theservice"));
            realm.clientRoles("theservice", "myrole");

            realm.clients(ClientBuilder.create("otherservice").attribute("resource_url", "https://otherservice"));
            realm.clientRoles("otherservice", "myrole");

            realm.clients(ClientBuilder.create("serviceWithoutResource"));
            realm.clientRoles("serviceWithoutResource", "myrole");

            realm.clients(ClientBuilder.create("https://custom-api").secret("introspection-secret"));

            realm.users(UserBuilder.create("user").firstName("user").lastName("user").password("pass").email("the@email.localhost")
                    .clientRoles("theservice", "myrole")
                    .clientRoles("otherservice", "myrole")
                    .clientRoles("serviceWithoutResource", "myrole"));

            return realm;
        }
    }

    private static final class OAuthClientConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client).fullScopeEnabled(true)
                    .protocolMappers(
                            createCustomAudienceMapper("custom-audience-mapper-1", "https://custom-api"),
                            createCustomAudienceMapper("custom-audience-mapper-2", "https://custom-api-2"),
                            createCustomAudienceMapper("custom-audience-mapper-3", "https://custom-api-3"));
        }

        private ProtocolMapperRepresentation createCustomAudienceMapper(String name, String audience) {
            ProtocolMapperRepresentation mapper = new ProtocolMapperRepresentation();
            mapper.setName(name);
            mapper.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            mapper.setProtocolMapper(AudienceProtocolMapper.PROVIDER_ID);

            Map<String, String> config = new HashMap<>();
            config.put(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE, audience);
            config.put(OIDCAttributeMapperHelper.INCLUDE_IN_ACCESS_TOKEN, "true");
            mapper.setConfig(config);
            return mapper;
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
