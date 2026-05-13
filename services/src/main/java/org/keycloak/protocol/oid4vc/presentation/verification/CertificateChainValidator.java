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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.PublicKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.util.KeyWrapperUtil;

class CertificateChainValidator {

    private final String trustedIssuerCertificate;
    private final boolean allowSelfSigned;

    CertificateChainValidator(String trustedIssuerCertificate, boolean allowSelfSigned) {
        this.trustedIssuerCertificate = trustedIssuerCertificate;
        this.allowSelfSigned = allowSelfSigned;
    }

    X509Certificate validateTrustedEncodedChain(List<String> encodedChain) throws VerificationException {
        return validateTrustedChain(decodeCertificateChain(encodedChain));
    }

    X509Certificate validateTrustedChain(List<X509Certificate> chain) throws VerificationException {
        validateCertificateDatesAndSignatures(chain);

        if (chain.size() == 1 && isSelfSigned(chain.get(0)) && !allowSelfSigned) {
            throw new VerificationException("Self-signed SD-JWT issuer certificates are only allowed in dev mode");
        }

        List<X509Certificate> trustedCertificates = trustedCertificates();
        if (trustedCertificates.isEmpty()) {
            throw new VerificationException("No trusted issuer certificate configured");
        }

        if (chain.stream().anyMatch(chainCertificate -> trustedCertificates.stream().anyMatch(chainCertificate::equals))) {
            return chain.get(0);
        }

        X509Certificate lastCertificate = chain.get(chain.size() - 1);
        for (X509Certificate trustedCertificate : trustedCertificates) {
            try {
                trustedCertificate.checkValidity();
                if (lastCertificate.getIssuerX500Principal().equals(trustedCertificate.getSubjectX500Principal())) {
                    lastCertificate.verify(trustedCertificate.getPublicKey());
                    return chain.get(0);
                }
            } catch (Exception ignored) {
            }
        }

        throw new VerificationException("SD-JWT issuer certificate chain is not anchored in a configured certificate");
    }

    void validateRealmChain(List<X509Certificate> chain) throws VerificationException {
        validateCertificateDatesAndSignatures(chain);
        if (chain.size() == 1 && isSelfSigned(chain.get(0)) && !allowSelfSigned) {
            throw new VerificationException("Self-signed SD-JWT issuer certificates are only allowed in dev mode");
        }
    }

    SignatureVerifierContext toVerifierContext(X509Certificate certificate, String algorithm, String kid)
            throws VerificationException {
        PublicKey publicKey = certificate.getPublicKey();
        KeyWrapper key = new KeyWrapper();
        key.setKid(kid);
        key.setUse(KeyUse.SIG);
        key.setAlgorithm(algorithm);
        key.setPublicKey(publicKey);
        key.setCertificate(certificate);

        if (publicKey instanceof RSAPublicKey) {
            key.setType(KeyType.RSA);
        } else if (publicKey instanceof ECPublicKey) {
            key.setType(KeyType.EC);
        } else {
            throw new VerificationException("Unsupported SD-JWT issuer certificate public key type: " + publicKey.getAlgorithm());
        }

        return KeyWrapperUtil.createSignatureVerifierContext(key);
    }

    private void validateCertificateDatesAndSignatures(List<X509Certificate> chain) throws VerificationException {
        if (chain.isEmpty()) {
            throw new VerificationException("Missing SD-JWT issuer certificate chain");
        }

        for (int i = 0; i < chain.size(); i++) {
            X509Certificate certificate = chain.get(i);
            try {
                certificate.checkValidity();
                if (i + 1 < chain.size()) {
                    certificate.verify(chain.get(i + 1).getPublicKey());
                }
            } catch (Exception e) {
                throw new VerificationException("Invalid SD-JWT issuer certificate chain", e);
            }
        }
    }

    private List<X509Certificate> trustedCertificates() throws VerificationException {
        if (trustedIssuerCertificate == null || trustedIssuerCertificate.isBlank()) {
            return List.of();
        }

        try {
            if (trustedIssuerCertificate.contains(PemUtils.END_CERT)) {
                return Arrays.asList(PemUtils.decodeCertificates(trustedIssuerCertificate));
            }
            return List.of(PemUtils.decodeCertificate(trustedIssuerCertificate));
        } catch (Exception e) {
            throw new VerificationException("Invalid trusted issuer certificate configuration", e);
        }
    }

    private List<X509Certificate> decodeCertificateChain(List<String> encodedChain) throws VerificationException {
        List<X509Certificate> chain = new ArrayList<>();
        for (String encodedCertificate : encodedChain) {
            chain.add(decodeCertificate(encodedCertificate));
        }
        return chain;
    }

    private X509Certificate decodeCertificate(String encodedCertificate) throws VerificationException {
        if (encodedCertificate.contains(PemUtils.BEGIN_CERT)) {
            return PemUtils.decodeCertificate(encodedCertificate);
        }

        try {
            byte[] certBytes = Base64.getMimeDecoder().decode(encodedCertificate);
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            try (InputStream in = new ByteArrayInputStream(certBytes)) {
                return (X509Certificate) certificateFactory.generateCertificate(in);
            }
        } catch (Exception ignored) {
            try {
                return PemUtils.decodeCertificate(encodedCertificate);
            } catch (Exception e) {
                throw new VerificationException("Invalid certificate in SD-JWT issuer certificate chain", e);
            }
        }
    }

    private boolean isSelfSigned(X509Certificate certificate) throws VerificationException {
        if (!certificate.getSubjectX500Principal().equals(certificate.getIssuerX500Principal())) {
            return false;
        }

        try {
            certificate.verify(certificate.getPublicKey());
            return true;
        } catch (Exception e) {
            throw new VerificationException("Invalid self-signed SD-JWT issuer certificate", e);
        }
    }
}
