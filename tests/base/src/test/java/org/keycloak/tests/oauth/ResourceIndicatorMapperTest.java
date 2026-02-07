package org.keycloak.tests.oauth;

import java.io.IOException;
import java.util.List;

import jakarta.ws.rs.BadRequestException;

import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RequiredActionProviderModel;
import org.keycloak.models.UserModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.ResourceIndicatorMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.executor.SecureResourceIndicatorExecutorFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.RealmConfig;
import org.keycloak.testframework.realm.RealmConfigBuilder;
import org.keycloak.testframework.remote.runonserver.InjectRunOnServer;
import org.keycloak.testframework.remote.runonserver.RunOnServerClient;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.LogoutConfirmPage;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.TokenRevocationResponse;
import org.keycloak.util.JsonSerialization;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static org.keycloak.services.clientpolicy.executor.SecureResourceIndicatorExecutor.ERR_DIFFERENT_RESOURCE;
import static org.keycloak.services.clientpolicy.executor.SecureResourceIndicatorExecutor.ERR_NOT_PERMITTED_RESOURCE;
import static org.keycloak.services.clientpolicy.executor.SecureResourceIndicatorExecutor.ERR_NO_RESOURCE_IN_TOKEN_REQUEST;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createResourceAudienceBindExecutorConfig;


@KeycloakIntegrationTest(config = ResourceIndicatorMapperTest.ResourceIndicatorMapperServerConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ResourceIndicatorMapperTest {

    @InjectRealm(config = ResourceIndicatorMapperRealmConfig.class)
    ManagedRealm testRealm;

    @InjectOAuthClient
    OAuthClient oAuthClient;

    @InjectRunOnServer
    RunOnServerClient runOnServer;

    @InjectPage
    protected LogoutConfirmPage logoutConfirmPage;

    private static final String TEST_REALM = "MyTestRealm";
    private static final String TEST_CLIENT = "MyTestClient";
    private static final String TEST_CLIENT_SECRET = "secret";
    private static final String TEST_USER = "MyTestUser";
    private static final String TEST_USER_PASSWORD = "password";
    private static final String TEST_MAPPER = "resource-indicator";

    private static final String POLICY_NAME = "MyPolicy";
    private static final String PROFILE_NAME = "MyProfile";

    @BeforeEach
    public void setup() {
        // It is enough to be executed only once before start testing.
        // However, static method annotated @BeforeAll cannot refer to the member runOnServer, so it is executed here.
        // It is better to do that in ResourceIndicatorMapperRealmConfig.configure, but no way to do that in that method.
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST_REALM);
            ClientModel target = realm.getClientByClientId(TEST_CLIENT);
            if (target.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, TEST_MAPPER) == null) {
                target.addProtocolMapper(ResourceIndicatorMapper.create(TEST_MAPPER, true, true));
                target.setServiceAccountsEnabled(true);
            }

            // disable verify profile required action
            RequiredActionProviderModel model = realm.getRequiredActionProviderByAlias(UserModel.RequiredAction.VERIFY_PROFILE.name());
            if (model.isEnabled()) {
                model.setDefaultAction(false);
                model.setEnabled(false);
                realm.updateRequiredActionProvider(model);
            }
        });
    }

    @AfterEach
    public void cleanup() {
        // logout
        oAuthClient.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();
    }

    @Test
    public void testSuccessfulBindMapper() throws IOException {
        // authorization code grant
        //  resource specified in an authorization request
        //  resource not specified in a token request
        //  -> bind with resource specified in an authorization request
        String resourceInAuthorizationRequest = "https://resource.example.com/v1";
        String resourceInTokenRequest = null;
        String code = loginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        AccessTokenResponse tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenValidResponse(tokenResponse, resourceInAuthorizationRequest);

        // introspect
        //  -> bind with resource specified in an authorization request
        IntrospectionResponse introspectionResponse = oAuthClient.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        assertIntrospectValidResponse(introspectionResponse, resourceInAuthorizationRequest);

        // refresh
        //  resource not specified in a token refresh request
        //  -> bind with resource specified in an authorization request
        tokenResponse = oAuthClient.doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertRefreshTokenResponse(tokenResponse, resourceInAuthorizationRequest);

        // revoke
        TokenRevocationResponse revokeResponse = oAuthClient.doTokenRevoke(tokenResponse.getAccessToken());
        assertRevokeValidResponse(revokeResponse, tokenResponse);

        // authorization code grant
        //  resource not specified in an authorization request
        //  resource not specified in a token request
        //  -> not bind
        resourceInAuthorizationRequest = null;
        resourceInTokenRequest = null;
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertNotBindTokenValidResponse(tokenResponse);

        // authorization code grant
        //  resource specified in an authorization request
        //  resource specified in a token request
        //  -> bind with resource specified in an authorization request
        resourceInAuthorizationRequest = "https://resource.example.com/v1";
        resourceInTokenRequest = "https://different.resource.example.com/";
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenValidResponse(tokenResponse, resourceInAuthorizationRequest);

        // authorization code grant
        // resource specified in an authorization request
        // resource specified in a token request
        // -> bind with resource specified in an authorization request
        resourceInAuthorizationRequest = "https://resource.example.com/v1";
        resourceInTokenRequest = resourceInAuthorizationRequest;
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenValidResponse(tokenResponse, resourceInAuthorizationRequest);

        // refresh
        //  resource specified in a token refresh request, but it is different from the one in the authorization request
        //  -> bind with resource specified in an authorization request
        tokenResponse = oAuthClient.refreshRequest(tokenResponse.getRefreshToken()).resource("https://different.resource.example.com/").send();
        assertRefreshTokenResponse(tokenResponse, resourceInAuthorizationRequest);
    }

    @Test
    public void testNotBindMapper() {
        // setup realm
        runOnServer.run(session -> {
            RealmModel realm = session.realms().getRealmByName(TEST_REALM);
            ClientModel target = realm.getClientByClientId(TEST_CLIENT);
            ProtocolMapperModel model = target.getProtocolMapperByName(OIDCLoginProtocol.LOGIN_PROTOCOL, TEST_MAPPER);
            if (model != null) {
                target.removeProtocolMapper(model);
            }
        });

        // resource specified in an authorization request but no mapper applied - not bind
        String resource = "https://resource.example.com/v1";
        String code = loginUserAndGetCode(TEST_CLIENT, resource);
        AccessTokenResponse tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resource).send();
        assertNotBindTokenValidResponse(tokenResponse);
    }

    @Test
    public void testSuccessfulBindMapperWithExecutor() throws Exception {
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
        AccessTokenResponse tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenValidResponse(tokenResponse, resourceInAuthorizationRequest);

        // introspect
        //  -> bind with resource specified in an authorization request
        IntrospectionResponse introspectionResponse = oAuthClient.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        assertIntrospectValidResponse(introspectionResponse, resourceInAuthorizationRequest);

        // refresh
        //  resource specified in a token refresh request, but it is different from the one in the authorization request
        //  -> bind with resource specified in an authorization request
        tokenResponse = oAuthClient.refreshRequest(tokenResponse.getRefreshToken()).resource("https://different.resource.example.com/").send();
        assertRefreshTokenResponse(tokenResponse, resourceInAuthorizationRequest);

        // refresh
        //  resource not specified in a token refresh request
        //  -> bind with resource specified in an authorization request
        tokenResponse = oAuthClient.refreshRequest(tokenResponse.getRefreshToken()).resource(null).send();
        assertRefreshTokenResponse(tokenResponse, resourceInAuthorizationRequest);

        // revoke
        TokenRevocationResponse revokeResponse = oAuthClient.doTokenRevoke(tokenResponse.getAccessToken());
        assertRevokeValidResponse(revokeResponse, tokenResponse);

        // authorization code grant
        //  resource not specified in an authorization request
        //  resource not specified in a token request
        //  -> not bind
        resourceInAuthorizationRequest = null;
        resourceInTokenRequest = null;
        code = ssoLoginUserAndGetCode(TEST_CLIENT, null);
        tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(null).send();
        assertNotBindTokenValidResponse(tokenResponse);

        // authorization code grant
        //  resource not specified in an authorization request
        //  resource specified in a token request
        //  -> not bind
        resourceInAuthorizationRequest = null;
        resourceInTokenRequest = "https://resource.example.com/v3";
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertNotBindTokenValidResponse(tokenResponse);

        // authorization code grant
        //  resource specified in an authorization request
        //  resource not specified in a token request
        //  -> error
        resourceInAuthorizationRequest = null;
        resourceInTokenRequest = "https://resource.example.com/v3";
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertNotBindTokenValidResponse(tokenResponse);

        // authorization code grant
        //  resource specified in an authorization request
        //  resource specified in a token request, but it is different from the one in the authorization request
        //  -> error
        resourceInAuthorizationRequest = "https://mcp.example.com/mcp";
        resourceInTokenRequest = "https://resource.example.com/v3";
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oAuthClient.
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
        tokenResponse = oAuthClient.
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
        tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenInvalidResponse(tokenResponse, ERR_DIFFERENT_RESOURCE);

        // authorization code grant
        //  resource specified in an authorization request, and it is included in the permitted resources
        //  resource not specified in a token request
        //  -> error
        resourceInAuthorizationRequest = permittedResources.get(1);
        resourceInTokenRequest = null;
        code = ssoLoginUserAndGetCode(TEST_CLIENT, resourceInAuthorizationRequest);
        tokenResponse = oAuthClient.
                client(TEST_CLIENT, TEST_CLIENT_SECRET).accessTokenRequest(code).resource(resourceInTokenRequest).send();
        assertTokenInvalidResponse(tokenResponse, ERR_NO_RESOURCE_IN_TOKEN_REQUEST);

        // logout
        oAuthClient.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();
    }

    public static class ResourceIndicatorMapperRealmConfig implements RealmConfig {

        @Override
        public RealmConfigBuilder configure(RealmConfigBuilder realm) {
            realm.name(TEST_REALM);
            realm.addClient(TEST_CLIENT).secret(TEST_CLIENT_SECRET).redirectUris("http://127.0.0.1:8500/callback/oauth");
            realm.addUser(TEST_USER).password(TEST_USER_PASSWORD);
            return realm;
        }
    }

    public static class ResourceIndicatorMapperServerConfig implements KeycloakServerConfig {

        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.RESOURCE_INDICATOR);
        }
    }

    private String loginUserAndGetCode(String clientId, String resource) {
        oAuthClient.client(clientId);
        oAuthClient.loginForm().resource(resource).doLogin(TEST_USER, TEST_USER_PASSWORD);

        String code = oAuthClient.parseLoginResponse().getCode();
        Assertions.assertNotNull(code);
        return code;
    }

    private String ssoLoginUserAndGetCode(String clientId, String resource) {
        oAuthClient.client(clientId);
        oAuthClient.loginForm().resource(resource).open();

        String code = oAuthClient.parseLoginResponse().getCode();
        Assertions.assertNotNull(code);
        return code;
    }

    private void assertTokenValidResponse(AccessTokenResponse tokenResponse, String resource) {
        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        AccessToken accessToken = oAuthClient.verifyToken(tokenResponse.getAccessToken());
        Assertions.assertEquals(1, accessToken.getAudience().length);
        Assertions.assertEquals(resource, accessToken.getAudience()[0]);
        IDToken idToken = oAuthClient.verifyIDToken(tokenResponse.getIdToken());
        Assertions.assertEquals(1, idToken.getAudience().length);
        Assertions.assertEquals(TEST_CLIENT, idToken.getAudience()[0]);
    }

    private void assertIntrospectValidResponse(IntrospectionResponse introspectionResponse, String resource) throws IOException {
        Assertions.assertEquals(200, introspectionResponse.getStatusCode());
        Assertions.assertEquals(1, introspectionResponse.asTokenMetadata().getAudience().length);
        Assertions.assertEquals(resource, introspectionResponse.asTokenMetadata().getAudience()[0]);
    }

    private void assertRefreshTokenResponse(AccessTokenResponse tokenResponse, String resource) {
        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        AccessToken accessToken = oAuthClient.verifyToken(tokenResponse.getAccessToken());
        Assertions.assertEquals(1, accessToken.getAudience().length);
        Assertions.assertEquals(resource, accessToken.getAudience()[0]);
    }

    private void assertRevokeValidResponse(TokenRevocationResponse revokeResponse, AccessTokenResponse tokenResponse) throws IOException {
        Assertions.assertEquals(200, revokeResponse.getStatusCode());
        IntrospectionResponse introspectionResponse = oAuthClient.doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        Assertions.assertEquals(200, introspectionResponse.getStatusCode());
        Assertions.assertFalse(introspectionResponse.asTokenMetadata().isActive());
    }

    private void assertNotBindTokenValidResponse(AccessTokenResponse tokenResponse) {
        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        AccessToken accessToken = oAuthClient.verifyToken(tokenResponse.getAccessToken());
        Assertions.assertNull(accessToken.getAudience());
    }

    private void assertTokenInvalidResponse(AccessTokenResponse tokenResponse, String errorDescription) {
        Assertions.assertEquals(400, tokenResponse.getStatusCode());
        Assertions.assertEquals(OAuthErrorException.INVALID_GRANT, tokenResponse.getError());
        Assertions.assertEquals(errorDescription, tokenResponse.getErrorDescription());
    }

    private void assertLoginError(String clientId, String resource, String error, String errorDescirption) {
        oAuthClient.client(clientId);
        oAuthClient.loginForm().resource(resource).open();

        AuthorizationEndpointResponse authorizationEndpointResponse = oAuthClient.parseLoginResponse();
        Assertions.assertEquals(error, authorizationEndpointResponse.getError());
        Assertions.assertEquals(errorDescirption, authorizationEndpointResponse.getErrorDescription());
    }

    // Client Policies Operations

    private void updateProfiles(String json) throws ClientPolicyException {
        try {
            ClientProfilesRepresentation clientProfiles = JsonSerialization.readValue(json, ClientProfilesRepresentation.class);
            testRealm.admin().clientPoliciesProfilesResource().updateProfiles(clientProfiles);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update profiles failed", e.getResponse().getStatusInfo().toString());
        } catch (Exception e) {
            throw new ClientPolicyException("update profiles failed", e.getMessage());
        }
    }

    private void updatePolicies(String json) throws ClientPolicyException {
        try {
            ClientPoliciesRepresentation clientPolicies = json==null ? null : JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
            testRealm.admin().clientPoliciesPoliciesResource().updatePolicies(clientPolicies);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update policies failed", e.getResponse().getStatusInfo().toString());
        } catch (IOException e) {
            throw new ClientPolicyException("update policies failed", e.getMessage());
        }
    }
}
