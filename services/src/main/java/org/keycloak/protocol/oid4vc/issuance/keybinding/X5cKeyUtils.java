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

package org.keycloak.protocol.oid4vc.issuance.keybinding;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.ErrorType;

final class X5cKeyUtils {

    private static final int MAX_CERTIFICATE_CHAIN_LENGTH = 10;

    private X5cKeyUtils() {
    }

    static List<X509Certificate> decodeCertificateChain(List<String> x5c) {
        if (x5c == null || x5c.isEmpty()) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "The x5c certificate chain is empty");
        }
        if (x5c.size() > MAX_CERTIFICATE_CHAIN_LENGTH) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "The x5c certificate chain is too long");
        }

        try {
            CertificateFactory certificateFactory = CryptoIntegration.getProvider().getX509CertFactory();
            List<X509Certificate> certificates = new ArrayList<>(x5c.size());
            for (String encodedCertificate : x5c) {
                byte[] der = Base64.getMimeDecoder().decode(encodedCertificate);
                try (InputStream input = new ByteArrayInputStream(der)) {
                    certificates.add((X509Certificate) certificateFactory.generateCertificate(input));
                }
            }
            return List.copyOf(certificates);
        } catch (Exception e) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, "Failed to decode x5c certificate chain", e);
        }
    }

    static JWK toJwk(X509Certificate leaf, String algorithm, List<X509Certificate> certificateChain) {
        PublicKey key = leaf.getPublicKey();
        if (key instanceof RSAPublicKey rsa) {
            return JWKBuilder.create().algorithm(algorithm).rsa(rsa, certificateChain);
        }
        if (key instanceof ECPublicKey ec) {
            return JWKBuilder.create().algorithm(algorithm).ec(ec, certificateChain, null);
        }
        throw new VCIssuerException(ErrorType.INVALID_PROOF,
                "Unsupported public key type in certificate: " + key.getClass().getName());
    }
}
