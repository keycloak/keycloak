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

import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

/**
 * The test configuration uploaded to the conformance suite for an OID4VP verifier plan: the
 * verifier's client id, the trust anchor used to validate the request object chain, and the signing
 * JWK the suite uses to sign the presented credential.
 */
record VpSuiteConfig(String alias, String description, String publish, Client client, Credential credential) {

    static VpSuiteConfig create(String alias, String clientId, String requestObjectTrustAnchorPem, JsonNode signingJwk) {
        return new VpSuiteConfig(
                alias,
                "Keycloak OID4VP verifier conformance",
                "private",
                new Client(clientId, requestObjectTrustAnchorPem),
                new Credential(signingJwk));
    }

    JsonNode toJson() {
        return JsonSerialization.writeValueAsNode(this);
    }

    record Client(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("request_object_trust_anchor_pem") String requestObjectTrustAnchorPem) {
    }

    record Credential(@JsonProperty("signing_jwk") JsonNode signingJwk) {
    }
}
