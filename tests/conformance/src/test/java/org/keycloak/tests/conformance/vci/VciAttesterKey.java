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

package org.keycloak.tests.conformance.vci;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.ECGenParameterSpec;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.CertificateUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.def.DefaultCryptoProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jwk.JWKUtil;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The attester key, generated at runtime so no private key material is committed to the repository. It is signed
 * by a CA so it serves both client attestation (verified against the trusted public JWKS) and key attestation
 * (which validates the x5c chain against the CA). The private JWKS is handed to the conformance suite to sign
 * attestations, while Keycloak trusts only the public JWKS and the CA certificate.
 */
final class VciAttesterKey {

    static final String KID = "ct_client_attester_key";

    private static final JsonNode PRIVATE_JWKS;
    private static final JsonNode PUBLIC_JWKS;
    private static final String CA_CERTIFICATE_PEM;

    static {
        try {
            if (!CryptoIntegration.isInitialised()) {
                CryptoIntegration.setProvider(new DefaultCryptoProvider());
            }

            KeyPairGenerator keyGenerator = KeyPairGenerator.getInstance("EC");
            keyGenerator.initialize(new ECGenParameterSpec("secp256r1"));
            KeyPair caKeyPair = keyGenerator.generateKeyPair();
            KeyPair attesterKeyPair = keyGenerator.generateKeyPair();

            X509Certificate caCertificate = CertificateUtils.generateV1SelfSignedCertificate(caKeyPair,
                    "OID4VCI Conformance Attester CA");
            X509Certificate attesterCertificate = CertificateUtils.generateV3Certificate(attesterKeyPair,
                    caKeyPair.getPrivate(), caCertificate, "OID4VCI Conformance Attester");

            PUBLIC_JWKS = jwks(publicJwk(attesterKeyPair, attesterCertificate));

            JWK privateJwk = publicJwk(attesterKeyPair, attesterCertificate);
            ECPrivateKey privateKey = (ECPrivateKey) attesterKeyPair.getPrivate();
            int fieldSize = privateKey.getParams().getCurve().getField().getFieldSize();
            privateJwk.setOtherClaims("d", Base64Url.encode(JWKUtil.toIntegerBytes(privateKey.getS(), fieldSize)));
            PRIVATE_JWKS = jwks(privateJwk);

            CA_CERTIFICATE_PEM = PemUtils.addCertificateBeginEnd(PemUtils.encodeCertificate(caCertificate));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OID4VCI conformance attester key", e);
        }
    }

    private VciAttesterKey() {
    }

    static JsonNode privateJwks() {
        return PRIVATE_JWKS;
    }

    static JsonNode publicJwks() {
        return PUBLIC_JWKS;
    }

    static String caCertificatePem() {
        return CA_CERTIFICATE_PEM;
    }

    private static JWK publicJwk(KeyPair keyPair, X509Certificate certificate) {
        return JWKBuilder.create().kid(KID).algorithm(Algorithm.ES256)
                .ec(keyPair.getPublic(), List.of(certificate), KeyUse.SIG);
    }

    private static JsonNode jwks(JWK key) {
        JSONWebKeySet keySet = new JSONWebKeySet();
        keySet.setKeys(new JWK[] { key });
        return JsonSerialization.writeValueAsNode(keySet);
    }
}
