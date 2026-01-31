package org.keycloak.testsuite.oauth;

import java.io.IOException;
import java.util.List;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ResourceIndicatorMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.policies.AbstractClientPoliciesTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.TokenRevocationResponse;

import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.auth.page.AuthRealm.TEST;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableFeature(value = Profile.Feature.RESOURCE_INDICATOR)
public class ResourceIndicatorMapperTest extends AbstractClientPoliciesTest {

    public final static String MAPPER_NAME = "resource-audience-mapper";

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void testSuccessfulBindMapper() throws Exception {
        // setup realm
        testingClient.server().run(session->{
            RealmModel realm = session.realms().getRealmByName(TEST);
            ClientModel target = realm.getClientByClientId(TEST_CLIENT);
            target.addProtocolMapper(ResourceIndicatorMapper.create(MAPPER_NAME, true, true));
            target.setServiceAccountsEnabled(true);
        });

        // authorization code grant
        //  resource specified in an authorization request
        //  resource not specified in a token request
        //  -> bind with resource specified in an authorization request
        String resourceInAuthorizationRequest = "https://resource.example.com/v1";
        String resourceInTokenRequest = null;
        String code = loginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        AccessTokenResponse tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenValidResponse(tokenResponse, resourceInAuthorizationRequest);

        // introspect
        //  -> bind with resource specified in an authorization request
        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        assertIntrospectValidResponse(introspectionResponse, resourceInAuthorizationRequest);

        // refresh
        //  resource not specified in a token refresh request
        //  -> bind with resource specified in an authorization request
        tokenResponse = oauth.doRefreshTokenRequest(tokenResponse.getRefreshToken());
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
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertNotBindTokenValidResponse(tokenResponse);

        // authorization code grant
        //  resource specified in an authorization request
        //  resource specified in a token request
        //  -> bind with resource specified in an authorization request
        resourceInAuthorizationRequest = "https://resource.example.com/v1";
        resourceInTokenRequest = "https://different.resource.example.com/";
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenValidResponse(tokenResponse, resourceInAuthorizationRequest);

        // authorization code grant
        // resource specified in an authorization request
        // resource specified in a token request
        // -> bind with resource specified in an authorization request
        resourceInAuthorizationRequest = "https://resource.example.com/v1";
        resourceInTokenRequest = resourceInAuthorizationRequest;
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenValidResponse(tokenResponse, resourceInAuthorizationRequest);

        // refresh
        //  resource specified in a token refresh request, but it is different from the one in the authorization request
        //  -> bind with resource specified in an authorization request
        tokenResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).resource("https://different.resource.example.com/").send();
        assertRefreshTokenResponse(tokenResponse, resourceInAuthorizationRequest);
    }

    @Test
    public void testNotBindMapper() throws Exception {
        // setup realm
        testingClient.server().run(session->{
            RealmModel realm = session.realms().getRealmByName(TEST);
            ClientModel target = realm.getClientByClientId(TEST_CLIENT);
            ProtocolMapperModel model = target.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, MAPPER_NAME);
            if (model != null) {
                target.removeProtocolMapper(model);
            }
        });

        // resource specified in an authorization request but no mapper applied - not bind
        String resource = "https://resource.example.com/v1";
        String code = loginUserAndGetCode(TEST_CLIENT, resource);
        AccessTokenResponse tokenResponse = oauth.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resource).send();
        assertNotBindTokenValidResponse(tokenResponse);
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
