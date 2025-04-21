/*
 * Copyright 2024 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.protocol.oid4vc.issuance.signing.vcdm;

/**
 * Enum containing the w3c-registered Signature Suites
 * {@see https://w3c-ccg.github.io/ld-cryptosuite-registry}
 *
 * @author <a href="https://github.com/wistefan">Stefan Wiedemann</a>
 */
public enum LDSignatureType {

    ED_25519_SIGNATURE_2018("Ed25519Signature2018"),
    ED_25519_SIGNATURE_2020("Ed25519Signature2020"),
    ECDSA_SECP_256K1_SIGNATURE_2019("EcdsaSecp256k1Signature2019"),
    RSA_SIGNATURE_2018("RsaSignature2018"),
    JSON_WEB_SIGNATURE_2020("JsonWebSignature2020"),
    JCS_ED_25519_SIGNATURE_2020("JcsEd25519Signature2020");

    private final String value;

    LDSignatureType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static LDSignatureType getByValue(String value) {
        for (LDSignatureType signatureType : values()) {
            if (signatureType.getValue().equalsIgnoreCase(value))
                return signatureType;
        }
        throw new IllegalArgumentException(String.format("No signature of type %s exists.", value));
    }
}