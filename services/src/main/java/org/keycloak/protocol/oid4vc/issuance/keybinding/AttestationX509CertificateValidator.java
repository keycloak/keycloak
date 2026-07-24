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

import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXBuilderParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509CertSelector;
import java.security.cert.X509Certificate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.keycloak.broker.provider.X509TrustMaterial;
import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.ErrorType;

final class AttestationX509CertificateValidator {

    private AttestationX509CertificateValidator() {
    }

    static JWK validate(List<String> x5c, String algorithm, List<X509TrustMaterial> trustMaterials) {
        VCIssuerException lastFailure = null;
        for (X509TrustMaterial trustMaterial : trustMaterials) {
            try {
                return validate(x5c, algorithm, trustMaterial);
            } catch (VCIssuerException e) {
                lastFailure = e;
            }
        }
        if (lastFailure != null) {
            throw lastFailure;
        }
        return null;
    }

    private static JWK validate(List<String> x5c, String algorithm, X509TrustMaterial trustMaterial) {
        try {
            List<X509Certificate> certificateChain = X5cKeyUtils.decodeCertificateChain(x5c);
            X509Certificate leaf = certificateChain.get(0);

            rejectSelfSignedLeaf(leaf);
            validateLeafPurpose(leaf, trustMaterial.allowedExtendedKeyUsages());

            Set<TrustAnchor> trustAnchors = new HashSet<>();
            trustMaterial.trustAnchors().forEach(certificate -> trustAnchors.add(new TrustAnchor(certificate, null)));

            X509CertSelector selector = new X509CertSelector();
            selector.setCertificate(leaf);
            PKIXBuilderParameters parameters = new PKIXBuilderParameters(trustAnchors, selector);
            parameters.addCertStore(CryptoIntegration.getProvider().getCertStore(
                    new CollectionCertStoreParameters(certificateChain)));
            parameters.setRevocationEnabled(trustMaterial.revocationEnabled());

            CryptoIntegration.getProvider().getCertPathBuilder().build(parameters);
            return X5cKeyUtils.toJwk(leaf, algorithm, certificateChain);
        } catch (VCIssuerException e) {
            throw e;
        } catch (Exception e) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF,
                    "Failed to validate x5c certificate chain against configured attestation trust", e);
        }
    }

    private static void rejectSelfSignedLeaf(X509Certificate leaf) throws Exception {
        if (!leaf.getSubjectX500Principal().equals(leaf.getIssuerX500Principal())) {
            return;
        }
        leaf.verify(leaf.getPublicKey());
        throw new VCIssuerException(ErrorType.INVALID_PROOF,
                "Self-signed certificates are not accepted for key attestation");
    }

    private static void validateLeafPurpose(X509Certificate leaf, List<String> allowedExtendedKeyUsages)
            throws Exception {
        leaf.checkValidity();
        if (leaf.getBasicConstraints() >= 0) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF,
                    "The key-attestation signing certificate must be an end-entity certificate");
        }

        boolean[] keyUsage = leaf.getKeyUsage();
        if (keyUsage != null && (keyUsage.length == 0 || !keyUsage[0])) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF,
                    "The key-attestation leaf certificate is not valid for digital signatures");
        }

        List<String> certificateExtendedKeyUsages = leaf.getExtendedKeyUsage();
        if (certificateExtendedKeyUsages == null
                || allowedExtendedKeyUsages.stream().noneMatch(certificateExtendedKeyUsages::contains)) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF,
                    "The key-attestation leaf certificate does not contain an allowed extended key usage");
        }
    }
}
