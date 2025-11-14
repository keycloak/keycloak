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

import java.util.Optional;
import java.util.function.Consumer;

import org.keycloak.it.junit5.extension.BeforeStartDistribution;
import org.keycloak.it.junit5.extension.CLIResult;
import org.keycloak.it.junit5.extension.DistributionTest;
import org.keycloak.it.junit5.extension.DryRun;
import org.keycloak.it.junit5.extension.KeepServerAlive;
import org.keycloak.it.junit5.extension.RawDistOnly;
import org.keycloak.it.junit5.extension.WithEnvVars;
import org.keycloak.it.utils.KeycloakDistribution;

import io.quarkus.test.junit.main.Launch;
import io.restassured.RestAssured;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static io.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DistributionTest(reInstall = DistributionTest.ReInstall.NEVER, defaultOptions = "--db=dev-file")
@WithEnvVars({"KC_CACHE", "local"}) // avoid flakey port conflicts
@RawDistOnly(reason = "Containers are immutable")
@Tag(DistributionTest.WIN)
@TestMethodOrder(OrderAnnotation.class)
public class QuarkusPropertiesDistTest {

    private static final String QUARKUS_BUILDTIME_HIBERNATE_METRICS_KEY = "quarkus.datasource.metrics.enabled";
    private static final String QUARKUS_RUNTIME_CONSOLE_HANDLER_ENABLED_KEY = "quarkus.log.handler.console.\"console-2\".enable";

    @DryRun
    @Test
    @Launch({"build"})
    @Order(1)
    void testBuildWithPropertyFromQuarkusProperties(CLIResult cliResult) {
        cliResult.assertBuild();
    }

    @Test
    @BeforeStartDistribution(QuarkusPropertiesDistTest.AddConsoleHandlerFromQuarkusProps.class)
    @Launch({"start", "--http-enabled=true", "--hostname-strict=false"})
    @Order(2)
    void testPropertyEnabledAtRuntime(CLIResult cliResult) {
        cliResult.assertMessage("Keycloak is the best");
    }

    @Test
    @Launch({"-Dquarkus.log.handler.console.\"console-2\".enable=false", "start", "--http-enabled=true", "--hostname-strict=false"})
    @DisabledOnOs(value = { OS.WINDOWS })
    @Order(3)
    void testIgnoreQuarkusSystemPropertiesAtStart(CLIResult cliResult) {
        cliResult.assertMessage("Keycloak is the best");
    }

    @Test
    @Launch({"-Dquarkus.log.handler.console.\\\"console-2\\\".enable=false", "start", "--http-enabled=true", "--hostname-strict=false"})
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "Different handling of quotes within arguments on Windows")
    @Order(3)
    void testIgnoreQuarkusSystemPropertiesAtStartWin(CLIResult cliResult) {
        cliResult.assertMessage("Keycloak is the best");
    }

    @Test
    @Launch({"-Dquarkus.log.handler.console.\"console-2\".enable=false", "build"})
    @DisabledOnOs(value = { OS.WINDOWS })
    @Order(4)
    void testIgnoreQuarkusSystemPropertyAtBuild(CLIResult cliResult) {
        cliResult.assertMessage("Keycloak is the best");
        cliResult.assertBuild();
    }

    @Test
    @Launch({"-Dquarkus.log.handler.console.\\\"console-2\\\".enable=false", "build"})
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "Different handling of quotes within arguments on Windows")
    @Order(4)
    void testIgnoreQuarkusSystemPropertyAtBuildWin(CLIResult cliResult) {
        cliResult.assertMessage("Keycloak is the best");
        cliResult.assertBuild();
    }

    @DryRun
    @Test
    @BeforeStartDistribution(UpdateConsoleHandlerFromKeycloakConf.class)
    @Launch({"build"})
    @Order(5)
    void testIgnoreQuarkusPropertyFromKeycloakConf(CLIResult cliResult) {
        cliResult.assertNoMessage("Keycloak is the best");
        cliResult.assertBuild();
    }

    @DryRun
    @Test
    @BeforeStartDistribution(UpdateConsoleHandlerFromQuarkusProps.class)
    @Launch({"start", "--http-enabled=true", "--hostname-strict=false"})
    @Order(6)
    @Disabled(value = "We don't properly differentiate between quarkus runtime and build time properties")
    void testRuntimePropFromQuarkusPropsIsAppliedWithoutRebuild(CLIResult cliResult) {
        cliResult.assertNoMessage("Keycloak is the best");
        cliResult.assertNoBuild();
    }

    @Test
    @BeforeStartDistribution(UpdateHibernateMetricsFromQuarkusProps.class)
    @Launch({ "build", "--metrics-enabled=true" })
    @Order(7)
    void buildFirstWithUnknownQuarkusBuildProperty(CLIResult cliResult) {
        cliResult.assertBuild();
    }

    @Test
    @KeepServerAlive
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--metrics-enabled=true"})
    @Order(8)
    void testUnknownQuarkusBuildTimePropertyApplied(CLIResult cliResult) {
        cliResult.assertNoBuild();
        RestAssured.port = 9000;
        when().get("/metrics").then().statusCode(200)
                .body(containsString("jvm_gc_"));
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--config-keystore=../../../../src/test/resources/keystore" })
    @Order(9)
    void testMissingSmallRyeKeyStorePasswordProperty(CLIResult cliResult) {
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
    void testMissingSmallRyeKeyStorePathProperty(CLIResult cliResult) {
        cliResult.assertBuild();
        cliResult.assertError("config-keystore must be specified");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--config-keystore=/invalid/path",
            "--config-keystore-password=secret" })
    @DisabledOnOs(value = { OS.WINDOWS })
    @Order(11)
    void testInvalidSmallRyeKeyStorePathProperty(CLIResult cliResult) {
        cliResult.assertError("java.lang.IllegalArgumentException: config-keystore path does not exist: /invalid/path");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false", "--config-keystore=C:\\invalid\\path",
            "--config-keystore-password=secret" })
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "Windows uses a different path separator.")
    @Order(11)
    void testInvalidSmallRyeKeyStorePathPropertyWin(CLIResult cliResult) {
        cliResult.assertError("java.lang.IllegalArgumentException: config-keystore path does not exist: C:\\invalid\\path");
    }

    @Test
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false",
            "--config-keystore=../../../../src/test/resources/keystore", "--config-keystore-password=secret" })
    @Order(12)
    void testSmallRyeKeyStoreConfigSource(CLIResult cliResult) {
        // keytool -importpass -alias kc.log-level -keystore keystore -storepass secret -storetype PKCS12 -v (with "org.keycloak.timer:debug" as the stored password)
        cliResult.assertNoMessage("DEBUG [org.keycloak.services");
        cliResult.assertMessage("DEBUG [org.keycloak.timer");
        cliResult.assertStarted();
    }

    @Test
    @BeforeStartDistribution(ForceRebuild.class)
    @DisabledOnOs(value = { OS.WINDOWS }, disabledReason = "Windows uses a different path separator.")
    @Launch({ "start", "--verbose", "--http-enabled=true", "--hostname-strict=false",
            "--https-certificate-file=/tmp/kc/bin/../conf/server.crt.pem",
            "--https-certificate-key-file=/tmp/kc/bin/../conf/server.key.pem" })
    @Order(13)
    void testHttpCertsPathTransformer(CLIResult cliResult) {
        cliResult.assertExitCode(1);
        cliResult.assertMessage("Failed to load 'https-*' material: NoSuchFileException");
    }

    @Test
    @BeforeStartDistribution(ForceRebuild.class)
    @EnabledOnOs(value = { OS.WINDOWS }, disabledReason = "Windows uses a different path separator.")
    @Launch({ "start", "--http-enabled=true", "--hostname-strict=false",
            "--https-certificate-file=C:\\tmp\\kc\\bin\\..\\conf/server.crt.pem",
            "--https-certificate-key-file=C:\\tmp\\kc\\bin\\..\\conf/server.key.pem" })
    @Order(14)
    void testHttpCertsPathTransformerOnWindows(CLIResult cliResult) {
        cliResult.assertExitCode(1);
        cliResult.assertMessage("ERROR: Failed to load 'https-*' material: NoSuchFileException C:");
    }

    public static class AddConsoleHandlerFromQuarkusProps implements Consumer<KeycloakDistribution> {
        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.setQuarkusProperty(QUARKUS_RUNTIME_CONSOLE_HANDLER_ENABLED_KEY, "true");
            distribution.setQuarkusProperty("quarkus.log.handler.console.\"console-2\".format", "Keycloak is the best");
            distribution.setQuarkusProperty("quarkus.log.handlers", "console-2");
        }
    }

    public static class UpdateConsoleHandlerFromKeycloakConf implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.deleteQuarkusProperties();
            distribution.setProperty(QUARKUS_RUNTIME_CONSOLE_HANDLER_ENABLED_KEY, "false");
        }
    }

    public static class UpdateConsoleHandlerFromQuarkusProps implements Consumer<KeycloakDistribution> {

        @Override
        public void accept(KeycloakDistribution distribution) {
            distribution.deleteQuarkusProperties();
            distribution.setQuarkusProperty(QUARKUS_RUNTIME_CONSOLE_HANDLER_ENABLED_KEY, "true");
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
