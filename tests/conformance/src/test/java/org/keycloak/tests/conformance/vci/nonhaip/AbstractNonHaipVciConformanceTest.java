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

import java.util.HashMap;
import java.util.Map;

import org.keycloak.tests.conformance.vci.AbstractVciConformanceTest;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Baseline for non-HAIP OID4VCI conformance tests. Non-HAIP profiles authenticate with private_key_jwt and use
 * neither client attestation nor key attestation, so the suite config advertises the client's private JWKS instead
 * of any attester JWKS.
 *
 * Unlike the HAIP plan, which pins every FAPI2/VCI variant dimension inside the plan, the non-HAIP plan
 * ({@code oid4vci-1_0-issuer-test-plan}) pins nothing, so the caller must supply the full variant selection. This
 * overrides {@link #planVariant(String)} to add the non-HAIP FAPI2 dimensions (profile {@code vci},
 * private_key_jwt, DPoP, unsigned PAR request, plain OAuth) on top of the credential format and flow variant.
 * Credential encryption is intentionally omitted so it stays discovered per module (it defaults to {@code plain}).
 */
public abstract class AbstractNonHaipVciConformanceTest extends AbstractVciConformanceTest {

    @Override
    protected Map<String, String> planVariant(String flowVariant) {
        return Map.of(
                "credential_format", "sd_jwt_vc",
                "vci_authorization_code_flow_variant", flowVariant,
                "fapi_profile", "vci",
                "client_auth_type", "private_key_jwt",
                "fapi_request_method", "unsigned",
                "openid", "plain_oauth",
                "sender_constrain", "dpop",
                "fapi_response_mode", "plain_response");
    }

    /**
     * Adds {@code vci_credential_encryption=encrypted} to a plan variant. The generic non-HAIP plan does not pin
     * credential encryption (it defaults to plain), so tests that must exercise the encrypted credential response
     * request it explicitly.
     */
    protected Map<String, String> encrypted(Map<String, String> planVariant) {
        Map<String, String> variant = new HashMap<>(planVariant);
        variant.put("vci_credential_encryption", "encrypted");
        return variant;
    }

    // Non-HAIP authenticates with private_key_jwt, so it advertises no client attestation material.
    @Override
    protected JsonNode clientAttesterJwks() {
        return null;
    }

    // Key attestation is off by default; the key-attestation tests re-enable it.
    @Override
    protected JsonNode keyAttestationJwks() {
        return null;
    }

    @Override
    protected JsonNode clientJwks() {
        return VciClientKey.privateJwks();
    }

    @Override
    protected JsonNode client2Jwks() {
        return VciClientKey.privateJwks2();
    }
}
