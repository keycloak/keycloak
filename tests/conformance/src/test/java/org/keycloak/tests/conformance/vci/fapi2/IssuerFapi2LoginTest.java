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

package org.keycloak.tests.conformance.vci.fapi2;

import java.util.List;
import java.util.stream.Stream;

import org.keycloak.testframework.annotations.InjectRealm;
import org.keycloak.testframework.annotations.KeycloakIntegrationTest;
import org.keycloak.testframework.injection.LifeCycle;
import org.keycloak.testframework.realm.ManagedRealm;
import org.keycloak.tests.conformance.runner.BrowserInteraction;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;
import org.keycloak.tests.conformance.runner.ConformanceResult;
import org.keycloak.tests.conformance.vci.AbstractVciConformanceTest;
import org.keycloak.tests.conformance.vci.VciConformanceRealmConfig;

@KeycloakIntegrationTest(config = VciConformanceRealmConfig.ServerConfig.class)
public class IssuerFapi2LoginTest extends AbstractVciConformanceTest {

    private static final List<String> MODULES = List.of(
            "fapi2-security-profile-final-happy-flow",
            "fapi2-security-profile-final-ensure-authorization-request-without-state-success",
            "fapi2-security-profile-final-ensure-authorization-request-with-long-state",
            "fapi2-security-profile-final-access-token-type-header-case-sensitivity",
            "fapi2-security-profile-final-check-dpop-proof-nbf-exp",
            "fapi2-security-profile-final-ensure-dpopproof-with-iat-10seconds-before-succeeds",
            "fapi2-security-profile-final-ensure-dpopproof-with-iat-10seconds-after-succeeds",
            "fapi2-security-profile-final-ensure-dpopproof-at-par-endpoint-binding-success",
            "fapi2-security-profile-final-ensure-dpop-auth-code-binding-success",
            "fapi2-security-profile-final-ensure-mismatched-dpop-jkt-fails",
            "fapi2-security-profile-final-ensure-token-endpoint-fails-with-mismatched-dpop-proof-jkt",
            "fapi2-security-profile-final-ensure-token-endpoint-fails-with-mismatched-dpop-jkt",
            "fapi2-security-profile-final-dpop-negative-tests",
            "fapi2-security-profile-final-ensure-client-id-in-token-endpoint",
            "fapi2-security-profile-final-ensure-authorization-code-is-bound-to-client",
            "fapi2-security-profile-final-attempt-reuse-authorization-code-after-one-second",
            "fapi2-security-profile-final-ensure-token-endpoint-fails-with-expired-auth-code",
            "fapi2-security-profile-final-par-without-duplicate-parameters",
            "fapi2-security-profile-final-ensure-pkce-code-verifier-required",
            "fapi2-security-profile-final-incorrect-pkce-code-verifier-rejected",
            "fapi2-security-profile-final-ensure-response-type-code-idtoken-fails",
            "fapi2-security-profile-final-ensure-response-type-token-fails",
            "fapi2-security-profile-final-ensure-redirect-uri-in-authorization-request",
            "fapi2-security-profile-final-plain-fapi-tolerate-unregistered-redirect-uri",
            "fapi2-security-profile-final-ensure-different-state-inside-and-outside-request-object");

    @InjectRealm(config = VciConformanceRealmConfig.class, lifecycle = LifeCycle.METHOD)
    ManagedRealm realm;

    @Override
    protected Stream<ConformanceModuleVariant> moduleVariants() {
        return discoverModuleVariants(HAIP_PLAN, WALLET_INITIATED, MODULES,
                ConformanceResult.PASSED, BrowserInteraction.LOGIN);
    }
}
