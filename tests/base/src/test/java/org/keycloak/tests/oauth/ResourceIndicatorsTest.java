package org.keycloak.tests.oauth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.broker.jwtauthorizationgrant.JWTAuthorizationGrantIdentityProviderFactory;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.OAuth2DeviceConfig;
import org.keycloak.protocol.oid4vc.model.PreAuthorizedCodeGrant;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.testframework.annotations.InjectEvents;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.annotations.TestSetup;
import org.keycloak.testframework.events.EventAssertion;
import org.keycloak.testframework.events.Events;
import org.keycloak.testframework.oauth.DefaultOAuthClientConfiguration;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientScopeBuilder;
import org.keycloak.testframework.realm.IdentityProviderBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmBuilder;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.UserBuilder;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.util.ApiUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.ParRequest;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.util.BasicAuthHelper;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.http.HttpHeaders;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.keycloak.OAuthErrorException.INVALID_TARGET;
import static org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants.ERROR_INVALID_RESOURCE;
import static org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants.ERROR_NOT_MATCHING;

@KeycloakIntegrationTest(config = ResourceIndicatorsTest.ResourceIndicatorServerConfig.class)
public class ResourceIndicatorsTest {

    // the identity provider that the client of DefaultOAuthClientConfiguration trusts for the JWT authorization grant
    private static final String JWT_GRANT_IDP_ALIAS = BaseAbstractJWTAuthorizationGrantTest.IDP_ALIAS;
    private static final String JWT_GRANT_IDP_ISSUER = BaseAbstractJWTAuthorizationGrantTest.IDP_ISSUER;
    private static final String JWT_GRANT_IDP_USER_ID = "user-id-of-idp";

    @InjectRealm(config = ResourceIndicatorsRealm.class)
    ManagedRealm realm;

    @InjectOAuthClient(config = OAuthClientConfig.class)
    OAuthClient oauth;

    @InjectOAuthClient(ref = "bare", config = BareClientConfig.class)
    OAuthClient bareOauth;

    @InjectEvents
    Events events;

    @InjectOAuthIdentityProvider
    OAuthIdentityProvider identityProvider;

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
    public void testNullAudienceReturnsInvalidTarget() {
        AccessTokenResponse tokenResponse = bareOauth.passwordGrantRequest("user-without-roles", "pass")
                .resource("urn:client:theservice").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testClientCredentialsWithoutRefreshTokenSetsAudience() {
        AccessTokenResponse tokenResponse = oauth.clientCredentialsGrantRequest()
                .resource("urn:client:test-app").send();
        assertValidResponse(tokenResponse, "test-app");
    }

    @Test
    public void testClientCredentialsInvalidResourceLogsErrorEvent() {
        events.clear();

        AccessTokenResponse tokenResponse = oauth.clientCredentialsGrantRequest()
                .resource("/invalid").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);

        EventAssertion.assertError(events.poll())
                .type(EventType.CLIENT_LOGIN_ERROR)
                .error(Errors.INVALID_REQUEST)
                .details(Details.REASON, ERROR_INVALID_RESOURCE);
        Assertions.assertNull(events.poll());
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

    @Test
    public void testMultipleResources() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass")
                .resource("urn:client:theservice").resource("https://otherservice").send();
        assertValidResponse(tokenResponse, "theservice", "https://otherservice");
    }

    @Test
    public void testMultipleResourcesClientCredentials() {
        AccessTokenResponse tokenResponse = oauth.clientCredentialsGrantRequest()
                .resource("urn:client:test-app").resource("https://custom-api").send();
        assertValidResponse(tokenResponse, "test-app", "https://custom-api");
    }

    @Test
    public void testMultipleResourcesWithSameValue() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass")
                .resource("urn:client:theservice").resource("urn:client:theservice").send();
        assertValidResponse(tokenResponse, "theservice");
    }

    @Test
    public void testMultipleResourcesOneNotResolved() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass")
                .resource("urn:client:theservice").resource("https://theservice2").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testMultipleResourcesOneInvalidSyntax() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass")
                .resource("urn:client:theservice").resource("/theservice2").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testAuthzMultipleResourcesOneInvalid() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .resource("urn:client:theservice").resource("/invalid").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());
        Assertions.assertEquals(INVALID_TARGET, authorizationEndpointResponse.getError());
        Assertions.assertEquals(ERROR_INVALID_RESOURCE, authorizationEndpointResponse.getErrorDescription());
    }

    @Test
    public void testAuthzMultipleResourcesNoResourceInTokenRequest() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .resource("urn:client:theservice").resource("https://otherservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).send();
        assertValidResponse(accessTokenResponse, "theservice", "https://otherservice");
    }

    @Test
    public void testAuthzMultipleResourcesAllInTokenRequest() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .resource("urn:client:theservice").resource("https://otherservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode())
                .resource("https://otherservice").resource("urn:client:theservice").send();
        assertValidResponse(accessTokenResponse, "theservice", "https://otherservice");
    }

    @Test
    public void testAuthzMultipleResourcesSubsetInTokenRequest() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .resource("urn:client:theservice").resource("https://otherservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode())
                .resource("https://otherservice").send();
        assertValidResponse(accessTokenResponse, "https://otherservice");
    }

    @Test
    public void testAuthzMultipleResourcesNotSubsetInTokenRequest() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .resource("urn:client:theservice").resource("https://otherservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode())
                .resource("urn:client:theservice").resource("https://custom-api").send();
        assertErrorResponse(accessTokenResponse, INVALID_TARGET, ERROR_NOT_MATCHING);
    }

    @Test
    public void testRefreshWithSubsetOfMultipleResources() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .resource("urn:client:theservice").resource("https://otherservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        // the access token is requested only for the first resource
        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode())
                .resource("urn:client:theservice").send();
        assertValidResponse(accessTokenResponse, "theservice");

        // the other originally granted resource can still be requested when refreshing
        AccessTokenResponse refreshResponse = oauth.refreshRequest(accessTokenResponse.getRefreshToken())
                .resource("https://otherservice").send();
        assertValidResponse(refreshResponse, "https://otherservice");

        // all the originally granted resources are used when no resource is requested
        refreshResponse = oauth.refreshRequest(refreshResponse.getRefreshToken()).send();
        assertValidResponse(refreshResponse, "theservice", "https://otherservice");

        refreshResponse = oauth.refreshRequest(refreshResponse.getRefreshToken())
                .resource("https://otherservice").resource("urn:client:theservice").send();
        assertValidResponse(refreshResponse, "theservice", "https://otherservice");
    }

    @Test
    public void testRefreshWithNotSubsetOfMultipleResources() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass")
                .resource("urn:client:theservice").resource("https://otherservice").send();
        Assertions.assertTrue(tokenResponse.isSuccess());

        AccessTokenResponse refreshResponse = oauth.refreshRequest(tokenResponse.getRefreshToken())
                .resource("urn:client:theservice").resource("https://custom-api").send();
        assertErrorResponse(refreshResponse, INVALID_TARGET, ERROR_NOT_MATCHING);
    }

    @Test
    public void testParMultipleResources() {
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .resource("urn:client:theservice").resource("https://otherservice").send();
        Assertions.assertEquals(201, parResponse.getStatusCode());

        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .requestUri(parResponse.getRequestUri()).doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).send();
        assertValidResponse(accessTokenResponse, "theservice", "https://otherservice");
    }

    @Test
    public void testJWTAuthorizationGrantMultipleResources() {
        AccessTokenResponse tokenResponse = oauth.jwtAuthorizationGrantRequest(createJWTAuthorizationGrantAssertion())
                .resource("urn:client:theservice").resource("https://otherservice").send();
        assertValidResponse(tokenResponse, "theservice", "https://otherservice");

        // the error of the resource parameter is not reported as an error of the grant
        tokenResponse = oauth.jwtAuthorizationGrantRequest(createJWTAuthorizationGrantAssertion())
                .resource("urn:client:theservice").resource("https://theservice2").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testResourceWithSpaceIsNotHandledAsMultipleResources() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass")
                .resource("urn:client:theservice https://otherservice").send();
        assertErrorResponse(tokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testAuthzResourceWithSpaceIsNotHandledAsMultipleResources() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .resource("urn:client:theservice https://otherservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());
        Assertions.assertEquals(INVALID_TARGET, authorizationEndpointResponse.getError());
        Assertions.assertEquals(ERROR_INVALID_RESOURCE, authorizationEndpointResponse.getErrorDescription());
    }

    @Test
    public void testAuthzMultipleResourcesOneNotResolved() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .resource("urn:client:theservice").resource("https://theservice2").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        // all the originally granted resources need to be resolved, even if only a subset of them is requested
        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode())
                .resource("urn:client:theservice").send();
        assertErrorResponse(accessTokenResponse, INVALID_TARGET, ERROR_INVALID_RESOURCE);
    }

    @Test
    public void testParInvalidResource() {
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .resource("urn:client:theservice").resource("/invalid").send();
        Assertions.assertEquals(400, parResponse.getStatusCode());
        Assertions.assertEquals(INVALID_TARGET, parResponse.getError());
        Assertions.assertEquals(ERROR_INVALID_RESOURCE, parResponse.getErrorDescription());
    }

    @Test
    public void testRefreshWithNarrowedScopeAndResource() {
        addOptionalAudienceScope("api-a-scope", "https://api-a");
        addOptionalAudienceScope("api-b-scope", "https://api-b");

        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .scope(OAuth2Constants.SCOPE_OPENID, "api-a-scope", "api-b-scope")
                .resource("https://api-a").resource("https://api-b").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).send();
        assertValidResponse(accessTokenResponse, "https://api-a", "https://api-b");

        // the audience of the other originally granted resource is not available with the narrowed scope
        AccessTokenResponse refreshResponse;
        oauth.scope("api-a-scope");
        try {
            refreshResponse = oauth.refreshRequest(accessTokenResponse.getRefreshToken()).resource("https://api-a").send();
        } finally {
            oauth.scope(null);
        }
        assertValidResponse(refreshResponse, "https://api-a");

        // the other originally granted resource can still be requested with the original scope
        refreshResponse = oauth.refreshRequest(refreshResponse.getRefreshToken()).resource("https://api-b").send();
        assertValidResponse(refreshResponse, "https://api-b");
    }

    @Test
    public void testOfflineRefreshWithSubsetOfMultipleResources() {
        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm()
                .scope(OAuth2Constants.SCOPE_OPENID, OAuth2Constants.OFFLINE_ACCESS)
                .resource("urn:client:theservice").resource("https://otherservice").doLoginWithCookie();
        Assertions.assertTrue(authorizationEndpointResponse.isRedirected());

        AccessTokenResponse accessTokenResponse = oauth.accessTokenRequest(authorizationEndpointResponse.getCode()).send();
        assertValidResponse(accessTokenResponse, "theservice", "https://otherservice");

        AccessTokenResponse refreshResponse = oauth.refreshRequest(accessTokenResponse.getRefreshToken())
                .resource("https://otherservice").send();
        assertValidResponse(refreshResponse, "https://otherservice");

        // the new offline token keeps all the originally granted resources
        refreshResponse = oauth.refreshRequest(refreshResponse.getRefreshToken()).send();
        assertValidResponse(refreshResponse, "theservice", "https://otherservice");
    }

    @Test
    public void testImplicitFlowMultipleResourcesRejected() {
        assertImplicitFlowMultipleResourcesRejected("urn:client:theservice", "https://otherservice");
    }

    @Test
    public void testImplicitFlowMultipleResourcesWithSameValueRejected() {
        // the same value sent multiple times is not handled as a single value
        assertImplicitFlowMultipleResourcesRejected("urn:client:theservice", "urn:client:theservice");
    }

    @Test
    public void testDeviceAuthorizationMultipleResourcesRejected() throws IOException {
        assertDuplicatedParameter(oauth.getEndpoints().getDeviceAuthorization(), List.of(
                new BasicNameValuePair(OAuth2Constants.RESOURCE, "urn:client:theservice"),
                new BasicNameValuePair(OAuth2Constants.RESOURCE, "https://otherservice")));
    }

    @Test
    public void testDeviceAuthorizationRequestObjectMultipleResourcesRejected() throws IOException {
        // the request object can not be used to send multiple values to an endpoint that does not process them
        String requestObject = new JWSBuilder().jsonContent(Map.of(
                OAuth2Constants.CLIENT_ID, oauth.config().getClientId(),
                OAuth2Constants.SCOPE, OAuth2Constants.SCOPE_OPENID,
                OAuth2Constants.RESOURCE, List.of("urn:client:theservice", "https://otherservice"))).none();
        assertInvalidRequest(oauth.getEndpoints().getDeviceAuthorization(),
                List.of(new BasicNameValuePair(OIDCLoginProtocol.REQUEST_PARAM, requestObject)),
                "Multiple resource parameters are not supported by this endpoint");
    }

    @Test
    public void testPreAuthorizedCodeMultipleResourcesRejected() throws IOException {
        assertDuplicatedParameter(oauth.getEndpoints().getToken(), List.of(
                new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrant.PRE_AUTH_GRANT_TYPE),
                new BasicNameValuePair(PreAuthorizedCodeGrant.CODE_REQUEST_PARAM, "code"),
                new BasicNameValuePair(OAuth2Constants.RESOURCE, "urn:client:theservice"),
                new BasicNameValuePair(OAuth2Constants.RESOURCE, "https://otherservice")));
    }

    @Test
    public void testParOversizedResourcesRejected() {
        // one of the values is longer than the maximum length of the parameter
        ParResponse parResponse = oauth.pushedAuthorizationRequest()
                .resource("urn:client:theservice").resource("https://otherservice/" + "a".repeat(4000)).send();
        Assertions.assertEquals(400, parResponse.getStatusCode());

        // all the values together are longer than the maximum length of the parameter
        ParRequest parRequest = oauth.pushedAuthorizationRequest();
        for (int i = 0; i < 300; i++) {
            parRequest.resource("https://otherservice");
        }
        Assertions.assertEquals(400, parRequest.send().getStatusCode());
    }

    @Test
    public void testTokenRequestOversizedResourcesRejected() {
        AccessTokenResponse tokenResponse = oauth.passwordGrantRequest("user", "pass")
                .resource("urn:client:theservice").resource("https://otherservice/" + "a".repeat(4000)).send();
        assertErrorResponse(tokenResponse, OAuthErrorException.INVALID_REQUEST, "The size of OIDC parameter 'resource' is longer than allowed.");
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

            realm.identityProviders(IdentityProviderBuilder.create()
                    .providerId(JWTAuthorizationGrantIdentityProviderFactory.PROVIDER_ID)
                    .alias(JWT_GRANT_IDP_ALIAS)
                    .attribute(IdentityProviderModel.ISSUER, JWT_GRANT_IDP_ISSUER)
                    .attribute(OIDCIdentityProviderConfig.USE_JWKS_URL, Boolean.TRUE.toString())
                    .attribute(OIDCIdentityProviderConfig.JWKS_URL, "http://127.0.0.1:8500/idp/jwks")
                    .build());

            realm.users(UserBuilder.create("user").firstName("user").lastName("user").password("pass").email("the@email.localhost")
                    .federatedLink(JWT_GRANT_IDP_ALIAS, JWT_GRANT_IDP_USER_ID, "user")
                    .realmRoles(OAuth2Constants.OFFLINE_ACCESS)
                    .clientRoles("theservice", "myrole")
                    .clientRoles("otherservice", "myrole")
                    .clientRoles("serviceWithoutResource", "myrole"));

            realm.users(UserBuilder.create("user-without-roles").firstName("noroles").lastName("noroles")
                    .password("pass").email("noroles@email.localhost"));

            return realm;
        }
    }

    private static final class BareClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("no-audience").secret("password").directAccessGrantsEnabled();
        }
    }

    private static final class OAuthClientConfig extends DefaultOAuthClientConfiguration {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return super.configure(client).fullScopeEnabled(true).implicitFlowEnabled(true)
                    .attribute(OAuth2DeviceConfig.OAUTH2_DEVICE_AUTHORIZATION_GRANT_ENABLED, "true")
                    .protocolMappers(
                            createCustomAudienceMapper("custom-audience-mapper-1", "https://custom-api"),
                            createCustomAudienceMapper("custom-audience-mapper-2", "https://custom-api-2"),
                            createCustomAudienceMapper("custom-audience-mapper-3", "https://custom-api-3"));
        }

    }

    protected static final class ResourceIndicatorServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.RESOURCE_INDICATORS, Profile.Feature.OID4VC_VCI, Profile.Feature.OID4VC_VCI_PREAUTH_CODE);
        }
    }

    private static ProtocolMapperRepresentation createCustomAudienceMapper(String name, String audience) {
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

    private String createJWTAuthorizationGrantAssertion() {
        JsonWebToken assertion = new JsonWebToken();
        assertion.id(UUID.randomUUID().toString());
        assertion.subject(JWT_GRANT_IDP_USER_ID);
        assertion.audience(oauth.getEndpoints().getIssuer());
        assertion.issuer(JWT_GRANT_IDP_ISSUER);
        assertion.exp(Time.currentTime() + 300L);
        return identityProvider.encodeToken(assertion);
    }

    private void addOptionalAudienceScope(String scopeName, String audience) {
        String scopeId = ApiUtil.getCreatedId(realm.admin().clientScopes().create(ClientScopeBuilder.create()
                .name(scopeName).protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                .mappers(createCustomAudienceMapper(scopeName + "-mapper", audience)).build()));
        oauth.clientResource().addOptionalClientScope(scopeId);
        realm.cleanup().add(r -> {
            oauth.clientResource().removeOptionalClientScope(scopeId);
            r.clientScopes().get(scopeId).remove();
        });
    }

    private void assertImplicitFlowMultipleResourcesRejected(String firstResource, String secondResource) {
        oauth.responseType(OIDCResponseType.TOKEN);
        try {
            AuthorizationEndpointResponse authorizationEndpointResponse = oauth.loginForm().nonce("nonce")
                    .resource(firstResource).resource(secondResource).doLoginWithCookie();
            Assertions.assertTrue(authorizationEndpointResponse.isRedirected());
            Assertions.assertNull(authorizationEndpointResponse.getAccessToken());
            Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, authorizationEndpointResponse.getError());
            Assertions.assertEquals("Multiple resource parameters are not supported when the authorization endpoint issues an access token",
                    authorizationEndpointResponse.getErrorDescription());
        } finally {
            oauth.responseType(OAuth2Constants.CODE);
        }
    }

    private void assertDuplicatedParameter(String endpoint, List<NameValuePair> parameters) throws IOException {
        assertInvalidRequest(endpoint, parameters, "duplicated parameter");
    }

    private void assertInvalidRequest(String endpoint, List<NameValuePair> parameters, String expectedErrorDescription) throws IOException {
        HttpPost post = new HttpPost(endpoint);
        post.addHeader(HttpHeaders.AUTHORIZATION, BasicAuthHelper.RFC6749.createHeader(oauth.config().getClientId(), oauth.config().getClientSecret()));
        post.setEntity(new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8));
        try (CloseableHttpResponse response = oauth.httpClient().get().execute(post)) {
            Assertions.assertEquals(400, response.getStatusLine().getStatusCode());
            JsonNode json = JsonSerialization.readValue(response.getEntity().getContent(), JsonNode.class);
            Assertions.assertEquals(OAuthErrorException.INVALID_REQUEST, json.path(OAuth2Constants.ERROR).asText());
            Assertions.assertEquals(expectedErrorDescription, json.path(OAuth2Constants.ERROR_DESCRIPTION).asText());
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
