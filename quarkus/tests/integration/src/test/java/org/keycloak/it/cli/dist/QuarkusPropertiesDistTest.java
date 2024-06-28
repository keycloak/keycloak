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

import java.util.Optional;
import java.util.function.Consumer;

import io.restassured.RestAssured;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
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

    private static final String QUARKUS_BUILDTIME_HIBERNATE_METRICS_KEY = "quarkus.datasource.metrics.enabled";
    private static final String QUARKUS_RUNTIME_CONSOLE_LOGLVL_KEY = "quarkus.log.console.level";

    @Test
    @Launch({ "build" })
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
    @Launch({ "build", "--metrics-enabled=true" })
    @Order(7)
    void buildFirstWithUnknownQuarkusBuildProperty(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
    }

    @Test
    @KeepServerAlive
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", OPTIMIZED_BUILD_OPTION_LONG})
    @Order(8)
    void testUnknownQuarkusBuildTimePropertyApplied(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertNoBuild();
        RestAssured.port = 9000;
        when().get("/metrics").then().statusCode(200)
                .body(containsString("jvm_gc_"));
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--config-keystore=../../../../src/test/resources/keystore" })
    @Order(9)
    void testMissingSmallRyeKeyStorePasswordProperty(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(
                Optional.of(cliResult.getErrorOutput())
                        .filter(s -> s.contains("config-keystore-password must be specified")
                                || s.contains("is required but it could not be found in any config source"))
                        .isPresent(),
                () -> "The Error Output:\n " + cliResult.getErrorOutput() + " doesn't warn about the missing password");
    }

    @Disabled("Ensuring config-keystore is used only at runtime removes proactive validation of the path when only the keystore is used")
    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--config-keystore-password=secret" })
    @Order(10)
    void testMissingSmallRyeKeyStorePathProperty(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertBuild();
        cliResult.assertError("config-keystore must be specified");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--config-keystore=/invalid/path",
            "--config-keystore-password=secret" })
    @Order(11)
    void testInvalidSmallRyeKeyStorePathProperty(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        cliResult.assertError("java.lang.IllegalArgumentException: config-keystore path does not exist: /invalid/path");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false",
            "--config-keystore=../../../../src/test/resources/keystore", "--config-keystore-password=secret" })
    @Order(12)
    void testSmallRyeKeyStoreConfigSource(LaunchResult result) {
        // keytool -importpass -alias kc.log-level -keystore keystore -storepass secret -storetype PKCS12 -v (with "debug" as the stored password)
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("DEBUG"));
        cliResult.assertStarted();
    }

    @Test
    @BeforeStartDistribution(ForceRebuild.class)
    @DisabledOnOs(value = { OS.WINDOWS }, disabledReason = "Windows uses a different path separator.")
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false",
            "--https-certificate-file=/tmp/kc/bin/../conf/server.crt.pem",
            "--https-certificate-key-file=/tmp/kc/bin/../conf/server.key.pem" })
    @Order(13)
    void testHttpCertsPathTransformer(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("ERROR: /tmp/kc/bin/../conf/server.crt.pem"));
    }

    @Test
    @BeforeStartDistribution(ForceRebuild.class)
    @DisabledOnOs(value = { OS.LINUX, OS.MAC }, disabledReason = "Windows uses a different path separator.")
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false",
            "--https-certificate-file=C:\\tmp\\kc\\bin\\..\\conf/server.crt.pem",
            "--https-certificate-key-file=C:\\tmp\\kc\\bin\\..\\conf/server.key.pem" })
    @Order(14)
    void testHttpCertsPathTransformerOnWindows(LaunchResult result) {
        CLIResult cliResult = (CLIResult) result;
        assertTrue(cliResult.getOutput().contains("ERROR: C:/tmp/kc/bin/../conf/server.crt.pem"));
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

    public static class ForceRebuild implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            CLIResult buildResult = distribution.run("build");
            buildResult.assertBuild();
        }
    }
}