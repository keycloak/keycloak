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

package org.keycloak.tests.conformance.vci.nonhaip;

import org.keycloak.tests.conformance.ConformanceSigningKey;
import org.keycloak.tests.conformance.vci.VciAttesterKey;

import com.fasterxml.jackson.databind.JsonNode;
import org.bouncycastle.asn1.x509.KeyPurposeId;

import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.REALM;

/**
 * The client signing key for non-HAIP private_key_jwt client authentication, generated at runtime so no private
 * key material is committed to the repository. The conformance suite (acting as the wallet client) holds the
 * private JWKS to sign client assertions, while Keycloak registers only the public JWKS on the client. Unlike
 * {@link VciAttesterKey} there is no x5c chain validation for private_key_jwt: Keycloak verifies the assertion
 * signature against the registered public JWKS, so the leaf certificate extended key usage is immaterial here.
 */
public final class VciClientKey {

    // Two independent keys, one per conformance client. The suite's multiple-clients module asserts the two
    // clients authenticate with different private keys (ValidateClientPrivateKeysAreDifferent), so the keys must
    // not be shared.
    private static final ConformanceSigningKey KEY = ConformanceSigningKey.generate(
            REALM, "ct_client_signing_key", "OID4VCI Conformance Client",
            KeyPurposeId.id_kp_clientAuth);

    private static final ConformanceSigningKey KEY2 = ConformanceSigningKey.generate(
            REALM, "ct_client2_signing_key", "OID4VCI Conformance Client 2",
            KeyPurposeId.id_kp_clientAuth);

    private VciClientKey() {
    }

    static JsonNode privateJwks() {
        return KEY.privateJwks();
    }

    static JsonNode publicJwks() {
        return KEY.publicJwks();
    }

    static JsonNode privateJwks2() {
        return KEY2.privateJwks();
    }

    static JsonNode publicJwks2() {
        return KEY2.publicJwks();
    }
}
