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

import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ModuleRun;
import org.keycloak.tests.conformance.AbstractConformanceTest;
import org.keycloak.tests.conformance.vci.haip.HaipVciConformanceRealmConfig;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Baseline for OID4VCI conformance tests. Test classes inject a realm with {@link HaipVciConformanceRealmConfig} or
 * a subclass of it for additional configuration.
 */
public abstract class AbstractVciConformanceTest extends AbstractConformanceTest {

    protected Map<String, String> walletInitiated() {
        return planVariant("wallet_initiated");
    }

    protected Map<String, String> issuerInitiated() {
        return planVariant("issuer_initiated");
    }

    /**
     * The plan variant selection sent to the conformance suite for the given authorization code flow variant. The
     * HAIP plan pins every other dimension itself, so the baseline supplies only the credential format and flow
     * variant; {@link org.keycloak.tests.conformance.vci.nonhaip.AbstractNonHaipVciConformanceTest} overrides this
     * to add the FAPI2 dimensions the generic non-HAIP plan does not pin. Credential encryption is intentionally
     * omitted so it stays discovered per module (it defaults to plain).
     */
    protected Map<String, String> planVariant(String flowVariant) {
        return Map.of(
                "credential_format", "sd_jwt_vc",
                "vci_authorization_code_flow_variant", flowVariant);
    }

    @Override
    protected JsonNode suiteConfig(ConformanceModuleVariant module) {
        return VciSuiteConfig.create(
                OpenIdConformanceServer.KEYCLOAK_BASE_URI,
                VciConformanceRealmUtil.credentialConfigurationId(module.planVariant().get("credential_format")),
                clientAttesterJwks(),
                keyAttestationJwks(),
                clientJwks(),
                client2Jwks(),
                VciTestSigningKey.caCertificatePem(),
                module.browserInteraction()).toJson();
    }

    /**
     * The client attester JWKS advertised to the conformance suite for client attestation based authentication.
     * HAIP publishes it; non-HAIP uses private_key_jwt and overrides this to {@code null}, which omits the client
     * attestation material from the suite config.
     */
    protected JsonNode clientAttesterJwks() {
        return VciConformanceRealmUtil.attesterJwks();
    }

    /**
     * The key attestation JWKS advertised to the conformance suite for key attestations on the credential proof.
     * HAIP publishes it; non-HAIP overrides this to {@code null} by default and re-enables it only in the tests
     * that exercise key attestation.
     */
    protected JsonNode keyAttestationJwks() {
        return VciConformanceRealmUtil.attesterJwks();
    }

    /**
     * The private JWKS the conformance suite signs the first client's private_key_jwt assertions with. HAIP
     * authenticates with client attestation and returns {@code null} here (the {@code jwks} client field is then
     * omitted); non-HAIP overrides this to publish the conformance client's private key.
     */
    protected JsonNode clientJwks() {
        return null;
    }

    /**
     * The private JWKS for the second conformance client, which must differ from {@link #clientJwks()} for the
     * multiple-clients module. HAIP returns {@code null}; non-HAIP overrides it.
     */
    protected JsonNode client2Jwks() {
        return null;
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
        return moduleRun -> server.client().visitTestEndpoint(alias, "credential_offer",
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
