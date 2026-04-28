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
package org.keycloak.mdoc;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.List;
import java.util.Map;

import org.keycloak.crypto.SignatureSignerContext;

import com.fasterxml.jackson.annotation.JsonValue;

final class MdocCose {

    private static final int HEADER_ALGORITHM = 1;
    private static final int HEADER_X5CHAIN = 33;

    private MdocCose() {
    }

    static Sign1 sign1(byte[] payload,
                       MdocAlgorithm algorithm,
                       SignatureSignerContext signerContext,
                       List<X509Certificate> certificateChain) {
        // COSE headers use integer labels: 1 = alg, 33 = x5chain.
        byte[] protectedHeader = MdocCbor.encodeIntegerMap(Map.of(
                HEADER_ALGORITHM, algorithm.getCoseAlgorithmIdentifier()
        ));
        Map<Integer, List<byte[]>> unprotectedHeader = Map.of(
                HEADER_X5CHAIN, certificateChain.stream().map(MdocCose::encodeCertificate).toList()
        );

        // The COSE signature covers Sig_structure, not the serialized COSE_Sign1 message.
        byte[] sigStructure = MdocCbor.encode(new SigStructure(
                "Signature1",
                protectedHeader,
                new byte[0],
                payload
        ));
        byte[] signature = signerContext.sign(sigStructure);

        return new Sign1(protectedHeader, unprotectedHeader, payload, signature);
    }

    private static byte[] encodeCertificate(X509Certificate certificate) {
        try {
            return certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw new MdocException("Could not encode mDoc issuer certificate", e);
        }
    }

    record Sign1(byte[] protectedHeader, Map<Integer, List<byte[]>> unprotectedHeader, byte[] payload, byte[] signature) {

        @JsonValue
        public List<?> toCborArray() {
            return List.of(protectedHeader, unprotectedHeader, payload, signature);
        }
    }

    record SigStructure(String context, byte[] bodyProtected, byte[] externalAad, byte[] payload) {

        @JsonValue
        public List<?> toCborArray() {
            return List.of(context, bodyProtected, externalAad, payload);
        }
    }
}
