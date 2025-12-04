package org.keycloak.testsuite.client.policies;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
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
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientIdUriSchemeCondition;
import org.keycloak.services.clientpolicy.condition.ClientIdUriSchemeConditionFactory;
import org.keycloak.services.clientpolicy.executor.AbstractClientIdMetadataDocumentExecutor;
import org.keycloak.services.clientpolicy.executor.ClientIdMetadataDocumentExecutor;
import org.keycloak.services.clientpolicy.executor.ClientIdMetadataDocumentExecutorFactory;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.pages.AppPage;
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
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
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setAllowLoopbackAddress(true);
                                    it.setAllowPermittedDomains(List.of("example.com","mcp.example.com"));
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
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

        // CIMD executor is not executed, so we expect the error showing that client is not found.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        oauth.client(clientId);
        oauth.openLoginForm();
        assertTrue(errorPage.isCurrent());
        assertEquals("Client not found.", errorPage.getError());
    }

    @Test
    public void testClientIdMetadataDocumentExecutor() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setAllowLoopbackAddress(true);
                                    it.setAllowPermittedDomains(List.of("*.example.com","localhost"));
                                    it.setRestrictSameDomain(true);
                                    it.setRequiredProperties(List.of("logo_uri", "scope"));}))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->
                                        it.setClientIdUriSchemes(List.of("http", "https"))))
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
            i.setLogoUri("https://mcpclient.example.com");
            i.setScope("address phone");
        });
        String cacheControlHeaderValue = "must-revalidate, max-age=3600, public, s-maxage=20";
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, cacheControlHeaderValue);

        // send an authorization request
        String code = loginUserAndGetCode(clientId, null);

        // get an access token
        String signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin(clientId);
        assertTrue(clientRepresentation.isConsentRequired()); // default
        assertFalse(clientRepresentation.isFullScopeAllowed()); // default

        // registered client metadata is still effective
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();
        clientMetadata = setupOIDCClientRepresentation(clientId, i-> {
            i.setLogoUri("https://mcpclient.example.com/logo");
            i.setScope("profile");
        });
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, cacheControlHeaderValue);
        code = loginUserAndGetCode(clientId, null);
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());
        clientRepresentation = findByClientIdByAdmin(clientId);
        Map<String, String> m = clientRepresentation.getAttributes();
        List<String> optionalScopeList = clientRepresentation.getOptionalClientScopes();
        // not updated
        assertEquals("https://mcpclient.example.com", m.get("logoUri"));
        assertEquals(2, optionalScopeList.size());
        assertTrue(optionalScopeList.contains("phone"));
        assertTrue(optionalScopeList.contains("address"));

        // registered client metadata is not effective
        Thread.sleep(1000 * 20);
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();
        code = loginUserAndGetCode(clientId, null);
        signedJwt = createSignedRequestToken(clientId, privateKey, publicKey, Algorithm.PS256);
        tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, DefaultHttpClient::new);
        oauth.verifyToken(tokenResponse.getAccessToken());
        clientRepresentation = findByClientIdByAdmin(clientId);
        m = clientRepresentation.getAttributes();
        optionalScopeList = clientRepresentation.getOptionalClientScopes();
        // updated
        assertEquals("https://mcpclient.example.com/logo", m.get("logoUri"));
        assertEquals(1, optionalScopeList.size());
        assertTrue(optionalScopeList.contains("profile"));

        // delete the persisted client
        String cid = clientRepresentation.getId();
        deleteClientByAdmin(cid);
        assertThrows(jakarta.ws.rs.NotFoundException.class, ()->getClientByAdmin(cid));
    }


    @Test
    public void testClientIdMetadataDocumentExecutorVerifyAuthorizationRequest() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setAllowLoopbackAddress(true)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->
                                        it.setClientIdUriSchemes(List.of("http", "https"))))
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
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);
        oauth.redirectUri(null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_INVALID_PARAMETER);

    }

    @Test
    public void testClientIdMetadataDocumentExecutorVerifyClientId() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{}))
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
        // Client identifier URLs MUST have an "https" scheme.
        assertLoginAndError("Malformed URL", AbstractClientIdMetadataDocumentExecutor.ERR_MALFORMED_URL);

        // Client ID Verification:
        // Client identifier URLs MUST have an "https" scheme.
        assertLoginAndError("http://example.com/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_INVALID_URL_SCHEME);

        // Client ID Verification:
        // Client identifier URLs MUST contain a path component.
        assertLoginAndError("https://example.com", AbstractClientIdMetadataDocumentExecutor.ERR_EMPTY_URL_PATH);

        // Client ID Verification:
        // Client identifier URLs MUST NOT contain single-dot or double-dot path segments.
        assertLoginAndError("https://example.com/mcp/.././index.html", AbstractClientIdMetadataDocumentExecutor.ERR_URL_PATH_TRAVERSAL);

        // Client ID Verification:
        // Client identifier URLs MUST NOT contain a fragment component.
        assertLoginAndError("https://example.com/mcp#prompts", AbstractClientIdMetadataDocumentExecutor.ERR_URL_FRAGMENT);

        // Client ID Verification:
        // Client identifier URLs MUST NOT contain a username or password.
        assertLoginAndError("https://alice:wonderland@example.com/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_URL_USERINFO);

        // Client ID Verification:
        // Client identifier URLs SHOULD NOT include a query string component.
        assertLoginAndError("https://example.com/mcp?p1=123&p2=abc", AbstractClientIdMetadataDocumentExecutor.ERR_URL_QUERY);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://localhost:8443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_LOOPBACK_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://127.0.0.1:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_LOOPBACK_ADDRESS);

        // Client ID Verification:
        assertLoginAndError("https://example.co.jp/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_HOST_UNRESOLVED);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://::1:8443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_LOOPBACK_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://[::1]:8443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_LOOPBACK_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://0:0:0:0:0:0:0:1/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_LOOPBACK_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://10.0.0.0:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_PRIVATE_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://10.255.255.255:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_PRIVATE_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://172.16.0.0:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_PRIVATE_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://172.31.255.255:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_PRIVATE_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://192.168.0.0:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_PRIVATE_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a private address
        assertLoginAndError("https://192.168.255.255:443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_PRIVATE_ADDRESS);

        // Client ID Verification:
        // Client identifier is not a loopback address
        assertLoginAndError("https://fe12:3456:789a:1:::443/mcp", AbstractClientIdMetadataDocumentExecutor.ERR_LOOPBACK_ADDRESS);
    }


    @Test
    public void testClientIdMetadataDocumentExecutorValidateClientId() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setAllowLoopbackAddress(true);
                                    it.setAllowPermittedDomains(List.of("*.example.com","mcp.example.co.jp"));
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->
                                        it.setClientIdUriSchemes(List.of("http", "https"))))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        String clientId;

        // Client ID Validation:
        // The authorization server MAY choose to have its own heuristics and policies
        // around the trust of domain names used as client IDs.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_NOTALLOWED_DOMAIN);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorVerifyClientMetadata() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->
                                        it.setAllowLoopbackAddress(true)))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->
                                        it.setClientIdUriSchemes(List.of("http", "https"))))
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
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_CLIENTID_UNMATCH);

        // Client Metadata Verification:
        // The token_endpoint_auth_method property MUST NOT include
        // client_secret_post, client_secret_basic, client_secret_jwt,
        // or any other method based around a shared symmetric secret.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_JWT));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_NOTALLOWED_CLIENTAUTH);

        // Client Metadata Verification:
        // The client_secret and client_secret_expires_at properties MUST NOT be used.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setClientSecret("ClientSecretNotAllowed"));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_CLIENTSECRET);

        // Client Metadata Verification:
        // An authorization server MUST validate redirect URIs presented in an authorization request
        // against those in the metadata document.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setRedirectUris(Collections.singletonList("https://mcp.example.com/mcp/123abc")));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);
        assertLoginAndError(clientId, AbstractClientIdMetadataDocumentExecutor.ERR_METADATA_REDIRECTURI);
    }

    @Test
    public void testClientIdMetadataDocumentExecutorValidateClientMetadata() throws Exception {
        // register profiles
        String  json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                        it.setAllowLoopbackAddress(true);
                                        it.setOnlyAllowConfidentialClient(true);
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(POLICY_NAME, "La Premiere Politique", Boolean.TRUE)
                        .addCondition(ClientIdUriSchemeConditionFactory.PROVIDER_ID,
                                createConditionConfig(new ClientIdUriSchemeCondition.Configuration(), it->
                                        it.setClientIdUriSchemes(List.of("http", "https"))))
                        .addProfile(PROFILE_NAME)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        String clientIdPath;
        String clientId;
        OIDCClientRepresentation clientMetadata;

        // Client Metadata Validation:
        // Only a confidential client is allowed.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setTokenEndpointAuthMethod(null));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);
        assertLoginAndError(clientId, ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_CONFIDENTIAL_CLIENT);

        // Client Metadata Validation:
        // ether jwks or jwks_uri is required.
        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setJwksUri(null));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);
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
                                    it.setAllowLoopbackAddress(true);
                                    it.setOnlyAllowConfidentialClient(false);
                                    it.setAllowPermittedDomains(List.of("*.example.com", "mcp.example.co.jp", "localhost"));
                                    it.setRestrictSameDomain(true);
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        String originalRedirectUri = oauth.getRedirectUri();
        oauth.redirectUri("https://mcp.example.co.jp/callback");
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setRedirectUris(List.of("https://mcp.example.co.jp/callback", "https://mcp.example.com/callback")));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);
        assertLoginAndError(clientId, ClientIdMetadataDocumentExecutor.ERR_METADATA_URIS_SAMEDOMAIN);
        oauth.redirectUri(originalRedirectUri);

        // required properties
        json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setAllowLoopbackAddress(true);
                                    it.setRequiredProperties(List.of("client_uri", "logo_uri"));
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->i.setClientUri("https://client.example.com"));
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);
        assertLoginAndError(clientId, ClientIdMetadataDocumentExecutor.ERR_METADATA_NO_REQUIRED_PROPERTIES);

        // consent required
        // default value = true -> false
        // full scope disabled
        // default value = true -> false
        json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                (new ClientPoliciesUtil.ClientProfileBuilder()).createProfile(PROFILE_NAME, "Le Premier Profil")
                        .addExecutor(ClientIdMetadataDocumentExecutorFactory.PROVIDER_ID,
                                createExecutorConfig(new ClientIdMetadataDocumentExecutor.Configuration(), it->{
                                    it.setAllowLoopbackAddress(true);
                                    it.setConsentRequired(false);
                                    it.setFullScopeDisabled(false);
                                }))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        clientId = TestApplicationResourceUrls.getClientIdMetadataUri(generateSuffixedName("CIMD"));
        clientMetadata = setupOIDCClientRepresentation(clientId, i->{});
        oidcClientEndpointsResource.registerClientIdMetadata(clientMetadata, null);

        // send an authorization request
        String code = loginUserAndGetCode(clientId, null);

        // check the persisted client settings
        ClientRepresentation clientRepresentation = findByClientIdByAdmin(clientId);
        assertFalse(clientRepresentation.isConsentRequired());
        assertTrue(clientRepresentation.isFullScopeAllowed());

        // delete the persisted client
        deleteClientByAdmin(clientRepresentation.getId());
        assertThrows(jakarta.ws.rs.NotFoundException.class, ()->getClientByAdmin(clientRepresentation.getId()));
    }

    private String loginUserAndGetCode(String clientId, String nonce) {
        oauth.client(clientId);
        oauth.loginForm().nonce(nonce).codeChallenge(null).request(request).requestUri(requestUri).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        //grantPage.assertCurrent();
        //grantPage.assertGrants(OAuthGrantPage.PROFILE_CONSENT_TEXT, OAuthGrantPage.EMAIL_CONSENT_TEXT, OAuthGrantPage.ROLES_CONSENT_TEXT);
        //grantPage.accept();

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
