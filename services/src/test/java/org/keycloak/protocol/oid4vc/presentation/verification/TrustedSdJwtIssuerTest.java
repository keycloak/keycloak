/*
 * Copyright 2026 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.protocol.oid4vc.presentation.verification;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import org.keycloak.OID4VCConstants;
import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.crypto.CryptoProvider;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.Algorithm;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.keys.DefaultKeyManager;
import org.keycloak.models.KeyManager;
import org.keycloak.models.RealmModel;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.keycloak.crypto.Algorithm.RS256;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class TrustedSdJwtIssuerTest {

    private static final String ISSUER = "https://issuer.example.org";
    private static final String REALM_ISSUER = "https://keycloak.example.org/realms/test";

    @BeforeClass
    public static void beforeClass() {
        CryptoIntegration.init(CryptoProvider.class.getClassLoader());
    }

    @Test
    public void testX5cTrustedIssuerResolvesCertificateBackedKey() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(ISSUER, testChain.issuerKey, testChain.x5c());
        X5cTrustedSdJwtIssuer trustedIssuer = new X5cTrustedSdJwtIssuer(PemUtils.encodeCertificate(testChain.rootCertificate), false);

        assertEquals(1, trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT).size());
    }

    @Test
    public void testX5cTrustedIssuerReturnsEmptyWithoutX5c() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(ISSUER, testChain.issuerKey, null);
        X5cTrustedSdJwtIssuer trustedIssuer = new X5cTrustedSdJwtIssuer(PemUtils.encodeCertificate(testChain.rootCertificate), false);

        assertEquals(0, trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT).size());
    }

    @Test
    public void testX5cTrustedIssuerRejectsUntrustedCertificateChain() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        TestCertificateChain unrelatedChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(ISSUER, testChain.issuerKey, testChain.x5c());
        X5cTrustedSdJwtIssuer trustedIssuer = new X5cTrustedSdJwtIssuer(
                PemUtils.encodeCertificate(unrelatedChain.rootCertificate), false);

        assertThrows(VerificationException.class, () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT));
    }

    @Test
    public void testJwtVcIssuerMetadataTrustedIssuerResolvesCertificateBackedJwks() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(ISSUER, testChain.issuerKey, null);
        ObjectNode metadata = issuerMetadata(ISSUER, testChain);
        JwtVcIssuerMetadataTrustedSdJwtIssuer trustedIssuer = new JwtVcIssuerMetadataTrustedSdJwtIssuer(
                null,
                PemUtils.encodeCertificate(testChain.rootCertificate),
                uri -> {
                    assertEquals("https://issuer.example.org/.well-known/jwt-vc-issuer", uri);
                    return metadata;
                },
                false);

        assertEquals(1, trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT).size());
    }

    @Test
    public void testJwtVcIssuerMetadataTrustedIssuerRejectsMetadataWithJwksAndJwksUri() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(ISSUER, testChain.issuerKey, null);
        ObjectNode metadata = issuerMetadata(ISSUER, testChain);
        metadata.put("jwks_uri", "https://issuer.example.org/jwks");
        JwtVcIssuerMetadataTrustedSdJwtIssuer trustedIssuer = new JwtVcIssuerMetadataTrustedSdJwtIssuer(
                null,
                PemUtils.encodeCertificate(testChain.rootCertificate),
                uri -> metadata,
                false);

        assertThrows(VerificationException.class, () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT));
    }

    @Test
    public void testJwtVcIssuerMetadataTrustedIssuerRejectsIssuerWithQueryComponent() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(ISSUER + "?iss=" + ISSUER, testChain.issuerKey, null);
        JwtVcIssuerMetadataTrustedSdJwtIssuer trustedIssuer = new JwtVcIssuerMetadataTrustedSdJwtIssuer(
                null,
                PemUtils.encodeCertificate(testChain.rootCertificate),
                uri -> {
                    throw new AssertionError("Metadata must not be fetched for invalid issuer URI");
                },
                false);

        assertThrows(VerificationException.class, () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT));
    }

    @Test
    public void testJwtVcIssuerMetadataTrustedIssuerRequiresExactMetadataIssuer() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(ISSUER, testChain.issuerKey, null);
        ObjectNode metadata = issuerMetadata(ISSUER + "/", testChain);
        JwtVcIssuerMetadataTrustedSdJwtIssuer trustedIssuer = new JwtVcIssuerMetadataTrustedSdJwtIssuer(
                null,
                PemUtils.encodeCertificate(testChain.rootCertificate),
                uri -> metadata,
                false);

        assertThrows(VerificationException.class, () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT));
    }

    @Test
    public void testJwtVcIssuerMetadataTrustedIssuerReturnsEmptyWhenX5cHeaderIsPresent() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(ISSUER, testChain.issuerKey, testChain.x5c());
        JwtVcIssuerMetadataTrustedSdJwtIssuer trustedIssuer = new JwtVcIssuerMetadataTrustedSdJwtIssuer(
                null,
                PemUtils.encodeCertificate(testChain.rootCertificate),
                uri -> {
                    throw new AssertionError("Metadata must not be fetched when x5c header is present");
                },
                false);

        assertEquals(0, trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT).size());
    }

    @Test
    public void testRealmCertificateTrustedIssuerResolvesRealmKey() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(REALM_ISSUER, testChain.issuerKey, null);
        RealmCertificateTrustedSdJwtIssuer trustedIssuer = new RealmCertificateTrustedSdJwtIssuer(
                null,
                keyManager(testChain.issuerKey),
                REALM_ISSUER,
                false);

        assertEquals(1, trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT).size());
    }

    @Test
    public void testRealmCertificateTrustedIssuerReturnsEmptyForDifferentIssuer() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(ISSUER, testChain.issuerKey, null);
        RealmCertificateTrustedSdJwtIssuer trustedIssuer = new RealmCertificateTrustedSdJwtIssuer(
                null,
                keyManager(testChain.issuerKey),
                REALM_ISSUER,
                false);

        assertEquals(0, trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT).size());
    }

    @Test
    public void testRealmCertificateTrustedIssuerRequiresExactIssuer() throws Exception {
        TestCertificateChain testChain = createCertificateChain();
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(REALM_ISSUER + "/", testChain.issuerKey, null);
        RealmCertificateTrustedSdJwtIssuer trustedIssuer = new RealmCertificateTrustedSdJwtIssuer(
                null,
                keyManager(testChain.issuerKey),
                REALM_ISSUER,
                false);

        assertEquals(0, trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT).size());
    }

    @Test
    public void testRealmCertificateTrustedIssuerRejectsRealmKeyWithoutCertificate() throws Exception {
        KeyWrapper key = createRsaKey("issuer-key");
        IssuerSignedJWT issuerSignedJWT = issuerSignedJwt(REALM_ISSUER, key, null);
        RealmCertificateTrustedSdJwtIssuer trustedIssuer = new RealmCertificateTrustedSdJwtIssuer(
                null,
                keyManager(key),
                REALM_ISSUER,
                false);

        assertThrows(VerificationException.class, () -> trustedIssuer.resolveIssuerVerifyingKeys(issuerSignedJWT));
    }

    private ObjectNode issuerMetadata(String issuer, TestCertificateChain testChain) {
        JSONWebKeySet jwks = new JSONWebKeySet();
        jwks.setKeys(new JWK[]{
                JWKBuilder.create()
                        .kid(testChain.issuerKey.getKid())
                        .algorithm(testChain.issuerKey.getAlgorithm())
                        .rsa(testChain.issuerKey.getPublicKey(), testChain.certificateChain())
        });

        ObjectNode metadata = JsonSerialization.mapper.createObjectNode();
        metadata.put("issuer", issuer);
        metadata.set("jwks", JsonSerialization.mapper.valueToTree(jwks));
        return metadata;
    }

    private IssuerSignedJWT issuerSignedJwt(String issuer, KeyWrapper key, List<String> x5c) {
        ObjectNode claims = JsonSerialization.mapper.createObjectNode();
        claims.put(OID4VCConstants.CLAIM_NAME_ISSUER, issuer);

        JWSHeader header = new JWSHeader();
        header.setAlgorithm(Algorithm.RS256);
        header.setKeyId(key.getKid());
        header.setX5c(x5c);

        return IssuerSignedJWT.builder()
                .withJwsHeader(header)
                .withClaims(claims)
                .build();
    }

    private static KeyManager keyManager(KeyWrapper key) {
        return new DefaultKeyManager(null) {
            @Override
            public KeyWrapper getKey(RealmModel realm, String kid, KeyUse use, String algorithm) {
                return key.getKid().equals(kid) ? key : null;
            }

            @Override
            public Stream<KeyWrapper> getKeysStream(RealmModel realm, KeyUse use, String algorithm) {
                return Stream.of(key);
            }
        };
    }

    private TestCertificateChain createCertificateChain() throws Exception {
        KeyWrapper rootKey = createRsaKey("root-key");
        KeyWrapper issuerKey = createRsaKey("issuer-key");
        X509Certificate rootCertificate = CertificateUtils.generateV1SelfSignedCertificate(keyPair(rootKey), "root-ca");
        X509Certificate issuerCertificate = CertificateUtils.generateV3Certificate(
                keyPair(issuerKey),
                (PrivateKey) rootKey.getPrivateKey(),
                rootCertificate,
                "issuer");
        issuerKey.setCertificate(issuerCertificate);
        issuerKey.setCertificateChain(List.of(issuerCertificate, rootCertificate));
        return new TestCertificateChain(rootCertificate, issuerCertificate, issuerKey);
    }

    private KeyWrapper createRsaKey(String prefix) throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        KeyWrapper key = new KeyWrapper();
        key.setKid(prefix + "-" + UUID.randomUUID());
        key.setUse(KeyUse.SIG);
        key.setAlgorithm(RS256);
        key.setType("RSA");
        key.setPublicKey(keyPair.getPublic());
        key.setPrivateKey(keyPair.getPrivate());
        return key;
    }

    private static String encodeCertificate(X509Certificate certificate) {
        try {
            return Base64.getEncoder().encodeToString(certificate.getEncoded());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static KeyPair keyPair(KeyWrapper key) {
        return new KeyPair((PublicKey) key.getPublicKey(), (PrivateKey) key.getPrivateKey());
    }

    private static class TestCertificateChain {

        private final X509Certificate rootCertificate;
        private final X509Certificate issuerCertificate;
        private final KeyWrapper issuerKey;

        private TestCertificateChain(X509Certificate rootCertificate, X509Certificate issuerCertificate, KeyWrapper issuerKey) {
            this.rootCertificate = rootCertificate;
            this.issuerCertificate = issuerCertificate;
            this.issuerKey = issuerKey;
        }

        private List<X509Certificate> certificateChain() {
            return List.of(issuerCertificate, rootCertificate);
        }

        private List<String> x5c() {
            return certificateChain().stream()
                    .map(TrustedSdJwtIssuerTest::encodeCertificate)
                    .toList();
        }
    }
}
