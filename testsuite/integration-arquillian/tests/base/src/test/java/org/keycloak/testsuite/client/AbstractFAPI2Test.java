/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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
 *
 */
package org.keycloak.testsuite.client;

import java.util.Collections;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testsuite.util.ClientPoliciesUtil;

import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test for the FAPI 2 specifications (still implementer's draft):
 * <a href="https://openid.bitbucket.io/fapi/fapi-2_0-security-profile.html">FAPI 2.0 Security Profile</a>
 * <a href="https://openid.bitbucket.io/fapi/fapi-2_0-message-signing.html">FAPI 2.0 Message Signing</a>
 * Mostly tests the global FAPI policies work as expected
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public abstract class AbstractFAPI2Test extends AbstractFAPITest {

    protected static final String clientId = "foo";

    protected void testFAPI2ClientRegistration(String profile) throws Exception {
        setupPolicyFAPI2ForAllClient(profile);

        // Register client with clientIdAndSecret - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Register client with signedJWT - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Register client with privateKeyJWT, but unsecured redirectUri - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
                clientRep.setRedirectUris(Collections.singletonList("http://foo"));
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Try to register client with "client-jwt" - should pass
        String clientUUID = createClientByAdmin("client-jwt", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID));
        ClientRepresentation client = getClientByAdmin(clientUUID);
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with "client-x509" - should pass
        clientUUID = createClientByAdmin("client-x509", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID));
        client = getClientByAdmin(clientUUID);
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with default authenticator - should pass. Client authenticator should be "client-jwt"
        clientUUID = createClientByAdmin("client-jwt-2", (ClientRepresentation clientRep) -> {
        });
        client = getClientByAdmin(clientUUID);
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check the Consent is enabled, Holder-of-key is enabled, fullScopeAllowed disabled and default signature algorithm.
        assertTrue(client.isConsentRequired());
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        assertEquals(Algorithm.PS256, clientConfig.getIdTokenSignedResponseAlg());
        assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());
        assertFalse(client.isFullScopeAllowed());
        switch (profile) {
            case FAPI2_SECURITY_PROFILE_NAME:
            case FAPI2_MESSAGE_SIGNING_PROFILE_NAME:
                assertTrue(clientConfig.isUseMtlsHokToken());
                assertFalse(clientConfig.isUseDPoP());
                break;
            case FAPI2_DPOP_SECURITY_PROFILE_NAME:
            case FAPI2_DPOP_MESSAGE_SIGNING_PROFILE_NAME:
                assertFalse(clientConfig.isUseMtlsHokToken());
                assertTrue(clientConfig.isUseDPoP());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + profile);
        }
    }

    protected void testFAPI2OIDCClientRegistration(String profile) throws Exception {
        setupPolicyFAPI2ForAllClient(profile);

        // Try to register client with clientIdAndSecret - should fail
        try {
            createClientDynamically(generateSuffixedName(clientId), (OIDCClientRepresentation clientRep) -> clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_BASIC));
            fail();
        } catch (ClientRegistrationException e) {
            assertEquals(ERR_MSG_CLIENT_REG_FAIL, e.getMessage());
        }

        // Try to register client with "client-jwt" - should pass
        String clientUUID = createClientDynamically("client-jwt", (OIDCClientRepresentation clientRep) -> {
            clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.PRIVATE_KEY_JWT);
            clientRep.setJwksUri("https://foo");
        });
        ClientRepresentation client = getClientByAdmin(clientUUID);
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertFalse(client.isFullScopeAllowed());

        // Set new initialToken for register new clients
        setInitialAccessTokenForDynamicClientRegistration();

        // Try to register client with "client-x509" - should pass
        clientUUID = createClientDynamically("client-x509", (OIDCClientRepresentation clientRep) -> clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH));
        client = getClientByAdmin(clientUUID);
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check the Consent is enabled, PKCS set to S256
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        assertTrue(client.isConsentRequired());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, clientConfig.getPkceCodeChallengeMethod());

        // Check Holder-of-key is enabled
        switch (profile) {
            case FAPI2_SECURITY_PROFILE_NAME:
            case FAPI2_MESSAGE_SIGNING_PROFILE_NAME:
                assertTrue(clientConfig.isUseMtlsHokToken());
                assertFalse(clientConfig.isUseDPoP());
                break;
            case FAPI2_DPOP_SECURITY_PROFILE_NAME:
            case FAPI2_DPOP_MESSAGE_SIGNING_PROFILE_NAME:
                assertFalse(clientConfig.isUseMtlsHokToken());
                assertTrue(clientConfig.isUseDPoP());
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + profile);
        }
    }

    protected void testFAPI2SignatureAlgorithms(String profile) throws Exception {
        setupPolicyFAPI2ForAllClient(profile);

        // Test that unsecured algorithm (RS256) is not possible
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
                OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
                clientConfig.setIdTokenSignedResponseAlg(Algorithm.RS256);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_REQUEST, e.getMessage());
        }

        // Test that secured algorithm is possible to explicitly set
        String clientUUID = createClientByAdmin("client-jwt", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientCfg = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientCfg.setIdTokenSignedResponseAlg(Algorithm.ES256);
        });
        ClientRepresentation client = getClientByAdmin(clientUUID);
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        assertEquals(Algorithm.ES256, clientConfig.getIdTokenSignedResponseAlg());
        assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());

        // Test default algorithms set everywhere
        clientUUID = createClientByAdmin("client-jwt-default-alg", (ClientRepresentation clientRep) -> clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID));
        client = getClientByAdmin(clientUUID);
        clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        assertEquals(Algorithm.PS256, clientConfig.getIdTokenSignedResponseAlg());
        assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());
        assertEquals(Algorithm.PS256, clientConfig.getUserInfoSignedResponseAlg());
        assertEquals(Algorithm.PS256, clientConfig.getTokenEndpointAuthSigningAlg());
        assertEquals(Algorithm.PS256, client.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

    }

    protected void setupPolicyFAPI2ForAllClient(String profile) throws Exception {
        String json = (new ClientPoliciesUtil.ClientPoliciesBuilder()).addPolicy(
                (new ClientPoliciesUtil.ClientPolicyBuilder()).createPolicy("MyPolicy", "Policy for enable FAPI 2.0 Security Profile for all clients", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(profile)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

}
