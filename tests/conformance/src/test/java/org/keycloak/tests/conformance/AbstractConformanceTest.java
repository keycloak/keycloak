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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.keycloak.testframework.config.Config;
import org.keycloak.testframework.conformance.OpenIdConformanceServer;
import org.keycloak.testframework.conformance.annotations.InjectOpenIdConformanceServer;
import org.keycloak.testframework.conformance.runner.BrowserInteraction;
import org.keycloak.testframework.conformance.runner.ConformanceAttempts;
import org.keycloak.testframework.conformance.runner.ConformanceModuleResult;
import org.keycloak.testframework.conformance.runner.ConformanceModuleVariant;
import org.keycloak.testframework.conformance.runner.ConformanceResult;
import org.keycloak.testframework.conformance.runner.ModuleRun;
import org.keycloak.testframework.https.CertificatesConfig;
import org.keycloak.testframework.https.CertificatesConfigBuilder;
import org.keycloak.testframework.https.InjectCertificates;
import org.keycloak.testframework.https.ManagedCertificates;

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

    /**
     * How often a module not finishing as expected, or whose run fails with an exception, is run again before the test
     * fails, see {@link ConformanceAttempts}. Set it with the {@value} test framework option (system property,
     * {@code KC_TEST_CONFORMANCE_RETRIES} environment variable or keycloak-test.properties) for all tests; without it,
     * each test uses its {@link #defaultRetries()}. 0 disables the retries.
     */
    public static final String RETRIES_PROPERTY = "kc.test.conformance.retries";

    // The aliases of the current attempt by base, null outside of an attempt, e.g. while discovering module variants
    private Map<String, String> attemptAliases;
    // Undoes what the test prepared for the aliases of the current attempt
    private final List<Runnable> attemptCleanups = new ArrayList<>();

    @InjectCertificates(config = TlsCertificates.class)
    protected ManagedCertificates certificates;

    @InjectOpenIdConformanceServer
    protected OpenIdConformanceServer server;

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
        return OpenIdConformanceServer.instance().client()
                .discoverModuleVariants(plan, planVariant, name, suiteConfig(template))
                .map(moduleVariant -> new ConformanceModuleVariant(plan, planVariant, name, moduleVariant,
                        expectedResult, browserInteraction));
    }

    /**
     * Discovers all variant combinations of every named module from a single created plan. The modules share the
     * same browser interaction and expected result, so a single test class can host a group of related modules.
     */
    protected Stream<ConformanceModuleVariant> discoverModuleVariants(String plan, Map<String, String> planVariant,
            List<String> names, ConformanceResult expectedResult, BrowserInteraction browserInteraction) {
        ConformanceModuleVariant template = new ConformanceModuleVariant(plan, planVariant, names.get(0), Map.of(),
                expectedResult, browserInteraction);
        Map<String, List<Map<String, String>>> discovered = OpenIdConformanceServer.instance().client()
                .discoverModuleVariants(plan, planVariant, names, suiteConfig(template));
        return names.stream().flatMap(name -> discovered.get(name).stream()
                .map(moduleVariant -> new ConformanceModuleVariant(plan, planVariant, name, moduleVariant,
                        expectedResult, browserInteraction)));
    }

    /**
     * Drives the system under test once the module waits for it, for example delivers an OID4VP verifier request or
     * the credential offer the issuer initiated modules wait for.
     */
    protected Consumer<ModuleRun> interaction(ConformanceModuleVariant moduleVariant) {
        return null;
    }

    /**
     * Prepares Keycloak once the module is configured and before it is started, for the modules the suite does not
     * start by itself, e.g. rotates the realm's signing keys for a module comparing them with the keys it fetched while
     * being configured.
     */
    protected Runnable beforeStart(ConformanceModuleVariant moduleVariant) {
        return null;
    }

    /**
     * How often a module of this test is run again by default, see {@link #RETRIES_PROPERTY}. Retries are opt-in, so
     * none by default.
     */
    protected int defaultRetries() {
        return 0;
    }

    /**
     * The number of attempts of a module, given the default number of retries of the test, see
     * {@link #RETRIES_PROPERTY}.
     */
    public static int attempts(int defaultRetries) {
        return 1 + Math.max(0, Config.get(RETRIES_PROPERTY, defaultRetries, Integer.class));
    }

    /**
     * The suite alias of the current run of a module, see {@link ConformanceAttempts#newAlias}: a new one for every
     * attempt, so a late callback of another run cannot reach this run's module. A test calls it in its
     * {@link #suiteConfig}. Outside of a run, e.g. while discovering module variants, it is {@code base}.
     */
    protected String suiteAlias(String base) {
        if (attemptAliases == null) {
            return base;
        }
        return attemptAliases.computeIfAbsent(base, b -> {
            String alias = ConformanceAttempts.newAlias(b);
            Runnable cleanup = aliasCreated(alias);
            if (cleanup != null) {
                attemptCleanups.add(cleanup);
            }
            return alias;
        });
    }

    /**
     * Prepares Keycloak for a run under a new suite alias, e.g. registers its callback URL as a redirect URI of the
     * static clients, as the suite routes the callbacks by the alias.
     *
     * @return what undoes it once the run finished, or {@code null}
     */
    protected Runnable aliasCreated(String alias) {
        return null;
    }

    @ParameterizedTest
    @MethodSource("moduleVariants")
    public void conformance(ConformanceModuleVariant moduleVariant) {
        ConformanceResult expectedResult = moduleVariant.expectedResult();
        ConformanceAttempts.Outcome outcome;
        try {
            outcome = ConformanceAttempts.run(moduleVariant.name(), attempts(defaultRetries()), attempt -> {
                attemptAliases = new HashMap<>();
                try {
                    // Each run creates a new plan, so an attempt does not depend on the previous one
                    return server.client()
                            .run(moduleVariant, suiteConfig(moduleVariant), beforeStart(moduleVariant), interaction(moduleVariant));
                } finally {
                    cleanUpAttempt();
                }
            }, result -> failure(result, expectedResult), AbstractConformanceTest::printRetryNotice);
        } finally {
            attemptAliases = null;
        }
        if (!outcome.succeeded()) {
            ConformanceModuleResult result = outcome.result();
            // Formatted upfront, as the test framework log filter replays buffered records without their parameters
            LOGGER.error("Full logs of failed conformance module " + result.module() + ":\n" + result.logs().toPrettyString());
            Assertions.fail((outcome.attempts() > 1 ? "All " + outcome.attempts() + " attempts failed, the last one with:\n" : "")
                    + outcome.failure());
        }
    }

    private void cleanUpAttempt() {
        for (Runnable cleanup : attemptCleanups) {
            try {
                cleanup.run();
            } catch (RuntimeException e) {
                // Formatted upfront, as the test framework log filter replays buffered records without their parameters
                LOGGER.warn("Failed to clean up after a conformance module run: " + e);
            }
        }
        attemptCleanups.clear();
    }

    /**
     * Prints to the test output rather than logging, as the test framework log filter drops the logs of a test that
     * passes, which would hide that a module passed only when run again.
     */
    private static void printRetryNotice(String message) {
        System.out.println("[conformance retry] " + message);
    }

    /**
     * Why the module did not finish as expected, or {@code null} if it did.
     */
    private static String failure(ConformanceModuleResult result, ConformanceResult expectedResult) {
        return result.finishedWith(expectedResult) ? null : result.failureSummary();
    }

    public static class TlsCertificates implements CertificatesConfig {

        @Override
        public CertificatesConfigBuilder configure(CertificatesConfigBuilder config) {
            return config.tlsEnabled(true);
        }
    }
}
