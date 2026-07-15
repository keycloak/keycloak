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

package org.keycloak.tests.conformance.vci;

import org.keycloak.tests.conformance.ConformanceSigningKey;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * The attester key, generated at runtime so no private key material is committed to the repository. It is signed
 * by a CA so it serves both client attestation (verified against the trusted public JWKS) and key attestation
 * (which validates the x5c chain against the CA). The private JWKS is handed to the conformance suite to sign
 * attestations, while Keycloak trusts only the public JWKS and the CA certificate.
 */
final class VciAttesterKey {

    static final String KID = "ct_client_attester_key";

    private static final ConformanceSigningKey KEY = ConformanceSigningKey.generate(
            VciConformanceRealmConfig.REALM, KID, "OID4VCI Conformance Attester");

    private VciAttesterKey() {
    }

    static JsonNode privateJwks() {
        return KEY.privateJwks();
    }

    static JsonNode publicJwks() {
        return KEY.publicJwks();
    }

    static String caCertificatePem() {
        return KEY.caCertificatePem();
    }
}
