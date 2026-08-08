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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

import org.keycloak.tests.conformance.AbstractConformanceTest;
import org.keycloak.tests.conformance.containers.OpenIdConformanceSuite;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;
import org.keycloak.tests.conformance.runner.ModuleRun;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Baseline for OID4VCI conformance tests. Test classes inject a realm with {@link VciConformanceRealmConfig} or
 * a subclass of it for additional configuration.
 */
public abstract class AbstractVciConformanceTest extends AbstractConformanceTest {

    protected static final String HAIP_PLAN = "oid4vci-1_0-issuer-haip-test-plan";

    // The plan variant pins every dimension but vci_credential_encryption, leaving the plain and encrypted
    // variants to be discovered per module. mdoc is intentionally excluded as Keycloak does not support it.
    protected static final Map<String, String> WALLET_INITIATED = Map.of(
            "credential_format", "sd_jwt_vc",
            "vci_authorization_code_flow_variant", "wallet_initiated");

    protected static final Map<String, String> ISSUER_INITIATED = Map.of(
            "credential_format", "sd_jwt_vc",
            "vci_authorization_code_flow_variant", "issuer_initiated");

    @Override
    protected JsonNode suiteConfig(ConformanceModuleVariant module) {
        return VciSuiteConfig.create(
                OpenIdConformanceSuite.KEYCLOAK_BASE_URI,
                VciConformanceRealmConfig.attesterJwks(),
                VciTestSigningKey.caCertificatePem(),
                module.browserInteraction()).toJson();
    }

    @Override
    protected Consumer<ModuleRun> interaction(ConformanceModuleVariant module) {
        if (!"issuer_initiated".equals(module.planVariant().get("vci_authorization_code_flow_variant"))) {
            return null;
        }
        // The issuer_initiated variant has the suite wait for a credential offer at its credential_offer
        // endpoint. Keycloak creates the offer through the AIA login, and the suite then fetches it from the
        // delivered credential_offer_uri and runs the normal authorization code flow.
        String alias = suiteConfig(module).path("alias").asText();
        return moduleRun -> suite.client().visitTestEndpoint(alias, "credential_offer",
                "credential_offer_uri=" + URLEncoder.encode(createCredentialOfferUri(), StandardCharsets.UTF_8));
    }

    /**
     * Creates the credential offer an issuer application delivers to the wallet in the issuer_initiated flow.
     * The offer must not have a target client, as the conformance suite redeems it with its own wallet client.
     */
    protected String createCredentialOfferUri() {
        return VciAiaCredentialOffer.createOfferUri();
    }
}
