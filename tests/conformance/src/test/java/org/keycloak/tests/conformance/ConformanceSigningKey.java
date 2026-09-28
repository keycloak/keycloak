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

package org.keycloak.tests.conformance;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.JavaAlgorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jwk.JWKUtil;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

/**
 * EC signing material generated at runtime for conformance tests, so no private key is committed to
 * the repository: a self-signed CA plus a CA-issued leaf certificate, exposed as the various PEM and
 * JWK representations the conformance suite and Keycloak need. The conformance suite signs with the
 * private JWK while Keycloak trusts the public JWKS and the CA certificate.
 */
public final class ConformanceSigningKey {

    public static final String KEYSTORE_PASSWORD = "password";

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Path KEYSTORES_BASE_DIR;

    static {
        try {
            KEYSTORES_BASE_DIR = Files.createTempDirectory("keycloak-conformance-keystores");
            KEYSTORES_BASE_DIR.toFile().deleteOnExit();
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final String realmName;
    private final String kid;
    private final KeyPair keyPair;
    private final X509Certificate certificate;
    private final X509Certificate caCertificate;
    private String keyStorePath;

    private ConformanceSigningKey(String realmName, String kid, KeyPair keyPair, X509Certificate certificate,
            X509Certificate caCertificate) {
        this.realmName = realmName;
        this.kid = kid;
        this.keyPair = keyPair;
        this.certificate = certificate;
        this.caCertificate = caCertificate;
    }

    // The Java keystore key provider only loads keystores under the realm's folder below the
    // configured keystores path, so all conformance keystores live in per realm folders under this
    // base directory and the test servers point the keystores path option at it.
    public static String keystoresBaseDir() {
        return KEYSTORES_BASE_DIR.toString();
    }

    public static synchronized Path realmKeystoreDir(String realmName) throws IOException {
        Path realmDir = KEYSTORES_BASE_DIR.resolve(realmName);
        if (!Files.exists(realmDir)) {
            Files.createDirectory(realmDir);
            realmDir.toFile().deleteOnExit();
        }
        return realmDir;
    }

    /**
     * Generates a CA-signed key. {@code leafEku} optionally restricts the leaf to a single extended key
     * usage purpose; pass {@code null} to leave it unrestricted.
     */
    public static ConformanceSigningKey generate(String realmName, String kid, String name, KeyPurposeId leafEku) {
        try {
            if (!CryptoIntegration.isInitialised()) {
                CryptoIntegration.setProvider(new DefaultCryptoProvider());
            }
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
            keyGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair caKeyPair = keyGenerator.generateKeyPair();
            KeyPair keyPair = keyGenerator.generateKeyPair();

            // The CA must be a proper v3 CA certificate with the CA basic constraint and keyCertSign key usage,
            // because X509TrustMaterial rejects trust anchors that are not CA certificates. The leaf must be a
            // plain end-entity certificate, as the key-attestation validator rejects CA-capable leaves.
            X509Certificate caCertificate = generateCaCertificate(caKeyPair, name + " CA");
            X509Certificate certificate = generateLeafCertificate(keyPair, caKeyPair, caCertificate, name, leafEku);
            return new ConformanceSigningKey(realmName, kid, keyPair, certificate, caCertificate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create conformance signing key " + kid, e);
        }
    }

    public JsonNode publicJwks() {
        return jwks(jwk(false));
    }

    public JsonNode privateJwks() {
        return jwks(jwk(true));
    }

    public JsonNode privateJwk() {
        return JsonSerialization.writeValueAsNode(jwk(true));
    }

    public String caCertificatePem() {
        return PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(caCertificate));
    }

    public String x509Hash() {
        try {
            return Base64Url.encode(HashUtils.hash(JavaAlgorithm.SHA256, certificate.getEncoded()));
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute x509_hash", e);
        }
    }

    public String keyAlias() {
        return kid;
    }

    // A PKCS12 keystore holding the leaf key and its certificate chain, e.g. to back a realm key provider.
    public synchronized String keyStorePath() {
        if (keyStorePath == null) {
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(null, null);
                keyStore.setKeyEntry(kid, keyPair.getPrivate(), KEYSTORE_PASSWORD.toCharArray(),
                        new Certificate[] {certificate, caCertificate});
                Path path = Files.createTempFile(realmKeystoreDir(realmName), "keycloak-conformance-" + kid, ".p12");
                try (OutputStream output = Files.newOutputStream(path)) {
                    keyStore.store(output, KEYSTORE_PASSWORD.toCharArray());
                }
                path.toFile().deleteOnExit();
                keyStorePath = path.toString();
            } catch (Exception e) {
                throw new RuntimeException("Failed to create conformance keystore", e);
            }
        }
        return keyStorePath;
    }

    private JWK jwk(boolean includePrivateKey) {
        JWK jwk = JWKBuilder.create().kid(kid).algorithm(Algorithm.ES256)
                .ec(keyPair.getPublic(), List.of(certificate), KeyUse.SIG);
        if (includePrivateKey) {
            ECPrivateKey privateKey = (ECPrivateKey) keyPair.getPrivate();
            int fieldSize = privateKey.getParams().getCurve().getField().getFieldSize();
            jwk.setOtherClaims("d", Base64Url.encode(JWKUtil.toIntegerBytes(privateKey.getS(), fieldSize)));
        }
        return jwk;
    }

    private static JsonNode jwks(JWK key) {
        JSONWebKeySet keySet = new JSONWebKeySet();
        keySet.setKeys(new JWK[] {key});
        return JsonSerialization.writeValueAsNode(keySet);
    }

    private static X509Certificate generateCaCertificate(KeyPair caKeyPair, String commonName) throws Exception {
        X500Name caName = new X500Name("CN=" + commonName);
        X509v3CertificateBuilder builder = certificateBuilder(caName, caName, caKeyPair);
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        return sign(builder, caKeyPair);
    }

    private static X509Certificate generateLeafCertificate(KeyPair keyPair, KeyPair caKeyPair,
            X509Certificate caCertificate, String name, KeyPurposeId leafEku) throws Exception {
        X500Name issuer = new X500Name(caCertificate.getSubjectX500Principal().getName());
        X500Name subject = new X500Name("CN=" + name);
        X509v3CertificateBuilder builder = certificateBuilder(issuer, subject, keyPair);
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(false));
        builder.addExtension(Extension.keyUsage, true, new KeyUsage(KeyUsage.digitalSignature));
        // A restrictive EKU is only needed for key attestation; other leaves must stay unrestricted.
        if (leafEku != null) {
            builder.addExtension(Extension.extendedKeyUsage, false, new ExtendedKeyUsage(leafEku));
        }
        return sign(builder, caKeyPair);
    }

    private static X509v3CertificateBuilder certificateBuilder(X500Name issuer, X500Name subject, KeyPair keyPair) {
        Instant now = Instant.now();
        return new X509v3CertificateBuilder(
                issuer,
                new BigInteger(160, RANDOM),
                Date.from(now.minus(1, ChronoUnit.DAYS)),
                Date.from(now.plus(365, ChronoUnit.DAYS)),
                subject,
                SubjectPublicKeyInfo.getInstance(keyPair.getPublic().getEncoded()));
    }

    private static X509Certificate sign(X509v3CertificateBuilder builder, KeyPair signingKeyPair) throws Exception {
        ContentSigner signer = new JcaContentSignerBuilder("SHA256withECDSA").build(signingKeyPair.getPrivate());
        return new JcaX509CertificateConverter().getCertificate(builder.build(signer));
    }
}
