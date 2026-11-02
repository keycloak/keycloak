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

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ConformanceResult;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.conformance.vci.nonhaip.configs.PreAuthVciRealmConfig;
import org.keycloak.tests.conformance.vci.nonhaip.configs.PreAuthVciServerConfig;

import static org.keycloak.tests.conformance.vci.nonhaip.configs.NonHaipVciRealmConfig.NON_HAIP_PLAN;

/**
 * The pre-authorized code grant skips PAR, so the token request is the first authenticated request and the token
 * endpoint must reject an invalid client attestation or attestation PoP with invalid_client.
 */
@KeycloakIntegrationTest(config = PreAuthVciServerConfig.class)
public class IssuerPreAuthInvalidClientAttestationTest extends AbstractPreAuthVciConformanceTest {

    @InjectRealm(config = PreAuthVciRealmConfig.class)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(
                NON_HAIP_PLAN,
                issuerInitiated(),
                List.of(
                        "oid4vci-1_0-issuer-fail-invalid-client-attestation-signature",
                        "oid4vci-1_0-issuer-fail-invalid-client-attestation-pop-signature",
                        "oid4vci-1_0-issuer-fail-client-attestation-exp-in-past",
                        "oid4vci-1_0-issuer-fail-client-attestation-no-sub",
                        "oid4vci-1_0-issuer-fail-client-attestation-pop-wrong-aud",
                        "oid4vci-1_0-issuer-fail-mismatched-client-attestation-pop-key"),
                ConformanceResult.PASSED,
                BrowserInteraction.NONE);
    }
}
