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
 */
package org.keycloak.testsuite.oauth.par;

import java.io.IOException;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import jakarta.ws.rs.HttpMethod;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwk.ECPublicJWK;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.client.policies.AbstractClientPoliciesTest;
import org.keycloak.testsuite.client.resources.TestApplicationResourceUrls;
import org.keycloak.testsuite.client.resources.TestOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.rest.resource.TestingOIDCEndpointsApplicationResource;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.ParResponse;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.testsuite.AbstractAdminTest.loadJson;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createEcJwk;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createRsaJwk;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateEcdsaKey;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateSignedDPoPProof;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParWithDPoPTest extends AbstractClientPoliciesTest {
    @Rule
    public AssertEvents events = new AssertEvents(this);

    private static final String REALM_NAME = "test";
    private static final String DPOP_JWT_HEADER_TYPE = "dpop+jwt";

    private KeyPair ecKeyPair;
    private KeyPair rsaKeyPair;
    private JWSHeader jwsRsaHeader;
    private JWSHeader jwsEcHeader;
    private String jktRsa;
    private String jktEc;

    // defined in testrealm.json
    private static final String TEST_USER_NAME = "test-user@localhost";
    private static final String TEST_USER_PASSWORD = "password";

    private static final String CLIENT_NAME = "Zahlungs-App";
    private static final String CLIENT_REDIRECT_URI = "https://localhost:8543/auth/realms/test/app/auth/cb";
    private static final String ERROR_DETAILS = "DPoP Proof public key thumbprint does not match dpop_jkt";

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

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    /*
     * Patters of where dpop_jkt appears
     *
     *        PAR                                 Authz
     * Case / DPoP Proof / Req Obj / Form Param / Query Param / Expected dpop_jkt / Note
     * C01      x          x           x             x          DPpP Proof          not valid form -> same as C04
     * C02      x          x           x             -          DPpP Proof          not valid form -> same as C04
     * C03      x          x           -             x          DPpP Proof          not valid form -> same as C04
     * C04      x          x           -             -          DPoP Proof          valid form: dpop_jkt from DPoP Proof = Req Obj
     * C05      x          -           x             x          DPoP Proof          not valid form -> same as C06
     * C06      x          -           x             -          DPoP Proof          valid form: dpop_jkt from DPoP Proof = Form Param
     * C07      x          -           -             x          DPoP Proof          not valid form -> same as C08
     * C08      x          -           -             -          DPoP Proof          valid form: dpop jkt from DPoP Proof
     * C09      -          x           x             x          Req Obj             not valid form -> same as C12
     * C10      -          x           x             -          Req Obj             not valid form -> same as C04
     * C11      -          x           -             x          Req Obj             not valid form -> same as C04
     * C12      -          x           -             -          Req Obj             valid form: dpop_jkt = Req Obj
     * C13      -          -           x             x          Form Param          not valid form -> same as C14
     * C14      -          -           x             -          Form Param          valid form: dpop_jkt = Form Param
     * C15      -          -           -             x          Query Param         not valid form -> same asC16
     * C16      -          -           -             -          N/A                 valid form: no dpop_jkt
     * 
     * - dpop_jkt in PAR: Req Obj precede the one in PAR: Form Param.
     * - dpop_jkt in Authz: Query Param is always ignored if request_uri exists (by PAR response) except that no dpop_jkt in PAR request.
     * 
     */

    @Test
    public void testSuccess_Proof_ReqObj_FormParam_QueryParam() throws Exception {
        // Result: Success
        // Patterns: C01
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (EC)
        //  - [2] attach dpop_jkt to Req Obj: YES (EC)
        //  - [3] attach dpop_jkt to Form Param: YES (RSA)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (RSA)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) [1] == [2] != [3] == [4] ([2] precede [3], [4] always ignores if PAR is used)
        //  - (2) DPoP Proof in Token Endpoint [5] == DPoP Proof/dpop_jkt in PAR Endpoint [1] == [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] EC Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        // [2] EC key 
        String requestObject = generateRequestObject(clientId, jktEc);
  
        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, dpopProofEncoded, jktRsa);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] jktRsat (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktRsa);

        // ----- Token Request -----
        // [5] EC key
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    @Test
    public void testFailure_Proof_ReqObj_FormParam_QueryParam_Token_Proof_Mismatch() throws Exception {
        // Result: Failure
        // Patterns: C01
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (EC)
        //  - [2] attach dpop_jkt to Req Obj: YES (EC)
        //  - [3] attach dpop_jkt to Form Param: YES (RSA)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (RSA)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (RSA)
        // Conditions:
        //  - (1) [1] == [2] != [3] == [4] ([2] precede [3], [4] always ignores if PAR is used)
        //  - (2) DPoP Proof in Token Endpoint [5] != DPoP Proof/dpop_jkt in PAR Endpoint [1] == [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] EC Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        // [2] EC key 
        String requestObject = generateRequestObject(clientId, jktEc);
  
        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, dpopProofEncoded, jktRsa);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] jktRsat (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktRsa);

        // ----- Token Request -----
        // [5] RSA key
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, res.getError());
        assertEquals(ERROR_DETAILS, res.getErrorDescription());
    }

    @Test
    public void testFailure_Proof_ReqObj_FormParam_QueryParam_PAR_Proof_Mismatch() throws Exception {
        // Result: Failure
        // Patterns: C01
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (EC)
        //  - [2] attach dpop_jkt to Req Obj: YES (RSA)
        //  - [3] attach dpop_jkt to Form Param: YES (EC)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: N/A (not reached)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: N/A (not reached)
        // Conditions:
        //  - (1) [3] == [1] != [2] ([2] precede [3])
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] EC Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        // [2] RSA key 
        String requestObject = generateRequestObject(clientId, jktRsa);
  
        // ----- PAR -----
        // [3] EC key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, dpopProofEncoded, jktEc);
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());
        assertEquals("DPoP Proof public key thumbprint does not match dpop_jkt.", pResp.getErrorDescription());
    }

    @Test
    public void testSuccess_Proof_ReqObj_FormParam() throws Exception {
        // Result: Success
        // Patterns: C02
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (EC)
        //  - [2] attach dpop_jkt to Req Obj: YES (EC)
        //  - [3] attach dpop_jkt to Form Param: YES (RSA)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: NO
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) [1] == [2] != [3] ([2] precede [3])
        //  - (2) DPoP Proof in Token Endpoint [5] == DPoP Proof/dpop_jkt in PAR Endpoint [1] == [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] EC Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        // [2] EC key 
        String requestObject = generateRequestObject(clientId, jktEc);
  
        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, dpopProofEncoded, jktRsa);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] no Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, null);

        // ----- Token Request -----
        // [5] EC key
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    @Test
    public void testFailure_Proof_ReqObj_FormParam_Token_Proof_Mismatch() throws Exception {
        // Result: Failure
        // Patterns: C02
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (EC)
        //  - [2] attach dpop_jkt to Req Obj: YES (EC)
        //  - [3] attach dpop_jkt to Form Param: YES (RSA)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: NO
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (RSA)
        // Conditions:
        //  - (1) [1] == [2] != [3] ([2] precede [3])
        //  - (2) DPoP Proof in Token Endpoint [5] != DPoP Proof/dpop_jkt in PAR Endpoint [1] == [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] EC Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        // [2] EC key 
        String requestObject = generateRequestObject(clientId, jktEc);
  
        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, dpopProofEncoded, jktRsa);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] no Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, null);

        // ----- Token Request -----
        // [5] RSA key
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, res.getError());
        assertEquals(ERROR_DETAILS, res.getErrorDescription());
    }

    @Test
    public void testSuccess_Proof_ReqObj_QueryParam() throws Exception {
        // Result: Success
        // Patterns: C03
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (RSA)
        //  - [2] attach dpop_jkt to Req Obj: YES (RSA)
        //  - [3] attach dpop_jkt to Form Param: NO
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (EC)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (RSA)
        // Conditions:
        //  - (1) [1] == [2] != [3] ([2] precede [3], [4] always ignores if PAR is used)
        //  - (2) DPoP Proof in Token Endpoint [5] == DPoP Proof/dpop_jkt in PAR Endpoint [1] == [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] RSA Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);

        // [2] RSA key 
        String requestObject = generateRequestObject(clientId, jktRsa);
  
        // ----- PAR -----
        // [3] no key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, dpopProofEncoded, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] EC Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktEc);

        // ----- Token Request -----
        // [5] RSA key
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    @Test
    public void testFailure_Proof_ReqObj_Token_Proof_Mismatch() throws Exception {
        // Result: Failure
        // Patterns: C04
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (RSA)
        //  - [2] attach dpop_jkt to Req Obj: YES (RSA)
        //  - [3] attach dpop_jkt to Form Param: NO
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: NO
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) [1] == [2] 
        //  - (2) DPoP Proof in Token Endpoint [5] != DPoP Proof/dpop_jkt in PAR Endpoint [1] == [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] RSA Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);

        // [2] RSA key 
        String requestObject = generateRequestObject(clientId, jktRsa);
  
        // ----- PAR -----
        // [3] no key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, dpopProofEncoded, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] no Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, null);

        // ----- Token Request -----
        // [5] EC key
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, res.getError());
        assertEquals(ERROR_DETAILS, res.getErrorDescription());
    }

    @Test
    public void testSuccess_Proof_FormObj_QueryParam() throws Exception {
        // Result: Success
        // Patterns: C05
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (RSA)
        //  - [2] attach dpop_jkt to Req Obj: NO
        //  - [3] attach dpop_jkt to Form Param: YES (RSA)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (EC)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (RSA)
        // Conditions:
        //  - (1) [1] != [3] ([4] always ignores if PAR is used)
        //  - (2) DPoP Proof in Token Endpoint [5] == DPoP Proof/dpop_jkt in PAR Endpoint [1]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] RSA Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
  
        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendGenerateAndPushedAuthorizationRequest(clientId, clientSecret, jktRsa, dpopProofEncoded);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] EC Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktEc);

        // ----- Token Request -----
        // [5] RSA key
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    @Test
    public void testFailure_Proof_FormObj_QueryParam_PAR_Proof_Mismatch() throws Exception {
        // Result: Failure
        // Patterns: C06
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (EC)
        //  - [2] attach dpop_jkt to Req Obj: No
        //  - [3] attach dpop_jkt to Form Param: YES (RSA)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: N/A (not reached)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: N/A (not reached)
        // Conditions:
        //  - (1) [1] != [3]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] EC Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendGenerateAndPushedAuthorizationRequest(clientId, clientSecret, jktRsa, dpopProofEncoded);
        assertEquals(400, pResp.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, pResp.getError());
        assertEquals("DPoP Proof public key thumbprint does not match dpop_jkt.", pResp.getErrorDescription());
    }

    @Test
    public void testSuccess_Proof_QueryParam() throws Exception {
        // Result: Success
        // Patterns: C07
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (RSA)
        //  - [2] attach dpop_jkt to Req Obj: NO
        //  - [3] attach dpop_jkt to Form Param: NO
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (EC)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (RSA)
        // Conditions:
        //  - (1) DPoP Proof in Token Endpoint [5] == DPoP Proof/dpop_jkt in PAR Endpoint [1]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] RSA Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
  
        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendGenerateAndPushedAuthorizationRequest(clientId, clientSecret, null, dpopProofEncoded);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] EC Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktEc);

        // ----- Token Request -----
        // [5] RSA key
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    @Test
    public void testFailure_Proof_Token_Proof_Mismatch() throws Exception {
        // Result: Failure
        // Patterns: C08
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: YES (RSA)
        //  - [2] attach dpop_jkt to Req Obj: NO
        //  - [3] attach dpop_jkt to Form Param: NO
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: NO
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) [1] == [2] 
        //  - (2) DPoP Proof in Token Endpoint [5] != DPoP Proof/dpop_jkt in PAR Endpoint [1] == [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [1] RSA Key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getPushedAuthorizationRequest(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);

        // ----- PAR -----
        // [3] no key
        ParResponse pResp =  sendGenerateAndPushedAuthorizationRequest(clientId, clientSecret, null, dpopProofEncoded);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] no Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, null);

        // ----- Token Request -----
        // [5] EC key
        dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, res.getError());
        assertEquals(ERROR_DETAILS, res.getErrorDescription());
    }

    @Test
    public void testSuccess_ReqObj_FormParam_QueryParam() throws Exception {
        // Result: Success
        // Patterns: C09
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: NO
        //  - [2] attach dpop_jkt to Req Obj: YES (EC)
        //  - [3] attach dpop_jkt to Form Param: YES (RSA)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (RSA)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) [2] != [3] = [4] ([2] precede [3], [4] always ignores if PAR is used)
        //  - (2) DPoP Proof in Token Endpoint [5] == DPoP Proof/dpop_jkt in PAR Endpoint [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [2] EC key 
        String requestObject = generateRequestObject(clientId, jktEc);
  
        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, null, jktRsa);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] RSA Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktRsa);

        // ----- Token Request -----
        // [5] EC key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    @Test
    public void testFailure_ReqObj_FormParam_Token_Proof_Mismatch() throws Exception {
        // Result: Failure
        // Patterns: C10
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: NO
        //  - [2] attach dpop_jkt to Req Obj: YES (EC)
        //  - [3] attach dpop_jkt to Form Param: YES (RSA)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: NO
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (RSA)
        // Conditions:
        //  - (1) [2] != [3] ([2] precede [3])
        //  - (2) DPoP Proof in Token Endpoint [5] == DPoP Proof/dpop_jkt in PAR Endpoint [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [2] EC key 
        String requestObject = generateRequestObject(clientId, jktEc);
  
        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, null, jktRsa);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] no Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, null);

        // ----- Token Request -----
        // [5] EC key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, res.getError());
        assertEquals(ERROR_DETAILS, res.getErrorDescription());
    }

    @Test
    public void testSuccess_ReqObj_QueryParam() throws Exception {
        // Result: Success
        // Patterns: C11
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: NO
        //  - [2] attach dpop_jkt to Req Obj: YES (EC)
        //  - [3] attach dpop_jkt to NO
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (RSA)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) [2] != [4] ([4] always ignores if PAR is used)
        //  - (2) DPoP Proof in Token Endpoint [5] == DPoP Proof/dpop_jkt in PAR Endpoint [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [2] EC key 
        String requestObject = generateRequestObject(clientId, jktEc);
  
        // ----- PAR -----
        // [3] no key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, null, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] RSA Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktRsa);

        // ----- Token Request -----
        // [5] EC key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    @Test
    public void testFailure_ReqObj_Token_Proof_Mismatch() throws Exception {
        // Result: Failure
        // Patterns: C12
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: NO
        //  - [2] attach dpop_jkt to Req Obj: YES (RSA)
        //  - [3] attach dpop_jkt to NO
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: NO
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) DPoP Proof in Token Endpoint [5] != DPoP Proof/dpop_jkt in PAR Endpoint [2]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);

        // [2] RSA key 
        String requestObject = generateRequestObject(clientId, jktRsa);
  
        // ----- PAR -----
        // [3] no key
        ParResponse pResp =  sendPushedAuthorizationRequestWithDpopJkt(clientId, clientSecret, requestObject, null, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] no Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, null);

        // ----- Token Request -----
        // [5] EC key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, res.getError());
        assertEquals(ERROR_DETAILS, res.getErrorDescription());
    }

    @Test
    public void testSuccess_FormParam_QueryParam() throws Exception {
        // Result: Success
        // Patterns: C13
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: NO
        //  - [2] attach dpop_jkt to Req Obj: No
        //  - [3] attach dpop_jkt to YES (EC)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (RSA)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) [3] != [4] ([4] always ignores if PAR is used)
        //  - (2) DPoP Proof in Token Endpoint [5] == DPoP Proof/dpop_jkt in PAR Endpoint [3]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);
 
        // ----- PAR -----
        // [3] EC key
        ParResponse pResp =  sendGenerateAndPushedAuthorizationRequest(clientId, clientSecret, jktEc, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] RSA Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktRsa);

        // ----- Token Request -----
        // [5] EC key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    @Test
    public void testFailure_FormParam_Token_Proof_Mismatch() throws Exception {
        // Result: Failure
        // Patterns: C14
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: NO
        //  - [2] attach dpop_jkt to Req Obj: No
        //  - [3] attach dpop_jkt to YES (RSA)
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: NO
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (2) DPoP Proof in Token Endpoint [5] != DPoP Proof/dpop_jkt in PAR Endpoint [3]
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);
 
        // ----- PAR -----
        // [3] RSA key
        ParResponse pResp =  sendGenerateAndPushedAuthorizationRequest(clientId, clientSecret, jktRsa, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] no Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, null);

        // ----- Token Request -----
        // [5] EC key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, res.getError());
        assertEquals(ERROR_DETAILS, res.getErrorDescription());
    }

    @Test
    public void testSuccess_QueryParam() throws Exception {
        // Result: Success
        // Patterns: C15
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: NO
        //  - [2] attach dpop_jkt to Req Obj: No
        //  - [3] attach dpop_jkt to No
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (EC)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) [5] == [4] (no DPoP Proof/dpop_jkt in PAR so [4] is effective)
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);
 
        // ----- PAR -----
        // [3] no key
        ParResponse pResp =  sendGenerateAndPushedAuthorizationRequest(clientId, clientSecret, null, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] EC Key (no DPoP Proof/dpop_jkt in PAR so it is effective)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktEc);

        // ----- Token Request -----
        // [5] EC key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    @Test
    public void testFailure_QueryParam() throws Exception {
        // Result: Failure
        // Patterns: C15
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: NO
        //  - [2] attach dpop_jkt to Req Obj: No
        //  - [3] attach dpop_jkt to No
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: YES (RSA)
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        //  - (1) [5] == [4] (no DPoP Proof/dpop_jkt in PAR so [4] is effective)
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);
 
        // ----- PAR -----
        // [3] no key
        ParResponse pResp =  sendGenerateAndPushedAuthorizationRequest(clientId, clientSecret, null, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] RSA Key (no DPoP Proof/dpop_jkt in PAR so it is effective)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, jktRsa);

        // ----- Token Request -----
        // [5] EC key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(400, res.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, res.getError());
        assertEquals(ERROR_DETAILS, res.getErrorDescription());
    }

    @Test
    public void testSuccess() throws Exception {
        // Result: Success
        // Patterns: C16
        //  PAR Endpoint:
        //  - [1] attach DPoP Proof: NO
        //  - [2] attach dpop_jkt to Req Obj: No
        //  - [3] attach dpop_jkt to No
        //  Authz Endpoint:
        //  - [4] attach dpop_jkt to Query Param: No
        //  Token Endpoint:
        //  - [5] attach DPoP Proof: YES (EC)
        // Conditions:
        int clockSkew = rand.nextInt(-10, 10); // acceptable clock skew is +-10sec

        // create client
        String clientId = createConfidentialClientForTest();
        String clientSecret = getConfidentialClientSecretForTest(clientId);
 
        // ----- PAR -----
        // [3] no key
        ParResponse pResp =  sendGenerateAndPushedAuthorizationRequest(clientId, clientSecret, null, null);
        assertEquals(201, pResp.getStatusCode());
        String requestUri = pResp.getRequestUri();

        // ----- Authorization Request -----
        // [4] no Key (always ignores if PAR is used)
        AuthorizationEndpointResponse loginResponse = sendAuthorizationRequest(clientId, requestUri, null);

        // ----- Token Request -----
        // [5] EC key
        String dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        AccessTokenResponse res = sentTokenRequest(clientId, clientSecret, loginResponse.getCode(), dpopProofEncoded);
        assertEquals(200, res.getStatusCode());
        oauth.verifyToken(res.getAccessToken());
    }

    private Random rand = new Random(System.currentTimeMillis());

    private String createConfidentialClientForTest() throws ClientRegistrationException {
        return createClientDynamically(generateSuffixedName(CLIENT_NAME), (OIDCClientRepresentation clientRep) -> {
            clientRep.setRequirePushedAuthorizationRequests(Boolean.TRUE);
            clientRep.setRedirectUris(new ArrayList<>(List.of(CLIENT_REDIRECT_URI)));
            clientRep.setDpopBoundAccessTokens(Boolean.TRUE); // apply DPoP
        });
    }

    private String getConfidentialClientSecretForTest(String clientId) throws ClientRegistrationException {
        OIDCClientRepresentation oidcCRep = getClientDynamically(clientId);
        String clientSecret = oidcCRep.getClientSecret();
        assertEquals(Boolean.TRUE, oidcCRep.getRequirePushedAuthorizationRequests());
        assertTrue(oidcCRep.getRedirectUris().contains(CLIENT_REDIRECT_URI));
        assertEquals(OIDCLoginProtocol.CLIENT_SECRET_BASIC, oidcCRep.getTokenEndpointAuthMethod());
        return clientSecret;
    }

    private String generateRequestObject(String clientId, String dpopJkt) throws IOException {
        TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject requestObject = new TestingOIDCEndpointsApplicationResource.AuthorizationEndpointRequestObject();
        requestObject.id(KeycloakModelUtils.generateId());
        requestObject.iat((long) Time.currentTime());
        requestObject.exp(requestObject.getIat() + 300L);
        requestObject.nbf(requestObject.getIat());
        requestObject.setClientId(clientId);
        requestObject.setResponseType(OAuth2Constants.CODE);
        requestObject.setRedirectUriParam(CLIENT_REDIRECT_URI);
        requestObject.setScope(OAuth2Constants.SCOPE_OPENID);
        requestObject.setNonce(KeycloakModelUtils.generateId());
        requestObject.setDpopJkt(dpopJkt);

        byte[] contentBytes = JsonSerialization.writeValueAsBytes(requestObject);
        String encodedRequestObject = Base64Url.encode(contentBytes);
        TestOIDCEndpointsApplicationResource client = testingClient.testApp().oidcClientEndpoints();

        // use and set jwks_url
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseJwksUrl(true);
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setJwksUrl(TestApplicationResourceUrls.clientJwksUri());
        clientResource.update(clientRep);
        client.generateKeys(org.keycloak.crypto.Algorithm.RS256);
        client.registerOIDCRequest(encodedRequestObject, org.keycloak.crypto.Algorithm.RS256);

        return client.getOIDCRequest();
    }

    private ParResponse sendGenerateAndPushedAuthorizationRequest(String clientId, String clientSecret, String dpopJkt, String dpopProofEncoded) throws IOException {
        oauth.client(clientId, clientSecret);
        oauth.responseType(OAuth2Constants.CODE);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        oauth.scope(OAuth2Constants.SCOPE_OPENID);
        // ----- PAR -----
        ParResponse pResp = oauth.pushedAuthorizationRequest().dpopJkt(dpopJkt).dpopProof(dpopProofEncoded).send();
        // revert
        oauth.client(null);
        oauth.responseType(null);
        oauth.redirectUri(null);
        oauth.scope(null);
        return pResp;
    }

    private ParResponse sendPushedAuthorizationRequestWithDpopJkt(String clientId, String clientSecret, String requestObject, String dpopProofEncoded, String dpopJkt) throws IOException {
        oauth.client(clientId, clientSecret);
        oauth.responseType(null);
        oauth.redirectUri(null);
        oauth.scope(null);
        // ----- PAR -----
        ParResponse pResp = oauth.pushedAuthorizationRequest().dpopJkt(dpopJkt).dpopProof(dpopProofEncoded).request(requestObject).send();
        // revert
        oauth.client(null);
        return pResp;
    }

    private AuthorizationEndpointResponse sendAuthorizationRequest(String clientId, String requestUri, String dpopJkt) {
        // Authorization Request with request_uri of PAR
        // remove parameters as query strings of uri
        oauth.clientId(clientId);
        oauth.responseType(null);
        oauth.redirectUri(null);
        oauth.scope(null);
        // ----- Authorization Request -----
        AuthorizationEndpointResponse loginResponse = oauth.loginForm().requestUri(requestUri).dpopJkt(dpopJkt).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        // revert
        oauth.clientId(null);
        return loginResponse;
    }

    private AccessTokenResponse sentTokenRequest(String clientId, String clientSecret, String code, String dpopProofEncoded) {
        oauth.client(clientId, clientSecret);
        oauth.redirectUri(CLIENT_REDIRECT_URI);
        // ----- Token Request -----
        AccessTokenResponse res = oauth.accessTokenRequest(code).dpopProof(dpopProofEncoded).send();
        // revert
        oauth.client(null);
        oauth.redirectUri(null);
        return res;
    }

}
