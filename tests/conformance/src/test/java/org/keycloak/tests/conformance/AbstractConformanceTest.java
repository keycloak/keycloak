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

package org.keycloak.tests.conformance;

import java.util.Map;
import java.util.stream.Stream;

import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;
import org.keycloak.tests.conformance.containers.InjectConformanceSuite;
import org.keycloak.tests.conformance.containers.OpenIdConformanceSuite;
import org.keycloak.tests.conformance.runner.BrowserInteraction;
import org.keycloak.tests.conformance.runner.ConformanceModuleResult;
import org.keycloak.tests.conformance.runner.ConformanceModuleVariant;
import org.keycloak.tests.conformance.runner.ConformanceResult;

import com.fasterxml.jackson.databind.JsonNode;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Base class for all conformance areas. Test classes inject a realm and implement {@link #moduleVariants()},
 * either listing the variants explicitly or using {@link #discoverModuleVariants}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractConformanceTest {

    private static final Logger LOGGER = Logger.getLogger(AbstractConformanceTest.class);

    // Not used directly, but required to start the Keycloak server with TLS enabled
    @InjectCertificates(config = TlsCertificates.class)
    ManagedCertificates certificates;

    @InjectConformanceSuite
    protected OpenIdConformanceSuite suite;

    protected abstract Stream<ConformanceModuleVariant> moduleVariants();

    protected abstract JsonNode suiteConfig(ConformanceModuleVariant moduleVariant);

    /**
     * Discovers all variant combinations of a module from the plan. Runs while tests are collected, before the
     * test framework injects the suite, hence the singleton access.
     */
    protected Stream<ConformanceModuleVariant> discoverModuleVariants(String plan, Map<String, String> planVariant, String name) {
        return discoverModuleVariants(plan, planVariant, name, ConformanceResult.PASSED, BrowserInteraction.NONE);
    }

    protected Stream<ConformanceModuleVariant> discoverModuleVariants(String plan, Map<String, String> planVariant,
            String name, ConformanceResult expectedResult, BrowserInteraction browserInteraction) {
        ConformanceModuleVariant template = new ConformanceModuleVariant(plan, planVariant, name, Map.of(),
                expectedResult, browserInteraction);
        return OpenIdConformanceSuite.instance().client()
                .discoverModuleVariants(plan, planVariant, name, suiteConfig(template))
                .map(moduleVariant -> new ConformanceModuleVariant(plan, planVariant, name, moduleVariant,
                        expectedResult, browserInteraction));
    }

    @ParameterizedTest
    @MethodSource("moduleVariants")
    public void conformance(ConformanceModuleVariant moduleVariant) {
        ConformanceModuleResult result = suite.client().run(moduleVariant, suiteConfig(moduleVariant));
        if (!result.finishedWith(moduleVariant.expectedResult())) {
            LOGGER.errorf("Full logs of failed conformance module %s:%n%s", result.module(), result.logs().toPrettyString());
            Assertions.fail(result.failureSummary());
        }
    }

    public static class TlsCertificates implements CertificatesConfig {

        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }
}
