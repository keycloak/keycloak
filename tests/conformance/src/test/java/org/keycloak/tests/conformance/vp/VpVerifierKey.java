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

package org.keycloak.tests.conformance.vp;

import org.keycloak.tests.conformance.ConformanceSigningKey;

import com.fasterxml.jackson.databind.JsonNode;

final class VpVerifierKey {

    private static final ConformanceSigningKey KEY =
            ConformanceSigningKey.generate("vp_verifier_key", "OID4VP Conformance Verifier");

    private VpVerifierKey() {
    }

    static String keyStorePath() {
        return KEY.keyStorePath();
    }

    static String keyStorePassword() {
        return ConformanceSigningKey.KEYSTORE_PASSWORD;
    }

    static String keyAlias() {
        return KEY.keyAlias();
    }

    static String caCertificatePem() {
        return KEY.caCertificatePem();
    }

    static JsonNode privateJwk() {
        return KEY.privateJwk();
    }

    static JsonNode publicJwks() {
        return KEY.publicJwks();
    }

    static String clientId() {
        return "x509_hash:" + KEY.x509Hash();
    }
}
