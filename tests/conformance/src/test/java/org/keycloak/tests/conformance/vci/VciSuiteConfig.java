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

import java.net.URI;
import java.util.List;

import org.keycloak.testframework.conformance.runner.BrowserFlow;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CLIENT2;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.CONFORMANCE_CALLBACK;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.HOLDER;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.PASSWORD;
import static org.keycloak.tests.conformance.vci.VciConformanceRealmUtil.REALM;

/**
 * The test configuration uploaded to the conformance suite. The structure is defined by the suite and issuer
 * plans require two clients, of which the second is only used by "multiple clients" modules.
 */
record VciSuiteConfig(
        String alias,
        Vci vci,
        Credential credential,
        Client client,
        Client client2,
        List<BrowserFlow> browser) {

    static VciSuiteConfig create(URI keycloakBaseUri, String credentialConfigurationId, JsonNode clientAttesterJwks,
            JsonNode keyAttestationJwks, JsonNode clientJwks, JsonNode client2Jwks, String trustAnchorPem,
            BrowserInteraction browserInteraction) {
        // Client attestation (HAIP) and key attestation (HAIP and non-HAIP key-attestation tests) are independent:
        // the client_attestation_* fields are only emitted when a client attester JWKS is supplied, so a non-HAIP
        // private_key_jwt client that still exercises key attestation advertises only key_attestation_jwks.
        String clientAttestationIssuer = clientAttesterJwks == null ? null : "https://example.com/client-attester";
        return new VciSuiteConfig(
                "keycloak",
                new Vci(keycloakBaseUri + "/realms/" + REALM,
                        credentialConfigurationId,
                        clientAttestationIssuer,
                        clientAttesterJwks,
                        keyAttestationJwks),
                new Credential(trustAnchorPem, trustAnchorPem),
                // HAIP passes null client JWKS (it authenticates with client attestation); non-HAIP passes the
                // distinct private JWKS the suite signs each client's private_key_jwt assertions with.
                new Client(CLIENT, clientJwks),
                new Client(CLIENT2, client2Jwks),
                browserInteraction.browserFlows(new BrowserInteraction.BrowserContext(
                        REALM,
                        HOLDER,
                        PASSWORD,
                        CONFORMANCE_CALLBACK)));
    }

    JsonNode toJson() {
        return JsonSerialization.writeValueAsNode(this);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Vci(
            @JsonProperty("credential_issuer_url") String credentialIssuerUrl,
            @JsonProperty("credential_configuration_id") String credentialConfigurationId,
            @JsonProperty("client_attestation_issuer") String clientAttestationIssuer,
            @JsonProperty("client_attester_keys_jwks") JsonNode clientAttesterKeysJwks,
            @JsonProperty("key_attestation_jwks") JsonNode keyAttestationJwks) {
    }

    record Credential(
            @JsonProperty("trust_anchor_pem") String trustAnchorPem,
            @JsonProperty("status_list_trust_anchor_pem") String statusListTrustAnchorPem) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record Client(
            @JsonProperty("client_id") String clientId,
            @JsonProperty("jwks") JsonNode jwks) {
    }
}
