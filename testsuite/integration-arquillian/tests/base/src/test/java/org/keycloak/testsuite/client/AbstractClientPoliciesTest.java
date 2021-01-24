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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.InvalidAlgorithmParameterException;
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
import java.util.ArrayList;
import java.util.Arrays;
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

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.adapters.AdapterUtils;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.Base64;
import org.keycloak.common.util.Base64Url;
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
import org.keycloak.representations.idm.ClientPolicyRepresentation;
import org.keycloak.representations.idm.ClientProfileRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientCondition;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeCondition;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientRolesCondition;
import org.keycloak.services.clientpolicy.condition.ClientRolesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientScopesCondition;
import org.keycloak.services.clientpolicy.condition.ClientScopesConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateContextCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdateContextConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceGroupsCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceGroupsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceHostsCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceHostsConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceRolesCondition;
import org.keycloak.services.clientpolicy.condition.ClientUpdateSourceRolesConditionFactory;
import org.keycloak.services.clientpolicy.executor.HolderOfKeyEnforceExecutor;
import org.keycloak.services.clientpolicy.executor.HolderOfKeyEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.PKCEEnforceExecutor;
import org.keycloak.services.clientpolicy.executor.PKCEEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthEnforceExecutor;
import org.keycloak.services.clientpolicy.executor.SecureClientAuthEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUriEnforceExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRedirectUriEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutor;
import org.keycloak.services.clientpolicy.executor.SecureRequestObjectExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutor;
import org.keycloak.services.clientpolicy.executor.SecureResponseTypeExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutor;
import org.keycloak.services.clientpolicy.executor.SecureSessionEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmEnforceExecutor;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmEnforceExecutorFactory;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtEnforceExecutor;
import org.keycloak.services.clientpolicy.executor.SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject;
import org.keycloak.testsuite.services.clientpolicy.condition.TestRaiseExeptionCondition;
import org.keycloak.testsuite.services.clientpolicy.executor.TestRaiseExeptionExecutor;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class AbstractClientPoliciesTest extends AbstractKeycloakTest {

    protected static final Logger logger = Logger.getLogger(AbstractClientPoliciesTest.class);

    protected static final String REALM_NAME = "test";
    protected static final String TEST_CLIENT = "test-app";
    protected static final String TEST_CLIENT_SECRET = "password";

    protected static final String POLICY_NAME = "MyPolicy";
    protected static final String PROFILE_NAME = "MyProfile";
    protected static final String SAMPLE_CLIENT_ROLE = "sample-client-role";

    protected static final String ERR_MSG_MISSING_NONCE = "Missing parameter: nonce";
    protected static final String ERR_MSG_MISSING_STATE = "Missing parameter: state";
    protected static final String ERR_MSG_CLIENT_REG_FAIL = "Failed to send request";

    protected ClientRegistration reg;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Before
    public void before() throws Exception {
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

    protected void loadValidProfilesAndPolicies() throws Exception {
        // load profiles
        ClientProfileRepresentation notLoadedOverrideExistingBuiltinProfile = (new ClientProfileBuilder()).createProfile("builtin-basic-security", "Enforce basic security level", Boolean.TRUE, null)
                    .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                        createSecureClientAuthEnforceExecutorConfig(
                            Boolean.FALSE, 
                            Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientAuthenticator.PROVIDER_ID),
                            null))
                    .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                        createPKCEEnforceExecutorConfig(Boolean.FALSE))
                    .addExecutor("no-such-executor", 
                            createPKCEEnforceExecutorConfig(Boolean.TRUE))
                    .toRepresentation();

        ClientProfileRepresentation notLoadedNewBuiltinProfileRep = (new ClientProfileBuilder()).createProfile("builtin-advanced-security", "Enforce advanced security level", Boolean.TRUE, null)
                .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                    createSecureClientAuthEnforceExecutorConfig(
                        Boolean.TRUE, 
                        Arrays.asList(ClientIdAndSecretAuthenticator.PROVIDER_ID, JWTClientAuthenticator.PROVIDER_ID),
                        JWTClientAuthenticator.PROVIDER_ID))
                .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                    createPKCEEnforceExecutorConfig(Boolean.TRUE))
                .addExecutor("no-such-executor", null)
                .toRepresentation();

        ClientProfileRepresentation notLoadedProfileRepWithoutName = (new ClientProfileBuilder()).createProfile(null, "No name profile that should be skipped.", Boolean.FALSE, null)
                .addExecutor(PKCEEnforceExecutorFactory.PROVIDER_ID, 
                    createPKCEEnforceExecutorConfig(Boolean.TRUE))
                .toRepresentation();

        ClientProfileRepresentation loadedProfileRep = (new ClientProfileBuilder()).createProfile("ordinal-test-profile", "The profile that can be loaded.", Boolean.FALSE, null)
                .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                    createSecureClientAuthEnforceExecutorConfig(
                        Boolean.TRUE, 
                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID),
                        JWTClientAuthenticator.PROVIDER_ID))
                .toRepresentation();

        ClientProfileRepresentation loadedProfileRepWithoutBuiltinField = (new ClientProfileBuilder()).createProfile("lack-of-builtin-field-test-profile", "Without builtin field that is treated as builtin=false.", null, null)
                .addExecutor(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, 
                    createSecureClientAuthEnforceExecutorConfig(
                        Boolean.TRUE, 
                        Arrays.asList(JWTClientAuthenticator.PROVIDER_ID),
                        JWTClientAuthenticator.PROVIDER_ID))
                .addExecutor(HolderOfKeyEnforceExecutorFactory.PROVIDER_ID, 
                    createHolderOfKeyEnforceExecutorConfig(Boolean.TRUE))
                .addExecutor(SecureRedirectUriEnforceExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureRequestObjectExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureResponseTypeExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureSessionEnforceExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureSigningAlgorithmEnforceExecutorFactory.PROVIDER_ID, null)
                .addExecutor(SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory.PROVIDER_ID, null)
                .toRepresentation();

        String json = (new ClientProfilesBuilder())
                .addProfile(notLoadedOverrideExistingBuiltinProfile)
                .addProfile(notLoadedNewBuiltinProfileRep)
                .addProfile(notLoadedProfileRepWithoutName)
                .addProfile(loadedProfileRep)
                .addProfile(loadedProfileRepWithoutBuiltinField)
                .toString();
        updateProfiles(json);

        // load policies
        ClientPolicyRepresentation loadedOverrideExistingBuiltinPolicy = 
                (new ClientPolicyBuilder()).createPolicy(
                        "builtin-default-policy",
                        "modified description is ignored.",
                        Boolean.TRUE,
                        Boolean.TRUE,
                        null,
                        Arrays.asList("builtin-default-profile", "ordinal-test-profile", "lack-of-builtin-field-test-profile"))
                    .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                        createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                    .toRepresentation();

        ClientPolicyRepresentation notLoadedNewBuiltinPolicyRep = 
                (new ClientPolicyBuilder()).createPolicy(
                        "builtin-new-policy",
                        "builtin new policy is ignored.",
                        Boolean.TRUE,
                        Boolean.TRUE,
                        null,
                        Arrays.asList("builtin-default-profile"))
                    .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                        createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                    .toRepresentation();

        ClientPolicyRepresentation loadedPolicyRepNotExistAndDuplicatedProfile = 
                (new ClientPolicyBuilder()).createPolicy(
                        "new-policy",
                        "not existed and duplicated profiles are ignored.",
                        Boolean.FALSE,
                        Boolean.TRUE,
                        null,
                        Arrays.asList("no-such-profile", "builtin-default-profile", "ordinal-test-profile", "lack-of-builtin-field-test-profile", "ordinal-test-profile"))
                    .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID, 
                        createClientAccessTypeConditionConfig(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_PUBLIC, ClientAccessTypeConditionFactory.TYPE_BEARERONLY)))
                    .addCondition(ClientRolesConditionFactory.PROVIDER_ID, 
                            createClientRolesConditionConfig(Arrays.asList(SAMPLE_CLIENT_ROLE)))
                    .addCondition(ClientScopesConditionFactory.PROVIDER_ID, 
                            createClientScopesConditionConfig(ClientScopesConditionFactory.OPTIONAL, Arrays.asList(SAMPLE_CLIENT_ROLE)))
                    .toRepresentation();

        ClientPolicyRepresentation loadedPolicyRepWithoutBuiltinField = 
                (new ClientPolicyBuilder()).createPolicy(
                        "lack-of-builtin-field-test-policy",
                        "Without builtin field that is treated as builtin=false.",
                        null,
                        null,
                        null,
                        Arrays.asList("lack-of-builtin-field-test-profile"))
                    .addCondition(ClientUpdateContextConditionFactory.PROVIDER_ID, 
                            createClientUpdateContextConditionConfig(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER)))
                    .addCondition(ClientUpdateSourceGroupsConditionFactory.PROVIDER_ID, 
                            createClientUpdateSourceGroupsConditionConfig(Arrays.asList("topGroup")))
                    .addCondition(ClientUpdateSourceHostsConditionFactory.PROVIDER_ID, 
                            createClientUpdateSourceHostsConditionConfig(Arrays.asList("localhost", "127.0.0.1"), Arrays.asList(Boolean.TRUE, Boolean.TRUE)))
                    .addCondition(ClientUpdateSourceRolesConditionFactory.PROVIDER_ID, 
                            createClientUpdateSourceRolesConditionConfig(Arrays.asList(AdminRoles.CREATE_CLIENT)))
                    .toRepresentation();

        json = (new ClientPoliciesBuilder())
                    .addPolicy(loadedOverrideExistingBuiltinPolicy)
                    .addPolicy(notLoadedNewBuiltinPolicyRep)
                    .addPolicy(loadedPolicyRepNotExistAndDuplicatedProfile)
                    .addPolicy(loadedPolicyRepWithoutBuiltinField)
                    .toString();
        updatePolicies(json);

    }


    protected void assertExpectedLoadedProfiles(Consumer<ClientProfilesRepresentation> modifiedAssertion) {

        // retrieve loaded builtin profiles
        ClientProfilesRepresentation actualProfilesRep = getProfiles();

        // same profiles
        assertExpectedProfiles(actualProfilesRep, Arrays.asList("builtin-default-profile", "ordinal-test-profile", "lack-of-builtin-field-test-profile"));

        // each profile - builtin-default-profile
        ClientProfileRepresentation actualProfileRep =  getProfileRepresentation(actualProfilesRep, "builtin-default-profile");
        assertExpectedProfile(actualProfileRep, "builtin-default-profile", "The built-in default profile for enforcing basic security level to clients.", true);

        // each executor
        assertExpectedExecutors(Arrays.asList(SecureSessionEnforceExecutorFactory.PROVIDER_ID), actualProfileRep);
        assertExpectedSecureSessionEnforceExecutor(actualProfileRep);

        // each profile - ordinal-test-profile - updated
        actualProfileRep =  getProfileRepresentation(actualProfilesRep, "ordinal-test-profile");
        modifiedAssertion.accept(actualProfilesRep);

        // each executor
        assertExpectedExecutors(Arrays.asList(SecureClientAuthEnforceExecutorFactory.PROVIDER_ID), actualProfileRep);
        assertExpectedSecureClientAuthEnforceExecutor(Arrays.asList(JWTClientAuthenticator.PROVIDER_ID), true, JWTClientAuthenticator.PROVIDER_ID, actualProfileRep);

        // each profile - lack-of-builtin-field-test-profile
        actualProfileRep =  getProfileRepresentation(actualProfilesRep, "lack-of-builtin-field-test-profile");
        assertExpectedProfile(actualProfileRep, "lack-of-builtin-field-test-profile", "Without builtin field that is treated as builtin=false.", false);

        // each executor
        assertExpectedExecutors(Arrays.asList(
                SecureClientAuthEnforceExecutorFactory.PROVIDER_ID,
                HolderOfKeyEnforceExecutorFactory.PROVIDER_ID,
                SecureRedirectUriEnforceExecutorFactory.PROVIDER_ID,
                SecureRequestObjectExecutorFactory.PROVIDER_ID,
                SecureResponseTypeExecutorFactory.PROVIDER_ID,
                SecureSessionEnforceExecutorFactory.PROVIDER_ID,
                SecureSigningAlgorithmEnforceExecutorFactory.PROVIDER_ID,
                SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory.PROVIDER_ID), actualProfileRep);
        assertExpectedSecureClientAuthEnforceExecutor(Arrays.asList(JWTClientAuthenticator.PROVIDER_ID), true, JWTClientAuthenticator.PROVIDER_ID, actualProfileRep);
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
        assertExpectedPolicies(Arrays.asList("builtin-default-policy", "new-policy", "lack-of-builtin-field-test-policy"), actualPoliciesRep);

        // each policy - builtin-default-policy
        ClientPolicyRepresentation actualPolicyRep = getPolicyRepresentation(actualPoliciesRep, "builtin-default-policy");
        assertExpectedPolicy("builtin-default-policy", "The built-in default policy applied to all clients.", true, true, Arrays.asList("builtin-default-profile"), actualPolicyRep);

        // each condition
        assertExpectedConditions(Arrays.asList(AnyClientConditionFactory.PROVIDER_ID), actualPolicyRep);
        assertExpectedAnyClientCondition(actualPolicyRep);

        // each policy - new-policy - updated
        actualPolicyRep =  getPolicyRepresentation(actualPoliciesRep, "new-policy");
        modifiedAssertion.accept(actualPoliciesRep);

        // each condition
        assertExpectedConditions(Arrays.asList(ClientAccessTypeConditionFactory.PROVIDER_ID, ClientRolesConditionFactory.PROVIDER_ID, ClientScopesConditionFactory.PROVIDER_ID), actualPolicyRep);
        assertExpectedClientAccessTypeCondition(Arrays.asList(ClientAccessTypeConditionFactory.TYPE_PUBLIC, ClientAccessTypeConditionFactory.TYPE_BEARERONLY), actualPolicyRep);
        assertExpectedClientRolesCondition(Arrays.asList(SAMPLE_CLIENT_ROLE), actualPolicyRep);
        assertExpectedClientScopesCondition(ClientScopesConditionFactory.OPTIONAL, Arrays.asList(SAMPLE_CLIENT_ROLE), actualPolicyRep);

        // each policy - lack-of-builtin-field-test-policy
        actualPolicyRep = getPolicyRepresentation(actualPoliciesRep, "lack-of-builtin-field-test-policy");
        assertExpectedPolicy("lack-of-builtin-field-test-policy", "Without builtin field that is treated as builtin=false.", false, false, Arrays.asList("lack-of-builtin-field-test-profile"), actualPolicyRep);

        // each condition
        assertExpectedConditions(Arrays.asList(ClientUpdateContextConditionFactory.PROVIDER_ID, ClientUpdateSourceGroupsConditionFactory.PROVIDER_ID, ClientUpdateSourceHostsConditionFactory.PROVIDER_ID, ClientUpdateSourceRolesConditionFactory.PROVIDER_ID), actualPolicyRep);
        assertExpectedClientUpdateContextCondition(Arrays.asList(ClientUpdateContextConditionFactory.BY_AUTHENTICATED_USER), actualPolicyRep);
        assertExpectedClientUpdateSourceGroupsCondition(Arrays.asList("topGroup"), actualPolicyRep);
        assertExpectedClientUpdateSourceHostsCondition(Arrays.asList("localhost", "127.0.0.1"), Arrays.asList(Boolean.TRUE, Boolean.TRUE), actualPolicyRep);
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

    private KeyPair getKeyPairFromGeneratedBase64(Map<String, String> generatedKeys, String algorithm) throws Exception {
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

    protected String createSignedRequestToken(String clientId, PrivateKey privateKey, PublicKey publicKey, String algorithm) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
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
        requestObject.nbf(Long.valueOf(0));
        requestObject.setClientId(clientId);
        requestObject.setResponseType("code");
        requestObject.setRedirectUriParam(oauth.getRedirectUri());
        requestObject.setScope("openid");
        String scope = KeycloakModelUtils.generateId();
        oauth.stateParamHardcoded(scope);
        requestObject.setState(scope);
        requestObject.setMax_age(Integer.valueOf(600));
        requestObject.setOtherClaims("custom_claim_ein", "rot");
        requestObject.audience(Urls.realmIssuer(new URI(suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth"), REALM_NAME), "https://example.com");
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
        ObjectMapper objectMapper = new ObjectMapper();
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

    // Client Profiles CRUD Operations

    protected static class ClientProfilesBuilder {
        private final ClientProfilesRepresentation profilesRep;

        public ClientProfilesBuilder() {
            profilesRep = new ClientProfilesRepresentation();
            profilesRep.setProfiles(new ArrayList<>());
        }

        public ClientProfilesBuilder addProfile(ClientProfileRepresentation rep) {
            profilesRep.getProfiles().add(rep);
            return this;
        }

        public ClientProfilesRepresentation toRepresentation() {
            return profilesRep;
        }

        public String toString() {
            ObjectMapper mapper = new ObjectMapper();
            String profilesJson = null;
            try {
                profilesJson = mapper.writeValueAsString(profilesRep);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                fail();
            }
            return profilesJson;
        }
    }

    protected static class ClientProfileBuilder {

        private final ClientProfileRepresentation profileRep;

        public ClientProfileBuilder() {
            profileRep = new ClientProfileRepresentation();
        }

        public ClientProfileBuilder createProfile(String name, String description, Boolean isBuiltin, List<Object> executors) {
            if (name != null) {
                profileRep.setName(name);
            }
            if (description != null) {
                profileRep.setDescription(description);
            }
            if (isBuiltin != null) {
                profileRep.setBuiltin(isBuiltin);
            } else {
                profileRep.setBuiltin(Boolean.FALSE);
            }
            if (executors != null) {
                profileRep.setExecutors(executors);
            } else {
                profileRep.setExecutors(new ArrayList<>());
            }
            return this;
        }

        public ClientProfileBuilder addExecutor(String providerId, Object config) {
            ObjectMapper mapper = new ObjectMapper();
            String configString = null;
            if (config == null) {
                configString = "{}";
            } else {
                try {
                    configString = mapper.writeValueAsString(config);
                } catch (JsonProcessingException e) {
                    fail();
                }
            }
            String executorJson = (new StringBuilder())
                    .append("{\"")
                    .append(providerId)
                    .append("\":")
                    .append(configString)
                    .append("}")
                    .toString();
            JsonNode node = null;
            try {
                node = mapper.readTree(executorJson);
            } catch (JsonProcessingException e) {
                fail();
            }
            profileRep.getExecutors().add(node);
            return this;
        }

        public ClientProfileRepresentation toRepresentation() {
            return profileRep;
        }

        public String toString() {
            ObjectMapper mapper = new ObjectMapper();
            String profileJson = null;
            try {
                profileJson = mapper.writeValueAsString(profileRep);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                fail();
            }
            return profileJson;
        }
    }

    // Client Profiles - Executor CRUD Operations

    protected Object createHolderOfKeyEnforceExecutorConfig(Boolean isArgument) {
        HolderOfKeyEnforceExecutor.Configuration config = new HolderOfKeyEnforceExecutor.Configuration();
        config.setAugment(isArgument);
        return config;
    }

    protected Object createPKCEEnforceExecutorConfig(Boolean isArgument) {
        PKCEEnforceExecutor.Configuration config = new PKCEEnforceExecutor.Configuration();
        config.setAugment(isArgument);
        return config;
    }

    protected Object createSecureClientAuthEnforceExecutorConfig(Boolean isArgument, List<String> clientAuthns, String clientAuthnsAugment) {
        SecureClientAuthEnforceExecutor.Configuration config = new SecureClientAuthEnforceExecutor.Configuration();
        config.setAugment(isArgument);
        config.setClientAuthns(clientAuthns);
        config.setClientAuthnsAugment(clientAuthnsAugment);
        return config;
    }

    protected Object createSecureRequestObjectExecutorConfig() {
        return new SecureRequestObjectExecutor.Configuration();
    }

    protected Object createSecureResponseTypeExecutorConfig() {
        return new SecureResponseTypeExecutor.Configuration();
    }

    protected Object createSecureRedirectUriEnforceExecutorConfig() {
        return new SecureRedirectUriEnforceExecutor.Configuration();
    }

    protected Object createSecureSessionEnforceExecutorConfig() {
        return new SecureSessionEnforceExecutor.Configuration();
    }

    protected Object createSecureSigningAlgorithmEnforceExecutorConfig() {
        return new SecureSigningAlgorithmEnforceExecutor.Configuration();
    }

    protected Object createSecureSigningAlgorithmForSignedJwtEnforceExecutorConfig() {
        return new SecureSigningAlgorithmForSignedJwtEnforceExecutor.Configuration();
    }

    // Client Policies CRUD Operation

    protected static class ClientPoliciesBuilder {
        private final ClientPoliciesRepresentation policiesRep;

        public ClientPoliciesBuilder() {
            policiesRep = new ClientPoliciesRepresentation();
            policiesRep.setPolicies(new ArrayList<>());
        }

        public ClientPoliciesBuilder addPolicy(ClientPolicyRepresentation rep) {
            policiesRep.getPolicies().add(rep);
            return this;
        }

        public ClientPoliciesRepresentation toRepresentation() {
            return policiesRep;
        }

        public String toString() {
            ObjectMapper mapper = new ObjectMapper();
            String policiesJson = null;
            try {
                policiesJson = mapper.writeValueAsString(policiesRep);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                fail();
            }
            return policiesJson;
        }
    }

    protected static class ClientPolicyBuilder {

        private final ClientPolicyRepresentation policyRep;

        public ClientPolicyBuilder() {
            policyRep = new ClientPolicyRepresentation();
        }

        public ClientPolicyBuilder createPolicy(String name, String description, Boolean isBuiltin, Boolean isEnabled, List<Object> conditions, List<String> profiles) {
            policyRep.setName(name);
            if (description != null) {
                policyRep.setDescription(description);
            }
            if (isBuiltin != null) {
                policyRep.setBuiltin(isBuiltin);
            } else {
                policyRep.setBuiltin(Boolean.FALSE);
            }
            if (isEnabled != null) {
                policyRep.setEnable(isEnabled);
            } else {
                policyRep.setEnable(Boolean.FALSE);
            }
            if (conditions != null) {
                policyRep.setConditions(conditions);
            } else {
                policyRep.setConditions(new ArrayList<>());
            }
            if (profiles != null) {
                policyRep.setProfiles(profiles);
            } else {
                policyRep.setProfiles(new ArrayList<>());
            }
            return this;
        }

        public ClientPolicyBuilder addCondition(String providerId, Object config) {
            ObjectMapper mapper = new ObjectMapper();
            String configString = null;
            if (config == null) {
                configString = "{}";
            } else {
                try {
                    configString = mapper.writeValueAsString(config);
                } catch (JsonProcessingException e) {
                    fail();
                }
            }
            String conditionJson = (new StringBuilder())
                    .append("{\"")
                    .append(providerId)
                    .append("\":")
                    .append(configString)
                    .append("}")
                    .toString();
            JsonNode node = null;
            try {
                node = mapper.readTree(conditionJson);
            } catch (JsonProcessingException e) {
                fail();
            }
            policyRep.getConditions().add(node);
            return this;
        }

        public ClientPolicyBuilder addProfile(String profileName) {
            policyRep.getProfiles().add(profileName);
            return this;
        }

        public ClientPolicyRepresentation toRepresentation() {
            return policyRep;
        }

        public String toString() {
            ObjectMapper mapper = new ObjectMapper();
            String policyJson = null;
            try {
                policyJson = mapper.writeValueAsString(policyRep);
            } catch (JsonProcessingException e) {
                fail();
            }
            return policyJson;
        }
    }

    // Client Policies - Condition CRUD Operations

    protected Object createTestRaiseExeptionConditionConfig() {
        return new TestRaiseExeptionCondition.Configuration();
    }

    protected Object createAnyClientConditionConfig() {
        return new AnyClientCondition.Configuration();
    }

    protected Object createAnyClientConditionConfig(Boolean isNegativeLogic) {
        AnyClientCondition.Configuration config = new AnyClientCondition.Configuration();
        config.setNegativeLogic(isNegativeLogic);
        return config;
    }

    protected Object createClientAccessTypeConditionConfig(List<String> types) {
        ClientAccessTypeCondition.Configuration config = new ClientAccessTypeCondition.Configuration();
        config.setType(types);
        return config;
    }

    protected Object createClientRolesConditionConfig(List<String> roles) {
        ClientRolesCondition.Configuration config = new ClientRolesCondition.Configuration();
        config.setRoles(roles);
        return config;
    }

    protected Object createClientScopesConditionConfig(String type, List<String> scopes) {
        ClientScopesCondition.Configuration config = new ClientScopesCondition.Configuration();
        config.setType(type);
        config.setScope(scopes);
        return config;
    }

    protected Object createClientUpdateContextConditionConfig(List<String> updateClientSource) {
        ClientUpdateContextCondition.Configuration config = new ClientUpdateContextCondition.Configuration();
        config.setUpdateClientSource(updateClientSource);
        return config;
    }

    protected Object createClientUpdateSourceGroupsConditionConfig(List<String> groups) {
        ClientUpdateSourceGroupsCondition.Configuration config = new ClientUpdateSourceGroupsCondition.Configuration();
        config.setGroups(groups);
        return config;
    }

    protected Object createClientUpdateSourceHostsConditionConfig(List<String> trustedHosts, List<Boolean> hostSendingRequestMustMatch) {
        ClientUpdateSourceHostsCondition.Configuration config = new ClientUpdateSourceHostsCondition.Configuration();
        config.setTrustedHosts(trustedHosts);
        config.setHostSendingRequestMustMatch(hostSendingRequestMustMatch);
        return config;
    }

    protected Object createClientUpdateSourceRolesConditionConfig(List<String> roles) {
        ClientUpdateSourceRolesCondition.Configuration config = new ClientUpdateSourceRolesCondition.Configuration();
        config.setRoles(roles);
        return config;
    }

    // Profiles Operation

    protected String convertToProfilesJson(ClientProfilesRepresentation reps) {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(reps);
        } catch (JsonProcessingException e) {
            fail();
        }
        return json;
    }

    protected ClientProfilesRepresentation convertToProfiles(String json) {
        ClientProfilesRepresentation reps = null;
        try {
            reps = JsonSerialization.readValue(json, ClientProfilesRepresentation.class);
        } catch (IOException e) {
            fail();
        }
        return reps;
    }

    protected String getProfilesJson() {
        return adminClient.realm(REALM_NAME).clientPoliciesProfilesResource().getProfiles();
    }

    protected void updateProfiles(String json) throws ClientPolicyException {
        Response resp = adminClient.realm(REALM_NAME).clientPoliciesProfilesResource().updateProfiles(json);
        if (resp.getStatus() != 204) {
            throw new ClientPolicyException("update profiles failed", resp.getStatusInfo().toString());
        }
    }

    protected void updateProfiles(ClientProfilesRepresentation reps) throws ClientPolicyException {
        updateProfiles(convertToProfilesJson(reps));
    }

    protected void revertToBuiltinProfiles() throws ClientPolicyException {
        updateProfiles("{}");
    }

    protected ClientProfilesRepresentation getProfiles() {
        return convertToProfiles(getProfilesJson());
    }

    protected String convertToProfileJson(ClientProfileRepresentation rep) {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(rep);
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

        ClientProfilesRepresentation reps = getProfiles();
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
        ClientProfilesRepresentation reps = getProfiles();
        if (reps == null || reps.getProfiles() == null) return;
        reps.getProfiles().add(profileRep);
        updateProfiles(convertToProfilesJson(reps));
        return;
    }

    protected void updateProfile(ClientProfileRepresentation profileRep) throws ClientPolicyException {
        if (profileRep == null || profileRep.getName() == null) return;
        String profileName = profileRep.getName();

        ClientProfilesRepresentation reps = getProfiles();

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

        ClientProfilesRepresentation reps = getProfiles();

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
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(reps);
        } catch (JsonProcessingException e) {
            fail();
        }
        return json;
    }

    protected ClientPoliciesRepresentation convertToPolicies(String json) {
        ClientPoliciesRepresentation reps = null;
        try {
            reps = JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
        } catch (IOException e) {
            fail();
        }
        return reps;
    }

    protected String getPoliciesJson() {
        return adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource().getPolicies();
    }

    protected void updatePolicies(String json) throws ClientPolicyException {
        Response resp = adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource().updatePolicies(json);
        if (resp.getStatus() != 204) {
            throw new ClientPolicyException("update profiles failed", resp.getStatusInfo().toString());
        }
    }

    protected void updatePolicies(ClientPoliciesRepresentation reps) throws ClientPolicyException {
        updatePolicies(convertToPoliciesJson(reps));
    }

    protected void revertToBuiltinPolicies() throws ClientPolicyException {
        updatePolicies("{}");
    }

    protected ClientPoliciesRepresentation getPolicies() {
        return convertToPolicies(getPoliciesJson());
    }

    protected String convertToPolicyJson(ClientPolicyRepresentation rep) {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(rep);
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

    protected ClientProfileRepresentation getProfileRepresentation(ClientProfilesRepresentation profilesRep, String name) {
        return getCompoundRepresentation(profilesRep, name, (ClientProfilesRepresentation i)->i.getProfiles(), (ClientProfileRepresentation i)->i.getName());
    }

    protected void assertExpectedProfiles(ClientProfilesRepresentation profilesRep, List<String> expectedProfiles) {
        assertExpetedCompounds(expectedProfiles, profilesRep, (ClientProfilesRepresentation i)->i.getProfiles(), (ClientProfileRepresentation i)->i.getName());
    }

    protected void assertExpectedProfile(ClientProfileRepresentation actualProfileRep, String name, String description, boolean isBuiltin) {
        assertNotNull(actualProfileRep);
        assertEquals(description, actualProfileRep.getDescription());
        assertEquals(isBuiltin, actualProfileRep.isBuiltin());
    }

    // executors

    protected void assertExpectedExecutors(List<String> expectedExecutors, ClientProfileRepresentation profileRep) {
        assertExpetedElement(expectedExecutors, profileRep, (ClientProfileRepresentation i)->i.getExecutors());
    }

    protected void assertExpectedHolderOfKeyEnforceExecutor(boolean isAugment, ClientProfileRepresentation profileRep) {
        assertExpectedAugmenedExecutor(isAugment, HolderOfKeyEnforceExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedPKCEEnforceExecutor(boolean isAugment, ClientProfileRepresentation profileRep) {
        assertExpectedAugmenedExecutor(isAugment, PKCEEnforceExecutorFactory.PROVIDER_ID, profileRep);
    }

    protected void assertExpectedSecureClientAuthEnforceExecutor(List<String> clientAuthns, boolean isAugment, String clientAuthnsAugment, ClientProfileRepresentation profileRep) {
        JsonNode actualExecutorConfig = assertExpectedAugmenedExecutor(isAugment, SecureClientAuthEnforceExecutorFactory.PROVIDER_ID, profileRep);

        Set<String> actualClientAuthns = new HashSet<>();
        if (actualExecutorConfig.findValue("client-authns") != null) actualExecutorConfig.findValue("client-authns").elements().forEachRemaining(i->actualClientAuthns.add(i.asText()));
        assertEquals(new HashSet<>(clientAuthns), actualClientAuthns);

        String actualClientAuthnAugment = null;
        if (actualExecutorConfig.findValue("client-authns-augment") != null) actualClientAuthnAugment = actualExecutorConfig.findValue("client-authns-augment").asText();
        assertEquals(clientAuthnsAugment, actualClientAuthnAugment);
    }

    protected void assertExpectedSecureRedirectUriEnforceExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedNoConfigElement(SecureRedirectUriEnforceExecutorFactory.PROVIDER_ID, profileRep, (ClientProfileRepresentation i)->i.getExecutors());
    }

    protected void assertExpectedSecureRequestObjectExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedNoConfigElement(SecureRequestObjectExecutorFactory.PROVIDER_ID, profileRep, (ClientProfileRepresentation i)->i.getExecutors());
    }

    protected void assertExpectedSecureResponseTypeExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedNoConfigElement(SecureResponseTypeExecutorFactory.PROVIDER_ID, profileRep, (ClientProfileRepresentation i)->i.getExecutors());
    }

    protected void assertExpectedSecureSessionEnforceExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedNoConfigElement(SecureSessionEnforceExecutorFactory.PROVIDER_ID, profileRep, (ClientProfileRepresentation i)->i.getExecutors());
    }

    protected void assertExpectedSecureSigningAlgorithmEnforceExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedNoConfigElement(SecureSigningAlgorithmEnforceExecutorFactory.PROVIDER_ID, profileRep, (ClientProfileRepresentation i)->i.getExecutors());
    }

    protected void assertExpectedSecureSigningAlgorithmForSignedJwtEnforceExecutor(ClientProfileRepresentation profileRep) {
        assertExpectedNoConfigElement(SecureSigningAlgorithmForSignedJwtEnforceExecutorFactory.PROVIDER_ID, profileRep, (ClientProfileRepresentation i)->i.getExecutors());
    }

    protected JsonNode assertExpectedAugmenedExecutor(boolean isAugment, String providerId, ClientProfileRepresentation profileRep) {
        assertNotNull(profileRep);
        JsonNode actualExecutorConfig = getConfig(profileRep.getExecutors(), providerId);
        assertNotNull(actualExecutorConfig);

        boolean actualIsAugment = false;
        if (actualExecutorConfig.findValue("is-augment") != null) actualIsAugment = actualExecutorConfig.findValue("is-augment").asBoolean();
        assertEquals(isAugment, actualIsAugment);

        return actualExecutorConfig;
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

    protected void assertExpectedPolicy(String name, String description, boolean isBuiltin, boolean isEnabled, List<String> profiles, ClientPolicyRepresentation actualPolicyRep) {
        assertNotNull(actualPolicyRep);
        assertEquals(description, actualPolicyRep.getDescription());
        assertEquals(isBuiltin, actualPolicyRep.isBuiltin());
        assertEquals(isEnabled, actualPolicyRep.isEnable());
        assertEquals(new HashSet<>(profiles), new HashSet<>(actualPolicyRep.getProfiles()));
    }

    // conditions

    protected void assertExpectedConditions(List<String> expectedConditions, ClientPolicyRepresentation policyRep) {
        assertExpetedElement(expectedConditions, policyRep, (ClientPolicyRepresentation i)->i.getConditions());
    }

    protected void assertExpectedAnyClientCondition(ClientPolicyRepresentation profileRep) {
        assertExpectedNoConfigElement(AnyClientConditionFactory.PROVIDER_ID, profileRep, (ClientPolicyRepresentation i)->i.getConditions());
    }

    protected void assertExpectedClientAccessTypeCondition(List<String> type, ClientPolicyRepresentation policyRep) {
        JsonNode actualConditionConfig = getConfig(policyRep.getConditions(), ClientAccessTypeConditionFactory.PROVIDER_ID);

        Set<String> actualTypes = new HashSet<>();
        if (actualConditionConfig.findValue("type") != null)
            actualConditionConfig.findValue("type").elements().forEachRemaining(i->actualTypes.add(i.asText()));
        assertEquals(new HashSet<>(type), actualTypes);
    }

    protected void assertExpectedClientRolesCondition(List<String> roles, ClientPolicyRepresentation policyRep) {
        JsonNode actualConditionConfig = getConfig(policyRep.getConditions(), ClientRolesConditionFactory.PROVIDER_ID);

        Set<String> actualRoles = new HashSet<>();
        if (actualConditionConfig.findValue("roles") != null)
            actualConditionConfig.findValue("roles").elements().forEachRemaining(i->actualRoles.add(i.asText()));
        assertEquals(new HashSet<>(roles), actualRoles);
    }

    protected void assertExpectedClientScopesCondition(String type, List<String> scopes, ClientPolicyRepresentation policyRep) {
        JsonNode actualConditionConfig = getConfig(policyRep.getConditions(), ClientScopesConditionFactory.PROVIDER_ID);

        String actualType = null;
        if (actualConditionConfig.findValue("type") != null) actualType = actualConditionConfig.findValue("type").asText();
        assertEquals(type, actualType);

        Set<String> actualScopes = new HashSet<>();
        if (actualConditionConfig.findValue("scope") != null)
            actualConditionConfig.findValue("scope").elements().forEachRemaining(i->actualScopes.add(i.asText()));
        assertEquals(new HashSet<>(scopes), actualScopes);
    }

    protected void assertExpectedClientUpdateContextCondition(List<String> updateClientSources, ClientPolicyRepresentation policyRep) {
        JsonNode actualConditionConfig = getConfig(policyRep.getConditions(), ClientUpdateContextConditionFactory.PROVIDER_ID);

        Set<String> actualUpdateClientSources = new HashSet<>();
        if (actualConditionConfig.findValue("update-client-source") != null)
            actualConditionConfig.findValue("update-client-source").elements().forEachRemaining(i->actualUpdateClientSources.add(i.asText()));
        assertEquals(new HashSet<>(updateClientSources), actualUpdateClientSources);
    }

    protected void assertExpectedClientUpdateSourceGroupsCondition(List<String> groups, ClientPolicyRepresentation policyRep) {
        JsonNode actualConditionConfig = getConfig(policyRep.getConditions(), ClientUpdateSourceGroupsConditionFactory.PROVIDER_ID);

        Set<String> actualGroups = new HashSet<>();
        if (actualConditionConfig.findValue("groups") != null)
            actualConditionConfig.findValue("groups").elements().forEachRemaining(i->actualGroups.add(i.asText()));
        assertEquals(new HashSet<>(groups), actualGroups);
    }

    protected void assertExpectedClientUpdateSourceHostsCondition(List<String> trustedHosts, List<Boolean> hostSendingRequestMustMatch, ClientPolicyRepresentation policyRep) {
        JsonNode actualConditionConfig = getConfig(policyRep.getConditions(), ClientUpdateSourceHostsConditionFactory.PROVIDER_ID);

        List<String> actualTrustedHosts = new ArrayList<>();
        if (actualConditionConfig.findValue("trusted-hosts") != null)
            actualConditionConfig.findValue("trusted-hosts").elements().forEachRemaining(i->actualTrustedHosts.add(i.asText()));
        assertEquals(trustedHosts, actualTrustedHosts);

        List<Boolean> actualHostSendingRequestMustMatch = new ArrayList<>();
        if (actualConditionConfig.findValue("host-sending-request-must-match") != null)
            actualConditionConfig.findValue("host-sending-request-must-match").elements().forEachRemaining(i->actualHostSendingRequestMustMatch.add(i.asBoolean()));
        assertEquals(trustedHosts, actualTrustedHosts);
    }

    protected void assertExpectedClientUpdateSourceRolesCondition(List<String> roles, ClientPolicyRepresentation policyRep) {
        JsonNode actualConditionConfig = getConfig(policyRep.getConditions(), ClientUpdateSourceRolesConditionFactory.PROVIDER_ID);

        Set<String> actualRoles = new HashSet<>();
        if (actualConditionConfig.findValue("roles") != null)
            actualConditionConfig.findValue("roles").elements().forEachRemaining(i->actualRoles.add(i.asText()));
        assertEquals(new HashSet<>(roles), actualRoles);
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

    private <T, R> void assertExpetedCompounds(List<String> expected, R rep, Function<R, List<T>> f, Function<T, String> g) {
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

    // condition/executor common (element)

    private <T> void assertExpetedElement(List<String> expected, T rep, Function<T, List<Object>> f) {
        assertNotNull(rep);
        List<Object> objs = f.apply(rep);
        if (objs == null) {
            assertNull(expected);
            return;
        }
        Set<String> actual = objs.stream().map(i->{
            ObjectMapper mp = new ObjectMapper();
            JsonNode node = mp.convertValue(i, JsonNode.class);
            return node.fieldNames().next();
        }).collect(Collectors.toSet());
        assertEquals(new HashSet<>(expected), actual);
    }

    private <T> void assertExpectedNoConfigElement(String providerId, T rep, Function<T, List<Object>> f) {
        assertNotNull(rep);
        JsonNode actualConfig = getConfig(f.apply(rep), providerId);
        assertEquals("", actualConfig.asText());
    }

    private JsonNode getConfig(List<Object> objs, String providerId) {
        List<JsonNode> nodes = objs.stream().map(i->(new ObjectMapper()).convertValue(i, JsonNode.class))
                .filter(j->j.fieldNames().next().equals(providerId)).collect(Collectors.toList());
        if (nodes == null || nodes.size() != 1) return null;
        return nodes.get(0);
    }

}
