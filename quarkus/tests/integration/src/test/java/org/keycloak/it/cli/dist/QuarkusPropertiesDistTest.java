/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.it.cli.dist;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.keycloak.quarkus.runtime.cli.command.AbstractStartCommand.OPTIMIZED_BUILD_OPTION_LONG;

import java.util.function.Consumer;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.KeepServerAlive;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;

@DistributionTest(reInstall = DistributionTest.ReInstall.NEVER)
@RawDistOnly(reason = "Containers are immutable")
@TestMethodOrder(OrderAnnotation.class)
public class QuarkusPropertiesDistTest {

    private static final String QUARKUS_BUILDTIME_HIBERNATE_METRICS_KEY = "quarkus.hibernate-orm.metrics.enabled";
    private static final String QUARKUS_RUNTIME_CONSOLE_LOGLVL_KEY = "quarkus.log.console.level";

    @Test
    @Launch({ "build", "--cache=local" })
    @Order(1)
    void testBuildWithPropertyFromQuarkusProperties(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
    }

    @Test
    @BeforeStartDistribution(QuarkusPropertiesDistTest.UpdateConsoleLogLevelToWarnFromQuarkusProps.class)
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false" })
    @Order(2)
    void testPropertyEnabledAtRuntime(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("INFO"));
    }

    @Test
    @Launch({ "-Dquarkus.log.console.level=info", "start", "--http-enabled=true", "--hostname-strict=false" })
    @Order(3)
    void testIgnoreQuarkusSystemPropertiesAtStart(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("INFO"));
    }

    @Test
    @Launch({ "-Dquarkus.log.console.level=info", "build" })
    @Order(4)
    void testIgnoreQuarkusSystemPropertyAtBuild(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertFalse(cliResult.getOutput().contains("INFO"));
        cliResult.assertBuild();
    }

    @Test
    @BeforeStartDistribution(UpdateConsoleLogLevelToInfoFromKeycloakConf.class)
    @Launch({ "build" })
    @Order(5)
    void testIgnoreQuarkusPropertyFromKeycloakConf(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("INFO"));
        cliResult.assertBuild();
    }

    @Test
    @BeforeStartDistribution(UpdateConsoleLogLevelToInfoFromQuarkusProps.class)
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false" })
    @Order(6)
    void testRuntimePropFromQuarkusPropsIsAppliedWithoutRebuild(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("INFO"));
        cliResult.assertNoBuild();
    }

    @Test
    @BeforeStartDistribution(UpdateHibernateMetricsFromQuarkusProps.class)
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false" })
    @Order(7)
    void testBuildRunTimeMismatchOnQuarkusBuildPropWarning(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuildRuntimeMismatchWarning(QUARKUS_BUILDTIME_HIBERNATE_METRICS_KEY);
    }

    @Test
    @BeforeStartDistribution(UpdateHibernateMetricsFromQuarkusProps.class)
    @Launch({ "build", "--metrics-enabled=true" })
    @Order(8)
    void buildFirstWithUnknownQuarkusBuildProperty(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
    }

    @Test
    @KeepServerAlive
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", OPTIMIZED_BUILD_OPTION_LONG})
    @Order(9)
    void testUnknownQuarkusBuildTimePropertyApplied(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoBuild();
        when().get("/metrics").then().statusCode(200)
                .body(containsString("vendor_hibernate_cache_query_plan_total"));
    }

    public static class UpdateConsoleLogLevelToWarnFromQuarkusProps implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setQuarkusProperty(QUARKUS_RUNTIME_CONSOLE_LOGLVL_KEY, "WARN");
        }
    }

    public static class UpdateConsoleLogLevelToInfoFromKeycloakConf implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.deleteQuarkusProperties();
            distribution.setProperty(QUARKUS_RUNTIME_CONSOLE_LOGLVL_KEY, "INFO");
        }
    }

    public static class UpdateConsoleLogLevelToInfoFromQuarkusProps implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.deleteQuarkusProperties();
            distribution.setQuarkusProperty(QUARKUS_RUNTIME_CONSOLE_LOGLVL_KEY, "INFO");
        }
    }

    public static class UpdateHibernateMetricsFromQuarkusProps implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.deleteQuarkusProperties();
            distribution.setQuarkusProperty(QUARKUS_BUILDTIME_HIBERNATE_METRICS_KEY, "true");
        }
    }
}