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

package org.keycloak.testsuite.oauth;

import java.io.IOException;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.events.Details;
import org.keycloak.events.EventType;
import org.keycloak.jose.jwk.ECPublicJWK;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.keys.AbstractEddsaKeyProviderFactory;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.protocol.oidc.OIDCConfigAttributes;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.idm.ClientInitialAccessCreatePresentation;
import org.keycloak.representations.idm.ClientInitialAccessPresentation;
import org.keycloak.representations.idm.ClientPoliciesRepresentation;
import org.keycloak.representations.idm.ClientProfilesRepresentation;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.condition.AnyClientConditionFactory;
import org.keycloak.services.clientpolicy.condition.ClientAccessTypeConditionFactory;
import org.keycloak.services.clientpolicy.executor.DPoPBindEnforcerExecutorFactory;
import org.keycloak.services.cors.Cors;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.broker.util.SimpleHttpDefault;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPoliciesBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientPolicyBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfileBuilder;
import org.keycloak.testsuite.util.ClientPoliciesUtil.ClientProfilesBuilder;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.testsuite.util.UserBuilder;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.AuthorizationEndpointResponse;
import org.keycloak.testsuite.util.oauth.IntrospectionResponse;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;
import org.keycloak.util.DPoPGenerator;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;
import org.keycloak.utils.MediaType;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.keycloak.OAuth2Constants.DPOP_HTTP_HEADER;
import static org.keycloak.OAuth2Constants.DPOP_JWT_HEADER_TYPE;
import static org.keycloak.OAuthErrorException.INVALID_TOKEN;
import static org.keycloak.services.util.DPoPUtil.DPOP_SCHEME;
import static org.keycloak.services.util.DPoPUtil.DPOP_TOKEN_TYPE;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createAnyClientConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createClientAccessTypeConditionConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createDPoPBindEnforcerExecutorConfig;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createEcJwk;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.createRsaJwk;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateEcdsaKey;
import static org.keycloak.testsuite.util.ClientPoliciesUtil.generateSignedDPoPProof;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class DPoPTest extends AbstractTestRealmKeycloakTest {

    private static final String REALM_NAME = "test";
    private static final String TEST_CONFIDENTIAL_CLIENT_ID = "test-app";
    private static final String TEST_CONFIDENTIAL_CLIENT_SECRET = "password";
    private static final String TEST_PUBLIC_CLIENT_ID = "test-public-client";
    private static final String TEST_USER_NAME = "test-user@localhost";
    private static final String TEST_USER_PASSWORD = "password";

    @Rule
    public AssertEvents events = new AssertEvents(this);
    private KeyPair ecKeyPair;
    private KeyPair rsaKeyPair;
    private JWK jwkRsa;
    private JWK jwkEc;
    private JWSHeader jwsRsaHeader;
    private JWSHeader jwsEcHeader;
    private String jktRsa;
    private String jktEc;
    private ClientRegistration reg;

    private HttpGet get;

    @Before
    public void beforeDPoPTest() throws Exception {
        rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.MODULUS, ((RSAPublicJWK) jwkRsa).getModulus());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.PUBLIC_EXPONENT, ((RSAPublicJWK) jwkRsa).getPublicExponent());
        jktRsa = JWKSUtils.computeThumbprint(jwkRsa);
        jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);

        ecKeyPair = generateEcdsaKey("secp256r1");
        jwkEc = createEcJwk(ecKeyPair.getPublic());
        jwkEc.getOtherClaims().put(ECPublicJWK.CRV, ((ECPublicJWK) jwkEc).getCrv());
        jwkEc.getOtherClaims().put(ECPublicJWK.X, ((ECPublicJWK) jwkEc).getX());
        jwkEc.getOtherClaims().put(ECPublicJWK.Y, ((ECPublicJWK) jwkEc).getY());
        jktEc = JWKSUtils.computeThumbprint(jwkEc);
        jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);

        changeDPoPBound(TEST_CONFIDENTIAL_CLIENT_ID, true);

        createClientByAdmin(TEST_PUBLIC_CLIENT_ID, (ClientRepresentation rep) -> rep.setPublicClient(Boolean.TRUE));
        changeDPoPBound(TEST_PUBLIC_CLIENT_ID, true);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
        UserBuilder testAdmin = UserBuilder.create()
                .id(KeycloakModelUtils.generateId())
                .username("test-admin@localhost")
                .password("password")
                .role(Constants.REALM_MANAGEMENT_CLIENT_ID, AdminRoles.REALM_ADMIN)
                .addRoles(OAuth2Constants.OFFLINE_ACCESS);
        testRealm.getUsers().add(testAdmin.build());
    }

    @Test
    public void testDuplicatedAuthorizationHeaderOnUserInfo() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        get = new HttpGet(oauth.getEndpoints().getUserInfo());
        get.addHeader("Accept", MediaType.APPLICATION_JSON);
        String authorization = "DPoP" + " " + response.getAccessToken();
        get.addHeader(HttpHeaders.AUTHORIZATION, authorization);
        get.addHeader(HttpHeaders.AUTHORIZATION, authorization);

        UserInfoResponse userInfoResponse = new UserInfoResponse(oauth.httpClient().get().execute(get));

        assertEquals(401, userInfoResponse.getStatusCode());
        assertEquals("HTTP 401 Unauthorized", userInfoResponse.getError());

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testDPoPAccessTokenButBearerAuthorizationHeader() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        get = new HttpGet(oauth.getEndpoints().getUserInfo());
        get.addHeader("Accept", MediaType.APPLICATION_JSON);
        String authorization = "Bearer" + " " + response.getAccessToken();
        get.addHeader(HttpHeaders.AUTHORIZATION, authorization);

        UserInfoResponse userInfoResponse = new UserInfoResponse(oauth.httpClient().get().execute(get));
        assertEquals(401, userInfoResponse.getStatusCode());

        oauth.doLogout(response.getRefreshToken());
    }


    @Test
    public void testDPoPByPublicClientWithDpopJkt() throws Exception {
        // use pre-computed EC key

        int clockSkew = 10; // acceptable clock skew is +-10sec

        sendAuthorizationRequestWithDPoPJkt(jktEc);

        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        successTokenProceduresWithDPoP(dpopProofEcEncoded, jktEc, true, true);
    }

    @Test
    public void testDPoPByPublicClientWithDpopJktWithDifferentDPoPProofKey() throws Exception {
        // use pre-computed EC and RSA key

        int clockSkew = 10; // acceptable clock skew is +-10sec

        sendAuthorizationRequestWithDPoPJkt(jktEc);
    
        // change key : EC key to RSA key
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);

        failureTokenProceduresWithDPoP(dpopProofRsaEncoded, "DPoP Proof public key thumbprint does not match dpop_jkt");
    }

    @Test
    public void testDPoPByPublicClient() throws Exception {
        // use pre-computed EC key

        int clockSkew = 10; // acceptable clock skew is +-10sec

        sendAuthorizationRequestWithDPoPJkt(null);

        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        successTokenProceduresWithDPoP(dpopProofEcEncoded, jktEc, true, true);
    }

    @Test
    public void testDPoPByPublicClientClockSkew() throws Exception {
        getTestingClient().testing().setTestingInfinispanTimeService();
        try {
            sendAuthorizationRequestWithDPoPJkt(null);

            // get a DPoP proof 10 seconds in the future
            String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(),
                    (long) (Time.currentTime() + 10), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

            AccessTokenResponse response = successTokenProceduresWithDPoP(dpopProofEcEncoded, jktEc, true, true, false);

            setTimeOffset(25); // 25 <= 10+10+15, proof not expired because clockSkew, detected by replay check
            response = oauth.refreshRequest(response.getRefreshToken()).dpopProof(dpopProofEcEncoded).send();
            assertEquals(400, response.getStatusCode());
            assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
            assertEquals("DPoP proof has already been used", response.getErrorDescription());

            setTimeOffset(36); // 36 > 10+10+15, proof expired definitely
            response = oauth.refreshRequest(response.getRefreshToken()).dpopProof(dpopProofEcEncoded).send();
            assertEquals(400, response.getStatusCode());
            assertEquals(response.getError(), OAuthErrorException.INVALID_REQUEST);
            assertEquals("DPoP proof is not active", response.getErrorDescription());

            oauth.logoutForm().idTokenHint(response.getIdToken()).open();
        } finally {
            getTestingClient().testing().revertTestingInfinispanTimeService();
        }
    }

    @Test
    public void testDPoPByPublicClientTokenRefreshWithoutDPoPProof() throws Exception {
        // use pre-computed EC key

        int clockSkew = 10; // acceptable clock skew is +-10sec

        sendAuthorizationRequestWithDPoPJkt(null);

        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        changeDPoPBound(TEST_PUBLIC_CLIENT_ID, false); // not enforce DPoP proof, but the refresh token is a DPoP type token.
        failureRefreshTokenProceduresWithoutDPoP(dpopProofEcEncoded, jktEc);
    }

    @Test
    public void testDPoPProofByConfidentialClient() throws Exception {
        // use pre-computed RSA key

        int clockSkew = -10; // acceptable clock skew is +-10sec

        oauth.client(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofRsaEncoded).send();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertEquals(jktRsa, accessToken.getConfirmation().getKeyThumbprint());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        // For confidential client, DPoP is not bind to refresh token (See "section 5 DPoP Access Token Request" of DPoP specification)
        assertNull(refreshToken.getConfirmation());

        TokenMetadataRepresentation tokenMetadataRepresentation = oauth.doIntrospectionRequest(response.getAccessToken(), "access_token").asTokenMetadata();
        Assert.assertTrue(tokenMetadataRepresentation.isActive());
        assertEquals(jktRsa, tokenMetadataRepresentation.getConfirmation().getKeyThumbprint());
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, tokenMetadataRepresentation.getOtherClaims().get(OAuth2Constants.TOKEN_TYPE));

        oauth.tokenRevocationRequest(response.getAccessToken()).accessToken().send();

        tokenMetadataRepresentation = oauth.doIntrospectionRequest(response.getAccessToken(), "access_token").asTokenMetadata();
        Assert.assertFalse(tokenMetadataRepresentation.isActive());

        // token refresh
        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        response = oauth.refreshRequest(response.getRefreshToken()).dpopProof(dpopProofEcEncoded).send();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertEquals(jktEc, accessToken.getConfirmation().getKeyThumbprint());
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertNull(refreshToken.getConfirmation());

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testDPoPProofByConfidentialClient_EdDSA() throws Exception {
        // Generating keys
        String curveName = AbstractEddsaKeyProviderFactory.DEFAULT_EDDSA_ELLIPTIC_CURVE;
        KeyPair keyPair = AbstractEddsaKeyProviderFactory.generateEddsaKeyPair(curveName);

        // JWK
        JWKBuilder b = JWKBuilder.create()
                .algorithm(Algorithm.EdDSA);
        JWK jwkEd = b.okp(keyPair.getPublic(), KeyUse.SIG);

        // Thumbprint
        String jktEd = JWKSUtils.computeThumbprint(jwkEd);

        // Header
        JWSHeader jwsEdHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.valueOf(Algorithm.EdDSA), DPOP_JWT_HEADER_TYPE, jwkEd.getKeyId(), jwkEd);

        int clockSkew = -10; // acceptable clock skew is +-10sec

        oauth.client(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String dpopProofEdEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.EdDSA, jwsEdHeader, keyPair.getPrivate(), null);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofEdEncoded).send();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertEquals(jktEd, accessToken.getConfirmation().getKeyThumbprint());
    }

    @Test
    public void testDPoPDisabledByPublicClient() throws Exception {

        changeDPoPBound(TEST_PUBLIC_CLIENT_ID, false);
        try {
            // with DPoP proof
            testDPoPByPublicClient();

            // without DPoP proof
            oauth.client(TEST_PUBLIC_CLIENT_ID);
            oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

            String code = oauth.parseLoginResponse().getCode();
            AccessTokenResponse response = oauth.doAccessTokenRequest(code);
            // token-type must be "Bearer" because no DPoP is present within the token-request
            assertEquals(TokenUtil.TOKEN_TYPE_BEARER, response.getTokenType());

            assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            assertNull(accessToken.getConfirmation());
            RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
            assertNull(refreshToken.getConfirmation());

            // token refresh
            response = oauth.doRefreshTokenRequest(response.getRefreshToken());
            assertEquals(TokenUtil.TOKEN_TYPE_BEARER, response.getTokenType());

            assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
            accessToken = oauth.verifyToken(response.getAccessToken());
            assertNull(accessToken.getConfirmation());
            refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
            assertNull(refreshToken.getConfirmation());

            oauth.logoutForm().idTokenHint(response.getIdToken()).open();
        } finally {
            changeDPoPBound(TEST_PUBLIC_CLIENT_ID, true);
        }
    }

    @Test
    public void testTokenRefreshWithReplayedDPoPProofByPublicClient() throws Exception {
        oauth.client(TEST_PUBLIC_CLIENT_ID);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofEcEncoded).send();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());

        // token refresh
        response = oauth.refreshRequest(response.getRefreshToken()).dpopProof(dpopProofEcEncoded).send();
        assertNull(response.getTokenType());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("DPoP proof has already been used", response.getErrorDescription());

        oauth.logoutForm().idTokenHint(response.getIdToken()).open();
    }

    @Test
    public void testTokenRefreshWithoutDPoPProofByConfidentialClient() throws Exception {
        oauth.client(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofRsaEncoded).send();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());

        // token refresh
        response = oauth.doRefreshTokenRequest(response.getRefreshToken());
        assertNull(response.getTokenType());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("DPoP proof is missing", response.getErrorDescription());

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testDPoPProofCorsPreflight() {
        Map<String, String> responseHeaders = TokenEndpointCorsTest.getTokenEndpointPreflightResponseHeaders(oauth);
        Set<String> allowedHeaders = Arrays.stream(responseHeaders.get(Cors.ACCESS_CONTROL_ALLOW_HEADERS).split(", ")).collect(Collectors.toSet());

        assertTrue(allowedHeaders.contains(TokenUtil.TOKEN_TYPE_DPOP));
    }

    @Test
    public void testDPoPProofWithoutJwk() throws Exception {
        JWSHeader jwsHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), null);
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(),
                Algorithm.ES256, jwsHeader, ecKeyPair.getPrivate(), null, new TestingDPoPGenerator()), "No JWK in DPoP header");
    }

    @Test
    public void testDPoPProofInvalidAlgorithm() throws Exception {
        JWSHeader jwsHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.none, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsHeader, ecKeyPair.getPrivate(), null), "Unsupported DPoP algorithm: none");
    }

    @Test
    public void testDPoPProofInvalidType() throws Exception {
        JWSHeader jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, "jwt+dpop", jwkEc.getKeyId(), jwkEc);
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null), "Invalid or missing type in DPoP header: jwt+dpop");
    }

    @Test
    public void testDPoPProofInvalidSignature() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(),
                Algorithm.PS256, jwsEcHeader, rsaKeyPair.getPrivate(), null, new TestingDPoPGenerator()), "DPoP verification failure: org.keycloak.exceptions.TokenSignatureInvalidException: Invalid token signature");
    }

    @Test
    public void testDPoPProofMandatoryClaimMissing() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(null, HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null), "DPoP mandatory claims are missing");
    }

    @Test
    public void testDPoPProofReplayed() throws Exception {
        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);

        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofEcEncoded).send();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());
        oauth.doLogout(response.getRefreshToken());

        testDPoPProofFailure(dpopProofEcEncoded, "DPoP proof has already been used");
    }

    @Test
    public void testDPoPProofExpired() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() - 100000), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null), "DPoP proof is not active");
    }

    @Test
    public void testDPoPProofHttpMethodMismatch() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null), "DPoP HTTP method mismatch");
    }

    @Test
    public void testDPoPProofHttpUrlMalformed() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, ":::*;", (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null), "Malformed HTTP URL in DPoP proof");
    }

    @Test
    public void testDPoPProofHttpUrlMismatch() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, "https://server.example.com/token", (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null), "DPoP HTTP URL mismatch");
    }

    @Test
    public void testWithoutDPoPProof() throws Exception {

        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.doAccessTokenRequest(code);

        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("DPoP proof is missing", response.getErrorDescription());
    }

    @Test
    public void testDPoPProofOnUserInfoByConfidentialClient() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);
        doSuccessfulUserInfoGet(response, rsaKeyPair);

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testDPoPProofOnUserInfoWithMissingAcccessTokenHash() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);
        JWK jwkRsa1 = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader1 = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa1.getKeyId(), jwkRsa1);
        // No ath
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getEndpoints().getUserInfo(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader1, rsaKeyPair.getPrivate(), null);
        UserInfoResponse userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(dpopProofRsaEncoded).send();
        assertEquals(401, userInfoResponse.getStatusCode());
    }

    @Test
    public void testDPoPDisabledOnUserInfo() throws Exception {

        changeDPoPBound(TEST_CONFIDENTIAL_CLIENT_ID, false);
        try {
            KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
            AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);
            doSuccessfulUserInfoGet(response, rsaKeyPair);

            UserInfoResponse userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(null).send();
            assertEquals(401, userInfoResponse.getStatusCode());
            testWWWAuthenticateHeaderError(userInfoResponse);

            oauth.doLogout(response.getRefreshToken());
        } finally {
            changeDPoPBound(TEST_CONFIDENTIAL_CLIENT_ID, true);
        }
    }

    @Test
    public void testWithoutDPoPProofOnUserInfo() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        UserInfoResponse userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(null).send();
        assertEquals(401, userInfoResponse.getStatusCode());
        testWWWAuthenticateHeaderError(userInfoResponse);

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testInvalidDPoPProofOnUserInfo() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        // invalid "htu" claim
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), response.getAccessToken());
        UserInfoResponse userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(dpopProofRsaEncoded).send();
        assertEquals(401, userInfoResponse.getStatusCode());
        testWWWAuthenticateHeaderError(userInfoResponse);

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testMultipleUseDPoPProofOnUserInfo() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);
        String dpopProof = doSuccessfulUserInfoGet(response, rsaKeyPair);

        // use the same DPoP proof
        UserInfoResponse userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(dpopProof).send();
        assertEquals(401, userInfoResponse.getStatusCode());
        testWWWAuthenticateHeaderError(userInfoResponse);

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testDifferentKeyDPoPProofOnUserInfo() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        // use different key
        rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getEndpoints().getUserInfo(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), response.getAccessToken());
        UserInfoResponse userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(dpopProofRsaEncoded).send();
        assertEquals(401, userInfoResponse.getStatusCode());
        testWWWAuthenticateHeaderError(userInfoResponse);

        oauth.doLogout(response.getRefreshToken());
    }

    private void testWWWAuthenticateHeaderError(UserInfoResponse userInfoResponse) {
        String wwwAuthenticate = userInfoResponse.getHeaders().get("WWW-Authenticate");
        Assert.assertThat(wwwAuthenticate, startsWith(DPOP_SCHEME));
        String chunks1 = wwwAuthenticate.substring(DPOP_SCHEME.length() + 1);
        Map<String, String> map = new HashMap<>();
        for (String p : chunks1.split(", ")) {
            String[] chunks2 = p.split("=");
            map.put(chunks2[0], chunks2[1]);
        }

        Assert.assertEquals(map.get(OAuth2Constants.ERROR), "\"" + INVALID_TOKEN + "\"");
        String algs = map.get(OAuth2Constants.ALGS_ATTRIBUTE);
        Assert.assertTrue(algs.contains(Algorithm.EdDSA));
        Assert.assertTrue(algs.contains(Algorithm.RS256));
    }

    @Test
    public void testDPoPBindEnforcerExecutor() throws Exception {
        setInitialAccessTokenForDynamicClientRegistration();

        KeyPair ecKeyPair = generateEcdsaKey("secp256r1");
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWK jwkEc = createEcJwk(ecKeyPair.getPublic());

        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile("MyProfile", "Le Premier Profil")
                        .addExecutor(DPoPBindEnforcerExecutorFactory.PROVIDER_ID, createDPoPBindEnforcerExecutorConfig(Boolean.FALSE, Boolean.FALSE, Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy", "La Primera Plitica", Boolean.TRUE)
                        .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID,
                                createClientAccessTypeConditionConfig(List.of(ClientAccessTypeConditionFactory.TYPE_PUBLIC)))
                        .addProfile("MyProfile")
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // register by Admin REST API - fail
        try {
            createClientByAdmin(generateSuffixedName("App-by-Admin"), (ClientRepresentation rep) -> rep.setPublicClient(Boolean.TRUE));
            fail();
        } catch (ClientPolicyException e) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, e.getMessage());
        }

        // register by Admin REST API - success
        String cAppAdminAlphaId = createClientByAdmin(generateSuffixedName("App-by-Admin-Alpha"), (ClientRepresentation clientRep) -> {
            clientRep.setPublicClient(Boolean.TRUE);
            clientRep.setAttributes(new HashMap<>());
            clientRep.getAttributes().put(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, Boolean.TRUE.toString());
        });

        // update by Admin REST API - fail
        try {
            updateClientByAdmin(cAppAdminAlphaId, (ClientRepresentation clientRep) -> clientRep.getAttributes().put(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS, Boolean.FALSE.toString()));
        } catch (ClientPolicyException cpe) {
            assertEquals(OAuthErrorException.INVALID_CLIENT_METADATA, cpe.getError());
        }
        ClientRepresentation cRep = getClientByAdmin(cAppAdminAlphaId);
        assertEquals(Boolean.TRUE.toString(), cRep.getAttributes().get(OIDCConfigAttributes.DPOP_BOUND_ACCESS_TOKENS));
        String appAlphaClientId = cRep.getClientId();

        json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile("MyProfile", "Le Premier Profil")
                        .addExecutor(DPoPBindEnforcerExecutorFactory.PROVIDER_ID, createDPoPBindEnforcerExecutorConfig(Boolean.TRUE, Boolean.FALSE, Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register by Dynamic Client Registration - success
        String cAppDynamicBetaId = createClientDynamically(generateSuffixedName("App-in-Dynamic-Beta"), (OIDCClientRepresentation clientRep) -> {
            clientRep.setTokenEndpointAuthMethod("none");
            clientRep.setDpopBoundAccessTokens(Boolean.FALSE);
        });
        events.expect(EventType.CLIENT_REGISTER).client(cAppDynamicBetaId).user(is(emptyOrNullString())).assertEvent();
        OIDCClientRepresentation oidcClientRep = getClientDynamically(cAppDynamicBetaId);
        assertEquals(Boolean.TRUE, oidcClientRep.getDpopBoundAccessTokens());

        // token request without a DPoP proof - fail
        oauth.client(appAlphaClientId);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        String code = oauth.parseLoginResponse().getCode();

        AccessTokenResponse response = oauth.doAccessTokenRequest(code);
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("DPoP proof is missing", response.getErrorDescription());
        oauth.logoutForm().idTokenHint(response.getIdToken()).open();

        // token request with a valid DPoP proof - success
        // EC key for client alpha
        oauth.openLoginForm();
        code = oauth.parseLoginResponse().getCode();

        JWSHeader jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);
        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        response = oauth.accessTokenRequest(code).dpopProof(dpopProofEcEncoded).send();

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        String encodedAccessToken = response.getAccessToken();
        String encodedRefreshToken = response.getRefreshToken();
        String encodedIdToken = response.getIdToken();
        AccessToken accessToken = oauth.verifyToken(encodedAccessToken);
        jwkEc.getOtherClaims().put(ECPublicJWK.CRV, ((ECPublicJWK) jwkEc).getCrv());
        jwkEc.getOtherClaims().put(ECPublicJWK.X, ((ECPublicJWK) jwkEc).getX());
        jwkEc.getOtherClaims().put(ECPublicJWK.Y, ((ECPublicJWK) jwkEc).getY());
        String jkt = JWKSUtils.computeThumbprint(jwkEc);
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());
        RefreshToken refreshToken = oauth.parseRefreshToken(encodedRefreshToken);
        assertEquals(jkt, refreshToken.getConfirmation().getKeyThumbprint());

        // userinfo request without a DPoP proof - fail
        UserInfoResponse userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(null).send();
        assertEquals(401, userInfoResponse.getStatusCode());

        // userinfo request with a valid DPoP proof - success
        jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getEndpoints().getUserInfo(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), response.getAccessToken());
        userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(dpopProofEcEncoded).send();
        assertEquals(200, userInfoResponse.getStatusCode());
        assertEquals(TEST_USER_NAME, userInfoResponse.getUserInfo().getPreferredUsername());

        // token refresh without a DPoP Proof - fail
        response = oauth.doRefreshTokenRequest(encodedRefreshToken);
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("DPoP proof is missing", response.getErrorDescription());

        // token refresh with a valid DPoP Proof - success
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), response.getIdToken());
        response = oauth.refreshRequest(encodedRefreshToken).dpopProof(dpopProofEcEncoded).send();
        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        encodedAccessToken = response.getAccessToken();
        encodedRefreshToken = response.getRefreshToken();
        accessToken = oauth.verifyToken(encodedAccessToken);
        jwkEc.getOtherClaims().put(ECPublicJWK.CRV, ((ECPublicJWK) jwkEc).getCrv());
        jwkEc.getOtherClaims().put(ECPublicJWK.X, ((ECPublicJWK) jwkEc).getX());
        jwkEc.getOtherClaims().put(ECPublicJWK.Y, ((ECPublicJWK) jwkEc).getY());
        jkt = JWKSUtils.computeThumbprint(jwkEc);
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());
        refreshToken = oauth.parseRefreshToken(encodedRefreshToken);
        assertEquals(jkt, refreshToken.getConfirmation().getKeyThumbprint());

        // revoke token without a valid DPoP proof - fail
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getRevocation(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), response.getAccessToken());
        assertEquals(400, oauth.tokenRevocationRequest(encodedAccessToken).accessToken().dpopProof(dpopProofRsaEncoded).send().getStatusCode());

        // revoke token with a valid DPoP proof - success
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getRevocation(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), response.getAccessToken());
        assertTrue(oauth.tokenRevocationRequest(encodedAccessToken).accessToken().dpopProof(dpopProofEcEncoded).send().isSuccess());
        IntrospectionResponse introspectionResponse = oauth.doIntrospectionAccessTokenRequest(encodedAccessToken);
        assertFalse(introspectionResponse.isSuccess());
        assertEquals("Client not allowed.", introspectionResponse.getErrorDescription());

        updatePolicies("{}");
        updateProfiles("{}");

        oauth.logoutForm().idTokenHint(encodedIdToken).open();
    }

    @Test
    public void testDPoPBindEnforcerExecutorWithEnforcedAuthzCodeBinding() throws Exception {
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile("MyProfile", "Le Premier Profil")
                        .addExecutor(DPoPBindEnforcerExecutorFactory.PROVIDER_ID, createDPoPBindEnforcerExecutorConfig(Boolean.FALSE, Boolean.TRUE, Boolean.FALSE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy", "La Primera Plitica", Boolean.TRUE)
                        .addCondition(ClientAccessTypeConditionFactory.PROVIDER_ID,
                                createClientAccessTypeConditionConfig(List.of(ClientAccessTypeConditionFactory.TYPE_PUBLIC)))
                        .addProfile("MyProfile")
                        .toRepresentation()
        ).toString();
        updatePolicies(json);

        // Login without dpop_jkt - failure
        oauth.client(TEST_PUBLIC_CLIENT_ID);
        oauth.openLoginForm();
        AuthorizationEndpointResponse response = oauth.parseLoginResponse();
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals("Missing parameter: dpop_jkt", response.getErrorDescription());
        events.expectClientPolicyError(EventType.LOGIN_ERROR, OAuthErrorException.INVALID_REQUEST,
                        Details.CLIENT_POLICY_ERROR, OAuthErrorException.INVALID_REQUEST,
                        "Missing parameter: dpop_jkt").client(oauth.getClientId()).user((String) null)
                .assertEvent();

        // Login with dpop_jkt -- should be OK
        long clockSkew = 10;
        sendAuthorizationRequestWithDPoPJkt(jktEc);
        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        successTokenProceduresWithDPoP(dpopProofEcEncoded, jktEc, true, true);

        updatePolicies("{}");
        updateProfiles("{}");
    }

    @Test
    public void testBindOnlyRefreshTokenDPoPEnforcerExecutor() throws Exception {
        changeDPoPBound(TEST_PUBLIC_CLIENT_ID, false);
        changeDPoPBound(TEST_CONFIDENTIAL_CLIENT_ID, false);
        // register profiles
        String json = (new ClientProfilesBuilder()).addProfile(
                (new ClientProfileBuilder()).createProfile("MyProfile", "Profile")
                        .addExecutor(DPoPBindEnforcerExecutorFactory.PROVIDER_ID, createDPoPBindEnforcerExecutorConfig(Boolean.FALSE, Boolean.FALSE, Boolean.TRUE))
                        .toRepresentation()
        ).toString();
        updateProfiles(json);

        // register policies
        json = (new ClientPoliciesBuilder()).addPolicy(
                (new ClientPolicyBuilder()).createPolicy("MyPolicy", "Policy", Boolean.TRUE)
                        .addCondition(AnyClientConditionFactory.PROVIDER_ID, createAnyClientConditionConfig())
                        .addProfile("MyProfile")
                        .toRepresentation()
        ).toString();
        updatePolicies(json);
        int clockSkew = 10; // acceptable clock skew is +-10sec

        //public client without proof
        sendAuthorizationRequestWithDPoPJkt(null);
        failureTokenProceduresWithDPoP(null, "DPoP proof is missing");
        oauth.getDriver().manage().deleteAllCookies();

        //public client with proof
        sendAuthorizationRequestWithDPoPJkt(null);
        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        successTokenProceduresWithDPoP(dpopProofEcEncoded, jktEc, false, true);

        //confidential client without proof
        oauth.client(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        successTokenProceduresWithDPoP(null, jktEc, false, false);

        //confidential client with proof
        oauth.client(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) (Time.currentTime() + clockSkew), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        successTokenProceduresWithDPoP(dpopProofEcEncoded, jktEc, true, false);

        updatePolicies("{}");
        updateProfiles("{}");
        changeDPoPBound(TEST_PUBLIC_CLIENT_ID, true);
        changeDPoPBound(TEST_CONFIDENTIAL_CLIENT_ID, true);
    }

    @Test
    public void testDPoPProofWithClientCredentialsGrant() throws Exception {
        modifyClient(TEST_CONFIDENTIAL_CLIENT_ID, (clientRepresentation, configWrapper) -> {
            clientRepresentation.setServiceAccountsEnabled(true);
            configWrapper.setUseDPoP(true);
        });
        oauth.client(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);

        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);

        AccessTokenResponse response = oauth.clientCredentialsGrantRequest().dpopProof(dpopProofRsaEncoded).send();
        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

        jwkRsa.getOtherClaims().put(RSAPublicJWK.MODULUS, ((RSAPublicJWK) jwkRsa).getModulus());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.PUBLIC_EXPONENT, ((RSAPublicJWK) jwkRsa).getPublicExponent());
        String jkt = JWKSUtils.computeThumbprint(jwkRsa);
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testDPoPProofWithResourceOwnerPasswordCredentialsGrant() throws Exception {
        modifyClient(TEST_CONFIDENTIAL_CLIENT_ID, (clientRepresentation, configWrapper) -> {
            clientRepresentation.setDirectAccessGrantsEnabled(true);
            configWrapper.setUseDPoP(true);
        });
        oauth.client(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);

        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);

        AccessTokenResponse response = oauth.passwordGrantRequest(TEST_USER_NAME, TEST_USER_PASSWORD).dpopProof(dpopProofRsaEncoded).send();
        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());

        jwkRsa.getOtherClaims().put(RSAPublicJWK.MODULUS, ((RSAPublicJWK) jwkRsa).getModulus());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.PUBLIC_EXPONENT, ((RSAPublicJWK) jwkRsa).getPublicExponent());
        String jkt = JWKSUtils.computeThumbprint(jwkRsa);
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testDPoPAdminRequestSuccess() throws Exception {
        modifyClient(TEST_CONFIDENTIAL_CLIENT_ID, (clientRepresentation, configWrapper) -> {
            clientRepresentation.setDirectAccessGrantsEnabled(true);
            configWrapper.setUseDPoP(true);
        });

        try (Keycloak adminClientDPoP = AdminClientUtil.createAdminClient(false, ServerURLs.getAuthServerContextRoot(), REALM_NAME,
                "test-admin@localhost", "password", TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET, null, true);
        ) {
            RealmRepresentation realm = adminClientDPoP.realm(REALM_NAME).toRepresentation();
            Assert.assertEquals(REALM_NAME, realm.getRealm());

            // To enforce token refresh by admin client in the next request
            setTimeOffset(700);

            realm = adminClientDPoP.realm(REALM_NAME).toRepresentation();
            Assert.assertEquals(REALM_NAME, realm.getRealm());
        }
    }

    @Test
    public void testDPoPAdminRequestFailure() throws Exception {
        modifyClient(TEST_CONFIDENTIAL_CLIENT_ID, (clientRepresentation, configWrapper) -> {
            clientRepresentation.setDirectAccessGrantsEnabled(true);
            configWrapper.setUseDPoP(true);
        });

        try (Keycloak adminClientDPoP = AdminClientUtil.createAdminClient(false, ServerURLs.getAuthServerContextRoot(), REALM_NAME,
                "test-admin@localhost", "password", TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET, null, false);
        ) {
            adminClientDPoP.realm(REALM_NAME).toRepresentation();
            Assert.fail("Expected exception when calling adminClient without DPoP for the client, which requires DPoP");
        } catch (ProcessingException pe) {
            Assert.assertTrue(pe.getCause() instanceof BadRequestException);
        }
    }

    @Test
    public void testDPoPAccountRequestSuccess() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            // Valid DPoP proof for the access-token
            JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
            JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
            String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, getAccountRootUrl(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), response.getAccessToken());

            int status = SimpleHttpDefault.doGet(getAccountRootUrl(), httpClient).header("Accept", "application/json")
                    .header("Authorization", DPOP_TOKEN_TYPE + " " + response.getAccessToken())
                    .header(DPOP_HTTP_HEADER, dpopProofRsaEncoded)
                    .asStatus();
            assertEquals(200, status);
        }

        oauth.doLogout(response.getRefreshToken());
    }

    @Test
    public void testDPoPAccountRequestFailures() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
            // Request with DPoP accessToken and with "Authorization: Bearer" header should fail
            int status = SimpleHttpDefault.doGet(getAccountRootUrl(), httpClient).header("Accept", "application/json")
                    .auth(response.getAccessToken())
                    .asStatus();
            assertEquals(401, status);

            // Request with DPoP accessToken and with "Authorization: DPoP" header should fail
            status = SimpleHttpDefault.doGet(getAccountRootUrl(), httpClient).header("Accept", "application/json")
                    .header("Authorization", DPOP_TOKEN_TYPE + " " + response.getAccessToken())
                    .asStatus();
            assertEquals(401, status);

            // Invalid DPoP proof for the access-token (Request URL is userInfo instead of getAccountRootUrl()
            JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
            JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
            String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getEndpoints().getUserInfo(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), response.getAccessToken());

            status = SimpleHttpDefault.doGet(getAccountRootUrl(), httpClient).header("Accept", "application/json")
                    .header("Authorization", DPOP_TOKEN_TYPE + " " + response.getAccessToken())
                    .header(DPOP_HTTP_HEADER, dpopProofRsaEncoded)
                    .asStatus();
            assertEquals(401, status);
        }

        oauth.doLogout(response.getRefreshToken());
    }

    private AccessTokenResponse getDPoPBindAccessToken(KeyPair rsaKeyPair) throws Exception {
        oauth.client(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), null);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofRsaEncoded).send();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.MODULUS, ((RSAPublicJWK) jwkRsa).getModulus());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.PUBLIC_EXPONENT, ((RSAPublicJWK) jwkRsa).getPublicExponent());
        String jkt = JWKSUtils.computeThumbprint(jwkRsa);
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());

        return response;
    }

    private String doSuccessfulUserInfoGet(AccessTokenResponse accessTokenResponse, KeyPair rsaKeyPair) throws Exception {
        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getEndpoints().getUserInfo(), (long) Time.currentTime(), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate(), accessTokenResponse.getAccessToken());
        UserInfoResponse userInfoResponse = oauth.userInfoRequest(accessTokenResponse.getAccessToken()).dpop(dpopProofRsaEncoded).send();
        assertEquals(TEST_USER_NAME, userInfoResponse.getUserInfo().getPreferredUsername());
        return dpopProofRsaEncoded;
    }

    private void testDPoPProofFailure(String dpopProofEncoded, String errorDescription) throws Exception {
        oauth.client(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofEncoded).send();

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_REQUEST, response.getError());
        assertEquals(errorDescription, response.getErrorDescription());
    }

    private void changeDPoPBound(String clientId, boolean isEnabled) {
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseDPoP(isEnabled);
        clientResource.update(clientRep);
    }

    private void modifyClient(String clientId, BiConsumer<ClientRepresentation, OIDCAdvancedConfigWrapper> modify) {
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper configWrapper = OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep);
        modify.accept(clientRep, configWrapper);
        clientResource.update(clientRep);
    }

    private String createClientByAdmin(String clientName, Consumer<ClientRepresentation> op) throws ClientPolicyException {
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

    private void updateProfiles(String json) throws ClientPolicyException {
        try {
            ClientProfilesRepresentation clientProfiles = JsonSerialization.readValue(json, ClientProfilesRepresentation.class);
            adminClient.realm(REALM_NAME).clientPoliciesProfilesResource().updateProfiles(clientProfiles);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update profiles failed", e.getResponse().getStatusInfo().toString());
        } catch (Exception e) {
            throw new ClientPolicyException("update profiles failed", e.getMessage());
        }
    }

    private void updatePolicies(String json) throws ClientPolicyException {
        try {
            ClientPoliciesRepresentation clientPolicies = json == null ? null : JsonSerialization.readValue(json, ClientPoliciesRepresentation.class);
            adminClient.realm(REALM_NAME).clientPoliciesPoliciesResource().updatePolicies(clientPolicies);
        } catch (BadRequestException e) {
            throw new ClientPolicyException("update policies failed", e.getResponse().getStatusInfo().toString());
        } catch (IOException e) {
            throw new ClientPolicyException("update policies failed", e.getMessage());
        }
    }

    private String generateSuffixedName(String name) {
        return name + "-" + UUID.randomUUID().toString().subSequence(0, 7);
    }

    private void updateClientByAdmin(String cId, Consumer<ClientRepresentation> op) throws ClientPolicyException {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        op.accept(clientRep);
        try {
            clientResource.update(clientRep);
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

    private ClientRepresentation getClientByAdmin(String cId) throws ClientPolicyException {
        ClientResource clientResource = adminClient.realm(REALM_NAME).clients().get(cId);
        try {
            return clientResource.toRepresentation();
        } catch (BadRequestException bre) {
            processClientPolicyExceptionByAdmin(bre);
        }
        return null;
    }

    private String createClientDynamically(String clientName, Consumer<OIDCClientRepresentation> op) throws ClientRegistrationException {
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

    private void setInitialAccessTokenForDynamicClientRegistration() {
        // get initial access token for Dynamic Client Registration with authentication
        reg = ClientRegistration.create().url(suiteContext.getAuthServerInfo().getContextRoot() + "/auth", REALM_NAME).build();
        ClientInitialAccessPresentation token = adminClient.realm(REALM_NAME).clientInitialAccess().create(new ClientInitialAccessCreatePresentation(0, 10));
        reg.auth(Auth.token(token));
    }

    private OIDCClientRepresentation getClientDynamically(String clientId) throws ClientRegistrationException {
        return reg.oidc().get(clientId);
    }


    private void sendAuthorizationRequestWithDPoPJkt(String dpopJkt) {
        oauth.client(TEST_PUBLIC_CLIENT_ID);
        oauth.loginForm().dpopJkt(dpopJkt).doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
    }

    private AccessTokenResponse successTokenProceduresWithDPoP(String dpopProofEncoded, String jkt, boolean accessTokenBound, boolean refreshTokenBound) throws Exception {
        return successTokenProceduresWithDPoP(dpopProofEncoded, jkt, accessTokenBound, refreshTokenBound, true);
    }

    private AccessTokenResponse successTokenProceduresWithDPoP(String dpopProofEncoded, String jkt, boolean accessTokenBound,
            boolean refreshTokenBound, boolean performLogout) throws Exception {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofEncoded).send();
        assertEquals(accessTokenBound ? TokenUtil.TOKEN_TYPE_DPOP : TokenUtil.TOKEN_TYPE_BEARER, response.getTokenType());
        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        if (accessTokenBound) {
            assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());
        }
        else {
            assertNull(accessToken.getConfirmation());
        }
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        if (refreshTokenBound) {
            assertEquals(jkt, refreshToken.getConfirmation().getKeyThumbprint());
        }
        else {
            assertNull(refreshToken.getConfirmation());
        }

        // token refresh
        if (dpopProofEncoded != null) {
            dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST, oauth.getEndpoints().getToken(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), null);
        }
        response = oauth.refreshRequest(response.getRefreshToken()).dpopProof(dpopProofEncoded).send();
        assertEquals(accessTokenBound ? TokenUtil.TOKEN_TYPE_DPOP : TokenUtil.TOKEN_TYPE_BEARER, response.getTokenType());

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        accessToken = oauth.verifyToken(response.getAccessToken());
        if (accessTokenBound) {
            assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());
        }
        else {
            assertNull(accessToken.getConfirmation());
        }
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        if (refreshTokenBound) {
            assertEquals(jkt, refreshToken.getConfirmation().getKeyThumbprint());
        }
        else {
            assertNull(refreshToken.getConfirmation());
        }

        if (accessTokenBound) {
            // userinfo access
            dpopProofEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET, oauth.getEndpoints().getUserInfo(), (long) Time.currentTime(), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate(), response.getAccessToken());
            UserInfoResponse userInfoResponse = oauth.userInfoRequest(response.getAccessToken()).dpop(dpopProofEncoded).send();
            assertEquals(TEST_USER_NAME, userInfoResponse.getUserInfo().getPreferredUsername());
        }

        // logout
        if (performLogout) {
            oauth.logoutForm().idTokenHint(response.getIdToken()).open();
        }
        return response;
    }

    private void failureRefreshTokenProceduresWithoutDPoP(String dpopProofEncoded, String jkt) throws Exception {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofEncoded).send();
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, response.getTokenType());
        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertEquals(jkt, refreshToken.getConfirmation().getKeyThumbprint());

        // token refresh without DPoP Proof
        response = oauth.refreshRequest(response.getRefreshToken()).dpopProof(null).send();
        assertEquals(400, response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_GRANT, response.getError());
        assertEquals("DPoP proof is missing", response.getErrorDescription());

        // logout
        oauth.logoutForm().idTokenHint(response.getIdToken()).open();
    }

    private void failureTokenProceduresWithDPoP(String dpopProofEncoded, String error) throws Exception {
        String code = oauth.parseLoginResponse().getCode();
        AccessTokenResponse response = oauth.accessTokenRequest(code).dpopProof(dpopProofEncoded).send();
        assertEquals(400, response.getStatusCode());
        assertEquals(error, response.getErrorDescription());
        oauth.logoutForm().idTokenHint(response.getIdToken()).open();
    }

    // DPoPGenerator with the ability to inject KeyWrapper. Useful for testing purposes of failure scenarios (EG. when different algorithm is used for JWS and for the underlying key etc)
    private class TestingDPoPGenerator extends DPoPGenerator {

        @Override
        protected KeyWrapper getKeyWrapper(JWSHeader jwsHeader, PrivateKey privateKey) {
            KeyWrapper keyWrapper = new KeyWrapper();
            keyWrapper.setKid(jwsHeader.getKeyId());
            keyWrapper.setAlgorithm(jwsHeader.getAlgorithm().toString());
            keyWrapper.setPrivateKey(privateKey);
            keyWrapper.setType(privateKey.getAlgorithm());
            keyWrapper.setUse(KeyUse.SIG);
            return keyWrapper;
        }

    }
}
