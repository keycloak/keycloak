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

import java.security.cert.X509Certificate;
import java.util.List;

import org.keycloak.common.VerificationException;
import org.keycloak.crypto.X509CertificateChainValidator;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.protocol.oid4vc.issuance.VCIssuerException;
import org.keycloak.protocol.oid4vc.model.ErrorType;

final class X5cKeyUtils {

    private X5cKeyUtils() {
    }

    static List<X509Certificate> decodeCertificateChain(List<String> x5c) {
        try {
            return X509CertificateChainValidator.decodeCertificateChain(x5c);
        } catch (VerificationException e) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, e.getMessage(), e);
        }
    }

    static JWK toJwk(X509Certificate leaf, String algorithm, List<X509Certificate> certificateChain) {
        try {
            return X509CertificateChainValidator.toJwk(leaf, algorithm, certificateChain);
        } catch (VerificationException e) {
            throw new VCIssuerException(ErrorType.INVALID_PROOF, e.getMessage(), e);
        }
    }
}
