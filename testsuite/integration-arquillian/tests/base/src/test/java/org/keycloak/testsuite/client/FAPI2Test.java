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
 *
 */
package org.keycloak.testsuite.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;

import java.util.Collections;

import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.ClientIdAndSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.JWTClientSecretAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.crypto.Algorithm;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.OAuthClient.ParResponse;

/**
 * Test for the FAPI 2 specifications (still implementer's draft):
 * - FAPI 2.0 Security Profile - https://openid.bitbucket.io/fapi/fapi-2_0-security-profile.html
 * - FAPI 2.0 Message Signing - https://openid.bitbucket.io/fapi/fapi-2_0-message-signing.html
 *
 * Mostly tests the global FAPI policies work as expected
 *
 * @author <a href="mailto:takashi.norimatsu.ws@hitachi.com">Takashi Norimatsu</a>
 */
public class FAPI2Test extends AbstractFAPITest {

    private static final String clientId = "foo";

    @Test
    public void testFAPI2SecurityProfileClientRegistration() throws Exception {
        testFAPI2ClientRegistration(FAPI2_SECURITY_PROFILE_NAME);
    }

    @Test
    public void testFAPI2SecurityProfileOIDCClientRegistration() throws Exception {
        testFAPI2OIDCClientRegistration(FAPI2_SECURITY_PROFILE_NAME);
    }

    @Test
    public void testFAPI2SecurityProfileSignatureAlgorithms(String profile) throws Exception {
        testFAPI2SignatureAlgorithms(FAPI2_SECURITY_PROFILE_NAME);
    }

    @Test
    public void testFAPI2SecurityProfileLoginWithPrivateKeyJWT() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(FAPI2_SECURITY_PROFILE_NAME);

        // Register client with private-key-jwt
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getTokenEndpointAuthSigningAlg());
        assertEquals(false, client.isImplicitFlowEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
        assertEquals(true, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).isUseMtlsHokToken());
        assertEquals(false, client.isFullScopeAllowed());
        assertEquals(true, client.isConsentRequired());

        // send a pushed authorization request
        oauth.clientId(clientId);
        String codeVerifier = "1234567890123456789012345678901234567890123"; // 43
        String codeChallenge = generateS256CodeChallenge(codeVerifier);

        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce("123456");
        requestObject.setCodeChallenge(codeChallenge);
        requestObject.setCodeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        String signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, null, signedJwt);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();
        oauth.requestUri(requestUri);
        oauth.request(null);

        // send an authorization request
        String code = loginUserAndGetCode(clientId, false);

        // send a token request
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        OAuthClient.AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, codeVerifier, () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        assertSuccessfulTokenResponse(tokenResponse);

        // check HoK required
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    @Test
    public void testFAPI2SecurityProfileLoginWithMTLS() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(FAPI2_SECURITY_PROFILE_NAME);

        // create client with MTLS authentication
        // Register client with X509
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn(MutualTLSUtils.DEFAULT_KEYSTORE_SUBJECT_DN);
            clientConfig.setAllowRegexPatternComparison(false);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getTokenEndpointAuthSigningAlg());
        assertEquals(false, client.isImplicitFlowEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
        assertEquals(true, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).isUseMtlsHokToken());
        assertEquals(false, client.isFullScopeAllowed());
        assertEquals(true, client.isConsentRequired());

        oauth.clientId(clientId);

        // without PAR request - should fail
        oauth.openLoginForm();
        assertBrowserWithError("request_uri not included.");

        String codeVerifier = "1234567890123456789012345678901234567890123"; // 43
        String codeChallenge = generateS256CodeChallenge(codeVerifier);
        oauth.codeChallenge(codeChallenge);
        oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        oauth.stateParamHardcoded(null);
        oauth.nonce("123456");

        // requiring hybrid request - should fail
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, null);
        assertEquals(401, pResp.getStatusCode());
        assertEquals(OAuthErrorException.UNAUTHORIZED_CLIENT, pResp.getError());

        // authorization request does not match PAR request - should fail
        oauth.responseType(OIDCResponseType.CODE);
        pResp = oauth.doPushedAuthorizationRequest(clientId, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();
        oauth.responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN);
        oauth.requestUri(requestUri);
        oauth.openLoginForm();
        assertRedirectedToClientWithError(OAuthErrorException.INVALID_REQUEST, false, "Parameter response_type does not match");

        oauth.responseType(OIDCResponseType.CODE);

        // an additional parameter in an authorization request that does not exist in a PAR request - should fail
        oauth.requestUri(null);
        pResp = oauth.doPushedAuthorizationRequest(clientId, null);
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        oauth.stateParamRandom();
        oauth.requestUri(requestUri);
        oauth.openLoginForm();
        assertBrowserWithError("PAR request did not include necessary parameters");

        // duplicated usage of a PAR request - should fail
        oauth.openLoginForm();
        assertBrowserWithError("PAR not found. not issued or used multiple times.");

        // send a pushed authorization request
        oauth.stateParamHardcoded(null);
        oauth.requestUri(null);
        pResp = oauth.doPushedAuthorizationRequest(clientId, null);
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();

        // send an authorization request
        oauth.requestUri(requestUri);
        String code = loginUserAndGetCode(clientId, false);

        // send a token request
        oauth.codeVerifier(codeVerifier);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, null);

        // check HoK required
        assertSuccessfulTokenResponse(tokenResponse);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    @Test
    public void testFAPI2MessageSigningClientRegistration() throws Exception {
        testFAPI2ClientRegistration(FAPI2_MESSAGE_SIGNING_PROFILE_NAME);
    }

    @Test
    public void testFAPI2MessageSigningOIDCClientRegistration() throws Exception {
        testFAPI2OIDCClientRegistration(FAPI2_MESSAGE_SIGNING_PROFILE_NAME);
    }

    @Test
    public void testFAPI2MessageSigningSignatureAlgorithms(String profile) throws Exception {
        testFAPI2SignatureAlgorithms(FAPI2_MESSAGE_SIGNING_PROFILE_NAME);
    }


    @Test
    public void testFAPI2MessageSigningLoginWithMTLS() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(FAPI2_MESSAGE_SIGNING_PROFILE_NAME);

        // create client with MTLS authentication
        // Register client with X509
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setTlsClientAuthSubjectDn(MutualTLSUtils.DEFAULT_KEYSTORE_SUBJECT_DN);
            clientConfig.setAllowRegexPatternComparison(false);
            clientConfig.setRequestObjectRequired("request or request_uri");
            clientConfig.setAuthorizationSignedResponseAlg(Algorithm.PS256);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getTokenEndpointAuthSigningAlg());
        assertEquals(false, client.isImplicitFlowEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
        assertEquals(true, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).isUseMtlsHokToken());
        assertEquals(false, client.isFullScopeAllowed());
        assertEquals(true, client.isConsentRequired());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getRequestObjectSignatureAlg());

        // Set request object and correct responseType
        oauth.clientId(clientId);
        oauth.stateParamHardcoded(null);
        String codeVerifier = "1234567890123456789012345678901234567890123"; // 43
        String codeChallenge = generateS256CodeChallenge(codeVerifier);
        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce("123456");
        requestObject.setResponseType(OIDCResponseType.CODE);
        requestObject.setResponseMode(OIDCResponseMode.QUERY_JWT.value());
        requestObject.setCodeChallenge(codeChallenge);
        requestObject.setCodeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        // send a pushed authorization request
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // send an authorization request
        oauth.codeChallenge(codeChallenge);
        oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        oauth.nonce("123456");
        oauth.responseType(OIDCResponseType.CODE);
        oauth.responseMode(OIDCResponseMode.QUERY_JWT.value());
        oauth.requestUri(requestUri);
        oauth.request(null);
        String code = loginUserAndGetCodeInJwtQueryResponseMode(clientId);

        // send a token request
        oauth.codeVerifier(codeVerifier);
        OAuthClient.AccessTokenResponse tokenResponse = oauth.doAccessTokenRequest(code, null);

        // check HoK required
        assertSuccessfulTokenResponse(tokenResponse);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    @Test
    public void testFAPI2MessageSigningLoginWithPrivateKeyJWT() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(FAPI2_MESSAGE_SIGNING_PROFILE_NAME);

        // create client with MTLS authentication
        // Register client with X509
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
            clientConfig.setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
            clientConfig.setRequestObjectRequired("request or request_uri");
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setAuthorizationSignedResponseAlg(Algorithm.PS256);
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getTokenEndpointAuthSigningAlg());
        assertEquals(Algorithm.PS256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getRequestObjectSignatureAlg());
        assertEquals(false, client.isImplicitFlowEnabled());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());
        assertEquals(true, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).isUseMtlsHokToken());
        assertEquals(false, client.isFullScopeAllowed());
        assertEquals(true, client.isConsentRequired());

        oauth.clientId(clientId);
        oauth.stateParamHardcoded(null);
        String codeVerifier = "1234567890123456789012345678901234567890123"; // 43
        String codeChallenge = generateS256CodeChallenge(codeVerifier);

        // without a request object - should fail
        oauth.codeChallenge(codeChallenge);
        oauth.codeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        oauth.stateParamHardcoded(null);
        oauth.nonce("123456");
        oauth.responseType(OIDCResponseType.CODE);
        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        registerRequestObject(requestObject, clientId, Algorithm.PS256, true);
        oauth.requestUri(null);
        oauth.request(null);
        String signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        ParResponse pResp = oauth.doPushedAuthorizationRequest(clientId, null, signedJwt);
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST_OBJECT, pResp.getError());

        // Set request object and correct responseType
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce("123456");
        requestObject.setResponseType(OIDCResponseType.CODE);
        requestObject.setResponseMode(OIDCResponseMode.QUERY_JWT.value());
        requestObject.setCodeChallenge(codeChallenge);
        requestObject.setCodeChallengeMethod(OAuth2Constants.PKCE_METHOD_S256);
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        // send a pushed authorization request
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        pResp = oauth.doPushedAuthorizationRequest(clientId, null, signedJwt);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // send an authorization request
        oauth.requestUri(requestUri);
        oauth.request(null);
        String code = loginUserAndGetCodeInJwtQueryResponseMode(clientId);

        // send a token request
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        OAuthClient.AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, codeVerifier, () -> MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        assertSuccessfulTokenResponse(tokenResponse);
 
        // check HoK required
        assertSuccessfulTokenResponse(tokenResponse);
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        Assert.assertNotNull(accessToken.getConfirmation().getCertThumbprint());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    private void testFAPI2ClientRegistration(String profile) throws Exception {
        setupPolicyFAPI2ForAllClient(profile);

        // Register client with clientIdAndSecret - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(ClientIdAndSecretAuthenticator.PROVIDER_ID);
            });
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // Register client with signedJWT - should fail
        try {
            createClientByAdmin("invalid", (ClientRepresentation clientRep) -> {
                clientRep.setClientAuthenticatorType(JWTClientSecretAuthenticator.PROVIDER_ID);
            });
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
        String clientUUID = createClientByAdmin("client-jwt", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        });
        ClientRepresentation client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with "client-x509" - should pass
        clientUUID = createClientByAdmin("client-x509", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(X509ClientAuthenticator.PROVIDER_ID);
        });
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Try to register client with default authenticator - should pass. Client authenticator should be "client-jwt"
        clientUUID = createClientByAdmin("client-jwt-2", (ClientRepresentation clientRep) -> {
        });
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check the Consent is enabled, Holder-of-key is enabled, fullScopeAllowed disabled and default signature algorithm.
        Assert.assertTrue(client.isConsentRequired());
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        Assert.assertTrue(clientConfig.isUseMtlsHokToken());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getIdTokenSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());
        Assert.assertFalse(client.isFullScopeAllowed());
    }

    private void testFAPI2OIDCClientRegistration(String profile) throws Exception {
        setupPolicyFAPI2ForAllClient(profile);

        // Try to register client with clientIdAndSecret - should fail
        try {
            createClientDynamically(generateSuffixedName(clientId), (OIDCClientRepresentation clientRep) -> {
                clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.CLIENT_SECRET_BASIC);
            });
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
        Assert.assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        Assert.assertFalse(client.isFullScopeAllowed());

        // Set new initialToken for register new clients
        setInitialAccessTokenForDynamicClientRegistration();

        // Try to register client with "client-x509" - should pass
        clientUUID = createClientDynamically("client-x509", (OIDCClientRepresentation clientRep) -> {
            clientRep.setTokenEndpointAuthMethod(OIDCLoginProtocol.TLS_CLIENT_AUTH);
        });
        client = getClientByAdmin(clientUUID);
        Assert.assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());

        // Check the Consent is enabled, PKCS set to S256
        Assert.assertTrue(client.isConsentRequired());
        Assert.assertEquals(OAuth2Constants.PKCE_METHOD_S256, OIDCAdvancedConfigWrapper.fromClientRepresentation(client).getPkceCodeChallengeMethod());

    }

    private void testFAPI2SignatureAlgorithms(String profile) throws Exception {
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
        Assert.assertEquals(Algorithm.ES256, clientConfig.getIdTokenSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());

        // Test default algorithms set everywhere
        clientUUID = createClientByAdmin("client-jwt-default-alg", (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
        });
        client = getClientByAdmin(clientUUID);
        clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        Assert.assertEquals(Algorithm.PS256, clientConfig.getIdTokenSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg().toString());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getUserInfoSignedResponseAlg());
        Assert.assertEquals(Algorithm.PS256, clientConfig.getTokenEndpointAuthSigningAlg());
        Assert.assertEquals(Algorithm.PS256, client.getAttributes().get(OIDCConfigAttributes.ACCESS_TOKEN_SIGNED_RESPONSE_ALG));

    }

    private void setupPolicyFAPI2ForAllClient(String profile) throws Exception {
        String json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy", "Policy for enable FAPI 2.0 Security Profile for all clients", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID,
                                createAnyClientConditionConfig())
                        .addProfile(profile)
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
    }

}
