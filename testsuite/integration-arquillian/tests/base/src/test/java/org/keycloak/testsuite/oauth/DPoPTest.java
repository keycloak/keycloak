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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.keycloak.jose.jwk.JWKUtil.toIntegerBytes;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.OAuthErrorException;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.common.Profile;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.jose.jwk.ECPublicJWK;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.protocol.oidc.OIDCAdvancedConfigWrapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.UserInfo;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.oidc.TokenMetadataRepresentation;
import org.keycloak.services.resources.Cors;
import org.keycloak.testsuite.AbstractTestRealmKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.AssertEvents;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.OAuthClient.UserInfoResponse;
import org.keycloak.testsuite.util.ServerURLs;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.util.TokenUtil;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.HttpMethod;

@EnableFeature(value = Profile.Feature.DPOP, skipRestart = true)
public class DPoPTest extends AbstractTestRealmKeycloakTest {

    private static final String REALM_NAME = "test";
    private static final String TEST_CONFIDENTIAL_CLIENT_ID = "test-app";
    private static final String TEST_CONFIDENTIAL_CLIENT_SECRET = "password";
    private static final String TEST_PUBLIC_CLIENT_ID = "test-public-client";
    private static final String TEST_USER_NAME = "test-user@localhost";
    private static final String TEST_USER_PASSWORD = "password";
    private static final String DPOP_JWT_HEADER_TYPE = "dpop+jwt";

    private KeyPair ecKeyPair;
    private KeyPair rsaKeyPair;
    private JWK jwkRsa;
    private JWK jwkEc;
    private JWSHeader jwsRsaHeader;
    private JWSHeader jwsEcHeader;

    @Rule
    public AssertEvents events = new AssertEvents(this);

    @Before
    public void beforeDPoPTest() throws Exception {
        ecKeyPair = generateEcdsaKey("secp256r1");
        rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        jwkEc = createEcJwk(ecKeyPair.getPublic());
        jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);

        changeDPoPBound(TEST_CONFIDENTIAL_CLIENT_ID, true);

        createClientByAdmin(TEST_PUBLIC_CLIENT_ID, (ClientRepresentation rep) -> {
            rep.setPublicClient(Boolean.TRUE);
        });
        changeDPoPBound(TEST_PUBLIC_CLIENT_ID, true);
    }

    @Override
    public void configureTestRealm(RealmRepresentation testRealm) {
    }

    @Test
    public void testDPoPByPublicClient() throws Exception {

        oauth.clientId(TEST_PUBLIC_CLIENT_ID);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);
        JWSHeader jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);
        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate());

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.dpopProof(dpopProofEcEncoded);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        jwkEc.getOtherClaims().put(ECPublicJWK.CRV, ((ECPublicJWK)jwkEc).getCrv());
        jwkEc.getOtherClaims().put(ECPublicJWK.X, ((ECPublicJWK)jwkEc).getX());
        jwkEc.getOtherClaims().put(ECPublicJWK.Y, ((ECPublicJWK)jwkEc).getY());
        String jkt = JWKSUtils.computeThumbprint(jwkEc);
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertEquals(jkt, refreshToken.getConfirmation().getKeyThumbprint());

        // token refresh
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate());

        oauth.dpopProof(dpopProofEcEncoded);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), null);

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertEquals(jkt, refreshToken.getConfirmation().getKeyThumbprint());

        // userinfo access
        dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET.toString(), oauth.getUserInfoUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate());
        oauth.dpopProof(dpopProofEcEncoded);
        UserInfoResponse userInfoResponse = oauth.doUserInfoRequestByGet(response.getAccessToken());
        assertEquals(TEST_USER_NAME, userInfoResponse.getUserInfo().getPreferredUsername());

        oauth.idTokenHint(response.getIdToken()).openLogout();
    }

    @Test
    public void testDPoPProofByConfidentialClient() throws Exception {

        oauth.clientId(TEST_CONFIDENTIAL_CLIENT_ID);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);

        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate());

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.dpopProof(dpopProofRsaEncoded);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, TEST_CONFIDENTIAL_CLIENT_SECRET);

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.MODULUS, ((RSAPublicJWK)jwkRsa).getModulus());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.PUBLIC_EXPONENT, ((RSAPublicJWK)jwkRsa).getPublicExponent());
        String jkt = JWKSUtils.computeThumbprint(jwkRsa);
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());
        RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        // For confidential client, DPoP is not bind to refresh token (See "section 5 DPoP Access Token Request" of DPoP specification)
        assertNull(refreshToken.getConfirmation());

        String tokenResponse = oauth.introspectTokenWithClientCredential(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET, "access_token", response.getAccessToken());
        Assert.assertNotNull(tokenResponse);
        TokenMetadataRepresentation tokenMetadataRepresentation = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);
        Assert.assertTrue(tokenMetadataRepresentation.isActive());
        assertEquals(jkt, tokenMetadataRepresentation.getConfirmation().getKeyThumbprint());
        assertEquals(TokenUtil.TOKEN_TYPE_DPOP, tokenMetadataRepresentation.getOtherClaims().get(OAuth2Constants.TOKEN_TYPE));

        CloseableHttpResponse closableHttpResponse = oauth.doTokenRevoke(response.getAccessToken(), "access_token", TEST_CONFIDENTIAL_CLIENT_SECRET);
        tokenResponse = oauth.introspectTokenWithClientCredential(TEST_CONFIDENTIAL_CLIENT_ID, TEST_CONFIDENTIAL_CLIENT_SECRET, "access_token", response.getAccessToken());
        Assert.assertNotNull(tokenResponse);
        tokenMetadataRepresentation = JsonSerialization.readValue(tokenResponse, TokenMetadataRepresentation.class);
        Assert.assertFalse(tokenMetadataRepresentation.isActive());
        closableHttpResponse.close();

        // token refresh
        rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate());

        oauth.dpopProof(dpopProofRsaEncoded);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.MODULUS, ((RSAPublicJWK)jwkRsa).getModulus());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.PUBLIC_EXPONENT, ((RSAPublicJWK)jwkRsa).getPublicExponent());
        jkt = JWKSUtils.computeThumbprint(jwkRsa);
        accessToken = oauth.verifyToken(response.getAccessToken());
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());
        refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
        assertEquals(null, refreshToken.getConfirmation());

        oauth.doLogout(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);
    }

    @Test
    public void testDPoPDisabledByPublicClient() throws Exception {

        changeDPoPBound(TEST_PUBLIC_CLIENT_ID, false);
        try {
            // with DPoP proof
            testDPoPByPublicClient();

            // without DPoP proof
            oauth.clientId(TEST_PUBLIC_CLIENT_ID);
            oauth.dpopProof(null);
            oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

            String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
            OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);

            assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
            AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
            assertEquals(null, accessToken.getConfirmation());
            RefreshToken refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
            assertEquals(null, refreshToken.getConfirmation());

            // token refresh
            response = oauth.doRefreshTokenRequest(response.getRefreshToken(), null);

            assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
            accessToken = oauth.verifyToken(response.getAccessToken());
            assertEquals(null, accessToken.getConfirmation());
            refreshToken = oauth.parseRefreshToken(response.getRefreshToken());
            assertEquals(null, refreshToken.getConfirmation());

            oauth.idTokenHint(response.getIdToken()).openLogout();
        } finally {
            changeDPoPBound(TEST_PUBLIC_CLIENT_ID, true);
        }
    }

    @Test
    public void testTokenRefreshWithReplayedDPoPProofByPublicClient() throws Exception {

        oauth.clientId(TEST_PUBLIC_CLIENT_ID);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate());

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.dpopProof(dpopProofEcEncoded);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, null);

        // token refresh
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), null);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_DPOP_PROOF, response.getError());
        assertEquals("DPoP proof has already been used", response.getErrorDescription());

        oauth.idTokenHint(response.getIdToken()).openLogout();
    }

    @Test
    public void testTokenRefreshWithoutDPoPProofByConfidentialClient() throws Exception {

        oauth.clientId(TEST_CONFIDENTIAL_CLIENT_ID);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate());

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.dpopProof(dpopProofRsaEncoded);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, TEST_USER_PASSWORD);

        // token refresh
        oauth.dpopProof(null);
        response = oauth.doRefreshTokenRequest(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);
        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_DPOP_PROOF, response.getError());
        assertEquals("DPoP proof is missing", response.getErrorDescription());

        oauth.doLogout(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);
    }

    @Test
    public void testDPoPProofCorsPreflight() throws Exception {
        CloseableHttpResponse response = oauth.doPreflightRequest();

        String[] headers = response.getHeaders(Cors.ACCESS_CONTROL_ALLOW_HEADERS)[0].getValue().split(", ");
        Set<String> allowedHeaders = new HashSet<String>(Arrays.asList(headers));

        assertTrue(allowedHeaders.contains(TokenUtil.TOKEN_TYPE_DPOP));
    }

    @Test
    public void testDPoPProofWithoutJwk() throws Exception {
        JWSHeader jwsHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), (JWK)null);
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsHeader, ecKeyPair.getPrivate()), "No JWK in DPoP header");
    }

    @Test
    public void testDPoPProofInvalidAlgorithm() throws Exception {
        JWSHeader jwsHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.none, DPOP_JWT_HEADER_TYPE, jwkEc.getKeyId(), jwkEc);
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsHeader, ecKeyPair.getPrivate()), "Unsupported DPoP algorithm: none");
    }

    @Test
    public void testDPoPProofInvalidType() throws Exception {
        JWSHeader jwsEcHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.ES256, "jwt+dpop", jwkEc.getKeyId(), jwkEc);
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate()), "Invalid or missing type in DPoP header: jwt+dpop");
    }

    @Test
    public void testDPoPProofInvalidSignature() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.PS256, jwsEcHeader, rsaKeyPair.getPrivate()), "DPoP verification failure");
    }

    @Test
    public void testDPoPProofMandatoryClaimMissing() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(null, HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate()), "DPoP mandatory claims are missing");
    }

    @Test
    public void testDPoPProofReplayed() throws Exception {

        String dpopProofEcEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate());

        oauth.dpopProof(dpopProofEcEncoded);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, TEST_USER_PASSWORD);
        oauth.doLogout(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);

        testDPoPProofFailure(dpopProofEcEncoded, "DPoP proof has already been used");
    }

    @Test
    public void testDPoPProofExpired() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime() - 100000), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate()), "DPoP proof is not active");
    }

    @Test
    public void testDPoPProofHttpMethodMismatch() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate()), "DPoP HTTP method mismatch");
    }

    @Test
    public void testDPoPProofHttpUrlMalformed() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), ":::*;", Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate()), "Malformed HTTP URL in DPoP proof");
    }

    @Test
    public void testDPoPProofHttpUrlMismatch() throws Exception {
        testDPoPProofFailure(generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), "https://server.example.com/token", Long.valueOf(Time.currentTime()), Algorithm.ES256, jwsEcHeader, ecKeyPair.getPrivate()), "DPoP HTTP URL mismatch");
    }

    @Test
    public void testWithoutDPoPProof() throws Exception {

        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, TEST_USER_PASSWORD);

        assertEquals(OAuthErrorException.INVALID_DPOP_PROOF, response.getError());
        assertEquals("DPoP proof is missing", response.getErrorDescription());
    }

    @Test
    public void testDPoPProofOnUserInfoByConfidentialClient() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        OAuthClient.AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);
        doSuccessfulUserInfoGet(response.getAccessToken(), rsaKeyPair);

        oauth.doLogout(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);
    }

    @Test
    public void testDPoPDisabledOnUserInfo() throws Exception {

        changeDPoPBound(TEST_CONFIDENTIAL_CLIENT_ID, false);
        try {
            KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
            OAuthClient.AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);
            doSuccessfulUserInfoGet(response.getAccessToken(), rsaKeyPair);

            // delete DPoP proof
            oauth.dpopProof(null);
            UserInfoResponse userInfoResponse = oauth.doUserInfoRequestByGet(response.getAccessToken());
            assertEquals(401, userInfoResponse.getStatusCode());
            assertEquals("Bearer realm=\"test\", error=\"invalid_token\", error_description=\"DPoP proof and token binding verification failed\"", userInfoResponse.getHeaders().get("WWW-Authenticate"));

            oauth.doLogout(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);
        } finally {
            changeDPoPBound(TEST_CONFIDENTIAL_CLIENT_ID, true);
        }
    }

    @Test
    public void testWithoutDPoPProofOnUserInfo() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        OAuthClient.AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        oauth.dpopProof(null);
        UserInfoResponse userInfoResponse = oauth.doUserInfoRequestByGet(response.getAccessToken());
        assertEquals(401, userInfoResponse.getStatusCode());
        assertEquals("Bearer realm=\"test\", error=\"invalid_token\", error_description=\"DPoP proof and token binding verification failed\"", userInfoResponse.getHeaders().get("WWW-Authenticate"));

        oauth.doLogout(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);
    }

    @Test
    public void testInvalidDPoPProofOnUserInfo() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        OAuthClient.AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        // invalid "htu" claim
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate());
        oauth.dpopProof(dpopProofRsaEncoded);
        UserInfoResponse userInfoResponse = oauth.doUserInfoRequestByGet(response.getAccessToken());
        assertEquals(401, userInfoResponse.getStatusCode());
        assertEquals("Bearer realm=\"test\", error=\"invalid_token\", error_description=\"DPoP proof and token binding verification failed\"", userInfoResponse.getHeaders().get("WWW-Authenticate"));

        oauth.doLogout(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);
    }

    @Test
    public void testMultipleUseDPoPProofOnUserInfo() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        OAuthClient.AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);
        doSuccessfulUserInfoGet(response.getAccessToken(), rsaKeyPair);

        // use the same DPoP proof
        UserInfoResponse userInfoResponse = oauth.doUserInfoRequestByGet(response.getAccessToken());
        assertEquals(401, userInfoResponse.getStatusCode());
        assertEquals("Bearer realm=\"test\", error=\"invalid_token\", error_description=\"DPoP proof and token binding verification failed\"", userInfoResponse.getHeaders().get("WWW-Authenticate"));

        oauth.doLogout(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);
    }

    @Test
    public void testDifferentKeyDPoPProofOnUserInfo() throws Exception {
        KeyPair rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        OAuthClient.AccessTokenResponse response = getDPoPBindAccessToken(rsaKeyPair);

        // use different key
        rsaKeyPair = KeyUtils.generateRsaKeyPair(2048);
        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET.toString(), oauth.getUserInfoUrl(), Long.valueOf(Time.currentTime()), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate());
        oauth.dpopProof(dpopProofRsaEncoded);
        UserInfoResponse userInfoResponse = oauth.doUserInfoRequestByGet(response.getAccessToken());
        assertEquals(401, userInfoResponse.getStatusCode());
        assertEquals("Bearer realm=\"test\", error=\"invalid_token\", error_description=\"DPoP proof and token binding verification failed\"", userInfoResponse.getHeaders().get("WWW-Authenticate"));

        oauth.doLogout(response.getRefreshToken(), TEST_CONFIDENTIAL_CLIENT_SECRET);
    }

    private OAuthClient.AccessTokenResponse getDPoPBindAccessToken(KeyPair rsaKeyPair) throws Exception {
        oauth.clientId(TEST_CONFIDENTIAL_CLIENT_ID);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.POST.toString(), oauth.getAccessTokenUrl(), Long.valueOf(Time.currentTime()), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate());

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        oauth.dpopProof(dpopProofRsaEncoded);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, TEST_CONFIDENTIAL_CLIENT_SECRET);

        assertEquals(Status.OK.getStatusCode(), response.getStatusCode());
        AccessToken accessToken = oauth.verifyToken(response.getAccessToken());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.MODULUS, ((RSAPublicJWK)jwkRsa).getModulus());
        jwkRsa.getOtherClaims().put(RSAPublicJWK.PUBLIC_EXPONENT, ((RSAPublicJWK)jwkRsa).getPublicExponent());
        String jkt = JWKSUtils.computeThumbprint(jwkRsa);
        assertEquals(jkt, accessToken.getConfirmation().getKeyThumbprint());

        return response;
    }

    private void doSuccessfulUserInfoGet(String accessToken, KeyPair rsaKeyPair) throws Exception {
        JWK jwkRsa = createRsaJwk(rsaKeyPair.getPublic());
        JWSHeader jwsRsaHeader = new JWSHeader(org.keycloak.jose.jws.Algorithm.PS256, DPOP_JWT_HEADER_TYPE, jwkRsa.getKeyId(), jwkRsa);
        String dpopProofRsaEncoded = generateSignedDPoPProof(UUID.randomUUID().toString(), HttpMethod.GET.toString(), oauth.getUserInfoUrl(), Long.valueOf(Time.currentTime()), Algorithm.PS256, jwsRsaHeader, rsaKeyPair.getPrivate());
        oauth.dpopProof(dpopProofRsaEncoded);
        UserInfoResponse userInfoResponse = oauth.doUserInfoRequestByGet(accessToken);
        assertEquals(TEST_USER_NAME, userInfoResponse.getUserInfo().getPreferredUsername());
    }

    private void testDPoPProofFailure(String dpopProofEncoded, String errorDescription) throws Exception {
        oauth.dpopProof(dpopProofEncoded);
        oauth.clientId(TEST_CONFIDENTIAL_CLIENT_ID);
        oauth.doLogin(TEST_USER_NAME, TEST_USER_PASSWORD);

        String code = oauth.getCurrentQuery().get(OAuth2Constants.CODE);
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(code, TEST_CONFIDENTIAL_CLIENT_SECRET);

        assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusCode());
        assertEquals(OAuthErrorException.INVALID_DPOP_PROOF, response.getError());
        assertEquals(errorDescription, response.getErrorDescription());
    }

    private JWK createRsaJwk(Key publicKey) {
        RSAPublicKey rsaKey = (RSAPublicKey) publicKey;

        RSAPublicJWK k = new RSAPublicJWK();
        k.setKeyType(KeyType.RSA);
        k.setModulus(Base64Url.encode(toIntegerBytes(rsaKey.getModulus())));
        k.setPublicExponent(Base64Url.encode(toIntegerBytes(rsaKey.getPublicExponent())));

        return k;
    }

    private JWK createEcJwk(Key publicKey) {
        ECPublicKey ecKey = (ECPublicKey) publicKey;

        int fieldSize = ecKey.getParams().getCurve().getField().getFieldSize();
        ECPublicJWK k = new ECPublicJWK();
        k.setKeyType(KeyType.EC);
        k.setCrv("P-" + fieldSize);
        k.setX(Base64Url.encode(toIntegerBytes(ecKey.getW().getAffineX(), fieldSize)));
        k.setY(Base64Url.encode(toIntegerBytes(ecKey.getW().getAffineY(), fieldSize)));

        return k;
    }

    private static KeyPair generateEcdsaKey(String ecDomainParamName) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC");
        SecureRandom randomGen = SecureRandom.getInstance("SHA1PRNG");
        ECGenParameterSpec ecSpec = new ECGenParameterSpec(ecDomainParamName);
        keyGen.initialize(ecSpec, randomGen);
        KeyPair keyPair = keyGen.generateKeyPair();
        return keyPair;
    }

    private static String generateSignedDPoPProof(String jti, String htm, String htu, Long iat, String algorithm, JWSHeader jwsHeader, PrivateKey privateKey) throws IOException {

        String dpopProofHeaderEncoded = Base64Url.encode(JsonSerialization.writeValueAsBytes(jwsHeader));

        DPoP dpop = new DPoP();
        dpop.id(jti);
        dpop.setHttpMethod(htm);
        dpop.setHttpUri(htu);
        dpop.iat(iat);

        String dpopProofPayloadEncoded = Base64Url.encode(JsonSerialization.writeValueAsBytes(dpop));

        try {
            Signature signature = Signature.getInstance(JavaAlgorithm.getJavaAlgorithm(algorithm));
            signature.initSign(privateKey);
            String data = dpopProofHeaderEncoded + "." + dpopProofPayloadEncoded;
            byte[] dataByteArray = data.getBytes();
            signature.update(dataByteArray);
            byte[] signatureByteArray = signature.sign();
            return data + "." + Base64Url.encode(signatureByteArray);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    private void changeDPoPBound(String clientId, boolean isEnabled) {
        ClientResource clientResource = ApiUtil.findClientByClientId(adminClient.realm(REALM_NAME), clientId);
        ClientRepresentation clientRep = clientResource.toRepresentation();
        OIDCAdvancedConfigWrapper.fromClientRepresentation(clientRep).setUseDPoP(isEnabled);
        clientResource.update(clientRep);
    }

    private String createClientByAdmin(String clientName, Consumer<ClientRepresentation> op) {
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
        resp.close();
        assertEquals(Response.Status.CREATED.getStatusCode(), resp.getStatus());
        // registered components will be removed automatically when a test method finishes regardless of its success or failure.
        String cId = ApiUtil.getCreatedId(resp);
        testContext.getOrCreateCleanup(REALM_NAME).addClientUuid(cId);
        return cId;
    }
}
