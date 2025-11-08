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

import java.security.KeyPair;
import java.util.Collections;
import java.util.Random;
import java.util.UUID;

import jakarta.ws.rs.HttpMethod;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.authentication.authenticators.client.JWTClientAuthenticator;
import org.keycloak.authentication.authenticators.client.X509ClientAuthenticator;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwk.ECPublicJWK;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.utils.OIDCResponseMode;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.MutualTLSUtils;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;
import org.keycloak.util.JWKSUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.util.ClientPoliciesUtil.createEcJwk;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createRsaJwk;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateEcdsaKey;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateSignedDPoPProof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class FAPI2DPoPTest extends AbstractFAPI2Test {

    private static final String DPOP_JWT_HEADER_TYPE = "dpop+jwt";
    private static final String nonce = "123456"; // need to be 123456.

    @Rule
    public AssertEvents events = new AssertEvents(this);
    private KeyPair ecKeyPair;
    private KeyPair rsaKeyPair;
    private JWSHeader jwsRsaHeader;
    private JWSHeader jwsEcHeader;
    private String jktRsa;
    private String jktEc;

    @Before
    public void beforeDPoPTest() throws Exception {
        rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.MODULUS, ((RSAPublicJWK) jwkRsa).getModulus());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.PUBLIC_EXPONENT, ((RSAPublicJWK) jwkRsa).getPublicExponent());
        jktRsa = JWKSUtils.computeThumbprint(jwkRsa);
        jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);

        ecKeyPair = generateEcdsaKey("secp256r1");
        JWK jwkEc = createEcJwk(ecKeyPair.getPublic());
        jwkEc.getOtherClaims().put(ECPublicJWK.CRV, ((ECPublicJWK) jwkEc).getCrv());
        jwkEc.getOtherClaims().put(ECPublicJWK.X, ((ECPublicJWK) jwkEc).getX());
        jwkEc.getOtherClaims().put(ECPublicJWK.Y, ((ECPublicJWK) jwkEc).getY());
        jktEc = JWKSUtils.computeThumbprint(jwkEc);
        jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);
    }

    private final Random rand = new Random(System.currentTimeMillis());

    @Test
    public void testFAPI2DPoPSecurityProfileClientRegistration() throws Exception {
        testFAPI2ClientRegistration(getSecurityProfileName());
    }

    @Test
    public void testFAPI2DPoPSecurityProfileOIDCClientRegistration() throws Exception {
        testFAPI2OIDCClientRegistration(getSecurityProfileName());
    }

    @Test
    public void testFAPI2DPoPSecurityProfileSignatureAlgorithms() throws Exception {
        testFAPI2SignatureAlgorithms(getSecurityProfileName());
    }

    @Test
    public void testFAPI2DPoPSecurityProfileLoginWithPrivateKeyJWT() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getSecurityProfileName());

        // Register client with private-key-jwt
        String clientUUID = createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        });
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(clientUUID);
        ClientRepresentation client = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, clientConfig.getTokenEndpointAuthSigningAlg());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, clientConfig.getPkceCodeChallengeMethod());
        assertFalse(client.isImplicitFlowEnabled());
        assertFalse(client.isFullScopeAllowed());
        assertFalse(clientConfig.isUseMtlsHokToken());
        assertTrue(clientConfig.isUseDPoP());
        assertTrue(client.isConsentRequired());

        // send a pushed authorization request
        // use EC key for DPoP proof and send dpop_jkt explicitly
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec
        oauth.client(clientId);
        pkceGenerator = PkceGenerator.s256();
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce(nonce);
        requestObject.setCodeChallenge(pkceGenerator.getCodeChallenge());
        requestObject.setCodeChallengeMethod(pkceGenerator.getCodeChallengeMethod());
        requestObject.setDpopJkt(jktEc);
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        String signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        ParResponse pResp = oauth
                .client(clientId)
                .pushedAuthorizationRequest()
                .codeChallenge(pkceGenerator)
                .nonce(nonce)
                .dpopProof(dpopProofEncoded)
                .signedJwt(signedJwt)
                .send();

        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        request = null;

        // send an authorization request
        String code = loginUserAndGetCode(clientId, nonce,false);

        // send a token request
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);

        oauth.client(clientId).httpClient().set(MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
        AccessTokenResponse tokenResponse = oauth
                .client(clientId)
                .accessTokenRequest(code)
                .codeVerifier(pkceGenerator.getCodeVerifier())
                .dpopProof(dpopProofEncoded)
                .signedJwt(signedJwt)
                .send();
        assertSuccessfulTokenResponse(tokenResponse);

        // check HoK required
        // use EC key for DPoP proof
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(jktEc, accessToken.getConfirmation().getKeyThumbprint());
        RefreshToken refreshToken = oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        assertNull(refreshToken.getConfirmation());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    @Test
    public void testFAPI2DPoPSecurityProfileLoginWithMTLS() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getSecurityProfileName());

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
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, clientConfig.getTokenEndpointAuthSigningAlg());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, clientConfig.getPkceCodeChallengeMethod());
        assertFalse(client.isImplicitFlowEnabled());
        assertFalse(client.isFullScopeAllowed());
        assertFalse(clientConfig.isUseMtlsHokToken());
        assertTrue(clientConfig.isUseDPoP());
        assertTrue(client.isConsentRequired());

        oauth.client(clientId);

        // without PAR request - should fail
        oauth.openLoginForm();
        assertBrowserWithError("request_uri not included.");

        pkceGenerator = PkceGenerator.s256();

        // requiring hybrid request - should fail
        ParResponse pResp = oauth
                .client(clientId)
                .responseType(OIDCResponseType.CODE + " " + OIDCResponseType.ID_TOKEN + " " + OIDCResponseType.TOKEN)
                .pushedAuthorizationRequest()
                .codeChallenge(pkceGenerator)
                .state(null)
                .nonce(nonce)
                .send();

        assertEquals(401, pResp.getStatusCode());
        assertEquals(OAuthErrorException.UNAUTHORIZED_CLIENT, pResp.getError());

        // an additional parameter in an authorization request that does not exist in a PAR request - should fail
        pResp = oauth
                .client(clientId)
                .responseType(OIDCResponseType.CODE)
                .pushedAuthorizationRequest()
                .requestUri(null)
                .codeChallenge(pkceGenerator)
                .state(null)
                .nonce(nonce)
                .send();
        assertEquals(201, pResp.getStatusCode());
        oauth.loginForm().requestUri(pResp.getRequestUri()).param("custom", "value").open();
        assertBrowserWithError("PAR request did not include necessary parameters");

        // duplicated usage of a PAR request - should fail
        oauth.loginForm().requestUri(pResp.getRequestUri()).open();
        assertBrowserWithError("PAR not found. not issued or used multiple times.");

        // send a pushed authorization request
        // use RSA key for DPoP proof but not send dpop_jkt
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        pResp = oauth
                .client(clientId)
                .responseType(OIDCResponseType.CODE)
                .pushedAuthorizationRequest()
                .requestUri(null)
                .codeChallenge(pkceGenerator)
                .state(null)
                .nonce(nonce)
                .dpopProof(dpopProofEncoded)
                .send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();

        // send an authorization request
        String code = loginUserAndGetCode(clientId, "123456",false);

        // send a token request
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        AccessTokenResponse tokenResponse = oauth.
                client(clientId).
                accessTokenRequest(code).
                codeVerifier(pkceGenerator.getCodeVerifier()).
                dpopProof(dpopProofEncoded).
                send();

        // check HoK required
        // use RSA key for DPoP proof
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(jktRsa, accessToken.getConfirmation().getKeyThumbprint());
        RefreshToken refreshToken = oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        assertNull(refreshToken.getConfirmation());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    @Test
    public void testFAPI2DPoPMessageSigningClientRegistration() throws Exception {
        testFAPI2ClientRegistration(getMessageSigningName());
    }

    @Test
    public void testFAPI2DPoPMessageSigningOIDCClientRegistration() throws Exception {
        testFAPI2OIDCClientRegistration(getMessageSigningName());
    }

    @Test
    public void testFAPI2DPoPMessageSigningSignatureAlgorithms() throws Exception {
        testFAPI2SignatureAlgorithms(getMessageSigningName());
    }

    @Test
    public void testFAPI2DPoPMessageSigningLoginWithMTLS() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getMessageSigningName());

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
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        assertEquals(X509ClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, clientConfig.getTokenEndpointAuthSigningAlg());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, clientConfig.getPkceCodeChallengeMethod());
        assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());
        assertFalse(client.isImplicitFlowEnabled());
        assertFalse(client.isFullScopeAllowed());
        assertFalse(clientConfig.isUseMtlsHokToken());
        assertTrue(clientConfig.isUseDPoP());
        assertTrue(client.isConsentRequired());

        // Set request object and correct responseType
        // use EC key for DPoP proof and send dpop_jkt explicitly
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        oauth.client(clientId);
        pkceGenerator = PkceGenerator.s256();
        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce(nonce);
        requestObject.setResponseType(OIDCResponseType.CODE);
        requestObject.setResponseMode(OIDCResponseMode.QUERY_JWT.value());
        requestObject.setCodeChallenge(pkceGenerator.getCodeChallenge());
        requestObject.setCodeChallengeMethod(pkceGenerator.getCodeChallengeMethod());
        requestObject.setDpopJkt(jktEc);
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        // send a pushed authorization request
        ParResponse pResp = oauth
                .client(clientId)
                .pushedAuthorizationRequest()
                .request(request)
                .requestUri(null)
                .codeChallenge(pkceGenerator)
                .nonce(nonce)
                .dpopProof(dpopProofEncoded)
                .dpopJkt(jktEc)
                .send();
        assertEquals(201, pResp.getStatusCode());

        // send an authorization request
        oauth.responseType(OIDCResponseType.CODE);
        oauth.responseMode(OIDCResponseMode.QUERY_JWT.value());
        requestUri = pResp.getRequestUri();
        request = null;
        String code = loginUserAndGetCodeInJwtQueryResponseMode(clientId, nonce);

        // send a token request
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse tokenResponse = oauth
                .client(clientId)
                .accessTokenRequest(code)
                .codeVerifier(pkceGenerator.getCodeVerifier())
                .dpopProof(dpopProofEncoded)
                .send();

        // check HoK required
        // use EC key for DPoP proof
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(jktEc, accessToken.getConfirmation().getKeyThumbprint());
        RefreshToken refreshToken = oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        assertNull(refreshToken.getConfirmation());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }


    @Test
    public void testFAPI2DPoPMessageSigningLoginWithPrivateKeyJWT() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getMessageSigningName());

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
        OIDCAdvancedConfigWrapper clientConfig = OIDCAdvancedConfigWrapper.fromClientRepresentation(client);
        assertEquals(JWTClientAuthenticator.PROVIDER_ID, client.getClientAuthenticatorType());
        assertEquals(Algorithm.PS256, clientConfig.getTokenEndpointAuthSigningAlg());
        assertEquals(Algorithm.PS256, clientConfig.getRequestObjectSignatureAlg());
        assertEquals(OAuth2Constants.PKCE_METHOD_S256, clientConfig.getPkceCodeChallengeMethod());
        assertFalse(client.isImplicitFlowEnabled());
        assertFalse(client.isFullScopeAllowed());
        assertFalse(clientConfig.isUseMtlsHokToken());
        assertTrue(clientConfig.isUseDPoP());
        assertTrue(client.isConsentRequired());

        oauth.client(clientId);
        pkceGenerator = PkceGenerator.s256();

        // without a request object - should fail
        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        registerRequestObject(requestObject, clientId, Algorithm.PS256, true);
        String signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        ParResponse pResp = oauth
                .client(clientId)
                .responseType(OIDCResponseType.CODE)
                .pushedAuthorizationRequest()
                .request(null)
                .requestUri(null)
                .codeChallenge(pkceGenerator)
                .state(null)
                .nonce(nonce)
                .signedJwt(signedJwt)
                .send();
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());

        // Set request object and correct responseType
        requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce(nonce);
        requestObject.setResponseType(OIDCResponseType.CODE);
        requestObject.setResponseMode(OIDCResponseMode.QUERY_JWT.value());
        requestObject.setCodeChallenge(pkceGenerator.getCodeChallenge());
        requestObject.setCodeChallengeMethod(pkceGenerator.getCodeChallengeMethod());
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        // send a pushed authorization request
        // use RSA key for DPoP proof but not send dpop_jkt
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        pResp = oauth
                .client(clientId)
                .pushedAuthorizationRequest()
                .request(request)
                .requestUri(null)
                .codeChallenge(pkceGenerator)
                .nonce(nonce)
                .dpopProof(dpopProofEncoded)
                .signedJwt(signedJwt)
                .send();
        assertEquals(201, pResp.getStatusCode());

        // send an authorization request
        requestUri = pResp.getRequestUri();
        request = null;
        String code = loginUserAndGetCodeInJwtQueryResponseMode(clientId, null);

        // send a token request
        // use RSA key for DPoP proof
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256);
        AccessTokenResponse tokenResponse = oauth
                .client(clientId)
                .accessTokenRequest(code)
                .codeVerifier(pkceGenerator.getCodeVerifier())
                .dpopProof(dpopProofEncoded)
                .signedJwt(signedJwt)
                .send();
        assertSuccessfulTokenResponse(tokenResponse);

        // check HoK required
        // use RSA key for DPoP proof
        AccessToken accessToken = oauth.verifyToken(tokenResponse.getAccessToken());
        assertEquals(jktRsa, accessToken.getConfirmation().getKeyThumbprint());
        RefreshToken refreshToken = oauth.parseRefreshToken(tokenResponse.getRefreshToken());
        assertNull(refreshToken.getConfirmation());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }


    @Test
    public void testSecureClientAuthenticationAssertion() throws Exception {
        // setup client policy
        setupPolicyFAPI2ForAllClient(getSecurityProfileName());

        // Register client with private-key-jwt
        createClientByAdmin(clientId, (ClientRepresentation clientRep) -> {
            clientRep.setClientAuthenticatorType(JWTClientAuthenticator.PROVIDER_ID);
            OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setRequestUris(Collections.singletonList(TestApplicationResourceUrls.clientRequestUri()));
        });

        oauth.client(clientId);

        PkceGenerator pkceGenerator = PkceGenerator.s256();

        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(clientId);
        requestObject.setNonce("123456");
        requestObject.setCodeChallenge(pkceGenerator.getCodeChallenge());
        requestObject.setCodeChallengeMethod(pkceGenerator.getCodeChallengeMethod());
        registerRequestObject(requestObject, clientId, Algorithm.PS256, false);

        // Send a push authorization request with invalid 'aud' . Should fail
        String signedJwt = createSignedRequestToken(clientId, Algorithm.PS256, getRealmInfoUrl() + "/protocol/openid-connect/ext/par/request");
        ParResponse pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        assertEquals(400, pResp.getStatusCode());

        // Send a push authorization request with valid 'aud' . Should succeed
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256, getRealmInfoUrl());
        pResp = oauth.pushedAuthorizationRequest().request(request).signedJwt(signedJwt).send();
        assertEquals(201, pResp.getStatusCode());
        requestUri = pResp.getRequestUri();
        request = null;

        // Send an authorization request . Should succeed
        String code = loginUserAndGetCode(clientId, null, false);
        assertNotNull(code);

        // Send a token request with invalid 'aud' . Should fail
        signedJwt = createSignedRequestToken(clientId, Algorithm.PS256, getRealmInfoUrl() + "/protocol/openid-connect/token");
        this.pkceGenerator = pkceGenerator;
        AccessTokenResponse tokenResponse = doAccessTokenRequestWithClientSignedJWT(code, signedJwt, MutualTLSUtils::newCloseableHttpClientWithDefaultKeyStoreAndTrustStore);
        assertEquals(400, tokenResponse.getStatusCode());

        // Logout and remove consent of the user for next logins
        logoutUserAndRevokeConsent(clientId, TEST_USERNAME);
    }

    protected String getSecurityProfileName() {
        return FAPI2_DPOP_SECURITY_PROFILE_NAME;
    }

    protected String getMessageSigningName() {
        return FAPI2_DPOP_MESSAGE_SIGNING_PROFILE_NAME;
    }
}
