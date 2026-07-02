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

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
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

/**
 * EC signing material generated at runtime for conformance tests, so no private key is committed to
 * the repository: a self-signed CA plus a CA-issued leaf certificate, exposed as the various PEM and
 * JWK representations the conformance suite and Keycloak need. The conformance suite signs with the
 * private JWK while Keycloak trusts the public JWKS and the CA certificate.
 */
public final class ConformanceSigningKey {

    public static final String KEYSTORE_PASSWORD = "password";

    private final String kid;
    private final KeyPair keyPair;
    private final X509Certificate certificate;
    private final X509Certificate caCertificate;
    private String keyStorePath;

    private ConformanceSigningKey(String kid, KeyPair keyPair, X509Certificate certificate,
            X509Certificate caCertificate) {
        this.kid = kid;
        this.keyPair = keyPair;
        this.certificate = certificate;
        this.caCertificate = caCertificate;
    }

    public static ConformanceSigningKey generate(String kid, String name) {
        try {
            if (!CryptoIntegration.isInitialised()) {
                CryptoIntegration.setProvider(new DefaultCryptoProvider());
            }
            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
            keyGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair caKeyPair = keyGenerator.generateKeyPair();
            KeyPair keyPair = keyGenerator.generateKeyPair();

            X509Certificate caCertificate =
                    CertificateUtils.generateV1SelfSignedCertificate(caKeyPair, name + " CA");
            X509Certificate certificate =
                    CertificateUtils.generateV3Certificate(keyPair, caKeyPair.getPrivate(), caCertificate, name);
            return new ConformanceSigningKey(kid, keyPair, certificate, caCertificate);
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
                Path path = Files.createTempFile("keycloak-conformance-" + kid, ".p12");
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
}
