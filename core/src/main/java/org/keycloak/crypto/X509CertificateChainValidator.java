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

package org.keycloak.crypto;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.common.VerificationException;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;

/**
 * Validates x5c certificate chains of end entity signing certificates against a set of trust
 * anchors and an optional extended key usage requirement.
 */
public final class X509CertificateChainValidator {

    private static final int MAX_CERTIFICATE_CHAIN_LENGTH = 10;

    private X509CertificateChainValidator() {
    }

    /**
     * Validates the x5c certificate chain against the given trust anchors and returns the leaf key
     * as JWK. The leaf must be a currently valid end entity certificate usable for digital
     * signatures. When {@code requiredExtendedKeyUsages} is not empty, the leaf must additionally
     * contain at least one of the given extended key usage OIDs.
     */
    public static JWK validate(List<String> x5c, String algorithm, Collection<X509Certificate> trustAnchors,
            List<String> requiredExtendedKeyUsages) throws VerificationException {
        try {
            List<X509Certificate> certificateChain = decodeCertificateChain(x5c);
            X509Certificate leaf = certificateChain.get(0);

            rejectSelfSignedLeaf(leaf);
            validateLeafPurpose(leaf, requiredExtendedKeyUsages);

            Set<TrustAnchor> anchors = new HashSet<>();
            trustAnchors.forEach(certificate -> anchors.add(new TrustAnchor(certificate, null)));

            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(leaf);
            PKIXBuilderParameters parameters = new PKIXBuilderParameters(anchors, selector);
            parameters.addCertStore(CryptoIntegration.getProvider().getCertStore(
                    new CollectionCertStoreParameters(certificateChain)));
            // PKIX enables its provider specific revocation mechanism by default. Certificate
            // revocation is not supported yet, so disable it explicitly until a configurable
            // mechanism is available.
            parameters.setRevocationEnabled(false);

            CryptoIntegration.getProvider().getCertPathBuilder().build(parameters);
            return toJwk(leaf, algorithm, certificateChain);
        } catch (VerificationException e) {
            throw e;
        } catch (Exception e) {
            throw new VerificationException(
                    "Failed to validate the x5c certificate chain against the configured trust anchors", e);
        }
    }

    public static List<X509Certificate> decodeCertificateChain(List<String> x5c) throws VerificationException {
        if (x5c == null || x5c.isEmpty()) {
            throw new VerificationException("The x5c certificate chain is empty");
        }
        if (x5c.size() > MAX_CERTIFICATE_CHAIN_LENGTH) {
            throw new VerificationException("The x5c certificate chain is too long");
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
            return Collections.unmodifiableList(certificates);
        } catch (Exception e) {
            throw new VerificationException("Failed to decode x5c certificate chain", e);
        }
    }

    public static JWK toJwk(X509Certificate leaf, String algorithm, List<X509Certificate> certificateChain)
            throws VerificationException {
        PublicKey key = leaf.getPublicKey();
        if (key instanceof RSAPublicKey) {
            return JWKBuilder.create().algorithm(algorithm).rsa((RSAPublicKey) key, certificateChain);
        }
        if (key instanceof ECPublicKey) {
            return JWKBuilder.create().algorithm(algorithm).ec((ECPublicKey) key, certificateChain, null);
        }
        throw new VerificationException("Unsupported public key type in certificate: " + key.getClass().getName());
    }

    private static void rejectSelfSignedLeaf(X509Certificate leaf) throws Exception {
        if (!leaf.getSubjectX500Principal().equals(leaf.getIssuerX500Principal())) {
            return;
        }
        leaf.verify(leaf.getPublicKey());
        throw new VerificationException("Self signed certificates are not accepted as chain leaf");
    }

    private static void validateLeafPurpose(X509Certificate leaf, List<String> requiredExtendedKeyUsages)
            throws Exception {
        leaf.checkValidity();
        if (leaf.getBasicConstraints() >= 0) {
            throw new VerificationException("The chain leaf must be an end entity certificate");
        }

        boolean[] keyUsage = leaf.getKeyUsage();
        if (keyUsage != null && (keyUsage.length == 0 || !keyUsage[0])) {
            throw new VerificationException("The chain leaf certificate is not valid for digital signatures");
        }

        if (requiredExtendedKeyUsages.isEmpty()) {
            return;
        }
        List<String> certificateExtendedKeyUsages = leaf.getExtendedKeyUsage();
        if (certificateExtendedKeyUsages == null
                || requiredExtendedKeyUsages.stream().noneMatch(certificateExtendedKeyUsages::contains)) {
            throw new VerificationException(
                    "The chain leaf certificate does not contain any of the required extended key usages");
        }
    }
}
