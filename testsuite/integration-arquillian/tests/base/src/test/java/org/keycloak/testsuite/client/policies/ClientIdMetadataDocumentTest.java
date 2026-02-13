package org.keycloak.testsuite.client.policies;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.crypto.Algorithm;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeCondition;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.condition.ClientIdUriSchemeConditionFactory;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutor;
import org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.ClientIdMetadataDocumentExecutorFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.TokenRevocationResponse;
import org.keycloak.util.JsonSerialization;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.arquillian.graphene.page.Page;
import org.junit.Test;

import static org.keycloak.protocol.oauth2.cimd.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_FETCH_FAILED;
import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createExecutorConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/**
 * This test class is for testing an executor of client policies.
 * 
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
@EnableFeature(value = Profile.Feature.CIMD)
public class ClientIdMetadataDocumentTest extends AbstractClientPoliciesTest {

    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected AppPage appPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;

    private static final int CIMD_EXECUTOR_MIN_CACHE_TIME_SEC = 300;

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void testClientIdUriSchemeCondition() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setTrustedDomains(List.of("localhost", "example.com","mcp.example.com"))))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies : no trusted domain
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->
                                        it.setClientIdUriSchemes(List.of("spiffe"))))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId;

        // allowed scheme: spiffe
        // actual scheme: https
        // -> CIMD executor is not executed, so we expect the error showing that client is not found.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        oauth.client(clientId);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
        assertEquals("Client not found.", errorPage.getError());

        // update policies: trusted domain vacant
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->
                                        it.setClientIdUriSchemes(List.of("http", "https"))))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // trusted domains: vacant
        // host: localhost
        // -> CIMD executor is not executed, so we expect the error showing that client is not found.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        oauth.client(clientId);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
        assertEquals("Client not found.", errorPage.getError());

        // update policies: trusted domain filled
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                    it.setClientIdUriSchemes(List.of("http", "https"));
                                    it.setTrustedDomains(List.of("example.com","mcp.example.com"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // trusted domains: example.com, mcp.example.com
        // host: localhost
        // -> CIMD executor is not executed, so we expect the error showing that client is not found.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        oauth.client(clientId);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
        assertEquals("Client not found.", errorPage.getError());
    }

    @Test
    public void testClientIdMetadataDocumentExecutorForConfidentialClient() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setTrustedDomains(List.of("*.example.com","localhost"));
                                    it.setRestrictSameDomain(true);
                                    it.setRequiredProperties(List.of("scope", "logo_uri", "client_uri", "tos_uri", "policy_uri", "jwks_uri"));}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("*.example.com","localhost"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Get keys of client. Will be used for client authentication
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.generateKeys(Algorithm.PS256);
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, Algorithm.PS256);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // set Client Metadata
        String clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        OIDCClientRepresentation clientMetadata = setupOIDCClientRepresentation(clientId, i-> {
            i.setLogoUri("https://localhost:8543/");
            i.setClientUri("https://localhost:8543/client");
            i.setTosUri("https://localhost:8543/tos");
            i.setPolicyUri("https://localhost:8543/policy");
            i.setScope("address phone");
        });
        // CIMD executor's min cache time is 300 as default, so if s-maxage = 20 then it is replaced with 300.
        String cacheControlHeaderValue = "must-revalidate, max-age=3600, public, s-maxage=20";
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, cacheControlHeaderValue, null);

        // send an authorization request
        String code = loginUserAndGetCode(clientId, true);

        // get an access token
        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin(clientId);
        assertTrue(clientRepresentation.isConsentRequired()); // default
        assertFalse(clientRepresentation.isFullScopeAllowed()); // default
        assertFalse(clientRepresentation.isPublicClient());
        assertEquals("client-jwt", clientRepresentation.getClientAuthenticatorType());

        // logout
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        // change the client metadata
        clientMetadata = setupOIDCClientRepresentation(clientId, i-> {
            i.setLogoUri("https://localhost:8543/logo");
            i.setClientUri("https://localhost:8543/client");
            i.setTosUri("https://localhost:8543/tos");
            i.setPolicyUri("https://localhost:8543/policy");
            i.setScope("profile");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, cacheControlHeaderValue, null);

        // do authorization code flow again, but registered client metadata is still effective
        code = loginUserAndGetCode(clientId, false);
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak does not fetch the client metadata.
        // the registered client metadata remains the same
        clientRepresentation = findByClientIdByAdmin(clientId);
        Map<String, String> m = clientRepresentation.getAttributes();
        List<String> optionalScopeList = clientRepresentation.getOptionalClientScopes();
        assertEquals("https://localhost:8543/", m.get("logoUri"));
        assertEquals(2, optionalScopeList.size());
        assertTrue(optionalScopeList.contains("phone"));
        assertTrue(optionalScopeList.contains("address"));
        assertFalse(clientRepresentation.isPublicClient());
        assertEquals("client-jwt", clientRepresentation.getClientAuthenticatorType());

        // logout
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        // move the time ahead so that the client metadata becomes ineffective.
        setTimeOffset(CIMD_EXECUTOR_MIN_CACHE_TIME_SEC + 3);

        // do authorization code flow again, and registered client metadata is not effective
        code = loginUserAndGetCode(clientId, false);
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak re-fetch the client metadata.
        // the registered client metadata changed
        clientRepresentation = findByClientIdByAdmin(clientId);
        m = clientRepresentation.getAttributes();
        optionalScopeList = clientRepresentation.getOptionalClientScopes();
        assertEquals("https://localhost:8543/logo", m.get("logoUri"));
        assertEquals(1, optionalScopeList.size());
        assertTrue(optionalScopeList.contains("profile"));

        // delete the persisted client
        String cid = clientRepresentation.getId();
        deleteClientByAdmin(cid);
        assertThrows(jakarta.ws.rs.NotFoundException.class, ()->getClientByAdmin(cid));

        // need to reset time offset for the offset does not affect other test's execution.
        resetTimeOffset();
    }

    @Test
    public void testClientIdMetadataDocumentExecutorForPublicClient() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setTrustedDomains(List.of("*.example.com","localhost"));
                                    it.setRestrictSameDomain(true);
                                    it.setRequiredProperties(List.of("scope", "logo_uri", "client_uri", "tos_uri", "policy_uri"));}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("*.example.com","localhost"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // set Client Metadata
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        String clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        OIDCClientRepresentation clientMetadata = setupOIDCClientRepresentation(clientId, i-> {
            i.setLogoUri("https://localhost:8543/");
            i.setClientUri("https://localhost:8543/client");
            i.setTosUri("https://localhost:8543/tos");
            i.setPolicyUri("https://localhost:8543/policy");
            i.setScope("address phone");
            i.setTokenEndpointAuthMethod(null); // public client
            i.setJwksUri(null);
        });
        String cacheControlHeaderValue = "must-revalidate, max-age=3600, public, s-maxage=20";
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, cacheControlHeaderValue, null);

        // send an authorization request
        String code = loginUserAndGetCode(clientId, true);

        // get an access token
        AccessTokenResponse tokenResponse = oauth.
                client(clientId).accessTokenRequest(code).send();
        assertEquals(200, tokenResponse.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(clientId, accessToken.getIssuedFor());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin(clientId);
        assertTrue(clientRepresentation.isConsentRequired()); // default
        assertFalse(clientRepresentation.isFullScopeAllowed()); // default
        assertTrue(clientRepresentation.isPublicClient());
        assertEquals("none", clientRepresentation.getClientAuthenticatorType());

        // introspect
        IntrospectionResponse introspectionResponse = oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET).doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        assertEquals(200, introspectionResponse.getStatusCode());
        assertEquals(clientId, introspectionResponse.asTokenMetadata().getClientId());

        // refresh
        tokenResponse = oauth.client(clientId).doRefreshTokenRequest(tokenResponse.getRefreshToken());
        assertEquals(200, tokenResponse.getStatusCode());
        accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(clientId, accessToken.getIssuedFor());

        // revoke
        TokenRevocationResponse revokeResponse = oauth.client(clientId).doTokenRevoke(tokenResponse.getAccessToken());
        assertEquals(200, revokeResponse.getStatusCode());
        introspectionResponse = oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET).doIntrospectionAccessTokenRequest(tokenResponse.getAccessToken());
        assertEquals(200, introspectionResponse.getStatusCode());
        assertFalse(introspectionResponse.asTokenMetadata().isActive());

        // get another token
        oauth.scope("phone");
        code = ssoLoginUserAndGetCode(clientId, OAuthGrantPage.PHONE_CONSENT_TEXT);
        tokenResponse = oauth.client(clientId).accessTokenRequest(code).send();
        assertEquals(200, tokenResponse.getStatusCode());
        accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(clientId, accessToken.getIssuedFor());
        assertTrue(Arrays.asList(accessToken.getScope().split(" ")).contains("phone"));

        // delete the persisted client
        clientRepresentation = findByClientIdByAdmin(clientId);
        String cid = clientRepresentation.getId();
        deleteClientByAdmin(cid);
        assertThrows(jakarta.ws.rs.NotFoundException.class, ()->getClientByAdmin(cid));
    }

    @Test
    public void testClientIdMetadataDocumentExecutorNotModifiedClientMetadata() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setTrustedDomains(List.of("*.example.com","localhost"));
                                    it.setRestrictSameDomain(true);
                                    it.setRequiredProperties(List.of("logo_uri", "scope"));}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("*.example.com","localhost"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Get keys of client. Will be used for client authentication
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.generateKeys(Algorithm.PS256);
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, Algorithm.PS256);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // set Client Metadata
        // clientId = https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/get-client-id-metadata
        // clientUri = https://localhost:8543/auth/realms/master/app/auth
        // jwksUri = https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/get-jwks
        String clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        OIDCClientRepresentation clientMetadata = setupOIDCClientRepresentation(clientId, i-> {
            i.setLogoUri("https://localhost:8443/logo");
            i.setScope("address phone");
        });
        String cacheControlHeaderValue = "must-revalidate, max-age=3600, public, s-maxage=20";
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, cacheControlHeaderValue, null);

        // send an authorization request
        String code = loginUserAndGetCode(clientId, true);

        // get an access token
        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin(clientId);
        assertTrue(clientRepresentation.isConsentRequired()); // default
        assertFalse(clientRepresentation.isFullScopeAllowed()); // default

        // logout
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        // change the client metadata
        // however, the client returns 304 Not Modified
        clientMetadata = setupOIDCClientRepresentation(clientId, i-> {
            i.setLogoUri("https://localhost:443/logo/v3");
            i.setScope("profile");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, cacheControlHeaderValue, "NotModified");

        // do authorization code flow again, but registered client metadata is still effective
        code = loginUserAndGetCode(clientId, false);
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak does not fetch the client metadata.
        // the registered client metadata remains the same
        clientRepresentation = findByClientIdByAdmin(clientId);
        Map<String, String> m = clientRepresentation.getAttributes();
        List<String> optionalScopeList = clientRepresentation.getOptionalClientScopes();
        assertEquals("https://localhost:8443/logo", m.get("logoUri"));
        assertEquals(2, optionalScopeList.size());
        assertTrue(optionalScopeList.contains("phone"));
        assertTrue(optionalScopeList.contains("address"));

        // logout
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        // move the time ahead so that the client metadata becomes ineffective.
        setTimeOffset(CIMD_EXECUTOR_MIN_CACHE_TIME_SEC + 3);

        // do authorization code flow again, and registered client metadata is not effective
        //Thread.sleep(1000 * 20);
        code = loginUserAndGetCode(clientId, false);
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak re-fetch the client metadata.
        // however, the client returns 304 Not Modified.
        // therefore, the client metadata remains the same.
        clientRepresentation = findByClientIdByAdmin(clientId);
        m = clientRepresentation.getAttributes();
        optionalScopeList = clientRepresentation.getOptionalClientScopes();
        assertEquals("https://localhost:8443/logo", m.get("logoUri"));
        assertEquals(2, optionalScopeList.size());
        assertTrue(optionalScopeList.contains("phone"));
        assertTrue(optionalScopeList.contains("address"));

        // delete the persisted client
        String cid = clientRepresentation.getId();
        deleteClientByAdmin(cid);
        assertThrows(jakarta.ws.rs.NotFoundException.class, ()->getClientByAdmin(cid));

        // need to reset time offset for the offset does not affect other test's execution.
        resetTimeOffset();
    }

    @Test
    public void testClientIdMetadataDocumentExecutorDefaultSetting() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it-> {}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("*.example.com","localhost"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        String clientId;
        OIDCClientRepresentation clientMetadata;

        // vacant trusted domains
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i-> {});
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorFetchClientMetadataFailed() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setTrustedDomains(List.of("*.example.com","localhost"))))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("*.example.com","localhost"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        String clientId;
        OIDCClientRepresentation clientMetadata;

        // 404 Not Found returns
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i-> {});
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, "NotFound");
        assertLoginAndError(clientId, ERR_METADATA_FETCH_FAILED);

        // 200 OK but malformed client metadata
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i-> {});
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, "MalformedResponse");
        assertLoginAndError(clientId, ERR_METADATA_FETCH_FAILED);
    }

    private void testClientIdMetadataDocumentExecutorCacheControl(String cacheControlHeaderValue, int expectedExpiry, TestOIDCEndpointsApplicationResource oidcClientEndpointsResource, PrivateKey privateKey, PublicKey publicKey) throws Exception {
        // set Client Metadata
        String clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        OIDCClientRepresentation clientMetadata = setupOIDCClientRepresentation(clientId, i-> {
            i.setLogoUri("https://www.example.com");
            i.setScope("address phone");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, cacheControlHeaderValue, null);

        // send an authorization request
        String code = loginUserAndGetCode(clientId, true);

        // get an access token
        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin(clientId);
        assertTrue(clientRepresentation.isConsentRequired()); // default
        assertFalse(clientRepresentation.isFullScopeAllowed()); // default

        // logout
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        // change the client metadata
        clientMetadata = setupOIDCClientRepresentation(clientId, i -> {
            i.setLogoUri("https://www.example.com/logo");
            i.setScope("profile");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, cacheControlHeaderValue, null);
        Thread.sleep(1000); // wait to complete the update

        Map<String, String> m;
        List<String> optionalScopeList;
        if (expectedExpiry > 0) {
            // do authorization code flow again, but registered client metadata is still effective
            code = loginUserAndGetCode(clientId, false);
            signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
            tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
            oauth.verifyToken(tokenResponse.getAccessToken());

            // therefore, keycloak does not fetch the client metadata.
            // the registered client metadata remains the same
            clientRepresentation = findByClientIdByAdmin(clientId);
            m = clientRepresentation.getAttributes();
            optionalScopeList = clientRepresentation.getOptionalClientScopes();
            assertEquals("https://www.example.com", m.get("logoUri"));
            assertEquals(2, optionalScopeList.size());
            assertTrue(optionalScopeList.contains("phone"));
            assertTrue(optionalScopeList.contains("address"));

            // logout
            oauth.openLogoutForm();
            logoutConfirmPage.assertCurrent();
            logoutConfirmPage.confirmLogout();

            // force both the client and the server time to go forward to shorten the completion time of the test
            setTimeOffset(expectedExpiry + 3);
        }

        // do authorization code flow again, and registered client metadata is not effective
        code = loginUserAndGetCode(clientId, false);
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());

        // therefore, keycloak re-fetch the client metadata.
        // the registered client metadata changed
        clientRepresentation = findByClientIdByAdmin(clientId);
        m = clientRepresentation.getAttributes();
        optionalScopeList = clientRepresentation.getOptionalClientScopes();
        assertEquals("https://www.example.com/logo", m.get("logoUri"));
        assertEquals(1, optionalScopeList.size());
        assertTrue(optionalScopeList.contains("profile"));

        // logout
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();

        // delete the persisted client
        String cid = clientRepresentation.getId();
        deleteClientByAdmin(cid);
        assertThrows(jakarta.ws.rs.NotFoundException.class, ()->getClientByAdmin(cid));

        // reset time offset
        resetTimeOffset();
    }

    @Test
    public void testClientIdMetadataDocumentExecutorCacheControl() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setTrustedDomains(List.of("*.example.com","localhost"))))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("*.example.com","localhost"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Get keys of client. Will be used for client authentication
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.generateKeys(Algorithm.PS256);
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, Algorithm.PS256);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        // CIMD Executor's min cache time default value: 300 sec
        // CIMD Executor's max cache time default value: 259200 sec

        // no Cache-Control header : cached in min cache time
        testClientIdMetadataDocumentExecutorCacheControl(null, CIMD_EXECUTOR_MIN_CACHE_TIME_SEC, oidcClientEndpointsResource, privateKey, publicKey);

        // empty Cache-Control header : cached in min cache time
        testClientIdMetadataDocumentExecutorCacheControl("", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC, oidcClientEndpointsResource, privateKey, publicKey);

        // max-age : max-age considered
        testClientIdMetadataDocumentExecutorCacheControl("max-age=320,    private", 320, oidcClientEndpointsResource, privateKey, publicKey);

        // s-maxage : s-maxage considered
        testClientIdMetadataDocumentExecutorCacheControl("private,S-MAXAGE=315,  no-transform", 315, oidcClientEndpointsResource, privateKey, publicKey);

        // max-age and s-maxage : s-maxage considered
        testClientIdMetadataDocumentExecutorCacheControl(" Max-Age=3600,public,S-MaxAge=312", 312, oidcClientEndpointsResource, privateKey, publicKey);

        // max-age and no-cache : cached in min cache time
        testClientIdMetadataDocumentExecutorCacheControl("max-age=320, NO-CACHE  ", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC, oidcClientEndpointsResource, privateKey, publicKey);

        // s-maxage and no-store : cached in min cache time
        testClientIdMetadataDocumentExecutorCacheControl("S-MAXAGE=320,no-store", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC, oidcClientEndpointsResource, privateKey, publicKey);

        // unknown values only : cached in min cache time
        // min-age=20, CACHE
        testClientIdMetadataDocumentExecutorCacheControl("min-age=20,CACHE ", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC, oidcClientEndpointsResource, privateKey, publicKey);

        // under the min cache time : 5 -> 300
        testClientIdMetadataDocumentExecutorCacheControl("max-age=5", CIMD_EXECUTOR_MIN_CACHE_TIME_SEC, oidcClientEndpointsResource, privateKey, publicKey);

        // over the max cache time : 365000 -> 259200
        testClientIdMetadataDocumentExecutorCacheControl("s-maxage=365000", 259200, oidcClientEndpointsResource, privateKey, publicKey);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorClientMetadataUpperLimit() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setTrustedDomains(List.of("*.example.com","localhost"))))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("*.example.com","localhost"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // set Client Metadata
        // CIMD Executor's client metadata upper limit byte length default value: 5000
        String clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        OIDCClientRepresentation clientMetadata = setupOIDCClientRepresentation(clientId, i->
                i.setClientName("0123456789".repeat(465) + "0123456"));
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(clientMetadata);
        assertEquals(5001, contentBytes.length);
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);

        // send an authorization request - fail
        assertLoginAndError(clientId, ERR_METADATA_FETCH_FAILED);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorVerifyAuthorizationRequest() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setTrustedDomains(List.of("localhost"))))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("localhost"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        String clientId;
        OIDCClientRepresentation clientMetadata;

        // Authorization Request Verification:
        // The authorization server MAY choose to have its own heuristics and policies
        // around the trust of domain names used as client IDs.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i-> {});
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        oauth.redirectUri(null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_INVALID_PARAMETER);

    }

    @Test
    public void testClientIdMetadataDocumentExecutorVerifyClientId() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setTrustedDomains(List.of("*.example.com", "localhost", "127.0.0.1",
                                                "10.0.0.0", "example.co.jp", "10.255.255.255", "172.16.0.0", "[::1]", "[0:0:0:0:0:0:0:1]",
                                                "172.31.255.255", "192.168.0.0", "192.168.255.255", "[fe12:3456:789a:1::]"))))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientPolicyConditionConfigurationRepresentation(), it->{}))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

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

        // update profiles
        json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setTrustedDomains(List.of("*.example.com"))))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

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
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setTrustedDomains(List.of("*.example.com", "localhost",
                                        "mcpclient.example.org", "www.example.org",
                                        "[::1]", "client.example.com"))))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("*.example.com", "localhost",
                                            "mcpclient.example.org", "www.example.org",
                                            "[::1]", "client.example.com"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        String clientIdPath;
        String clientId;
        OIDCClientRepresentation clientMetadata;

        // Client Metadata Verification:
        // The client metadata document MUST contain a client_id property.
        // clientId = not set
        // clientUri = https://localhost:8543/auth/realms/master/app/auth
        // jwksUri = https://localhost:8543/auth/realms/master/app/oidc-client-endpoints/get-jwks
        clientIdPath = generateSuffixedName("CIMD");
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(clientIdPath);
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setClientId(null));
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(clientMetadata);
        String encodedClientMetadata = Base64Url.encode(contentBytes);
        oidcClientEndpointsResource.setClientIdMetadata(clientIdPath, encodedClientMetadata, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_NOCLIENTID);

        // Client Metadata Verification:
        // The client_id property's value MUST match the URL of the document
        // using simple string comparison as defined in [RFC3986] Section 6.2.1.
        clientIdPath = generateSuffixedName("CIMD");
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(clientIdPath);
        final String differentClientId = clientId + "/different_path/" + clientIdPath;
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setClientId(differentClientId));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_CLIENTID_UNMATCH);

        // Client Metadata Verification:
        // The token_endpoint_auth_method property MUST NOT include
        // client_secret_post, client_secret_basic, client_secret_jwt,
        // or any other method based around a shared symmetric secret.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_JWT));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_NOTALLOWED_CLIENTAUTH);

        // Client Metadata Verification:
        // The client_secret and client_secret_expires_at properties MUST NOT be used.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setClientSecret("ClientSecretNotAllowed"));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_CLIENTSECRET);

        // Client Metadata Verification:
        // An authorization server MUST validate redirect URIs presented in an authorization request
        // against those in the metadata document.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setRedirectUris(Collections.singletonList("https://mcp.example.com/mcp/123abc")));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_REDIRECTURI);

        // SSRF attack countermeasure
        // It checks if an address resolved from a property whose value is URI is loopback address.
        //  -> difficult to check because allowing loopback address is needed to fetch client metadata from loop-back addressed test-provider server
        //  -> also jwks_uri is hosted by also the loop-back addressed test-provider server.
        // It checks if an address resolved from a property whose value is URI is private address.
        // RFC 7591: logo_uri, client_uri, tos_uri, policy_uri, jwks_uri.

        // logo_uri : private address
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i-> i.setLogoUri("https://10.255.255.255:443/mcp"));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // client_uri : not allowed domain
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->{
            i.setLogoUri("https://localhost:8443/logo");
            i.setClientUri("https://mcpclient.example.or.jp/client");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // tos_uri : private address
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->{
            i.setLogoUri("https://localhost:8443/logo");
            i.setClientUri("https://www.example.org");
            i.setTosUri("https://172.31.255.254:443/mcp");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // policy_uri : not allowed domain
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->{
            i.setLogoUri("https://localhost:8443/logo");
            i.setClientUri("https://www.example.org");
            i.setTosUri("https://[::1]:8443/mcp");
            i.setPolicyUri("https://client.example.co.de");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);

        // jwks_uri : private address
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->{
            i.setLogoUri("https://localhost:8443/logo");
            i.setClientUri("https://www.example.org");
            i.setTosUri("https://[::1]:8443/mcp");
            i.setPolicyUri("https://localhost:8443/policy");
            i.setJwksUri("https://10.255.255.1:443/mcp");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorValidateClientMetadata() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                        it.setOnlyAllowConfidentialClient(true);
                                        it.setTrustedDomains(List.of("*.example.com", "localhost"));
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->{
                                        it.setClientIdUriSchemes(List.of("http", "https"));
                                        it.setTrustedDomains(List.of("*.example.com", "localhost"));
                                }))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        String clientId;
        OIDCClientRepresentation clientMetadata;

        // Client Metadata Validation:
        // Only a confidential client is allowed.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setTokenEndpointAuthMethod(null));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_CONFIDENTIAL_CLIENT);

        // Client Metadata Validation:
        // ether jwks or jwks_uri is required.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setJwksUri(null));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_CONFIDENTIAL_CLIENT_JWKS);

        // Client Metadata Validation:
        // An authorization server MAY impose restrictions or relationships
        // between the redirect_uris and the client_id or client_uri properties
        //
        // same domain
        json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setOnlyAllowConfidentialClient(false);
                                    it.setTrustedDomains(List.of("*.example.com", "localhost", "www.example.co.jp"));
                                    it.setRestrictSameDomain(true);
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        String originalRedirectUri = oauth.getRedirectUri();
        oauth.redirectUri("https://www.example.com/callback");
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setRedirectUris(List.of("https://localhost:8443/callback", "https://www.example.com/callback")));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, ClientIdMetadataDocumentExecutor.ERR_METADATA_URIS_SAMEDOMAIN);
        oauth.redirectUri(originalRedirectUri);

        // required properties
        json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setRequiredProperties(List.of("client_uri", "logo_uri"));
                                    it.setTrustedDomains(List.of("*.example.com", "[::1]", "localhost", "127.0.0.1"));
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setClientUri("https://www.example.com"));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_REQUIRED_PROPERTIES);

        // All URIs under the same domain of permitted domains
        json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setRestrictSameDomain(true);
                                    it.setTrustedDomains(List.of("*.example.org", "localhost", "[::1]", "127.0.0.1"));
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->{
            i.setLogoUri("https://localhost:8443/logo");
            i.setClientUri("https://www.example.org");
            i.setTosUri("https://[::1]:8443/mcp");
            i.setPolicyUri("https://localhost/mcp");
            i.setJwksUri("https://127.0.0.1:443/mcp");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null, null);
        assertLoginAndError(clientId, ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_ALL_URIS_SAMEDOMAIN);
    }

    private String loginUserAndGetCode(String clientId, boolean isGrantRequred) {
        oauth.client(clientId);
        oauth.loginForm().codeChallenge(null).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        if (isGrantRequred) {
            grantPage.assertCurrent();
            grantPage.assertGrants("The client's hostname is localhost");
            grantPage.accept();
        }

        String code = oauth.parseLoginResponse().getCode();
        org.keycloak.testsuite.Assert.assertNotNull(code);
        return code;
    }

    private String ssoLoginUserAndGetCode(String clientId, String expectedGrant) {
        oauth.client(clientId);
        oauth.loginForm().codeChallenge(null).open();

        if (expectedGrant != null) {
            grantPage.assertCurrent();
            grantPage.assertGrants(expectedGrant);
            grantPage.accept();
        }

        String code = oauth.parseLoginResponse().getCode();
        org.keycloak.testsuite.Assert.assertNotNull(code);
        return code;
    }

    private AccessTokenResponse doAccessTokenRequestWithClientSignedJWT(String code, String signedJwt, Supplier<CloseableHttpClient> httpClientSupplier) {
        try {
            List<NameValuePair> parameters = new LinkedList<>();
            parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));

            parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
            parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

            CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters, httpClientSupplier);
            return new AccessTokenResponse(response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters, Supplier<CloseableHttpClient> httpClientSupplier) throws Exception {
        try (CloseableHttpClient client = httpClientSupplier.get()) {
            HttpPost post = new HttpPost(requestUrl);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);
            return client.execute(post);
        }
    }

    private void assertLoginAndError(String clientId, String errorMessage) {
        oauth.client(clientId);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
        assertEquals(errorMessage, errorPage.getError());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                        errorMessage).client((String) null)
                .user((String) null).assertEvent();
    }

    private OIDCClientRepresentation setupOIDCClientRepresentation(
            String clientId, Consumer<OIDCClientRepresentation> apply) {
        OIDCClientRepresentation clientMetadata = new OIDCClientRepresentation();
        clientMetadata.setClientId(clientId);
        clientMetadata.setRedirectUris(Collections.singletonList(oauth.getRedirectUri()));
        clientMetadata.setTokenEndpointAuthMethod(OIDCLoginProtocol.PRIVATE_KEY_JWT);
        clientMetadata.setJwksUri(TestApplicationResourceUrls.clientJwksUri());
        if (apply != null) {
            apply.accept(clientMetadata);
        }
        return clientMetadata;
    }
}
