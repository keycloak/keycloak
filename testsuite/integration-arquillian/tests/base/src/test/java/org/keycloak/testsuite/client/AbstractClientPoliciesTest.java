/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.BouncyIntegration;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionConfigurationRepresentation;
import org.keycloak.representations.idm.ClientPolicyConditionRepresentation;
import org.keycloak.representations.idm.ClientPolicyExecutorRepresentation;
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeCondition;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesCondition;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesCondition;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterContextConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceGroupsCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceGroupsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceHostsCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceHostsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceRolesCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdaterSourceRolesConditionFactory;
import org.keycloak.services.clientpolicy.executor.ConsentRequiredExecutorFactory;
import org.keycloak.services.clientpolicy.executor.FullScopeDisabledExecutorFactory;
import org.keycloak.services.clientpolicy.executor.HolderOfKeyEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforcerExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthenticatorExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientUrisExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtExecutorFactory;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientAccessTypeConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientScopesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateContextConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateSourceGroupsConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateSourceHostsConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateSourceRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createHolderOfKeyEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureClientAuthenticatorExecutorConfig;

/**
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class AbstractClientPoliciesTest extends AbstractKeycloakTest {

    protected static final Logger logger = Logger.getLogger(AbstractClientPoliciesTest.class);

    protected static final String REALM_NAME = "test";
    protected static final String TEST_CLIENT = "test-app";
    protected static final String TEST_CLIENT_SECRET = "password";

    protected static final String POLICY_NAME = "MyPolicy";
    protected static final String PROFILE_NAME = "MyProfile";
    protected static final String SAMPLE_CLIENT_ROLE = "sample-client-role";

    protected static final String FAPI1_BASELINE_PROFILE_NAME = "fapi-1-baseline";
    protected static final String FAPI1_ADVANCED_PROFILE_NAME = "fapi-1-advanced";

    protected static final String ERR_MSG_MISSING_NONCE = "Missing parameter: nonce";
    protected static final String ERR_MSG_MISSING_STATE = "Missing parameter: state";
    protected static final String ERR_MSG_CLIENT_REG_FAIL = "Failed to send request";

    protected ClientRegistration reg;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeClass
    public static void beforeClientPoliciesTest() {
        BouncyIntegration.init();
    }

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Before
    public void before() throws Exception {
        setInitialAccessTokenForDynamicClientRegistration();
    }

    protected void setInitialAccessTokenForDynamicClientRegistration() {
        // get initial access token for Dynamic Client Registration with authentication
        reg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", REALM_NAME).build();
        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    @After
    public void after() throws Exception {
        reg.close();
        revertToBuiltinProfiles();
        revertToBuiltinPolicies();
    }

    protected void setupValidProfilesAndPolicies() throws Exception {
        // load profiles
        ClientProfileRepresentation loadedProfileRep = (new ClientProfileBuilder()).createProfile("ordinal-test-profile", "The profile that can be loaded.")
                .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                    createSecureClientAuthenticatorExecutorConfig(
                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID),
                        JWTClientAuthenticator.PROVIDER_ID))
                .toRepresentation();

        ClientProfileRepresentation loadedProfileRepWithoutBuiltinField = (new ClientProfileBuilder()).createProfile("lack-of-builtin-field-test-profile", "Without builtin field that is treated as builtin=false.")
                .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                    createSecureClientAuthenticatorExecutorConfig(
                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID),
                        JWTClientAuthenticator.PROVIDER_ID))
                .addExecutor(HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID,
                    createHolderOfKeyEnforceExecutorConfig(Boolean.TRUE))
                .addExecutor(SecureClientUrisExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureSigningAlgorithmForSignedJwtExecutorFactory.PROVIDER_ID, null)
                .toRepresentation();

        String json = (new ClientProfilesBuilder())
                .addProfile(loadedProfileRep)
                .addProfile(loadedProfileRepWithoutBuiltinField)
                .toString();
        updateProfiles(json);

        // load policies
        ClientPolicyRepresentation loadedPolicyRepNotExistAndDuplicatedProfile = 
                (new ClientPolicyBuilder()).createPolicy(
                        "new-policy",
                        "duplicated profiles are ignored.",
                        Boolean.TRUE)
                    .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, 
                        createClientAccessTypeConditionConfig(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_PUBLIC, ClientAccessTypeConditionFactory.TYPE_BEARERONLY)))
                    .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                            createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                    .addCondition(ClientScopesConditionFactory.PROVIDER_ID, 
                            createClientScopesConditionConfig(ClientScopesConditionFactory.OPTIONAL, Arrays.asList(SAMPLE_CLIENT_ROLE)))
                        .addProfile("ordinal-test-profile")
                        .addProfile("lack-of-builtin-field-test-profile")
                        .addProfile("ordinal-test-profile")

                    .toRepresentation();

        ClientPolicyRepresentation loadedPolicyRepWithoutBuiltinField = 
                (new ClientPolicyBuilder()).createPolicy(
                        "lack-of-builtin-field-test-policy",
                        "Without builtin field that is treated as builtin=false.",
                        null)
                    .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                            createClientUpdateContextConditionConfig(Arrays.asList(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER)))
                    .addCondition(ClientUpdaterSourceGroupsConditionFactory.PROVIDER_ID,
                            createClientUpdateSourceGroupsConditionConfig(Arrays.asList("topGroup")))
                    .addCondition(ClientUpdaterSourceHostsConditionFactory.PROVIDER_ID,
                            createClientUpdateSourceHostsConditionConfig(Arrays.asList("localhost", "127.0.0.1")))
                    .addCondition(ClientUpdaterSourceRolesConditionFactory.PROVIDER_ID,
                            createClientUpdateSourceRolesConditionConfig(Arrays.asList(AdminRoles.CREATE_CLIENT)))
                        .addProfile("lack-of-builtin-field-test-profile")
                    .toRepresentation();

        json = (new ClientPoliciesBuilder())
                    .addPolicy(loadedPolicyRepNotExistAndDuplicatedProfile)
                    .addPolicy(loadedPolicyRepWithoutBuiltinField)
                    .toString();
        updatePolicies(json);

    }


    protected void assertExpectedLoadedProfiles(Consumer<ClientProfilesRepresentation> modifiedAssertion) throws Exception {

        // retrieve loaded builtin profiles
        ClientProfilesRepresentation actualProfilesRep = getProfilesWithGlobals();

        // same profiles
        assertExpectedProfiles(actualProfilesRep, Arrays.asList(FAPI1_BASELINE_PROFILE_NAME, FAPI1_ADVANCED_PROFILE_NAME), Arrays.asList("ordinal-test-profile", "lack-of-builtin-field-test-profile"));

        // each profile - fapi-1-baseline
        ClientProfileRepresentation actualProfileRep =  getProfileRepresentation(actualProfilesRep, FAPI1_BASELINE_PROFILE_NAME, true);
        assertExpectedProfile(actualProfileRep, FAPI1_BASELINE_PROFILE_NAME, "Client profile, which enforce clients to conform 'Financial-grade API Security Profile 1.0 - Part 1: Baseline' specification.");

        // each executor
        assertExpectedExecutors(Arrays.asList(SecureSessionEnforceExecutorFactory.PROVIDER_ID, PKCEEnforcerExecutorFactory.PROVIDER_ID, SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                SecureClientUrisExecutorFactory.PROVIDER_ID, ConsentRequiredExecutorFactory.PROVIDER_ID, FullScopeDisabledExecutorFactory.PROVIDER_ID), actualProfileRep);
        assertExpectedSecureSessionEnforceExecutor(actualProfileRep);

        // each profile - ordinal-test-profile - updated
        actualProfileRep =  getProfileRepresentation(actualProfilesRep, "ordinal-test-profile", false);
        modifiedAssertion.accept(actualProfilesRep);

        // each executor
        assertExpectedExecutors(Arrays.asList(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID), actualProfileRep);
        assertExpectedSecureClientAuthEnforceExecutor(Arrays.asList(JWTClientAuthenticator.PROVIDER_ID), JWTClientAuthenticator.PROVIDER_ID, actualProfileRep);

        // each profile - lack-of-builtin-field-test-profile
        actualProfileRep =  getProfileRepresentation(actualProfilesRep, "lack-of-builtin-field-test-profile", false);
        assertExpectedProfile(actualProfileRep, "lack-of-builtin-field-test-profile", "Without builtin field that is treated as builtin=false.");

        // each executor
        assertExpectedExecutors(Arrays.asList(
                SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID,
                SecureClientUrisExecutorFactory.PROVIDER_ID,
                SecureRequestObjectExecutorFactory.PROVIDER_ID,
                SecureResponseTypeExecutorFactory.PROVIDER_ID,
                SecureSessionEnforceExecutorFactory.PROVIDER_ID,
                SecureSigningAlgorithmExecutorFactory.PROVIDER_ID,
                SecureSigningAlgorithmForSignedJwtExecutorFactory.PROVIDER_ID), actualProfileRep);
        assertExpectedSecureClientAuthEnforceExecutor(Arrays.asList(JWTClientAuthenticator.PROVIDER_ID), JWTClientAuthenticator.PROVIDER_ID, actualProfileRep);
        assertExpectedHolderOfKeyEnforceExecutor(true, actualProfileRep);
        assertExpectedSecureRedirectUriEnforceExecutor(actualProfileRep);
        assertExpectedSecureRequestObjectExecutor(actualProfileRep);
        assertExpectedSecureResponseTypeExecutor(actualProfileRep);
        assertExpectedSecureSessionEnforceExecutor(actualProfileRep);
        assertExpectedSecureSigningAlgorithmEnforceExecutor(actualProfileRep);
        assertExpectedSecureSigningAlgorithmForSignedJwtEnforceExecutor(actualProfileRep);
    }

    protected void assertExpectedLoadedPolicies(Consumer<ClientPoliciesRepresentation> modifiedAssertion) {

        // retrieve loaded builtin policies
        ClientPoliciesRepresentation actualPoliciesRep = getPolicies();

        // same policies
        assertExpectedPolicies(Arrays.asList("new-policy", "lack-of-builtin-field-test-policy"), actualPoliciesRep);

        // each policy - new-policy - updated
        ClientPolicyRepresentation actualPolicyRep =  getPolicyRepresentation(actualPoliciesRep, "new-policy");
        modifiedAssertion.accept(actualPoliciesRep);

        // each condition
        assertExpectedConditions(Arrays.asList(ClientAccessTypeConditionFactory.PROVIDER_ID, ClientRolesConditionFactory.PROVIDER_ID, ClientScopesConditionFactory.PROVIDER_ID), actualPolicyRep);
        assertExpectedClientAccessTypeCondition(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_PUBLIC, ClientAccessTypeConditionFactory.TYPE_BEARERONLY), actualPolicyRep);
        assertExpectedClientRolesCondition(Arrays.asList(SAMPLE_CLIENT_ROLE), actualPolicyRep);
        assertExpectedClientScopesCondition(ClientScopesConditionFactory.OPTIONAL, Arrays.asList(SAMPLE_CLIENT_ROLE), actualPolicyRep);

        // each policy - lack-of-builtin-field-test-policy
        actualPolicyRep = getPolicyRepresentation(actualPoliciesRep, "lack-of-builtin-field-test-policy");
        assertExpectedPolicy("lack-of-builtin-field-test-policy", "Without builtin field that is treated as builtin=false.", false, Arrays.asList("lack-of-builtin-field-test-profile"), actualPolicyRep);

        // each condition
        assertExpectedConditions(Arrays.asList(ClientUpdaterContextConditionFactory.PROVIDER_ID, ClientUpdaterSourceGroupsConditionFactory.PROVIDER_ID, ClientUpdaterSourceHostsConditionFactory.PROVIDER_ID, ClientUpdaterSourceRolesConditionFactory.PROVIDER_ID), actualPolicyRep);
        assertExpectedClientUpdateContextCondition(Arrays.asList(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER), actualPolicyRep);
        assertExpectedClientUpdateSourceGroupsCondition(Arrays.asList("topGroup"), actualPolicyRep);
        assertExpectedClientUpdateSourceHostsCondition(Arrays.asList("localhost", "127.0.0.1"), actualPolicyRep);
        assertExpectedClientUpdateSourceRolesCondition(Arrays.asList(AdminRoles.CREATE_CLIENT), actualPolicyRep);
    }


    protected String generateSuffixedName(String name) {
        return name + "-" + UUID.randomUUID().toString().subSequence(0, 7);
    }

    // Utilities for Request Object retrieved by reference from jwks_uri

    protected KeyPair setupJwks(String algorithm, ClientRepresentation clientRepresentation, ClientResource clientResource) throws Exception {
        // generate and register client keypair
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();
        oidcClientEndpointsResource.generateKeys(algorithm);
        Map<String, String> generatedKeys = oidcClientEndpointsResource.getKeysAsBase64();
        KeyPair keyPair = getKeyPairFromGeneratedBase64(generatedKeys, algorithm);

        // use and set jwks_url
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRepresentation).setJwksUrl(jwksUrl);
        clientResource.update(clientRepresentation);

        // set time offset, so that new keys are downloaded
        setTimeOffset(20);

        return keyPair;
    }

    protected KeyPair getKeyPairFromGeneratedBase64(Map<String, String> generatedKeys, String algorithm) throws Exception {
        // It seems that PemUtils.decodePrivateKey, decodePublicKey can only treat RSA type keys, not EC type keys. Therefore, these are not used.
        String privateKeyBase64 = generatedKeys.get(TestingOIDCEndpointsApplicationResource.PRIVATE_KEY);
        String publicKeyBase64 =  generatedKeys.get(TestingOIDCEndpointsApplicationResource.PUBLIC_KEY);
        PrivateKey privateKey = decodePrivateKey(Base64.decode(privateKeyBase64), algorithm);
        PublicKey publicKey = decodePublicKey(Base64.decode(publicKeyBase64), algorithm);
        return new KeyPair(publicKey, privateKey);
    }

    private PrivateKey decodePrivateKey(byte[] der, String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        String keyAlg = getKeyAlgorithmFromJwaAlgorithm(algorithm);
        KeyFactory kf = KeyFactory.getInstance(keyAlg, "BC");
        return kf.generatePrivate(spec);
    }

    private PublicKey decodePublicKey(byte[] der, String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        String keyAlg = getKeyAlgorithmFromJwaAlgorithm(algorithm);
        KeyFactory kf = KeyFactory.getInstance(keyAlg, "BC");
        return kf.generatePublic(spec);
    }

    private String getKeyAlgorithmFromJwaAlgorithm(String jwaAlgorithm) {
        String keyAlg = null;
        switch (jwaAlgorithm) {
            case org.keycloak.crypto.Algorithm.RS256:
            case org.keycloak.crypto.Algorithm.RS384:
            case org.keycloak.crypto.Algorithm.RS512:
            case org.keycloak.crypto.Algorithm.PS256:
            case org.keycloak.crypto.Algorithm.PS384:
            case org.keycloak.crypto.Algorithm.PS512:
                keyAlg = KeyType.RSA;
                break;
            case org.keycloak.crypto.Algorithm.ES256:
            case org.keycloak.crypto.Algorithm.ES384:
            case org.keycloak.crypto.Algorithm.ES512:
                keyAlg = KeyType.EC;
                break;
            default :
                throw new RuntimeException("Unsupported signature algorithm");
        }
        return keyAlg;
    }

   // Signed JWT for client authentication utility

    protected String createSignedRequestToken(String clientId, PrivateKey privateKey, PublicKey publicKey, String algorithm) {
        JsonWebToken jwt = createRequestToken(clientId, getRealmInfoUrl());
        String kid = KeyUtils.createKeyId(publicKey);
        SignatureSignerContext signer = oauth.createSigner(privateKey, kid, algorithm);
        return new JWSBuilder().kid(kid).jsonContent(jwt).sign(signer);
    }

    private String getRealmInfoUrl() {
        String authServerBaseUrl = UriUtils.getOrigin(oauth.getRedirectUri()) + "/auth";
        return KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.REALM_INFO_PATH).build(REALM_NAME).toString();
    }

    private JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
        JsonWebToken reqToken = new JsonWebToken();
        reqToken.id(AdapterUtils.generateId());
        reqToken.issuer(clientId);
        reqToken.subject(clientId);
        reqToken.audience(realmInfoUrl);

        int now = Time.currentTime();
        reqToken.iat(Long.valueOf(now));
        reqToken.exp(Long.valueOf(now + 10));
        reqToken.nbf(Long.valueOf(now));

        return reqToken;
    }

    // OAuth2 protocol operation with signed JWT for client authentication

    protected OAuthClient.AccessTokenResponse doAccessTokenRequestWithSignedJWT(String code, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getAccessTokenUrl(), parameters);
        return new OAuthClient.AccessTokenResponse(response);
    }

    protected OAuthClient.AccessTokenResponse doRefreshTokenRequestWithSignedJWT(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getRefreshTokenUrl(), parameters);
        return new OAuthClient.AccessTokenResponse(response);
    }

    protected HttpResponse doTokenIntrospectionWithSignedJWT(String tokenType, String tokenToIntrospect, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair("token", tokenToIntrospect));
        parameters.add(new BasicNameValuePair("token_type_hint", tokenType));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getTokenIntrospectionUrl(), parameters);
    }

    protected HttpResponse doTokenRevokeWithSignedJWT(String tokenType, String tokenToIntrospect, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair("token", tokenToIntrospect));
        parameters.add(new BasicNameValuePair("token_type_hint", tokenType));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getTokenRevocationUrl(), parameters);
    }

    protected HttpResponse doLogoutWithSignedJWT(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getLogoutUrl().build(), parameters);
    }

    private CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters) throws Exception {
        CloseableHttpClient client = new DefaultHttpClient();
        try {
            HttpPost post = new HttpPost(requestUrl);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, "UTF-8");
            post.setEntity(formEntity);
            return client.execute(post);
        } finally {
            oauth.closeClient(client);
        }
    }

    // Request Object utility

    protected AuthorizationEndpointRequestObject createValidRequestObjectForSecureRequestObjectExecutor(String clientId) throws URISyntaxException {
        AuthorizationEndpointRequestObject requestObject = new AuthorizationEndpointRequestObject();
        requestObject.id(KeycloakModelUtils.generateId());
        requestObject.iat(Long.valueOf(Time.currentTime()));
        requestObject.exp(requestObject.getIat() + Long.valueOf(300));
        requestObject.nbf(requestObject.getIat());
        requestObject.setClientId(clientId);
        requestObject.setResponseType("code");
        requestObject.setRedirectUriParam(oauth.getRedirectUri());
        requestObject.setScope("openid");
        String state = KeycloakModelUtils.generateId();
        oauth.stateParamHardcoded(state);
        requestObject.setState(state);
        requestObject.setMax_age(Integer.valueOf(600));
        requestObject.setOtherClaims("custom_claim_ein", "rot");
        requestObject.audience(Urls.realmIssuer(new URI(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"), REALM_NAME), "https://example.com");
        requestObject.setNonce(KeycloakModelUtils.generateId());
        return requestObject;
    }

    protected void registerRequestObject(AuthorizationEndpointRequestObject requestObject, String clientId, Algorithm sigAlg, boolean isUseRequestUri) throws URISyntaxException, IOException {
        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // Set required signature for request_uri
        // use and set jwks_url
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectSignatureAlg(sigAlg);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
        clientResource.update(clientRep);

        oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // generate and register client keypair
        oidcClientEndpointsResource.generateKeys(sigAlg.name());

        // register request object
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        oidcClientEndpointsResource.registerOIDCRequest(encodedRequestObject, sigAlg.name());

        if (isUseRequestUri) {
            oauth.request(null);
            oauth.requestUri(TestApplicationResourceUrls.clientRequestUri());
        } else {
            oauth.requestUri(null);
            oauth.request(oidcClientEndpointsResource.getOIDCRequest());
        }
    }

    // PKCE utility

    protected String generateS256CodeChallenge(String codeVerifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        md.update(codeVerifier.getBytes("ISO_8859_1"));
        byte[] digestBytes = md.digest();
        String codeChallenge = Base64Url.encode(digestBytes);
        return codeChallenge;
    }

    // OAuth2 protocol operation

    protected void doIntrospectAccessToken(OAuthClient.AccessTokenResponse tokenRes, String username, String clientId, String clientSecret) throws IOException {
        String tokenResponse = oauth.introspectAccessTokenWithClientCredential(clientId, clientSecret, tokenRes.getAccessToken());
        JsonNode jsonNode = objectMapper.readTree(tokenResponse);
        assertEquals(true, jsonNode.get("active").asBoolean());
        assertEquals(username, jsonNode.get("username").asText());
        assertEquals(clientId, jsonNode.get("client_id").asText());
        TokenMetadataRepresentation rep = objectMapper.readValue(tokenResponse, TokenMetadataRepresentation.class);
        assertEquals(true, rep.isActive());
        assertEquals(clientId, rep.getClientId());
        assertEquals(clientId, rep.getIssuedFor());
        events.expect(EventType.INTROSPECT_TOKEN).client(clientId).user((String)null).clearDetails().assertEvent();
    }

    protected void doTokenRevoke(String refreshToken, String clientId, String clientSecret, String userId, boolean isOfflineAccess) throws IOException {
        oauth.clientId(clientId);
        oauth.doTokenRevoke(refreshToken, "refresh_token", clientSecret);

        // confirm revocation
        OAuthClient.AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(refreshToken, clientSecret);
        assertEquals(400, tokenRes.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenRes.getError());
        if (isOfflineAccess) assertEquals("Offline user session not found", tokenRes.getErrorDescription());
        else assertEquals("Session not active", tokenRes.getErrorDescription());

        events.expect(EventType.REVOKE_GRANT).clearDetails().client(clientId).user(userId).assertEvent();
    }

    // Client CRUD operation by Admin REST API primitives

    protected String createClientByAdmin(String clientName, Consumer<ClientRepresentation> op) throws ClientPolicyException {
        ClientRepresentation clientRep = new ClientRepresentation();
        clientRep.setClientId(clientName);
        clientRep.setName(clientName);
        clientRep.setProtocol("openid-connect");
        clientRep.setBearerOnly(Boolean.FALSE);
        clientRep.setPublicClient(Boolean.FALSE);
        clientRep.setServiceAccountsEnabled(Boolean.TRUE);
        clientRep.setRedirectUris(Collections.singletonList(ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));
        op.accept(clientRep);
        Response resp = adminClient.realm(REALM_NAME).clients().create(clientRep);
        if (resp.getStatus() == Response.Status.BAD_REQUEST.getStatusCode()) {
            String respBody = resp.readEntity(String.class);
            Map<String, String> responseJson = null;
            try {
                responseJson = JsonSerialization.readValue(respBody, Map.class);
            } catch (IOException e) {
                fail();
            }
            throw new ClientPolicyException(responseJson.get(OAuth2Constants.ERROR), responseJson.get(OAuth2Constants.ERROR_DESCRIPTION));
        }
        resp.close();
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        // registered components will be removed automatically when a test method finishes regardless of its success or failure.
        String cId = ApiUtil.getCreatedId(resp);
        testContext.getOrCreateCleanup(REALM_NAME).addClientUuid(cId);
        return cId;
    }

    protected ClientRepresentation getClientByAdmin(String cId) throws ClientPolicyException {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cId);
        try {
            return clientResource.toRepresentation();
        } catch (BadRequestException bre) {
            processClientPolicyExceptionByAdmin(bre);
        }
        return null;
    }

    protected ClientRepresentation getClientByAdminWithName(String clientName) {
        return adminClient.realm(REALM_NAME).clients().findByClientId(clientName).get(0);
    }

    protected void updateClientByAdmin(String cId, Consumer<ClientRepresentation> op) throws ClientPolicyException {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        op.accept(clientRep);
        try {
            clientResource.update(clientRep);
        } catch (BadRequestException bre) {
            processClientPolicyExceptionByAdmin(bre);
        }
    }

    protected void deleteClientByAdmin(String cId) throws ClientPolicyException {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cId);
        try {
            clientResource.remove();
        } catch (BadRequestException bre) {
            processClientPolicyExceptionByAdmin(bre);
        }
    }

    private void processClientPolicyExceptionByAdmin(BadRequestException bre) throws ClientPolicyException {
        Response resp = bre.getResponse();
        if (resp.getStatus() != Response.Status.BAD_REQUEST.getStatusCode()) {
            resp.close();
            return;
        }

        String respBody = resp.readEntity(String.class);
        Map<String, String> responseJson = null;
        try {
            responseJson = JsonSerialization.readValue(respBody, Map.class);
        } catch (IOException e) {
            fail();
        }
        throw new ClientPolicyException(responseJson.get(OAuth2Constants.ERROR), responseJson.get(OAuth2Constants.ERROR_DESCRIPTION));
    }

    // Registration/Initial Access Token acquisition for Dynamic Client Registration

    protected void restartAuthenticatedClientRegistrationSetting() throws ClientRegistrationException {
        reg.close();
        reg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", REALM_NAME).build();
        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    protected void authCreateClients() {
        reg.auth(Auth.token(getToken("create-clients", "password")));
    }

    protected void authManageClients() {
        reg.auth(Auth.token(getToken("manage-clients", "password")));
    }

    protected void authNoAccess() {
        reg.auth(Auth.token(getToken("no-access", "password")));
    }

    private String getToken(String username, String password) {
        try {
            return oauth.doGrantAccessTokenRequest(REALM_NAME, username, password, null, Constants.ADMIN_CLI_CLIENT_ID, null).getAccessToken();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // Client CRUD operation by Dynamic Client Registration primitives

    protected String createClientDynamically(String clientName, Consumer<OIDCClientRepresentation> op) throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = new OIDCClientRepresentation();
        clientRep.setClientName(clientName);
        clientRep.setClientUri(ServerURLs.getAuthServerContextRoot());
        clientRep.setRedirectUris(Collections.singletonList(ServerURLs.getAuthServerContextRoot() + "/auth/realms/master/app/auth"));
        op.accept(clientRep);
        OIDCClientRepresentation response = reg.oidc().create(clientRep);
        reg.auth(Auth.token(response));
        // registered components will be removed automatically when a test method finishes regardless of its success or failure.
        String clientId = response.getClientId();
        testContext.getOrCreateCleanup(REALM_NAME).addClientUuid(clientId);
        return clientId;
    }

    protected OIDCClientRepresentation getClientDynamically(String clientId) throws ClientRegistrationException {
        return reg.oidc().get(clientId);
    }

    protected void updateClientDynamically(String clientId, Consumer<OIDCClientRepresentation> op) throws ClientRegistrationException {
        OIDCClientRepresentation clientRep = reg.oidc().get(clientId);
        op.accept(clientRep);
        OIDCClientRepresentation response = reg.oidc().update(clientRep);
        reg.auth(Auth.token(response));
    }

    protected void deleteClientDynamically(String clientId) throws ClientRegistrationException {
        reg.oidc().delete(clientId);
    }

    // Profiles Operation

    protected String convertToProfilesJson(ClientProfilesRepresentation reps) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(reps);
        } catch (JsonProcessingException e) {
            fail();
        }
        return json;
    }

    // TODO: Possibly change this to accept ClientProfilesRepresentation instead of String to have more type-safety.
    protected void updateProfiles(String json) throws ClientPolicyException {
        try {
            ClientProfilesRepresentation clientProfiles = JsonSerialization.readValue(json, ClientProfilesRepresentation.class);
            adminClient.realm(REALM_NAME).clientPoliciesProfilesResource().updateProfiles(clientProfiles);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update profiles failed", e.getResponse().getStatusInfo().toString());
        } catch (Exception e) {
            throw new ClientPolicyException("update profiles failed", e.getMessage());
        }
    }

    protected void updateProfiles(ClientProfilesRepresentation reps) throws ClientPolicyException {
        updateProfiles(convertToProfilesJson(reps));
    }

    protected void revertToBuiltinProfiles() throws ClientPolicyException {
        updateProfiles("{}");
    }

    protected ClientProfilesRepresentation getProfilesWithGlobals() {
        return adminClient.realm(REALM_NAME).clientPoliciesProfilesResource().getProfiles(true);
    }

    protected ClientProfilesRepresentation getProfilesWithoutGlobals() {
        return adminClient.realm(REALM_NAME).clientPoliciesProfilesResource().getProfiles(false);
    }

    protected String convertToProfileJson(ClientProfileRepresentation rep) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(rep);
        } catch (JsonProcessingException e) {
            fail();
        }
        return json;
    }

    protected ClientProfileRepresentation convertToProfile(String json) {
        ClientProfileRepresentation rep = null;
        try {
            rep = JsonSerialization.readValue(json, ClientProfileRepresentation.class);
        } catch (IOException e) {
            fail();
        }
        return rep;
    }

    protected ClientProfileRepresentation getProfile(String name) {
        if (name == null) return null;

        ClientProfilesRepresentation reps = getProfilesWithGlobals();
        if (reps == null || reps.getProfiles() == null) return null;

        if (reps.getProfiles().stream().anyMatch(i->name.equals(i.getName()))) {
            return reps.getProfiles().stream().filter(i->name.equals(i.getName())).collect(Collectors.toList()).get(0);
        } else {
            return null;
        }
    }

    protected String getProfileJson(String name) {
        return convertToProfileJson(getProfile(name));
    }

    protected void addProfile(ClientProfileRepresentation profileRep) throws ClientPolicyException {
        ClientProfilesRepresentation reps = getProfilesWithoutGlobals();
        if (reps == null || reps.getProfiles() == null) return;
        reps.getProfiles().add(profileRep);
        updateProfiles(convertToProfilesJson(reps));
        return;
    }

    protected void updateProfile(ClientProfileRepresentation profileRep) throws ClientPolicyException {
        if (profileRep == null || profileRep.getName() == null) return;
        String profileName = profileRep.getName();

        ClientProfilesRepresentation reps = getProfilesWithoutGlobals();

        if (reps.getProfiles().stream().anyMatch(i->profileName.equals(i.getName()))) {
            ClientProfileRepresentation rep = reps.getProfiles().stream().filter(i->profileName.equals(i.getName())).collect(Collectors.toList()).get(0);
            reps.getProfiles().remove(rep);
            reps.getProfiles().add(profileRep);
            updateProfiles(convertToProfilesJson(reps));
        } else {
            return;
        }
    }

    protected void deleteProfile(String profileName) throws ClientPolicyException {
        if (profileName == null) return;

        ClientProfilesRepresentation reps = getProfilesWithoutGlobals();

        if (reps.getProfiles().stream().anyMatch(i->profileName.equals(i.getName()))) {
            ClientProfileRepresentation rep = reps.getProfiles().stream().filter(i->profileName.equals(i.getName())).collect(Collectors.toList()).get(0);
            reps.getProfiles().remove(rep);
            updateProfiles(convertToProfilesJson(reps));
        } else {
            return;
        }
    }

    // Policies Operation

    protected String convertToPoliciesJson(ClientPoliciesRepresentation reps) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(reps);
        } catch (JsonProcessingException e) {
            fail();
        }
        return json;
    }

    // TODO: Possibly change this to accept ClientPoliciesRepresentation instead of String to have more type-safety.
    protected void updatePolicies(String json) throws ClientPolicyException {
        try {
            ClientPoliciesRepresentation clientPolicies = json==null ? null : JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
            adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource().updatePolicies(clientPolicies);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update policies failed", e.getResponse().getStatusInfo().toString());
        } catch (IOException e) {
            throw new ClientPolicyException("update policies failed", e.getMessage());
        }
    }

    protected void revertToBuiltinPolicies() throws ClientPolicyException {
        updatePolicies("{}");
    }

    protected ClientPoliciesRepresentation getPolicies() {
        return adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource().getPolicies();
    }

    protected String convertToPolicyJson(ClientPolicyRepresentation rep) {
        String json = null;
        try {
            json = objectMapper.writeValueAsString(rep);
        } catch (JsonProcessingException e) {
            fail();
        }
        return json;
    }

    protected ClientPolicyRepresentation convertToPolicy(String json) {
    	ClientPolicyRepresentation rep = null;
        try {
            rep = JsonSerialization.readValue(json, ClientPolicyRepresentation.class);
        } catch (IOException e) {
            fail();
        }
        return rep;
    }

    protected ClientPolicyRepresentation getPolicy(String name) {
        if (name == null) return null;

        ClientPoliciesRepresentation reps = getPolicies();
        if (reps == null || reps.getPolicies() == null) return null;

        if (reps.getPolicies().stream().anyMatch(i->name.equals(i.getName()))) {
            return reps.getPolicies().stream().filter(i->name.equals(i.getName())).collect(Collectors.toList()).get(0);
        } else {
            return null;
        }
    }

    protected String getPolicyJson(String name) {
        return convertToPolicyJson(getPolicy(name));
    }

    protected void addPolicy(ClientPolicyRepresentation policyRep) throws ClientPolicyException {
        ClientPoliciesRepresentation reps = getPolicies();
        if (reps == null || reps.getPolicies() == null) return;
        reps.getPolicies().add(policyRep);
        updatePolicies(convertToPoliciesJson(reps));
        return;
    }

    protected void updatePolicy(ClientPolicyRepresentation policyRep) throws ClientPolicyException {
        if (policyRep == null || policyRep.getName() == null) return;
        String policyName = policyRep.getName();

        ClientPoliciesRepresentation reps = getPolicies();

        if (reps.getPolicies().stream().anyMatch(i->policyName.equals(i.getName()))) {
            ClientPolicyRepresentation rep = reps.getPolicies().stream().filter(i->policyName.equals(i.getName())).collect(Collectors.toList()).get(0);
            reps.getPolicies().remove(rep);
            reps.getPolicies().add(policyRep);
            updatePolicies(convertToPoliciesJson(reps));
        } else {
            return;
        }
    }

    protected void deletePolicy(String policyName) throws ClientPolicyException {
        if (policyName == null) return;

        ClientPoliciesRepresentation reps = getPolicies();

        if (reps.getPolicies().stream().anyMatch(i->policyName.equals(i.getName()))) {
            ClientPolicyRepresentation rep = reps.getPolicies().stream().filter(i->policyName.equals(i.getName())).collect(Collectors.toList()).get(0);
            reps.getPolicies().remove(rep);
            updatePolicies(convertToPoliciesJson(reps));
        } else {
            return;
        }
    }

    // Assertions about profiles

    // profiles

    protected ClientProfilesRepresentation getProfilesRepresentation(String json) {
        return getCompoundsRepresentation(json, ClientProfilesRepresentation.class);
    }

    // profile

    protected ClientProfileRepresentation getProfileRepresentation(ClientProfilesRepresentation profilesRep, String name, boolean global) {
        Function<ClientProfilesRepresentation, List<ClientProfileRepresentation>> profilesListGetter = global ? ClientProfilesRepresentation::getGlobalProfiles : ClientProfilesRepresentation::getProfiles;
        return getCompoundRepresentation(profilesRep, name, profilesListGetter, (ClientProfileRepresentation i)->i.getName());
    }

    protected void assertExpectedProfiles(ClientProfilesRepresentation profilesRep, List<String> expectedGlobalProfiles, List<String> expectedRealmProfiles) {
        assertExpectedCompounds(expectedGlobalProfiles, profilesRep, (ClientProfilesRepresentation i)->i.getGlobalProfiles(), (ClientProfileRepresentation i)->i.getName());
        assertExpectedCompounds(expectedRealmProfiles, profilesRep, (ClientProfilesRepresentation i)->i.getProfiles(), (ClientProfileRepresentation i)->i.getName());
    }

    protected void assertExpectedProfile(ClientProfileRepresentation actualProfileRep, String name, String description) {
        assertNotNull(actualProfileRep);
        assertEquals(description, actualProfileRep.getDescription());
    }

    // executors

    protected void assertExpectedExecutors(List<String> expectedExecutors, ClientProfileRepresentation profileRep) {
        List<String> actualExecutorNames = profileRep.getExecutors().stream()
                .map(ClientPolicyExecutorRepresentation::getExecutorProviderId)
                .collect(Collectors.toList());
        assertThat(actualExecutorNames, Matchers.containsInAnyOrder(expectedExecutors.toArray()));
    }

    protected void assertExpectedHolderOfKeyEnforceExecutor(boolean autoConfigure, ClientProfileRepresentation profileRep) {
        assertExpectedAutoConfiguredExecutor(autoConfigure, HolderOfKeyEnforcerExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedPKCEEnforceExecutor(boolean autoConfigure, ClientProfileRepresentation profileRep) {
        assertExpectedAutoConfiguredExecutor(autoConfigure, PKCEEnforcerExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedSecureClientAuthEnforceExecutor(List<String> expectedAllowedClientAuthenticators, String expectedAutoConfiguredClientAuthenticator, ClientProfileRepresentation profileRep) throws Exception {
        assertNotNull(profileRep);
        JsonNode actualExecutorConfig = getConfigOfExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID, profileRep);
        assertNotNull(actualExecutorConfig);
        Set<String> actualClientAuthns = new HashSet<>((Collection<String>) JsonSerialization.readValue(actualExecutorConfig.get(SecureClientAuthenticatorExecutorFactory.ALLOWED_CLIENT_AUTHENTICATORS).toString(), List.class));
        assertEquals(new HashSet<>(expectedAllowedClientAuthenticators), actualClientAuthns);

        String actualAutoConfiguredClientAuthenticator = actualExecutorConfig.get(SecureClientAuthenticatorExecutorFactory.DEFAULT_CLIENT_AUTHENTICATOR).textValue();
        assertEquals(expectedAutoConfiguredClientAuthenticator, actualAutoConfiguredClientAuthenticator);
    }

    protected void assertExpectedSecureRedirectUriEnforceExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedEmptyConfig(SecureClientUrisExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedSecureRequestObjectExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedEmptyConfig(SecureRequestObjectExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedSecureResponseTypeExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedEmptyConfig(SecureResponseTypeExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedSecureSessionEnforceExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedEmptyConfig(SecureSessionEnforceExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedSecureSigningAlgorithmEnforceExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedEmptyConfig(SecureSigningAlgorithmExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedSecureSigningAlgorithmForSignedJwtEnforceExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedEmptyConfig(SecureSigningAlgorithmForSignedJwtExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedAutoConfiguredExecutor(boolean expectedAutoConfigure, String providerId, ClientProfileRepresentation profileRep) {
        assertNotNull(profileRep);
        JsonNode actualExecutorConfig = getConfigOfExecutor(providerId, profileRep);
        assertNotNull(actualExecutorConfig);
        boolean actualAutoConfigure = actualExecutorConfig.get("auto-configure") == null ? false : actualExecutorConfig.get("auto-configure").asBoolean();
        assertEquals(expectedAutoConfigure, actualAutoConfigure);
    }

    private JsonNode getConfigOfExecutor(String providerId, ClientProfileRepresentation profileRep) {
        ClientPolicyExecutorRepresentation executorRep = profileRep.getExecutors().stream()
                .filter(profileRepp -> providerId.equals(profileRepp.getExecutorProviderId()))
                .findFirst().orElse(null);
        return executorRep == null ? null : executorRep.getConfiguration();
    }

    // Assertions about policies

    // policies

    protected ClientPoliciesRepresentation getPoliciesRepresentation(String json) {
        return getCompoundsRepresentation(json, ClientPoliciesRepresentation.class);
    }

    // policy

    protected ClientPolicyRepresentation getPolicyRepresentation(ClientPoliciesRepresentation policiesRep, String name) {
        return getCompoundRepresentation(policiesRep, name, (ClientPoliciesRepresentation i)->i.getPolicies(), (ClientPolicyRepresentation i)->i.getName());
    }

    protected void assertExpectedPolicies(List<String> expectedPolicies, ClientPoliciesRepresentation policiesRep) {
        assertNotNull(policiesRep);
        List<ClientPolicyRepresentation> reps = policiesRep.getPolicies();
        if (reps == null) {
            assertNull(expectedPolicies);
            return;
        }
        Set<String> actualPolicies = reps.stream().map(i->i.getName()).collect(Collectors.toSet());
        assertEquals(new HashSet<>(expectedPolicies), actualPolicies);
    }

    protected void assertExpectedPolicy(String name, String description, boolean isEnabled, List<String> profiles, ClientPolicyRepresentation actualPolicyRep) {
        assertNotNull(actualPolicyRep);
        assertEquals(description, actualPolicyRep.getDescription());
        assertEquals(isEnabled, actualPolicyRep.isEnabled());
        assertEquals(new HashSet<>(profiles), new HashSet<>(actualPolicyRep.getProfiles()));
    }

    // conditions

    protected void assertExpectedConditions(List<String> expectedConditions, ClientPolicyRepresentation policyRep) {
        List<String> actualConditionNames = policyRep.getConditions().stream()
                .map(ClientPolicyConditionRepresentation::getConditionProviderId)
                .collect(Collectors.toList());
        assertThat(actualConditionNames, Matchers.containsInAnyOrder(expectedConditions.toArray()));
    }

    protected void assertExpectedAnyClientCondition(ClientPolicyRepresentation policyRep) {
        ClientPolicyConditionConfigurationRepresentation config = getConfigAsExpectedType(policyRep, AnyClientConditionFactory.PROVIDER_ID, ClientPolicyConditionConfigurationRepresentation.class);
        Assert.assertTrue("Expected empty configuration for provider " + AnyClientConditionFactory.PROVIDER_ID, config.getConfigAsMap().isEmpty());
    }

    protected void assertExpectedClientAccessTypeCondition(List<String> type, ClientPolicyRepresentation policyRep) {
        ClientAccessTypeCondition.Configuration cfg = getConfigAsExpectedType(policyRep, ClientAccessTypeConditionFactory.PROVIDER_ID, ClientAccessTypeCondition.Configuration.class);
        Assert.assertEquals(cfg.getType(), type);
    }

    protected void assertExpectedClientRolesCondition(List<String> roles, ClientPolicyRepresentation policyRep) {
        ClientRolesCondition.Configuration cfg = getConfigAsExpectedType(policyRep, ClientRolesConditionFactory.PROVIDER_ID,  ClientRolesCondition.Configuration.class);
        Assert.assertEquals(cfg.getRoles(), roles);
    }

    protected void assertExpectedClientScopesCondition(String type, List<String> scopes, ClientPolicyRepresentation policyRep) {
        ClientScopesCondition.Configuration cfg = getConfigAsExpectedType(policyRep, ClientScopesConditionFactory.PROVIDER_ID,  ClientScopesCondition.Configuration.class);
        Assert.assertEquals(cfg.getType(), type);
        Assert.assertEquals(cfg.getScope(), scopes);
    }

    protected void assertExpectedClientUpdateContextCondition(List<String> updateClientSources, ClientPolicyRepresentation policyRep) {
        ClientUpdaterContextCondition.Configuration cfg = getConfigAsExpectedType(policyRep, ClientUpdaterContextConditionFactory.PROVIDER_ID,  ClientUpdaterContextCondition.Configuration.class);
        Assert.assertEquals(cfg.getUpdateClientSource(), updateClientSources);
    }

    protected void assertExpectedClientUpdateSourceGroupsCondition(List<String> groups, ClientPolicyRepresentation policyRep) {
        ClientUpdaterSourceGroupsCondition.Configuration cfg = getConfigAsExpectedType(policyRep, ClientUpdaterSourceGroupsConditionFactory.PROVIDER_ID,  ClientUpdaterSourceGroupsCondition.Configuration.class);
        Assert.assertEquals(cfg.getGroups(), groups);
    }

    protected void assertExpectedClientUpdateSourceHostsCondition(List<String> trustedHosts, ClientPolicyRepresentation policyRep) {
        ClientUpdaterSourceHostsCondition.Configuration cfg = getConfigAsExpectedType(policyRep, ClientUpdaterSourceHostsConditionFactory.PROVIDER_ID,  ClientUpdaterSourceHostsCondition.Configuration.class);
        Assert.assertEquals(cfg.getTrustedHosts(), trustedHosts);
    }

    protected void assertExpectedClientUpdateSourceRolesCondition(List<String> roles, ClientPolicyRepresentation policyRep) {
        ClientUpdaterSourceRolesCondition.Configuration cfg = getConfigAsExpectedType(policyRep, ClientUpdaterSourceRolesConditionFactory.PROVIDER_ID,  ClientUpdaterSourceRolesCondition.Configuration.class);
        Assert.assertEquals(cfg.getRoles(), roles);
    }

    private <CFG extends ClientPolicyConditionConfigurationRepresentation> CFG getConfigAsExpectedType(ClientPolicyRepresentation policyRep, String conditionProviderId, Class<CFG> configClass) {
        ClientPolicyConditionRepresentation conditionRep = policyRep.getConditions().stream()
                .filter(condition -> conditionProviderId.equals(condition.getConditionProviderId()))
                .findFirst().orElseThrow(() -> new AssertionError("Expected to contain configuration for condition " + conditionProviderId));

        return JsonSerialization.mapper.convertValue(conditionRep.getConfiguration(), configClass);
    }

    // profiles/policies common (compounds)

    private <T> T getCompoundsRepresentation(String json, Class<T> clazz) {
        T rep = null;
        try {
            rep = JsonSerialization.readValue(json, clazz);
        } catch (IOException ioe) {
            fail();
        }
        return rep;
    }

    private <T, R> void assertExpectedCompounds(List<String> expected, R rep, Function<R, List<T>> f, Function<T, String> g) {
        assertNotNull(rep);
        List<T> reps = f.apply(rep);
        if (reps == null) {
            assertNull(expected);
            return;
        }
        Set<String> actual = reps.stream().map(i->g.apply(i)).collect(Collectors.toSet());
        assertEquals(new HashSet<>(expected), actual);
    }

    // profile/policy common (compound)

    private <T, R> T getCompoundRepresentation(R rep, String name, Function<R, List<T>> f, Function<T, String> g) {
        assertNotNull(rep);
        if (f.apply(rep) == null) return null;
        List<T> reps = f.apply(rep).stream().filter(i->g.apply(i).equals(name)).collect(Collectors.toList());
        if (reps == null) return null;
        if (reps.size() != 1) return null;
        return reps.get(0);
    }

    private void assertExpectedEmptyConfig(String executorProviderId, ClientProfileRepresentation profileRep) {
        JsonNode config = getConfigOfExecutor(executorProviderId, profileRep);
        Assert.assertTrue("Expected empty configuration for provider " + executorProviderId, config.isEmpty());
    }

}
