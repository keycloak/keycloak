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

import java.security.cert.X509Certificate;
import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.sdjwt.IssuerSignedJWT;
import org.keycloak.sdjwt.consumer.TrustedSdJwtIssuer;

class X5cTrustedSdJwtIssuer implements TrustedSdJwtIssuer {

    private final CertificateChainValidator certificateChainValidator;

    X5cTrustedSdJwtIssuer(String trustedIssuerCertificate, boolean allowSelfSigned) {
        this.certificateChainValidator = new CertificateChainValidator(trustedIssuerCertificate, allowSelfSigned);
    }

    @Override
    public List<SignatureVerifierContext> resolveIssuerVerifyingKeys(IssuerSignedJWT issuerSignedJWT)
            throws VerificationException {
        JWSHeader header = issuerSignedJWT.getJwsHeader();
        List<String> x5c = header != null ? header.getX5c() : null;
        if (x5c == null || x5c.isEmpty()) {
            return List.of();
        }

        String algorithm = algorithm(issuerSignedJWT);
        X509Certificate certificate = certificateChainValidator.validateTrustedEncodedChain(x5c);
        return List.of(certificateChainValidator.toVerifierContext(certificate, algorithm, header.getKeyId()));
    }

    private String algorithm(IssuerSignedJWT issuerSignedJWT) throws VerificationException {
        JWSHeader header = issuerSignedJWT.getJwsHeader();
        String algorithm = header != null && header.getAlgorithm() != null ? header.getAlgorithm().name() : null;
        if (algorithm == null) {
            throw new VerificationException("Missing SD-JWT issuer signature algorithm");
        }
        return algorithm;
    }
}
