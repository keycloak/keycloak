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

package org.keycloak.tests.conformance.vci.nonhaip.preauth;

import java.util.HashMap;
import java.util.Map;

import org.keycloak.tests.conformance.vci.VciAiaCredentialOffer;
import org.keycloak.tests.conformance.vci.VciConformanceRealmUtil;
import org.keycloak.tests.conformance.vci.nonhaip.AbstractNonHaipVciConformanceTest;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Baseline for the pre-authorized code grant. The suite receives a pre-authorized credential offer and redeems the
 * code at the token endpoint, authenticating with client attestation as the HAIP wallet does (HAIP 1.0 section
 * 4.4.1 requires client authentication at the token endpoint). Only the issuer_initiated flow applies to this grant.
 */
public abstract class AbstractPreAuthVciConformanceTest extends AbstractNonHaipVciConformanceTest {

    @Override
    protected Map<String, String> planVariant(String credentialFormat, String flowVariant) {
        Map<String, String> variant = new HashMap<>(super.planVariant(credentialFormat, flowVariant));
        variant.put("vci_grant_type", "pre_authorization_code");
        variant.put("client_auth_type", "client_attestation");
        return variant;
    }

    @Override
    protected JsonNode clientAttesterJwks() {
        return VciConformanceRealmUtil.attesterJwks();
    }

    @Override
    protected JsonNode clientJwks() {
        return null;
    }

    @Override
    protected JsonNode client2Jwks() {
        return null;
    }

    // The suite always sends a tx_code, which Keycloak ignores as the offer has none.
    @Override
    protected String staticTxCode() {
        return "unused";
    }

    @Override
    protected String createCredentialOfferUri() {
        return VciAiaCredentialOffer.createPreAuthorizedOfferUri();
    }
}
