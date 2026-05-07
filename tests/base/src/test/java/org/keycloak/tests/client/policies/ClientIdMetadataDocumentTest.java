package org.keycloak.tests.client.policies;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.core.Response;

import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeCondition;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeConditionFactory;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutorFactory;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutorFactoryProviderConfig;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.CimdProvider;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.OIDCClientRepresentationBuilder;
import org.keycloak.testframework.oauth.annotations.InjectCimdProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.testframework.realm.ManagedUser;
import org.keycloak.testframework.remote.timeoffset.InjectTimeOffSet;
import org.keycloak.testframework.remote.timeoffset.TimeOffSet;
import org.keycloak.testframework.server.KeycloakServerConfig;
import org.keycloak.testframework.server.KeycloakServerConfigBuilder;
import org.keycloak.testframework.ui.annotations.InjectPage;
import org.keycloak.testframework.ui.page.ErrorPage;
import org.keycloak.testframework.ui.page.OAuthGrantPage;
import org.keycloak.tests.oauth.AbstractJWTAuthorizationGrantTest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.TokenRevocationResponse;
import org.keycloak.util.JsonSerialization;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

/**
 *
 * @author rmartinc
 */
@KeycloakIntegrationTest(config = ClientIdMetadataDocumentTest.CimdServerConfig.class)
@TestMethodOrder(MethodOrderer.MethodName.class)
public class ClientIdMetadataDocumentTest {

    private static final String CLIENT_ID = "http://localhost:8500/cimd/metadata";
    private static final String REDIRECT_URI = "http://localhost:8500/";
    private static final String JWKS_URI = "http://localhost:8500/idp/jwks";
    private static final String LOGO_URI = "http://localhost:8500/logo.png";
    private static final int CIMD_EXECUTOR_MIN_CACHE_TIME_SEC = 300;

    @InjectRealm
    protected ManagedRealm realm;

    @InjectUser(config = AbstractJWTAuthorizationGrantTest.FederatedUserConfiguration.class)
    protected ManagedUser user;

    @InjectOAuthIdentityProvider
    OAuthIdentityProvider identityProvider;

    @InjectCimdProvider(config = CimdClientConfig.class, lifecycle = LifeCycle.METHOD)
    CimdProvider cimd;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectTimeOffSet
    TimeOffSet timeOffSet;

    @InjectPage
    ErrorPage errorPage;

    @InjectPage
    protected OAuthGrantPage grantPage;

    @Test
    public void testClientIdUriSchemeCondition() {
        // register policies : no trusted domain
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("spiffe"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setTrustedDomains(List.of("localhost", "example.com","mcp.example.com"));

        updatePolicy(conditionConfig, executorConfig);

        // allowed scheme: spiffe
        // actual scheme: http
        // -> CIMD executor is not executed, so we expect the error showing that client is not found.
        oauth.client(CLIENT_ID);
        oauth.openLoginForm();
        errorPage.assertCurrent();
        Assertions.assertEquals("Client not found.", errorPage.getError());

        // update policies: trusted domain vacant
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of());
        updatePolicy(conditionConfig, executorConfig);

        // trusted domains: vacant
        // host: localhost
        // -> CIMD executor is not executed, so we expect the error showing that client is not found.
        oauth.client(CLIENT_ID);
        oauth.openLoginForm();
        errorPage.assertCurrent();
        Assertions.assertEquals("Client not found.", errorPage.getError());

        // update policies: trusted domain filled
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("example.com","mcp.example.com"));
        updatePolicy(conditionConfig, executorConfig);

        // trusted domains: example.com, mcp.example.com
        // host: localhost
        // -> CIMD executor is not executed, so we expect the error showing that client is not found.
        oauth.client(CLIENT_ID);
        oauth.openLoginForm();
        errorPage.assertCurrent();
        Assertions.assertEquals("Client not found.", errorPage.getError());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorForConfidentialClient() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        executorConfig.setRestrictSameDomain(true);
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setRequiredProperties(List.of("scope", "logo_uri", "client_uri", "tos_uri", "policy_uri", "jwks_uri"));
        updatePolicy(conditionConfig, executorConfig);

        // send an authorization request
        String code = loginUserAndGetCode(true);

        // get an access token
        String signedJwt = createSignedRequestToken();
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
        oauth.verifyToken(tokenResponse.getAccessToken());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertTrue(clientRepresentation.isConsentRequired()); // default
        Assertions.assertFalse(clientRepresentation.isFullScopeAllowed()); // default
        Assertions.assertFalse(clientRepresentation.isPublicClient());
        Assertions.assertEquals("client-jwt", clientRepresentation.getClientAuthenticatorType());

        // logout
        logout(tokenResponse.getIdToken());

        // change the client metadata
        cimd.getRepresentation().setScope("profile");
        cimd.getRepresentation().setLogoUri("http://localhost:8500/logo2.png");

        // do authorization code flow again, but registered client metadata is still effective
        code = loginUserAndGetCode(false);
        signedJwt = createSignedRequestToken();
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak does not fetch the client metadata.
        // the registered client metadata remains the same
        clientRepresentation = findByClientIdByAdmin();
        Map<String, String> m = clientRepresentation.getAttributes();
        List<String> optionalScopeList = clientRepresentation.getOptionalClientScopes();
        Assertions.assertEquals(LOGO_URI, m.get("logoUri"));
        Assertions.assertEquals(2, optionalScopeList.size());
        Assertions.assertTrue(optionalScopeList.contains("phone"));
        Assertions.assertTrue(optionalScopeList.contains("address"));
        Assertions.assertFalse(clientRepresentation.isPublicClient());
        Assertions.assertEquals("client-jwt", clientRepresentation.getClientAuthenticatorType());

        // logout
        logout(tokenResponse.getIdToken());

        // move the time ahead so that the client metadata becomes ineffective.
        timeOffSet.set(CIMD_EXECUTOR_MIN_CACHE_TIME_SEC + 3);

        // do authorization code flow again, and registered client metadata is not effective
        code = loginUserAndGetCode(false);
        signedJwt = createSignedRequestToken();
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak re-fetch the client metadata.
        // the registered client metadata changed
        clientRepresentation = findByClientIdByAdmin();
        m = clientRepresentation.getAttributes();
        optionalScopeList = clientRepresentation.getOptionalClientScopes();
        Assertions.assertEquals("http://localhost:8500/logo2.png", m.get("logoUri"));
        Assertions.assertEquals(1, optionalScopeList.size());
        Assertions.assertTrue(optionalScopeList.contains("profile"));

        // delete the persisted client
        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorForPublicClient() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        executorConfig.setRestrictSameDomain(true);
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setRequiredProperties(List.of("scope", "logo_uri", "client_uri", "tos_uri", "policy_uri"));
        updatePolicy(conditionConfig, executorConfig);

        // set Client Metadata
        cimd.getRepresentation().setTokenEndpointAuthMethod(null); // public client
        cimd.getRepresentation().setJwksUri(null);

        // send an authorization request
        String code = loginUserAndGetCode(true);

        // get an access token
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assertions.assertEquals(CLIENT_ID, accessToken.getIssuedFor());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertTrue(clientRepresentation.isConsentRequired()); // default
        Assertions.assertFalse(clientRepresentation.isFullScopeAllowed()); // default
        Assertions.assertTrue(clientRepresentation.isPublicClient());
        Assertions.assertEquals("none", clientRepresentation.getClientAuthenticatorType());

        // introspect
        IntrospectionResponse introspectionResponse = oauth.client("test-app", "test-secret").doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        Assertions.assertEquals(200, introspectionResponse.getStatusCode());
        Assertions.assertEquals(CLIENT_ID, introspectionResponse.asTokenMetadata().getClientId());

        // refresh
        tokenResponse = oauth.client(CLIENT_ID).doRefreshTokenRequest(tokenResponse.getRefreshToken());
        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assertions.assertEquals(CLIENT_ID, accessToken.getIssuedFor());

        // revoke
        TokenRevocationResponse revokeResponse = oauth.client(CLIENT_ID).doTokenRevoke(tokenResponse.getAccessToken());
        Assertions.assertEquals(200, revokeResponse.getStatusCode());
        introspectionResponse = oauth.client("test-app", "test-secret").doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        Assertions.assertEquals(200, introspectionResponse.getStatusCode());
        Assertions.assertFalse(introspectionResponse.asTokenMetadata().isActive());

        // get another token
        oauth.scope("phone");
        code = ssoLoginUserAndGetCode(OAuthGrantPage.PHONE_CONSENT_TEXT);
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assertions.assertEquals(CLIENT_ID, accessToken.getIssuedFor());
        Assertions.assertTrue(Arrays.asList(accessToken.getScope().split(" ")).contains("phone"));

        // delete the persisted client
        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorNotModifiedClientMetadata() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        executorConfig.setRestrictSameDomain(true);
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setRequiredProperties(List.of("logo_uri", "scope"));
        updatePolicy(conditionConfig, executorConfig);

        // send an authorization request
        String code = loginUserAndGetCode(true);

        // get an access token
        String signedJwt = createSignedRequestToken();
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
        oauth.verifyToken(tokenResponse.getAccessToken());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertTrue(clientRepresentation.isConsentRequired()); // default
        Assertions.assertFalse(clientRepresentation.isFullScopeAllowed()); // default

        // logout
        logout(tokenResponse.getIdToken());

        // change the client metadata
        // however, the client returns 304 Not Modified
        cimd.getRepresentation().setScope("profile");
        cimd.getRepresentation().setLogoUri("http://localhost:8500/logo2.png");
        cimd.setStatus(Response.Status.NOT_MODIFIED);

        // do authorization code flow again, but registered client metadata is still effective
        code = loginUserAndGetCode(false);
        signedJwt = createSignedRequestToken();
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak does not fetch the client metadata.
        // the registered client metadata remains the same
        clientRepresentation = findByClientIdByAdmin();
        Map<String, String> m = clientRepresentation.getAttributes();
        List<String> optionalScopeList = clientRepresentation.getOptionalClientScopes();
        Assertions.assertEquals(LOGO_URI, m.get("logoUri"));
        Assertions.assertEquals(2, optionalScopeList.size());
        Assertions.assertTrue(optionalScopeList.contains("phone"));
        Assertions.assertTrue(optionalScopeList.contains("address"));

        // logout
        logout(tokenResponse.getIdToken());

        // move the time ahead so that the client metadata becomes ineffective.
        timeOffSet.set(CIMD_EXECUTOR_MIN_CACHE_TIME_SEC + 3);

        // do authorization code flow again, and registered client metadata is not effective
        code = loginUserAndGetCode(false);
        signedJwt = createSignedRequestToken();
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak re-fetch the client metadata.
        // however, the client returns 304 Not Modified.
        // therefore, the client metadata remains the same.
        clientRepresentation = findByClientIdByAdmin();
        m = clientRepresentation.getAttributes();
        optionalScopeList = clientRepresentation.getOptionalClientScopes();
        Assertions.assertEquals(LOGO_URI, m.get("logoUri"));
        Assertions.assertEquals(2, optionalScopeList.size());
        Assertions.assertTrue(optionalScopeList.contains("phone"));
        Assertions.assertTrue(optionalScopeList.contains("address"));

        // delete the persisted client
        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorDefaultSetting() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowHttpScheme(true);
        updatePolicy(conditionConfig, executorConfig);

        // vacant trusted domains
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorFetchClientMetadataFailed() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        updatePolicy(conditionConfig, executorConfig);

        cimd.setStatus(Response.Status.NOT_FOUND);

        // 200 OK but malformed client metadata
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_FETCH_FAILED);
    }

    private void testClientIdMetadataDocumentExecutorCacheControl(String cacheControlHeaderValue, int expectedExpiry) {
        // set Client Metadata
        cimd.getRepresentation().setLogoUri("https://www.example.com");
        cimd.getRepresentation().setScope("address phone");
        cimd.setCacheControlHeader(cacheControlHeaderValue);

        // send an authorization request
        String code = loginUserAndGetCode(true);

        // get an access token
        String signedJwt = createSignedRequestToken();
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
        oauth.verifyToken(tokenResponse.getAccessToken());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertTrue(clientRepresentation.isConsentRequired()); // default
        Assertions.assertFalse(clientRepresentation.isFullScopeAllowed()); // default

        // logout
        logout(tokenResponse.getIdToken());

        // change the client metadata
        cimd.getRepresentation().setLogoUri(LOGO_URI);
        cimd.getRepresentation().setScope("profile");

        Map<String, String> m;
        List<String> optionalScopeList;
        if (expectedExpiry > 0) {
            // do authorization code flow again, but registered client metadata is still effective
            code = loginUserAndGetCode(false);
            signedJwt = createSignedRequestToken();
            tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
            oauth.verifyToken(tokenResponse.getAccessToken());

            // therefore, keycloak does not fetch the client metadata.
            // the registered client metadata remains the same
            clientRepresentation = findByClientIdByAdmin();
            m = clientRepresentation.getAttributes();
            optionalScopeList = clientRepresentation.getOptionalClientScopes();
            Assertions.assertEquals("https://www.example.com", m.get("logoUri"));
            Assertions.assertEquals(2, optionalScopeList.size());
            Assertions.assertTrue(optionalScopeList.contains("phone"));
            Assertions.assertTrue(optionalScopeList.contains("address"));

            // logout
            logout(tokenResponse.getIdToken());

            // force both the client and the server time to go forward to shorten the completion time of the test
            timeOffSet.set(expectedExpiry + 3);
        }

        // do authorization code flow again, and registered client metadata is not effective
        code = loginUserAndGetCode(false);
        signedJwt = createSignedRequestToken();
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak re-fetch the client metadata.
        // the registered client metadata changed
        clientRepresentation = findByClientIdByAdmin();
        m = clientRepresentation.getAttributes();
        optionalScopeList = clientRepresentation.getOptionalClientScopes();
        Assertions.assertEquals(LOGO_URI, m.get("logoUri"));
        Assertions.assertEquals(1, optionalScopeList.size());
        Assertions.assertTrue(optionalScopeList.contains("profile"));

        // logout
        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
        timeOffSet.set(0);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorCacheControl() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        updatePolicy(conditionConfig, executorConfig);

        // CIMD Executor's min cache time default value: 300 sec
        // CIMD Executor's max cache time default value: 259200 sec

        // no Cache-Control header : cached in min cache time
        testClientIdMetadataDocumentExecutorCacheControl(null, CIMD_EXECUTOR_MIN_CACHE_TIME_SEC);

        // empty Cache-Control header : cached in min cache time
        testClientIdMetadataDocumentExecutorCacheControl("", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC);

        // max-age : max-age considered
        testClientIdMetadataDocumentExecutorCacheControl("max-age=320,    private", 320);

        // s-maxage : s-maxage considered
        testClientIdMetadataDocumentExecutorCacheControl("private,S-MAXAGE=315,  no-transform", 315);

        // max-age and s-maxage : s-maxage considered
        testClientIdMetadataDocumentExecutorCacheControl(" Max-Age=3600,public,S-MaxAge=312", 312);

        // max-age and no-cache : cached in min cache time
        testClientIdMetadataDocumentExecutorCacheControl("max-age=320, NO-CACHE  ", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC);

        // s-maxage and no-store : cached in min cache time
        testClientIdMetadataDocumentExecutorCacheControl("S-MAXAGE=320,no-store", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC);

        // unknown values only : cached in min cache time
        // min-age=20, CACHE
        testClientIdMetadataDocumentExecutorCacheControl("min-age=20,CACHE ", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC);

        // under the min cache time : 5 -> 300
        testClientIdMetadataDocumentExecutorCacheControl("max-age=5", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC);

        // over the max cache time : 365000 -> 259200
        testClientIdMetadataDocumentExecutorCacheControl("s-maxage=365000", 259200);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorClientMetadataUpperLimit() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        updatePolicy(conditionConfig, executorConfig);

        // set Client Metadata
        // CIMD Executor's client metadata upper limit byte length default value: 5000
        cimd.getRepresentation().setLogoUri("0123456789".repeat(460));
        MatcherAssert.assertThat(Long.valueOf(JsonSerialization.writeValueAsBytes(cimd.getRepresentation()).length),
                Matchers.greaterThan(ClientIdMetadataDocumentExecutorFactoryProviderConfig.DEFAULT_CONFIG_UPPER_LIMIT_METADATA_BYTES));

        // send an authorization request - fail
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_FETCH_FAILED);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorVerifyAuthorizationRequest() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setTrustedDomains(List.of("localhost"));
        updatePolicy(conditionConfig, executorConfig);

        oauth.redirectUri(null);
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_INVALID_PARAMETER);
        oauth.redirectUri(REDIRECT_URI);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorVerifyClientId() throws Exception {
        // register profiles
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost", "127.0.0.1",
                "10.0.0.0", "example.co.jp", "10.255.255.255", "172.16.0.0", "[::1]", "[0:0:0:0:0:0:0:1]",
                "172.31.255.255", "192.168.0.0", "192.168.255.255", "[fe12:3456:789a:1::]"));
        updatePolicy(AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation(),
                ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig);

        // Client ID Verification:
        // Client identifier URLs MUST have a "https" scheme.
        assertLoginAndError("Malformed URL", AbstractClientIdMetadataDocumentExecutor.ERR_CLIENTID_MALFORMED_URL);

        // Client ID Verification:
        // Client identifier URLs MUST have a "https" scheme.
        assertLoginAndError("http://mcp.example.co.jp/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_CLIENTID_INVALID_SCHEME);

        // Client ID Verification:
        // Client identifier URLs MUST contain a path component.
        assertLoginAndError("https://example.com", AbstractClientIdMetadataDocumentExecutor.ERR_CLIENTID_EMPTY_PATH);

        // Client ID Verification:
        // Client identifier URLs MUST NOT contain single-dot or double-dot path segments.
        assertLoginAndError("https://example.com/mcp/.././index.html", AbstractClientIdMetadataDocumentExecutor.ERR_CLIENTID_PATH_TRAVERSAL);

        // Client ID Verification:
        // Client identifier URLs MUST NOT contain a fragment component.
        assertLoginAndError("https://example.com/mcp#prompts", AbstractClientIdMetadataDocumentExecutor.ERR_CLIENTID_FRAGMENT);

        // Client ID Verification:
        // Client identifier URLs MUST NOT contain a username or password.
        assertLoginAndError("https://alice:wonderland@example.com/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_CLIENTID_USERINFO);

        // Client ID Verification:
        // Client identifier URLs SHOULD NOT include a query string component.
        assertLoginAndError("https://example.com/mcp?p1=123&p2=abc", AbstractClientIdMetadataDocumentExecutor.ERR_CLIENTID_QUERY);

        // Client ID Verification:
        assertLoginAndError("https://example.co.jp/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_FETCH_FAILED);

        // Client ID Verification:
        // Client identifier is not a valid IPv6 address
        assertLoginAndError("https://0:0:0:0:0:0:0:1/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_HOST_UNRESOLVED);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorVerifyClientId2() throws Exception {
        // update profiles
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setTrustedDomains(List.of("*.example.com"));
        updatePolicy(AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation(),
                ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://localhost:8443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://127.0.0.1:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://[::1]:8443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://[0:0:0:0:0:0:0:1]/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://10.0.0.0:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://10.255.255.255:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://172.16.0.0:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://172.31.255.255:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://192.168.0.0:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://192.168.255.255:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorVerifyClientMetadata() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost",
                "mcpclient.example.org", "www.example.org",
                "[::1]", "client.example.com"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost",
                "mcpclient.example.org", "www.example.org",
                "[::1]", "client.example.com"));
        updatePolicy(conditionConfig, executorConfig);

        oauth.redirectUri(REDIRECT_URI);

        // Client Metadata Verification:
        // The client metadata document MUST contain a client_id property.
        // clientId = not set
        // clientUri = https://localhost:8543/auth/realms/master/app/auth
        // jwksUri = https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/get-jwks
        cimd.getRepresentation().setClientId(null);
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_NOCLIENTID);

        // Client Metadata Verification:
        // The client_id property's value MUST match the URL of the document
        // using simple string comparison as defined in [RFC3986] Section 6.2.1.
        cimd.getRepresentation().setClientId(CLIENT_ID + "/something");
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_CLIENTID_UNMATCH);

        // Client Metadata Verification:
        // The token_endpoint_auth_method property MUST NOT include
        // client_secret_post, client_secret_basic, client_secret_jwt,
        // or any other method based around a shared symmetric secret.
        cimd.getRepresentation().setClientId(CLIENT_ID);
        cimd.getRepresentation().setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_JWT);
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_NOTALLOWED_CLIENTAUTH);

        // Client Metadata Verification:
        // The client_secret and client_secret_expires_at properties MUST NOT be used.
        cimd.getRepresentation().setTokenEndpointAuthMethod(null);
        cimd.getRepresentation().setClientSecret("ClientSecretNotAllowed");
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_CLIENTSECRET);

        // Client Metadata Verification:
        // An authorization server MUST validate redirect URIs presented in an authorization request
        // against those in the metadata document.
        cimd.getRepresentation().setClientSecret(null);
        cimd.getRepresentation().setRedirectUris(List.of("https://mcp.example.com/mcp/123abc"));
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_REDIRECTURI);

        // SSRF attack countermeasure
        // It checks if an address resolved from a property whose value is URI is loopback address.
        //  -> difficult to check because allowing loopback address is needed to fetch client metadata from loop-back addressed test-provider server
        //  -> also jwks_uri is hosted by also the loop-back addressed test-provider server.
        // It checks if an address resolved from a property whose value is URI is private address.
        // RFC 7591: logo_uri, client_uri, tos_uri, policy_uri, jwks_uri.

        // logo_uri : private address
        cimd.getRepresentation().setRedirectUris(List.of(REDIRECT_URI));
        cimd.getRepresentation().setLogoUri("https://10.255.255.255:443/mcp");
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // client_uri : not allowed domain
        cimd.getRepresentation().setLogoUri("https://localhost:8443/logo");
        cimd.getRepresentation().setClientUri("https://mcpclient.example.or.jp/client");
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // tos_uri : private address
        cimd.getRepresentation().setLogoUri("https://localhost:8443/logo");
        cimd.getRepresentation().setClientUri("https://www.example.org");
        cimd.getRepresentation().setTosUri("https://172.31.255.254:443/mcp");
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // policy_uri : not allowed domain
        cimd.getRepresentation().setLogoUri("https://localhost:8443/logo");
        cimd.getRepresentation().setClientUri("https://www.example.org");
        cimd.getRepresentation().setTosUri("https://[::1]:8443/mcp");
        cimd.getRepresentation().setPolicyUri("https://client.example.co.de");
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // jwks_uri : private address
        cimd.getRepresentation().setLogoUri("https://localhost:8443/logo");
        cimd.getRepresentation().setClientUri("https://www.example.org");
        cimd.getRepresentation().setTosUri("https://[::1]:8443/mcp");
        cimd.getRepresentation().setPolicyUri("https://localhost:8443/policy");
        cimd.getRepresentation().setJwksUri("https://10.255.255.1:443/mcp");
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorValidateClientMetadataConfidentialClient() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setOnlyAllowConfidentialClient(true);
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        updatePolicy(conditionConfig, executorConfig);

        oauth.redirectUri(REDIRECT_URI);

        // Client Metadata Validation:
        // Only a confidential client is allowed.
        cimd.getRepresentation().setTokenEndpointAuthMethod(null);
        assertLoginAndError(ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_CONFIDENTIAL_CLIENT);

        // Client Metadata Validation:
        // ether jwks or jwks_uri is required.
        cimd.getRepresentation().setTokenEndpointAuthMethod(OIDCLoginProtocol.PRIVATE_KEY_JWT);
        cimd.getRepresentation().setJwksUri(null);
        assertLoginAndError(ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorValidateClientMetadataRestrictSameDomain() throws Exception {
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setOnlyAllowConfidentialClient(false);
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost", "www.example.co.jp"));
        executorConfig.setRestrictSameDomain(true);
        updatePolicy(conditionConfig, executorConfig);

        // Client Metadata Validation:
        // An authorization server MAY impose restrictions or relationships
        // between the redirect_uris and the client_id or client_uri properties
        //
        // same domain

        oauth.redirectUri("https://www.example.com/callback");
        cimd.getRepresentation().setRedirectUris(List.of("https://localhost:8443/callback", "https://www.example.com/callback"));
        assertLoginAndError(ClientIdMetadataDocumentExecutor.ERR_METADATA_URIS_SAMEDOMAIN);

        // All URIs under the same domain of permitted domains
        oauth.redirectUri(REDIRECT_URI);
        cimd.getRepresentation().setRedirectUris(List.of(REDIRECT_URI));
        cimd.getRepresentation().setLogoUri("https://localhost:8443/logo");
        cimd.getRepresentation().setClientUri("https://www.example.com");
        cimd.getRepresentation().setTosUri("https://www.example.co.jp/mcp");
        cimd.getRepresentation().setPolicyUri("https://localhost/mcp");
        assertLoginAndError(ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_ALL_URIS_SAMEDOMAIN);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorValidateClientMetadataRequiredProperties() throws Exception {
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setTrustedDomains(List.of("*.example.com", "[::1]", "localhost", "127.0.0.1"));
        executorConfig.setRequiredProperties(List.of("client_uri", "logo_uri"));
        updatePolicy(conditionConfig, executorConfig);

        oauth.redirectUri(REDIRECT_URI);
        cimd.getRepresentation().setLogoUri(null);
        assertLoginAndError(ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_REQUIRED_PROPERTIES);
    }

    private String loginUserAndGetCode(boolean isGrantRequred) {
        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.loginForm().codeChallenge(null).open();
        oauth.fillLoginForm(user.getUsername(), user.getPassword());

        if (isGrantRequred) {
            grantPage.assertCurrent();
            grantPage.assertGrants("The client's hostname is localhost");
            grantPage.accept();
        }

        String code = oauth.parseLoginResponse().getCode();
        Assertions.assertNotNull(code);
        return code;
    }

    private String ssoLoginUserAndGetCode(String expectedGrant) {
        oauth.client(CLIENT_ID);
        oauth.loginForm().codeChallenge(null).open();

        if (expectedGrant != null) {
            grantPage.assertCurrent();
            grantPage.assertGrants(expectedGrant);
            grantPage.accept();
        }

        String code = oauth.parseLoginResponse().getCode();
        Assertions.assertNotNull(code);
        return code;
    }

    private void assertLoginAndError(String errorMessage) {
        assertLoginAndError(CLIENT_ID, errorMessage);
    }

    private void assertLoginAndError(String clientId, String errorMessage) {
        oauth.client(clientId);
        oauth.openLoginForm();
        errorPage.assertCurrent();
        Assertions.assertEquals(errorMessage, errorPage.getError());
    }

    private void updatePolicy(ClientIdUriSchemeCondition.Configuration conditionConfig,
            ClientIdMetadataDocumentExecutor.Configuration executorConfig) {
        updatePolicy(ClientIdUriSchemeConditionFactory.PROVIDER_ID, conditionConfig,
                ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig);
    }

    private void updatePolicy(String conditionProvider, ClientPolicyConditionConfigurationRepresentation conditionConfig,
            String executorProvider, ClientIdMetadataDocumentExecutor.Configuration executorConfig) {
        realm.updateWithCleanup(r -> {
            r.resetClientProfiles()
                    .clientProfile(ClientProfileBuilder.create()
                    .name("executor")
                    .description("executor description")
                    .executor(executorProvider, executorConfig)
                    .build());
            r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                    .name("policy")
                    .description("description of policy")
                    .condition(conditionProvider, conditionConfig)
                    .profile("executor")
                    .build());
            return r;
        });
    }

    private String createSignedRequestToken() {
        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(KeycloakModelUtils.generateId());
        reqToken.issuer(CLIENT_ID);
        reqToken.subject(CLIENT_ID);
        reqToken.audience(oauth.getEndpoints().getIssuer());

        int now = Time.currentTime();
        reqToken.iat((long) now);
        reqToken.exp((long) (now + 10));
        reqToken.nbf((long) now);

        return identityProvider.encodeToken(reqToken);
    }

    private ClientRepresentation findByClientIdByAdmin() {
        List<ClientRepresentation> clients = realm.admin().clients().findByClientId(CLIENT_ID);
        Assertions.assertEquals(1, clients.size());
        return clients.iterator().next();
    }

    private void deleteClientByAdmin(String id) {
        realm.admin().clients().get(id).remove();
    }

    private void logout(String idToken) {
        oauth.scope(null).logoutRequest().idTokenHint(idToken).send();
    }

    private void logoutAndDelete(String cid, String idToken) {
        logout(idToken);
        deleteClientByAdmin(cid);
    }

    public static class CimdServerConfig implements KeycloakServerConfig {
        @Override
        public KeycloakServerConfigBuilder configure(KeycloakServerConfigBuilder config) {
            return config.features(Profile.Feature.CIMD);
        }
    }

    public static class CimdClientConfig implements OIDCClientRepresentationBuilder {
        @Override
        public OIDCClientRepresentation build() {
            OIDCClientRepresentation client = new OIDCClientRepresentation();
            client.setClientId(CLIENT_ID);
            client.setRedirectUris(List.of(REDIRECT_URI));
            client.setJwksUri(JWKS_URI);
            client.setClientName("sample");
            client.setClientUri("http://localhost:8500");
            client.setTokenEndpointAuthMethod(OIDCLoginProtocol.PRIVATE_KEY_JWT);
            client.setGrantTypes(List.of("authorization_code", "refresh_token"));
            client.setLogoUri(LOGO_URI);
            client.setTosUri("https://localhost:8500/tos");
            client.setPolicyUri("https://localhost:8500/policy");
            client.setScope("address phone");
            return client;
        }
    }
}
