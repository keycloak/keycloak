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

import org.keycloak.tests.conformance.runner.BrowserFlow;
import org.keycloak.tests.conformance.runner.BrowserInteraction;
import org.keycloak.util.JsonSerialization;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;


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

    static VciSuiteConfig create(URI keycloakBaseUri, JsonNode attesterJwks, String trustAnchorPem,
            BrowserInteraction browserInteraction) {
        return new VciSuiteConfig(
                "keycloak",
                new Vci(keycloakBaseUri + "/realms/" + VciConformanceRealmConfig.REALM,
                        VciConformanceRealmConfig.CREDENTIAL_CONFIGURATION_ID,
                        "https://example.com/client-attester",
                        attesterJwks,
                        attesterJwks),
                new Credential(trustAnchorPem, trustAnchorPem),
                new Client(VciConformanceRealmConfig.CLIENT),
                new Client(VciConformanceRealmConfig.CLIENT2),
                browserInteraction.browserFlows(new BrowserInteraction.BrowserContext(
                        VciConformanceRealmConfig.REALM,
                        VciConformanceRealmConfig.HOLDER,
                        VciConformanceRealmConfig.PASSWORD,
                        VciConformanceRealmConfig.CONFORMANCE_CALLBACK)));
    }

    JsonNode toJson() {
        return JsonSerialization.writeValueAsNode(this);
    }

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

    record Client(@JsonProperty("client_id") String clientId) {
    }
}
