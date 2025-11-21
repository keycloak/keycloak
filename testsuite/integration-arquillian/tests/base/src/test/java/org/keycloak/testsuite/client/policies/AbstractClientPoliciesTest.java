/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.testsuite.client.policies;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
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

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.core.Response;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.common.util.Time;
import org.keycloak.common.util.UriUtils;
import org.keycloak.constants.ServiceUrlConstants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.LoginProtocol;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.JsonWebToken;
import org.keycloak.representations.RefreshToken;
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
import org.keycloak.representations.idm.EventRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
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
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutor;
import org.keycloak.services.clientpolicy.executor.ClientSecretRotationExecutorFactory;
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
import org.keycloak.testsuite.pages.ErrorPage;
import org.keycloak.testsuite.pages.LogoutConfirmPage;
import org.keycloak.testsuite.pages.OAuthGrantPage;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.util.ClientPoliciesUtil;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.ClientScopeBuilder;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.SignatureSignerUtil;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.LogoutResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.util.JsonSerialization;

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
import org.jboss.arquillian.graphene.page.Page;
import org.jboss.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;

import static org.keycloak.testsuite.admin.ApiUtil.findUserByUsername;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientAccessTypeConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientScopesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateContextConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateSourceGroupsConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateSourceHostsConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientUpdateSourceRolesConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createHolderOfKeyEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createPKCEEnforceExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createSecureClientAuthenticatorExecutorConfig;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    protected static final String SAMPLE_CLIENT_SCOPE = "sample-client-scope";

    protected static final String FAPI1_BASELINE_PROFILE_NAME = "fapi-1-baseline";
    protected static final String FAPI1_ADVANCED_PROFILE_NAME = "fapi-1-advanced";
    protected static final String FAPI_CIBA_PROFILE_NAME = "fapi-ciba";
    protected static final String FAPI2_SECURITY_PROFILE_NAME = "fapi-2-security-profile";
    protected static final String FAPI2_MESSAGE_SIGNING_PROFILE_NAME = "fapi-2-message-signing";
    protected static final String OAUTH2_1_CONFIDENTIAL_CLIENT_PROFILE_NAME = "oauth-2-1-for-confidential-client";
    protected static final String OAUTH2_1_PUBLIC_CLIENT_PROFILE_NAME = "oauth-2-1-for-public-client";
    protected static final String SAML_SECURITY_PROFILE_NAME = "saml-security-profile";
    protected static final String FAPI2_DPOP_SECURITY_PROFILE_NAME = "fapi-2-dpop-security-profile";
    protected static final String FAPI2_DPOP_MESSAGE_SIGNING_PROFILE_NAME = "fapi-2-dpop-message-signing";

    protected static final String ERR_MSG_MISSING_NONCE = "Missing parameter: nonce";
    protected static final String ERR_MSG_MISSING_STATE = "Missing parameter: state";
    protected static final String ERR_MSG_CLIENT_REG_FAIL = "Failed to send request";
    protected static final String ERR_MSG_REQ_NOT_ALLOWED = "Request not allowed";

    protected ClientRegistration reg;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    protected static final String CLIENT_NAME = "Zahlungs-App";
    protected static final String TEST_USER_NAME = "test-user@localhost";
    protected static final String TEST_USER_PASSWORD = "password";

    protected static final String DEVICE_APP = "test-device";
    protected static final String DEVICE_APP_PUBLIC = "test-device-public";
    protected static String userId;

    protected static final String SECRET_ROTATION_PROFILE = "ClientSecretRotationProfile";
    protected static final String SECRET_ROTATION_POLICY = "ClientSecretRotationPolicy";
    
    @Page
    protected OAuthGrantPage grantPage;

    @Page
    protected ErrorPage errorPage;

    @Page
    protected LogoutConfirmPage logoutConfirmPage;
    
    @Rule
    public AssertEvents events = new AssertEvents(this);

    private PkceGenerator pkceGenerator;

    protected String request;
    protected String requestUri;

    @Before
    public void before() throws Exception {
        setInitialAccessTokenForDynamicClientRegistration();
        adminClient.realm(REALM_NAME).clientScopes().create(ClientScopeBuilder.create().name(SAMPLE_CLIENT_SCOPE).protocol(OIDCLoginProtocol.LOGIN_PROTOCOL).build());
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
        pkceGenerator = null;
        request = null;
        requestUri = null;
    }

    protected void setupValidProfilesAndPolicies() throws Exception {
        // load profiles
        ClientProfileRepresentation loadedProfileRep = (new ClientProfileBuilder()).createProfile("ordinal-test-profile", "The profile that can be loaded.")
                .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                    createSecureClientAuthenticatorExecutorConfig(
                            List.of(JWTClientAuthenticator.PROVIDER_ID),
                            JWTClientAuthenticator.PROVIDER_ID))
                .toRepresentation();

        ClientProfileRepresentation loadedProfileRepWithoutBuiltinField = (new ClientProfileBuilder()).createProfile("lack-of-builtin-field-test-profile", "Without builtin field that is treated as builtin=false.")
                .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                    createSecureClientAuthenticatorExecutorConfig(
                            List.of(JWTClientAuthenticator.PROVIDER_ID),
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
                            createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                    .addCondition(ClientScopesConditionFactory.PROVIDER_ID, 
                            createClientScopesConditionConfig(ClientScopesConditionFactory.OPTIONAL, List.of(SAMPLE_CLIENT_SCOPE)))
                        .addProfile("ordinal-test-profile")
                        .addProfile("lack-of-builtin-field-test-profile")

                    .toRepresentation();

        ClientPolicyRepresentation loadedPolicyRepWithoutBuiltinField = 
                (new ClientPolicyBuilder()).createPolicy(
                        "lack-of-builtin-field-test-policy",
                        "Without builtin field that is treated as builtin=false.",
                        null)
                    .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                            createClientUpdateContextConditionConfig(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER)))
                    .addCondition(ClientUpdaterSourceGroupsConditionFactory.PROVIDER_ID,
                            createClientUpdateSourceGroupsConditionConfig(List.of("topGroup")))
                    .addCondition(ClientUpdaterSourceHostsConditionFactory.PROVIDER_ID,
                            createClientUpdateSourceHostsConditionConfig(Arrays.asList("localhost", "127.0.0.1")))
                    .addCondition(ClientUpdaterSourceRolesConditionFactory.PROVIDER_ID,
                            createClientUpdateSourceRolesConditionConfig(List.of(AdminRoles.CREATE_CLIENT)))
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
        assertExpectedProfiles(actualProfilesRep, Arrays.asList(FAPI1_BASELINE_PROFILE_NAME, FAPI1_ADVANCED_PROFILE_NAME, FAPI_CIBA_PROFILE_NAME, FAPI2_SECURITY_PROFILE_NAME, FAPI2_MESSAGE_SIGNING_PROFILE_NAME, OAUTH2_1_CONFIDENTIAL_CLIENT_PROFILE_NAME, OAUTH2_1_PUBLIC_CLIENT_PROFILE_NAME, SAML_SECURITY_PROFILE_NAME, FAPI2_DPOP_SECURITY_PROFILE_NAME, FAPI2_DPOP_MESSAGE_SIGNING_PROFILE_NAME), Arrays.asList("ordinal-test-profile", "lack-of-builtin-field-test-profile"));

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
        assertExpectedExecutors(List.of(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID), actualProfileRep);
        assertExpectedSecureClientAuthEnforceExecutor(List.of(JWTClientAuthenticator.PROVIDER_ID), JWTClientAuthenticator.PROVIDER_ID, actualProfileRep);

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
        assertExpectedSecureClientAuthEnforceExecutor(List.of(JWTClientAuthenticator.PROVIDER_ID), JWTClientAuthenticator.PROVIDER_ID, actualProfileRep);
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
        assertExpectedClientRolesCondition(List.of(SAMPLE_CLIENT_ROLE), actualPolicyRep);
        assertExpectedClientScopesCondition(ClientScopesConditionFactory.OPTIONAL, List.of(SAMPLE_CLIENT_SCOPE), actualPolicyRep);

        // each policy - lack-of-builtin-field-test-policy
        actualPolicyRep = getPolicyRepresentation(actualPoliciesRep, "lack-of-builtin-field-test-policy");
        assertExpectedPolicy("lack-of-builtin-field-test-policy", "Without builtin field that is treated as builtin=false.", false, List.of("lack-of-builtin-field-test-profile"), actualPolicyRep);

        // each condition
        assertExpectedConditions(Arrays.asList(ClientUpdaterContextConditionFactory.PROVIDER_ID, ClientUpdaterSourceGroupsConditionFactory.PROVIDER_ID, ClientUpdaterSourceHostsConditionFactory.PROVIDER_ID, ClientUpdaterSourceRolesConditionFactory.PROVIDER_ID), actualPolicyRep);
        assertExpectedClientUpdateContextCondition(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER), actualPolicyRep);
        assertExpectedClientUpdateSourceGroupsCondition(List.of("topGroup"), actualPolicyRep);
        assertExpectedClientUpdateSourceHostsCondition(Arrays.asList("localhost", "127.0.0.1"), actualPolicyRep);
        assertExpectedClientUpdateSourceRolesCondition(List.of(AdminRoles.CREATE_CLIENT), actualPolicyRep);
    }


    protected String generateSuffixedName(String name) {
        return name + "-" + UUID.randomUUID().toString().subSequence(0, 7);
    }

    // Utilities for Request Object retrieved by reference from jwks_uri

    protected KeyPair setupJwksUrl(String algorithm, ClientRepresentation clientRepresentation, ClientResource clientResource) throws Exception {
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
        PrivateKey privateKey = decodePrivateKey(Base64.getDecoder().decode(privateKeyBase64), algorithm);
        PublicKey publicKey = decodePublicKey(Base64.getDecoder().decode(publicKeyBase64), algorithm);
        return new KeyPair(publicKey, privateKey);
    }

    private PrivateKey decodePrivateKey(byte[] der, String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
        String keyAlg = getKeyAlgorithmFromJwaAlgorithm(algorithm);
        KeyFactory kf = CryptoIntegration.getProvider().getKeyFactory(keyAlg);
        return kf.generatePrivate(spec);
    }

    private PublicKey decodePublicKey(byte[] der, String algorithm) throws NoSuchAlgorithmException, InvalidKeySpecException, NoSuchProviderException {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
        String keyAlg = getKeyAlgorithmFromJwaAlgorithm(algorithm);
        KeyFactory kf = CryptoIntegration.getProvider().getKeyFactory(keyAlg);
        return kf.generatePublic(spec);
    }

    private String getKeyAlgorithmFromJwaAlgorithm(String jwaAlgorithm) {
        return switch (jwaAlgorithm) {
            case Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.PS256, Algorithm.PS384, Algorithm.PS512 -> KeyType.RSA;
            case Algorithm.ES256, Algorithm.ES384, Algorithm.ES512 -> KeyType.EC;
            case Algorithm.Ed25519, Algorithm.Ed448 -> KeyType.OKP;
            default -> throw new RuntimeException("Unsupported signature algorithm");
        };
    }

   // Signed JWT for client authentication utility

    protected void allowMultipleAudiencesForClientJWTOnServer(boolean allowMultipleAudiences) {
        getTestingClient().testing().setSystemPropertyOnServer("oidc." + OIDCLoginProtocolFactory.CONFIG_OIDC_ALLOW_MULTIPLE_AUDIENCES_FOR_JWT_CLIENT_AUTHENTICATION, String.valueOf(allowMultipleAudiences));
        getTestingClient().testing().reinitializeProviderFactoryWithSystemPropertiesScope(LoginProtocol.class.getName(), OIDCLoginProtocol.LOGIN_PROTOCOL, "oidc.");
    }

    protected String createSignedRequestToken(String clientId, PrivateKey privateKey, PublicKey publicKey, String algorithm) {
        JsonWebToken jwt = createRequestToken(clientId, getRealmInfoUrl());
        String kid = KeyUtils.createKeyId(publicKey);
        SignatureSignerContext signer = SignatureSignerUtil.createSigner(privateKey, kid, algorithm);
        return new JWSBuilder().kid(kid).jsonContent(jwt).sign(signer);
    }

    protected String getRealmInfoUrl() {
        String authServerBaseUrl = UriUtils.getOrigin(oauth.getRedirectUri()) + "/auth";
        return KeycloakUriBuilder.fromUri(authServerBaseUrl).path(ServiceUrlConstants.REALM_INFO_PATH).build(REALM_NAME).toString();
    }

    protected JsonWebToken createRequestToken(String clientId, String realmInfoUrl) {
        JsonWebToken reqToken = new JsonWebToken();
        if (realmInfoUrl != null && !realmInfoUrl.isEmpty()) {
            reqToken.audience(realmInfoUrl);
        }
        return createRequestToken(reqToken, clientId);
    }

    protected JsonWebToken createRequestToken(String clientId, String[] audienceUrls) {
        JsonWebToken reqToken = new JsonWebToken();
        if (audienceUrls != null && audienceUrls.length > 0) {
            reqToken.audience(audienceUrls);
        }
        return createRequestToken(reqToken, clientId);
    }

    private JsonWebToken createRequestToken(JsonWebToken reqToken, String clientId) {
        reqToken.id(KeycloakModelUtils.generateId());
        reqToken.issuer(clientId);
        reqToken.subject(clientId);

        int now = Time.currentTime();
        reqToken.iat((long) now);
        reqToken.exp((long) (now + 10));
        reqToken.nbf((long) now);

        return reqToken;
    }

    // OAuth2 protocol operation with signed JWT for client authentication

    protected AccessTokenResponse doAccessTokenRequestWithSignedJWT(String code, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.AUTHORIZATION_CODE));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CODE, code));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REDIRECT_URI, oauth.getRedirectUri()));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters);
        return new AccessTokenResponse(response);
    }

    protected AccessTokenResponse doRefreshTokenRequestWithSignedJWT(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        CloseableHttpResponse response = sendRequest(oauth.getEndpoints().getToken(), parameters);
        return new AccessTokenResponse(response);
    }

    protected HttpResponse doTokenIntrospectionWithSignedJWT(String tokenType, String tokenToIntrospect, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair("token", tokenToIntrospect));
        parameters.add(new BasicNameValuePair("token_type_hint", tokenType));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getEndpoints().getIntrospection(), parameters);
    }

    protected HttpResponse doTokenRevokeWithSignedJWT(String tokenType, String tokenToIntrospect, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair("token", tokenToIntrospect));
        parameters.add(new BasicNameValuePair("token_type_hint", tokenType));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getEndpoints().getRevocation(), parameters);
    }

    protected HttpResponse doLogoutWithSignedJWT(String refreshToken, String signedJwt) throws Exception {
        List<NameValuePair> parameters = new LinkedList<>();
        parameters.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, OAuth2Constants.REFRESH_TOKEN));
        parameters.add(new BasicNameValuePair(OAuth2Constants.REFRESH_TOKEN, refreshToken));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION_TYPE, OAuth2Constants.CLIENT_ASSERTION_TYPE_JWT));
        parameters.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ASSERTION, signedJwt));

        return sendRequest(oauth.getEndpoints().getLogout(), parameters);
    }

    private CloseableHttpResponse sendRequest(String requestUrl, List<NameValuePair> parameters) throws Exception {
        try (CloseableHttpClient client = new DefaultHttpClient()) {
            HttpPost post = new HttpPost(requestUrl);
            UrlEncodedFormEntity formEntity = new UrlEncodedFormEntity(parameters, StandardCharsets.UTF_8);
            post.setEntity(formEntity);
            return client.execute(post);
        }
    }

    // Request Object utility

    protected AuthorizationEndpointRequestObject createValidRequestObjectForSecureRequestObjectExecutor(String clientId) throws URISyntaxException {
        AuthorizationEndpointRequestObject requestObject = new AuthorizationEndpointRequestObject();
        requestObject.id(KeycloakModelUtils.generateId());
        requestObject.iat((long) Time.currentTime());
        requestObject.exp(requestObject.getIat() + 300L);
        requestObject.nbf(requestObject.getIat());
        requestObject.setClientId(clientId);
        requestObject.setResponseType("code");
        requestObject.setRedirectUriParam(oauth.getRedirectUri());
        requestObject.setScope("openid");
        String state = KeycloakModelUtils.generateId();
        requestObject.setState(state);
        requestObject.setMax_age(600);
        requestObject.setOtherClaims("custom_claim_ein", "rot");
        requestObject.audience(Urls.realmIssuer(new URI(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"), REALM_NAME), "https://example.com");
        requestObject.setNonce(KeycloakModelUtils.generateId());
        return requestObject;
    }

    protected void registerRequestObject(AuthorizationEndpointRequestObject requestObject, String clientId, String sigAlg, boolean isUseRequestUri) throws IOException {
        // Set required signature for request_uri
        // use and set jwks_url
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        assert clientResource != null;
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestObjectSignatureAlg(sigAlg);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
        String jwksUrl = TestApplicationResourceUrls.clientJwksUri();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(jwksUrl);
        clientResource.update(clientRep);

        TestOIDCEndpointsApplicationResource oidcClientEndpointsResource = testingClient.testApp().oidcClientEndpoints();

        // generate and register client keypair
        oidcClientEndpointsResource.generateKeys(sigAlg);

        // register request object
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        oidcClientEndpointsResource.registerOIDCRequest(encodedRequestObject, sigAlg);

        if (isUseRequestUri) {
            requestUri = TestApplicationResourceUrls.clientRequestUri();
            request = null;
        } else {
            request = oidcClientEndpointsResource.getOIDCRequest();
            requestUri = null;
        }
    }

    // OAuth2 protocol operation

    protected void doIntrospectAccessToken(AccessTokenResponse tokenRes, String username, String clientId, String sessionId, String clientSecret) throws IOException {
        TokenMetadataRepresentation rep = oauth.client(clientId, clientSecret).doIntrospectionAccessTokenRequest(tokenRes.getAccessToken()).asTokenMetadata();
        assertTrue(rep.isActive());
        assertEquals(clientId, rep.getClientId());
        assertEquals(clientId, rep.getIssuedFor());
        assertEquals(username, rep.getUserName());
        events.expect(EventType.INTROSPECT_TOKEN).client(clientId).session(sessionId).user(AssertEvents.isUUID()).clearDetails().assertEvent();
    }

    protected void doTokenRevoke(String refreshToken, String clientId, String clientSecret, String userId, String sessionId, boolean isOfflineAccess) {
        oauth.client(clientId, clientSecret);
        oauth.tokenRevocationRequest(refreshToken).refreshToken().send();

        // confirm revocation
        AccessTokenResponse tokenRes = oauth.doRefreshTokenRequest(refreshToken);
        assertEquals(400, tokenRes.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, tokenRes.getError());
        if (isOfflineAccess) assertEquals("Offline user session not found", tokenRes.getErrorDescription());
        else assertEquals("Session not active", tokenRes.getErrorDescription());

        events.expect(EventType.REVOKE_GRANT).clearDetails()
                .client(clientId)
                .user(userId)
                .session(sessionId)
                .assertEvent();
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
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setPostLogoutRedirectUris(Collections.singletonList("+"));
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

    private String getToken(String username, String password) {
        try {
            return oauth.client(Constants.ADMIN_CLI_CLIENT_ID).doPasswordGrantRequest(username, password).getAccessToken();
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

    protected void addProfile(ClientProfileRepresentation profileRep) throws ClientPolicyException {
        ClientProfilesRepresentation reps = getProfilesWithoutGlobals();
        if (reps == null || reps.getProfiles() == null) return;
        reps.getProfiles().add(profileRep);
        updateProfiles(convertToProfilesJson(reps));
    }

    protected void updateProfile(ClientProfileRepresentation profileRep) throws ClientPolicyException {
        if (profileRep == null || profileRep.getName() == null) return;
        String profileName = profileRep.getName();

        ClientProfilesRepresentation reps = getProfilesWithoutGlobals();

        if (reps.getProfiles().stream().anyMatch(i->profileName.equals(i.getName()))) {
            ClientProfileRepresentation rep = reps.getProfiles().stream().filter(i->profileName.equals(i.getName())).toList().get(0);
            reps.getProfiles().remove(rep);
            reps.getProfiles().add(profileRep);
            updateProfiles(convertToProfilesJson(reps));
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

    protected void addPolicy(ClientPolicyRepresentation policyRep) throws ClientPolicyException {
        ClientPoliciesRepresentation reps = getPolicies();
        if (reps == null || reps.getPolicies() == null) return;
        reps.getPolicies().add(policyRep);
        updatePolicies(convertToPoliciesJson(reps));
    }

    protected void updatePolicy(ClientPolicyRepresentation policyRep) throws ClientPolicyException {
        if (policyRep == null || policyRep.getName() == null) return;
        String policyName = policyRep.getName();

        ClientPoliciesRepresentation reps = getPolicies();

        if (reps.getPolicies().stream().anyMatch(i->policyName.equals(i.getName()))) {
            ClientPolicyRepresentation rep = reps.getPolicies().stream().filter(i->policyName.equals(i.getName())).toList().get(0);
            reps.getPolicies().remove(rep);
            reps.getPolicies().add(policyRep);
            updatePolicies(convertToPoliciesJson(reps));
        }
    }

    protected void deletePolicy(String policyName) throws ClientPolicyException {
        if (policyName == null) return;

        ClientPoliciesRepresentation reps = getPolicies();

        if (reps.getPolicies().stream().anyMatch(i->policyName.equals(i.getName()))) {
            ClientPolicyRepresentation rep = reps.getPolicies().stream().filter(i->policyName.equals(i.getName())).toList().get(0);
            reps.getPolicies().remove(rep);
            updatePolicies(convertToPoliciesJson(reps));
        }
    }

    // Assertions about profiles

    // profile

    protected ClientProfileRepresentation getProfileRepresentation(ClientProfilesRepresentation profilesRep, String name, boolean global) {
        Function<ClientProfilesRepresentation, List<ClientProfileRepresentation>> profilesListGetter = global ? ClientProfilesRepresentation::getGlobalProfiles : ClientProfilesRepresentation::getProfiles;
        return getCompoundRepresentation(profilesRep, name, profilesListGetter, ClientProfileRepresentation::getName);
    }

    protected void assertExpectedProfiles(ClientProfilesRepresentation profilesRep, List<String> expectedGlobalProfiles, List<String> expectedRealmProfiles) {
        assertExpectedCompounds(expectedGlobalProfiles, profilesRep, ClientProfilesRepresentation::getGlobalProfiles, ClientProfileRepresentation::getName);
        assertExpectedCompounds(expectedRealmProfiles, profilesRep, ClientProfilesRepresentation::getProfiles, ClientProfileRepresentation::getName);
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
        boolean actualAutoConfigure = actualExecutorConfig.get("auto-configure") != null && actualExecutorConfig.get("auto-configure").asBoolean();
        assertEquals(expectedAutoConfigure, actualAutoConfigure);
    }

    private JsonNode getConfigOfExecutor(String providerId, ClientProfileRepresentation profileRep) {
        ClientPolicyExecutorRepresentation executorRep = profileRep.getExecutors().stream()
                .filter(profileRepp -> providerId.equals(profileRepp.getExecutorProviderId()))
                .findFirst().orElse(null);
        return executorRep == null ? null : executorRep.getConfiguration();
    }

    // Assertions about policies

    // policy

    protected ClientPolicyRepresentation getPolicyRepresentation(ClientPoliciesRepresentation policiesRep, String name) {
        return getCompoundRepresentation(policiesRep, name, ClientPoliciesRepresentation::getPolicies, ClientPolicyRepresentation::getName);
    }

    protected void assertExpectedPolicies(List<String> expectedPolicies, ClientPoliciesRepresentation policiesRep) {
        assertNotNull(policiesRep);
        List<ClientPolicyRepresentation> reps = policiesRep.getPolicies();
        if (reps == null) {
            assertNull(expectedPolicies);
            return;
        }
        Set<String> actualPolicies = reps.stream().map(ClientPolicyRepresentation::getName).collect(Collectors.toSet());
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
        Assert.assertEquals(cfg.getScopes(), scopes);
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

    private <T, R> void assertExpectedCompounds(List<String> expected, R rep, Function<R, List<T>> f, Function<T, String> g) {
        assertNotNull(rep);
        List<T> reps = f.apply(rep);
        if (reps == null) {
            assertNull(expected);
            return;
        }
        Set<String> actual = reps.stream().map(g).collect(Collectors.toSet());
        assertEquals(new HashSet<>(expected), actual);
    }

    // profile/policy common (compound)

    private <T, R> T getCompoundRepresentation(R rep, String name, Function<R, List<T>> f, Function<T, String> g) {
        assertNotNull(rep);
        if (f.apply(rep) == null) return null;
        List<T> reps = f.apply(rep).stream().filter(i->g.apply(i).equals(name)).toList();
        if (reps.size() != 1) return null;
        return reps.get(0);
    }

    private void assertExpectedEmptyConfig(String executorProviderId, ClientProfileRepresentation profileRep) {
        JsonNode config = getConfigOfExecutor(executorProviderId, profileRep);
        assert config != null;
        Assert.assertTrue("Expected empty configuration for provider " + executorProviderId, config.isEmpty());
    }

    protected String signRequestObject(AuthorizationEndpointRequestObject requestObject) throws IOException {
        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        TestOIDCEndpointsApplicationResource client = testingClient.testApp().oidcClientEndpoints();

        // use and set jwks_url
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(oauth.getRealm()), oauth.getClientId());
        assert clientResource != null;
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(TestApplicationResourceUrls.clientJwksUri());
        clientResource.update(clientRep);
        client.generateKeys(Algorithm.PS256);
        client.registerOIDCRequest(encodedRequestObject, Algorithm.PS256);

        // do not send any other parameter but the request request parameter
        return client.getOIDCRequest();
    }

    protected void setAttributeMultivalued(ClientRepresentation clientRep, String attrKey, List<String> attrValues) {
        String attrValueFull = String.join(Constants.CFG_DELIMITER, attrValues);
        clientRep.getAttributes().put(attrKey, attrValueFull);
    }
    
    protected void openVerificationPage(String verificationUri) {
        driver.navigate().to(verificationUri);
    }

    protected void checkMtlsFlow() throws IOException {
        // Check login.
        AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        Assert.assertNull(loginResponse.getError());

        String code = oauth.parseLoginResponse().getCode();

        // Check token obtaining.
        AccessTokenResponse accessTokenResponse;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            accessTokenResponse = oauth.doAccessTokenRequest(code);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        assertEquals(200, accessTokenResponse.getStatusCode());

        // Check token refresh.
        AccessTokenResponse accessTokenResponseRefreshed;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            accessTokenResponseRefreshed = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        assertEquals(200, accessTokenResponseRefreshed.getStatusCode());

        // Check token introspection.
        IntrospectionResponse tokenResponse;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.client(TEST_CLIENT, TEST_CLIENT_SECRET).httpClient().set(client);
            tokenResponse = oauth.doIntrospectionRequest(accessTokenResponse.getAccessToken(), "access_token");
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        Assert.assertNotNull(tokenResponse);
        TokenMetadataRepresentation tokenMetadataRepresentation = tokenResponse.asTokenMetadata();
        Assert.assertTrue(tokenMetadataRepresentation.isActive());

        // Check token revoke.
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            assertTrue(oauth.tokenRevocationRequest(accessTokenResponse.getRefreshToken()).refreshToken().send().isSuccess());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }

        // Check logout.
        LogoutResponse logoutResponse;
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            logoutResponse = oauth.doLogout(accessTokenResponse.getRefreshToken());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        assertTrue(logoutResponse.isSuccess());

        // Check login.
        loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        Assert.assertNull(loginResponse.getError());

        code = oauth.parseLoginResponse().getCode();

        // Check token obtaining without certificate
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            accessTokenResponse = oauth.doAccessTokenRequest(code);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        assertEquals(400, accessTokenResponse.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, accessTokenResponse.getError());

        // Check frontchannel logout and login.
        oauth.openLogoutForm();
        logoutConfirmPage.assertCurrent();
        logoutConfirmPage.confirmLogout();
        loginResponse = oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        Assert.assertNull(loginResponse.getError());

        code = oauth.parseLoginResponse().getCode();

        // Check token obtaining.
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            accessTokenResponse = oauth.doAccessTokenRequest(code);
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        assertEquals(200, accessTokenResponse.getStatusCode());

        // Check token refresh with other certificate
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithOtherKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            accessTokenResponseRefreshed = oauth.doRefreshTokenRequest(accessTokenResponse.getRefreshToken());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        assertEquals(400, accessTokenResponseRefreshed.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, accessTokenResponseRefreshed.getError());

        // Check token revoke with other certificate
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithOtherKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            assertEquals(401, oauth.tokenRevocationRequest(accessTokenResponse.getRefreshToken()).refreshToken().send().getStatusCode());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }

        // Check logout without certificate
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithoutKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            logoutResponse = oauth.doLogout(accessTokenResponse.getRefreshToken());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
        assertEquals(401, logoutResponse.getStatusCode());

        // Check logout.
        try (CloseableHttpClient client = MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore()) {
            oauth.httpClient().set(client);
            oauth.doLogout(accessTokenResponse.getRefreshToken());
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            oauth.httpClient().reset();
        }
    }

    protected void setupPolicyClientIdAndSecretNotAcceptableAuthType(String policyName) throws Exception {
        // register profiles
        String profileName = "MyProfile";
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(profileName, "Primum Profile")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(
                                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID, JWTClientSecretAuthenticator.PROVIDER_ID, X509ClientAuthenticator.PROVIDER_ID),
                                        null))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(policyName, "Primum Consilium", Boolean.TRUE)
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(List.of(ClientUpdaterContextConditionFactory.BY_AUTHENTICATED_USER)))
                        .addProfile(profileName)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

    protected void setupPolicyAuthzCodeFlowUnderMultiPhasePolicy(String policyName) throws Exception {
        // register profiles
        String profileName = "MyProfile";
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile(profileName, "Primul Profil")
                        .addExecutor(SecureClientAuthenticatorExecutorFactory.PROVIDER_ID,
                                createSecureClientAuthenticatorExecutorConfig(
                                        Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientAuthenticator.PROVIDER_ID),
                                        ClientIdAndSecretAuthenticator.PROVIDER_ID))
                        .addExecutor(PKCEEnforcerExecutorFactory.PROVIDER_ID,
                                createPKCEEnforceExecutorConfig(Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy(policyName, "Prima Politica", Boolean.TRUE)
                        .addCondition(ClientRolesConditionFactory.PROVIDER_ID,
                                createClientRolesConditionConfig(List.of(SAMPLE_CLIENT_ROLE)))
                        .addCondition(ClientUpdaterContextConditionFactory.PROVIDER_ID,
                                createClientUpdateContextConditionConfig(List.of(ClientUpdaterContextConditionFactory.BY_INITIAL_ACCESS_TOKEN)))
                        .addProfile(profileName)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

    protected void successfulLoginAndLogout(String clientId, String clientSecret) {
        successfulLoginAndLogout(clientId, clientSecret, null, null);
    }

    protected void successfulLoginAndLogout(String clientId, String clientSecret, String nonce, String state) {
        AccessTokenResponse res = successfulLogin(clientId, clientSecret, nonce, state);
        oauth.doLogout(res.getRefreshToken());
        events.expectLogout(res.getSessionState()).client(clientId).clearDetails().assertEvent();
    }

    protected AccessTokenResponse successfulLogin(String clientId, String clientSecret) {
        return successfulLogin(clientId, clientSecret, null, null);
    }

    protected AccessTokenResponse successfulLogin(String clientId, String clientSecret, String nonce, String state) {
        oauth.client(clientId, clientSecret);
        oauth.loginForm().nonce(nonce).state(state).request(request).requestUri(requestUri).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        return res;
    }

    protected void successfulLoginAndLogoutWithPKCE(String clientId, String clientSecret, String userName, String userPassword) throws Exception {
        oauth.client(clientId, clientSecret);

        pkceGenerator = PkceGenerator.s256();

        oauth.loginForm().nonce("bjapewiziIE083d").codeChallenge(pkceGenerator).doLogin(userName, userPassword);

        EventRepresentation loginEvent = events.expectLogin().client(clientId).assertEvent();
        String sessionId = loginEvent.getSessionId();
        String codeId = loginEvent.getDetails().get(Details.CODE_ID);
        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse res = oauth.accessTokenRequest(code).codeVerifier(pkceGenerator).send();
        assertEquals(200, res.getStatusCode());
        events.expectCodeToToken(codeId, sessionId).client(clientId).assertEvent();

        AccessToken token = oauth.verifyToken(res.getAccessToken());
        String userId = findUserByUsername(adminClient.realm(REALM_NAME), userName).getId();
        assertEquals(userId, token.getSubject());
        // The following check is not valid anymore since file store does have the same ID, and is redundant due to the previous line
        // Assert.assertNotEquals(userName, token.getSubject());
        assertEquals(sessionId, token.getSessionId());
        assertEquals(clientId, token.getIssuedFor());

        String refreshTokenString = res.getRefreshToken();
        RefreshToken refreshToken = oauth.parseRefreshToken(refreshTokenString);
        assertEquals(sessionId, refreshToken.getSessionId());
        assertEquals(clientId, refreshToken.getIssuedFor());

        AccessTokenResponse refreshResponse = oauth.doRefreshTokenRequest(refreshTokenString);
        assertEquals(200, refreshResponse.getStatusCode());
        events.expectRefresh(refreshToken.getId(), sessionId).client(clientId).assertEvent();

        AccessToken refreshedToken = oauth.verifyToken(refreshResponse.getAccessToken());
        RefreshToken refreshedRefreshToken = oauth.parseRefreshToken(refreshResponse.getRefreshToken());
        assertEquals(sessionId, refreshedToken.getSessionId());
        assertEquals(sessionId, refreshedRefreshToken.getSessionId());
        assertEquals(findUserByUsername(adminClient.realm(REALM_NAME), userName).getId(), refreshedToken.getSubject());

        doIntrospectAccessToken(refreshResponse, userName, clientId, sessionId, clientSecret);

        doTokenRevoke(refreshResponse.getRefreshToken(), clientId, clientSecret, userId, sessionId, false);
    }

    protected void failLoginByNotFollowingPKCE(String clientId) {
        oauth.client(clientId, TEST_CLIENT_SECRET);
        oauth.openLoginForm();
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("Missing parameter: code_challenge_method", response.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                "Missing parameter: code_challenge_method").client(clientId).user((String) null)
                .assertEvent();
    }

    protected void failLoginByNotFollowingPKCEWithoutClientPolicyValidation(String clientId) {
        oauth.client(clientId, TEST_CLIENT_SECRET);
        oauth.openLoginForm();
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("Missing parameter: code_challenge_method", response.getErrorDescription());
        events.expect(EventType.LOGIN_ERROR).error(OAuthErrorException.INVALID_REQUEST)
                .detail(Details.REASON, "Missing parameter: code_challenge_method").client(clientId)
                .user((String) null).assertEvent();
    }

    protected void failLoginWithoutSecureSessionParameter(String clientId, String errorDescription) {
        oauth.client(clientId, TEST_CLIENT_SECRET);
        oauth.openLoginForm();
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals(errorDescription, response.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST, errorDescription).client(clientId)
                .user((String) null).assertEvent();
    }

    protected void failLoginWithoutNonce(String clientId) {
        oauth.client(clientId);
        oauth.openLoginForm();
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals(ERR_MSG_MISSING_NONCE, response.getErrorDescription());
    }

    protected void doConfigProfileAndPolicy(ClientPoliciesUtil.ClientProfileBuilder profileBuilder,
                                          ClientSecretRotationExecutor.Configuration profileConfig) throws Exception {
        String json = (new ClientPoliciesUtil.ClientProfilesBuilder()).addProfile(
                profileBuilder.createProfile(SECRET_ROTATION_PROFILE, "Enable Client Secret Rotation")
                        .addExecutor(ClientSecretRotationExecutorFactory.PROVIDER_ID, profileConfig)
                        .toRepresentation()).toString();
        updateProfiles(json);

        // register policies
        ClientAccessTypeCondition.Configuration config = new ClientAccessTypeCondition.Configuration();
        config.setType(List.of(ClientAccessTypeConditionFactory.TYPE_CONFIDENTIAL));
        json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy(SECRET_ROTATION_POLICY,
                                "Policy for Client Secret Rotation",
                                Boolean.TRUE).addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, config)
                        .addProfile(SECRET_ROTATION_PROFILE).toRepresentation()).toString();
        updatePolicies(json);
    }

    protected void configureCustomProfileAndPolicy(int secretExpiration, int rotatedExpiration,
                                                 int remainingExpiration) throws Exception {
        ClientPoliciesUtil.ClientProfileBuilder profileBuilder = new ClientPoliciesUtil.ClientProfileBuilder();
        ClientSecretRotationExecutor.Configuration profileConfig = getClientProfileConfiguration(
                secretExpiration, rotatedExpiration, remainingExpiration);

        doConfigProfileAndPolicy(profileBuilder, profileConfig);
    }

    @NotNull
    protected ClientSecretRotationExecutor.Configuration getClientProfileConfiguration(
            int expirationPeriod, int rotatedExpirationPeriod, int remainExpirationPeriod) {
        ClientSecretRotationExecutor.Configuration profileConfig = new ClientSecretRotationExecutor.Configuration();
        profileConfig.setExpirationPeriod(expirationPeriod);
        profileConfig.setRotatedExpirationPeriod(rotatedExpirationPeriod);
        profileConfig.setRemainExpirationPeriod(remainExpirationPeriod);
        return profileConfig;
    }

    protected void assertLoginAndLogoutStatus(String clientId, String secret, Response.Status status) {
        oauth.client(clientId, secret);
        AuthorizationEndpointResponse loginResponse = oauth.doLogin(TEST_USER_NAME,
                TEST_USER_PASSWORD);
        String code = loginResponse.getCode();
        AccessTokenResponse res = oauth.doAccessTokenRequest(code);
        assertThat(res.getStatusCode(), equalTo(status.getStatusCode()));
        oauth.doLogout(res.getRefreshToken());
    }
}
