package org.keycloak.tests.client.policies;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Time;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.CibaConfig;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeCondition;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeConditionFactory;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutorFactory;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutorFactoryProviderConfig;
import org.keycloak.protocol.oauth2.cimd.provider.AbstractPersistentClientIdMetadataDocumentProvider;
import org.keycloak.protocol.oauth2.cimd.resourceindicators.CimdResourceIndicatorAudienceMapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.mappers.AudienceProtocolMapper;
import org.keycloak.protocol.oidc.mappers.PairwiseSubMapperHelper;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.resourceindicators.ResourceIndicatorConstants;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyMode;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesCondition;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.GrantTypeCondition;
import org.keycloak.services.clientpolicy.condition.GrantTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforcerExecutor;
import org.keycloak.services.clientpolicy.executor.PKCEEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectImplicitGrantExecutor;
import org.keycloak.services.clientpolicy.executor.RejectImplicitGrantExecutorFactory;
import org.keycloak.services.clientpolicy.executor.RejectResourceOwnerPasswordCredentialsGrantExecutor;
import org.keycloak.services.clientpolicy.executor.RejectResourceOwnerPasswordCredentialsGrantExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUrisEnforcerExecutorFactory;
import org.keycloak.testframework.annotations.InjectClient;
import org.keycloak.testframework.annotations.InjectHttpClient;
import org.keycloak.testframework.annotations.InjectHttpServer;
import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.InjectUser;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.oauth.CimdProvider;
import org.keycloak.testframework.oauth.OAuthClient;
import org.keycloak.testframework.oauth.OAuthIdentityProvider;
import org.keycloak.testframework.oauth.OIDCClientRepresentationBuilder;
import org.keycloak.testframework.oauth.SectorIdentifierRedirectUrisProvider;
import org.keycloak.testframework.oauth.annotations.InjectCimdProvider;
import org.keycloak.testframework.oauth.annotations.InjectOAuthClient;
import org.keycloak.testframework.oauth.annotations.InjectOAuthIdentityProvider;
import org.keycloak.testframework.oauth.annotations.InjectSectorIdentifierRedirectUrisProvider;
import org.keycloak.testframework.realm.ClientBuilder;
import org.keycloak.testframework.realm.ClientConfig;
import org.keycloak.testframework.realm.ClientPolicyBuilder;
import org.keycloak.testframework.realm.ClientProfileBuilder;
import org.keycloak.testframework.realm.ManagedClient;
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
import org.keycloak.tests.providers.client.policies.ResourceParameterClientPolicyCondition;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.testsuite.util.oauth.TokenRevocationResponse;
import org.keycloak.util.JsonSerialization;

import com.sun.net.httpserver.HttpServer;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
    private static final String RESOURCE = "https://mcp.example.com/api";
    private static final int CIMD_EXECUTOR_MIN_CACHE_TIME_SEC = 300;

    @InjectRealm
    protected ManagedRealm realm;

    @InjectUser(config = AbstractJWTAuthorizationGrantTest.FederatedUserConfiguration.class)
    protected ManagedUser user;

    @InjectOAuthIdentityProvider
    OAuthIdentityProvider identityProvider;

    @InjectCimdProvider(config = CimdClientConfig.class, lifecycle = LifeCycle.METHOD)
    CimdProvider cimd;

    @InjectSectorIdentifierRedirectUrisProvider
    SectorIdentifierRedirectUrisProvider sectorIdentifierRedirectUrisProvider;

    @InjectOAuthClient
    OAuthClient oauth;

    @InjectClient(config = StoredUriClientConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedClient storedUriClient;

    @InjectHttpClient(followRedirects = false)
    CloseableHttpClient httpClient;

    @InjectHttpServer
    HttpServer httpServer;

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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
    public void testResourceIndicatorPermissiveModeWithoutResourceParam() throws Exception {
        // Resource Indicators feature is enabled (server config), allow list is vacant, client not yet registered,
        // authorization request does not include the resource parameter -> CIMD should not enforce anything.
        updatePolicy(createDefaultConditionConfig(), createDefaultExecutorConfig());

        String code = loginUserAndGetCode(true);
        String signedJwt = createSignedRequestToken();
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(signedJwt).send();
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        String[] audience = accessToken.getAudience();
        Assertions.assertFalse(audience != null && Arrays.asList(audience).contains(RESOURCE));

        logoutAndDelete(findByClientIdByAdmin().getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorRejectedWhenAllowListVacant() throws Exception {
        // Resource Indicators feature is enabled (server config), allow list is vacant, client not yet registered,
        // authorization request includes the resource parameter -> invalid_target.
        updatePolicy(createDefaultConditionConfig(), createDefaultExecutorConfig());

        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.loginForm().codeChallenge(null).resource(RESOURCE).open();
        errorPage.assertCurrent();
        Assertions.assertEquals(ResourceIndicatorConstants.ERROR_INVALID_RESOURCE, errorPage.getError());
    }

    @Test
    public void testResourceIndicatorAllowedForNewClientAssignsAudienceMapper() throws Exception {
        // allow list includes the resource, client not yet registered -> client gets registered
        // and an audience mapper for the resource is attached to the client.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(createDefaultConditionConfig(), executorConfig);

        String code = loginUserAndGetCode(true, RESOURCE);
        String signedJwt = createSignedRequestToken();
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).resource(RESOURCE).signedJwt(signedJwt).send();
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        MatcherAssert.assertThat(Arrays.asList(accessToken.getAudience()), Matchers.hasItem(RESOURCE));

        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertTrue(hasResourceIndicatorAudienceMapper(clientRepresentation.getId(), RESOURCE));

        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorRejectedWhenNotInAllowList() throws Exception {
        // allow list does not include the resource requested, client not yet registered -> invalid_target
        // and no audience mapper is assigned to the newly registered client.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of("https://mcp.example.com/other-api"));
        updatePolicy(createDefaultConditionConfig(), executorConfig);

        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.loginForm().codeChallenge(null).resource(RESOURCE).open();
        errorPage.assertCurrent();
        Assertions.assertEquals(ResourceIndicatorConstants.ERROR_INVALID_RESOURCE, errorPage.getError());
    }

    @Test
    public void testResourceIndicatorRemovedFromClientWhenNoLongerAllowListed() throws Exception {
        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);

        String code = loginUserAndGetCode(true, RESOURCE);
        String signedJwt = createSignedRequestToken();
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).resource(RESOURCE).signedJwt(signedJwt).send();
        Assertions.assertTrue(tokenResponse.isSuccess());
        logout(tokenResponse.getIdToken());

        // 2. remove the resource from the allow list -> the client's own cache is still valid (NO_UPDATE),
        // but the resource indicator is still enforced on every authorization request.
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(conditionConfig, executorConfig);

        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.loginForm().codeChallenge(null).resource(RESOURCE).open();
        errorPage.assertCurrent();
        Assertions.assertEquals(ResourceIndicatorConstants.ERROR_INVALID_RESOURCE, errorPage.getError());

        // the audience mapper of the rejected resource got removed.
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertFalse(hasResourceIndicatorAudienceMapper(clientRepresentation.getId(), RESOURCE));

        deleteClientByAdmin(clientRepresentation.getId());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenNoLongerAllowListedAndResourceParamOmitted() throws Exception {
        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        logout(tokenResponse.getIdToken());

        // 2. remove the resource from the allow list, then send an authorization request without the resource parameter
        // -> the resource is no longer included in the token's audience.
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(conditionConfig, executorConfig);

        String code = loginUserAndGetCode(false);
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(createSignedRequestToken()).send();
        Assertions.assertTrue(tokenResponse.isSuccess());
        MatcherAssert.assertThat(getAudience(tokenResponse.getAccessToken()), Matchers.not(Matchers.hasItem(RESOURCE)));

        logoutAndDelete(findByClientIdByAdmin().getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenNoLongerAllowListedAndOtherResourceRequested() throws Exception {
        String otherResource = "https://mcp.example.com/other-api";

        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE, otherResource));
        updatePolicy(conditionConfig, executorConfig);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        logout(tokenResponse.getIdToken());

        // 2. remove only the resource from the allow list, then request the other still allow-listed resource
        // -> the audience mapper for the other resource is attached, and only the other resource is issued.
        executorConfig.setResourceIndicatorAllowList(List.of(otherResource));
        updatePolicy(conditionConfig, executorConfig);

        String code = loginUserAndGetCode(false, otherResource);
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).resource(otherResource).signedJwt(createSignedRequestToken()).send();
        Assertions.assertTrue(tokenResponse.isSuccess());
        MatcherAssert.assertThat(getAudience(tokenResponse.getAccessToken()), Matchers.not(Matchers.hasItem(RESOURCE)));

        String id = findByClientIdByAdmin().getId();
        Assertions.assertTrue(hasResourceIndicatorAudienceMapper(id, otherResource));
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));
        Assertions.assertTrue(isResourceIncludedInAudience(id, otherResource));

        logoutAndDelete(id, tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenNoLongerAllowListedOnRefreshTokenRequest() throws Exception {
        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        MatcherAssert.assertThat(getAudience(tokenResponse.getAccessToken()), Matchers.hasItem(RESOURCE));

        // 2. remove the resource from the allow list, then refresh the token without any new authorization request
        // -> the audience mapper does not add the resource, so the resource bound to the refresh token is no longer resolvable.
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(conditionConfig, executorConfig);

        AccessTokenResponse refreshResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).clientJwt(createSignedRequestToken()).send();
        Assertions.assertFalse(refreshResponse.isSuccess());
        Assertions.assertEquals(OAuthErrorException.INVALID_TARGET, refreshResponse.getError());

        logoutAndDelete(findByClientIdByAdmin().getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenNoLongerAllowListedOnCodeExchange() throws Exception {
        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);

        String code = loginUserAndGetCode(true, RESOURCE);

        // 2. remove the resource from the allow list between the authorization request and the code exchange
        // -> the token request does not get the audience of the revoked resource.
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(conditionConfig, executorConfig);

        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).resource(RESOURCE).signedJwt(createSignedRequestToken()).send();
        Assertions.assertFalse(tokenResponse.isSuccess());
        Assertions.assertEquals(OAuthErrorException.INVALID_TARGET, tokenResponse.getError());

        // the user session created by the authorization request remains, so log out by the admin.
        realm.admin().logoutAll();
        deleteClientByAdmin(findByClientIdByAdmin().getId());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenNoLongerAllowListedOnClientCredentialsGrant() throws Exception {
        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);
        cimd.getRepresentation().setGrantTypes(List.of(
                OAuth2Constants.AUTHORIZATION_CODE,
                OAuth2Constants.REFRESH_TOKEN,
                OAuth2Constants.CLIENT_CREDENTIALS
        ));

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();
        logout(tokenResponse.getIdToken());

        // the client credentials grant gets the audience of the resource while it is allow-listed.
        AccessTokenResponse ccResponse = oauth.clientCredentialsGrantRequest().clientJwt(createSignedRequestToken()).send();
        Assertions.assertTrue(ccResponse.isSuccess());
        MatcherAssert.assertThat(getAudience(ccResponse.getAccessToken()), Matchers.hasItem(RESOURCE));

        // 2. remove the resource from the allow list, then send a client credentials grant without any authorization request
        // -> the audience mapper is still attached to the client, but the resource is no longer included in the token's audience.
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(conditionConfig, executorConfig);

        ccResponse = oauth.clientCredentialsGrantRequest().clientJwt(createSignedRequestToken()).send();
        Assertions.assertTrue(ccResponse.isSuccess(), "error = " + ccResponse.getError() + ", " + ccResponse.getErrorDescription());
        MatcherAssert.assertThat(getAudience(ccResponse.getAccessToken()), Matchers.not(Matchers.hasItem(RESOURCE)));
        Assertions.assertTrue(hasResourceIndicatorAudienceMapper(id, RESOURCE));

        // requesting the revoked resource explicitly is rejected.
        ccResponse = oauth.clientCredentialsGrantRequest().clientJwt(createSignedRequestToken()).resource(RESOURCE).send();
        Assertions.assertEquals(OAuthErrorException.INVALID_TARGET, ccResponse.getError());

        // 3. add the resource to the allow list again -> the resource is included in the token's audience again,
        // without any authorization request.
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);

        ccResponse = oauth.clientCredentialsGrantRequest().clientJwt(createSignedRequestToken()).send();
        Assertions.assertTrue(ccResponse.isSuccess());
        MatcherAssert.assertThat(getAudience(ccResponse.getAccessToken()), Matchers.hasItem(RESOURCE));

        deleteClientByAdmin(id);
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenNoLongerAllowListedOnImplicitFlowWithSso() throws Exception {
        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);
        setCimdImplicitFlow();

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);

        // 2. remove the resource from the allow list, then send an implicit flow authorization request completed
        // through the existing SSO session in the same request -> the access token does not include the audience.
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(conditionConfig, executorConfig);

        try {
            oauth.responseType(OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN);
            oauth.loginForm().codeChallenge(null).nonce(KeycloakModelUtils.generateId()).open();
            String accessToken = getFragmentParameter(oauth.getDriver().getCurrentUrl(), OAuth2Constants.ACCESS_TOKEN);
            Assertions.assertNotNull(accessToken, "url = " + oauth.getDriver().getCurrentUrl());
            MatcherAssert.assertThat(getAudience(accessToken), Matchers.not(Matchers.hasItem(RESOURCE)));
        } finally {
            oauth.responseType(OIDCResponseType.CODE);
        }

        logoutAndDelete(findByClientIdByAdmin().getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenNoLongerAllowListedDuringImplicitFlowLogin() throws Exception {
        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);
        setCimdImplicitFlow();

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        logout(tokenResponse.getIdToken());

        // 2. start an implicit flow authorization request while the resource is allow-listed, then remove it from
        // the allow list before the user completes the login -> the issued access token does not include the audience.
        try {
            oauth.responseType(OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN);
            oauth.loginForm().codeChallenge(null).nonce(KeycloakModelUtils.generateId()).resource(RESOURCE).open();

            executorConfig.setResourceIndicatorAllowList(List.of());
            updatePolicy(conditionConfig, executorConfig);

            oauth.fillLoginForm(user.getUsername(), user.getPassword());
            String redirectedUrl = oauth.getDriver().getCurrentUrl();
            MatcherAssert.assertThat(redirectedUrl, Matchers.startsWith(REDIRECT_URI));
            String accessToken = getFragmentParameter(redirectedUrl, OAuth2Constants.ACCESS_TOKEN);
            Assertions.assertNotNull(accessToken, "url = " + redirectedUrl);
            MatcherAssert.assertThat(getAudience(accessToken), Matchers.not(Matchers.hasItem(RESOURCE)));
        } finally {
            oauth.responseType(OIDCResponseType.CODE);
        }

        realm.admin().logoutAll();
        deleteClientByAdmin(findByClientIdByAdmin().getId());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenConditionHasNullUriScheme() throws Exception {
        // 1. the condition includes a null URI scheme, which is ignored -> client registers and gets the audience
        // mapper assigned.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        conditionConfig.setClientIdUriSchemes(Arrays.asList(null, "http", "https"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));

        // 2. remove the resource from the allow list -> the policy still applies to the client, so the resource is
        // no longer issued.
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(conditionConfig, executorConfig);
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        logoutAndDelete(id, tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenClientIdChanged() throws Exception {
        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(createDefaultConditionConfig(), executorConfig);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));
        logout(tokenResponse.getIdToken());

        // 2. change the client ID to a non-URL one, by sending back the whole client representation including the
        // audience mapper -> the CIMD policy no longer applies to the client, so the resource is no longer issued,
        // although it is still allow-listed.
        ClientRepresentation clientRepresentation = realm.admin().clients().get(id).toRepresentation();
        clientRepresentation.setClientId("cimd-renamed-client");
        realm.admin().clients().get(id).update(clientRepresentation);
        Assertions.assertEquals("cimd-renamed-client", realm.admin().clients().get(id).toRepresentation().getClientId());
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        deleteClientByAdmin(id);
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenClientRoleCreated() throws Exception {
        // 1. the CIMD policy applies to the client unless it has a client role, and its allow list includes the
        // resource -> client registers and gets the audience mapper assigned.
        String role = "cimd-role";
        ClientRolesCondition.Configuration negatedClientRolesConditionConfig = new ClientRolesCondition.Configuration();
        negatedClientRolesConditionConfig.setRoles(List.of(role));
        negatedClientRolesConditionConfig.setNegativeLogic(Boolean.TRUE);
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updateCimdPolicies(executorConfig, null, createCimdPolicy("policy")
                .condition(ClientRolesConditionFactory.PROVIDER_ID, negatedClientRolesConditionConfig).profile("executor").build());

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));
        logout(tokenResponse.getIdToken());

        // 2. the client role is created -> the policy no longer applies to the client, so the resource is no longer issued.
        realm.admin().clients().get(id).roles().create(new RoleRepresentation(role, null, false));
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        deleteClientByAdmin(id);
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenClientRoleRemoved() throws Exception {
        // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(createDefaultConditionConfig(), executorConfig);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();
        logout(tokenResponse.getIdToken());

        // 2. the client gets a client role, and the CIMD policy also requires it -> the policy still applies to the
        // client, so the resource is still issued.
        String role = "cimd-role";
        realm.admin().clients().get(id).roles().create(new RoleRepresentation(role, null, false));
        ClientRolesCondition.Configuration clientRolesConditionConfig = new ClientRolesCondition.Configuration();
        clientRolesConditionConfig.setRoles(List.of(role));
        updateCimdPolicies(executorConfig, null, createCimdPolicy("policy")
                .condition(ClientRolesConditionFactory.PROVIDER_ID, clientRolesConditionConfig).profile("executor").build());
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));

        // 3. the client role is removed -> the policy no longer applies to the client, so the resource is no longer issued.
        realm.admin().clients().get(id).roles().deleteRole(role);
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        deleteClientByAdmin(id);
    }

    @Test
    public void testResourceIndicatorNotIssuedToClientImportedWithNotAllowListedResource() throws Exception {
        // the allow list does not include the resource.
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(conditionConfig, executorConfig);

        // import a client with an audience mapper added by CIMD for the resource (e.g. exported before the allow list
        // was tightened) -> the resource is not issued.
        ClientRepresentation importedClient = new ClientRepresentation();
        importedClient.setClientId(CLIENT_ID);
        importedClient.setProtocolMappers(List.of(createResourceIndicatorAudienceMapper(CimdResourceIndicatorAudienceMapper.PROVIDER_ID, RESOURCE)));
        PartialImportRepresentation partialImport = new PartialImportRepresentation();
        partialImport.setIfResourceExists(PartialImportRepresentation.Policy.FAIL.name());
        partialImport.setClients(List.of(importedClient));
        realm.admin().partialImport(partialImport).close();

        String id = findByClientIdByAdmin().getId();
        Assertions.assertTrue(hasResourceIndicatorAudienceMapper(id, RESOURCE));
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        // the resource is issued once the allow list includes it.
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(conditionConfig, executorConfig);
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));

        deleteClientByAdmin(id);
    }

    @Test
    public void testResourceIndicatorNotIssuedToNonUrlClientImportedWithAudienceMapper() throws Exception {
        // the allow list includes the resource.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(createDefaultConditionConfig(), executorConfig);

        // import a client whose client ID is not a URL with an audience mapper added by CIMD (e.g. a CIMD client
        // renamed before an export) -> CIMD does not manage the client, so the resource is not issued.
        String nonUrlClientId = "cimd-non-url-client";
        ClientRepresentation importedClient = new ClientRepresentation();
        importedClient.setClientId(nonUrlClientId);
        importedClient.setProtocolMappers(List.of(createResourceIndicatorAudienceMapper(CimdResourceIndicatorAudienceMapper.PROVIDER_ID, RESOURCE)));
        PartialImportRepresentation partialImport = new PartialImportRepresentation();
        partialImport.setIfResourceExists(PartialImportRepresentation.Policy.FAIL.name());
        partialImport.setClients(List.of(importedClient));
        realm.admin().partialImport(partialImport).close();

        List<ClientRepresentation> clients = realm.admin().clients().findByClientId(nonUrlClientId);
        Assertions.assertEquals(1, clients.size());
        String id = clients.get(0).getId();
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        deleteClientByAdmin(id);
    }

    @Test
    public void testResourceIndicatorAudienceMapperOfEarlierVersionConvertedOnAuthorizationRequest() throws Exception {
        // 1. the client registers without any resource, and the allow list does not include the resource.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(createDefaultConditionConfig(), executorConfig);

        String code = loginUserAndGetCode(true);
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(createSignedRequestToken()).send();
        Assertions.assertTrue(tokenResponse.isSuccess());
        String id = findByClientIdByAdmin().getId();
        logout(tokenResponse.getIdToken());

        // 2. the client gets an audience mapper for the resource of the type that the earlier versions used (e.g. by
        // importing a representation exported from such a version) -> it adds the resource regardless of the allow list.
        realm.admin().clients().get(id).getProtocolMappers()
                .createMapper(createResourceIndicatorAudienceMapper(AudienceProtocolMapper.PROVIDER_ID, RESOURCE)).close();
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));

        // 3. an authorization request of the client -> the audience mapper is converted, so that it adds the resource
        // only while the resource is allow-listed.
        code = loginUserAndGetCode(false);
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(createSignedRequestToken()).send();
        Assertions.assertTrue(tokenResponse.isSuccess());
        MatcherAssert.assertThat(getAudience(tokenResponse.getAccessToken()), Matchers.not(Matchers.hasItem(RESOURCE)));
        Assertions.assertTrue(hasResourceIndicatorAudienceMapper(id, RESOURCE));
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        logoutAndDelete(id, tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorAudienceMappersOfEarlierVersionConvertedOnRejectedAuthorizationRequest() throws Exception {
        String otherResource = "https://mcp.example.com/other-api";

        // 1. the client registers without any resource, and the allow list includes no resource.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of());
        updatePolicy(createDefaultConditionConfig(), executorConfig);

        try {
            String code = loginUserAndGetCode(true);
            AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(createSignedRequestToken()).send();
            Assertions.assertTrue(tokenResponse.isSuccess());
            String id = findByClientIdByAdmin().getId();
            logout(tokenResponse.getIdToken());

            // 2. the client gets audience mappers of the type that the earlier versions used, for two resources -> they
            // add the resources regardless of the allow list.
            realm.admin().clients().get(id).getProtocolMappers()
                    .createMapper(createResourceIndicatorAudienceMapper(AudienceProtocolMapper.PROVIDER_ID, RESOURCE)).close();
            realm.admin().clients().get(id).getProtocolMappers()
                    .createMapper(createResourceIndicatorAudienceMapper(AudienceProtocolMapper.PROVIDER_ID, otherResource)).close();
            Assertions.assertTrue(isResourceIncludedInAudience(id, otherResource));

            // 3. an authorization request for one of the resources is rejected -> although the request is rolled back,
            // the audience mapper of the rejected resource is removed and the other one is converted, so that neither
            // resource is issued.
            oauth.client(CLIENT_ID);
            oauth.redirectUri(REDIRECT_URI);
            oauth.loginForm().codeChallenge(null).resource(RESOURCE).open();
            errorPage.assertCurrent();
            Assertions.assertEquals(ResourceIndicatorConstants.ERROR_INVALID_RESOURCE, errorPage.getError());
            Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));
            Assertions.assertTrue(hasResourceIndicatorAudienceMapper(id, otherResource));
            Assertions.assertFalse(isResourceIncludedInAudience(id, otherResource));
        } finally {
            deleteCimdClientAndSessions();
        }
    }

    @Test
    public void testResourceIndicatorNotAddedOnIntrospectionOfTokenIssuedWithoutIt() throws Exception {
        // the client that introspects is not in the audience of the token, so let it introspect any token.
        ClientRepresentation introspector = realm.admin().clients().findByClientId("test-app").get(0);
        setAllowTokenIntrospectionWithoutAudienceCheck(introspector, true);
        try {
            // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
            ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
            ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
            executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
            updatePolicy(conditionConfig, executorConfig);

            AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
            logout(tokenResponse.getIdToken());

            // 2. remove the resource from the allow list, then get an access token -> it is issued without the resource.
            executorConfig.setResourceIndicatorAllowList(List.of());
            updatePolicy(conditionConfig, executorConfig);

            String code = loginUserAndGetCode(false);
            tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).signedJwt(createSignedRequestToken()).send();
            Assertions.assertTrue(tokenResponse.isSuccess());
            MatcherAssert.assertThat(getAudience(tokenResponse.getAccessToken()), Matchers.not(Matchers.hasItem(RESOURCE)));

            // 3. add the resource to the allow list again, then introspect that access token -> the resource is not
            // added to the introspection response, because the token has not been issued for it.
            executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
            updatePolicy(conditionConfig, executorConfig);

            IntrospectionResponse introspectionResponse = oauth.client("test-app", "test-secret").doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
            Assertions.assertEquals(200, introspectionResponse.getStatusCode());
            Assertions.assertTrue(introspectionResponse.asTokenMetadata().isActive());
            String[] audience = introspectionResponse.asTokenMetadata().getAudience();
            Assertions.assertFalse(audience != null && Arrays.asList(audience).contains(RESOURCE));
        } finally {
            setAllowTokenIntrospectionWithoutAudienceCheck(introspector, false);
            deleteCimdClientAndSessions();
        }
    }

    @Test
    public void testResourceIndicatorIssuedInLightweightAccessToken() throws Exception {
        // the client that introspects is not in the audience of the token, so let it introspect any token.
        ClientRepresentation introspector = realm.admin().clients().findByClientId("test-app").get(0);
        setAllowTokenIntrospectionWithoutAudienceCheck(introspector, true);
        try {
            // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
            ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
            executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
            updatePolicy(createDefaultConditionConfig(), executorConfig);

            AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);

            // 2. the client uses lightweight access tokens, then refreshes the token -> the lightweight access token
            // has the resource, and so has its introspection response, to which the audience mapper adds nothing.
            setClientAttribute(findByClientIdByAdmin(), Constants.USE_LIGHTWEIGHT_ACCESS_TOKEN_ENABLED, Boolean.TRUE.toString());

            AccessTokenResponse refreshResponse = oauth.client(CLIENT_ID).refreshRequest(tokenResponse.getRefreshToken()).clientJwt(createSignedRequestToken()).send();
            Assertions.assertTrue(refreshResponse.isSuccess());
            MatcherAssert.assertThat(getAudience(refreshResponse.getAccessToken()), Matchers.hasItem(RESOURCE));

            IntrospectionResponse introspectionResponse = oauth.client("test-app", "test-secret").doIntrospectionAccessTokenRequest(refreshResponse.getAccessToken());
            Assertions.assertEquals(200, introspectionResponse.getStatusCode());
            Assertions.assertTrue(introspectionResponse.asTokenMetadata().isActive());
            MatcherAssert.assertThat(Arrays.asList(introspectionResponse.asTokenMetadata().getAudience()), Matchers.hasItem(RESOURCE));
        } finally {
            setAllowTokenIntrospectionWithoutAudienceCheck(introspector, false);
            deleteCimdClientAndSessions();
        }
    }

    private void setAllowTokenIntrospectionWithoutAudienceCheck(ClientRepresentation client, boolean allow) {
        setClientAttribute(client, OIDCConfigAttributes.ALLOW_TOKEN_INTROSPECTION_WITHOUT_AUDIENCE_CHECK, Boolean.toString(allow));
    }

    private void setClientAttribute(ClientRepresentation client, String name, String value) {
        ClientRepresentation update = realm.admin().clients().get(client.getId()).toRepresentation();
        Map<String, String> attributes = new HashMap<>(update.getAttributes() == null ? Map.of() : update.getAttributes());
        attributes.put(name, value);
        update.setAttributes(attributes);
        realm.admin().clients().get(client.getId()).update(update);
    }

    // removes the client registered by a test and the sessions, also when the test has failed, so that the failure does
    // not affect the other tests.
    private void deleteCimdClientAndSessions() {
        realm.admin().logoutAll();
        realm.admin().clients().findByClientId(CLIENT_ID).forEach(client -> deleteClientByAdmin(client.getId()));
    }

    @Test
    public void testResourceIndicatorKeptWhenAllowListedAgainWhileAuthorizationRequestRejectsIt() throws Exception {
        // the sector identifier URI of the client is fetched when an authorization request updates the client metadata,
        // i.e. after the request has read the client policies and before the executor checks the resource, so that
        // the authorization request can be suspended there.
        String sectorIdentifierPath = "/suspendable-sector-identifier-redirect-uris";
        AtomicBoolean suspend = new AtomicBoolean();
        CountDownLatch suspended = new CountDownLatch(1);
        CountDownLatch resumed = new CountDownLatch(1);
        httpServer.createContext(sectorIdentifierPath, exchange -> {
            try (exchange) {
                if (suspend.get()) {
                    suspended.countDown();
                    resumed.await(30, TimeUnit.SECONDS);
                }
                byte[] redirectUris = JsonSerialization.writeValueAsBytes(List.of(REDIRECT_URI));
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(Response.Status.OK.getStatusCode(), redirectUris.length);
                exchange.getResponseBody().write(redirectUris);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        try {
            // 1. allow list includes the resource -> client registers and gets the audience mapper assigned.
            ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
            ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
            executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
            updatePolicy(conditionConfig, executorConfig);
            setCimdPublicClient();
            cimd.getRepresentation().setSubjectType("pairwise");
            cimd.getRepresentation().setSectorIdentifierUri("http://localhost:" + httpServer.getAddress().getPort() + sectorIdentifierPath);

            oauth.client(CLIENT_ID);
            oauth.redirectUri(REDIRECT_URI);
            String authorizationRequestUrl = oauth.loginForm().codeChallenge(null).resource(RESOURCE).build();
            Assertions.assertEquals(200, sendAuthorizationRequest(authorizationRequestUrl));
            String id = findByClientIdByAdmin().getId();
            Assertions.assertTrue(hasResourceIndicatorAudienceMapper(id, RESOURCE));

            // 2. remove the resource from the allow list, then send an authorization request for the resource, which
            // re-fetches the expired client metadata, and is suspended after it has read the client policies.
            executorConfig.setResourceIndicatorAllowList(List.of());
            updatePolicy(conditionConfig, executorConfig);
            timeOffSet.set(CIMD_EXECUTOR_MIN_CACHE_TIME_SEC + 3);
            suspend.set(true);
            CompletableFuture<Integer> authorizationRequest = CompletableFuture.supplyAsync(() -> sendAuthorizationRequest(authorizationRequestUrl));
            Assertions.assertTrue(suspended.await(30, TimeUnit.SECONDS));

            // 3. meanwhile, the resource is allow-listed again.
            executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
            updatePolicy(conditionConfig, executorConfig);

            // 4. the authorization request rejects the resource according to the client policies it has read -> the
            // audience mapper is kept, because the rejection is obsolete, so the resource is issued.
            resumed.countDown();
            Assertions.assertEquals(400, authorizationRequest.get(30, TimeUnit.SECONDS));
            Assertions.assertTrue(hasResourceIndicatorAudienceMapper(id, RESOURCE));
            Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));
        } finally {
            resumed.countDown();
            httpServer.removeContext(sectorIdentifierPath);
            deleteCimdClientAndSessions();
        }
    }

    private int sendAuthorizationRequest(String url) {
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(url))) {
            EntityUtils.consume(response.getEntity());
            return response.getStatusLine().getStatusCode();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void testResourceIndicatorIssuedWhenStrictPolicyNeverAppliesOnAuthorizationRequest() throws Exception {
        // the other CIMD policy is in the strict mode and has a grant-type condition, which abstains on authorization
        // requests, so its executor never works.
        GrantTypeCondition.Configuration grantTypeConditionConfig = new GrantTypeCondition.Configuration();
        grantTypeConditionConfig.setGrantTypes(List.of(OAuth2Constants.AUTHORIZATION_CODE));
        testResourceIndicatorIssuedWhenOtherPolicyNeverApplies(createCimdPolicy("other-policy").mode(ClientPolicyMode.STRICT)
                .condition(GrantTypeConditionFactory.PROVIDER_ID, grantTypeConditionConfig)
                .profile("other-executor").build());
    }

    @Test
    public void testResourceIndicatorIssuedWhenNegatedAnyClientPolicyNeverApplies() throws Exception {
        // the other CIMD policy has a negated any-client condition, which never matches, so its executor never works.
        ClientPolicyConditionConfigurationRepresentation negatedAnyClientConfig = new ClientPolicyConditionConfigurationRepresentation();
        negatedAnyClientConfig.setNegativeLogic(Boolean.TRUE);
        testResourceIndicatorIssuedWhenOtherPolicyNeverApplies(ClientPolicyBuilder.create().name("other-policy")
                .condition(AnyClientConditionFactory.PROVIDER_ID, negatedAnyClientConfig)
                .profile("other-executor").build());
    }

    private void testResourceIndicatorIssuedWhenOtherPolicyNeverApplies(ClientPolicyRepresentation otherPolicy) throws Exception {
        // 1. a CIMD policy allows the resource -> client registers and gets the audience mapper assigned.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        updatePolicy(createDefaultConditionConfig(), executorConfig);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();

        // 2. add another CIMD policy that allows nothing, but never applies to the client on authorization requests
        // -> the resource is still issued.
        ClientIdMetadataDocumentExecutor.Configuration otherExecutorConfig = createDefaultExecutorConfig();
        otherExecutorConfig.setResourceIndicatorAllowList(List.of());
        updateCimdPolicies(executorConfig, otherExecutorConfig, createCimdPolicy("policy").profile("executor").build(), otherPolicy);
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));
        AccessTokenResponse refreshResponse = oauth.refreshRequest(tokenResponse.getRefreshToken()).clientJwt(createSignedRequestToken()).send();
        Assertions.assertTrue(refreshResponse.isSuccess());

        logoutAndDelete(id, tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorIssuedWhenPolicyForOtherDomainDoesNotAllowIt() throws Exception {
        // a CIMD policy for the client's domain allows the resource, and another CIMD policy for another domain allows
        // nothing -> the policy for the other domain does not apply to the client, so the resource is issued.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        ClientIdUriSchemeCondition.Configuration otherConditionConfig = new ClientIdUriSchemeCondition.Configuration();
        otherConditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        otherConditionConfig.setTrustedDomains(List.of("*.example.com"));
        ClientIdMetadataDocumentExecutor.Configuration otherExecutorConfig = createDefaultExecutorConfig();
        otherExecutorConfig.setResourceIndicatorAllowList(List.of());
        updateCimdPolicies(executorConfig, otherExecutorConfig, createCimdPolicy("policy").profile("executor").build(),
                ClientPolicyBuilder.create().name("other-policy").condition(ClientIdUriSchemeConditionFactory.PROVIDER_ID, otherConditionConfig)
                        .profile("other-executor").build());

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));

        logoutAndDelete(id, tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenAnyApplicablePolicyNoLongerAllowsIt() throws Exception {
        // two CIMD policies both applicable to the client allow the resource.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        ClientIdMetadataDocumentExecutor.Configuration otherExecutorConfig = createDefaultExecutorConfig();
        otherExecutorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        ClientPolicyRepresentation policy = createCimdPolicy("policy").profile("executor").build();
        ClientPolicyRepresentation otherPolicy = createCimdPolicy("other-policy").profile("other-executor").build();
        updateCimdPolicies(executorConfig, otherExecutorConfig, policy, otherPolicy);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));

        // only one of them no longer allows the resource -> an authorization request with the resource would be
        // rejected by that policy's executor, so the resource is no longer issued.
        otherExecutorConfig.setResourceIndicatorAllowList(List.of());
        updateCimdPolicies(executorConfig, otherExecutorConfig, policy, otherPolicy);
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        logoutAndDelete(id, tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorNotIssuedWhenAnyExecutorOfPossiblyApplicablePolicyNoLongerAllowsIt() throws Exception {
        // a CIMD policy with two CIMD executors both allowing the resource. whether the policy applies depends on the
        // values of an actual request, so it only might apply to the client.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        ClientIdMetadataDocumentExecutor.Configuration otherExecutorConfig = createDefaultExecutorConfig();
        otherExecutorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        ClientPolicyRepresentation policy = createCimdPolicy("policy")
                .condition(ResourceParameterClientPolicyCondition.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation())
                .profile("executor", "other-executor").build();
        updateCimdPolicies(executorConfig, otherExecutorConfig, policy);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));

        // only one of them no longer allows the resource -> both executors work whenever the policy applies, so an
        // authorization request with the resource would be rejected, and the resource is no longer issued.
        otherExecutorConfig.setResourceIndicatorAllowList(List.of());
        updateCimdPolicies(executorConfig, otherExecutorConfig, policy);
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        logoutAndDelete(id, tokenResponse.getIdToken());
    }

    @Test
    public void testResourceIndicatorRemovedFromClientWhenRejectedByExecutorOfPossiblyApplicablePolicy() throws Exception {
        // a CIMD policy always applicable to the client, and another CIMD policy that only might apply to the client,
        // depending on the values of an actual request, both allow the resource.
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        ClientIdMetadataDocumentExecutor.Configuration otherExecutorConfig = createDefaultExecutorConfig();
        otherExecutorConfig.setResourceIndicatorAllowList(List.of(RESOURCE));
        ClientPolicyRepresentation policy = createCimdPolicy("policy").profile("executor").build();
        ClientPolicyRepresentation otherPolicy = createCimdPolicy("other-policy")
                .condition(ResourceParameterClientPolicyCondition.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation())
                .profile("other-executor").build();
        updateCimdPolicies(executorConfig, otherExecutorConfig, policy, otherPolicy);

        AccessTokenResponse tokenResponse = loginAndGetTokens(RESOURCE);
        String id = findByClientIdByAdmin().getId();
        logout(tokenResponse.getIdToken());

        // the policy that only might apply no longer allows the resource -> it alone does not deny the resource, so
        // the resource is still issued.
        otherExecutorConfig.setResourceIndicatorAllowList(List.of());
        updateCimdPolicies(executorConfig, otherExecutorConfig, policy, otherPolicy);
        Assertions.assertTrue(isResourceIncludedInAudience(id, RESOURCE));

        // an authorization request with the resource is rejected by the executor of that policy -> the audience mapper
        // is removed although the request is rolled back, so the resource is no longer issued.
        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.loginForm().codeChallenge(null).resource(RESOURCE).open();
        errorPage.assertCurrent();
        Assertions.assertEquals(ResourceIndicatorConstants.ERROR_INVALID_RESOURCE, errorPage.getError());
        Assertions.assertFalse(hasResourceIndicatorAudienceMapper(id, RESOURCE));
        Assertions.assertFalse(isResourceIncludedInAudience(id, RESOURCE));

        deleteClientByAdmin(id);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorForPublicClient() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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

        // Add audience mapper to allow test-app to introspect tokens
        ClientRepresentation client = findByClientIdByAdmin();
        ProtocolMapperRepresentation audienceMapper = ModelToRepresentation.toRepresentation(
            AudienceProtocolMapper.createClaimMapper(
                "test-app-audience",
                "test-app",
                null,
                true,
                false,
                false
            )
        );
        realm.admin().clients().get(client.getId()).getProtocolMappers().createMapper(audienceMapper);

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
    public void testClientPolicyAutoConfigurationAppliedOnCimdCreation() throws Exception {
        // register CIMD profiles/policies
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setRestrictSameDomain(true);

        // Also register auto-configure executors for all clients
        RejectImplicitGrantExecutor.Configuration rejectImplicitConfig = new RejectImplicitGrantExecutor.Configuration();
        rejectImplicitConfig.setAutoConfigure(true);
        RejectResourceOwnerPasswordCredentialsGrantExecutor.Configuration rejectRopcConfig = new RejectResourceOwnerPasswordCredentialsGrantExecutor.Configuration();
        rejectRopcConfig.setAutoConfigure(true);
        PKCEEnforcerExecutor.Configuration pkceConfig = new PKCEEnforcerExecutor.Configuration();
        pkceConfig.setAutoConfigure(true);

        realm.updateWithCleanup(r -> {
            r.resetClientProfiles()
                    .clientProfile(ClientProfileBuilder.create()
                    .name("cimd-profile")
                    .description("CIMD executor profile")
                    .executor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig)
                    .build())
                    .clientProfile(ClientProfileBuilder.create()
                    .name("auto-configure-profile")
                    .description("Auto-configure executors profile")
                    .executor(RejectImplicitGrantExecutorFactory.PROVIDER_ID, rejectImplicitConfig)
                    .executor(RejectResourceOwnerPasswordCredentialsGrantExecutorFactory.PROVIDER_ID, rejectRopcConfig)
                    .executor(PKCEEnforcerExecutorFactory.PROVIDER_ID, pkceConfig)
                    .build());
            r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                    .name("cimd-policy")
                    .description("CIMD policy")
                    .condition(ClientIdUriSchemeConditionFactory.PROVIDER_ID, conditionConfig)
                    .profile("cimd-profile")
                    .build())
                    .clientPolicy(ClientPolicyBuilder.create()
                    .name("auto-configure-policy")
                    .description("Auto-configure policy for all clients")
                    .condition(AnyClientConditionFactory.PROVIDER_ID, null)
                    .profile("auto-configure-profile")
                    .build());
            return r;
        });

        // set Client Metadata for a public client
        setCimdPublicClient();

        // send an authorization request with PKCE (required by PKCEEnforcer)
        PkceGenerator pkce = PkceGenerator.s256();
        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.loginForm().codeChallenge(pkce).open();
        oauth.fillLoginForm(user.getUsername(), user.getPassword());

        grantPage.assertCurrent();
        grantPage.accept();

        String code = oauth.parseLoginResponse().getCode();
        Assertions.assertNotNull(code);

        // get an access token with PKCE verifier
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).codeVerifier(pkce.getCodeVerifier()).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());

        // check the persisted client settings - auto-configuration should have been applied
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertTrue(clientRepresentation.isPublicClient());
        // Verify auto-configuration was applied during CIMD client creation
        Assertions.assertFalse(clientRepresentation.isImplicitFlowEnabled(), "Implicit flow should be disabled by RejectImplicitGrantExecutor auto-configuration");
        Assertions.assertFalse(clientRepresentation.isDirectAccessGrantsEnabled(), "Direct access grants should be disabled by RejectResourceOwnerPasswordCredentialsGrantExecutor auto-configuration");
        Assertions.assertEquals("S256", clientRepresentation.getAttributes().get("pkce.code.challenge.method"), "PKCE should be configured by PKCEEnforcerExecutor auto-configuration");

        // delete the persisted client
        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
    }

    @ParameterizedTest
    @CsvSource({
            "http, code, 400",
            "http, , 400",
            "https, code, 200"
    })
    public void testSecureRedirectUrisEnforcerWithClientIdUriCondition(String redirectScheme,
            String responseType, int expectedStatus) throws Exception {
        String redirectUri = redirectScheme + "://client.example.com/callback";
        // The managed client's callbacks are stored before enabling the policy, as after an upgrade.
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("https"));
        conditionConfig.setTrustedDomains(List.of("client.example.com"));
        SecureRedirectUrisEnforcerExecutor.Configuration executorConfig = new SecureRedirectUrisEnforcerExecutor.Configuration();
        executorConfig.setAllowHttpScheme(false);
        realm.updateWithCleanup(r -> r.resetClientProfiles().clientProfile(ClientProfileBuilder.create()
                .name("redirect-uris-profile")
                .executor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID, executorConfig)
                .build())
                .resetClientPolicies().clientPolicy(ClientPolicyBuilder.create()
                        .name("redirect-uris-policy")
                        .condition(ClientIdUriSchemeConditionFactory.PROVIDER_ID, conditionConfig)
                        .profile("redirect-uris-profile")
                        .build()));

        oauth.client(storedUriClient.getClientId()).redirectUri(redirectUri).responseType(responseType).scope("openid");
        try (CloseableHttpResponse response = httpClient.execute(new HttpGet(oauth.loginForm().build()))) {
            Assertions.assertEquals(expectedStatus, response.getStatusLine().getStatusCode());
            Assertions.assertNull(response.getFirstHeader(HttpHeaders.LOCATION));
            String body = EntityUtils.toString(response.getEntity());
            if (expectedStatus == 400) {
                Assertions.assertTrue(body.contains(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI));
            } else {
                Assertions.assertTrue(body.contains("name=\"username\""));
            }
        }
    }

    @Test
    public void testSecureRedirectUrisEnforcerOnCimdCreate() throws Exception {
        // register CIMD policy and SecureRedirectUrisEnforcer policy
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();

        // SecureRedirectUrisEnforcer that does NOT allow http scheme
        SecureRedirectUrisEnforcerExecutor.Configuration redirectUrisConfig = new SecureRedirectUrisEnforcerExecutor.Configuration();
        redirectUrisConfig.setAllowHttpScheme(false);
        redirectUrisConfig.setAllowIPv4LoopbackAddress(true);

        updateCimdAndSecureRedirectUrisPolicy(conditionConfig, executorConfig, redirectUrisConfig);

        // set Client Metadata for a public client with http redirect URI (should be rejected)
        setCimdPublicClient();
        cimd.getRepresentation().setRedirectUris(List.of("http://localhost:8500/callback"));

        // The SecureRedirectUrisEnforcer should reject the http redirect URI during CIMD creation
        oauth.client(CLIENT_ID);
        oauth.redirectUri("http://localhost:8500/callback");
        oauth.openLoginForm();
        errorPage.assertCurrent();
        Assertions.assertEquals(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI, errorPage.getError());
    }

    @Test
    public void testSecureRedirectUrisEnforcerOnCimdUpdate() throws Exception {
        // register CIMD policy and SecureRedirectUrisEnforcer policy
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();

        // SecureRedirectUrisEnforcer that allows http scheme initially
        SecureRedirectUrisEnforcerExecutor.Configuration redirectUrisConfig = new SecureRedirectUrisEnforcerExecutor.Configuration();
        redirectUrisConfig.setAllowHttpScheme(true);
        redirectUrisConfig.setAllowIPv4LoopbackAddress(true);

        updateCimdAndSecureRedirectUrisPolicy(conditionConfig, executorConfig, redirectUrisConfig);

        // set Client Metadata for a public client with http redirect URI (allowed initially)
        setCimdPublicClient();

        // CIMD creation should succeed with http redirect URI since allowHttpScheme=true
        String code = loginUserAndGetCode(true);
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());

        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertTrue(clientRepresentation.isPublicClient());

        // logout
        logout(tokenResponse.getIdToken());

        // Now tighten the policy to disallow http scheme
        redirectUrisConfig.setAllowHttpScheme(false);
        updateCimdAndSecureRedirectUrisPolicy(conditionConfig, executorConfig, redirectUrisConfig);

        // Move the time ahead so that the client metadata cache expires
        timeOffSet.set(CIMD_EXECUTOR_MIN_CACHE_TIME_SEC + 3);

        // The CIMD update should now reject the http redirect URI
        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.openLoginForm();
        errorPage.assertCurrent();
        Assertions.assertEquals(SecureRedirectUrisEnforcerExecutor.ERR_NORMALURI, errorPage.getError());

        // cleanup
        deleteClientByAdmin(clientRepresentation.getId());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorNotModifiedClientMetadata() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setTrustedDomains(List.of("*.example.com","localhost"));
        updatePolicy(conditionConfig, executorConfig);

        cimd.setStatus(Response.Status.NOT_FOUND);

        // 200 OK but malformed client metadata
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_FETCH_FAILED);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorIgnoresUnknownProperties() throws Exception {
        // register profiles
        updatePolicy(createDefaultConditionConfig(), createDefaultExecutorConfig());

        // public client whose client metadata document contains additional, non-registered properties.
        // The CIMD specification (Section 4.1) allows a client metadata document to define additional properties,
        // and RFC 7591 (Section 2) requires ignoring any client metadata the authorization server does not understand.
        // "code_challenge_methods_used" is a non-registered property served by real-world MCP clients.
        setCimdPublicClient();
        cimd.setAdditionalProperties(Map.of(
                "code_challenge_methods_used", List.of("S256"),
                "x_unknown_property", "arbitrary-value"));

        // send an authorization request - success
        String code = loginUserAndGetCode(true);

        // get an access token
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assertions.assertEquals(CLIENT_ID, accessToken.getIssuedFor());

        // the unknown properties are ignored: the recognized metadata is registered as usual
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertTrue(clientRepresentation.isPublicClient());

        // delete the persisted client
        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorParseClientMetadataFailed() throws Exception {
        // register profiles
        updatePolicy(createDefaultConditionConfig(), createDefaultExecutorConfig());

        // 200 OK but the response body is not a valid JSON document
        cimd.setRawMetadata("{\"client_id\": \"" + CLIENT_ID + "\", \"redirect_uris\": \"not-json");

        // send an authorization request - fail
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_PARSE_FAILED);
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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
    public void testClientIdMetadataDocumentExecutorVerifyClientId3() throws Exception {
        // the domains below are trusted, but resolve to an address that must not be fetched from
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setTrustedDomains(List.of("localhost", "127.0.0.1", "[::1]", "10.0.0.1", "169.254.0.1",
                "192.168.0.1", "224.0.0.1"));
        updatePolicy(AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation(),
                ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig);

        // loopback addresses
        assertLoginAndError("https://localhost:8443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_ADDRESS);
        assertLoginAndError("https://127.0.0.1:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_ADDRESS);
        assertLoginAndError("https://[::1]:8443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_ADDRESS);

        // private address
        assertLoginAndError("https://10.0.0.1:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_ADDRESS);
        assertLoginAndError("https://192.168.0.1:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_ADDRESS);

        // link-local address
        assertLoginAndError("https://169.254.0.1:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_ADDRESS);

        // multicast address
        assertLoginAndError("https://224.0.0.1:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_ADDRESS);

        // the check can be switched off for development
        executorConfig.setAllowPrivateUseAddresses(true);
        updatePolicy(AnyClientConditionFactory.PROVIDER_ID, new ClientPolicyConditionConfigurationRepresentation(),
                ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig);
        assertLoginAndError("https://127.0.0.1:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_FETCH_FAILED);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorNotManagedClient() throws Exception {
        // register profiles
        updatePolicy(createDefaultConditionConfig(), createDefaultExecutorConfig());

        // a client whose clientId is a URL matching the policy, but not registered by CIMD
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(CLIENT_ID);
        clientRep.setRedirectUris(List.of(REDIRECT_URI));
        clientRep.setPublicClient(true);
        try (Response response = realm.admin().clients().create(clientRep)) {
            Assertions.assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        }
        String cid = findByClientIdByAdmin().getId();

        // a fetch of the metadata would fail the authorization request
        cimd.setStatus(Response.Status.NOT_FOUND);

        try {
            // the executor must leave the client untouched
            oauth.client(CLIENT_ID);
            oauth.redirectUri(REDIRECT_URI);
            oauth.loginForm().codeChallenge(null).open();
            oauth.fillLoginForm(user.getUsername(), user.getPassword());
            String code = oauth.parseLoginResponse().getCode();
            Assertions.assertNotNull(code);

            AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
            Assertions.assertEquals(200, tokenResponse.getStatusCode());

            // the client is still the one created by the administrator
            ClientRepresentation persisted = findByClientIdByAdmin();
            Assertions.assertTrue(persisted.isPublicClient());
            Assertions.assertNull(persisted.getAttributes().get(AbstractPersistentClientIdMetadataDocumentProvider.CIMD_CACHE_EXPIRY_TIME_IN_SEC));

            logout(tokenResponse.getIdToken());
        } finally {
            deleteClientByAdmin(cid);
        }
    }

    @Test
    public void testClientIdMetadataDocumentExecutorVerifyClientMetadata() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost",
                "mcpclient.example.org", "www.example.org",
                "[::1]", "client.example.com"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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

        // sector_identifier_uri : not allowed domain
        cimd.getRepresentation().setJwksUri(JWKS_URI);
        cimd.getRepresentation().setSectorIdentifierUri("https://sector.example.org/sector");
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // request_uris : not allowed domain
        cimd.getRepresentation().setSectorIdentifierUri(null);
        cimd.getRepresentation().setRequestUris(List.of("https://request.example.org/request"));
        assertLoginAndError(AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);
        cimd.getRepresentation().setRequestUris(null);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorTrustedSectorIdentifierUri() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = createDefaultConditionConfig();
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = createDefaultExecutorConfig();
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost", "127.0.0.1"));
        updatePolicy(conditionConfig, executorConfig);

        // set Client Metadata
        setCimdPublicClient();
        sectorIdentifierRedirectUrisProvider.setSectorIdentifierRedirectUris(List.of(REDIRECT_URI));
        cimd.getRepresentation().setSubjectType("pairwise");
        cimd.getRepresentation().setSectorIdentifierUri(sectorIdentifierRedirectUrisProvider.getUri());

        // send an authorization request
        String code = loginUserAndGetCode(true);
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());

        // check the persisted pairwise mapper
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        List<ProtocolMapperRepresentation> pairwiseMappers = clientRepresentation.getProtocolMappers().stream()
                .filter(m -> PairwiseSubMapperHelper.getSectorIdentifierUri(m) != null)
                .toList();
        Assertions.assertEquals(1, pairwiseMappers.size());
        Assertions.assertEquals(sectorIdentifierRedirectUrisProvider.getUri(), PairwiseSubMapperHelper.getSectorIdentifierUri(pairwiseMappers.get(0)));

        // delete the persisted client
        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorValidateClientMetadataConfidentialClient() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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

        // Client Metadata Validation:
        // either jwks or jwks_uri is required. (both jwks and jwks_uri are set)
        cimd.getRepresentation().setJwksUri(JWKS_URI);
        cimd.getRepresentation().setJwks(new JSONWebKeySet());
        assertLoginAndError(ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorAcceptPublicClientWithConfidentialOnlyGrantTypes() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setAcceptPublicClientWithConfidentialClientOnlyGrant(true);
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        updatePolicy(conditionConfig, executorConfig);

        // set Client Metadata for a public client with confidential-only grant types
        setCimdPublicClient();
        cimd.getRepresentation().setGrantTypes(List.of(
                OAuth2Constants.AUTHORIZATION_CODE,
                OAuth2Constants.REFRESH_TOKEN,
                OAuth2Constants.CLIENT_CREDENTIALS,
                OAuth2Constants.CIBA_GRANT_TYPE,
                OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE,
                OAuth2Constants.JWT_AUTHORIZATION_GRANT
        ));

        // send an authorization request
        String code = loginUserAndGetCode(true);

        // get an access token
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Assertions.assertTrue(clientRepresentation.isPublicClient());

        // Verify confidential-only grant types were removed
        Map<String, String> attrs = clientRepresentation.getAttributes();
        Assertions.assertFalse(clientRepresentation.isServiceAccountsEnabled());
        Assertions.assertNotEquals(Boolean.TRUE, clientRepresentation.getAuthorizationServicesEnabled());
        Assertions.assertNotEquals("true", attrs.get(CibaConfig.OIDC_CIBA_GRANT_ENABLED));
        Assertions.assertNotEquals("true", attrs.get(OIDCConfigAttributes.STANDARD_TOKEN_EXCHANGE_ENABLED));
        Assertions.assertNotEquals("true", attrs.get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED));

        // Verify public grant types were kept
        Assertions.assertTrue(clientRepresentation.isStandardFlowEnabled()); // authorization_code kept
        Assertions.assertEquals("true", attrs.get(OIDCConfigAttributes.USE_REFRESH_TOKEN)); // refresh_token kept

        // delete the persisted client
        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorRejectPublicClientWithConfidentialOnlyGrantTypesByDefault() throws Exception {
        // register profiles without setting acceptPublicClientWithConfidentialClientOnlyGrant (default: false)
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        // acceptPublicClientWithConfidentialClientOnlyGrant is not set, so default value (false) is applied
        updatePolicy(conditionConfig, executorConfig);

        // set Client Metadata for a public client with tokenEndpointAuthMethod = "none"
        // and grant types including jwt authorization grant
        cimd.getRepresentation().setTokenEndpointAuthMethod("none");
        cimd.getRepresentation().setJwksUri(null);
        cimd.getRepresentation().setGrantTypes(List.of(
                OAuth2Constants.AUTHORIZATION_CODE,
                OAuth2Constants.REFRESH_TOKEN,
                OAuth2Constants.JWT_AUTHORIZATION_GRANT
        ));

        // registration should fail because a public client cannot have
        // confidential-only grant types when acceptPublicClientWithConfidentialClientOnlyGrant is false
        assertLoginAndError("invalid request");
    }

    @Test
    public void testClientIdMetadataDocumentExecutorAcceptPublicClientWithConfidentialOnlyGrantTypesOnRegisterAndRefetch() throws Exception {
        // register profiles
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setAcceptPublicClientWithConfidentialClientOnlyGrant(true);
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        updatePolicy(conditionConfig, executorConfig);

        cimd.getRepresentation().setTokenEndpointAuthMethod("none");
        cimd.getRepresentation().setJwksUri(null);
        cimd.getRepresentation().setGrantTypes(List.of(
                OAuth2Constants.AUTHORIZATION_CODE,
                OAuth2Constants.REFRESH_TOKEN,
                OAuth2Constants.JWT_AUTHORIZATION_GRANT
        ));

        // send an authorization request (first registration)
        String code = loginUserAndGetCode(true);

        // get an access token
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());

        // check the persisted client settings after first registration
        ClientRepresentation clientRepresentation = findByClientIdByAdmin();
        Map<String, String> attrs = clientRepresentation.getAttributes();
        Assertions.assertTrue(clientRepresentation.isPublicClient());
        Assertions.assertTrue(clientRepresentation.isStandardFlowEnabled());
        Assertions.assertEquals("true", attrs.get(OIDCConfigAttributes.USE_REFRESH_TOKEN));
        // Verify JWT Authorization Grant was removed
        Assertions.assertNotEquals("true", attrs.get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED));

        // logout
        logout(tokenResponse.getIdToken());

        // move the time ahead so that the client metadata cache expires
        timeOffSet.set(CIMD_EXECUTOR_MIN_CACHE_TIME_SEC + 3);

        // do authorization code flow again, forcing a re-fetch of CIMD
        code = loginUserAndGetCode(false);
        tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).send();
        Assertions.assertEquals(200, tokenResponse.getStatusCode());

        // check the persisted client settings after re-fetch
        clientRepresentation = findByClientIdByAdmin();
        attrs = clientRepresentation.getAttributes();
        Assertions.assertTrue(clientRepresentation.isPublicClient());
        Assertions.assertTrue(clientRepresentation.isStandardFlowEnabled());
        Assertions.assertEquals("true", attrs.get(OIDCConfigAttributes.USE_REFRESH_TOKEN));
        // Verify JWT Authorization Grant is still removed after re-fetch
        Assertions.assertNotEquals("true", attrs.get(OIDCConfigAttributes.JWT_AUTHORIZATION_GRANT_ENABLED));

        // delete the persisted client
        logoutAndDelete(clientRepresentation.getId(), tokenResponse.getIdToken());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorValidateClientMetadataRestrictSameDomain() throws Exception {
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
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
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
        executorConfig.setAllowHttpScheme(true);
        executorConfig.setTrustedDomains(List.of("*.example.com", "[::1]", "localhost", "127.0.0.1"));
        executorConfig.setRequiredProperties(List.of("client_uri", "logo_uri"));
        updatePolicy(conditionConfig, executorConfig);

        oauth.redirectUri(REDIRECT_URI);
        cimd.getRepresentation().setLogoUri(null);
        assertLoginAndError(ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_REQUIRED_PROPERTIES);
    }

    @Test
    public void testWellKnownNoneInTokenEndpointAuthMethods() {
        OIDCConfigurationRepresentation oidcConfig = oauth.doWellKnownRequest();
        Assertions.assertTrue(oidcConfig.getClientIdMetadataDocumentSupported());
        Assertions.assertTrue(oidcConfig.getTokenEndpointAuthMethodsSupported().contains("none"));
    }

    private String loginUserAndGetCode(boolean isGrantRequred) {
        return loginUserAndGetCode(isGrantRequred, null);
    }

    private String loginUserAndGetCode(boolean isGrantRequred, String resource) {
        oauth.client(CLIENT_ID);
        oauth.redirectUri(REDIRECT_URI);
        oauth.loginForm().codeChallenge(null).resource(resource).open();
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

    private ClientIdUriSchemeCondition.Configuration createDefaultConditionConfig() {
        ClientIdUriSchemeCondition.Configuration conditionConfig = new ClientIdUriSchemeCondition.Configuration();
        conditionConfig.setClientIdUriSchemes(List.of("http", "https"));
        conditionConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        return conditionConfig;
    }

    private ClientIdMetadataDocumentExecutor.Configuration createDefaultExecutorConfig() {
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = newExecutorConfig();
        executorConfig.setTrustedDomains(List.of("*.example.com", "localhost"));
        executorConfig.setAllowHttpScheme(true);
        return executorConfig;
    }

    private void setCimdPublicClient() {
        cimd.getRepresentation().setTokenEndpointAuthMethod(null);
        cimd.getRepresentation().setJwksUri(null);
    }

    private void updateCimdAndSecureRedirectUrisPolicy(
            ClientIdUriSchemeCondition.Configuration conditionConfig,
            ClientIdMetadataDocumentExecutor.Configuration executorConfig,
            SecureRedirectUrisEnforcerExecutor.Configuration redirectUrisConfig) {
        realm.updateWithCleanup(r -> {
            r.resetClientProfiles()
                    .clientProfile(ClientProfileBuilder.create()
                    .name("cimd-profile")
                    .description("CIMD executor profile")
                    .executor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig)
                    .build())
                    .clientProfile(ClientProfileBuilder.create()
                    .name("redirect-uris-profile")
                    .description("SecureRedirectUris profile")
                    .executor(SecureRedirectUrisEnforcerExecutorFactory.PROVIDER_ID, redirectUrisConfig)
                    .build());
            r.resetClientPolicies()
                    .clientPolicy(ClientPolicyBuilder.create()
                    .name("cimd-policy")
                    .description("CIMD policy")
                    .condition(ClientIdUriSchemeConditionFactory.PROVIDER_ID, conditionConfig)
                    .profile("cimd-profile")
                    .build())
                    .clientPolicy(ClientPolicyBuilder.create()
                    .name("redirect-uris-policy")
                    .description("SecureRedirectUris policy for all clients")
                    .condition(AnyClientConditionFactory.PROVIDER_ID, null)
                    .profile("redirect-uris-profile")
                    .build());
            return r;
        });
    }

    // the metadata and the jwks are served from localhost, so the address check has to be switched off
    private static ClientIdMetadataDocumentExecutor.Configuration newExecutorConfig() {
        ClientIdMetadataDocumentExecutor.Configuration executorConfig = new ClientIdMetadataDocumentExecutor.Configuration();
        executorConfig.setAllowPrivateUseAddresses(true);
        return executorConfig;
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

    private boolean hasResourceIndicatorAudienceMapper(String clientId, String resource) {
        return realm.admin().clients().get(clientId).getProtocolMappers().getMappers().stream()
                .filter(m -> CimdResourceIndicatorAudienceMapper.PROVIDER_ID.equals(m.getProtocolMapper()))
                .anyMatch(m -> resource.equals(m.getConfig().get(AudienceProtocolMapper.INCLUDED_CUSTOM_AUDIENCE)));
    }

    // whether an access token issued to the client now includes the resource in its audience, according to the example
    // access token that the admin API generates through the protocol mappers of the client.
    private boolean isResourceIncludedInAudience(String clientId, String resource) {
        String[] audience = realm.admin().clients().get(clientId).clientScopesEvaluate()
                .generateAccessToken(OAuth2Constants.SCOPE_OPENID, user.getId(), null).getAudience();
        return audience != null && Arrays.asList(audience).contains(resource);
    }

    private List<String> getAudience(String accessToken) {
        String[] audience = oauth.verifyToken(accessToken).getAudience();
        return audience == null ? List.of() : Arrays.asList(audience);
    }

    // an audience mapper for the resource as a CIMD executor adds it to a client, of the protocol mapper type given.
    private static ProtocolMapperRepresentation createResourceIndicatorAudienceMapper(String protocolMapper, String resource) {
        ProtocolMapperRepresentation mapper = ModelToRepresentation.toRepresentation(AudienceProtocolMapper.createClaimMapper(
                CimdResourceIndicatorAudienceMapper.NAME_PREFIX + KeycloakModelUtils.generateId(), null, resource, true, false, true));
        mapper.setProtocolMapper(protocolMapper);
        return mapper;
    }

    // registers the client by an authorization request with the resource, and gets the tokens bound to the resource.
    private AccessTokenResponse loginAndGetTokens(String resource) {
        String code = loginUserAndGetCode(true, resource);
        AccessTokenResponse tokenResponse = oauth.client(CLIENT_ID).accessTokenRequest(code).resource(resource).signedJwt(createSignedRequestToken()).send();
        Assertions.assertTrue(tokenResponse.isSuccess());
        return tokenResponse;
    }

    private ClientPolicyBuilder createCimdPolicy(String name) {
        return ClientPolicyBuilder.create().name(name).condition(ClientIdUriSchemeConditionFactory.PROVIDER_ID, createDefaultConditionConfig());
    }

    // the client profile "executor" and, if its configuration is given, the client profile "other-executor", each with a
    // CIMD executor, and the client policies using them.
    private void updateCimdPolicies(ClientIdMetadataDocumentExecutor.Configuration executorConfig,
            ClientIdMetadataDocumentExecutor.Configuration otherExecutorConfig, ClientPolicyRepresentation... policies) {
        realm.updateWithCleanup(r -> {
            r.resetClientProfiles()
                    .clientProfile(ClientProfileBuilder.create().name("executor").executor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, executorConfig).build());
            if (otherExecutorConfig != null) {
                r.clientProfile(ClientProfileBuilder.create().name("other-executor").executor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID, otherExecutorConfig).build());
            }
            r.resetClientPolicies();
            for (ClientPolicyRepresentation policy : policies) {
                r.clientPolicy(policy);
            }
            return r;
        });
    }

    private static String getFragmentParameter(String url, String name) {
        int i = url.indexOf('#');
        if (i < 0) {
            return null;
        }
        for (String pair : url.substring(i + 1).split("&")) {
            int j = pair.indexOf('=');
            if (j > 0 && pair.substring(0, j).equals(name)) {
                return java.net.URLDecoder.decode(pair.substring(j + 1), java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private void setCimdImplicitFlow() {
        cimd.getRepresentation().setGrantTypes(List.of(
                OAuth2Constants.AUTHORIZATION_CODE,
                OAuth2Constants.IMPLICIT,
                OAuth2Constants.REFRESH_TOKEN
        ));
        cimd.getRepresentation().setResponseTypes(List.of(
                OIDCResponseType.CODE,
                OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN
        ));
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
            return config.features(Profile.Feature.CIMD, Profile.Feature.RESOURCE_INDICATORS)
                    .dependency("org.keycloak.tests", "keycloak-tests-custom-providers");
        }
    }

    public static class StoredUriClientConfig implements ClientConfig {

        @Override
        public ClientBuilder configure(ClientBuilder client) {
            return client.clientId("https://client.example.com/metadata")
                    .protocol(OIDCLoginProtocol.LOGIN_PROTOCOL)
                    .publicClient(true)
                    .redirectUris("http://client.example.com/callback", "https://client.example.com/callback");
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
