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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.keycloak.crypto.SignatureSignerContext;

import com.fasterxml.jackson.annotation.JsonValue;

final class MdocCose {

    private static final int HEADER_ALGORITHM = 1;
    private static final int HEADER_X5CHAIN = 33;
    private static final int SIGN1_PROTECTED_HEADER = 0;
    private static final int SIGN1_UNPROTECTED_HEADER = 1;
    private static final int SIGN1_PAYLOAD = 2;
    private static final int SIGN1_SIGNATURE = 3;
    private static final int SIGN1_SIZE = SIGN1_SIGNATURE + 1;

    private MdocCose() {
    }

    static Sign1 sign1(byte[] payload,
                       MdocAlgorithm algorithm,
                       SignatureSignerContext signerContext,
                       List<X509Certificate> certificateChain) {
        // COSE headers use integer labels: 1 = alg, 33 = x5chain.
        byte[] protectedHeader = CborUtil.encodeIntegerMap(Collections.singletonMap(
                HEADER_ALGORITHM, algorithm.getCoseAlgorithmIdentifier()
        ));
        Map<Integer, List<byte[]>> unprotectedHeader = Collections.singletonMap(
                HEADER_X5CHAIN, certificateChain.stream().map(MdocCose::encodeCertificate).collect(Collectors.toList())
        );

        // The COSE signature covers Sig_structure, not the serialized COSE_Sign1 message.
        byte[] sigStructure = CborUtil.encode(new SigStructure(
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

    static final class Sign1 {

        private final byte[] protectedHeader;
        private final Map<Integer, List<byte[]>> unprotectedHeader;
        private final byte[] payload;
        private final byte[] signature;

        Sign1(byte[] protectedHeader, Map<Integer, List<byte[]>> unprotectedHeader, byte[] payload, byte[] signature) {
            this.protectedHeader = protectedHeader;
            this.unprotectedHeader = unprotectedHeader;
            this.payload = payload;
            this.signature = signature;
        }

        static ParsedSign1 fromCbor(Object item, String name) {
            List<Object> coseSign1 = CborUtil.asList(item, name);
            if (coseSign1.size() != SIGN1_SIZE) {
                throw new MdocException("Unexpected COSE_Sign1 structure for " + name);
            }

            return new ParsedSign1(
                    CborUtil.asByteArray(coseSign1.get(SIGN1_PROTECTED_HEADER), name + " protected header"),
                    CborUtil.asMap(coseSign1.get(SIGN1_UNPROTECTED_HEADER), name + " unprotected header"),
                    CborUtil.asByteArray(coseSign1.get(SIGN1_PAYLOAD), name + " payload"),
                    CborUtil.asByteArray(coseSign1.get(SIGN1_SIGNATURE), name + " signature")
            );
        }

        @JsonValue
        public List<?> toCborArray() {
            return Arrays.asList(protectedHeader, unprotectedHeader, payload, signature);
        }
    }

    static final class ParsedSign1 {

        private final byte[] protectedHeader;
        private final Map<Object, Object> unprotectedHeader;
        private final byte[] payload;
        private final byte[] signature;

        ParsedSign1(byte[] protectedHeader, Map<Object, Object> unprotectedHeader, byte[] payload, byte[] signature) {
            this.protectedHeader = protectedHeader;
            this.unprotectedHeader = unprotectedHeader;
            this.payload = payload;
            this.signature = signature;
        }

        byte[] protectedHeader() {
            return protectedHeader;
        }

        byte[] payload() {
            return payload;
        }

        byte[] signature() {
            return signature;
        }

        Object header(int key) {
            Object value = unprotectedHeader.get(key);
            if (value == null) {
                value = unprotectedHeader.get((long) key);
            }
            if (value == null) {
                value = unprotectedHeader.get(String.valueOf(key));
            }
            return value;
        }
    }

    static final class SigStructure {

        private final String context;
        private final byte[] bodyProtected;
        private final byte[] externalAad;
        private final byte[] payload;

        SigStructure(String context, byte[] bodyProtected, byte[] externalAad, byte[] payload) {
            this.context = context;
            this.bodyProtected = bodyProtected;
            this.externalAad = externalAad;
            this.payload = payload;
        }

        @JsonValue
        public List<?> toCborArray() {
            return Arrays.asList(context, bodyProtected, externalAad, payload);
        }
    }
}
