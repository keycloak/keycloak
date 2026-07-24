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

package org.keycloak.broker.provider;

import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Set;

/**
 * X.509 trust material and validation policy exposed by a trust-material identity provider.
 *
 * @param trustAnchors             self-signed CA roots for one trust domain
 * @param allowedExtendedKeyUsages extended-key-usage OIDs accepted for end-entity certificates
 * @param revocationEnabled        whether PKIX revocation checking is required
 */
public record X509TrustMaterial(Set<X509Certificate> trustAnchors,
                                List<String> allowedExtendedKeyUsages,
                                boolean revocationEnabled) {

    public X509TrustMaterial {
        trustAnchors = Set.copyOf(trustAnchors);
        allowedExtendedKeyUsages = List.copyOf(allowedExtendedKeyUsages);
        if (trustAnchors.isEmpty()) {
            throw new IllegalArgumentException("At least one X.509 trust anchor is required");
        }
        if (allowedExtendedKeyUsages.isEmpty()) {
            throw new IllegalArgumentException("At least one attestation extended key usage is required");
        }
        trustAnchors.forEach(X509TrustMaterial::validateTrustAnchor);
    }

    private static void validateTrustAnchor(X509Certificate certificate) {
        if (certificate.getBasicConstraints() < 0) {
            throw new IllegalArgumentException("X.509 trust anchors must be CA certificates");
        }

        boolean[] keyUsage = certificate.getKeyUsage();
        if (keyUsage != null && (keyUsage.length <= 5 || !keyUsage[5])) {
            throw new IllegalArgumentException("X.509 trust anchors must be valid for certificate signing");
        }

        if (!certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())) {
            throw new IllegalArgumentException("X.509 trust anchors must be self-issued root certificates");
        }

        try {
            certificate.verify(certificate.getPublicKey());
        } catch (Exception e) {
            throw new IllegalArgumentException("X.509 trust anchors must be self-signed root certificates", e);
        }
    }
}
