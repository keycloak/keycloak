package org.keycloak.testsuite.client.policies;

import java.io.IOException;
import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.mappers.ResourceIndicatorMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureResourceIndicatorExecutorFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.TokenRevocationResponse;

import org.junit.Test;

import static org.keycloak.services.clientpolicy.executor.SecureResourceIndicatorExecutor.ERR_DIFFERENT_RESOURCE;
import static org.keycloak.services.clientpolicy.executor.SecureResourceIndicatorExecutor.ERR_NOT_PERMITTED_RESOURCE;
import static org.keycloak.services.clientpolicy.executor.SecureResourceIndicatorExecutor.ERR_NO_RESOURCE_IN_TOKEN_REQUEST;
import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;
import static org.keycloak.testsuite.oauth.ResourceIndicatorMapperTest.MAPPER_NAME;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createResourceAudienceBindExecutorConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableFeature(value = Profile.Feature.RESOURCE_INDICATOR)
public class SecureResourceIndicatorExecutorTest extends AbstractClientPoliciesTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void testSuccessfulBind() throws Exception {
        // setup realm
        testingClient.server().run(session->{
            RealmModel realm = session.realms().getRealmByName(TEST);
            ClientModel target = realm.getClientByClientId(TEST_CLIENT);
            target.addProtocolMapper(ResourceIndicatorMapper.create(MAPPER_NAME, true, true));
            target.setServiceAccountsEnabled(true);
        });

        // register profiles
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResourceIndicatorExecutorFactory.PROVIDER_ID, null)
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // authorization code grant
        //  resource specified in an authorization request
        //  resource specified in a token request, and it is the same as the one in the authorization request
        //  -> bind with resource specified in an authorization request
        String resourceInAuthorizationRequest = "https://resource.example.com/v1";
        String resourceInTokenRequest = resourceInAuthorizationRequest;
        String code = loginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        AccessTokenResponse tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenValidResponse(tokenResponse, resourceInAuthorizationRequest);

        // introspect
        //  -> bind with resource specified in an authorization request
        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        assertIntrospectValidResponse(introspectionResponse, resourceInAuthorizationRequest);

        // refresh
        //  resource specified in a token refresh request, but it is different from the one in the authorization request
        //  -> bind with resource specified in an authorization request
        tokenResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).resource("https://different.resource.example.com/").send();
        assertRefreshTokenResponse(tokenResponse, resourceInAuthorizationRequest);

        // refresh
        //  resource not specified in a token refresh request
        //  -> bind with resource specified in an authorization request
        tokenResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).resource(null).send();
        assertRefreshTokenResponse(tokenResponse, resourceInAuthorizationRequest);

        // revoke
        TokenRevocationResponse revokeResponse = oauth.doTokenRevoke(tokenResponse.getAccessToken());
        assertRevokeValidResponse(revokeResponse, tokenResponse);

        // authorization code grant
        //  resource not specified in an authorization request
        //  resource not specified in a token request
        //  -> not bind
        resourceInAuthorizationRequest = null;
        resourceInTokenRequest = null;
        code = ssoLoginUserAndGetCode(TEST_CLIENT, null);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(null).send();
        assertNotBindTokenValidResponse(tokenResponse);

        // authorization code grant
        //  resource not specified in an authorization request
        //  resource specified in a token request
        //  -> not bind
        resourceInAuthorizationRequest = null;
        resourceInTokenRequest = "https://resource.example.com/v3";
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertNotBindTokenValidResponse(tokenResponse);

        // authorization code grant
        //  resource specified in an authorization request
        //  resource not specified in a token request
        //  -> error
        resourceInAuthorizationRequest = null;
        resourceInTokenRequest = "https://resource.example.com/v3";
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertNotBindTokenValidResponse(tokenResponse);

        // authorization code grant
        //  resource specified in an authorization request
        //  resource specified in a token request, but it is different from the one in the authorization request
        //  -> error
        resourceInAuthorizationRequest = "https://mcp.example.com/mcp";
        resourceInTokenRequest = "https://resource.example.com/v3";
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenInvalidResponse(tokenResponse, ERR_DIFFERENT_RESOURCE);

        // add permitted resources
        List<String> permittedResources = List.of("https://example.com/resource", "https://www.example.com/res");
        json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "O Primeiro Perfil")
                        .addExecutor(SecureResourceIndicatorExecutorFactory.PROVIDER_ID, createResourceAudienceBindExecutorConfig(permittedResources))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // authorization code grant
        //  resource specified in an authorization request, and it is included in the permitted resources
        //  resource specified in a token request, and it is the same one in the authorization request
        //  -> bind with resource specified in an authorization request
        resourceInAuthorizationRequest = permittedResources.get(0);
        resourceInTokenRequest = resourceInAuthorizationRequest;
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenValidResponse(tokenResponse, resourceInAuthorizationRequest);

        // authorization code grant
        //  resource is not specified in an authorization request
        //  -> error
        assertLoginError(TEST_CLIENT, null, OAuthErrorException.INVALID_REQUEST, ERR_NOT_PERMITTED_RESOURCE);

        // authorization code grant
        //  resource specified in an authorization request, but it is not included in the permitted resources
        //  -> error
        assertLoginError(TEST_CLIENT, "https://different.resource.example.com/", OAuthErrorException.INVALID_REQUEST, ERR_NOT_PERMITTED_RESOURCE);

        // authorization code grant
        //  resource specified in an authorization request, and it is included in the permitted resources
        //  resource specified in a token request, and it is included in the permitted resources, but it is different from the same one in the authorization request
        //  -> error
        resourceInAuthorizationRequest = permittedResources.get(1);
        resourceInTokenRequest = permittedResources.get(0);
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenInvalidResponse(tokenResponse, ERR_DIFFERENT_RESOURCE);

        // authorization code grant
        //  resource specified in an authorization request, and it is included in the permitted resources
        //  resource not specified in a token request
        //  -> error
        resourceInAuthorizationRequest = permittedResources.get(1);
        resourceInTokenRequest = null;
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenInvalidResponse(tokenResponse, ERR_NO_RESOURCE_IN_TOKEN_REQUEST);
    }

    private String loginUserAndGetCode(String clientId, String resource) {
        oauth.client(clientId);
        oauth.loginForm().resource(resource).requestUri(requestUri).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String code = oauth.parseLoginResponse().getCode();
        Assert.assertNotNull(code);
        return code;
    }

    private String ssoLoginUserAndGetCode(String clientId, String resource) {
        oauth.client(clientId);
        oauth.loginForm().resource(resource).open();

        String code = oauth.parseLoginResponse().getCode();
        Assert.assertNotNull(code);
        return code;
    }

    private void assertLoginError(String clientId, String resource, String error, String errorDescirption) {
        oauth.client(clientId);
        oauth.loginForm().resource(resource).open();

        AuthorizationEndpointResponse authorizationEndpointResponse = oauth.parseLoginResponse();
        assertEquals(error, authorizationEndpointResponse.getError());
        assertEquals(errorDescirption, authorizationEndpointResponse.getErrorDescription());
    }

    private void assertTokenValidResponse(AccessTokenResponse tokenResponse, String resource) {
        assertEquals(200, tokenResponse.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(2, accessToken.getAudience().length);
        assertEquals(resource, accessToken.getAudience()[0]);
        IDToken idToken = oauth.verifyIDToken(tokenResponse.getIdToken());
        assertEquals(1, idToken.getAudience().length);
        assertEquals(TEST_CLIENT, idToken.getAudience()[0]);
    }

    private void assertIntrospectValidResponse(IntrospectionResponse introspectionResponse, String resource) throws IOException {
        assertEquals(200, introspectionResponse.getStatusCode());
        assertEquals(2, introspectionResponse.asTokenMetadata().getAudience().length);
        assertEquals(resource, introspectionResponse.asTokenMetadata().getAudience()[0]);
    }

    private void assertRefreshTokenResponse(AccessTokenResponse tokenResponse, String resource) {
        assertEquals(200, tokenResponse.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(2, accessToken.getAudience().length);
        assertEquals(resource, accessToken.getAudience()[0]);
    }

    private void assertRevokeValidResponse(TokenRevocationResponse revokeResponse, AccessTokenResponse tokenResponse) throws IOException {
        assertEquals(200, revokeResponse.getStatusCode());
        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        assertEquals(200, introspectionResponse.getStatusCode());
        assertFalse(introspectionResponse.asTokenMetadata().isActive());
    }

    private void assertNotBindTokenValidResponse(AccessTokenResponse tokenResponse) {
        assertEquals(200, tokenResponse.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(1, accessToken.getAudience().length);
        assertEquals("account", accessToken.getAudience()[0]);
    }

    private void assertTokenInvalidResponse(AccessTokenResponse tokenResponse, String errorDescription) {
        assertEquals(400, tokenResponse.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenResponse.getError());
        assertEquals(errorDescription, tokenResponse.getErrorDescription());
    }
}
