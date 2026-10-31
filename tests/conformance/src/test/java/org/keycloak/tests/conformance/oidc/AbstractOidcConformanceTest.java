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

package org.keycloak.tests.conformance.oidc;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ConformanceResult;
import org.keycloak.tests.conformance.AbstractConformanceTest;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Baseline for the OpenID Connect Core certification plans. Keycloak is tested with discovery and statically
 * registered clients; each certification plan pins the response type and client authentication of its modules.
 *
 * Without the protocol mappers of {@link org.keycloak.tests.conformance.oidc.configs.OidcRealmConfig} some modules
 * would finish with a WARNING because of Keycloak behaviour the specification permits but the suite flags:
 * <ul>
 * <li>the ID token always carries the {@code typ} claim ({@code oidcc-server})</li>
 * <li>the claims of a requested scope, e.g. email, are put in the ID token even when an access token is issued for
 * the userinfo endpoint ({@code oidcc-scope-email}, {@code oidcc-alternate-happy-flow})</li>
 * <li>an essential {@code name} claim requested through the claims parameter alone is not returned
 * ({@code oidcc-claims-essential})</li>
 * </ul>
 */
public abstract class AbstractOidcConformanceTest extends AbstractConformanceTest {

    protected abstract String plan();

    protected Map<String, String> planVariant() {
        return Map.of(
                "server_metadata", "discovery",
                "client_registration", "static_client");
    }

    /**
     * Discovers the variant combinations of the named modules, which share the expected result and browser
     * interaction. A module the plan runs with several response types is discovered once per response type.
     */
    protected Stream<ConformanceModuleVariant> modules(ConformanceResult expectedResult,
            BrowserInteraction browserInteraction, String... names) {
        return discoverModuleVariants(plan(), planVariant(), List.of(names), expectedResult, browserInteraction);
    }

    /**
     * Like {@link #modules(ConformanceResult, BrowserInteraction, String...)}, for the plan created with the given plan
     * variant, e.g. for a plan whose response type is a plan variant.
     */
    protected Stream<ConformanceModuleVariant> modules(Map<String, String> planVariant, ConformanceResult expectedResult,
            BrowserInteraction browserInteraction, String... names) {
        return discoverModuleVariants(plan(), planVariant, List.of(names), expectedResult, browserInteraction);
    }

    @Override
    protected JsonNode suiteConfig(ConformanceModuleVariant module) {
        return OidcSuiteConfig.create(OpenIdConformanceServer.KEYCLOAK_BASE_URI, module.browserInteraction()).toJson();
    }
}
